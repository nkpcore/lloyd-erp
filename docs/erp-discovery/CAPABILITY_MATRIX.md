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
| **Weekly Timetable Routine** | **✓ Verified** | `GET /api/student/me/weekly-attendance` | API fetched; Android UI currently uses hardcoded Section A-1 fallback. | High (Requires UI hookup) |
| **Subject Attendance Analytics**| **✓ Verified** | Aggregated from `/api/attendance/student` | Rendered in Tab 4 Subject Breakdown cards. | Production Ready |
| **Attendance History Ledger**| **✓ Verified** | `GET /api/attendance/student` | Rendered in Tab 3 Chronological list with multi-filters. | Production Ready |
| **Faculty Attribution** | **✓ Verified** | `created_by_name` in `/attendance/student` | Displayed on lecture cards and notification banners. | Production Ready |
| **Classroom Locations** | **✓ Verified** | `room_no` in `/student/me/weekly-attendance` | Available in weekly schedule DTOs. | High |
| **Bunk & Recovery Planner** | **✓ Verified** | Client-side domain engine | Implemented via continuous seekbar simulator in Tab 4. | Production Ready |
| **Home Screen Widgets** | **✓ Verified** | Client-side `AttendanceWidgetProvider` | Native 4x2 Android widget with on-demand refresh. | Production Ready |
| **Dynamic Heads-Up Alerts** | **✓ Verified** | Client-side `NotificationHelper` | Apple Dynamic Island-styled RemoteViews banner. | Production Ready |
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

1. **Timetable Status (`⚠ Partially verified`)**:
   - The ERP server reliably serves `/api/student/me/weekly-attendance`.
   - The data contains real periods, room numbers (e.g. `NB-101`), and teacher names.
   - However, because `MainActivity.java` currently renders from `TimetableRepository.java` (which has hardcoded Section A-1 slots), this capability is classified as partially verified until the UI layer is connected directly to the remote DTO.
2. **Push Notifications (`✗ Not available`)**:
   - The Lloyd ERP does not maintain a Firebase Cloud Messaging (FCM) server key or WebSocket connection for student clients.
   - All notifications in the Android app are generated through a local diff engine inside `AttendanceSyncWorker.java` running on an hourly/15-minute background loop.
