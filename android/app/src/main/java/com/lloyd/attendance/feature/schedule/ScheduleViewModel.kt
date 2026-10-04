package com.lloyd.attendance.feature.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lloyd.attendance.api.ErpApiClient
import com.lloyd.attendance.core.schedule.DayScheduleResult
import com.lloyd.attendance.core.schedule.TimetableRepository
import com.lloyd.attendance.data.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class ScheduleUiState(
    val selectedCalendarDay: Int = getInitialDay(),
    val scheduleResult: DayScheduleResult = DayScheduleResult.Unavailable("Loading schedule..."),
    val section: String = "",
    val isRefreshing: Boolean = false
) {
    companion object {
        fun getInitialDay(): Int {
            val now = Calendar.getInstance()
            val day = now.get(Calendar.DAY_OF_WEEK)
            return if (day == Calendar.SUNDAY) Calendar.MONDAY else day
        }
    }
}

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)
    private val repository = TimetableRepository(application, prefs)
    private val apiClient = ErpApiClient(application)

    private val _uiState = MutableStateFlow(
        ScheduleUiState(
            section = prefs.selectedSection.ifBlank { prefs.userProfile?.section.orEmpty() }
        )
    )
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadScheduleForDay(_uiState.value.selectedCalendarDay)
    }

    fun selectDay(calendarDay: Int) {
        _uiState.value = _uiState.value.copy(selectedCalendarDay = calendarDay)
        loadScheduleForDay(calendarDay)
    }

    fun loadScheduleForDay(calendarDay: Int) {
        val result = repository.getScheduleForDay(calendarDay)
        _uiState.value = _uiState.value.copy(
            scheduleResult = result,
            section = prefs.selectedSection.ifBlank { prefs.userProfile?.section.orEmpty() }
        )
    }

    fun refreshSchedule() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            try {
                apiClient.weeklyAttendance
            } catch (ignored: Exception) {
            } finally {
                val updatedResult = repository.getScheduleForDay(_uiState.value.selectedCalendarDay)
                _uiState.value = _uiState.value.copy(
                    scheduleResult = updatedResult,
                    isRefreshing = false,
                    section = prefs.selectedSection.ifBlank { prefs.userProfile?.section.orEmpty() }
                )
            }
        }
    }
}
