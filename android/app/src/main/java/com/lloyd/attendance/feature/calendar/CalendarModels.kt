package com.lloyd.attendance.feature.calendar

import java.time.LocalDate

enum class DayAttendanceStatus {
    PRESENT,      // All lectures attended
    ABSENT,       // All lectures absent
    MIXED,        // Partial attendance
    NO_LECTURES   // Holiday, weekend, or no records
}

data class CalendarDayInfo(
    val date: LocalDate,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val status: DayAttendanceStatus,
    val presentCount: Int = 0,
    val totalCount: Int = 0
)

data class DayLectureItem(
    val id: String,
    val subjectName: String,
    val facultyName: String,
    val timeSlot: String,
    val isPresent: Boolean
)
