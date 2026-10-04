package com.lloyd.attendance.core.domain

/**
 * Pure Kotlin mathematical engine for attendance metrics and threshold compliance.
 * Implements discrete integer arithmetic to avoid IEEE-754 floating point rounding errors.
 */
object AttendanceCalculator {

    val DEFAULT_TARGETS = listOf(75, 80, 85, 90)

    /**
     * Calculates classes needed to reach target percentage K (e.g. 75, 80, 85, 90).
     * If already at or above target, returns 0.
     * If totalClasses == 0, returns 0.
     */
    fun calculateClassesNeeded(present: Int, total: Int, targetPercentage: Int): Int {
        if (total <= 0 || targetPercentage <= 0 || targetPercentage >= 100) return 0
        val clampedPresent = present.coerceIn(0, total)
        val numerator = (targetPercentage * total) - (100 * clampedPresent)
        if (numerator <= 0) return 0

        val denominator = 100 - targetPercentage
        // Ceiling division: (N + D - 1) / D
        return (numerator + denominator - 1) / denominator
    }

    /**
     * Calculates consecutive classes that can be bunked while maintaining >= target percentage K.
     * If currently below target or totalClasses == 0, returns 0.
     */
    fun calculateCanBunk(present: Int, total: Int, targetPercentage: Int): Int {
        if (total <= 0 || targetPercentage <= 0 || targetPercentage >= 100) return 0
        val clampedPresent = present.coerceIn(0, total)
        val margin = (100 * clampedPresent) - (targetPercentage * total)
        if (margin < 0) return 0

        // Floor division: margin / targetPercentage
        return margin / targetPercentage
    }

    /**
     * Generates a TargetThreshold for a specific target percentage.
     */
    fun calculateThreshold(present: Int, total: Int, targetPercentage: Int): TargetThreshold {
        return TargetThreshold(
            targetPercentage = targetPercentage,
            classesNeededToReach = calculateClassesNeeded(present, total, targetPercentage),
            canBunkWhileMaintaining = calculateCanBunk(present, total, targetPercentage)
        )
    }

    /**
     * Computes all standard target thresholds (75%, 80%, 85%, 90%).
     */
    fun calculateAllThresholds(present: Int, total: Int, targets: List<Int> = DEFAULT_TARGETS): Map<Int, TargetThreshold> {
        return targets.associateWith { target ->
            calculateThreshold(present, total, target)
        }
    }
}
