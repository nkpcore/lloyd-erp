# AGY Capability Matrix — Lloyd ERP Student Feature Surface

**Target System:** `https://erp.lloydcollege.in`  
**Classification Protocol:**
- `VERIFIED`: Directly observed with confirmed HTTP network transactions and response payloads.
- `PARTIALLY VERIFIED`: Supported by backend API models but partially implemented or using static fallback in client UI.
- `NOT EXPOSED`: Typical collegiate feature that is absent from the student REST API surface.
- `RESTRICTED`: Capability reserved for elevated roles (Faculty `3` / Admin `1`, `2`) and properly denied to students.

---

## 1. Master Capability Matrix

| Capability Category | Status | API Source | Android Integration Status | Feasibility / Production Strategy |
| :--- | :---: | :--- | :--- | :--- |
| **Authentication & Session** | `VERIFIED` | `POST /api/auth/login`, `POST /api/auth/refresh` | Fully Integrated (`ErpApiClient`) | Production Ready |
| **Student Identity Profile** | `VERIFIED` | `data.user` in `/api/auth/login` | Fully Integrated (Header Bar) | Production Ready |
| **Overall Attendance Stats** | `VERIFIED` | `GET /api/student/me/monthly-attendance` | Fully Integrated (Dashboard Hero) | Production Ready |
| **Monthly Attendance Summary**| `VERIFIED` | `GET /api/student/me/monthly-attendance` | Fully Integrated (Dashboard List) | Production Ready |
| **Weekly Schedule / Timetable**| `PARTIALLY VERIFIED` | `GET /api/student/me/weekly-attendance` | Partially Integrated (Uses Section A-1 fallback) | High (Hook up remote DTO directly to UI) |
| **Class Attendance Ledger** | `VERIFIED` | `GET /api/attendance/student` | Fully Integrated (`HistoryAdapter`) | Production Ready (Requires BOLA fix) |
| **Subject Attendance Analytics**| `VERIFIED` | Computed from Attendance Ledger | Fully Integrated (Tab 4 Cards) | Production Ready |
| **Faculty Attribution** | `VERIFIED` | `created_by_name` in Ledger | Fully Integrated on Lecture Cards | Production Ready |
| **Classroom Locations** | `VERIFIED` | `room_no` in `/weekly-attendance` | Available in DTO | High (Expose on schedule timeline) |
| **Bunk & Recovery Planner** | `VERIFIED` | Client mathematical engine | Fully Integrated in Tab 4 | Production Ready |
| **Home Screen Widgets** | `VERIFIED` | Client `AttendanceWidgetProvider` | Fully Integrated (4x2 widget) | Production Ready |
| **Heads-Up Alert Banners** | `VERIFIED` | Client `NotificationHelper` | Fully Integrated (Dynamic Island style) | Production Ready |
| **Exam Schedules** | `NOT EXPOSED` | *No endpoint on student API* | Not Implemented | Blocked by ERP |
| **Marks & Exam Results** | `NOT EXPOSED` | *No endpoint on student API* | Not Implemented | Blocked by ERP |
| **Campus Bulletins / Notices**| `NOT EXPOSED` | *No endpoint on student API* | Not Implemented | Blocked by ERP |
| **Course Assignments** | `NOT EXPOSED` | *No endpoint on student API* | Not Implemented | Blocked by ERP |
| **Fee Ledgers & Dues** | `NOT EXPOSED` | *No endpoint on student API* | Not Implemented | Blocked by ERP |
| **Fee Payment Receipts** | `NOT EXPOSED` | *No endpoint on student API* | Not Implemented | Blocked by ERP |
| **Library Management** | `NOT EXPOSED` | *No endpoint on student API* | Not Implemented | Blocked by ERP |
| **Academic Digital Documents**| `NOT EXPOSED` | *No endpoint on student API* | Not Implemented | Blocked by ERP |
| **Server-Driven Push (FCM)** | `NOT EXPOSED` | *No push gateway configured* | Local `WorkManager` background diff | Production Ready via Local Polling |
| **Attendance Modification** | `RESTRICTED` | `/api/attendance/mark` | Prohibited for student role | Intended security boundary (`403`) |

---

## 2. Capability Integration Analysis

### 2.1 Dynamic Timetable Integration Pathway
The Lloyd ERP exposes real weekly class slots, lecture times, teacher assignments, and classroom identifiers (e.g., `NB-101`) via `GET /api/student/me/weekly-attendance`. The Android client currently fetches this in the background, but displays a hardcoded table from `TimetableRepository.java`. Connecting the UI directly to the verified API model will enable dynamic schedules for all semesters and sections without manual updates.

### 2.2 Local Polling vs. Remote Push
Because the ERP has no Firebase Cloud Messaging (FCM) integration, real-time alerts are achieved cleanly using Android's `WorkManager` periodically querying `/api/student/me/monthly-attendance` and `/api/attendance/student`. A local diffing algorithm detects newly recorded classes and fires high-priority notifications.
