# Lloyd ERP — Comprehensive Repository Audit & Technical Analysis

**Target Repository:** `nkpcore/lloyd-erp`  
**Date of Audit:** October 5, 2026  
**Audit Scope:** Android Native Application, Web/PWA Client, iOS Scriptable Widget, API Architecture, Authentication & Keystore Security, Data Persistence, Timetable & Attendance Intelligence, Home Screen Widgets, Dynamic Notifications, CI/CD Workflows, Dependency Health, and Test Coverage.

---

## 1. Executive Summary

This repository hosts a multi-platform student companion application designed for students of Lloyd College (`erp.lloydcollege.in`). The codebase consists of:
1. **Android Native Application** (`android/`): Written in Java 17 targeting Android SDK 34 (Android 14+), utilizing Material Design 3 Expressive tokens, OkHttp3 connection pooling, EncryptedSharedPreferences, WorkManager background synchronization, an Apple Dynamic Island-styled notification banner, and an interactive home screen widget (`AttendanceWidgetProvider`).
2. **Web / PWA Client** (`web/`): A web application originally authored as a single-page responsive HTML/CSS/JS dashboard with service worker offline caching, recently transitioned toward compiled modern assets.
3. **iOS Scriptable Widget** (`LloydWidget.scriptable.js`): A standalone JavaScript widget for iOS running in the Scriptable engine, presenting overall percentage and bunk safety margins.

While the Android client possesses an engaging visual direction (dark slate `#0F172A`, emerald/rose status semantics, rolling numbers, and custom notification RemoteViews), the codebase suffers from severe architectural bottlenecks: monolithic God-activities (`MainActivity` exceeds 900 lines), raw JSON blob caching in SharedPreferences rather than structured database models (e.g., Room), plaintext password persistence, a completely hardcoded section timetable in `TimetableRepository`, zero automated unit or instrumentation tests, and remaining hardcoded mock student defaults across layout XML files.

---

## 2. Current Architecture

```text
┌────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                            │
│  MainActivity (Monolithic 939 LOC)  │  LoginActivity (147 LOC)         │
│  - Tab Switching & View Inflation   │  - Credential Input              │
│  - Animated UI & Micro-interactions │  - Keystore Credential Storage   │
│  - View-level Bunk Calculations     │                                  │
├────────────────────────────────────────────────────────────────────────┤
│                          SCHEDULING & WIDGETS                          │
│  TimetableRepository               │  AttendanceWidgetProvider         │
│  - Hardcoded Section A-1 Timetable  │  - RemoteViews Home Widget        │
│  - In-memory minute arithmetic     │  - On-demand Sync Trigger         │
├────────────────────────────────────────────────────────────────────────┤
│                          BACKGROUND & ALERTS                           │
│  AttendanceSyncWorker (WorkManager)│  NotificationHelper               │
│  - 15-minute Periodic Sync Loop    │  - Custom Heads-Up Notification   │
│  - Last-seen Attendance ID Diff    │  - Apple Dynamic Island Layout    │
├────────────────────────────────────────────────────────────────────────┤
│                          PERSISTENCE LAYER                             │
│  AppPreferences                                                        │
│  - EncryptedSharedPreferences (AES256-GCM / MasterKey)                 │
│  - Raw JSON Strings: "monthly_json", "weekly_json", "attendance_logs"  │
│  - Plaintext username & password cached for auto-relogin               │
├────────────────────────────────────────────────────────────────────────┤
│                           NETWORKING LAYER                             │
│  ErpApiClient (OkHttp3 + Gson)                                         │
│  - Static ConnectionPool(10, 5m)   │  - Token Refresh Interceptor      │
│  - Direct HTTP Calls to ERP        │  - Synchronized Mutex Re-auth     │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Current Features

| Module | Feature | Implementation | Current State |
| :--- | :--- | :--- | :--- |
| **Authentication** | Student Sign-In | `LoginActivity` $\to$ `ErpApiClient.login()` | Working. Stores tokens & credentials in Keystore. Auto-navigates if session valid. |
| **Authentication** | Silent Token Refresh | `ErpApiClient.refreshToken()` | Working. Automatically handles 401 Unauthorized by calling `/auth/refresh` or re-login. |
| **Dashboard** | Hero Attendance Percentage | `MainActivity` Tab 1 | Working. Shows rolling number animations, safety badge (`SAFE` vs `SHORTAGE`). |
| **Dashboard** | Metric Tiles | `MainActivity` Tab 1 | Working. Present, Absent, Total count cards. |
| **Dashboard** | Live Classroom Glance | `MainActivity` Tab 1 | Working. Computes ongoing/upcoming lecture for Section A-1 with breathing pulse dot. |
| **Schedule** | Day Timeline | `MainActivity` Tab 2 | Working for Section A-1. Hardcoded in `TimetableRepository`. |
| **Attendance Log** | Semester Lectures List | `MainActivity` Tab 3 | Working. Grouped by Date header cards with Present/Absent badges. |
| **Attendance Log** | Multi-Criteria Filters | `MainActivity` Tab 3 | Working. Filter by status (`All`, `Present`, `Absent`), month, and subject chips. |
| **Simulator** | Attendance Planner | `MainActivity` Tab 4 | Working. Continuous SeekBar (`-15` to `+30`) predicting percentage and bunk buffer. |
| **Subject Breakdown** | Subject Analytics | `MainActivity` Tab 4 | Working. Dynamically computed from `allAttendanceLogs`. |
| **Notifications** | Dynamic Island Alert | `NotificationHelper` | Working. Custom `RemoteViews` with high priority heads-up banner when attendance marked. |
| **Widgets** | Home Screen Widget | `AttendanceWidgetProvider` | Working. RemoteViews with live percentage, attendance ratio, status pill, and refresh button. |
| **Background Sync** | WorkManager Periodic Sync | `AttendanceSyncWorker` | Working. Configured for 15-minute intervals with network constraint. |

---

## 4. Current API Endpoints & Contract

The application communicates with the upstream ERP at `https://erp.lloydcollege.in/api`:

### 4.1 `POST /auth/login`
- **Purpose**: Authenticates student with admission number and password.
- **Request Body**:
  ```json
  {
    "username": "STUDENT_ADMISSION_NO",
    "password": "PLAINTEXT_PASSWORD",
    "device_id": "UUID",
    "app_version": "2.0.0",
    "timezone": "Asia/Kolkata",
    "browser_name": "AndroidApp",
    "browser_version": "1.0",
    "os_name": "Android",
    "device_type": "mobile"
  }
  ```
- **Response**:
  ```json
  {
    "status": true,
    "message": "Login successful",
    "data": {
      "access_token": "JWT_ACCESS_TOKEN",
      "refresh_token": "REFRESH_TOKEN",
      "expires_in": 86400,
      "profile_id": 28960,
      "name": "STUDENT NAME",
      "role_id": 4,
      "school_id": 1,
      "user": {
        "id": 28960,
        "name": "STUDENT NAME",
        "username": "ADMISSION_NO",
        "email": "student@lloydcollege.in",
        "role": "student",
        "admission_no": "ADMISSION_NO",
        "course": "B.Tech CSE",
        "semester": "1st Semester",
        "section": "A-1"
      }
    }
  }
  ```

### 4.2 `POST /auth/refresh`
- **Purpose**: Issues a new JWT access token using a valid refresh token.
- **Request Body**: `{"refresh_token": "REFRESH_TOKEN"}`
- **Response**: `{ "status": true, "data": { "access_token": "...", "refresh_token": "...", "expires_in": 86400 } }`

### 4.3 `GET /student/me/monthly-attendance`
- **Purpose**: Retrieves month-by-month attendance summary for the authenticated student.
- **Headers**: `Authorization: Bearer <token>`
- **Response Data Structure**:
  ```json
  {
    "status": true,
    "data": {
      "student_id": 28960,
      "student_name": "STUDENT NAME",
      "months": [
        {
          "year": 2026,
          "month_number": 10,
          "month_label": "Oct 2026",
          "present": 2,
          "total": 5,
          "percentage": 40.0
        }
      ]
    }
  }
  ```

### 4.4 `GET /student/me/weekly-attendance`
- **Purpose**: Exposes the student's weekly schedule routine and attendance status per period.
- **Headers**: `Authorization: Bearer <token>`
- **Response Data Structure**:
  ```json
  {
    "status": true,
    "data": {
      "student_id": 28960,
      "student_name": "STUDENT NAME",
      "week_start": "2026-09-28",
      "week_end": "2026-10-03",
      "days": [
        {
          "date": "2026-09-28",
          "day": "Monday",
          "periods": [
            {
              "routine_id": 101,
              "subject_id": 501,
              "subject_name": "Applied Chemistry",
              "start_time": "09:00",
              "end_time": "09:50",
              "room_no": "NB-101",
              "status": "present",
              "teacher_name": "Dr. Vivek Das",
              "date": "2026-09-28",
              "day": "Monday"
            }
          ]
        }
      ]
    }
  }
  ```

### 4.5 `GET /attendance/student?student_id={id}&page={page}&page_size=100&sort_by=attendance_date&sort_dir=desc`
- **Purpose**: Paginated class-by-class attendance logs.
- **Headers**: `Authorization: Bearer <token>`
- **Query Parameters**:
  - `student_id`: Numerical student identity.
  - `page`: Page index (1-based).
  - `page_size`: 100 items per request.
  - `sort_by`: `attendance_date`.
  - `sort_dir`: `desc`.
- **Response Structure**:
  ```json
  {
    "status": true,
    "data": [
      {
        "id": 98214,
        "school_id": 1,
        "class_id": 12,
        "semester_id": 1,
        "section_id": 2,
        "subject_id": 304,
        "subject_name": "Applied Mathematics-I",
        "student_id": 28960,
        "student_name": "STUDENT NAME",
        "roll_no": "26",
        "attendance_date": "2026-10-01",
        "status": "Present",
        "class_lecture": "3",
        "created_at": "2026-10-01 10:45:12",
        "created_by_name": "Dr. Digvijay Singh"
      }
    ],
    "meta": {
      "total_items": 74,
      "current_page": 1,
      "page_size": 100,
      "total_pages": 1,
      "current_page_items": 74
    }
  }
  ```

---

## 5. Security & Privacy Audit

### 5.1 Critical Security Weaknesses

1. **Plaintext Password Retention**:
   - In `AppPreferences.java`: `saveCredentials(String username, String password)`.
   - The user's plaintext password is permanently saved to SharedPreferences so that the client can auto-relogin when tokens expire.
   - *Risk*: Although `EncryptedSharedPreferences` provides AES-256-GCM encryption backed by Android Keystore, if Keystore initialization throws an exception on certain Android vendor builds, lines 56-59 fall back to:
     `this.prefs = appContext.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE);`
     storing the student's raw password in unencrypted plaintext on the filesystem.
   - *Remediation*:
     - Token-based authentication should rely strictly on OAuth2 / JWT refresh token lifecycles (`/auth/refresh`).
     - Passwords should only be kept in memory during the initial login call and never stored unencrypted.
     - Keystore fallback must refuse to store sensitive credentials in plaintext, or require biometric authorization.

2. **Client-Supplied Student ID Authorization**:
   - `getStudentAttendanceLogs(int studentId)` calls `/attendance/student?student_id={id}`.
   - The client obtains `studentId` from `/student/me/monthly-attendance` or the login response.
   - *Risk*: In multitenant academic systems, if the backend lacks strict row-level authorization, arbitrary student logs could be retrieved by altering the query parameter.
   - *Remediation*: Ensure the app always binds requests to the verified ID returned by `/student/me/monthly-attendance` or `/auth/login`, and validate that the token matches the student record.

3. **Sensitive Telemetry & Logging in Production**:
   - In `ErpApiClient.java`:
     - Line 193: `android.util.Log.i("ERP_RAW", "Monthly (" + response.code() + "): " + resStr);`
     - Line 226: `android.util.Log.i("ERP_RAW", "Weekly (" + response.code() + "): " + resStr);`
     - Line 283: `android.util.Log.i("ERP_RAW", "Requesting URL: " + url);`
   - These statements print raw JSON dumps containing full student rosters, names, roll numbers, and dates to system logcat.
   - *Remediation*: Strip or sanitize all `Log.i`/`Log.d` statements in release builds using Proguard rules or a custom Logger with `BuildConfig.DEBUG` checks.

4. **Scriptable iOS Script Hardcoding**:
   - `LloydWidget.scriptable.js` prompts students to paste their credentials directly into the script source code (`const USERNAME = "YOUR_ADMISSION_NO"; const PASSWORD = "YOUR_PASSWORD";`).
   - *Risk*: Users sharing screenshots or uploading their Scriptable scripts to iCloud/Git expose their student ERP passwords.

---

## 6. Technical Debt & Code Smells

1. **God Activity Pattern (`MainActivity.java`)**:
   - `MainActivity.java` is 939 lines long. It handles:
     - Network dispatching with custom executors
     - JSON parsing with Gson
     - Dynamic view generation for 4 tabs
     - Seekbar change listeners and mathematical simulations
     - ValueAnimator loops and breathing pulse animations
     - Offline caching and UI state orchestration
   - *Impact*: High coupling, impossible to unit test without Robolectric/instrumentation, high risk of regression when touching UI.

2. **JSON Blob Storage Pattern vs Relational Database**:
   - `AppPreferences` stores entire semester history as one giant JSON string (`attendance_logs_json`).
   - Every time `MainActivity` loads, it deserializes the entire JSON string into memory.
   - Filtering operations (by status, month, subject) iterate through the in-memory `ArrayList` on every tab change.
   - *Impact*: No database indexing, high memory churn, potential OutOfMemory on low-end devices with multi-semester logs. Room DB should be adopted.

3. **Double Network Call Race Condition**:
   - `MainActivity.fetchDataFromErp()` previously spawned `apiClient.fetchAndCalculateStats()` and `apiClient.getStudentAttendanceLogs()` concurrently.
   - Because `fetchAndCalculateStats()` calls `/monthly-attendance` which resolves the `studentId`, calling `getStudentAttendanceLogs(0)` concurrently caused a race where `targetStudentId` was still 0 or fell back to hardcoded defaults.

4. **Hardcoded Section Timetable**:
   - `TimetableRepository.java` contains 197 lines of hardcoded periods for Section A-1.
   - Any schedule change by the college requires an app release.
   - The ERP already provides `/student/me/weekly-attendance` which delivers real period routines.

5. **Lack of Automated Testing**:
   - Zero test files exist in `src/test/java` or `src/androidTest/java`.
   - Critical domain logic (bunk allowance formula, recovery calculations, date parsing) has no automated validation.

---

## 7. Hardcoded / Mock Data Inventory

| File | Location | Hardcoded Value | Issue & Status |
| :--- | :--- | :--- | :--- |
| `AppPreferences.java` | Line 160 | `prefs.getInt(KEY_STUDENT_ID, 28960)` | Hardcoded student ID default. *(Identified & fixed to default `0`)* |
| `ErpApiClient.java` | Line 257 | `if (targetStudentId <= 0) targetStudentId = 28960` | Hardcoded student ID fallback. *(Identified & fixed to dynamic resolution)* |
| `activity_main.xml` | Line 54 | `android:text="NIKHIL KUMAR PANDEY"` | Hardcoded user name. *(Replaced with dynamic `tv_main_student_name`)* |
| `activity_main.xml` | Lines 211, 232, 256, 286 | `"60.8%"`, `"45"`, `"29"`, `"74"` | Hardcoded attendance statistics in layout XML. |
| `activity_main.xml` | Line 434 | `"All (74)"` | Hardcoded filter count. |
| `activity_main.xml` | Line 612 | `"60.8%"` | Hardcoded simulator projected percentage. |
| `activity_main.xml` | Line 343 | `"Applied Mathematics-I"` | Hardcoded live glance subject. |
| `TimetableRepository.java` | Lines 81-145 | All 6 days of periods and faculty | Static timetable for Section A-1. Must be backed by ERP `/weekly-attendance` with offline fallback. |
| `LloydWidget.scriptable.js` | Lines 13-14 | `"YOUR_ADMISSION_NO"`, `"YOUR_PASSWORD"` | Plaintext credential placeholders. |

---

## 8. Dependencies, Build & Release Health

### 8.1 Android Dependency Analysis
```groovy
implementation 'androidx.appcompat:appcompat:1.7.0'          // Stable
implementation 'com.google.android.material:material:1.12.0' // Modern MD3 library
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
implementation 'androidx.cardview:cardview:1.0.0'
implementation 'com.squareup.okhttp3:okhttp:4.12.0'          // Connection pooling & HTTP/2
implementation 'com.google.code.gson:gson:2.11.0'           // Serialization
implementation 'androidx.work:work-runtime:2.9.1'            // Background tasks
implementation 'androidx.security:security-crypto:1.1.0-alpha06' // Keystore crypto
```
- **Evaluation**: Core libraries are modern and stable.
- **Missing**:
  - `androidx.room:room-runtime` & compiler: Needed for structured offline caching.
  - `androidx.lifecycle:lifecycle-viewmodel` & `lifecycle-livedata`: Needed for clean MVVM separation.
  - `junit:junit:4.13.2`, `androidx.test.ext:junit:1.2.1`: Needed for unit test suite.

### 8.2 CI/CD Pipeline
- `.github/workflows/android-build.yml` runs on push to `main` using `ubuntu-latest`, `temurin-17`, and Gradle 8.10.2.
- Automatically compiles debug APK and releases to GitHub Releases.
- *Issue*: Builds only debug APK (`assembleDebug`). A production release workflow should sign with a release keystore and run lint/tests before publishing.

---

## 9. Recommended Target Architecture

To satisfy the non-negotiable rules (No Mock Data, No Hardcoded User Data, Offline-First, Clean Architecture):

```text
                               Upstream Lloyd ERP
                                       │
                                       ▼
                       ┌──────────────────────────────┐
                       │        ErpApiClient          │
                       │   (Auth, Monthly, Weekly,    │
                       │       Attendance Logs)       │
                       └──────────────┬───────────────┘
                                      │
                                      ▼
                       ┌──────────────────────────────┐
                       │     AttendanceRepository     │
                       │   - Offline-First Cache      │
                       │   - Remote Sync Reconciliation│
                       └──────────────┬───────────────┘
                                      │
                       ┌──────────────┴───────────────┐
                       │                              │
                       ▼                              ▼
          ┌─────────────────────────┐    ┌─────────────────────────┐
          │   Local Room Database   │    │  AttendanceDomainEngine │
          │  - StudentProfile       │    │  - BunkCalculator       │
          │  - AttendanceRecord     │    │  - RecoveryCalculator   │
          │  - TimetableEntry       │    │  - RiskProjectionEngine │
          │  - SyncMetadata         │    │  - TrendAnalyzer        │
          └─────────────────────────┘    └────────────┬────────────┘
                                                      │
                                                      ▼
                                         ┌─────────────────────────┐
                                         │  ViewModel / StateFlow  │
                                         │  (MainViewModel)        │
                                         └────────────┬────────────┘
                                                      │
                       ┌──────────────────────────────┼──────────────────────────────┐
                       │                              │                              │
                       ▼                              ▼                              ▼
             ┌───────────────────┐          ┌───────────────────┐          ┌───────────────────┐
             │ MainActivity (MD3)│          │ Home Screen Widget│          │Dynamic Alerts / OS│
             │ - Dashboard       │          │ (WidgetProvider)  │          │ (NotificationMgr) │
             │ - Real Timetable  │          └───────────────────┘          └───────────────────┘
             │ - Attendance Logs │
             │ - Goal Simulator  │
             └───────────────────┘
```

---

## 10. Audit Sign-Off

The audit confirms that the core ERP networking and authentication pipelines are fully capable of providing 100% real data without any mock fallbacks. The immediate engineering priorities are:
1. Hardening Keystore security and removing password plaintext persistence.
2. Purging remaining XML placeholder constants in favor of dynamic placeholders (`--.-%`).
3. Decoupling mathematical domain calculations into dedicated, testable domain classes.
4. Integrating real timetable data from `/student/me/weekly-attendance`.
5. Establishing a complete unit testing harness for attendance mathematics.
