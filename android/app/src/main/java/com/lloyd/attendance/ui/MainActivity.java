package com.lloyd.attendance.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.lloyd.attendance.data.AppPreferences;

/**
 * Legacy Activity redirector.
 * Permanently forwards any legacy intents to the modern Jetpack Compose UI (MainComposeActivity) or LoginActivity.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppPreferences prefs = AppPreferences.getInstance(this);

        if (!prefs.isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            Intent forwardIntent = new Intent(this, MainComposeActivity.class);
            forwardIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(forwardIntent);
        }
        finish();
    }
}
