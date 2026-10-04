# Lloyd ERP — Assessment Limitations & Boundaries

**Assessment Context:** Authorized Academic Capstone Security & API Discovery Evaluation  
**Engagement Rule:** Strict non-destructive testing boundaries.  

---

## 1. Ethical & Authorization Boundaries

To preserve the operational integrity of Lloyd College's production ERP infrastructure (`erp.lloydcollege.in`) and protect fellow students, the following constraints were strictly observed:

### 1.1 Non-Destructive Testing Mandate
- **No Record Modification**: Under no circumstances was write/mutation testing performed to alter attendance statuses, modify student marks, adjust fee structures, or delete academic records.
- **No Credential Brute-Forcing**: Dictionary attacks, automated password sprays, and OTP brute-forcing were prohibited.
- **No Denial-of-Service (DoS)**: Rate-limiting tests were conducted using passive inspection and low-volume normal requests. Automated fuzzing and request-flooding tools were not deployed against production.
- **No Unconsented Data Enumeration**: Although BOLA vulnerability in `/api/attendance/student?student_id={id}` was confirmed via controlled cross-testing between two pre-authorized test accounts (`<TEST_STUDENT_A>` and `<TEST_STUDENT_B>`), no horizontal scraping or student enumeration was conducted.

---

## 2. API Surface Limitations (Unobserved Modules)

The following academic modules are commonly found in comprehensive institutional ERP systems, but **could not be verified** during this assessment:

| Module | Observed Status | Reason for Omission in Current Discovery |
| :--- | :---: | :--- |
| **Examinations & Admit Cards** | **UNKNOWN** | No exam endpoint is called in the `nkpcore/lloyd-erp` repository or observed during normal student navigation. |
| **Semester Grade Sheets / Results** | **UNKNOWN** | No grade or marks endpoints were discovered within the student API scope. |
| **Institutional Notices & Circulars**| **UNKNOWN** | Notices are not currently surfaced via the REST API endpoints consumed by the client. |
| **Assignments & Submissions** | **UNKNOWN** | Academic assignment features were not observed in the student API contract. |
| **Fee Ledgers & Online Payments** | **UNKNOWN** | Fee payment gateways and transaction ledgers are either served via separate web portals or not exposed via `/api/`. |
| **Library Management** | **UNKNOWN** | No book checkout, return, or library search endpoints were present. |
| **Push Notification Server (FCM)** | **NOT AVAILABLE** | The ERP lacks a cloud messaging gateway; student alerts must be generated via local client polling (`WorkManager`). |

---

## 3. Platform & Client Limitations

1. **Android Keystore Hardware Variation**:
   - `EncryptedSharedPreferences` relies on Android Keystore. On certain non-standard OEM devices, initialization exceptions trigger fallback branches. Hardening the fallback to refuse plaintext storage is essential.
2. **iOS Scriptable Script Constraints**:
   - `LloydWidget.scriptable.js` operates within the sandboxed JavaScript environment of the third-party Scriptable app. It cannot leverage the iOS native Keychain API, forcing users to hardcode credentials.
3. **Absence of Server-Side WebSockets**:
   - The absence of WebSockets or SSE (Server-Sent Events) means real-time attendance alerts have a 15-minute latency floor governed by Android WorkManager battery constraints.
