# AGENTS.md — Global Engineering Directives

## 🚨 TOP PRIORITY MANDATE: ZERO HARDCODING POLICY

### 1. Absolute Prohibition of Hardcoded Data & Fallbacks
- **Zero Mock / Fake Data**: NEVER hardcode mock student IDs (e.g. `28960`), fabricated student names (`"Nikhil"`, `"Nikhil Pandey"`, `"John Doe"`), fake sections (`"A-1"`, `"CSE-1"`), fake attendance percentages, or dummy lecture timetables in production application logic.
- **Strict Dynamic Grounding**: Every data point displayed, exported, or computed by the app MUST originate from:
  1. Authenticated ERP network responses (`/api/auth/login`, `/api/student/me/monthly-attendance`, `/api/attendance/student`).
  2. Local persistent encrypted storage or Room database representing authenticated data.
  3. Dynamic user inputs or user profile state.
- **No Dummy Fallback Strings**: When a field (such as student name, section, branch, roll number, faculty name) is missing or blank, the app MUST NOT replace it with a dummy or hardcoded identity string (e.g., do NOT fall back to `"Lloyd Student"`, `"Nikhil"`, etc.). Use clean structural placeholders (e.g., `""`, `"--"`, `"Not available"`) or omit the field.

### 2. Zero Developer Branding in Functional/Export Data
- **No Author Tags in User Reports**: Exported student reports (PDF, CSV, plain text, Android ShareSheet payloads) are official student academic documents. They MUST NEVER contain developer attributions, author watermarks, or personal branding.
- **Attribution Isolation**: Developer credit (e.g. *"Crafted by Nikhil Pandey"*) is strictly constrained to static informational screens (About dialog, bottom footer of Settings/Profile) explicitly defined in the design spec. It MUST NEVER be injected into data models, business logic, attendance exports, or calculation engines.

### 3. Identity & BOLA Enforcement
- **Session Identity Lock**: `student_id` is strictly bound to the authenticated user ID received upon login. The app must never query, default to, or allow querying arbitrary student IDs.
- **Hardware Keystore Vault**: Credentials and tokens are stored in the Hardware Keystore (AES-256-GCM). Never hardcode API keys, secrets, or fallback credentials in code.

### 4. Dynamic Routine & Timetable Rule
- Never fall back to a hardcoded section (e.g. Section A-1) when timetable data is missing. Display `"Schedule unavailable"` or empty state.

---

## 2. Architecture & Design Principles

1. **Production-Grade Material 3 Expressive**:
   - Palette: Honey Bronze (`#F6BD60`), Linen (`#F7EDE2`), Cotton Rose (`#F5CAC3`), Muted Teal (`#84A59D`), Light Coral (`#F28482`).
   - `dynamicColor = false` to preserve brand aesthetic across all Android 12+ devices.
2. **Real Reconciliation Engine**:
   - Reconcile monthly attendance totals with class-level daily logs with zero discrepancies.
3. **Decoupled Math Domain**:
   - Mathematical attendance calculation rules ($75\%$, $80\%$, $85\%$, $90\%$) are pure Kotlin functions, unit-tested with boundary cases ($0/0 \to \text{NoData}$).
4. **Verification Gate**:
   - Run unit tests (`./gradlew testDebugUnitTest`) before committing any task. No speculative completions.

---

## 3. 🚨 Pre-Push & OTA Release Integrity Gate (MANDATORY CROSS-CHECK)

Before ANY `git push`, release deployment, or PR merge, you MUST perform this strict end-to-end cross-check:

1. **Tag Alignment**:
   - Always run `git fetch --tags` to ensure local tags match remote tags.
   - Verify local `versionName` matches the latest release tag or targeted increment.
2. **Zero Stale APK Serving**:
   - Never serve or redirect to outdated static local/Vercel files when a newer GitHub Release asset exists.
   - Dynamic endpoints (`/api/ota`, `/api/config`) must resolve and redirect directly to the live GitHub Release asset matching the version.
3. **No Infinite Update Loops**:
   - The APK delivered by the download URL must contain the exact `versionName` advertised as `latest_version`.
   - Never repeatedly prompt or nag the user with modal update dialogs in `onResume()`.
   - Honor user dismissals: save `lastDismissedUpdateVersion` and suppress non-mandatory update dialogs once dismissed by the user.
   - Only block with non-dismissible lockout screens when the update is mandatory (`currentVersionCode < minVersionCode`).
4. **Build & Release Cross-Check**:
   - Cleanly verify `./gradlew testDebugUnitTest` passes 100%.
   - Verify `./gradlew assembleRelease` builds without warnings or errors.
   - Inspect `git diff` for accidental regressions, mock data, or hardcoded fallbacks before finishing.
