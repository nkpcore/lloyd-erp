# Lloyd ERP Security Assessment — Broken Object Property Authorization

**Evaluation Standard:** OWASP API Security Top 10 (2023) — API3:2023 Broken Object Property Authorization  
**Target Host:** `https://erp.lloydcollege.in`  

---

## 1. Property Exposure Assessment

Broken Object Property Authorization occurs when an API endpoint exposes more properties and fields than necessary (Excessive Data Exposure / Mass Exposure) or allows clients to alter properties they shouldn't (Mass Assignment).

In accordance with our authorized non-destructive testing scope, this assessment analyzes **Read-Side Property Exposure** across verified response payloads.

---

## 2. Field-by-Field Exposure Analysis

### 2.1 `/api/auth/login` Payload Properties

```json
{
  "status": true,
  "message": "Login successful",
  "data": {
    "access_token": "...",
    "refresh_token": "...",
    "expires_in": 86400,
    "profile_id": 28960,
    "name": "STUDENT NAME",
    "role_id": 4,
    "school_id": 1,
    "user": {
      "id": 28960,
      "name": "STUDENT NAME",
      "username": "ADMISSION_NO",
      "email": "student@lloydcollege.in",
      "role": "student",
      "admission_no": "ADMISSION_NO",
      "course": "B.Tech CSE",
      "semester": "1st Semester",
      "section": "A-1"
    }
  }
}
```

| Field | Sensitivity Classification | Required by UI? | Risk Evaluation |
| :--- | :--- | :---: | :--- |
| `access_token` / `refresh_token` | **HIGHLY-SENSITIVE** | Yes | Session credentials. Must be stored strictly in hardware keystore. |
| `role_id` (`4`) | **INTERNAL** | No | Internal database role identifier. Exposes role enumeration schema. |
| `school_id` (`1`) | **INTERNAL** | No | Multitenancy tenant identifier. Useful for multitenant scoping. |
| `profile_id` / `user.id` | **STUDENT-SPECIFIC** | Yes | Primary key. Used in subsequent API calls. |
| `user.email` | **SENSITIVE** | No | Student institutional email address. Not rendered in current Android UI. |
| `password` / `hash` | **HIGHLY-SENSITIVE** | — | **PASS**: No password hashes, salts, or recovery secrets are returned. |

---

### 2.2 `/api/attendance/student` Payload Properties

```json
{
  "id": 98214,
  "school_id": 1,
  "class_id": 12,
  "semester_id": 1,
  "section_id": 2,
  "subject_id": 304,
  "subject_name": "Applied Mathematics-I",
  "student_id": 28960,
  "student_name": "STUDENT NAME",
  "roll_no": "26",
  "attendance_date": "2026-10-01",
  "status": "Present",
  "class_lecture": "3",
  "created_at": "2026-10-01 10:45:12",
  "created_by_name": "Dr. Digvijay Singh"
}
```

| Field | Sensitivity Classification | Required by UI? | Risk Evaluation |
| :--- | :--- | :---: | :--- |
| `id` | **INTERNAL** | Yes | Transaction ID (used for diffing notifications). |
| `school_id` (`1`) | **INTERNAL** | No | Internal multitenant database ID. |
| `class_id` (`12`) | **INTERNAL** | No | Internal curriculum database foreign key. |
| `semester_id` (`1`) | **INTERNAL** | No | Internal academic session foreign key. |
| `section_id` (`2`) | **INTERNAL** | No | Internal section grouping ID. |
| `subject_id` (`304`) | **INTERNAL** | No | Internal course catalog key. |
| `created_at` | **STUDENT-SPECIFIC** | Yes | Exact server timestamp of submission. |
| `created_by_name` | **ADMINISTRATIVE** | Yes | Name of teacher marking record. |

---

## 3. Data Overexposure Findings

1. **Internal Database Foreign Key Leakage**:
   - The `/attendance/student` endpoint returns five distinct database foreign keys (`school_id`, `class_id`, `semester_id`, `section_id`, `subject_id`) on every record.
   - For 100 paginated records, this inflates payload size unnecessarily and exposes backend schema relational details.
2. **Duplicative Identity Claims**:
   - `student_name` and `roll_no` are redundantly repeated inside every single array entry in `/attendance/student`, despite all records belonging to the same query subject.
3. **Mass Assignment Posture**:
   - Since students cannot update attendance records (all student endpoints are read-only `GET`), mass assignment vulnerabilities are not exploitable from the student role.
