package com.lloyd.attendance.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.lloyd.attendance.api.Models;
import com.lloyd.attendance.core.security.KeystoreTokenStore;
import com.lloyd.attendance.core.security.SecureTokenStore;
import java.util.UUID;

public class AppPreferences {
    private static final String PREF_NAME = "lloyd_attendance_secure_prefs";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_PROFILE = "user_profile";
    private static final String KEY_CACHED_STATS = "cached_stats";
    private static final String KEY_MONTHLY_JSON = "monthly_json";
    private static final String KEY_WEEKLY_JSON = "weekly_json";
    private static final String KEY_ATTENDANCE_LOGS_JSON = "attendance_logs_json";
    private static final String KEY_STUDENT_ID = "student_id";
    private static final String KEY_LAST_SEEN_ATTENDANCE_ID = "last_seen_attendance_id";
    private static final String KEY_SEEN_ATTENDANCE_IDS = "seen_attendance_ids_set";
    private static final String KEY_INITIALIZED_ATTENDANCE_HISTORY = "initialized_attendance_history";
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";
    private static final String KEY_SELECTED_SECTION = "selected_section";

    private static volatile AppPreferences instance;
    private final SharedPreferences prefs;
    private final SecureTokenStore tokenStore;
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
        this.prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.tokenStore = new KeystoreTokenStore(appContext);
    }

    public SecureTokenStore getTokenStore() {
        return tokenStore;
    }

    public synchronized String getOrCreateDeviceId() {
        String id = prefs.getString(KEY_DEVICE_ID, null);
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_ID, id).apply();
        }
        return id;
    }

    public void saveUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    public void saveCredentials(String username) {
        saveUsername(username);
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public void saveTokens(String accessToken, String refreshToken) {
        tokenStore.setAccessToken(accessToken);
        if (refreshToken != null) {
            tokenStore.setRefreshToken(refreshToken);
        }
    }

    public String getAccessToken() {
        String token = tokenStore.getAccessToken();
        return token != null ? token : "";
    }

    public String getRefreshToken() {
        String token = tokenStore.getRefreshToken();
        return token != null ? token : "";
    }

    public void saveUserProfile(Models.UserProfile profile) {
        if (profile != null) {
            prefs.edit().putString(KEY_USER_PROFILE, gson.toJson(profile)).apply();
            if (profile.id > 0) {
                saveStudentId(profile.id);
            }
            if (profile.section != null && !profile.section.trim().isEmpty()) {
                saveSelectedSection(profile.section.trim());
            }
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
        tokenStore.setVerifiedStudentId(id);
    }

    public int getStudentId() {
        int id = tokenStore.getVerifiedStudentId();
        if (id > 0) return id;
        return prefs.getInt(KEY_STUDENT_ID, 0);
    }

    public void saveSelectedSection(String section) {
        prefs.edit().putString(KEY_SELECTED_SECTION, section != null ? section.trim() : "").apply();
    }

    public String getSelectedSection() {
        // Strict: Never default to Section A-1. If empty, returns empty string.
        return prefs.getString(KEY_SELECTED_SECTION, "");
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

    public boolean hasInitializedAttendanceHistory() {
        return prefs.getBoolean(KEY_INITIALIZED_ATTENDANCE_HISTORY, false);
    }

    public void setInitializedAttendanceHistory(boolean initialized) {
        prefs.edit().putBoolean(KEY_INITIALIZED_ATTENDANCE_HISTORY, initialized).apply();
    }

    public java.util.Set<String> getSeenAttendanceIds() {
        java.util.Set<String> set = prefs.getStringSet(KEY_SEEN_ATTENDANCE_IDS, null);
        return set != null ? new java.util.HashSet<>(set) : new java.util.HashSet<>();
    }

    public void addSeenAttendanceIds(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        java.util.Set<String> current = getSeenAttendanceIds();
        for (Long id : ids) {
            if (id != null && id > 0) {
                current.add(String.valueOf(id));
            }
        }
        // Limit cache size to last 200 IDs to keep SharedPreferences fast and compact
        if (current.size() > 200) {
            java.util.List<String> list = new java.util.ArrayList<>(current);
            current = new java.util.HashSet<>(list.subList(list.size() - 200, list.size()));
        }
        prefs.edit().putStringSet(KEY_SEEN_ATTENDANCE_IDS, current).apply();
    }

    public boolean isLoggedIn() {
        return !getAccessToken().isEmpty() || tokenStore.hasValidRefreshToken();
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply();
    }

    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true);
    }

    public void clearAll() {
        String devId = getOrCreateDeviceId();
        tokenStore.clearTokens();
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
}
