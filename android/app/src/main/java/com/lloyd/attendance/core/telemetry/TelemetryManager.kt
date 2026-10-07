package com.lloyd.attendance.core.telemetry

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import com.lloyd.attendance.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

object TelemetryManager {

    private const val PREFS_NAME = "lloyd_telemetry_prefs"
    private const val KEY_DEVICE_UUID = "telemetry_device_uuid"

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_DEVICE_UUID, null)
        if (deviceId.isNullOrBlank()) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_UUID, deviceId).apply()
        }
        return deviceId
    }

    fun buildPayload(
        deviceId: String,
        studentId: Int?,
        studentName: String?,
        appVersion: String,
        versionCode: Int,
        osVersion: String,
        deviceModel: String
    ): Map<String, Any?> {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val timestamp = isoFormat.format(Date())

        return mapOf(
            "device_id" to deviceId,
            "student_id" to studentId,
            "student_name" to (studentName ?: ""),
            "app_version" to appVersion,
            "version_code" to versionCode,
            "os_version" to osVersion,
            "device_model" to deviceModel,
            "timestamp" to timestamp
        )
    }

    suspend fun sendTelemetry(
        context: Context,
        endpointUrl: String?,
        studentId: Int?,
        studentName: String?
    ): Result<Unit> {
        if (endpointUrl.isNullOrBlank()) {
            return Result.success(Unit) // No-op if endpoint not configured
        }

        return withContext(Dispatchers.IO) {
            try {
                val deviceId = getOrCreateDeviceId(context)
                val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
                val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"

                val payload = buildPayload(
                    deviceId = deviceId,
                    studentId = studentId,
                    studentName = studentName,
                    appVersion = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE,
                    osVersion = osVersion,
                    deviceModel = deviceModel
                )

                val body = gson.toJson(payload).toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url(endpointUrl)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Telemetry HTTP error: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
