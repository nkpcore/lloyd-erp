package com.lloyd.attendance.feature.latest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lloyd.attendance.api.Models
import com.lloyd.attendance.core.data.AttendanceRepository
import com.lloyd.attendance.feature.logs.LogStatusFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LatestAttendanceUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val allRecords: List<Models.StudentAttendanceItem> = emptyList(),
    val filteredRecords: List<Models.StudentAttendanceItem> = emptyList(),
    val statusFilter: LogStatusFilter = LogStatusFilter.ALL,
    val searchQuery: String = ""
)

class LatestAttendanceViewModel(
    application: Application,
    private val repository: AttendanceRepository = AttendanceRepository.getInstance(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LatestAttendanceUiState())
    val uiState: StateFlow<LatestAttendanceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.snapshot.collect { snapshot ->
                // Sort strictly descending by attendance date and ID
                val sorted = snapshot.records.sortedWith(
                    compareByDescending<Models.StudentAttendanceItem> { it.attendanceDate ?: "" }
                        .thenByDescending { it.id }
                )
                _uiState.update { state ->
                    state.copy(
                        allRecords = sorted,
                        filteredRecords = filterRecords(sorted, state.searchQuery, state.statusFilter)
                    )
                }
            }
        }
    }

    fun setStatusFilter(filter: LogStatusFilter) {
        _uiState.update { state ->
            state.copy(
                statusFilter = filter,
                filteredRecords = filterRecords(state.allRecords, state.searchQuery, filter)
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredRecords = filterRecords(state.allRecords, query, state.statusFilter)
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            repository.refresh(isForeground = true)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun filterRecords(
        records: List<Models.StudentAttendanceItem>,
        query: String,
        filter: LogStatusFilter
    ): List<Models.StudentAttendanceItem> {
        val q = query.trim().lowercase()
        return records.filter { item ->
            val matchesFilter = when (filter) {
                LogStatusFilter.ALL -> true
                LogStatusFilter.PRESENT -> item.isPresent
                LogStatusFilter.ABSENT -> !item.isPresent
            }
            val matchesQuery = q.isEmpty() ||
                    item.subjectDisplayName.lowercase().contains(q) ||
                    (item.facultyDisplayName ?: "").lowercase().contains(q) ||
                    (item.attendanceDate ?: "").lowercase().contains(q)

            matchesFilter && matchesQuery
        }
    }
}
