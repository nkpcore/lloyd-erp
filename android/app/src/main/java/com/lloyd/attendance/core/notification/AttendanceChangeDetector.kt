package com.lloyd.attendance.core.notification

import com.lloyd.attendance.api.Models

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
     * Evaluates incoming attendance logs against known IDs.
     *
     * @param logs The raw list of items from ERP `/attendance/student`.
     * @param seenIds Set of string IDs already seen in prior sync cycles.
     * @param isFirstBootstrap True if the app is performing initial sync after sign in.
     * @return Chronologically ordered list of newly marked attendance events.
     */
    fun detectNewMarks(
        logs: List<Models.StudentAttendanceItem>?,
        seenIds: Set<String>,
        isFirstBootstrap: Boolean
    ): List<AttendanceMarkEvent.Marked> {
        if (logs.isNullOrEmpty() || isFirstBootstrap) {
            return emptyList()
        }

        val newEvents = mutableListOf<AttendanceMarkEvent.Marked>()

        for (item in logs) {
            if (item.id <= 0) continue
            val idStr = item.id.toString()
            if (!seenIds.contains(idStr)) {
                val status = item.status?.trim()
                val isPresent = "Present".equals(status, ignoreCase = true)
                val isAbsent = "Absent".equals(status, ignoreCase = true)

                // ONLY notify if faculty explicitly marked Present or Absent
                if (isPresent || isAbsent) {
                    val lectureStr = if (!item.classLecture.isNullOrBlank()) {
                        "Lecture #${item.classLecture.trim()}"
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
        }

        // Return sorted chronologically by attendance record ID
        return newEvents.sortedBy { it.attendanceId }
    }
}
