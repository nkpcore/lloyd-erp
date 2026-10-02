package com.lloyd.attendance.data;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import com.google.gson.Gson;
import com.lloyd.attendance.api.Models;
import java.util.UUID;

public class AppPreferences {
    private static final String PREF_NAME = "lloyd_attendance_secure_prefs";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_PROFILE = "user_profile";
    private static final String KEY_CACHED_STATS = "cached_stats";
    private static final String KEY_MONTHLY_JSON = "monthly_json";
    private static final String KEY_WEEKLY_JSON = "weekly_json";
    private static final String KEY_ATTENDANCE_LOGS_JSON = "attendance_logs_json";
    private static final String KEY_STUDENT_ID = "student_id";
    private static final String KEY_LAST_SEEN_ATTENDANCE_ID = "last_seen_attendance_id";
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";

    private static volatile AppPreferences instance;
    private SharedPreferences prefs;
    private final Gson gson = new Gson();

    public static AppPreferences getInstance(Context context) {
        if (instance == null) {
            synchronized (AppPreferences.class) {
                if (instance == null) {
                    instance = new AppPreferences(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public AppPreferences(Context context) {
        Context appContext = context.getApplicationContext();
        try {
            MasterKey masterKey = new MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            this.prefs = EncryptedSharedPreferences.create(
                    appContext,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // Fallback to standard private preferences if Keystore unavailable
            this.prefs = appContext.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE);
        }
    }

    public synchronized String getOrCreateDeviceId() {
        String id = prefs.getString(KEY_DEVICE_ID, null);
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_ID, id).apply();
        }
        return id;
    }

    public void saveCredentials(String username, String password) {
        prefs.edit()
                .putString(KEY_USERNAME, username)
                .putString(KEY_PASSWORD, password)
                .apply();
    }

    public void saveCredentials(String username) {
        prefs.edit()
                .putString(KEY_USERNAME, username)
                .apply();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public String getPassword() {
        return prefs.getString(KEY_PASSWORD, "");
    }

    public void saveTokens(String accessToken, String refreshToken) {
        SharedPreferences.Editor editor = prefs.edit();
        if (accessToken != null) editor.putString(KEY_ACCESS_TOKEN, accessToken);
        if (refreshToken != null) editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, "");
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, "");
    }

    public void saveUserProfile(Models.UserProfile profile) {
        if (profile != null) {
            prefs.edit().putString(KEY_USER_PROFILE, gson.toJson(profile)).apply();
        }
    }

    public Models.UserProfile getUserProfile() {
        String json = prefs.getString(KEY_USER_PROFILE, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, Models.UserProfile.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void saveCachedStats(Models.CalculatedStats stats) {
        if (stats != null) {
            prefs.edit().putString(KEY_CACHED_STATS, gson.toJson(stats)).apply();
        }
    }

    public Models.CalculatedStats getCachedStats() {
        String json = prefs.getString(KEY_CACHED_STATS, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, Models.CalculatedStats.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void saveMonthlyData(String json) {
        prefs.edit().putString(KEY_MONTHLY_JSON, json).apply();
    }

    public String getMonthlyData() {
        return prefs.getString(KEY_MONTHLY_JSON, null);
    }

    public void saveWeeklyData(String json) {
        prefs.edit().putString(KEY_WEEKLY_JSON, json).apply();
    }

    public String getWeeklyData() {
        return prefs.getString(KEY_WEEKLY_JSON, null);
    }

    public void saveStudentId(int id) {
        prefs.edit().putInt(KEY_STUDENT_ID, id).apply();
    }

    public int getStudentId() {
        return prefs.getInt(KEY_STUDENT_ID, 28960);
    }

    public void saveAttendanceLogs(String json) {
        prefs.edit().putString(KEY_ATTENDANCE_LOGS_JSON, json).apply();
    }

    public String getAttendanceLogs() {
        return prefs.getString(KEY_ATTENDANCE_LOGS_JSON, null);
    }

    public void saveLastSeenAttendanceId(long id) {
        prefs.edit().putLong(KEY_LAST_SEEN_ATTENDANCE_ID, id).apply();
    }

    public long getLastSeenAttendanceId() {
        return prefs.getLong(KEY_LAST_SEEN_ATTENDANCE_ID, 0);
    }

    public boolean isLoggedIn() {
        return !getAccessToken().isEmpty() || (!getUsername().isEmpty() && !getPassword().isEmpty());
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply();
    }

    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true);
    }

    public void clearAll() {
        String devId = getOrCreateDeviceId();
        prefs.edit().clear().putString(KEY_DEVICE_ID, devId).apply();
    }

    public void logout() {
        clearAll();
    }

    public void saveAttendanceLogsJson(String json) {
        saveAttendanceLogs(json);
    }

    public String getAttendanceLogsJson() {
        return getAttendanceLogs();
    }

    public String getMonthlyAttendanceJson() {
        return getMonthlyData();
    }

    public void setDemoMode(boolean demo) {
        prefs.edit().putBoolean("demo_mode", demo).apply();
    }

    public boolean isDemoMode() {
        return prefs.getBoolean("demo_mode", false);
    }
}
