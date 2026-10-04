# Lloyd ERP Security Assessment — Risk Assessment Matrix

**Standard:** OWASP Risk Rating Methodology & NIST SP 800-30  
**Target Codebase:** `nkpcore/lloyd-erp` & `erp.lloydcollege.in`  

---

## 1. Risk Heatmap

```text
┌──────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│  LIKELIHOOD  │   INSIGNIF   │    MINOR     │   MODERATE   │    MAJOR     │
├──────────────┼──────────────┼──────────────┼──────────────┼──────────────┤
│ HIGH         │              │ FINDING-07   │ FINDING-04   │ FINDING-01   │
│              │              │ (Wildcard)   │ (Logcat)     │ (BOLA API1)  │
├──────────────┼──────────────┼──────────────┼──────────────┼──────────────┤
│ MEDIUM       │ FINDING-08   │ FINDING-06   │ FINDING-05   │ FINDING-02   │
│              │ (Schema Info)│ (Broadcast)  │ (No Logout)  │ (Password)   │
├──────────────┼──────────────┼──────────────┼──────────────┼──────────────┤
│ LOW          │              │              │              │ FINDING-03   │
│              │              │              │              │ (Scriptable) │
└──────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
```

---

## 2. Priority Ranking & Impact Summary

| Rank | Finding ID | Severity | Threat Area | Effort to Fix | Remediator |
| :---: | :--- | :---: | :--- | :---: | :--- |
| **P0** | **SEC-FINDING-001** | **CRITICAL** | BOLA Cross-Student Data Access | Low (1 day) | ERP Backend Team |
| **P1** | **SEC-FINDING-002** | **HIGH** | Plaintext Password Persistence | Low (1 day) | Mobile Dev Team |
| **P1** | **SEC-FINDING-003** | **HIGH** | Scriptable Plaintext Credentials | Low (1 day) | Mobile Dev Team |
| **P2** | **SEC-FINDING-004** | **MEDIUM** | Verbose Telemetry & Disabled R8 | Low (1 day) | Mobile Dev Team |
| **P2** | **SEC-FINDING-005** | **MEDIUM** | Lack of Server-Side Token Revocation | Med (2 days) | ERP Backend Team |
| **P3** | **SEC-FINDING-006** | **LOW** | Exported Receiver Without Signature | Low (1 day) | Mobile Dev Team |
| **P3** | **SEC-FINDING-007** | **LOW** | Permissive Wildcard CORS Policy | Low (1 day) | Infrastructure Team |
| **P4** | **SEC-FINDING-008** | **INFO** | Internal Schema FK Overexposure | Low (1 day) | ERP Backend Team |

---

## 3. Exploitability vs Business Impact Analysis

1. **BOLA in `/api/attendance/student` (P0)**:
   - **Exploitability**: Very High. Requires only an authenticated student session and modifying an integer in the query string.
   - **Business Impact**: Severe. Direct violation of Indian Digital Personal Data Protection (DPDP) Act and student privacy regulations.
2. **Plaintext Password Storage in Android (P1)**:
   - **Exploitability**: Moderate (requires device access or custom ROM fallback).
   - **Business Impact**: High. Student master credentials compromised, giving attackers access to institutional records and future fee transactions.
