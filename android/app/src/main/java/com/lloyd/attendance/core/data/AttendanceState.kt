package com.lloyd.attendance.core.data

import com.lloyd.attendance.api.Models
import com.lloyd.attendance.core.domain.OverallAttendance

enum class DataSourceState {
    FRESH,
    STALE,
    OFFLINE,
    PARTIAL,
    ERROR
}

sealed interface SyncError {
    data object NetworkUnavailable : SyncError
    data object AuthExpired : SyncError
    data class ServerError(val code: Int, val message: String? = null) : SyncError
    data class ParseError(val message: String) : SyncError
    data class Unknown(val message: String?) : SyncError
}

data class AttendanceReconciliation(
    val aggregatePresent: Int,
    val aggregateTotal: Int,
    val ledgerPresent: Int,
    val ledgerAbsent: Int,
    val ledgerTotal: Int,
    val discrepancyCount: Int,
    val isReconciled: Boolean,
    val explanation: String
) {
    companion object {
        fun compute(
            aggregatePresent: Int,
            aggregateTotal: Int,
            ledgerPresent: Int,
            ledgerAbsent: Int
        ): AttendanceReconciliation {
            val ledgerTotal = ledgerPresent + ledgerAbsent
            val discrepancy = kotlin.math.abs(aggregateTotal - ledgerTotal)
            val isReconciled = discrepancy == 0 && (aggregatePresent == ledgerPresent)

            val explanation = if (isReconciled) {
                "Monthly aggregate and detailed session logs match exactly."
            } else {
                "Monthly aggregate records $aggregateTotal total classes ($aggregatePresent Present), while detailed ledger logs $ledgerTotal class entries ($ledgerPresent Present, $ledgerAbsent Absent). Difference of $discrepancy unrecorded session(s)."
            }

            return AttendanceReconciliation(
                aggregatePresent = aggregatePresent,
                aggregateTotal = aggregateTotal,
                ledgerPresent = ledgerPresent,
                ledgerAbsent = ledgerAbsent,
                ledgerTotal = ledgerTotal,
                discrepancyCount = discrepancy,
                isReconciled = isReconciled,
                explanation = explanation
            )
        }
    }
}

data class AttendanceSnapshot(
    val overall: OverallAttendance,
    val records: List<Models.StudentAttendanceItem>,
    val reconciliation: AttendanceReconciliation,
    val lastUpdatedMillis: Long,
    val sourceState: DataSourceState,
    val error: SyncError? = null
)
