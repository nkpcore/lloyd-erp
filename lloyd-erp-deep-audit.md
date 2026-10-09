# Lloyd ERP — Deep Audit (Design → Algorithms → Connections → Upgrades)

**Repo:** `nkpcore/lloyd-erp` (uploaded `lloyd-erp-main` zip) · **Date:** 9 Oct 2026
**Method:** static source review of the zip, plus executed checks where possible (`node --check`, targeted greps). Nothing was run against the real ERP, no student account used, no device testing.
**Cross-checked against:** the ChatGPT audit (`lloyd-erp-audit.md`). Every claim from it that I cite below was re-verified in your zip.

**Confidence tags:** `[V]` verified in code or by running a command · `[L]` likely, depends on runtime or deployment · `[?]` not verifiable from the zip.

**Not covered in depth:** Compose UI screens, `CampusHealthMonitor` internals, `docs/security-audit/*` contents (only filenames seen), ProGuard rules, instrumented tests.

---

## 1. Executive summary

The Android domain core is solid: integer attendance math with tests, a pure change detector, Keystore-backed storage, a non-exported signature-protected receiver, and `allowBackup=false`. The surrounding system is not release-safe:

| # | Headline | Sev |
|---|---|---|
| 1 | Admin write APIs (`/api/config`, `/api/ban`) have **no authentication**; telemetry is public and unvalidated | Critical |
| 2 | **Admin dashboard JS does not parse** (`await` in non-async method) `[V]` | Critical (availability) |
| 3 | Release signing silently falls back to a throwaway debug key created fresh each CI run → OTA updates likely fail `[L]` | Critical |
| 4 | Campus auto-login submits stored credentials to whatever captive portal answers; no SSID/host trust check `[V]` | Critical |
| 5 | "Biometric" vault is **not cryptographically bound** to biometrics `[V]` | High |
| 6 | Attendance data can be labelled `FRESH` when partial/empty; notification pipeline can flood or miss alerts `[V]` | High |
| 7 | PWA has a hardcoded fallback student ID, float math, 500-record cap, tokens in `localStorage`, stale service worker `[V]` | High |
| 8 | Persistence layer reads different env vars than documented; writes fail silently `[V]` | High |

**Bottom line:** the biggest risk is not code quality inside the app but the *control plane* around it: unauthenticated admin APIs, a broken admin UI, release signing, and credentials flowing to untrusted endpoints. Fix those first.

---

## 2. Findings

### 2.1 Backend / control plane (`api/*`, `server.js`, `web/admin`)

#### B1 — Unauthenticated admin mutations `[V]` · Critical
`api/config.js` POST merges `...body` into stored config; `api/ban.js` POST toggles bans. No auth, CORS `*`.
**Impact:** anyone can ban all students, enable maintenance mode, raise `min_version_code` (remote kill-switch), or set `download_url` (OTA routing / phishing link). `AttendanceSyncWorker` calls `cancelAllWork()` on `Revoked`, so a forged ban stops background sync on affected devices until the app is reopened `[L]`. Android signature checks should stop a *differently-signed* APK from replacing the app, but they don't stop lockout or a malicious link.
**Fix:**
```js
// api/lib/auth.js
const crypto = require('crypto');
module.exports = function requireAdmin(req, res) {
  const given = Buffer.from((req.headers.authorization || '').replace(/^Bearer /, ''));
  const want  = Buffer.from(process.env.ADMIN_SECRET || '');
  if (!want.length || given.length !== want.length || !crypto.timingSafeEqual(given, want)) {
    res.status(401).json({ error: 'unauthorized' }); return false;
  }
  return true;
};
// in POST handlers: if (!requireAdmin(req, res)) return;
// then whitelist: const { min_version_code, latest_version_name, download_url, maintenance_mode, maintenance_message, broadcast_notice } = body;
```
Also: validate types (`Number.isInteger`), reject `http://` download URLs, restrict CORS to the admin origin, add rate limiting.

#### B2 — Telemetry public read/write, no validation `[V]` · High
`GET /api/telemetry` returns identifiable records (student ID, name, device ID, model, OS). POST only checks `device_id` exists; unbounded fields. Errors return `200 []`, hiding outages as "no devices".
**Fix:** admin-only reads (or aggregates only), schema + length limits, rate limit, 5xx on errors, retention policy.

#### B3 — Admin dashboard fails to parse `[V]` · Critical (availability)
`node --check web/admin/app.js` → `SyntaxError: await is only valid in async functions` at line 110. `saveConfig()` is not `async`, so the whole script fails and the dashboard never initializes.
**Fix:** `async saveConfig() { … }` and add `node --check` / ESLint to CI.

#### B4 — Ban action undoes itself `[V]` · High (masked by B3)
`app.js` updates `banned_devices` and calls `saveConfig()` (persists via `/api/config`), then POSTs `/api/ban`, which **toggles** the same device (`if has → delete else add`). Net effect: reversed.
**Fix:** one idempotent endpoint: `POST /api/ban { device_id, banned: true|false }`. UI renders from the server response.

#### B5 — Stored XSS in admin table `[V]` · High (masked by B3)
`student_name`, `device_id`, versions, models go into `innerHTML` templates; telemetry is attacker-controlled (B2).
**Fix:** build rows with `textContent`/`createElement`; add CSP (`default-src 'self'`), and sanitize server-side.

#### B6 — Persistence layer is unreliable `[V]` · High
- `.env.example` documents `KV_REST_API_URL/TOKEN`; `db.js` reads only `REDIS_URL` or `KV_URL` and uses `redis` (TCP) client. With the documented vars, Redis is never used.
- `redis.hSet(...).catch(() => {})` is not awaited; `saveConfig()` reports success when stores fail.
- Fallback writes to `process.cwd()/fleet_config.json`. Serverless filesystems are typically read-only/ephemeral, so state is lost between instances `[L]`.
- `/api/config` returns `200` with fallback config on any exception.
**Fix:** pick one store (`@upstash/redis` for REST creds), `await` writes, return 5xx on failure, add a health endpoint and a cold-start persistence test.

#### B7 — Config merge hazards `[V]` · Medium
`...body` allows arbitrary keys; `parseInt(body.min_version_code)` is not NaN-checked (a bad value can break the version gate). Public `GET /api/config` exposes `banned_devices`/`banned_students` — split a public model (version, URL, notice) from admin data.

#### B8 — OTA resolution and integrity `[V]` · High
- `api/ota.js` prefers `config.download_url` over the latest GitHub release; the checked-in config points at the committed static APK. ChatGPT reports the static APK differs in size from the v1.0.14 asset (3,415,116 vs 3,464,500 bytes) — I did not re-measure.
- Android `OtaUpdateManager` accepts `http://` URLs and I found no SHA-256 verification before installing; CI publishes a `.sha256` that the client never checks.
**Fix:** HTTPS-only + host allowlist, signed manifest with SHA-256, install only if hash matches *and* certificate matches the installed one; update URL and version atomically.

#### B9 — Two backends that can drift `[V]` · Medium
`server.js` (440 lines, zero-dependency) and `api/*` duplicate release lookup, CORS, config merge. `package.json`: `test` is a placeholder, `start` runs `server.js`. *(ChatGPT stated `server.js` doesn't exist; it does in your zip — possibly a different revision.)*
**Fix:** one shared handler module; Vercel and local server both import it.

#### B10 — Hardening gaps `[V]` · Low–Medium
Static serving uses `targetPath.startsWith(ADMIN_DIR)` without a path separator (sibling-prefix edge case) and no decode handling; no rate limiting; no security headers; wildcard CORS.

---

### 2.2 Android — security & identity

#### A1 — Release signing `[L]` · Critical
CI runs `keytool -genkeypair … -storepass android` on every fresh runner. `build.gradle` uses `RELEASE_KEYSTORE_PATH` only `if file(path).exists()`; the workflow passes a *path* secret, not a decoded keystore file, so on a clean runner it likely doesn't exist → fallback to the debug key. Result: each release may carry a different certificate → `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, users must uninstall (losing local data), and "signed APK" claims are false.
**Verify:** `apksigner verify --print-certs` on two releases and compare SHA-256 fingerprints.
**Fix:**
```yaml
- name: Restore keystore
  run: echo "${{ secrets.RELEASE_KEYSTORE_B64 }}" | base64 -d > $RUNNER_TEMP/release.jks
- name: Build
  env: { RELEASE_KEYSTORE_PATH: ${{ runner.temp }}/release.jks, ... }
```
Delete the debug-key fallback; make Gradle `throw new GradleException` if release signing is unavailable; add a CI step comparing the new cert fingerprint to the previous release.

#### A2 — Campus Wi-Fi auto-login trust boundary `[V]` · Critical
- Monitor reacts to Wi-Fi generally; the saved SSID is used for display, not as a precondition.
- `DirectHttpPortalAuthenticator` posts stored credentials to the parsed `<form action>` with no host/scheme allowlist.
- DNS falls back to `InetAddress.getAllByName` (default network) if Wi-Fi-bound lookup fails — also in `CampusPortalDetector` and `CampusHealthMonitor`.
- `WebViewPortalAuthenticator` injects credentials into JS as `'$username'` / `'$password'` (breaks on quotes, injection hazard), calls `bindProcessToNetwork` without restoring it, and detects success by URL substring.
- Captive detection = `INTERNET && !VALIDATED`, which also matches transient DNS/upstream outages.
**Fix:** require SSID/BSSID match + known portal fingerprint before any unattended submit; allowlist portal host (HTTPS except documented LAN gateway); remove default-network DNS fallback; pass JS values via `JSONObject.quote()`; restore binding in `finally`; confirm success with a real probe.

#### A3 — "Biometric" vault is a UI gate, not a cryptographic one `[V]` · High
`BiometricCredentialVault`'s AES-GCM key is built **without** `setUserAuthenticationRequired(true)`, `setInvalidatedByBiometricEnrollment`, `setUnlockedDeviceRequired`, or StrongBox; `BiometricAuthManager.authenticate` shows a prompt with no `CryptoObject`; `BIOMETRIC_WEAK` is allowed. Any code running in the app process (or an attacker with runtime hooks/root) can decrypt the saved password without biometrics.
**Upgrade:** auth-bound key (`setUserAuthenticationRequired(true)`, per-use auth), use `BiometricPrompt` with `CryptoObject(cipher)`, `BIOMETRIC_STRONG` only for key use, handle `KeyPermanentlyInvalidatedException` by re-enrolling. Better still: don't store the ERP password; store only a refresh token and re-prompt on expiry.

#### A4 — Ban/access control is bypassable and fail-open `[V]` · High
`AccessControlManager` is default-allow on first network failure (5 s timeout) and uses cached config otherwise. Device IDs are random UUIDs in preferences (`UUID.randomUUID()`), so clearing data/reinstalling evades a device ban; `student_id` is client-supplied. Combined with B1, bans are neither trustworthy nor safe.
**Fix:** server-issued signed config with TTL (deny after expiry), bind bans to the server-verified student ID from the ERP session, not self-reported fields.

#### A5 — Transport hardening `[V]` · Medium
`network_security_config` sets `base-config cleartextTrafficPermitted="true"` and blocks cleartext only for `erp.lloydcollege.in`. No certificate pinning. Invert default to `false`; scope cleartext to the specific captive-gateway hosts. `security-crypto:1.1.0-alpha06` is declared (alpha, library deprecated) — confirm whether it's still used since stores use raw Keystore `[?]`.

---

### 2.3 Android — data, sync, notifications

#### D1 — `FRESH` label on partial/empty data `[V]` · High
`AttendanceRepository.refresh()` replaces failed log fetches with `emptyList()`, subject fallback is a stub, and the snapshot is still marked `FRESH`. `ErpApiClient.getStudentAttendanceLogs` `break`s on non-success pages and caps pagination, returning partial logs as complete.
**Fix:** result type `Complete | Partial(missingPages) | Failed(error)`; only `Complete` → `FRESH`; keep last-good cache and show stale/partial UI.

#### D2 — Notification pipeline defects `[V]` · High
`AttendanceChangeDetector` is ID-based and unit-tested (good). Issues around it, in `AttendanceSyncWorker`:
1. **Disabled → enabled burst:** the check only runs when notifications are enabled, and seen-IDs aren't updated while disabled. Re-enabling notifies every lecture marked in the meantime. *Fix:* always advance seen-IDs; only suppress display.
2. **Partial bootstrap storm:** if the first sync is partial (D1), older records fetched later look "new" and notify.
3. **Mid-loop failure duplicates:** IDs are persisted only after the loop; an exception (swallowed by `catch (Exception ignored)`) means already-shown notifications repeat next cycle. *Fix:* persist each ID right after showing.
4. **Corrections not detected:** Absent→Present on the same ID is never re-notified, and the notification percentage comes from cached stats that may be stale if refresh failed.
5. `cancelAllWork()` on `Revoked` (see B1/A4) with no verified restart path `[?]`.

#### D3 — Auth retry recursion `[V]` · Medium
On 401, `getMonthlyAttendance()` etc. call `handleUnauthorized()` then call themselves again; the comment says "retry once" but a persistent 401 re-enters. Centralize in an OkHttp `Authenticator` with a one-retry guard, then emit a single session-expired event.

#### D4 — Wrong-record Wi-Fi credentials `[V]` · High
`fetchInternetCredentials()` returns `apiRes.data.get(0)` (and `paginatedRes.data.get(0)`) when no record matches the student. A fuzzy search could assign someone else's campus credentials. Never fall back to first result; require verified student match.

#### D5 — Reconciliation and telemetry timestamps `[V]` · Medium
`discrepancyCount = (aggregate − ledger).coerceAtLeast(0)` yields "Difference of 0" when ledger > aggregate. Telemetry uses `SimpleDateFormat("…'Z'")` in the **local** zone, so IST timestamps are 5 h 30 m ahead when read as UTC (breaks "Active Today"). Use `Instant.now().toString()`.

#### D6 — Campus state machine `[V]` · Medium
- `Online` is published even if validation never arrives (5 s wait, then success path).
- `disconnect()` resets UI but doesn't stop automation; callbacks can flip state back.
- Wi-Fi loss callbacks for an old network can clear a new network's state.
- Session start resets on every validated callback.
- Stall counter is cumulative (never decays).
**Fix:** explicit state machine with network identity in each transition; early-return on mismatched `Network`; rolling-window stall score.

---

### 2.4 PWA (`web/index.html`, bundled `web/assets/*`) and iOS widget

| ID | Finding | Sev |
|---|---|---|
| W1 | Fallback `student_id` **28960** hardcoded in the logs fetch when the real ID is unknown → wrong/other-user data `[V]` | High |
| W2 | Bunk math uses floats `(present − .75·total)/.75`; **safety flag computed from the percentage rounded to 0.1**, so 74.96% shows "SAFE" `[V]` | High |
| W3 | Log fetch capped at 5 pages × 100 = 500 records; subject stats derive from that subset while overall comes from the monthly API → silent disagreement `[V]` | Medium |
| W4 | Access + refresh tokens and cached attendance in `localStorage` (readable by any XSS) `[V]` | Medium |
| W5 | Service worker is cache-first with fixed name `lloyd-attendance-v1`; cached `index.html` can reference old hashed bundles → blank/stale app after deploy `[V]` | Medium |
| W6 | No fetch timeouts; 5-min polling continues in hidden tabs `[V]` | Low |
| W7 | Simulator slider is −15…+20 in the app vs README's −10…+15 `[V]` | Low |
| W8 | PWA login payload spoofs `app_version "3.0.0"`, `browser_name Chrome`, `os_name Android`, `device_type mobile` — impersonates the official client; breakage/detection and ToS risk `[V]` | Medium |
| W9 | iOS Scriptable widget stores only the token in Keychain (good) but ships a plaintext `USERNAME/PASSWORD` constants pattern for first run `[V]` | Low |

**Algorithm fix (port the Android logic):**
```js
// integer-only; target as whole percent K
const needed = (p, t, K) => { const n = K*t - 100*p, d = 100-K; return n<=0 ? 0 : Math.ceil(n/d); };
const canBunk = (p, t, K) => { const m = 100*p - K*t; return m<0 ? 0 : Math.floor(m/K); };
const isSafe  = (p, t, K) => 100*p >= K*t;   // never from a rounded percentage
```

---

### 2.5 Repo, CI/CD and compliance

- **Release cadence:** every push to `main` publishes `v1.0.<run_number>` (14 releases in ~7 days). Release on tags only; generate changelogs; consider SemVer by commit type.
- **Binary APKs in git** (`web/downloads/*.apk`, gitignore exception) — repo bloat and the OTA staleness in B8. Serve from Releases/CDN.
- **CI scope:** Android unit tests + lint run (good). No Node/JS lint, API tests, or XSS tests; `npm test` is a placeholder.
- **No `LICENSE` file** despite MIT claim in README; README is stale (still describes Java MVC).
- **Sensitive docs in a public repo:** `docs/security-audit/*` (e.g., faculty-authorization and attendance-write audit filenames) appear to analyze the college ERP's own weaknesses. Move to a private repo and report issues to the college (responsible disclosure). *Contents not reviewed here.*
- **Privacy/legal:** unofficial client for a college system; handles credentials and student records; telemetry collects name/ID/device. Add privacy notice, retention limits, a disclaimer of non-affiliation, and rate limits so you don't load the ERP.

---

## 3. What is already good

- `AttendanceCalculator` — pure Kotlin, integer arithmetic, clamping, unit-tested.
- `AttendanceChangeDetector` — pure, ID-based, bootstrap-aware, tested.
- Keystore AES-GCM for tokens and campus credentials; `allowBackup=false`; refresh receiver is `exported=false` with a signature permission; targetSdk 36; lint + unit tests in CI; SHA-256 published per release.
- Repository pattern with a single snapshot source of truth is the right shape.

---

## 4. Design and architecture upgrades

1. **Control-plane contract:** one OpenAPI spec for `/config`, `/ban`, `/telemetry`, `/ota`; generate server validators and Kotlin/JS clients from it.
2. **Roles:** public read model (`/config/public`), admin API behind `ADMIN_SECRET` → later OIDC/passkeys with audit log.
3. **Android:** finish the Java→Kotlin migration (`ErpApiClient`, `AppPreferences`, worker), move to Room for logs/subjects (replace JSON-in-SharedPreferences), single `Authenticator`, typed errors (`SyncError`) surfaced in UI.
4. **Sync strategy:** WorkManager (15-min floor) + optional FCM-style push from your own server later; stop calling the feature "real-time".
5. **PWA:** share the calculator by compiling one canonical algorithm spec/test vectors used by both Kotlin and JS (golden tests).
6. **UI/UX:** show data freshness (`Fresh / Stale / Partial / Offline`) everywhere; add empty/error/loading states; don't rely on color alone (add icons/text); configurable target % per course; TalkBack labels on widget actions.
7. **Observability:** structured logs without PII; health endpoint; alert on persistence failure.

---

## 5. Fix roadmap

**Gate 1 — before the next release**
1. B1/B2/B7: admin auth, public/private config split, validation, rate limits.
2. B3/B4/B5: fix `async`, single ban path, safe rendering, CSP.
3. A1: stable keystore from secret; fail build if missing; fingerprint gate.
4. A2/D4: campus trust checks; never use `data.get(0)`.

**Gate 2 — correctness**
5. D1/D2/D3/D5/D6: freshness states, notification fixes, one-retry auth, UTC timestamps, campus state machine.
6. W1–W3: remove hardcoded ID, integer math, full pagination.
7. B6/B8: persistence + OTA integrity.

**Gate 3 — hardening and hygiene**
8. A3/A4/A5: auth-bound vault key (or drop password storage), signed config TTL, cleartext off by default, pinning.
9. Tag-only releases, remove APKs from git, `LICENSE`, README, privacy notice, move audit docs private.
10. Tests: Node syntax/lint/API/XSS tests; Android fakes for `ConnectivityManager` and portal responses; golden calculator vectors shared with the PWA.

---

## 6. Acceptance tests

- `node --check web/admin/app.js` passes; dashboard loads with zero console errors; ban/unban survives reload.
- Anonymous `POST /api/config`, `/api/ban`, `GET /api/telemetry` → `401/403`; invalid payloads → `400`.
- Telemetry with `<img onerror=…>` renders as inert text.
- Two consecutive release APKs have identical signing cert fingerprints; install-over succeeds.
- OTA URL returns the APK whose SHA-256 equals the release asset's; `http://` URL rejected.
- Non-campus SSID or fake portal never receives credentials; DNS failure doesn't use cellular.
- `Online` appears only after validated connectivity; stale-network loss doesn't clear the new network.
- Failed/partial log page ⇒ snapshot is not `FRESH`; re-enabling notifications causes no burst; invalid refresh token ⇒ one retry then clean sign-out.
- 74.96% attendance shows **not safe** in both app and PWA; calculator golden vectors match across Kotlin and JS.
- Config, bans, telemetry survive a cold start; storage failure returns 5xx.

---

## 7. Cross-validation with the ChatGPT report

| Topic | ChatGPT | Mine | Verdict |
|---|---|---|---|
| Admin JS parse error | ✔ | missed in first pass | **Confirmed** (`node --check`) |
| Ban double-toggle | ✔ | missed | **Confirmed** |
| Unauth config/ban/telemetry | ✔ | ✔ | Agreed |
| Signing rotation | ✔ | ✔ | Agreed (`[L]`) |
| Campus trust/DNS/WebView | ✔ | partial | **Confirmed** |
| `data.get(0)` credential fallback | ✔ | missed | **Confirmed** |
| FRESH on partial data | ✔ | missed | **Confirmed** |
| Redis env mismatch | ✔ | partial | **Confirmed** |
| `server.js` missing | ✔ (claimed) | present in zip | **Not reproduced** |
| PWA hardcoded ID / float math / 500 cap / SW staleness | — | ✔ | Mine only |
| Vault not biometric-bound | — | ✔ | Mine only |
| Notification burst/duplicates, fail-open ban, device-ID evasion, cleartext default | — | ✔ | Mine only |
| OTA no hash / http accepted | partial | ✔ | Agreed, more detail here |

---

*This report is source-level and conservative: items marked `[L]` or `[?]` should be confirmed on a device or deployment before being treated as bugs. Happy to turn Gate 1 into patches.*
