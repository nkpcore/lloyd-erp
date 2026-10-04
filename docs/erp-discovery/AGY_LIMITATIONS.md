# AGY Platform Limitations & Data Integrity Protocol — Lloyd ERP

**Standard:** Absolute Data Integrity & Non-Destructive Assessment Constraints  
**Target:** `https://erp.lloydcollege.in`  
**Application Scope:** Lloyd Student Android Application (`nkpcore/lloyd-erp`)  

---

## 1. Absolute Data Integrity Protocol

In compliance with professional engineering standards and academic research ethics, the following **Absolute Data Integrity Rules** are strictly enforced across the client codebase and documentation:

```text
REAL ERP
   ↓
VERIFIED API SURFACE
   ↓
SECURITY VALIDATION
   ↓
VERIFIED DATA MODEL
   ↓
SECURE ANDROID FEATURES
```

### Mandatory Rules:
1. **Zero Synthetic / Mock Production Data:**
   * Never inject fake mock data, artificial attendance percentages, or dummy subjects into release builds.
   * If an endpoint is offline, unreachable, or returns an empty collection, the client must display an explicit state:
     - `No Data`
     - `Unavailable`
     - `Not Exposed`
     - `Unknown`
2. **Purge All Hardcoded Entities:**
   * **Hardcoded Student IDs:** Purged. All identifiers must be extracted dynamically from the authenticated JWT session.
   * **Hardcoded Schedules:** Deprecate static timetable tables (`TimetableRepository.java`); parse dynamic routines from `/student/me/weekly-attendance`.
   * **Hardcoded Credentials:** Permanently prohibit hardcoded student credentials in scripts (`LloydWidget.scriptable.js`) or configuration files.

---

## 2. Features Blocked by Missing ERP Capabilities

The following capabilities are frequently requested by students, but are currently **unsupported** because the upstream Lloyd ERP does not expose corresponding REST API endpoints:

| Feature Proposed | ERP API Status | Architectural Limitation | Recommended Client UX State |
| :--- | :---: | :--- | :--- |
| **Exam Schedules & Admit Cards** | `NOT EXPOSED` | No examination endpoints exist under `/api/*`. | Omit tab or display "Exam module unavailable in ERP". |
| **Semester Marks & Grade Cards** | `NOT EXPOSED` | Result publication is not integrated into the REST API. | Display "Results must be viewed on web portal". |
| **Institutional Campus Notices** | `NOT EXPOSED` | Circulars are either PDF-hosted on external CMS or unexposed. | Omit notice board feature until backend support exists. |
| **Course Assignment Tracker** | `NOT EXPOSED` | No homework/assignment submission API endpoints exist. | Omit module to avoid misleading students. |
| **Fee Ledgers & Online Receipts**| `NOT EXPOSED` | Financial ledgers are unmapped on the student API surface. | Omit fee tracking; direct students to accounts office. |
| **Library Book Availability** | `NOT EXPOSED` | Library OPAC is unintegrated with the central ERP REST gateway. | Omit library features. |
| **Instant Server Push Alerts** | `NOT EXPOSED` | No Firebase Cloud Messaging (FCM) or WebSocket infrastructure. | Rely on local background polling (`WorkManager` 15-min cadence). |

---

## 3. Platform & Architectural Constraints

1. **Android Keystore Hardware Variation:**
   * Certain custom Android ROMs or older hardware throw cryptographic provider exceptions when initializing Android Keystore.
   * *Mitigation:* Catch the exception and refuse to persist credentials in unencrypted fallback XML. Prompt the user to log in per session.
2. **iOS Scriptable Runtime Constraints:**
   * Scriptable runs in an isolated third-party JavaScript sandbox with no direct access to the native iOS Keychain or secure enclave.
   * *Mitigation:* Replace the Scriptable script with a native Swift / WidgetKit iOS application using iOS Keychain Services.
3. **Background Battery & Network Throttling:**
   * Android Doze mode and manufacturer battery optimizations restrict background `WorkManager` execution when the device is idle.
   * *Mitigation:* Educate students to disable battery optimization for the app if they require guaranteed 15-minute attendance alerts.
