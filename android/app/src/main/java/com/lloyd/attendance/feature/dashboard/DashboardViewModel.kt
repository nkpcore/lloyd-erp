package com.lloyd.attendance.feature.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lloyd.attendance.core.data.AttendanceReconciliation
import com.lloyd.attendance.core.data.AttendanceRepository
import com.lloyd.attendance.core.data.DataSourceState
import com.lloyd.attendance.core.domain.OverallAttendance
import com.lloyd.attendance.data.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val sourceState: DataSourceState = DataSourceState.FRESH,
    val studentName: String = "",
    val studentId: Int = 0,
    val section: String = "",
    val overall: OverallAttendance = OverallAttendance(
        studentName = "",
        totalPresent = 0,
        totalClasses = 0
    ),
    val reconciliation: AttendanceReconciliation = AttendanceReconciliation.compute(0, 0, 0, 0),
    val lastSyncMillis: Long = 0L,
    val broadcastNotice: String? = null,
    val errorMessage: String? = null
) {
    val isOffline: Boolean get() = sourceState == DataSourceState.OFFLINE || sourceState == DataSourceState.STALE
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences.getInstance(application)
    private val repository = AttendanceRepository.getInstance(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Collect repository snapshot continuously
        viewModelScope.launch {
            repository.snapshot.collect { snap ->
                val user = prefs.userProfile
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    sourceState = snap.sourceState,
                    studentName = snap.overall.studentName.ifBlank { user?.name.orEmpty() },
                    studentId = snap.overall.studentId,
                    section = prefs.selectedSection.ifBlank { user?.section.orEmpty() },
                    overall = snap.overall,
                    reconciliation = snap.reconciliation,
                    lastSyncMillis = snap.lastUpdatedMillis,
                    errorMessage = snap.error?.let { "Unable to refresh latest attendance" }
                )
            }
        }

        // Initial foreground sync
        refresh()
    }

    fun setBroadcastNotice(notice: String?) {
        _uiState.value = _uiState.value.copy(broadcastNotice = notice)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            repository.refresh(isForeground = true)
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        prefs.logout()
        onLoggedOut()
    }
}
