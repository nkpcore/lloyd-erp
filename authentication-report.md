# Lloyd ERP authentication assessment — current status

Assessment date: 2026-10-06. This is an interim, non-destructive report.

## Verified without authentication

- The public sign-in page is available at `/erp/auth/login`.
- `GET /api/attendance/student` without credentials returned `401 Unauthorized`.
- The API responses observed used `cache-control: no-store, private`.
- Observed headers included `X-Frame-Options: DENY` and `X-Content-Type-Options: nosniff`.
- The supplied assessment scope identifies `POST /api/auth/login` and `POST /api/auth/refresh`; their request and response schemas have not been exercised.

## Blocked tests

The following cannot be safely or validly tested without explicitly authorized synthetic fixtures:

| Test | Required fixture |
|---|---|
| Login success/failure behavior | One disposable authorized test account and its approved test credential workflow |
| JWT claim inspection (`iat`, `exp`, `nbf`, `iss`, `aud`, `sub`, role, token type) | Sanitized access token from a dedicated test account; do not provide real tokens in chat or commit them |
| Refresh token storage, rotation, reuse detection, and expiry | Dedicated test session and refresh-token test procedure |
| Logout revocation | Dedicated test session plus documented logout action/route |
| Password reset/change invalidation | Disposable test account with approved reset/change workflow |
| Account disablement invalidation | Dedicated test account and authorized administrator fixture |
| Malformed/expired/invalid-signature/algorithm validation | Test environment or explicit approval to use locally generated non-sensitive tokens against a test account, with request rate limits |

## Current conclusion

No authentication or JWT vulnerability is confirmed. The previous claims of approximately 24-hour access-token lifetime and ineffective logout remain unverified hypotheses until the required test fixtures are available.

## Safe evidence handling

Record only claim names, expiry deltas, status codes, response schemas, and redacted headers. Never store passwords, full JWTs, refresh tokens, or real student information in this repository.
