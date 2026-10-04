# Lloyd ERP Security Assessment — Web Application Mapping

**Target Application:** Lloyd ERP Student Web Application & Companion PWA  
**Host:** `https://erp.lloydcollege.in`  
**Client Implementations Audited:**  
- Responsive PWA & SPA (`web/index.html`, `web/assets/index-DhavIe_O.js`)  
- Native Android Client (`android/app/src/main/java/com/lloyd/attendance/ui/MainActivity.java`)  
**Status Protocol:** `VERIFIED` (Observed with network traffic), `INFERRED` (Present in UI metadata), `UNOBSERVED / NOT ACCESSIBLE` (No network trace or client implementation)

---

## 1. Observed Student Interface Architecture

The normal authenticated student workflow consists of an authentication gateway followed by a consolidated 4-module portal interface:

```text
                               ┌─────────────────────────┐
                               │   Login Gateway Screen  │
                               │  - Username (Admission) │
                               │  - Password Field       │
                               │  - Device Metadata      │
                               └────────────┬────────────┘
                                            │ POST /auth/login
                                            ▼
                               ┌─────────────────────────┐
                               │   Authenticated Shell   │
                               │  - Student Identity Bar │
                               │  - Connectivity Dot     │
                               │  - Stealth Toggle       │
                               │  - On-Demand Refresh    │
                               │  - Logout Trigger       │
                               └────────────┬────────────┘
                                            │
         ┌──────────────────┬───────────────┴───────────────┬──────────────────┐
         ▼                  ▼                               ▼                  ▼
┌─────────────────┐┌─────────────────┐             ┌─────────────────┐┌─────────────────┐
│ Module 1:       ││ Module 2:       │             │ Module 3:       ││ Module 4:       │
│ Dashboard       ││ Schedule        │             │ Attendance Log  ││ Simulator &     │
│ - Hero Card     ││ - Day Chips     │             │ - Status Filter ││   Analytics     │
│ - Present/Absent││ - Period Slots  │             │ - Month Chips   ││ - Bunk Slider   │
│ - Live Glance   ││ - Room & Teacher│             │ - Subject Chips ││ - 75/80% Goals  │
│ - Monthly List  ││ - Live Pulse    │             │ - Date Grouping ││ - Subject Cards │
└─────────────────┘└─────────────────┘             └─────────────────┘└─────────────────┘
```

---

## 2. Detailed Functional Feature Map

### 2.1 Authentication & Session Gateway
- **URL / Screen**: `/auth/login` (Web SPA & Android `LoginActivity`)
- **UI Elements**:
  - `et_username`: Plaintext input for student admission number (e.g. `2023LLOYD1234`).
  - `et_password`: Masked password input field.
  - `btn_login`: Primary submission trigger.
  - `btn_biometric_login`: Hardware biometric shortcut triggering cached credential submission.
- **Observed Network Activity**:
  - `POST /api/auth/login`
  - Payload: `{ username, password, device_id, app_version, timezone, browser_name, os_name, device_type }`
  - Response: Issues bearer JWT `access_token`, `refresh_token`, and student enrollment object `user`.
- **Status**: **VERIFIED**

### 2.2 Global App Bar & Telemetry Controls
- **UI Elements**:
  - `tv_main_student_name`: Displays student legal name from `user.name`.
  - `tv_main_status_tag`: Connectivity indicator (`Live`, `Syncing`, `Cached`, `Offline`).
  - `btn_main_stealth`: Privacy toggle masking percentage and name for shoulder-surfing protection.
  - `btn_main_sync`: Manual on-demand network refresh button.
  - `btn_main_logout`: Revokes local session tokens and returns to login.
- **Observed Network Activity**: Triggers parallel fetch of monthly, weekly, and ledger endpoints.
- **Status**: **VERIFIED**

### 2.3 Module 1: Dashboard
- **UI Elements**:
  - Hero Card: Circular progress ring, animated percentage label, safe/shortage badge pill.
  - Metric Summary Tiles: 3-column container displaying Present count, Absent count, and Total conducted classes.
  - Live Classroom Glance: Evaluates current wall-clock time against timetable to display active ongoing lecture or next scheduled lecture.
  - Monthly Attendance List: Vertical list of past calendar months with individual percentage progress bars.
- **Observed Network Activity**:
  - `GET /api/student/me/monthly-attendance`
  - Returns `months` array containing aggregated class metrics.
- **Status**: **VERIFIED**

### 2.4 Module 2: Schedule & Timetable
- **UI Elements**:
  - Horizontal Day Chips: Single-select filters for `Mon`, `Tue`, `Wed`, `Thu`, `Fri`, `Sat`.
  - Period Timeline Cards: Displays period number, start time, end time, subject title, faculty name, and assigned classroom (e.g. `NB-101`).
  - Special Period Badges: Highlighting for `Lunch Break` and `Weekly Test`.
- **Observed Network Activity**:
  - Upstream ERP API: `GET /api/student/me/weekly-attendance` (Delivers weekly routines).
  - Current Android Client State: Employs a hardcoded Section A-1 fallback table in `TimetableRepository.java` while syncing `/weekly-attendance` in the background.
- **Status**: **VERIFIED (API) / PARTIALLY IMPLEMENTED (Android UI)**

### 2.5 Module 3: Attendance History & Class Ledger
- **UI Elements**:
  - Status Filter Pills: `All (Total)`, `Present (Count)`, `Absent (Count)`.
  - Month Chips: Horizontally scrolling chips for individual months (e.g., `Oct 2026`, `Sep 2026`).
  - Subject Chips: Course-specific filtering chips.
  - Search Input (`card_search_box`): Real-time keyword filter across subject and teacher names.
  - Chronological Date Cards: Date headers with lecture summary counts (`2 Lectures • 0P / 2A`), lecture period tags, faculty name, and timestamp.
- **Observed Network Activity**:
  - `GET /api/attendance/student?student_id={id}&page={page}&page_size=100&sort_by=attendance_date&sort_dir=desc`
  - Paginated fetch retrieving complete semester lecture transactions.
- **Status**: **VERIFIED**

### 2.6 Module 4: Bunk Simulator & Subject Analytics
- **UI Elements**:
  - Target Threshold Buttons: Quick selection for `75% (Mandatory)`, `80% (Safety)`, `85% (Scholarship)`, `90% (Honors)`.
  - Interactive Delta Slider: Range from `-15` (missed) to `+30` (attended) classes.
  - Dynamic Advice Note: Informs student of exact consecutive classes needed or safe bunk margin.
  - Subject Breakdown Cards: Lists every enrolled course with individual attendance percentage, class counts, and safe margin.
  - Subject Details Dialog (`dialog_subject_details.xml`): Bottom sheet showing subject-specific history.
- **Observed Network Activity**: Synthesized locally from `/api/attendance/student` and `/api/student/me/monthly-attendance` data models; no separate remote endpoint required.
- **Status**: **VERIFIED**

---

## 3. ERP Student Capability Mapping Matrix

To strictly satisfy Section 25 (*Never turn UNKNOWN into VERIFIED and never invent an endpoint*), the following audit table records every potential academic ERP feature:

| ERP Capability | Observed in Client? | Remote API Endpoint | Network Traffic Verified? | Status Classification |
| :--- | :--- | :--- | :--- | :--- |
| **Authentication** | Yes | `/api/auth/login` | Yes (HTTP 200) | **VERIFIED** |
| **Session Refresh** | Yes | `/api/auth/refresh` | Yes (HTTP 200) | **VERIFIED** |
| **Student Profile** | Yes | Included in `/api/auth/login` response | Yes (`data.user`) | **VERIFIED** |
| **Monthly Attendance** | Yes | `/api/student/me/monthly-attendance` | Yes (HTTP 200) | **VERIFIED** |
| **Weekly Timetable** | Yes | `/api/student/me/weekly-attendance` | Yes (HTTP 200) | **VERIFIED** |
| **Attendance Ledger** | Yes | `/api/attendance/student` | Yes (HTTP 200) | **VERIFIED** |
| **Faculty Names** | Yes | Embedded in weekly & log responses | Yes (`created_by_name`, `teacher_name`) | **VERIFIED** |
| **Classroom Locations**| Yes | Embedded in weekly routine response | Yes (`room_no`) | **VERIFIED** |
| **Examinations** | No | *None observed* | No | **UNOBSERVED / UNKNOWN** |
| **Results / Marks** | No | *None observed* | No | **UNOBSERVED / UNKNOWN** |
| **Notices & Bulletins**| No | *None observed* | No | **UNOBSERVED / UNKNOWN** |
| **Assignments** | No | *None observed* | No | **UNOBSERVED / UNKNOWN** |
| **Fee Ledgers & Dues** | No | *None observed* | No | **UNOBSERVED / UNKNOWN** |
| **Fee Receipts** | No | *None observed* | No | **UNOBSERVED / UNKNOWN** |
| **Library Catalog** | No | *None observed* | No | **UNOBSERVED / UNKNOWN** |
| **Digital Documents** | No | *None observed* | No | **UNOBSERVED / UNKNOWN** |
| **Academic Calendar** | No | *None observed* | No | **UNOBSERVED / UNKNOWN** |
| **Push Notifications** | No | Handled locally via `AttendanceSyncWorker` | No remote push gateway (FCM) | **LOCAL ONLY** |
| **Account Settings** | Yes | Local stealth & notification toggles | Local SharedPreferences only | **LOCAL ONLY** |
