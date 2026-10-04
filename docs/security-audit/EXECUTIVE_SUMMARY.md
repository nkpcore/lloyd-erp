# Lloyd ERP API Security Assessment — Executive Summary

**Target System:** Lloyd ERP (`https://erp.lloydcollege.in`)  
**Assessment Framework:** OWASP API Security Top 10 (2023) & Mobile Application Security (MASVS)  
**Target Repository:** `nkpcore/lloyd-erp`  
**Assessment Period:** October 2026  
**Auditor Classification:** Authorized Academic Security Evaluation  

---

## 1. Engagement Overview

This engagement was conducted under professor-authorized parameters to assess the API attack surface, authentication/authorization boundaries, and data exposure mechanisms of the student portal powering Lloyd College (`erp.lloydcollege.in`), as well as the client architecture implemented in the `nkpcore/lloyd-erp` repository across Android, Web/PWA, and iOS Scriptable clients.

The primary objectives were:
1. Map the authenticated student API surface and data structures exposed by the Lloyd ERP.
2. Evaluate adherence to the **OWASP API Security Top 10 (2023)**.
3. Rigorously analyze Broken Object Level Authorization (BOLA / IDOR) risks.
4. Assess client-side security (credential storage, Android Keystore, logging, inter-process communication).
5. Catalog legitimate, verified ERP capabilities to inform the production Android roadmap with zero mock data.

---

## 2. High-Level Posture Summary

The Lloyd ERP backend provides a structured REST/JSON API serving authentication, monthly summary statistics, weekly timetable routines, and class-by-class attendance transaction logs. Authentication utilizes JSON Web Tokens (JWT) with bearer authorization headers.

However, the architecture exhibits distinct architectural vulnerabilities and security misconfigurations across both the remote API surface and client implementations:

| Threat Category | Finding Reference | Severity | Description |
| :--- | :--- | :--- | :--- |
| **API1:2023 BOLA** | FINDING-01 | **CRITICAL** | Client-supplied `student_id` in `/api/attendance/student?student_id={id}` decouples identity from the authenticated JWT session. |
| **Client Storage** | FINDING-02 | **HIGH** | Plaintext password persistence in `AppPreferences` with unencrypted XML fallback on custom ROMs. |
| **Data Exposure** | FINDING-03 | **MEDIUM** | Excessive verbose logging of raw student rosters and JSON dumps in production Logcat. |
| **API4:2023 Resource** | FINDING-04 | **MEDIUM** | Exported `AttendanceWidgetProvider` broadcast receiver without signature permissions permits forced sync triggers. |
| **Configuration** | FINDING-05 | **LOW** | R8/ProGuard code minification and log stripping disabled in `release` build type (`minifyEnabled false`). |
| **Scriptable Client** | FINDING-06 | **HIGH** | iOS Scriptable script prompts users to hardcode admission number and password in plaintext code. |

---

## 3. Verified API Surface at a Glance

| Endpoint | Method | Authentication | Scope & Binding | Status |
| :--- | :--- | :--- | :--- | :--- |
| `/api/auth/login` | POST | None (Public) | Authenticates student, issues JWT & Refresh Token | **VERIFIED** |
| `/api/auth/refresh` | POST | Refresh Token | Exchanges refresh token for new access token | **VERIFIED** |
| `/api/student/me/monthly-attendance` | GET | Bearer JWT | Binds to session via `/me`; returns aggregated monthly stats | **VERIFIED** |
| `/api/student/me/weekly-attendance` | GET | Bearer JWT | Binds to session via `/me`; returns daily schedule routine & attendance | **VERIFIED** |
| `/api/attendance/student` | GET | Bearer JWT | Requires `student_id` query param; returns class ledger logs | **VERIFIED** |

---

## 4. Key Recommendations

1. **Enforce Server-Side Identity Binding (BOLA Remediation)**:
   - Deprecate client-supplied query parameter `?student_id={id}` on `/api/attendance/student`.
   - Migrate to `/api/student/me/attendance` where student identity is exclusively resolved from the validated `sub` claim of the verified JWT.
2. **Eliminate Password Persistence in Android**:
   - Cease storing plaintext student passwords in `AppPreferences`.
   - Rely strictly on the `/api/auth/refresh` lifecycle. When refresh tokens expire (HTTP 401), prompt user re-authentication cleanly.
3. **Purge Production Telemetry**:
   - Strip all `Log.i("ERP_RAW", ...)` calls from networking interceptors and enable R8/ProGuard `-assumenosideeffects` stripping in `build.gradle`.
4. **Harden Broadcast Receivers**:
   - Protect `ACTION_REFRESH_WIDGET` with signature-level custom permissions or verify calling package UID.
5. **Modernize Client Architecture**:
   - Transition Android codebase to Room DB local caching, Kotlin Coroutines, and clean MVVM to decouple presentation from networking.
