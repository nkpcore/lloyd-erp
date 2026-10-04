# Lloyd ERP Security Assessment — Web & PWA Client Security Audit

**Target Components:** `web/index.html`, `web/assets/index-DhavIe_O.js`, `web/sw.js`  
**Application Architecture:** Single-Page Application (SPA) / Progressive Web App (PWA)  
**Host Target:** `https://erp.lloydcollege.in`  

---

## 1. Web Client Storage Architecture

The web client (`web/index.html` and compiled Vite bundle `index-DhavIe_O.js`) interfaces directly with the Lloyd ERP REST API.

```text
┌────────────────────────────────────────────────────────┐
│             WEB CLIENT STORAGE MECHANISMS              │
├───────────────────┬──────────────────┬─────────────────┤
│ STORAGE MEDIUM    │ DATA STORED      │ ACCESSIBILITY   │
├───────────────────┼──────────────────┼─────────────────┤
│ localStorage      │ JWT Access Token │ Any JS execution│
│                   │ Refresh Token    │ on same origin  │
│                   │ User Profile     │                 │
├───────────────────┼──────────────────┼─────────────────┤
│ sessionStorage    │ View states      │ Tab-scoped      │
├───────────────────┼──────────────────┼─────────────────┤
│ Cookies (HttpOnly)│ None             │ Not utilized    │
├───────────────────┼──────────────────┼─────────────────┤
│ Service Worker    │ App shell HTML,  │ Offline cache   │
│ Cache API         │ CSS, JS bundles  │                 │
├───────────────────┼──────────────────┼─────────────────┤
│ In-Memory (React) │ Calculated stats │ Component state │
└───────────────────┴──────────────────┴─────────────────┘
```

---

## 2. Storage Security & Vulnerability Analysis

### 2.1 `localStorage` Token Exposure
- **Observed Implementation:**
  ```javascript
  localStorage.setItem('lloyd_token', data.access_token);
  localStorage.setItem('lloyd_user', JSON.stringify(data.user));
  localStorage.setItem('lloyd_refresh_token', data.refresh_token);
  ```
- **Security Implications:**
  - `localStorage` has **no protection against Cross-Site Scripting (XSS)**.
  - Any JavaScript code executing on `erp.lloydcollege.in` (including third-party analytics, compromised CDN dependencies, or injected scripts) can execute `localStorage.getItem('lloyd_token')` and exfiltrate the student's active bearer token.
- **Remediation:**
  - In a hardened web portal, authentication tokens should ideally be set in **`HttpOnly; Secure; SameSite=Strict` cookies** by the backend.
  - This prevents client-side script access entirely while permitting the browser to include the session cookie automatically on requests.

---

## 3. Service Worker & Offline Cache Security (`web/sw.js`)

Inspection of `web/sw.js`:
```javascript
const CACHE_NAME = 'lloyd-attendance-v1';
const ASSETS = [
  '/',
  '/index.html',
  '/manifest.json',
  '/icon.png',
  '/icon-512.png'
];

self.addEventListener('fetch', (e) => {
  if (e.request.url.includes('/api/')) {
    // API calls go direct to network (never cached by SW)
    return;
  }
  e.respondWith(
    caches.match(e.request).then((res) => res || fetch(e.request))
  );
});
```

### Analysis:
- **Evaluation:** **PASS**.
- The service worker explicitly exempts `/api/` traffic from the Cache API (`if (e.request.url.includes('/api/')) return;`).
- This guarantees that sensitive student attendance JSON payloads are **never written to unencrypted browser CacheStorage**, preventing offline data leakage on shared campus terminals.

---

## 4. Scriptable iOS Client Security (`LloydWidget.scriptable.js`)

The repository includes a Scriptable widget script for iOS devices:
```javascript
const USERNAME = "YOUR_ADMISSION_NO"; // e.g. "2023LLOYD1234"
const PASSWORD = "YOUR_PASSWORD";
```

### Security Risks:
1. **Plaintext Credential Storage in Source**: Users must paste their ERP password directly into the script.
2. **iCloud & Repository Sync**: Scriptable scripts by default sync to iCloud Drive. If a student backs up their iOS device or publishes their scripts to a public GitHub repo, their student password is exposed.
3. **Absence of Hardware Enclave**: Unlike the Android app which utilizes Android Keystore (when available), Scriptable scripts store credentials in unencrypted text files within the Scriptable app container.
