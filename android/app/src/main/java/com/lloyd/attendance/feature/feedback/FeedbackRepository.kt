package com.lloyd.attendance.feature.feedback

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.lloyd.attendance.BuildConfig
import com.lloyd.attendance.data.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class FeedbackRepository(context: Context) {

    private val prefs: AppPreferences = AppPreferences.getInstance(context)
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun submitFeedback(
        category: FeedbackCategory,
        message: String
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Message cannot be empty"))
        }
        if (trimmed.length > 2000) {
            return@withContext Result.failure(IllegalArgumentException("Message exceeds maximum length of 2000 characters"))
        }

        val baseFleet = prefs.telemetryEndpoint?.takeIf { it.isNotBlank() }
            ?: BuildConfig.DEFAULT_FLEET_URL.takeIf { it.isNotBlank() }
            ?: "https://lloyd-erp-sand.vercel.app"

        val cleanBase = baseFleet.trim().removeSuffix("/")
        val endpointUrl = if (cleanBase.endsWith("/api/feedback") || cleanBase.endsWith("/feedback")) cleanBase
        else "$cleanBase/api/feedback"

        val studentId = prefs.getStudentId().takeIf { it > 0 }?.toString()

        val payload = JsonObject().apply {
            addProperty("message", trimmed)
            addProperty("category", category.apiValue)
            if (studentId != null) {
                addProperty("student_id", studentId)
            }
            addProperty("app_version", BuildConfig.VERSION_NAME)
            addProperty("device_model", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
        }

        val request = Request.Builder()
            .url(endpointUrl)
            .post(gson.toJson(payload).toRequestBody(jsonMediaType))
            .header("Accept", "application/json")
            .header("User-Agent", "LloydERP-Android/${BuildConfig.VERSION_NAME}")
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errMsg = try {
                        val json = gson.fromJson(bodyStr, JsonObject::class.java)
                        json.get("error")?.asString ?: "Server returned HTTP ${response.code}"
                    } catch (_: Exception) {
                        "Server returned HTTP ${response.code}"
                    }
                    throw IOException(errMsg)
                }

                val json = gson.fromJson(bodyStr, JsonObject::class.java)
                val id = json.get("id")?.asString ?: ""
                val submittedAt = json.get("submitted_at")?.asString ?: ""
                Pair(id, submittedAt)
            }
        }
    }
}
