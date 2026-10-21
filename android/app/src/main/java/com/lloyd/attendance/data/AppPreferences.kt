package com.lloyd.attendance.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.lloyd.attendance.BuildConfig
import com.lloyd.attendance.api.Models
import com.lloyd.attendance.core.security.KeystoreTokenStore
import com.lloyd.attendance.core.security.SecureTokenStore
import java.util.UUID

class AppPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val tokenStore: SecureTokenStore = KeystoreTokenStore(context.applicationContext)
    private val gson = Gson()

    // ── Device ID ────────────────────────────────────────────────────────────

    @Synchronized
    fun getOrCreateDeviceId(): String {
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id.isNullOrEmpty()) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    // ── Credentials & Tokens ─────────────────────────────────────────────────

    fun saveUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }

    fun saveCredentials(username: String) = saveUsername(username)

    fun getUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""

    fun saveTokens(accessToken: String, refreshToken: String?) {
        tokenStore.setAccessToken(accessToken)
        if (refreshToken != null) tokenStore.setRefreshToken(refreshToken)
    }

    fun getAccessToken(): String = tokenStore.getAccessToken() ?: ""

    fun getRefreshToken(): String = tokenStore.getRefreshToken() ?: ""

    fun isLoggedIn(): Boolean = getAccessToken().isNotEmpty() || tokenStore.hasValidRefreshToken()

    // ── Student ID ───────────────────────────────────────────────────────────

    fun saveStudentId(id: Int) {
        prefs.edit().putInt(KEY_STUDENT_ID, id).apply()
        tokenStore.setVerifiedStudentId(id)
    }

    fun getStudentId(): Int {
        val id = tokenStore.getVerifiedStudentId()
        return if (id > 0) id else prefs.getInt(KEY_STUDENT_ID, 0)
    }

    // ── User Profile ─────────────────────────────────────────────────────────

    fun saveUserProfile(profile: Models.UserProfile?) {
        profile ?: return
        prefs.edit().putString(KEY_USER_PROFILE, gson.toJson(profile)).apply()
        if (profile.id > 0) saveStudentId(profile.id)
        val section = profile.section?.trim()
        if (!section.isNullOrEmpty()) saveSelectedSection(section)
    }

    fun getUserProfile(): Models.UserProfile? {
        val json = prefs.getString(KEY_USER_PROFILE, null) ?: return null
        return try { gson.fromJson(json, Models.UserProfile::class.java) } catch (e: Exception) { null }
    }

    @Synchronized
    fun updateOrEnrichUserProfile(
        name: String?,
        id: Int?,
        rollNo: String?,
        course: String?,
        semester: String?,
        section: String?,
        photoUrl: String? = null
    ) {
        val current = getUserProfile() ?: Models.UserProfile()
        var modified = false

        fun String?.orCurrent(field: String?): Pair<String?, Boolean> {
            val trimmed = this?.trim()
            return if (!trimmed.isNullOrEmpty() && field.isNullOrEmpty()) trimmed to true
            else field to false
        }

        name.orCurrent(current.name).let { (v, m) -> if (m) { current.name = v; modified = true } }
        if (id != null && id > 0 && current.id <= 0) { current.id = id; modified = true }
        rollNo.orCurrent(current.admission_no).let { (v, m) -> if (m) { current.admission_no = v; modified = true } }
        course.orCurrent(current.course).let { (v, m) -> if (m) { current.course = v; modified = true } }
        semester.orCurrent(current.semester).let { (v, m) -> if (m) { current.semester = v; modified = true } }
        section.orCurrent(current.section).let { (v, m) -> if (m) { current.section = v; modified = true } }
        photoUrl.orCurrent(current.photo_url).let { (v, m) -> if (m) { current.photo_url = v; modified = true } }

        if (current.username.isNullOrEmpty()) {
            val uname = getUsername().trim()
            if (uname.isNotEmpty()) { current.username = uname; modified = true }
        }

        if (modified) saveUserProfile(current)
    }

    // ── Stats & Cached Data ──────────────────────────────────────────────────

    fun saveCachedStats(stats: Models.CalculatedStats?) {
        stats ?: return
        prefs.edit().putString(KEY_CACHED_STATS, gson.toJson(stats)).apply()
    }

    fun getCachedStats(): Models.CalculatedStats? {
        val json = prefs.getString(KEY_CACHED_STATS, null) ?: return null
        return try { gson.fromJson(json, Models.CalculatedStats::class.java) } catch (e: Exception) { null }
    }

    fun saveMonthlyData(json: String) { prefs.edit().putString(KEY_MONTHLY_JSON, json).apply() }
    fun getMonthlyData(): String? = prefs.getString(KEY_MONTHLY_JSON, null)
    fun getMonthlyAttendanceJson(): String? = getMonthlyData()



    fun saveWeeklyData(json: String) { prefs.edit().putString(KEY_WEEKLY_JSON, json).apply() }
    fun getWeeklyData(): String? = prefs.getString(KEY_WEEKLY_JSON, null)

    fun saveAttendanceLogs(json: String) { prefs.edit().putString(KEY_ATTENDANCE_LOGS_JSON, json).apply() }
    fun getAttendanceLogs(): String? = prefs.getString(KEY_ATTENDANCE_LOGS_JSON, null)
    fun saveAttendanceLogsJson(json: String) = saveAttendanceLogs(json)
    fun getAttendanceLogsJson(): String? = getAttendanceLogs()

    // ── Section ──────────────────────────────────────────────────────────────

    fun saveSelectedSection(section: String) {
        prefs.edit().putString(KEY_SELECTED_SECTION, section.trim()).apply()
    }

    /** Strict: Never default to Section A-1. Returns empty string when unset. */
    fun getSelectedSection(): String = prefs.getString(KEY_SELECTED_SECTION, "") ?: ""

    // ── Notification dedup ────────────────────────────────────────────────────

    fun saveLastSeenAttendanceId(id: Long) { prefs.edit().putLong(KEY_LAST_SEEN_ATTENDANCE_ID, id).apply() }
    fun getLastSeenAttendanceId(): Long = prefs.getLong(KEY_LAST_SEEN_ATTENDANCE_ID, 0)

    fun hasInitializedAttendanceHistory(): Boolean =
        prefs.getBoolean(KEY_INITIALIZED_ATTENDANCE_HISTORY, false)

    fun setInitializedAttendanceHistory(initialized: Boolean) {
        prefs.edit().putBoolean(KEY_INITIALIZED_ATTENDANCE_HISTORY, initialized).apply()
    }

    fun getSeenAttendanceIds(): MutableSet<String> {
        val set = prefs.getStringSet(KEY_SEEN_ATTENDANCE_IDS, null)
        return if (set != null) HashSet(set) else HashSet()
    }

    fun addSeenAttendanceIds(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        val current = getSeenAttendanceIds()
        ids.forEach { id -> if (id > 0) current.add(id.toString()) }
        // Limit cache to last 200 IDs to keep SharedPreferences fast and compact
        val trimmed: Set<String> = if (current.size > 200) {
            val list = current.toList()
            list.subList(list.size - 200, list.size).toHashSet()
        } else current
        prefs.edit().putStringSet(KEY_SEEN_ATTENDANCE_IDS, trimmed).apply()
    }

    // ── Notifications ────────────────────────────────────────────────────────

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
    }

    fun isNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)

    // ── Telemetry ─────────────────────────────────────────────────────────────

    fun getTelemetryEndpoint(): String {
        val endpoint = prefs.getString(KEY_TELEMETRY_ENDPOINT, "") ?: ""
        return if (endpoint.isBlank() || endpoint.contains("192.168.") || endpoint.contains("localhost")) {
            BuildConfig.DEFAULT_FLEET_URL
        } else endpoint
    }

    fun setTelemetryEndpoint(endpoint: String) {
        prefs.edit().putString(KEY_TELEMETRY_ENDPOINT, endpoint).apply()
    }

    // ── Onboarding ────────────────────────────────────────────────────────────

    fun isOnboardingCompleted(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    // ── OTA update dismissal ─────────────────────────────────────────────────

    fun getLastDismissedUpdateVersion(): String = prefs.getString(KEY_LAST_DISMISSED_UPDATE, "") ?: ""

    fun setLastDismissedUpdateVersion(version: String?) {
        prefs.edit().putString(KEY_LAST_DISMISSED_UPDATE, version ?: "").apply()
    }

    // ── Kotlin property aliases (callers use property syntax) ─────────────────

    @get:JvmName("isLoggedInProperty")
    val isLoggedIn: Boolean get() = isLoggedIn()

    @get:JvmName("usernameProperty")
    val username: String get() = getUsername()

    @get:JvmName("telemetryEndpointProperty")
    val telemetryEndpoint: String get() = getTelemetryEndpoint()

    @get:JvmName("studentIdProperty")
    val studentId: Int get() = getStudentId()

    @get:JvmName("userProfileProperty")
    val userProfile: Models.UserProfile? get() = getUserProfile()

    @get:JvmName("cachedStatsProperty")
    val cachedStats: Models.CalculatedStats? get() = getCachedStats()

    @get:JvmName("selectedSectionProperty")
    val selectedSection: String get() = getSelectedSection()

    @get:JvmName("monthlyDataProperty")
    val monthlyData: String? get() = getMonthlyData()

    @get:JvmName("attendanceLogsProperty")
    val attendanceLogs: String? get() = getAttendanceLogs()

    @get:JvmName("weeklyDataProperty")
    val weeklyData: String? get() = getWeeklyData()

    var isOnboardingCompleted: Boolean
        @JvmName("isOnboardingCompletedProperty") get() = isOnboardingCompleted()
        @JvmName("setOnboardingCompletedProperty") set(value) { setOnboardingCompleted(value) }

    var lastDismissedUpdateVersion: String?
        @JvmName("getLastDismissedUpdateVersionProperty") get() = getLastDismissedUpdateVersion()
        @JvmName("setLastDismissedUpdateVersionProperty") set(value) { setLastDismissedUpdateVersion(value) }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun clearAll() {
        val devId = getOrCreateDeviceId()
        tokenStore.clearTokens()
        prefs.edit().clear().putString(KEY_DEVICE_ID, devId).apply()
    }

    fun logout() = clearAll()

    // ─────────────────────────────────────────────────────────────────────────
    companion object {
        private const val PREF_NAME = "lloyd_attendance_secure_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_PROFILE = "user_profile"
        private const val KEY_CACHED_STATS = "cached_stats"
        private const val KEY_MONTHLY_JSON = "monthly_json"
        private const val KEY_WEEKLY_JSON = "weekly_json"
        private const val KEY_ATTENDANCE_LOGS_JSON = "attendance_logs_json"
        private const val KEY_STUDENT_ID = "student_id"
        private const val KEY_LAST_SEEN_ATTENDANCE_ID = "last_seen_attendance_id"
        private const val KEY_SEEN_ATTENDANCE_IDS = "seen_attendance_ids_set"
        private const val KEY_INITIALIZED_ATTENDANCE_HISTORY = "initialized_attendance_history"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        private const val KEY_SELECTED_SECTION = "selected_section"
        private const val KEY_TELEMETRY_ENDPOINT = "telemetry_endpoint"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_LAST_DISMISSED_UPDATE = "last_dismissed_update_version"

        @Volatile private var instance: AppPreferences? = null

        @JvmStatic
        fun getInstance(context: Context): AppPreferences =
            instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
    }
}
