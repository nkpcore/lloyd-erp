package com.lloyd.attendance.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
            // Strict defense-in-depth: reject null package or sender package mismatch
            val callerPkg = intent.`package` ?: return
            if (callerPkg != context.packageName) return

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    AttendanceWidgetProvider.handleRefreshRequest(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
