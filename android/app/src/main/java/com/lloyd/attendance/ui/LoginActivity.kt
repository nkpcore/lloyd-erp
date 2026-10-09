package com.lloyd.attendance.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.lloyd.attendance.api.ErpApiClient
import com.lloyd.attendance.core.designsystem.theme.LloydTheme
import com.lloyd.attendance.core.security.BiometricAuthManager
import com.lloyd.attendance.core.security.BiometricCredentialVault
import com.lloyd.attendance.data.AppPreferences
import com.lloyd.attendance.feature.auth.LoginScreen
import com.lloyd.attendance.widget.AttendanceWidgetProvider
import com.lloyd.attendance.BuildConfig
import com.lloyd.attendance.core.designsystem.components.InAppUpdateDialog
import com.lloyd.attendance.core.ota.OtaReleaseInfo
import com.lloyd.attendance.core.ota.OtaUpdateManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : FragmentActivity() {

    private lateinit var prefs: AppPreferences
    private lateinit var apiClient: ErpApiClient
    private lateinit var credentialVault: BiometricCredentialVault

    private var isLoading by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)
    private var pendingUpdateInfo by mutableStateOf<OtaReleaseInfo?>(null)
    private var isDownloadingUpdate by mutableStateOf(false)
    private var updateDownloadProgress by mutableStateOf(0f)
    private var pendingInstallApk by mutableStateOf<File?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPreferences.getInstance(this)
        apiClient = ErpApiClient(this)
        credentialVault = BiometricCredentialVault.getInstance(this)

        // Auto-navigate if already authenticated
        if (prefs.isLoggedIn) {
            navigateToMain()
            return
        }

        val authError = intent.getStringExtra("AUTH_ERROR")
        if (!authError.isNullOrBlank()) {
            errorMessage = authError
        }

        val isBiometricAvailable = BiometricAuthManager.isBiometricReady(this) && credentialVault.hasSavedCredentials()
        val initialUser = prefs.username.ifBlank { credentialVault.getSavedUsername().orEmpty() }

        // If session expired and biometrics enrolled with saved credentials, auto-prompt fast fingerprint unlock
        if (!authError.isNullOrBlank() && isBiometricAvailable) {
            performBiometricLogin(autoPrompt = true)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val otaResult = OtaUpdateManager.checkForUpdates(
                    currentVersion = BuildConfig.VERSION_NAME,
                    fleetEndpoint = prefs.telemetryEndpoint
                )
                otaResult.onSuccess { info ->
                    if (info.hasUpdate && !info.downloadUrl.isNullOrBlank()) {
                        val lastDismissed = prefs.lastDismissedUpdateVersion
                        if (info.latestVersion != lastDismissed) {
                            withContext(Dispatchers.Main) {
                                pendingUpdateInfo = info
                            }
                        }
                    }
                }
            } catch (ignored: Exception) {}
        }

        enableEdgeToEdge()
        setContent {
            LloydTheme {
                LoginScreen(
                    initialUsername = initialUser,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onLoginClick = { user, pass ->
                        performLogin(user, pass)
                    },
                    onBiometricClick = {
                        performBiometricLogin(autoPrompt = false)
                    },
                    isBiometricAvailable = isBiometricAvailable
                )

                pendingUpdateInfo?.let { updateInfo ->
                    InAppUpdateDialog(
                        updateInfo = updateInfo,
                        isDownloading = isDownloadingUpdate,
                        downloadProgress = updateDownloadProgress,
                        onConfirmUpdate = {
                            performDownloadAndInstall(updateInfo)
                        },
                        onDismiss = {
                            prefs.lastDismissedUpdateVersion = updateInfo.latestVersion
                            pendingUpdateInfo = null
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        pendingInstallApk?.let { apkFile ->
            if (OtaUpdateManager.canInstallApk(this) && apkFile.exists()) {
                pendingInstallApk = null
                OtaUpdateManager.promptInstallApk(this, apkFile)
            }
        }
    }

    private fun performDownloadAndInstall(updateInfo: OtaReleaseInfo) {
        val downloadUrl = updateInfo.downloadUrl ?: return
        isDownloadingUpdate = true
        lifecycleScope.launch(Dispatchers.IO) {
            val result = OtaUpdateManager.downloadApkWithProgress(this@LoginActivity, downloadUrl) { progress ->
                updateDownloadProgress = progress
            }
            withContext(Dispatchers.Main) {
                isDownloadingUpdate = false
                result.onSuccess { apkFile ->
                    pendingUpdateInfo = null
                    pendingInstallApk = apkFile
                    if (!OtaUpdateManager.canInstallApk(this@LoginActivity)) {
                        Toast.makeText(
                            this@LoginActivity,
                            "Allow Lloyd Attendance to install unknown apps, then return to complete update",
                            Toast.LENGTH_LONG
                        ).show()
                        OtaUpdateManager.openInstallPermissionSettings(this@LoginActivity)
                    } else {
                        OtaUpdateManager.promptInstallApk(this@LoginActivity, apkFile)
                    }
                }.onFailure { err ->
                    Toast.makeText(this@LoginActivity, "Update download failed: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun performLogin(user: String, pass: String) {
        isLoading = true
        errorMessage = null

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                apiClient.login(user, pass)

                // Persist encrypted credentials in Keystore for seamless fingerprint re-login
                credentialVault.saveCredentials(user, pass)

                val studentId = prefs.studentId
                val deviceId = prefs.getOrCreateDeviceId()
                val accessDecision = com.lloyd.attendance.core.access.AccessControlManager.checkAccess(
                    context = applicationContext,
                    endpointUrl = prefs.telemetryEndpoint,
                    studentId = studentId,
                    deviceId = deviceId
                )

                if (accessDecision is com.lloyd.attendance.core.access.AccessDecision.Revoked) {
                    prefs.saveTokens("", "")
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        errorMessage = accessDecision.reason
                    }
                    return@launch
                } else if (accessDecision is com.lloyd.attendance.core.access.AccessDecision.Maintenance) {
                    prefs.saveTokens("", "")
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        errorMessage = accessDecision.message
                    }
                    return@launch
                } else if (accessDecision is com.lloyd.attendance.core.access.AccessDecision.OutdatedVersion) {
                    prefs.saveTokens("", "")
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        errorMessage = "App version is outdated (minimum required: v${accessDecision.minVersionCode}). Please update to continue."
                        val dlUrl = accessDecision.downloadUrl ?: "https://lloyd-erp-sand.vercel.app/downloads/LloydAttendance-latest.apk"
                        pendingUpdateInfo = OtaReleaseInfo(
                            hasUpdate = true,
                            latestVersion = "v${accessDecision.minVersionCode}",
                            currentVersion = BuildConfig.VERSION_NAME,
                            releaseNotes = "Mandatory security and stability update.",
                            downloadUrl = dlUrl
                        )
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    isLoading = false
                    AttendanceWidgetProvider.triggerRefresh(this@LoginActivity)
                    navigateToMain()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    val msg = e.message
                    errorMessage = if (msg.isNullOrBlank()) {
                        "Failed to sign in. Please verify your admission number and password."
                    } else {
                        msg
                    }
                }
            }
        }
    }

    private fun performBiometricLogin(autoPrompt: Boolean = false) {
        if (!credentialVault.hasSavedCredentials()) {
            if (!autoPrompt) {
                Toast.makeText(this, "No saved credentials found. Please sign in with password.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val savedUser = credentialVault.getSavedUsername()
        val savedPass = credentialVault.getSavedPassword()
        if (savedUser.isNullOrBlank() || savedPass.isNullOrBlank()) {
            if (!autoPrompt) {
                Toast.makeText(this, "Could not decrypt saved credentials. Please enter password.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        BiometricAuthManager.authenticate(
            activity = this,
            title = "Sign in to Lloyd ERP",
            subtitle = "Verify fingerprint to restore session",
            negativeButtonText = "Use Password",
            onSuccess = {
                performLogin(savedUser, savedPass)
            },
            onError = { error ->
                if (!autoPrompt) {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            },
            onCancel = {
                // User chose to sign in manually
            }
        )
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainComposeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
