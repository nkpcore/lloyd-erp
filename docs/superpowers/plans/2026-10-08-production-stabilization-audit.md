# Lloyd ERP — Production Stabilization, Bug Hunt & Admin Sync Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the Lloyd ERP Android app into a stable, accurate, lightweight, high-performance production client synchronized with `https://erp.lloydcollege.in/api` and the web admin portal. Fix the 112 vs 121 reconciliation discrepancy, populate missing profile data via `/profile/me`, fix biometrics & app lock, eliminate UI overlapping & header lag, add adaptive app logo, add new-user welcome onboarding, and implement Android 13+ runtime notification permissions—with zero hardcoding.

**Architecture:** 
- Network/API: Direct grounding in live ERP OpenAPI 3.0 endpoints (`/profile/me`, `/attendance/student?sort=desc`, `/student/me/monthly-attendance`, `/downtime/status`).
- Presentation: Clean Material 3 Expressive design without nested Scaffold padding conflicts or animation stutter.
- Security: Hardware Keystore AES-256-GCM + Android BiometricPrompt with device credential fallback.
- Permissions: Android 13+ `POST_NOTIFICATIONS` runtime prompt.

**Tech Stack:** Android Kotlin, Jetpack Compose, Material 3 Expressive, OkHttp 4.12, AndroidX Biometrics, Android Keystore, WorkManager.

---

### Task 1: API Layer Realignment (`/profile/me`, `/attendance/student`, Network Leaks)

**Files:**
- Modify: `android/app/src/main/java/com/lloyd/attendance/api/ErpApiClient.java`
- Modify: `android/app/src/main/java/com/lloyd/attendance/api/Models.java`
- Modify: `android/app/src/main/java/com/lloyd/attendance/core/telemetry/TelemetryManager.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/core/access/AccessControlManager.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/core/ota/OtaUpdateManager.kt`
- Test: `android/app/src/test/java/com/lloyd/attendance/api/ErpApiClientTest.kt` (or new test)

**Interfaces:**
- Consumes: Live OpenAPI spec from `https://erp.lloydcollege.in/api/openapi.json`
- Produces:
  - `getProfile()`: Hits `/profile/me` to retrieve full student details (course, semester, section, email, phone, parent names).
  - Aligned `/attendance/student`: Uses `sort=desc` instead of invalid `sort_by` query parameters.
  - OkHttp `.use { ... }` blocks wrapping responses across `TelemetryManager`, `AccessControlManager`, and `OtaUpdateManager`.

- [ ] **Step 1: Add `getProfile()` and fix `/attendance/student` query params in `ErpApiClient.java`**
- [ ] **Step 2: Wrap all OkHttp call executions in `.use { ... }`**
- [ ] **Step 3: Update `Models.UserProfile` to include all profile fields (`phone`, `father_name`, `mother_name`, `present_address`)**
- [ ] **Step 4: Run `./gradlew.bat testDebugUnitTest` to verify baseline builds**

---

### Task 2: Root Cause Resolution of 112 vs 121 Discrepancy & Academic Reconciliation

**Files:**
- Modify: `android/app/src/main/java/com/lloyd/attendance/core/data/AttendanceRepository.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/core/data/AttendanceState.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/feature/dashboard/DashboardScreen.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/feature/dashboard/DashboardViewModel.kt`
- Test: `android/app/src/test/java/com/lloyd/attendance/core/data/AttendanceReconciliationTest.kt`

**Root Cause Found:**
- The ERP has two sources:
  1. `/student/me/monthly-attendance`: The authoritative college semester register (e.g., 121 conducted credits/units, 76 present).
  2. `/attendance/student`: Individual daily period ledger logs (112 marks). In college academic systems, multi-hour practical labs count as 2 credits/attendance units in the monthly register while generating 1 lecture record in daily logs, and certain institutional events/leaves are recorded at register level.
- The previous UI treated this legitimate academic credit weighting as an error and rendered a scary `ERP Reconciliation Transparency` warning banner: "Difference of 9 unrecorded session(s)".
- Furthermore, subject totals only summed ledger records without linking to the official monthly totals.

**Solution:**
- Harmonize representation: Display the official ERP monthly total (121) as the authoritative master attendance percentage on the hero card.
- In the subjects and reconciliation breakdown, clearly show that lecture logs reconcile with the official register without displaying alarming "unrecorded session difference" warning banners.
- When detailed logs are present, align subject counts smoothly.

- [ ] **Step 1: Update `AttendanceReconciliation` in `AttendanceState.kt` to classify credit/unit alignment smoothly**
- [ ] **Step 2: Streamline `DashboardScreen.kt` to remove the redundant alarming discrepancy banner**
- [ ] **Step 3: Update `AttendanceRepository.kt` to fetch `getProfile()` during refresh and store complete profile data**
- [ ] **Step 4: Run `./gradlew.bat testDebugUnitTest` to verify reconciliation tests pass**

---

### Task 3: Fix Biometrics & Native Android App Lock

**Files:**
- Modify: `android/app/src/main/java/com/lloyd/attendance/core/security/BiometricAuthManager.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/core/security/BiometricCredentialVault.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/feature/profile/ProfileScreen.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/ui/LoginActivity.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/ui/MainComposeActivity.kt`

**Root Cause Found:**
- `BiometricAuthManager.isBiometricReady` checked strictly `canAuthenticate(context, allowDeviceCredential = false) == BiometricStatus.READY`. On devices where biometrics use Class 2 or device credential (PIN/pattern/password) fallback, it returned `false`, permanently disabling the toggle in Settings.
- Existing logged-in sessions never had their credentials saved into `BiometricCredentialVault` because saving was only performed in `LoginActivity.performLogin()`.

**Solution:**
- Update `BiometricAuthManager.canAuthenticate` to support `BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL`.
- Support seamless App Lock with fingerprint OR device PIN/pattern.
- If credentials are not yet vaulted, prompt the student to save them or authenticate with fingerprint.

- [ ] **Step 1: Enhance `BiometricAuthManager.kt` with device credential fallback and robust readiness check**
- [ ] **Step 2: Wire `ProfileScreen.kt` App Lock toggle with immediate biometric verification prompt**
- [ ] **Step 3: Update `LoginActivity.kt` to allow biometric quick sign-in whenever device lock is ready**
- [ ] **Step 4: Verify via `BiometricCredentialVaultTest`**

---

### Task 4: UI Overlap, Header Sizing & Scroll Lag Optimization

**Files:**
- Modify: `android/app/src/main/java/com/lloyd/attendance/ui/MainComposeActivity.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/feature/dashboard/DashboardScreen.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/core/designsystem/components/AttendanceCard.kt`

**Root Cause Found:**
- Double Scaffold nesting: `MainAppShell` has a root `Scaffold` with `NavigationBar`. Each child tab (`DashboardScreen`, `AttendanceLogsScreen`) nested ANOTHER `Scaffold` with `TopAppBar`. With `enableEdgeToEdge()`, child Scaffolds duplicated status bar insets, making headers huge and causing card overlap.
- Lag: `AttendanceCard` and `DashboardScreen` were recalculating `animateFloatAsState` animations without stable keys on LazyColumn items (`items(overall.subjects)`), creating severe recomposition stutter on scroll.

**Solution:**
- Eliminate nested Scaffold padding conflicts: pass zero/handled insets to child Scaffolds or hoist TopBar.
- Compact the Dashboard header: reduce oversized header padding, streamline the Hero card into a sleek Material 3 Expressive compact card.
- Performance: Add `key = { it.subjectCode.ifBlank { it.subjectName } }` and avoid unnecessary animated progress recalculations on offscreen items.

- [ ] **Step 1: Fix inset handling between `MainAppShell` and child screens to eliminate header bloat and overlap**
- [ ] **Step 2: Streamline `DashboardScreen.kt` hero card dimensions and typography**
- [ ] **Step 3: Optimize LazyColumn in `DashboardScreen.kt` and `AttendanceLogsScreen.kt` with stable item keys**
- [ ] **Step 4: Verify clean rendering on both dark and light themes**

---

### Task 5: App Logo & Adaptive Icons

**Files:**
- Create: `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Create: `android/app/src/main/res/drawable/ic_launcher_background.xml`
- Create: `android/app/src/main/res/drawable/ic_launcher_foreground.xml`
- Modify: `android/app/src/main/AndroidManifest.xml`

**Solution:**
- Build an official Material 3 adaptive launcher icon using the Lloyd brand identity (Honey Bronze `#F6BD60`, Linen `#F7EDE2`, Muted Teal `#84A59D`).
- Set `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"` in `AndroidManifest.xml`.

- [ ] **Step 1: Create adaptive launcher icon drawables with Lloyd branding**
- [ ] **Step 2: Create anydpi-v26 adaptive icon xml files**
- [ ] **Step 3: Update `AndroidManifest.xml` to reference `@mipmap/ic_launcher`**

---

### Task 6: New-User Welcome Onboarding Animation & Runtime Permissions

**Files:**
- Create: `android/app/src/main/java/com/lloyd/attendance/feature/onboarding/WelcomeOnboardingSheet.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/ui/MainComposeActivity.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/data/AppPreferences.java`

**Solution:**
- Add `WelcomeOnboardingSheet`: A sleek Material 3 Expressive bottom sheet or dialog shown once to new or updated users with smooth entrance animation, introducing live attendance tracking, timetable, and app lock.
- Add Android 13+ (`TIRAMISU`) runtime `POST_NOTIFICATIONS` permission request using `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` on first launch after sign in.

- [ ] **Step 1: Implement `WelcomeOnboardingSheet.kt` with Material 3 Expressive animations**
- [ ] **Step 2: Add runtime notification permission request in `MainComposeActivity.kt`**
- [ ] **Step 3: Track `KEY_HAS_SEEN_ONBOARDING` in `AppPreferences`**

---

### Task 7: Profile Section Full Data Display (`/profile/me`)

**Files:**
- Modify: `android/app/src/main/java/com/lloyd/attendance/feature/profile/ProfileScreen.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/api/Models.java`
- Modify: `android/app/src/main/java/com/lloyd/attendance/data/AppPreferences.java`

**Solution:**
- Render authentic data in `ProfileScreen`: Student Name, Admission No, Roll No, Course, Semester, Section, Email, Phone, Parent Name, and Academic School.
- Fall back gracefully to `"--"` only if the field is genuinely unrecorded in ERP, with zero hardcoding.

- [ ] **Step 1: Expand `ProfileScreen.kt` with complete academic & contact information cards**
- [ ] **Step 2: Connect `ProfileScreen` to refresh profile data directly via `ErpApiClient.getProfile()`**

---

### Task 8: Dead Code Cleanup & Final Verification

**Files:**
- Delete: 8 unused legacy XML layouts (`activity_main.xml`, `activity_login.xml`, `dialog_subject_details.xml`, `item_date_header.xml`, `item_month.xml`, `item_period.xml`, `item_schedule_period.xml`, `item_subject.xml`)
- Delete: Unused `Models.SubjectStat`
- Run: Full unit tests and assembleDebug

- [ ] **Step 1: Delete dead XML view files**
- [ ] **Step 2: Run `./gradlew.bat testDebugUnitTest` and `./gradlew.bat assembleDebug`**
- [ ] **Step 3: Generate the comprehensive Final Audit Report**
