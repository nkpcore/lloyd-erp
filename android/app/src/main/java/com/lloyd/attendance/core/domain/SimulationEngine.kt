package com.lloyd.attendance.core.domain

/**
 * Interactive 'What-If' Simulation Engine for evaluating hypothetical future attendances.
 */
object SimulationEngine {

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
        val clampedAddP = additionalPresent.coerceAtLeast(0)
        val clampedAddA = additionalAbsent.coerceAtLeast(0)

        val newP = present + clampedAddP
        val newTotal = total + clampedAddP + clampedAddA

        val basePct = AttendancePercentage.of(present, total)
        val projPct = AttendancePercentage.of(newP, newTotal)

        val baseNumeric = basePct.numericValue ?: 0.0
        val projNumeric = projPct.numericValue ?: 0.0

        val health = when (projPct) {
            is AttendancePercentage.NoData -> AttendanceHealth.UNRECORDED
            is AttendancePercentage.Value -> when {
                projPct.percentage >= 75.0 -> AttendanceHealth.HEALTHY
                projPct.percentage >= 70.0 -> AttendanceHealth.BORDERLINE
                else -> AttendanceHealth.CRITICAL
            }
        }

        return SimulationResult(
            baselinePresent = present,
            baselineTotal = total,
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
