package com.lloyd.attendance.core.schedule

import java.util.Calendar

/**
 * Period model representing an individual scheduled class or break.
 */
data class SchedulePeriod(
    val id: String,
    val subjectName: String,
    val subjectCode: String? = null,
    val startTime: String, // e.g. "09:30 AM" or "09:30"
    val endTime: String,   // e.g. "10:30 AM" or "10:30"
    val facultyName: String? = null,
    val roomNumber: String? = null,
    val isBreak: Boolean = false,
    val isOngoing: Boolean = false,
    val isCompleted: Boolean = false,
    val remainingMinutes: Int = 0
)

/**
 * Result representing daily schedule state, avoiding synthetic Section A-1 fallbacks.
 */
sealed interface DayScheduleResult {
    data class Success(
        val dayName: String,
        val dateString: String? = null,
        val section: String? = null,
        val periods: List<SchedulePeriod> = emptyList(),
        val activePeriod: SchedulePeriod? = null,
        val nextPeriod: SchedulePeriod? = null
    ) : DayScheduleResult

    data object Weekend : DayScheduleResult

    data class Unavailable(val reason: String) : DayScheduleResult
}
