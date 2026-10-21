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
            triggerRefresh(context)
        }

        fun triggerRefresh(context: Context) {
            val appContext = context.applicationContext

            backgroundExecutor.execute {
                val prefs = AppPreferences.getInstance(appContext)
                if (!prefs.isLoggedIn()) {
                    updateAllWidgets(appContext, stats = null, isRefreshing = false, customStatus = "Tap to sign in")
                    return@execute
                }

                try {
                    val repository = AttendanceRepository.getInstance(appContext)
                    repository.refreshBlocking(false)

                    val snapshot = repository.snapshot.value
                    if (snapshot?.error is SyncError.AuthExpired) {
                        Handler(Looper.getMainLooper()).post {
                            updateAllWidgets(appContext, stats = null, isRefreshing = false, customStatus = "Session expired")
                        }
                        return@execute
                    }

                    val freshStats = prefs.getCachedStats()
                    Handler(Looper.getMainLooper()).post {
                        updateAllWidgets(appContext, freshStats, isRefreshing = false, customStatus = null)
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

            when {
                isRefreshing -> {
                    views.setTextViewText(R.id.tv_widget_updated, "Syncing...")
                    views.setTextViewText(R.id.tv_widget_badge, "REFRESHING")
                }
                stats != null && stats.lastUpdatedMillis > 0 -> {
                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    views.setTextViewText(R.id.tv_widget_updated, sdf.format(Date(stats.lastUpdatedMillis)))
                }
                else -> {
                    views.setTextViewText(R.id.tv_widget_updated, "Live")
                }
            }

            if (stats != null) {
                val displayName = stats.studentName?.takeIf { it.isNotEmpty() }?.uppercase() ?: "LLOYD ERP"
                views.setTextViewText(R.id.tv_widget_student_name, displayName)
                views.setTextViewText(
                    R.id.tv_widget_classes,
                    "${stats.totalPresent} / ${stats.totalClasses} classes attended"
                )

                when {
                    stats.totalClasses == 0 -> {
                        views.setTextViewText(R.id.tv_widget_percentage, "--.--%")
                        views.setTextColor(R.id.tv_widget_percentage, Color.parseColor("#C4C7C5"))
                        views.setTextViewText(R.id.tv_widget_badge, "NO RECORDS")
                        views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#C4C7C5"))
                        views.setTextViewText(R.id.tv_widget_bunk, "No classes recorded yet")
                        views.setImageViewResource(R.id.iv_widget_bunk_icon, R.drawable.ic_check)
                    }
                    stats.overallPercentage >= 75.0 -> {
                        views.setTextViewText(
                            R.id.tv_widget_percentage,
                            String.format(Locale.US, "%.1f%%", stats.overallPercentage)
                        )
                        views.setTextColor(R.id.tv_widget_percentage, Color.parseColor("#6DD58C")) // M3 Safe Green
                        views.setTextViewText(R.id.tv_widget_badge, "SAFE (>= 75%)")
                        views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#6DD58C"))
                        views.setImageViewResource(R.id.iv_widget_bunk_icon, R.drawable.ic_check)

                        val bunkMsg = if (stats.bunkAllowance > 0) {
                            val plural = if (stats.bunkAllowance > 1) "es" else ""
                            "Can safely bunk ${stats.bunkAllowance} next class$plural"
                        } else {
                            "Safe for now! Don't miss next class."
                        }
                        views.setTextViewText(R.id.tv_widget_bunk, bunkMsg)
                    }
                    else -> {
                        views.setTextViewText(
                            R.id.tv_widget_percentage,
                            String.format(Locale.US, "%.1f%%", stats.overallPercentage)
                        )
                        views.setTextColor(R.id.tv_widget_percentage, Color.parseColor("#F2B8B5")) // M3 Danger Red
                        views.setTextViewText(R.id.tv_widget_badge, "SHORTAGE (< 75%)")
                        views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#F2B8B5"))
                        views.setImageViewResource(R.id.iv_widget_bunk_icon, R.drawable.ic_alert)

                        val plural = if (stats.neededToReach75 > 1) "es" else ""
                        views.setTextViewText(
                            R.id.tv_widget_bunk,
                            "Attend next ${stats.neededToReach75} class$plural to reach 75%"
                        )
                    }
                }
            } else {
                views.setTextViewText(R.id.tv_widget_student_name, "LLOYD ERP")
                views.setTextViewText(R.id.tv_widget_percentage, "--%")
                views.setTextColor(R.id.tv_widget_percentage, Color.parseColor("#C4C7C5"))
                views.setTextViewText(R.id.tv_widget_badge, "NOT LOGGED IN")
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#F2B8B5"))
                views.setTextViewText(
                    R.id.tv_widget_classes,
                    customStatus?.takeIf { it.isNotEmpty() } ?: "Tap here to log in"
                )
                views.setTextViewText(R.id.tv_widget_bunk, "Sign in to track attendance")
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
            handleRefreshRequest(context)
        }
    }
}
