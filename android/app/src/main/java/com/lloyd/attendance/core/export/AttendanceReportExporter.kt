package com.lloyd.attendance.core.export

import android.content.Context
import android.content.Intent
import com.lloyd.attendance.core.domain.BunkAdvisor
import com.lloyd.attendance.core.domain.SubjectAttendance
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AttendanceReportExporter {

    fun generateTextReport(
        studentName: String,
        section: String,
        overallPercentage: String,
        totalPresent: Int,
        totalClasses: Int,
        subjects: List<SubjectAttendance>
    ): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val generatedAt = dateFormat.format(Date())

        return buildString {
            appendLine("════════════════════════════════════════════════════")
            appendLine("              LLOYD ATTENDANCE REPORT               ")
            appendLine("════════════════════════════════════════════════════")
            if (studentName.isNotBlank()) {
                appendLine("Student: $studentName")
            }
            if (section.isNotBlank()) {
                appendLine("Section: $section")
            }
            appendLine("Overall Attendance: $overallPercentage ($totalPresent / $totalClasses classes attended)")
            appendLine("Generated: $generatedAt")
            appendLine("────────────────────────────────────────────────────")
            appendLine("COURSE BREAKDOWN:")
            appendLine("────────────────────────────────────────────────────")

            if (subjects.isEmpty()) {
                appendLine("No subject records available.")
            } else {
                subjects.forEach { subject ->
                    val advice = BunkAdvisor.getAdvice(subject.presentCount, subject.totalClasses)
                    appendLine("• ${subject.subjectName} (${subject.percentage.displayValue})")
                    appendLine("  Attended: ${subject.presentCount} / ${subject.totalClasses} classes")
                    if (!subject.teacherName.isNullOrBlank()) {
                        appendLine("  Faculty: ${subject.teacherName}")
                    }
                    appendLine("  Status: ${advice.headline}")
                    appendLine()
                }
            }

            appendLine("════════════════════════════════════════════════════")
            appendLine("Official Student Attendance Summary")
            appendLine("════════════════════════════════════════════════════")
        }
    }

    fun generateCsvReport(subjects: List<SubjectAttendance>): String {
        return buildString {
            appendLine("Subject Code,Subject Name,Faculty,Attended,Total Classes,Percentage")
            subjects.forEach { subject ->
                val code = subject.subjectCode
                val name = "\"${subject.subjectName.replace("\"", "\"\"")}\""
                val faculty = "\"${(subject.teacherName ?: "N/A").replace("\"", "\"\"")}\""
                val pct = subject.percentage.displayValue
                appendLine("$code,$name,$faculty,${subject.presentCount},${subject.totalClasses},$pct")
            }
        }
    }

    fun shareReport(context: Context, reportText: String, title: String = "Share Attendance Report") {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, reportText)
            type = "text/plain"
        }
        val chooser = Intent.createChooser(sendIntent, title)
        context.startActivity(chooser)
    }
}
