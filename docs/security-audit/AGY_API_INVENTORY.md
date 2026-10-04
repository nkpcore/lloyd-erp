# AGY API Inventory — Lloyd ERP Observed Network Surface

**Target System:** `https://erp.lloydcollege.in`  
**API Namespace:** `/api`  
**Protocol:** HTTPS / TLS 1.2+  
**Data Integrity:** Strictly observed, verified requests and server responses. Zero invented or synthetic endpoints.  

---

## 1. Master API Surface Table

| ID | Feature | HTTP Method | Endpoint | Query Parameters | Request Body | Auth Type | Status Code | Content-Type | Classification |
| :--- | :--- | :---: | :--- | :--- | :--- | :--- | :---: | :--- | :--- |
| **API-001** | Student Sign-In | `POST` | `/api/auth/login` | None | `{username, password, device_id, ...}` | None (Public) | `200 OK` | `application/json` | Public Entry |
| **API-002** | Token Renewal | `POST` | `/api/auth/refresh` | None | `{refresh_token}` | Body Token | `200 OK` | `application/json` | Authenticated |
| **API-003** | Monthly Summary | `GET` | `/api/student/me/monthly-attendance` | None | None | Bearer JWT | `200 OK` | `application/json` | Student-Specific |
| **API-004** | Weekly Schedule | `GET` | `/api/student/me/weekly-attendance` | None | None | Bearer JWT | `200 OK` | `application/json` | Student-Specific |
| **API-005** | Attendance Ledger | `GET` | `/api/attendance/student` | `student_id`, `page`, `page_size`, `sort_by`, `sort_dir` | None | Bearer JWT | `200 OK` | `application/json` | Sensitive Ledger |

---

## 2. Exhaustive Endpoint Specifications

### API-001: Student Authentication (`/api/auth/login`)
* **Method:** `POST`
* **Path:** `/api/auth/login`
* **Query Parameters:** None
* **Headers:**
  * `Content-Type: application/json; charset=utf-8`
  * `Accept: application/json`
  * `User-Agent: LloydERP-AndroidWidget/1.0`
* **Authentication:** None (Public)
* **Request Payload (JSON):**
  ```json
  {
    "username": "<STUDENT_ADMISSION_NO>",
    "password": "<STUDENT_PORTAL_PASSWORD>",
    "device_id": "c1f7a288-3482-411a-b30a-9d93b9ad4187",
    "app_version": "2.0.0",
    "timezone": "Asia/Kolkata",
    "browser_name": "AndroidApp",
    "browser_version": "1.0",
    "os_name": "Android",
    "device_type": "mobile"
  }
  ```
* **Response Status Codes:** `200 OK` (Success), `401 Unauthorized` (Bad Credentials), `422 Unprocessable` (Malformed JSON).
* **Response Content-Type:** `application/json; charset=utf-8`
* **Pagination:** None
* **Returned Fields:**
  * `status`: Boolean
  * `message`: String
  * `data.access_token`: Signed JWT Bearer token
  * `data.refresh_token`: Opaque string for token renewal
  * `data.expires_in`: Integer (86400 seconds / 24 hours)
  * `data.profile_id`: Integer
  * `data.role_id`: Integer (`4` = Student)
  * `data.school_id`: Integer (`1` = Engineering & Technology)
  * `data.user`: Object containing `id`, `name`, `username`, `email`, `role`, `admission_no`, `course`, `semester`, `section`

---

### API-002: Token Renewal (`/api/auth/refresh`)
* **Method:** `POST`
* **Path:** `/api/auth/refresh`
* **Query Parameters:** None
* **Headers:**
  * `Content-Type: application/json; charset=utf-8`
  * `Accept: application/json`
* **Authentication:** Refresh Token in JSON body
* **Request Payload (JSON):**
  ```json
  {
    "refresh_token": "<REDACTED_REFRESH_TOKEN>"
  }
  ```
* **Response Status Codes:** `200 OK` (Success), `401 Unauthorized` (Token expired or revoked).
* **Response Content-Type:** `application/json; charset=utf-8`
* **Pagination:** None
* **Returned Fields:**
  * `status`: Boolean
  * `message`: String
  * `data.access_token`: Fresh signed JWT
  * `data.refresh_token`: Rotated refresh token
  * `data.expires_in`: Integer (86400)

---

### API-003: Monthly Attendance Aggregate (`/api/student/me/monthly-attendance`)
* **Method:** `GET`
* **Path:** `/api/student/me/monthly-attendance`
* **Query Parameters:** None
* **Headers:**
  * `Authorization: Bearer <JWT_ACCESS_TOKEN>`
  * `Accept: application/json`
* **Authentication:** Bearer Token (Bound to session via `/me`)
* **Response Status Codes:** `200 OK` (Success), `401 Unauthorized` (Invalid/Missing Token).
* **Response Content-Type:** `application/json; charset=utf-8`
* **Pagination:** None (Aggregates complete current semester)
* **Returned Fields:**
  * `status`: Boolean
  * `data.student_id`: Integer
  * `data.student_name`: String
  * `data.months[]`: Array of monthly aggregate objects
    * `year`: Integer
    * `month_number`: Integer (1-12)
    * `month_label`: String (e.g., "Aug 2026", "Sep 2026", "Oct 2026")
    * `present`: Integer (Classes attended in month)
    * `total`: Integer (Classes conducted in month)
    * `percentage`: Float (Calculated attendance percentage)

---

### API-004: Weekly Routine & Schedule (`/api/student/me/weekly-attendance`)
* **Method:** `GET`
* **Path:** `/api/student/me/weekly-attendance`
* **Query Parameters:** None
* **Headers:**
  * `Authorization: Bearer <JWT_ACCESS_TOKEN>`
  * `Accept: application/json`
* **Authentication:** Bearer Token (Bound to session via `/me`)
* **Response Status Codes:** `200 OK` (Success), `401 Unauthorized` (Invalid/Missing Token).
* **Response Content-Type:** `application/json; charset=utf-8`
* **Pagination:** Active academic week
* **Returned Fields:**
  * `status`: Boolean
  * `data.student_id`: Integer
  * `data.student_name`: String
  * `data.week_start`: String (Date YYYY-MM-DD)
  * `data.week_end`: String (Date YYYY-MM-DD)
  * `data.days[]`: Array of days (Monday through Saturday)
    * `date`: String (Date YYYY-MM-DD)
    * `day`: String ("Monday", "Tuesday", etc.)
    * `periods[]`: Array of lecture period objects
      * `routine_id`: Integer
      * `subject_id`: Integer
      * `subject_name`: String (e.g., "Applied Chemistry", "Applied Mathematics-I")
      * `start_time`: String (HH:MM)
      * `end_time`: String (HH:MM)
      * `room_no`: String (e.g., "NB-101")
      * `status`: String ("present", "absent", or "unmarked")
      * `teacher_name`: String (Faculty title and legal name)

---

### API-005: Student Class Attendance Ledger (`/api/attendance/student`)
* **Method:** `GET`
* **Path:** `/api/attendance/student`
* **Query Parameters:**
  * `student_id` (Integer, Required): Numeric student database identifier.
  * `page` (Integer, Optional, Default: 1): Page number for pagination.
  * `page_size` (Integer, Optional, Default: 20, Client uses 100): Maximum records per page.
  * `sort_by` (String, Optional, Client uses `attendance_date`): Sort column.
  * `sort_dir` (String, Optional, Client uses `desc`): Sort direction (`asc` or `desc`).
* **Headers:**
  * `Authorization: Bearer <JWT_ACCESS_TOKEN>`
  * `Accept: application/json`
* **Authentication:** Bearer Token. *(Vulnerable to BOLA: trusts client-supplied query parameter).*
* **Response Status Codes:** `200 OK` (Success), `401 Unauthorized` (Missing Token).
* **Response Content-Type:** `application/json; charset=utf-8`
* **Pagination:** Paginated response with metadata block.
* **Returned Fields:**
  * `status`: Boolean
  * `data[]`: Array of attendance ledger entries
    * `id`: Integer (Unique attendance transaction ID)
    * `student_id`: Integer
    * `student_name`: String
    * `roll_no`: String
    * `attendance_date`: String (Date YYYY-MM-DD)
    * `status`: String ("Present" or "Absent")
    * `subject_id`: Integer
    * `subject_name`: String
    * `created_by_name`: String (Faculty member who recorded attendance)
    * `created_at`: String (Timestamp YYYY-MM-DD HH:MM:SS)
  * `meta`: Object
    * `total_items`: Integer (Total records available)
    * `current_page`: Integer
    * `page_size`: Integer
    * `total_pages`: Integer
    * `current_page_items`: Integer
