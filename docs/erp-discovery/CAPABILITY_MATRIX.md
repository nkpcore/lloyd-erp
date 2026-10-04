# Lloyd ERP — Student Capability Matrix

**Classification Standard:**  
- **`✓ Verified`**: Directly observed and confirmed through API traffic and data payloads.  
- **`⚠ Partially verified`**: Supported by backend models or metadata, but partially integrated or hardcoded in current clients.  
- **`? Unknown`**: Typical academic ERP capability, but no endpoint or client implementation exists in the audited student scope.  
- **`✗ Not available`**: Explicitly confirmed as unsupported or restricted from the student role.  

---

## 1. Master Capability Matrix

| Capability Category | Status | API Source | Client Integration Status | Integration Feasibility |
| :--- | :---: | :--- | :--- | :--- |
| **Authentication & Tokens** | **✓ Verified** | `POST /api/auth/login`, `POST /api/auth/refresh` | Fully implemented in Android & Web. | Production Ready |
| **Student Profile Identity** | **✓ Verified** | `user` object in `/api/auth/login` | Implemented in Android Header & User Preferences. | Production Ready |
| **Overall Attendance Stats** | **✓ Verified** | `GET /api/student/me/monthly-attendance` | Rendered on Hero Dashboard card & Widget. | Production Ready |
| **Monthly Attendance Summary**| **✓ Verified** | `GET /api/student/me/monthly-attendance` | Rendered in Dashboard monthly breakdown list. | Production Ready |
| **Weekly Timetable Routine** | **✓ Verified** | `GET /api/student/me/weekly-attendance` | Fully implemented via `TimetableRepository.kt`, Jetpack Compose `ScheduleScreen`, and Widget. | Production Ready |
| **Subject Attendance Analytics**| **✓ Verified** | Aggregated from `/api/attendance/student` | Rendered in Compose Drilldown & Subject Breakdown cards. | Production Ready |
| **Attendance History Ledger**| **✓ Verified** | `GET /api/attendance/student` | Rendered in Compose Ledger with multi-filters & BOLA validation. | Production Ready |
| **Faculty Attribution** | **✓ Verified** | `created_by_name` in `/attendance/student` | Displayed on lecture cards and notification banners. | Production Ready |
| **Classroom Locations** | **✓ Verified** | `room_no` in `/student/me/weekly-attendance` | Displayed on Compose timeline & Widget live indicator. | Production Ready |
| **Bunk & Recovery Planner** | **✓ Verified** | Client-side domain engine | Implemented via Compose slider & What-If Simulation screen. | Production Ready |
| **Home Screen Widgets** | **✓ Verified** | Client-side `AttendanceWidgetProvider` | Native 4x2 Android widget with dynamic live schedule & 15s debounce. | Production Ready |
| **Dynamic Heads-Up Alerts** | **✓ Verified** | `AttendanceChangeDetector.kt` + `NotificationHelper` | Event-driven alerts on authentic Present/Absent teacher markings. | Production Ready |
| **Examinations & Schedules** | **? Unknown** | *None observed* | Not present in codebase. | Dependent on backend discovery |
| **Exam Results & Grades** | **? Unknown** | *None observed* | Not present in codebase. | Dependent on backend discovery |
| **Notices & Bulletins** | **? Unknown** | *None observed* | Not present in codebase. | Dependent on backend discovery |
| **Assignments & Homework** | **? Unknown** | *None observed* | Not present in codebase. | Dependent on backend discovery |
| **Fee Ledgers & Dues** | **? Unknown** | *None observed* | Not present in codebase. | Dependent on backend discovery |
| **Fee Payment Receipts** | **? Unknown** | *None observed* | Not present in codebase. | Dependent on backend discovery |
| **Library Book Tracker** | **? Unknown** | *None observed* | Not present in codebase. | Dependent on backend discovery |
| **Academic Digital Documents**| **? Unknown** | *None observed* | Not present in codebase. | Dependent on backend discovery |
| **Academic Event Calendar** | **? Unknown** | *None observed* | Not present in codebase. | Dependent on backend discovery |
| **Remote Push (FCM / APNs)** | **✗ Not available**| No remote push gateway configured | Implemented locally via `WorkManager` background polling. | Local Polling Architecture |
| **Attendance Modification** | **✗ Not available**| Restricted to faculty (`role_id: 3`) | Strictly prohibited and denied by server (`403 Forbidden`).| Invariant |

---

## 2. In-Depth Verification Notes

1. **Timetable Status (`✓ Verified & Implemented`)**:
   - The ERP server reliably serves `/api/student/me/weekly-attendance`.
   - The data contains real periods, room numbers (e.g. `NB-101`), and teacher names.
   - Fully integrated in Jetpack Compose `ScheduleScreen` with dynamic fallback section parsing and real-time live class tracking in `AttendanceWidgetProvider`.
2. **Push Notifications & Marking Alerts (`✓ Verified & Implemented`)**:
   - The Lloyd ERP does not maintain a Firebase Cloud Messaging (FCM) server key or WebSocket connection for student clients.
   - All notifications in the Android app are generated through pure domain event detection in `AttendanceChangeDetector.kt` inside `AttendanceSyncWorker.java` running on an hourly/15-minute background loop.
   - Strictly dispatches alerts when faculty marks attendance (Present or Absent), with persistent seen ID tracking and bootstrap suppression to prevent notification spam.
