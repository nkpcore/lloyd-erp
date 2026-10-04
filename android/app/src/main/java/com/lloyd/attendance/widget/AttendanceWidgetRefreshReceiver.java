package com.lloyd.attendance.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Dedicated internal BroadcastReceiver for home screen widget manual refresh triggers.
 *
 * Security Remediations (SEC-FINDING-006):
 * 1. Marked android:exported="false" in AndroidManifest.xml to block external app IPC triggers.
 * 2. Protected by signature-level permission com.lloyd.attendance.permission.WIDGET_REFRESH.
 * 3. Enforces internal package match check on incoming intents.
 * 4. Delegates to AttendanceWidgetProvider.handleRefreshRequest() with rate-limiting debounce.
 */
public class AttendanceWidgetRefreshReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || context == null) {
            return;
        }

        String action = intent.getAction();
        if (AttendanceWidgetProvider.ACTION_REFRESH_WIDGET.equals(action)) {
            // Strict defense-in-depth: check sender package when specified
            String pkg = intent.getPackage();
            if (pkg != null && !pkg.equals(context.getPackageName())) {
                return;
            }
            AttendanceWidgetProvider.handleRefreshRequest(context);
        }
    }
}
