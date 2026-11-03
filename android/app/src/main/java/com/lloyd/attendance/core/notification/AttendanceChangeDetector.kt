package com.lloyd.attendance.core.notification

import com.lloyd.attendance.api.Models
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

sealed class AttendanceMarkEvent {
    data class Marked(
        val attendanceId: Long,
        val subjectName: String,
        val facultyName: String,
        val lectureDetails: String,
        val isPresent: Boolean,
        val attendanceDate: String
    ) : AttendanceMarkEvent()
}

/**
 * Pure domain logic for detecting genuine teacher attendance marks.
 * Ensures zero spam notifications, handles bootstrap suppression, and supports multi-class batches.
 */
object AttendanceChangeDetector {

    /**
     * Checks if a date string represents today's date in Indian Standard Time (Asia/Kolkata).
     */
    @JvmStatic
    fun isTodayRecord(dateStr: String?, referenceDate: Date? = null): Boolean {
        if (dateStr.isNullOrBlank()) return false
        val cleanDate = dateStr.trim().take(10) // "YYYY-MM-DD"
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            }
            val todayStr = sdf.format(referenceDate ?: Date())
            cleanDate == todayStr
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if an attendance record date is from TODAY.
     * Restricts alerts strictly to classes marked on the same date/time.
     * Prevents false alerts for past historical lectures marked days, weeks, or months ago.
     */
    @JvmStatic
    @JvmOverloads
    fun isEligibleNotificationDate(dateStr: String?, referenceDate: Date? = null): Boolean {
        if (dateStr.isNullOrBlank()) return true // fail-open if date is omitted so we rely on ID dedup
        return isTodayRecord(dateStr, referenceDate)
    }

    /**
     * Checks if an item was recorded or held today (either class date or submission timestamp is today).
     */
    @JvmStatic
    @JvmOverloads
    fun isEligibleRecord(item: Models.StudentAttendanceItem, referenceDate: Date? = null): Boolean {
        if (item.attendanceDate.isNullOrBlank() && item.createdAt.isNullOrBlank()) return true
        val dateToday = isTodayRecord(item.attendanceDate, referenceDate)
        val createdToday = isTodayRecord(item.createdAt, referenceDate)
        return dateToday || createdToday
    }

    /**
     * Generates a stable composite signature for an attendance record to guard against duplicate alerts.
     */
    @JvmStatic
    fun getRecordSignature(item: Models.StudentAttendanceItem): String {
        val date = item.attendanceDate?.trim().orEmpty()
        val lecture = item.classLecture?.trim().orEmpty()
        val status = item.status?.trim().orEmpty()
        return "${item.id}_${date}_${lecture}_${status}"
    }

    /**
     * Evaluates incoming attendance logs against known IDs.
     *
     * @param logs The raw list of items from ERP `/attendance/student`.
     * @param seenIds Set of string IDs already seen in prior sync cycles.
     * @param isFirstBootstrap True if the app is performing initial sync after sign in.
     * @param lastSeenId Highest attendance record ID seen in previous sync cycles (safeguard against historical floods).
     * @param enforceDateFilter When true, strictly restricts alerts to classes marked on the same date (today).
     * @param referenceDate Optional reference date for testing.
     * @return Chronologically ordered list of newly marked attendance events.
     */
    @JvmOverloads
    fun detectNewMarks(
        logs: List<Models.StudentAttendanceItem>?,
        seenIds: Set<String>,
        isFirstBootstrap: Boolean,
        lastSeenId: Long = 0L,
        enforceDateFilter: Boolean = false,
        referenceDate: Date? = null
    ): List<AttendanceMarkEvent.Marked> {
        if (logs.isNullOrEmpty() || isFirstBootstrap) {
            return emptyList()
        }

        val newEvents = mutableListOf<AttendanceMarkEvent.Marked>()

        for (item in logs) {
            if (item.id <= 0) continue

            // 1. Same-Day Eligibility Guard: ONLY classes marked on the same date/time (today) trigger notifications
            if (enforceDateFilter && !isEligibleRecord(item, referenceDate)) {
                continue
            }

            // 2. Monotonic ID Guard: Records with ID <= lastSeenId are strictly historical
            if (lastSeenId > 0L && item.id <= lastSeenId) {
                continue
            }

            // 3. Composite Signature & ID Dedup Guard
            val idStr = item.id.toString()
            val signature = getRecordSignature(item)
            if (seenIds.contains(idStr) || seenIds.contains(signature)) {
                continue
            }

            val status = item.status?.trim()
            val isPresent = "Present".equals(status, ignoreCase = true)
            val isAbsent = "Absent".equals(status, ignoreCase = true)

            // 4. Genuine Status Verification: ONLY notify if faculty explicitly marked Present or Absent
            if (isPresent || isAbsent) {
                val lecture = item.classLecture
                val lectureStr = if (!lecture.isNullOrBlank()) {
                    "Lecture #${lecture.trim()}"
                } else {
                    ""
                }

                newEvents.add(
                    AttendanceMarkEvent.Marked(
                        attendanceId = item.id,
                        subjectName = item.subjectDisplayName,
                        facultyName = item.facultyDisplayName,
                        lectureDetails = lectureStr,
                        isPresent = isPresent,
                        attendanceDate = item.attendanceDate ?: ""
                    )
                )
            }
        }

        // Return sorted chronologically by attendance record ID
        return newEvents.sortedBy { it.attendanceId }
    }
}
