package com.lloyd.attendance.feature.logs

import com.lloyd.attendance.api.Models
import com.lloyd.attendance.core.domain.AttendanceCalculator
import com.lloyd.attendance.core.domain.SubjectAttendance

data class AttendanceLogsSummary(
    val totalMarked: Int,
    val totalPresent: Int,
    val totalAbsent: Int,
    val attendancePercentage: Double
)

object AttendanceLogsProcessor {

    fun filterLogs(
        logs: List<Models.StudentAttendanceItem?>,
        searchQuery: String,
        statusFilter: LogStatusFilter
    ): List<Models.StudentAttendanceItem> {
        val nonNullLogs = logs.filterNotNull()
        val query = searchQuery.trim().lowercase()

        return nonNullLogs.filter { item ->
            val matchesFilter = when (statusFilter) {
                LogStatusFilter.ALL -> true
                LogStatusFilter.PRESENT -> item.isPresent
                LogStatusFilter.ABSENT -> !item.isPresent
            }

            val subject = (item.subjectName ?: "").lowercase()
            val faculty = (item.createdByName ?: "").lowercase()
            val date = (item.attendanceDate ?: "").lowercase()

            val matchesQuery = query.isEmpty() ||
                    subject.contains(query) ||
                    faculty.contains(query) ||
                    date.contains(query)

            matchesFilter && matchesQuery
        }
    }

    fun groupByDate(logs: List<Models.StudentAttendanceItem?>): Map<String, List<Models.StudentAttendanceItem>> {
        return logs.filterNotNull().groupBy { it.attendanceDate ?: "Unknown Date" }
    }

    fun computeSummary(logs: List<Models.StudentAttendanceItem?>): AttendanceLogsSummary {
        val nonNull = logs.filterNotNull()
        val total = nonNull.size
        val present = nonNull.count { it.isPresent }
        val absent = total - present
        val percentage = if (total > 0) (present * 100.0) / total else 0.0
        return AttendanceLogsSummary(
            totalMarked = total,
            totalPresent = present,
            totalAbsent = absent,
            attendancePercentage = percentage
        )
    }

    fun reconcileSubjectAttendance(logs: List<Models.StudentAttendanceItem?>): List<SubjectAttendance> {
        val nonNull = logs.filterNotNull()
        if (nonNull.isEmpty()) return emptyList()

        val grouped = nonNull.groupBy { it.subjectName ?: "General Subject" }
        return grouped.map { (name, items) ->
            val present = items.count { it.status.equals("present", ignoreCase = true) }
            val total = items.size
            val firstItem = items.firstOrNull()
            val subCode = firstItem?.subjectId?.toString().orEmpty()
            val teacher = firstItem?.getFacultyDisplayName() ?: firstItem?.createdByName
            SubjectAttendance(
                subjectCode = subCode,
                subjectName = name,
                presentCount = present,
                totalClasses = total,
                teacherName = teacher,
                thresholds = AttendanceCalculator.calculateAllThresholds(present, total)
            )
        }.sortedBy { it.percentage.numericValue ?: 100.0 }
    }
}
