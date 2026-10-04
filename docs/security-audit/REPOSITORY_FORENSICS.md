# Lloyd ERP Security Assessment — Repository Forensics & Source Code Audit

**Target Repository:** `nkpcore/lloyd-erp`  
**Local Codebase:** `C:\Users\nikhil\Desktop\erpwidgit`  
**Git Remote:** `https://github.com/nkpcore/lloyd-erp.git`  
**Branch:** `main`  
**Analysis Date:** October 5, 2026  

---

## 1. Repository Overview & Git Evolution

Forensic analysis of the Git history reveals an active 4-commit sequence transitioning the repository from a rapid initial prototype to a Material Design 3 student dashboard with widgets:

```text
2ec5f71 (HEAD -> main) fix: remove demo mode, eliminate hardcoded text, add automated GitHub release on build
1111862 feat(ui): complete Material Design 3 Expressive overhaul and Section A-1 timetable
7a1d034 feat(api): optimize student attendance logs retrieval and pagination
98bc499 Initial commit: Lloyd ERP Attendance Pro Max with Dynamic Island Alerts, Widgets, and Class Logs
```

### Key Architectural Shifts Identified in History:
1. **Commit `98bc499` (Initial Commit)**:
   - Established the baseline OkHttp client (`ErpApiClient.java`), `EncryptedSharedPreferences` (`AppPreferences.java`), background worker (`AttendanceSyncWorker.java`), and Scriptable iOS widget.
   - Introduced a hardcoded fallback student ID (`28960`) in both `AppPreferences.java` and `ErpApiClient.java`.
   - Included demo credentials and layout placeholders.
2. **Commit `7a1d034` (API Optimization)**:
   - Enhanced `/attendance/student` query logic to paginate with `page_size=100`, sort descending by `attendance_date`, and loop through all available pages.
3. **Commit `1111862` (MD3 Expressive Redesign)**:
   - Replaced basic Android UI with a 4-tab BottomNavigationView (Dashboard, Schedule, Attendance History, Simulator).
   - Hardcoded the complete Section A-1 room NB-101 weekly schedule into `TimetableRepository.java`.
   - Added `AnimationHelper.java` for card press and rolling number animations.
4. **Commit `2ec5f71` (Cleanup & CI/CD)**:
   - Purged explicit demo mode buttons from `activity_login.xml`.
   - Added automated GitHub Actions build workflow (`.github/workflows/android-build.yml`).

---

## 2. Comprehensive Search Analysis

A systematic grep across all repository source files for key domain strings yields the following forensic findings:

| Search Term | Found In | Key Context & Usage |
| :--- | :--- | :--- |
| `erp.lloydcollege.in` | `ErpApiClient.java`, `LloydWidget.scriptable.js`, `network_security_config.xml`, `README.md`, `index-DhavIe_O.js` | Upstream production base URL (`https://erp.lloydcollege.in/api`). Whitelisted in network security config. |
| `/api/` | `ErpApiClient.java`, `LloydWidget.scriptable.js` | Endpoint routing prefix: `/auth/login`, `/auth/refresh`, `/student/me/monthly-attendance`, `/student/me/weekly-attendance`, `/attendance/student`. |
| `login` | `ErpApiClient.java`, `LoginActivity.java`, `LloydWidget.scriptable.js` | Auth entrypoint sending credentials with client device metadata. |
| `auth` | `ErpApiClient.java`, `Models.java` | Namespaces for `/auth/login` and `/auth/refresh`. |
| `token` / `refresh` | `ErpApiClient.java`, `AppPreferences.java`, `Models.java` | JWT bearer token lifecycle. Silent refresh handled via synchronized mutex in `ensureValidToken()`. |
| `cookie` | *None* | Neither the Android app nor the Scriptable widget uses session cookies; authentication is strictly stateless Bearer JWT. |
| `Authorization` / `Bearer` | `ErpApiClient.java`, `LloydWidget.scriptable.js` | HTTP header `Authorization: Bearer <access_token>` supplied on all authenticated endpoints. |
| `student_id` | `ErpApiClient.java`, `Models.java`, `AppPreferences.java` | Used as query parameter in `/attendance/student?student_id={id}`. Returned in user profile and monthly summary payloads. |
| `attendance` | `ErpApiClient.java`, `MainActivity.java`, `AttendanceSyncWorker.java` | Primary business domain: monthly aggregates, weekly routines, ledger logs, and bunk calculations. |
| `timetable` | `TimetableRepository.java`, `MainActivity.java` | Schedule domain: Currently hardcoded for Section A-1; ERP provides dynamic weekly routine via `/student/me/weekly-attendance`. |
| `profile` | `Models.java`, `AppPreferences.java`, `ErpApiClient.java` | `UserProfile` DTO deserializes `data.user` containing admission number, course, section, and semester. |
| `subject` / `faculty` | `Models.java`, `TimetableRepository.java`, `MainActivity.java` | Course name, subject ID, teacher name, lecture room number. |
| `notice` / `exam` / `result` | *Not present in API client* | Exam, results, notices, assignments, fees, and library endpoints are **not implemented** in the client codebase. |

---

## 3. Current API Endpoints & Contract Analysis

The application communicates exclusively with 5 distinct API routes on `https://erp.lloydcollege.in/api`:

### 3.1 `POST /auth/login`
- **Request Headers**: `Content-Type: application/json`, `Accept: application/json`, `User-Agent: LloydERP-AndroidWidget/1.0`
- **Request Body Payload**:
  ```json
  {
    "username": "<STUDENT_ADMISSION_NO>",
    "password": "<PLAINTEXT_PASSWORD>",
    "device_id": "<UUID>",
    "app_version": "2.0.0",
    "timezone": "Asia/Kolkata",
    "browser_name": "AndroidApp",
    "browser_version": "1.0",
    "os_name": "Android",
    "device_type": "mobile"
  }
  ```
- **Response Structure (`Models.ApiResponse<Models.LoginData>`)**:
  ```json
  {
    "status": true,
    "message": "Login successful",
    "data": {
      "access_token": "<REDACTED_JWT>",
      "refresh_token": "<REDACTED_REFRESH_TOKEN>",
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

### 3.2 `POST /auth/refresh`
- **Request Body**: `{"refresh_token": "<REFRESH_TOKEN>"}`
- **Response**: Returns fresh `access_token`, `refresh_token`, and `expires_in: 86400`.

### 3.3 `GET /student/me/monthly-attendance`
- **Headers**: `Authorization: Bearer <access_token>`, `Accept: application/json`
- **Response**: Aggregated summary partitioned by calendar month:
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

### 3.4 `GET /student/me/weekly-attendance`
- **Headers**: `Authorization: Bearer <access_token>`, `Accept: application/json`
- **Response**: Exposes the weekly schedule routine, period boundaries, classroom numbers, and attendance status:
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
              "teacher_name": "Dr. Vivek Das"
            }
          ]
        }
      ]
    }
  }
  ```

### 3.5 `GET /attendance/student?student_id={id}&page={page}&page_size=100&sort_by=attendance_date&sort_dir=desc`
- **Headers**: `Authorization: Bearer <access_token>`, `Accept: application/json`
- **Response**: Paginated array of individual attendance records:
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

## 4. Current Storage Architecture

### 4.1 Android Client (`AppPreferences.java`)
- Primary storage is configured using `EncryptedSharedPreferences` backed by `MasterKey.KeyScheme.AES256_GCM`.
- Encrypted Keys:
  - `KEY_DEVICE_ID`: Persistent UUID.
  - `KEY_USERNAME`: Student admission number.
  - `KEY_PASSWORD`: **Plaintext student password**.
  - `KEY_ACCESS_TOKEN`: Bearer JWT.
  - `KEY_REFRESH_TOKEN`: Refresh credential.
  - `KEY_USER_PROFILE`: JSON serialized `UserProfile`.
  - `KEY_CACHED_STATS`: JSON serialized `CalculatedStats`.
  - `KEY_MONTHLY_JSON`: Raw JSON string of monthly attendance.
  - `KEY_WEEKLY_JSON`: Raw JSON string of weekly attendance.
  - `KEY_ATTENDANCE_LOGS_JSON`: Raw JSON string of all semester class logs.
  - `KEY_STUDENT_ID`: Integer student identity.
  - `KEY_LAST_SEEN_ATTENDANCE_ID`: Long ID of the latest attendance transaction.
- **Critical Flaw**: Lines 56-59 of `AppPreferences.java`:
  ```java
  } catch (Exception e) {
      // Fallback to standard private preferences if Keystore unavailable
      this.prefs = appContext.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE);
  }
  ```
  If Android Keystore encounters hardware or vendor issues (common on custom OEM ROMs), the app falls back to standard `SharedPreferences`, storing the student's **plaintext password** in unencrypted XML on the device filesystem (`/data/data/com.lloyd.attendance/shared_prefs/`).

### 4.2 Web Client (`web/`)
- In `web/index.html` (and the React SPA bundle `web/assets/index-DhavIe_O.js`), credentials and tokens are stored in browser `localStorage`.
- Storage keys: `lloyd_token`, `lloyd_user`, `lloyd_stats`.
- **Risk**: Any cross-site scripting (XSS) vulnerability or untrusted third-party script can immediately read `localStorage.getItem('lloyd_token')`.

### 4.3 iOS Scriptable Widget (`LloydWidget.scriptable.js`)
- Credentials are hardcoded directly at the top of the script file:
  ```javascript
  const USERNAME = "YOUR_ADMISSION_NO";
  const PASSWORD = "YOUR_PASSWORD";
  ```
- **Risk**: High risk of accidental disclosure if users share scripts, sync to shared repositories, or export screenshots.

---

## 5. Hardcoded Values & Mock Data Audit

| Location | Hardcoded Value | Issue Classification | Remediation Status |
| :--- | :--- | :--- | :--- |
| `AppPreferences.java` (Line 160) | `KEY_STUDENT_ID` default was `28960` | Hardcoded student identity fallback | Resolved to default `0` |
| `ErpApiClient.java` (Line 257) | `if (targetStudentId <= 0) targetStudentId = 28960;` | Hardcoded fallback student ID | Resolved: dynamically resolved from `/monthly-attendance` |
| `activity_main.xml` (Line 54) | `android:text="NIKHIL KUMAR PANDEY"` | Hardcoded user name placeholder | Modified to generic `Lloyd Student` |
| `activity_main.xml` (Lines 211, 232, 256, 286) | `"60.8%"`, `"45"`, `"29"`, `"74"` | Hardcoded attendance statistics in XML | Flashes on screen prior to sync; requires neutral skeleton (`--.-%`) |
| `TimetableRepository.java` (Lines 81-145) | Static 6-day timetable for Section A-1 | Static classroom schedule | Must be dynamically populated from `/student/me/weekly-attendance` |
| `LloydWidget.scriptable.js` (Lines 13-14) | `"YOUR_ADMISSION_NO"`, `"YOUR_PASSWORD"` | Plaintext credential placeholders | Inherent limitation of Scriptable engine |

---

## 6. Current Security Controls & Weaknesses

1. **Network Security Configuration (`network_security_config.xml`)**:
   - `cleartextTrafficPermitted="false"`: Disallows plain HTTP traffic across the application.
   - `<certificates src="system" />`: Rejects user-installed certificates, protecting students from unassisted MITM on campus networks.
2. **Build & Obfuscation (`build.gradle` & `proguard-rules.pro`)**:
   - `proguard-rules.pro` contains rules to strip `android.util.Log` and keep Gson models.
   - **Crucial Defect**: `minifyEnabled false` is configured for the release build type in `build.gradle`! The ProGuard rules are never executed, meaning classes are un-obfuscated and `Log.i` statements remain active in production APKs.
3. **Inter-Process Communication (IPC)**:
   - `AttendanceWidgetProvider` is an exported `AppWidgetProvider` receiver listening for `com.lloyd.attendance.ACTION_REFRESH_WIDGET`.
   - Lacks custom signature permissions, allowing any third-party app to broadcast this action and force background network synchronization.
