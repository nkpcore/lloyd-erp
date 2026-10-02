package com.lloyd.attendance.widget;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.widget.RemoteViews;
import androidx.core.app.NotificationCompat;
import com.lloyd.attendance.R;
import com.lloyd.attendance.ui.MainActivity;
import java.util.Locale;

public class NotificationHelper {

    public static final String CHANNEL_ID = "lloyd_attendance_dynamic_island";
    private static final String CHANNEL_NAME = "Live Attendance Alerts";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Instant Apple Dynamic Island-styled alerts when faculty marks attendance");
            channel.enableLights(true);
            channel.setLightColor(Color.parseColor("#4F46E5"));
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 250, 150, 250});
            manager.createNotificationChannel(channel);
        }
    }

    public static void showAttendanceMarkedNotification(
            Context context,
            String subjectName,
            String teacherName,
            String roomNo,
            boolean isPresent,
            double overallPercentage
    ) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        createNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // RemoteViews for Apple-styled Dynamic Island banner
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.notification_dynamic);
        views.setTextViewText(R.id.notif_subject, subjectName != null ? subjectName : "Class Lecture");

        String teacherInfo = (teacherName != null && !teacherName.isEmpty() ? "Faculty: " + teacherName : "Faculty: Assigned Teacher") +
                (roomNo != null && !roomNo.isEmpty() ? " • Room " + roomNo : "");
        views.setTextViewText(R.id.notif_teacher_room, teacherInfo);

        views.setTextViewText(R.id.notif_overall_pct, String.format(Locale.US, "%.1f%%", overallPercentage));
        views.setTextColor(R.id.notif_overall_pct, overallPercentage >= 75.0 ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));

        if (isPresent) {
            views.setTextViewText(R.id.notif_header_tag, "MARKED PRESENT ✅");
            views.setTextColor(R.id.notif_header_tag, Color.parseColor("#34D399"));
            views.setImageViewResource(R.id.notif_status_icon, R.drawable.ic_check);
        } else {
            views.setTextViewText(R.id.notif_header_tag, "MARKED ABSENT ❌");
            views.setTextColor(R.id.notif_header_tag, Color.parseColor("#F87171"));
            views.setImageViewResource(R.id.notif_status_icon, R.drawable.ic_alert);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_school)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(views)
                .setCustomHeadsUpContentView(views)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(new long[]{0, 200, 100, 200})
                .setAutoCancel(true);

        int notifId = (int) (System.currentTimeMillis() % 100000);
        manager.notify(notifId, builder.build());
    }
}
