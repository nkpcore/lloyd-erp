package com.lloyd.attendance.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.widget.RemoteViews;
import com.lloyd.attendance.R;
import com.lloyd.attendance.api.ErpApiClient;
import com.lloyd.attendance.api.Models;
import com.lloyd.attendance.data.AppPreferences;
import com.lloyd.attendance.ui.MainActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AttendanceWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH_WIDGET = "com.lloyd.attendance.ACTION_REFRESH_WIDGET";
    private static final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        AppPreferences prefs = AppPreferences.getInstance(context);
        Models.CalculatedStats cached = prefs.getCachedStats();

        for (int widgetId : appWidgetIds) {
            updateWidgetUI(context, appWidgetManager, widgetId, cached, false);
        }

        // Trigger fresh fetch in background
        triggerRefresh(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_REFRESH_WIDGET.equals(intent.getAction())) {
            // Immediately show refreshing state on widget
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, AttendanceWidgetProvider.class);
            int[] allIds = manager.getAppWidgetIds(thisWidget);

            AppPreferences prefs = AppPreferences.getInstance(context);
            Models.CalculatedStats cached = prefs.getCachedStats();

            for (int id : allIds) {
                updateWidgetUI(context, manager, id, cached, true);
            }

            // Perform network update
            triggerRefresh(context);
        }
    }

    public static void triggerRefresh(Context context) {
        final Context appContext = context.getApplicationContext();

        backgroundExecutor.execute(() -> {
            AppPreferences prefs = AppPreferences.getInstance(appContext);
            if (!prefs.isLoggedIn()) {
                updateAllWidgets(appContext, null, false, "Tap to sign in");
                return;
            }

            try {
                ErpApiClient client = new ErpApiClient(appContext);
                Models.CalculatedStats freshStats = client.fetchAndCalculateStats();

                new Handler(Looper.getMainLooper()).post(() -> {
                    updateAllWidgets(appContext, freshStats, false, null);
                });
            } catch (Exception e) {
                // If network failed, fall back to cached data
                Models.CalculatedStats fallback = prefs.getCachedStats();
                new Handler(Looper.getMainLooper()).post(() -> {
                    updateAllWidgets(appContext, fallback, false, "Offline: " + (e.getMessage() != null ? e.getMessage() : "Sync failed"));
                });
            }
        });
    }

    public static void updateAllWidgets(Context context, Models.CalculatedStats stats, boolean isRefreshing, String customStatus) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, AttendanceWidgetProvider.class);
        int[] allIds = manager.getAppWidgetIds(thisWidget);

        for (int id : allIds) {
            updateWidgetUI(context, manager, id, stats, isRefreshing, customStatus);
        }
    }

    private static void updateWidgetUI(Context context, AppWidgetManager appWidgetManager, int widgetId, Models.CalculatedStats stats, boolean isRefreshing) {
        updateWidgetUI(context, appWidgetManager, widgetId, stats, isRefreshing, null);
    }

    private static void updateWidgetUI(Context context, AppWidgetManager appWidgetManager, int widgetId, Models.CalculatedStats stats, boolean isRefreshing, String customStatus) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_attendance);

        // Click on widget -> Open MainActivity
        Intent openAppIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingOpen = PendingIntent.getActivity(
                context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, pendingOpen);

        // Click on refresh button -> Broadcast refresh action
        Intent refreshIntent = new Intent(context, AttendanceWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH_WIDGET);
        PendingIntent pendingRefresh = PendingIntent.getBroadcast(
                context, widgetId, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.btn_widget_refresh, pendingRefresh);

        if (isRefreshing) {
            views.setTextViewText(R.id.tv_widget_updated, "Syncing...");
            views.setTextViewText(R.id.tv_widget_badge, "REFRESHING");
        } else if (stats != null && stats.lastUpdatedMillis > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            views.setTextViewText(R.id.tv_widget_updated, sdf.format(new Date(stats.lastUpdatedMillis)));
        } else {
            views.setTextViewText(R.id.tv_widget_updated, "Live");
        }

        if (stats != null) {
            if (stats.studentName != null && !stats.studentName.isEmpty()) {
                views.setTextViewText(R.id.tv_widget_student_name, stats.studentName.toUpperCase());
            } else {
                views.setTextViewText(R.id.tv_widget_student_name, "LLOYD ERP");
            }

            // Authentic Schedule Resolution via modern TimetableRepository
            try {
                com.lloyd.attendance.core.schedule.TimetableRepository repo = 
                        new com.lloyd.attendance.core.schedule.TimetableRepository(context, new AppPreferences(context), new com.google.gson.Gson());
                com.lloyd.attendance.core.schedule.DayScheduleResult scheduleResult = repo.getScheduleForDay(
                        java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK),
                        com.lloyd.attendance.core.schedule.TimetableRepository.Companion.getCurrentMinutesFromMidnight()
                );

                if (scheduleResult instanceof com.lloyd.attendance.core.schedule.DayScheduleResult.Success) {
                    com.lloyd.attendance.core.schedule.DayScheduleResult.Success success = 
                            (com.lloyd.attendance.core.schedule.DayScheduleResult.Success) scheduleResult;
                    if (success.getActivePeriod() != null) {
                        views.setTextViewText(R.id.tv_widget_classes, "Live: " + success.getActivePeriod().getSubjectName() + " (" + success.getActivePeriod().getRemainingMinutes() + "m)");
                    } else if (success.getNextPeriod() != null) {
                        views.setTextViewText(R.id.tv_widget_classes, "Next: " + success.getNextPeriod().getStartTime() + " " + success.getNextPeriod().getSubjectName());
                    } else {
                        views.setTextViewText(R.id.tv_widget_classes, stats.totalPresent + " / " + stats.totalClasses + " classes attended");
                    }
                } else if (scheduleResult instanceof com.lloyd.attendance.core.schedule.DayScheduleResult.Weekend) {
                    views.setTextViewText(R.id.tv_widget_classes, "Sunday • Campus Closed");
                } else {
                    views.setTextViewText(R.id.tv_widget_classes, stats.totalPresent + " / " + stats.totalClasses + " classes attended");
                }
            } catch (Exception e) {
                views.setTextViewText(R.id.tv_widget_classes, stats.totalPresent + " / " + stats.totalClasses + " classes attended");
            }

            if (stats.totalClasses == 0) {
                views.setTextViewText(R.id.tv_widget_percentage, "--.-%");
                views.setTextColor(R.id.tv_widget_percentage, Color.parseColor("#94A3B8"));
                views.setTextViewText(R.id.tv_widget_badge, "NO RECORDS");
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#94A3B8"));
                views.setTextViewText(R.id.tv_widget_bunk, "No classes recorded yet");
                views.setImageViewResource(R.id.iv_widget_bunk_icon, R.drawable.ic_check);
            } else if (stats.overallPercentage >= 75.0) {
                views.setTextViewText(R.id.tv_widget_percentage, String.format(Locale.US, "%.1f%%", stats.overallPercentage));
                views.setTextColor(R.id.tv_widget_percentage, Color.parseColor("#10B981")); // Emerald
                views.setTextViewText(R.id.tv_widget_badge, "SAFE (>= 75%)");
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#34D399"));
                views.setImageViewResource(R.id.iv_widget_bunk_icon, R.drawable.ic_check);

                if (stats.bunkAllowance > 0) {
                    views.setTextViewText(R.id.tv_widget_bunk, "Can safely bunk " + stats.bunkAllowance + " next class" + (stats.bunkAllowance > 1 ? "es" : ""));
                } else {
                    views.setTextViewText(R.id.tv_widget_bunk, "Safe for now! Don't miss next class.");
                }
            } else {
                views.setTextViewText(R.id.tv_widget_percentage, String.format(Locale.US, "%.1f%%", stats.overallPercentage));
                views.setTextColor(R.id.tv_widget_percentage, Color.parseColor("#EF4444")); // Red
                views.setTextViewText(R.id.tv_widget_badge, "SHORTAGE (< 75%)");
                views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#F87171"));
                views.setImageViewResource(R.id.iv_widget_bunk_icon, R.drawable.ic_alert);

                views.setTextViewText(R.id.tv_widget_bunk, "Attend next " + stats.neededToReach75 + " class" + (stats.neededToReach75 > 1 ? "es" : "") + " to reach 75%");
            }
        } else {
            views.setTextViewText(R.id.tv_widget_student_name, "LLOYD ERP");
            views.setTextViewText(R.id.tv_widget_percentage, "--%");
            views.setTextColor(R.id.tv_widget_percentage, Color.parseColor("#94A3B8"));
            views.setTextViewText(R.id.tv_widget_badge, "NOT LOGGED IN");
            views.setTextColor(R.id.tv_widget_badge, Color.parseColor("#FBBF24"));
            views.setTextViewText(R.id.tv_widget_classes, (customStatus != null && !customStatus.isEmpty()) ? customStatus : "Tap here to log in");
            views.setTextViewText(R.id.tv_widget_bunk, "Sign in to track attendance");
        }

        appWidgetManager.updateAppWidget(widgetId, views);
    }
}
