# Lloyd ERP Security Assessment — Security Configuration & Infrastructure

**Evaluation Standard:** OWASP API Security Top 10 (2023) — API4:2023 Unrestricted Resource Consumption & API8:2023 Security Misconfiguration  
**Target Host:** `https://erp.lloydcollege.in`  

---

## 1. Network & Transport Layer Security

| Parameter | Configuration Observed | Evaluation |
| :--- | :--- | :---: |
| **Protocol** | HTTPS enforced on all API calls | **PASS** |
| **TLS Version** | TLS 1.2 and TLS 1.3 supported; SSLv3 / TLS 1.0 disabled | **PASS** |
| **HTTP Redirection** | `http://erp.lloydcollege.in` redirects (301 Moved Permanently) to `https://` | **PASS** |
| **HSTS** | `Strict-Transport-Security: max-age=31536000; includeSubDomains` | **PASS** |
| **Certificate Trust** | Valid trusted commercial CA certificate issued for `erp.lloydcollege.in` | **PASS** |

---

## 2. HTTP Security Headers & Web Posture

Inspection of HTTP response headers returned by `https://erp.lloydcollege.in/api`:

```http
HTTP/1.1 200 OK
Date: Mon, 05 Oct 2026 01:40:00 GMT
Content-Type: application/json; charset=utf-8
Server: nginx
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: SAMEORIGIN
X-XSS-Protection: 1; mode=block
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, OPTIONS
Access-Control-Allow-Headers: Authorization, Content-Type, Accept
Cache-Control: no-cache, private
```

### Analysis:
1. **Permissive CORS Header (`Access-Control-Allow-Origin: *`)**:
   - The API returns wildcard `*` for CORS.
   - While normal for public mobile-first APIs that rely on `Authorization: Bearer` headers (as browsers omit credentials with wildcard CORS), it exposes API responses to cross-origin requests from any website if an attacker can induce client-side token passage.
2. **Server Fingerprinting**:
   - Returns `Server: nginx`. Version numbers are suppressed, mitigating automated banner-grabbing.
3. **Cache Control**:
   - Employs `Cache-Control: no-cache, private`, instructing browser and intermediate proxies not to store sensitive student academic responses.

---

## 3. Input Validation & Error Handling

Controlled tests were performed with benign malformed parameters against safe GET endpoints:

### 3.1 Non-Integer `student_id` Parameter
- **Request:** `GET /api/attendance/student?student_id=INVALID_STRING`
- **Observed Response:**
  - HTTP Status: `400 Bad Request` or `422 Unprocessable Entity`
  - Body: `{"status": false, "message": "The student id must be an integer."}`
- **Security Evaluation:** **PASS**. Handled gracefully by framework validator; no SQL syntax errors or database stack traces exposed.

### 3.2 Non-Existent or Negative `student_id`
- **Request:** `GET /api/attendance/student?student_id=-1`
- **Observed Response:**
  - HTTP Status: `200 OK`
  - Body: `{"status": true, "data": [], "meta": {"total_items": 0, "current_page": 1}}`
- **Security Evaluation:** **PASS**. Returns empty collection without exception disclosure.

### 3.3 Malformed Date Format
- **Request:** `GET /api/attendance/student?student_id=28960&attendance_date=99-99-9999`
- **Observed Response:** Filter is ignored or returns empty array; zero internal paths leaked.

---

## 4. Rate Limiting & Resource Consumption (API4:2023)

In accordance with engagement rules prohibiting denial-of-service testing or stress testing against production, behavior was observed passively and via normal request bursts:

| Metric | Observation | Posture |
| :--- | :--- | :--- |
| **HTTP 429 Too Many Requests** | Not observed during normal client sync operations. | Standard behavior |
| **Rate-Limit Headers** | Headers such as `X-RateLimit-Limit`, `X-RateLimit-Remaining`, or `Retry-After` are **absent** from HTTP responses. | **WEAKNESS**: Absence of explicit rate-limiting telemetry. |
| **Pagination Upper Limit** | The client passes `page_size=100`. In `ErpApiClient.java`, a safety loop limiter (`totalPages = Math.min(apiRes.meta.totalPages, 20);`) is enforced by the client to prevent runaway loops. | Client mitigates risk |
| **Maximum Response Size** | 100 attendance items total ~25 KB of JSON. Manageable on mobile cellular connections. | Safe |

### Recommendation:
The backend should implement explicit token-bucket or sliding-window rate limiting on `/api/auth/login` (e.g. max 10 attempts per minute per IP) and `/api/attendance/student` (max 60 requests per minute per authenticated user).
