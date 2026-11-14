package com.lloyd.attendance.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lloyd.attendance.BuildConfig
import com.lloyd.attendance.api.Models
import com.lloyd.attendance.core.notification.NotificationStateStore
import com.lloyd.attendance.core.security.KeystoreTokenStore
import com.lloyd.attendance.core.security.SecureTokenStore
import java.util.UUID

data class NotificationEvent(
    val id: String,
    val subject: String,
    val status: String,
    val timestamp: Long,
    val delivered: Boolean,
    val date: String = ""
)

class AppPreferences private constructor(context: Context) : NotificationStateStore {

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
        if (accessToken.isNotEmpty()) setAuthExpired(false)
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

    fun saveMonthlyCacheMetadata(etag: String?, lastModified: String?, fetchTimeMillis: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putString(KEY_MONTHLY_ETAG, etag)
            .putString(KEY_MONTHLY_LAST_MODIFIED, lastModified)
            .putLong(KEY_MONTHLY_LAST_FETCH_TIME, fetchTimeMillis)
            .apply()
    }
    fun getMonthlyEtag(): String? = prefs.getString(KEY_MONTHLY_ETAG, null)
    fun getMonthlyLastModified(): String? = prefs.getString(KEY_MONTHLY_LAST_MODIFIED, null)
    fun getMonthlyLastFetchTime(): Long = prefs.getLong(KEY_MONTHLY_LAST_FETCH_TIME, 0L)



    fun saveWeeklyData(json: String) { prefs.edit().putString(KEY_WEEKLY_JSON, json).apply() }
    fun getWeeklyData(): String? = prefs.getString(KEY_WEEKLY_JSON, null)

    fun saveAttendanceLogs(json: String) { prefs.edit().putString(KEY_ATTENDANCE_LOGS_JSON, json).apply() }
    fun getAttendanceLogs(): String? = prefs.getString(KEY_ATTENDANCE_LOGS_JSON, null)
    fun saveAttendanceLogsJson(json: String) = saveAttendanceLogs(json)
    fun getAttendanceLogsJson(): String? = getAttendanceLogs()

    fun saveLastSuccessfulLogSync(timestampMillis: Long) {
        prefs.edit().putLong(KEY_LAST_SUCCESSFUL_LOG_SYNC, timestampMillis).apply()
    }
    fun getLastSuccessfulLogSync(): Long = prefs.getLong(KEY_LAST_SUCCESSFUL_LOG_SYNC, 0L)

    fun saveLastGoodSubjectModel(json: String) {
        prefs.edit().putString(KEY_LAST_GOOD_SUBJECT_MODEL, json).apply()
    }
    fun getLastGoodSubjectModel(): String? = prefs.getString(KEY_LAST_GOOD_SUBJECT_MODEL, null)

    // ── Section ──────────────────────────────────────────────────────────────

    fun saveSelectedSection(section: String) {
        prefs.edit().putString(KEY_SELECTED_SECTION, section.trim()).apply()
    }

    /** Strict: Never default to Section A-1. Returns empty string when unset. */
    fun getSelectedSection(): String = prefs.getString(KEY_SELECTED_SECTION, "") ?: ""

    // ── Notification dedup & delivery log ─────────────────────────────────────

    override fun saveLastSeenAttendanceId(id: Long) {
        prefs.edit().putLong(KEY_LAST_SEEN_ATTENDANCE_ID, id).commit()
    }
    override fun getLastSeenAttendanceId(): Long = prefs.getLong(KEY_LAST_SEEN_ATTENDANCE_ID, 0)

    fun hasInitializedAttendanceHistory(): Boolean =
        prefs.getBoolean(KEY_INITIALIZED_ATTENDANCE_HISTORY, false)

    fun setInitializedAttendanceHistory(initialized: Boolean) {
        prefs.edit().putBoolean(KEY_INITIALIZED_ATTENDANCE_HISTORY, initialized).commit()
    }

    override fun isBootstrapComplete(): Boolean =
        prefs.getBoolean(KEY_BOOTSTRAP_COMPLETE, false) || hasInitializedAttendanceHistory()

    override fun setBootstrapComplete(timestamp: Long) {
        prefs.edit()
            .putBoolean(KEY_BOOTSTRAP_COMPLETE, true)
            .putLong(KEY_BOOTSTRAP_TIMESTAMP, timestamp)
            .putBoolean(KEY_INITIALIZED_ATTENDANCE_HISTORY, true)
            .commit()
    }

    fun getBootstrapTimestamp(): Long = prefs.getLong(KEY_BOOTSTRAP_TIMESTAMP, 0L)

    override fun getLastEmptyBootstrapAttempt(): Long =
        prefs.getLong(KEY_LAST_EMPTY_BOOTSTRAP_ATTEMPT, 0L)

    override fun setLastEmptyBootstrapAttempt(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_EMPTY_BOOTSTRAP_ATTEMPT, timestamp).commit()
    }

    fun getLastNotificationCheckTimestamp(): Long =
        prefs.getLong(KEY_LAST_NOTIFICATION_CHECK_TIMESTAMP, 0L)

    override fun setLastNotificationCheckTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_NOTIFICATION_CHECK_TIMESTAMP, timestamp).commit()
    }

    fun getSeenAttendanceIds(): MutableSet<String> {
        val set = prefs.getStringSet(KEY_SEEN_ATTENDANCE_IDS, null)
        return if (set != null) LinkedHashSet(set) else LinkedHashSet()
    }

    fun addSeenAttendanceIds(ids: Collection<Long>) {
        appendBoundedSet(KEY_SEEN_ATTENDANCE_IDS, ids.filter { it > 0 }.map { it.toString() })
    }

    fun addSeenSignatures(signatures: Collection<String>) {
        appendBoundedSet(KEY_SEEN_ATTENDANCE_IDS, signatures.mapNotNull { it.trim().takeIf(String::isNotEmpty) })
    }

    override fun getDetectedIds(): MutableSet<String> {
        val detected = prefs.getStringSet(KEY_DETECTED_ATTENDANCE_IDS, null)
        val legacySeen = prefs.getStringSet(KEY_SEEN_ATTENDANCE_IDS, null)
        val combined = LinkedHashSet<String>()
        if (legacySeen != null) combined.addAll(legacySeen)
        if (detected != null) combined.addAll(detected)
        return combined
    }

    override fun addDetectedIds(ids: Collection<Long>) {
        appendBoundedSet(KEY_DETECTED_ATTENDANCE_IDS, ids.filter { it > 0 }.map { it.toString() })
    }

    override fun addDetectedSignatures(signatures: Collection<String>) {
        appendBoundedSet(KEY_DETECTED_ATTENDANCE_IDS, signatures.mapNotNull { it.trim().takeIf(String::isNotEmpty) })
    }

    override fun getDeliveredIds(): MutableSet<String> {
        val set = prefs.getStringSet(KEY_DELIVERED_ATTENDANCE_IDS, null)
        return if (set != null) LinkedHashSet(set) else LinkedHashSet()
    }

    override fun addDeliveredIds(ids: Collection<Long>) {
        appendBoundedSet(KEY_DELIVERED_ATTENDANCE_IDS, ids.filter { it > 0 }.map { it.toString() })
    }

    @Synchronized
    override fun saveNotificationEvent(
        id: String,
        subject: String,
        status: String,
        timestamp: Long,
        delivered: Boolean,
        date: String
    ) {
        val updated = getNotificationEvents().toMutableList()
        updated.add(
            NotificationEvent(
                id = id,
                subject = subject,
                status = status,
                timestamp = timestamp,
                delivered = delivered,
                date = date
            )
        )
        while (updated.size > MAX_NOTIFICATION_EVENTS) {
            updated.removeAt(0)
        }
        prefs.edit().putString(KEY_NOTIFICATION_EVENTS, gson.toJson(updated)).commit()
    }

    fun getNotificationEvents(): List<NotificationEvent> {
        val json = prefs.getString(KEY_NOTIFICATION_EVENTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<NotificationEvent>>() {}.type
            gson.fromJson<List<NotificationEvent>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun appendBoundedSet(key: String, entries: Iterable<String>) {
        val items = entries.toList()
        if (items.isEmpty()) return
        val existing = prefs.getStringSet(key, null)
        val updated = if (existing != null) LinkedHashSet(existing) else LinkedHashSet()
        updated.addAll(items)
        // Store up to 10,000 historical records safely (~4 years of lectures)
        val iterator = updated.iterator()
        while (updated.size > MAX_TRACKED_IDS && iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
        // Strict: Synchronous commit so background workers never lose IDs on process exit
        prefs.edit().putStringSet(key, updated).commit()
    }

    // ── Auth Expiration ───────────────────────────────────────────────────────

    fun setAuthExpired(expired: Boolean) {
        prefs.edit().putBoolean(KEY_AUTH_EXPIRED, expired).apply()
    }

    fun isAuthExpired(): Boolean = prefs.getBoolean(KEY_AUTH_EXPIRED, false)

    // ── Notifications ────────────────────────────────────────────────────────

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
    }

    fun isNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)

    fun setNotificationPermissionState(granted: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_GRANTED, granted).apply()
    }

    fun getNotificationPermissionState(): Boolean =
        prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_GRANTED, false)

    fun setNotificationPermissionDenied(denied: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_DENIED, denied).apply()
    }

    fun wasNotificationPermissionDenied(): Boolean =
        prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_DENIED, false)

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

    var isAuthExpired: Boolean
        @JvmName("isAuthExpiredProperty") get() = isAuthExpired()
        @JvmName("setAuthExpiredProperty") set(value) { setAuthExpired(value) }

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
        private const val KEY_MONTHLY_ETAG = "monthly_etag"
        private const val KEY_MONTHLY_LAST_MODIFIED = "monthly_last_modified"
        private const val KEY_MONTHLY_LAST_FETCH_TIME = "monthly_last_fetch_time"
        private const val KEY_WEEKLY_JSON = "weekly_json"
        private const val KEY_ATTENDANCE_LOGS_JSON = "attendance_logs_json"
        private const val KEY_LAST_SUCCESSFUL_LOG_SYNC = "last_successful_log_sync"
        private const val KEY_LAST_GOOD_SUBJECT_MODEL = "last_good_subject_model"
        private const val KEY_STUDENT_ID = "student_id"
        private const val KEY_LAST_SEEN_ATTENDANCE_ID = "last_seen_attendance_id"
        private const val KEY_SEEN_ATTENDANCE_IDS = "seen_attendance_ids_set"
        private const val KEY_DETECTED_ATTENDANCE_IDS = "detected_attendance_ids_set"
        private const val KEY_DELIVERED_ATTENDANCE_IDS = "delivered_attendance_ids_set"
        private const val KEY_NOTIFICATION_EVENTS = "notification_events_log"
        private const val KEY_INITIALIZED_ATTENDANCE_HISTORY = "initialized_attendance_history"
        private const val KEY_BOOTSTRAP_COMPLETE = "bootstrap_complete"
        private const val KEY_BOOTSTRAP_TIMESTAMP = "bootstrap_timestamp"
        private const val KEY_LAST_EMPTY_BOOTSTRAP_ATTEMPT = "last_empty_bootstrap_attempt"
        private const val KEY_LAST_NOTIFICATION_CHECK_TIMESTAMP = "last_notification_check_timestamp"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        private const val KEY_NOTIFICATION_PERMISSION_GRANTED = "notification_permission_granted"
        private const val KEY_NOTIFICATION_PERMISSION_DENIED = "notification_permission_denied"
        private const val MAX_TRACKED_IDS = 10000
        private const val MAX_NOTIFICATION_EVENTS = 50
        private const val KEY_SELECTED_SECTION = "selected_section"
        private const val KEY_TELEMETRY_ENDPOINT = "telemetry_endpoint"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_LAST_DISMISSED_UPDATE = "last_dismissed_update_version"
        private const val KEY_AUTH_EXPIRED = "pref_auth_expired"

        @Volatile private var instance: AppPreferences? = null

        @JvmStatic
        fun getInstance(context: Context): AppPreferences =
            instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
    }
}
