# Material Design 3 Expressive Student Client - Design Specification

**Date:** 2026-10-03  
**Target:** Lloyd College Attendance Client (Android Native)  
**Style System:** Material Design 3 (Material You) Expressive + UI/UX Pro Max  
**Core Constraint:** Zero emojis, zero fake data, 100% offline-first speed for weak classroom networks, 4 clean tabs.

---

## 1. Design Tokens & Visual Hierarchy

### 1.1 Tonal Surfaces (Dark Theme)
- `surface`: `#0F172A` (Deep Slate background)
- `surfaceContainerLowest`: `#0B0F19` (Grounded recesses)
- `surfaceContainer`: `#1B243B` (Base container for list items)
- `surfaceContainerHigh`: `#222E49` (Elevated cards and dialogs)
- `surfaceContainerHighest`: `#2B3958` (Hover/press states & active navigation indicators)

### 1.2 Accent & Semantic Color Roles
- `primary`: `#818CF8` (Soft Indigo)
- `primaryContainer`: `#312E81` (Tonal pill fills)
- `onPrimaryContainer`: `#E0E7FF` (High contrast text on primary pills)
- `secondary`: `#38BDF8` (Sky Blue)
- `statusSafe`: `#10B981` (Emerald — Present / $\ge 75\%$)
- `statusDanger`: `#EF4444` (Rose — Absent / $< 75\%$)
- `statusWarning`: `#F59E0B` (Amber — Critical threshold 70-75%)
- `outlineVariant`: `#334155` (1dp crisp card and input strokes)

### 1.3 Shapes (MD3 Scale)
- `shapeExtraSmall`: 4dp (Status dots, subtle tags)
- `shapeSmall`: 8dp (Input fields, time badges)
- `shapeMedium`: 12dp (Period timeline cards, subject items)
- `shapeLarge`: 20dp (Hero cards, elevated dashboard panels)
- `shapeExtraLarge`: 28dp (Login card, bottom sheets)
- `shapeFull`: 9999dp (Buttons, filter chips, active navigation indicators)

### 1.4 Vector Iconography (Strictly No Emojis)
- Dashboard: `ic_dashboard.xml` (Geometric 4-pane grid)
- Schedule: `ic_calendar_today.xml` (Minimal calendar stroke)
- Attendance Log: `ic_history.xml` (Clockwise circular history arrow)
- Simulator & Analytics: `ic_tune.xml` (Clean slider adjust toggles)
- Connectivity: `ic_cloud_done.xml` / `ic_wifi_off.xml`
- Security / Privacy: `ic_visibility.xml` / `ic_visibility_off.xml` / `ic_fingerprint.xml`

---

## 2. Navigation & 4-Tab Architecture

```
┌────────────────────────────────────────────────────────┐
│ App Bar: Profile Pill | ● Live (Synced) | Refresh | Eye │
├────────────────────────────────────────────────────────┤
│                                                        │
│   [ TAB CONTENT CONTAINER: Smooth Cross-Fade ]          │
│                                                        │
│   1. Dashboard                                         │
│   2. Schedule (Section A-1)                            │
│   3. Attendance Log                                    │
│   4. Simulator & Analytics                             │
│                                                        │
├────────────────────────────────────────────────────────┤
│  [■] Dashboard  [■] Schedule  [■] History  [■] Sim     │
│             MD3 Pill Indicator Navigation              │
└────────────────────────────────────────────────────────┘
```

### Tab 1: Dashboard
- **Hero Attendance Card**:
  - `Display Medium` bold percentage (`60.8%`) with rolling number animation.
  - Safe bunk / shortage badge pill (`Need 42 consecutive lectures for 75%`).
  - Circular progress ring styled with tonal Emerald/Rose accents.
- **Metric Tiles (3-Column Grid)**:
  - Present: `45` (Emerald highlight)
  - Absent: `29` (Rose highlight)
  - Total Classes: `74` (Muted Slate highlight)
- **Live Classroom Glance Card**:
  - If Section A-1 has an ongoing lecture: Shows `ONGOING: Applied Mathematics-I (Dr. Digvijay Singh) in Room NB-101 • 24m remaining` with a breathing emerald pulse.
  - If class is upcoming: Shows `NEXT UP: 01:10 PM Fundamentals of Electronics Engineering (Dr. Ridhima) in NB-101`.

### Tab 2: Schedule (Section A-1 Timetable)
- **Section Isolation**:
  - Checks student section (`A-1` from profile or defaults to A-1).
  - Designed with an extensible `TimetableRepository` interface to effortlessly plug in other sections, syllabi, and notes later without rewriting UI code.
- **Day Selector Chips**:
  - Horizontal scroll of MD3 single-select chips: `Mon`, `Tue`, `Wed`, `Thu`, `Fri`, `Sat`.
- **Period Timeline**:
  - Periods I through VII (09:00 to 03:40) + Lunch Break (12:20–01:10) + Weekly Test (03:50–04:50).
  - All faculty mapped:
    - Fundamentals of Mechanical Engineering (`AME101`): `Mr. Mukesh Kumar (MK)` in Room `NB-101`
    - Applied Chemistry (`AAS102`): `Dr. Vivek Das (DVD)` in Room `NB-101`
    - Applied Mathematics-I (`AAS103`): `Dr. Digvijay Singh (DDS)` in Room `NB-101`
    - Fundamentals of Electronics Engineering (`AEC101`): `Dr. Ridhima (DR)` in Room `NB-101`
    - Programming Language (`ACS101`): `Pankaj Kumar (PK)` in Room `NB-101`
    - Environment & Sustainability (`AAC201`): `Dr. Divya Gairola (DDG)` in Room `NB-101`
    - Professional Communication (`AAS105`): `Ms. Mitali Gupta (MG)` in Room `NB-101`

### Tab 3: Attendance Log
- **Multi-Dimension Filter Row**:
  - Status Pills: `All (74)` | `Present (45)` | `Absent (29)`
  - Month Chips: `All Dates` | `Oct 2026` | `Sep 2026` | `Aug 2026`
  - Subject Chips: `All Subjects` | `Electronics` | `Maths` | `Chemistry` | etc.
- **Chronological List**:
  - Grouped by Date (e.g. `Thursday, Oct 01, 2026`) with summary badge (`2 Lectures • 0P / 2A`).
  - Left vertical accent bar (Emerald `#10B981` / Rose `#EF4444`).
  - Lecture number pill (`LEC #6`), faculty name, and mark timestamp (`Marked 10:52`).

### Tab 4: Simulator & Analytics
- **Interactive Bunk Simulator**:
  - Target selection chips: `75% Target (Mandatory)` vs `80% Target (Safety Margin)`.
  - Smooth MD3 continuous Slider (`-15` to `+30`).
  - Real-time rolling projected percentage.
  - Actionable advice: *"Attend next 5 classes to reach 64.6%"* or *"Can safely bunk next 2 classes"*.
- **Subject Analytics Cards**:
  - Each subject card shows individual attendance percentage bar, attended/total count, and safe bunk / shortage count.

---

## 3. Zero-Latency Offline-First Architecture

### 3.1 Startup Flow (< 30ms paint)
1. `MainActivity.onCreate()` reads cached stats and logs from `AppPreferences` immediately and populates all 4 tabs.
2. The user can interact with all 74 logs, timetable, and simulator instantly without waiting for network.
3. A background thread fires `ErpApiClient.fetchAndCalculateStats()` with a 5-second connection timeout.
4. If connection succeeds: silently updates data with smooth rolling numbers and turns the status pill to `● Live`.
5. If connection times out or cellular signal is weak in class: silently catches the error, maintains full offline usability, and displays `● Cached Today 10:45 AM`. Never interrupts the student with error popups.

---

## 4. Verification Plan

1. **Gradle Clean Build**:
   ```powershell
   cd c:\Users\nikhil\Desktop\erpwidgit\android
   C:\Users\nikhil\.gradle\wrapper\dists\gradle-8.10.2-bin\a04bxjujx95o3nb99gddekhwo\gradle-8.10.2\bin\gradle.bat assembleDebug
   ```
2. **Device QA on Motorola moto g85 5G**:
   - Install APK: `adb -s ZA222PG8PR install -r ...`
   - Test bottom navigation transitions across all 4 tabs.
   - Test Section A-1 schedule timeline and live ongoing class pulse.
   - Test offline mode (turn off mobile data / airplane mode) to confirm instant launch.
   - Verify zero emojis across all screens.
3. **Commit & Push to GitHub**:
   - Push to `https://github.com/nkpcore/lloyd-erp.git` on branch `main`.
