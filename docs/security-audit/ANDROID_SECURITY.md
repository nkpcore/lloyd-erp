# Lloyd ERP Security Assessment — Android Application Security Audit

**Target Codebase:** `android/` within `nkpcore/lloyd-erp`  
**Package Name:** `com.lloyd.attendance`  
**Target SDK:** 34 (Android 14) / Min SDK: 26 (Android 8.0)  
**Security Standard:** OWASP Mobile Application Security Verification Standard (MASVS v2.0)  

---

## 1. Static Configuration & Manifest Audit

### 1.1 `AndroidManifest.xml` Analysis

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:allowBackup="false"
        android:networkSecurityConfig="@xml/network_security_config"
        android:theme="@style/Theme.LloydAttendance">
        ...
```

| Check Item | Value / Configuration | Security Evaluation |
| :--- | :--- | :---: |
| **Permissions** | `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS` | **PASS (Minimal)**. No dangerous or extraneous permissions requested. |
| **Data Backup** | `android:allowBackup="false"` | **PASS**. Prevents `adb backup` extraction of app sandbox data. |
| **Cleartext Traffic** | Disallowed via `@xml/network_security_config` | **PASS**. Prevents inadvertent HTTP traffic. |
| **Exported Activities** | `MainActivity` (`exported="true"`), `LoginActivity` (`exported="false"`) | **PASS**. Only launcher activity is exported; login screen is protected. |
| **Exported Receivers** | `AttendanceWidgetProvider` (`exported="true"`) | **RISK**. Exported receiver handles action `com.lloyd.attendance.ACTION_REFRESH_WIDGET` without custom signature permission. |
| **Deep Links / WebViews** | No intent-filter deep links; zero WebViews in codebase | **PASS**. Eliminates URL spoofing, deep link hijacking, and WebView XSS. |

---

## 2. Storage & Cryptography Security (MASVS-STORAGE / CRYPTO)

### 2.1 Credential & Password Persistence Flaw
- **Location:** `com.lloyd.attendance.data.AppPreferences.java`
- **Implementation:**
  ```java
  public void saveCredentials(String username, String password) {
      prefs.edit()
              .putString(KEY_USERNAME, username)
              .putString(KEY_PASSWORD, password)
              .apply();
  }
  ```
- **Audit Finding:** The application stores the student's **raw, unhashed plaintext password** in local preferences.
- **Rationale in Code:** Used to perform transparent auto-relogin if both the access token and refresh token fail.
- **Vulnerability:**
  - Modern authentication designs must **never persist user passwords** once a session/refresh token is issued.
  - If a device is rooted, compromised by malware with root privileges, or inspected via memory dumps, the student's primary institutional password is fully compromised.
- **The Keystore Fallback Trap (Lines 56-59):**
  ```java
  } catch (Exception e) {
      // Fallback to standard private preferences if Keystore unavailable
      this.prefs = appContext.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE);
  }
  ```
  If Android Keystore initialization throws an exception (frequent on custom Android ROMs or older devices), the app falls back to standard `MODE_PRIVATE` SharedPreferences. Consequently, the plaintext password is saved in **unencrypted XML** on the local filesystem (`/data/data/com.lloyd.attendance/shared_prefs/lloyd_attendance_secure_prefs_fallback.xml`).

---

## 3. Network & Logging Security (MASVS-NETWORK / CODE)

### 3.1 Network Security Configuration (`network_security_config.xml`)
```xml
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">erp.lloydcollege.in</domain>
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </domain-config>
</network-security-config>
```
- **Evaluation:** **EXCELLENT**. Cleartext is disabled and trust anchors are locked to `system` CAs. User-installed proxy certificates (Burp Suite / Charles Proxy) will be rejected out-of-the-box on non-rooted production devices.

### 3.2 Sensitive Telemetry & Logcat Data Leakage
- **Location:** `com.lloyd.attendance.api.ErpApiClient.java`
- **Code Audit:**
  - Line 193: `android.util.Log.i("ERP_RAW", "Monthly (" + response.code() + "): " + resStr);`
  - Line 226: `android.util.Log.i("ERP_RAW", "Weekly (" + response.code() + "): " + resStr);`
  - Line 283: `android.util.Log.i("ERP_RAW", "Requesting URL: " + url);`
- **Audit Finding:** The application dumps full JSON responses containing student names, roll numbers, teacher names, and academic dates directly to the system log buffer (`Logcat`).
- **Build Setting Aggravation:**
  - In `android/app/build.gradle`:
    ```groovy
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    ```
  - Because `minifyEnabled` is set to `false`, the ProGuard rule in `proguard-rules.pro` (`-assumenosideeffects class android.util.Log { ... }`) is **never executed**! The sensitive log statements remain active even in release APKs.

---

## 4. Specific Audit Inquiries Answered

### 4.1 Student Identity Binding
- **Question:** Does the attendance endpoint properly bind the authenticated account to the requested student?
- **Finding:** **NO**. The client calls `/attendance/student?student_id={id}`. While `ErpApiClient` dynamically resolves `targetStudentId` from the student's profile, the server allows any authenticated student to query any student ID (see `BOLA_TESTING.md`).

### 4.2 Credential Persistence
- **Question:** Does the Android application unnecessarily store the ERP password?
- **Finding:** **YES**. As documented in Section 2.1, `AppPreferences` stores `password` in plaintext to support auto-relogin. The application should rely strictly on OAuth2 refresh tokens.

### 4.3 Production Logging
- **Question:** Are API responses or student information written to device logs?
- **Finding:** **YES**. `Log.i("ERP_RAW", ...)` logs complete raw monthly and weekly API responses to logcat.

### 4.4 Timetable Origin
- **Question:** Is the app's current timetable data ERP-derived or hardcoded?
- **Finding:** **DUAL ARCHITECTURE**:
  - The ERP backend provides a dynamic schedule endpoint: `GET /api/student/me/weekly-attendance`.
  - The Android app fetches and caches `/weekly-attendance` in `AttendanceSyncWorker` and `ErpApiClient`.
  - **However**, in `MainActivity.java` and `TimetableRepository.java`, the active timetable UI is hardcoded for Section A-1! The UI has not yet been hooked up to dynamically render from the cached `/weekly-attendance` model.

### 4.5 API Coverage vs Implemented Features
- **Attendance Summary:** ERP provides `/monthly-attendance` $\rightarrow$ Fully implemented in Dashboard.
- **Weekly Schedule:** ERP provides `/weekly-attendance` $\rightarrow$ API client implemented; UI currently bound to static repository.
- **Class Logs:** ERP provides `/attendance/student` $\rightarrow$ Fully implemented in Attendance History tab with filters.
- **Other Modules:** Exam marks, notices, assignments, fees, library $\rightarrow$ Zero API endpoints implemented or discovered.
