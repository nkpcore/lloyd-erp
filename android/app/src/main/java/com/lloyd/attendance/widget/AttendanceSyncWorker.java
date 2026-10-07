package com.lloyd.attendance.widget;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.lloyd.attendance.api.ErpApiClient;
import com.lloyd.attendance.api.Models;
import com.lloyd.attendance.core.access.AccessControlManager;
import com.lloyd.attendance.core.access.AccessDecision;
import com.lloyd.attendance.core.data.AttendanceRepository;
import com.lloyd.attendance.core.data.AttendanceSnapshot;
import com.lloyd.attendance.core.telemetry.TelemetryManager;
import com.lloyd.attendance.data.AppPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Background worker responsible for periodic sync and live attendance alerts.
 * Strictly adheres to zero-spam policy: ONLY fires notifications when a faculty member
 * actually marks a student Present or Absent in the ERP.
 */
public class AttendanceSyncWorker extends Worker {

    public static final String WORK_NAME = "lloyd_attendance_periodic_sync";

    public AttendanceSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppPreferences prefs = AppPreferences.getInstance(context);

        if (!prefs.isLoggedIn()) {
            return Result.success();
        }

        try {
            int studentId = prefs.getStudentId();
            String deviceId = TelemetryManager.getOrCreateDeviceId(context);

            // 1. Fleet revocation check
            AccessDecision decision = AccessControlManager.checkAccessBlocking(
                    context,
                    prefs.getTelemetryEndpoint(),
                    studentId,
                    deviceId,
                    com.lloyd.attendance.BuildConfig.VERSION_CODE
            );

            if (decision instanceof AccessDecision.Revoked) {
                WorkManager.getInstance(context).cancelAllWork();
                AttendanceWidgetProvider.updateAllWidgets(context, null, false, "Access Revoked");
                return Result.failure();
            }

            // 2. Transactional refresh via single source of truth
            AttendanceRepository repository = AttendanceRepository.getInstance(context);
            repository.refreshBlocking(false);

            AttendanceSnapshot snapshot = repository.getSnapshot().getValue();
            if (snapshot.getError() instanceof com.lloyd.attendance.core.data.SyncError.AuthExpired) {
                WorkManager.getInstance(context).cancelAllWork();
                AttendanceWidgetProvider.updateAllWidgets(context, null, false, "Session expired");
                return Result.failure();
            }
            Models.CalculatedStats freshStats = prefs.getCachedStats();

            try {
                ErpApiClient client = new ErpApiClient(context);
                client.getWeeklyAttendance(); // Cache latest timetable routine
            } catch (Exception ignored) {}

            // 3. Update all widgets on home screen
            AttendanceWidgetProvider.updateAllWidgets(context, freshStats, false, null);

            // 4. Check if faculty has marked any new attendance records using reconciled logs
            if (prefs.isNotificationsEnabled()) {
                checkForNewAttendanceMarks(context, prefs, snapshot.getRecords(), freshStats);
            }

            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    /**
     * Inspects attendance logs and only notifies when a new Present or Absent record has been added.
     * Prevents false alerts on first install/login via bootstrap initialization.
     */
    private void checkForNewAttendanceMarks(
            Context context,
            AppPreferences prefs,
            List<Models.StudentAttendanceItem> logs,
            Models.CalculatedStats freshStats
    ) {
        try {
            if (logs == null || logs.isEmpty()) {
                return;
            }

            Set<String> seenIds = prefs.getSeenAttendanceIds();
            boolean isInitialBootstrap = !prefs.hasInitializedAttendanceHistory() || seenIds.isEmpty();

            if (isInitialBootstrap) {
                // First run: bootstrap history with existing records so we NEVER alert for past historical lectures
                Set<Long> currentIds = new HashSet<>();
                long maxId = 0;
                for (Models.StudentAttendanceItem item : logs) {
                    if (item != null && item.id > 0) {
                        currentIds.add(item.id);
                        if (item.id > maxId) {
                            maxId = item.id;
                        }
                    }
                }
                prefs.addSeenAttendanceIds(currentIds);
                if (maxId > 0) {
                    prefs.saveLastSeenAttendanceId(maxId);
                }
                prefs.setInitializedAttendanceHistory(true);
                return;
            }

            // Subsequent sync: identify genuine newly marked records via pure domain detector
            List<com.lloyd.attendance.core.notification.AttendanceMarkEvent.Marked> newlyMarkedItems =
                    com.lloyd.attendance.core.notification.AttendanceChangeDetector.INSTANCE.detectNewMarks(
                            logs,
                            seenIds,
                            false
                    );

            if (newlyMarkedItems.isEmpty()) {
                return;
            }

            Set<Long> newlySeenIds = new HashSet<>();
            double currentOverallPct = freshStats != null ? freshStats.overallPercentage : 0.0;

            for (com.lloyd.attendance.core.notification.AttendanceMarkEvent.Marked newMark : newlyMarkedItems) {
                NotificationHelper.showAttendanceMarkedNotification(
                        context,
                        newMark.getAttendanceId(),
                        newMark.getSubjectName(),
                        newMark.getFacultyName(),
                        newMark.getLectureDetails(),
                        newMark.isPresent(),
                        currentOverallPct
                );

                newlySeenIds.add(newMark.getAttendanceId());
            }

            // Persist the newly seen record IDs
            prefs.addSeenAttendanceIds(newlySeenIds);
            long highestNewId = newlyMarkedItems.get(newlyMarkedItems.size() - 1).getAttendanceId();
            if (highestNewId > prefs.getLastSeenAttendanceId()) {
                prefs.saveLastSeenAttendanceId(highestNewId);
            }

        } catch (Exception ignored) {
            // Silently ignore log parsing errors in background sync
        }
    }

    public static void schedulePeriodicSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                AttendanceSyncWorker.class,
                60, TimeUnit.MINUTES,
                15, TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }
}
