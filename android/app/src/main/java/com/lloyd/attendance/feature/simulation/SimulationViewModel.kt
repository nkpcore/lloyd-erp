package com.lloyd.attendance.feature.simulation

import androidx.lifecycle.ViewModel
import com.lloyd.attendance.core.domain.AttendanceCalculator
import com.lloyd.attendance.core.domain.BunkAdvisor
import com.lloyd.attendance.core.domain.SimulationEngine
import com.lloyd.attendance.core.domain.TargetThreshold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SimulationUiState(
    val subjectName: String = "Overall Attendance",
    val subjectCode: String? = null,
    val teacherName: String? = null,
    val baselinePresent: Int = 0,
    val baselineTotal: Int = 0,
    val addedPresent: Int = 0,
    val addedAbsent: Int = 0,
    val simulationResult: SimulationEngine.SimulationResult = SimulationEngine.simulate(0, 0, 0, 0),
    val advice: BunkAdvisor.Advice = BunkAdvisor.getAdvice(0, 0),
    val multiTargetThresholds: Map<Int, TargetThreshold> = emptyMap()
)

class SimulationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SimulationUiState())
    val uiState: StateFlow<SimulationUiState> = _uiState.asStateFlow()

    fun initialize(
        subjectName: String,
        present: Int,
        total: Int,
        code: String? = null,
        teacher: String? = null
    ) {
        val initialSim = SimulationEngine.simulate(present, total, 0, 0)
        val advice = BunkAdvisor.getAdvice(present, total)
        val thresholds = AttendanceCalculator.calculateAllThresholds(present, total)

        _uiState.value = SimulationUiState(
            subjectName = subjectName,
            subjectCode = code,
            teacherName = teacher,
            baselinePresent = present,
            baselineTotal = total,
            addedPresent = 0,
            addedAbsent = 0,
            simulationResult = initialSim,
            advice = advice,
            multiTargetThresholds = thresholds
        )
    }

    fun addPresent(delta: Int) {
        _uiState.update { current ->
            val newAddP = (current.addedPresent + delta).coerceAtLeast(0)
            val newSim = SimulationEngine.simulate(
                current.baselinePresent,
                current.baselineTotal,
                newAddP,
                current.addedAbsent
            )
            current.copy(
                addedPresent = newAddP,
                simulationResult = newSim
            )
        }
    }

    fun addAbsent(delta: Int) {
        _uiState.update { current ->
            val newAddA = (current.addedAbsent + delta).coerceAtLeast(0)
            val newSim = SimulationEngine.simulate(
                current.baselinePresent,
                current.baselineTotal,
                current.addedPresent,
                newAddA
            )
            current.copy(
                addedAbsent = newAddA,
                simulationResult = newSim
            )
        }
    }

    fun reset() {
        _uiState.update { current ->
            val resetSim = SimulationEngine.simulate(
                current.baselinePresent,
                current.baselineTotal,
                0,
                0
            )
            current.copy(
                addedPresent = 0,
                addedAbsent = 0,
                simulationResult = resetSim
            )
        }
    }
}
