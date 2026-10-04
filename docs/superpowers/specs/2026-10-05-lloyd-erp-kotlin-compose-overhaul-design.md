# Lloyd ERP — Production Kotlin & Jetpack Compose Overhaul Design

**Document ID**: `2026-10-05-lloyd-erp-kotlin-compose-overhaul-design`  
**Date**: October 5, 2026  
**Status**: APPROVED  
**Target Environment**: Android 16 (API 36), minSdk 26, JVM 17  
**Author**: Antigravity Pairing Agent & Human Engineering Lead

---

## 1. Executive Summary & Goals

This specification details the transformation of `nkpcore/lloyd-erp` from a legacy Java/XML Android application with static timetable fallbacks and plaintext credential caching into an enterprise-grade, offline-first, real-data student operating system.

The overhaul achieves:
1. **Zero-Mock & Zero-Fake Data Policy**: Eliminates all hardcoded student profiles (e.g. `28960`), hardcoded timetables, and fake 100% attendance calculations.
2. **Modern Build Chain**: Aligns Kotlin 2.x, AGP 8.9.x+, official Kotlin Compose Compiler plugin (`org.jetbrains.kotlin.plugin.compose`), and Compose BOM `2026.09.00` targeting Android 16 (API 36).
3. **Decoupled Keystore Security**: Replaces deprecated `EncryptedSharedPreferences` with a hardware-backed `SecureTokenStore` (AES-256-GCM). Passwords are never persisted.
4. **Pure Mathematical Domain Engine**: Decouples mathematical attendance calculation from human advisory policies across configurable targets ($75\%$, $80\%$, $85\%$, $90\%$).
5. **Dynamic Timetable Engine**: Replaces hardcoded Section A-1 data in `TimetableRepository.java` with a Room-backed, dynamic routine mapper consuming ERP `/student/me/weekly-attendance`. If no section is found, the UI renders `"Schedule unavailable"`—never defaulting to A-1.
6. **Design System First**: Implements `core/designsystem/` with Material Design 3 Expressive tokens, Material Symbols, and neutral skeleton indicators before migrating screens.
7. **Phased Compose Migration**: Migrates screens iteratively from Java/XML to pure Jetpack Compose, culminating in the removal of legacy XML layouts and Java UI controllers.

---

## 2. High-Level Architecture & Layer Boundaries

```
com.lloyd.attendance/
├── core/
│   ├── common/                  // Result, NetworkMonitor, DateTimeUtils
│   ├── designsystem/            // LloydTheme, Color, Typography, Shapes, Spacing, Motion, Components
│   ├── security/                // SecureTokenStore (Keystore AES-256-GCM)
│   ├── database/                // Room Database, TypeConverters, Migration specs
│   └── datastore/               // User preferences, App state
├── data/
│   ├── remote/
│   │   ├── api/                 // Retrofit/OkHttp ERP service interfaces
│   │   └── dto/                 // Raw ERP DTOs (MonthlyAttendanceDto, WeeklyRoutineDto)
│   ├── local/
│   │   ├── dao/                 // AttendanceDao, RoutineDao, ProfileDao
│   │   └── entity/              // AttendanceRecordEntity, RoutinePeriodEntity
│   ├── mapper/                  // DTO <-> Domain <-> Entity bidirectional mappers
│   └── repository/              // AttendanceRepositoryImpl, TimetableRepositoryImpl, AuthRepositoryImpl
├── domain/
│   ├── attendance/              // AttendanceSnapshot, AttendanceThreshold, AttendanceCalculator, BunkAdvisor
│   ├── timetable/               // RoutinePeriod, DaySchedule, ScheduleEngine
│   ├── student/                 // StudentProfile, Section
│   └── planner/                 // SimulationEngine, TargetProjection
├── feature/
│   ├── auth/                    // LoginScreen.kt, LoginViewModel.kt
│   ├── dashboard/               // DashboardScreen.kt, DashboardViewModel.kt
│   ├── attendance/              // AttendanceScreen.kt, AttendanceViewModel.kt (Room Flow Search)
│   ├── schedule/                // ScheduleScreen.kt, ScheduleViewModel.kt
│   ├── planner/                 // PlannerScreen.kt, PlannerViewModel.kt (Multi-target simulation)
│   └── profile/                 // ProfileScreen.kt, ProfileViewModel.kt
└── widget/                      // AttendanceWidgetProvider, Glance/RemoteViews, SyncWorker
```

### Unidirectional Data Flow (UDF)
$$\text{ERP API} \xrightarrow{\text{OkHttp}} \text{DTO} \xrightarrow{\text{Mapper}} \text{Room DB} \xrightarrow{\text{DAO Flow}} \text{Repository} \xrightarrow{\text{Use Case}} \text{ViewModel (StateFlow)} \xrightarrow{\text{Compose}} \text{M3 UI}$$

---

## 3. Build & Toolchain Specifications

### 3.1 Compatibility Matrix
To fully support **Android 16 (API 36)** in compliance with Google Play requirements without experimental instability:
- **Android Gradle Plugin (AGP)**: `8.9.x` (or latest stable supporting API 36).
- **Gradle Wrapper**: `8.11+`.
- **Kotlin Gradle Plugin (KGP)**: Kotlin `2.0.21` / `2.1.x` matching official AGP 8.9.x matrix.
- **Kotlin Compose Compiler Plugin**: `org.jetbrains.kotlin.plugin.compose` applied in module `build.gradle`. (Eliminates legacy `composeOptions.kotlinCompilerExtensionVersion`).
- **Compose BOM**: `androidx.compose:compose-bom:2026.09.00`
- **Material 3**: `androidx.compose.material3:material3` (stable `1.4.0`).
- **Icons**: Google Material Symbols vector drawables.
- **Java / JVM Target**: Java 17 (`JavaVersion.VERSION_17`, `jvmTarget = "17"`).
- **SDK Targets**: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.

### 3.2 Testing Framework
- **Unit Testing**: **JUnit 4** (`junit:junit:4.13.2`) with `kotlinx-coroutines-test:1.8.1`.
- **Compose UI Testing**: `androidx.compose.ui:ui-test-junit4` in `androidTest/`.

---

## 4. Security & Credential Architecture

### 4.1 Threat Model & Deficiencies Addressed
- **Legacy Issue**: `AppPreferences.java` stored plaintext user passwords to execute automatic background re-login and fell back to unencrypted SharedPreferences if Keystore initialisation threw an exception.
- **Resolution**:
  1. Passwords are **never** persisted to disk or SharedPreferences under any circumstance.
  2. Deprecated `EncryptedSharedPreferences` and `MasterKey` are replaced by an explicit, hardware-backed `SecureTokenStore`.
  3. Tokens are encrypted using AES-256-GCM cipher with non-exportable Keystore keys.
  4. Active `access_token` resides in memory cache.
  5. `refresh_token` stored in Keystore-backed encrypted storage.
  6. Refresh lifecycle respects real ERP `/auth/refresh` HTTP semantics. If the refresh token expires or is rejected (HTTP 401), local tokens are wiped and the user is prompted to authenticate—no raw credentials are replayed.

### 4.2 Security Interface
```kotlin
package com.lloyd.attendance.core.security

interface SecureTokenStore {
    fun getAccessToken(): String?
    fun setAccessToken(token: String?)
    fun getRefreshToken(): String?
    fun setRefreshToken(token: String?)
    fun clearTokens()
    fun hasValidRefreshToken(): Boolean
}
```

---

## 5. Domain Mathematics & Multi-Target Engine

### 5.1 Separation of Mathematics from Advisory Policy

```kotlin
package com.lloyd.attendance.domain.attendance

sealed interface AttendanceSnapshot {
    data object NoData : AttendanceSnapshot
    data class Recorded(
        val present: Int,
        val total: Int,
        val percentage: Double
    ) : AttendanceSnapshot
}

data class AttendanceThreshold(val percentage: Double = 75.0) {
    init {
        require(percentage in 1.0..100.0) { "Target percentage must be between 1.0 and 100.0" }
    }
}

data class AttendanceThresholdAnalysis(
    val threshold: AttendanceThreshold,
    val isAboveTarget: Boolean,
    val bunkAllowance: Int,
    val recoveryRequired: Int
)
```

### 5.2 Pure Mathematics (`AttendanceCalculator.kt`)
1. **Empty Data Law**:
   $$\text{total} == 0 \implies \text{AttendanceSnapshot.NoData}$$
   *(UI renders neutral indicators: `--.-%`, "No attendance recorded yet"; never $100\%$)*.
2. **Attendance Percentage**:
   $$P = \text{round}\left(\frac{\text{present}}{\text{total}} \times 1000.0\right) / 10.0$$
3. **Threshold Analysis**:
   Given target $T = \frac{\text{threshold.percentage}}{100}$:
   - **Bunk Allowance ($B$)**:
     $$B = \max\left(0, \left\lfloor \frac{\text{present} - T \times \text{total}}{T} \right\rfloor\right)$$
   - **Recovery Classes ($R$)**:
     $$R = \max\left(0, \left\lceil \frac{T \times \text{total} - \text{present}}{1 - T} \right\rceil\right)$$
4. **Mathematical Invariant Assertion**:
   For any calculated bunk allowance $B$, the projected percentage must satisfy:
   $$\frac{\text{present}}{\text{total} + B} \ge T$$
   Bunk and recovery calculations are independently verified against the same threshold and tested at boundary conditions.

### 5.3 Multi-Target Simulator (`SimulationEngine.kt`)
The engine evaluates any discrete combination of future attended ($a$) or missed ($m$) classes across multiple targets ($75\%$, $80\%$, $85\%$, $90\%$):
$$P_{\text{projected}} = \frac{\text{present} + a}{\text{total} + a + m} \times 100$$

### 5.4 Human-Facing Policy (`BunkAdvisor.kt`)
- If `AttendanceSnapshot.NoData` $\implies$ `"No attendance recorded yet"`.
- If `isAboveTarget` with $B > 0 \implies$ `"Safe! Can bunk $B classes while maintaining ${threshold.percentage}%"`.
- If `isAboveTarget` with $B == 0 \implies$ `"On the edge! Cannot miss any class"`.
- If not `isAboveTarget` $\implies$ `"Shortage! Must attend $R consecutive classes to reach ${threshold.percentage}%"`.

### 5.5 Discrete Boundary Unit Tests (`AttendanceCalculatorTest.kt`)
- $0/0 \to \text{NoData}$
- $0/1 \to 0.0\%$ (Shortage, $R = 3$ for $75\%$)
- $1/1 \to 100.0\%$ (Safe, $B = 0$ for $75\%$)
- $74/100 \to 74.0\%$ (Shortage, $R = 4$ for $75\%$)
- $74/99 \approx 74.7\%$ (Shortage, $R = 1$ for $75\%$)
- $75/100 = 75.0\%$ (Safe, $B = 0, R = 0$)
- $3/4 = 75.0\%$ (Safe, $B = 0, R = 0$)
- $14/19 \approx 73.68\%$ (Shortage, $R = 1$ for $75\%$)
- $15/20 = 75.0\%$ (Safe, $B = 0, R = 0$)
- $100/100 = 100.0\%$ (Safe, $B = 33$ for $75\%$)
- $149/200 = 74.5\%$ (Shortage, $R = 2$ for $75\%$)
- $150/200 = 75.0\%$ (Safe, $B = 0, R = 0$)
- Multi-target verification at $80\%$, $85\%$, $90\%$.

---

## 6. Dynamic Timetable Subsystem & Schedule Engine

### 6.1 Replacement of Hardcoded Section A-1
- Existing `TimetableRepository.java` containing hardcoded Section A-1 strings (e.g. NB-101, Dr. Vivek Das, AAS102) is deprecated and replaced by `TimetableRepository.kt`.
- Dynamic schedule fetched from `/student/me/weekly-attendance`.
- **Strict Section Rule**:
  - Section provided in student profile $\implies$ load matching schedule routine from Room.
  - Section missing or empty $\implies$ display `"Schedule unavailable"` (never default to Section A-1).

### 6.2 Schedule Edge Cases Handled
- **Holidays & Weekends**: Render dedicated weekend/holiday empty state card; no synthetic classes.
- **Cancelled / Unmarked Periods**: Displayed with status `"Cancelled"` or `"Unmarked"` per ERP payload.
- **Changed Routines**: Room database invalidates stale weekly schedules when week identifier advances.
- **Current/Next Class Resolution**: Computed dynamically against device clock with remaining minutes countdown.

---

## 7. Design System First (`core/designsystem/`)

Before migrating application screens, the design system is established:
- **`LloydTheme.kt`**: Dark/Light tonal palettes adhering to M3 Expressive color harmony.
- **`Color.kt`**: Semantic tokens (`attendanceSafe`, `attendanceWarning`, `attendanceDanger`, `surfaceContainer`, `surfaceContainerHigh`).
- **`Typography.kt`**: Outfit/Inter typographic hierarchy.
- **`Spacing.kt`**: 4dp / 8dp rhythmic tokens.
- **`Motion.kt`**: Spring-based physics transitions for cards and sheets.
- **Components**:
  - `AttendanceCard`: Donut/circular progress indicator with large numerals and neutral skeleton states.
  - `ScheduleCard`: Hero card for active/upcoming class with countdown.
  - `SubjectCard`: Individual course summary with tap-to-inspect trigger.
  - `StatusBadge`: Semantic pill badge (Safe, Shortage, Neutral).
  - `Timeline`: Chronological class attendance ledger.
  - `Skeleton`: Shimmering placeholder during network fetch (`--.-%`).
  - `EmptyState`: Contextual illustration and messaging.
  - `OfflineState`: Subtle status bar chip indicating live vs cached data.

---

## 8. Phased Jetpack Compose Migration Strategy

To avoid breaking working features during development, the UI migration proceeds screen-by-screen:

```
[Phase 1] Toolchain, Build Matrix & Keystore Security Baseline
    ↓
[Phase 2] core/designsystem/ Tokens & Reusable M3 Components
    ↓
[Phase 3] Domain Mathematics Engine, Multi-Target Planner & Room Database
    ↓
[Phase 4] Dynamic Timetable Routine Mapper & Repository (Retire hardcoded A-1)
    ↓
[Phase 5] Dashboard Screen -> Jetpack Compose
    ↓
[Phase 6] Attendance History & Flow Search -> Jetpack Compose
    ↓
[Phase 7] Schedule Screen -> Jetpack Compose
    ↓
[Phase 8] Planner & Multi-Target Simulator -> Jetpack Compose
    ↓
[Phase 9] Profile & Settings Screen -> Jetpack Compose
    ↓
[Phase 10] AppWidget & OS Notification Hardening
    ↓
[Phase 11] Deprecate & Purge Legacy Java/XML UI Controllers & Layouts
    ↓
[Phase 12] Android 16 Behavior Hardening, Compose Tests & Upgrade Verification
```

---

## 9. Data Layer, Search & Trustworthy Sharing

### 9.1 Reactive Search in Room
Attendance log filtering by subject, teacher, status, and month executes via Room DAO queries:
```kotlin
@Query("""
    SELECT * FROM attendance_records 
    WHERE (:query IS NULL OR subjectName LIKE '%' || :query || '%' OR createdByName LIKE '%' || :query || '%')
    AND (:status IS NULL OR status = :status)
    AND (:monthYear IS NULL OR attendanceDate LIKE :monthYear || '%')
    ORDER BY attendanceDate DESC
""")
fun searchAttendance(query: String?, status: String?, monthYear: String?): Flow<List<AttendanceRecordEntity>>
```
Offloads filtering from UI threads to background database workers.

### 9.2 Trustworthy Report Sharing
Generated export text strictly distinguishes official records from on-device math:
```
Lloyd Student Attendance Report
Student: [Student Name] ([Roll No])
Overall Attendance: 78.4% (58 Present / 16 Absent / 74 Total)

Source: Verified Lloyd ERP Academic Records
Analysis: Calculated on device using official institutional thresholds.
Generated on: 2026-10-05 09:30
```

---

## 10. Migration Safety & Verification Gates

### 10.1 Upgrade Verification
1. **Fresh Installation**: Clean install $\to$ authenticate $\to$ store tokens in Keystore $\to$ sync $\to$ offline cache.
2. **Upgrade Migration from Legacy App**:
   - Verify legacy credentials/tokens migrate cleanly into `SecureTokenStore`.
   - Purge plaintext passwords during first boot of the updated app.
   - Cache migration preserves existing local attendance data without deletion.

### 10.2 Android 16 Behavior Testing
- Edge-to-edge layout compliance with `WindowInsetsCompat`.
- `POST_NOTIFICATIONS` runtime permission checks.
- Notification icons rendered strictly with vector drawables (no emojis).
- `AttendanceSyncWorker` constrained to network connectivity with `ExistingPeriodicWorkPolicy.KEEP`.
- All `PendingIntent` instances flagged with `FLAG_IMMUTABLE`.

### 10.3 Quality Gates
```bash
./gradlew lintDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
```

---

## 11. Self-Review Checklist

- [x] **Placeholder Scan**: Zero "TBD", "TODO", or unresolved requirements.
- [x] **Internal Consistency**: Build matrix, Compose BOM, Room queries, and Domain math align across all sections.
- [x] **Scope Check**: Full 12-phase migration decomposed into clean, executable increments.
- [x] **Ambiguity Check**: Section fallbacks, empty data states ($0/0$), threshold math, and token storage are explicitly specified.
