# Lloyd ERP (nkpcore/lloyd-erp) – Code Review Findings

Static code review of the uploaded repository snapshot · October 30, 2026

## Scope and caveats

I read about 60% of the code: the Android networking, auth, storage, widget, worker, OTA and access-control layers, `server.js`, `api/*`, the admin UI, CI and build config. I did not read most of the Compose screens, ViewModels, the campus Wi-Fi/portal code (`CampusConnectManager`, `DirectHttpPortalAuthenticator`, `WebViewPortalAuthenticator`), `BiometricCredentialVault`, the PWA or `tools/*.py`. I did not run the build or the tests, so every item comes from reading the code. The first three Critical items should be fixed first.




**5. Notification burst.**

- `addSeenAttendanceIds` caps the seen set at 200 using `HashSet.toList().subList(...)`, which has no order (`AppPreferences.kt:166-176`).
- A semester has more than 200 records, so after the first sync random old IDs fall out of the set and `detectNewMarks` re-alerts on old lectures.
- Also, if the first sync has no logs, the bootstrap flag is never set and the first real record is silently swallowed (`AttendanceSyncWorker.kt:118-137`).

**6. Surprise logouts and stalled background sync.**

- `refreshToken()` swallows every exception and returns false (`ErpApiClient.kt:71-94`). `handleUnauthorized()` then wipes both tokens, so a brief network drop on campus Wi-Fi logs the user out permanently.
- The access token lives only in memory (`KeystoreTokenStore`), so every worker or widget run in a fresh process must refresh first.
- `@Synchronized` does not help because a new `ErpApiClient` is created per call. The worker, widget and UI can race on a rotating refresh token.
- The worker treats any "auth expired" result as fatal and calls `cancelAllWork()`, which cancels all WorkManager jobs. Only `MainComposeActivity.onCreate` reschedules, so alerts stop until the app is reopened.

**7. Unbounded recursion on 401.** `getWeeklyAttendance`, `getProfile`, `getDashboardProfile`, `fetchInternetCredentials`, `getFeeTransactions`, `getFeeReceipts` and `getReceiptPrintHtml` call themselves after a refresh with no retry guard and the response still open. Only the monthly and logs calls have `isRetry`.

**8. Attendance logs can be silently truncated.** In `ErpApiClient.kt:259-284`, a failed page does `return@use` and the partial list is cached as if complete. It also stops at 20 pages. A failure on page 1 returns an empty list instead of an error.

**9. The widget security fix is incomplete.**

- The widget provider is exported and also handles `ACTION_REFRESH_WIDGET` (`AttendanceWidgetProvider.kt:252-258`), so any app can trigger it and bypass the signature-protected receiver.
- The package check in `AttendanceWidgetRefreshReceiver` does not help, because an attacker simply leaves the package null.
- The refresh runs on a raw executor after `onReceive` returns, without `goAsync()`, so the process can be killed mid-refresh.

**10. OTA integrity is weak.**

- SHA-256 is optional, and neither `api/ota.js` nor `server.js` ever sends one, even though CI produces `.sha256` files. The GitHub fallback path has no hash at all.
- The allowlist accepts any URL on `github.com`, and OkHttp follows redirects without rechecking the host.
- `file_paths.xml` exposes the whole external storage root.
- Fix: publish the hash from CI and require it, restrict URLs to your repo's releases path, and narrow `file_paths.xml` to `cache/updates/`.

**11. Telemetry fields are dropped in production.**

- The Android app sends `app_version`, `version_code` and `student_name`.
- The `api/telemetry.js:47-50` allowlist keeps `app_version_code` and `app_version_name` instead, so all three real fields are dropped.
- Version stats, "Outdated" badges and the latest-version fallback in `ota.js` and `config.js` are always empty.

**12. Config changes can silently fail to save.**

- `db.getConfig` falls back to stale memory or the committed `fleet_config.json` (no bans) when Upstash times out at 1.2s or on a cold start.
- `saveConfig` swallows write failures and returns success.
- The admin UI also says "Action executed locally (offline)" when a ban call fails, so a failed revoke looks done.

**13. `server.js` exposes data if anyone hosts it.** `GET /telemetry` is unauthenticated and returns student IDs, names and device models. The same server accepts arbitrary telemetry (which can poison the "latest version") and returns ban lists. Production runs `api/*.js` per `vercel.json`, but `npm start` runs `server.js`. Delete it or bring it in line with `api/`.

## Medium

- **Plaintext personal data.** Profile, stats, monthly, weekly and log JSON, username and student ID are in plain `MODE_PRIVATE` SharedPreferences. Only the tokens use Keystore. The README claims everything is encrypted.
- **Network config.** `network_security_config.xml` allows cleartext for every domain. Limit it to the captive-portal and connectivity-check hosts. The portal login also sends Wi-Fi credentials over plain HTTP.
- **Possible wrong-student credentials.** `fetchInternetCredentials` takes the first search hit where `studentId == null` (`ErpApiClient.kt:444-473`). That can return another student's Wi-Fi credentials. Require an exact match.
- **Widget.**
  - The `customStatus` message is ignored whenever cached stats exist, so offline mode still shows "Live".
  - The REFRESHING badge gets overwritten by "NOT LOGGED IN".
  - Raw `e.message` is shown to the user, and `resetThrottleForTesting` is in production code.
  - Both the widget (30 min) and the worker (60 min) poll. The README says "instant" alerts, but the worker runs hourly and Doze delays it further.
- **Notifications.**
  - No `POST_NOTIFICATIONS` check before `notify`.
  - Notification IDs are `% 100_000`, so collisions are possible.
  - Attendance details show on the lock screen. Set the visibility to private.
- **Resource leaks.** `downloadAndInstallApk` creates an unscoped `CoroutineScope`. Responses are not closed on non-2xx in `AccessControlManager`, `OtaUpdateManager` and `TelemetryManager`. Downloads have no size cap, and `.part` files are not cleaned up on error.
- **Dead code.** The `period.also { it }` loop in `getWeeklyAttendance` does nothing, despite its comment about copying day and date.
- **`server.js` robustness.**
  - GET handlers have no try/catch, so a database error crashes the process.
  - No negative caching for the GitHub release lookup.
  - `/ota/download` needs `GITHUB_TOKEN` even for public repos, though `/config` advertises it.
  - URLs are built from the `Host` header.
  - The ban toggle has a read-modify-write race.
  - No security headers (CSP, X-Frame-Options) in `server.js` or `vercel.json`.
- **CI.**
  - `${{ inputs.version_tag }}` is interpolated straight into shell scripts, and the `curl` JSON is unescaped.
  - Actions are pinned by tag rather than SHA.
  - The `versionCode` formula differs between CI (`200 + run_number`) and Gradle (`200 + commit count`).
  - Two APKs (about 3.4 MB each) are committed in `web/downloads/` and are the OTA fallback. They can go stale.
- **Privacy.** The app sends student ID, name and device model to your Vercel server without disclosure. Consider consent and data minimization, since India's DPDP Act may apply (not legal advice).
- **Scriptable widget.** It needs a plaintext `PASSWORD` constant in the script and never refreshes tokens.
- **Alpha dependencies.** `security-crypto:1.1.0-alpha06` and `biometric:1.2.0-alpha05` are alpha releases.

## Low and cleanup

- Add `dataExtractionRules` for Android 12+ device transfer. `allowBackup=false` is already set.
- `SimulationEngine` does not clamp present > total, and its 70% "borderline" is hardcoded.
- The two Keystore store classes duplicate each other.
- The app uses two different device IDs: `AppPreferences` for login and `TelemetryManager` for the fleet.
- The license is MIT in the README and ISC in `package.json`, and `package.json` has a copy-pasted description and a `main` pointing at the Scriptable file.
- `server.js` says "zero-dependency" but `package.json` depends on `redis`.
- The repo holds a lot of generated planning docs (`docs/superpowers`, `AGY_*`, a 62 KB prompt file) and the AKTU PDF twice.
- The Android unit tests looked thin from the directory listing, but I only skimmed them.

## Already done well

The bunk and "classes needed" math is correct and uses integer arithmetic. Admin password comparison is constant-time. TLS is strict for the ERP domain. The widget receiver uses a signature permission. The PendingIntents use `FLAG_IMMUTABLE`. Pagination has a page cap. The CI also runs tests and lint.




## most imp
 the sallaybus viewer is fucked it never opens and the recipt printer is also fucked and it does not gets proper error handling
 the app crashses most of the time when going through screens or opening anythiand i dont like that 3 dont drop down menu do something exclusicve and immersive and fully functions use all the routes needed from C:\Users\nikhil\Desktop\erpwidgit\LLOYD_ERP_ALL_ROUTES.md and make a plan and implement 
i want you to go through every line of code and fix everything 
