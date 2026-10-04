# Lloyd ERP Security Assessment — Network & API Inventory

**Target Host:** `https://erp.lloydcollege.in`  
**API Prefix:** `/api`  
**Protocol:** HTTPS (TLS 1.2 / TLS 1.3 enforced)  
**Evidential Standard:** Strictly observed requests and verified responses. Zero invented routes.  

---

## 1. Master API Inventory Table

| ID | Feature | Method | Endpoint | Auth | Parameters | Response Type | Verified |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :---: |
| **API-001** | Student Sign-In | `POST` | `/api/auth/login` | None (Public) | JSON Body (`username`, `password`, `device_id`, etc.) | JSON (`status`, `data.access_token`, `data.user`) | ✓ |
| **API-002** | Token Renewal | `POST` | `/api/auth/refresh` | None (Body token) | JSON Body (`refresh_token`) | JSON (`status`, `data.access_token`, `data.refresh_token`) | ✓ |
| **API-003** | Monthly Attendance | `GET` | `/api/student/me/monthly-attendance` | `Bearer <JWT>` | None | JSON (`status`, `data.student_id`, `data.months[]`) | ✓ |
| **API-004** | Weekly Schedule | `GET` | `/api/student/me/weekly-attendance` | `Bearer <JWT>` | None | JSON (`status`, `data.days[].periods[]`) | ✓ |
| **API-005** | Student Class Ledger| `GET` | `/api/attendance/student` | `Bearer <JWT>` | Query: `student_id`, `page`, `page_size`, `sort_by`, `sort_dir` | JSON (`status`, `data[]`, `meta`) | ✓ |

---

## 2. Exhaustive Endpoint Specifications

### API-001: `/api/auth/login`
- **Method:** `POST`
- **Hostname:** `erp.lloydcollege.in`
- **Path:** `/api/auth/login`
- **Query Parameters:** None
- **Request Headers:**
  - `Content-Type: application/json; charset=utf-8`
  - `Accept: application/json`
  - `User-Agent: LloydERP-AndroidWidget/1.0`
- **Authentication:** None (Public Entrance)
- **Cookies:** None
- **Request Body (JSON):**
  ```json
  {
    "username": "<STUDENT_ADMISSION_NO>",
    "password": "<PLAINTEXT_PASSWORD>",
    "device_id": "c1f7a288-3482-411a-b30a-9d93b9ad4187",
    "app_version": "2.0.0",
    "timezone": "Asia/Kolkata",
    "browser_name": "AndroidApp",
    "browser_version": "1.0",
    "os_name": "Android",
    "device_type": "mobile"
  }
  ```
- **Response Content-Type:** `application/json; charset=utf-8`
- **Observed HTTP Status:** `200 OK` (Valid credentials), `401 Unauthorized` / `422 Unprocessable` (Invalid credentials)
- **Pagination:** None
- **Filtering / Sorting:** None
- **Approximate Response Size:** ~1.2 KB
- **Response Body Sample:**
  ```json
  {
    "status": true,
    "message": "Login successful",
    "data": {
      "access_token": "<REDACTED_JWT>",
      "refresh_token": "<REDACTED_REFRESH_TOKEN>",
      "expires_in": 86400,
      "profile_id": 28960,
      "name": "<REDACTED_STUDENT_NAME>",
      "role_id": 4,
      "school_id": 1,
      "user": {
        "id": 28960,
        "name": "<REDACTED_STUDENT_NAME>",
        "username": "<REDACTED_ADMISSION_NO>",
        "email": "<REDACTED_EMAIL>@lloydcollege.in",
        "role": "student",
        "admission_no": "<REDACTED_ADMISSION_NO>",
        "course": "B.Tech CSE",
        "semester": "1st Semester",
        "section": "A-1"
      }
    }
  }
  ```

---

### API-002: `/api/auth/refresh`
- **Method:** `POST`
- **Hostname:** `erp.lloydcollege.in`
- **Path:** `/api/auth/refresh`
- **Query Parameters:** None
- **Request Headers:**
  - `Content-Type: application/json; charset=utf-8`
  - `Accept: application/json`
- **Authentication:** Refresh Token in JSON body
- **Cookies:** None
- **Request Body (JSON):**
  ```json
  {
    "refresh_token": "<REDACTED_REFRESH_TOKEN>"
  }
  ```
- **Response Content-Type:** `application/json; charset=utf-8`
- **Observed HTTP Status:** `200 OK` (Valid refresh token), `401 Unauthorized` (Expired / Revoked)
- **Pagination:** None
- **Approximate Response Size:** ~850 bytes
- **Response Body Sample:**
  ```json
  {
    "status": true,
    "message": "Token refreshed successfully",
    "data": {
      "access_token": "<REDACTED_FRESH_JWT>",
      "refresh_token": "<REDACTED_FRESH_REFRESH_TOKEN>",
      "expires_in": 86400
    }
  }
  ```

---

### API-003: `/api/student/me/monthly-attendance`
- **Method:** `GET`
- **Hostname:** `erp.lloydcollege.in`
- **Path:** `/api/student/me/monthly-attendance`
- **Query Parameters:** None
- **Request Headers:**
  - `Authorization: Bearer <REDACTED_JWT>`
  - `Accept: application/json`
- **Authentication:** Bearer Token (Identity derived server-side via `/me`)
- **Cookies:** None
- **Response Content-Type:** `application/json; charset=utf-8`
- **Observed HTTP Status:** `200 OK`
- **Pagination:** None (Aggregates entire academic session)
- **Filtering / Sorting:** Chronological ascending by year and month
- **Approximate Response Size:** ~1.5 KB
- **Response Body Sample:**
  ```json
  {
    "status": true,
    "data": {
      "student_id": 28960,
      "student_name": "<REDACTED_STUDENT_NAME>",
      "months": [
        {
          "year": 2026,
          "month_number": 8,
          "month_label": "Aug 2026",
          "present": 22,
          "total": 35,
          "percentage": 62.9
        },
        {
          "year": 2026,
          "month_number": 9,
          "month_label": "Sep 2026",
          "present": 21,
          "total": 34,
          "percentage": 61.8
        },
        {
          "year": 2026,
          "month_number": 10,
          "month_label": "Oct 2026",
          "present": 2,
          "total": 5,
          "percentage": 40.0
        }
      ]
    }
  }
  ```

---

### API-004: `/api/student/me/weekly-attendance`
- **Method:** `GET`
- **Hostname:** `erp.lloydcollege.in`
- **Path:** `/api/student/me/weekly-attendance`
- **Query Parameters:** None
- **Request Headers:**
  - `Authorization: Bearer <REDACTED_JWT>`
  - `Accept: application/json`
- **Authentication:** Bearer Token (Identity derived server-side via `/me`)
- **Cookies:** None
- **Response Content-Type:** `application/json; charset=utf-8`
- **Observed HTTP Status:** `200 OK`
- **Pagination:** Returns active 7-day academic week
- **Approximate Response Size:** ~4.8 KB
- **Response Body Sample:**
  ```json
  {
    "status": true,
    "data": {
      "student_id": 28960,
      "student_name": "<REDACTED_STUDENT_NAME>",
      "week_start": "2026-09-28",
      "week_end": "2026-10-03",
      "earliest_week_start": "2026-08-01",
      "latest_week_start": "2026-10-05",
      "days": [
        {
          "date": "2026-09-28",
          "day": "Monday",
          "periods": [
            {
              "routine_id": 101,
              "subject_id": 501,
              "subject_name": "Applied Chemistry",
              "start_time": "09:00",
              "end_time": "09:50",
              "room_no": "NB-101",
              "status": "present",
              "teacher_name": "Dr. Vivek Das"
            },
            {
              "routine_id": 102,
              "subject_id": 502,
              "subject_name": "Applied Mathematics-I",
              "start_time": "09:50",
              "end_time": "10:40",
              "room_no": "NB-101",
              "status": "absent",
              "teacher_name": "Dr. Digvijay Singh"
            }
          ]
        }
      ]
    }
  }
  ```

---

### API-005: `/api/attendance/student`
- **Method:** `GET`
- **Hostname:** `erp.lloydcollege.in`
- **Path:** `/api/attendance/student`
- **Query Parameters:**
  - `student_id` (Integer, Required): The numeric identity of the student.
  - `page` (Integer, Optional, Default: 1): Page index.
  - `page_size` (Integer, Optional, Client uses 100): Page limit.
  - `sort_by` (String, Optional, Client uses `attendance_date`): Sort column.
  - `sort_dir` (String, Optional, Client uses `desc`): Sort direction.
- **Request Headers:**
  - `Authorization: Bearer <REDACTED_JWT>`
  - `Accept: application/json`
- **Authentication:** Bearer Token. **Security Issue:** Uses client-supplied `student_id` parameter rather than enforcing identity from JWT.
- **Cookies:** None
- **Response Content-Type:** `application/json; charset=utf-8`
- **Observed HTTP Status:** `200 OK`
- **Pagination:** Supported via metadata object (`total_items`, `current_page`, `page_size`, `total_pages`, `current_page_items`).
- **Filtering / Sorting:** Sortable by `attendance_date` ascending/descending.
- **Approximate Response Size:** ~18.5 KB (for 74 records)
- **Response Body Sample:**
  ```json
  {
    "status": true,
    "data": [
      {
        "id": 98214,
        "school_id": 1,
        "class_id": 12,
        "semester_id": 1,
        "section_id": 2,
        "subject_id": 304,
        "subject_name": "Applied Mathematics-I",
        "student_id": 28960,
        "student_name": "<REDACTED_STUDENT_NAME>",
        "roll_no": "26",
        "attendance_date": "2026-10-01",
        "status": "Present",
        "class_lecture": "3",
        "created_at": "2026-10-01 10:45:12",
        "created_by_name": "Dr. Digvijay Singh"
      }
    ],
    "meta": {
      "total_items": 74,
      "current_page": 1,
      "page_size": 100,
      "total_pages": 1,
      "current_page_items": 74
    }
  }
  ```
