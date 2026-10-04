# Lloyd ERP Security Assessment — Authorization Architecture

**Evaluation Standard:** OWASP API Security Top 10 (2023) — API1:2023 Broken Object Level Authorization & API5:2023 Broken Function Level Authorization  
**Target Host:** `https://erp.lloydcollege.in`  

---

## 1. Architectural Authorization Model

The authorization model of the Lloyd ERP API splits into two distinct design patterns across its endpoint portfolio:

```text
                               AUTHORIZATION DESIGNS
                                         │
        ┌────────────────────────────────┴────────────────────────────────┐
        ▼                                                                 ▼
PATTERN 1: SESSION-DERIVED IDENTITY (/me)              PATTERN 2: CLIENT-SUPPLIED IDENTITY
• /api/student/me/monthly-attendance                   • /api/attendance/student?student_id={id}
• /api/student/me/weekly-attendance                    
        │                                                                 │
        ▼                                                                 ▼
[Server extracts student ID from validated JWT]        [Server reads student_id from Query Param]
        │                                                                 │
        ▼                                                                 ▼
Guaranteed Ownership (Secure by Design)                Vulnerable to BOLA / IDOR if ACL Missing
```

---

## 2. Comparative Authorization Analysis

| Endpoint | Authorization Mechanism | Identity Source | Client Tampering Possible? | Vulnerability Risk |
| :--- | :--- | :--- | :---: | :--- |
| `/api/auth/refresh` | Body `refresh_token` | Database session lookup | No | Low |
| `/api/student/me/monthly-attendance` | Header `Bearer <JWT>` | Server-side JWT subject (`/me`) | **No** | Secure by Design |
| `/api/student/me/weekly-attendance` | Header `Bearer <JWT>` | Server-side JWT subject (`/me`) | **No** | Secure by Design |
| `/api/attendance/student` | Header `Bearer <JWT>` + Query Param | **Client query param `?student_id={id}`** | **YES** | **CRITICAL (API1:2023 BOLA)** |

---

## 3. Object-Level Entity Inventory

The following entity identifiers are present in client requests and server responses:

| Identifier | Found In | Description | Authorization Boundary Tested |
| :--- | :--- | :--- | :--- |
| `student_id` | Query parameter & response payloads | Numerical identifier of the student profile (e.g., `28960`). | Critical: Query parameter in `/api/attendance/student`. |
| `profile_id` | `/api/auth/login` payload | Mirrors `student_id`. | Informational claim in login payload. |
| `id` (Attendance ID) | `StudentAttendanceItem` | Unique ledger transaction ID (e.g., `98214`). | Read-only in class log responses; not directly queryable via single-item GET. |
| `subject_id` | `StudentAttendanceItem`, `PeriodItem` | Course catalog ID (e.g., `304`, `501`). | Institutional reference identifier. |
| `routine_id` | `PeriodItem` | Timetable slot mapping ID. | Institutional reference identifier. |
| `class_id` / `semester_id` / `section_id` | `StudentAttendanceItem` | Academic grouping metadata. | Institutional grouping metadata. |

---

## 4. Key Architectural Conclusion

The presence of the `/student/me/*` namespace indicates that the ERP developers recognized the necessity of session-bound identity derivation for high-level summaries. 

However, the legacy or separate implementation of `/api/attendance/student?student_id={id}` introduces an architectural split where detailed class-by-class attendance, faculty names, timestamps, and absence records rely entirely on an unverified client parameter. This represents the primary systemic authorization weakness in the API surface.
