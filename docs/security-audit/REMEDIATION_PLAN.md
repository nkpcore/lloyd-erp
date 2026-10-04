# Lloyd ERP Security Assessment — Comprehensive Remediation Plan

**Target Systems:** Lloyd ERP API (`https://erp.lloydcollege.in`) & Android Application (`nkpcore/lloyd-erp`)  
**Scope:** Actionable, engineering-level remediation blueprints for backend, infrastructure, and mobile client teams.  

---

## 1. ERP Backend Remediation Blueprint

### 1.1 Remediation for BOLA (SEC-FINDING-001)
- **Action**: Deprecate client-supplied query parameter `?student_id={id}` for student callers.
- **Implementation Strategy**:
  1. Route all student ledger queries to a session-bound endpoint: `GET /api/student/me/attendance-logs`.
  2. In the controller, extract `student_id` directly from the authenticated JWT session claim:
     ```python
     # Secure Controller Pattern
     @api_view(['GET'])
     @permission_classes([IsAuthenticated])
     def get_student_attendance(request):
         # Identity is extracted strictly from the validated Bearer token
         auth_student_id = request.user.profile_id
         
         # Query strictly bound to authenticated student
         records = AttendanceRecord.objects.filter(student_id=auth_student_id).order_by('-attendance_date')
         return Response(AttendanceSerializer(records, many=True).data)
     ```
  3. If `/api/attendance/student` must remain active for administrative and faculty roles, enforce an explicit role gate:
     ```python
     if request.user.role == 'student' and int(request.GET.get('student_id')) != request.user.profile_id:
         return Response({"error": "Forbidden: You cannot view peer attendance."}, status=403)
     ```

### 1.2 Remediation for Server-Side Logout (SEC-FINDING-005)
- **Action**: Implement token invalidation endpoint `POST /api/auth/logout`.
- **Implementation Strategy**:
  1. Store the JWT token's unique identifier (`jti`) or token hash in a short-lived Redis denylist for the remaining duration of its `expires_in` window.
  2. Reject any future requests carrying a blacklisted token with `HTTP 401 Unauthorized`.

### 1.3 Remediation for CORS Misconfiguration (SEC-FINDING-007)
- **Action**: Replace `Access-Control-Allow-Origin: *` with an explicit trusted origin whitelist:
  ```nginx
  # Nginx Configuration
  map $http_origin $cors_header {
      default "";
      "https://erp.lloydcollege.in" "$http_origin";
  }
  add_header Access-Control-Allow-Origin $cors_header always;
  ```

---

## 2. Android Client Remediation Blueprint (`nkpcore/lloyd-erp`)

### 2.1 Complete Purge of Plaintext Password Persistence (SEC-FINDING-002)
- **Target File:** `android/app/src/main/java/com/lloyd/attendance/data/AppPreferences.java`
- **Action:**
  1. Delete `KEY_PASSWORD` and remove `saveCredentials(String username, String password)`.
  2. Implement a dedicated `saveUsername(String username)` method.
  3. Delete the insecure fallback branch that writes unencrypted XML. If Keystore fails, prompt user authentication per session.
- **Target File:** `android/app/src/main/java/com/lloyd/attendance/api/ErpApiClient.java`
  - In `ensureValidToken()`, rely strictly on `refreshToken()`.
  - When refresh fails (HTTP 401), trigger an observable session expiry callback that routes the user to `LoginActivity`, rather than silently attempting to log in with cached raw credentials.

### 2.2 Telemetry Scrubbing & R8 Obfuscation (SEC-FINDING-004)
- **Target File:** `android/app/build.gradle`
  - Enable minification and resource shrinking for release builds:
    ```groovy
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    ```
- **Target File:** `android/app/src/main/java/com/lloyd/attendance/api/ErpApiClient.java`
  - Remove all `android.util.Log.i("ERP_RAW", ...)` calls from networking interceptors.

### 2.3 Hardening Exported Broadcast Receiver (SEC-FINDING-006)
- **Target File:** `android/app/src/main/AndroidManifest.xml`
  - Declare a custom signature permission to restrict `ACTION_REFRESH_WIDGET` to the app:
    ```xml
    <permission
        android:name="com.lloyd.attendance.permission.WIDGET_REFRESH"
        android:protectionLevel="signature" />

    <receiver
        android:name=".widget.AttendanceWidgetProvider"
        android:exported="true"
        android:permission="com.lloyd.attendance.permission.WIDGET_REFRESH">
        <intent-filter>
            <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            <action android:name="com.lloyd.attendance.ACTION_REFRESH_WIDGET" />
        </intent-filter>
    </receiver>
    ```

---

## 3. Retesting & Verification Checklist

| Checkpoint | Verification Command / Procedure | Acceptance Gate |
| :--- | :--- | :--- |
| **BOLA Cross-Access** | `GET /api/attendance/student?student_id={B}` with Token A | `HTTP 403 Forbidden` |
| **Password Purge** | Search app sandbox XML on disk for string `password` | 0 occurrences |
| **R8 Obfuscation** | Inspect compiled release APK via `dexdump` or APK Analyzer | ProGuard rules applied; `Log.i` absent |
| **IPC Protection** | Execute `adb shell am broadcast -a com.lloyd.attendance.ACTION_REFRESH_WIDGET` from untrusted UID | SecurityException / Ignored |
