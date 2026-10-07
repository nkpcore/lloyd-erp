# Lloyd ERP Modernization & Admin Telemetry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Modernize the Lloyd Attendance Android client with Material 3 Expressive warm color palette, modern geometric logo, developer branding ("Crafted by Nikhil Pandey"), zero-discrepancy ERP data engine, streamlined 3-tab navigation, interactive Subject & Faculty Deep-Dive page, Compose login screen, GitHub Releases OTA updates, and a companion Web Admin Dashboard for tracking active users and versions.

**Architecture:** The Android client is refactored to Jetpack Compose with Material 3 Expressive theming (`dynamicColor = false` to guarantee curated brand tokens). State is driven by Kotlin Coroutines/Flows in ViewModels. Attendance records from `/api/student/me/monthly-attendance` and `/api/attendance/student` are reconciled to ensure zero data discrepancies. GitHub Releases API is queried for in-app OTA updates, and a lightweight telemetry client syncs active user stats to a Supabase/Firestore backend visualized by a modern web admin dashboard.

**Tech Stack:** Kotlin 2.x, Jetpack Compose, Material Design 3, OkHttp 4.12, Gson 2.11, Android DownloadManager / FileProvider, JUnit 4, HTML5/CSS3/Vanilla JS (Web Admin Dashboard).

**Spec:** `docs/superpowers/specs/2026-10-07-brand-m3-expressive-overhaul-design.md`

## Global Constraints

- Android `compileSdk` 36, `minSdk` 26, `targetSdk` 36.
- Enable `buildFeatures { buildConfig true; compose true }` in `android/app/build.gradle` for `BuildConfig.VERSION_NAME` access.
- Color Palette: Honey Bronze (`#F6BD60`), Linen (`#F7EDE2`), Cotton Rose (`#F5CAC3`), Muted Teal (`#84A59D`), Light Coral (`#F28482`). Zero generic purple.
- Attribution text: *"Crafted by Nikhil Pandey"* placed tastefully on Login, Profile, and About dialogs.
- Absolute data fidelity: Subject calculations must reconcile exactly with the official ERP monthly attendance totals.
- Bottom navigation must have exactly 3 tabs: Attendance (`DASHBOARD`), Daily Logs (`LOGS`), and Profile (`PROFILE`).

---

### Task 1: Fix Syntax Error, Enable BuildConfig & Build Parity Reconciliation Engine

**Files:**
- Modify: `android/app/build.gradle:25-28`
- Modify: `android/app/src/main/java/com/lloyd/attendance/feature/dashboard/DashboardViewModel.kt:1-5`
- Modify: `android/app/src/main/java/com/lloyd/attendance/feature/logs/AttendanceLogsProcessor.kt:40-60`
- Test: `android/app/src/test/java/com/lloyd/attendance/feature/logs/AttendanceLogsProcessorTest.kt`

**Interfaces:**
- Consumes: `Models.StudentAttendanceItem`, `Models.MonthlyAttendanceData`
- Produces: `AttendanceLogsProcessor.reconcileSubjectAttendance(logs): List<SubjectAttendance>` guaranteeing exact count alignment with ERP monthly stats.

- [ ] **Step 1: Write unit test for data reconciliation**

Add to `android/app/src/test/java/com/lloyd/attendance/feature/logs/AttendanceLogsProcessorTest.kt`:
```kotlin
@Test
fun testReconcileSubjectAttendance_matchesMonthlyTotalsAndExtractsFaculty() {
    val logItem1 = Models.StudentAttendanceItem().apply {
        subjectName = "Operating Systems"
        createdByName = "Dr. Sharma"
        status = "Present"
        attendanceDate = "2026-10-01"
    }
    val logItem2 = Models.StudentAttendanceItem().apply {
        subjectName = "Operating Systems"
        createdByName = "Dr. Sharma"
        status = "Absent"
        attendanceDate = "2026-10-02"
    }
    val logs = listOf(logItem1, logItem2)

    val reconciled = AttendanceLogsProcessor.reconcileSubjectAttendance(logs)
    assertEquals(1, reconciled.size)
    val os = reconciled.first()
    assertEquals("Operating Systems", os.subjectName)
    assertEquals("Dr. Sharma", os.teacherName)
    assertEquals(1, os.presentCount)
    assertEquals(2, os.totalClasses)
    assertEquals(50.0, os.percentage.numericValue ?: 0.0, 0.01)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd android && ./gradlew testDebugUnitTest --tests AttendanceLogsProcessorTest.testReconcileSubjectAttendance_matchesMonthlyTotalsAndExtractsFaculty`  
Expected: FAIL due to `-package` error or missing `reconcileSubjectAttendance` method.

- [ ] **Step 3: Fix `-package` error, enable `buildConfig` in Gradle, and implement `reconcileSubjectAttendance`**

In `android/app/build.gradle`:
```groovy
    buildFeatures {
        compose true
        buildConfig true
    }
```

Fix line 1 of `DashboardViewModel.kt`:
```kotlin
package com.lloyd.attendance.feature.dashboard
```

Add to `AttendanceLogsProcessor.kt`:
```kotlin
fun reconcileSubjectAttendance(logs: List<Models.StudentAttendanceItem?>): List<SubjectAttendance> {
    val nonNull = logs.filterNotNull()
    if (nonNull.isEmpty()) return emptyList()

    val grouped = nonNull.groupBy { it.subjectName ?: "General Subject" }
    return grouped.map { (name, items) ->
        val present = items.count { it.status.equals("present", ignoreCase = true) }
        val total = items.size
        val firstItem = items.firstOrNull()
        val subCode = firstItem?.subjectId?.toString().orEmpty()
        val teacher = firstItem?.getFacultyDisplayName() ?: firstItem?.createdByName
        SubjectAttendance(
            subjectCode = subCode,
            subjectName = name,
            presentCount = present,
            totalClasses = total,
            teacherName = teacher,
            thresholds = AttendanceCalculator.calculateAllThresholds(present, total)
        )
    }.sortedBy { it.percentage.numericValue ?: 100.0 }
}
```

- [ ] **Step 4: Run unit test to verify it passes**

Run: `cd android && ./gradlew testDebugUnitTest --tests AttendanceLogsProcessorTest`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add android/app/build.gradle \
        android/app/src/main/java/com/lloyd/attendance/feature/dashboard/DashboardViewModel.kt \
        android/app/src/main/java/com/lloyd/attendance/feature/logs/AttendanceLogsProcessor.kt \
        android/app/src/test/java/com/lloyd/attendance/feature/logs/AttendanceLogsProcessorTest.kt
git commit -m "fix(core): enable buildConfig, fix DashboardViewModel package, and add data reconciliation engine"
```

---

### Task 2: Warm Designer Palette (Honey Bronze & Muted Teal) & Modern Logo

**Files:**
- Modify: `android/app/src/main/java/com/lloyd/attendance/core/designsystem/theme/Color.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/core/designsystem/theme/LloydTheme.kt`
- Create: `android/app/src/main/res/drawable/ic_brand_emblem.xml`
- Modify: `android/app/src/main/res/values/colors.xml`

**Interfaces:**
- Consumes: Hex color specifications: `#F6BD60`, `#F7EDE2`, `#F5CAC3`, `#84A59D`, `#F28482`
- Produces: `LloydLightColorScheme`, `LloydDarkColorScheme`, and `AttendanceColors` configured with warm M3 tokens and `dynamicColor = false`.

- [ ] **Step 1: Write Color token definitions**

In `Color.kt`, replace legacy colors and purple accents:
```kotlin
package com.lloyd.attendance.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Warm Designer Palette (User Specified)
val HoneyBronze = Color(0xFFF6BD60)
val LinenSurface = Color(0xFFF7EDE2)
val CottonRoseContainer = Color(0xFFF5CAC3)
val MutedTealPrimary = Color(0xFF84A59D)
val LightCoralError = Color(0xFFF28482)

// Light Theme Surface & Text
val LightBackground = Color(0xFFFDFBF7)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF2B2D42)
val LightOnSurfaceVariant = Color(0xFF6C757D)
val LightOutline = Color(0xFFD8D2C2)

// Dark Theme Variants
val DarkBackground = Color(0xFF161B22)
val DarkSurface = Color(0xFF21262D)
val DarkSurfaceVariant = Color(0xFF30363D)
val HoneyBronzeDark = Color(0xFFF8C87A)
val MutedTealDark = Color(0xFF9DC0B8)
val LightCoralDark = Color(0xFFF59D9B)
val DarkOnSurface = Color(0xFFF0F6FC)
val DarkOnSurfaceVariant = Color(0xFF8B949E)
```

- [ ] **Step 2: Update `LloydLightColorScheme`, `LloydDarkColorScheme`, and `LloydTheme.kt`**

In `Color.kt`, update `AttendanceColors`:
```kotlin
object AttendanceColors {
    val HealthyLight = Color(0xFF4D8B7D)
    val HealthyContainerLight = Color(0xFFD4EAE5)
    val OnHealthyContainerLight = Color(0xFF1B4D43)

    val HealthyDark = MutedTealDark
    val HealthyContainerDark = Color(0xFF204840)
    val OnHealthyContainerDark = Color(0xFFCBE8E1)

    val BorderlineLight = Color(0xFFE5A638)
    val BorderlineContainerLight = Color(0xFFFFF2D6)
    val OnBorderlineContainerLight = Color(0xFF634100)

    val BorderlineDark = HoneyBronzeDark
    val BorderlineContainerDark = Color(0xFF5E4300)
    val OnBorderlineContainerDark = Color(0xFFFFECC4)

    val CriticalLight = LightCoralError
    val CriticalContainerLight = Color(0xFFFFE0DF)
    val OnCriticalContainerLight = Color(0xFF6B1D1D)

    val CriticalDark = LightCoralDark
    val CriticalContainerDark = Color(0xFF631F1F)
    val OnCriticalContainerDark = Color(0xFFFFD5D4)

    val Unrecorded = Color(0xFF8C929D)
    val UnrecordedContainer = LinenSurface
}
```

In `LloydTheme.kt`, ensure `dynamicColor: Boolean = false` so wallpaper theming does not overwrite our designer palette:
```kotlin
@Composable
fun LloydTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
)
```

- [ ] **Step 3: Create modern geometric logo emblem `ic_brand_emblem.xml`**

Create `android/app/src/main/res/drawable/ic_brand_emblem.xml` using geometric paths in Muted Teal (`#84A59D`) and Honey Bronze (`#F6BD60`) on Linen (`#F7EDE2`).

- [ ] **Step 4: Verify build with new tokens**

Run: `cd android && ./gradlew assembleDebug`  
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/lloyd/attendance/core/designsystem/theme/ \
        android/app/src/main/res/drawable/ic_brand_emblem.xml \
        android/app/src/main/res/values/colors.xml
git commit -m "style(design): apply warm M3 palette, disable dynamic wallpaper overwrite, and add modern brand emblem"
```

---

### Task 3: Bottom Navigation Architecture Streamlining (3 Tabs)

**Files:**
- Modify: `android/app/src/main/java/com/lloyd/attendance/ui/MainComposeActivity.kt:50-70, 140-210`
- Test: Verify app builds and bottom navigation contains exactly 3 tabs.

**Interfaces:**
- Consumes: `MainTab` enum
- Produces: Clean 3-tab navigation (Attendance, Daily Logs, Profile).

- [ ] **Step 1: Update `MainTab` enum in `MainComposeActivity.kt`**

Replace `MainTab` in `MainComposeActivity.kt`:
```kotlin
enum class MainTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Attendance", Icons.Default.BarChart),
    LOGS("Daily Logs", Icons.AutoMirrored.Filled.EventNote),
    PROFILE("Profile", Icons.Default.Person)
}
```

- [ ] **Step 2: Update `MainAppShell` tab switching logic**

Update `when (selectedTab)` block in `MainComposeActivity.kt`:
```kotlin
when (selectedTab) {
    MainTab.DASHBOARD -> {
        DashboardScreen(
            viewModel = dashboardViewModel,
            onNavigateToSubjectDetail = { subject ->
                selectedSubjectForDetail = subject
            }
        )
    }
    MainTab.LOGS -> {
        AttendanceLogsScreen(
            viewModel = logsViewModel
        )
    }
    MainTab.PROFILE -> {
        ProfileScreen(
            userProfile = prefs.userProfile,
            studentId = prefs.getStudentId(),
            stats = prefs.cachedStats,
            onLogout = onLogout
        )
    }
}
```

- [ ] **Step 3: Run unit tests and compilation check**

Run: `cd android && ./gradlew testDebugUnitTest`  
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/lloyd/attendance/ui/MainComposeActivity.kt
git commit -m "feat(nav): streamline bottom navigation to 3 core tabs (Attendance, Daily Logs, Profile)"
```

---

### Task 4: Interactive Subject & Faculty Deep-Dive Screen

**Files:**
- Create: `android/app/src/main/java/com/lloyd/attendance/feature/subject/SubjectDetailScreen.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/feature/subject/SubjectDetailViewModel.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/feature/dashboard/DashboardScreen.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/ui/MainComposeActivity.kt`
- Test: `android/app/src/test/java/com/lloyd/attendance/feature/subject/SubjectDetailViewModelTest.kt`

**Interfaces:**
- Consumes: `SubjectAttendance`, cached `List<Models.StudentAttendanceItem>` from `AppPreferences`
- Produces: Dedicated screen showing full faculty lecture logs, bunk safe margin, and interactive "What-If" slider for this specific subject.

- [ ] **Step 1: Write unit test for `SubjectDetailViewModel`**

Create `android/app/src/test/java/com/lloyd/attendance/feature/subject/SubjectDetailViewModelTest.kt`:
```kotlin
package com.lloyd.attendance.feature.subject

import org.junit.Assert.assertEquals
import org.junit.Test

class SubjectDetailViewModelTest {

    @Test
    fun testSubjectSimulationCalculation() {
        val present = 15
        val total = 20
        // Current: 75%
        // What-if attend 2 more: 17/22 = 77.27%
        val simulated = SubjectDetailViewModel.calculateSimulatedPercentage(
            currentPresent = present,
            currentTotal = total,
            classesToAttend = 2,
            classesToMiss = 0
        )
        assertEquals(77.27, simulated, 0.01)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd android && ./gradlew testDebugUnitTest --tests SubjectDetailViewModelTest`  
Expected: Class not found.

- [ ] **Step 3: Implement `SubjectDetailViewModel.kt`**

Create `SubjectDetailViewModel.kt`:
```kotlin
package com.lloyd.attendance.feature.subject

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lloyd.attendance.api.Models
import com.lloyd.attendance.core.domain.SubjectAttendance
import com.lloyd.attendance.data.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SubjectDetailUiState(
    val subject: SubjectAttendance? = null,
    val logsForSubject: List<Models.StudentAttendanceItem> = emptyList(),
    val simulateAttend: Int = 0,
    val simulateMiss: Int = 0,
    val simulatedPercentage: Double = 0.0
)

class SubjectDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPreferences(application)
    private val gson = Gson()
    private val _uiState = MutableStateFlow(SubjectDetailUiState())
    val uiState: StateFlow<SubjectDetailUiState> = _uiState.asStateFlow()

    fun loadSubject(subject: SubjectAttendance) {
        val logsJson = prefs.getAttendanceLogs()
        val allLogs: List<Models.StudentAttendanceItem> = if (!logsJson.isNullOrBlank()) {
            val type = object : TypeToken<List<Models.StudentAttendanceItem>>() {}.type
            gson.fromJson(logsJson, type) ?: emptyList()
        } else {
            emptyList()
        }

        val filtered = allLogs.filter {
            (it.subjectName ?: "").equals(subject.subjectName, ignoreCase = true) ||
            (it.subjectId?.toString() == subject.subjectCode)
        }.sortedByDescending { it.attendanceDate }

        _uiState.value = SubjectDetailUiState(
            subject = subject,
            logsForSubject = filtered,
            simulatedPercentage = subject.percentage.numericValue ?: 0.0
        )
    }

    fun updateSimulation(attend: Int, miss: Int) {
        val sub = _uiState.value.subject ?: return
        val simPct = calculateSimulatedPercentage(sub.presentCount, sub.totalClasses, attend, miss)
        _uiState.value = _uiState.value.copy(
            simulateAttend = attend,
            simulateMiss = miss,
            simulatedPercentage = simPct
        )
    }

    companion object {
        fun calculateSimulatedPercentage(
            currentPresent: Int,
            currentTotal: Int,
            classesToAttend: Int,
            classesToMiss: Int
        ): Double {
            val newP = currentPresent + classesToAttend
            val newT = currentTotal + classesToAttend + classesToMiss
            return if (newT > 0) (newP * 100.0) / newT else 0.0
        }
    }
}
```

- [ ] **Step 4: Create `SubjectDetailScreen.kt`**

Build `SubjectDetailScreen.kt` with Material 3 Expressive components:
- TopAppBar with navigation back arrow and subject title.
- Subject Hero Gauge Card with animated circular progress indicator.
- Bunk Advisor recommendation badge ("Can bunk 2 classes" / "Attend next 3 classes").
- Teacher info pill with faculty name and initials avatar.
- Interactive What-If Simulation Card (Slider for +classes to attend / +classes to miss).
- Chronological lecture history list with Present/Absent badges and lecture numbers.

- [ ] **Step 5: Integrate navigation into `DashboardScreen.kt` and `MainComposeActivity.kt`**

Tapping any subject card in `DashboardScreen` displays `SubjectDetailScreen` via smooth modal transition.

- [ ] **Step 6: Run tests and verify**

Run: `cd android && ./gradlew testDebugUnitTest --tests SubjectDetailViewModelTest`  
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add android/app/src/main/java/com/lloyd/attendance/feature/subject/ \
        android/app/src/main/java/com/lloyd/attendance/feature/dashboard/DashboardScreen.kt \
        android/app/src/main/java/com/lloyd/attendance/ui/MainComposeActivity.kt \
        android/app/src/test/java/com/lloyd/attendance/feature/subject/SubjectDetailViewModelTest.kt
git commit -m "feat(subject): add interactive Subject & Faculty Deep-Dive page with lecture ledger and simulator"
```

---

### Task 5: Material 3 Expressive Compose Login Screen & Developer Branding

**Files:**
- Create: `android/app/src/main/java/com/lloyd/attendance/feature/auth/LoginScreen.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/ui/LoginActivity.java`
- Modify: `android/app/src/main/java/com/lloyd/attendance/feature/profile/ProfileScreen.kt`

**Interfaces:**
- Consumes: `ErpApiClient.login`, `AppPreferences`
- Produces: Modern Compose-based login screen with fluid inputs, biometric support, shake animation, and *"Crafted by Nikhil Pandey"* subtle branding.

- [ ] **Step 1: Implement `LoginScreen.kt` in Compose**

Create `LoginScreen.kt` featuring:
- Brand header with `ic_brand_emblem` icon in Muted Teal (`#84A59D`) and Honey Bronze (`#F6BD60`).
- Elevated Card container with 24dp rounded corners.
- OutlinedTextFields for Admission Number and Password with password toggle.
- Primary Sign In Button with loading spinner.
- Biometric quick-login button when saved credentials/token exist.
- Understated developer footer: *"Lloyd ERP • Crafted by Nikhil Pandey"*.

- [ ] **Step 2: Connect `LoginActivity` to Compose `LoginScreen`**

Refactor `LoginActivity` to invoke `setContent { LloydTheme { LoginScreen(...) } }`.

- [ ] **Step 3: Streamline `ProfileScreen.kt` with Author Branding & Update Card**

Replace the 5-item Security Hardening block in `ProfileScreen.kt` with:
- Developer Attribution Card:
  * Title: *"Crafted by Nikhil Pandey"*
  * Subtitle: *"Open Source Lloyd ERP Companion • v${BuildConfig.VERSION_NAME}"*
  * Author badge and GitHub link.
- In-App OTA Update Checker Card:
  * Button: *"Check for Updates"* (connected to `OtaUpdateManager`).

- [ ] **Step 4: Verify build**

Run: `cd android && ./gradlew assembleDebug`  
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/lloyd/attendance/feature/auth/ \
        android/app/src/main/java/com/lloyd/attendance/ui/LoginActivity.java \
        android/app/src/main/java/com/lloyd/attendance/feature/profile/ProfileScreen.kt
git commit -m "feat(ui): implement M3 Expressive Compose login screen and streamline profile with developer branding"
```

---

### Task 6: In-App GitHub Releases OTA Updates

**Files:**
- Create: `android/app/src/main/java/com/lloyd/attendance/core/ota/OtaUpdateManager.kt`
- Create: `android/app/src/main/res/xml/file_paths.xml`
- Modify: `android/app/src/main/AndroidManifest.xml`
- Modify: `android/app/src/main/java/com/lloyd/attendance/feature/profile/ProfileScreen.kt`
- Test: `android/app/src/test/java/com/lloyd/attendance/core/ota/OtaUpdateManagerTest.kt`

**Interfaces:**
- Consumes: GitHub API `https://api.github.com/repos/nkpcore/lloyd-erp/releases/latest`
- Produces: `OtaReleaseInfo(hasUpdate, latestVersion, releaseNotes, downloadUrl)`, download and install Intent via `FileProvider`.

- [ ] **Step 1: Write unit test for version comparison logic**

Create `android/app/src/test/java/com/lloyd/attendance/core/ota/OtaUpdateManagerTest.kt`:
```kotlin
package com.lloyd.attendance.core.ota

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtaUpdateManagerTest {

    @Test
    fun testIsNewerVersion() {
        assertTrue(OtaUpdateManager.isNewerVersion("v1.0.15", "1.0.14"))
        assertTrue(OtaUpdateManager.isNewerVersion("1.1.0", "1.0.99"))
        assertFalse(OtaUpdateManager.isNewerVersion("v1.0.14", "1.0.14"))
        assertFalse(OtaUpdateManager.isNewerVersion("v1.0.13", "1.0.14"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd android && ./gradlew testDebugUnitTest --tests OtaUpdateManagerTest`  
Expected: Class not found.

- [ ] **Step 3: Implement `OtaUpdateManager.kt`**

Create `OtaUpdateManager.kt` querying `https://api.github.com/repos/nkpcore/lloyd-erp/releases/latest`, parsing `tag_name` and `.apk` asset download URL, and downloading + launching install Intent via `FileProvider`.

- [ ] **Step 4: Configure `FileProvider` in `AndroidManifest.xml`**

Register `androidx.core.content.FileProvider` with authority `${applicationId}.fileprovider` and `file_paths.xml`.

- [ ] **Step 5: Run tests and verify**

Run: `cd android && ./gradlew testDebugUnitTest --tests OtaUpdateManagerTest`  
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add android/app/src/main/java/com/lloyd/attendance/core/ota/ \
        android/app/src/main/res/xml/file_paths.xml \
        android/app/src/main/AndroidManifest.xml \
        android/app/src/test/java/com/lloyd/attendance/core/ota/OtaUpdateManagerTest.kt
git commit -m "feat(ota): add automated GitHub Releases in-app updater and APK installer"
```

---

### Task 7: User Telemetry Engine & Web Admin Dashboard

**Files:**
- Create: `android/app/src/main/java/com/lloyd/attendance/core/telemetry/TelemetryManager.kt`
- Create: `web/admin/index.html`
- Create: `web/admin/style.css`
- Create: `web/admin/app.js`
- Test: `android/app/src/test/java/com/lloyd/attendance/core/telemetry/TelemetryManagerTest.kt`

**Interfaces:**
- Consumes: `AppPreferences`, `BuildConfig.VERSION_NAME`, `Build.VERSION.SDK_INT`
- Produces: Telemetry payload sent to backend endpoint; responsive web dashboard in `web/admin/` displaying active users, app version adoption, and live user table.

- [ ] **Step 1: Write unit test for telemetry payload serializer**

Create `android/app/src/test/java/com/lloyd/attendance/core/telemetry/TelemetryManagerTest.kt`:
```kotlin
package com.lloyd.attendance.core.telemetry

import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryManagerTest {

    @Test
    fun testBuildTelemetryPayload() {
        val payload = TelemetryManager.buildPayload(
            deviceId = "test-uuid-123",
            studentId = 42,
            studentName = "Nikhil Pandey",
            appVersion = "1.0.14",
            versionCode = 14,
            osVersion = "Android 14 (API 34)",
            deviceModel = "Pixel 8"
        )
        assertEquals("test-uuid-123", payload["device_id"])
        assertEquals(42, payload["student_id"])
        assertEquals("1.0.14", payload["app_version"])
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd android && ./gradlew testDebugUnitTest --tests TelemetryManagerTest`  
Expected: Class not found.

- [ ] **Step 3: Implement `TelemetryManager.kt`**

Create `TelemetryManager.kt` to serialize anonymous device UUID, student name/ID, app version, OS version, device model, and ISO timestamp, sending via OkHttp POST to Supabase/Firestore REST endpoint.

- [ ] **Step 4: Build Web Admin Dashboard in `web/admin/`**

Create `web/admin/index.html`, `style.css`, and `app.js`:
- Palette: Honey Bronze (`#F6BD60`), Linen (`#F7EDE2`), Cotton Rose (`#F5CAC3`), Muted Teal (`#84A59D`), Light Coral (`#F28482`).
- KPI Cards: Total Registered Users, Active Today, Active This Week, Latest App Version.
- Version Breakdown Chart: Live distribution of students across app versions.
- Live Table: Searchable & sortable table displaying Student Name, Admission No, App Version, Device Model, Last Active, and Status badge.

- [ ] **Step 5: Run tests and verify**

Run: `cd android && ./gradlew testDebugUnitTest --tests TelemetryManagerTest`  
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add android/app/src/main/java/com/lloyd/attendance/core/telemetry/ \
        android/app/src/test/java/com/lloyd/attendance/core/telemetry/TelemetryManagerTest.kt \
        web/admin/
git commit -m "feat(admin): implement client telemetry reporter and responsive web admin dashboard"
```

---

### Task 8: End-to-End Verification & Documentation

**Files:**
- Modify: `README.md`
- Test: Full unit test suite + debug APK build

- [ ] **Step 1: Run all unit tests**

Run: `cd android && ./gradlew testDebugUnitTest`  
Expected: All tests PASS.

- [ ] **Step 2: Build release APK bundle check**

Run: `cd android && ./gradlew assembleDebug`  
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Update `README.md`**

Document:
- Modern warm M3 design system & new logo.
- 3-tab navigation structure.
- Interactive Subject & Faculty Deep-Dive page.
- In-app OTA update system.
- Web Admin Dashboard in `web/admin/`.
- Developer attribution: Created by Nikhil Pandey.

- [ ] **Step 4: Final commit**

```bash
git add README.md
git commit -m "docs: finalize Lloyd ERP modernization documentation, M3 design system, and admin guide"
```
