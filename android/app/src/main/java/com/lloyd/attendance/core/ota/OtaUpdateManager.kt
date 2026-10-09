package com.lloyd.attendance.core.ota

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.lloyd.attendance.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import android.widget.Toast
import android.content.pm.PackageManager

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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
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

    private fun parseSemver(v: String): Triple<Int, Int, Int>? {
        val clean = cleanVersion(v)
        val match = Regex("""^(\d+)\.(\d+)(?:\.(\d+))?""").find(clean) ?: return null
        val major = match.groupValues[1].toIntOrNull() ?: return null
        val minor = match.groupValues[2].toIntOrNull() ?: return null
        val patch = match.groupValues.getOrNull(3)?.toIntOrNull() ?: 0
        return Triple(major, minor, patch)
    }

    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val cleanRemote = cleanVersion(remoteTag)
        val cleanCurrent = cleanVersion(currentVersion)
        if (cleanRemote == cleanCurrent) return false

        val rSemver = parseSemver(cleanRemote)
        val cSemver = parseSemver(cleanCurrent)

        // If current version is not valid semver (e.g. git hash like 12641d3), treat remote as newer
        if (cSemver == null && rSemver != null) return true
        if (rSemver == null) return false
        if (cSemver == null) return true

        val (rMajor, rMinor, rPatch) = rSemver
        val (cMajor, cMinor, cPatch) = cSemver

        if (rMajor != cMajor) return rMajor > cMajor
        if (rMinor != cMinor) return rMinor > cMinor
        return rPatch > cPatch
    }

    suspend fun checkForUpdates(
        currentVersion: String = BuildConfig.VERSION_NAME,
        fleetEndpoint: String? = null
    ): Result<OtaReleaseInfo> {
        return withContext(Dispatchers.IO) {
            val cleanCurrent = cleanVersion(currentVersion)

            val effectiveFleet = fleetEndpoint?.takeIf { it.isNotBlank() }
                ?: BuildConfig.DEFAULT_FLEET_URL.takeIf { it.isNotBlank() }
                ?: "https://lloyd-erp-sand.vercel.app"

            // 1. Check live fleet server /ota endpoint
            if (effectiveFleet.isNotBlank()) {
                val cleanBase = effectiveFleet.trim().removeSuffix("/")

                try {
                    val otaUrl = if (cleanBase.endsWith("/ota") || cleanBase.endsWith("/api/ota")) cleanBase
                    else "$cleanBase/ota?current_version=$cleanCurrent"

                    val otaReq = Request.Builder()
                        .url(otaUrl)
                        .header("Accept", "application/json")
                        .header("User-Agent", "LloydERP-Android/${BuildConfig.VERSION_NAME}")
                        .build()

                    val otaResp = client.newCall(otaReq).execute()
                    if (otaResp.isSuccessful) {
                        val body = otaResp.body?.string()
                        if (!body.isNullOrBlank()) {
                            val otaJson = gson.fromJson(body, com.google.gson.JsonObject::class.java)
                            val hasUpdate = otaJson.get("has_update")?.asBoolean ?: false
                            val latestVer = otaJson.get("latest_version")?.asString ?: currentVersion
                            val downloadUrl = otaJson.get("download_url")?.asString
                            val notes = otaJson.get("release_notes")?.asString ?: "New update available from Lloyd Fleet Portal."

                            if (hasUpdate && !downloadUrl.isNullOrBlank()) {
                                val resolvedUrl = if (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://")) {
                                    downloadUrl
                                } else {
                                    "$cleanBase/${downloadUrl.removePrefix("/")}"
                                }
                                return@withContext Result.success(
                                    OtaReleaseInfo(
                                        hasUpdate = true,
                                        latestVersion = cleanVersion(latestVer),
                                        currentVersion = cleanCurrent,
                                        releaseNotes = notes,
                                        downloadUrl = resolvedUrl
                                    )
                                )
                            } else if (!hasUpdate) {
                                return@withContext Result.success(
                                    OtaReleaseInfo(
                                        hasUpdate = false,
                                        latestVersion = cleanVersion(latestVer),
                                        currentVersion = cleanCurrent,
                                        releaseNotes = notes,
                                        downloadUrl = null
                                    )
                                )
                            }
                        }
                    }
                } catch (ignored: Exception) {
                }

                // Fallback check to /config endpoint
                try {
                    val configUrl = if (cleanBase.endsWith("/config") || cleanBase.endsWith("/api/config")) cleanBase
                    else "$cleanBase/config"

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
                            val latestName = config.get("latest_version_name")?.asString
                            val rawUrl = config.get("download_url")?.asString
                            val downloadUrl = if (!rawUrl.isNullOrBlank()) {
                                if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) rawUrl
                                else "$cleanBase/${rawUrl.removePrefix("/")}"
                            } else null

                            if (!latestName.isNullOrBlank() && !downloadUrl.isNullOrBlank() && isNewerVersion(latestName, currentVersion)) {
                                return@withContext Result.success(
                                    OtaReleaseInfo(
                                        hasUpdate = true,
                                        latestVersion = cleanVersion(latestName),
                                        currentVersion = cleanCurrent,
                                        releaseNotes = config.get("broadcast_notice")?.asString ?: "New update available from Lloyd Fleet Portal.",
                                        downloadUrl = downloadUrl
                                    )
                                )
                            }
                        }
                    }
                } catch (ignored: Exception) {
                }
            }

            // 2. Query GitHub Releases API directly
            try {
                val request = Request.Builder()
                    .url(GITHUB_API_URL)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "LloydERP-Android/${BuildConfig.VERSION_NAME}")
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        val release = gson.fromJson(bodyStr, GitHubRelease::class.java)
                        val tagName = release.tagName.orEmpty()
                        val apkAsset = release.assets?.firstOrNull { it.name?.endsWith(".apk", ignoreCase = true) == true }
                        val downloadUrl = apkAsset?.downloadUrl

                        if (tagName.isNotBlank() && isNewerVersion(tagName, currentVersion) && !downloadUrl.isNullOrBlank()) {
                            return@withContext Result.success(
                                OtaReleaseInfo(
                                    hasUpdate = true,
                                    latestVersion = cleanVersion(tagName),
                                    currentVersion = cleanCurrent,
                                    releaseNotes = release.body.orEmpty().ifBlank { "Latest release from GitHub Releases." },
                                    downloadUrl = downloadUrl
                                )
                            )
                        }
                    }
                }
            } catch (ignored: Exception) {
            }

            // Current version is the latest known
            Result.success(
                OtaReleaseInfo(
                    hasUpdate = false,
                    latestVersion = cleanCurrent,
                    currentVersion = cleanCurrent,
                    releaseNotes = "You are running the latest version.",
                    downloadUrl = null
                )
            )
        }
    }

    /**
     * Resolves the standardized local storage destination for downloaded update APKs.
     * Uses internal cacheDir to avoid Android 11+ external-storage permission blocks.
     */
    fun getDownloadedUpdateApk(context: Context): File {
        val updatesDir = File(context.cacheDir, "updates")
        return File(updatesDir, "LloydAttendance-update.apk")
    }

    /**
     * Downloads the APK directly with real-time percentage progress.
     */
    suspend fun downloadApkWithProgress(
        context: Context,
        downloadUrl: String,
        onProgress: (progress: Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "LloydERP-Android/${BuildConfig.VERSION_NAME}")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Download failed with HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(IOException("Empty response body"))
            val contentLength = body.contentLength()

            val destinationFile = getDownloadedUpdateApk(context)
            destinationFile.parentFile?.mkdirs()
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            body.byteStream().use { input ->
                destinationFile.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (contentLength > 0) {
                            val progress = (totalRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        } else {
                            val simulated = ((totalRead % (4 * 1024 * 1024)).toFloat() / (4 * 1024 * 1024).toFloat()).coerceIn(0.1f, 0.95f)
                            withContext(Dispatchers.Main) {
                                onProgress(simulated)
                            }
                        }
                    }
                    output.flush()
                }
            }

            destinationFile.setReadable(true, false)

            withContext(Dispatchers.Main) {
                onProgress(1f)
            }
            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if the app has permission to request package installs on Android 8.0+.
     */
    fun canInstallApk(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Opens system Settings for "Install Unknown Apps" permission.
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Triggers the native Android PackageInstaller to prompt the user to install the APK.
     */
    fun promptInstallApk(context: Context, apkFile: File) {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            Toast.makeText(context, "Update file not found or corrupted", Toast.LENGTH_SHORT).show()
            return
        }

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        }

        try {
            val resolveList = context.packageManager.queryIntentActivities(installIntent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resolveList) {
                val pkg = resolveInfo.activityInfo.packageName
                context.grantUriPermission(pkg, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (ignored: Exception) {}

        try {
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Helper that triggers download via OkHttp with progress and automatically opens installer.
     */
    fun downloadAndInstallApk(context: Context, downloadUrl: String, fileName: String = "lloyd-erp-update.apk") {
        Toast.makeText(context, "Downloading update...", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            val result = downloadApkWithProgress(context, downloadUrl)
            withContext(Dispatchers.Main) {
                result.onSuccess { apkFile ->
                    if (!canInstallApk(context)) {
                        Toast.makeText(context, "Please allow Lloyd Attendance to install updates", Toast.LENGTH_LONG).show()
                        openInstallPermissionSettings(context)
                    } else {
                        promptInstallApk(context, apkFile)
                    }
                }.onFailure { err ->
                    Toast.makeText(context, "Download failed: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
