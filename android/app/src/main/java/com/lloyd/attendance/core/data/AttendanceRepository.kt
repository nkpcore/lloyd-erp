package com.lloyd.attendance.core.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.lloyd.attendance.api.ErpApiClient
import com.lloyd.attendance.api.Models
import com.lloyd.attendance.core.domain.AttendanceCalculator
import com.lloyd.attendance.core.domain.OverallAttendance
import com.lloyd.attendance.core.domain.SubjectAttendance
import com.lloyd.attendance.core.domain.SubjectSummary
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
                val parsed: List<Models.StudentAttendanceItem>? = gson.fromJson(cachedLogsJson, type)
                parsed ?: emptyList()
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
                var logsFetchSucceeded = false
                val freshLogs: List<Models.StudentAttendanceItem> = if (studentId > 0) {
                    try {
                        val fetched = apiClient.getStudentAttendanceLogs(studentId)
                        logsFetchSucceeded = true
                        fetched
                    } catch (e: Exception) {
                        logsFetchSucceeded = false
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                val nowMillis = System.currentTimeMillis()

                // 3. Persist logs on any successful API response (including empty list []);
                //    do NOT overwrite cache on API error.
                persistLogSyncResult(logsFetchSucceeded, freshLogs, nowMillis)
                apiClient.calculateStatsFromMonthly(monthlyData)

                // 4. Derive reconciled models, keeping last good cached records if log fetch failed
                val currentRecords = _snapshot.value.records
                val effectiveLogs = if (logsFetchSucceeded) {
                    freshLogs
                } else if (currentRecords.isNotEmpty()) {
                    currentRecords
                } else {
                    freshLogs
                }

                val subjects = if (effectiveLogs.isNotEmpty()) {
                    AttendanceLogsProcessor.reconcileSubjectAttendance(effectiveLogs).also {
                        saveLastGoodSubjectModel(it)
                    }
                } else {
                    parseSubjectsFromMonthly(prefs.monthlyData)
                }

                val totalP = monthlyData.months?.sumOf { it.present } ?: 0
                val totalC = monthlyData.months?.sumOf { it.total } ?: 0
                val user = prefs.userProfile
                val studentName = monthlyData.studentName ?: user?.name.orEmpty()

                val overall = OverallAttendance(
                    studentName = studentName,
                    studentId = studentId,
                    totalPresent = totalP,
                    totalClasses = totalC,
                    subjects = subjects,
                    thresholds = AttendanceCalculator.calculateAllThresholds(totalP, totalC),
                    lastUpdatedMillis = nowMillis
                )

                val ledgerPresent = effectiveLogs.count { it.isPresent }
                val ledgerAbsent = effectiveLogs.size - ledgerPresent
                val reconciliation = AttendanceReconciliation.compute(
                    aggregatePresent = totalP,
                    aggregateTotal = totalC,
                    ledgerPresent = ledgerPresent,
                    ledgerAbsent = ledgerAbsent
                )

                val derivedState = if (!logsFetchSucceeded || (totalC > 0 && effectiveLogs.isEmpty())) {
                    DataSourceState.PARTIAL
                } else {
                    DataSourceState.FRESH
                }

                val freshSnapshot = AttendanceSnapshot(
                    overall = overall,
                    records = effectiveLogs,
                    reconciliation = reconciliation,
                    lastUpdatedMillis = nowMillis,
                    sourceState = derivedState,
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

    internal fun persistLogSyncResult(
        logsFetchSucceeded: Boolean,
        freshLogs: List<Models.StudentAttendanceItem>,
        syncTimeMillis: Long
    ) {
        if (logsFetchSucceeded) {
            prefs.saveAttendanceLogs(gson.toJson(freshLogs))
            prefs.saveLastSuccessfulLogSync(syncTimeMillis)
        }
    }

    private fun saveLastGoodSubjectModel(subjects: List<SubjectAttendance>) {
        if (subjects.isEmpty()) return
        val summaries = subjects.map {
            SubjectSummary(
                subjectName = it.subjectName,
                present = it.presentCount,
                absent = it.absentCount,
                total = it.totalClasses,
                subjectCode = it.subjectCode,
                teacherName = it.teacherName
            )
        }
        prefs.saveLastGoodSubjectModel(gson.toJson(summaries))
    }

    private fun loadLastGoodSubjectModel(): List<SubjectAttendance> {
        val cachedJson = prefs.getLastGoodSubjectModel()
        if (cachedJson.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<SubjectSummary>>() {}.type
            val list: List<SubjectSummary>? = gson.fromJson(cachedJson, type)
            list?.map { it.toSubjectAttendance() } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    internal fun parseSubjectsFromMonthly(monthlyJson: String?): List<SubjectAttendance> {
        val parsed = parseMonthlySubjectSummaries(monthlyJson)
        if (parsed.isNotEmpty()) {
            prefs.saveLastGoodSubjectModel(gson.toJson(parsed))
            return parsed.map { it.toSubjectAttendance() }
        }
        return loadLastGoodSubjectModel()
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

        private fun JsonObject.stringOrNull(vararg keys: String): String? {
            for (key in keys) {
                val el: JsonElement = get(key) ?: continue
                if (el.isJsonPrimitive) {
                    val str = el.asString.trim()
                    if (str.isNotEmpty()) return str
                }
            }
            return null
        }

        private fun JsonObject.intOrNull(vararg keys: String): Int? {
            for (key in keys) {
                val el: JsonElement = get(key) ?: continue
                if (el.isJsonPrimitive) {
                    try {
                        return el.asInt
                    } catch (_: Exception) {}
                }
            }
            return null
        }

        /**
         * Parses monthly aggregate API JSON into a fallback list of [SubjectSummary] items.
         * Supports explicit subject-level arrays (`subjects`, `subject_wise`, `subject_summary`)
         * as well as the standard `months` aggregate breakdown when detailed logs are unavailable.
         */
        fun parseMonthlySubjectSummaries(monthlyJson: String?): List<SubjectSummary> {
            if (monthlyJson.isNullOrBlank()) return emptyList()
            return try {
                val root = JsonParser.parseString(monthlyJson)
                if (!root.isJsonObject) return emptyList()
                val rootObj = root.asJsonObject
                val dataObj = rootObj.get("data")?.takeIf { it.isJsonObject }?.asJsonObject ?: rootObj

                val subjectArray = listOf("subjects", "subject_wise", "subject_summary")
                    .firstNotNullOfOrNull { key -> dataObj.get(key)?.takeIf { it.isJsonArray }?.asJsonArray }

                if (subjectArray != null) {
                    val parsedSubjects = subjectArray.mapNotNull { element ->
                        if (!element.isJsonObject) return@mapNotNull null
                        val obj = element.asJsonObject
                        val name = obj.stringOrNull("subject_name", "subjectName", "name")
                            ?: return@mapNotNull null
                        val present = (obj.intOrNull("present", "presentCount", "attended") ?: 0).coerceAtLeast(0)
                        val total = (obj.intOrNull("total", "totalClasses", "conducted") ?: present).coerceAtLeast(present)
                        val absent = (obj.intOrNull("absent", "absentCount") ?: (total - present)).coerceAtLeast(0)
                        val code = obj.stringOrNull("subject_code", "subjectCode", "subject_id") ?: ""
                        val teacher = obj.stringOrNull("teacher_name", "faculty_name", "teacherName")
                        SubjectSummary(
                            subjectName = name,
                            present = present,
                            absent = absent,
                            total = total,
                            subjectCode = code,
                            teacherName = teacher
                        )
                    }
                    if (parsedSubjects.isNotEmpty()) return parsedSubjects
                }

                val monthsArray = dataObj.get("months")?.takeIf { it.isJsonArray }?.asJsonArray
                    ?: return emptyList()

                monthsArray.mapNotNull { element ->
                    if (!element.isJsonObject) return@mapNotNull null
                    val obj = element.asJsonObject
                    val year = obj.intOrNull("year") ?: 0
                    val monthNum = obj.intOrNull("month_number", "monthNumber") ?: 0
                    val label = obj.stringOrNull("month_label", "monthLabel")
                    val present = (obj.intOrNull("present") ?: 0).coerceAtLeast(0)
                    val total = (obj.intOrNull("total") ?: 0).coerceAtLeast(0)
                    if (total <= 0 && present <= 0) return@mapNotNull null

                    val name = label ?: if (monthNum > 0 && year > 0) {
                        "Monthly Summary ($monthNum/$year)"
                    } else {
                        "Monthly Summary"
                    }
                    val code = if (year > 0 && monthNum > 0) "$year-$monthNum" else ""
                    val absent = (obj.intOrNull("absent") ?: (total - present)).coerceAtLeast(0)
                    SubjectSummary(
                        subjectName = name,
                        present = present,
                        absent = absent,
                        total = total,
                        subjectCode = code
                    )
                }
            } catch (_: Exception) {
                emptyList()
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
