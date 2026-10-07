#!/usr/bin/env python3
"""
Lloyd ERP Live Data Verification Tool
====================================
Interactively authenticates with https://erp.lloydcollege.in/api and retrieves
verified, real-time student attendance records and schedules.
"""

import sys
import getpass
import json
import ssl
import urllib.request
import urllib.error
import argparse
import os

API_BASE = "https://erp.lloydcollege.in/api"

def make_request(url, method="GET", headers=None, data=None):
    ctx = ssl.create_default_context()
    if headers is None:
        headers = {}
    
    headers.setdefault("User-Agent", "LloydERP-CLI/1.0")
    headers.setdefault("Accept", "application/json")
    
    body = None
    if data is not None:
        headers["Content-Type"] = "application/json; charset=utf-8"
        body = json.dumps(data).encode("utf-8")
        
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15, context=ctx) as resp:
            content = resp.read().decode("utf-8")
            return resp.status, json.loads(content)
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8")
        try:
            return e.code, json.loads(err_body)
        except Exception:
            return e.code, {"status": False, "message": err_body}
    except Exception as e:
        return 0, {"status": False, "message": str(e)}

def login(username, password):
    url = f"{API_BASE}/auth/login"
    payload = {
        "username": username,
        "password": password,
        "device_id": "cli-verification-tool",
        "app_version": "2.1.0",
        "timezone": "Asia/Kolkata",
        "browser_name": "PythonCLI",
        "browser_version": "1.0",
        "os_name": "Windows",
        "device_type": "desktop"
    }
    
    status, res = make_request(url, method="POST", data=payload)
    if status == 200 and res.get("status") and "data" in res:
        return res["data"]
    
    msg = res.get("message") or res.get("detail") or f"HTTP {status}"
    raise RuntimeError(f"Login failed: {msg}")

def fetch_monthly_attendance(token):
    url = f"{API_BASE}/student/me/monthly-attendance"
    headers = {"Authorization": f"Bearer {token}"}
    status, res = make_request(url, method="GET", headers=headers)
    if status == 200 and res.get("status"):
        return res.get("data", {})
    raise RuntimeError(f"Failed to fetch monthly attendance: {res.get('message', status)}")

def fetch_weekly_attendance(token):
    url = f"{API_BASE}/student/me/weekly-attendance"
    headers = {"Authorization": f"Bearer {token}"}
    status, res = make_request(url, method="GET", headers=headers)
    if status == 200 and res.get("status"):
        return res.get("data", {})
    return None

def fetch_attendance_logs(token, student_id, limit=10):
    url = f"{API_BASE}/attendance/student?student_id={student_id}&page=1&page_size={limit}&sort_by=attendance_date&sort_dir=desc"
    headers = {"Authorization": f"Bearer {token}"}
    status, res = make_request(url, method="GET", headers=headers)
    if status == 200 and res.get("status"):
        return res.get("data", [])
    return []

def main():
    parser = argparse.ArgumentParser(description="Lloyd ERP Live Data Verification Tool")
    parser.add_argument("-u", "--username", help="Student Admission No / Username", default=os.getenv("LLOYD_USERNAME"))
    parser.add_argument("-p", "--password", help="Student Password", default=os.getenv("LLOYD_PASSWORD"))
    args = parser.parse_args()

    print("=" * 65)
    print("🎓 LLOYD ERP — REAL DATA VERIFICATION CLIENT")
    print("=" * 65)
    print(f"Target Server: {API_BASE}\n")

    username = args.username
    if not username:
        username = input("Enter Admission Number / Username: ").strip()

    password = args.password
    if not password:
        password = getpass.getpass("Enter Password: ").strip()

    if not username or not password:
        print("\n❌ Error: Username and password are required.")
        sys.exit(1)

    print("\n⏳ Authenticating with Lloyd ERP...")
    try:
        login_data = login(username, password)
    except Exception as e:
        print(f"\n❌ {e}")
        sys.exit(1)

    user = login_data.get("user", {})
    student_id = login_data.get("profile_id") or user.get("id")
    student_name = login_data.get("name") or user.get("name")
    token = login_data.get("access_token")

    print("\n✅ Authentication Successful!")
    print("-" * 65)
    print(f"  • Student Name : {student_name}")
    print(f"  • Student ID   : {student_id}")
    print(f"  • Admission No : {user.get('admission_no', username)}")
    print(f"  • Course       : {user.get('course', 'N/A')}")
    print(f"  • Semester     : {user.get('semester', 'N/A')}")
    print(f"  • Section      : {user.get('section', 'N/A')}")
    print(f"  • Token Expiry : {login_data.get('expires_in', 86400)} seconds (24 hours)")
    print("-" * 65)

    # 1. Fetch Monthly Summary
    print("\n📊 Fetching Monthly Attendance Metrics...")
    try:
        monthly_data = fetch_monthly_attendance(token)
        months = monthly_data.get("months", [])
        
        total_p = sum(m.get("present", 0) for m in months)
        total_c = sum(m.get("total", 0) for m in months)
        pct = (total_p / total_c * 100) if total_c > 0 else 0.0

        print(f"\n  Overall Attendance: {pct:.1f}% ({total_p} Present / {total_c} Total)")
        
        # Bunk / Recovery Calculation
        if pct >= 75.0:
            can_bunk = int((100 * total_p - 75 * total_c) // 75)
            print(f"  Status            : ✅ SAFE (≥ 75%)")
            print(f"  Bunk Buffer       : You can safely miss {can_bunk} upcoming classes.")
        else:
            needed = int(((75 * total_c - 100 * total_p) + 24) // 25)
            print(f"  Status            : ⚠️ SHORTAGE (< 75%)")
            print(f"  Recovery Plan     : You must attend the next {needed} consecutive classes.")

        print("\n  Monthly Breakdown:")
        for m in months:
            lbl = m.get("month_label", f"{m.get('month_number')}/{m.get('year')}")
            mp = m.get("present", 0)
            mt = m.get("total", 0)
            mpct = m.get("percentage", 0.0)
            print(f"    - {lbl:12}: {mp:2} / {mt:2} classes attended ({mpct:5.1f}%)")

    except Exception as e:
        print(f"  ❌ Error fetching monthly data: {e}")

    # 2. Fetch Latest 10 Class Logs
    if student_id:
        print("\n📋 Fetching Recent Class Ledger Logs...")
        try:
            logs = fetch_attendance_logs(token, student_id, limit=10)
            if logs:
                print(f"  Latest {len(logs)} recorded lectures:")
                for log in logs:
                    date = log.get("attendance_date", "—")
                    subj = log.get("subject_name", "Unknown Subject")
                    status = log.get("status", "—")
                    faculty = log.get("created_by_name", "Faculty")
                    lecture = log.get("class_lecture", "")
                    icon = "✅" if status.lower() == "present" else "❌"
                    lec_str = f"L#{lecture}" if lecture else ""
                    print(f"    {icon} [{date}] {lec_str:4} {subj[:24]:24} | {status:7} | by {faculty}")
            else:
                print("  No recent lecture logs found.")
        except Exception as e:
            print(f"  ❌ Error fetching logs: {e}")

    # 3. Weekly Timetable Routine
    print("\n📅 Fetching Timetable Routine...")
    try:
        weekly = fetch_weekly_attendance(token)
        if weekly and weekly.get("days"):
            print(f"  Active Week: {weekly.get('week_start')} to {weekly.get('week_end')}")
            for day in weekly["days"][:2]: # Show first 2 days as preview
                day_name = day.get("day", "")
                date_str = day.get("date", "")
                periods = day.get("periods", [])
                print(f"\n    🗓️  {day_name} ({date_str}) — {len(periods)} periods:")
                for p in periods:
                    st = p.get("start_time", "")
                    et = p.get("end_time", "")
                    sub = p.get("subject_name", "")
                    room = p.get("room_no", "")
                    tch = p.get("teacher_name", "")
                    print(f"       • {st}-{et} | {sub} (Room: {room}) — {tch}")
        else:
            print("  Timetable routine not available.")
    except Exception as e:
        print(f"  ❌ Error fetching timetable: {e}")

    print("\n" + "=" * 65)
    print("✅ Live Data Verification Finished.")
    print("=" * 65)

if __name__ == "__main__":
    main()
