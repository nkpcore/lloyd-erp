# Lloyd ERP — Verified API Endpoint Catalog

**Base URL:** `https://erp.lloydcollege.in/api`  
**Protocol:** HTTPS (TLS 1.2 / 1.3)  
**Authentication Standard:** HTTP Authorization Header (`Bearer <JWT>`)  
**Data Exchange Format:** `application/json; charset=utf-8`  

---

## 1. Overview of Verified Endpoints

| ID | Method | Endpoint Path | Primary Purpose | Auth Scope | Status |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **EP-01** | `POST` | `/auth/login` | Student credential authentication & token issuance | Public | **VERIFIED** |
| **EP-02** | `POST` | `/auth/refresh` | Silent token renewal using refresh token | Public (Body) | **VERIFIED** |
| **EP-03** | `GET` | `/student/me/monthly-attendance` | Session-bound aggregated monthly attendance metrics | Bearer JWT | **VERIFIED** |
| **EP-04** | `GET` | `/student/me/weekly-attendance` | Session-bound weekly schedule routine & period logs | Bearer JWT | **VERIFIED** |
| **EP-05** | `GET` | `/attendance/student` | Paginated semester attendance transaction ledger | Bearer JWT | **VERIFIED** |

---

## 2. Exhaustive Endpoint Specifications

### EP-01: Student Authentication (`POST /auth/login`)

#### Purpose
Authenticates enrolled students using their college admission number and master password. Issues the initial JWT bearer access token, long-lived refresh token, and student profile details.

#### Request Specification
- **Method:** `POST`
- **URL:** `https://erp.lloydcollege.in/api/auth/login`
- **Headers:**
  ```http
  Content-Type: application/json; charset=utf-8
  Accept: application/json
  User-Agent: LloydERP-AndroidWidget/1.0
  ```
- **Body Schema (JSON):**
  ```json
  {
    "username": "<STUDENT_ADMISSION_NO>",
    "password": "<PLAINTEXT_PASSWORD>",
    "device_id": "<UUID_STRING>",
    "app_version": "2.0.0",
    "timezone": "Asia/Kolkata",
    "browser_name": "AndroidApp",
    "browser_version": "1.0",
    "os_name": "Android",
    "device_type": "mobile"
  }
  ```

#### Response Specification
- **Success Status Code:** `200 OK`
- **Failure Status Codes:** `401 Unauthorized` (Invalid credentials), `422 Unprocessable Entity` (Missing fields)
- **Success Response Schema:**
  ```json
  {
    "status": true,
    "message": "Login successful",
    "data": {
      "access_token": "string (JWT)",
      "refresh_token": "string (Opaque Token)",
      "expires_in": 86400,
      "profile_id": 28960,
      "name": "string",
      "role_id": 4,
      "school_id": 1,
      "user": {
        "id": 28960,
        "name": "string",
        "username": "string",
        "email": "string",
        "role": "student",
        "admission_no": "string",
        "course": "string",
        "semester": "string",
        "section": "string"
      }
    }
  }
  ```

#### Field Dictionary
- `data.access_token`: Base64URL encoded RFC 7519 JSON Web Token valid for 86,400 seconds (24 hours).
- `data.refresh_token`: Opaque token used to request new access tokens at `/auth/refresh`.
- `data.profile_id`: Integer primary key matching `user.id`.
- `data.role_id`: Role identifier (`4` represents enrolled student).
- `data.user.section`: Assigned classroom cohort (e.g., `"A-1"`, `"B-2"`). Crucial for dynamic timetable resolution.

---

### EP-02: Token Refresh (`POST /auth/refresh`)

#### Purpose
Exchanges an existing valid refresh token for a newly generated access token without requiring re-prompting the student for their password.

#### Request Specification
- **Method:** `POST`
- **URL:** `https://erp.lloydcollege.in/api/auth/refresh`
- **Headers:**
  ```http
  Content-Type: application/json; charset=utf-8
  Accept: application/json
  ```
- **Body Schema (JSON):**
  ```json
  {
    "refresh_token": "<REFRESH_TOKEN_STRING>"
  }
  ```

#### Response Specification
- **Success Status Code:** `200 OK`
- **Failure Status Code:** `401 Unauthorized` (Refresh token expired or revoked)
- **Success Response Schema:**
  ```json
  {
    "status": true,
    "message": "Token refreshed successfully",
    "data": {
      "access_token": "string (JWT)",
      "refresh_token": "string (Opaque Token)",
      "expires_in": 86400
    }
  }
  ```

---

### EP-03: Monthly Attendance Aggregates (`GET /student/me/monthly-attendance`)

#### Purpose
Retrieves aggregated monthly attendance figures (attended count, conducted total, calculated percentage) for the authenticated student.

#### Request Specification
- **Method:** `GET`
- **URL:** `https://erp.lloydcollege.in/api/student/me/monthly-attendance`
- **Headers:**
  ```http
  Authorization: Bearer <ACCESS_TOKEN>
  Accept: application/json
  ```

#### Response Specification
- **Success Status Code:** `200 OK`
- **Failure Status Code:** `401 Unauthorized`
- **Success Response Schema:**
  ```json
  {
    "status": true,
    "data": {
      "student_id": 28960,
      "student_name": "string",
      "months": [
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

### EP-04: Weekly Timetable & Period Status (`GET /student/me/weekly-attendance`)

#### Purpose
Delivers the complete weekly academic schedule, daily timetable routines, period time boundaries, classroom numbers, and attendance status per period.

#### Request Specification
- **Method:** `GET`
- **URL:** `https://erp.lloydcollege.in/api/student/me/weekly-attendance`
- **Headers:**
  ```http
  Authorization: Bearer <ACCESS_TOKEN>
  Accept: application/json
  ```

#### Response Specification
- **Success Status Code:** `200 OK`
- **Failure Status Code:** `401 Unauthorized`
- **Success Response Schema:**
  ```json
  {
    "status": true,
    "data": {
      "student_id": 28960,
      "student_name": "string",
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
            }
          ]
        }
      ]
    }
  }
  ```

---

### EP-05: Student Class Attendance Ledger (`GET /attendance/student`)

#### Purpose
Returns paginated, chronological class-by-class attendance records, including the exact timestamp of faculty marking and lecture sequence numbers.

#### Request Specification
- **Method:** `GET`
- **URL:** `https://erp.lloydcollege.in/api/attendance/student?student_id={id}&page={page}&page_size=100&sort_by=attendance_date&sort_dir=desc`
- **Headers:**
  ```http
  Authorization: Bearer <ACCESS_TOKEN>
  Accept: application/json
  ```
- **Query Parameters:**
  - `student_id` (Integer, Required): The numeric student ID.
  - `page` (Integer, Optional, Default 1): Page index.
  - `page_size` (Integer, Optional, Client uses 100): Records per page.
  - `sort_by` (String, Optional): Column to sort (`attendance_date`).
  - `sort_dir` (String, Optional): Sort direction (`desc` or `asc`).

#### Response Specification
- **Success Status Code:** `200 OK`
- **Failure Status Code:** `401 Unauthorized`
- **Success Response Schema:**
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
        "student_name": "string",
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
