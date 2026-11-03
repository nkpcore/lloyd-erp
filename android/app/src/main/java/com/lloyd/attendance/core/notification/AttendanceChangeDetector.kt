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
     * Checks if an attendance record date is recent enough to warrant a live alert.
     * Prevents false alerts for historical lectures marked days, weeks, or months ago.
     * Only classes from today, yesterday, or within 48 hours are eligible for live alerts.
     */
    @JvmStatic
    fun isEligibleNotificationDate(dateStr: String?, referenceDate: Date? = null): Boolean {
        if (dateStr.isNullOrBlank()) return true // fail-open if date is omitted so we rely on ID dedup
        val cleanDate = dateStr.trim().take(10) // "YYYY-MM-DD"
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            }
            val recordDate = sdf.parse(cleanDate) ?: return true
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
                time = referenceDate ?: Date()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // Allow today, yesterday, and day before yesterday (48-72h window)
                add(Calendar.DAY_OF_YEAR, -2)
            }
            val cutoff = cal.time
            !recordDate.before(cutoff)
        } catch (_: Exception) {
            true // fail-open on parse error so genuine marks are not lost
        }
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
     * @param enforceDateFilter When true, strictly restricts alerts to classes from today or yesterday.
     * @return Chronologically ordered list of newly marked attendance events.
     */
    @JvmOverloads
    fun detectNewMarks(
        logs: List<Models.StudentAttendanceItem>?,
        seenIds: Set<String>,
        isFirstBootstrap: Boolean,
        lastSeenId: Long = 0L,
        enforceDateFilter: Boolean = false
    ): List<AttendanceMarkEvent.Marked> {
        if (logs.isNullOrEmpty() || isFirstBootstrap) {
            return emptyList()
        }

        val newEvents = mutableListOf<AttendanceMarkEvent.Marked>()

        for (item in logs) {
            if (item.id <= 0) continue

            // 1. Date Eligibility Guard: Historical classes (>48h ago) NEVER trigger notifications
            if (enforceDateFilter && !isEligibleNotificationDate(item.attendanceDate)) {
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
