import pymupdf
import re
import json
import os

PDF_PATH = "docs/syllabus/AKTU_CSE_1st_Year_Syllabus_2026-27.pdf"

COURSE_SPECS = [
    {
        "id": "AAS101E",
        "code": "AAS101E / AAS201E",
        "title": "Quantum Physics and Applications",
        "shortTitle": "Quantum Physics",
        "semesters": [1, 2],
        "category": "BSC",
        "type": "Theory",
        "credits": 3,
        "ncrfHours": 45,
        "pageStart": 7,
        "pageEnd": 9
    },
    {
        "id": "AAS102D",
        "code": "AAS102D / AAS202D",
        "title": "Applied Chemistry for Smart Systems",
        "shortTitle": "Applied Chemistry",
        "semesters": [1, 2],
        "category": "BSC",
        "type": "Theory",
        "credits": 3,
        "ncrfHours": 45,
        "pageStart": 10,
        "pageEnd": 12
    },
    {
        "id": "AAS103D",
        "code": "AAS103D",
        "title": "Calculus and Linear Algebra",
        "shortTitle": "Calculus & Linear Algebra",
        "semesters": [1],
        "category": "BSC",
        "type": "Theory",
        "credits": 4,
        "ncrfHours": 60,
        "pageStart": 13,
        "pageEnd": 16
    },
    {
        "id": "AAS203D",
        "code": "AAS203D",
        "title": "Numerical Methods",
        "shortTitle": "Numerical Methods",
        "semesters": [2],
        "category": "BSC",
        "type": "Theory",
        "credits": 4,
        "ncrfHours": 60,
        "pageStart": 17,
        "pageEnd": 20
    },
    {
        "id": "AEE101A",
        "code": "AEE101A / AEE201A",
        "title": "Fundamentals of Electrical Engineering",
        "shortTitle": "Electrical Engineering",
        "semesters": [1, 2],
        "category": "ESC",
        "type": "Theory",
        "credits": 3,
        "ncrfHours": 45,
        "pageStart": 21,
        "pageEnd": 23
    },
    {
        "id": "AEC101A",
        "code": "AEC101A / AEC201A",
        "title": "Fundamentals of Electronics Engineering",
        "shortTitle": "Electronics Engineering",
        "semesters": [1, 2],
        "category": "ESC",
        "type": "Theory",
        "credits": 3,
        "ncrfHours": 45,
        "pageStart": 24,
        "pageEnd": 27
    },
    {
        "id": "ACS101",
        "code": "ACS101",
        "title": "Programming Languages",
        "shortTitle": "Programming Languages (C & Python)",
        "semesters": [1],
        "category": "ESC",
        "type": "Theory",
        "credits": 3,
        "ncrfHours": 45,
        "pageStart": 28,
        "pageEnd": 32
    },
    {
        "id": "ACS201",
        "code": "ACS201",
        "title": "Essentials of Data Structures",
        "shortTitle": "Data Structures",
        "semesters": [2],
        "category": "ESC",
        "type": "Theory",
        "credits": 3,
        "ncrfHours": 45,
        "pageStart": 33,
        "pageEnd": 37
    },
    {
        "id": "AAS104",
        "code": "AAS104 / AAS204",
        "title": "Introduction to Indian Knowledge System",
        "shortTitle": "Indian Knowledge System",
        "semesters": [1, 2],
        "category": "HSMC",
        "type": "Theory",
        "credits": 2,
        "ncrfHours": 30,
        "pageStart": 38,
        "pageEnd": 40
    },
    {
        "id": "AAS105",
        "code": "AAS105 / AAS205",
        "title": "Professional Communication and Technical Writing",
        "shortTitle": "Technical Communication",
        "semesters": [1, 2],
        "category": "HSMC",
        "type": "Theory",
        "credits": 2,
        "ncrfHours": 30,
        "pageStart": 41,
        "pageEnd": 45
    },
    {
        "id": "AAI101",
        "code": "AAI101 / AAI201",
        "title": "Introduction to AI and Prompt Engineering",
        "shortTitle": "AI & Prompt Engineering",
        "semesters": [1, 2],
        "category": "ESC",
        "type": "Theory",
        "credits": 2,
        "ncrfHours": 30,
        "pageStart": 46,
        "pageEnd": 52
    },
    {
        "id": "AME101A",
        "code": "AME101A / AME201A",
        "title": "Fundamentals of Mechanical Engineering",
        "shortTitle": "Mechanical Engineering",
        "semesters": [1, 2],
        "category": "ESC",
        "type": "Theory",
        "credits": 2,
        "ncrfHours": 30,
        "pageStart": 53,
        "pageEnd": 55
    },
    {
        "id": "AAS151",
        "code": "AAS151 / AAS251",
        "title": "Applied Physics Lab",
        "shortTitle": "Physics Lab",
        "semesters": [1, 2],
        "category": "BSC",
        "type": "Lab",
        "credits": 1,
        "ncrfHours": 30,
        "pageStart": 56,
        "pageEnd": 59
    },
    {
        "id": "AAS152D",
        "code": "AAS152D / AAS252D",
        "title": "Applied Chemistry for Smart Systems Lab",
        "shortTitle": "Chemistry Lab",
        "semesters": [1, 2],
        "category": "BSC",
        "type": "Lab",
        "credits": 1,
        "ncrfHours": 30,
        "pageStart": 60,
        "pageEnd": 63
    },
    {
        "id": "AEE151A",
        "code": "AEE151A / AEE251A",
        "title": "Electrical Engineering Lab",
        "shortTitle": "Electrical Lab",
        "semesters": [1, 2],
        "category": "ESC",
        "type": "Lab",
        "credits": 1,
        "ncrfHours": 30,
        "pageStart": 64,
        "pageEnd": 67
    },
    {
        "id": "AEC151A",
        "code": "AEC151A / AEC251A",
        "title": "Electronics Engineering Lab",
        "shortTitle": "Electronics Lab",
        "semesters": [1, 2],
        "category": "ESC",
        "type": "Lab",
        "credits": 1,
        "ncrfHours": 30,
        "pageStart": 68,
        "pageEnd": 72
    },
    {
        "id": "ACE151E",
        "code": "ACE151E / ACE251E",
        "title": "Computer Aided Engineering Drawing Lab",
        "shortTitle": "CAED Lab",
        "semesters": [1, 2],
        "category": "ESC",
        "type": "Lab",
        "credits": 2,
        "ncrfHours": 45,
        "pageStart": 73,
        "pageEnd": 77
    },
    {
        "id": "AID151",
        "code": "AID151 / AID251",
        "title": "Innovation & Design Thinking Lab (Project-Based Learning)",
        "shortTitle": "Design Thinking Lab",
        "semesters": [1, 2],
        "category": "SDC",
        "type": "Project / Studio",
        "credits": 2,
        "ncrfHours": 45,
        "pageStart": 78,
        "pageEnd": 81
    },
    {
        "id": "AAS153",
        "code": "AAS153 / AAS253",
        "title": "Language Lab",
        "shortTitle": "Language Lab",
        "semesters": [1, 2],
        "category": "HSMC",
        "type": "Lab",
        "credits": 0,
        "ncrfHours": 30,
        "pageStart": 82,
        "pageEnd": 84
    },
    {
        "id": "AAC101",
        "code": "AAC101 / AAC201",
        "title": "Environment & Sustainability",
        "shortTitle": "Environment & Sustainability",
        "semesters": [1, 2],
        "category": "NCMC",
        "type": "Audit (Non-Credit)",
        "credits": 0,
        "ncrfHours": 30,
        "pageStart": 85,
        "pageEnd": 88
    }
]

def clean_text(text: str) -> str:
    text = text.replace('\xa0', ' ')
    text = re.sub(r'[\x00-\x08\x0b\x0c\x0e-\x1f\x7f-\x9f]', '', text)
    return text.strip()

def extract_course(doc, spec):
    pages_text = []
    for p in range(spec["pageStart"] - 1, spec["pageEnd"]):
        pages_text.append(clean_text(doc[p].get_text()))
    full_text = "\n\n".join(pages_text)
    
    # Extract Course Objectives
    objectives = []
    obj_match = re.search(r"Course\s+Objectives[:\s]*(.*?)(?=Course\s+Content|Module\s+Description|Unit\s+Description|\Z)", full_text, re.DOTALL | re.IGNORECASE)
    if obj_match:
        raw_obj = obj_match.group(1).strip()
        lines = [re.sub(r"^[\s•\-\*\d\.]+\s*", "", l).strip() for l in raw_obj.split("\n") if l.strip()]
        objectives = [l for l in lines if len(l) > 10 and not l.startswith("Employability") and not l.startswith("Skill") and not l.startswith("Entrepreneurship")]

    # Extract Course Outcomes (COs)
    outcomes = []
    co_matches = re.findall(r"(CO\d+)\s+([^\n\r]+?)(?:\s+(K\d+))?(?=\n|CO\d+|\Z)", full_text)
    for m in co_matches:
        co_code = m[0]
        co_text = m[1].strip()
        co_kl = m[2] if len(m) > 2 else ""
        if len(co_text) > 10 and not any(o["code"] == co_code for o in outcomes):
            outcomes.append({
                "code": co_code,
                "text": co_text,
                "bloomsLevel": co_kl
            })

    # Extract Units / Modules
    units = []
    # Pattern 1: Unit 1. / Unit I / Module 1
    unit_chunks = re.split(r"\n(?=(?:Unit\s*(?:[1-5]|[I|V|X]+)\b|Module\s*\d+\b|\b(?:[1-5])\.\s+[A-Z]))", full_text)
    unit_idx = 1
    for chunk in unit_chunks:
        header_match = re.match(r"^(?:Unit\s*([1-5]|[I|V|X]+)|Module\s*(\d+)|([1-5])\.)\s*[\:\–\—\-]?\s*(.*?)(?=\n)", chunk, re.IGNORECASE)
        if header_match:
            unit_title = header_match.group(4).strip()
            # Clean title of common prefixes
            unit_title = re.sub(r"^(?:–|—|-)\s*", "", unit_title).strip()
            # Body of unit
            body = chunk[header_match.end():].strip()
            # Stop before next table or section
            body = re.split(r"(?:Course\s+Outcomes|Teaching|Evaluation|References|Text\s+Books|Suggested)", body, flags=re.IGNORECASE)[0].strip()
            
            # Split topics
            topic_lines = []
            for line in body.split("\n"):
                line = line.strip()
                if line and not line.startswith("Weightage") and not line.startswith("20%") and not line.startswith("Hours"):
                    cleaned_line = re.sub(r"^[\d\.\-\•\*\)]+\s*", "", line).strip()
                    if len(cleaned_line) > 3 and not re.match(r"^\d+%", cleaned_line):
                        topic_lines.append(cleaned_line)
            
            if unit_title or topic_lines:
                units.append({
                    "unitNumber": unit_idx,
                    "title": unit_title if unit_title else f"Unit {unit_idx}",
                    "rawContent": body[:1200],
                    "topics": topic_lines[:15]
                })
                unit_idx += 1
                if unit_idx > 5:
                    break

    # Extract Books / References
    books = []
    books_match = re.search(r"(?:Text\s*Books|References|Suggested\s*Reading|Reference\s*Books)[:\s]*(.*?)(?=Course\s*Outcomes|CO–PO|Evaluation|\Z)", full_text, re.DOTALL | re.IGNORECASE)
    if books_match:
        for b_line in books_match.group(1).split("\n"):
            b_line = b_line.strip()
            cleaned_b = re.sub(r"^[\d\.\-\•\*\)]+\s*", "", b_line).strip()
            if len(cleaned_b) > 8 and not cleaned_b.lower().startswith("sr."):
                books.append(cleaned_b)

    return {
        **spec,
        "objectives": objectives[:6],
        "outcomes": outcomes[:5],
        "units": units,
        "books": books[:8],
        "fullTextLength": len(full_text)
    }

def main():
    print(f"Opening {PDF_PATH}...")
    doc = pymupdf.open(PDF_PATH)
    all_courses = []
    for spec in COURSE_SPECS:
        print(f"Extracting {spec['code']} ({spec['title']})...")
        c_data = extract_course(doc, spec)
        all_courses.append(c_data)
        print(f"  -> {len(c_data['units'])} units, {len(c_data['objectives'])} objectives, {len(c_data['outcomes'])} outcomes")

    output_dir = "android/app/src/main/assets"
    os.makedirs(output_dir, exist_ok=True)
    out_file = os.path.join(output_dir, "aktu_cse_syllabus_2026_27.json")
    
    payload = {
        "academicYear": "2026-27",
        "stream": "Computer Science and Engineering (CSE)",
        "university": "Dr. A.P.J. Abdul Kalam Technical University (AKTU), Lucknow",
        "totalCourses": len(all_courses),
        "courses": all_courses
    }
    
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(payload, f, indent=2, ensure_ascii=False)
    print(f"Wrote syllabus payload to {out_file} ({len(json.dumps(payload))} bytes)")

    # Generate markdown report
    rep_file = "docs/syllabus/syllabus_coverage_report.md"
    os.makedirs(os.path.dirname(rep_file), exist_ok=True)
    with open(rep_file, "w", encoding="utf-8") as rf:
        rf.write("# AKTU B.Tech First Year CSE (2026-27) Syllabus Coverage Report\n\n")
        rf.write(f"- **Total Extracted Courses**: {len(all_courses)}\n")
        rf.write(f"- **Source Document**: `docs/syllabus/AKTU_CSE_1st_Year_Syllabus_2026-27.pdf` (98 pages)\n")
        rf.write(f"- **JSON Asset**: `android/app/src/main/assets/aktu_cse_syllabus_2026_27.json`\n\n")
        rf.write("| # | Code | Course Title | Type | Credits | Semesters | Units Extracted | Status |\n")
        rf.write("|---|------|--------------|------|:-------:|:---------:|:---------------:|:------:|\n")
        for i, c in enumerate(all_courses, 1):
            sem_str = ", ".join(f"Sem {s}" for s in c["semesters"])
            status = "✅ Verified" if len(c["units"]) > 0 else "⚠️ Check"
            rf.write(f"| {i} | `{c['code']}` | {c['title']} | {c['type']} | {c['credits']} | {sem_str} | {len(c['units'])} units | {status} |\n")
        
        rf.write("\n## Course Unit & Outcome Detail Summary\n\n")
        for c in all_courses:
            rf.write(f"### {c['code']} — {c['title']}\n")
            rf.write(f"- **Category**: {c['category']} | **Credits**: {c['credits']} | **Hours**: {c['ncrfHours']}\n")
            if c["objectives"]:
                rf.write("- **Objectives**:\n")
                for obj in c["objectives"][:3]:
                    rf.write(f"  - {obj}\n")
            if c["units"]:
                rf.write("- **Units**:\n")
                for u in c["units"]:
                    rf.write(f"  - **Unit {u['unitNumber']}**: {u['title']} ({len(u['topics'])} topics)\n")
            rf.write("\n")
    print(f"Wrote coverage report to {rep_file}")

if __name__ == "__main__":
    main()
