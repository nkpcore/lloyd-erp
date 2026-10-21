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
import com.lloyd.attendance.core.data.SyncError
import com.lloyd.attendance.core.notification.AttendanceChangeDetector
import com.lloyd.attendance.core.notification.AttendanceMarkEvent
import com.lloyd.attendance.core.telemetry.TelemetryManager
import com.lloyd.attendance.data.AppPreferences
import java.util.concurrent.TimeUnit

/**
 * Background worker responsible for periodic sync and live attendance alerts.
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
                WorkManager.getInstance(context).cancelAllWork()
                AttendanceWidgetProvider.updateAllWidgets(context, null, false, "Session expired")
                return Result.failure()
            }

            val freshStats: Models.CalculatedStats? = prefs.getCachedStats()

            try {
                ErpApiClient(context).getWeeklyAttendance() // Cache latest timetable routine
            } catch (_: Exception) {}

            // 3. Update all widgets on home screen
            AttendanceWidgetProvider.updateAllWidgets(context, freshStats, false, null)

            // 4. Check if faculty has marked any new attendance records using reconciled logs
            checkForNewAttendanceMarks(
                context,
                prefs,
                snapshot.records,
                freshStats,
                prefs.isNotificationsEnabled()
            )

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /**
     * Inspects attendance logs and notifies when a new Present or Absent record has been added.
     * Prevents false alerts on first install/login via bootstrap initialization.
     * Always tracks seen IDs so re-enabling notifications later does not cause a burst.
     */
    private fun checkForNewAttendanceMarks(
        context: Context,
        prefs: AppPreferences,
        logs: List<Models.StudentAttendanceItem>?,
        freshStats: Models.CalculatedStats?,
        notifyUser: Boolean
    ) {
        try {
            if (logs.isNullOrEmpty()) return

            val seenIds = prefs.getSeenAttendanceIds()
            val isInitialBootstrap = !prefs.hasInitializedAttendanceHistory() || seenIds.isEmpty()

            if (isInitialBootstrap) {
                // First run: bootstrap history with existing records so we NEVER alert for past historical lectures
                val currentIds = mutableSetOf<Long>()
                var maxId = 0L
                logs.forEach { item ->
                    if (item.id > 0) {
                        currentIds.add(item.id)
                        if (item.id > maxId) maxId = item.id
                    }
                }
                prefs.addSeenAttendanceIds(currentIds)
                if (maxId > 0) prefs.saveLastSeenAttendanceId(maxId)
                prefs.setInitializedAttendanceHistory(true)
                return
            }

            // Subsequent sync: identify genuine newly marked records via pure domain detector
            val newlyMarkedItems: List<AttendanceMarkEvent.Marked> =
                AttendanceChangeDetector.detectNewMarks(logs, seenIds, false)

            if (newlyMarkedItems.isEmpty()) return

            val newlySeenIds = mutableSetOf<Long>()
            val currentOverallPct = freshStats?.overallPercentage ?: 0.0

            newlyMarkedItems.forEach { newMark ->
                if (notifyUser) {
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
                        subjectId = null
                    )
                }
                newlySeenIds.add(newMark.attendanceId)
            }

            // Persist the newly seen record IDs
            prefs.addSeenAttendanceIds(newlySeenIds)
            val highestNewId = newlyMarkedItems.last().attendanceId
            if (highestNewId > prefs.getLastSeenAttendanceId()) {
                prefs.saveLastSeenAttendanceId(highestNewId)
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
