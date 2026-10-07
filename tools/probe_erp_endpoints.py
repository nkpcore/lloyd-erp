import urllib.request
import urllib.error
import ssl
import json

API_BASE = "https://erp.lloydcollege.in/api"

endpoints_to_test = [
    # Auth
    ("POST", "/auth/login"),
    ("POST", "/auth/refresh"),
    ("POST", "/auth/logout"),
    ("GET", "/auth/me"),
    ("GET", "/auth/user"),
    # Student Profile & Details
    ("GET", "/student/me"),
    ("GET", "/student/me/profile"),
    ("GET", "/student/profile"),
    ("GET", "/student/details"),
    ("GET", "/student/info"),
    ("GET", "/user/me"),
    ("GET", "/user/profile"),
    ("GET", "/me"),
    ("GET", "/profile"),
    # Student Attendance & Academics
    ("GET", "/student/me/monthly-attendance"),
    ("GET", "/student/me/weekly-attendance"),
    ("GET", "/student/me/attendance"),
    ("GET", "/attendance/student"),
    ("GET", "/attendance/me"),
    ("GET", "/attendance/summary"),
    ("GET", "/student/subjects"),
    ("GET", "/student/me/subjects"),
    ("GET", "/student/timetable"),
    ("GET", "/student/me/timetable"),
    ("GET", "/student/schedule"),
    ("GET", "/student/me/schedule"),
    ("GET", "/student/dashboard"),
    ("GET", "/student/me/dashboard"),
    ("GET", "/student/notices"),
    ("GET", "/notices"),
    ("GET", "/notifications"),
    ("GET", "/student/notifications"),
    # System / Docs
    ("GET", "/docs"),
    ("GET", "/openapi.json"),
    ("GET", "/swagger.json"),
    ("GET", "/api-docs"),
]

ctx = ssl.create_default_context()

results = []
for method, path in endpoints_to_test:
    url = f"{API_BASE}{path}"
    req = urllib.request.Request(url, method=method, headers={
        "User-Agent": "LloydERP-AndroidWidget/1.0",
        "Accept": "application/json"
    })
    try:
        with urllib.request.urlopen(req, timeout=5, context=ctx) as resp:
            results.append((method, path, resp.status, "SUCCESS"))
    except urllib.error.HTTPError as e:
        results.append((method, path, e.code, e.reason))
    except Exception as e:
        results.append((method, path, 0, str(e)))

for method, path, code, reason in results:
    status_indicator = "EXISTS" if code in (200, 400, 401, 403, 405, 422) else "NOT FOUND"
    print(f"[{code}] {method} {path} -> {status_indicator} ({reason})")
