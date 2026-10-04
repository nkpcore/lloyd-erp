# Lloyd ERP Security Assessment — Scope & Authorization Boundary

**Document Reference:** SEC-AUDIT-SCOPE-2026-10  
**Target Hostname:** `erp.lloydcollege.in` (Base API: `https://erp.lloydcollege.in/api`)  
**Target Codebase:** `nkpcore/lloyd-erp` (Local Worktree: `C:\Users\nikhil\Desktop\erpwidgit`)  
**Academic Authorization:** Professor-Authorized Capstone Assessment  

---

## 1. Authorization Boundaries

This assessment strictly operates under defined academic ethical constraints. Permission granted for this evaluation is confined to controlled testing and passive inspection of authorized student workflows.

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        AUTHORIZATION BOUNDARY                          │
├───────────────────────────────────┬────────────────────────────────────┤
│         AUTHORIZED SCOPE          │       STRICTLY EXCLUDED SCOPE      │
├───────────────────────────────────┼────────────────────────────────────┤
│ • Own Lloyd ERP student account   │ • Accessing unrelated student data │
│ • Professor-provided test accounts│ • Enumerating real student IDs     │
│ • Endpoints serving student UI    │ • Dumping student database         │
│ • Normal web/mobile API traffic   │ • Brute-forcing passwords or OTPs  │
│ • Passive inspection of requests  │ • Bypassing MFA / CAPTCHA          │
│ • Controlled A-vs-B BOLA tests    │ • Exploiting server infrastructure │
│ • Repository source code audit    │ • Denial of Service / Stress tests │
│ • Non-destructive configuration QA│ • Altering attendance/marks/fees   │
│                                   │ • Deleting files or audit records  │
│                                   │ • Privilege escalation to Admin    │
└───────────────────────────────────┴────────────────────────────────────┘
```

---

## 2. In-Scope Targets

### 2.1 Remote API Endpoints
The following endpoints served by `https://erp.lloydcollege.in/api` were evaluated:
- `/api/auth/login` (Authentication entrance)
- `/api/auth/refresh` (Session renewal)
- `/api/student/me/monthly-attendance` (Student aggregated statistics)
- `/api/student/me/weekly-attendance` (Student weekly timetable & schedule)
- `/api/attendance/student` (Paginated student attendance ledger)

### 2.2 Client Source Repositories & Artifacts
The full repository tree of `nkpcore/lloyd-erp` was audited across all branches and commits:
- **Android Client**:
  - `AndroidManifest.xml` & `network_security_config.xml`
  - `com.lloyd.attendance.api.ErpApiClient`
  - `com.lloyd.attendance.api.Models`
  - `com.lloyd.attendance.data.AppPreferences`
  - `com.lloyd.attendance.schedule.TimetableRepository`
  - `com.lloyd.attendance.ui.LoginActivity` & `MainActivity`
  - `com.lloyd.attendance.widget.AttendanceSyncWorker`, `AttendanceWidgetProvider`, `NotificationHelper`
  - Build scripts (`build.gradle`, `proguard-rules.pro`, GitHub Actions workflows)
- **Web / PWA Client**:
  - `web/index.html` (Original and compiled Vite bundles)
  - `web/assets/index-DhavIe_O.js` (Compiled frontend SPA bundle)
  - `web/sw.js` (Service Worker cache policies)
- **iOS Scriptable Widget**:
  - `LloydWidget.scriptable.js`

---

## 3. Testing Principles & Rules of Engagement

1. **Controlled Object Authorization Testing**:
   - Authorization testing must not query random student identifiers.
   - Cross-account tests are strictly executed between authorized test identities:
     - **Test Identity A (`<TEST_STUDENT_A>`)**: Authenticated session requesting own records vs requesting records belonging to Identity B.
     - **Test Identity B (`<TEST_STUDENT_B>`)**: Pre-registered authorized peer test account.
2. **Minimal Evidence Handling**:
   - If an endpoint improperly allows access to peer data, do not extract full rosters.
   - Record only minimal response headers and sanitized metadata proving the control failure.
3. **Redaction Protocol**:
   - Live tokens must be redacted as `<REDACTED_TOKEN>` or `<REDACTED_JWT>`.
   - Real admission numbers, names, and passwords must be scrubbed from all project artifacts.
4. **No Destructive Operations**:
   - All tests must use read-only HTTP methods (`GET`, `POST` for login/refresh).
   - Zero modifications to real academic records.
