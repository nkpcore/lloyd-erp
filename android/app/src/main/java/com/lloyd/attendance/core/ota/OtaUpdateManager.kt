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

    suspend fun checkForUpdates(currentVersion: String = BuildConfig.VERSION_NAME): Result<OtaReleaseInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(GITHUB_API_URL)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("GitHub API error: ${response.code}"))
                }

                val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val release = gson.fromJson(bodyStr, GitHubRelease::class.java)

                val tagName = release.tagName.orEmpty()
                val apkAsset = release.assets?.firstOrNull { it.name?.endsWith(".apk", ignoreCase = true) == true }
                val downloadUrl = apkAsset?.downloadUrl

                val hasUpdate = isNewerVersion(tagName, currentVersion)
                Result.success(
                    OtaReleaseInfo(
                        hasUpdate = hasUpdate,
                        latestVersion = cleanVersion(tagName),
                        currentVersion = cleanVersion(currentVersion),
                        releaseNotes = release.body.orEmpty(),
                        downloadUrl = downloadUrl
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
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
