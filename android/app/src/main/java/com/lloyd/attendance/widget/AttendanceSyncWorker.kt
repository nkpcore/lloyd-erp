package com.lloyd.attendance.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.lloyd.attendance.BuildConfig
import com.lloyd.attendance.api.ErpApiClient
import com.lloyd.attendance.api.Models
import com.lloyd.attendance.core.access.AccessControlManager
import com.lloyd.attendance.core.access.AccessDecision
import com.lloyd.attendance.core.data.AttendanceRepository
import com.lloyd.attendance.core.data.DataSourceState
import com.lloyd.attendance.core.data.SyncError
import com.lloyd.attendance.core.notification.NotificationSyncCoordinator
import com.lloyd.attendance.core.telemetry.TelemetryManager
import com.lloyd.attendance.data.AppPreferences
import java.util.concurrent.TimeUnit

/**
 * Background worker responsible for periodic sync and background attendance alerts.
 * Strictly adheres to zero-spam policy: ONLY fires notifications when a faculty member
 * actually marks a student Present or Absent in the ERP.
 */
class AttendanceSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val context = applicationContext
        val prefs = AppPreferences.getInstance(context)

        if (!prefs.isLoggedIn()) return Result.success()

        return try {
            val studentId = prefs.getStudentId()
            val deviceId = TelemetryManager.getOrCreateDeviceId(context)

            // 1. Fleet revocation check
            val decision = AccessControlManager.checkAccessBlocking(
                context,
                prefs.getTelemetryEndpoint(),
                studentId,
                deviceId,
                BuildConfig.VERSION_CODE
            )

            when (decision) {
                is AccessDecision.Revoked -> {
                    WorkManager.getInstance(context).cancelAllWork()
                    AttendanceWidgetProvider.updateAllWidgets(context, null, false, "Access Revoked")
                    return Result.failure()
                }
                is AccessDecision.Maintenance -> {
                    AttendanceWidgetProvider.updateAllWidgets(context, null, false, "Under Maintenance")
                    return Result.retry()
                }
                is AccessDecision.OutdatedVersion -> {
                    AttendanceWidgetProvider.updateAllWidgets(context, null, false, "Update App")
                    return Result.failure()
                }
                else -> Unit
            }

            // 2. Transactional refresh via single source of truth
            val repository = AttendanceRepository.getInstance(context)
            repository.refreshBlocking(false)

            val snapshot = repository.snapshot.value
            if (snapshot.error is SyncError.AuthExpired) {
                prefs.setAuthExpired(true)
                AttendanceWidgetProvider.updateAllWidgets(context, null, false, "Session expired")
                return Result.retry()
            }

            val freshStats: Models.CalculatedStats? = prefs.getCachedStats()

            try {
                ErpApiClient(context).getWeeklyAttendance() // Cache latest timetable routine
            } catch (_: Exception) {}

            // 3. Update all widgets on home screen
            val syncFailureStatus = if (snapshot.error != null) "Offline: Sync failed" else null
            AttendanceWidgetProvider.updateAllWidgets(context, freshStats, false, syncFailureStatus)

            // 4. Check if faculty has marked any new attendance records using reconciled logs
            val syncSucceeded = snapshot.error == null && snapshot.sourceState == DataSourceState.FRESH
            checkForNewAttendanceMarks(
                context,
                prefs,
                snapshot.records,
                freshStats,
                prefs.isNotificationsEnabled(),
                syncSucceeded
            )

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /**
     * Inspects attendance logs and notifies when a new Present or Absent record has been added.
     * Handles empty-start bootstrap safely and tracks detected vs delivered IDs separately.
     */
    private fun checkForNewAttendanceMarks(
        context: Context,
        prefs: AppPreferences,
        logs: List<Models.StudentAttendanceItem>?,
        freshStats: Models.CalculatedStats?,
        notifyUser: Boolean,
        syncSucceeded: Boolean
    ) {
        try {
            // Fleet remote kill-switch check: if disabled from admin portal, suppress all alerts
            val remoteConfig = AccessControlManager.getCachedFleetConfig(context)
            val fleetEnabled = remoteConfig?.notificationsEnabled ?: true
            val shouldNotify = notifyUser && fleetEnabled
            val currentOverallPct = freshStats?.overallPercentage ?: 0.0

            val summary = NotificationSyncCoordinator.processLogs(
                store = prefs,
                logs = logs,
                shouldNotify = shouldNotify,
                syncSucceeded = syncSucceeded
            ) { newMark ->
                NotificationHelper.showAttendanceMarkedNotification(
                    context = context,
                    attendanceId = newMark.attendanceId,
                    subjectName = newMark.subjectName,
                    teacherName = newMark.facultyName,
                    lectureDetails = newMark.lectureDetails,
                    isPresent = newMark.isPresent,
                    overallPercentage = currentOverallPct,
                    recordDate = newMark.attendanceDate,
                    recordId = newMark.attendanceId.toString(),
                    subjectId = newMark.subjectId?.toString()
                )
            }

            if (summary.deliveredIds.size > 1) {
                NotificationHelper.showGroupSummaryNotification(
                    context = context,
                    totalCount = summary.deliveredIds.size,
                    summaryText = "${summary.deliveredIds.size} new attendance marked"
                )
            }
        } catch (_: Exception) {
            // Silently ignore log parsing errors in background sync
        }
    }

    companion object {
        const val WORK_NAME = "lloyd_attendance_periodic_sync"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequest.Builder(
                AttendanceSyncWorker::class.java,
                60, TimeUnit.MINUTES,
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
 