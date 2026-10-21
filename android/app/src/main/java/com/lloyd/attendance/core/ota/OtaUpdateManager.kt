package com.lloyd.attendance.core.ota

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
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
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import android.content.pm.PackageManager
import android.content.pm.Signature

// ──────────────────────────────────────────────────────────────────────────────
// HTTPS host allowlist — only these hosts may serve OTA downloads.
// Add entries in lowercase without scheme or trailing slash.
private val ALLOWED_OTA_HOSTS = setOf(
    "lloyd-erp-sand.vercel.app",
    "github.com",
    "objects.githubusercontent.com",
    "objects.githubassets.com",
    "releases.githubusercontent.com"
)
// ──────────────────────────────────────────────────────────────────────────────

data class OtaReleaseInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val releaseNotes: String,
    val downloadUrl: String?,
    /** Expected SHA-256 hex of the APK as advertised by the fleet server (nullable). */
    val expectedSha256: String? = null
)

object OtaUpdateManager {

    private const val GITHUB_API_URL =
        "https://api.github.com/repos/nkpcore/lloyd-erp/releases/latest"

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

    // ── Version helpers ────────────────────────────────────────────────────────

    fun cleanVersion(version: String): String =
        version.trim().removePrefix("v").removePrefix("V").trim()

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

        if (cSemver == null && rSemver != null) return true
        if (rSemver == null) return false
        if (cSemver == null) return true

        val (rMaj, rMin, rPat) = rSemver
        val (cMaj, cMin, cPat) = cSemver

        if (rMaj != cMaj) return rMaj > cMaj
        if (rMin != cMin) return rMin > cMin
        return rPat > cPat
    }

    // ── Host allowlist enforcement ─────────────────────────────────────────────

    /**
     * Returns the URL unchanged if it passes the allowlist check, or throws
     * [SecurityException] with a clear description if it does not.
     * Rules:
     *  - Must be https://
     *  - Host must be in [ALLOWED_OTA_HOSTS] (case-insensitive suffix match for subdomains)
     */
    private fun enforceAllowedOtaUrl(url: String): String {
        if (!url.startsWith("https://")) {
            throw SecurityException("OTA download URL must use HTTPS. Rejected: $url")
        }
        val host = try {
            Uri.parse(url).host?.lowercase() ?: ""
        } catch (e: Exception) {
            throw SecurityException("Malformed OTA download URL: $url")
        }
        val allowed = ALLOWED_OTA_HOSTS.any { allowed ->
            host == allowed || host.endsWith(".$allowed")
        }
        if (!allowed) {
            throw SecurityException(
                "OTA download host '$host' is not in the allowlist. " +
                "Allowed hosts: ${ALLOWED_OTA_HOSTS.joinToString()}"
            )
        }
        return url
    }

    // ── Update check ──────────────────────────────────────────────────────────

    suspend fun checkForUpdates(
        currentVersion: String = BuildConfig.VERSION_NAME,
        fleetEndpoint: String? = null
    ): Result<OtaReleaseInfo> = withContext(Dispatchers.IO) {
        val cleanCurrent = cleanVersion(currentVersion)

        val effectiveFleet = fleetEndpoint?.takeIf { it.isNotBlank() }
            ?: BuildConfig.DEFAULT_FLEET_URL.takeIf { it.isNotBlank() }
            ?: "https://lloyd-erp-sand.vercel.app"

        // 1. Fleet server /ota endpoint
        val fleetResult = runCatching {
            val cleanBase = effectiveFleet.trim().removeSuffix("/")
            val otaUrl = if (cleanBase.endsWith("/ota") || cleanBase.endsWith("/api/ota")) cleanBase
            else "$cleanBase/ota?current_version=$cleanCurrent"

            val otaReq = Request.Builder()
                .url(otaUrl)
                .header("Accept", "application/json")
                .header("User-Agent", "LloydERP-Android/${BuildConfig.VERSION_NAME}")
                .build()

            val otaResp = client.newCall(otaReq).execute()
            if (!otaResp.isSuccessful) {
                throw IOException("Fleet /ota responded HTTP ${otaResp.code}")
            }
            val body = otaResp.body?.string()
                ?: throw IOException("Fleet /ota returned empty body")

            val otaJson = gson.fromJson(body, com.google.gson.JsonObject::class.java)
            val hasUpdate = otaJson.get("has_update")?.asBoolean ?: false
            val latestVer = otaJson.get("latest_version")?.asString ?: currentVersion
            val rawUrl = otaJson.get("download_url")?.asString
            val sha256 = otaJson.get("sha256")?.asString
            val notes = otaJson.get("release_notes")?.asString ?: "New update available."

            if (hasUpdate && !rawUrl.isNullOrBlank()) {
                val resolvedUrl = if (rawUrl.startsWith("https://") || rawUrl.startsWith("http://")) rawUrl
                else "$cleanBase/${rawUrl.removePrefix("/")}"
                val safeUrl = enforceAllowedOtaUrl(resolvedUrl)
                OtaReleaseInfo(
                    hasUpdate = true,
                    latestVersion = cleanVersion(latestVer),
                    currentVersion = cleanCurrent,
                    releaseNotes = notes,
                    downloadUrl = safeUrl,
                    expectedSha256 = sha256
                )
            } else {
                OtaReleaseInfo(
                    hasUpdate = false,
                    latestVersion = cleanVersion(latestVer),
                    currentVersion = cleanCurrent,
                    releaseNotes = notes,
                    downloadUrl = null
                )
            }
        }
        if (fleetResult.isSuccess) return@withContext fleetResult

        // 2. Fleet /config fallback
        val configResult = runCatching {
            val cleanBase = effectiveFleet.trim().removeSuffix("/")
            val configUrl = if (cleanBase.endsWith("/config") || cleanBase.endsWith("/api/config")) cleanBase
            else "$cleanBase/config"

            val cfgReq = Request.Builder()
                .url(configUrl)
                .header("Accept", "application/json")
                .header("User-Agent", "LloydERP-Android/${BuildConfig.VERSION_NAME}")
                .build()

            val cfgResp = client.newCall(cfgReq).execute()
            if (!cfgResp.isSuccessful) {
                throw IOException("Fleet /config responded HTTP ${cfgResp.code}")
            }
            val cfgBody = cfgResp.body?.string()
                ?: throw IOException("Fleet /config returned empty body")

            val config = gson.fromJson(cfgBody, com.google.gson.JsonObject::class.java)
            val latestName = config.get("latest_version_name")?.asString
            val rawUrl = config.get("download_url")?.asString
            val sha256 = config.get("sha256")?.asString

            if (!latestName.isNullOrBlank() && !rawUrl.isNullOrBlank() &&
                isNewerVersion(latestName, currentVersion)
            ) {
                val resolvedUrl = if (rawUrl.startsWith("https://") || rawUrl.startsWith("http://")) rawUrl
                else "$cleanBase/${rawUrl.removePrefix("/")}"
                val safeUrl = enforceAllowedOtaUrl(resolvedUrl)
                OtaReleaseInfo(
                    hasUpdate = true,
                    latestVersion = cleanVersion(latestName),
                    currentVersion = cleanCurrent,
                    releaseNotes = config.get("broadcast_notice")?.asString ?: "New update available.",
                    downloadUrl = safeUrl,
                    expectedSha256 = sha256
                )
            } else {
                OtaReleaseInfo(
                    hasUpdate = false,
                    latestVersion = cleanVersion(latestName ?: currentVersion),
                    currentVersion = cleanCurrent,
                    releaseNotes = "You are running the latest version.",
                    downloadUrl = null
                )
            }
        }
        if (configResult.isSuccess) return@withContext configResult

        // 3. GitHub Releases API
        val githubResult = runCatching {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "LloydERP-Android/${BuildConfig.VERSION_NAME}")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("GitHub Releases API responded HTTP ${response.code}")
            }
            val bodyStr = response.body?.string()
                ?: throw IOException("GitHub Releases API returned empty body")

            val release = gson.fromJson(bodyStr, GitHubRelease::class.java)
            val tagName = release.tagName.orEmpty()
            val apkAsset = release.assets?.firstOrNull {
                it.name?.endsWith(".apk", ignoreCase = true) == true
            }
            val downloadUrl = apkAsset?.downloadUrl

            if (tagName.isNotBlank() && isNewerVersion(tagName, currentVersion) &&
                !downloadUrl.isNullOrBlank()
            ) {
                val safeUrl = enforceAllowedOtaUrl(downloadUrl)
                OtaReleaseInfo(
                    hasUpdate = true,
                    latestVersion = cleanVersion(tagName),
                    currentVersion = cleanCurrent,
                    releaseNotes = release.body.orEmpty().ifBlank { "Latest release from GitHub." },
                    downloadUrl = safeUrl
                )
            } else {
                OtaReleaseInfo(
                    hasUpdate = false,
                    latestVersion = cleanVersion(tagName.ifBlank { currentVersion }),
                    currentVersion = cleanCurrent,
                    releaseNotes = "You are running the latest version.",
                    downloadUrl = null
                )
            }
        }
        if (githubResult.isSuccess) return@withContext githubResult

        // All three sources failed — surface the last error honestly
        Result.failure(
            IOException(
                "OTA check failed from all sources. " +
                "Fleet error: ${fleetResult.exceptionOrNull()?.message}; " +
                "Config error: ${configResult.exceptionOrNull()?.message}; " +
                "GitHub error: ${githubResult.exceptionOrNull()?.message}"
            )
        )
    }

    // ── Download helpers ──────────────────────────────────────────────────────

    /**
     * Resolves the final destination file for a downloaded APK.
     * Uses internal cacheDir to avoid Android 11+ storage permission blocks.
     */
    fun getDownloadedUpdateApk(context: Context): File {
        val updatesDir = File(context.cacheDir, "updates")
        return File(updatesDir, "LloydAttendance-update.apk")
    }

    /**
     * Downloads the APK to a `.part` staging file, verifies SHA-256 if provided,
     * then atomically renames to the final destination.
     *
     * Returns [Result.failure] with a descriptive [IOException] on any error —
     * no silent swallowing.
     */
    suspend fun downloadApkWithProgress(
        context: Context,
        downloadUrl: String,
        expectedSha256: String? = null,
        onProgress: (progress: Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Enforce allowlist before any network activity
            val safeUrl = enforceAllowedOtaUrl(downloadUrl)

            val request = Request.Builder()
                .url(safeUrl)
                .header("User-Agent", "LloydERP-Android/${BuildConfig.VERSION_NAME}")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Download failed: HTTP ${response.code} from $safeUrl")
                )
            }

            val body = response.body
                ?: return@withContext Result.failure(IOException("Download returned empty body"))

            val contentLength = body.contentLength()
            val destinationFile = getDownloadedUpdateApk(context)
            destinationFile.parentFile?.mkdirs()

            // Write to .part staging file first — atomic rename after verification
            val partFile = File(destinationFile.parent, "${destinationFile.name}.part")
            if (partFile.exists()) partFile.delete()

            val digest = if (!expectedSha256.isNullOrBlank()) {
                MessageDigest.getInstance("SHA-256")
            } else null

            body.byteStream().use { input ->
                partFile.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var totalRead = 0L

                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        output.write(buffer, 0, bytesRead)
                        digest?.update(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val progress = if (contentLength > 0) {
                            (totalRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 0.99f)
                        } else {
                            ((totalRead % (4 * 1024 * 1024)).toFloat() / (4 * 1024 * 1024).toFloat())
                                .coerceIn(0.1f, 0.95f)
                        }
                        withContext(Dispatchers.Main) { onProgress(progress) }
                    }
                    output.flush()
                }
            }

            // SHA-256 integrity check (if server advertised a hash)
            if (digest != null && !expectedSha256.isNullOrBlank()) {
                val actualHex = digest.digest().joinToString("") { "%02x".format(it) }
                if (!actualHex.equals(expectedSha256.trim().lowercase(), ignoreCase = true)) {
                    partFile.delete()
                    return@withContext Result.failure(
                        SecurityException(
                            "APK integrity check failed. " +
                            "Expected SHA-256: $expectedSha256, got: $actualHex. " +
                            "The download may be corrupted or tampered with."
                        )
                    )
                }
            }

            // Atomic rename from .part to final destination
            if (destinationFile.exists()) destinationFile.delete()
            if (!partFile.renameTo(destinationFile)) {
                // renameTo can fail across filesystems — copy + delete as fallback
                partFile.copyTo(destinationFile, overwrite = true)
                partFile.delete()
            }
            destinationFile.setReadable(true, false)

            withContext(Dispatchers.Main) { onProgress(1f) }
            Result.success(destinationFile)
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(IOException("APK download error: ${e.message}", e))
        }
    }

    // ── Install helpers ───────────────────────────────────────────────────────

    fun canInstallApk(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

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
     * Verifies that the APK at [apkFile] is signed with the same leaf certificate
     * as the currently-running application.
     *
     * Returns `true` if the certificate fingerprints match, `false` otherwise.
     * Any parsing error is treated as a mismatch (fail-closed).
     */
    @Suppress("DEPRECATION")
    private fun verifyApkSigningCertificate(context: Context, apkFile: File): Boolean {
        return try {
            val pm = context.packageManager

            // --- Current app's signer fingerprint ---
            val currentSigs: Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                info.signingInfo?.apkContentsSigners ?: emptyArray()
            } else {
                val info = pm.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
                info.signatures ?: emptyArray()
            }
            val currentDigests = currentSigs.map { sig ->
                MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
                    .joinToString("") { "%02x".format(it) }
            }.toSet()

            // --- Downloaded APK's signer fingerprint ---
            val downloadedSigs: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                info?.signingInfo?.apkContentsSigners
            } else {
                val info = pm.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.GET_SIGNATURES
                )
                info?.signatures
            }

            if (downloadedSigs.isNullOrEmpty()) return false

            val downloadedDigests = downloadedSigs.map { sig ->
                MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
                    .joinToString("") { "%02x".format(it) }
            }.toSet()

            // Require at least one signer certificate to be common
            currentDigests.intersect(downloadedDigests).isNotEmpty()
        } catch (e: Exception) {
            false // Fail closed
        }
    }

    fun promptInstallApk(context: Context, apkFile: File) {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            Toast.makeText(context, "Update file not found or corrupted", Toast.LENGTH_SHORT).show()
            return
        }

        // Certificate pinning — reject APK if it is not signed by the same key as this app
        if (!verifyApkSigningCertificate(context, apkFile)) {
            Toast.makeText(
                context,
                "Update rejected: certificate mismatch. Download may be tampered.",
                Toast.LENGTH_LONG
            ).show()
            apkFile.delete()
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
            val resolveList = context.packageManager
                .queryIntentActivities(installIntent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resolveList) {
                context.grantUriPermission(
                    resolveInfo.activityInfo.packageName,
                    contentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        } catch (ignored: Exception) {
            // Best-effort URI permission grant; continue regardless
        }

        try {
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Convenience: download with progress then prompt installation.
     * All errors are surfaced as Toast messages.
     */
    fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        expectedSha256: String? = null
    ) {
        Toast.makeText(context, "Downloading update…", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            val result = downloadApkWithProgress(
                context = context,
                downloadUrl = downloadUrl,
                expectedSha256 = expectedSha256
            )
            withContext(Dispatchers.Main) {
                result.onSuccess { apkFile ->
                    if (!canInstallApk(context)) {
                        Toast.makeText(
                            context,
                            "Please allow Lloyd Attendance to install updates",
                            Toast.LENGTH_LONG
                        ).show()
                        openInstallPermissionSettings(context)
                    } else {
                        promptInstallApk(context, apkFile)
                    }
                }.onFailure { err ->
                    Toast.makeText(context, "Update failed: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
