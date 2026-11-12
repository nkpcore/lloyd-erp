package com.lloyd.attendance.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import com.lloyd.attendance.R
import com.lloyd.attendance.api.Models
import com.lloyd.attendance.core.data.AttendanceRepository
import com.lloyd.attendance.core.data.SyncError
import com.lloyd.attendance.data.AppPreferences
import com.lloyd.attendance.ui.MainComposeActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

enum class WidgetAttendanceStatus {
    SAFE,
    SHORTAGE,
    NO_RECORDS,
    UNKNOWN
}

enum class WidgetSyncStatus {
    REFRESHING,
    OFFLINE,
    OK
}

data class WidgetUiModel(
    val attendanceStatus: WidgetAttendanceStatus,
    val syncStatus: WidgetSyncStatus,
    val studentName: String,
    val percentageText: String,
    val percentageColorHex: String,
    val badgeText: String,
    val badgeColorHex: String,
    val syncIndicatorText: String?,
    val updatedText: String,
    val classesText: String,
    val bunkText: String,
    val isAlertIcon: Boolean
)

class AttendanceWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.lloyd.attendance.ACTION_REFRESH_WIDGET"

        /** 15-second debounce against DoS/battery drain */
        const val MIN_REFRESH_INTERVAL_MS = 15_000L

        @Volatile
        private var lastRefreshTimeMillis = 0L

        private val backgroundExecutor = Executors.newSingleThreadExecutor()

        @Synchronized
        fun canRefresh(): Boolean {
            val now = System.currentTimeMillis()
            if (now - lastRefreshTimeMillis < MIN_REFRESH_INTERVAL_MS) return false
            lastRefreshTimeMillis = now
            return true
        }

        @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)
        @Synchronized
        fun resetThrottleForTesting() {
            lastRefreshTimeMillis = 0L
        }

        /**
         * Handles manual widget refresh requests with rate-limiting debounce.
         */
        fun handleRefreshRequest(context: Context) {
            if (!canRefresh()) return

            // Immediately show refreshing state on widget
            val manager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, AttendanceWidgetProvider::class.java)
            val allIds = manager.getAppWidgetIds(thisWidget)

            val prefs = AppPreferences.getInstance(context)
            val cached = prefs.getCachedStats()

            for (id in allIds) {
                updateWidgetUI(context, manager, id, cached, isRefreshing = true)
            }

            // Perform network update
            performRefresh(context)
        }

        fun performRefresh(context: Context) {
            val appContext = context.applicationContext
            val prefs = AppPreferences.getInstance(appContext)
            if (!prefs.isLoggedIn()) {
                updateAllWidgets(appContext, stats = null, isRefreshing = false, customStatus = "Tap to sign in")
                return
            }

            try {
                val repository = AttendanceRepository.getInstance(appContext)
                repository.refreshBlocking(false)

                val snapshot = repository.snapshot.value
                if (snapshot?.error is SyncError.AuthExpired) {
                    Handler(Looper.getMainLooper()).post {
                        updateAllWidgets(appContext, stats = null, isRefreshing = false, customStatus = "Session expired")
                    }
                    return
                }

                val freshStats = prefs.getCachedStats()
                val syncFailureStatus = if (snapshot?.error != null) "Offline: Sync failed" else null
                Handler(Looper.getMainLooper()).post {
                    updateAllWidgets(appContext, freshStats, isRefreshing = false, customStatus = syncFailureStatus)
                }
            } catch (e: Exception) {
                // If network failed, fall back to cached data
                val fallback = prefs.getCachedStats()
                Handler(Looper.getMainLooper()).post {
                    updateAllWidgets(
                        appContext,
                        fallback,
                        isRefreshing = false,
                        customStatus = "Offline: ${e.message ?: "Sync failed"}"
                    )
                }
            }
        }

        fun triggerRefresh(context: Context) {
            backgroundExecutor.execute {
                performRefresh(context)
            }
        }

        fun updateAllWidgets(
            context: Context,
            stats: Models.CalculatedStats?,
            isRefreshing: Boolean,
            customStatus: String?
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, AttendanceWidgetProvider::class.java)
            val allIds = manager.getAppWidgetIds(thisWidget)

            for (id in allIds) {
                updateWidgetUI(context, manager, id, stats, isRefreshing, customStatus)
            }
        }

        fun resolveWidgetUiModel(
            stats: Models.CalculatedStats?,
            isRefreshing: Boolean,
            customStatus: String? = null
        ): WidgetUiModel {
            val attendanceStatus = when {
                stats == null -> WidgetAttendanceStatus.UNKNOWN
                stats.totalClasses == 0 -> WidgetAttendanceStatus.NO_RECORDS
                stats.overallPercentage >= 75.0 -> WidgetAttendanceStatus.SAFE
                else -> WidgetAttendanceStatus.SHORTAGE
            }

            val syncStatus = when {
                isRefreshing -> WidgetSyncStatus.REFRESHING
                !customStatus.isNullOrBlank() -> WidgetSyncStatus.OFFLINE
                else -> WidgetSyncStatus.OK
            }

            val updatedText = when {
                syncStatus == WidgetSyncStatus.REFRESHING -> "Syncing..."
                stats != null && stats.lastUpdatedMillis > 0 -> {
                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    sdf.format(Date(stats.lastUpdatedMillis))
                }
                else -> "Live"
            }

            val syncIndicatorText = when {
                stats == null -> null
                syncStatus == WidgetSyncStatus.REFRESHING -> "↻ Syncing"
                syncStatus == WidgetSyncStatus.OFFLINE -> "⚠ Cached"
                else -> null
            }

            if (stats != null) {
                val displayName = stats.studentName?.takeIf { it.isNotEmpty() }?.uppercase() ?: "LLOYD ERP"
                val classesText = "${stats.totalPresent} / ${stats.totalClasses} classes attended"

                return when (attendanceStatus) {
                    WidgetAttendanceStatus.NO_RECORDS -> WidgetUiModel(
                        attendanceStatus = attendanceStatus,
                        syncStatus = syncStatus,
                        studentName = displayName,
                        percentageText = "--.--%",
                        percentageColorHex = "#C4C7C5",
                        badgeText = "NO RECORDS",
                        badgeColorHex = "#C4C7C5",
                        syncIndicatorText = syncIndicatorText,
                        updatedText = updatedText,
                        classesText = classesText,
                        bunkText = "No classes recorded yet",
                        isAlertIcon = false
                    )
                    WidgetAttendanceStatus.SAFE -> {
                        val bunkMsg = if (stats.bunkAllowance > 0) {
                            val plural = if (stats.bunkAllowance > 1) "es" else ""
                            "Can safely bunk ${stats.bunkAllowance} next class$plural"
                        } else {
                            "Safe for now! Don't miss next class."
                        }
                        WidgetUiModel(
                            attendanceStatus = attendanceStatus,
                            syncStatus = syncStatus,
                            studentName = displayName,
                            percentageText = String.format(Locale.US, "%.1f%%", stats.overallPercentage),
                            percentageColorHex = "#6DD58C",
                            badgeText = "SAFE (>= 75%)",
                            badgeColorHex = "#6DD58C",
                            syncIndicatorText = syncIndicatorText,
                            updatedText = updatedText,
                            classesText = classesText,
                            bunkText = bunkMsg,
                            isAlertIcon = false
                        )
                    }
                    else -> {
                        val plural = if (stats.neededToReach75 > 1) "es" else ""
                        WidgetUiModel(
                            attendanceStatus = attendanceStatus,
                            syncStatus = syncStatus,
                            studentName = displayName,
                            percentageText = String.format(Locale.US, "%.1f%%", stats.overallPercentage),
                            percentageColorHex = "#F2B8B5",
                            badgeText = "SHORTAGE (< 75%)",
                            badgeColorHex = "#F2B8B5",
                            syncIndicatorText = syncIndicatorText,
                            updatedText = updatedText,
                            classesText = classesText,
                            bunkText = "Attend next ${stats.neededToReach75} class$plural to reach 75%",
                            isAlertIcon = true
                        )
                    }
                }
            } else {
                // Only show customStatus when no stats exist at all
                val custom = customStatus?.takeIf { it.isNotBlank() }
                val badgeText = when {
                    isRefreshing -> "REFRESHING"
                    custom != null -> custom.uppercase()
                    else -> "NOT LOGGED IN"
                }
                val badgeColorHex = if (isRefreshing) "#F6BD60" else "#F2B8B5"
                return WidgetUiModel(
                    attendanceStatus = attendanceStatus,
                    syncStatus = syncStatus,
                    studentName = "LLOYD ERP",
                    percentageText = "--%",
                    percentageColorHex = "#C4C7C5",
                    badgeText = badgeText,
                    badgeColorHex = badgeColorHex,
                    syncIndicatorText = null,
                    updatedText = updatedText,
                    classesText = custom ?: "Tap here to log in",
                    bunkText = if (isRefreshing) "Syncing attendance..." else "Sign in to track attendance",
                    isAlertIcon = false
                )
            }
        }

        private fun updateWidgetUI(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            stats: Models.CalculatedStats?,
            isRefreshing: Boolean,
            customStatus: String? = null
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_attendance)

            // Click on widget → Open MainComposeActivity
            val openAppIntent = Intent(context, MainComposeActivity::class.java)
            val pendingOpen = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingOpen)

            // Click on refresh button → Broadcast refresh action to internal secured receiver (SEC-FINDING-006)
            val refreshIntent = Intent(context, AttendanceWidgetRefreshReceiver::class.java).apply {
                action = ACTION_REFRESH_WIDGET
                setPackage(context.packageName)
            }
            val pendingRefresh = PendingIntent.getBroadcast(
                context, widgetId, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_refresh, pendingRefresh)

            val uiModel = resolveWidgetUiModel(stats, isRefreshing, customStatus)

            views.setTextViewText(R.id.tv_widget_updated, uiModel.updatedText)
            views.setTextViewText(R.id.tv_widget_student_name, uiModel.studentName)
            views.setTextViewText(R.id.tv_widget_percentage, uiModel.percentageText)
            views.setTextColor(R.id.tv_widget_percentage, Color.parseColor(uiModel.percentageColorHex))
            views.setTextViewText(R.id.tv_widget_badge, uiModel.badgeText)
            views.setTextColor(R.id.tv_widget_badge, Color.parseColor(uiModel.badgeColorHex))
            views.setTextViewText(R.id.tv_widget_classes, uiModel.classesText)
            views.setTextViewText(R.id.tv_widget_bunk, uiModel.bunkText)
            views.setImageViewResource(
                R.id.iv_widget_bunk_icon,
                if (uiModel.isAlertIcon) R.drawable.ic_alert else R.drawable.ic_check
            )

            if (!uiModel.syncIndicatorText.isNullOrBlank()) {
                views.setTextViewText(R.id.tv_widget_sync_status, uiModel.syncIndicatorText)
                views.setViewVisibility(R.id.tv_widget_sync_status, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.tv_widget_sync_status, View.GONE)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = AppPreferences.getInstance(context)
        val cached = prefs.getCachedStats()

        for (widgetId in appWidgetIds) {
            updateWidgetUI(context, appWidgetManager, widgetId, cached, isRefreshing = false)
        }

        // Trigger fresh fetch in background
        triggerRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)

        if (intent?.action == ACTION_REFRESH_WIDGET) {
            val callingPkg = context.packageManager.getNameForUid(android.os.Binder.getCallingUid())
            if (callingPkg != context.packageName) return

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    handleRefreshRequest(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
