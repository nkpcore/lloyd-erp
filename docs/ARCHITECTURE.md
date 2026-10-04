# Lloyd Student Application Architecture Specification

## 1. Architectural Philosophy

The Lloyd Student Application is architected around four core pillars:
1. **Real Data First**: The application strictly interfaces with authentic ERP services. No fake or placeholder data is ever injected into the domain or presentation layers.
2. **Offline-First & Zero Latency**: Classroom cellular conditions are often unreliable. The user interface must paint in under 30 milliseconds from local disk cache upon launch, reconciling silently against the network.
3. **Unidirectional Data Flow**: Data flows predictably from upstream remote endpoints through a unified repository layer into domain engines, producing immutable UI state models.
4. **Security by Default**: Credentials, tokens, and academic records are treated as sensitive assets, utilizing Android Keystore encryption and strict token lifecycle rotation.

---

## 2. High-Level Architecture Diagram

```text
                           Lloyd ERP Server
                       (https://erp.lloydcollege.in)
                                   │
                                   ▼
                       ┌───────────────────────┐
                       │     Networking Layer  │
                       │     (ErpApiClient)    │
                       │  - OkHttp3 Pool       │
                       │  - Auth Interceptor   │
                       └───────────┬───────────┘
                                   │
                                   ▼
                       ┌───────────────────────┐
                       │   Repository Layer    │
                       │(AttendanceRepository) │
                       └─────┬───────────┬─────┘
                             │           │
              ┌──────────────┘           └──────────────┐
              ▼                                         ▼
   ┌───────────────────────┐               ┌───────────────────────┐
   │    Local Persistence  │               │     Domain Layer      │
   │  - AppPreferences     │               │  - AttendanceCalc     │
   │  - EncryptedStore     │               │  - BunkAdvisor        │
   │  - JSON/DB Cache      │               │  - ScheduleEngine     │
   └───────────────────────┘               └───────────┬───────────┘
                                                       │
                                                       ▼
                                           ┌───────────────────────┐
                                           │    UI State Model     │
                                           │  - DashboardState     │
                                           │  - ScheduleState      │
                                           │  - LogsState          │
                                           └───────────┬───────────┘
                                                       │
                       ┌───────────────────────────────┼───────────────────────────────┐
                       │                               │                               │
                       ▼                               ▼                               ▼
             ┌──────────────────┐            ┌──────────────────┐            ┌──────────────────┐
             │   MainActivity   │            │ AttendanceWidget │            │Dynamic Island OS │
             │  (MD3 Expressive)│            │ (RemoteViews)    │            │(NotificationMgr) │
             └──────────────────┘            └──────────────────┘            └──────────────────┘
```

---

## 3. Layer Breakdown & Responsibilities

### 3.1 Networking Layer (`com.lloyd.attendance.api`)
- **`ErpApiClient`**: Handles HTTP/TLS connectivity using OkHttp3 with a static connection pool (`ConnectionPool(10, 5, TimeUnit.MINUTES)`).
- **Authentication Handling**: Performs JWT token exchange and transparent token refresh via `/auth/refresh`. Mutex synchronization prevents parallel requests from triggering redundant re-auth handshakes.
- **DTOs (`Models`)**: Pure data classes strictly representing the ERP request and response schema.

### 3.2 Repository Layer (`com.lloyd.attendance.repository`)
- Acts as the single source of truth for the entire application (UI, Widgets, Background Workers).
- Enforces the **Cache-Then-Network** pattern:
  1. Instantly emits cached data from disk to UI consumers.
  2. Dispatches an asynchronous network fetch with a 5-second connection timeout.
  3. Upon network arrival, updates local storage and notifies subscribers with fresh state.
  4. If network fails or times out, transitions connection state to `OFFLINE` while preserving complete UI interactivity.

### 3.3 Domain Layer (`com.lloyd.attendance.domain`)
- **Framework Independence**: Pure Java/Kotlin classes containing zero Android SDK dependencies.
- **`AttendanceCalculator`**: Computes accurate percentages, rounding to one decimal place, guarding against division-by-zero.
- **`BunkAdvisor`**: Implements college-compliant attendance policy mathematics:
  - Minimum mandatory threshold: $75.0\%$
  - Computes exact lectures that can be missed before falling below threshold.
  - Computes exact consecutive lectures required to recover from a shortage.
- **`ScheduleEngine`**: Evaluates the student's timetable routine against current wall-clock time to identify:
  - Current active class (with remaining time in minutes).
  - Next upcoming teaching period.
  - Excludes breaks, lunches, and non-teaching slots from class count metrics.

### 3.4 Presentation Layer (`com.lloyd.attendance.ui`)
- **`MainActivity`**: Material Design 3 Expressive 4-tab interface:
  - Tab 1: Hero Dashboard & Live Classroom Glance
  - Tab 2: Dynamic Timetable & Period Timeline
  - Tab 3: Attendance History with Multi-criteria Filter & Keyword Search
  - Tab 4: Interactive Goal Simulator (75%, 80%, 85%, 90% targets) & Subject Analytics
- **`LoginActivity`**: Secure, minimalist credential screen with Keystore token handling and biometric convenience.
- **`dialog_subject_details.xml`**: Bottom sheet modal providing deep inspection into individual subject requirements and historical class sessions.

### 3.5 OS Integration Layer (`com.lloyd.attendance.widget`)
- **`AttendanceWidgetProvider`**: RemoteViews app widget rendering attendance metrics, live class context, and an on-demand refresh trigger.
- **`NotificationHelper`**: Custom Heads-Up Notification layout mimicking Apple Dynamic Island aesthetics to alert the student when attendance is recorded by faculty.
- **`AttendanceSyncWorker`**: WorkManager periodic sync job running in the background under network constraints.

---

## 4. Cross-Cutting Concerns

### 4.1 Error Handling & Resilience
- Errors are categorized as:
  - `NetworkException` (Timeout, DNS failure, offline): Silent fallback to cached data with "Offline" status tag.
  - `AuthException` (HTTP 401 unresolvable): Clean session expiration routing to LoginActivity.
  - `ServerException` (HTTP 500, 502): Informative user feedback without crashes or data wiping.

### 4.2 Offline Cache Integrity
- Disk storage is validated upon every read.
- Corrupted or partial payloads are safely caught and reset without crashing the application.
