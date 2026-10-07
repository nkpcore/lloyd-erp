import json
import os
import re

OPENAPI_PATH = os.path.join(os.path.dirname(__file__), "..", "discovery", "openapi.json")

def analyze():
    if not os.path.exists(OPENAPI_PATH):
        print(f"Error: {OPENAPI_PATH} not found.")
        return

    with open(OPENAPI_PATH, "r", encoding="utf-8") as f:
        spec = json.load(f)

    paths = spec.get("paths", {})
    print(f"Total OpenAPI paths found: {len(paths)}")

    # Endpoints currently called in ErpApiClient.java
    CURRENTLY_CALLED = {
        "/auth/login": "POST",
        "/auth/refresh": "POST",
        "/student/me/monthly-attendance": "GET",
        "/student/me/weekly-attendance": "GET",
        "/attendance/student": "GET"
    }

    categories = {
        "Profile / User / Me": [],
        "Attendance": [],
        "Auth": [],
        "Notifications / Announcements": [],
        "Academics / Timetable / Classes": [],
        "Exams / Grades / Results": [],
        "Fees / Dues": [],
        "System / Downtime / Version": [],
        "Other Student Endpoints": []
    }

    student_related = []

    for path, methods in sorted(paths.items()):
        for method, details in methods.items():
            if method.lower() not in ["get", "post", "put", "delete", "patch"]:
                continue
            
            summary = details.get("summary", "")
            description = details.get("description", "")
            tags = details.get("tags", [])
            operation_id = details.get("operationId", "")
            params = details.get("parameters", [])

            entry = {
                "method": method.upper(),
                "path": path,
                "summary": summary,
                "tags": tags,
                "operation_id": operation_id,
                "parameters": [
                    {
                        "name": p.get("name"),
                        "in": p.get("in"),
                        "required": p.get("required", False),
                        "schema": p.get("schema", {}).get("type", "unknown")
                    }
                    for p in params
                ]
            }

            path_lower = path.lower()
            tags_lower = [t.lower() for t in tags]

            # Categorize
            if "profile" in path_lower or "/me" in path_lower or "user" in path_lower:
                categories["Profile / User / Me"].append(entry)
            elif "attendance" in path_lower:
                categories["Attendance"].append(entry)
            elif "auth" in path_lower or "login" in path_lower or "logout" in path_lower or "token" in path_lower:
                categories["Auth"].append(entry)
            elif "notif" in path_lower or "announc" in path_lower or "notice" in path_lower:
                categories["Notifications / Announcements"].append(entry)
            elif "timetable" in path_lower or "schedule" in path_lower or "subject" in path_lower or "class" in path_lower:
                categories["Academics / Timetable / Classes"].append(entry)
            elif "exam" in path_lower or "grade" in path_lower or "result" in path_lower or "mark" in path_lower:
                categories["Exams / Grades / Results"].append(entry)
            elif "fee" in path_lower or "due" in path_lower or "payment" in path_lower:
                categories["Fees / Dues"].append(entry)
            elif "downtime" in path_lower or "health" in path_lower or "version" in path_lower or "system" in path_lower:
                categories["System / Downtime / Version"].append(entry)
            elif "student" in path_lower:
                categories["Other Student Endpoints"].append(entry)

    print("\n================== SUMMARY BY CATEGORY ==================")
    for cat_name, entries in categories.items():
        print(f"\n### {cat_name} ({len(entries)} endpoints)")
        for e in entries[:15]:  # print first 15 in each category
            is_used = " [CURRENTLY USED]" if e["path"] in CURRENTLY_CALLED else ""
            param_str = ", ".join([f"{p['name']} ({p['in']})" for p in e["parameters"]])
            print(f"  {e['method']:<6} {e['path']:<40} {is_used}")
            if param_str:
                print(f"         Params: {param_str}")
        if len(entries) > 15:
            print(f"         ... and {len(entries) - 15} more")

    # Specifically analyze /profile/me
    print("\n================== DETAILED SCHEMA OF /profile/me ==================")
    if "/profile/me" in paths:
        print(json.dumps(paths["/profile/me"], indent=2))
    elif "/student/me/profile" in paths:
        print(json.dumps(paths["/student/me/profile"], indent=2))
    else:
        # Search all paths matching me or profile
        for p in paths:
            if "profile" in p or "me" in p:
                print(f"Found related path: {p}")

    # Specifically analyze /attendance/student
    print("\n================== DETAILED SCHEMA OF /attendance/student ==================")
    if "/attendance/student" in paths:
        print(json.dumps(paths["/attendance/student"], indent=2))

if __name__ == "__main__":
    analyze()
