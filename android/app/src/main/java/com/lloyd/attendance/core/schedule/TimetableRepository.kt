package com.lloyd.attendance.core.schedule

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lloyd.attendance.api.Models
import com.lloyd.attendance.data.AppPreferences
import java.util.Calendar
import java.util.Locale

/**
 * Modern, section-aware timetable repository.
 * Parses authentic weekly timetable routines from ERP, avoiding hardcoded Section A-1 fallbacks.
 */
class TimetableRepository(
    private val context: Context,
    private val prefs: AppPreferences = AppPreferences(context),
    private val gson: Gson = Gson()
) {

    /**
     * Resolves the schedule for a given calendar day (e.g. Calendar.MONDAY = 2, Calendar.SUNDAY = 1).
     * If calendarDay is not specified, uses current day of week.
     */
    fun getScheduleForDay(
        calendarDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK),
        currentMinutes: Int = getCurrentMinutesFromMidnight()
    ): DayScheduleResult {
        if (calendarDay == Calendar.SUNDAY) {
            return DayScheduleResult.Weekend
        }

        val dayName = getDayNameFromCalendar(calendarDay)
        val weeklyJson = prefs.getWeeklyData()
        if (weeklyJson.isNullOrBlank()) {
            return DayScheduleResult.Unavailable("Weekly timetable has not been synced yet. Pull down to refresh.")
        }

        val weeklyData = try {
            val type = object : TypeToken<Models.ApiResponse<Models.WeeklyAttendanceData>>() {}.type
            val apiRes: Models.ApiResponse<Models.WeeklyAttendanceData>? = gson.fromJson(weeklyJson, type)
            apiRes?.data
        } catch (e: Exception) {
            null
        }

        if (weeklyData?.days.isNullOrEmpty()) {
            return DayScheduleResult.Unavailable("No routine published for this week.")
        }

        // Find matching DayItem
        val matchedDay = weeklyData!!.days.firstOrNull { dayItem ->
            dayItem.day.equals(dayName, ignoreCase = true) ||
            dayItem.day.startsWith(dayName.take(3), ignoreCase = true)
        }

        if (matchedDay == null || matchedDay.periods.isNullOrEmpty()) {
            return DayScheduleResult.Unavailable("No classes scheduled for $dayName.")
        }

        val studentSection = prefs.getSelectedSection().ifBlank {
            prefs.userProfile?.section?.trim().orEmpty()
        }

        val mappedPeriods = matchedDay.periods.mapIndexed { index, p ->
            val startMin = parseTimeToMinutes(p.startTime)
            val endMin = parseTimeToMinutes(p.endTime)
            val isOngoing = currentMinutes in startMin..endMin
            val isCompleted = currentMinutes > endMin
            val remainingMin = if (isOngoing) (endMin - currentMinutes).coerceAtLeast(0) else 0

            SchedulePeriod(
                id = "${matchedDay.day}_${p.routineId ?: index}",
                subjectName = p.subjectName ?: "Unspecified Class",
                subjectCode = p.subjectId?.toString(),
                startTime = formatDisplayTime(p.startTime),
                endTime = formatDisplayTime(p.endTime),
                facultyName = p.teacherName?.takeIf { it.isNotBlank() },
                roomNumber = p.roomNo?.takeIf { it.isNotBlank() },
                isBreak = (p.subjectName?.contains("BREAK", ignoreCase = true) == true) ||
                          (p.subjectName?.contains("LUNCH", ignoreCase = true) == true),
                isOngoing = isOngoing,
                isCompleted = isCompleted,
                remainingMinutes = remainingMin
            )
        }

        val active = mappedPeriods.firstOrNull { it.isOngoing && !it.isBreak }
        val next = mappedPeriods.firstOrNull {
            val startMin = parseTimeToMinutes(it.startTime)
            startMin > currentMinutes && !it.isBreak
        }

        return DayScheduleResult.Success(
            dayName = matchedDay.day,
            dateString = matchedDay.date,
            section = studentSection.ifBlank { null },
            periods = mappedPeriods,
            activePeriod = active,
            nextPeriod = next
        )
    }

    companion object {
        fun getCurrentMinutesFromMidnight(): Int {
            val now = Calendar.getInstance()
            return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        }

        fun getDayNameFromCalendar(dayOfWeek: Int): String {
            return when (dayOfWeek) {
                Calendar.MONDAY -> "Monday"
                Calendar.TUESDAY -> "Tuesday"
                Calendar.WEDNESDAY -> "Wednesday"
                Calendar.THURSDAY -> "Thursday"
                Calendar.FRIDAY -> "Friday"
                Calendar.SATURDAY -> "Saturday"
                Calendar.SUNDAY -> "Sunday"
                else -> "Monday"
            }
        }

        /**
         * Parses "09:30:00", "09:30", "9:30 AM", "02:15 PM" into minutes from midnight.
         */
        fun parseTimeToMinutes(timeStr: String?): Int {
            if (timeStr.isNullOrBlank()) return 0
            val clean = timeStr.trim().uppercase(Locale.US)

            var isPm = clean.endsWith("PM")
            var isAm = clean.endsWith("AM")
            val withoutAmPm = clean.replace("AM", "").replace("PM", "").trim()

            val parts = withoutAmPm.split(":")
            if (parts.isEmpty()) return 0

            var hours = parts[0].toIntOrNull() ?: 0
            val minutes = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0

            if (isPm && hours < 12) hours += 12
            if (isAm && hours == 12) hours = 0

            return (hours * 60 + minutes).coerceIn(0, 1439)
        }

        /**
         * Formats 24h or raw string into user-friendly "09:30 AM" display string.
         */
        fun formatDisplayTime(timeStr: String?): String {
            if (timeStr.isNullOrBlank()) return "--:--"
            val minutes = parseTimeToMinutes(timeStr)
            val h24 = minutes / 60
            val m = minutes % 60
            val amPm = if (h24 >= 12) "PM" else "AM"
            val h12 = when {
                h24 == 0 -> 12
                h24 > 12 -> h24 - 12
                else -> h24
            }
            return String.format(Locale.US, "%02d:%02d %s", h12, m, amPm)
        }
    }
}
