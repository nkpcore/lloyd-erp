package com.lloyd.attendance.feature.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lloyd.attendance.api.ErpApiClient
import com.lloyd.attendance.api.Models
import com.lloyd.attendance.core.domain.AttendanceCalculator
import com.lloyd.attendance.core.domain.OverallAttendance
import com.lloyd.attendance.core.domain.SubjectAttendance
import com.lloyd.attendance.data.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val studentName: String = "",
    val studentId: Int = 0,
    val section: String = "",
    val overall: OverallAttendance = OverallAttendance(
        studentName = "",
        totalPresent = 0,
        totalClasses = 0
    ),
    val lastSyncMillis: Long = 0L,
    val errorMessage: String? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)
    private val apiClient = ErpApiClient(application)
    private val gson = Gson()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadCachedData()
        syncWithErp(isPullToRefresh = false)
    }

    fun loadCachedData() {
        val user = prefs.userProfile
        val cachedMonthly = prefs.getMonthlyData()
        val cachedStats = prefs.getCachedStats()

        val subjectsList = parseSubjectsFromMonthly(cachedMonthly)
        val totalP = cachedStats?.totalPresent ?: subjectsList.sumOf { it.presentCount }
        val totalC = cachedStats?.totalClasses ?: subjectsList.sumOf { it.totalClasses }

        val studentName = user?.name ?: cachedStats?.studentName.orEmpty()
        val overall = OverallAttendance(
            studentName = studentName,
            studentId = prefs.getStudentId(),
            totalPresent = totalP,
            totalClasses = totalC,
            subjects = subjectsList,
            thresholds = AttendanceCalculator.calculateAllThresholds(totalP, totalC),
            lastUpdatedMillis = cachedStats?.lastUpdatedMillis ?: System.currentTimeMillis()
        )

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            studentName = studentName,
            studentId = prefs.getStudentId(),
            section = prefs.getSelectedSection().ifBlank { user?.section.orEmpty() },
            overall = overall,
            lastSyncMillis = cachedStats?.lastUpdatedMillis ?: 0L
        )
    }

    fun refresh() {
        syncWithErp(isPullToRefresh = true)
    }

    private fun syncWithErp(isPullToRefresh: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                isRefreshing = isPullToRefresh,
                errorMessage = null
            )

            try {
                // 1. Fetch monthly aggregate attendance
                val monthlyData = apiClient.monthlyAttendance

                // 2. Concurrently sync student attendance logs for teacher breakdown
                val targetStudentId = monthlyData.studentId.takeIf { it > 0 } ?: prefs.getStudentId()
                if (targetStudentId > 0) {
                    try {
                        apiClient.getStudentAttendanceLogs(targetStudentId)
                    } catch (ignored: Exception) {
                    }
                }

                // 3. Update parsed state
                val subjects = parseSubjectsFromMonthly(prefs.getMonthlyData())
                val totalP = monthlyData.months?.sumOf { it.present } ?: 0
                val totalC = monthlyData.months?.sumOf { it.total } ?: 0

                val user = prefs.userProfile
                val studentName = monthlyData.studentName ?: user?.name.orEmpty()
                val nowMillis = System.currentTimeMillis()

                val overall = OverallAttendance(
                    studentName = studentName,
                    studentId = monthlyData.studentId.takeIf { it > 0 } ?: prefs.getStudentId(),
                    totalPresent = totalP,
                    totalClasses = totalC,
                    subjects = subjects,
                    thresholds = AttendanceCalculator.calculateAllThresholds(totalP, totalC),
                    lastUpdatedMillis = nowMillis
                )

                // Cache calculated stats for widget and offline
                apiClient.calculateStatsFromMonthly(monthlyData)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isOffline = false,
                    studentName = studentName,
                    studentId = overall.studentId,
                    section = prefs.getSelectedSection().ifBlank { user?.section.orEmpty() },
                    overall = overall,
                    lastSyncMillis = nowMillis,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isOffline = true,
                    errorMessage = e.message ?: "Failed to sync with Lloyd ERP"
                )
            }
        }
    }

    private fun parseSubjectsFromMonthly(json: String?): List<SubjectAttendance> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<Models.ApiResponse<Models.MonthlyAttendanceData>>() {}.type
            val res: Models.ApiResponse<Models.MonthlyAttendanceData>? = gson.fromJson(json, type)
            val months = res?.data?.months ?: return emptyList()

            // Parse logs for fine-grained subject breakdown if available
            val logsJson = prefs.getAttendanceLogs()
            if (!logsJson.isNullOrBlank()) {
                val logType = object : TypeToken<List<Models.StudentAttendanceItem>>() {}.type
                val logs: List<Models.StudentAttendanceItem>? = gson.fromJson(logsJson, logType)
                if (!logs.isNullOrEmpty()) {
                    val grouped = logs.groupBy { it.subjectName ?: "General Subject" }
                    return grouped.map { (name, items) ->
                        val present = items.count { it.status.equals("present", ignoreCase = true) }
                        val total = items.size
                        val firstItem = items.firstOrNull()
                        val subCode = firstItem?.subjectId?.toString().orEmpty()
                        val teacher = firstItem?.getFacultyDisplayName() ?: firstItem?.createdByName
                        SubjectAttendance(
                            subjectCode = subCode,
                            subjectName = name,
                            presentCount = present,
                            totalClasses = total,
                            teacherName = teacher,
                            thresholds = AttendanceCalculator.calculateAllThresholds(present, total)
                        )
                    }.sortedBy { it.percentage.numericValue ?: 100.0 }
                }
            }

            // Fallback to monthly labels if granular subject logs are not yet cached
            months.map { m ->
                SubjectAttendance(
                    subjectCode = "${m.year}-${m.monthNumber}",
                    subjectName = m.monthLabel ?: "Month ${m.monthNumber}",
                    presentCount = m.present,
                    totalClasses = m.total,
                    thresholds = AttendanceCalculator.calculateAllThresholds(m.present, m.total)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        prefs.logout()
        onLoggedOut()
    }
}
