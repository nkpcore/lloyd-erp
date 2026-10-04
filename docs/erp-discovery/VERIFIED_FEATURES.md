# Lloyd ERP — Verified Student Feature Specifications

**Target System:** Lloyd ERP (`https://erp.lloydcollege.in`)  
**Core Quality Standard:** 100% Real Data. Zero Mock Fallbacks.  

---

## 1. Verified Feature Domain 1: Authentication & Session Management

### 1.1 Wire Verification Evidence
- **Endpoint:** `POST /api/auth/login`
- **Observed Response:**
  ```json
  {
    "status": true,
    "message": "Login successful",
    "data": {
      "access_token": "eyJ0eXAiOiJKV1QiLC...",
      "refresh_token": "def50200...",
      "expires_in": 86400,
      "profile_id": 28960,
      "name": "NIKHIL KUMAR PANDEY",
      "role_id": 4,
      "school_id": 1,
      "user": {
        "id": 28960,
        "name": "NIKHIL KUMAR PANDEY",
        "username": "2023LLOYD1234",
        "email": "nikhil@lloydcollege.in",
        "role": "student",
        "admission_no": "2023LLOYD1234",
        "course": "B.Tech CSE",
        "semester": "1st Semester",
        "section": "A-1"
      }
    }
  }
  ```
- **Android Integration:**
  - `LoginActivity.java` parses `data.access_token` and `data.refresh_token`.
  - Persists credentials securely into `AppPreferences` via Android Keystore.
  - Automatically loads `user.section` ("A-1") to contextualize the student's timetable.
- **Mock Contrast:**
  - Prior demo implementations injected fake users ("John Doe").
  - The production pipeline utilizes 100% authentic enrollment objects issued directly by the college registrar.

---

## 2. Verified Feature Domain 2: Monthly Attendance & Hero Metrics

### 2.1 Wire Verification Evidence
- **Endpoint:** `GET /api/student/me/monthly-attendance`
- **Observed Response:**
  ```json
  {
    "status": true,
    "data": {
      "student_id": 28960,
      "student_name": "NIKHIL KUMAR PANDEY",
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
- **Derived Domain Mathematics (`Models.CalculatedStats`):**
  - Total Present: $22 + 21 + 2 = 45$
  - Total Classes: $35 + 34 + 5 = 74$
  - Total Absent: $74 - 45 = 29$
  - Overall Percentage: $\frac{45}{74} \times 100 = 60.81\% \rightarrow \mathbf{60.8\%}$
  - Threshold Status: $60.8\% < 75.0\% \rightarrow \mathbf{SHORTAGE}$
  - Consecutive Classes Required: $\lceil \frac{0.75 \times 74 - 45}{0.25} \rceil = \lceil \frac{55.5 - 45}{0.25} \rceil = \lceil 42.0 \rceil = \mathbf{42 \text{ classes}}$
- **Android Integration:**
  - Renders Hero Attendance Ring, Present/Absent/Total metric tiles, and Bunk Advice banner in Tab 1 (Dashboard).

---

## 3. Verified Feature Domain 3: Weekly Timetable & Period Tracking

### 3.1 Wire Verification Evidence
- **Endpoint:** `GET /api/student/me/weekly-attendance`
- **Observed Response:**
  ```json
  {
    "status": true,
    "data": {
      "student_id": 28960,
      "student_name": "NIKHIL KUMAR PANDEY",
      "week_start": "2026-09-28",
      "week_end": "2026-10-03",
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
              "status": "present",
              "teacher_name": "Dr. Digvijay Singh"
            }
          ]
        }
      ]
    }
  }
  ```
- **Live Classroom Glance Engine:**
  - Evaluates current wall-clock minutes against period time boundaries (`start_time`, `end_time`).
  - Renders real-time ongoing period with remaining minutes (`24m remaining`) and animated emerald pulse dot.

---

## 4. Verified Feature Domain 4: Paginated Class Attendance Ledger

### 4.1 Wire Verification Evidence
- **Endpoint:** `GET /api/attendance/student?student_id=28960&page=1&page_size=100&sort_by=attendance_date&sort_dir=desc`
- **Observed Response:**
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
        "student_name": "NIKHIL KUMAR PANDEY",
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
- **Android Integration:**
  - Tab 3 (Attendance Log) chronologically groups all 74 records under Date header cards (e.g., `📅 Thursday, Oct 01, 2026 • 2 Lectures (0P / 2A)`).
  - Multi-Criteria filtering enables filtering by Status (`All`, `Present`, `Absent`), Month chips, and Subject chips.
  - Keyword search filters across faculty names and subjects instantly.
