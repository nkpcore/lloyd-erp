package com.lloyd.attendance.core.domain

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Represents attendance percentage strictly distinguishing 0/0 (No Data) from 0.0%.
 * Prevents false 100% or false 0% reporting when classes have not yet commenced.
 */
sealed interface AttendancePercentage {
    val displayValue: String
    val numericValue: Double?

    data class Value(val percentage: Double) : AttendancePercentage {
        override val displayValue: String = String.format("%.1f%%", percentage)
        override val numericValue: Double = percentage
    }

    data object NoData : AttendancePercentage {
        override val displayValue: String = "--.-%"
        override val numericValue: Double? = null
    }

    companion object {
        fun of(present: Int, total: Int): AttendancePercentage {
            if (total <= 0) return NoData
            val clampedPresent = present.coerceIn(0, total)
            val pct = (clampedPresent.toDouble() / total.toDouble()) * 100.0
            return Value(pct)
        }
    }
}

/**
 * Attendance status classification based on 75% regulatory requirement.
 */
enum class AttendanceHealth {
    HEALTHY,      // >= 75%
    BORDERLINE,   // 70% .. <75%
    CRITICAL,     // < 70%
    UNRECORDED    // No classes held yet
}

/**
 * Target calculation representing needed classes or allowed bunks to meet a target percentage.
 */
data class TargetThreshold(
    val targetPercentage: Int, // e.g. 75, 80, 85, 90
    val classesNeededToReach: Int, // Number of consecutive attendances needed if below target
    val canBunkWhileMaintaining: Int // Number of consecutive bunks allowed if at or above target
)

/**
 * Core Subject Attendance entity.
 */
data class SubjectAttendance(
    val subjectCode: String,
    val subjectName: String,
    val presentCount: Int,
    val totalClasses: Int,
    val absentCount: Int = (totalClasses - presentCount).coerceAtLeast(0),
    val percentage: AttendancePercentage = AttendancePercentage.of(presentCount, totalClasses),
    val health: AttendanceHealth = when (percentage) {
        is AttendancePercentage.NoData -> AttendanceHealth.UNRECORDED
        is AttendancePercentage.Value -> when {
            percentage.percentage >= 75.0 -> AttendanceHealth.HEALTHY
            percentage.percentage >= 70.0 -> AttendanceHealth.BORDERLINE
            else -> AttendanceHealth.CRITICAL
        }
    },
    val teacherName: String? = null,
    val isElective: Boolean = false,
    val thresholds: Map<Int, TargetThreshold> = emptyMap()
)

/**
 * Overall aggregate attendance statistics.
 */
data class OverallAttendance(
    val studentName: String,
    val rollNumber: String? = null,
    val studentId: Int = 0,
    val totalPresent: Int,
    val totalClasses: Int,
    val totalAbsent: Int = (totalClasses - totalPresent).coerceAtLeast(0),
    val percentage: AttendancePercentage = AttendancePercentage.of(totalPresent, totalClasses),
    val health: AttendanceHealth = when (percentage) {
        is AttendancePercentage.NoData -> AttendanceHealth.UNRECORDED
        is AttendancePercentage.Value -> when {
            percentage.percentage >= 75.0 -> AttendanceHealth.HEALTHY
            percentage.percentage >= 70.0 -> AttendanceHealth.BORDERLINE
            else -> AttendanceHealth.CRITICAL
        }
    },
    val subjects: List<SubjectAttendance> = emptyList(),
    val thresholds: Map<Int, TargetThreshold> = emptyMap(),
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)
