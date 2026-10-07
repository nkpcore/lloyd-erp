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
        private final boolean isAuthExpired;

        public ErpException(String message) {
            this(message, false);
        }

        public ErpException(String message, boolean isAuthExpired) {
            super(message);
            this.isAuthExpired = isAuthExpired;
        }

        public boolean isAuthExpired() {
            return isAuthExpired;
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
                } else {
                    prefs.updateOrEnrichUserProfile(
                        apiRes.data.name,
                        apiRes.data.profileId,
                        username,
                        null,
                        null,
                        null
                    );
                }
                try {
                    getProfile();
                } catch (Exception ignored) {
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

        throw new ErpException("Please sign in to your Lloyd ERP account.", true);
    }

    private synchronized void handleUnauthorized() throws Exception {
        prefs.saveTokens("", "");
        if (refreshToken()) {
            return;
        }
        throw new ErpException("Session expired. Please sign in again.", true);
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
                if (apiRes.data.studentName != null && !apiRes.data.studentName.trim().isEmpty()) {
                    prefs.updateOrEnrichUserProfile(
                        apiRes.data.studentName,
                        apiRes.data.studentId > 0 ? apiRes.data.studentId : null,
                        null,
                        null,
                        null,
                        null
                    );
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
                    "&page=" + page + "&page_size=100&sort=desc";

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
            Models.StudentAttendanceItem sample = allItems.get(0);
            if (sample != null) {
                String sem = (sample.semesterId != null && sample.semesterId > 0) ? "Semester " + sample.semesterId : null;
                prefs.updateOrEnrichUserProfile(
                    sample.studentName,
                    sample.studentId,
                    sample.rollNo,
                    null,
                    sem,
                    null
                );
            }
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

    public Models.UserProfile getProfile() throws Exception {
        String token = ensureValidToken();

        Request request = new Request.Builder()
                .url(BASE_URL + "/profile/me")
                .get()
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 401) {
                handleUnauthorized();
                return getProfile();
            }

            if (!response.isSuccessful()) {
                throw new ErpException("Server error (" + response.code() + ") loading profile.");
            }

            String resStr = response.body() != null ? response.body().string() : "";
            Models.UserProfile profile = null;

            // 1. Parse authentic ERP nested structure: { data: { role, profile, academic } }
            try {
                Type meType = new TypeToken<Models.ApiResponse<Models.ProfileMeData>>() {}.getType();
                Models.ApiResponse<Models.ProfileMeData> meRes = gson.fromJson(resStr, meType);
                if (meRes != null && meRes.data != null) {
                    profile = new Models.UserProfile();
                    if (meRes.data.profile != null) {
                        profile.name = meRes.data.profile.name;
                        profile.username = meRes.data.profile.username;
                        profile.email = meRes.data.profile.email;
                        profile.photo_url = meRes.data.profile.photo_url;
                    }
                    if (meRes.data.academic != null) {
                        profile.admission_no = meRes.data.academic.admissionNo != null ? meRes.data.academic.admissionNo : profile.username;
                        profile.course = meRes.data.academic.className;
                        profile.semester = meRes.data.academic.semesterName;
                        profile.section = meRes.data.academic.sectionName;
                        if (meRes.data.academic.rollNo != null && !meRes.data.academic.rollNo.isEmpty()) {
                            profile.admission_no = meRes.data.academic.rollNo;
                        }
                    }
                    if (meRes.data.role != null) {
                        profile.role = meRes.data.role;
                    }
                }
            } catch (Exception ignored) {
            }

            // 2. Fallback to flat Models.ApiResponse<Models.UserProfile>
            if (profile == null) {
                try {
                    Type flatType = new TypeToken<Models.ApiResponse<Models.UserProfile>>() {}.getType();
                    Models.ApiResponse<Models.UserProfile> apiRes = gson.fromJson(resStr, flatType);
                    if (apiRes != null && apiRes.data != null) {
                        profile = apiRes.data;
                    }
                } catch (Exception ignored) {
                }
            }

            if (profile == null) {
                try {
                    profile = gson.fromJson(resStr, Models.UserProfile.class);
                } catch (Exception ignored) {
                }
            }

            if (profile != null) {
                prefs.saveUserProfile(profile);
                if (profile.id > 0) {
                    prefs.saveStudentId(profile.id);
                }
                if (profile.section != null && !profile.section.trim().isEmpty()) {
                    prefs.saveSelectedSection(profile.section.trim());
                }
                prefs.updateOrEnrichUserProfile(
                    profile.name,
                    profile.id > 0 ? profile.id : null,
                    profile.admission_no,
                    profile.course,
                    profile.semester,
                    profile.section,
                    profile.photo_url
                );
                return profile;
            } else {
                return getDashboardProfile();
            }
        }
    }

    public Models.UserProfile getDashboardProfile() throws Exception {
        String token = ensureValidToken();
        Request request = new Request.Builder()
                .url(BASE_URL + "/dashboard")
                .get()
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 401) {
                handleUnauthorized();
                return getDashboardProfile();
            }
            if (!response.isSuccessful()) {
                throw new ErpException("Server error (" + response.code() + ") loading dashboard profile.");
            }
            String resStr = response.body() != null ? response.body().string() : "";
            Type type = new TypeToken<Models.ApiResponse<Models.DashboardData>>() {}.getType();
            Models.ApiResponse<Models.DashboardData> apiRes = gson.fromJson(resStr, type);
            if (apiRes != null && apiRes.data != null && apiRes.data.dashboard != null && apiRes.data.dashboard.profile != null) {
                Models.UserProfile profile = apiRes.data.dashboard.profile;
                prefs.saveUserProfile(profile);
                if (profile.id > 0) {
                    prefs.saveStudentId(profile.id);
                }
                if (profile.section != null && !profile.section.trim().isEmpty()) {
                    prefs.saveSelectedSection(profile.section.trim());
                }
                prefs.updateOrEnrichUserProfile(
                    profile.name,
                    profile.id > 0 ? profile.id : null,
                    profile.admission_no,
                    profile.course,
                    profile.semester,
                    profile.section,
                    profile.photo_url
                );
                return profile;
            }
            throw new ErpException("Unable to parse dashboard profile response");
        }
    }

    public Models.CalculatedStats fetchAndCalculateStats() throws Exception {
        Models.MonthlyAttendanceData monthly = getMonthlyAttendance();
        return calculateStatsFromMonthly(monthly);
    }
}
