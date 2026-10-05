package com.lloyd.attendance.feature.logs

import com.lloyd.attendance.api.Models

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
}
