package com.lloyd.attendance.core.export

import com.lloyd.attendance.core.domain.AttendanceCalculator
import com.lloyd.attendance.core.domain.SubjectAttendance
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceReportExporterTest {

    @Test
    fun testGenerateTextReport_containsHeaderStudentAndSubjects() {
        val subjects = listOf(
            SubjectAttendance(
                subjectCode = "CS101",
                subjectName = "Programming in C",
                presentCount = 18,
                totalClasses = 20,
                teacherName = "Prof. Roy",
                thresholds = AttendanceCalculator.calculateAllThresholds(18, 20)
            ),
            SubjectAttendance(
                subjectCode = "MA101",
                subjectName = "Engineering Mathematics",
                presentCount = 14,
                totalClasses = 20,
                teacherName = "Dr. Gupta",
                thresholds = AttendanceCalculator.calculateAllThresholds(14, 20)
            )
        )

        val report = AttendanceReportExporter.generateTextReport(
            studentName = "Nikhil Pandey",
            section = "CSE-1",
            overallPercentage = "80.0%",
            totalPresent = 32,
            totalClasses = 40,
            subjects = subjects
        )

        assertTrue(report.contains("Nikhil Pandey"))
        assertTrue(report.contains("CSE-1"))
        assertTrue(report.contains("80.0%"))
        assertTrue(report.contains("Programming in C"))
        assertTrue(report.contains("Engineering Mathematics"))
        assertTrue(report.contains("Official Student Attendance Summary"))
    }

    @Test
    fun testGenerateCsvReport_containsCsvHeaderAndRows() {
        val subjects = listOf(
            SubjectAttendance(
                subjectCode = "CS101",
                subjectName = "Programming in C",
                presentCount = 18,
                totalClasses = 20,
                teacherName = "Prof. Roy",
                thresholds = AttendanceCalculator.calculateAllThresholds(18, 20)
            )
        )

        val csv = AttendanceReportExporter.generateCsvReport(subjects)
        assertTrue(csv.contains("Subject Code,Subject Name,Faculty,Attended,Total Classes,Percentage"))
        assertTrue(csv.contains("CS101,\"Programming in C\",\"Prof. Roy\",18,20,90.0%"))
    }
}
