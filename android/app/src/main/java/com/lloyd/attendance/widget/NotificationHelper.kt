package com.lloyd.attendance.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.lloyd.attendance.R
import com.lloyd.attendance.ui.MainComposeActivity
import java.util.Locale
import kotlin.math.abs

object NotificationHelper {

    const val CHANNEL_ID = "lloyd_attendance_marks"
    private const val CHANNEL_NAME = "Live Attendance Alerts"

    // Deep-link intent extras — consumed by MainComposeActivity on cold + warm start
    const val EXTRA_NAV_TARGET      = "EXTRA_NAV_TARGET"
    const val EXTRA_ATTENDANCE_DATE = "EXTRA_ATTENDANCE_DATE"
    const val EXTRA_RECORD_ID       = "EXTRA_RECORD_ID"
    const val EXTRA_SUBJECT_ID      = "EXTRA_SUBJECT_ID"

    /** Deep-link target value for the Logs tab */
    const val NAV_TARGET_LOGS = "LOGS"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as? NotificationManager ?: return

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Instant alerts when faculty marks attendance as Present or Absent"
                enableLights(true)
                lightColor = Color.parseColor("#1E40AF")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Simple overload — no deep-link extras beyond NAV_TARGET=LOGS.
     * Uses current time as a fallback notification ID.
     */
    @JvmStatic
    fun showAttendanceMarkedNotification(
        context: Context,
        subjectName: String?,
        teacherName: String?,
        lectureDetails: String?,
        isPresent: Boolean,
        overallPercentage: Double,
    ) {
        showAttendanceMarkedNotification(
            context = context,
            attendanceId = System.currentTimeMillis(),
            subjectName = subjectName,
            teacherName = teacherName,
            lectureDetails = lectureDetails,
            isPresent = isPresent,
            overallPercentage = overallPercentage,
            recordDate = null,
            recordId = null,
            subjectId = null,
        )
    }

    /**
     * Legacy overload with explicit [attendanceId] but no extra deep-link context.
     */
    @JvmStatic
    fun showAttendanceMarkedNotification(
        context: Context,
        attendanceId: Long,
        subjectName: String?,
        teacherName: String?,
        lectureDetails: String?,
        isPresent: Boolean,
        overallPercentage: Double,
    ) {
        showAttendanceMarkedNotification(
            context = context,
            attendanceId = attendanceId,
            subjectName = subjectName,
            teacherName = teacherName,
            lectureDetails = lectureDetails,
            isPresent = isPresent,
            overallPercentage = overallPercentage,
            recordDate = null,
            recordId = null,
            subjectId = null,
        )
    }

    /**
     * Rich overload with full deep-link context.
     *
     * @param recordDate ISO-8601 date string (e.g. "2024-11-15") for pre-filtering the Logs screen.
     * @param recordId   Server attendance record ID for direct lookup.
     * @param subjectId  Subject identifier to pre-filter the Logs screen.
     */
    @JvmStatic
    @JvmOverloads
    fun showAttendanceMarkedNotification(
        context: Context,
        attendanceId: Long,
        subjectName: String?,
        teacherName: String?,
        lectureDetails: String?,
        isPresent: Boolean,
        overallPercentage: Double,
        recordDate: String?,
        recordId: String?,
        subjectId: String?,
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as? NotificationManager ?: return

        createNotificationChannel(context)

        val intent = Intent(context, MainComposeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Deep-link extras: MainComposeActivity reads these in handleDeepLinkIntent()
            putExtra(EXTRA_NAV_TARGET, NAV_TARGET_LOGS)
            if (!recordDate.isNullOrEmpty()) putExtra(EXTRA_ATTENDANCE_DATE, recordDate)
            if (!recordId.isNullOrEmpty())   putExtra(EXTRA_RECORD_ID, recordId)
            if (!subjectId.isNullOrEmpty())  putExtra(EXTRA_SUBJECT_ID, subjectId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (abs(attendanceId) % Int.MAX_VALUE).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val cleanSubject = subjectName?.trim()?.takeIf { it.isNotEmpty() } ?: "Class Lecture"
        val facultyStr   = teacherName?.trim()?.takeIf { it.isNotEmpty() } ?: "Assigned Faculty"
        val statusTag    = if (isPresent) "MARKED PRESENT" else "MARKED ABSENT"
        val statusColor  = if (isPresent) "#34D399" else "#F87171"

        // Standard text for Wear OS, lock screen, and accessibility services
        val titleText   = "$statusTag: $cleanSubject"
        val detailPart  = if (!lectureDetails.isNullOrEmpty()) " • $lectureDetails" else ""
        val contentText = "By $facultyStr$detailPart • Overall: ${String.format(Locale.US, "%.1f%%", overallPercentage)}"

        // RemoteViews for Dynamic Island-style heads-up banner
        val views = RemoteViews(context.packageName, R.layout.notification_dynamic).apply {
            setTextViewText(R.id.notif_subject, cleanSubject)
            setTextViewText(
                R.id.notif_teacher_room,
                "Faculty: $facultyStr$detailPart",
            )
            setTextViewText(
                R.id.notif_overall_pct,
                String.format(Locale.US, "%.1f%%", overallPercentage),
            )
            setTextColor(
                R.id.notif_overall_pct,
                if (overallPercentage >= 75.0) Color.parseColor("#10B981") else Color.parseColor("#EF4444"),
            )
            setTextViewText(R.id.notif_header_tag, statusTag)
            setTextColor(R.id.notif_header_tag, Color.parseColor(statusColor))
            setImageViewResource(
                R.id.notif_status_icon,
                if (isPresent) R.drawable.ic_check else R.drawable.ic_alert,
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_school)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(views)
            .setCustomHeadsUpContentView(views)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .setAutoCancel(true)
            .build()

        val notifId = if (attendanceId > 0) (attendanceId % 100_000).toInt()
                      else (System.currentTimeMillis() % 100_000).toInt()

        manager.notify(notifId, notification)
    }
}
