# Lloyd ERP Security Assessment — Authentication & Session Assessment

**Evaluation Standard:** OWASP API Security Top 10 (2023) — API2:2023 Broken Authentication  
**Target Host:** `https://erp.lloydcollege.in/api`  
**Authentication Standard:** OAuth2 / Stateless JSON Web Tokens (Bearer JWT)  

---

## 1. Authentication Lifecycle Architecture

The Lloyd ERP authentication architecture utilizes stateless JSON Web Tokens (JWT) accompanied by an opaque refresh token:

```text
Student / Client
       │
       │ 1. POST /api/auth/login { username, password, device_id, ... }
       ▼
Lloyd ERP Gateway
       │
       │ 2. Validate credentials against user database
       │ 3. Generate Access Token (JWT, 24h) & Refresh Token
       ▼
Student Client Receives Tokens
       │
       │ 4. GET /api/student/me/monthly-attendance
       │    Header: Authorization: Bearer <access_token>
       ▼
Lloyd ERP Gateway validates JWT signature & claims
       │
       │ [ Normal Operations for ~24 Hours ]
       │
       ▼ [ Access Token Expires ]
Student Client receives HTTP 401 Unauthorized
       │
       │ 5. POST /api/auth/refresh { refresh_token }
       ▼
Lloyd ERP Gateway validates Refresh Token
       │
       │ 6. Issues new Access Token & Refresh Token
       ▼
Client resumes authenticated requests
       │
       │ 7. Student taps "Logout"
       ▼
Local Client clears tokens (Remote token revocation audit below)
```

---

## 2. Authentication Mechanism Components

| Mechanism | Present? | Implementation Specifics | Security Posture |
| :--- | :---: | :--- | :--- |
| **Username / Password** | Yes | Admission number + account password via JSON body. | Passwords transmitted over TLS. No CAPTCHA observed on standard student login endpoint. |
| **Session Cookie** | No | Neither `Set-Cookie` nor cookie storage is used by the mobile client; strictly token-based. | Eliminates CSRF vectors for native mobile, but SPA web clients require careful token storage. |
| **Access Token** | Yes | RFC 7519 JSON Web Token (JWT) sent via `Authorization: Bearer <token>`. | Standard bearer token. Exposes user claims client-side. |
| **Refresh Token** | Yes | String token exchanged via `POST /api/auth/refresh`. | Prevents constant re-entry of credentials. |
| **Token Expiry** | Yes | `expires_in: 86400` seconds (24 hours). | 24-hour lifetime is relatively long for a financial/academic system with sensitive student data. |
| **Device Identifier** | Yes | Client sends client-generated `device_id: UUID`. | Stored in preferences; does not appear to bind token to device hardware cryptographically. |
| **CSRF Token** | No | Not applicable to mobile Bearer auth. | Normal for stateless Bearer APIs; web client must avoid vulnerable cookie fallback. |

---

## 3. Controlled Authentication Test Results

### 3.1 Test Case A: Credential Validation & Negative Scenarios
- **Scenario A.1 (Invalid Password)**:
  - Input: Valid test student admission number + intentionally incorrect password.
  - Expected: HTTP 401 Unauthorized or HTTP 422 with generic error message.
  - Observed: Server returns JSON response with `"status": false, "message": "Invalid credentials"`.
  - Security Evaluation: **PASS**. Does not leak specific account existence details.
- **Scenario A.2 (Non-Existent Account Identifier)**:
  - Input: Randomly structured non-existent admission format (`TEST_NONEXISTENT_9999`).
  - Expected: Generic "Invalid credentials" response.
  - Observed: Identical error response returned; timing differences are negligible.
  - Security Evaluation: **PASS**. Resistant to basic username enumeration via error message divergence.

### 3.2 Test Case B: Token Expiration & Rejection
- **Observed Token Lifetime**: `expires_in: 86400` (1 full day / 24 hours).
- **Behavior with Expired Token**:
  - Sending an expired Bearer token to `/api/student/me/monthly-attendance` yields:
    - HTTP Status: `401 Unauthorized`
    - Response: `{"status": false, "message": "Unauthenticated."}` or `{"message": "Token has expired"}`.
  - Security Evaluation: **PASS**. Expired access tokens cannot access protected endpoints.
- **Refresh Flow**:
  - Android client `ErpApiClient.ensureValidToken()` catches HTTP 401 and calls `POST /api/auth/refresh`.
  - Fresh token pair is returned, and initial request is replayed transparently.
  - Security Evaluation: **PASS**.

### 3.3 Test Case C: Logout & Server-Side Token Invalidation
- **Client-Side Logout**:
  - Android client executes `AppPreferences.logout()`, which clears all SharedPreferences keys and removes tokens from disk.
- **Server-Side Revocation Audit**:
  - When a student logs out on the client, does the client dispatch a revocation request to the ERP?
  - Code Review: **No `/auth/logout` endpoint is called in `ErpApiClient.java` or `MainActivity.java`**.
  - Impact: The previously issued JWT access token remains cryptographically valid on the backend until its natural 24-hour expiration window lapses!
  - Severity: **MEDIUM (API2:2023 Broken Authentication / Incomplete Logout)**.
  - Remediation: Implement a server-side token blacklist / revocation endpoint (`POST /api/auth/logout`) and call it prior to wiping local credentials.

### 3.4 Test Case D: Session Handling & Concurrency
- **Concurrent Sessions**:
  - Signing into the same student test account from a secondary client (e.g. Web PWA while Android is logged in) issues a distinct second token.
  - The previous token issued to the Android app is **not immediately invalidated** by the ERP server. Both sessions can query attendance concurrently.
  - Evaluation: Normal behavior for student multi-device access, provided refresh tokens are tracked in a revocable device table.
- **Session Rotation**:
  - When `/api/auth/refresh` is executed, the server issues a new `access_token` and updates `refresh_token`.
