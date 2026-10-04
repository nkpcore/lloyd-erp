# Lloyd ERP Kotlin & Jetpack Compose Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform `nkpcore/lloyd-erp` into a production-grade, real-data, offline-first student operating system powered by Kotlin and Jetpack Compose with Material Design 3 Expressive, targeting Android 16 (API 36).

**Architecture:** Unidirectional Data Flow (UDF) through Room cache-first repository layer with reactive Kotlin Flows feeding Compose ViewModels and pure mathematical domain engines. Security is hardened via Keystore-backed AES-256-GCM token storage with zero password persistence, BOLA identity locks, and sanitized telemetry.

**Tech Stack:** Kotlin 2.0.21/2.1+, AGP 8.9.x+, Gradle 8.11+, Jetpack Compose BOM 2026.09.00, Material 3 1.4.0, Material Symbols, Android Keystore, Room 2.6+, OkHttp 4.12+, WorkManager 2.9+, JUnit 4.

**Specs & Audits:**
- Master Design: [docs/superpowers/specs/2026-10-05-lloyd-erp-kotlin-compose-overhaul-design.md](file:///c:/Users/nikhil/Desktop/erpwidgit/docs/superpowers/specs/2026-10-05-lloyd-erp-kotlin-compose-overhaul-design.md)
- ERP Discovery Catalog: [docs/erp-discovery/ENDPOINTS.md](file:///c:/Users/nikhil/Desktop/erpwidgit/docs/erp-discovery/ENDPOINTS.md)
- Security Audit Findings: [docs/security-audit/FINDINGS.md](file:///c:/Users/nikhil/Desktop/erpwidgit/docs/security-audit/FINDINGS.md)
- BOLA Assessment: [docs/security-audit/BOLA_TESTING.md](file:///c:/Users/nikhil/Desktop/erpwidgit/docs/security-audit/BOLA_TESTING.md)
- Android Security Posture: [docs/security-audit/ANDROID_SECURITY.md](file:///c:/Users/nikhil/Desktop/erpwidgit/docs/security-audit/ANDROID_SECURITY.md)

## Global Constraints
- **Target SDK**: `compileSdk 36`, `targetSdk 36`, `minSdk 26`.
- **Zero Mock / Fake Data**: Empty data states must display neutral indicators (`--.-%`, "No attendance recorded yet", "Schedule unavailable"). Never default to 100% or Section A-1.
- **BOLA Prevention (FINDING-01)**: Client-side `student_id` passed to `/attendance/student` must strictly match the verified session student ID received from `/auth/login` or `/student/me/monthly-attendance`. Never construct, accept, or serialize arbitrary IDs.
- **Zero Plaintext Password Storage (FINDING-02)**: Passwords must never be saved to disk, SharedPreferences, or local databases.
- **Zero Unencrypted Fallback (FINDING-02)**: If Android Keystore initialization fails, the app must abort securely with an explicit error dialog—never silently falling back to unencrypted `MODE_PRIVATE` storage on disk.
- **Telemetry Sanitization (FINDING-03)**: Zero raw JSON or PII logging in production. Strip all `ERP_RAW` logs via ProGuard `-assumenosideeffects`.
- **Broadcast Receiver Hardening (FINDING-04)**: `AttendanceWidgetProvider` must enforce internal intent validation or signature permissions.
- **Release Minification (FINDING-05)**: Set `minifyEnabled true` in `buildTypes.release` with ProGuard rules.
- **Test Framework**: JUnit 4 (`junit:junit:4.13.2`) with `kotlinx-coroutines-test:1.8.1`.
- **Icons**: Google Material Symbols vector assets only. Zero emojis in UI or notifications.
- **Dual-Compilation Safety**: Keep existing code compiling while introducing new Kotlin and Compose components side-by-side.

---

### Task 1: Build Toolchain Baseline, Minification & Android 16 Alignment

**Files:**
- Modify: `android/build.gradle`
- Modify: `android/app/build.gradle`
- Modify: `android/app/proguard-rules.pro`
- Modify: `android/gradle/wrapper/gradle-wrapper.properties`

**Interfaces:**
- Consumes: Existing Gradle configuration
- Produces: Compiling Android 16 (API 36) build environment with Kotlin 2.x, Compose Compiler plugin, and ProGuard log-stripping enabled.

- [ ] **Step 1: Update Gradle wrapper to Gradle 8.11.1**

Update `android/gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 2: Update root `android/build.gradle` with AGP and Kotlin plugins**

```groovy
plugins {
    id 'com.android.application' version '8.9.0' apply false
    id 'org.jetbrains.kotlin.android' version '2.0.21' apply false
    id 'org.jetbrains.kotlin.plugin.compose' version '2.0.21' apply false
}
```

- [ ] **Step 3: Update `android/app/build.gradle` with release minification and SDK 36**

```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'org.jetbrains.kotlin.plugin.compose'
}

android {
    namespace 'com.lloyd.attendance'
    compileSdk 36

    defaultConfig {
        applicationId "com.lloyd.attendance"
        minSdk 26
        targetSdk 36
        versionCode 1
        versionName "1.0.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose true
    }

    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        debug {
            minifyEnabled false
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = '17'
    }
}

dependencies {
    implementation platform('androidx.compose:compose-bom:2026.09.00')
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.ui:ui-graphics'
    implementation 'androidx.compose.ui:ui-tooling-preview'
    implementation 'androidx.compose.material3:material3'
    implementation 'androidx.activity:activity-compose:1.9.3'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6'
    implementation 'androidx.lifecycle:lifecycle-runtime-compose:2.8.6'
    
    // Core Android & Coroutines
    implementation 'androidx.core:core-ktx:1.15.0'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1'

    // Existing Libraries
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.google.code.gson:gson:2.11.0'
    implementation 'androidx.work:work-runtime:2.9.1'

    // Testing
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1'
    androidTestImplementation platform('androidx.compose:compose-bom:2026.09.00')
    androidTestImplementation 'androidx.compose.ui:ui-test-junit4'
}
```

- [ ] **Step 4: Configure ProGuard rules to strip Logcat PII in release builds**

Add to `android/app/proguard-rules.pro`:
```proguard
# Strip sensitive debug logging in release builds (Mitigating FINDING-03)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Preserve Room, Gson and DTO models
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.lloyd.attendance.api.Models$* { *; }
-keep class com.lloyd.attendance.domain.** { *; }
```

- [ ] **Step 5: Verify build compiles cleanly**

Run in powershell:
```powershell
cd c:\Users\nikhil\Desktop\erpwidgit\android
.\gradlew.bat compileDebugJavaWithJavac compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit build toolchain updates**

```bash
git add android/build.gradle android/app/build.gradle android/app/proguard-rules.pro android/gradle/wrapper/gradle-wrapper.properties
git commit -m "build: align toolchain with AGP 8.9, Kotlin 2.0.21, Compose BOM 2026.09.00, SDK 36 and ProGuard log stripping"
```

---

### Task 2: Hardware Keystore `SecureTokenStore`, BOLA Guard & Credential Purge

**Files:**
- Create: `android/app/src/main/java/com/lloyd/attendance/core/security/SecureTokenStore.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/core/security/KeystoreTokenStore.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/data/AppPreferences.java`
- Modify: `android/app/src/main/java/com/lloyd/attendance/api/ErpApiClient.java`
- Test: `android/app/src/test/java/com/lloyd/attendance/core/security/SecureTokenStoreTest.kt`

**Interfaces:**
- Produces: `SecureTokenStore` delivering hardware-backed encrypted token lifecycle and BOLA identity lock.
- Consumes: Android Keystore (`AndroidKeyStore`, `AES/GCM/NoPadding`).

- [ ] **Step 1: Define `SecureTokenStore` interface with BOLA student ID verification**

Create `android/app/src/main/java/com/lloyd/attendance/core/security/SecureTokenStore.kt`:
```kotlin
package com.lloyd.attendance.core.security

interface SecureTokenStore {
    fun getAccessToken(): String?
    fun setAccessToken(token: String?)
    fun getRefreshToken(): String?
    fun setRefreshToken(token: String?)
    fun getVerifiedStudentId(): Int
    fun setVerifiedStudentId(id: Int)
    fun clearTokens()
    fun hasValidRefreshToken(): Boolean
}
```

- [ ] **Step 2: Implement `KeystoreTokenStore.kt` without unencrypted fallbacks**

Create `android/app/src/main/java/com/lloyd/attendance/core/security/KeystoreTokenStore.kt`:
```kotlin
package com.lloyd.attendance.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreTokenStore(private val context: Context) : SecureTokenStore {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val sharedPrefs = context.getSharedPreferences("lloyd_secure_vault", Context.MODE_PRIVATE)

    @Volatile
    private var cachedAccessToken: String? = null

    init {
        getOrCreateKey()
    }

    private fun getOrCreateKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) return existingKey.secretKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    override fun getAccessToken(): String? = cachedAccessToken

    override fun setAccessToken(token: String?) {
        cachedAccessToken = token
    }

    override fun getRefreshToken(): String? {
        val encrypted = sharedPrefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val iv = sharedPrefs.getString(KEY_IV, null) ?: return null
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
            val decoded = cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP))
            String(decoded, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    override fun setRefreshToken(token: String?) {
        if (token.isNullOrEmpty()) {
            sharedPrefs.edit().remove(KEY_REFRESH_TOKEN).remove(KEY_IV).apply()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))

        sharedPrefs.edit()
            .putString(KEY_REFRESH_TOKEN, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
    }

    override fun getVerifiedStudentId(): Int = sharedPrefs.getInt(KEY_VERIFIED_STUDENT_ID, 0)

    override fun setVerifiedStudentId(id: Int) {
        sharedPrefs.edit().putInt(KEY_VERIFIED_STUDENT_ID, id).apply()
    }

    override fun clearTokens() {
        cachedAccessToken = null
        sharedPrefs.edit().remove(KEY_REFRESH_TOKEN).remove(KEY_IV).remove(KEY_VERIFIED_STUDENT_ID).apply()
    }

    override fun hasValidRefreshToken(): Boolean = !getRefreshToken().isNullOrEmpty()

    companion object {
        private const val KEY_ALIAS = "lloyd_erp_auth_token_key"
        private const val KEY_REFRESH_TOKEN = "enc_r_token"
        private const val KEY_IV = "enc_iv"
        private const val KEY_VERIFIED_STUDENT_ID = "verified_student_id"
    }
}
```

- [ ] **Step 3: Purge password storage and unencrypted fallback from `AppPreferences.java`**

Remove `KEY_PASSWORD`, `saveCredentials(username, password)`, and `getPassword()`.
Remove fallback to `MODE_PRIVATE` unencrypted XML on disk.

- [ ] **Step 4: Update `ErpApiClient.java` with BOLA lock and log sanitization**

1. In `ensureValidToken()`, rely exclusively on `refreshToken()`.
2. In `getAttendanceLogs(int studentId, ...)`, enforce:
   ```java
   int verifiedId = prefs.getStudentId();
   if (studentId <= 0 || (verifiedId > 0 && studentId != verifiedId)) {
       throw new SecurityException("Unauthorized: Attempted to query attendance for unverified student ID.");
   }
   ```
3. Strip all `android.util.Log.i("ERP_RAW", ...)` calls.

- [ ] **Step 5: Verify build compiles cleanly**

Run:
```powershell
.\gradlew.bat compileDebugJavaWithJavac compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit security hardening**

```bash
git add android/app/src/main/java/com/lloyd/attendance/core/security/ android/app/src/main/java/com/lloyd/attendance/data/AppPreferences.java android/app/src/main/java/com/lloyd/attendance/api/ErpApiClient.java
git commit -m "security: introduce KeystoreTokenStore, enforce BOLA identity verification and purge Logcat telemetry"
```

---

### Task 3: Pure Kotlin Domain Engine & Discrete Boundary Tests

**Files:**
- Create: `android/app/src/main/java/com/lloyd/attendance/domain/attendance/AttendanceModels.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/domain/attendance/AttendanceCalculator.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/domain/attendance/BunkAdvisor.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/domain/planner/SimulationEngine.kt`
- Create: `android/app/src/test/java/com/lloyd/attendance/domain/attendance/AttendanceCalculatorTest.kt`

**Interfaces:**
- Produces: `AttendanceSnapshot`, `AttendanceThreshold`, `AttendanceThresholdAnalysis`, `AttendanceCalculator`, `BunkAdvisor`, `SimulationEngine`.

- [ ] **Step 1: Write failing unit tests for discrete boundaries**

Create `android/app/src/test/java/com/lloyd/attendance/domain/attendance/AttendanceCalculatorTest.kt`:
```kotlin
package com.lloyd.attendance.domain.attendance

import org.junit.Assert.*
import org.junit.Test

class AttendanceCalculatorTest {

    @Test
    fun testEmptyDataYieldsNoData() {
        val result = AttendanceCalculator.calculate(present = 0, total = 0)
        assertEquals(AttendanceSnapshot.NoData, result)
    }

    @Test
    fun testZeroPresentOneTotal() {
        val result = AttendanceCalculator.calculate(present = 0, total = 1)
        assertTrue(result is AttendanceSnapshot.Recorded)
        assertEquals(0.0, (result as AttendanceSnapshot.Recorded).percentage, 0.01)
    }

    @Test
    fun testPerfectAttendance() {
        val result = AttendanceCalculator.calculate(present = 1, total = 1)
        assertTrue(result is AttendanceSnapshot.Recorded)
        assertEquals(100.0, (result as AttendanceSnapshot.Recorded).percentage, 0.01)
    }

    @Test
    fun testBoundary74Outof100() {
        val analysis = AttendanceCalculator.analyzeThreshold(present = 74, total = 100, threshold = AttendanceThreshold(75.0))
        assertFalse(analysis.isAboveTarget)
        assertEquals(0, analysis.bunkAllowance)
        assertEquals(4, analysis.recoveryRequired)
    }

    @Test
    fun testBoundary75Outof100ExactThreshold() {
        val analysis = AttendanceCalculator.analyzeThreshold(present = 75, total = 100, threshold = AttendanceThreshold(75.0))
        assertTrue(analysis.isAboveTarget)
        assertEquals(0, analysis.bunkAllowance)
        assertEquals(0, analysis.recoveryRequired)
    }

    @Test
    fun testThreeOutOfFourExactThreshold() {
        val analysis = AttendanceCalculator.analyzeThreshold(present = 3, total = 4, threshold = AttendanceThreshold(75.0))
        assertTrue(analysis.isAboveTarget)
        assertEquals(0, analysis.bunkAllowance)
        assertEquals(0, analysis.recoveryRequired)
    }

    @Test
    fun test100OutOf100BunkAllowanceInvariant() {
        val analysis = AttendanceCalculator.analyzeThreshold(present = 100, total = 100, threshold = AttendanceThreshold(75.0))
        assertTrue(analysis.isAboveTarget)
        assertEquals(33, analysis.bunkAllowance)
        val projected = (100.0 / 133.0) * 100.0
        assertTrue("Projected attendance must be >= 75.0%", projected >= 75.0)
    }

    @Test
    fun testMultiTargetThresholds() {
        val at80 = AttendanceCalculator.analyzeThreshold(present = 80, total = 100, threshold = AttendanceThreshold(80.0))
        assertTrue(at80.isAboveTarget)
        assertEquals(0, at80.bunkAllowance)

        val at90 = AttendanceCalculator.analyzeThreshold(present = 80, total = 100, threshold = AttendanceThreshold(90.0))
        assertFalse(at90.isAboveTarget)
        assertEquals(100, at90.recoveryRequired)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests com.lloyd.attendance.domain.attendance.AttendanceCalculatorTest`
Expected: FAIL (unresolved classes).

- [ ] **Step 3: Implement domain models, calculator, advisor and simulation engine**

Implement `AttendanceModels.kt`, `AttendanceCalculator.kt`, `BunkAdvisor.kt`, and `SimulationEngine.kt`.

- [ ] **Step 4: Run unit tests to verify they pass**

Run: `.\gradlew.bat testDebugUnitTest --tests com.lloyd.attendance.domain.attendance.AttendanceCalculatorTest`
Expected: `BUILD SUCCESSFUL`, all tests PASS.

- [ ] **Step 5: Commit domain mathematics engine**

```bash
git add android/app/src/main/java/com/lloyd/attendance/domain/ android/app/src/test/java/com/lloyd/attendance/domain/
git commit -m "feat(domain): implement AttendanceCalculator, BunkAdvisor, and SimulationEngine with discrete boundary tests"
```

---

### Task 4: Dynamic Timetable Engine & Purge Hardcoded Section A-1

**Files:**
- Create: `android/app/src/main/java/com/lloyd/attendance/domain/timetable/TimetableModels.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/data/repository/TimetableRepository.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/data/repository/TimetableRepositoryImpl.kt`
- Modify: `android/app/src/main/java/com/lloyd/attendance/schedule/TimetableRepository.java` (deprecate)

**Interfaces:**
- Produces: `TimetableRepository` delivering dynamic routines mapped from `EP-04 (/student/me/weekly-attendance)`.
- Consumes: Real ERP weekly payload schema (`days`, `periods`, `start_time`, `end_time`, `room_no`, `teacher_name`).

- [ ] **Step 1: Define `TimetableModels.kt`**

Model `RoutinePeriod` and `DayScheduleResult` (`Unavailable`, `Holiday`, `Weekend`, `Scheduled`).

- [ ] **Step 2: Implement dynamic `TimetableRepositoryImpl.kt`**

Enforce rule: If `studentSection.isNullOrBlank()`, emit `DayScheduleResult.Unavailable` immediately. Never default to A-1.

- [ ] **Step 3: Deprecate `TimetableRepository.java`**

Mark legacy Java repository as `@Deprecated` and remove hardcoded section fallbacks.

- [ ] **Step 4: Verify compilation**

Run: `.\gradlew.bat compileDebugJavaWithJavac compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit dynamic timetable engine**

```bash
git add android/app/src/main/java/com/lloyd/attendance/domain/timetable/ android/app/src/main/java/com/lloyd/attendance/data/repository/ android/app/src/main/java/com/lloyd/attendance/schedule/TimetableRepository.java
git commit -m "feat(timetable): introduce dynamic TimetableRepository and eliminate hardcoded Section A-1 fallback"
```

---

### Task 5: Core Design System & Telemetry Components (`core/designsystem/`)

**Files:**
- Create: `android/app/src/main/java/com/lloyd/attendance/core/designsystem/theme/Color.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/core/designsystem/theme/Typography.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/core/designsystem/theme/Shapes.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/core/designsystem/theme/Spacing.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/core/designsystem/theme/Motion.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/core/designsystem/theme/LloydTheme.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/core/designsystem/components/StatusBadge.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/core/designsystem/components/Skeleton.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/core/designsystem/components/AttendanceCard.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/core/designsystem/components/OfflineState.kt`

- [ ] **Step 1: Implement Theme Tokens (`Color.kt`, `Spacing.kt`, `Shapes.kt`, `Typography.kt`, `LloydTheme.kt`)**

- [ ] **Step 2: Implement `StatusBadge.kt`, `Skeleton.kt` and `OfflineState.kt`**

Build `OfflineState.kt` chip supporting `LIVE`, `SYNCING`, `CACHED`, `OFFLINE` status per `SYNC_STRATEGY.md`.

- [ ] **Step 3: Implement `AttendanceCard.kt`**

- [ ] **Step 4: Verify compilation**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit design system foundation**

```bash
git add android/app/src/main/java/com/lloyd/attendance/core/designsystem/
git commit -m "feat(ui): implement core/designsystem with M3 Expressive tokens and telemetry indicators"
```

---

### Task 6: Phased Screen Migration: Dashboard $\to$ Compose

**Files:**
- Create: `android/app/src/main/java/com/lloyd/attendance/feature/dashboard/DashboardUiState.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/feature/dashboard/DashboardViewModel.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/feature/dashboard/DashboardScreen.kt`

- [ ] **Step 1: Create `DashboardUiState.kt` and `DashboardViewModel.kt`**

- [ ] **Step 2: Build `DashboardScreen.kt` in Compose**

Assemble screen using `AttendanceCard`, hero active period card, and `OfflineState` telemetry banner.

- [ ] **Step 3: Verify compilation**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit Dashboard Compose screen**

```bash
git add android/app/src/main/java/com/lloyd/attendance/feature/dashboard/
git commit -m "feat(dashboard): migrate dashboard to Jetpack Compose with M3 Expressive components"
```

---

### Task 7: Phased Screen Migration: Attendance History & Reactive Search $\to$ Compose

**Files:**
- Create: `android/app/src/main/java/com/lloyd/attendance/feature/attendance/AttendanceUiState.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/feature/attendance/AttendanceViewModel.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/feature/attendance/AttendanceScreen.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/feature/attendance/SubjectDetailBottomSheet.kt`

- [ ] **Step 1: Implement `AttendanceViewModel.kt` with Room Flow search**

Filter records reactively by search query, subject, faculty, and month.

- [ ] **Step 2: Build `AttendanceScreen.kt` and `SubjectDetailBottomSheet.kt`**

- [ ] **Step 3: Verify compilation**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit Attendance Compose screen**

```bash
git add android/app/src/main/java/com/lloyd/attendance/feature/attendance/
git commit -m "feat(attendance): migrate attendance history and search to Jetpack Compose"
```

---

### Task 8: Phased Screen Migration: Schedule, Planner & Settings $\to$ Compose

**Files:**
- Create: `android/app/src/main/java/com/lloyd/attendance/feature/schedule/ScheduleScreen.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/feature/planner/PlannerScreen.kt`
- Create: `android/app/src/main/java/com/lloyd/attendance/feature/profile/ProfileScreen.kt`

- [ ] **Step 1: Build `ScheduleScreen.kt`**

- [ ] **Step 2: Build `PlannerScreen.kt` with multi-target selector (75%, 80%, 85%, 90%)**

- [ ] **Step 3: Build `ProfileScreen.kt` with trustworthy report export**

- [ ] **Step 4: Verify compilation**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit remaining Compose feature screens**

```bash
git add android/app/src/main/java/com/lloyd/attendance/feature/schedule/ android/app/src/main/java/com/lloyd/attendance/feature/planner/ android/app/src/main/java/com/lloyd/attendance/feature/profile/
git commit -m "feat(ui): complete Compose migration for Schedule, Planner, and Profile screens"
```

---

### Task 9: Deprecate Legacy Java UI & Migrate `MainActivity` to Compose Host

**Files:**
- Modify: `android/app/src/main/java/com/lloyd/attendance/ui/MainActivity.java` $\to$ `MainActivity.kt`
- Delete: `android/app/src/main/res/layout/activity_main.xml`
- Delete: `android/app/src/main/res/layout/dialog_subject_details.xml`

- [ ] **Step 1: Convert `MainActivity` to `ComponentActivity` using `setContent { LloydApp() }`**

- [ ] **Step 2: Remove obsolete XML layouts**

- [ ] **Step 3: Verify clean build without legacy XML UI**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit legacy UI deprecation**

```bash
git add android/app/src/main/java/com/lloyd/attendance/ui/ android/app/src/main/res/layout/
git commit -m "refactor(ui): transition MainActivity to pure Compose host and purge legacy XML layouts"
```

---

### Task 10: Android 16 Behavior Hardening, Security Hardening & Quality Gates

**Files:**
- Create: `android/app/src/androidTest/java/com/lloyd/attendance/DashboardScreenTest.kt`
- Create: `android/app/src/androidTest/java/com/lloyd/attendance/AttendanceScreenTest.kt`
- Modify: `android/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Write Compose UI tests for Dashboard and Attendance**

- [ ] **Step 2: Enforce Android 16 edge-to-edge insets in Compose**

- [ ] **Step 3: Harden `AttendanceWidgetProvider` in `AndroidManifest.xml` (Mitigating FINDING-04)**

Restrict broadcast receiver intent handling to internal app intents or verify calling identity.

- [ ] **Step 4: Execute full verification gate**

Run:
```powershell
.\gradlew.bat lintDebug testDebugUnitTest assembleDebug
```
Expected: All checks PASS with zero errors.

- [ ] **Step 5: Commit Android 16 hardening & tests**

```bash
git add android/app/src/androidTest/ android/app/src/main/AndroidManifest.xml
git commit -m "test: add Compose UI tests, Android 16 edge-to-edge verification, receiver hardening and CI quality gates"
```
