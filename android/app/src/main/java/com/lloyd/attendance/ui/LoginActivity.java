package com.lloyd.attendance.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.lloyd.attendance.R;
import com.lloyd.attendance.api.ErpApiClient;
import com.lloyd.attendance.data.AppPreferences;
import com.lloyd.attendance.util.AnimationHelper;
import com.lloyd.attendance.widget.AttendanceWidgetProvider;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private View btnBiometric;
    private ProgressBar pbLogin;
    private TextView tvError;
    private View cardLoginForm;

    private AppPreferences prefs;
    private ErpApiClient apiClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = AppPreferences.getInstance(this);
        apiClient = new ErpApiClient(this);

        // Auto-navigate if already logged in
        if (prefs.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnBiometric = findViewById(R.id.btn_biometric_login);
        pbLogin = findViewById(R.id.progress_login);
        tvError = findViewById(R.id.tv_login_error);
        cardLoginForm = findViewById(R.id.card_login_form);

        // Entrance motion
        if (cardLoginForm != null) {
            cardLoginForm.setAlpha(0f);
            cardLoginForm.setTranslationY(40f);
            cardLoginForm.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                    .start();
        }

        // Pre-fill username if remembered
        String savedUser = prefs.getUsername();
        if (!savedUser.isEmpty()) {
            etUsername.setText(savedUser);
            etPassword.requestFocus();
        }

        btnLogin.setOnClickListener(v -> {
            AnimationHelper.animateCardPress(v);
            attemptLogin();
        });

        if (btnBiometric != null) {
            btnBiometric.setOnClickListener(v -> {
                AnimationHelper.animateCardPress(v);
                String savedPassword = prefs.getPassword();
                if (!savedUser.isEmpty() && !savedPassword.isEmpty()) {
                    etPassword.setText(savedPassword);
                    attemptLogin();
                } else {
                    Toast.makeText(this, "Sign in with password once to enable quick login", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (username.isEmpty()) {
            etUsername.setError("Please enter your admission number or username");
            etUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Please enter your password");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);
        tvError.setVisibility(View.GONE);

        executor.execute(() -> {
            try {
                apiClient.login(username, password);

                new Handler(Looper.getMainLooper()).post(() -> {
                    setLoading(false);
                    AttendanceWidgetProvider.triggerRefresh(LoginActivity.this);
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    setLoading(false);
                    String msg = e.getMessage();
                    if (msg == null || msg.isEmpty()) msg = "Failed to sign in. Please verify your credentials.";
                    tvError.setText(msg);
                    tvError.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        pbLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        if (btnBiometric != null) btnBiometric.setEnabled(!loading);
    }
}
