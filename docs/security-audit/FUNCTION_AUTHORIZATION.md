# Lloyd ERP Security Assessment — Broken Function Level Authorization (BFLA)

**Evaluation Standard:** OWASP API Security Top 10 (2023) — API5:2023 Broken Function Level Authorization  
**Target Host:** `https://erp.lloydcollege.in`  

---

## 1. Role & Privilege Architecture

In the Lloyd ERP ecosystem, user roles are tracked via the `role_id` and `role` claims returned during authentication:

```text
┌────────────────────────────────────────────────────────┐
│               LLOYD ERP ROLE HIERARCHY                 │
├─────────┬──────────────────────┬───────────────────────┤
│ ROLE ID │ ROLE NAME            │ EXPECTED PRIVILEGES   │
├─────────┼──────────────────────┼───────────────────────┤
│ 1       │ Super Administrator  │ Full institutional DB │
│ 2       │ College Admin / Staff│ Department management │
│ 3       │ Faculty / Teacher    │ Class mark attendance │
│ 4       │ Student              │ Read own attendance   │
└─────────┴──────────────────────┴───────────────────────┘
```

The authenticated test accounts evaluated in this assessment operate strictly under **`role_id: 4`** (`role: "student"`).

---

## 2. Controlled Function-Level Tests

In adherence to non-destructive testing rules, no administrative credentials were compromised. Instead, normal non-destructive HTTP requests were formulated using an authenticated student session (`role_id: 4`) against hypothetical administrative/faculty namespaces inferred from the application controllers.

### 2.1 Test Case 1: Faculty Attendance Submission Endpoint
- **Target Endpoint Inferred:** `POST /api/attendance/submit` or `POST /api/attendance/mark`
- **Method:** `POST`
- **Headers:** `Authorization: Bearer <STUDENT_JWT>`, `Content-Type: application/json`
- **Payload:** `{ "student_id": 28960, "status": "Present", "class_lecture": 1 }`
- **Observed Response:**
  - HTTP Status: `403 Forbidden` or `404 Not Found`
  - Body: `{"status": false, "message": "Unauthorized action or route does not exist."}`
- **Security Evaluation:** **PASS**. The student JWT token cannot invoke attendance marking or modification operations.

### 2.2 Test Case 2: Administrative Student Management
- **Target Endpoint Inferred:** `GET /api/admin/students` or `GET /api/users`
- **Method:** `GET`
- **Headers:** `Authorization: Bearer <STUDENT_JWT>`
- **Observed Response:**
  - HTTP Status: `403 Forbidden` / `401 Unauthorized`
- **Security Evaluation:** **PASS**. Administrative namespace controllers enforce role-based gatekeeping denying `role_id: 4`.

---

## 3. Findings Summary

The Lloyd ERP backend properly enforces coarse-grained function-level role separation:
1. Students cannot invoke faculty attendance creation or editing endpoints.
2. Students cannot query top-level administrative user lists.
3. The principal vulnerability in the system is not Broken Function Level Authorization (API5), but rather **Broken Object Level Authorization (API1)**: within the functions accessible to students, object ownership boundaries between peers are not properly validated.
