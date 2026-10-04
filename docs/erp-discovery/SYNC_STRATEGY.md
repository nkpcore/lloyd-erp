# Lloyd ERP — Offline-First Synchronization & Caching Strategy

**Target Platform:** Android (Java / Kotlin / Android Jetpack WorkManager)  
**Core Directive:** Zero UI lag, 100% real data, sub-30ms startup paint from disk, transparent background sync.  

---

## 1. Architectural Philosophy: Cache-Then-Network

Campus classrooms are notoriously prone to signal dropouts, dead zones, and saturated Wi-Fi access points. An academic app that blocks UI rendering behind a spinner will cause severe frustration.

The Lloyd Student client enforces a strict **Cache-Then-Network** unidirectional data pipeline:

```text
                                App Launch (Cold / Warm)
                                           │
                                           ▼
                           ┌───────────────────────────────┐
                           │      AttendanceRepository     │
                           └───────────────┬───────────────┘
                                           │
                      ┌────────────────────┴────────────────────┐
                      ▼                                         ▼
         1. Read Local Disk Cache                  2. Dispatch Remote Reconcile
         (Room DB / Keystore Prefs)                (ErpApiClient async)
                      │                                         │
                      ▼ (< 30ms)                                │
         ┌─────────────────────────┐                            │
         │ Render Instant UI State │                            │
         │ Tag: ● Cached / Offline │                            │
         └─────────────────────────┘                            │
                      ▲                                         ▼
                      │                             Network Arrives (HTTP 200)
                      │                                         │
                      │                                         ▼
                      │                            3. Update Local Room DB
                      │                                         │
                      │                                         ▼
                      └──────────────────────────── 4. Emit Fresh Live State
                                                    - Smooth rolling number animation
                                                    - Tag: ● Live (Synced)
```

---

## 2. Startup Lifecycle & Latency SLA

1. **Phase 1: Local Paint (< 30 milliseconds)**:
   - `MainActivity.onCreate()` reads pre-calculated stats and serialized attendance logs from disk cache.
   - All 4 tabs (Dashboard hero card, timetable, class logs, simulator) render immediately with complete interactivity.
   - The status tag displays `● Cached Today hh:mm a`.
2. **Phase 2: Silent Remote Reconcile (5-second timeout)**:
   - A background thread dispatches `ErpApiClient.getMonthlyAttendance()`, `getWeeklyAttendance()`, and `getStudentAttendanceLogs()`.
   - If network completes successfully:
     - New records are inserted into Room DB.
     - Derived statistics are recomputed.
     - Numbers smoothly roll to their updated values using `ValueAnimator`.
     - Status tag transitions to `● Live`.
3. **Phase 3: Network Timeout / Offline Fallback**:
   - If the network fails, times out, or the phone is in airplane mode, the error is caught silently.
   - The user is **never interrupted with blocking error dialogs**.
   - The status tag cleanly updates to `● Offline (Cached)`.

---

## 3. Background Synchronization Engine (WorkManager)

### 3.1 Worker Configuration (`AttendanceSyncWorker.java`)
- **Execution Engine:** Android Jetpack `WorkManager`.
- **Interval:** 15 minutes (or 60 minutes with 15-minute flex window).
- **Constraints:**
  ```java
  Constraints constraints = new Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build();
  ```
- **Policy:** `ExistingPeriodicWorkPolicy.KEEP` ensures only one sync job runs concurrently across reboots.

### 3.2 Attendance Diffing & Dynamic Alert Engine
To deliver real-time feedback without battery-draining continuous polling or noisy ambient shortage spam:
1. `AttendanceSyncWorker` fetches `getStudentAttendanceLogs(studentId)`.
2. Passes incoming attendance records, the persisted seen ID set (`KEY_SEEN_ATTENDANCE_IDS`), and the bootstrap flag (`KEY_INITIALIZED_ATTENDANCE_HISTORY`) into `AttendanceChangeDetector.kt`.
3. **Bootstrap Suppression**:
   - On the first post-login sync run, all historical records are ingested into the seen ID set without triggering historical notifications.
4. **Event-Driven Present/Absent Detection**:
   - Compares incoming records against the seen set. Only genuine newly marked classes with status `Present` or `Absent` emit `AttendanceMarkEvent.Marked` events.
   - Non-attendance ledger entries (e.g. cancelled/leave) and pre-existing classes are strictly ignored.
   - For each marked event, dispatches a notification via `NotificationHelper.showAttendanceMarkedNotification()`:
     - Title: `MARKED PRESENT` (Emerald) or `MARKED ABSENT` (Rose).
     - Context: Course title, faculty name, lecture period, and updated overall percentage.
   - Adds newly marked IDs to the persistent seen ID set.
5. Automatically updates all home screen widgets via `AttendanceWidgetProvider.updateAllWidgets()`.

---

## 4. Home Screen Widget Sync Protocol

- **Widget Class:** `AttendanceWidgetProvider` (RemoteViews).
- **User-Initiated Refresh**:
  - The widget contains an interactive refresh button (`btn_widget_refresh`).
  - Tapping the button dispatches an explicit broadcast targeting `AttendanceWidgetRefreshReceiver` (`com.lloyd.attendance.ACTION_REFRESH_WIDGET`).
  - **Security & Throttling (SEC-FINDING-006)**:
    - Protected by custom signature-level permission `com.lloyd.attendance.permission.WIDGET_REFRESH`.
    - Marked `android:exported="false"` in `AndroidManifest.xml` to prevent third-party IPC injection.
    - Rate-limited with a 15-second debounce throttle (`MIN_REFRESH_INTERVAL_MS = 15_000L`) to prevent battery drain or network DoS.
  - The widget UI displays a spinning progress tag (`Syncing...`).
  - An asynchronous worker fetches fresh stats and updates the RemoteViews upon arrival.
- **Fail-Safe Offline Mode**:
  - If the widget refresh fails due to network outage, it gracefully redisplays the last-known cached percentage with tag `Offline`.
