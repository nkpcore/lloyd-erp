# Lloyd ERP Modernization & Admin Telemetry Design Specification

**Author:** Nikhil Pandey  
**Date:** 2026-10-07  
**Status:** Approved  
**Version:** 2.0.0  

---

## 1. Executive Summary & Goals

This specification defines the complete modernization of the Lloyd Attendance Android client and companion ecosystem. The overhaul delivers:

1. **Brand & Visual Identity Overhaul**:
   - Migration from legacy purple accents to an intentional, warm designer palette: Honey Bronze (`#F6BD60`), Linen (`#F7EDE2`), Cotton Rose (`#F5CAC3`), Muted Teal (`#84A59D`), and Light Coral (`#F28482`).
   - High-contrast dark mode pairing with warm slate and muted emerald accents.
   - Modern vector logo emblem (academic shield + modern geometric checkmark).
   - Elegant, understated developer attribution: *"Designed & Developed with precision by Nikhil Pandey"*.
2. **Absolute Data Integrity (Zero-Discrepancy Guarantee)**:
   - Resolution of the syntax error in `DashboardViewModel.kt`.
   - Reconciliation algorithm aligning aggregate monthly data (`/api/student/me/monthly-attendance`) with granular class logs (`/api/attendance/student`).
   - Clear ledger verification: no lost lectures, no phantom sessions, exact attendance percentage computation.
3. **Streamlined 3-Tab Information Architecture**:
   - Removal of the redundant 3rd bottom navigation item ("Bunk Advisor").
   - 3 focused primary tabs: **Attendance** (Dashboard), **Daily Logs**, and **Profile**.
4. **Subject & Faculty Deep-Dive Screen**:
   - Tapping any subject opens a dedicated screen with lecture history, teacher logs, bunk safety margin, and interactive "What-If" simulator.
5. **Material 3 Expressive Login Screen**:
   - Full migration of legacy `LoginActivity.java` to Jetpack Compose.
   - Smooth entrance motion, fluid input fields, shake animations on error, and biometric quick-authentication.
6. **In-App GitHub Releases OTA Updater**:
   - Seamless background release check against `https://api.github.com/repos/nkpcore/lloyd-erp/releases/latest`.
   - In-app update notification banner, changelog preview, and one-tap download + install via `FileProvider`.
7. **User Analytics & Admin Web Dashboard**:
   - Lightweight, privacy-respecting client telemetry reporting device UUID, student ID/name, app version, OS version, and last active timestamp to Supabase/Firestore.
   - Beautiful, responsive web admin dashboard in `web/admin/` tracking active installs, version distribution, and live user table.

---

## 2. Color System & Design Tokens

### 2.1 Color Tokens
```kotlin
// Light Theme Palette
val HoneyBronze = Color(0xFFF6BD60)
val LinenSurface = Color(0xFFF7EDE2)
val CottonRoseContainer = Color(0xFFF5CAC3)
val MutedTealPrimary = Color(0xFF84A59D)
val LightCoralError = Color(0xFFF28482)
val DarkSlateText = Color(0xFF2D3142)

// Dark Theme Palette
val DarkBackground = Color(0xFF161B22)
val DarkSurface = Color(0xFF21262D)
val DarkSurfaceVariant = Color(0xFF30363D)
val HoneyBronzeDark = Color(0xFFF8C87A)
val MutedTealDark = Color(0xFF9DC0B8)
val LightCoralDark = Color(0xFFF59D9B)
val LightText = Color(0xFFF0F6FC)
```

### 2.2 Modern Vector Logo
- **Concept**: A minimalist, geometric academic shield crafted with clean angled lines, enclosing an upward progress arrow and checkmark.
- **Colors**: Muted Teal primary outline with Honey Bronze inner emblem on Linen background.
- **Assets**: Replaces `ic_launcher` and in-app header logos in `android/app/src/main/res/drawable/`.

### 2.3 Developer Attribution
- **Placement**:
  - `LoginScreen`: Discreet bottom typography: `"Lloyd ERP • Crafted by Nikhil Pandey"`
  - `ProfileScreen`: Card footer with GitHub link and author badge.
  - `AboutDialog`: Developer bio, version info, and acknowledgments.

---

## 3. Architecture & Data Flow

```
+----------------------------------------------------------------+
|                         LLOYD ERP API                          |
|  - POST /api/auth/login                                        |
|  - GET  /api/student/me/monthly-attendance                     |
|  - GET  /api/attendance/student?student_id={id}&page=1..N      |
+----------------------------------------------------------------+
                               |
                               v
+----------------------------------------------------------------+
|                       ANDROID CLIENT                           |
|                                                                |
|  +------------------------+      +---------------------------+ |
|  |     ErpApiClient       | ---> | Data Reconciliation Engine | |
|  +------------------------+      +---------------------------+ |
|                                                |               |
|                                                v               |
|  +------------------------+      +---------------------------+ |
|  |   DashboardViewModel   |      |  SubjectDetailViewModel   | |
|  +------------------------+      +---------------------------+ |
|               |                                |               |
|               v                                v               |
|  +------------------------+      +---------------------------+ |
|  |    DashboardScreen     |      |    SubjectDetailScreen    | |
|  | (Overall Gauge, List)  |      | (Logs, Teacher, Simulator)| |
|  +------------------------+      +---------------------------+ |
|                                                                |
|  +------------------------+      +---------------------------+ |
|  |    OtaUpdateManager    |      |    TelemetryReporter      | |
|  |  (GitHub Releases API) |      | (Supabase/Firestore Ping) | |
|  +------------------------+      +---------------------------+ |
+----------------------------------------------------------------+
                                                 |
                                                 v
                                   +---------------------------+
                                   |    WEB ADMIN DASHBOARD    |
                                   | (Live Users & Versions)   |
                                   +---------------------------+
```

---

## 4. Component Specifications

### 4.1 Navigation Architecture (3 Tabs)
`MainTab` enum in `MainComposeActivity.kt`:
```kotlin
enum class MainTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Attendance", Icons.Default.BarChart),
    LOGS("Daily Logs", Icons.AutoMirrored.Filled.EventNote),
    PROFILE("Profile", Icons.Default.Person)
}
```

### 4.2 Subject & Faculty Deep-Dive Screen
- Triggered by tapping any `AttendanceCard` on `DashboardScreen`.
- Navigation: Animated slide-in transition from right.
- Contents:
  1. **Top Bar**: Subject Name, Back Button, Subject Code badge.
  2. **Hero Card**:
     - Circular attendance gauge (animated fill).
     - Classes attended / Classes held.
     - Teacher Name with avatar and department.
     - Contextual Bunk Advisor advice pill.
  3. **Interactive Simulator**:
     - "Attend next X classes" / "Miss next Y classes" dynamic sliders with immediate feedback.
  4. **Subject Lecture Ledger**:
     - Chronological list of lectures for this subject.
     - Date, lecture number, faculty who took the class, Present/Absent status badge.

### 4.3 Material 3 Expressive Login Screen
- Replaces legacy Java XML activity.
- Visual elements:
  - Floating logo emblem with subtle entrance spring animation.
  - Floating Card container with 24dp rounded corners.
  - Material 3 `OutlinedTextField` with custom colors (`MutedTeal` border focus, `HoneyBronze` cursor).
  - Interactive "Sign In" button with loading progress state and tactile press effect.
  - Biometric quick-login button when saved credentials/token exist.
  - Animated error banner with gentle horizontal shake on invalid login.
  - Subtle footer: `"Crafted by Nikhil Pandey"`.

### 4.4 In-App GitHub Releases OTA Updater
- Class: `com.lloyd.attendance.core.ota.OtaUpdateManager`
- API Endpoint: `https://api.github.com/repos/nkpcore/lloyd-erp/releases/latest`
- Update Detection:
  - Strips leading `v` from `tag_name` (e.g. `v1.0.14` -> `1.0.14`).
  - Compares with `BuildConfig.VERSION_NAME`.
- User Flow:
  - If a newer version is found, display an update card in `ProfileScreen` (and dismissible banner in `DashboardScreen`).
  - Tapping "Update Now" downloads the APK to `getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)`.
  - Upon download completion, launches Android `Intent.ACTION_VIEW` with `FileProvider` URI and `FLAG_GRANT_READ_URI_PERMISSION`.

### 4.5 Telemetry & Web Admin Dashboard
- Client class: `com.lloyd.attendance.core.telemetry.TelemetryManager`
  - Records on login and daily sync:
    * `device_id`: Secure random UUID stored in SharedPreferences.
    * `student_id`: Lloyd ERP Student Profile ID.
    * `student_name`: Student full name.
    * `app_version`: `BuildConfig.VERSION_NAME` (e.g. `1.0.14`).
    * `version_code`: `BuildConfig.VERSION_CODE`.
    * `os_version`: Android SDK version & release (e.g. Android 14).
    * `device_model`: `Build.MANUFACTURER + " " + Build.MODEL`.
    * `last_active`: ISO-8601 timestamp.
- Supabase / Firestore Table: `app_users`
- Web Dashboard (`web/admin/index.html`):
  - Modern, responsive single-page application styled with the Honey Bronze / Linen / Muted Teal palette.
  - Metrics cards: Total Registered Students, Active Today, Active This Week, Latest App Version.
  - Version Breakdown Bar Chart: distribution of students across app versions.
  - Live Student Table: Search by name/roll number, filter by version, sort by last active date.

---

## 5. Verification & Testing Protocol

1. **Compilation & Unit Tests**:
   - Fix `-package` error in `DashboardViewModel.kt`.
   - Run unit tests: `./gradlew testDebugUnitTest` verifying `AttendanceLogsProcessorTest`, `AttendanceCalculatorTest`, and new telemetry/OTA tests.
2. **Data Consistency Verification**:
   - Verify that summing subject classes matches overall attendance numbers.
   - Verify pagination in `getStudentAttendanceLogs` handles 100+ lectures without dropping records.
3. **UI & Navigation Verification**:
   - Verify bottom navigation has exactly 3 tabs.
   - Verify tapping a subject navigates to `SubjectDetailScreen` with accurate teacher logs and simulator.
   - Verify new warm palette renders cleanly in both Light and Dark themes.
4. **OTA & Telemetry Verification**:
   - Verify mock GitHub release response triggers update flow.
   - Verify telemetry payload conforms to JSON schema.
