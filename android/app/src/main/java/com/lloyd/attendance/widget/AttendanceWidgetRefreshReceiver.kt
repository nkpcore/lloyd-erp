package com.lloyd.attendance.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Dedicated internal BroadcastReceiver for home screen widget manual refresh triggers.
 *
 * Security Remediations (SEC-FINDING-006):
 * 1. Marked android:exported="false" in AndroidManifest.xml to block external app IPC triggers.
 * 2. Protected by signature-level permission com.lloyd.attendance.permission.WIDGET_REFRESH.
 * 3. Enforces internal package match check on incoming intents.
 * 4. Delegates to AttendanceWidgetProvider.handleRefreshRequest() with rate-limiting debounce.
 */
class AttendanceWidgetRefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == AttendanceWidgetProvider.ACTION_REFRESH_WIDGET) {
            // Strict defense-in-depth: reject if sender package is explicitly set and doesn't match
            val pkg = intent.`package`
            if (pkg != null && pkg != context.packageName) return
            AttendanceWidgetProvider.handleRefreshRequest(context)
        }
    }
}
