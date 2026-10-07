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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : FragmentActivity() {

    private lateinit var prefs: AppPreferences
    private lateinit var apiClient: ErpApiClient
    private lateinit var credentialVault: BiometricCredentialVault

    private var isLoading by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)

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
