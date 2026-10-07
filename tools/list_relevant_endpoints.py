import json
import os

OPENAPI_PATH = os.path.join(os.path.dirname(__file__), "..", "discovery", "openapi.json")

with open(OPENAPI_PATH, "r", encoding="utf-8") as f:
    spec = json.load(f)

paths = spec.get("paths", {})

student_endpoints = []
for p, methods in paths.items():
    if "/student" in p or "/profile" in p or "/attendance" in p or "/notice" in p or "/notification" in p or "/timetable" in p or "/downtime" in p:
        for m, op in methods.items():
            if m.lower() in ["get", "post", "put", "delete"]:
                student_endpoints.append((m.upper(), p, op.get("summary", ""), [t for t in op.get("tags", [])]))

print(f"Total matching endpoints: {len(student_endpoints)}")
print("\n--- Student / Me / Profile Endpoints ---")
for m, p, summary, tags in sorted(student_endpoints, key=lambda x: x[1]):
    if "/me" in p or "profile" in p:
        print(f"{m:<6} {p:<50} | {summary}")

print("\n--- Student Portal Endpoints (starts with /student or /attendance) ---")
for m, p, summary, tags in sorted(student_endpoints, key=lambda x: x[1]):
    if not ("/me" in p or "profile" in p) and ("/attendance" in p or "/student/student" in p or "/downtime" in p or "/notice" in p):
        print(f"{m:<6} {p:<50} | {summary}")
