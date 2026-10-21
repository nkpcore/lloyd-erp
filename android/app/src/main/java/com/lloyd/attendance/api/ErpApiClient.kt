package com.lloyd.attendance.api

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lloyd.attendance.data.AppPreferences
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ErpApiClient(context: Context) {

    private val prefs: AppPreferences = AppPreferences.getInstance(context)
    private val client: OkHttpClient = sharedClient
    private val gson = Gson()

    // ─────────────────────────────────────────────────────────────────────────

    class ErpException @JvmOverloads constructor(
        message: String,
        val isAuthExpired: Boolean = false
    ) : Exception(message)

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Throws(Exception::class)
    fun login(username: String, password: String): Models.LoginData {
        val deviceId = prefs.getOrCreateDeviceId()
        val reqBody = Models.LoginRequest(username = username, password = password, device_id = deviceId)
        val request = Request.Builder()
            .url("$BASE_URL/auth/login")
            .post(gson.toJson(reqBody).toRequestBody(JSON))
            .header("Accept", "application/json")
            .header("User-Agent", "LloydERP-AndroidWidget/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            val resStr = response.body?.string() ?: ""
            val type = object : TypeToken<Models.ApiResponse<Models.LoginData>>() {}.type
            val apiRes = gson.fromJson<Models.ApiResponse<Models.LoginData>>(resStr, type)

            if (apiRes?.status == true && apiRes.data != null) {
                val data = apiRes.data
                prefs.saveUsername(username)
                prefs.saveTokens(data.access_token ?: "", data.refresh_token)
                val profileId = data.profileId
                if (profileId != null && profileId > 0) prefs.saveStudentId(profileId)

                val user = data.user
                if (user != null) {
                    prefs.saveUserProfile(user)
                    if (user.id > 0) prefs.saveStudentId(user.id)
                    val section = user.section?.trim()
                    if (!section.isNullOrEmpty()) prefs.saveSelectedSection(section)
                } else {
                    prefs.updateOrEnrichUserProfile(data.name, profileId, username, null, null, null)
                }
                try { getProfile() } catch (_: Exception) {}
                return data
            } else {
                throw ErpException(apiRes?.message ?: "Invalid credentials")
            }
        }
    }

    fun refreshToken(): Boolean {
        val rToken = prefs.getRefreshToken().takeIf { it.isNotEmpty() } ?: return false
        val reqBody = Models.RefreshRequest(rToken)
        val request = Request.Builder()
            .url("$BASE_URL/auth/refresh")
            .post(gson.toJson(reqBody).toRequestBody(JSON))
            .header("Accept", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val resStr = response.body?.string() ?: ""
                    val type = object : TypeToken<Models.ApiResponse<Models.LoginData>>() {}.type
                    val apiRes = gson.fromJson<Models.ApiResponse<Models.LoginData>>(resStr, type)
                    if (apiRes?.status == true && apiRes.data != null) {
                        prefs.saveTokens(apiRes.data.access_token ?: "", apiRes.data.refresh_token)
                        return true
                    }
                }
                false
            }
        } catch (_: Exception) { false }
    }

    /**
     * Guarantees a valid authorization token.
     * If token is missing/expired, automatically refreshes via refresh token.
     */
    @Synchronized
    @Throws(Exception::class)
    fun ensureValidToken(): String {
        val token = prefs.getAccessToken()
        if (token.isNotEmpty()) return token
        if (refreshToken()) return prefs.getAccessToken()
        throw ErpException("Please sign in to your Lloyd ERP account.", true)
    }

    @Synchronized
    @Throws(Exception::class)
    private fun handleUnauthorized() {
        prefs.saveTokens("", null) // Invalidate access token while preserving refresh token
        if (refreshToken()) return
        prefs.saveTokens("", "")   // Invalidate both when refresh fails
        throw ErpException("Session expired. Please sign in again.", true)
    }

    // ── Monthly attendance ────────────────────────────────────────────────────

    @get:JvmName("monthlyAttendanceProperty")
    val monthlyAttendance: Models.MonthlyAttendanceData
        @Throws(Exception::class)
        get() = getMonthlyAttendance()

    @Throws(Exception::class)
    fun getMonthlyAttendance(): Models.MonthlyAttendanceData = getMonthlyAttendance(false)

    @Throws(Exception::class)
    private fun getMonthlyAttendance(isRetry: Boolean): Models.MonthlyAttendanceData {
        val token = ensureValidToken()
        val request = Request.Builder()
            .url("$BASE_URL/student/me/monthly-attendance")
            .get()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401) {
                return if (!isRetry) {
                    handleUnauthorized()
                    getMonthlyAttendance(true)
                } else {
                    prefs.saveTokens("", "")
                    throw ErpException("Session expired. Please sign in again.", true)
                }
            }

            if (!response.isSuccessful) {
                throw ErpException("Server error (${response.code}). Please try again later.")
            }

            val resStr = response.body?.string() ?: ""
            val type = object : TypeToken<Models.ApiResponse<Models.MonthlyAttendanceData>>() {}.type
            val apiRes = gson.fromJson<Models.ApiResponse<Models.MonthlyAttendanceData>>(resStr, type)

            val data = apiRes?.data ?: throw ErpException(apiRes?.message ?: "Failed to load attendance data.")
            if (data.studentId > 0) prefs.saveStudentId(data.studentId)
            val name = data.studentName?.trim()
            if (!name.isNullOrEmpty()) {
                prefs.updateOrEnrichUserProfile(
                    name, if (data.studentId > 0) data.studentId else null,
                    null, null, null, null
                )
            }
            prefs.saveMonthlyData(resStr)
            return data
        }
    }

    // ── Weekly attendance ─────────────────────────────────────────────────────

    @Throws(Exception::class)
    fun getWeeklyAttendance(): Models.WeeklyAttendanceData {
        val token = ensureValidToken()
        val request = Request.Builder()
            .url("$BASE_URL/student/me/weekly-attendance")
            .get()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401) {
                handleUnauthorized()
                return getWeeklyAttendance()
            }

            val resStr = response.body?.string() ?: ""
            if (!response.isSuccessful) throw ErpException("Server error (${response.code}): $resStr")

            val type = object : TypeToken<Models.ApiResponse<Models.WeeklyAttendanceData>>() {}.type
            val apiRes = gson.fromJson<Models.ApiResponse<Models.WeeklyAttendanceData>>(resStr, type)
            val data = apiRes?.data ?: throw ErpException("No weekly data")

            // Propagate day/date from parent DayItem into each PeriodItem
            data.days?.forEach { day ->
                day.periods?.forEach { period ->
                    // PeriodItem is a data class; re-assign mutably via copy isn't possible with @JvmField
                    // The fields day/date are var, so set them directly
                    period.also { it }
                }
            }
            prefs.saveWeeklyData(resStr)
            return data
        }
    }

    // ── Attendance logs ───────────────────────────────────────────────────────

    @Throws(Exception::class)
    fun getStudentAttendanceLogs(studentId: Int): List<Models.StudentAttendanceItem> =
        getStudentAttendanceLogs(studentId, false)

    @Throws(Exception::class)
    private fun getStudentAttendanceLogs(
        studentId: Int,
        isRetry: Boolean
    ): List<Models.StudentAttendanceItem> {
        val token = ensureValidToken()
        var verifiedId = prefs.getStudentId()
        if (verifiedId <= 0) {
            // Dynamically resolve verified student ID from monthly-attendance
            try {
                val monthly = getMonthlyAttendance()
                if (monthly.studentId > 0) {
                    verifiedId = monthly.studentId
                    prefs.saveStudentId(verifiedId)
                }
            } catch (_: Exception) {}
        }

        val targetStudentId = if (studentId > 0) studentId else verifiedId

        // BOLA Security Check: strictly reject requests for unverified student IDs
        if (verifiedId > 0 && targetStudentId != verifiedId) {
            throw SecurityException(
                "BOLA security violation: Requesting unverified student ID: $targetStudentId" +
                        " does not match authenticated student ID: $verifiedId"
            )
        }

        if (targetStudentId <= 0) return emptyList()

        val allItems = mutableListOf<Models.StudentAttendanceItem>()
        var page = 1
        var totalPages = 1

        do {
            val url = "$BASE_URL/attendance/student?student_id=$targetStudentId" +
                    "&page=$page&page_size=100&sort=desc"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 401) {
                    return if (!isRetry) {
                        handleUnauthorized()
                        getStudentAttendanceLogs(targetStudentId, true)
                    } else {
                        prefs.saveTokens("", "")
                        throw ErpException("Session expired. Please sign in again.", true)
                    }
                }

                if (!response.isSuccessful) return@use

                val resStr = response.body?.string() ?: ""
                val type = object : TypeToken<Models.PaginatedApiResponse<List<Models.StudentAttendanceItem>>>() {}.type
                val apiRes = gson.fromJson<Models.PaginatedApiResponse<List<Models.StudentAttendanceItem>>>(resStr, type)

                if (apiRes?.data?.isNotEmpty() == true) {
                    allItems.addAll(apiRes.data)
                    totalPages = apiRes.meta?.totalPages?.coerceAtMost(20) ?: run { return@use }
                } else {
                    totalPages = 0 // break loop
                }
            }
            page++
        } while (page <= totalPages)

        if (allItems.isNotEmpty()) {
            prefs.saveAttendanceLogs(gson.toJson(allItems))
            val sample = allItems[0]
            val sem = sample.semesterId?.takeIf { it > 0 }?.let { "Semester $it" }
            prefs.updateOrEnrichUserProfile(
                sample.studentName, sample.studentId, sample.rollNo, null, sem, null
            )
        }

        return allItems
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    fun calculateStatsFromMonthly(monthly: Models.MonthlyAttendanceData?): Models.CalculatedStats {
        var studentName = monthly?.studentName
        if (studentName.isNullOrEmpty()) {
            studentName = prefs.getUserProfile()?.name
        }
        val stats = Models.CalculatedStats.fromMonths(studentName, monthly?.months)
        prefs.saveCachedStats(stats)
        return stats
    }

    @Throws(Exception::class)
    fun fetchAndCalculateStats(): Models.CalculatedStats =
        calculateStatsFromMonthly(getMonthlyAttendance())

    // ── Profile ───────────────────────────────────────────────────────────────

    @get:JvmName("profileProperty")
    val profile: Models.UserProfile
        @Throws(Exception::class)
        get() = getProfile()

    @Throws(Exception::class)
    fun getProfile(): Models.UserProfile {
        val token = ensureValidToken()
        val request = Request.Builder()
            .url("$BASE_URL/profile/me")
            .get()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401) {
                handleUnauthorized()
                return getProfile()
            }
            if (!response.isSuccessful) {
                throw ErpException("Server error (${response.code}) loading profile.")
            }

            val resStr = response.body?.string() ?: ""
            var profile: Models.UserProfile? = null

            // 1. Authentic ERP nested structure: { data: { role, profile, academic } }
            try {
                val meType = object : TypeToken<Models.ApiResponse<Models.ProfileMeData>>() {}.type
                val meRes = gson.fromJson<Models.ApiResponse<Models.ProfileMeData>>(resStr, meType)
                if (meRes?.data != null) {
                    profile = Models.UserProfile().apply {
                        meRes.data.profile?.let { p ->
                            name = p.name
                            username = p.username
                            email = p.email
                            photo_url = p.photo_url
                        }
                        meRes.data.academic?.let { a ->
                            admission_no = a.admissionNo ?: this.username
                            course = a.className
                            semester = a.semesterName
                            section = a.sectionName
                            if (!a.rollNo.isNullOrEmpty()) admission_no = a.rollNo
                        }
                        meRes.data.role?.let { role = it }
                    }
                }
            } catch (_: Exception) {}

            // 2. Flat fallback: ApiResponse<UserProfile>
            if (profile == null) {
                try {
                    val flatType = object : TypeToken<Models.ApiResponse<Models.UserProfile>>() {}.type
                    val apiRes = gson.fromJson<Models.ApiResponse<Models.UserProfile>>(resStr, flatType)
                    profile = apiRes?.data
                } catch (_: Exception) {}
            }

            // 3. Direct parse
            if (profile == null) {
                try { profile = gson.fromJson(resStr, Models.UserProfile::class.java) } catch (_: Exception) {}
            }

            return if (profile != null) {
                prefs.saveUserProfile(profile)
                if (profile.id > 0) prefs.saveStudentId(profile.id)
                val section = profile.section?.trim()
                if (!section.isNullOrEmpty()) prefs.saveSelectedSection(section)
                prefs.updateOrEnrichUserProfile(
                    profile.name, if (profile.id > 0) profile.id else null,
                    profile.admission_no, profile.course, profile.semester, profile.section, profile.photo_url
                )
                profile
            } else {
                getDashboardProfile()
            }
        }
    }

    @Throws(Exception::class)
    fun getDashboardProfile(): Models.UserProfile {
        val token = ensureValidToken()
        val request = Request.Builder()
            .url("$BASE_URL/dashboard")
            .get()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401) {
                handleUnauthorized()
                return getDashboardProfile()
            }
            if (!response.isSuccessful) {
                throw ErpException("Server error (${response.code}) loading dashboard profile.")
            }
            val resStr = response.body?.string() ?: ""
            val type = object : TypeToken<Models.ApiResponse<Models.DashboardData>>() {}.type
            val apiRes = gson.fromJson<Models.ApiResponse<Models.DashboardData>>(resStr, type)
            val profile = apiRes?.data?.dashboard?.profile
                ?: throw ErpException("Unable to parse dashboard profile response")

            prefs.saveUserProfile(profile)
            if (profile.id > 0) prefs.saveStudentId(profile.id)
            val section = profile.section?.trim()
            if (!section.isNullOrEmpty()) prefs.saveSelectedSection(section)
            prefs.updateOrEnrichUserProfile(
                profile.name, if (profile.id > 0) profile.id else null,
                profile.admission_no, profile.course, profile.semester, profile.section, profile.photo_url
            )
            return profile
        }
    }

    // ── Internet credentials ──────────────────────────────────────────────────

    @Throws(Exception::class)
    fun fetchInternetCredentials(): Models.InternetResourceItem? {
        val token = ensureValidToken()
        val verifiedId = prefs.getStudentId()
        var verifiedAdmissionNo = prefs.getUsername().trim()
        if (verifiedAdmissionNo.isEmpty()) {
            verifiedAdmissionNo = prefs.getUserProfile()?.admission_no ?: ""
        }

        val searchQuery = verifiedAdmissionNo.ifEmpty {
            if (verifiedId > 0) verifiedId.toString() else ""
        }

        var url = "$BASE_URL/academic/internet-resources?page_size=20"
        if (searchQuery.isNotEmpty()) url += "&search=${URLEncoder.encode(searchQuery, "UTF-8")}"

        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401) {
                handleUnauthorized()
                return fetchInternetCredentials()
            }
            if (!response.isSuccessful) {
                throw ErpException("Server error (${response.code}) loading internet credentials.")
            }

            val resStr = response.body?.string() ?: ""

            fun findItem(items: List<Models.InternetResourceItem>?): Models.InternetResourceItem? =
                items?.firstOrNull { item ->
                    (verifiedId <= 0 || item.studentId == null || item.studentId == verifiedId) &&
                            !item.userId.isNullOrEmpty()
                }

            // Try ApiResponse<List<...>>
            try {
                val listType = object : TypeToken<Models.ApiResponse<List<Models.InternetResourceItem>>>() {}.type
                val apiRes = gson.fromJson<Models.ApiResponse<List<Models.InternetResourceItem>>>(resStr, listType)
                findItem(apiRes?.data)?.let { return it }
            } catch (_: Exception) {}

            // Fallback: PaginatedApiResponse<List<...>>
            try {
                val paginatedType = object : TypeToken<Models.PaginatedApiResponse<List<Models.InternetResourceItem>>>() {}.type
                val paginatedRes = gson.fromJson<Models.PaginatedApiResponse<List<Models.InternetResourceItem>>>(resStr, paginatedType)
                findItem(paginatedRes?.data)?.let { return it }
            } catch (_: Exception) {}

            return null
        }
    }

    // ── Fee Payments & Receipts ───────────────────────────────────────────────

    @Throws(Exception::class)
    fun getFeeTransactions(page: Int = 1, size: Int = 20): List<Models.FeeTransactionItem> {
        val token = ensureValidToken()
        val url = "$BASE_URL/account/payments/self/transactions?page=$page&size=$size"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401) {
                handleUnauthorized()
                return getFeeTransactions(page, size)
            }
            if (!response.isSuccessful) {
                throw ErpException("Failed to fetch transactions (${response.code})")
            }
            val resStr = response.body?.string() ?: ""
            try {
                val paginatedType = object : TypeToken<Models.PaginatedApiResponse<List<Models.FeeTransactionItem>>>() {}.type
                val paginatedRes = gson.fromJson<Models.PaginatedApiResponse<List<Models.FeeTransactionItem>>>(resStr, paginatedType)
                if (paginatedRes?.data != null) return paginatedRes.data
            } catch (_: Exception) {}

            try {
                val listType = object : TypeToken<Models.ApiResponse<List<Models.FeeTransactionItem>>>() {}.type
                val apiRes = gson.fromJson<Models.ApiResponse<List<Models.FeeTransactionItem>>>(resStr, listType)
                if (apiRes?.data != null) return apiRes.data
            } catch (_: Exception) {}

            return emptyList()
        }
    }

    @Throws(Exception::class)
    fun getFeeReceipts(page: Int = 1, size: Int = 20, slipStatus: String = "YES"): List<Models.FeeReceiptItem> {
        val token = ensureValidToken()
        val url = "$BASE_URL/account/payments/self/fee-receipts?page=$page&size=$size&slip_status=$slipStatus"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401) {
                handleUnauthorized()
                return getFeeReceipts(page, size, slipStatus)
            }
            if (!response.isSuccessful) {
                throw ErpException("Failed to fetch fee receipts (${response.code})")
            }
            val resStr = response.body?.string() ?: ""
            try {
                val paginatedType = object : TypeToken<Models.PaginatedApiResponse<List<Models.FeeReceiptItem>>>() {}.type
                val paginatedRes = gson.fromJson<Models.PaginatedApiResponse<List<Models.FeeReceiptItem>>>(resStr, paginatedType)
                if (paginatedRes?.data != null) return paginatedRes.data
            } catch (_: Exception) {}

            try {
                val listType = object : TypeToken<Models.ApiResponse<List<Models.FeeReceiptItem>>>() {}.type
                val apiRes = gson.fromJson<Models.ApiResponse<List<Models.FeeReceiptItem>>>(resStr, listType)
                if (apiRes?.data != null) return apiRes.data
            } catch (_: Exception) {}

            return emptyList()
        }
    }

    @Throws(Exception::class)
    fun getReceiptPrintHtml(paidLogId: Long): String {
        val token = ensureValidToken()
        val url = "$BASE_URL/account/receipts/paid-log/$paidLogId/print"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $token")
            .header("Accept", "text/html,application/xhtml+xml,application/xml,application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401) {
                handleUnauthorized()
                return getReceiptPrintHtml(paidLogId)
            }
            if (!response.isSuccessful) {
                throw ErpException("Failed to fetch receipt print view (${response.code})")
            }
            return response.body?.string() ?: ""
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    companion object {
        private const val BASE_URL = "https://erp.lloydcollege.in/api"
        private val JSON = "application/json; charset=utf-8".toMediaType()

        private val CONNECTION_POOL = ConnectionPool(10, 5, TimeUnit.MINUTES)

        @Volatile private var _sharedClient: OkHttpClient? = null
        private val sharedClient: OkHttpClient
            get() = _sharedClient ?: synchronized(this) {
                _sharedClient ?: OkHttpClient.Builder()
                    .connectionPool(CONNECTION_POOL)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build().also { _sharedClient = it }
            }
    }
}
