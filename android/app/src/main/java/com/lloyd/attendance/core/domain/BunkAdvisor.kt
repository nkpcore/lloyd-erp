package com.lloyd.attendance.core.domain

/**
 * Provides contextual advice and recommendations based on attendance health and targets.
 */
object BunkAdvisor {

    data class Advice(
        val headline: String,
        val detail: String,
        val isSafe: Boolean,
        val canBunk75: Int,
        val needed75: Int,
        val thresholds: Map<Int, TargetThreshold>
    )

    fun getAdvice(present: Int, total: Int): Advice {
        if (total <= 0) {
            return Advice(
                headline = "No Classes Recorded Yet",
                detail = "Attendance monitoring and target advisories will activate once your first class is held.",
                isSafe = true,
                canBunk75 = 0,
                needed75 = 0,
                thresholds = emptyMap()
            )
        }

        val thresholds = AttendanceCalculator.calculateAllThresholds(present, total)
        val t75 = thresholds[75] ?: TargetThreshold(75, 0, 0)
        val pct = (present.toDouble() / total.toDouble()) * 100.0

        return when {
            pct >= 75.0 -> {
                if (t75.canBunkWhileMaintaining > 0) {
                    Advice(
                        headline = "Safe: ${t75.canBunkWhileMaintaining} Bunk(s) Available",
                        detail = "You can miss up to ${t75.canBunkWhileMaintaining} class(es) and remain at or above the 75% requirement.",
                        isSafe = true,
                        canBunk75 = t75.canBunkWhileMaintaining,
                        needed75 = 0,
                        thresholds = thresholds
                    )
                } else {
                    Advice(
                        headline = "On the Line (75%)",
                        detail = "You are currently meeting the 75% cutoff. Missing the next class will push you into the shortage zone.",
                        isSafe = true,
                        canBunk75 = 0,
                        needed75 = 0,
                        thresholds = thresholds
                    )
                }
            }
            else -> {
                Advice(
                    headline = "Action Required: Need Next ${t75.classesNeededToReach} Class(es)",
                    detail = "You are below the 75% threshold. Attend the next ${t75.classesNeededToReach} consecutive class(es) to recover.",
                    isSafe = false,
                    canBunk75 = 0,
                    needed75 = t75.classesNeededToReach,
                    thresholds = thresholds
                )
            }
        }
    }
}
