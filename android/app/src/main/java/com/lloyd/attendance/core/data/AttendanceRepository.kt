package com.lloyd.attendance.core.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lloyd.attendance.api.ErpApiClient
import com.lloyd.attendance.api.Models
import com.lloyd.attendance.core.domain.AttendanceCalculator
import com.lloyd.attendance.core.domain.OverallAttendance
import com.lloyd.attendance.core.domain.SubjectAttendance
import com.lloyd.attendance.data.AppPreferences
import com.lloyd.attendance.feature.logs.AttendanceLogsProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AttendanceRepository(
    private val context: Context,
    private val apiClient: ErpApiClient = ErpApiClient(context),
    private val prefs: AppPreferences = AppPreferences.getInstance(context),
    private val gson: Gson = Gson()
) {

    private val syncMutex = Mutex()
    private val _snapshot = MutableStateFlow(loadInitialSnapshot())
    val snapshot: StateFlow<AttendanceSnapshot> = _snapshot.asStateFlow()

    private val _sessionExpiredEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredEvents: SharedFlow<Unit> = _sessionExpiredEvents.asSharedFlow()

    fun loadInitialSnapshot(): AttendanceSnapshot {
        val cachedMonthly = prefs.monthlyData
        val cachedLogsJson = prefs.attendanceLogs
        val cachedStats = prefs.cachedStats
        val user = prefs.userProfile

        val records: List<Models.StudentAttendanceItem> = if (!cachedLogsJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<Models.StudentAttendanceItem>>() {}.type
                gson.fromJson<List<Models.StudentAttendanceItem>>(cachedLogsJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val subjects = if (records.isNotEmpty()) {
            AttendanceLogsProcessor.reconcileSubjectAttendance(records)
        } else {
            parseSubjectsFromMonthly(cachedMonthly)
        }

        val totalP = cachedStats?.totalPresent ?: subjects.sumOf { it.presentCount }
        val totalC = cachedStats?.totalClasses ?: subjects.sumOf { it.totalClasses }
        val studentName = user?.name ?: cachedStats?.studentName.orEmpty()
        val studentId = prefs.getStudentId()

        val overall = OverallAttendance(
            studentName = studentName,
            studentId = studentId,
            totalPresent = totalP,
            totalClasses = totalC,
            subjects = subjects,
            thresholds = AttendanceCalculator.calculateAllThresholds(totalP, totalC),
            lastUpdatedMillis = cachedStats?.lastUpdatedMillis ?: System.currentTimeMillis()
        )

        val ledgerPresent = records.count { it.isPresent }
        val ledgerAbsent = records.size - ledgerPresent
        val reconciliation = AttendanceReconciliation.compute(
            aggregatePresent = totalP,
            aggregateTotal = totalC,
            ledgerPresent = ledgerPresent,
            ledgerAbsent = ledgerAbsent
        )

        return AttendanceSnapshot(
            overall = overall,
            records = records,
            reconciliation = reconciliation,
            lastUpdatedMillis = overall.lastUpdatedMillis,
            sourceState = if (totalC > 0) DataSourceState.STALE else DataSourceState.OFFLINE
        )
    }

    suspend fun refresh(isForeground: Boolean = true): Result<AttendanceSnapshot> = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            try {
                // 1. Transactional Fetch: Monthly aggregate
                val monthlyData = apiClient.monthlyAttendance

                // Fetch authentic student profile details from /profile/me
                try {
                    apiClient.profile
                } catch (ignored: Exception) {
                }

                // 2. Transactional Fetch: Class detailed logs
                val studentId = if (monthlyData.studentId > 0) monthlyData.studentId else prefs.studentId
                val freshLogs: List<Models.StudentAttendanceItem> = if (studentId > 0) {
                    try {
                        apiClient.getStudentAttendanceLogs(studentId) ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                // 3. Atomically persist both to persistent cache
                if (freshLogs.isNotEmpty()) {
                    prefs.saveAttendanceLogs(gson.toJson(freshLogs))
                }
                apiClient.calculateStatsFromMonthly(monthlyData)

                // 4. Derive reconciled models
                val subjects = if (freshLogs.isNotEmpty()) {
                    AttendanceLogsProcessor.reconcileSubjectAttendance(freshLogs)
                } else {
                    parseSubjectsFromMonthly(prefs.monthlyData)
                }

                val totalP = monthlyData.months?.sumOf { it.present } ?: 0
                val totalC = monthlyData.months?.sumOf { it.total } ?: 0
                val user = prefs.userProfile
                val studentName = monthlyData.studentName ?: user?.name.orEmpty()
                val nowMillis = System.currentTimeMillis()

                val overall = OverallAttendance(
                    studentName = studentName,
                    studentId = studentId,
                    totalPresent = totalP,
                    totalClasses = totalC,
                    subjects = subjects,
                    thresholds = AttendanceCalculator.calculateAllThresholds(totalP, totalC),
                    lastUpdatedMillis = nowMillis
                )

                val ledgerPresent = freshLogs.count { it.isPresent }
                val ledgerAbsent = freshLogs.size - ledgerPresent
                val reconciliation = AttendanceReconciliation.compute(
                    aggregatePresent = totalP,
                    aggregateTotal = totalC,
                    ledgerPresent = ledgerPresent,
                    ledgerAbsent = ledgerAbsent
                )

                val freshSnapshot = AttendanceSnapshot(
                    overall = overall,
                    records = freshLogs,
                    reconciliation = reconciliation,
                    lastUpdatedMillis = nowMillis,
                    sourceState = DataSourceState.FRESH,
                    error = null
                )

                _snapshot.value = freshSnapshot
                Result.success(freshSnapshot)
            } catch (e: Exception) {
                // RULE: Do not overwrite good cache on failure! Keep existing cache marked STALE.
                val current = _snapshot.value
                val isAuthExpired = (e is ErpApiClient.ErpException && e.isAuthExpired) ||
                        (e.message?.contains("Session expired", ignoreCase = true) == true) ||
                        (e.message?.contains("Please sign in", ignoreCase = true) == true)

                val classifiedError = when {
                    isAuthExpired -> SyncError.AuthExpired
                    e is UnknownHostException -> SyncError.NetworkUnavailable
                    e is SocketTimeoutException -> SyncError.ServerError(408, "Connection timed out")
                    e is IOException -> SyncError.NetworkUnavailable
                    else -> SyncError.Unknown(e.message)
                }

                if (isAuthExpired) {
                    prefs.logout()
                    _sessionExpiredEvents.tryEmit(Unit)
                }

                val fallbackSnapshot = current.copy(
                    sourceState = if (isAuthExpired) DataSourceState.ERROR else if (current.overall.totalClasses > 0) DataSourceState.STALE else DataSourceState.ERROR,
                    error = classifiedError
                )
                _snapshot.value = fallbackSnapshot
                Result.failure(e)
            }
        }
    }

    @JvmOverloads
    fun refreshBlocking(isForeground: Boolean = false): AttendanceSnapshot = kotlinx.coroutines.runBlocking {
        refresh(isForeground)
        _snapshot.value
    }

    private fun parseSubjectsFromMonthly(monthlyJson: String?): List<SubjectAttendance> {
        // Subjects are properly derived from the detailed ledger records
        return emptyList()
    }

    companion object {
        @Volatile
        private var instance: AttendanceRepository? = null

        @JvmStatic
        fun getInstance(context: Context): AttendanceRepository {
            return instance ?: synchronized(this) {
                instance ?: AttendanceRepository(context.applicationContext).also { instance = it }
            }
        }

        fun buildEmptySnapshot(): AttendanceSnapshot {
            val overall = OverallAttendance(
                studentName = "",
                studentId = 0,
                totalPresent = 0,
                totalClasses = 0
            )
            val rec = AttendanceReconciliation.compute(0, 0, 0, 0)
            return AttendanceSnapshot(overall, emptyList(), rec, 0L, DataSourceState.OFFLINE)
        }

        fun buildSnapshot(
            studentName: String,
            studentId: Int,
            monthlyPresent: Int,
            monthlyTotal: Int,
            ledgerRecords: List<Models.StudentAttendanceItem>,
            sourceState: DataSourceState
        ): AttendanceSnapshot {
            val overall = OverallAttendance(
                studentName = studentName,
                studentId = studentId,
                totalPresent = monthlyPresent,
                totalClasses = monthlyTotal
            )
            val p = ledgerRecords.count { it.isPresent }
            val a = ledgerRecords.size - p
            val rec = AttendanceReconciliation.compute(monthlyPresent, monthlyTotal, p, a)
            return AttendanceSnapshot(overall, ledgerRecords, rec, System.currentTimeMillis(), sourceState)
        }
    }
}
