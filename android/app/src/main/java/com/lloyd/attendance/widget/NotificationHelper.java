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
import com.lloyd.attendance.ui.MainComposeActivity;
import java.util.Locale;

public class NotificationHelper {

    public static final String CHANNEL_ID = "lloyd_attendance_marks";
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
            channel.setDescription("Instant alerts when faculty marks attendance as Present or Absent");
            channel.enableLights(true);
            channel.setLightColor(Color.parseColor("#1E40AF"));
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 250, 150, 250});
            manager.createNotificationChannel(channel);
        }
    }

    public static void showAttendanceMarkedNotification(
            Context context,
            String subjectName,
            String teacherName,
            String lectureDetails,
            boolean isPresent,
            double overallPercentage
    ) {
        showAttendanceMarkedNotification(
                context,
                System.currentTimeMillis(),
                subjectName,
                teacherName,
                lectureDetails,
                isPresent,
                overallPercentage
        );
    }

    public static void showAttendanceMarkedNotification(
            Context context,
            long attendanceId,
            String subjectName,
            String teacherName,
            String lectureDetails,
            boolean isPresent,
            double overallPercentage
    ) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        createNotificationChannel(context);

        Intent intent = new Intent(context, MainComposeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) (Math.abs(attendanceId) % Integer.MAX_VALUE),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String cleanSubject = (subjectName != null && !subjectName.trim().isEmpty())
                ? subjectName.trim()
                : "Class Lecture";

        String facultyStr = (teacherName != null && !teacherName.trim().isEmpty())
                ? teacherName.trim()
                : "Assigned Faculty";

        String statusTag = isPresent ? "MARKED PRESENT" : "MARKED ABSENT";
        String statusColor = isPresent ? "#34D399" : "#F87171";

        // Standard notification text for Smartwatches, Wear OS, Lockscreen, and Accessibility
        String titleText = statusTag + ": " + cleanSubject;
        String contentText = "By " + facultyStr +
                (lectureDetails != null && !lectureDetails.isEmpty() ? " • " + lectureDetails : "") +
                " • Overall: " + String.format(Locale.US, "%.1f%%", overallPercentage);

        // RemoteViews for Apple-styled Dynamic Island banner
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.notification_dynamic);
        views.setTextViewText(R.id.notif_subject, cleanSubject);

        String teacherInfo = "Faculty: " + facultyStr +
                (lectureDetails != null && !lectureDetails.isEmpty() ? " • " + lectureDetails : "");
        views.setTextViewText(R.id.notif_teacher_room, teacherInfo);

        views.setTextViewText(R.id.notif_overall_pct, String.format(Locale.US, "%.1f%%", overallPercentage));
        views.setTextColor(R.id.notif_overall_pct, overallPercentage >= 75.0 ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));

        views.setTextViewText(R.id.notif_header_tag, statusTag);
        views.setTextColor(R.id.notif_header_tag, Color.parseColor(statusColor));

        if (isPresent) {
            views.setImageViewResource(R.id.notif_status_icon, R.drawable.ic_check);
        } else {
            views.setImageViewResource(R.id.notif_status_icon, R.drawable.ic_alert);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_school)
                .setContentTitle(titleText)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(views)
                .setCustomHeadsUpContentView(views)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(new long[]{0, 200, 100, 200})
                .setAutoCancel(true);

        int notifId = attendanceId > 0
                ? (int) (attendanceId % 100000)
                : (int) (System.currentTimeMillis() % 100000);

        manager.notify(notifId, builder.build());
    }
}
