# Lloyd ERP — Android Feature Mapping Specification

**Target Application:** Native Android Client (`nkpcore/lloyd-erp`)  
**Architecture:** Offline-First Repository Pattern with Room DB, WorkManager, and MD3 Expressive  

---

## 1. Feature Mapping Matrix

| Capability | Verified? | Android Feature Component | Priority | Required API | Local Entity | Sync Frequency | Sensitive? | Offline? | Widget? | Notif? |
| :--- | :---: | :--- | :---: | :--- | :--- | :--- | :---: | :---: | :---: | :---: |
| **Authentication** | ✓ | `LoginActivity` / Splash Gate | **P0** | `POST /auth/login` | `AppPreferences` (Encrypted) | On-demand / 24h refresh | Yes | No | No | No |
| **Silent Refresh** | ✓ | `TokenRefreshInterceptor` | **P0** | `POST /auth/refresh` | `AppPreferences` (Encrypted) | On HTTP 401 | Yes | No | No | No |
| **Hero Attendance**| ✓ | Tab 1: Hero Card & Stats Ring | **P0** | `/student/me/monthly-attendance` | `MonthlyAttendanceEntity` | 15 mins / On-demand | No | Yes | Yes | Yes |
| **Metric Tiles** | ✓ | Tab 1: Present / Absent / Total | **P0** | `/student/me/monthly-attendance` | `AttendanceStatsEntity` | 15 mins / On-demand | No | Yes | Yes | No |
| **Live Glance** | ✓ | Tab 1: Active Lecture Card | **P0** | `/student/me/weekly-attendance` | `TimetableRoutineEntity` | Daily / On-launch | No | Yes | Yes | No |
| **Timetable** | ✓ | Tab 2: Day Schedule Timeline | **P0** | `/student/me/weekly-attendance` | `TimetableRoutineEntity` | Weekly / Daily | No | Yes | No | No |
| **Attendance Logs**| ✓ | Tab 3: Chronological History | **P0** | `/attendance/student` | `AttendanceRecordEntity` | 15 mins / On-demand | Yes | Yes | No | Yes |
| **Search & Filters**| ✓ | Tab 3: Multi-Criteria Filter Bar| **P1** | None (Computed from Room) | `AttendanceRecordEntity` | Instant local | No | Yes | No | No |
| **Subject Analytics**| ✓ | Tab 4: Subject Card List | **P1** | Aggregated from Records | Derived Room Query | Instant local | No | Yes | No | No |
| **Bunk Simulator** | ✓ | Tab 4: Interactive Goal Slider | **P1** | None (Pure Domain Engine) | Pure Domain Model | Instant local | No | Yes | Yes | No |
| **Subject Details**| ✓ | Bottom Sheet Dialog | **P1** | `/attendance/student` | `AttendanceRecordEntity` | Instant local | No | Yes | No | No |
| **Dynamic Alert** | ✓ | Heads-Up Dynamic Island Banner | **P0** | Diff in `AttendanceSyncWorker` | `SyncMetadataEntity` | 15 mins (Background) | Yes | Yes | No | Yes |
| **Home Widget** | ✓ | 4x2 / 2x2 AppWidgetProvider | **P0** | Cached Stats in Room / Prefs | `CalculatedStats` | 15 mins / Tap refresh | No | Yes | Yes | No |
| **Report Export** | ✓ | Sharesheet Text Summary | **P2** | Formatted from Room DB | Local String Builder | On-demand | Yes | Yes | No | No |
| **Stealth Mode** | ✓ | Privacy Eye Masking Toggle | **P2** | Local UI State | UI State Flow | Instant | No | Yes | No | No |

---

## 2. Priority Definition & Engineering Milestones

### Priority 0 (Mandatory Core)
- **Zero-Friction Authentication**: Store tokens in Android Keystore, purge password plaintext, handle silent `/auth/refresh` recovery.
- **Hero Dashboard & Bunk Buffer**: Instantly paint overall percentage and bunk safety advice from local Room database.
- **Dynamic Weekly Timetable**: Migrate Tab 2 from hardcoded Section A-1 in `TimetableRepository` to real periods fetched from `/student/me/weekly-attendance`.
- **Class-by-Class Attendance Ledger**: Infinite scroll of attendance history with real faculty names and lecture numbers.
- **Home Screen Widget**: High-performance RemoteViews widget showing percentage, bunk buffer, and live period info.

### Priority 1 (High-Value Academic Intelligence)
- **Multi-Dimension Search & Filter**: Real-time keyword filter across courses and faculty; filter by month chips and present/absent pills.
- **Subject-Specific Analytics**: Horizontal progress indicators with subject-level bunk and recovery counters.
- **Interactive Multi-Target Simulator**: Custom goals (75% mandatory, 80% safety, 85% scholarship, 90% honors).

### Priority 2 (Convenience & Polish)
- **Attendance Report Sharing**: Generates clean markdown or formatted text summaries of attendance to share with parents or mentors.
- **Stealth / Privacy Mode**: Instantly obscures name and attendance percentages in crowded lecture halls.
