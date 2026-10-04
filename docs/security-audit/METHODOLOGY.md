# Lloyd ERP Security Assessment — Methodology & Assessment Framework

**Framework Standard:** OWASP API Security Top 10 (2023) & OWASP Mobile Application Security Verification Standard (MASVS v2.0)  
**Target Codebase:** `nkpcore/lloyd-erp`  
**Host Target:** `https://erp.lloydcollege.in`  

---

## 1. Assessment Standards & Classification

The assessment methodology follows an adversarial yet strictly non-destructive testing lifecycle tailored for higher-education academic platforms and their accompanying native client applications.

```text
┌────────────────────────────────────────────────────────────────────────┐
│                      SECURITY ASSESSMENT LIFECYCLE                     │
├──────────────────┬──────────────────┬─────────────────┬────────────────┤
│    DISCOVERY     │  AUTHENTICATION  │  AUTHORIZATION  │ CLIENT POSTURE │
│  • Repo Forensics│  • JWT Analysis  │  • BOLA (API1)  │  • Keystore    │
│  • SPA Mapping   │  • Refresh Loop  │  • Property Auth│  • Logcat Dumps│
│  • API Inventory │  • Cookie/Session│  • Function Auth│  • Receivers   │
└────────┬─────────┴────────┬─────────┴────────┬────────┴────────┬───────┘
         │                  │                  │                 │
         └──────────────────┼──────────────────┼─────────────────┘
                            ▼
              ┌───────────────────────────┐
              │    EVIDENCE & TRIAGE      │
              │  • Controlled Cross-Tests │
              │  • OWASP Risk Scoring     │
              │  • Root Cause Diagnostics │
              └─────────────┬─────────────┘
                            ▼
              ┌───────────────────────────┐
              │    REMEDIATION ROADMAP    │
              └───────────────────────────┘
```

### Classification Strictness
Every finding and endpoint status in this report is maintained under three rigorous evidential states:
- **`VERIFIED`**: Directly observed through executed network requests, live server responses, or static source code analysis with confirmed runtime traces.
- **`INFERRED`**: Strongly suggested by architectural clues, naming conventions, or standard ERP framework patterns, but lacking direct wire verification.
- **`UNKNOWN`**: No empirical evidence exists; hypothetical endpoints are strictly forbidden from being classified as existing.

---

## 2. OWASP API Security Top 10 (2023) Mapping

| OWASP API Risk | Assessment Focus | Test Strategy |
| :--- | :--- | :--- |
| **API1:2023 BOLA** | User-supplied IDs in object lookup | Test Account A requests Identity B's logs via `/api/attendance/student?student_id={B}`. |
| **API2:2023 Broken Auth** | Token handling, session renewal, password storage | Analyze JWT claim structure, expiration enforcement, `/auth/refresh` validation, logout revocation. |
| **API3:2023 Broken Object Property** | Excessive data exposure & mass assignment | Review API response JSON payloads for unnecessary fields (passwords, emails, roles, internal IDs). |
| **API4:2023 Unrestricted Resource** | Rate limits, pagination caps, query size | Verify if pagination parameters (`page_size=100`) have backend upper limits or permit memory exhaustion. |
| **API5:2023 Broken Function Level** | Administrative / Faculty endpoint access | Attempt to call teacher or admin functions using student JWT token; verify HTTP 403 Forbidden enforcement. |
| **API6:2023 Sensitive Business Flows** | Attendance tampering & attendance manipulation | Verify that student endpoints are strictly read-only and disallow attendance self-marking or alteration. |
| **API7:2023 SSRF** | Parameter-driven webhook/URL ingestion | Inspect URL parameters in API calls for backend URL fetching vectors. |
| **API8:2023 Security Misconfiguration** | TLS configuration, headers, error disclosures | Audit HTTP response headers, CORS preflights, HSTS headers, stack trace leakage on error. |
| **API9:2023 Improper Inventory** | Undocumented endpoints, legacy routes, versioning | Identify divergence between documented APIs, web SPA bundles, and mobile client implementations. |
| **API10:2023 Unsafe API Consumption** | Third-party dependencies & upstream trust | Audit client-side parsing of untrusted JSON payloads without validation or bounds checking. |

---

## 3. Mobile Security (MASVS) Verification Standards

1. **MASVS-STORAGE (Data Storage & Privacy)**:
   - Verify that credentials and tokens are stored using hardware-backed keystore mechanisms (`MasterKey` + `EncryptedSharedPreferences`).
   - Audit fallback branches to ensure failed keystore initialization does not revert to plaintext storage.
   - Inspect local caches for sensitive data remnants.
2. **MASVS-CRYPTO (Cryptographic Practices)**:
   - Ensure standard algorithms (AES-256-GCM, AES-256-SIV) are employed.
   - Verify zero hardcoded cryptographic keys or initialization vectors.
3. **MASVS-NETWORK (Network Communication)**:
   - Inspect `network_security_config.xml` for cleartext traffic bans (`cleartextTrafficPermitted="false"`).
   - Evaluate certificate pinning and system CA trust anchors.
4. **MASVS-CODE (Code Quality & Build Settings)**:
   - Review ProGuard / R8 minification and obfuscation settings in `build.gradle`.
   - Audit system logcat outputs for token or credential leakage (`android.util.Log`).
   - Verify IPC attack surface (exported activities, broadcast receivers, pending intents).

---

## 4. Root Cause Diagnostic Standard

In accordance with root-cause diagnostic engineering, every finding distinguishes:
- **Symptom**: The observable failure or data leak visible in the user interface, network inspector, or logs.
- **Contributing Factor**: Architectural or implementation circumstances that enabled or compounded the issue (e.g., absence of automated tests, fallback logic, rapid prototyping).
- **Root Cause**: The foundational flaw in logic, design, or trust boundary configuration that created the vulnerability.
