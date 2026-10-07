# Lloyd ERP — Stable Release Hardening & Quality Report

**Repository:** `nkpcore/lloyd-erp`  
**Assessment date:** 7 October 2026  
**Scope:** Current Android app only.  
**Goal:** Ship a stable, predictable release with no known correctness, sync, UI-state, build, or reliability faults.

> **Scope decision:** Notes, PYQs, assignments, community, AI, and other expansion features are intentionally excluded from this release plan. They can be added after the stable foundation is proven.

---

## 1. Executive Summary

The project has a good functional foundation, but it is **not ready to be treated as a final stable release yet**.

The priority should now be **quality over feature count**.

The app already contains:

- ERP authentication and profile data
- monthly attendance
- detailed attendance logs
- ERP-backed weekly timetable
- attendance calculations
- bunk simulation
- background synchronization
- attendance notifications
- home-screen widget
- Material 3 Compose UI
- unit tests
- CI build/test automation

The main release risks are:

1. **Two attendance data sources are not reconciled.**
2. **The Compose dashboard can display fresh overall attendance alongside stale subject-level ledger data.**
3. **The app still contains a deprecated legacy Activity and duplicate implementation paths.**
4. **Attendance logs are stored as a large JSON blob in SharedPreferences rather than structured local data.**
5. **The release build currently uses the debug signing configuration.**
6. **The CI pipeline runs unit tests but does not perform a sufficiently broad release-quality verification pass.**
7. **There is no dedicated integration test for the most important ERP data mismatch: monthly aggregate vs detailed ledger.**
8. **Background sync and widget sync have overlapping data-fetch paths that can make freshness behavior difficult to reason about.**
9. **The project still mixes Java and Kotlin implementations for important functionality.**
10. **The current UI needs stronger loading, stale-data, error, retry, and sync-state behavior.**

The stable-release objective should therefore be:

```text
CORRECT DATA
     ↓
CONSISTENT STATE
     ↓
RELIABLE SYNC
     ↓
NO CRASHES / NO ANRs
     ↓
PREDICTABLE OFFLINE BEHAVIOR
     ↓
CONSISTENT UI
     ↓
PERFORMANCE TESTING
     ↓
RELEASE
```

Android's current quality guidance explicitly emphasizes startup feedback, 60 FPS rendering, StrictMode testing, avoiding ANRs/crashes, current SDK compatibility, dependency maintenance, and release-quality testing. citehttps://developer.android.com/docs/quality-guidelines/core-app-quality

---

# 2. Stable Release Definition

For this release, “stable” should mean:

### Correctness

- Overall attendance is always calculated from the intended authoritative source.
- Detailed attendance never silently pretends to contain records it does not have.
- Subject attendance is not stale when the user performs a refresh.
- Timetable data comes from the current ERP-backed implementation.
- Attendance dates and record creation timestamps are never confused.
- Bunk/recovery calculations are mathematically correct at boundaries.

### Reliability

- No known crash paths in login, refresh, logout, widget, schedule, simulator, or rotation/recreation.
- No UI-thread network or disk operations.
- No uncontrolled coroutine/thread leaks.
- Background sync failures do not damage or erase good cached data.
- Offline mode continues to display the last known valid state.

### UX

- The user always knows whether data is fresh, cached, stale, syncing, or unavailable.
- Refresh has deterministic behavior.
- Errors are actionable.
- Empty states are intentional rather than blank.
- Navigation does not reset important state unexpectedly.

### Release engineering

- Release APK is correctly signed.
- CI runs all required tests before publishing.
- Release artifacts are reproducible and versioned.
- No debug-only dependencies or behavior enter production.
- Proguard/R8 rules are validated against the release build.

---

# 3. Current Architecture Assessment

The modern application currently looks approximately like:

```text
MainComposeActivity
       │
       ├── DashboardViewModel ── ErpApiClient
       │                         └── AppPreferences
       │
       ├── ScheduleViewModel ─── ErpApiClient
       │                         └── TimetableRepository
       │
       ├── SimulationViewModel
       │
       └── ProfileScreen

Background:
AttendanceSyncWorker ── ErpApiClient

Widget:
AttendanceWidgetProvider ── ErpApiClient

Legacy:
MainActivity.java ── duplicate attendance/log UI
```

This works, but it has too many places that can fetch, calculate, cache, or display the same information.

Android's current architecture recommendations strongly favor a defined data layer, repositories between UI and data sources, persistent models, a single source of truth, and unidirectional data flow. citehttps://developer.android.com/topic/architecture/recommendations

That principle maps directly to the biggest problem in this project.

---

# 4. P0 — Attendance Data Consistency

## 4.1 The observed mismatch

The supplied screens show:

```text
Attendance summary
Present = 76
Total   = 121
Rate    = 62.8%
```

while Daily Attendance Logs show:

```text
Present = 76
Absent  = 36
Total   = 112
```

Therefore:

```text
121 - 112 = 9
```

and:

```text
45 implied aggregate absences - 36 ledger absences = 9
```

The present count matches exactly.

## 4.2 Why this happens in the source

The dashboard obtains the overall figure from the monthly aggregate endpoint:

```text
/student/me/monthly-attendance
```

The detailed log screen obtains individual records from:

```text
/attendance/student
```

These are not the same representation.

The monthly endpoint provides aggregate month values such as:

```text
present
 total
 percentage
```

The detailed endpoint provides individual records such as:

```text
attendance_date
subject
status
lecture
faculty
created_at
```

Therefore the application must **not assume that summing the detailed ledger will always reproduce the monthly aggregate**.

## 4.3 Most likely explanation

The exact pattern strongly suggests that the monthly aggregate may count conducted classes for which no explicit individual `Absent` transaction exists in the detailed endpoint.

This is a hypothesis until a live record-by-record comparison proves it.

Do not manufacture nine missing absence records.

## 4.4 Correct product behavior

The app should show:

```text
Official Attendance
62.8%
76 / 121

Detailed Records
112 records
76 Present · 36 Absent
```

If useful, show a non-alarming explanation:

> ERP summary and detailed records currently contain different record counts.

This is much safer than silently altering one dataset to match the other.

---

# 5. P0 — Create a Single Attendance Repository

Create:

```text
AttendanceRepository
```

It becomes the only application component responsible for attendance data.

Recommended flow:

```text
                    AttendanceRepository
                            │
             ┌──────────────┼──────────────┐
             ↓              ↓              ↓
          Monthly         Ledger       Reconciliation
          Aggregate       Records          │
             │              │              ↓
             └──────────────┴──────→ AttendanceState
                                           │
                                           ↓
                                      ViewModels
                                           │
                                           ↓
                                           UI
```

The UI and ViewModels should not independently decide which ERP endpoint to call.

---

# 6. P0 — Fix the Compose Dashboard Freshness Bug

The current `DashboardViewModel` refresh path fetches monthly attendance and weekly attendance.

It does **not** explicitly fetch the detailed ledger during that foreground refresh.

However, subject cards can be constructed from:

```text
prefs.getAttendanceLogs()
```

That means the current architecture can produce:

```text
Overall = fresh
Subjects = stale
```

Example:

```text
User taps Refresh

Monthly endpoint → updated
Weekly endpoint  → updated
Ledger cache     → old

Result:
Overall attendance = current
Subject attendance = old
```

### Required fix

The attendance refresh must fetch all required attendance data together:

```text
refresh()
 ├── monthly aggregate
 ├── detailed ledger
 └── reconciliation
        ↓
   persist snapshot
        ↓
   publish UI state
```

A successful refresh should never publish half-fresh attendance state.

---

# 7. P0 — Define an Explicit Attendance State

Use one immutable state model:

```kotlin
data class AttendanceState(
    val aggregate: AttendanceAggregate,
    val records: List<AttendanceRecord>,
    val reconciliation: AttendanceReconciliation,
    val lastUpdated: Instant,
    val source: DataSourceState
)
```

For example:

```text
DataSourceState

FRESH
STALE
OFFLINE
PARTIAL
ERROR
```

This prevents individual screens from inventing their own interpretation of cached data.

---

# 8. P0 — Do Not Overwrite Good Cache With Bad/Partial Data

A failed sync must not do this:

```text
Good cache
   ↓
request fails
   ↓
empty state
```

Instead:

```text
Good cache
   ↓
request fails
   ↓
keep cache
   ↓
mark STALE
   ↓
show retry
```

Example UI:

```text
Attendance
62.8%
76 / 121

Updated 17 minutes ago

⚠ Couldn't refresh
[Try Again]
```

The user still gets useful information.

---

# 9. P0 — Make Offline Behavior Explicit

There should be three distinct states:

### Fresh

```text
Updated just now
```

### Cached/stale

```text
Offline · Showing data from 18 min ago
```

### No data

```text
No attendance data available yet
[Retry]
```

Do not use an empty dashboard to represent a network failure.

---

# 10. P0 — Legacy UI Removal

`MainComposeActivity` is the launcher.

`MainActivity.java` is deprecated and still contains the older attendance/log UI.

This creates a maintenance hazard because a future change can fix one implementation while leaving the other wrong.

Migration should be:

```text
Compose Dashboard
        ↓
Compose Detailed Attendance
        ↓
Compose Schedule
        ↓
Compose Simulator
        ↓
Feature parity verified
        ↓
Delete MainActivity.java
```

Do not continue adding production features to the legacy Activity.

---

# 11. P0 — Detailed Attendance Must Be Compose-Native

The stable release needs a proper detailed attendance screen in the current UI stack.

It should provide:

```text
All
Present
Absent
```

plus:

```text
Month
Subject
Date
```

The screen should show:

```text
Tuesday, 6 Oct
3 lectures · 2P / 1A

✓ Mathematics
  Lecture 3
  Faculty name

✕ Electronics
  Lecture 2
  Faculty name
```

And the summary should clearly say whether it represents:

```text
112 detailed records
```

rather than implying that it is the official aggregate total.

---

# 12. P0 — Date Handling

The ERP provides at least two important date concepts:

```text
attendance_date
created_at
```

They must remain separate.

Use:

```text
attendance_date → when the lecture occurred
created_at       → when the attendance record was submitted/created
```

The UI should group attendance by `attendance_date`.

`created_at` can be shown as secondary information such as:

```text
Marked 06:11 AM
```

Never use `created_at` to decide which lecture day the record belongs to.

---

# 13. P0 — Pagination and Deduplication

The API is paginated.

The repository should guarantee:

```text
all requested pages fetched
        ↓
records deduplicated by stable ID
        ↓
sorted consistently
        ↓
persisted
```

Do not assume:

```text
page 1 = complete semester
```

Add tests for:

- one page,
- multiple pages,
- empty page,
- repeated page,
- duplicate record IDs,
- malformed page metadata.

---

# 14. P0 — Background Sync Reliability

`AttendanceSyncWorker` is useful, but it should become a thin orchestration layer.

Target:

```text
Worker
  ↓
SyncCoordinator
  ↓
AttendanceRepository
```

The Worker should not own attendance business logic.

It should:

1. start sync,
2. receive success/failure,
3. update widgets/notifications,
4. return the appropriate WorkManager result.

Android's quality guidance recommends avoiding unnecessary long-running background work and validating behavior under Doze/App Standby. citehttps://developer.android.com/docs/quality-guidelines/core-app-quality

---

# 15. P0 — Widget Must Use the Same Attendance Source

The widget currently independently calls the ERP client.

That means the app can theoretically have:

```text
Dashboard → one cached state
Widget    → another network result
Worker    → another network result
```

Instead:

```text
AttendanceRepository
       ↓
   persisted state
       ↓
 ┌─────┴──────┐
 ↓            ↓
App         Widget
```

The widget should read the canonical persisted attendance snapshot.

A widget refresh may trigger a sync, but the resulting data should flow through the same repository.

---

# 16. P0 — Notification Correctness

The current notification detector is already tested and has good behavior around Present/Absent changes.

Keep the current principle:

```text
initial installation
    ↓
bootstrap existing IDs
    ↓
NO historical notifications
```

Then:

```text
new ERP attendance ID
    ↓
Present/Absent?
    ↓
notify
```

Add tests for:

- edited existing record,
- duplicate record,
- record reordered by server,
- missing ID,
- pagination boundary,
- logout/login with a different student,
- notification disabled,
- worker retry.

---

# 17. P1 — Replace JSON Blob Storage

Current `AppPreferences` stores:

```text
monthly_json
weekly_json
attendance_logs_json
cached_stats
```

This is one of the biggest technical-debt areas.

For the stable release, there are two options.

## Option A — Minimal stabilization

Keep SharedPreferences temporarily, but create a proper repository around it.

Rules:

- only repository writes data,
- JSON parsing happens in one place,
- writes are atomic,
- cache metadata is stored,
- schema/version is tracked,
- corrupted JSON never destroys the last good snapshot.

## Option B — Recommended

Move structured attendance/schedule data to Room.

Android architecture guidance recommends persistent data models and a database as the typical source of truth for offline-first application data. citehttps://developer.android.com/topic/architecture

For a stable release, Option A is acceptable if Room migration would delay the release substantially. Room can then become the next controlled refactor.

---

# 18. P1 — Cache Versioning

Add a cache schema version:

```text
attendance_cache_version = 1
```

On incompatible changes:

```text
old cache
   ↓
recognized as incompatible
   ↓
discard only invalid cache
   ↓
fetch fresh data
```

Never let a changed Gson model silently produce partially populated UI.

---

# 19. P1 — Corrupt Cache Recovery

Every cached JSON read should handle:

```text
null
empty
malformed
unexpected schema
partial fields
```

Behavior:

```text
Corrupt cache
   ↓
log diagnostic
   ↓
remove/ignore corrupt snapshot
   ↓
try network
   ↓
show clean error if network unavailable
```

Never crash the dashboard because cached JSON cannot be parsed.

---

# 20. P1 — Release Signing Must Be Fixed

The current Gradle configuration has:

```gradle
release {
    minifyEnabled true
    shrinkResources true
    signingConfig signingConfigs.debug
}
```

That should **not** be the final production signing arrangement.

The stable release pipeline needs a proper release keystore/signing configuration stored through GitHub Actions secrets or another secure CI secret mechanism.

The repository's workflow currently builds and publishes a release APK automatically from `main`, so signing must be treated as release infrastructure rather than a local-development detail.

This is a release-quality issue rather than a feature issue.

---

# 21. P1 — CI Needs to Become a Quality Gate

Current CI does:

```text
unit tests
↓
assembleRelease
↓
GitHub release
```

That is a good start but too permissive for a “stable release”.

Change it to:

```text
Checkout
  ↓
Dependency/build validation
  ↓
Unit tests
  ↓
Lint
  ↓
Debug instrumentation/UI tests
  ↓
Release build
  ↓
Release smoke tests
  ↓
Artifact verification
  ↓
Publish
```

Most importantly:

> **Do not publish a GitHub release if tests, lint, or release verification fail.**

Android's core quality guidance recommends testing stability, latest-platform compatibility, dependency health, and production build quality, including ensuring debug libraries are not included. citehttps://developer.android.com/docs/quality-guidelines/core-app-quality

---

# 22. P1 — Add Android Lint

CI should run:

```bash
./gradlew lintDebug
```

and ideally:

```bash
./gradlew lintRelease
```

Treat new high-confidence lint errors as CI failures.

---

# 23. P1 — Add Instrumentation/UI Tests

Current tests are mostly pure JVM tests.

Add Android tests for:

### Login

```text
valid session
expired session
invalid credentials
network unavailable
```

### Dashboard

```text
cached data
fresh data
refresh failure
offline state
```

### Attendance

```text
filter
subject selection
empty records
112/121 discrepancy
```

### Schedule

```text
weekday
Sunday
empty timetable
active class
next class
```

### Logout

```text
logout
back navigation
relaunch
```

### Widget

```text
cached state
refresh
offline fallback
```

---

# 24. P1 — Add a Fake ERP Layer

The app currently depends heavily on live ERP behavior for meaningful testing.

Create an interface:

```kotlin
interface ErpDataSource
```

Production:

```text
ErpApiClient
```

Tests:

```text
FakeErpDataSource
```

Fixtures:

```text
attendance-normal.json
attendance-discrepancy.json
attendance-empty.json
attendance-multi-page.json
weekly-normal.json
weekly-empty.json
profile-normal.json
```

This is one of the highest-value testing changes you can make.

---

# 25. P1 — Exact Regression Test for 121 vs 112

Create a test fixture representing the current issue:

```text
Monthly:
76 present
121 total

Ledger:
76 present
36 absent
112 rows
```

Expected:

```text
aggregate.total == 121
ledger.size == 112
aggregate.present == ledger.present
reconciliation.discrepancy == 9
```

And importantly:

```text
No fake absent records created.
```

This test prevents future developers from “fixing” the discrepancy by corrupting the data model.

---

# 26. P1 — Make Refresh Transactional

Current conceptual risk:

```text
monthly saved
weekly failed
ledger failed
UI partially updated
```

Better:

```text
Fetch all required data
        ↓
validate/parse all
        ↓
calculate reconciliation
        ↓
commit snapshot
        ↓
publish state
```

If the complete refresh cannot be committed:

```text
keep previous valid snapshot
mark stale
show error
```

This is a major stability improvement.

---

# 27. P1 — Separate Network Errors From Data Errors

Do not turn every exception into:

```text
Failed to sync
```

Classify:

```text
AuthenticationExpired
NetworkUnavailable
Timeout
ServerError
InvalidResponse
ParseError
EmptyData
Unknown
```

The UI can then provide useful messages.

Example:

```text
No internet connection.
Showing your last saved attendance.
```

versus:

```text
Lloyd ERP returned invalid attendance data.
Your previous data is still available.
```

---

# 28. P1 — Timeouts and Retry Policy

Every network request should have explicit sensible timeouts.

Retries should be limited.

Avoid:

```text
failure → immediate retry → failure → immediate retry → battery drain
```

Prefer:

```text
request
 ↓
controlled retry
 ↓
backoff
 ↓
final failure
```

WorkManager should handle longer-lived retry scheduling.

---

# 29. P1 — Concurrency Control

Prevent multiple simultaneous foreground refreshes.

Example:

```text
User taps refresh
       ↓
refresh running
       ↓
second tap ignored/coalesced
```

The repository should have one active synchronization operation.

Do not allow:

```text
refresh A
refresh B
worker C
widget D
```

to all independently modify the same cache at the same time.

---

# 30. P1 — Lifecycle Safety

ViewModels are already used in the Compose app, which is good.

Continue the rule:

```text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
network/database
```

Do not put long-running work directly inside Composables.

Use lifecycle-aware collection for StateFlow.

Android's architecture guidance recommends driving UI from persistent/data models and using unidirectional data flow because it improves consistency and testability. citehttps://developer.android.com/topic/architecture

---

# 31. P1 — Performance Audit

Android's current quality guidance expects fast startup, smooth rendering, StrictMode compliance, and no ANR-causing work on the UI thread. citehttps://developer.android.com/docs/quality-guidelines/core-app-quality

Test:

```text
cold start
warm start
login
refresh
scroll attendance
open schedule
open simulator
widget refresh
logout
```

Targets:

```text
startup → fast or visible progress
scrolling → no visible jank
network → never blocks UI
large attendance list → smooth
```

Use:

- Android Studio Profiler
- Perfetto
- StrictMode
- Logcat
- Android Vitals after release

Android's performance documentation specifically recommends these tools for diagnosing startup, rendering, memory, background work, and stability issues. citehttps://developer.android.com/topic/performance/issues

---

# 32. P1 — Startup Audit

At launch, the app currently initializes authentication, permissions, WorkManager scheduling, ViewModels, and Compose.

Keep startup minimal.

Recommended:

```text
Launch
 ↓
check local session
 ↓
show cached UI immediately
 ↓
refresh asynchronously
```

Do not make the user wait for ERP network data before seeing a usable cached screen.

Android's quality guidance says apps should load quickly or provide feedback when startup takes longer than two seconds. citehttps://developer.android.com/docs/quality-guidelines/core-app-quality

---

# 33. P1 — UI State Matrix

Every major screen should support all of these states:

```text
INITIAL_LOADING
CONTENT
REFRESHING
OFFLINE_WITH_CACHE
ERROR_WITH_CACHE
ERROR_WITHOUT_CACHE
EMPTY
```

Example:

```text
Loading:
Skeleton cards

Content:
Normal dashboard

Refreshing:
Existing content + progress indicator

Offline:
Existing content + offline banner

Error:
Existing content + retry

Empty:
Clear explanation + action
```

This is much more important for stability than adding another feature.

---

# 34. P1 — Attendance Screen UX

Keep the current strong visual direction, but improve hierarchy.

Recommended:

```text
62.8%
76 / 121
Critical

Need 59 classes to reach 75%

[View Detailed Attendance]

Subject Breakdown

Physics       72%   18/25
Chemistry     82%   23/28
Mathematics   60%   12/20
```

Then:

```text
Detailed History
```

should be a separate destination rather than forcing the dashboard to show everything.

---

# 35. P1 — Simulator UX

Keep the simulator.

Do not delete it.

But make it a contextual tool:

```text
Attendance
   ↓
Subject
   ↓
Simulate
```

The existing bottom navigation can remain temporarily for the stable release if moving it would create unnecessary migration risk.

Do **not** change navigation architecture just for aesthetics immediately before release.

For this release, correctness and stability take priority over redesign.

---

# 36. P1 — Schedule Reliability

The modern Kotlin `TimetableRepository` is already using ERP weekly data.

Keep it as the production implementation.

Verify:

- Monday–Saturday
- Sunday
- no classes
- multiple periods
- breaks
- malformed times
- missing rooms
- missing faculty
- current class
- next class
- week boundary

The old hardcoded Java timetable should be removed after the Compose path is verified.

---

# 37. P1 — Timetable Time Parsing

Current tests already cover:

```text
09:30:00
9:30 AM
02:15 PM
14:15
12:00 PM
12:00 AM
```

Add:

```text
09:00
09:00:00
9:00
9 AM
12 AM
12 PM
invalid
empty
null
```

Also test:

```text
end == start
end < start
```

because bad ERP data should not produce a bizarre active-class state.

---

# 38. P1 — Bunk Calculation Verification

The existing tests are good.

Keep boundary coverage around:

```text
0/0
0/1
1/1
74/100
75/100
3/4
4/4
```

Add property-style tests if practical:

```text
calculated bunk count must never produce < target
next bunk after allowance must produce < target
needed classes must produce >= target
one fewer must remain < target when currently below target
```

This protects the feature students rely on most.

---

# 39. P1 — Release Build Configuration

Current build configuration includes:

```text
compileSdk 36
minSdk 26
targetSdk 36
Java 17
Kotlin 2.0.21
```

The target/compile SDK is appropriate for the current release direction, but dependencies should be reviewed before release.

The Compose BOM is currently pinned to:

```text
2024.10.01
```

and several AndroidX libraries are older than the current 2026 ecosystem.

Do not blindly upgrade everything immediately.

Instead:

```text
inventory dependencies
 ↓
check release notes
 ↓
upgrade one family at a time
 ↓
run full tests
 ↓
measure regressions
```

A stable release is better served by controlled upgrades than a giant dependency jump.

Android's quality guidance explicitly recommends maintaining current platform and third-party SDK/dependency health. citehttps://developer.android.com/docs/quality-guidelines/core-app-quality

---

# 40. P1 — WorkManager Version and Sync Review

The project currently uses WorkManager 2.9.1.

Before final release:

1. Check the current compatible WorkManager version.
2. Review release notes.
3. Upgrade only if the change is low-risk.
4. Test periodic sync under Doze/App Standby.
5. Verify that duplicate work does not occur.

Do not change sync cadence simply to make it appear “more live”.

Reliability and battery efficiency matter more.

---

# 41. P1 — Release Signing and Artifact Validation

The CI workflow currently:

```text
run unit tests
↓
assembleRelease
↓
create GitHub release
```

Before final release, change it to:

```text
unit tests
↓
lint
↓
instrumentation/UI tests
↓
release build
↓
verify APK exists
↓
verify package/version
↓
verify signing
↓
optional smoke install
↓
publish
```

A failed verification must block publication.

---

# 42. P1 — Do Not Auto-Publish Every Main Commit During Stabilization

The current GitHub workflow publishes a release from `main` on every push.

That is convenient during development but risky for a stable release process.

Recommended:

```text
main
 ↓
CI validation only

release tag
 ↓
full release pipeline
 ↓
GitHub release
```

For example:

```text
v1.0.0
v1.0.1
v1.0.2
```

This makes release selection deliberate.

---

# 43. P1 — Crash and ANR Monitoring

After release, monitor Android Vitals.

Android identifies crash and ANR behavior as core app-quality concerns and recommends using Play Console/Android Vitals and pre-launch testing to identify stability problems. citehttps://developer.android.com/topic/performance/vitals/crash

For this project, track at minimum:

```text
Crash-free users
Crash-free sessions
ANR rate
Startup time
Jank
Battery impact
```

Do not declare the app stable just because the APK builds.

---

# 44. P1 — Release Device Matrix

At minimum test:

### Android versions

```text
Android 12
Android 13
Android 14
Android 15
Android 16
```

and the newest available Android version used by your target audience.

### Device classes

```text
budget device
mid-range device
high-end device
```

### Conditions

```text
good Wi-Fi
mobile data
slow network
offline
ERP server unavailable
fresh install
upgrade from previous release
logout/login
screen rotation
process death
battery saver
Doze/App Standby
```

Android's core quality guidance explicitly recommends testing the latest public platform and checking behavior under power-management conditions. citehttps://developer.android.com/docs/quality-guidelines/core-app-quality

---

# 45. P1 — Upgrade/Install Migration Tests

Test:

```text
Install v0.x
 ↓
login
 ↓
cache data
 ↓
upgrade to v1.0
 ↓
data remains valid
```

Also:

```text
old cache schema
 ↓
new app
 ↓
safe migration
```

And:

```text
uninstall/reinstall
 ↓
clean login
```

---

# 46. P1 — Logging Strategy

Production logs should be useful but not noisy.

Keep structured diagnostics such as:

```text
SYNC_START
SYNC_SUCCESS
SYNC_FAILURE
ATTENDANCE_RECONCILIATION
CACHE_LOAD_FAILURE
```

Avoid dumping huge raw ERP JSON responses during normal production operation.

Use debug logging only in debug builds.

The existing R8 rules already strip Android `Log` calls from release builds, which is useful; the code should still avoid building a dependency on verbose raw logging for normal diagnostics.

---

# 47. P1 — Error Reporting Must Preserve Context

When a sync fails, record:

```text
operation
endpoint class
error category
timestamp
whether cached data exists
```

Do not expose internal stack traces to students.

Student-facing message:

```text
Couldn't refresh attendance.
Your saved data is still available.
```

Developer diagnostic:

```text
ATTENDANCE_SYNC_FAILED
reason=TIMEOUT
cache_age=17m
```

---

# 48. P1 — Remove Duplicate Business Logic

The following concepts should have exactly one implementation:

```text
attendance percentage
bunk allowance
attendance health
schedule resolution
attendance status parsing
attendance synchronization
```

If Java and Kotlin each implement the same calculation, one will eventually drift.

The Kotlin domain layer should become canonical.

---

# 49. P1 — Make Models Immutable Where Possible

Prefer:

```kotlin
data class
```

for domain state.

ERP DTOs can remain Gson-friendly mutable classes if required, but convert them into immutable domain models at the data boundary.

Architecture becomes:

```text
ERP DTO
  ↓
Mapper
  ↓
Domain model
  ↓
UI state
```

This keeps API quirks away from the UI.

---

# 50. P1 — Keep API-Specific Logic Out of UI

The UI should never need to know:

```text
/api/attendance/student
/api/student/me/monthly-attendance
```

It should only know:

```text
attendanceState
```

This makes future ERP changes dramatically easier.

---

# 51. P1 — One Source of Truth for Profile

Profile data currently exists through multiple paths such as:

```text
userProfile
studentId
selectedSection
cachedStats
```

Create one profile state:

```kotlin
data class StudentProfile(
    val id: Int,
    val name: String,
    val admissionNumber: String?,
    val course: String?,
    val semester: String?,
    val section: String?
)
```

Then derive other screens from it.

---

# 52. P1 — Section Handling

The modern timetable implementation correctly avoids blindly defaulting to A-1.

Keep that behavior.

The stable release must never silently display:

```text
A-1
```

unless the ERP actually says the student belongs to A-1.

If section data is unavailable:

```text
Section unavailable
```

is preferable to displaying a false schedule.

---

# 53. P1 — Loading and Refresh UX

Use one predictable refresh pattern:

```text
Initial:
Skeleton

Pull/refresh:
content remains visible
small progress indicator

Success:
Updated just now

Failure:
content remains visible
retry action
```

Do not replace an already useful screen with a full-screen spinner every time the user refreshes.

---

# 54. P1 — Navigation Stability

Before release, verify:

```text
Dashboard → Subject simulator → Back
Dashboard → Schedule → Back
Profile → Logout
Notification → App
Widget → App
```

Important state should survive:

```text
rotation
process recreation
background/foreground
```

The selected tab can reset if intentionally designed that way, but data must remain available.

---

# 55. P1 — Accessibility and Touch Quality

Before release verify:

- touch targets are comfortably tappable,
- content descriptions exist for meaningful icons,
- text does not truncate critical information,
- contrast remains readable in dark mode,
- large text does not break layouts,
- TalkBack can navigate core screens,
- buttons have visible states.

This is part of stable UX, not an optional polish phase.

---

# 56. P1 — Adaptive Layout

Even if the primary target is phones, test:

```text
small phone
normal phone
large phone
landscape
split-screen
```

Android's current quality guidance emphasizes adaptive layouts across window sizes and form factors. citehttps://developer.android.com/docs/quality-guidelines/core-app-quality

Do not let the attendance cards or schedule become unusable on unusual aspect ratios.

---

# 57. Stable Release Test Matrix

## Authentication

```text
□ Fresh login
□ Correct credentials
□ Wrong credentials
□ Expired token
□ Refresh token
□ Network lost during login
□ Logout
□ Relaunch after login
```

## Dashboard

```text
□ Fresh data
□ Cached data
□ Offline
□ Refresh
□ Refresh failure
□ Corrupt cache
□ 0/0 attendance
□ 76/121 attendance
```

## Attendance

```text
□ Detailed logs
□ Pagination
□ Present filter
□ Absent filter
□ Month filter
□ Subject filter
□ Date grouping
□ 121/112 discrepancy
□ Missing subject name
□ Missing faculty
□ Missing lecture number
```

## Schedule

```text
□ Monday
□ Tuesday
□ Wednesday
□ Thursday
□ Friday
□ Saturday
□ Sunday
□ Active class
□ Next class
□ No class
□ Missing room
□ Missing teacher
```

## Simulator

```text
□ 0/0
□ 0/1
□ 75/100
□ Below target
□ Above target
□ Multiple targets
□ Rapid slider movement
```

## Widget

```text
□ Install widget
□ Cached data
□ Refresh
□ Offline
□ Open app
□ Login required
□ Multiple widget instances
```

## Background

```text
□ Worker scheduled
□ Worker succeeds
□ Worker retries
□ Worker offline
□ Doze
□ Battery saver
□ Notifications disabled
```

---

# 58. Release Gates

Do not ship until all are true:

### Gate A — Build

```text
□ Debug build passes
□ Release build passes
□ R8 succeeds
□ No missing classes/resources
□ Correct version code/name
□ Correct release signing
```

### Gate B — Tests

```text
□ Unit tests pass
□ Instrumentation tests pass
□ UI smoke tests pass
□ Attendance reconciliation tests pass
□ Lint passes
```

### Gate C — Data

```text
□ Monthly attendance verified
□ Detailed attendance verified
□ 121/112 discrepancy understood and represented correctly
□ Schedule verified
□ Profile verified
□ Cache verified
```

### Gate D — Reliability

```text
□ Offline works
□ Retry works
□ No UI-thread network work
□ No obvious ANRs
□ No crashes in smoke test
□ Background sync behaves correctly
```

### Gate E — UX

```text
□ Loading states
□ Empty states
□ Error states
□ Offline state
□ Accessibility pass
□ Small/large screen pass
```

---

# 59. Recommended Implementation Order

Do not implement everything at once.

## Phase 1 — Correctness

```text
1. AttendanceRepository
2. AttendanceState
3. Monthly + ledger synchronized refresh
4. Reconciliation model
5. Compose detailed attendance screen
6. Fix stale subject data
7. Date handling
8. Pagination/deduplication tests
```

## Phase 2 — Reliability

```text
9. Cache validation
10. Offline/stale state
11. Transactional refresh
12. Sync concurrency control
13. Worker cleanup
14. Widget source-of-truth cleanup
15. Error classification
```

## Phase 3 — Release Engineering

```text
16. Proper release signing
17. Lint in CI
18. Instrumentation tests
19. Release smoke test
20. Tag-based release workflow
21. Dependency audit
```

## Phase 4 — Cleanup

```text
22. Remove legacy MainActivity
23. Remove duplicate timetable implementation
24. Remove duplicate attendance calculations
25. Remove obsolete code
26. Final UI consistency pass
```

## Phase 5 — Final validation

```text
27. Device matrix
28. Offline testing
29. Doze testing
30. Upgrade testing
31. Crash/ANR review
32. Release candidate
```

Only after this should the app be called stable.

---

# 60. What NOT to Do Before Stable Release

Do not add:

```text
❌ Notes
❌ PYQ system
❌ Community
❌ AI assistant
❌ Assignment platform
❌ Major backend expansion
❌ Major navigation redesign
❌ Large visual redesign
❌ Multiple new data providers
```

Do not introduce major architectural changes simply because they look cleaner if they are not required for correctness.

The stable-release rule should be:

> **Every change must either fix a known fault, improve reliability, improve testability, or improve an existing core workflow without increasing risk.**

---

# 61. Final Priority Table

| Area | Current assessment | Priority |
|---|---|---|
| Attendance correctness | Two representations; reconciliation needed | **P0** |
| Compose data freshness | Subject ledger can be stale | **P0** |
| Detailed attendance in Compose | Legacy implementation remains | **P0** |
| Single attendance repository | Missing | **P0** |
| Offline behavior | Needs explicit stale/error states | **P0** |
| Sync consistency | Multiple independent paths | **P0** |
| Release signing | Debug signing configured for release | **P0** |
| CI quality gates | Unit tests + build only | **P1** |
| Instrumentation/UI tests | Limited | **P1** |
| Cache architecture | JSON blobs in SharedPreferences | **P1** |
| Widget source of truth | Independent API path | **P1** |
| Background sync | Functional but needs centralization | **P1** |
| Legacy Java cleanup | Duplicate implementation | **P1** |
| Timetable | Modern path is good | P1 verification |
| Attendance math | Good test coverage | Maintain |
| Simulator | Good feature; keep stable | Maintain |
| Notes/PYQs/etc. | Deferred | **Later** |

---

# 62. Final Recommendation

The project should **stop expanding for now**.

The best next milestone is not a feature release. It is:

# `v1.0 Stable`

with this definition:

```text
One attendance source-of-truth layer
             ↓
Correct aggregate + ledger handling
             ↓
Consistent cached state
             ↓
Reliable refresh/offline behavior
             ↓
Compose-only production UI
             ↓
Tested background sync + widget
             ↓
Release-quality CI
             ↓
Proper signed release
             ↓
Device / offline / lifecycle validation
```

The app already has enough functionality to be genuinely useful. The biggest value now comes from making every existing feature **boringly reliable**.

That means when a student opens it:

```text
Attendance is correct.
Schedule is correct.
Refresh works.
Offline still works.
Widget agrees with the app.
Notifications are accurate.
Back navigation works.
Logout works.
The app doesn't crash.
The UI never lies about freshness.
```

That is the right definition of the first stable Lloyd release.

---

## Sources / Engineering Basis

This report also cross-checks the release strategy against current Android guidance on:

- core app quality,
- stability and ANR prevention,
- startup and rendering performance,
- StrictMode,
- latest platform/dependency compatibility,
- repositories and single-source-of-truth architecture,
- persistent models and offline-first behavior.

Key references:

- Android Core App Quality: https://developer.android.com/docs/quality-guidelines/core-app-quality
- Android App Architecture: https://developer.android.com/topic/architecture
- Android Architecture Recommendations: https://developer.android.com/topic/architecture/recommendations
- Android Performance Issues: https://developer.android.com/topic/performance/issues
- Android Crashes / Vitals: https://developer.android.com/topic/performance/vitals/crash
