package com.lloyd.attendance.feature.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lloyd.attendance.api.Models
import com.lloyd.attendance.core.data.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val days: List<CalendarDayInfo> = emptyList(),
    val selectedDate: LocalDate? = null,
    val selectedDateLogs: List<Models.StudentAttendanceItem> = emptyList(),
    val monthPresentDays: Int = 0,
    val monthAbsentDays: Int = 0,
    val monthMixedDays: Int = 0,
    val isRefreshing: Boolean = false
)

class AttendanceCalendarViewModel(
    application: Application,
    private val repository: AttendanceRepository = AttendanceRepository.getInstance(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var rawRecords: List<Models.StudentAttendanceItem> = emptyList()

    init {
        viewModelScope.launch {
            repository.snapshot.collect { snapshot ->
                rawRecords = snapshot.records
                recalculateCalendar()
            }
        }
    }

    fun previousMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.minusMonths(1)) }
        recalculateCalendar()
    }

    fun nextMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.plusMonths(1)) }
        recalculateCalendar()
    }

    fun selectDate(date: LocalDate) {
        val dateLogs = findLogsForDate(date)
        _uiState.update { it.copy(selectedDate = date, selectedDateLogs = dateLogs) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            repository.refresh(isForeground = true)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun findLogsForDate(date: LocalDate): List<Models.StudentAttendanceItem> {
        val targetDateStr = date.toString() // YYYY-MM-DD
        return rawRecords.filter { item ->
            val d = item.attendanceDate?.trim() ?: ""
            d.startsWith(targetDateStr)
        }
    }

    private fun recalculateCalendar() {
        val ym = _uiState.value.currentMonth
        val today = LocalDate.now()
        val firstDayOfMonth = ym.atDay(1)
        val daysInMonth = ym.lengthOfMonth()
        
        // Day of week for 1st day (1=Mon, 7=Sun)
        val startDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 to 7

        // Map records by date string
        val recordsByDate = mutableMapOf<String, MutableList<Models.StudentAttendanceItem>>()
        rawRecords.forEach { item ->
            val dateStr = item.attendanceDate?.trim()?.take(10)
            if (!dateStr.isNullOrEmpty()) {
                recordsByDate.getOrPut(dateStr) { mutableListOf() }.add(item)
            }
        }

        val calendarDays = mutableListOf<CalendarDayInfo>()

        // Previous month padding days
        val prevMonth = ym.minusMonths(1)
        val daysInPrevMonth = prevMonth.lengthOfMonth()
        val prevPaddingCount = startDayOfWeek - 1
        for (i in (daysInPrevMonth - prevPaddingCount + 1)..daysInPrevMonth) {
            val d = prevMonth.atDay(i)
            calendarDays.add(
                CalendarDayInfo(
                    date = d,
                    dayOfMonth = i,
                    isCurrentMonth = false,
                    isToday = d == today,
                    status = DayAttendanceStatus.NO_LECTURES
                )
            )
        }

        var presentDays = 0
        var absentDays = 0
        var mixedDays = 0

        // Current month days
        for (day in 1..daysInMonth) {
            val d = ym.atDay(day)
            val dStr = d.toString()
            val dayLogs = recordsByDate[dStr] ?: emptyList()

            val status: DayAttendanceStatus
            val presentCount: Int
            val totalCount: Int

            if (dayLogs.isEmpty()) {
                status = DayAttendanceStatus.NO_LECTURES
                presentCount = 0
                totalCount = 0
            } else {
                presentCount = dayLogs.count { it.isPresent }
                totalCount = dayLogs.size
                status = when {
                    presentCount == totalCount -> {
                        presentDays++
                        DayAttendanceStatus.PRESENT
                    }
                    presentCount == 0 -> {
                        absentDays++
                        DayAttendanceStatus.ABSENT
                    }
                    else -> {
                        mixedDays++
                        DayAttendanceStatus.MIXED
                    }
                }
            }

            calendarDays.add(
                CalendarDayInfo(
                    date = d,
                    dayOfMonth = day,
                    isCurrentMonth = true,
                    isToday = d == today,
                    status = status,
                    presentCount = presentCount,
                    totalCount = totalCount
                )
            )
        }

        // Trailing padding to fill complete weeks (multiples of 7)
        val totalCells = calendarDays.size
        val nextMonth = ym.plusMonths(1)
        val nextPaddingCount = (7 - (totalCells % 7)) % 7
        for (i in 1..nextPaddingCount) {
            val d = nextMonth.atDay(i)
            calendarDays.add(
                CalendarDayInfo(
                    date = d,
                    dayOfMonth = i,
                    isCurrentMonth = false,
                    isToday = d == today,
                    status = DayAttendanceStatus.NO_LECTURES
                )
            )
        }

        val selDate = _uiState.value.selectedDate ?: if (ym == YearMonth.now()) today else firstDayOfMonth
        val selLogs = findLogsForDate(selDate)

        _uiState.update { state ->
            state.copy(
                days = calendarDays,
                selectedDate = selDate,
                selectedDateLogs = selLogs,
                monthPresentDays = presentDays,
                monthAbsentDays = absentDays,
                monthMixedDays = mixedDays
            )
        }
    }
}
