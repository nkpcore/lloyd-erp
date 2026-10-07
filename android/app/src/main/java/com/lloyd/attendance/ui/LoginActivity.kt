package com.lloyd.attendance.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.lloyd.attendance.api.ErpApiClient
import com.lloyd.attendance.core.designsystem.theme.LloydTheme
import com.lloyd.attendance.data.AppPreferences
import com.lloyd.attendance.feature.auth.LoginScreen
import com.lloyd.attendance.widget.AttendanceWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : ComponentActivity() {

    private lateinit var prefs: AppPreferences
    private lateinit var apiClient: ErpApiClient

    private var isLoading by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPreferences.getInstance(this)
        apiClient = ErpApiClient(this)

        // Auto-navigate if already authenticated
        if (prefs.isLoggedIn) {
            navigateToMain()
            return
        }

        enableEdgeToEdge()
        setContent {
            LloydTheme {
                LoginScreen(
                    initialUsername = prefs.username,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onLoginClick = { user, pass ->
                        performLogin(user, pass)
                    },
                    onBiometricClick = {
                        performQuickAuth()
                    },
                    isBiometricAvailable = prefs.isLoggedIn
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

    private fun performQuickAuth() {
        if (!prefs.isLoggedIn) {
            Toast.makeText(this, "Please sign in with your password first.", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        errorMessage = null

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                apiClient.ensureValidToken()
                withContext(Dispatchers.Main) {
                    isLoading = false
                    navigateToMain()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(this@LoginActivity, "Session expired. Please sign in with your password.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainComposeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
