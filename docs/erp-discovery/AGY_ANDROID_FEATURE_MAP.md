# AGY Android Feature Mapping Specification — Lloyd Student

**Target Application:** Lloyd Student Android Application (`nkpcore/lloyd-erp`)  
**Package:** `com.lloyd.attendance`  
**Architecture:** Offline-First MVVM with Room Database, Kotlin Coroutines/Flow, WorkManager, and Material Design 3  

---

## 1. ERP-to-Android Feature Implementation Matrix

| ERP Capability | Upstream Source | Android Target Component | Priority | Local Storage Layer | Sync Cadence | Offline Ready? | Status in Codebase |
| :--- | :--- | :--- | :---: | :--- | :--- | :---: | :--- |
| **Authentication** | `POST /api/auth/login` | `LoginActivity` / AuthGate | **P0** | `EncryptedSharedPreferences` | On-demand / 24h | No | **Integrated** |
| **Silent Refresh** | `POST /api/auth/refresh` | `TokenRefreshInterceptor` | **P0** | `EncryptedSharedPreferences` | On HTTP 401 | No | **Integrated** |
| **Dashboard Hero** | `/student/me/monthly-attendance` | Tab 1: Hero Progress Ring | **P0** | Room: `MonthlyAttendanceEntity` | Hourly / On-launch | **Yes** | **Integrated** |
| **Attendance Metrics**| `/student/me/monthly-attendance` | Tab 1: Metric Summary Tiles | **P0** | Room: `AttendanceStatsEntity` | Hourly / On-launch | **Yes** | **Integrated** |
| **Active Lecture** | `/student/me/weekly-attendance` | Tab 1: Live Class Glance | **P0** | Room: `TimetableEntity` | Daily / On-launch | **Yes** | **Integrated** |
| **Weekly Timetable**| `/student/me/weekly-attendance` | Tab 2: Day Timeline Cards | **P0** | Room: `TimetableEntity` | Daily (06:00 AM) | **Yes** | **Partially Integrated** (Uses A-1 fallback) |
| **Attendance Ledger**| `/api/attendance/student` | Tab 3: History Ledger Stream | **P0** | Room: `AttendanceRecordEntity` | Hourly / Background | **Yes** | **Integrated** (Unsafe query param) |
| **Ledger Filtering**| None (Local Room Query) | Tab 3: Chip & Keyword Filter | **P1** | Room Indexed Query | Instant Local | **Yes** | **Integrated** |
| **Subject Analytics**| Computed from Ledger | Tab 4: Course Cards | **P1** | Room Aggregation View | Instant Local | **Yes** | **Integrated** |
| **Bunk Simulator** | Client Mathematical Engine | Tab 4: 75% / 80% Slider | **P1** | Domain Model Logic | Instant Local | **Yes** | **Integrated** |
| **Heads-Up Banner** | Background Diff Engine | `NotificationHelper` | **P0** | Room Diff Entity | 15-min Worker | **Yes** | **Integrated** |
| **Home Widget** | Cached Room / Prefs Data | `AttendanceWidgetProvider` | **P0** | `CalculatedStats` | 15-min / Tap | **Yes** | **Integrated** |
| **Stealth Mode** | Local UI State | Top Bar Privacy Toggle | **P2** | In-Memory Flow | Instant | **Yes** | **Integrated** |

---

## 2. Capability Integration Categories

### 2.1 Already Integrated (Production Ready)
* **Hero Dashboard & Metrics:** Overall percentage, attended count, total conducted classes, and monthly bar breakdowns.
* **Attendance Ledger with Multi-Filters:** Chronological history cards displaying teacher names, dates, lecture slots, and search filters.
* **Bunk & Goal Planner:** Mathematical simulation for attendance targets (75% mandatory, 80% safe) without server dependency.
* **Home Screen Widget:** 4x2 interactive RemoteViews widget displaying real-time metrics and dynamic refresh triggers.
* **Heads-Up Alert Notifications:** Local diffing engine in `AttendanceSyncWorker` firing alerts upon detecting newly marked lectures.

### 2.2 Partially Integrated (Action Required)
* **Weekly Schedule:** Upstream `/student/me/weekly-attendance` is polled, but the UI currently displays a hardcoded timetable table in `TimetableRepository.java`. Action: Connect dynamic DTO response to Tab 2 timeline adapters.

### 2.3 Unsafe Implementation (Security Remediation Required)
* **Client-Supplied `student_id`:** `ErpApiClient.java` passes a query parameter `student_id` on `/api/attendance/student`. Action: Switch to session-bound route or enforce strict identity matching.
* **Plaintext Password Storage:** `AppPreferences.java` caches user password to attempt auto-relogin. Action: Delete `KEY_PASSWORD` and rely exclusively on `/api/auth/refresh`.

### 2.4 Not Available (Blocked by Missing ERP Capabilities)
* Exam Schedules, Marks/Results, Campus Bulletins, Homework/Assignments, Fee Ledgers, and Library Records. Action: Do not create fake or mock data; present clean empty states or omit tabs.
