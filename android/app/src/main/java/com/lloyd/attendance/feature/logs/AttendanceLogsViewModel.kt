package com.lloyd.attendance.feature.logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lloyd.attendance.api.Models
import com.lloyd.attendance.core.data.AttendanceRepository
import com.lloyd.attendance.core.data.DataSourceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LogStatusFilter {
    ALL,
    PRESENT,
    ABSENT
}

data class AttendanceLogsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val allLogs: List<Models.StudentAttendanceItem> = emptyList(),
    val filteredLogs: List<Models.StudentAttendanceItem> = emptyList(),
    val groupedByDate: Map<String, List<Models.StudentAttendanceItem>> = emptyMap(),
    val searchQuery: String = "",
    val statusFilter: LogStatusFilter = LogStatusFilter.ALL,
    val totalMarked: Int = 0,
    val totalPresent: Int = 0,
    val totalAbsent: Int = 0,
    val attendancePercentage: Double = 0.0
)

class AttendanceLogsViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: AttendanceRepository = AttendanceRepository.getInstance(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AttendanceLogsUiState())
    val uiState: StateFlow<AttendanceLogsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.snapshot.collect { snapshot ->
                val isOffline = snapshot.sourceState == DataSourceState.OFFLINE || snapshot.sourceState == DataSourceState.STALE
                applyLogs(snapshot.records, isLoading = false, isOffline = isOffline)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            repository.refresh(isForeground = true)
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun setSearchQuery(query: String) {
        val current = _uiState.value
        val updated = current.copy(searchQuery = query)
        _uiState.value = recomputeFilteredState(updated)
    }

    fun setStatusFilter(filter: LogStatusFilter) {
        val current = _uiState.value
        val updated = current.copy(statusFilter = filter)
        _uiState.value = recomputeFilteredState(updated)
    }

    private fun applyLogs(
        logs: List<Models.StudentAttendanceItem>,
        isLoading: Boolean,
        isOffline: Boolean
    ) {
        val summary = AttendanceLogsProcessor.computeSummary(logs)

        val baseState = _uiState.value.copy(
            isLoading = isLoading,
            isRefreshing = false,
            isOffline = isOffline,
            errorMessage = null,
            allLogs = logs,
            totalMarked = summary.totalMarked,
            totalPresent = summary.totalPresent,
            totalAbsent = summary.totalAbsent,
            attendancePercentage = summary.attendancePercentage
        )

        _uiState.value = recomputeFilteredState(baseState)
    }

    private fun recomputeFilteredState(state: AttendanceLogsUiState): AttendanceLogsUiState {
        val filtered = AttendanceLogsProcessor.filterLogs(
            logs = state.allLogs,
            searchQuery = state.searchQuery,
            statusFilter = state.statusFilter
        )
        val grouped = AttendanceLogsProcessor.groupByDate(filtered)

        return state.copy(
            filteredLogs = filtered,
            groupedByDate = grouped
        )
    }
}

class AttendanceLogsViewModelFactory(
    private val application: Application,
    private val repository: AttendanceRepository = AttendanceRepository.getInstance(application)
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttendanceLogsViewModel::class.java)) {
            return AttendanceLogsViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
