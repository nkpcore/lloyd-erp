package com.lloyd.attendance.core.ota

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.lloyd.attendance.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class OtaReleaseInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val releaseNotes: String,
    val downloadUrl: String?
)

object OtaUpdateManager {

    private const val GITHUB_API_URL = "https://api.github.com/repos/nkpcore/lloyd-erp/releases/latest"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    private data class GitHubRelease(
        @SerializedName("tag_name") val tagName: String?,
        @SerializedName("name") val name: String?,
        @SerializedName("body") val body: String?,
        @SerializedName("assets") val assets: List<GitHubAsset>?
    )

    private data class GitHubAsset(
        @SerializedName("name") val name: String?,
        @SerializedName("browser_download_url") val downloadUrl: String?
    )

    fun cleanVersion(version: String): String {
        return version.trim().removePrefix("v").removePrefix("V").trim()
    }

    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val remoteParts = cleanVersion(remoteTag).split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanVersion(currentVersion).split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    suspend fun checkForUpdates(
        currentVersion: String = BuildConfig.VERSION_NAME,
        fleetEndpoint: String? = null
    ): Result<OtaReleaseInfo> {
        return withContext(Dispatchers.IO) {
            // 1. Check remote fleet server config if available
            if (!fleetEndpoint.isNullOrBlank()) {
                try {
                    val configUrl = if (fleetEndpoint.endsWith("/config")) fleetEndpoint
                    else if (fleetEndpoint.endsWith("/")) "${fleetEndpoint}config"
                    else "$fleetEndpoint/config"

                    val cfgReq = Request.Builder()
                        .url(configUrl)
                        .header("Accept", "application/json")
                        .header("User-Agent", "LloydERP-Android/${BuildConfig.VERSION_NAME}")
                        .build()

                    val cfgResp = client.newCall(cfgReq).execute()
                    if (cfgResp.isSuccessful) {
                        val cfgJson = cfgResp.body?.string()
                        if (!cfgJson.isNullOrBlank()) {
                            val config = gson.fromJson(cfgJson, com.google.gson.JsonObject::class.java)
                            val latestName = config.get("latest_version_name")?.asString ?: currentVersion
                            val downloadUrl = config.get("download_url")?.asString
                            val hasUpdate = isNewerVersion(latestName, currentVersion)
                            if (hasUpdate) {
                                return@withContext Result.success(
                                    OtaReleaseInfo(
                                        hasUpdate = true,
                                        latestVersion = cleanVersion(latestName),
                                        currentVersion = cleanVersion(currentVersion),
                                        releaseNotes = "New update available from Lloyd Fleet Portal.",
                                        downloadUrl = downloadUrl
                                    )
                                )
                            }
                        }
                    }
                } catch (ignored: Exception) {
                }
            }

            // 2. Query GitHub Releases API
            try {
                val request = Request.Builder()
                    .url(GITHUB_API_URL)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "LloydERP-Android/${BuildConfig.VERSION_NAME}")
                    .build()

                val response = client.newCall(request).execute()

                // When GitHub returns 404 (no releases published yet on repo) or rate limit, handle gracefully
                if (response.code == 404 || !response.isSuccessful) {
                    return@withContext Result.success(
                        OtaReleaseInfo(
                            hasUpdate = false,
                            latestVersion = cleanVersion(currentVersion),
                            currentVersion = cleanVersion(currentVersion),
                            releaseNotes = "You are running the latest version.",
                            downloadUrl = null
                        )
                    )
                }

                val bodyStr = response.body?.string()
                if (bodyStr.isNullOrBlank()) {
                    return@withContext Result.success(
                        OtaReleaseInfo(
                            hasUpdate = false,
                            latestVersion = cleanVersion(currentVersion),
                            currentVersion = cleanVersion(currentVersion),
                            releaseNotes = "You are running the latest version.",
                            downloadUrl = null
                        )
                    )
                }

                val release = gson.fromJson(bodyStr, GitHubRelease::class.java)
                val tagName = release.tagName.orEmpty()
                val apkAsset = release.assets?.firstOrNull { it.name?.endsWith(".apk", ignoreCase = true) == true }
                val downloadUrl = apkAsset?.downloadUrl

                val hasUpdate = isNewerVersion(tagName, currentVersion)
                Result.success(
                    OtaReleaseInfo(
                        hasUpdate = hasUpdate,
                        latestVersion = cleanVersion(if (tagName.isNotBlank()) tagName else currentVersion),
                        currentVersion = cleanVersion(currentVersion),
                        releaseNotes = release.body.orEmpty().ifBlank { "Latest release" },
                        downloadUrl = downloadUrl
                    )
                )
            } catch (e: Exception) {
                // Network failure or unreachable: current version is latest known
                Result.success(
                    OtaReleaseInfo(
                        hasUpdate = false,
                        latestVersion = cleanVersion(currentVersion),
                        currentVersion = cleanVersion(currentVersion),
                        releaseNotes = "You are running the latest version.",
                        downloadUrl = null
                    )
                )
            }
        }
    }

    fun downloadAndInstallApk(context: Context, downloadUrl: String, fileName: String = "lloyd-erp-update.apk") {
        val uri = Uri.parse(downloadUrl)
        val request = DownloadManager.Request(uri).apply {
            setTitle("Downloading Lloyd ERP Update")
            setDescription("Fetching latest release...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
    }

    fun installDownloadedApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
