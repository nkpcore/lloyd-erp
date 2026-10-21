# Lloyd ERP — Repository-Grounded Stability, Security & Feature Implementation

**Repository:** https://github.com/nkpcore/lloyd-erp  
**Target branch:** `main` (inspect the latest checked-out revision; do not assume older audit findings are still current)  
**Platform:** Android/Kotlin + Jetpack Compose client, Node.js/Vercel fleet-control/admin service, companion Scriptable widget  
**Authoritative supporting files:**
- `LLOYD_ERP_ALL_ROUTES.md` — external college ERP route inventory.
- `AKTU_CSE_1st_Year_Syllabus_2026-27.pdf` — AKTU B.Tech First Year CSE Stream syllabus effective 2026–27.
- Repository `AGENTS.md`, current source, current tests, `lloyd-erp-deep-audit.md`, and `lloyd-erp-stable-release-report.md`.

## Mission

Improve the existing Lloyd ERP application for a stable student-facing release. Fix verified defects first, then implement the requested attendance calendar, latest-attendance view, feedback hub, More menu, receipt printing, notification navigation, update reliability, syllabus browsing, and focused UX improvements.

**This is an implementation task against the real repository, not a request for another speculative audit report.** Inspect the code, trace real flows, use the relevant skills supplied by the repository/`.agents.zip` when genuinely available, make the smallest safe changes, add regression tests, run them, and report exactly what is complete versus blocked or unverified. Keep a requirements ledger so the long scope is not reduced to whichever feature is easiest to finish.

Do not modify the college's external ERP server (`erp.lloydcollege.in`). This repository is a client plus its own fleet-control/admin service. Use the external APIs only through the existing authenticated client and the documented contracts. Do not scrape, bypass access controls, change external records, or invent endpoints.

---

## 0. Non-negotiable engineering rules

1. Read `AGENTS.md` first and obey its zero-hardcoding rule. No fake student IDs, student names, timetable entries, subjects, sections, attendance, receipt details, or success responses.
2. Begin from the exact current `main` revision. Record `git status --short`, `git rev-parse HEAD`, and the current tag/release information. Do not overwrite unrelated user changes.
3. Run a baseline before making edits. Record actual successes and failures; do not describe unrun tests as passing.
4. Treat historical audit documents as a checklist, not ground truth. For every finding, re-check the latest source and label it **confirmed-current**, **already fixed/stale**, **not reproducible**, or **cannot verify**. Never re-introduce a bug while applying an old suggested fix.
5. Preserve the existing architecture and three main tabs unless code inspection demonstrates a real need to change them. Reuse the attendance repository, timetable engine, Compose components, API client, notification worker, update manager, secure storage, and existing unit tests.
6. Do not add a new backend, database, framework, or large dependency for a feature that can use an existing contract safely. If the external API does not support the necessary operation, make the limitation clear rather than faking it.
7. Avoid opportunistic rewrites. Keep changes scoped, backward-compatible, reviewable, and independently testable. Avoid destructive schema changes; add migration and rollback details if one is unavoidable.
8. Do not log credentials, access/refresh tokens, passwords, raw portal form data, full device identifiers, or unnecessary student PII. Use synthetic test fixtures only in tests.
9. Keep claims of “real-time,” “secure,” “persisted,” “up to date,” “holiday,” and “successful” tied to evidence. A timeout is not an empty dataset; an empty dataset is not proof of a holiday; a completed download is not proof of a successful update installation.
10. Work in phases. Resolve critical production risks before expanding the UI. Do not deploy or publish a release unless explicitly authorized and every required release gate passes.



## 0A. Skill-aware execution protocol — use the strongest relevant tools, not every tool

The repository may include a `.agents/` directory and the supplied `.agents.zip` contains reusable skills and specialist agent definitions. Inspect the actual checked-out repository first; do not assume the uploaded archive is installed, current, or compatible with the active coding environment.

### 0A.1 Discover and load skills before implementation

1. Read repository `AGENTS.md` and all more-specific `AGENTS.md` files governing the target paths before editing.
2. Inspect `.agents/skills/`, `.agents/agents/`, skill metadata/lock files, and the active coding environment's available skill invocation mechanism. If the archive is not already installed, inspect it as reference material; do not overwrite the repository's `.agents/` directory or install/execute unknown scripts automatically.
3. Load the full instructions for the applicable skills before using them. If a named skill is missing or the active environment cannot invoke it, use the workflow below manually and report that limitation. Never claim a skill or subagent ran unless it actually did.
4. Use skills selectively. Do not activate every skill simply because it exists; avoid unrelated design, branding, deployment, or reverse-engineering workflows.

Recommended skill routing when those skills are present and callable:

| Workstream | First-choice skill(s) | Use it for | Guardrail |
|---|---|---|---|
| Repository orientation and dependency mapping | `understand`, `understand-domain`, `understand-explain` | Build a focused map of Android, API, fleet, OTA, attendance, and syllabus flows | Treat generated graphs as navigation aids, then verify findings in source |
| Goal tracking / long-running implementation | `goal` or the environment's native task/plan mechanism | Keep the full objective and acceptance ledger alive across implementation batches | Do not mark complete until every required deliverable is evidence-checked |
| Android UI/UX and accessibility | `material-3-expressive`, `material-3`, `ui-ux-pro-max`, `design-system` | Define and apply an intentional Material 3 Expressive visual language across Compose screens, components, motion, layout, and accessibility | Load the actual available skill instructions; prefer the existing project-compatible Compose APIs and do not invent APIs or add a web-only stack |
| Security review | `review-security` | Independently review the final diff and critical auth, network, storage, OTA, and telemetry changes | Run only if the review skill/subagent is actually available; otherwise perform a documented manual review |
| General bug/regression review | `review` and/or `review-bugbot` | Review changed files and regressions after implementation | Follow the skill's own invocation requirements; do not pretend review occurred |
| Skill discovery | `find-skills` | Discover a specific missing capability only when it materially helps | Do not install extra skills or dependencies without a concrete need and permission |
| Vercel deployment workflow | `deploy-with-vercel` | Understand deployment configuration when necessary for diagnosis | No deployment, production mutation, or release publication without explicit authorization |

Specialist agent files in `.agents/agents/` may be used for **bounded, read-only, parallel investigations** (for example architecture mapping, domain flow, UI review, or security checklist review) if the active environment supports subagents. Give each investigator a narrow question and require file/line/test evidence. The primary implementer remains responsible for reconciling findings, editing code, and running tests. Do not let multiple agents edit the same files concurrently.

### 0A.1A Mandatory design language — Material 3 Expressive (M3 Expressive)

**The product must have a deliberate, cohesive Material 3 Expressive design language.** Do not treat design as a final coat of polish, and do not build generic default Compose screens. Before implementing feature UI, inspect the current `LloydTheme`, color and typography tokens, component library, navigation patterns, and installed design skills. If available, load and follow the `material-3-expressive` skill first, alongside `material-3`, `ui-ux-pro-max`, and `design-system` where relevant. If the exact skill is unavailable, follow official Material 3 Expressive guidance using the APIs and dependencies already compatible with this repository, and state that the skill was unavailable.

Design requirements:

- Establish a compact visual system using semantic color roles, a consistent type scale, spacing scale, shapes, elevation, icon treatment, and component states. Reuse and extend `LloydTheme` rather than scattering literal colors, dimensions, and typography throughout screens.
- Use expressive hierarchy intentionally: clear page titles, readable section headers, meaningful emphasis, appropriately shaped surfaces, strong primary actions, and calm secondary information. Expressive does **not** mean adding decoration everywhere, excessive pills/cards, oversized banners, or gratuitous gradients.
- Use Material 3 Expressive components and patterns only where supported by the project's Compose/Material dependencies. Verify API availability in the repository before using any experimental or newer component; do not guess imports or silently add a major dependency.
- Apply motion purposefully for transitions, selection, expansion, and feedback. Motion must be brief, interruptible, respect system animation settings where possible, and never delay core tasks or obscure errors.
- Keep the student experience fast and content-first. Prioritize attendance, notices, and the task at hand; avoid dashboard clutter, unnecessary nested cards, visual noise, and decorative charts without a real data purpose.
- Design every feature with loading, empty, offline, stale/partial where applicable, error, disabled, and success states. Never use color alone to convey status.
- Meet accessibility expectations: TalkBack semantics, meaningful labels, adequate contrast, minimum 48dp interactive targets, scalable text, logical focus order, and correct edge-to-edge/system-bar insets.
- Keep navigation and interaction patterns consistent across dashboard, calendar, latest attendance, feedback, receipts, update settings, syllabus selector/details, dialogs, and the More menu. Preserve the existing three main tabs unless source inspection and usability evidence justify otherwise.
- Make layouts adapt gracefully to compact phones, larger phones, landscape, and larger font settings. Avoid hard-coded screen dimensions and layouts that clip behind system bars.
- Do not redesign the entire app in one speculative pass. Create a small, reusable set of tokens/components and apply it consistently to the requested screens. Inspect screenshots or run the app on an emulator/device when available; distinguish visual inspection from build/test verification.

**Design acceptance gate:** before calling UI work complete, document the design tokens/components reused or added, verify that screens are visually coherent with M3 Expressive, check light/dark themes if both are supported, and test accessibility plus loading/empty/error states. Do not claim a design skill was run unless it was actually loaded and followed.

### 0A.2 Run an evidence-first, vertical-slice workflow

Maintain a requirements ledger in a working document, for example `implementation/acceptance-ledger.md` or an existing repository-equivalent. Do not create a new documentation tree if the repository has an established convention. For each requirement, record:

- stable ID and priority (`P0`, `P1`, `P2`);
- source of truth and relevant source files;
- status: `not-started`, `confirmed-current`, `implemented`, `tested`, `blocked`, or `not-applicable`;
- implementation commit/diff location (if any), test name/command, and actual result;
- unresolved assumptions, external permissions, and manual verification needed.

Work in dependency order and complete thin vertical slices rather than adding many screens before their data flows work:

1. **Baseline and trust boundaries:** repository state, instructions, current tests, build/signing, API contracts, data flows, environment requirements.
2. **P0 security and correctness:** fleet admin authorization/config/telemetry/persistence, OTA integrity/signing and CI release broadcast. Add negative tests before calling a fix complete.
3. **Syllabus data pipeline:** inspect the supplied PDF pages/tables, extract and validate structured records with provenance, build the coverage report, then implement the section-independent selector/detail browser.
4. **Student features:** attendance calendar/latest attendance using existing supported data flows, feedback and receipt access only after confirming the real API schemas and permissions; then More menu/navigation and the relevant flows.
5. **UI implementation using Material 3 Expressive:** establish the design direction and component patterns before implementing feature screens; then apply responsive layouts, semantics, state restoration, and loading/offline/error states consistently.
6. **Independent review and release gates:** run focused tests after each slice, then full required suites and final diff/security review.

This sequence is a dependency guide, not permission to omit lower-priority explicit requirements. Track all requested work to completion. If the environment, context, or time cannot support all work in one pass, finish the current slice cleanly, preserve the ledger and exact next actions, and report what remains; do not silently redefine the objective as an audit-only task.

### 0A.3 Parallelize research, serialize risky changes

- Parallel work is appropriate for independent read-only tasks: current OTA contract tracing, attendance architecture mapping, PDF coverage inventory, and UI accessibility review.
- Keep changes to authentication, signing, persistence, database schema, and external API writes under one owner at a time.
- Before integrating a delegated finding, independently verify the cited source and current behavior.
- If a finding depends on a real student session, production secret, device, live portal, or administrator permission, document the exact blocker and the safest verification procedure. Never manufacture evidence to unblock the work.

### 0A.4 Definition of evidence and stop conditions

- Distinguish **source inspection**, **unit-tested**, **build-verified**, **device-verified**, and **production-verified**. They are not interchangeable.
- A passing unit test does not prove production configuration, external API authorization, update installation, or Android device behavior.
- A command not run is `not run`, not `pass`; an unavailable dependency is `blocked`, not `pass`.
- Do not weaken tests, lint, auth, TLS, signing, or CI gates just to obtain green output. Fix the underlying issue or document the blocker.
- Do not perform destructive cleanup, production writes, key rotation, deploys, releases, pushes, or commits unless the user has authorized that specific action.
- Stop and ask for direction only when a genuine decision cannot be resolved from repository policy or reliable evidence, especially for credential exposure, signing-key continuity, data deletion, external permission boundaries, or incompatible requirements. Continue all safe independent work meanwhile.

## 1. Phase 0 — Baseline and source-of-truth map

### 1.1 Inspect the live repository source

Review at minimum:

- `AGENTS.md`, `README.md`, `package.json`, `vercel.json`, `.env.example`.
- Android build/signing configuration: `android/app/build.gradle`, Gradle settings, manifest, network security config, and `.github/workflows/android-build.yml`.
- Attendance: `core/data/AttendanceRepository.kt`, `AttendanceState.kt`, `core/domain/AttendanceCalculator.kt`, `AttendanceModels.kt`, `core/schedule/TimetableRepository.kt`, `TimetableModels.kt`, `feature/dashboard/*`, `feature/logs/*`, `widget/AttendanceSyncWorker.java`, `core/notification/AttendanceChangeDetector.kt`, `widget/NotificationHelper.java`.
- Update: `core/ota/OtaUpdateManager.kt`, `core/designsystem/components/InAppUpdateDialog.kt`, `ui/LoginActivity.kt`, `ui/MainComposeActivity.kt`, `data/AppPreferences.java`, `api/ota.js`.
- Fleet/admin: `api/lib/auth.js`, `api/config.js`, `api/ban.js`, `api/telemetry.js`, `api/lib/db.js`, `api/auth.js`, `server.js`, and `web/admin/{app.js,index.html,style.css}`.
- Profile, navigation, exported report/receipt code, and all relevant existing tests.

### 1.2 Baseline checks

Run from a clean, non-destructive working tree (or record pre-existing changes):

```bash
git status --short
git rev-parse HEAD
git fetch --tags
cd android
./gradlew testDebugUnitTest --no-daemon
./gradlew lintDebug --no-daemon
```

From repository root, verify the current fleet UI and scripts:

```bash
node --check web/admin/app.js
node --check server.js
```

`package.json` currently defines `npm test` as a placeholder failure. Inspect current test/dependency setup and replace the placeholder with a real, deterministic Node test command as part of the work; do not merely change the exit code to zero.

Capture the existing state of the named audit findings and compare it with current `main`. Build a simple tracking table:

| Finding | Current status | File/line or test evidence | Action |
|---|---|---|---|

Do not use production student data or submit write requests to the real college ERP during investigation.

---

## 2. P0 — Secure and stabilize the fleet admin/control plane

This area governs device access, release versioning, update metadata, and telemetry. Address it before adding convenience features.

### 2.1 Eliminate default admin credentials and client-side identity checks

Inspect and correct these risks if still present:

- `api/lib/auth.js` has historically used a default password such as `loyderp` and accepted secrets through several locations, including query parameters and request bodies.
- `api/auth.js` has historically returned the password itself as a reusable token.
- `web/admin/app.js` has historically allowed an offline hardcoded-password fallback.
- `ProfileScreen.kt` has historically determined admin visibility by matching a hardcoded student/admission ID (`260094709`).

Required behavior:

- No default production password. If required auth secrets are absent, protected admin mutations must fail closed and deployment validation must flag the missing configuration.
- Never accept secrets in URL query parameters. URLs leak into browser history, telemetry, proxies, and logs. Prefer a server-validated short-lived session or authenticated secure cookie with appropriate `HttpOnly`, `Secure`, and `SameSite` settings; protect state-changing requests against CSRF where cookies are used.
- Never return the configured password or secret in an authentication response. Return a minimal success response or short-lived session, not the underlying credential.
- Compare secret material safely; rate-limit failed attempts; return consistent non-revealing errors.
- Restrict admin CORS/origins to known deployed admin origins where feasible. Do not treat CORS as a substitute for authorization.
- Remove client-side hardcoded-admin-ID checks. UI may hide privileged controls, but server-side permission checks are authoritative. Do not grant admin access based on a student ID, saved username, device UUID, local preference, or a hidden menu.
- Do not keep long-lived admin passwords in `localStorage`. If a session expires, require re-authentication. On network failure, do not fall back to offline “authenticated” mode.

### 2.2 Separate public configuration from private governance state

`api/config.js` has historically returned broad configuration including ban arrays on its GET endpoint. Review the exact current contract.

- Return a strictly allowlisted public update/config projection to normal app clients: only fields required by app operation, such as approved version/mandatory-update information, generic maintenance state/message, and a public notice.
- Keep device/student ban lists and administration-only data behind admin authorization.
- Apply allowlist-based input validation to updates. Never merge arbitrary request bodies into persisted configuration with an unrestricted spread.
- Validate version numbers, URLs, string lengths, booleans, and array item types. Reject malformed or unsupported fields.
- Restrict release download URLs to HTTPS and explicitly approved hosts. Never accept an arbitrary URL that could steer users to a malicious download.
- Return honest HTTP errors for failures. Do not send status 200 with fallback config after a database error, since clients would mistake outages for real state.

### 2.3 Make ban/revoke actions idempotent and race-safe

- Use explicit actions (`ban` / `unban`, or `banned: true|false`); do not default to `toggle` for retryable network actions.
- Make one authoritative server mutation per user action. Do not save the changed array through `/config` and then call a toggle endpoint for the same identifier.
- Return the server's authoritative resulting state, and update UI from that response only.
- Test repeated requests, rapid clicks, retry after timeout, concurrent requests, and network failure. A retry must not reverse the intended state.
- Preserve legitimate separate installations. A student can have more than one device. Never merge records by student name or remove devices just because names match.
- Record auditable administrative changes (action, target, timestamp, actor identity where available) without logging secrets.

### 2.4 Protect telemetry and make persistence truthful

Re-verify `api/telemetry.js` and `api/lib/db.js` against the latest code.

- Public ingestion must validate a strict allowlist of fields, type/length limits, version/platform formats, payload size, and timestamp formats. Do not accept arbitrary fields into stored records.
- Admin telemetry listing, delete, and purge actions must require valid admin authorization. Normal clients must not read the full fleet dataset.
- Add reasonable request limits/rate limits and define minimal telemetry retention. Avoid collecting student name/ID unless a demonstrated administrative need exists; prefer aggregated and pseudonymous telemetry. Explain the data collected in a privacy notice.
- Do not return success merely because the write reached in-memory/disk fallback. Vercel/serverless local disk is not a durable cross-instance database. Define the configured persistent provider and make write outcomes explicit.
- If Upstash/Redis persistence fails, expose a truthful error or explicit degraded status; do not silently promise durable success. Test read-after-write from a fresh handler/module instance.
- Make telemetry upserts idempotent using the correct installation/device identity; retain legitimate history only where the product needs it.
- Sanitize user-controlled values for HTML and attribute contexts. Avoid injecting untrusted telemetry into `innerHTML`/inline `onclick`; prefer DOM APIs, safe text nodes, and event delegation. Add regression tests with hostile strings and quote/markup characters.
- Re-verify `node --check web/admin/app.js`. Historical audit claims that this file fails parsing must not be accepted without current evidence.

### 2.5 Fix CI release broadcast and release integrity

Inspect `.github/workflows/android-build.yml`, `api/config.js`, and current signing configuration as one flow.

- The workflow has historically called the fleet `/config` write endpoint without authorization and swallowed failures with `|| true`. Add a server-to-server credential stored only in GitHub Actions/Vercel secrets, send it through the intended auth mechanism, verify the response, and surface a failed broadcast honestly. Never put secrets in workflow logs or source.
- Avoid embedding private release credentials in the APK, repository, `BuildConfig`, web bundle, or committed static JSON.
- Confirm the signing certificate fingerprint of the last publicly distributed APK and compare it to the configured build key before modifying the release pipeline.
- Do not blindly rotate signing keys: Android users generally need the update to be signed by the same signer as their installed application (unless an explicitly supported signing-key upgrade path applies). If the key cannot be established safely, stop release publishing and document the recovery plan rather than creating a fresh key.
- Remove fallback release signing with a debug key or dynamically generated ephemeral key for production. Release builds must fail clearly when the approved signing key is unavailable. Never hardcode keystore passwords such as `lloyd123`.
- Verify app ID, versionCode, versionName, APK signature certificate, artifact filename, SHA-256, and GitHub release tag match each other.
- Reduce unnecessary release creation on every commit only if compatible with the current deployment workflow; separate test CI from deliberate, version-aligned release publishing.

### 2.6 Focused security checklist

Use OWASP MASVS categories (storage, cryptography, authentication, network, platform interaction, code quality, resilience, privacy) as a coverage checklist without turning this into an unrelated rewrite.

Re-check:

- Token/session storage and logout/session-expiry behavior.
- Unauthorized cross-student attendance access (object-level authorization / BOLA).
- Sensitive values in logs, crash reports, clipboard, exports, or profile UI.
- Exported Android components, intent extras, `PendingIntent` flags, `FileProvider` paths, and backup settings.
- Cleartext traffic policy. The current network-security XML has a broad cleartext allowance for campus captive-portal work; scope exceptions to the minimum required flow and keep official ERP/API and OTA traffic HTTPS-only.
- Attachment validation, upload size limits, and safe file handling for feedback.
- Do not loosen TLS, disable certificate checks, trust arbitrary redirects, or use broad Android permissions to make tests pass.

Do not claim a security fix is complete until the affected code path and a negative test demonstrate the intended behavior.

---

## 3. Attendance calendar preview

Add a month calendar within the existing attendance experience (for example, a dashboard calendar card with a full calendar/detail screen). Do not add a new bottom navigation tab unless testing proves that existing navigation cannot support it cleanly.

### 5.1 Daily indicators from real records

Derive daily state from deduplicated records for the authenticated student, grouped by the ERP's actual attendance date and explicit present/absent status:

- **All present:** green indicator.
- **All absent:** red indicator.
- **Mixed present and absent:** half-green/half-red indicator.
- **Confirmed non-working day/holiday:** neutral indicator, only when reliable routine/holiday data supports it.
- **No attendance records / not yet marked / uncertain:** distinct neutral or amber state with an explicit label. Do not mark this red by default.
- **Partial/inconsistent source response:** visibly distinguish from a confirmed all-present/all-absent day.

Use a legend and do not rely on color alone: include meaningful semantics/labels for TalkBack, and use shape/icon or text differences for accessibility.

### 5.2 Holiday rule

The requested shortcut is: “if no class attendance was marked on a day, treat it as a holiday.” Implement it only when the timetable, known holidays, or verified college behavior supports that inference. If the system has expected classes on that date but no records, label it **Attendance not recorded / unconfirmed**, not “Holiday” and not “Absent.” The current weekly timetable alone may not be enough to determine past exceptions, substitute classes, college closures, or teacher non-submission. If authoritative calendar data is unavailable, document this limitation and avoid false certainty.

### 5.3 Tap-through behavior

- Every date with a record or a meaningful state is selectable.
- Tapping a date opens the existing Daily Logs screen filtered to that exact local date, showing the real lecture records in that day's order.
- Keep the selected date and visible month when navigating back.
- Display subject, lecture/period, present/absent status, recording timestamp when available, and faculty name if returned by the source.
- Month navigation, today shortcut, clear legend, loading/empty/offline states, and state restoration are required.
- Avoid one API request per day. Reuse cached records/month summary; fetch only missing data if needed and supported.

Add unit tests for all-present, all-absent, mixed, no records on a scheduled date, confirmed holiday, duplicate record IDs, unknown status, partial data, month boundaries, leap day, and timezone edges.

---

## 4. Latest Attendance activity and marked Present/Absent alerts

Add a **Latest Attendance** entry accessible from the dashboard or More menu. It should reuse the same real attendance records and include:

- Newest recorded attendance entries sorted by trustworthy `created_at`/server timestamp and stable ID, not device fetch time alone.
- Subject/course, attendance date, lecture period, Present/Absent status, and faculty/recorded-by metadata if actually returned.
- Filters for All, Present, and Absent; sensible “no entries” and offline states.
- A clear indication of newly discovered entries that does not label old history as new on first install or after logout/cache reset.
- On tapping an alert or entry, open that exact record/date in the app.

Audit `AttendanceChangeDetector`, `AttendanceSyncWorker`, and preferences for duplicate/burst/missed events. The detector is already a tested component; preserve it and extend only as required.

- Bootstrap known historical IDs without a notification flood on first installation/login.
- Remember newly observed IDs even when notifications are disabled if that is the defined product behavior, so re-enabling does not flood the user with stale events. Validate this with tests.
- Handle corrections to an existing record only if the API exposes a reliable updated timestamp/version or the product clearly defines how changed status is identified.
- Do not create a notification from partial, failed, or stale sync results.
- A new attendance record may be Present or Absent; label it accurately and do not infer one status from percentages.

WorkManager's periodic work is deferred work and may run later than its requested interval. The current worker is scheduled at a 60-minute period with a flex window. Do not promise instant/real-time notifications from this system. Keep a reliable user-triggered refresh and use push delivery only if a properly authorized push backend is actually available; do not add aggressive polling to simulate real time.

---

## 5. Fix automatic update checking end to end

The app already has `OtaUpdateManager`, `InAppUpdateDialog`, `lastDismissedUpdateVersion`, and an OTA API. Fix this chain; do not add a second updater.

### 7.1 Trace and normalize the contract

Inspect:

- Android `OtaUpdateManager.checkForUpdates`, its endpoint fallback order and version parser.
- `LoginActivity` and `MainComposeActivity` lifecycle invocation/duplicate check behavior.
- `AppPreferences.lastDismissedUpdateVersion` and mandatory update handling.
- `api/ota.js`, `api/config.js`, deployment rewrite rules, actual GitHub latest release/tag/assets, and CI broadcast.
- The repository has inconsistent historical hostname strings (`lloyd-erp-sander.vercel.app` vs `lloyd-erp-sand.vercel.app`). Verify which host actually exists and is deployed before selecting the canonical base. Do not guess; centralize the verified origin and remove stale duplicates.

### 7.2 User-visible states and lifecycle

Create explicit, observable states: `Idle`, `Checking`, `UpToDate`, `UpdateAvailable`, `Paused`, `Downloading(progress)`, `AwaitingInstallPermission`, `Installing/HandedOff`, and `Failed(reason)` as appropriate.

- Check in a lifecycle-aware, debounced way at startup/resume and provide a manual **Check for updates** action in the profile/More menu.
- Avoid duplicate requests from recompositions, simultaneous activities, rapid taps, and returning from install-permission settings.
- Show a proper result for up to date, updates paused, offline, API failure, and bad metadata. Never convert an exception into “you are up to date.”
- Keep the last check time and offer Retry when useful.
- Honor dismissals of optional update versions; do not repeatedly interrupt users on every resume. A newer release may be shown again. Mandatory lockout is allowed only when trusted policy explicitly requires a version code higher than the installed code.
- Keep startup responsive; do not block the UI thread.

### 7.3 Validate the artifact before installation

- Accept HTTPS only and only approved release/download hosts. Reject arbitrary HTTP or redirect destinations; do not silently follow an untrusted host change.
- Use authenticated or public GitHub Releases metadata consistently; require a matching expected asset name and validate package/version metadata where practical.
- Verify SHA-256 before installation against GitHub release asset `digest` (`sha256:...`) when present or the release's exact `.sha256` companion. Fail closed when required integrity metadata is missing or mismatched; do not install a corrupt/unverified APK.
- Download to a temporary `.part` file, enforce sensible maximum size/content type, verify completed length/checksum, and atomically rename only after validation. Remove partial/corrupt stale files after failure.
- Keep downloads under app-private storage and use narrowly scoped FileProvider paths. Preserve `FileProvider` URI grants and use Android's supported install-permission flow.
- Do not claim installation succeeded merely because an install intent was opened. Confirm the installed app version on a subsequent launch or rely on a truthful installer callback.
- Unit-test strict version parsing, same/older/newer versions, invalid tags, versionCode/minimum-version handling, incorrect host/scheme, malformed JSON, no APK asset, wrong hash, incomplete downloads, timeout, permission denial, and user dismissal behavior.

### 7.4 Release workflow integration

The GitHub Action has historically built a release on every push and sent an unauthenticated `/config` broadcast whose error was swallowed. Re-verify it. Ensure the release workflow and server agree on auth, version/tag/asset names, signer fingerprint, and hash. A release must never advertise an APK different from the file actually downloaded. If the broadcast is required to make updates discoverable, fail the release job when broadcast fails; if it is optional, mark it clearly and alert the maintainer instead of hiding the failure.

---

## 6. Feedback & Issue Hub (reuse the ERP's actual feedback capability)

The attached route inventory lists authenticated endpoints including:

- `POST /api/feedback`
- `GET /api/feedback/me`
- `PATCH /api/feedback/{feedback_id}/reply`
- `POST /api/grievances/student` and associated grievance routes

These are **external college ERP routes**, not routes implemented by this repository. Before coding, verify the live OpenAPI schema, actual request/response fields, student permissions, and existing `ErpApiClient` behavior. Do not assume a route response from its name alone.

Requirements:

- Create a concise student-facing hub with appropriate categories such as Bug/Technical Issue, Suggestion, General Feedback, and Formal Grievance where the API's actual schema supports them.
- Use the logged-in student's existing authenticated session. Show only feedback/grievances belonging to the current user, unless verified backend authorization explicitly permits more.
- Show submission result, server-generated ID/reference, status, and official reply when provided by the API. Do not invent statuses or imply a ticket was sent before a successful server response.
- Prevent accidental duplicate submissions with button state and request/idempotency safeguards where the API supports them. On timeout, distinguish unknown outcome from definite failure and avoid blindly duplicating a submission.
- Add field validation, character limits, loading/retry/empty/error states, and sanitized optional screenshot upload only if an authorized upload endpoint and size/type policy are confirmed.
- Reuse official feedback/grievance flows rather than maintaining a parallel feedback database. If the external schema or permission denies an operation, explain it in the result and implement only what is supported.
- Replies and status changes must be read from the server, not faked in the client.

Do not mix student feedback for the official college ERP with private fleet-server bug reports. If a fleet admin issue log is genuinely needed, design it as a separate, authenticated admin-only feature with clear ownership.

---

## 7. More menu replacing the current Share action

Replace the intended top/profile Share action with a tidy, extensible **More** menu using the existing Material 3 Expressive design system and current navigation.

Initial actions:

1. Receipt Printer / Receipts
2. Feedback & Issue Hub
3. Latest Attendance
4. Check for Updates
6. About / Privacy, if useful and not already provided elsewhere

Rules:

- Reuse working screens/actions; no decorative items that do nothing.
- If the Share function is still useful, retain it inside the menu or in an appropriate existing destination rather than silently removing useful behavior.
- Ensure menu opens and closes correctly, supports keyboard/screen reader navigation, has meaningful content descriptions and appropriate touch targets, and does not get clipped by status/navigation bars.
- Keep item definitions central so future items can be added without hardcoded duplicated navigation logic.
- Do not add a fourth bottom tab or a new complex navigation library just for this menu.

---

## 8. Receipt printing / export

Inspect the current authenticated self-service fee receipt and transaction endpoints in `LLOYD_ERP_ALL_ROUTES.md` and verify against OpenAPI and current account/role permissions before implementation. Candidate routes listed by the inventory include `/api/account/payments/self/fee-receipts`, `/api/account/payments/self/transactions`, and existing printable receipt routes under `/api/account/receipts/...`; some receipt endpoints may be accountant/admin-only. Do not attempt to bypass those permissions.

- Provide a clear receipt list/detail and a print-friendly layout only for receipts the current account is authorized to access.
- Preserve source receipt number, student/payer, amount, payment date, payment status, and line items exactly as returned by the server. Do not generate an “official” receipt from pending or unverified payment data.
- Use Android Print Framework / a properly sanitized print document / PDF export according to the current stack, and make standard-paper output reliable. Support thermal printers only if the device/driver path is genuinely compatible; don't imply universal Bluetooth printer support.
- Handle no receipt access, pending payments, expired session, network failure, missing fields, multi-page layout, and retry honestly.
- Add tests that confirm unauthorized users cannot open another student's receipt and that render/export values match source data.

If the external ERP provides no student-accessible receipt endpoint, state that limitation clearly and provide only an authorized link/route; do not invent access or money records.

---

## 9. Notification UX and click-through navigation

The project already has `AttendanceSyncWorker`, `AttendanceChangeDetector`, and `NotificationHelper`. Improve reliability and navigation using them.

- A notification click should open the exact attendance date/record or the relevant content, not always land on the generic dashboard. Pass stable record ID/date/subject metadata in the intent and route it into the existing logs/detail navigation safely.
- Verify cold-start and warm-start behavior, singleTop/task flags, back stack, duplicate taps, and stale/deleted record handling. The Back action should lead to the relevant app context naturally.
- Do not add a full-screen activity that hijacks the user when a notification arrives. Let Android determine heads-up behavior; the user explicitly tapping the notification is the navigation trigger.
- Respect Android 13+ `POST_NOTIFICATIONS` permission: explain the value in context, handle denial/blocked settings gracefully, and never crash or silently claim notifications are enabled.
- Preserve notification channels and user preferences. Do not try to change an existing channel's importance in code after creation; explain the system setting when users need to change it.
- Use stable unique notification IDs per attendance record to prevent unintended overwrites or duplicate alerts. Optionally group multiple new marks into a summary, while retaining exact record navigation.
- Keep notifications concise and truthful: “Marked Present” / “Marked Absent”, subject, date/period, and actual timestamp when available. Avoid showing private information unnecessarily on lock screen.
- Run a full notification state matrix: first bootstrap, one new record, multiple records, duplicate records, sync failure, partial data, notifications disabled/re-enabled, no permission, signed-out user, changed status, and clicking old notifications.
- Do not describe WorkManager hourly sync as instant. If genuinely faster updates are desired, first establish a reliable authorized push backend; otherwise tune the existing deferred sync within Android platform limits and provide manual refresh.

---

## 10. Remove visible Device UUID from Profile safely

- Remove the UUID label/value/copy control from the student-facing profile screen and any equivalent visible screen.
- Search the whole source for other unwanted UUID presentation, clipboard access, logs, and exports.
- Keep the internal installation identifier only where strictly necessary for fleet identity/revocation, and avoid returning it through public APIs or sending it to the official college ERP unless the integration contract requires it.
- Do not break telemetry upsert/revocation by deleting internal state as part of a UI-only change.
- Remove the hardcoded profile admin identity check noted above independently as a security fix; no student UI identifier can determine privileged access.

---

## 11. Contextual greetings and dashboard polish

Add a subtle greeting to the existing dashboard:

- Morning/afternoon/evening based on the user's actual local time (default policy is `Asia/Kolkata` when needed).
- Use the student's real profile display name only if present in authenticated data. Do not insert a guessed name.
- Optional small status text such as unread notification count or last successful sync time only when backed by real app state.
- Never show “attendance up to date” from stale, partial, or failed data.
- Keep content compact; do not let a greeting/banner push attendance and warnings below the fold.

Improve the app with focused, measurable UX refinements:

- Consistent spacing, typography, icon use, and card hierarchy following current `LloydTheme` / design system.
- Distinct loading, stale, partial, offline, empty, auth-expired, and retry states.
- Appropriate skeletons instead of flashing blank layouts; use caching only where invalidation and freshness are clear.
- Stable list keys and cheap Compose item rendering; avoid expensive operations inside each recomposition/list item.
- Correct system-bar/inset handling and adaptive layouts on small/large screens.
- Meaningful TalkBack descriptions, minimum 48dp targets, adequate contrast, and status labels beyond color alone.
- Keep animations subtle and skippable; no UI polish should hide errors or slow startup.
- Respect Back navigation/state restoration from calendar, latest attendance, feedback, receipt print, update settings, and notification tap-through.

Do not redesign every screen at once. Each UI change must solve a verified usability issue or support a requested feature.

---

## 12. AKTU syllabus — searchable subject picker and complete syllabus browser

**Source of truth:** `AKTU_CSE_1st_Year_Syllabus_2026-27.pdf` attached to this task. It is the 98-page AKTU B.Tech First Year CSE Stream syllabus for academic session 2026–27, covering Semesters I and II and detailed course syllabuses.

### 14.1 Product requirement — NO CSE A / CSE B split

Do **not** build separate syllabus screens, different syllabus databases, or curriculum filters for CSE A versus CSE B. Do not require section detection or section mapping to browse the syllabus.

Instead, create one complete, searchable **Subject Selector** containing every distinct subject/course option listed in the PDF's Semester I and Semester II scheme. Every option must be available to choose and inspect, irrespective of the logged-in student's section. The subject list must be sourced from the PDF-backed structured data, never hardcoded independently in UI code.

Group choices by semester, and show the source course code with the exact course title. Include a semester filter (`Semester I`, `Semester II`, or `All`) and search by course code or title. Selecting an option opens its full detailed syllabus. Because some scheme rows use slash-separated alternative codes and titles, represent each code/title pairing as a **separate selectable option**, while preserving the PDF's alternative relationship as informational metadata. Do not merge alternatives into one indistinguishable item, omit alternatives, or imply that the student is enrolled in every alternative simultaneously.

At minimum, inventory and validate the following subject choices from the PDF scheme tables. Verify every entry against the PDF rather than trusting this typed inventory blindly:

**Semester I**
- `AAS101E` — Quantum Physics and Applications
- `AAS102D` — Applied Chemistry for Smart Systems
- `AAS103D` — Calculus and Linear Algebra
- `AEE101A` — Fundamentals of Electrical Engineering
- `AEC101A` — Fundamentals of Electronics Engineering
- `ACS101` — Programming Languages
- `AAS104` — Introduction to Indian Knowledge System
- `AAS105` — Professional Communication & Technical Writing
- `AAI101` — Introduction to AI & Prompt Engineering
- `AME101A` — Fundamentals of Mechanical Engineering
- `AAS151` — Applied Physics Lab
- `AAS152D` — Applied Chemistry for Smart Systems Lab
- `AEE151A` — Electrical Engineering Lab
- `AEC151A` — Electronics Engineering Lab
- `ACE151E` — Computer-Aided Engineering Drawing Lab
- `AID151` — Innovation & Design Thinking Lab (Project-Based Learning)
- `AAS153` — Language Lab
- `AAC101` — Environment & Sustainability
- `AGP151` — General Proficiency

**Semester II**
- `AAS201E` — Quantum Physics and Applications
- `AAS202D` — Applied Chemistry for Smart Systems
- `AAS203D` — Numerical Methods
- `AEE201A` — Fundamentals of Electrical Engineering
- `AEC201A` — Fundamentals of Electronics Engineering
- `ACS201` — Essentials of Data Structure
- `AAS204` — Introduction to Indian Knowledge System
- `AAS205` — Professional Communication & Technical Writing
- `AAI201` — Introduction to AI & Prompt Engineering
- `AME201A` — Fundamentals of Mechanical Engineering
- `AAS251` — Applied Physics Lab
- `AAS252D` — Applied Chemistry for Smart Systems Lab
- `AEE251A` — Electrical Engineering Lab
- `AEC251A` — Electronics Engineering Lab
- `ACE251E` — Computer-Aided Engineering Drawing Lab
- `AID251` — Innovation & Design Thinking Lab (Project-Based Learning)
- `AAS253` — Language Lab
- `AAC201` — Environment & Sustainability
- `AGP251` — General Proficiency

A scheme row may list alternative titles/codes with slashes; the above list deliberately breaks those into separately selectable options. Retain the scheme row's source relationship in metadata to support auditing, but **do not add CSE A/B logic**. Treat repeated titles in different semesters as distinct semester/code records.

### 14.2 Subject detail screen — reference only, no personal tracking

Selecting a subject must open a complete, readable, scrollable syllabus detail page showing all information available for that course in the PDF:

- Exact title, course code, semester, course category, teaching department, source PDF page(s), and academic session.
- L/T/P pattern, NCrF/contact hours, internal and external assessment marks, total marks, credits, and non-credit/mandatory classification wherever specified in the scheme.
- Every course objective and course outcome (CO), without paraphrasing away meaningful distinctions.
- Each unit in source order, its exact unit description, all topics and subtopics, and stated weightage.
- Teaching-learning methodology, evaluation pattern, reference books/textbooks, online resources, and additional references wherever shown.
- Lab/practical course content, experiments, activities, and expected deliverables wherever the source contains them.
- Any general requirements shown by the PDF, including Student Induction Programme and AICTE activity-point requirements, in a clearly separate general-information area rather than incorrectly assigning them to an individual subject.

This is a **syllabus reference browser only**. Do not add personal study plans, topic checkboxes, completion statuses, per-student progress tracking, progress percentages, reminders, or local persistence for study state. The requirement is to let students select a subject and read its complete, accurate syllabus.

The UI should include clear unit navigation/expand-collapse, comfortable reading typography, a visible way to return to the subject selector, search across subjects and within a subject's unit/topic text, and appropriate loading, empty, and error states. Keep it fast and uncluttered; prioritize the actual syllabus content over dashboards and gamification.

### 14.3 Full extraction, structure and provenance

Inspect all relevant PDF text **and rendered visual tables/pages** where extracted text is incomplete or ambiguous. Preserve exact source meaning, code/title pairing, ordering, and unit hierarchy. Build structured data in a maintainable JSON/resource/seed asset or the established data layer, not scattered UI literals.

Maintain for each imported course and topic:
- academic session and semester;
- course category and every code/title variant shown;
- scheme row and detailed syllabus source page(s);
- the source text or normalized representation needed to audit the import;
- any uncertainty or extraction note requiring manual verification.

Do not fill missing material from general knowledge, other universities, an older syllabus, or guessed course outlines. If a detail is absent or unclear in the supplied PDF, mark it as absent/uncertain in the internal coverage report rather than inventing it.

### 14.4 Coverage and quality gate

Create a machine-readable syllabus coverage report mapping every scheme row, course code, title, objective, unit, topic, subtopic, outcome, reference section, practical/lab entry, and relevant general requirement to the imported structured record and source page(s). Verify:

- every distinct code/title choice from both semester scheme tables is separately selectable;
- every detailed syllabus section present in the PDF is attached to the correct subject code and semester;
- all units, nested topics/subtopics, unit weights, objectives, COs, evaluations, books/resources, and listed practicals are accounted for where present;
- alternative code/title pairs remain separate choices and retain their source relationship;
- repeated titles in Semester I and Semester II do not collide;
- there are no omitted topics, invented content, accidental duplicates, broken source-page links, or malformed hierarchy;
- every subject can be found by code/title, selected, opened, and navigated without CSE A/B or section mapping;
- the syllabus feature contains no personal study tracking, completion checklist, or saved progress state.

Run validation scripts/tests that compare the structured inventory against a source-derived coverage inventory. A matching course count alone is insufficient. Visually inspect PDF tables/pages whenever parsed text could have merged rows or lost codes. If any page or course cannot be transcribed with confidence, report that specific gap and do not claim full coverage.

### 14.5 Use of the route catalog / live ERP

Use the attached `LLOYD_ERP_ALL_ROUTES.md` to understand existing API capabilities, and inspect live OpenAPI only when helpful for validating current course/subject/unit/topic data. Do not change external ERP data. The PDF controls the official 2026–27 syllabus content for this feature; live ERP data may be used for a clearly labeled comparison report, but must not hide subject choices, silently replace PDF content, or reintroduce section-based curriculum filtering. Report discrepancies instead of silently reconciling them.

---

## 13. Admin duplicate records — diagnose before cleanup

Identify what “duplicate” means in this app (device telemetry entries, imported student records, attendance logs, list rendering, or duplicated writes). Reproduce the issue and trace it to the source.

For fleet telemetry:

- Use `device_id`/stable installation identity as the record key if the data contract defines one, and perform an upsert rather than append-only writes.
- Use trustworthy server timestamps and one authoritative persistence strategy. Avoid divergent memory, disk, Upstash, and Redis states silently returning different counts.
- Ensure a failed or retried write cannot create a second record for the same installation.
- Keep multiple devices for one student as separate valid installations.
- Test duplicate ingestion, out-of-order timestamps, retry, serverless cold start, Redis unavailability, and deletion across persisted storage.

For attendance logs:

- Deduplicate exact repeated record IDs; never collapse records merely because date, subject, teacher, or student name match.
- Reconcile pagination/summary counts and show the source of truth.

For official ERP admin/student records:

- The actual college ERP is external. Do not perform any cleanup mutation against it without explicit authorization and a supported admin workflow.

Prepare a dry-run duplicate report first. For any existing data cleanup, list primary key, proposed survivor, reason, dependencies, and rollback/export. Never delete based on matching names alone.

---

## 14. External route catalog usage (attached `LLOYD_ERP_ALL_ROUTES.md`)

Use this inventory as a navigational map, not an unverified guarantee that every call is currently permitted to the student role. Verify live OpenAPI schemas and current authentication behavior before integrating an endpoint.

Relevant routes listed in the file include:

- Student's own attendance summary/routine: `GET /api/student/me/monthly-attendance`, `GET /api/student/me/weekly-attendance`.
- Detailed attendance ledger, currently used by the app per its own `AGENTS.md`/data models: inspect `/api/attendance/student` and its supported pagination/schema.
- Feedback: `POST /api/feedback`, `GET /api/feedback/me`, reply/list routes subject to role scope.
- Formal student grievance routes, including `POST /api/grievances/student`, where permitted.
- Routine/subject/unit/topic/syllabus endpoints under `/api/routines/{school_id}` and `/api/academic/...`.
- Receipt/payment endpoints, several of which may be accounts-role restricted.

Keep API-specific parsing in the existing API/repository layer, not inside Compose UI. Do not change auth scope or impersonate roles to access an endpoint. Add schema fixtures for request/response parsing and redact tokens from errors.

---

## 15. Test and release gates

Add targeted regression tests and run them. At minimum:

### Android

```bash
cd android
./gradlew testDebugUnitTest --no-daemon
./gradlew lintDebug --no-daemon
```

Build a release only after signing integrity has been verified:

```bash
./gradlew assembleRelease --no-daemon
```

Also exercise relevant Compose navigation/manual UI states on an emulator or device if available. Be explicit if no device testing can be run.

### Node / fleet admin

- `node --check web/admin/app.js`
- `node --check server.js`
- Replace the placeholder `npm test` with deterministic tests covering auth, config allowlisting, public/private responses, ban/unban idempotency, sanitization/XSS, payload validation, persistence errors, OTA metadata, and rate limiting.
- Use isolated test stores/config and synthetic records; no production student records or destructive actions.
- Run tests through the actual `npm test` CI path.

### Mandatory acceptance scenarios

**Admin/security**
- No hardcoded/default credential grants access.
- Query/body secret bypass is rejected; no admin secret returned in JSON.
- Anonymous public config excludes private ban/admin data.
- Anonymous telemetry GET is denied or returns only a specifically designed aggregate; ingestion is validated and rate-limited.
- Ban/unban retries have stable intended state.
- Stored XSS payloads render as text and do not execute.
- Persistence failure does not return a misleading durable-success response.

**OTA/release**
- Auto and manual checks; up-to-date; available; paused; timeout/offline; malformed data; dismissed version; mandatory minimum; bad host/scheme; digest mismatch; wrong signer/version/asset.
- The downloaded APK's tag/version, package ID, signature, and checksum match the release metadata.
- Release broadcast authenticates and reports failures rather than swallowing them.

**Attendance/calendar**
- Pagination/counts reconcile; partial responses don't overwrite good cache; duplicate IDs ignored; present/absent/mixed/no-record/confirmed-holiday states correct; clicking a date opens the correct daily log; timezone/month edges correct.

**Notifications**
- No historic bootstrap burst, no repeated alerts from duplicate IDs, correct Present/Absent status, permission denial is handled, tap-through works on cold and warm start and opens the right record.

**Feedback/receipts/syllabus**
- Only current-user feedback/receipts are shown; send status comes from real API response; receipt values exactly match verified source; syllabus coverage report maps every selectable course code/title, topic/unit/subtopic, and source page; the syllabus browser has no CSE A/B split.

**UX**
- No dead More-menu items; UUID is not shown in student UI; greetings use actual local time/profile; offline/partial states are clear; accessible labels/touch targets remain correct; Back navigation restores useful context.

---

## 16. Deliverables and done definition

Before the detailed deliverables, include a short release-readiness summary with three separate lists: **verified complete**, **implemented but not fully verified**, and **blocked/not implemented**. Each item must point to evidence (file, test, command output, or external dependency). Do not collapse uncertainty into a single overall “done” score.


Produce:

1. Implemented source changes—not just a report—within the scoped modules.
2. A concise file-by-file change summary.
3. Baseline-versus-final test results with actual command outputs/pass/fail counts.
4. An updated current-main audit checklist distinguishing fixed, stale, unresolved, and unverified items.
5. A syllabus coverage artifact mapping every selectable subject code/title, unit/topic/subtopic, and source page, with all unresolved ambiguity documented. No personal study-progress feature or persistence tests are required.
6. An OTA/release integrity summary: verified production host, version/tag/artifact mapping, signing certificate fingerprint comparison, digest verification, and CI broadcast result.
7. A duplicate-record dry-run/reconciliation report and rollback plan if cleanup is needed.
8. Remaining blockers that need a real student account, device, Vercel environment, signing secret, or college administrator confirmation.

**A feature is not complete merely because its button or screen exists.** It is complete only when it uses the correct source, handles loading/error/empty/auth states, preserves existing behavior, and has appropriate regression evidence.

Do not claim 100% syllabus coverage, secure admin controls, a successful persistent write, working OTA installation, or real-time notifications unless the defined verification gate proves it.

---

## External technical references for implementation decisions

Use these as supporting implementation references; the repository's current behavior and provided API contracts remain the primary source of truth:

- Android WorkManager periodic work and constraints: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work
- Android notification runtime permission: https://developer.android.com/develop/ui/compose/notifications/notification-permission
- Android notification navigation/task back stack: https://developer.android.com/develop/ui/views/notifications/navigation
- Android app signing/update compatibility: https://developer.android.com/studio/publish/app-signing
- GitHub Releases API and release asset metadata/digests: https://docs.github.com/en/rest/releases/releases
- Android Network Security Configuration: https://developer.android.com/privacy-and-security/security-config
- Compose performance best practices: https://developer.android.com/develop/ui/compose/performance/bestpractices
- Compose accessibility semantics: https://developer.android.com/develop/ui/compose/accessibility/semantics
- OWASP Mobile Application Security Verification Standard (MASVS): https://mas.owasp.org/MASVS/
