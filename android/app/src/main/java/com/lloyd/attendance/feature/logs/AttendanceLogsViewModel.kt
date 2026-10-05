package com.lloyd.attendance.feature.logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lloyd.attendance.api.ErpApiClient
import com.lloyd.attendance.api.Models
import com.lloyd.attendance.data.AppPreferences
import kotlinx.coroutines.Dispatchers
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
    val isLoading: Boolean = true,
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
    private val prefs: AppPreferences = AppPreferences.getInstance(application),
    private val apiClient: ErpApiClient = ErpApiClient(application),
    private val gson: Gson = Gson()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AttendanceLogsUiState())
    val uiState: StateFlow<AttendanceLogsUiState> = _uiState.asStateFlow()

    init {
        loadCachedLogs()
        refreshLogs(isPullToRefresh = false)
    }

    private fun loadCachedLogs() {
        val cachedJson = prefs.attendanceLogsJson
        if (!cachedJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<Models.StudentAttendanceItem>>() {}.type
                val cachedList: List<Models.StudentAttendanceItem> = gson.fromJson(cachedJson, type) ?: emptyList()
                if (cachedList.isNotEmpty()) {
                    applyLogs(cachedList, isLoading = false, isOffline = true)
                }
            } catch (ignored: Exception) {
            }
        }
    }

    fun refresh() {
        refreshLogs(isPullToRefresh = true)
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

    private fun refreshLogs(isPullToRefresh: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                isRefreshing = isPullToRefresh,
                isLoading = !isPullToRefresh && _uiState.value.allLogs.isEmpty(),
                errorMessage = null
            )

            try {
                val studentId = prefs.getStudentId()
                val freshLogs = apiClient.getStudentAttendanceLogs(studentId)

                if (!freshLogs.isNullOrEmpty()) {
                    prefs.saveAttendanceLogsJson(gson.toJson(freshLogs))
                }

                applyLogs(freshLogs ?: emptyList(), isLoading = false, isOffline = false)
            } catch (e: Exception) {
                // If network failure, preserve offline logs and display message
                val current = _uiState.value
                val message = e.message ?: "Failed to refresh attendance logs"
                _uiState.value = current.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isOffline = true,
                    errorMessage = if (current.allLogs.isNotEmpty()) "Offline: Displaying cached logs" else message
                )
            }
        }
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
    private val prefs: AppPreferences = AppPreferences.getInstance(application),
    private val apiClient: ErpApiClient = ErpApiClient(application)
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttendanceLogsViewModel::class.java)) {
            return AttendanceLogsViewModel(application, prefs, apiClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
