package com.lloyd.attendance.core.access

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.lloyd.attendance.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class FleetConfig(
    @SerializedName("min_version_code")
    val minVersionCode: Int = 1,
    @SerializedName("latest_version_name")
    val latestVersionName: String = BuildConfig.VERSION_NAME,
    @SerializedName("banned_students")
    val bannedStudents: List<Any> = emptyList(),
    @SerializedName("banned_devices")
    val bannedDevices: List<String> = emptyList(),
    @SerializedName("broadcast_notice")
    val broadcastNotice: String? = null,
    @SerializedName("maintenance_mode")
    val maintenanceMode: Boolean = false,
    @SerializedName("maintenance_message")
    val maintenanceMessage: String? = null,
    @SerializedName("download_url")
    val downloadUrl: String? = null
)

sealed interface AccessDecision {
    data class Authorized(val broadcastNotice: String? = null) : AccessDecision
    data class Revoked(val reason: String) : AccessDecision
    data class OutdatedVersion(val minVersionCode: Int, val downloadUrl: String?) : AccessDecision
    data class Maintenance(val message: String) : AccessDecision
}

object AccessControlManager {

    private const val PREFS_NAME = "lloyd_fleet_access_prefs"
    private const val KEY_CACHED_CONFIG = "cached_fleet_config"
    private const val KEY_LOCAL_BANS = "local_banned_devices_set"
    private const val KEY_LOCAL_STUDENT_BANS = "local_banned_students_set"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    @JvmStatic
    fun normalizeEndpointUrl(endpointUrl: String?): String? {
        if (endpointUrl.isNullOrBlank()) return null
        val cleanUrl = endpointUrl.trim().removeSuffix("/")
        return when {
            cleanUrl.endsWith("/config") || cleanUrl.endsWith("/api/config") -> cleanUrl
            else -> "$cleanUrl/config"
        }
    }

    @JvmStatic
    fun evaluateAccess(
        config: FleetConfig,
        studentId: Int,
        deviceId: String,
        currentVersionCode: Int
    ): AccessDecision {
        if (config.maintenanceMode) {
            val msg = config.maintenanceMessage ?: "Service is temporarily suspended for maintenance."
            return AccessDecision.Maintenance(msg)
        }

        // Device-isolated revocation: targets rogue device installation by UUID only.
        val cleanDevId = deviceId.trim()
        if (cleanDevId.isNotBlank() && config.bannedDevices.any { it.trim().equals(cleanDevId, ignoreCase = true) }) {
            return AccessDecision.Revoked(
                "Access has been restricted for this device. Please contact the administrator."
            )
        }

        // Account-level revocation: checks if student ID is banned
        if (studentId > 0 && config.bannedStudents.any { it.toString().trim() == studentId.toString() }) {
            return AccessDecision.Revoked(
                "Access has been restricted for your student account. Please contact the administrator."
            )
        }

        if (currentVersionCode < config.minVersionCode) {
            return AccessDecision.OutdatedVersion(
                minVersionCode = config.minVersionCode,
                downloadUrl = config.downloadUrl
            )
        }

        return AccessDecision.Authorized(broadcastNotice = config.broadcastNotice)
    }

    fun parseFleetConfig(json: String?): FleetConfig {
        if (json.isNullOrBlank() || json.trim().startsWith("<")) return FleetConfig()
        return try {
            gson.fromJson(json, FleetConfig::class.java) ?: FleetConfig()
        } catch (e: Exception) {
            FleetConfig()
        }
    }

    suspend fun checkAccess(
        context: Context,
        endpointUrl: String?,
        studentId: Int,
        deviceId: String,
        currentVersionCode: Int = BuildConfig.VERSION_CODE
    ): AccessDecision = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. Check local persistent bans (offline ban persistence)
        val localBans = prefs.getStringSet(KEY_LOCAL_BANS, emptySet()) ?: emptySet()
        val cleanDevId = deviceId.trim()
        if (cleanDevId.isNotBlank() && localBans.any { it.trim().equals(cleanDevId, ignoreCase = true) }) {
            return@withContext AccessDecision.Revoked(
                "Access has been restricted for this device. Please contact the administrator."
            )
        }

        val localStudentBans = prefs.getStringSet(KEY_LOCAL_STUDENT_BANS, emptySet()) ?: emptySet()
        if (studentId > 0 && localStudentBans.contains(studentId.toString())) {
            return@withContext AccessDecision.Revoked(
                "Access has been restricted for your student account. Please contact the administrator."
            )
        }

        // 2. Normalize fleet endpoint URL (ensure it queries /config instead of root HTML)
        val targetUrl = normalizeEndpointUrl(endpointUrl)
            ?: return@withContext AccessDecision.Authorized()

        // 3. Query remote fleet config
        try {
            val request = Request.Builder()
                .url(targetUrl)
                .header("Accept", "application/json")
                .header("User-Agent", "LloydERP-Android/${BuildConfig.VERSION_NAME}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string()?.trim()
                if (!bodyStr.isNullOrBlank() && !bodyStr.startsWith("<")) {
                    val config = parseFleetConfig(bodyStr)

                    // Persist cache
                    prefs.edit().putString(KEY_CACHED_CONFIG, bodyStr).apply()

                    // Update local device ban set
                    val updatedBans = config.bannedDevices.map { it.trim() }.filter { it.isNotBlank() }.toSet()
                    val updatedStudentBans = config.bannedStudents.map { it.toString().trim() }.filter { it.isNotBlank() }.toSet()
                    prefs.edit()
                        .putStringSet(KEY_LOCAL_BANS, updatedBans)
                        .putStringSet(KEY_LOCAL_STUDENT_BANS, updatedStudentBans)
                        .apply()

                    return@withContext evaluateAccess(config, studentId, deviceId, currentVersionCode)
                }
            }
        } catch (ignored: Exception) {
            // Fail-open for network errors: use cached config if available, otherwise permit
            val cachedJson = prefs.getString(KEY_CACHED_CONFIG, null)
            if (!cachedJson.isNullOrBlank() && !cachedJson.startsWith("<")) {
                val cachedConfig = parseFleetConfig(cachedJson)
                return@withContext evaluateAccess(cachedConfig, studentId, deviceId, currentVersionCode)
            }
        }

        // Default-allow on initial network failure (fail-open offline policy)
        AccessDecision.Authorized()
    }

    @JvmStatic
    fun checkAccessBlocking(
        context: Context,
        endpointUrl: String?,
        studentId: Int,
        deviceId: String,
        currentVersionCode: Int = BuildConfig.VERSION_CODE
    ): AccessDecision = kotlinx.coroutines.runBlocking {
        checkAccess(context, endpointUrl, studentId, deviceId, currentVersionCode)
    }
}
