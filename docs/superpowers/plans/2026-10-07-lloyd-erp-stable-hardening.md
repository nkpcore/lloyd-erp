# Lloyd ERP v1.0 Unified Release Hardening & Fleet Governance Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish an authoritative, production-grade Android client for **Lloyd ERP** that unites:
1. **Core Application Stability & Hardening** (from `lloyd-erp-stable-release-report.md`):
   - Single authoritative `AttendanceRepository` with atomic transactional synchronization.
   - Transparent reconciliation of the 121 aggregate vs 112 detailed ledger records discrepancy with **zero fake records** (strictly enforcing `AGENTS.md`).
   - Explicit UI state machine (`FRESH`, `STALE`, `OFFLINE`, `PARTIAL`, `ERROR`) with non-destructive cache preservation.
   - Consolidation of `AttendanceSyncWorker` and `AttendanceWidgetProvider`.
   - Purging legacy `MainActivity.java` and configuring production release signing with CI gates.
2. **Fleet Access Control & Targeted Revocation** (Master Control Plane):
   - **Default-Allow / Permissive Access**: Any student with ERP credentials can install and use the app immediately with zero friction.
   - **Student Identity Discovery**: Authenticated student names and IDs are captured dynamically so you know exactly who is in your fleet.
   - **Targeted Revocation (Device-Isolated Banlist)**: You hold unilateral power in the Web Admin Portal (`web/admin`) to revoke access for any specific rogue device UUID without blanket-blocking the student account across other devices.
   - **Instant Device Lockout**: If revoked, the app wipes local sessions and displays a clean lockout screen.
3. **Previous Version Control (Deprecation Gating)**:
   - Admin specifies `min_supported_version_code`.
   - Outdated clients running deprecated versions are blocked with a `VersionDeprecatedDialog` offering a 1-tap OTA update via `OtaUpdateManager.kt`.
4. **Dynamic Remote Announcements & Branding**:
   - Push real-time announcements to the Compose Dashboard and manage branding centrally.

**Tech Stack:** Kotlin 2.0, Jetpack Compose Material 3 Expressive, StateFlow & Coroutines, OkHttp 4.12, Gson, Android Keystore, WorkManager 2.9, JUnit 4, GitHub Actions CI.

---

## Failure Scenarios & Defensive Matrix ("Expect Every Scenario That Can Go Wrong")

| Scenario | Potential Failure / Risk | Architectural Defense & Mitigation |
|---|---|---|
| **1. Offline / Airplane Mode** | Client cannot reach admin server; app wrongly locks user out. | **Fail-Open Policy**: Network timeout/unavailability is treated as a temporary network state. Active sessions continue running in offline mode. |
| **2. Admin API Down / 500 / Malformed JSON** | Corrupt response crashes app or causes random bans. | **Strict Parsing & Fallback**: Wrapped in `try-catch`; invalid or empty responses fall back to cached config or default permissive state. |
| **3. Student Profile Name Pending on Initial Login** | Telemetry logs blank/empty name. | **Two-Stage Identity Pipeline**: If `userProfile.name` is null at initial login, telemetry automatically re-dispatches with `monthlyAttendance.studentName` as soon as first sync succeeds. |
| **4. Banned Student Clears Data / Reinstalls APK** | Student tries to bypass local ban by reinstalling. | **Pre-Entry Login Gate**: `LoginActivity.kt` queries `AccessControlManager` immediately upon ERP authentication. If banned, session is wiped and login aborts before reaching `MainComposeActivity`. |
| **5. Student Downgrades to Older Version (Version Bypass)** | Student stays on an old APK lacking security controls. | **Fleet Version Control**: Admin sets `min_supported_version_code`. Clients below this version are intercepted by `VersionDeprecatedDialog` with 1-tap update. |
| **6. Multi-Device Student Isolation** | Blocking a student account globally locks them out of valid secondary phones or tablets. | **Device-Isolated Revocation**: Revocation targets the rogue `device_id` as the primary key. Banning a compromised device blocks that device specifically without impacting legitimate access from other devices. |
| **7. 121 Aggregate vs 112 Detailed Ledger Mismatch** | App invents fake absences to make numbers match. | **Zero Fake Records**: Discrepancy is handled mathematically and explained transparently as 9 unrecorded sessions (`AttendanceReconciliation`). |
| **8. Academic Document Contamination** | Admin notices or developer credits bleed into official exports. | **Attribution Isolation**: Announcements and developer credits are strictly constrained to UI layers (About screen, dashboard banner), never injected into PDF/CSV student exports (`AGENTS.md`). |

---

## File Structure & Responsibilities

| File Path | Responsibility |
|---|---|
| `android/app/src/main/java/com/lloyd/attendance/core/access/AccessControlManager.kt` | Manages blocklist, version deprecation gating, fail-open offline logic, and remote announcements. |
| `android/app/src/test/java/com/lloyd/attendance/core/access/AccessControlManagerTest.kt` | Unit tests for all access, version deprecation, and offline fallback scenarios. |
| `android/app/src/main/java/com/lloyd/attendance/core/data/AttendanceState.kt` | Canonical domain models: `DataSourceState`, `SyncError`, `AttendanceReconciliation`, and `AttendanceSnapshot`. |
| `android/app/src/main/java/com/lloyd/attendance/core/data/AttendanceRepository.kt` | Single source of truth: transactional sync of monthly aggregate + detailed ledger, concurrency coalescing, safe cache persistence, and error classification. |
| `android/app/src/test/java/com/lloyd/attendance/core/data/AttendanceReconciliationTest.kt` | Regression test fixture verifying exact 121 vs 112 behavior with zero fake absences. |
| `android/app/src/test/java/com/lloyd/attendance/core/data/AttendanceRepositoryTest.kt` | Unit tests for transactional sync, cache fallback on error, and concurrency deduplication. |
| `android/app/src/main/java/com/lloyd/attendance/feature/dashboard/DashboardViewModel.kt` | Refactored to observe `AttendanceRepository.attendanceState` and expose rich UI state. |
| `android/app/src/main/java/com/lloyd/attendance/feature/dashboard/DashboardScreen.kt` | M3 UI supporting `FRESH`, `STALE`, `OFFLINE`, and `ERROR_WITH_CACHE` with reconciliation and broadcast banners. |
| `android/app/src/main/java/com/lloyd/attendance/feature/logs/AttendanceLogsViewModel.kt` | Refactored to observe `AttendanceRepository.attendanceState`. |
| `android/app/src/main/java/com/lloyd/attendance/ui/LoginActivity.kt` | Validates access with `AccessControlManager` immediately upon login. |
| `android/app/src/main/java/com/lloyd/attendance/ui/MainComposeActivity.kt` | Enforces access/version gate on resume and displays `AccessRevokedDialog` or `VersionDeprecatedDialog`. |
| `android/app/src/main/java/com/lloyd/attendance/widget/AttendanceSyncWorker.java` | Thin orchestration delegating sync to `AttendanceRepository` and checking revocation. |
| `android/app/src/main/java/com/lloyd/attendance/widget/AttendanceWidgetProvider.java` | Renders canonical persisted snapshot from `AttendanceRepository`. |
| `web/admin/index.html` | Fleet dashboard with Revoke/Restore buttons, Version Control card, and Broadcast Banner form. |
| `web/admin/app.js` | Interactive fleet blocklist management and version control state handling. |
| `android/app/build.gradle` | Production release signing configuration with CI secret fallback. |
| `.github/workflows/android-build.yml` | Quality gate pipeline: unit tests + lint + APK artifact checksum validation. |

---

### Task 1: Canonical Attendance State Models & Exact 121 vs 112 Regression Fixture
- [x] Create `AttendanceState.kt`
- [x] Create `AttendanceReconciliationTest.kt`
- [x] Verify test passes and commit

### Task 2: Transactional `AttendanceRepository` Implementation
- [x] Create `AttendanceRepository.kt`
- [x] Create `AttendanceRepositoryTest.kt`
- [x] Verify test passes and commit

### Task 3: Access Control & Version Deprecation Engine (`AccessControlManager.kt`)
- [x] Create `AccessControlManager.kt`
- [x] Create `AccessControlManagerTest.kt` covering all failure matrix scenarios
- [x] Verify test passes and commit

### Task 4: Student Identity & Fleet Discovery Pipeline
- [x] Wire authenticated student name from profile/monthly attendance in `TelemetryManager.kt`, `MainComposeActivity.kt`, and `LoginActivity.kt`
- [x] Verify tests pass and commit

### Task 5: Gatekeeping UI & Dialogs (Lockout, Outdated Version, Broadcast Notice)
- [x] Wire access check in `LoginActivity.kt` and `MainComposeActivity.kt`
- [x] Add `AccessRevokedDialog`, `VersionDeprecatedDialog` (with OTA 1-tap update), and broadcast banner in `DashboardScreen.kt`
- [x] Verify tests pass and commit

### Task 6: Refactor `DashboardViewModel` & Screens to Consume `AttendanceRepository`
- [x] Refactor `DashboardViewModel.kt` and `DashboardScreen.kt`
- [x] Refactor `AttendanceLogsViewModel.kt` and `SubjectDetailViewModel.kt`
- [x] Verify tests pass and commit

### Task 7: Unify `AttendanceSyncWorker` & `AttendanceWidgetProvider`
- [x] Refactor worker and widget to route through `AttendanceRepository`
- [x] Verify tests pass and commit

### Task 8: Web Admin Fleet Control Portal (`web/admin`)
- [x] Update `index.html`, `style.css`, and `app.js` with Revoke/Restore buttons, Version Control Panel, and Broadcast Banner controls
- [x] Test admin controls

### Task 9: Legacy Code Removal (`MainActivity.java`)
- [x] Delete `MainActivity.java` and clean `AndroidManifest.xml`
- [x] Verify build

### Task 10: Release Signing & CI Quality Gate Hardening
- [x] Update `build.gradle` release signing
- [x] Update `.github/workflows/android-build.yml` with lint gate
- [x] Full `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, and `./gradlew assembleRelease` verification
