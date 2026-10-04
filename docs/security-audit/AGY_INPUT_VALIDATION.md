# AGY Input Validation & Boundary Review — Lloyd ERP

**Standard:** OWASP API8:2023 Security Misconfiguration & Input Validation Patterns  
**Target:** `https://erp.lloydcollege.in/api`  
**Scope:** Controlled, Non-Destructive Boundary Probes on Read-Only Endpoints  

---

## 1. Input Validation Methodology

Input validation testing was conducted using safe, non-destructive boundary probes on authorized read-only requests (`/api/attendance/student?student_id=...`). The objective was to ascertain whether the backend handles abnormal parameter formats gracefully or leaks system internals via stack traces, SQL errors, or unhandled 500 exceptions.

---

## 2. Parameter Boundary Matrix

| Test ID | Parameter Tested | Probe Input | Expected Behavior | Actual Response | Security Evaluation |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **VAL-01** | `student_id` | Empty string (`?student_id=`) | `400 Bad Request` or `422` | `HTTP 422 Unprocessable Entity` ("student_id is required") | **PASS** |
| **VAL-02** | `student_id` | Non-numeric string (`?student_id=abc`) | `400 Bad Request` | `HTTP 422 Unprocessable Entity` ("student_id must be an integer") | **PASS** |
| **VAL-03** | `student_id` | Negative integer (`?student_id=-1`) | `400 Bad Request` or `404` | `HTTP 200 OK` (Empty data array `{data: []}`) | Acceptable |
| **VAL-04** | `page` | Negative index (`?page=-1`) | `400 Bad Request` | Normalized to page 1 (`current_page: 1`) | **PASS** (Defensive normalization) |
| **VAL-05** | `page` | Floating point (`?page=1.5`) | `400 Bad Request` | Truncated to integer 1 | **PASS** |
| **VAL-06** | `page_size` | Zero (`?page_size=0`) | `400 Bad Request` | Reset to default page size (20) | **PASS** |
| **VAL-07** | `page_size` | Excessively large (`?page_size=999999`) | `400 Bad Request` or capped at maximum (e.g., 100) | Clamped to institutional ceiling (100) | **PASS** (Protected against memory exhaustion) |
| **VAL-08** | `sort_dir` | Invalid enum (`?sort_dir=INVALID`) | `400 Bad Request` | Defaults safely to `asc` | **PASS** |
| **VAL-09** | `sort_by` | Non-existent column (`?sort_by=nonexistent_col`) | `400 Bad Request` | Ignored or defaults to primary key; no SQL error leaked | **PASS** |

---

## 3. Error Handling & Information Disclosure Review

### 3.1 Stack Trace & Database Error Audit
Across all boundary probes:
* **No Database Syntax Errors Leaked:** Responses never revealed raw SQL statements, database table names, or PDO/ORM exception traces.
* **No Unhandled HTTP 500 Errors:** The API consistently returned structured JSON payloads (`{"status": false, "message": "..."}`) with appropriate 4xx status codes.
* **No Debug Header Exposure:** Headers did not expose internal file paths, framework versions, or debugging signatures (e.g., `X-Debug-Token` or `X-Powered-By: Express/Laravel`).

### 3.2 Pagination Abuse Resilience (OWASP API4:2023)
* The client in `ErpApiClient.java` sets `page_size=100`.
* Probing with `page_size=999999` confirmed that the API gateway enforces an upper bound limit of **100 records per page**, preventing arbitrary denial-of-service via huge single-query allocations.

---

## 4. Remediation Recommendations

1. **Explicit Schema Validation:** Continue enforcing strict type assertions on all endpoint controllers using standard request validation schemas (e.g., JSON Schema, Pydantic, or Laravel FormRequest).
2. **Reject Rather Than Normalize Unsanitized Input:** For parameters like `page=-1` or `sort_dir=INVALID`, returning an explicit `HTTP 400 Bad Request` is preferred over silent normalization to prevent ambiguous client behavior.
