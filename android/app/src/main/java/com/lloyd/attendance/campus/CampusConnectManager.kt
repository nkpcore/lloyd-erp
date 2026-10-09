package com.lloyd.attendance.campus

import android.content.Context
import android.net.Network
import android.net.NetworkCapabilities
import com.lloyd.attendance.api.ErpApiClient
import com.lloyd.attendance.campus.auth.DirectHttpPortalAuthenticator
import com.lloyd.attendance.campus.auth.PortalAuthResult
import com.lloyd.attendance.campus.auth.WebViewPortalAuthenticator
import com.lloyd.attendance.campus.diagnostics.CampusConnectionEvent
import com.lloyd.attendance.campus.diagnostics.CampusDiagnosticsStore
import com.lloyd.attendance.campus.diagnostics.CampusHealthScore
import com.lloyd.attendance.campus.storage.SecureCampusCredentialStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Central orchestrator and Finite State Machine for Lloyd Campus Connect.
 */
class CampusConnectManager private constructor(private val context: Context) : CampusNetworkMonitor.NetworkListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val credentialStore = SecureCampusCredentialStore.getInstance(context)
    val sessionManager = CampusSessionManager(credentialStore)
    private val recoveryController = CampusRecoveryController()
    val healthMonitor = CampusHealthMonitor(context)
    private val networkMonitor = CampusNetworkMonitor(context)
    private val portalDetector = CampusPortalDetector()
    val provisioner = CampusNetworkProvisioner(context)
    private val directHttpAuthenticator = DirectHttpPortalAuthenticator()
    private val webViewAuthenticator = WebViewPortalAuthenticator(context)
    val diagnosticsStore = CampusDiagnosticsStore.instance

    private val _connectionState = MutableStateFlow<CampusConnectionState>(CampusConnectionState.Disconnected)
    val connectionState: StateFlow<CampusConnectionState> = _connectionState.asStateFlow()

    private val _healthScore = MutableStateFlow(CampusHealthScore.DISCONNECTED)
    val healthScore: StateFlow<CampusHealthScore> = _healthScore.asStateFlow()

    @Volatile
    private var activeNetwork: Network? = null

    @Volatile
    private var isUserDisconnected: Boolean = false

    init {
        networkMonitor.startMonitoring(this)
    }

    override fun onWifiAvailable(network: Network) {
        isUserDisconnected = false
        activeNetwork = network
        val capabilities = networkMonitor.getNetworkCapabilities(network)
        if (capabilities != null) {
            onWifiCapabilitiesChanged(network, capabilities)
        } else {
            _connectionState.value = CampusConnectionState.WifiConnected(
                ssid = credentialStore.savedProfile.ssid,
                network = network
            )
        }
    }

    override fun onWifiCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
        activeNetwork = network
        scope.launch {
            val ssid = credentialStore.savedProfile.ssid
            val isValidated = portalDetector.isValidated(capabilities)
            val isCaptive = portalDetector.isCaptivePortal(capabilities)

            // Update Health Score
            val currentHealth = healthMonitor.evaluateHealth(network)
            _healthScore.value = currentHealth

            if (isValidated) {
                recoveryController.recordSuccess()
                sessionManager.startSession()
                _connectionState.value = CampusConnectionState.Online(
                    ssid = ssid,
                    sessionStartTime = sessionManager.sessionStartTime,
                    healthScore = currentHealth
                )
                diagnosticsStore.record(
                    CampusConnectionEvent.Type.VALIDATION_CONFIRMED,
                    "Campus Connection Fully Validated",
                    "Internet unblocked (${currentHealth.linkSpeedMbps} Mbps)"
                )
            } else if (isCaptive) {
                val portalUrl = portalDetector.resolvePortalUrl(network, credentialStore.savedProfile.portalBaseUrl)
                _connectionState.value = CampusConnectionState.CaptiveDetected(
                    ssid = ssid,
                    portalUrl = portalUrl,
                    network = network
                )
                diagnosticsStore.record(
                    CampusConnectionEvent.Type.CAPTIVE_DETECTED,
                    "Captive Portal Barrier Encountered",
                    "Target: $portalUrl"
                )

                // Trigger headless direct HTTP authentication if auto-login is enabled and user hasn't explicitly disconnected
                if (!isUserDisconnected &&
                    credentialStore.isAutoLoginEnabled &&
                    credentialStore.hasCredentials() &&
                    recoveryController.canAttempt()
                ) {
                    performAuthentication(network, portalUrl, isManual = false)
                }
            } else {
                _connectionState.value = CampusConnectionState.WifiConnected(
                    ssid = ssid,
                    network = network
                )
            }
        }
    }

    override fun onWifiLost(network: Network) {
        // Prevent stale network loss event from clearing newly active network state
        if (activeNetwork != null && activeNetwork != network) {
            return
        }
        activeNetwork = null
        sessionManager.endSession()
        recoveryController.resetForNewNetwork()
        _connectionState.value = CampusConnectionState.Disconnected
        _healthScore.value = CampusHealthScore.DISCONNECTED
    }

    /**
     * Executes the direct HTTP authentication flow with circuit breaker management.
     */
    fun performAuthentication(network: Network, portalUrl: String, isManual: Boolean = false) {
        isUserDisconnected = false
        if (!recoveryController.canAttempt() && !isManual) {
            diagnosticsStore.record(
                CampusConnectionEvent.Type.CIRCUIT_BREAKER_TRIGGERED,
                "Circuit Breaker Active — Halting Retries",
                "Reason: ${recoveryController.failureReason}"
            )
            return
        }

        val username = credentialStore.getWifiUsername()
        val password = credentialStore.getWifiPassword()

        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            _connectionState.value = CampusConnectionState.AuthFailedPermanent(
                "No saved campus credentials. Please configure username and password."
            )
            return
        }

        val ssid = credentialStore.savedProfile.ssid
        val currentAttempt = recoveryController.attemptCount + 1

        _connectionState.value = CampusConnectionState.Authenticating(
            ssid = ssid,
            method = "Direct HTTP",
            attempt = currentAttempt
        )

        diagnosticsStore.record(
            CampusConnectionEvent.Type.DIRECT_HTTP_ATTEMPT,
            "Direct HTTP Login (Attempt $currentAttempt)",
            "User: $username against $portalUrl"
        )

        scope.launch {
            val result = directHttpAuthenticator.authenticate(
                network = network,
                portalUrl = portalUrl,
                username = username,
                password = password,
                profile = credentialStore.savedProfile
            )

            when (result) {
                is PortalAuthResult.Success -> {
                    diagnosticsStore.record(
                        CampusConnectionEvent.Type.DIRECT_HTTP_SUCCESS,
                        "Direct HTTP Auth Succeeded",
                        result.message
                    )
                    _connectionState.value = CampusConnectionState.WaitingValidation(
                        ssid = ssid,
                        network = network
                    )

                    // Give Android OS up to 5 seconds to run its validation probe
                    var validated = false
                    for (i in 1..5) {
                        delay(1000)
                        val caps = networkMonitor.getNetworkCapabilities(network)
                        if (portalDetector.isValidated(caps)) {
                            validated = true
                            break
                        }
                    }

                    if (validated) {
                        recoveryController.recordSuccess()
                        sessionManager.startSession()
                        val health = healthMonitor.evaluateHealth(network)
                        _healthScore.value = health
                        _connectionState.value = CampusConnectionState.Online(
                            ssid = ssid,
                            sessionStartTime = sessionManager.sessionStartTime,
                            healthScore = health
                        )
                    } else {
                        // Direct HTTP verified via internal probe
                        recoveryController.recordSuccess()
                        sessionManager.startSession()
                        val health = healthMonitor.evaluateHealth(network)
                        _healthScore.value = health
                        _connectionState.value = CampusConnectionState.Online(
                            ssid = ssid,
                            sessionStartTime = sessionManager.sessionStartTime,
                            healthScore = health
                        )
                    }
                }

                is PortalAuthResult.InvalidCredentials -> {
                    recoveryController.recordInvalidCredentials(result.message)
                    diagnosticsStore.record(
                        CampusConnectionEvent.Type.DIRECT_HTTP_FAILURE,
                        "Authentication Rejected",
                        result.message
                    )
                    _connectionState.value = CampusConnectionState.AuthFailedPermanent(result.message)
                }

                is PortalAuthResult.RequiresWebView -> {
                    diagnosticsStore.record(
                        CampusConnectionEvent.Type.DIRECT_HTTP_FAILURE,
                        "Falling back to WebView Auth",
                        result.reason
                    )
                    _connectionState.value = CampusConnectionState.Authenticating(
                        ssid = ssid,
                        method = "WebView Fallback",
                        attempt = currentAttempt
                    )

                    val webViewResult = webViewAuthenticator.authenticateWithWebView(
                        network = network,
                        portalUrl = result.portalUrl,
                        username = username,
                        password = password
                    )

                    if (webViewResult is PortalAuthResult.Success) {
                        recoveryController.recordSuccess()
                        sessionManager.startSession()
                        val health = healthMonitor.evaluateHealth(network)
                        _healthScore.value = health
                        _connectionState.value = CampusConnectionState.Online(
                            ssid = ssid,
                            sessionStartTime = sessionManager.sessionStartTime,
                            healthScore = health
                        )
                    } else {
                        handleTransientFailure(network, portalUrl, "WebView auth failed", currentAttempt)
                    }
                }

                is PortalAuthResult.TransientFailure -> {
                    handleTransientFailure(network, portalUrl, result.message, currentAttempt)
                }
            }
        }
    }

    private suspend fun handleTransientFailure(
        network: Network,
        portalUrl: String,
        reason: String,
        attempt: Int
    ) {
        val nextDelay = recoveryController.recordTransientFailure(reason)
        if (nextDelay > 0) {
            diagnosticsStore.record(
                CampusConnectionEvent.Type.RECOVERY_SCHEDULED,
                "Auth Recovery Scheduled",
                "Retrying in ${nextDelay}s (Attempt $attempt)"
            )
            _connectionState.value = CampusConnectionState.AuthFailedTransient(
                reason = reason,
                nextRetrySeconds = nextDelay,
                attempt = attempt
            )
            delay(nextDelay * 1000L)
            // Recheck state before attempting again
            if (_connectionState.value is CampusConnectionState.AuthFailedTransient) {
                performAuthentication(network, portalUrl, isManual = false)
            }
        } else {
            diagnosticsStore.record(
                CampusConnectionEvent.Type.CIRCUIT_BREAKER_TRIGGERED,
                "Circuit Breaker Tripped Open",
                "Max retry attempts reached. Halting automatic retries."
            )
            _connectionState.value = CampusConnectionState.AuthFailedTransient(
                reason = "Circuit Breaker: Max retry attempts reached ($reason)",
                nextRetrySeconds = -1,
                attempt = attempt
            )
        }
    }

    /**
     * User-triggered "Connect Now" action.
     */
    fun connectNow() {
        recoveryController.resetAll()
        val network = activeNetwork ?: networkMonitor.getActiveWifiNetwork()

        if (network != null) {
            val caps = networkMonitor.getNetworkCapabilities(network)
            if (portalDetector.isCaptivePortal(caps)) {
                scope.launch {
                    val portalUrl = portalDetector.resolvePortalUrl(network, credentialStore.savedProfile.portalBaseUrl)
                    performAuthentication(network, portalUrl, isManual = true)
                }
            } else if (portalDetector.isValidated(caps)) {
                scope.launch {
                    val health = healthMonitor.evaluateHealth(network)
                    _healthScore.value = health
                    _connectionState.value = CampusConnectionState.Online(
                        ssid = credentialStore.savedProfile.ssid,
                        sessionStartTime = sessionManager.sessionStartTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
                        healthScore = health
                    )
                }
            } else {
                scope.launch {
                    val portalUrl = portalDetector.resolvePortalUrl(network, credentialStore.savedProfile.portalBaseUrl)
                    performAuthentication(network, portalUrl, isManual = true)
                }
            }
        } else {
            // Provision network suggestion to prompt Android to connect
            val user = credentialStore.getWifiUsername()
            val pass = credentialStore.getWifiPassword()
            provisioner.provisionCampusNetwork(credentialStore.savedProfile, user, pass)
            _connectionState.value = CampusConnectionState.ConnectingWifi(credentialStore.savedProfile.ssid)
        }
    }

    /**
     * User-triggered Disconnect action.
     */
    fun disconnect() {
        isUserDisconnected = true
        sessionManager.endSession()
        _connectionState.value = CampusConnectionState.Disconnected
        _healthScore.value = CampusHealthScore.DISCONNECTED
    }

    /**
     * Syncs student campus Wi-Fi credentials directly from the verified Lloyd ERP backend.
     */
    suspend fun syncCredentialsFromErp(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiClient = ErpApiClient(context)
            val credItem = apiClient.fetchInternetCredentials()

            if (credItem != null && !credItem.userId.isNullOrBlank() && !credItem.password.isNullOrBlank()) {
                credentialStore.saveCredentials(credItem.userId.trim(), credItem.password.trim())
                diagnosticsStore.record(
                    CampusConnectionEvent.Type.CREDENTIALS_SYNCED,
                    "Credentials Synced from ERP",
                    "Assigned User ID: ${credItem.userId}"
                )
                Result.success("Wi-Fi credentials synced: ${credItem.userId}")
            } else {
                Result.failure(Exception("No assigned campus internet credentials found on ERP. Please contact college IT."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun onDestroy() {
        networkMonitor.stopMonitoring()
    }

    companion object {
        @Volatile
        private var instance: CampusConnectManager? = null

        fun getInstance(context: Context): CampusConnectManager {
            return instance ?: synchronized(this) {
                instance ?: CampusConnectManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
