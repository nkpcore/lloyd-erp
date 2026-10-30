package com.lloyd.attendance.core.domain

/**
 * Interactive 'What-If' Simulation Engine for evaluating hypothetical future attendances.
 */
object SimulationEngine {

    const val BORDERLINE_THRESHOLD = 70.0

    data class SimulationResult(
        val baselinePresent: Int,
        val baselineTotal: Int,
        val baselinePercentage: AttendancePercentage,
        val addedPresent: Int,
        val addedAbsent: Int,
        val projectedPresent: Int,
        val projectedTotal: Int,
        val projectedPercentage: AttendancePercentage,
        val projectedHealth: AttendanceHealth,
        val percentageDelta: Double,
        val updatedThresholds: Map<Int, TargetThreshold>
    )

    fun simulate(
        present: Int,
        total: Int,
        additionalPresent: Int,
        additionalAbsent: Int
    ): SimulationResult {
        val safeTotalIn = total.coerceAtLeast(0)
        val safePresentIn = present.coerceIn(0, safeTotalIn)
        val clampedAddP = additionalPresent.coerceAtLeast(0)
        val clampedAddA = additionalAbsent.coerceAtLeast(0)

        val newP = safePresentIn + clampedAddP
        val newTotal = safeTotalIn + clampedAddP + clampedAddA

        val basePct = AttendancePercentage.of(safePresentIn, safeTotalIn)
        val projPct = AttendancePercentage.of(newP, newTotal)

        val baseNumeric = basePct.numericValue ?: 0.0
        val projNumeric = projPct.numericValue ?: 0.0

        val health = when (projPct) {
            is AttendancePercentage.NoData -> AttendanceHealth.UNRECORDED
            is AttendancePercentage.Value -> when {
                projPct.percentage >= 75.0 -> AttendanceHealth.HEALTHY
                projPct.percentage >= BORDERLINE_THRESHOLD -> AttendanceHealth.BORDERLINE
                else -> AttendanceHealth.CRITICAL
            }
        }

        return SimulationResult(
            baselinePresent = safePresentIn,
            baselineTotal = safeTotalIn,
            baselinePercentage = basePct,
            addedPresent = clampedAddP,
            addedAbsent = clampedAddA,
            projectedPresent = newP,
            projectedTotal = newTotal,
            projectedPercentage = projPct,
            projectedHealth = health,
            percentageDelta = projNumeric - baseNumeric,
            updatedThresholds = AttendanceCalculator.calculateAllThresholds(newP, newTotal)
        )
    }
}
