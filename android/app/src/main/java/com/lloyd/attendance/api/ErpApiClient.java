package com.lloyd.attendance.api;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lloyd.attendance.data.AppPreferences;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ErpApiClient {
    private static final String BASE_URL = "https://erp.lloydcollege.in/api";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // Shared connection pool and singleton OkHttpClient to reuse HTTP/TLS connections
    private static final ConnectionPool CONNECTION_POOL = new ConnectionPool(10, 5, TimeUnit.MINUTES);
    private static volatile OkHttpClient sharedClient;

    private static OkHttpClient getSharedClient() {
        if (sharedClient == null) {
            synchronized (ErpApiClient.class) {
                if (sharedClient == null) {
                    sharedClient = new OkHttpClient.Builder()
                            .connectionPool(CONNECTION_POOL)
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(10, TimeUnit.SECONDS)
                            .writeTimeout(10, TimeUnit.SECONDS)
                            .retryOnConnectionFailure(true)
                            .build();
                }
            }
        }
        return sharedClient;
    }

    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private final AppPreferences prefs;

    public ErpApiClient(Context context) {
        this.prefs = AppPreferences.getInstance(context);
        this.client = getSharedClient();
    }

    public static class ErpException extends Exception {
        public ErpException(String message) {
            super(message);
        }
    }

    public Models.LoginData login(String username, String password) throws Exception {
        String deviceId = prefs.getOrCreateDeviceId();
        Models.LoginRequest reqBody = new Models.LoginRequest(username, password, deviceId);

        String jsonPayload = gson.toJson(reqBody);
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/login")
                .post(RequestBody.create(jsonPayload, JSON))
                .header("Accept", "application/json")
                .header("User-Agent", "LloydERP-AndroidWidget/1.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String resStr = response.body() != null ? response.body().string() : "";
            Type type = new TypeToken<Models.ApiResponse<Models.LoginData>>() {}.getType();
            Models.ApiResponse<Models.LoginData> apiRes = gson.fromJson(resStr, type);

            if (apiRes != null && apiRes.status && apiRes.data != null) {
                // Persist username & encrypted tokens on this phone
                prefs.saveUsername(username);
                prefs.saveTokens(apiRes.data.access_token, apiRes.data.refresh_token);
                if (apiRes.data.profileId != null && apiRes.data.profileId > 0) {
                    prefs.saveStudentId(apiRes.data.profileId);
                }
                if (apiRes.data.user != null) {
                    prefs.saveUserProfile(apiRes.data.user);
                    if (apiRes.data.user.id > 0) {
                        prefs.saveStudentId(apiRes.data.user.id);
                    }
                    if (apiRes.data.user.section != null && !apiRes.data.user.section.trim().isEmpty()) {
                        prefs.saveSelectedSection(apiRes.data.user.section.trim());
                    }
                }
                return apiRes.data;
            } else {
                String errMsg = apiRes != null && apiRes.message != null ? apiRes.message : "Invalid credentials";
                throw new ErpException(errMsg);
            }
        }
    }

    public boolean refreshToken() {
        String rToken = prefs.getRefreshToken();
        if (rToken == null || rToken.isEmpty()) {
            return false;
        }

        Models.RefreshRequest reqBody = new Models.RefreshRequest(rToken);
        String jsonPayload = gson.toJson(reqBody);
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/refresh")
                .post(RequestBody.create(jsonPayload, JSON))
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String resStr = response.body() != null ? response.body().string() : "";
                Type type = new TypeToken<Models.ApiResponse<Models.LoginData>>() {}.getType();
                Models.ApiResponse<Models.LoginData> apiRes = gson.fromJson(resStr, type);
                if (apiRes != null && apiRes.status && apiRes.data != null) {
                    prefs.saveTokens(apiRes.data.access_token, apiRes.data.refresh_token);
                    return true;
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    /**
     * Guarantees a valid authorization token.
     * If token is missing/expired, it automatically refreshes via refresh token.
     */
    public synchronized String ensureValidToken() throws Exception {
        String token = prefs.getAccessToken();
        if (!token.isEmpty()) {
            return token;
        }

        // Try refresh token
        if (refreshToken()) {
            return prefs.getAccessToken();
        }

        throw new ErpException("Please sign in to your Lloyd ERP account.");
    }

    private synchronized void handleUnauthorized() throws Exception {
        prefs.saveTokens("", "");
        if (refreshToken()) {
            return;
        }
        throw new ErpException("Session expired. Please sign in again.");
    }

    public Models.MonthlyAttendanceData getMonthlyAttendance() throws Exception {
        String token = ensureValidToken();

        Request request = new Request.Builder()
                .url(BASE_URL + "/student/me/monthly-attendance")
                .get()
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 401) {
                handleUnauthorized();
                return getMonthlyAttendance(); // Retry once with fresh credentials
            }

            if (!response.isSuccessful()) {
                throw new ErpException("Server error (" + response.code() + "). Please try again later.");
            }

            String resStr = response.body() != null ? response.body().string() : "";
            Type type = new TypeToken<Models.ApiResponse<Models.MonthlyAttendanceData>>() {}.getType();
            Models.ApiResponse<Models.MonthlyAttendanceData> apiRes = gson.fromJson(resStr, type);

            if (apiRes != null && apiRes.data != null) {
                if (apiRes.data.studentId > 0) {
                    prefs.saveStudentId(apiRes.data.studentId);
                }
                prefs.saveMonthlyData(resStr);
                return apiRes.data;
            } else {
                throw new ErpException(apiRes != null && apiRes.message != null ? apiRes.message : "Failed to load attendance data.");
            }
        }
    }

    public Models.WeeklyAttendanceData getWeeklyAttendance() throws Exception {
        String token = ensureValidToken();

        Request request = new Request.Builder()
                .url(BASE_URL + "/student/me/weekly-attendance")
                .get()
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 401) {
                handleUnauthorized();
                return getWeeklyAttendance(); // Retry once with fresh credentials
            }

            String resStr = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                throw new ErpException("Server error (" + response.code() + "): " + resStr);
            }

            Type type = new TypeToken<Models.ApiResponse<Models.WeeklyAttendanceData>>() {}.getType();
            Models.ApiResponse<Models.WeeklyAttendanceData> apiRes = gson.fromJson(resStr, type);

            if (apiRes != null && apiRes.data != null) {
                if (apiRes.data.days != null) {
                    for (Models.DayItem d : apiRes.data.days) {
                        if (d.periods != null) {
                            for (Models.PeriodItem p : d.periods) {
                                p.day = d.day;
                                p.date = d.date;
                            }
                        }
                    }
                }
                prefs.saveWeeklyData(resStr);
                return apiRes.data;
            } else {
                throw new ErpException("No weekly data");
            }
        }
    }

    public List<Models.StudentAttendanceItem> getStudentAttendanceLogs(int studentId) throws Exception {
        String token = ensureValidToken();
        int verifiedId = prefs.getStudentId();
        if (verifiedId <= 0) {
            // Dynamically resolve verified student ID directly from student/me/monthly-attendance
            try {
                Models.MonthlyAttendanceData monthly = getMonthlyAttendance();
                if (monthly != null && monthly.studentId > 0) {
                    verifiedId = monthly.studentId;
                    prefs.saveStudentId(verifiedId);
                }
            } catch (Exception ignored) {
            }
        }

        int targetStudentId = studentId > 0 ? studentId : verifiedId;

        // BOLA Security Check: strictly reject requests for unverified student IDs
        if (verifiedId > 0 && targetStudentId != verifiedId) {
            throw new SecurityException("BOLA security violation: Requesting unverified student ID: " + targetStudentId + " does not match authenticated student ID: " + verifiedId);
        }

        if (targetStudentId <= 0) {
            return new ArrayList<>();
        }

        List<Models.StudentAttendanceItem> allItems = new ArrayList<>();
        int page = 1;
        int totalPages = 1;

        do {
            String url = BASE_URL + "/attendance/student?student_id=" + targetStudentId +
                    "&page=" + page + "&page_size=100&sort_by=attendance_date&sort_dir=desc";

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 401) {
                    handleUnauthorized();
                    return getStudentAttendanceLogs(targetStudentId);
                }

                if (!response.isSuccessful()) {
                    break;
                }

                String resStr = response.body() != null ? response.body().string() : "";
                Type type = new TypeToken<Models.PaginatedApiResponse<List<Models.StudentAttendanceItem>>>() {}.getType();
                Models.PaginatedApiResponse<List<Models.StudentAttendanceItem>> apiRes = gson.fromJson(resStr, type);

                if (apiRes != null && apiRes.data != null && !apiRes.data.isEmpty()) {
                    allItems.addAll(apiRes.data);
                    if (apiRes.meta != null && apiRes.meta.totalPages > 0) {
                        totalPages = Math.min(apiRes.meta.totalPages, 20); // Safety limit
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            page++;
        } while (page <= totalPages);

        if (!allItems.isEmpty()) {
            prefs.saveAttendanceLogs(gson.toJson(allItems));
        }

        return allItems;
    }

    public Models.CalculatedStats calculateStatsFromMonthly(Models.MonthlyAttendanceData monthly) {
        String studentName = monthly != null ? monthly.studentName : "";
        if (studentName == null || studentName.isEmpty()) {
            Models.UserProfile p = prefs.getUserProfile();
            if (p != null && p.name != null) studentName = p.name;
        }

        Models.CalculatedStats stats = Models.CalculatedStats.fromMonths(studentName, monthly != null ? monthly.months : null);
        prefs.saveCachedStats(stats);
        return stats;
    }

    public Models.CalculatedStats fetchAndCalculateStats() throws Exception {
        Models.MonthlyAttendanceData monthly = getMonthlyAttendance();
        return calculateStatsFromMonthly(monthly);
    }
}
