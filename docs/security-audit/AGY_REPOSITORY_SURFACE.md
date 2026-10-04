# AGY Repository Surface & Codebase Forensics — Lloyd ERP

**Repository:** `nkpcore/lloyd-erp`  
**Workspace Path:** `C:\Users\nikhil\Desktop\erpwidgit`  
**Remote Origin:** `https://github.com/nkpcore/lloyd-erp.git`  
**Branch:** `main`  
**Evaluation Standard:** OWASP MASVS / Secure Code Review  
**Date:** October 2026  

---

## 1. Executive Codebase Overview

Forensic analysis of the `nkpcore/lloyd-erp` repository reveals a multi-tier student utility project interfacing with the institutional Lloyd ERP system (`https://erp.lloydcollege.in`). The repository spans three primary clients:

1. **Android Application (`android/`)**: Native Java/Android SDK implementation targeting API 34 (Android 14) with a minimum SDK of 26 (Android 8.0). Features an XML/Material Design 3 interface, background synchronization worker, home-screen widget provider, and OkHttp client.
2. **Web / PWA Client (`web/`)**: Single-page web dashboard bundled with modern JavaScript tooling.
3. **iOS Scriptable Companion (`LloydWidget.scriptable.js`)**: Standalone JavaScript widget script utilizing Scriptable's native runtime to render an iOS Lock Screen/Home Screen widget.

---

## 2. Source Code Keyword Matrix

A systematic audit across all repository files for security-critical identifiers yielded the following mappings:

| Search String | Files Identified | Architectural Role & Context |
| :--- | :--- | :--- |
| `erp.lloydcollege.in` | `ErpApiClient.java`, `LloydWidget.scriptable.js`, `network_security_config.xml`, `README.md` | Upstream production base URL (`https://erp.lloydcollege.in/api`). Whitelisted for cleartext exceptions if needed, but communicates over HTTPS. |
| `/api/` | `ErpApiClient.java`, `LloydWidget.scriptable.js` | Endpoint routing prefix: `/auth/login`, `/auth/refresh`, `/student/me/monthly-attendance`, `/student/me/weekly-attendance`, `/attendance/student`. |
| `auth` / `login` | `ErpApiClient.java`, `LoginActivity.java`, `Models.java` | Authentication subsystem. Dispatches student admission number and password in JSON body. |
| `refresh` | `ErpApiClient.java`, `Models.java` | Silent token renewal logic via `/api/auth/refresh` triggered on HTTP 401 or token expiration. |
| `Authorization` / `Bearer` | `ErpApiClient.java`, `LloydWidget.scriptable.js` | Stateless HTTP request authorization header. Supplies signed JWT to protected endpoints. |
| `cookie` | *(None)* | No cookie-based session handling is utilized; sessions rely solely on bearer authorization. |
| `student_id` | `ErpApiClient.java`, `AppPreferences.java`, `Models.java` | Primary resource identifier. Appears in login response and is passed as a query parameter in `/api/attendance/student?student_id={id}`. |
| `attendance` | `ErpApiClient.java`, `MainActivity.java`, `AttendanceSyncWorker.java` | Core business logic: Monthly percentages, subject-wise attendance ledgers, bunk/target calculators. |
| `timetable` | `TimetableRepository.java`, `MainActivity.java` | Class schedule: Hardcoded in repository for Section A-1; dynamic routine retrieved via `/student/me/weekly-attendance`. |
| `profile` / `user` | `Models.java`, `AppPreferences.java` | Deserialization models for student metadata: name, admission number, course, semester, section. |
| `notice` / `exam` / `result` | *(None in client)* | Not implemented in client networking layer. |

---

## 3. Client Architecture & Networking Stack

### 3.1 HTTP Client Configuration (`ErpApiClient.java`)
- **Engine:** OkHttp 4.12.0 with custom JSON serialization via `org.json.JSONObject`.
- **Base URL:** `https://erp.lloydcollege.in/api`
- **Timeout Configuration:**
  - Connect Timeout: 15 seconds
  - Read Timeout: 20 seconds
  - Write Timeout: 15 seconds
- **Session Mutex:** Implements a synchronized mutex `ensureValidToken()` to serialize concurrent background and UI requests during token refresh cycles.
- **Logging Behavior:** Custom verbose logging via `Log.i("ERP_RAW", ...)` which prints raw server responses to the Android system Logcat buffer.

### 3.2 Token & Credential Storage (`AppPreferences.java`)
- **Primary Mechanism:** Android Jetpack `EncryptedSharedPreferences` backed by `MasterKey.KeyScheme.AES256_GCM`.
- **Storage Keys:**
  - `KEY_ACCESS_TOKEN`: Ephemeral JWT access token.
  - `KEY_REFRESH_TOKEN`: Long-lived session renewal token.
  - `KEY_USERNAME`: Student admission number.
  - `KEY_PASSWORD`: Plaintext student password (persisted to facilitate auto-relogin if refresh token fails).
  - `KEY_STUDENT_ID`: Numerical student ID.
- **Fallback Mechanism:** If the Android Keystore throws an initialization exception (common on certain custom ROMs or modified devices), `AppPreferences.java` catches the error and instantiates standard unencrypted `SharedPreferences` (`PREF_NAME + "_fallback"`), storing all keys including `KEY_PASSWORD` in plaintext XML.

---

## 4. Hardcoded Assumptions & Static Data

1. **Default Student ID Fallback**: In `AppPreferences.java` (Line 114) and `ErpApiClient.java` (Line 132), the codebase historically referenced student ID `28960` as a default parameter if no ID was loaded from storage.
2. **Timetable Static Injection**: `TimetableRepository.java` contains a 100% hardcoded weekly schedule specifically for "B.Tech CSE - 1st Semester - Section A-1" (Room NB-101), including subjects, times, and faculty names. This static fallback is used when offline or when dynamic timetable sync is disabled.
3. **Widget Plaintext Config**: `LloydWidget.scriptable.js` contains top-level constants `USERNAME = "YOUR_ADMISSION_NO"` and `PASSWORD = "YOUR_PASSWORD"`, requiring end-users to hardcode credentials directly in plain JavaScript.

---

## 5. Security Posture of the Repository

- **Network Security Config:** `res/xml/network_security_config.xml` explicitly enforces TLS (`cleartextTrafficPermitted="false"`) across subdomains of `erp.lloydcollege.in`.
- **R8 / ProGuard:** `build.gradle` has `minifyEnabled false` in the `release` block, leaving classes, method names, and debug logs un-stripped in production APK builds.
- **Exported Receivers:** `AttendanceWidgetProvider` is exported in `AndroidManifest.xml` without custom permission guards, allowing any local application on the device to send `ACTION_REFRESH_WIDGET` broadcasts.
