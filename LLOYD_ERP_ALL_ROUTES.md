# Lloyd College ERP -- Comprehensive Master Route Catalog

> **Target Domain:** `erp.lloydcollege.in`  
> **API Base URL:** `https://erp.lloydcollege.in/api`  
> **Frontend Web Client (SPA):** `https://erp.lloydcollege.in/erp/` (Angular 18+)  
> **Audit Timestamp:** 2026-10-09  
> **Catalog Coverage:** 100% Complete -- Includes all live Backend REST API endpoints (OpenAPI 3.0), Frontend Angular SPA client routes, and System / Infrastructure paths.

---

## 1. Executive Summary & Route Statistics

| Metric | Count |
| :--- | :--- |
| **Total Unique Backend API Paths** | **987** |
| **Total Backend HTTP Operations (Endpoints)** | **1356** |
| **Backend API Functional Categories / Modules** | **224** |
| **Frontend SPA Top-Level Modules** | **37** |
| **Frontend SPA Sub-Routes & Pages** | **220** |

### HTTP Methods Breakdown

| HTTP Method | Endpoints Count |
| :--- | :--- |
| `GET` | 634 |
| `POST` | 356 |
| `PUT` | 148 |
| `DELETE` | 140 |
| `PATCH` | 78 |

---

## 2. Frontend Web Application Routes (Angular SPA)

These client-side routes are hosted at `https://erp.lloydcollege.in/erp/` and mapped by the Angular routing framework:

### Module: `/academic`

- `/erp/academic` (Base Root)
- `/erp/academic/assignments`
- `/erp/academic/classes`
- `/erp/academic/co-po-mapping`
- `/erp/academic/course-file`
- `/erp/academic/course-outcomes`
- `/erp/academic/groups`
- `/erp/academic/internet`
- `/erp/academic/mini-project`
- `/erp/academic/optional-subjects`
- `/erp/academic/peos`
- `/erp/academic/program-outcomes`
- `/erp/academic/promotions`
- `/erp/academic/routine`
- `/erp/academic/sections`
- `/erp/academic/semesters`
- `/erp/academic/study-materials`
- `/erp/academic/subject-allocated`
- `/erp/academic/subject-topics`
- `/erp/academic/subject-units`
- `/erp/academic/subjects`
- `/erp/academic/submissions`
- `/erp/academic/syllabus`
- `/erp/academic/unit-outcomes`
- `/erp/academic/vision-mission`

### Module: `/account`

- `/erp/account` (Base Root)
- `/erp/account/activity`
- `/erp/account/discounts`
- `/erp/account/due-receipt`
- `/erp/account/fee-type`
- `/erp/account/fees`
- `/erp/account/gateway-logs`
- `/erp/account/invoices`
- `/erp/account/new-admission`
- `/erp/account/offline-fee`
- `/erp/account/pay`
- `/erp/account/re-registration`
- `/erp/account/report`
- `/erp/account/upload-fee`

### Module: `/accreditation`

- `/erp/accreditation` (Base Root)
- `/erp/accreditation/analyzer`
- `/erp/accreditation/aqar`
- `/erp/accreditation/aqar/:id`
- `/erp/accreditation/calendar`
- `/erp/accreditation/criteria`
- `/erp/accreditation/cycles`
- `/erp/accreditation/dashboard`
- `/erp/accreditation/data-explorer`
- `/erp/accreditation/data-hub`
- `/erp/accreditation/evidence`
- `/erp/accreditation/gaps`
- `/erp/accreditation/guidance`
- `/erp/accreditation/iqac`
- `/erp/accreditation/iqac-dashboard`
- `/erp/accreditation/knowledge-base`
- `/erp/accreditation/metric-definitions`
- `/erp/accreditation/nba`
- `/erp/accreditation/nba/applications`
- `/erp/accreditation/nba/applications/:id`
- `/erp/accreditation/nirf`
- `/erp/accreditation/nirf-cycle-dashboard`
- `/erp/accreditation/nirf-submissions`
- `/erp/accreditation/nirf-submissions/:id`
- `/erp/accreditation/programmes`
- `/erp/accreditation/recognitions`
- `/erp/accreditation/review`

### Module: `/administrator`

- `/erp/administrator` (Base Root)
- `/erp/administrator/activity-logs`
- `/erp/administrator/college-transfer-groups`
- `/erp/administrator/colleges`
- `/erp/administrator/colleges/:id`
- `/erp/administrator/continuation-form`
- `/erp/administrator/credentials`
- `/erp/administrator/permissions`
- `/erp/administrator/re-registration`
- `/erp/administrator/re-registration/approvals`
- `/erp/administrator/roles`
- `/erp/administrator/session-management`
- `/erp/administrator/settings/app-version`
- `/erp/administrator/settings/dashboard-config`
- `/erp/administrator/settings/email`
- `/erp/administrator/settings/general`
- `/erp/administrator/settings/payment`
- `/erp/administrator/settings/payment/:collegeId`
- `/erp/administrator/settings/payment/new`
- `/erp/administrator/settings/sms`
- `/erp/administrator/superadmins`
- `/erp/administrator/user-permissions`
- `/erp/administrator/users`

### Module: `/ai-teacher`

- `/erp/ai-teacher` (Base Root)
- `/erp/ai-teacher/chat/:id`
- `/erp/ai-teacher/my-sessions`

### Module: `/announcements`

- `/erp/announcements` (Base Root)
- `/erp/announcements/holiday`
- `/erp/announcements/news`
- `/erp/announcements/notice`

### Module: `/attendance`

- `/erp/attendance` (Base Root)
- `/erp/attendance/absent-notification`
- `/erp/attendance/director-mail-settings`
- `/erp/attendance/employee`
- `/erp/attendance/guest-lecture-setup`
- `/erp/attendance/mark`
- `/erp/attendance/mini-project/mark`
- `/erp/attendance/mini-project/report`
- `/erp/attendance/overview`
- `/erp/attendance/previous`
- `/erp/attendance/previous-report`
- `/erp/attendance/report`
- `/erp/attendance/report-logs`
- `/erp/attendance/teacher`
- `/erp/attendance/teacher-marking-class-wise`
- `/erp/attendance/teacher-tracking`

### Module: `/audit`

- `/erp/audit` (Standalone Page / Dashboard)

### Module: `/auth`

- `/erp/auth` (Base Root)
- `/erp/auth/employee-registration`
- `/erp/auth/erp-registration`
- `/erp/auth/forgot-password`
- `/erp/auth/login`

### Module: `/cards`

- `/erp/cards` (Base Root)
- `/erp/cards/admit-card-settings`
- `/erp/cards/generate`
- `/erp/cards/id-card-settings`
- `/erp/cards/my-admit-card`

### Module: `/career`

- `/erp/career` (Standalone Page / Dashboard)

### Module: `/certifications`

- `/erp/certifications` (Base Root)
- `/erp/certifications/list`
- `/erp/certifications/settings`

### Module: `/compliance-acknowledge`

- `/erp/compliance-acknowledge` (Base Root)
- `/erp/compliance-acknowledge/:policyId`

### Module: `/dashboard`

- `/erp/dashboard` (Standalone Page / Dashboard)

### Module: `/databank`

- `/erp/databank` (Standalone Page / Dashboard)

### Module: `/ednectar`

- `/erp/ednectar` (Standalone Page / Dashboard)

### Module: `/events`

- `/erp/events` (Base Root)
- `/erp/events/abstract-submissions`
- `/erp/events/event`
- `/erp/events/impact-registrations`

### Module: `/feedback`

- `/erp/feedback` (Base Root)
- `/erp/feedback/list`

### Module: `/grievances`

- `/erp/grievances` (Base Root)
- `/erp/grievances/all`

### Module: `/guardian`

- `/erp/guardian` (Standalone Page / Dashboard)

### Module: `/hostel`

- `/erp/hostel` (Base Root)
- `/erp/hostel/approvals`
- `/erp/hostel/beds`
- `/erp/hostel/checkout-list`
- `/erp/hostel/fee-report`
- `/erp/hostel/hostler`
- `/erp/hostel/members`
- `/erp/hostel/registration`
- `/erp/hostel/report`
- `/erp/hostel/setup`

### Module: `/hrm`

- `/erp/hrm` (Base Root)
- `/erp/hrm/add-staff`
- `/erp/hrm/background-checks`
- `/erp/hrm/birthday-celebrations`
- `/erp/hrm/birthday-settings`
- `/erp/hrm/candidates`
- `/erp/hrm/compliance`
- `/erp/hrm/designations`
- `/erp/hrm/documents`
- `/erp/hrm/employee-lifecycle`
- `/erp/hrm/exits`
- `/erp/hrm/interviews`
- `/erp/hrm/new-joiners`
- `/erp/hrm/notice-period`
- `/erp/hrm/offboarding`
- `/erp/hrm/offer-letter-settings`
- `/erp/hrm/offers`
- `/erp/hrm/policies`
- `/erp/hrm/probation-status`
- `/erp/hrm/promotions`
- `/erp/hrm/recruitment`
- `/erp/hrm/requisitions`
- `/erp/hrm/staff`
- `/erp/hrm/transfers`
- `/erp/hrm/upcoming-anniversaries`
- `/erp/hrm/upcoming-confirmations`
- `/erp/hrm/work-anniversary-settings`

### Module: `/it-support`

- `/erp/it-support` (Base Root)
- `/erp/it-support/stats`
- `/erp/it-support/tickets`

### Module: `/leave`

- `/erp/leave` (Base Root)
- `/erp/leave/allocation`
- `/erp/leave/applications`
- `/erp/leave/applications-approved`
- `/erp/leave/applications-pending`
- `/erp/leave/applications-rejected`
- `/erp/leave/approval-setup`
- `/erp/leave/dashboard`
- `/erp/leave/dashboard/manage`
- `/erp/leave/holidays`
- `/erp/leave/my-duties`
- `/erp/leave/reporting-managers`
- `/erp/leave/tracker`
- `/erp/leave/types`

### Module: `/library`

- `/erp/library` (Base Root)
- `/erp/library/books`
- `/erp/library/ebooks`
- `/erp/library/issues`
- `/erp/library/members`

### Module: `/my-exit`

- `/erp/my-exit` (Standalone Page / Dashboard)

### Module: `/niyukti`

- `/erp/niyukti` (Standalone Page / Dashboard)

### Module: `/offline-exam`

- `/erp/offline-exam` (Base Root)
- `/erp/offline-exam/control`
- `/erp/offline-exam/date-sheet`
- `/erp/offline-exam/question-papers`

### Module: `/register`

- `/erp/register` (Base Root)
- `/erp/register/certificate`
- `/erp/register/employee`
- `/erp/register/kiosk`
- `/erp/register/qrcode`

### Module: `/scholarship`

- `/erp/scholarship` (Base Root)
- `/erp/scholarship/applications`
- `/erp/scholarship/batch-fee`
- `/erp/scholarship/windows`

### Module: `/security`

- `/erp/security` (Base Root)
- `/erp/security/audit`
- `/erp/security/configuration`
- `/erp/security/dashboard`
- `/erp/security/emergency-occupancy`
- `/erp/security/gates`
- `/erp/security/live-feed`
- `/erp/security/reports`
- `/erp/security/retention`
- `/erp/security/vendors`
- `/erp/security/visit-policy`
- `/erp/security/visitor-approval`
- `/erp/security/visitor-registration`
- `/erp/security/watchlist`

### Module: `/settings`

- `/erp/settings` (Base Root)
- `/erp/settings/profile`
- `/erp/settings/profile/edit`

### Module: `/students`

- `/erp/students` (Base Root)
- `/erp/students/bci`
- `/erp/students/internships`
- `/erp/students/manage`
- `/erp/students/new-users`
- `/erp/students/types`

### Module: `/system`

- `/erp/system` (Base Root)
- `/erp/system/date-time`
- `/erp/system/downtime`

### Module: `/teacher`

- `/erp/teacher` (Base Root)
- `/erp/teacher/departments`
- `/erp/teacher/manage`

### Module: `/templates`

- `/erp/templates` (Base Root)
- `/erp/templates/email`
- `/erp/templates/sms`

### Module: `/theme`

- `/erp/theme` (Standalone Page / Dashboard)

### System & Global Frontend Routes

- `/erp/` (Default Index / Login Redirect)
- `/erp/404` (Not Found Fallback)
- `/erp/unauthorized` (Access Denied / Forbidden)
- `/erp/**` (Wildcard Fallback)

---

## 3. Backend REST API Routes (`/api/...`)

Every single API endpoint listed below is verified against the live ERP server specification.

### Category: AI Teacher (20 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/ai-teacher/quizzes/{quiz_id}/submit` | Submit quiz answers | Bearer JWT |
| `POST` | `/api/ai-teacher/schedules` | Create a schedule slot for an AI teacher | Bearer JWT |
| `DELETE` | `/api/ai-teacher/schedules/{schedule_id}` | Remove a schedule slot | Bearer JWT |
| `POST` | `/api/ai-teacher/sessions` | Create a new AI session | Bearer JWT |
| `GET` | `/api/ai-teacher/sessions/{session_id}/assignments` | List students assigned to a session, with progress status | Bearer JWT |
| `GET` | `/api/ai-teacher/sessions/{session_id}/chat` | Get chat history for a session | Bearer JWT |
| `POST` | `/api/ai-teacher/sessions/{session_id}/chat` | Send a message to the AI teacher | Bearer JWT |
| `PUT` | `/api/ai-teacher/sessions/{session_id}/end` | End an AI session | Bearer JWT |
| `GET` | `/api/ai-teacher/sessions/{session_id}/quiz` | Get the active quiz for a session | Bearer JWT |
| `POST` | `/api/ai-teacher/sessions/{session_id}/quiz/generate` | Generate a quiz for the session (admin/teacher) | Bearer JWT |
| `PUT` | `/api/ai-teacher/sessions/{session_id}/start` | Start an AI session (AI sends opening message) | Bearer JWT |
| `POST` | `/api/ai-teacher/teachers` | Create an AI teacher | Bearer JWT |
| `POST` | `/api/ai-teacher/teachers/activate` | Activate an existing school teacher as an AI teacher (find-or-create) | Bearer JWT |
| `DELETE` | `/api/ai-teacher/teachers/{teacher_id}` | Archive an AI teacher | Bearer JWT |
| `PUT` | `/api/ai-teacher/teachers/{teacher_id}` | Update an AI teacher | Bearer JWT |
| `DELETE` | `/api/ai-teacher/teachers/{teacher_id}/deactivate` | Deactivate AI teacher (keep record, set status=0) | Bearer JWT |
| `GET` | `/api/ai-teacher/{school_id}/my-sessions` | Every AI session this student has been assigned to | Bearer JWT |
| `GET` | `/api/ai-teacher/{school_id}/schedules` | List AI teacher schedules for a college | Bearer JWT |
| `GET` | `/api/ai-teacher/{school_id}/sessions` | List AI sessions for a college (admin) | Bearer JWT |
| `GET` | `/api/ai-teacher/{school_id}/teachers` | List AI teachers for a college | Bearer JWT |

### Category: Account - Activity Log (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/account/activity` | Who did what in Accounts (filterable, paginated) | Bearer JWT |
| `GET` | `/api/account/activity/actions` | Action slugs + labels for the filter dropdown | Bearer JWT |
| `GET` | `/api/account/activity/actors` | Staff who actually have activity rows | Bearer JWT |
| `GET` | `/api/account/activity/student/{student_id}` | One student's Accounts action timeline | Bearer JWT |

### Category: Account - Due Receipts (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/account/fees/due-receipts` | List due receipts (paginated) | Bearer JWT |
| `POST` | `/api/account/fees/due-receipts` | Create a due fee receipt | Bearer JWT |
| `GET` | `/api/account/fees/due-receipts/{receipt_id}` | Get due receipt by ID | Bearer JWT |

### Category: Account - Expenditure Heads (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/account/expenditure-heads` | List expenditure heads | Bearer JWT |
| `POST` | `/api/account/expenditure-heads` | Create an expenditure head | Bearer JWT |
| `DELETE` | `/api/account/expenditure-heads/{expenditure_head_id}` | Delete expenditure head (admin only; cannot delete if in use) | Bearer JWT |
| `GET` | `/api/account/expenditure-heads/{expenditure_head_id}` | Get expenditure head by ID | Bearer JWT |
| `PATCH` | `/api/account/expenditure-heads/{expenditure_head_id}` | Update an expenditure head | Bearer JWT |

### Category: Account - Expenditures (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/account/expenditures` | List expenditure entries (paginated) | Bearer JWT |
| `POST` | `/api/account/expenditures` | Record an expenditure entry | Bearer JWT |
| `DELETE` | `/api/account/expenditures/{expenditure_id}` | Soft-delete an expenditure entry (admin only) | Bearer JWT |
| `GET` | `/api/account/expenditures/{expenditure_id}` | Get expenditure entry by ID | Bearer JWT |
| `PATCH` | `/api/account/expenditures/{expenditure_id}` | Update an expenditure entry | Bearer JWT |

### Category: Account - Fee Collection (7 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/account/fees/collect` | List fee-collection invoices (role-scoped, from invoices) | Bearer JWT |
| `POST` | `/api/account/fees/collect` | Create a fee collection record | Bearer JWT |
| `GET` | `/api/account/fees/collect/stats` | Aggregate totals for the invoice list (same filters as list) | Bearer JWT |
| `DELETE` | `/api/account/fees/collect/{collection_id}` | Delete a fee collection record (admin only) | Bearer JWT |
| `GET` | `/api/account/fees/collect/{collection_id}` | Get fee collection by ID | Bearer JWT |
| `PATCH` | `/api/account/fees/collect/{collection_id}` | Update a fee collection record | Bearer JWT |
| `PATCH` | `/api/account/fees/invoices/{invoice_id}/total-amount` | Update invoice total (gross) amount - Super Admin & Accountant only | Bearer JWT |

### Category: Account - Fee Statement Export (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/account/fees/collect/export/excel` | Fee statement as one .xlsx - `split` becomes worksheets | Bearer JWT |
| `GET` | `/api/account/fees/collect/export/pdf` | Fee statement as a PDF (or a ZIP when split) | Bearer JWT |

### Category: Account - Fee Types (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/account/fee-types` | List fee types | Bearer JWT |
| `POST` | `/api/account/fee-types` | Create a fee type | Bearer JWT |
| `DELETE` | `/api/account/fee-types/{fee_type_id}` | Delete a fee type (admin only; cannot delete if in use) | Bearer JWT |
| `GET` | `/api/account/fee-types/{fee_type_id}` | Get fee type by ID | Bearer JWT |
| `PATCH` | `/api/account/fee-types/{fee_type_id}` | Update a fee type | Bearer JWT |

### Category: Account - Fee Upload (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/account/fees/upload` | List fee uploads (paginated) | Bearer JWT |
| `POST` | `/api/account/fees/upload` | Upload fee file (csv/xlsx) | Bearer JWT |
| `POST` | `/api/account/fees/upload/students` | Upload student fees via CSV (creates/updates invoices) | Bearer JWT |
| `GET` | `/api/account/fees/upload/students/template` | Download the student fee CSV template | Bearer JWT |
| `GET` | `/api/account/fees/upload/{upload_id}` | Get upload record and status | Bearer JWT |

### Category: Account - Hostel Fees (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/account/hostel-fees` | List hostel fees (paginated) | Bearer JWT |
| `POST` | `/api/account/hostel-fees` | Create a hostel fee record | Bearer JWT |
| `GET` | `/api/account/hostel-fees/{hostel_fee_id}` | Get hostel fee by ID | Bearer JWT |

### Category: Account - Income Heads (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/account/income-heads` | List income heads | Bearer JWT |
| `POST` | `/api/account/income-heads` | Create an income head | Bearer JWT |
| `DELETE` | `/api/account/income-heads/{income_head_id}` | Delete income head (admin only; cannot delete if in use) | Bearer JWT |
| `GET` | `/api/account/income-heads/{income_head_id}` | Get income head by ID | Bearer JWT |
| `PATCH` | `/api/account/income-heads/{income_head_id}` | Update an income head | Bearer JWT |

### Category: Account - Incomes (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/account/incomes` | List income entries (paginated) | Bearer JWT |
| `POST` | `/api/account/incomes` | Record an income entry | Bearer JWT |
| `DELETE` | `/api/account/incomes/{income_id}` | Soft-delete an income entry (admin only) | Bearer JWT |
| `GET` | `/api/account/incomes/{income_id}` | Get income entry by ID | Bearer JWT |
| `PATCH` | `/api/account/incomes/{income_id}` | Update an income entry | Bearer JWT |

### Category: Account - New Admission (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/account/new-admission/collect` | Collect a new-admission fee (offline) | Bearer JWT |
| `GET` | `/api/account/new-admission/students` | List CRM offer-letter applicants for a school | Bearer JWT |
| `GET` | `/api/account/new-admission/students/{application_no}` | Get one CRM applicant (payNow header) | Bearer JWT |

### Category: Account - Offline Fee (6 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/account/fees/offline` | Record an offline fee payment | Bearer JWT |
| `POST` | `/api/account/fees/offline/collect` | Collect an offline fee (raises invoice + payment_init/log) | Bearer JWT |
| `GET` | `/api/account/fees/offline/{offline_id}` | Get offline fee record by ID | Bearer JWT |
| `PATCH` | `/api/account/fees/offline/{offline_id}/approve` | Approve an offline fee payment | Bearer JWT |
| `GET` | `/api/account/offline/students` | List students for offline fee payment | Bearer JWT |
| `GET` | `/api/account/offline/students/{student_id}` | Get one student for offline payment (payNow header) | Bearer JWT |

### Category: Account - Payment Updates (12 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/account/payments` | List payment updates (paginated) | Bearer JWT |
| `POST` | `/api/account/payments` | Record a payment status update | Bearer JWT |
| `GET` | `/api/account/payments/gateway/logs` | List tbl_payment_log rows (raw gateway callbacks) - account desk / Super Admin | Bearer JWT |
| `GET` | `/api/account/payments/init/logs` | List payment_init rows (fee demands sent to the gateway) - account desk / Super Admin | Bearer JWT |
| `POST` | `/api/account/payments/law/initiate` | Create a payment_init row before redirecting the student to the bank | Bearer JWT |
| `POST` | `/api/account/payments/law/success` | Receive law department payment gateway success callback | Bearer JWT |
| `GET` | `/api/account/payments/log/search` | Get payment log by reference number | Bearer JWT |
| `GET` | `/api/account/payments/self/fee-receipts` | Gateway fee receipt log for the logged-in student (tbl_payment_log rows) | Bearer JWT |
| `POST` | `/api/account/payments/self/initiate` | Create a payment_init row for a logged-in student paying their own fee | Bearer JWT |
| `GET` | `/api/account/payments/self/methods` | Enabled payment gateways for the student's / ward's college (Pay screen) | Bearer JWT |
| `GET` | `/api/account/payments/self/transactions` | Payment history (settled receipts) for the logged-in student | Bearer JWT |
| `POST` | `/api/account/payments/success` | Receive payment gateway callback (all schools: Law, LIET, LBS, Pharmacy, Biotech) | Bearer JWT |

### Category: Account - Receipts (11 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/account/receipts/due` | List gateway-paid receipts pending/after slip verification | Bearer JWT |
| `GET` | `/api/account/receipts/due/stats` | Aggregate counts and collection total for the due-receipt page header cards | Bearer JWT |
| `GET` | `/api/account/receipts/due/{invoice_id}` | Get single due receipt (invoice with line items) | Bearer JWT |
| `DELETE` | `/api/account/receipts/due/{log_id}` | Delete a due receipt (verified ones are fully reversed) - accountant / super admin only | Bearer JWT |
| `POST` | `/api/account/receipts/due/{log_id}/generate` | Verify a pending due receipt and generate its receipt | Bearer JWT |
| `GET` | `/api/account/receipts/paid` | List paid invoice receipts | Bearer JWT |
| `GET` | `/api/account/receipts/paid-log/{log_id}` | Get printable receipt from a verified gateway payment log | Bearer JWT |
| `GET` | `/api/account/receipts/paid-log/{log_id}/print` | Rendered HTML fee receipt for a verified payment log (print / download) | Bearer JWT |
| `GET` | `/api/account/receipts/paid/{invoice_id}` | Get single paid receipt (invoice with line items) | Bearer JWT |
| `GET` | `/api/account/receipts/report` | Verified / non-verified gateway payment report | Bearer JWT |
| `GET` | `/api/account/receipts/student-details` | Resolve student/payer details from a transaction number | Bearer JWT |

### Category: Account - Reports (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/account/reports` | Get account report | Bearer JWT |
| `GET` | `/api/account/reports/export` | Export account report as CSV | Bearer JWT |

### Category: Account - School Fee Types (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/account/fee-type` | List school fee types | Bearer JWT |
| `POST` | `/api/account/fee-type` | Add a school fee type | Bearer JWT |
| `DELETE` | `/api/account/fee-type/{fee_type_id}` | Delete a school fee type (soft delete, status - 0) | Bearer JWT |
| `GET` | `/api/account/fee-type/{fee_type_id}` | Get school fee type by ID | Bearer JWT |
| `PATCH` | `/api/account/fee-type/{fee_type_id}` | Edit a school fee type | Bearer JWT |

### Category: Account - Student Fee Discounts (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/account/discounts` | List student fee discounts (paginated) | Bearer JWT |
| `POST` | `/api/account/discounts` | Create a student fee discount | Bearer JWT |
| `DELETE` | `/api/account/discounts/{discount_id}` | Revoke a student fee discount | Bearer JWT |
| `PATCH` | `/api/account/discounts/{discount_id}` | Update a student fee discount | Bearer JWT |
| `POST` | `/api/account/discounts/{discount_id}/apply` | Apply a discount to a specific student invoice | Bearer JWT |

### Category: Account - Summary (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/account/summary/fees` | Successful fee transactions summary (all schools) | Bearer JWT |
| `GET` | `/api/account/summary/school/{college_id}` | Get consolidated school financial summary | Bearer JWT |
| `GET` | `/api/account/summary/student/{student_id}` | Get student fee summary | Bearer JWT |
| `GET` | `/api/account/summary/students` | Class-wise student account summary list | Bearer JWT |

### Category: Accounts - Re-Registration (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/account/re-registration/students` | Students in a scope with re-registration state + fees | Bearer JWT |
| `POST` | `/api/account/re-registration/{student_id}/activate` | Open a student's re-registration form | Bearer JWT |
| `POST` | `/api/account/re-registration/{student_id}/deactivate` | Close a student's re-registration form | Bearer JWT |

### Category: Accreditation - AI Copilot (6 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/accreditation/ai/ask` | Ask Accreditation | Bearer JWT |
| `POST` | `/api/accreditation/ai/evidence-finder` | Evidence Finder | Bearer JWT |
| `POST` | `/api/accreditation/ai/explain-metric` | Explain Metric | Bearer JWT |
| `POST` | `/api/accreditation/ai/explain-requirement` | Explain Requirement | Bearer JWT |
| `POST` | `/api/accreditation/ai/gap-analysis` | Gap Analysis | Bearer JWT |
| `POST` | `/api/accreditation/ai/narrative-draft` | Narrative Draft | Bearer JWT |

### Category: Accreditation - AQAR (11 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/aqar` | List AQAR Reports | Bearer JWT |
| `POST` | `/api/accreditation/aqar` | Start an AQAR Report for an academic year | Bearer JWT |
| `DELETE` | `/api/accreditation/aqar/{report_id}` | Remove AQAR Report | Bearer JWT |
| `GET` | `/api/accreditation/aqar/{report_id}` | Get AQAR Report detail | Bearer JWT |
| `PUT` | `/api/accreditation/aqar/{report_id}` | Update AQAR Report (cycle_id only - see /status for workflow) | Bearer JWT |
| `GET` | `/api/accreditation/aqar/{report_id}/sections` | List an AQAR Report's sections | Bearer JWT |
| `POST` | `/api/accreditation/aqar/{report_id}/sections` | Add a section to an AQAR Report | Bearer JWT |
| `PUT` | `/api/accreditation/aqar/{report_id}/sections/{section_id}` | Update an AQAR Report section | Bearer JWT |
| `GET` | `/api/accreditation/aqar/{report_id}/sections/{section_id}/evidence` | List Evidence linked to an AQAR Report section | Bearer JWT |
| `PUT` | `/api/accreditation/aqar/{report_id}/sections/{section_id}/review` | Review an AQAR Report section (verify/block) | Bearer JWT |
| `PUT` | `/api/accreditation/aqar/{report_id}/status` | Transition AQAR Report status (draft/review/approved/submitted/locked) | Bearer JWT |

### Category: Accreditation - Action Plans (7 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/action-plans` | List Action Plans | Bearer JWT |
| `POST` | `/api/accreditation/action-plans` | Add Action Plan | Bearer JWT |
| `DELETE` | `/api/accreditation/action-plans/{item_id}` | Remove Action Plan | Bearer JWT |
| `PUT` | `/api/accreditation/action-plans/{item_id}` | Update Action Plan | Bearer JWT |
| `PUT` | `/api/accreditation/action-plans/{item_id}/complete` | Complete an Action Plan (outcome + impact) | Bearer JWT |
| `GET` | `/api/accreditation/action-plans/{item_id}/evidence` | List Evidence linked to an Action Plan | Bearer JWT |
| `PUT` | `/api/accreditation/action-plans/{item_id}/verify` | Verify a completed Action Plan's outcome/impact | Bearer JWT |

### Category: Accreditation - Calendar (1 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/calendar` | Get the accreditation deadline/expiry calendar | Bearer JWT |

### Category: Accreditation - Criteria (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/criteria` | List Accreditation Criteria | Bearer JWT |
| `POST` | `/api/accreditation/criteria` | Add a NIRF criterion node (Category/Parameter) | Bearer JWT |
| `PUT` | `/api/accreditation/criteria/{criterion_id}` | Update a NIRF criterion node | Bearer JWT |

### Category: Accreditation - Criterion Progress (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/progress` | List criterion progress for a framework | Bearer JWT |
| `PUT` | `/api/accreditation/progress/{criterion_id}` | Upsert criterion progress | Bearer JWT |
| `PUT` | `/api/accreditation/progress/{criterion_id}/override` | Manually override a criterion's calculated progress | Bearer JWT |

### Category: Accreditation - Cycles (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/cycles` | List Accreditation Cycles | Bearer JWT |
| `POST` | `/api/accreditation/cycles` | Create Accreditation Cycle | Bearer JWT |
| `GET` | `/api/accreditation/cycles/{cycle_id}` | Get Accreditation Cycle detail | Bearer JWT |
| `PUT` | `/api/accreditation/cycles/{cycle_id}` | Update Accreditation Cycle | Bearer JWT |
| `PUT` | `/api/accreditation/cycles/{cycle_id}/status` | Transition Accreditation Cycle status | Bearer JWT |

### Category: Accreditation - Dashboard (1 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/dashboard` | Accreditation Dashboard summary | Bearer JWT |

### Category: Accreditation - Data Hub (11 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/data-hub/definitions` | List Metric Definitions | Bearer JWT |
| `POST` | `/api/accreditation/data-hub/definitions` | Add a Metric Definition | Bearer JWT |
| `PUT` | `/api/accreditation/data-hub/definitions/{definition_id}` | Update a Metric Definition | Bearer JWT |
| `GET` | `/api/accreditation/data-hub/metrics` | List Metric Snapshots | Bearer JWT |
| `POST` | `/api/accreditation/data-hub/metrics/recalculate` | Recalculate (or first-calculate) a Metric | Bearer JWT |
| `GET` | `/api/accreditation/data-hub/metrics/{snapshot_id}` | Get one Metric Snapshot (with lineage) | Bearer JWT |
| `GET` | `/api/accreditation/data-hub/metrics/{snapshot_id}/history` | Get a Metric's value history (live + frozen) | Bearer JWT |
| `POST` | `/api/accreditation/data-hub/metrics/{snapshot_id}/snapshot` | Freeze the current value into an immutable snapshot | Bearer JWT |
| `PUT` | `/api/accreditation/data-hub/metrics/{snapshot_id}/validate` | Re-validate a Metric Snapshot | Bearer JWT |
| `PUT` | `/api/accreditation/data-hub/metrics/{snapshot_id}/verify` | Verify or reject a Metric Snapshot | Bearer JWT |
| `GET` | `/api/accreditation/data-hub/summary` | Get Data Hub summary counts | Bearer JWT |

### Category: Accreditation - Evidence Repository (12 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/evidence` | List Evidence | Bearer JWT |
| `POST` | `/api/accreditation/evidence` | Add Evidence | Bearer JWT |
| `DELETE` | `/api/accreditation/evidence/{evidence_id}` | Remove Evidence | Bearer JWT |
| `GET` | `/api/accreditation/evidence/{evidence_id}` | Get Evidence detail | Bearer JWT |
| `PUT` | `/api/accreditation/evidence/{evidence_id}` | Update Evidence | Bearer JWT |
| `GET` | `/api/accreditation/evidence/{evidence_id}/links` | List what this Evidence is linked to | Bearer JWT |
| `POST` | `/api/accreditation/evidence/{evidence_id}/links` | Link Evidence to a criterion/metric/... | Bearer JWT |
| `DELETE` | `/api/accreditation/evidence/{evidence_id}/links/{link_id}` | Remove an Evidence link | Bearer JWT |
| `GET` | `/api/accreditation/evidence/{evidence_id}/versions` | List Evidence version history | Bearer JWT |
| `POST` | `/api/accreditation/evidence/{evidence_id}/versions` | Upload a new Evidence version | Bearer JWT |
| `PUT` | `/api/accreditation/evidence/{evidence_id}/versions/{version_id}/restore` | Mark an older Evidence version current | Bearer JWT |
| `PUT` | `/api/accreditation/evidence/{evidence_id}/versions/{version_id}/verify` | Verify or reject an Evidence version | Bearer JWT |

### Category: Accreditation - Frameworks (1 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/frameworks` | List Accreditation Frameworks (NBA/NAAC/NIRF) | Bearer JWT |

### Category: Accreditation - Gap Analysis (6 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/gaps` | List Gaps | Bearer JWT |
| `POST` | `/api/accreditation/gaps` | Add Gap | Bearer JWT |
| `DELETE` | `/api/accreditation/gaps/{gap_id}` | Remove Gap | Bearer JWT |
| `GET` | `/api/accreditation/gaps/{gap_id}` | Get Gap detail | Bearer JWT |
| `PUT` | `/api/accreditation/gaps/{gap_id}` | Update Gap | Bearer JWT |
| `PUT` | `/api/accreditation/gaps/{gap_id}/status` | Transition Gap status | Bearer JWT |

### Category: Accreditation - Guidance (11 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/guidance` | List Guidance | Bearer JWT |
| `POST` | `/api/accreditation/guidance` | Add Guidance | Bearer JWT |
| `GET` | `/api/accreditation/guidance/by-criterion/{criterion_id}` | Get Guidance for a Criterion/Requirement | Bearer JWT |
| `GET` | `/api/accreditation/guidance/{guidance_id}` | Get Guidance detail | Bearer JWT |
| `PUT` | `/api/accreditation/guidance/{guidance_id}` | Update Guidance | Bearer JWT |
| `POST` | `/api/accreditation/guidance/{guidance_id}/examples` | Add a Guidance Example | Bearer JWT |
| `DELETE` | `/api/accreditation/guidance/{guidance_id}/examples/{example_id}` | Remove a Guidance Example | Bearer JWT |
| `PUT` | `/api/accreditation/guidance/{guidance_id}/examples/{example_id}` | Update a Guidance Example | Bearer JWT |
| `POST` | `/api/accreditation/guidance/{guidance_id}/references` | Add an Official Reference | Bearer JWT |
| `DELETE` | `/api/accreditation/guidance/{guidance_id}/references/{reference_id}` | Remove an Official Reference | Bearer JWT |
| `PUT` | `/api/accreditation/guidance/{guidance_id}/references/{reference_id}` | Update an Official Reference | Bearer JWT |

### Category: Accreditation - IQAC (10 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `DELETE` | `/api/accreditation/action-items/{item_id}` | Remove Action Item | Bearer JWT |
| `PUT` | `/api/accreditation/action-items/{item_id}` | Update Action Item | Bearer JWT |
| `GET` | `/api/accreditation/iqac-meetings` | List IQAC Meetings | Bearer JWT |
| `POST` | `/api/accreditation/iqac-meetings` | Log an IQAC Meeting | Bearer JWT |
| `DELETE` | `/api/accreditation/iqac-meetings/{meeting_id}` | Remove IQAC Meeting | Bearer JWT |
| `GET` | `/api/accreditation/iqac-meetings/{meeting_id}` | Get IQAC Meeting detail | Bearer JWT |
| `PUT` | `/api/accreditation/iqac-meetings/{meeting_id}` | Update IQAC Meeting | Bearer JWT |
| `GET` | `/api/accreditation/iqac-meetings/{meeting_id}/action-items` | List Action Items for a Meeting | Bearer JWT |
| `POST` | `/api/accreditation/iqac-meetings/{meeting_id}/action-items` | Add an Action Item | Bearer JWT |
| `GET` | `/api/accreditation/iqac/dashboard` | IQAC quality-improvement dashboard | Bearer JWT |

### Category: Accreditation - Knowledge Base (8 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/knowledge-base` | Search/List Knowledge Base items | Bearer JWT |
| `POST` | `/api/accreditation/knowledge-base` | Add a Knowledge Base item | Bearer JWT |
| `GET` | `/api/accreditation/knowledge-base/by-target/{linkable_type}/{linkable_id}` | Related Knowledge for a Criterion/Metric/Section/etc. | Bearer JWT |
| `GET` | `/api/accreditation/knowledge-base/{article_id}` | Get Knowledge Base item detail | Bearer JWT |
| `PUT` | `/api/accreditation/knowledge-base/{article_id}` | Update a Knowledge Base item | Bearer JWT |
| `POST` | `/api/accreditation/knowledge-base/{article_id}/links` | Relate a Knowledge Base item to a Criterion/Guidance/Metric/etc. | Bearer JWT |
| `DELETE` | `/api/accreditation/knowledge-base/{article_id}/links/{link_id}` | Remove a Knowledge Base relationship | Bearer JWT |
| `PUT` | `/api/accreditation/knowledge-base/{article_id}/status` | Transition Knowledge Base item status (draft/review/published/archived) | Bearer JWT |

### Category: Accreditation - Metrics (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/criteria/{criterion_progress_id}/metrics` | List Metrics for a criterion | Bearer JWT |
| `POST` | `/api/accreditation/criteria/{criterion_progress_id}/metrics` | Add Metric | Bearer JWT |
| `DELETE` | `/api/accreditation/metrics/{metric_id}` | Remove Metric | Bearer JWT |
| `PUT` | `/api/accreditation/metrics/{metric_id}` | Update Metric | Bearer JWT |
| `PUT` | `/api/accreditation/metrics/{metric_id}/verify` | Verify or reject a Metric | Bearer JWT |

### Category: Accreditation - NBA (27 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/nba/applications` | List NBA Applications | Bearer JWT |
| `POST` | `/api/accreditation/nba/applications` | Start an NBA Application | Bearer JWT |
| `DELETE` | `/api/accreditation/nba/applications/{application_id}` | Remove NBA Application | Bearer JWT |
| `GET` | `/api/accreditation/nba/applications/{application_id}` | Get NBA Application detail | Bearer JWT |
| `PUT` | `/api/accreditation/nba/applications/{application_id}` | Update NBA Application metadata | Bearer JWT |
| `GET` | `/api/accreditation/nba/applications/{application_id}/dashboard` | Programme-level NBA dashboard | Bearer JWT |
| `GET` | `/api/accreditation/nba/applications/{application_id}/sections` | List an Application's Pre-Qualifier/SAR sections | Bearer JWT |
| `POST` | `/api/accreditation/nba/applications/{application_id}/sections` | Add a Pre-Qualifier/SAR section | Bearer JWT |
| `PUT` | `/api/accreditation/nba/applications/{application_id}/sections/{section_id}` | Update a Pre-Qualifier/SAR section | Bearer JWT |
| `GET` | `/api/accreditation/nba/applications/{application_id}/sections/{section_id}/evidence` | List Evidence linked to a section | Bearer JWT |
| `PUT` | `/api/accreditation/nba/applications/{application_id}/sections/{section_id}/review` | Review a section (verify/block) | Bearer JWT |
| `GET` | `/api/accreditation/nba/applications/{application_id}/snapshots` | List an Application's submission snapshots | Bearer JWT |
| `PUT` | `/api/accreditation/nba/applications/{application_id}/status` | Transition NBA Application status | Bearer JWT |
| `GET` | `/api/accreditation/nba/applications/{application_id}/visits` | List an Application's Visits | Bearer JWT |
| `POST` | `/api/accreditation/nba/applications/{application_id}/visits` | Schedule a Visit | Bearer JWT |
| `GET` | `/api/accreditation/nba/programme-mappings` | List a Programme's mapped classes | Bearer JWT |
| `POST` | `/api/accreditation/nba/programme-mappings` | Map a Class to a Programme | Bearer JWT |
| `DELETE` | `/api/accreditation/nba/programme-mappings/{mapping_id}` | Remove a Programme-Course mapping | Bearer JWT |
| `GET` | `/api/accreditation/nba/programmes/{programme_id}/attainment` | Programme attainment (unavailable - see docs) | Bearer JWT |
| `GET` | `/api/accreditation/nba/programmes/{programme_id}/outcomes` | Programme CO/PO/PEO rollup | Bearer JWT |
| `PUT` | `/api/accreditation/nba/visits/{visit_id}` | Update a Visit | Bearer JWT |
| `GET` | `/api/accreditation/nba/visits/{visit_id}/observations` | List a Visit's observations | Bearer JWT |
| `POST` | `/api/accreditation/nba/visits/{visit_id}/observations` | Record an observation | Bearer JWT |
| `PUT` | `/api/accreditation/nba/visits/{visit_id}/observations/{observation_id}` | Update an observation | Bearer JWT |
| `POST` | `/api/accreditation/nba/visits/{visit_id}/observations/{observation_id}/convert-to-gap` | Escalate an observation into a tracked Gap | Bearer JWT |
| `POST` | `/api/accreditation/nba/visits/{visit_id}/panel-members` | Add a panel member | Bearer JWT |
| `DELETE` | `/api/accreditation/nba/visits/{visit_id}/panel-members/{member_id}` | Remove a panel member | Bearer JWT |

### Category: Accreditation - NIRF (Structured) (9 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/nirf/cycles/{cycle_id}/readiness` | Cycle-level NIRF readiness (aggregates existing submissions) | Bearer JWT |
| `GET` | `/api/accreditation/nirf/submissions` | List NIRF Submissions | Bearer JWT |
| `POST` | `/api/accreditation/nirf/submissions` | Create a NIRF Submission | Bearer JWT |
| `GET` | `/api/accreditation/nirf/submissions/{submission_id}` | Get NIRF Submission detail | Bearer JWT |
| `PUT` | `/api/accreditation/nirf/submissions/{submission_id}/status` | Transition a NIRF Submission's workflow status | Bearer JWT |
| `GET` | `/api/accreditation/nirf/submissions/{submission_id}/values` | List a NIRF Submission's parameter values | Bearer JWT |
| `POST` | `/api/accreditation/nirf/submissions/{submission_id}/values` | Add a NIRF Parameter Value | Bearer JWT |
| `PUT` | `/api/accreditation/nirf/values/{value_id}` | Update a NIRF Parameter Value | Bearer JWT |
| `GET` | `/api/accreditation/nirf/values/{value_id}/evidence` | List Evidence linked to a NIRF Parameter Value | Bearer JWT |

### Category: Accreditation - NIRF Data Entry (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/nirf` | List NIRF Data Entries | Bearer JWT |
| `POST` | `/api/accreditation/nirf` | Add NIRF Data Entry | Bearer JWT |
| `DELETE` | `/api/accreditation/nirf/{entry_id}` | Remove NIRF Data Entry | Bearer JWT |
| `GET` | `/api/accreditation/nirf/{entry_id}` | Get NIRF Data Entry detail | Bearer JWT |
| `PUT` | `/api/accreditation/nirf/{entry_id}` | Update NIRF Data Entry | Bearer JWT |

### Category: Accreditation - Programmes (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/programmes` | List Programmes | Bearer JWT |
| `POST` | `/api/accreditation/programmes` | Add Programme | Bearer JWT |
| `GET` | `/api/accreditation/programmes/{programme_id}` | Get Programme detail | Bearer JWT |
| `PUT` | `/api/accreditation/programmes/{programme_id}` | Update Programme | Bearer JWT |

### Category: Accreditation - Readiness Analyzer (9 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/analyzer/data-explorer` | ERP Data Explorer - what data exists for each metric | Bearer JWT |
| `GET` | `/api/accreditation/analyzer/data-explorer/{metric_code}/source` | Source lineage for one metric (aggregates only) | Bearer JWT |
| `POST` | `/api/accreditation/analyzer/findings/gap` | Create a gap from a finding (user-confirmed) | Bearer JWT |
| `GET` | `/api/accreditation/analyzer/programmes` | Programme accreditation dashboard | Bearer JWT |
| `GET` | `/api/accreditation/analyzer/programmes/{programme_id}/mapping-health` | Programme - Class mapping health | Bearer JWT |
| `POST` | `/api/accreditation/analyzer/run` | Run a readiness analysis | Bearer JWT |
| `GET` | `/api/accreditation/analyzer/runs` | List past analysis runs | Bearer JWT |
| `GET` | `/api/accreditation/analyzer/runs/latest` | Latest analysis run for a scope (+ staleness) | Bearer JWT |
| `GET` | `/api/accreditation/analyzer/runs/{run_id}` | Get an analysis run with its findings | Bearer JWT |

### Category: Accreditation - Recognition Registry (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/recognitions` | List Recognitions | Bearer JWT |
| `POST` | `/api/accreditation/recognitions` | Add Recognition | Bearer JWT |
| `DELETE` | `/api/accreditation/recognitions/{recognition_id}` | Remove Recognition | Bearer JWT |
| `GET` | `/api/accreditation/recognitions/{recognition_id}` | Get Recognition detail | Bearer JWT |
| `PUT` | `/api/accreditation/recognitions/{recognition_id}` | Update Recognition | Bearer JWT |

### Category: Accreditation - Review Queue (1 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/accreditation/review-queue` | Get the accreditation Review Queue | Bearer JWT |

### Category: Admin - App Version (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/app-versions` | List published mobile builds | Bearer JWT |
| `POST` | `/api/admin/app-versions` | Publish a mobile build | Bearer JWT |
| `DELETE` | `/api/admin/app-versions/{version_id}` | Delete a build record | Bearer JWT |
| `GET` | `/api/admin/app-versions/{version_id}` | Get one published build | Bearer JWT |
| `PUT` | `/api/admin/app-versions/{version_id}` | Update a published build (all fields optional) | Bearer JWT |

### Category: All Grievances (1 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/grievances/all` | Admin: list all grievances across the system | Bearer JWT |

### Category: Alumni Registration (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/alumni-registrations` | List alumni registrations | Bearer JWT |
| `GET` | `/api/alumni-registrations/{registration_id}` | Get a single alumni registration | Bearer JWT |

### Category: Announcements - Holidays (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/announcements/holidays` | List holidays (global, paginated) | Bearer JWT |
| `POST` | `/api/announcements/holidays` | Create a holiday (admin only) | Bearer JWT |
| `DELETE` | `/api/announcements/holidays/{holiday_id}` | Soft-delete a holiday (admin only; cannot delete past holidays) | Bearer JWT |
| `GET` | `/api/announcements/holidays/{holiday_id}` | Get holiday by ID | Bearer JWT |
| `PATCH` | `/api/announcements/holidays/{holiday_id}` | Update a holiday (admin only) | Bearer JWT |

### Category: Announcements - News (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/announcements/news` | List news articles (paginated) | Bearer JWT |
| `POST` | `/api/announcements/news` | Create a news article | Bearer JWT |
| `DELETE` | `/api/announcements/news/{news_id}` | Soft-delete a news article (teacher: own only) | Bearer JWT |
| `GET` | `/api/announcements/news/{news_id}` | Get news article by ID | Bearer JWT |
| `PATCH` | `/api/announcements/news/{news_id}` | Update a news article (teacher: own only) | Bearer JWT |

### Category: Announcements - Notices (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/announcements/notices` | List notices (paginated) | Bearer JWT |
| `POST` | `/api/announcements/notices` | Create a notice | Bearer JWT |
| `DELETE` | `/api/announcements/notices/{notice_id}` | Soft-delete a notice (teacher: own only) | Bearer JWT |
| `GET` | `/api/announcements/notices/{notice_id}` | Get notice by ID | Bearer JWT |
| `PATCH` | `/api/announcements/notices/{notice_id}` | Update a notice (teacher: own only) | Bearer JWT |

### Category: Anti Ragging (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/student/antiragging` | List students with an anti-ragging certificate on file (offset pagination + search + sorting) | Bearer JWT |
| `PATCH` | `/api/student/antiragging` | Upload / update a student's anti-ragging certificate | Bearer JWT |

### Category: App Version (1 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/app-version/check` | Is this build current? (open - no auth) | Bearer JWT |

### Category: Assignments (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/assignments` | List assignments | Bearer JWT |
| `POST` | `/api/academic/assignments` | Create assignment | Bearer JWT |
| `DELETE` | `/api/academic/assignments/{assignment_id}` | Delete assignment | Bearer JWT |
| `GET` | `/api/academic/assignments/{assignment_id}` | Get assignment detail | Bearer JWT |
| `PUT` | `/api/academic/assignments/{assignment_id}` | Update assignment | Bearer JWT |

### Category: Attendance (47 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/attendance/absent-recipients` | List actual people holding a Role Type, for the Absent Email/SMS Role picker | Bearer JWT |
| `GET` | `/api/attendance/analytics/daily-marker-count` | Teachers who marked student attendance - one row per (date, teacher), paginated | Bearer JWT |
| `GET` | `/api/attendance/analytics/teacher-tracking/chart` | Bar chart data: marked-teacher count per day for the last N days | Bearer JWT |
| `GET` | `/api/attendance/analytics/teacher-tracking/class-wise` | Class-wise teacher marking report - one row per (date, teacher, class, section, subject) | Bearer JWT |
| `GET` | `/api/attendance/analytics/teacher-tracking/class-wise/summary` | Summary stats for the class-wise teacher marking report (same filters, no pagination) | Bearer JWT |
| `GET` | `/api/attendance/analytics/teacher-tracking/class-wise/teachers` | Teachers who have marked attendance - lightweight dropdown source for the class-wise report | Bearer JWT |
| `GET` | `/api/attendance/analytics/teacher-tracking/detail` | Paginated teacher detail: name, department, status, marked-at, records count | Bearer JWT |
| `GET` | `/api/attendance/analytics/teacher-tracking/summary` | Stats cards: total teachers, marked, not-marked, marking rate vs yesterday | Bearer JWT |
| `GET` | `/api/attendance/analytics/teacher-tracking/weekly` | Weekly overview: Mon - Sun marking rate pills | Bearer JWT |
| `GET` | `/api/attendance/email-preview` | Preview the daily/semester attendance report email for a student, without sending it | Bearer JWT |
| `GET` | `/api/attendance/freeze` | Check whether attendance is frozen | Bearer JWT |
| `POST` | `/api/attendance/freeze` | Freeze / unfreeze attendance for a college/semester/month | Bearer JWT |
| `GET` | `/api/attendance/mini-project` | List / filter mini-project attendance records | Bearer JWT |
| `POST` | `/api/attendance/mini-project` | Mark mini-project attendance for multiple students | Bearer JWT |
| `DELETE` | `/api/attendance/mini-project/bulk` | Clear mini-project attendance marks for a lecture | Bearer JWT |
| `GET` | `/api/attendance/mini-project/report/date-wise` | Mini-project attendance report - date-wise breakdown per subject | Bearer JWT |
| `GET` | `/api/attendance/mini-project/report/subject-wise` | Mini-project attendance report - subject-wise breakdown per student | Bearer JWT |
| `GET` | `/api/attendance/mini-project/students` | List students assigned to the caller's mini-project group | Bearer JWT |
| `DELETE` | `/api/attendance/mini-project/{attendance_id}` | Soft-delete a single mini-project attendance record | Bearer JWT |
| `PUT` | `/api/attendance/mini-project/{attendance_id}` | Update a mini-project attendance record's status | Bearer JWT |
| `GET` | `/api/attendance/report` | Attendance reports (daily/monthly/semester/subject_wise/student_wise/percentage/matrix) | Bearer JWT |
| `GET` | `/api/attendance/report-logs` | List attendance-report cron run logs (daily/weekly/semester, scheduled or manually triggered) | Bearer JWT |
| `POST` | `/api/attendance/report-logs/{log_id}/retry` | Retry a failed/cancelled attendance report run, skipping recipients already emailed | Bearer JWT |
| `GET` | `/api/attendance/report/export/excel` | Export an attendance report as XLSX (same filters as GET /attendance/report) | Bearer JWT |
| `GET` | `/api/attendance/report/export/pdf` | Export an attendance report as PDF (same filters as GET /attendance/report) | Bearer JWT |
| `GET` | `/api/attendance/report/mark-attendance` | Mark attendance report - student-wise Present/Absent list, filterable by course, subject, teacher, and date | Bearer JWT |
| `GET` | `/api/attendance/report/semester_summary/{school_id}/{class_id}` | Per-student attendance percentage across all semesters | Bearer JWT |
| `POST` | `/api/attendance/send-absent-email` | Send an absent email to a user | Bearer JWT |
| `POST` | `/api/attendance/send-absent-sms` | Send an absent SMS to a user | Bearer JWT |
| `GET` | `/api/attendance/staff` | List / filter staff attendance | Bearer JWT |
| `POST` | `/api/attendance/staff` | Mark staff (teacher/employee) attendance | Bearer JWT |
| `GET` | `/api/attendance/staff/lookup/roles` | "Role" options for the staff-attendance lookup picker (excludes Admin/Super Admin) | Bearer JWT |
| `GET` | `/api/attendance/staff/lookup/users` | "User" options for the staff-attendance lookup picker, optionally narrowed by role | Bearer JWT |
| `GET` | `/api/attendance/staff/my-reports` | Employees reporting to the logged-in manager (attendance picker) | Bearer JWT |
| `GET` | `/api/attendance/staff/register` | Staff attendance register - one row per staff member, columns per day of month | Bearer JWT |
| `GET` | `/api/attendance/staff/register/export` | Download the staff attendance register as XLSX (every filtered row, not just one page) | Bearer JWT |
| `GET` | `/api/attendance/student` | List / filter student attendance | Bearer JWT |
| `POST` | `/api/attendance/student` | Mark attendance for multiple students | Bearer JWT |
| `DELETE` | `/api/attendance/student/bulk` | Clear marked attendance for a lecture | Bearer JWT |
| `DELETE` | `/api/attendance/student/bulk-delete` | Soft-delete multiple student attendance records | Bearer JWT |
| `GET` | `/api/attendance/student/ids` | All student attendance ids matching the filters (unpaginated, for select-all) | Bearer JWT |
| `GET` | `/api/attendance/student/pickable` | List students for marking attendance (semester resolved via enrollments fallback) | Bearer JWT |
| `GET` | `/api/attendance/student/picker` | Slim student list for filter dropdowns (Modify/Report attendance screens) | Bearer JWT |
| `DELETE` | `/api/attendance/student/{attendance_id}` | Soft-delete student attendance record | Bearer JWT |
| `PUT` | `/api/attendance/student/{attendance_id}` | Update student attendance status | Bearer JWT |
| `POST` | `/api/attendance/trigger-daily-report` | Manually trigger the daily attendance report email job (students + directors, no parents) | Bearer JWT |
| `POST` | `/api/attendance/trigger-semester-report` | Manually trigger the weekly semester-attendance report email job | Bearer JWT |

### Category: Attendance - Director Mail Settings (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/attendance/director-mail-settings` | List director-mail settings (optionally scoped to one college) | Bearer JWT |
| `POST` | `/api/attendance/director-mail-settings` | Create a director-mail setting for a college | Bearer JWT |
| `DELETE` | `/api/attendance/director-mail-settings/{setting_id}` | Delete a director-mail setting | Bearer JWT |
| `PUT` | `/api/attendance/director-mail-settings/{setting_id}` | Update a director-mail setting | Bearer JWT |

### Category: Audit Trail (7 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/audit/client-error` | Report a client-side failure the BE middleware never saw | Bearer JWT |
| `GET` | `/api/audit/crashes` | Failing endpoints ranked by occurrence count (Crash Report tab) | Bearer JWT |
| `GET` | `/api/audit/filters` | Colleges/roles/modules that actually have rows in range | Bearer JWT |
| `GET` | `/api/audit/live` | Poll for new audit rows since a cursor (Live Monitor tab) | Bearer JWT |
| `GET` | `/api/audit/logs` | List audit trail rows (filterable, paginated) | Bearer JWT |
| `DELETE` | `/api/audit/prune` | Delete audit rows older than N days - Super Admin only | Bearer JWT |
| `GET` | `/api/audit/summary` | Stat tiles + hourly trend for the current filter set | Bearer JWT |

### Category: Authentication (17 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/change-password` | Change own password (authenticated user) | Bearer JWT |
| `POST` | `/api/auth/delete-account` | Permanently delete own account (authenticated user) | Bearer JWT |
| `GET` | `/api/auth/employee-onboarding/form` | Prefill data for the employee onboarding form | Bearer JWT |
| `POST` | `/api/auth/employee-onboarding/register` | Submit bank details - completes onboarding, then signs the employee in | Bearer JWT |
| `POST` | `/api/auth/employee-onboarding/upload` | Upload an onboarding document (new employee) | Bearer JWT |
| `POST` | `/api/auth/employee-onboarding/upload-file` | Upload a per-entry document (education certificate / employment-history letter) - new employee, no fixed Employee column | Bearer JWT |
| `POST` | `/api/auth/forgot-password` | Request a 6-digit OTP sent to the registered email address | Bearer JWT |
| `POST` | `/api/auth/login` | Username + password login - returns access + refresh token | Public |
| `POST` | `/api/auth/logout` | Logout from current device - uses Bearer access token, no body needed | Bearer JWT |
| `GET` | `/api/auth/my-permissions` | Get current user's effective permissions | Bearer JWT |
| `GET` | `/api/auth/onboarding/form` | Prefill data for the ERP registration form (pending New User) | Bearer JWT |
| `POST` | `/api/auth/onboarding/register` | Submit the ERP registration form - creates the student + login, then signs the student in | Bearer JWT |
| `POST` | `/api/auth/onboarding/upload` | Upload a registration document (pending New User) | Bearer JWT |
| `POST` | `/api/auth/refresh` | Renew access token using refresh token | Bearer JWT |
| `POST` | `/api/auth/reset-password-otp` | Reset password using the OTP received by email | Bearer JWT |
| `GET` | `/api/auth/sessions` | List all active login sessions for current user | Bearer JWT |
| `POST` | `/api/auth/verify-otp` | Verify the OTP without resetting the password (optional pre-check step) | Bearer JWT |

### Category: Bulk Admission (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/student/bulk/admit` | Bulk admit students - full profile CSV (multipart/form-data) | Bearer JWT |
| `GET` | `/api/student/bulk/export` | Export students as the canonical Import/Update CSV | Bearer JWT |
| `GET` | `/api/student/bulk/template` | Download the Student Import/Update CSV template | Bearer JWT |
| `POST` | `/api/student/bulk/update` | Bulk update existing students via CSV (matched by admission_no / student_id / username) | Bearer JWT |
| `POST` | `/api/student/bulk/update-credentials` | Bulk update student credentials via CSV (multipart/form-data - file required) | Bearer JWT |

### Category: Career (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/career/applications` | List job applications | Bearer JWT |
| `POST` | `/api/career/applications` | Add a job application | Bearer JWT |
| `DELETE` | `/api/career/applications/{app_id}` | Delete a job application (soft) | Bearer JWT |

### Category: Certifications (22 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/certificates` | List Certificate of Participation entries | Bearer JWT |
| `GET` | `/api/certificates/options` | Filter options: sessions, titles (with counts) and programmes | Bearer JWT |
| `POST` | `/api/certificates/send-bulk` | Email certificates to the selected / all matching entries | Bearer JWT |
| `GET` | `/api/certificates/sessions` | Certificate sessions of a college, with counts | Bearer JWT |
| `POST` | `/api/certificates/sessions` | Create a certificate session | Bearer JWT |
| `GET` | `/api/certificates/sessions/defaults` | Suggested values for a new session of a college | Bearer JWT |
| `DELETE` | `/api/certificates/sessions/{setting_id}` | Delete a certificate session without entries | Bearer JWT |
| `GET` | `/api/certificates/sessions/{setting_id}` | One certificate session | Bearer JWT |
| `PUT` | `/api/certificates/sessions/{setting_id}` | Save a certificate session | Bearer JWT |
| `GET` | `/api/certificates/sessions/{setting_id}/preview` | Sample certificate PDF of a session | Bearer JWT |
| `DELETE` | `/api/certificates/sessions/{setting_id}/{kind}` | Remove a session's signature or template | Bearer JWT |
| `POST` | `/api/certificates/sessions/{setting_id}/{kind}` | Upload a session's signature or template (PNG/JPG/PDF) | Bearer JWT |
| `PUT` | `/api/certificates/settings` | Save a college's certificate configuration | Bearer JWT |
| `GET` | `/api/certificates/settings/{school_id}` | Certificate configuration of one college | Bearer JWT |
| `GET` | `/api/certificates/settings/{school_id}/preview` | Sample certificate PDF with the current configuration | Bearer JWT |
| `DELETE` | `/api/certificates/settings/{school_id}/{kind}` | Remove the signature or template | Bearer JWT |
| `POST` | `/api/certificates/settings/{school_id}/{kind}` | Upload signature or template (PNG/JPG/PDF) | Bearer JWT |
| `GET` | `/api/certificates/titles` | Session titles of a college or certificate session, with entry counts | Bearer JWT |
| `POST` | `/api/certificates/titles` | Add a session title | Bearer JWT |
| `PATCH` | `/api/certificates/titles/{title_id}` | Edit / activate / deactivate a session title | Bearer JWT |
| `GET` | `/api/certificates/{entry_id}/download` | Download the certificate PDF for one entry | Bearer JWT |
| `POST` | `/api/certificates/{entry_id}/send` | Email the certificate PDF to the participant | Bearer JWT |

### Category: College Transfer Groups (13 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/college-transfer` | Look up a student for college transfer (shows allowed target colleges) | Bearer JWT |
| `POST` | `/api/admin/college-transfer` | Transfer a student to another college within the same group | Bearer JWT |
| `GET` | `/api/admin/college-transfer-groups` | List all college transfer groups | Bearer JWT |
| `POST` | `/api/admin/college-transfer-groups` | Create a new college transfer group | Bearer JWT |
| `GET` | `/api/admin/college-transfer-groups/users` | List users by school + role for the operator assignment dropdown | Bearer JWT |
| `DELETE` | `/api/admin/college-transfer-groups/{group_id}` | Delete a transfer group | Bearer JWT |
| `GET` | `/api/admin/college-transfer-groups/{group_id}` | Get transfer group details (colleges + operators) | Bearer JWT |
| `PUT` | `/api/admin/college-transfer-groups/{group_id}` | Update transfer group name / description | Bearer JWT |
| `POST` | `/api/admin/college-transfer-groups/{group_id}/colleges` | Add a college to a transfer group | Bearer JWT |
| `DELETE` | `/api/admin/college-transfer-groups/{group_id}/colleges/{school_id}` | Remove a college from a transfer group | Bearer JWT |
| `POST` | `/api/admin/college-transfer-groups/{group_id}/operators` | Assign a user as a transfer operator for a group | Bearer JWT |
| `DELETE` | `/api/admin/college-transfer-groups/{group_id}/operators/{user_id}` | Remove a user from a transfer group's operator list | Bearer JWT |
| `POST` | `/api/admin/college-transfer/bulk` | Bulk-transfer ALL active students from one college to another (same group) | Bearer JWT |

### Category: Continuation Form Setup (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/continuation-form` | List configured classes for a school | Bearer JWT |
| `PUT` | `/api/admin/continuation-form` | Create or replace a class's form overrides | Bearer JWT |
| `GET` | `/api/admin/continuation-form/catalog` | Configurable field catalog for the setup UI | Bearer JWT |
| `GET` | `/api/admin/continuation-form/{school_id}/{class_id}` | Get a class's form overrides | Bearer JWT |

### Category: Course File (1 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/course-file/export/pdf` | Download the Course File as a PDF | Bearer JWT |

### Category: Course Promotion (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/course-promote` | Get students eligible for promotion | Bearer JWT |
| `POST` | `/api/academic/course-promote` | Promote selected students to a new course / section | Bearer JWT |
| `GET` | `/api/academic/course-promote/history` | Promotion history (offset pagination) | Bearer JWT |

### Category: Course Routine (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/routines` | Create a new routine slot | Bearer JWT |
| `DELETE` | `/api/routines/{routine_id}` | Delete a routine slot | Bearer JWT |
| `PUT` | `/api/routines/{routine_id}` | Update a routine slot | Bearer JWT |
| `GET` | `/api/routines/{school_id}` | List routines for a college (filter by routine/day/course/section/teacher) | Bearer JWT |

### Category: Courses (6 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/courses` | List all courses (offset pagination + search + sorting) | Bearer JWT |
| `POST` | `/api/academic/courses` | Create a new course | Bearer JWT |
| `POST` | `/api/academic/courses/bulk` | Create multiple courses at once | Bearer JWT |
| `DELETE` | `/api/academic/courses/{class_id}` | Delete course (returns 409 if sections are linked) | Bearer JWT |
| `GET` | `/api/academic/courses/{class_id}` | Get course by ID | Bearer JWT |
| `PUT` | `/api/academic/courses/{class_id}` | Update course (all fields optional) | Bearer JWT |

### Category: Credential Delivery (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/email-queue` | List / filter credential emails and their delivery status | Bearer JWT |
| `POST` | `/api/admin/email-queue/process` | Drain + retry pending / queued / failed credential emails | Bearer JWT |
| `GET` | `/api/admin/email-queue/{queue_id}` | Get a single credential email's status and rendered body | Bearer JWT |
| `POST` | `/api/admin/email-queue/{queue_id}/resend` | Resend a credential email (re-renders from stored placeholders) | Bearer JWT |

### Category: Dashboard (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/dashboard` | Role-based dynamic dashboard | Bearer JWT |
| `GET` | `/api/dashboard/celebrations` | Self birthday / work-anniversary greeting | Bearer JWT |

### Category: Dashboard Config Setup (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/dashboard-config` | Get a school's merged dashboard card config | Bearer JWT |
| `PUT` | `/api/admin/dashboard-config` | Replace a school's dashboard card overrides | Bearer JWT |
| `GET` | `/api/admin/dashboard-config/catalog` | Card catalog for the setup UI | Bearer JWT |

### Category: Date & Time (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/date-time/now` | Server date & time (open - no auth) | Bearer JWT |
| `GET` | `/api/date-time/settings` | Get date & time settings | Bearer JWT |
| `PUT` | `/api/date-time/settings` | Update date & time settings (all fields optional) | Bearer JWT |
| `GET` | `/api/date-time/timezones` | Selectable timezones | Bearer JWT |

### Category: Departments (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/admin/departments` | Create a department | Bearer JWT |
| `GET` | `/api/admin/departments/{department_id}` | Get department detail | Bearer JWT |
| `PUT` | `/api/admin/departments/{department_id}` | Update department | Bearer JWT |

### Category: Directory Export (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/guardians/export/{fmt}` | Guardian directory as a PDF or .xlsx - `split` cuts it per batch | Bearer JWT |
| `GET` | `/api/hostel/report/export/{fmt}` | Hostel occupancy as a PDF or .xlsx - `split` cuts it per hostel or room | Bearer JWT |
| `GET` | `/api/hrm/employees/export/{fmt}` | Employee directory as a PDF or .xlsx - `split` cuts it per designation | Bearer JWT |
| `GET` | `/api/teacher/manage/export/{fmt}` | Staff directory as a PDF or .xlsx - `split` cuts it per department | Bearer JWT |

### Category: Downtime (7 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/downtime` | List downtime windows | Bearer JWT |
| `POST` | `/api/downtime` | Schedule a downtime window | Bearer JWT |
| `GET` | `/api/downtime/status` | Current downtime status (open - no auth) | Bearer JWT |
| `DELETE` | `/api/downtime/{downtime_id}` | Delete downtime window | Bearer JWT |
| `GET` | `/api/downtime/{downtime_id}` | Get downtime detail | Bearer JWT |
| `PUT` | `/api/downtime/{downtime_id}` | Update downtime window (all fields optional) | Bearer JWT |
| `POST` | `/api/downtime/{downtime_id}/cancel` | Cancel a downtime / end it now | Bearer JWT |

### Category: E-Library - Genres (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/elibrary/genres` | List e-library genres (paginated) | Bearer JWT |
| `POST` | `/api/elibrary/genres` | Create an e-library genre | Bearer JWT |
| `DELETE` | `/api/elibrary/genres/{genre_id}` | Soft-delete an e-library genre | Bearer JWT |
| `GET` | `/api/elibrary/genres/{genre_id}` | Get genre by ID | Bearer JWT |
| `PATCH` | `/api/elibrary/genres/{genre_id}` | Update an e-library genre | Bearer JWT |

### Category: E-Library - Items (7 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/elibrary/items` | List e-library items (paginated) | Bearer JWT |
| `POST` | `/api/elibrary/items` | Create an e-library item (upload file via POST /files/upload first) | Bearer JWT |
| `DELETE` | `/api/elibrary/items/{item_id}` | Soft-delete an e-library item | Bearer JWT |
| `GET` | `/api/elibrary/items/{item_id}` | Get e-library item by ID | Bearer JWT |
| `PATCH` | `/api/elibrary/items/{item_id}` | Update an e-library item | Bearer JWT |
| `POST` | `/api/elibrary/items/{item_id}/publish` | Publish an e-library item | Bearer JWT |
| `POST` | `/api/elibrary/items/{item_id}/unpublish` | Unpublish an e-library item | Bearer JWT |

### Category: E-Library - Types (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/elibrary/types` | List e-library types (paginated) | Bearer JWT |
| `POST` | `/api/elibrary/types` | Create an e-library type | Bearer JWT |
| `DELETE` | `/api/elibrary/types/{type_id}` | Soft-delete an e-library type | Bearer JWT |
| `GET` | `/api/elibrary/types/{type_id}` | Get type by ID | Bearer JWT |
| `PATCH` | `/api/elibrary/types/{type_id}` | Update an e-library type | Bearer JWT |

### Category: ERP Chat Assistant (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/erp-assistant/session` | Get or create the user's active chat session | Bearer JWT |
| `DELETE` | `/api/erp-assistant/session/{session_id}` | Close session and start fresh on next open | Bearer JWT |
| `POST` | `/api/erp-assistant/session/{session_id}/message` | Send a message and receive an AI response | Bearer JWT |
| `GET` | `/api/erp-assistant/session/{session_id}/messages` | Retrieve full chat history for a session | Bearer JWT |

### Category: ERP Credentials (6 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/credentials` | List ERP credentials (username + plain password) by role / course / user | Bearer JWT |
| `GET` | `/api/admin/credentials/admins` | List Admin (role_id=2) users of a school, for the notify-admins dropdown | Bearer JWT |
| `POST` | `/api/admin/credentials/send_credentials` | Email login credentials to a batch of users | Bearer JWT |
| `POST` | `/api/admin/credentials/{user_id}/reset-password` | Reset a user's password and update the stored plain-text credential | Bearer JWT |
| `POST` | `/api/admin/credentials/{user_id}/reset-username` | Reset a user's username | Bearer JWT |
| `POST` | `/api/admin/credentials/{user_id}/send` | Send ERP credentials to a single user (auto-selects template by role) | Bearer JWT |

### Category: ERP Feedback (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/feedback` | List ERP feedback by role, school or department | Bearer JWT |
| `POST` | `/api/feedback` | Send feedback or a suggestion | Bearer JWT |
| `GET` | `/api/feedback/me` | List the current user's own feedback and suggestions | Bearer JWT |
| `GET` | `/api/feedback/summary` | ERP feedback counts, grouped | Bearer JWT |
| `PATCH` | `/api/feedback/{feedback_id}/reply` | Reply to a feedback or suggestion row | Bearer JWT |

### Category: Ednectar (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/ednectar/assign` | Assign an Ednectar token to a student | Bearer JWT |
| `GET` | `/api/ednectar/students` | List students for the Ednectar screen | Bearer JWT |
| `GET` | `/api/ednectar/tokens` | List Ednectar tokens | Bearer JWT |
| `POST` | `/api/ednectar/tokens` | Bulk-add Ednectar tokens | Bearer JWT |
| `DELETE` | `/api/ednectar/tokens/{token_id}` | Delete an Ednectar token | Bearer JWT |

### Category: Email Templates (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/email-templates` | List email templates (paginated) | Bearer JWT |
| `POST` | `/api/email-templates` | Create an email template | Bearer JWT |
| `DELETE` | `/api/email-templates/{template_id}` | Soft-delete an email template | Bearer JWT |
| `GET` | `/api/email-templates/{template_id}` | Get email template by ID | Bearer JWT |
| `PATCH` | `/api/email-templates/{template_id}` | Update an email template | Bearer JWT |

### Category: Events (7 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/events/abstract-submissions` | List abstract submissions (paginated) | Bearer JWT |
| `GET` | `/api/events/events` | List events (paginated) | Bearer JWT |
| `POST` | `/api/events/events` | Create an event | Bearer JWT |
| `DELETE` | `/api/events/events/{event_id}` | Soft-delete an event | Bearer JWT |
| `GET` | `/api/events/events/{event_id}` | Get event by ID | Bearer JWT |
| `PATCH` | `/api/events/events/{event_id}` | Update an event | Bearer JWT |
| `GET` | `/api/events/impact-registrations` | List impact registrations (paginated) | Bearer JWT |

### Category: Exam (6 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/exam/final-result` | Final result aggregated across all exams | Bearer JWT |
| `GET` | `/api/exam/list` | List exams for a school | Bearer JWT |
| `GET` | `/api/exam/mark` | Marks for a student in a specific subject/exam | Bearer JWT |
| `GET` | `/api/exam/result` | Full result card for a student in an exam | Bearer JWT |
| `GET` | `/api/exam/student-position` | Student rank in class | Bearer JWT |
| `GET` | `/api/exam/total-mark` | Total marks across all subjects for an exam | Bearer JWT |

### Category: File Upload (1 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/files/upload` | Universal file upload - use folder param to specify context | Bearer JWT |

### Category: General (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/announcements/colleges` | Active colleges for the Announcements college picker | Bearer JWT |
| `GET` | `/api/announcements/roles` | Active roles for the Notice audience picker/filter | Bearer JWT |

### Category: Generate Card (28 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/cards/admit-card-settings` | List admit card settings | Bearer JWT |
| `POST` | `/api/cards/admit-card-settings` | Add an admit card setting | Bearer JWT |
| `DELETE` | `/api/cards/admit-card-settings/{setting_id}` | Delete an admit card setting (soft) | Bearer JWT |
| `PUT` | `/api/cards/admit-card-settings/{setting_id}` | Update an admit card setting | Bearer JWT |
| `POST` | `/api/cards/admit-card-settings/{setting_id}/copy` | Paste an admit card setting's design into another college | Bearer JWT |
| `GET` | `/api/cards/generate/employee-id` | Employee ID card data (setting + staff) | Bearer JWT |
| `GET` | `/api/cards/generate/exam-terms` | Exam terms for the admit card generator | Bearer JWT |
| `GET` | `/api/cards/generate/student-admit` | Student admit card data (setting + students) | Bearer JWT |
| `GET` | `/api/cards/generate/student-admit/pdf` | Admit cards for a class as one PDF | Bearer JWT |
| `PATCH` | `/api/cards/generate/student-admit/permissions/bulk` | Bulk set admit card download permission for multiple students | Bearer JWT |
| `GET` | `/api/cards/generate/student-admit/{student_id}/pdf` | Student admit card PDF | Bearer JWT |
| `PATCH` | `/api/cards/generate/student-admit/{student_id}/permission` | Toggle admit card download permission for one student | Bearer JWT |
| `GET` | `/api/cards/generate/student-id` | Student ID card data (setting + students) | Bearer JWT |
| `GET` | `/api/cards/generate/sub-employee-id` | Sub-employee ID card data (setting + emp_idcard) | Bearer JWT |
| `GET` | `/api/cards/generate/teacher-id` | Teacher ID card data (setting + teachers) | Bearer JWT |
| `GET` | `/api/cards/generate/{card_type}/{person_id}/back.png` | Back ID card PNG | Bearer JWT |
| `GET` | `/api/cards/generate/{card_type}/{person_id}/front.png` | Front ID card PNG | Bearer JWT |
| `GET` | `/api/cards/id-card-settings` | List ID card settings | Bearer JWT |
| `POST` | `/api/cards/id-card-settings` | Add an ID card setting | Bearer JWT |
| `DELETE` | `/api/cards/id-card-settings/{setting_id}` | Delete an ID card setting (soft) | Bearer JWT |
| `PUT` | `/api/cards/id-card-settings/{setting_id}` | Update an ID card setting | Bearer JWT |
| `GET` | `/api/cards/my-admit-card` | My own admit card data (student self-service) | Bearer JWT |
| `GET` | `/api/cards/my-admit-card/exam-terms` | Exam terms for the student's own admit card (no special permission) | Bearer JWT |
| `GET` | `/api/cards/my-admit-card/pdf` | Download my own admit card PDF (student self-service) | Bearer JWT |
| `GET` | `/api/cards/student-card-fields` | List all configurable student ID card fields | Bearer JWT |
| `GET` | `/api/cards/sub-employee-departments` | Departments present on emp_idcard | Bearer JWT |
| `PUT` | `/api/cards/sub-employee/{emp_id}` | Update a sub-employee's ID-card details + photo | Bearer JWT |
| `POST` | `/api/cards/sub-employee/{emp_id}/erp-account` | Create ERP - give an emp_idcard employee a login (teachers / employees) | Bearer JWT |

### Category: Grievances (6 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/grievances` | List grievances with optional filters (school/student/receiver names, searchable) | Bearer JWT |
| `POST` | `/api/grievances` | Create a new grievance | Bearer JWT |
| `GET` | `/api/grievances/receivers` | List actual people (users) holding a Role Type, for the Receiver dropdown | Bearer JWT |
| `DELETE` | `/api/grievances/{grievance_id}` | Delete a grievance | Bearer JWT |
| `GET` | `/api/grievances/{grievance_id}` | Get a single grievance | Bearer JWT |
| `PUT` | `/api/grievances/{grievance_id}` | Update a grievance (remark / approval / status) | Bearer JWT |

### Category: Groups (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/groups` | List student groups (offset pagination + search + sorting) | Bearer JWT |
| `POST` | `/api/academic/groups` | Create a student group | Bearer JWT |
| `DELETE` | `/api/academic/groups/{group_id}` | Delete group | Bearer JWT |
| `GET` | `/api/academic/groups/{group_id}` | Get group detail | Bearer JWT |
| `PUT` | `/api/academic/groups/{group_id}` | Update group | Bearer JWT |

### Category: Guardian (7 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/guardians` | List guardians | Bearer JWT |
| `POST` | `/api/guardians` | Create guardian + user account | Bearer JWT |
| `GET` | `/api/guardians/children/{student_id}/monthly-attendance` | Monthly attendance percentage chart for a guardian's child | Bearer JWT |
| `GET` | `/api/guardians/children/{student_id}/weekly-attendance` | Weekly class routine + per-period attendance for a guardian's child | Bearer JWT |
| `DELETE` | `/api/guardians/{guardian_id}` | Deactivate guardian | Bearer JWT |
| `GET` | `/api/guardians/{guardian_id}` | Get guardian detail | Bearer JWT |
| `PATCH` | `/api/guardians/{guardian_id}` | Update guardian | Bearer JWT |

### Category: Guest Lecture (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/academic/guest-lecture/provision` | Enable/disable the guest lecture for the given (college - course) targets | Bearer JWT |
| `GET` | `/api/academic/guest-lecture/targets` | Colleges - courses with their current guest-lecture on/off state | Bearer JWT |

### Category: HRM - Birthday (9 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hrm/birthday-celebrations` | List birthday celebrations | Bearer JWT |
| `GET` | `/api/hrm/birthday-celebrations/{source_type}/{source_id}` | Get birthday record detail (source_type: employee/teacher) | Bearer JWT |
| `PUT` | `/api/hrm/birthday-celebrations/{source_type}/{source_id}` | Update birthday record - writes back to the underlying Employee/Teacher row | Bearer JWT |
| `GET` | `/api/hrm/birthday-celebrations/{source_type}/{source_id}/email-preview` | Preview the birthday email for a record, without sending it | Bearer JWT |
| `POST` | `/api/hrm/birthday-celebrations/{source_type}/{source_id}/send-email` | Send the birthday email for a record right now | Bearer JWT |
| `GET` | `/api/hrm/birthday-settings` | List birthday templates (college-wise pool, cycled within each college) | Bearer JWT |
| `POST` | `/api/hrm/birthday-settings` | Create birthday card template (multipart/form-data) | Bearer JWT |
| `DELETE` | `/api/hrm/birthday-settings/{setting_id}` | Delete birthday setting (permanent) | Bearer JWT |
| `PUT` | `/api/hrm/birthday-settings/{setting_id}` | Update birthday template (multipart/form-data) | Bearer JWT |

### Category: HRM - Designations (6 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hrm/designations` | List designations | Bearer JWT |
| `POST` | `/api/hrm/designations` | Create a designation | Bearer JWT |
| `DELETE` | `/api/hrm/designations/{designation_id}` | Archive designation (soft delete) | Bearer JWT |
| `GET` | `/api/hrm/designations/{designation_id}` | Get designation detail | Bearer JWT |
| `PUT` | `/api/hrm/designations/{designation_id}` | Update designation | Bearer JWT |
| `PATCH` | `/api/hrm/designations/{designation_id}/status` | Activate or archive a designation | Bearer JWT |

### Category: HRM - Documents & Compliance (17 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hrm/compliance/background-checks` | List employee background verification status | Bearer JWT |
| `PATCH` | `/api/hrm/compliance/background-checks/{employee_id}` | Update an employee's background verification status | Bearer JWT |
| `POST` | `/api/hrm/compliance/background-checks/{employee_id}/notify` | Notify the employee about their background check | Bearer JWT |
| `GET` | `/api/hrm/compliance/documents` | List employee documents | Bearer JWT |
| `GET` | `/api/hrm/compliance/documents/missing` | List (employee, missing mandatory doc) pairs - the other half of Pending Documents | Bearer JWT |
| `POST` | `/api/hrm/compliance/documents/missing/notify` | Remind an employee to upload a missing mandatory document | Bearer JWT |
| `POST` | `/api/hrm/compliance/documents/{document_id}/notify` | Notify the employee about this document | Bearer JWT |
| `PATCH` | `/api/hrm/compliance/documents/{document_id}/reject` | Reject a document | Bearer JWT |
| `PATCH` | `/api/hrm/compliance/documents/{document_id}/verify` | Verify a document | Bearer JWT |
| `GET` | `/api/hrm/compliance/policies` | List compliance policies | Bearer JWT |
| `POST` | `/api/hrm/compliance/policies` | Publish a new compliance policy | Bearer JWT |
| `GET` | `/api/hrm/compliance/policies/{policy_id}` | Get a policy (employee self-service, for the Acknowledge page and the onboarding wizard's View Policy links) | Bearer JWT |
| `PATCH` | `/api/hrm/compliance/policies/{policy_id}` | Edit or deactivate a compliance policy | Bearer JWT |
| `POST` | `/api/hrm/compliance/policies/{policy_id}/acknowledge` | Acknowledge a policy (employee self-service) | Bearer JWT |
| `GET` | `/api/hrm/compliance/policies/{policy_id}/acknowledgements` | List acknowledgement status per employee | Bearer JWT |
| `POST` | `/api/hrm/compliance/policies/{policy_id}/acknowledgements/{employee_id}/notify` | Notify an employee to acknowledge a policy | Bearer JWT |
| `GET` | `/api/hrm/compliance/summary` | Overview counts for all 8 compliance KPIs | Bearer JWT |

### Category: HRM - Employee Exit / Offboarding (16 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hrm/offboarding/exits` | List exits | Bearer JWT |
| `POST` | `/api/hrm/offboarding/exits` | HR directly initiates an exit (termination/retirement/contract end/etc.) | Bearer JWT |
| `GET` | `/api/hrm/offboarding/exits/{exit_id}` | Exit detail - checklist, interview, settlement | Bearer JWT |
| `PATCH` | `/api/hrm/offboarding/exits/{exit_id}/approve` | Approve an employee-submitted resignation request | Bearer JWT |
| `POST` | `/api/hrm/offboarding/exits/{exit_id}/cancel` | Cancel an in-progress exit | Bearer JWT |
| `PATCH` | `/api/hrm/offboarding/exits/{exit_id}/checklist/{item_id}` | Update a clearance checklist item | Bearer JWT |
| `POST` | `/api/hrm/offboarding/exits/{exit_id}/complete` | Complete the exit - deactivates the employee's account | Bearer JWT |
| `POST` | `/api/hrm/offboarding/exits/{exit_id}/experience-letter` | Attach the experience letter (already uploaded via /files/upload) | Bearer JWT |
| `POST` | `/api/hrm/offboarding/exits/{exit_id}/notify` | Notify the employee about their exit status | Bearer JWT |
| `POST` | `/api/hrm/offboarding/exits/{exit_id}/relieving-letter` | Attach the relieving letter (already uploaded via /files/upload) | Bearer JWT |
| `PATCH` | `/api/hrm/offboarding/exits/{exit_id}/settlement` | Update full & final settlement status | Bearer JWT |
| `GET` | `/api/hrm/offboarding/my-exit` | My latest exit record, if any (employee self-service) | Bearer JWT |
| `POST` | `/api/hrm/offboarding/my-exit/cancel` | Withdraw my still-pending resignation request (employee self-service) | Bearer JWT |
| `POST` | `/api/hrm/offboarding/my-exit/interview` | Submit my exit interview (employee self-service) | Bearer JWT |
| `POST` | `/api/hrm/offboarding/resign` | Submit a resignation request (employee self-service) | Bearer JWT |
| `GET` | `/api/hrm/offboarding/summary` | Overview counts for all 6 offboarding KPIs | Bearer JWT |

### Category: HRM - Employee Lifecycle (10 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hrm/employee-lifecycle/exits` | Staff who exited in a given month | Bearer JWT |
| `GET` | `/api/hrm/employee-lifecycle/new-joiners` | Employees/teachers who joined in a given month | Bearer JWT |
| `GET` | `/api/hrm/employee-lifecycle/notice-period` | Staff currently serving notice period | Bearer JWT |
| `GET` | `/api/hrm/employee-lifecycle/probation-status` | Full roster currently on probation | Bearer JWT |
| `POST` | `/api/hrm/employee-lifecycle/promote` | Record a promotion (designation/salary-grade change) | Bearer JWT |
| `GET` | `/api/hrm/employee-lifecycle/promotions` | Promotions recorded in a given month | Bearer JWT |
| `GET` | `/api/hrm/employee-lifecycle/summary` | Overview counts for all 6 lifecycle sections | Bearer JWT |
| `POST` | `/api/hrm/employee-lifecycle/transfer` | Transfer a staff member or teacher to another school | Bearer JWT |
| `GET` | `/api/hrm/employee-lifecycle/transfers` | List staff/teacher transfers for a school | Bearer JWT |
| `GET` | `/api/hrm/employee-lifecycle/upcoming-confirmations` | Probation ending soon, not yet confirmed | Bearer JWT |

### Category: HRM - Employees (7 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hrm/employees` | List staff (non-teaching or teaching) | Bearer JWT |
| `POST` | `/api/hrm/employees` | Create employee + user account | Bearer JWT |
| `DELETE` | `/api/hrm/employees/{emp_id}` | Archive employee (soft delete, does not erase data) | Bearer JWT |
| `GET` | `/api/hrm/employees/{emp_id}` | Get employee detail | Bearer JWT |
| `PUT` | `/api/hrm/employees/{emp_id}` | Update employee | Bearer JWT |
| `POST` | `/api/hrm/employees/{emp_id}/notify-official-email` | Notify IT & the candidate once the official email/password are set | Bearer JWT |
| `PATCH` | `/api/hrm/employees/{emp_id}/status` | Activate or archive an employee | Bearer JWT |

### Category: HRM - Manager Assignment (1 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hrm/employees/assigned` | Employees reporting to the logged-in manager | Bearer JWT |

### Category: HRM - Recruitment (20 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hrm/recruitment/candidates` | List candidates | Bearer JWT |
| `POST` | `/api/hrm/recruitment/candidates` | Register a candidate against a requisition | Bearer JWT |
| `PATCH` | `/api/hrm/recruitment/candidates/{candidate_id}/school` | Update a candidate's school/college | Bearer JWT |
| `GET` | `/api/hrm/recruitment/interviews` | List candidate interviews | Bearer JWT |
| `POST` | `/api/hrm/recruitment/interviews` | Schedule a candidate interview | Bearer JWT |
| `GET` | `/api/hrm/recruitment/interviews/by-candidate` | List interviews collapsed to one row per candidate (their latest round) | Bearer JWT |
| `PATCH` | `/api/hrm/recruitment/interviews/{interview_id}` | Reschedule a scheduled interview | Bearer JWT |
| `PATCH` | `/api/hrm/recruitment/interviews/{interview_id}/feedback` | Submit interview feedback and outcome | Bearer JWT |
| `GET` | `/api/hrm/recruitment/offer-letter-settings` | Get this school's offer-letter letterhead setting | Bearer JWT |
| `PUT` | `/api/hrm/recruitment/offer-letter-settings` | Create or update this school's offer-letter letterhead setting | Bearer JWT |
| `GET` | `/api/hrm/recruitment/offers` | List offer letters | Bearer JWT |
| `POST` | `/api/hrm/recruitment/offers` | Send an offer letter to a candidate | Bearer JWT |
| `GET` | `/api/hrm/recruitment/offers/{offer_id}/letter` | Download the offer letter PDF (HR's own copy) | Bearer JWT |
| `PATCH` | `/api/hrm/recruitment/offers/{offer_id}/mark-joined` | Mark an accepted offer's candidate as joined - creates a real Employee record | Bearer JWT |
| `PATCH` | `/api/hrm/recruitment/offers/{offer_id}/respond` | Record the candidate's response to an offer | Bearer JWT |
| `POST` | `/api/hrm/recruitment/offers/{offer_id}/send-letter` | Generate and email the offer letter PDF | Bearer JWT |
| `GET` | `/api/hrm/recruitment/requisitions` | List job requisitions | Bearer JWT |
| `POST` | `/api/hrm/recruitment/requisitions` | Create a job requisition | Bearer JWT |
| `PATCH` | `/api/hrm/recruitment/requisitions/{requisition_id}/status` | Change a job requisition's status | Bearer JWT |
| `GET` | `/api/hrm/recruitment/summary` | Overview counts for all 8 recruitment KPIs | Bearer JWT |

### Category: HRM - Work Anniversary (14 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hrm/work-anniversaries/upcoming` | List upcoming work anniversaries | Bearer JWT |
| `GET` | `/api/hrm/work-anniversaries/upcoming/{entity_id}/email-preview` | Preview the work-anniversary email for a teacher/employee, without sending it | Bearer JWT |
| `POST` | `/api/hrm/work-anniversaries/upcoming/{entity_id}/send-email` | Send the work-anniversary email for a teacher/employee right now | Bearer JWT |
| `GET` | `/api/hrm/work-anniversary-celebrations` | List work anniversary celebrations | Bearer JWT |
| `POST` | `/api/hrm/work-anniversary-celebrations` | Add an employee to work anniversary celebration list (multipart/form-data) | Bearer JWT |
| `DELETE` | `/api/hrm/work-anniversary-celebrations/{record_id}` | Remove from work anniversary list (permanent) | Bearer JWT |
| `GET` | `/api/hrm/work-anniversary-celebrations/{record_id}` | Get work anniversary record detail | Bearer JWT |
| `PUT` | `/api/hrm/work-anniversary-celebrations/{record_id}` | Update work anniversary record (multipart/form-data) | Bearer JWT |
| `GET` | `/api/hrm/work-anniversary-celebrations/{record_id}/email-preview` | Preview the work-anniversary email for a record, without sending it | Bearer JWT |
| `POST` | `/api/hrm/work-anniversary-celebrations/{record_id}/send-email` | Send the work-anniversary email for a record right now | Bearer JWT |
| `GET` | `/api/hrm/work-anniversary-settings` | List work anniversary settings | Bearer JWT |
| `POST` | `/api/hrm/work-anniversary-settings` | Create work anniversary email setting (multipart/form-data) | Bearer JWT |
| `DELETE` | `/api/hrm/work-anniversary-settings/{setting_id}` | Delete work anniversary setting (permanent) | Bearer JWT |
| `PUT` | `/api/hrm/work-anniversary-settings/{setting_id}` | Update work anniversary setting (multipart/form-data) | Bearer JWT |

### Category: Health (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | Liveness probe (public; no database access) | Public |
| `GET` | `/api/health/ready` | Readiness probe (public; verifies the database is reachable) | Public |

### Category: Hostel - Allotments (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hostel/allotments` | List allotment status (paginated) | Bearer JWT |
| `GET` | `/api/hostel/allotments/{member_id}` | Get single allotment detail | Bearer JWT |
| `PATCH` | `/api/hostel/allotments/{member_id}/issue` | Mark allotment as issued | Bearer JWT |
| `GET` | `/api/hostel/allotments/{member_id}/letter` | Get allotment letter data (for PDF rendering) | Bearer JWT |

### Category: Hostel - Attendance (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hostel/attendance` | List attendance records for a date (paginated) | Bearer JWT |
| `GET` | `/api/hostel/attendance/export` | Export attendance records as CSV | Bearer JWT |
| `GET` | `/api/hostel/attendance/summary` | Attendance summary by employee for a date range | Bearer JWT |
| `GET` | `/api/hostel/attendance/{employee_id}` | Get attendance records for a single resident | Bearer JWT |

### Category: Hostel - Bed Assign (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hostel/bed-assign` | List bed assignments | Bearer JWT |
| `POST` | `/api/hostel/bed-assign` | Assign a member to a bed | Bearer JWT |
| `DELETE` | `/api/hostel/bed-assign/{assign_id}` | Unassign a bed from a member | Bearer JWT |

### Category: Hostel - Beds (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hostel/beds` | List hostel beds | Bearer JWT |
| `POST` | `/api/hostel/beds` | Create hostel bed | Bearer JWT |
| `DELETE` | `/api/hostel/beds/{bed_id}` | Delete hostel bed | Bearer JWT |
| `GET` | `/api/hostel/beds/{bed_id}` | Get a single bed | Bearer JWT |
| `PATCH` | `/api/hostel/beds/{bed_id}` | Update hostel bed | Bearer JWT |

### Category: Hostel - Categories (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hostel/categories` | List hostel categories | Bearer JWT |
| `POST` | `/api/hostel/categories` | Create hostel category | Bearer JWT |
| `DELETE` | `/api/hostel/categories/{category_id}` | Delete hostel category | Bearer JWT |
| `GET` | `/api/hostel/categories/{category_id}` | Get a single hostel category | Bearer JWT |
| `PATCH` | `/api/hostel/categories/{category_id}` | Update hostel category | Bearer JWT |

### Category: Hostel - Checkouts (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hostel/checkouts` | List checkout records (paginated) | Bearer JWT |
| `POST` | `/api/hostel/checkouts/{registration_id}/checkout` | Process member checkout | Bearer JWT |

### Category: Hostel - Discounts (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hostel/discounts` | List discount requests (paginated) | Bearer JWT |
| `POST` | `/api/hostel/discounts` | Create discount request | Bearer JWT |
| `PATCH` | `/api/hostel/discounts/{discount_id}/approve` | Approve or reject a discount request | Bearer JWT |

### Category: Hostel - Fee Report (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hostel/fee-report` | List hostel fee report (paginated) | Bearer JWT |
| `GET` | `/api/hostel/fee-report/export` | Export hostel fee report as CSV | Bearer JWT |
| `GET` | `/api/hostel/fee-report/{user_id}` | Get a resident's fee payment details | Bearer JWT |

### Category: Hostel - Fees (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hostel/fees` | List fee records (paginated) | Bearer JWT |
| `POST` | `/api/hostel/fees` | Record fee payment | Bearer JWT |
| `GET` | `/api/hostel/fees/summary/{member_id}` | Fee summary for a hostel member | Bearer JWT |
| `GET` | `/api/hostel/fees/{fee_id}` | Get a single fee record | Bearer JWT |
| `PATCH` | `/api/hostel/fees/{fee_id}/approve` | Approve or reject a fee payment | Bearer JWT |

### Category: Hostel - Floors (6 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hostel/floors` | List hostel floors | Bearer JWT |
| `POST` | `/api/hostel/floors` | Create hostel floor | Bearer JWT |
| `GET` | `/api/hostel/floors/dropdown` | Floor dropdown by hostel (AJAX) | Bearer JWT |
| `DELETE` | `/api/hostel/floors/{floor_id}` | Delete hostel floor | Bearer JWT |
| `GET` | `/api/hostel/floors/{floor_id}` | Get a single floor | Bearer JWT |
| `PATCH` | `/api/hostel/floors/{floor_id}` | Update hostel floor | Bearer JWT |

### Category: Hostel - Hostels (6 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hostel/hostels` | List hostels | Bearer JWT |
| `POST` | `/api/hostel/hostels` | Create hostel building | Bearer JWT |
| `GET` | `/api/hostel/hostels/dropdown` | Hostel dropdown by school (AJAX) | Bearer JWT |
| `DELETE` | `/api/hostel/hostels/{hostel_id}` | Delete hostel building | Bearer JWT |
| `GET` | `/api/hostel/hostels/{hostel_id}` | Get a single hostel | Bearer JWT |
| `PATCH` | `/api/hostel/hostels/{hostel_id}` | Update hostel building | Bearer JWT |

### Category: Hostel - Hostlers (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hostel/hostlers` | List students with hostler registration status (paginated) | Bearer JWT |
| `POST` | `/api/hostel/hostlers/promote` | Promote a student to hostler | Bearer JWT |
| `PATCH` | `/api/hostel/hostlers/{reg_id}/revoke` | Revoke hostler status | Bearer JWT |
| `GET` | `/api/hostel/hostlers/{student_id}` | Get hostler profile by student ID | Bearer JWT |

### Category: Hostel - Members (15 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hostel/members` | List hostel members (paginated) | Bearer JWT |
| `POST` | `/api/hostel/members` | Assign member to room (create member) | Bearer JWT |
| `GET` | `/api/hostel/members/floors-dropdown` | Available floors for assignment (AJAX) | Bearer JWT |
| `GET` | `/api/hostel/members/rooms-dropdown` | Available rooms by floor (AJAX) | Bearer JWT |
| `DELETE` | `/api/hostel/members/{member_id}` | Delete a hostel member (checkout without keeping history) | Bearer JWT |
| `GET` | `/api/hostel/members/{member_id}` | Get a single hostel member | Bearer JWT |
| `PATCH` | `/api/hostel/members/{member_id}` | Update member room assignment | Bearer JWT |
| `GET` | `/api/hostel/members/{member_id}/payments` | Payment history for a member (Payment Status) | Bearer JWT |
| `POST` | `/api/hostel/members/{member_id}/payments` | Record a fee payment with an optional receipt (multipart/form-data) | Bearer JWT |
| `DELETE` | `/api/hostel/members/{member_id}/payments/{payment_id}` | Delete a discount entry from a member's payment history | Bearer JWT |
| `DELETE` | `/api/hostel/members/{member_id}/payments/{payment_id}/receipt` | Delete the receipt attached to a payment (keeps the payment record) | Bearer JWT |
| `GET` | `/api/hostel/members/{member_id}/payments/{payment_id}/receipt` | Download the receipt attached to a payment | Bearer JWT |
| `POST` | `/api/hostel/members/{member_id}/payments/{payment_id}/receipt` | Upload / replace the receipt on a payment (multipart/form-data) | Bearer JWT |
| `PATCH` | `/api/hostel/members/{member_id}/reassign` | Assign / Re-Assign a member to a room (and optional bed) | Bearer JWT |
| `POST` | `/api/hostel/members/{member_id}/send-brochure` | Send Hostel Brochure email | Bearer JWT |

### Category: Hostel - Registrations (6 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hostel/registrations` | List hostel registrations (paginated) | Bearer JWT |
| `POST` | `/api/hostel/registrations` | Create new hostel registration (multipart/form-data) | Bearer JWT |
| `DELETE` | `/api/hostel/registrations/{registration_id}` | Delete registration | Bearer JWT |
| `GET` | `/api/hostel/registrations/{registration_id}` | Get a single registration | Bearer JWT |
| `PATCH` | `/api/hostel/registrations/{registration_id}` | Update registration (JSON body) | Bearer JWT |
| `GET` | `/api/hostel/registrations/{registration_id}/admission-form` | Download the Admission Form PDF | Bearer JWT |

### Category: Hostel - Report (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hostel/report` | List hostel report (paginated) | Bearer JWT |
| `GET` | `/api/hostel/report/export` | Export hostel report as CSV | Bearer JWT |
| `GET` | `/api/hostel/report/{reg_id}` | Get single resident deep report | Bearer JWT |

### Category: Hostel - Room Charges (7 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hostel/rooms` | List room charges (paginated) | Bearer JWT |
| `POST` | `/api/hostel/rooms` | Create room charge | Bearer JWT |
| `GET` | `/api/hostel/rooms/floors-dropdown` | Floors dropdown by hostel with room data (AJAX) | Bearer JWT |
| `GET` | `/api/hostel/rooms/types-dropdown` | Room types dropdown by floor (AJAX) | Bearer JWT |
| `DELETE` | `/api/hostel/rooms/{room_id}` | Delete room charge | Bearer JWT |
| `GET` | `/api/hostel/rooms/{room_id}` | Get a single room charge | Bearer JWT |
| `PATCH` | `/api/hostel/rooms/{room_id}` | Update room charge | Bearer JWT |

### Category: Hostel - Room Names (6 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hostel/room-names` | List room names | Bearer JWT |
| `POST` | `/api/hostel/room-names` | Create room name | Bearer JWT |
| `GET` | `/api/hostel/room-names/available` | Available rooms by floor & type (AJAX) | Bearer JWT |
| `DELETE` | `/api/hostel/room-names/{room_name_id}` | Delete room name | Bearer JWT |
| `GET` | `/api/hostel/room-names/{room_name_id}` | Get a single room name | Bearer JWT |
| `PATCH` | `/api/hostel/room-names/{room_name_id}` | Update room name | Bearer JWT |

### Category: IT Support (12 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/it-support/engineers` | List IT engineers available for assignment | Bearer JWT |
| `GET` | `/api/it-support/engineers/by-manager/{manager_id}` | Employees reporting to any of the given managers (legacy assign-list parity) | Bearer JWT |
| `GET` | `/api/it-support/engineers/my-team` | Caller's own assignable team for the Assign Engineer dropdown (pool-aware) | Bearer JWT |
| `GET` | `/api/it-support/engineers/school/{school_id}` | Employees reporting to whoever this college's users report to (dynamic assign-list fallback) | Bearer JWT |
| `GET` | `/api/it-support/tickets` | List tickets (role-filtered; cursor or page pagination) | Bearer JWT |
| `POST` | `/api/it-support/tickets` | Raise a new IT support ticket | Bearer JWT |
| `GET` | `/api/it-support/tickets/search` | Full-text search across tickets | Bearer JWT |
| `GET` | `/api/it-support/tickets/stats` | IT support statistics dashboard - scope depends on the caller: Super Admin/IT Department get every ticket, a reporting manager gets their team's, anyone tickets get assigned to gets their own, everyone else gets the tickets they personally raised | Bearer JWT |
| `GET` | `/api/it-support/tickets/{ticket_id}` | Get single ticket detail | Bearer JWT |
| `PUT` | `/api/it-support/tickets/{ticket_id}/assign` | Assign or reassign IT engineer on a ticket | Bearer JWT |
| `PUT` | `/api/it-support/tickets/{ticket_id}/remark` | Add or update resolution remark | Bearer JWT |
| `PUT` | `/api/it-support/tickets/{ticket_id}/status` | Update ticket status | Bearer JWT |

### Category: Internet Resources (7 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/internet-resources` | List internet resources (offset pagination + search + sorting) | Bearer JWT |
| `POST` | `/api/academic/internet-resources` | Register a student for internet access | Bearer JWT |
| `GET` | `/api/academic/internet-resources/registered-students` | Ids of students that already hold an active registration | Bearer JWT |
| `DELETE` | `/api/academic/internet-resources/{resource_id}` | Delete resource | Bearer JWT |
| `GET` | `/api/academic/internet-resources/{resource_id}` | Get resource detail | Bearer JWT |
| `PUT` | `/api/academic/internet-resources/{resource_id}` | Update resource | Bearer JWT |
| `POST` | `/api/academic/internet-resources/{resource_id}/resend-credentials` | Email the internet credentials to the student again | Bearer JWT |

### Category: Internship (9 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/student/internships` | List internships (enriched with student details) | Bearer JWT |
| `POST` | `/api/student/internships` | Create an internship record | Bearer JWT |
| `POST` | `/api/student/internships/bulk` | Bulk-create internships via CSV | Bearer JWT |
| `GET` | `/api/student/internships/export` | Export internships as the canonical Import CSV | Bearer JWT |
| `GET` | `/api/student/internships/template` | Download the Internship Import CSV template | Bearer JWT |
| `DELETE` | `/api/student/internships/{internship_id}` | Soft delete an internship record | Bearer JWT |
| `GET` | `/api/student/internships/{internship_id}` | Get one internship | Bearer JWT |
| `PUT` | `/api/student/internships/{internship_id}` | Update an internship record | Bearer JWT |
| `PATCH` | `/api/student/internships/{internship_id}/status` | Toggle an internship Active (1) / Inactive (0) | Bearer JWT |

### Category: Leave - Approval Setup (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `PUT` | `/api/leave/approval-setup` | Apply one Level 1 / Level 2 setup to the selected employees | Bearer JWT |
| `GET` | `/api/leave/approval-setup/approvers` | Search active staff in every college | Bearer JWT |
| `POST` | `/api/leave/approval-setup/clear` | Remove the approval setup of the selected employees | Bearer JWT |
| `GET` | `/api/leave/approval-setup/employees` | Staff with their Level 1 / Level 2 approval setup | Bearer JWT |
| `GET` | `/api/leave/approval-setup/groups` | Programmes or designations for the staff filter | Bearer JWT |

### Category: Leave - Duty Assignment (6 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/leave/duty-assignment` | Assign or reassign duty for a leave application to at least 3 colleagues | Bearer JWT |
| `GET` | `/api/leave/duty-assignment/eligible-employees` | Active employees (or, for a Teacher applicant, active teachers) at a college for the 'Duty Assign To' picker | Bearer JWT |
| `GET` | `/api/leave/duty-assignment/leave/{leave_id}` | Every duty assignment made for one leave application (applicant or Admin) | Bearer JWT |
| `GET` | `/api/leave/duty-assignment/my-duties` | Duties assigned to me | Bearer JWT |
| `PATCH` | `/api/leave/duty-assignment/{assignment_id}/accept` | Accept an assigned duty | Bearer JWT |
| `PATCH` | `/api/leave/duty-assignment/{assignment_id}/reject` | Reject an assigned duty | Bearer JWT |

### Category: Leave - Manage Reporting (7 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/leave/reporting-managers` | List everyone currently flagged as a reporting manager | Bearer JWT |
| `POST` | `/api/leave/reporting-managers` | Promote a user to reporting manager | Bearer JWT |
| `GET` | `/api/leave/reporting-managers/picker/employees` | Lean employee list for the Add-team picker (no employees.view permission required) | Bearer JWT |
| `GET` | `/api/leave/reporting-managers/{manager_user_id}/assignments` | Employees currently reporting to this manager | Bearer JWT |
| `POST` | `/api/leave/reporting-managers/{manager_user_id}/assignments` | Assign employees to report to this manager (legacy 'Reporting to you' picker) | Bearer JWT |
| `DELETE` | `/api/leave/reporting-managers/{manager_user_id}/assignments/{employee_user_id}` | Remove one employee from this manager's team | Bearer JWT |
| `DELETE` | `/api/leave/reporting-managers/{user_id}` | Demote a user from reporting manager | Bearer JWT |

### Category: Leave - Tracker (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/leave/tracker` | Every leave request with its current stage | Bearer JWT |
| `GET` | `/api/leave/tracker/employees` | Search active staff for the employee filter | Bearer JWT |
| `GET` | `/api/leave/tracker/groups` | Programmes or designations for the department filter | Bearer JWT |
| `GET` | `/api/leave/tracker/summary` | Count per stage for the current tracker filters | Bearer JWT |

### Category: Leave Allocation (10 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/leave/allocation` | List leave allocations | Bearer JWT |
| `POST` | `/api/leave/allocation` | Create a leave allocation | Bearer JWT |
| `POST` | `/api/leave/allocation/bulk-upload` | Bulk-upload allocations via CSV | Bearer JWT |
| `POST` | `/api/leave/allocation/set` | Bulk set (create-or-update) several leave-type allocations for one user in one call | Bearer JWT |
| `GET` | `/api/leave/allocation/summary` | List view - every allocated user, leave types grouped (offset pagination) | Bearer JWT |
| `GET` | `/api/leave/allocation/template` | Download the leave allocation CSV template | Bearer JWT |
| `GET` | `/api/leave/allocation/users` | Employee search for the Set Allocation form (cross-college when no school_id is given) | Bearer JWT |
| `DELETE` | `/api/leave/allocation/{allocation_id}` | Delete an allocation | Bearer JWT |
| `GET` | `/api/leave/allocation/{allocation_id}` | Get a single allocation | Bearer JWT |
| `PUT` | `/api/leave/allocation/{allocation_id}` | Update an allocation | Bearer JWT |

### Category: Leave Applications (18 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/leave/applications` | List leave applications | Bearer JWT |
| `POST` | `/api/leave/applications` | Submit a leave application | Bearer JWT |
| `GET` | `/api/leave/applications/balance/{user_id}` | Leave balance per type for a user | Bearer JWT |
| `GET` | `/api/leave/applications/my` | My own leave applications | Bearer JWT |
| `GET` | `/api/leave/applications/pending` | Pending (waiting) applications - admin view | Bearer JWT |
| `GET` | `/api/leave/applications/pending-approvals` | Applications waiting on me to act | Bearer JWT |
| `GET` | `/api/leave/applications/rejected` | Declined applications for the current user | Bearer JWT |
| `GET` | `/api/leave/applications/summary` | Counts for the Leave Applications summary cards | Bearer JWT |
| `DELETE` | `/api/leave/applications/{application_id}` | Soft-delete a leave application | Bearer JWT |
| `GET` | `/api/leave/applications/{application_id}` | Get a leave application | Bearer JWT |
| `PUT` | `/api/leave/applications/{application_id}` | Edit a still-pending leave application | Bearer JWT |
| `POST` | `/api/leave/applications/{application_id}/approve` | Approve - only the current approver may call this | Bearer JWT |
| `POST` | `/api/leave/applications/{application_id}/attachment` | Upload attachment for a leave application | Bearer JWT |
| `POST` | `/api/leave/applications/{application_id}/forward` | Forward to the next approver in the chain | Bearer JWT |
| `GET` | `/api/leave/applications/{application_id}/history` | Full approval history for an application | Bearer JWT |
| `GET` | `/api/leave/applications/{application_id}/progress` | Current stage and next step (duty colleagues, approvers, informed people) | Bearer JWT |
| `POST` | `/api/leave/applications/{application_id}/reject` | Reject - only the current approver may call this | Bearer JWT |
| `PATCH` | `/api/leave/applications/{application_id}/status` | Legacy: update leave status (waiting / approved / declined) - Admin or current approver only | Bearer JWT |

### Category: Leave Dashboard (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/leave/dashboard` | Leave module dashboard tiles + balance + recent applications | Bearer JWT |
| `GET` | `/api/leave/dashboard/manage` | Admin 'Manage Leave' overview tiles (leave applications/types/roles/designations) | Bearer JWT |
| `GET` | `/api/leave/dashboard/users` | College+role scoped user picker for the Super Admin/Admin dashboard browse (?user_id=) | Bearer JWT |

### Category: Leave Holidays (6 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/leave/holidays` | List holidays (global - same list for every college, paginated) | Bearer JWT |
| `POST` | `/api/leave/holidays` | Create a holiday | Bearer JWT |
| `GET` | `/api/leave/holidays/calendar` | Calendar view - shows weekends, holidays, leave counts | Bearer JWT |
| `DELETE` | `/api/leave/holidays/{holiday_id}` | Delete a holiday | Bearer JWT |
| `GET` | `/api/leave/holidays/{holiday_id}` | Get a single holiday | Bearer JWT |
| `PUT` | `/api/leave/holidays/{holiday_id}` | Update a holiday | Bearer JWT |

### Category: Leave Types (6 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/leave/types` | List leave types | Bearer JWT |
| `POST` | `/api/leave/types` | Create a new leave type | Bearer JWT |
| `GET` | `/api/leave/types/applicant-roles` | "Applicant Type" options - only roles with a leave type configured for this college | Bearer JWT |
| `DELETE` | `/api/leave/types/{type_id}` | Delete a leave type | Bearer JWT |
| `GET` | `/api/leave/types/{type_id}` | Get a leave type by ID | Bearer JWT |
| `PUT` | `/api/leave/types/{type_id}` | Update a leave type | Bearer JWT |

### Category: Library (16 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/library/books` | List books | Bearer JWT |
| `POST` | `/api/library/books` | Add a book | Bearer JWT |
| `DELETE` | `/api/library/books/{book_id}` | Delete a book (soft) | Bearer JWT |
| `PUT` | `/api/library/books/{book_id}` | Update a book | Bearer JWT |
| `GET` | `/api/library/ebooks` | List e-books | Bearer JWT |
| `POST` | `/api/library/ebooks` | Add an e-book | Bearer JWT |
| `DELETE` | `/api/library/ebooks/{ebook_id}` | Delete an e-book (soft) | Bearer JWT |
| `PUT` | `/api/library/ebooks/{ebook_id}` | Update an e-book | Bearer JWT |
| `GET` | `/api/library/issues` | List book issues | Bearer JWT |
| `POST` | `/api/library/issues` | Issue a book to a member | Bearer JWT |
| `DELETE` | `/api/library/issues/{issue_id}` | Delete a book issue (soft) | Bearer JWT |
| `PUT` | `/api/library/issues/{issue_id}/return` | Mark a book issue as returned | Bearer JWT |
| `GET` | `/api/library/members` | List library members | Bearer JWT |
| `POST` | `/api/library/members` | Add a student as a library member | Bearer JWT |
| `DELETE` | `/api/library/members/{member_id}` | Remove a library member (soft) | Bearer JWT |
| `GET` | `/api/library/non-members` | List students who are not library members | Bearer JWT |

### Category: Live Classes (8 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/live-classes` | List live classes (offset pagination + sorting) | Bearer JWT |
| `POST` | `/api/academic/live-classes` | Schedule a live class | Bearer JWT |
| `DELETE` | `/api/academic/live-classes/{lc_id}` | Cancel / delete live class | Bearer JWT |
| `GET` | `/api/academic/live-classes/{lc_id}` | Get live class detail | Bearer JWT |
| `PUT` | `/api/academic/live-classes/{lc_id}` | Update live class (all fields optional) | Bearer JWT |
| `POST` | `/api/academic/live-classes/{lc_id}/end` | End live class - sets class_status to 'completed' | Bearer JWT |
| `GET` | `/api/academic/live-classes/{lc_id}/join` | Get meeting credentials to join a live class | Bearer JWT |
| `POST` | `/api/academic/live-classes/{lc_id}/start` | Start live class - sets class_status to 'live' | Bearer JWT |

### Category: Lookup - Global Dropdowns (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/lookup/dropdowns` | Global master dropdown - all ERP modules | Bearer JWT |
| `GET` | `/api/lookup/roles` | All active roles, id+name only - any authenticated user | Bearer JWT |

### Category: Manage Colleges (6 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/all_colleges` | List all colleges (offset pagination + search + sorting) | Bearer JWT |
| `GET` | `/api/admin/college_details/{school_id}` | Get college detail | Bearer JWT |
| `PUT` | `/api/admin/colleges/{school_id}` | Update college (JSON) - upload new logos via POST /files/upload first | Bearer JWT |
| `GET` | `/api/admin/colleges/{school_id}/stats` | Get college usage statistics | Bearer JWT |
| `PUT` | `/api/admin/colleges/{school_id}/status` | Toggle college active / inactive | Bearer JWT |
| `POST` | `/api/admin/create-colleges` | Register a new college (JSON) - upload logos via POST /files/upload first | Bearer JWT |

### Category: Manage Student (23 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/student/students` | List students (filter by course, semester, section, status, continuation form, admission date) | Bearer JWT |
| `POST` | `/api/student/students` | Admit student - full profile (JSON) | Bearer JWT |
| `GET` | `/api/student/students/bci` | BCI student list - only regulatory columns fetched from DB | Bearer JWT |
| `GET` | `/api/student/students/next-admission-no` | Peek the admission number the next admit will use | Bearer JWT |
| `PATCH` | `/api/student/students/status` | Bulk toggle students Active (1) / Inactive (0) by user_id | Bearer JWT |
| `POST` | `/api/student/students/validate-unique` | Check identity fields for duplicates (pre-submit) | Bearer JWT |
| `POST` | `/api/student/students/with-files` | Admit student - multipart (profile JSON + documents in one request) | Bearer JWT |
| `DELETE` | `/api/student/students/{student_id}` | Soft delete student + deactivate login account | Bearer JWT |
| `GET` | `/api/student/students/{student_id}` | Get full student detail | Bearer JWT |
| `PUT` | `/api/student/students/{student_id}` | Update student profile (JSON) | Bearer JWT |
| `PATCH` | `/api/student/students/{student_id}/admission-date` | Update a student's admission date | Bearer JWT |
| `PATCH` | `/api/student/students/{student_id}/attendance-basis` | Count a student's attendance from their joining date or the semester start | Bearer JWT |
| `PATCH` | `/api/student/students/{student_id}/college-email` | Update a student's college email | Bearer JWT |
| `GET` | `/api/student/students/{student_id}/documents` | Student documents - every slot, uploaded or not | Bearer JWT |
| `GET` | `/api/student/students/{student_id}/documents/merge` | Every uploaded student document merged into one downloadable PDF | Bearer JWT |
| `DELETE` | `/api/student/students/{student_id}/documents/{key}` | Remove one student document | Bearer JWT |
| `POST` | `/api/student/students/{student_id}/documents/{key}` | Upload or replace one student document (multipart) | Bearer JWT |
| `POST` | `/api/student/students/{student_id}/guardian-login` | Create the Guardian ERP login for a student who has none | Bearer JWT |
| `PATCH` | `/api/student/students/{student_id}/internship-status` | Update a student's internship status | Bearer JWT |
| `PATCH` | `/api/student/students/{student_id}/placement` | Move a student - school / course / semester / section only | Bearer JWT |
| `PATCH` | `/api/student/students/{student_id}/roll-no` | Update a student's roll number | Bearer JWT |
| `PATCH` | `/api/student/students/{student_id}/student-status` | Update a student's academic status (Regular / Drop / Transfer / Passed) | Bearer JWT |
| `PUT` | `/api/student/students/{student_id}/with-files` | Update student profile - multipart (profile JSON + documents in one request) | Bearer JWT |

### Category: Media (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/assets/{path}` | Serve a stored asset (authenticated) | Bearer JWT |
| `GET` | `/api/uploads/{path}` | Serve a stored upload (authenticated) | Bearer JWT |

### Category: Mini Project (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/mini-project` | List mini-project teacher assignments | Bearer JWT |
| `POST` | `/api/academic/mini-project` | Assign teacher to student mini-project | Bearer JWT |
| `DELETE` | `/api/academic/mini-project/{record_id}` | Remove mini-project teacher assignment | Bearer JWT |

### Category: Mini Project (Self-service) (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/student/me/mini-project` | My current mini-project teacher assignment | Bearer JWT |
| `POST` | `/api/student/me/mini-project` | Choose a mini-project teacher for myself | Bearer JWT |
| `GET` | `/api/student/me/mini-project/teachers` | List available mini-project teachers for my school | Bearer JWT |

### Category: Move Group (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/move-group` | List students for group move | Bearer JWT |
| `POST` | `/api/academic/move-group` | Move selected students to another group | Bearer JWT |

### Category: My Profile (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/profile/me` | Get my profile (resolved from JWT role) | Bearer JWT |
| `PUT` | `/api/profile/me` | Update my profile (JSON) - upload photos via POST /files/upload first | Bearer JWT |
| `GET` | `/api/profile/me/documents` | My documents - every slot, uploaded or not | Bearer JWT |
| `DELETE` | `/api/profile/me/documents/{key}` | Remove one of my documents | Bearer JWT |
| `POST` | `/api/profile/me/documents/{key}` | Upload or replace one of my documents (multipart) | Bearer JWT |

### Category: New User (13 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/student/new-users` | List New Users awaiting ERP onboarding | Bearer JWT |
| `POST` | `/api/student/new-users` | Create New User (legacy new_user; auto username + password + email) | Bearer JWT |
| `POST` | `/api/student/new-users/bulk` | Bulk-create New Users via CSV | Bearer JWT |
| `GET` | `/api/student/new-users/export` | Export New Users as the canonical Import CSV | Bearer JWT |
| `GET` | `/api/student/new-users/template` | Download the New User Import CSV template | Bearer JWT |
| `POST` | `/api/student/new-users/with-files` | Create New User - multipart (details JSON + admission photo in one request) | Bearer JWT |
| `DELETE` | `/api/student/new-users/{new_user_id}` | Soft delete New User + deactivate login | Bearer JWT |
| `PUT` | `/api/student/new-users/{new_user_id}` | Update a New User (legacy new_user record) | Bearer JWT |
| `PATCH` | `/api/student/new-users/{new_user_id}/college-email` | Assign / clear the college-issued email on a New User | Bearer JWT |
| `PATCH` | `/api/student/new-users/{new_user_id}/form-status` | Update onboarding progress (0=Pending, 1=Completed, 2=In Progress) | Bearer JWT |
| `POST` | `/api/student/new-users/{new_user_id}/resend-credentials` | Re-queue and resend ERP login credentials to a New User | Bearer JWT |
| `PATCH` | `/api/student/new-users/{new_user_id}/status` | Toggle a New User: ERP completed (1) / Inactive (0) | Bearer JWT |
| `PUT` | `/api/student/new-users/{new_user_id}/with-files` | Update a New User - multipart (details JSON + admission photo in one request) | Bearer JWT |

### Category: Niyukti (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/niyukti/list` | List Niyukti (job fest) candidate registrations | Bearer JWT |
| `GET` | `/api/niyukti/list/{registration_id}` | Get a single Niyukti registration | Bearer JWT |

### Category: Notifications (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/notifications` | List my notifications | Bearer JWT |
| `POST` | `/api/notifications/{notification_id}/read` | Mark a notification as read | Bearer JWT |

### Category: OBE - CO-PO Mapping (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/co-po-mapping/matrix` | Get the CO-PO matrix for a subject | Bearer JWT |
| `POST` | `/api/academic/co-po-mapping/matrix` | Save (upsert) the CO-PO matrix for a subject | Bearer JWT |

### Category: OBE - Course Outcomes (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/course-outcomes` | List Course Outcomes | Bearer JWT |
| `POST` | `/api/academic/course-outcomes` | Create a Course Outcome | Bearer JWT |
| `DELETE` | `/api/academic/course-outcomes/{co_id}` | Remove Course Outcome | Bearer JWT |
| `GET` | `/api/academic/course-outcomes/{co_id}` | Get Course Outcome detail | Bearer JWT |
| `PUT` | `/api/academic/course-outcomes/{co_id}` | Update Course Outcome | Bearer JWT |

### Category: OBE - PEOs (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/peos` | List PEOs | Bearer JWT |
| `POST` | `/api/academic/peos` | Create a PEO | Bearer JWT |
| `DELETE` | `/api/academic/peos/{peo_id}` | Remove PEO | Bearer JWT |
| `GET` | `/api/academic/peos/{peo_id}` | Get PEO detail | Bearer JWT |
| `PUT` | `/api/academic/peos/{peo_id}` | Update PEO | Bearer JWT |

### Category: OBE - Program Outcomes (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/program-outcomes` | List Program Outcomes | Bearer JWT |
| `POST` | `/api/academic/program-outcomes` | Create a Program Outcome | Bearer JWT |
| `DELETE` | `/api/academic/program-outcomes/{po_id}` | Remove Program Outcome | Bearer JWT |
| `GET` | `/api/academic/program-outcomes/{po_id}` | Get Program Outcome detail | Bearer JWT |
| `PUT` | `/api/academic/program-outcomes/{po_id}` | Update Program Outcome | Bearer JWT |

### Category: OBE - Unit Outcomes (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/unit-outcomes` | List Unit Outcomes | Bearer JWT |
| `POST` | `/api/academic/unit-outcomes` | Create a Unit Outcome | Bearer JWT |
| `DELETE` | `/api/academic/unit-outcomes/{uo_id}` | Remove Unit Outcome | Bearer JWT |
| `GET` | `/api/academic/unit-outcomes/{uo_id}` | Get Unit Outcome detail | Bearer JWT |
| `PUT` | `/api/academic/unit-outcomes/{uo_id}` | Update Unit Outcome | Bearer JWT |

### Category: OBE - Vision/Mission (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/vision-mission` | List Vision/Mission (offset pagination + search) | Bearer JWT |
| `POST` | `/api/academic/vision-mission` | Create a Vision/Mission | Bearer JWT |
| `DELETE` | `/api/academic/vision-mission/{vm_id}` | Remove Vision/Mission | Bearer JWT |
| `GET` | `/api/academic/vision-mission/{vm_id}` | Get Vision/Mission detail | Bearer JWT |
| `PUT` | `/api/academic/vision-mission/{vm_id}` | Update Vision/Mission | Bearer JWT |

### Category: Offline Exam - Calculations (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/exam/marks/calculate` | List calculation records (paginated) | Bearer JWT |
| `POST` | `/api/exam/marks/calculate` | Calculate marks for an exam term (grade + stats) | Bearer JWT |
| `GET` | `/api/exam/marks/calculate/{calculation_id}` | Get a single calculation record | Bearer JWT |

### Category: Offline Exam - Date Sheet (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/exam/date-sheet` | Date sheet for a term, class and semester | Bearer JWT |
| `PUT` | `/api/exam/date-sheet` | Save (replace) a date sheet | Bearer JWT |

### Category: Offline Exam - Examination Control (10 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/exam/control-config` | List examination policy rules | Bearer JWT |
| `POST` | `/api/exam/control-config` | Create an examination policy rule | Bearer JWT |
| `GET` | `/api/exam/control-config/resolve` | Effective examination policy for a scope | Bearer JWT |
| `DELETE` | `/api/exam/control-config/{config_id}` | Delete an examination policy rule | Bearer JWT |
| `PATCH` | `/api/exam/control-config/{config_id}` | Update an examination policy rule | Bearer JWT |
| `GET` | `/api/exam/marks/freeze` | List marks freeze locks | Bearer JWT |
| `POST` | `/api/exam/marks/freeze` | Freeze marks entry for a scope | Bearer JWT |
| `GET` | `/api/exam/marks/freeze/status` | Is this marks scope frozen? | Bearer JWT |
| `GET` | `/api/exam/marks/revisions` | Marks audit trail (who changed what, when, why) | Bearer JWT |
| `POST` | `/api/exam/marks/unfreeze` | Reopen marks entry for a scope | Bearer JWT |

### Category: Offline Exam - Marks (12 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/exam/marks` | List exam marks (paginated) | Bearer JWT |
| `POST` | `/api/exam/marks` | Record an exam mark | Bearer JWT |
| `POST` | `/api/exam/marks/bulk` | Bulk save marks for a whole section (one subject) | Bearer JWT |
| `POST` | `/api/exam/marks/import` | Import marks from a filled CSV sheet | Bearer JWT |
| `GET` | `/api/exam/marks/roster` | Mark-entry sheet for one subject - exam term | Bearer JWT |
| `GET` | `/api/exam/marks/roster/candidates` | Students who can be added to a previous semester's sheet | Bearer JWT |
| `GET` | `/api/exam/marks/student/{student_id}` | One student's whole mark sheet - every semester/section they have sat a paper in | Bearer JWT |
| `POST` | `/api/exam/marks/student/{student_id}/bulk` | Bulk save marks for one student across many subjects/exam terms | Bearer JWT |
| `GET` | `/api/exam/marks/template` | Download the mark sheet as CSV (roster pre-filled) | Bearer JWT |
| `DELETE` | `/api/exam/marks/{mark_id}` | Delete an exam mark (admin only; cannot delete finalized) | Bearer JWT |
| `GET` | `/api/exam/marks/{mark_id}` | Get a single exam mark | Bearer JWT |
| `PATCH` | `/api/exam/marks/{mark_id}` | Update an exam mark (freeze- and approval-gated) | Bearer JWT |

### Category: Offline Exam - Marks Workflow (9 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/exam/marks/attempts` | Attempt history (regular / re-exam / back paper) | Bearer JWT |
| `POST` | `/api/exam/marks/attempts` | Record a re-exam / back-paper attempt | Bearer JWT |
| `GET` | `/api/exam/marks/backlog` | Students who failed or were absent - the back-paper worklist | Bearer JWT |
| `GET` | `/api/exam/marks/change-requests` | List marks change requests | Bearer JWT |
| `POST` | `/api/exam/marks/change-requests` | Raise a marks change request | Bearer JWT |
| `GET` | `/api/exam/marks/change-requests/{request_id}` | Get one change request | Bearer JWT |
| `POST` | `/api/exam/marks/change-requests/{request_id}/approve` | Approve a change request | Bearer JWT |
| `POST` | `/api/exam/marks/change-requests/{request_id}/cancel` | Cancel your own change request | Bearer JWT |
| `POST` | `/api/exam/marks/change-requests/{request_id}/reject` | Reject a change request | Bearer JWT |

### Category: Offline Exam - Question Papers (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/exam/question-papers` | List exam question papers (paginated) | Bearer JWT |
| `POST` | `/api/exam/question-papers` | Attach a question paper | Bearer JWT |
| `DELETE` | `/api/exam/question-papers/{paper_id}` | Remove a question paper | Bearer JWT |
| `GET` | `/api/exam/question-papers/{paper_id}` | Get question paper by ID | Bearer JWT |
| `PUT` | `/api/exam/question-papers/{paper_id}` | Update a question paper | Bearer JWT |

### Category: Offline Exam - Result Cards (9 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/exam/result-cards` | List result cards (paginated) | Bearer JWT |
| `POST` | `/api/exam/result-cards` | Create a result card | Bearer JWT |
| `GET` | `/api/exam/result-cards/consolidated/{student_id}` | Consolidated report data for one student (all exam terms) | Bearer JWT |
| `GET` | `/api/exam/result-cards/report-card/students` | Students of one class, for the Report Card list | Bearer JWT |
| `GET` | `/api/exam/result-cards/report-card/{student_id}` | Credit / grade point / SGPA report card for one student | Bearer JWT |
| `DELETE` | `/api/exam/result-cards/{result_card_id}` | Delete a result card (admin only) | Bearer JWT |
| `GET` | `/api/exam/result-cards/{result_card_id}` | Get result card by ID | Bearer JWT |
| `PATCH` | `/api/exam/result-cards/{result_card_id}` | Update a result card | Bearer JWT |
| `POST` | `/api/exam/result-cards/{result_card_id}/publish` | Publish a result card (admin only) | Bearer JWT |

### Category: Offline Exam - Result Publication (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/exam/results/publications` | List published result batches | Bearer JWT |
| `POST` | `/api/exam/results/publications/{publication_id}/notify` | Re-send result notifications | Bearer JWT |
| `POST` | `/api/exam/results/publications/{publication_id}/withdraw` | Withdraw published results | Bearer JWT |
| `POST` | `/api/exam/results/publish` | Publish results for a class and notify students | Bearer JWT |

### Category: Offline Exam - Terms (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/exam/terms` | List exam terms (paginated) | Bearer JWT |
| `POST` | `/api/exam/terms` | Create an exam term | Bearer JWT |
| `DELETE` | `/api/exam/terms/{term_id}` | Delete exam term (admin only; cannot delete if marks exist) | Bearer JWT |
| `GET` | `/api/exam/terms/{term_id}` | Get exam term by ID | Bearer JWT |
| `PATCH` | `/api/exam/terms/{term_id}` | Update an exam term | Bearer JWT |

### Category: Online Admission (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/student/online-admissions` | List admission applications (filter by status: 0=pending,1=approved,2=rejected) | Bearer JWT |
| `POST` | `/api/student/online-admissions` | Submit an online admission application | Bearer JWT |
| `GET` | `/api/student/online-admissions/{admission_id}` | Get a single application detail | Bearer JWT |
| `POST` | `/api/student/online-admissions/{admission_id}/approve` | Approve application - creates student + login account | Bearer JWT |
| `POST` | `/api/student/online-admissions/{admission_id}/reject` | Reject a pending application | Bearer JWT |

### Category: Optional Subjects (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/optional-subjects` | List optional subject students | Bearer JWT |
| `POST` | `/api/academic/optional-subjects` | Assign optional subjects to students | Bearer JWT |
| `PUT` | `/api/academic/optional-subjects` | Update assigned optional subjects for a student | Bearer JWT |
| `DELETE` | `/api/academic/optional-subjects/{optional_subject_id}` | Remove a subject | Bearer JWT |

### Category: Optional Subjects (Self-service) (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/student/me/optional-subjects` | My chosen optional/additional subjects | Bearer JWT |
| `POST` | `/api/student/me/optional-subjects` | Choose a catalog subject for myself | Bearer JWT |
| `GET` | `/api/student/me/optional-subjects/catalog` | Catalog subjects available for my class/semester | Bearer JWT |
| `DELETE` | `/api/student/me/optional-subjects/{optional_subject_id}` | Remove one of my own chosen subjects | Bearer JWT |

### Category: Payment Status (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/student/payments` | Consolidated payment status list (one row per student) | Bearer JWT |
| `GET` | `/api/student/payments/{student_id}` | Full payment detail for one student (summary + inits + transactions + gateway logs) | Bearer JWT |

### Category: Permissions (18 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/permissions` | List all permissions (offset pagination + search + sorting) | Bearer JWT |
| `POST` | `/api/admin/permissions` | Create a new permission | Bearer JWT |
| `GET` | `/api/admin/permissions/modules` | List all modules with permissions | Bearer JWT |
| `GET` | `/api/admin/permissions/roles/unlocked` | List roles unlocked for user-specific permissions | Bearer JWT |
| `GET` | `/api/admin/permissions/roles/{role_id}` | Get permissions assigned to a role | Bearer JWT |
| `POST` | `/api/admin/permissions/roles/{role_id}` | Assign permissions to a role by module | Bearer JWT |
| `PUT` | `/api/admin/permissions/roles/{role_id}` | Update specific permissions for a role | Bearer JWT |
| `DELETE` | `/api/admin/permissions/roles/{role_id}/revoke` | Revoke all permissions from a role | Bearer JWT |
| `GET` | `/api/admin/permissions/roles/{role_id}/scopes` | Get a role's per-feature data-visibility scopes | Bearer JWT |
| `PUT` | `/api/admin/permissions/roles/{role_id}/scopes` | Update a role's per-feature data-visibility scopes | Bearer JWT |
| `PUT` | `/api/admin/permissions/roles/{role_id}/unlock` | Enable or disable user-specific permissions for a role | Bearer JWT |
| `GET` | `/api/admin/permissions/roles/{role_id}/users` | List users in a role with their permission override status | Bearer JWT |
| `GET` | `/api/admin/permissions/scopes/options` | List available scope levels and scopable features | Bearer JWT |
| `GET` | `/api/admin/permissions/users/{user_id}` | Get a user's effective permissions | Bearer JWT |
| `PUT` | `/api/admin/permissions/users/{user_id}` | Set a user's extra/revoked permission overrides | Bearer JWT |
| `DELETE` | `/api/admin/permissions/{permission_id}` | Delete permission | Bearer JWT |
| `GET` | `/api/admin/permissions/{permission_id}` | Get permission detail | Bearer JWT |
| `PUT` | `/api/admin/permissions/{permission_id}` | Update permission | Bearer JWT |

### Category: Public - CRM (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/public/crm/create-erp` | Create the ERP account for an admitted CRM lead (public; no auth) | Bearer JWT |
| `POST` | `/api/public/crm/resend-erp` | Resend ERP credentials for an existing CRM lead (public; no auth) | Bearer JWT |

### Category: Public - Certificate Form (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/public/certificate-form` | Submit the Certificate of Participation form | Bearer JWT |
| `GET` | `/api/public/certificate-form/config` | The certificate session behind a form link, with its programmes, years and titles | Bearer JWT |

### Category: Public - Employee ID Card (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/public/employee-idcard` | Download an employee ID card (front+back) as a PNG | Bearer JWT |
| `POST` | `/api/public/employee-idcard/batch` | Download multiple employee ID cards as a ZIP | Bearer JWT |

### Category: Public - Employee Registration (1 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/public/employee-registration` | Submit employee ID-card registration (open; multipart form + photo) | Bearer JWT |

### Category: Public - Kiosk Registration (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/public/kiosk-registration/gate-info` | Gate/college display info for the kiosk header | Bearer JWT |
| `POST` | `/api/public/kiosk-registration/otp/request` | Send a registration OTP to phone or email | Bearer JWT |
| `POST` | `/api/public/kiosk-registration/otp/verify` | Verify the registration OTP | Bearer JWT |
| `POST` | `/api/public/kiosk-registration/register` | Submit self-registration after OTP verification (multipart form + optional photo) | Bearer JWT |

### Category: Public - Law Forms (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/public/law/continuation-form` | Law re-registration / continuation form (x-api-key; school_id 1 & 8 only) | Bearer JWT |
| `GET` | `/api/public/law/continuation-form/eligibility` | Should the re-registration popup open for the logged-in student? (+ prefill) | Bearer JWT |

### Category: Public - Machine Log (1 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/public/machinelog` | Record a biometric (face/QR) employee punch (public; device intake) | Bearer JWT |

### Category: Public - Payment (11 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/public/payment/details` | Resolve a paynow redirect (cid/pid) to its Unpaid fee demand (public) | Bearer JWT |
| `POST` | `/api/public/payment/eazypay/initialise` | Build the EazyPay checkout URL for a fee demand (public; plain text) | Bearer JWT |
| `POST` | `/api/public/payment/hostel/initiate` | Create a hostel fee demand and return the EazyPay checkout URL (public; plain text) | Bearer JWT |
| `POST` | `/api/public/payment/initiate` | Create a fee demand for any college and return its details + EazyPay URL (public) | Bearer JWT |
| `POST` | `/api/public/payment/law/initiate` | Build the Law EazyPay checkout URL for a fee reference (public; plain text) | Bearer JWT |
| `POST` | `/api/public/payment/lbs/initiate` | Build the LBS EazyPay checkout URL for a fee reference (public; plain text) | Bearer JWT |
| `POST` | `/api/public/payment/liet/initiate` | Build the LIET EazyPay checkout URL for a fee reference (public; plain text) | Bearer JWT |
| `POST` | `/api/public/payment/liet/initiate-form` | Create a LIET fee demand and return its details + EazyPay URL (public) | Bearer JWT |
| `POST` | `/api/public/payment/lifs/initiate` | Create a LIFS fee demand and return its details + EazyPay URL (public) | Bearer JWT |
| `POST` | `/api/public/payment/limt/initiate` | Create a LIMT fee demand and return its details + EazyPay URL (public) | Bearer JWT |
| `POST` | `/api/public/payment/pharma/initiate` | Build the Pharma EazyPay checkout URL for a fee reference (public; plain text) | Bearer JWT |

### Category: Public - PlacementHub (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/public/placement-hub/schools` | Active schools (PlacementHub sync) | Bearer JWT |
| `GET` | `/api/public/placement-hub/students` | Students for one school, incremental (PlacementHub sync) | Bearer JWT |

### Category: Public - QR Code (1 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/public/qrcode` | Generate Qrcode | Bearer JWT |

### Category: Public - Student (1 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/public/student/{student_id}` | Student prefill for the public hostel-payment page (x-api-key) | Bearer JWT |

### Category: Public - Student Registration (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/public/student-registration` | Registration form prefill by emailed link (x-api-key; uid + tkn) | Bearer JWT |
| `POST` | `/api/public/student-registration` | Submit ERP student registration (x-api-key; multipart form + files) | Bearer JWT |

### Category: Re-Registration Approval (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/re-registration/approvals/pending` | Students awaiting re-registration approval | Bearer JWT |
| `POST` | `/api/admin/re-registration/approvals/{student_id}/approve` | Approve a submitted re-registration | Bearer JWT |
| `POST` | `/api/admin/re-registration/approvals/{student_id}/reject` | Reject a submitted re-registration | Bearer JWT |

### Category: Re-Registration Setup (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/re-registration` | List enforcement rules for a school | Bearer JWT |
| `PUT` | `/api/admin/re-registration` | Create or replace one enforcement scope rule | Bearer JWT |
| `PUT` | `/api/admin/re-registration/bulk` | Enable/disable re-registration for many courses at once | Bearer JWT |
| `DELETE` | `/api/admin/re-registration/{config_id}` | Delete an enforcement scope rule | Bearer JWT |

### Category: Registration Form Setup (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/registration-form` | List configured classes for a school | Bearer JWT |
| `PUT` | `/api/admin/registration-form` | Create or replace a class's form overrides | Bearer JWT |
| `GET` | `/api/admin/registration-form/catalog` | Configurable field catalog for the setup UI | Bearer JWT |
| `GET` | `/api/admin/registration-form/{school_id}/{class_id}` | Get a class's form overrides | Bearer JWT |

### Category: Roles (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/admin/create-roles` | Create a new role | Bearer JWT |
| `GET` | `/api/admin/roles` | List all roles (offset pagination + search + sorting) | Bearer JWT |
| `DELETE` | `/api/admin/roles/{role_id}` | Archive (soft-delete) a role | Bearer JWT |
| `GET` | `/api/admin/roles/{role_id}` | Get role detail | Bearer JWT |
| `PUT` | `/api/admin/update-roles/{role_id}` | Update role | Bearer JWT |

### Category: Routine PDF (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/routine-pdfs` | Upload a routine PDF (JSON - upload file via POST /files/upload first) | Bearer JWT |
| `DELETE` | `/api/routine-pdfs/{routine_pdf_id}` | Delete a routine PDF (soft) | Bearer JWT |
| `PUT` | `/api/routine-pdfs/{routine_pdf_id}` | Update a routine PDF (JSON) | Bearer JWT |
| `GET` | `/api/routine-pdfs/{school_id}` | List routine PDFs (offset pagination) | Bearer JWT |

### Category: SMS Templates (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/sms-templates` | List sms templates (paginated) | Bearer JWT |
| `POST` | `/api/sms-templates` | Create an sms template | Bearer JWT |
| `DELETE` | `/api/sms-templates/{sms_template_id}` | Soft-delete an sms template | Bearer JWT |
| `GET` | `/api/sms-templates/{sms_template_id}` | Get sms template by ID | Bearer JWT |
| `PATCH` | `/api/sms-templates/{sms_template_id}` | Update an sms template | Bearer JWT |

### Category: Scholarship - Management (23 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/scholarship/applications` | Scholarship application queue | Bearer JWT |
| `POST` | `/api/admin/scholarship/applications/pdf-zip` | Download several applications' PDFs as one ZIP | Bearer JWT |
| `GET` | `/api/admin/scholarship/applications/signature-sheet` | Signature sheet PDF (S. No. / Name / Parent's Name / Signature) for the filtered queue | Bearer JWT |
| `GET` | `/api/admin/scholarship/applications/{application_id}` | Scholarship application detail | Bearer JWT |
| `POST` | `/api/admin/scholarship/applications/{application_id}/approve` | Approve an application | Bearer JWT |
| `POST` | `/api/admin/scholarship/applications/{application_id}/edit` | Correct a field/document on a student's application (multipart) | Bearer JWT |
| `POST` | `/api/admin/scholarship/applications/{application_id}/lock-edit` | Revoke a student's edit access to their submitted application | Bearer JWT |
| `PATCH` | `/api/admin/scholarship/applications/{application_id}/payable-fee` | Set the payable fee on an approved application (Approved tab) | Bearer JWT |
| `GET` | `/api/admin/scholarship/applications/{application_id}/pdf` | Download form + documents as one PDF | Bearer JWT |
| `POST` | `/api/admin/scholarship/applications/{application_id}/reject` | Reject an application | Bearer JWT |
| `POST` | `/api/admin/scholarship/applications/{application_id}/review` | Mark an application reviewed | Bearer JWT |
| `POST` | `/api/admin/scholarship/applications/{application_id}/unlock-edit` | Reopen a student's submitted application for them to edit + resubmit | Bearer JWT |
| `POST` | `/api/admin/scholarship/applications/{application_id}/verify` | Verify & lock an approved application | Bearer JWT |
| `GET` | `/api/admin/scholarship/batch-fees` | Batch (class) fee mappings for a college (all colleges if omitted) | Bearer JWT |
| `POST` | `/api/admin/scholarship/batch-fees` | Map a batch (class) to its full fee | Bearer JWT |
| `DELETE` | `/api/admin/scholarship/batch-fees/{batch_fee_id}` | Remove a batch fee mapping | Bearer JWT |
| `PUT` | `/api/admin/scholarship/batch-fees/{batch_fee_id}` | Edit a batch fee mapping | Bearer JWT |
| `GET` | `/api/admin/scholarship/batches` | Class (batch) + section pickers for the scholarship queue | Bearer JWT |
| `GET` | `/api/admin/scholarship/catalog` | Scholarship form catalog (claim type / category option lists) | Bearer JWT |
| `GET` | `/api/admin/scholarship/windows` | Scholarship windows for a college (all colleges if omitted) | Bearer JWT |
| `POST` | `/api/admin/scholarship/windows` | Open a scholarship window | Bearer JWT |
| `DELETE` | `/api/admin/scholarship/windows/{window_id}` | Close a scholarship window | Bearer JWT |
| `PUT` | `/api/admin/scholarship/windows/{window_id}` | Edit a scholarship window | Bearer JWT |

### Category: School Upgrade (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/school-upgrade` | Look up a student's current placement + payment status by username | Bearer JWT |
| `POST` | `/api/admin/school-upgrade` | Upgrade a student's school / class / section | Bearer JWT |

### Category: Section Upgrade (6 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/section-upgrade` | Get students eligible for section upgrade | Bearer JWT |
| `POST` | `/api/academic/section-upgrade` | Move selected students to a new section | Bearer JWT |
| `GET` | `/api/academic/section-upgrade/export` | Export students eligible for section upgrade as CSV | Bearer JWT |
| `POST` | `/api/academic/section-upgrade/import` | Bulk move students to sections via CSV upload | Bearer JWT |
| `POST` | `/api/academic/section-upgrade/move-cross` | Move students by admission number to a target class + section | Bearer JWT |
| `GET` | `/api/academic/section-upgrade/template` | Download the Section Upgrade CSV import template | Bearer JWT |

### Category: Sections (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/sections` | List sections (offset pagination + search + sorting) | Bearer JWT |
| `POST` | `/api/academic/sections` | Create a new section | Bearer JWT |
| `DELETE` | `/api/academic/sections/{section_id}` | Delete section | Bearer JWT |
| `GET` | `/api/academic/sections/{section_id}` | Get section detail | Bearer JWT |
| `PUT` | `/api/academic/sections/{section_id}` | Update section (all fields optional) | Bearer JWT |

### Category: Security - Audit (1 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/security/audit` | Security module audit trail (SECURITY_AUDIT_VIEW) | Bearer JWT |

### Category: Security - Dashboard (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/security/currently-inside` | Live occupancy - everyone currently checked in (visitors + vendors) | Bearer JWT |
| `GET` | `/api/security/dashboard/stats` | Security dashboard summary counts | Bearer JWT |

### Category: Security - Emergency Occupancy (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/security/dashboard/occupancy-count` | Live headcount for evacuation/drill scenarios | Bearer JWT |
| `GET` | `/api/security/emergency` | List emergency events | Bearer JWT |
| `POST` | `/api/security/emergency/start` | Start an emergency/evacuation event (SECURITY_OVERRIDE) | Bearer JWT |
| `PUT` | `/api/security/emergency/{event_id}/end` | End an emergency/evacuation event (SECURITY_OVERRIDE) | Bearer JWT |

### Category: Security - Files (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `DELETE` | `/api/security/files/{file_id}` | Delete an abandoned (never-linked) upload | Bearer JWT |
| `GET` | `/api/security/files/{file_id}` | Download a visitor/vendor file (authorized, college-scoped) | Bearer JWT |

### Category: Security - Gates (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/security/gates` | List gates | Bearer JWT |
| `POST` | `/api/security/gates` | Add a gate | Bearer JWT |
| `DELETE` | `/api/security/gates/{gate_id}` | Delete a gate | Bearer JWT |
| `PUT` | `/api/security/gates/{gate_id}` | Update a gate | Bearer JWT |
| `GET` | `/api/security/gates/{gate_id}/poster-qr` | QR of a kiosk registration URL for this gate - returns the PNG directly (not a signed pass) | Bearer JWT |

### Category: Security - Live Feed (1 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/security/feed` | Operations live feed - recent audit events + gate scans, merged (VISITOR_DASHBOARD_VIEW) | Bearer JWT |

### Category: Security - QR Passes (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/security/passes/resolve` | Resolve a scanned QR token (mobile gate scanner) | Bearer JWT |
| `PUT` | `/api/security/passes/{pass_id}/revoke` | Revoke a QR pass | Bearer JWT |
| `GET` | `/api/security/visits/{visit_id}/qr` | Get the current active QR pass's metadata (no image, no token) | Bearer JWT |
| `POST` | `/api/security/visits/{visit_id}/qr/generate` | Generate (or regenerate) a QR pass - returns the PNG directly; invalidates any prior active pass | Bearer JWT |

### Category: Security - Reports (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/security/reports/occupancy-trend` | Daily occupancy trend report (PDF/Excel) | Bearer JWT |
| `GET` | `/api/security/reports/vendor-visits` | Vendor visit history report (PDF/Excel) | Bearer JWT |
| `GET` | `/api/security/reports/visits` | Visitor history report (PDF/Excel) | Bearer JWT |

### Category: Security - Retention (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/security/retention-policy` | Get this school's retention policy (defaults if unconfigured) | Bearer JWT |
| `PUT` | `/api/security/retention-policy` | Update this school's retention policy | Bearer JWT |
| `POST` | `/api/security/retention-policy/run` | Preview (dry_run=true, RETENTION.EDIT) or execute (dry_run=false, SECURITY_OVERRIDE.EDIT) a purge | Bearer JWT |

### Category: Security - Vendors (9 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `PUT` | `/api/security/vendor-visits/{visit_id}/check-out` | Check out a vendor visit | Bearer JWT |
| `GET` | `/api/security/vendors` | List vendors | Bearer JWT |
| `POST` | `/api/security/vendors` | Add a vendor | Bearer JWT |
| `GET` | `/api/security/vendors/expiring` | Vendors whose document/contract expires within N days | Bearer JWT |
| `POST` | `/api/security/vendors/expiring/notify` | Notify vendors whose document/contract expires soon | Bearer JWT |
| `DELETE` | `/api/security/vendors/{vendor_id}` | Delete a vendor | Bearer JWT |
| `PUT` | `/api/security/vendors/{vendor_id}` | Update a vendor | Bearer JWT |
| `GET` | `/api/security/vendors/{vendor_id}/visits` | List a vendor's visit log | Bearer JWT |
| `POST` | `/api/security/vendors/{vendor_id}/visits` | Log a vendor visit (check-in) | Bearer JWT |

### Category: Security - Visit Policy (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/security/visit-policy` | Get this school's visit policy (defaults if unconfigured) | Bearer JWT |
| `PUT` | `/api/security/visit-policy` | Update this school's visit policy | Bearer JWT |

### Category: Security - Visits (10 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/security/visitor-profiles/search` | Search visitor profiles for dedupe (phone/email/name/organization) | Bearer JWT |
| `GET` | `/api/security/visits` | List visits (filters: status, search, host, gate, date range) | Bearer JWT |
| `POST` | `/api/security/visits` | Register a new visit (new or existing visitor) | Bearer JWT |
| `GET` | `/api/security/visits/{visit_id}` | Get a single visit | Bearer JWT |
| `PUT` | `/api/security/visits/{visit_id}/approve` | Approve a pending visit | Bearer JWT |
| `PUT` | `/api/security/visits/{visit_id}/cancel` | Cancel a pending or approved visit | Bearer JWT |
| `PUT` | `/api/security/visits/{visit_id}/check-in` | Check in an approved visit | Bearer JWT |
| `PUT` | `/api/security/visits/{visit_id}/check-out` | Check out a checked-in visit | Bearer JWT |
| `PUT` | `/api/security/visits/{visit_id}/override` | Security override - bypass the normal state machine (mandatory reason, audited) | Bearer JWT |
| `PUT` | `/api/security/visits/{visit_id}/reject` | Reject a pending visit | Bearer JWT |

### Category: Security - Watchlist (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/security/watchlist` | List watchlist entries | Bearer JWT |
| `POST` | `/api/security/watchlist` | Add a watchlist entry | Bearer JWT |
| `DELETE` | `/api/security/watchlist/{entry_id}` | Delete a watchlist entry | Bearer JWT |
| `PUT` | `/api/security/watchlist/{entry_id}` | Update a watchlist entry | Bearer JWT |

### Category: Semester Upgrade (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/semester-upgrade` | Get students eligible for semester upgrade | Bearer JWT |
| `POST` | `/api/academic/semester-upgrade` | Upgrade semester for selected students (single or bulk) | Bearer JWT |

### Category: Semesters (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/semesters` | List semesters (offset pagination + search + sorting) | Bearer JWT |
| `POST` | `/api/academic/semesters` | Create a semester | Bearer JWT |
| `DELETE` | `/api/academic/semesters/{semester_id}` | Delete semester | Bearer JWT |
| `GET` | `/api/academic/semesters/{semester_id}` | Get semester detail | Bearer JWT |
| `PUT` | `/api/academic/semesters/{semester_id}` | Update semester | Bearer JWT |

### Category: Session Management (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/admin/sessions/logout-all` | Force-logout all users - terminates every active session system-wide | Bearer JWT |
| `GET` | `/api/admin/sessions/stats` | Session dashboard stats - logins today, password changes, active sessions by role & module | Bearer JWT |

### Category: Settings (19 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/admin/settings/create_email_settings` | Create email/SMTP settings for a college | Bearer JWT |
| `POST` | `/api/admin/settings/create_general_settings` | Create global brand settings (first-time setup) | Bearer JWT |
| `POST` | `/api/admin/settings/create_payment_settings` | Create payment gateway settings for a college | Bearer JWT |
| `POST` | `/api/admin/settings/create_sms_settings` | Create SMS gateway settings for a college | Bearer JWT |
| `DELETE` | `/api/admin/settings/email/{email_setting_id}` | Archive (soft-delete) email/SMTP settings | Bearer JWT |
| `POST` | `/api/admin/settings/email/{email_setting_id}/restore` | Restore archived email/SMTP settings | Bearer JWT |
| `GET` | `/api/admin/settings/email_settings_list` | List email/SMTP settings for all colleges | Bearer JWT |
| `GET` | `/api/admin/settings/get_email_settings/{school_id}` | Get email/SMTP settings | Bearer JWT |
| `GET` | `/api/admin/settings/get_general_settings` | Get global brand settings | Bearer JWT |
| `GET` | `/api/admin/settings/get_payment_settings/{school_id}` | Get payment gateway settings | Bearer JWT |
| `GET` | `/api/admin/settings/get_sms_settings/{school_id}` | Get SMS gateway settings | Bearer JWT |
| `GET` | `/api/admin/settings/payment_settings_list` | List payment gateway settings for all colleges | Bearer JWT |
| `DELETE` | `/api/admin/settings/sms/{sms_setting_id}` | Archive (soft-delete) SMS gateway settings | Bearer JWT |
| `POST` | `/api/admin/settings/sms/{sms_setting_id}/restore` | Restore archived SMS gateway settings | Bearer JWT |
| `GET` | `/api/admin/settings/sms_settings_list` | List SMS gateway settings for all colleges | Bearer JWT |
| `PUT` | `/api/admin/settings/update_email_settings/{school_id}` | Update email/SMTP settings | Bearer JWT |
| `PUT` | `/api/admin/settings/update_general_settings` | Update global settings | Bearer JWT |
| `PUT` | `/api/admin/settings/update_payment_settings/{school_id}` | Update payment gateway settings | Bearer JWT |
| `PUT` | `/api/admin/settings/update_sms_settings/{school_id}` | Update SMS gateway settings | Bearer JWT |

### Category: Staff Activity (4 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/hrm/staff-activities` | List activities (filter by user_id) | Bearer JWT |
| `POST` | `/api/hrm/staff-activities` | Add an activity record for a staff member | Bearer JWT |
| `DELETE` | `/api/hrm/staff-activities/{activity_id}` | Delete an activity record (soft) | Bearer JWT |
| `PUT` | `/api/hrm/staff-activities/{activity_id}` | Update an activity record | Bearer JWT |

### Category: Student - Continuation Form (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/continuation-form` | Submit my re-registration / continuation form | Bearer JWT |
| `GET` | `/api/continuation-form/eligibility` | Should the re-registration popup open for me? (+ prefill) | Bearer JWT |

### Category: Student - Scholarship Form (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/scholarship` | Submit / resubmit my scholarship application | Bearer JWT |
| `GET` | `/api/scholarship/eligibility` | Should the scholarship form open for me? (+ prefill + catalog) | Bearer JWT |
| `GET` | `/api/scholarship/me` | My current scholarship application + status | Bearer JWT |

### Category: Student Activity (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/student/activities` | List activities (filter by student_id) | Bearer JWT |
| `POST` | `/api/student/activities` | Add an activity record for a student | Bearer JWT |

### Category: Student Grievances (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/grievances/student` | List student grievances (with school name, searchable) | Bearer JWT |
| `POST` | `/api/grievances/student` | Create a student grievance | Bearer JWT |
| `DELETE` | `/api/grievances/student/{grievance_id}` | Delete a student grievance | Bearer JWT |
| `GET` | `/api/grievances/student/{grievance_id}` | Get a student grievance by ID | Bearer JWT |
| `PUT` | `/api/grievances/student/{grievance_id}` | Update a student grievance | Bearer JWT |

### Category: Student Roster (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/student/students/bci/export/excel` | BCI student return as one .xlsx | Bearer JWT |
| `GET` | `/api/student/students/bci/export/pdf` | BCI student return as a PDF (or a ZIP when split) | Bearer JWT |
| `GET` | `/api/student/students/roster/excel` | Student list as one .xlsx - `split` becomes worksheets | Bearer JWT |
| `GET` | `/api/student/students/roster/options` | Course - batches - sections tree for the download dialog | Bearer JWT |
| `GET` | `/api/student/students/roster/pdf` | Student list as a PDF (or a ZIP when split) | Bearer JWT |

### Category: Student Self-Service (2 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/student/me/monthly-attendance` | My own monthly attendance percentage chart | Bearer JWT |
| `GET` | `/api/student/me/weekly-attendance` | My own weekly class routine + per-period attendance | Bearer JWT |

### Category: Student Type (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/student/types` | Add a new student type | Bearer JWT |
| `GET` | `/api/student/types/{school_id}` | List student types (status: 1 active, 0 inactive, -1 all) | Bearer JWT |
| `DELETE` | `/api/student/types/{type_id}` | Delete a student type (soft) | Bearer JWT |
| `PUT` | `/api/student/types/{type_id}` | Update a student type | Bearer JWT |
| `PATCH` | `/api/student/types/{type_id}/status` | Activate / mark inactive a student type | Bearer JWT |

### Category: Study Materials (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/study-materials` | List study materials (offset pagination + search + sorting) | Bearer JWT |
| `POST` | `/api/academic/study-materials` | Create study material (JSON) - upload file via POST /files/upload first | Bearer JWT |
| `DELETE` | `/api/academic/study-materials/{material_id}` | Delete study material | Bearer JWT |
| `GET` | `/api/academic/study-materials/{material_id}` | Get study material detail | Bearer JWT |
| `PUT` | `/api/academic/study-materials/{material_id}` | Update study material (JSON) - upload new file via POST /files/upload first | Bearer JWT |

### Category: Subject Allocated (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/subject-allocated` | List subject allocations | Bearer JWT |
| `POST` | `/api/academic/subject-allocated` | Allocate a subject to a teacher | Bearer JWT |
| `DELETE` | `/api/academic/subject-allocated/{allocation_id}` | Remove subject allocation | Bearer JWT |
| `GET` | `/api/academic/subject-allocated/{allocation_id}` | Get subject allocation detail | Bearer JWT |
| `PUT` | `/api/academic/subject-allocated/{allocation_id}` | Update subject allocation | Bearer JWT |

### Category: Subject Topics (10 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/subject-topics` | List topics (pagination + search + sort, filter by college/course/subject) | Bearer JWT |
| `POST` | `/api/academic/subject-topics` | Create topic(s) within a unit | Bearer JWT |
| `POST` | `/api/academic/subject-topics/bulk` | Bulk import topics via CSV (multipart/form-data) | Bearer JWT |
| `GET` | `/api/academic/subject-topics/export` | Export subject topics as the canonical Import CSV | Bearer JWT |
| `PATCH` | `/api/academic/subject-topics/reorder` | Reorder a unit's topics (teaching sequence) | Bearer JWT |
| `GET` | `/api/academic/subject-topics/template` | Download the Subject Topic Import CSV template | Bearer JWT |
| `DELETE` | `/api/academic/subject-topics/{topic_id}` | Delete subject topic (soft) | Bearer JWT |
| `GET` | `/api/academic/subject-topics/{topic_id}` | Get subject topic detail | Bearer JWT |
| `PUT` | `/api/academic/subject-topics/{topic_id}` | Update subject topic | Bearer JWT |
| `PATCH` | `/api/academic/subject-topics/{topic_id}/status` | Activate / mark inactive a subject topic | Bearer JWT |

### Category: Subject Units (11 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/subject-units` | List lesson/units for a class (pagination + search + sort) | Bearer JWT |
| `POST` | `/api/academic/subject-units` | Create lesson/unit(s) for a subject | Bearer JWT |
| `POST` | `/api/academic/subject-units/bulk` | Bulk import lesson/units via CSV (multipart/form-data) | Bearer JWT |
| `GET` | `/api/academic/subject-units/export` | Export subject units as the canonical Import CSV | Bearer JWT |
| `PATCH` | `/api/academic/subject-units/reorder` | Reorder a subject's units (teaching sequence) | Bearer JWT |
| `GET` | `/api/academic/subject-units/template` | Download the Subject Unit Import CSV template | Bearer JWT |
| `DELETE` | `/api/academic/subject-units/{unit_id}` | Delete subject unit (soft) | Bearer JWT |
| `GET` | `/api/academic/subject-units/{unit_id}` | Get subject unit detail | Bearer JWT |
| `PUT` | `/api/academic/subject-units/{unit_id}` | Update subject unit | Bearer JWT |
| `PATCH` | `/api/academic/subject-units/{unit_id}/status` | Activate / mark inactive a subject unit | Bearer JWT |
| `GET` | `/api/academic/unit-plan` | Unit plan tree (Subject - Unit - Topic) for the planner and the printable plan | Bearer JWT |

### Category: Subjects (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/subjects` | List all subjects (offset pagination + search + sorting) | Bearer JWT |
| `POST` | `/api/academic/subjects` | create a subject | Bearer JWT |
| `DELETE` | `/api/academic/subjects/{subject_id}` | Remove subject | Bearer JWT |
| `GET` | `/api/academic/subjects/{subject_id}` | Get subject detail | Bearer JWT |
| `PUT` | `/api/academic/subjects/{subject_id}` | Update subject | Bearer JWT |

### Category: Submissions (7 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/submissions` | List submissions (offset pagination + sorting) | Bearer JWT |
| `POST` | `/api/academic/submissions` | Submit assignment (JSON - upload file first via POST /files/upload) | Bearer JWT |
| `GET` | `/api/academic/submissions/by-assignment/{assignment_id}` | All submissions for a specific assignment (offset pagination) | Bearer JWT |
| `DELETE` | `/api/academic/submissions/{submission_id}` | Delete submission | Bearer JWT |
| `GET` | `/api/academic/submissions/{submission_id}` | Get submission detail | Bearer JWT |
| `PUT` | `/api/academic/submissions/{submission_id}` | Update submission (JSON - upload new file via POST /files/upload first) | Bearer JWT |
| `POST` | `/api/academic/submissions/{submission_id}/evaluate` | Evaluate / grade a submission (teacher action) | Bearer JWT |

### Category: Superadmins (8 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/admin/create-superadmins` | Create superadmin | Bearer JWT |
| `GET` | `/api/admin/superadmins` | List superadmins (offset pagination + sorting + search) | Bearer JWT |
| `DELETE` | `/api/admin/superadmins/{admin_id}` | Archive (soft-delete) a superadmin (403 if is_default) | Bearer JWT |
| `GET` | `/api/admin/superadmins/{admin_id}` | Get superadmin detail | Bearer JWT |
| `POST` | `/api/admin/superadmins/{admin_id}/reset-password` | Reset a superadmin's password | Bearer JWT |
| `POST` | `/api/admin/superadmins/{admin_id}/reset-username` | Reset a superadmin's username | Bearer JWT |
| `POST` | `/api/admin/superadmins/{admin_id}/restore` | Restore an archived superadmin | Bearer JWT |
| `PUT` | `/api/admin/update-superadmins/{admin_id}` | Update superadmin | Bearer JWT |

### Category: Syllabus (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/academic/syllabus` | List syllabuses (offset pagination + search + sorting) | Bearer JWT |
| `POST` | `/api/academic/syllabus` | Create syllabus (JSON) - upload file via POST /files/upload first | Bearer JWT |
| `DELETE` | `/api/academic/syllabus/{syllabus_id}` | Delete syllabus | Bearer JWT |
| `GET` | `/api/academic/syllabus/{syllabus_id}` | Get syllabus detail | Bearer JWT |
| `PUT` | `/api/academic/syllabus/{syllabus_id}` | Update syllabus (JSON) - upload new file via POST /files/upload first | Bearer JWT |

### Category: Teacher - Colleges (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/teacher/manage/{teacher_id}/colleges` | List a teacher's assigned colleges | Bearer JWT |
| `POST` | `/api/teacher/manage/{teacher_id}/colleges` | Link a teacher to an additional college | Bearer JWT |
| `DELETE` | `/api/teacher/manage/{teacher_id}/colleges/{school_id}` | Unlink a teacher from an additional college | Bearer JWT |

### Category: Teacher - Department (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/teacher/departments` | List departments | Bearer JWT |
| `POST` | `/api/teacher/departments` | Create a department | Bearer JWT |
| `DELETE` | `/api/teacher/departments/{dept_id}` | Delete department | Bearer JWT |
| `GET` | `/api/teacher/departments/{dept_id}` | Get department detail | Bearer JWT |
| `PUT` | `/api/teacher/departments/{dept_id}` | Update department | Bearer JWT |

### Category: Teacher - Manage (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/teacher/manage` | List teachers | Bearer JWT |
| `POST` | `/api/teacher/manage` | Create teacher + user account | Bearer JWT |
| `DELETE` | `/api/teacher/manage/{teacher_id}` | Delete teacher | Bearer JWT |
| `GET` | `/api/teacher/manage/{teacher_id}` | Get teacher detail | Bearer JWT |
| `PUT` | `/api/teacher/manage/{teacher_id}` | Update teacher | Bearer JWT |

### Category: Teacher - Profile (3 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/teacher/list` | Paginated list of teachers | Bearer JWT |
| `GET` | `/api/teacher/me` | Get my teacher profile (from token) | Bearer JWT |
| `GET` | `/api/teacher/me/colleges` | Colleges I'm linked to (home + additional) | Bearer JWT |

### Category: Teacher Grievances (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/grievances/teacher` | List teacher grievances (with school name, searchable) | Bearer JWT |
| `POST` | `/api/grievances/teacher` | Create a teacher grievance | Bearer JWT |
| `DELETE` | `/api/grievances/teacher/{grievance_id}` | Delete a teacher grievance | Bearer JWT |
| `GET` | `/api/grievances/teacher/{grievance_id}` | Get a teacher grievance by ID | Bearer JWT |
| `PUT` | `/api/grievances/teacher/{grievance_id}` | Update a teacher grievance | Bearer JWT |

### Category: Themes (5 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/themes` | List themes | Bearer JWT |
| `POST` | `/api/themes` | Create theme | Bearer JWT |
| `DELETE` | `/api/themes/{theme_id}` | Delete theme | Bearer JWT |
| `GET` | `/api/themes/{theme_id}` | Get theme detail | Bearer JWT |
| `PUT` | `/api/themes/{theme_id}` | Update theme (all fields optional) | Bearer JWT |

### Category: Users (7 endpoints)

| Method | Path | Summary / Function | Authentication |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/user-activities` | List user activity logs - filter by college, role, class, users, search | Bearer JWT |
| `GET` | `/api/admin/user-activities/filters` | Modules that actually have activity rows (optionally scoped to one college) | Bearer JWT |
| `GET` | `/api/admin/users` | List users - filter by college + role, sortable with page pagination | Bearer JWT |
| `GET` | `/api/admin/users/{user_id}` | Get single user detail | Bearer JWT |
| `PUT` | `/api/admin/users/{user_id}` | Edit user - updates Profile + Social Link fields | Bearer JWT |
| `GET` | `/api/admin/users/{user_id}/courses` | Get course assignments for an admin user | Bearer JWT |
| `PUT` | `/api/admin/users/{user_id}/courses` | Set course assignments for an admin user | Bearer JWT |

---

## 4. Static & Infrastructure Endpoints

| Method | Path | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/health` | Application health ping | Public |
| `GET` | `/health/ready` | Readiness probe | Public |
| `GET` | `/api/openapi.json` | Complete live OpenAPI 3.0 specification | Public |
| `GET` | `/api/assets/{path}` | Static media and uploaded college assets | Public / Session |
| `GET` | `/api/uploads/{path}` | User and student uploaded documents | Public / Session |
| `GET` | `/erp/assets/...` | Frontend assets (logos, icons, fonts) | Public |
