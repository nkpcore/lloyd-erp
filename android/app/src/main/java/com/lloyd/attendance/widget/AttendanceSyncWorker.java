package com.lloyd.attendance.widget;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.lloyd.attendance.R;
import com.lloyd.attendance.api.ErpApiClient;
import com.lloyd.attendance.api.Models;
import com.lloyd.attendance.data.AppPreferences;
import com.lloyd.attendance.ui.MainActivity;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AttendanceSyncWorker extends Worker {

    public static final String WORK_NAME = "lloyd_attendance_periodic_sync";
    public static final String CHANNEL_ID = "lloyd_attendance_channel";

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
            ErpApiClient client = new ErpApiClient(context);
            Models.CalculatedStats oldStats = prefs.getCachedStats();

            Models.CalculatedStats freshStats = client.fetchAndCalculateStats();
            client.getWeeklyAttendance(); // Cache latest timetable

            java.util.List<Models.StudentAttendanceItem> logs = client.getStudentAttendanceLogs(0);
            if (logs != null && !logs.isEmpty()) {
                Models.StudentAttendanceItem latest = logs.get(0);
                long lastSeenId = prefs.getLastSeenAttendanceId();
                if (lastSeenId > 0 && latest.id != lastSeenId && prefs.isNotificationsEnabled()) {
                    boolean isPresent = "Present".equalsIgnoreCase(latest.status);
                    NotificationHelper.showAttendanceMarkedNotification(
                            context,
                            latest.subjectName != null ? latest.subjectName : "Lecture",
                            latest.createdByName != null ? latest.createdByName : "Faculty",
                            latest.classLecture != null && !latest.classLecture.isEmpty() ? "Lecture #" + latest.classLecture : "",
                            isPresent,
                            freshStats != null ? freshStats.overallPercentage : 75.0
                    );
                }
                prefs.saveLastSeenAttendanceId(latest.id);
            }

            // Update all widgets on home screen
            AttendanceWidgetProvider.updateAllWidgets(context, freshStats, false, null);

            // Check if we need to notify student
            if (prefs.isNotificationsEnabled() && freshStats != null) {
                checkAndSendNotification(context, oldStats, freshStats);
            }

            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    private void checkAndSendNotification(Context context, Models.CalculatedStats oldStats, Models.CalculatedStats freshStats) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        createNotificationChannel(context, manager);

        String title = null;
        String message = null;

        if (freshStats.overallPercentage < 75.0) {
            title = String.format(Locale.US, "⚠️ Attendance Alert: %.1f%%", freshStats.overallPercentage);
            message = "Shortage alert! You need to attend next " + freshStats.neededToReach75 + " classes to reach 75%.";
        } else if (freshStats.overallPercentage >= 75.0 && freshStats.overallPercentage <= 76.5) {
            title = String.format(Locale.US, "⚡ Safe Margin Warning: %.1f%%", freshStats.overallPercentage);
            message = "You are close to the 75% threshold! Missing a single class may cause attendance shortage.";
        } else if (oldStats != null && freshStats.totalClasses > oldStats.totalClasses) {
            int newClasses = freshStats.totalClasses - oldStats.totalClasses;
            int newPresent = freshStats.totalPresent - oldStats.totalPresent;
            boolean markedPresent = newPresent > 0;
            NotificationHelper.showAttendanceMarkedNotification(
                    context,
                    "Attendance Updated",
                    (markedPresent ? "Marked Present" : "Marked Absent") + " (" + newClasses + " new class)",
                    "",
                    markedPresent,
                    freshStats.overallPercentage
            );
            return;
        }

        if (title != null && message != null) {
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pending = PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_school)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pending)
                    .setAutoCancel(true);

            manager.notify(1001, builder.build());
        }
    }

    private void createNotificationChannel(Context context, NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Attendance & Bunk Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alerts when attendance changes or approaches 75% threshold");
            manager.createNotificationChannel(channel);
        }
    }

    public static void schedulePeriodicSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                AttendanceSyncWorker.class,
                60, TimeUnit.MINUTES, // Run every hour
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
