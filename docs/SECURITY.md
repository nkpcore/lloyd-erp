# Lloyd Student Application Security Specification

## 1. Security Architecture & Threat Model

This document establishes the security guidelines, token lifecycles, data protection mechanisms, and threat mitigation strategies for the Lloyd Student application.

---

## 2. Threat Analysis & Mitigations

| Threat | Description | Risk Level | Mitigation Strategy |
| :--- | :--- | :--- | :--- |
| **Credential Theft via Storage** | An attacker extracts stored passwords from device storage. | **Critical** | Do NOT persist plaintext passwords. Rely exclusively on OAuth2/JWT refresh tokens with hardware-backed encryption. |
| **Token Hijacking via Man-in-the-Middle** | An attacker on unencrypted campus Wi-Fi intercepts API traffic. | **High** | Strictly enforce HTTPS and TLS 1.3 via `network_security_config.xml`. Deny cleartext HTTP traffic across all domains. |
| **Unauthorized Student Record Access** | Client alters query parameters to request attendance for other students (`/attendance/student?student_id=X`). | **High** | Disallow client tampering. Validate that requested `student_id` strictly matches the verified ID in the authenticated session token. |
| **Sensitive Log Leakage** | System logcat displays user tokens or personal information. | **Medium** | Strip all debug logging in production using Proguard (`-assumenosideeffects class android.util.Log { ... }`). |
| **Token Refresh Denial-of-Service** | Expired tokens trigger infinite re-login loops against ERP. | **Medium** | Mutex synchronization on token refresh; immediate token clearing upon HTTP 401; abort after 1 failed refresh attempt. |

---

## 3. Storage Security (Android Keystore)

- **Library**: `androidx.security:security-crypto:1.1.0-alpha06`
- **MasterKey Configuration**:
  - Key Scheme: `MasterKey.KeyScheme.AES256_GCM`
  - Backed by Android Keystore hardware security module (TEE/StrongBox where available).
- **EncryptedSharedPreferences**:
  - Key Encryption: `PrefKeyEncryptionScheme.AES256_SIV`
  - Value Encryption: `PrefValueEncryptionScheme.AES256_GCM`
- **Fallback Rule**:
  - In the event of a Keystore initialization failure on non-standard Android distributions, the application MUST NOT fall back to storing sensitive passwords in plaintext.
  - Only non-sensitive cache items (e.g. cached timetable) may be stored in private mode preferences.

---

## 4. Token Lifecycle & Session Management

```text
                           [ App Launch ]
                                 │
                                 ▼
                     Has valid Access Token?
                     ├── YES ──► Execute API Request
                     └── NO
                          │
                          ▼
                     Has Refresh Token?
                     ├── YES ──► POST /auth/refresh
                     │                │
                     │                ├── HTTP 200: Save fresh tokens ──► Continue
                     │                └── HTTP 401: Clear tokens ──► Force Login
                     └── NO ──► Prompt User Sign-In
```

1. **Short-Lived Access Tokens**:
   - Transmitted in `Authorization: Bearer <access_token>` headers.
   - Kept in memory and encrypted disk.
2. **Refresh Tokens**:
   - Used only at `POST /auth/refresh`.
   - Never sent to general attendance endpoints.
3. **Session Revocation**:
   - Calling `logout()` immediately wipes the Keystore preferences, cancels all background WorkManager jobs, and clears widget RemoteViews.
