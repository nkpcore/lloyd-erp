package com.lloyd.attendance.feature.subject

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lloyd.attendance.api.Models
import com.lloyd.attendance.core.domain.SubjectAttendance
import com.lloyd.attendance.data.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SubjectDetailUiState(
    val subject: SubjectAttendance? = null,
    val logsForSubject: List<Models.StudentAttendanceItem> = emptyList(),
    val simulateAttend: Int = 0,
    val simulateMiss: Int = 0,
    val simulatedPercentage: Double = 0.0
)

class SubjectDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = com.lloyd.attendance.core.data.AttendanceRepository.getInstance(application)
    private val _uiState = MutableStateFlow(SubjectDetailUiState())
    val uiState: StateFlow<SubjectDetailUiState> = _uiState.asStateFlow()

    fun loadSubject(subject: SubjectAttendance) {
        val allLogs = repository.snapshot.value.records

        val filtered = allLogs.filter {
            (it.subjectName ?: "").equals(subject.subjectName, ignoreCase = true) ||
            (it.subjectId != null && it.subjectId.toString() == subject.subjectCode)
        }.sortedByDescending { it.attendanceDate }

        _uiState.value = SubjectDetailUiState(
            subject = subject,
            logsForSubject = filtered,
            simulateAttend = 0,
            simulateMiss = 0,
            simulatedPercentage = subject.percentage.numericValue ?: 0.0
        )
    }

    fun updateSimulation(attend: Int, miss: Int) {
        val sub = _uiState.value.subject ?: return
        val simPct = calculateSimulatedPercentage(sub.presentCount, sub.totalClasses, attend, miss)
        _uiState.value = _uiState.value.copy(
            simulateAttend = attend,
            simulateMiss = miss,
            simulatedPercentage = simPct
        )
    }

    companion object {
        fun calculateSimulatedPercentage(
            currentPresent: Int,
            currentTotal: Int,
            classesToAttend: Int,
            classesToMiss: Int
        ): Double {
            val newP = currentPresent + classesToAttend
            val newT = currentTotal + classesToAttend + classesToMiss
            return if (newT > 0) (newP * 100.0) / newT else 0.0
        }
    }
}
