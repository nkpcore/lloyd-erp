package com.lloyd.attendance.feature.logs

import com.lloyd.attendance.api.Models
import com.lloyd.attendance.core.domain.AttendanceCalculator
import com.lloyd.attendance.core.domain.SubjectAttendance
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AttendanceLogsSummary(
    val totalMarked: Int,
    val totalPresent: Int,
    val totalAbsent: Int,
    val attendancePercentage: Double
)

object AttendanceLogsProcessor {

    private val fallbackDateFormatters = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.US),
        DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.US),
        DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.US)
    )

    /**
     * Single normalized date string extractor: tries [Models.StudentAttendanceItem.attendanceDate]
     * first and falls back to [Models.StudentAttendanceItem.createdAt], stripping any time component.
     */
    fun extractDateString(item: Models.StudentAttendanceItem): String? {
        val raw = item.attendanceDate?.trim().takeUnless { it.isNullOrEmpty() }
            ?: item.createdAt?.trim().takeUnless { it.isNullOrEmpty() }
            ?: return null
        return raw.substringBefore("T").substringBefore(" ").trim().takeIf { it.isNotEmpty() }
    }

    fun parseDateString(raw: String?): LocalDate? {
        val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val candidate = trimmed.substringBefore("T").substringBefore(" ").trim().takeIf { it.isNotEmpty() }
            ?: return null
        for (formatter in fallbackDateFormatters) {
            try {
                return LocalDate.parse(candidate, formatter)
            } catch (_: Exception) {
            }
        }
        return null
    }

    /**
     * Single normalized [LocalDate] extractor: tries [Models.StudentAttendanceItem.attendanceDate]
     * first and falls back to [Models.StudentAttendanceItem.createdAt].
     */
    fun extractLocalDate(item: Models.StudentAttendanceItem): LocalDate? {
        return parseDateString(item.attendanceDate) ?: parseDateString(item.createdAt)
    }

    @JvmOverloads
    fun filterLogs(
        logs: List<Models.StudentAttendanceItem?>,
        searchQuery: String,
        statusFilter: LogStatusFilter,
        dateFilter: LocalDate? = null
    ): List<Models.StudentAttendanceItem> {
        val nonNullLogs = logs.filterNotNull()
        val query = searchQuery.trim().lowercase()

        return nonNullLogs.filter { item ->
            val matchesFilter = when (statusFilter) {
                LogStatusFilter.ALL -> true
                LogStatusFilter.PRESENT -> item.isPresent
                LogStatusFilter.ABSENT -> !item.isPresent
            }

            val matchesDate = dateFilter == null || extractLocalDate(item) == dateFilter

            val subject = (item.subjectName ?: "").lowercase()
            val faculty = (item.createdByName ?: "").lowercase()
            val date = (extractDateString(item) ?: "").lowercase()

            val matchesQuery = query.isEmpty() ||
                    subject.contains(query) ||
                    faculty.contains(query) ||
                    date.contains(query)

            matchesFilter && matchesDate && matchesQuery
        }
    }

    fun groupByDate(logs: List<Models.StudentAttendanceItem?>): Map<String, List<Models.StudentAttendanceItem>> {
        val nonNull = logs.filterNotNull()
        val grouped = LinkedHashMap<String, MutableList<Models.StudentAttendanceItem>>()
        for (item in nonNull) {
            val dateKey = extractDateString(item) ?: "Unknown Date"
            grouped.getOrPut(dateKey) { mutableListOf() }.add(item)
        }
        return grouped
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
            val teacher = firstItem?.facultyDisplayName ?: firstItem?.createdByName
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
