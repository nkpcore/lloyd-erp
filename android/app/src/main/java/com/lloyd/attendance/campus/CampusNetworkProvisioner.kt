package com.lloyd.attendance.campus

import android.content.Context
import android.net.wifi.WifiEnterpriseConfig
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import androidx.annotation.RequiresApi
import com.lloyd.attendance.campus.diagnostics.CampusConnectionEvent
import com.lloyd.attendance.campus.diagnostics.CampusDiagnosticsStore

/**
 * Provisions the campus Wi-Fi profile directly into the Android System Wi-Fi stack
 * using the official AOSP [WifiNetworkSuggestion] API (Android 10+ / API 29+).
 *
 * Guarantees:
 * - Does not fight AOSP ThroughputScorer or roaming algorithms.
 * - Requests [setIsAppInteractionRequired(true)] so Android notifies the app via
 *   [ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION] to immediately trigger captive authentication.
 */
class CampusNetworkProvisioner(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    data class ProvisionResult(
        val isSuccess: Boolean,
        val statusCode: Int = 0,
        val message: String
    )

    fun provisionCampusNetwork(
        profile: CampusNetworkProfile = CampusNetworkProfile.DEFAULT_PROFILE,
        username: String? = null,
        password: String? = null
    ): ProvisionResult {
        if (wifiManager == null) {
            return ProvisionResult(false, -1, "Wi-Fi hardware manager unavailable")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return provisionSuggestionApi29(profile, username, password)
        }

        return ProvisionResult(
            isSuccess = true,
            statusCode = 0,
            message = "Legacy Android system will use standard Wi-Fi network association"
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun provisionSuggestionApi29(
        profile: CampusNetworkProfile,
        username: String?,
        password: String?
    ): ProvisionResult {
        try {
            val builder = WifiNetworkSuggestion.Builder()
                .setSsid(profile.ssid)
                .setIsAppInteractionRequired(true) // Crucial: prompts post-connection broadcast to app
                .setIsUserInteractionRequired(false)

            when (profile.securityType) {
                CampusNetworkProfile.SecurityType.OPEN_OR_PORTAL -> {
                    // Open Wi-Fi that redirects to portal
                }
                CampusNetworkProfile.SecurityType.WPA2_PSK -> {
                    if (!password.isNullOrBlank()) {
                        builder.setWpa2Passphrase(password)
                    }
                }
                CampusNetworkProfile.SecurityType.WPA3_SAE -> {
                    if (!password.isNullOrBlank()) {
                        builder.setWpa3Passphrase(password)
                    }
                }
                CampusNetworkProfile.SecurityType.WPA2_ENTERPRISE_PEAP -> {
                    if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                        val enterpriseConfig = WifiEnterpriseConfig().apply {
                            eapMethod = WifiEnterpriseConfig.Eap.PEAP
                            phase2Method = WifiEnterpriseConfig.Phase2.MSCHAPV2
                            identity = username
                            this.password = password
                        }
                        builder.setWpa2EnterpriseConfig(enterpriseConfig)
                    }
                }
            }

            val suggestions = listOf(builder.build())
            val status = wifiManager?.addNetworkSuggestions(suggestions) ?: -1

            val isSuccess = status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
            val msg = when (status) {
                WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS -> "Campus Wi-Fi provisioned successfully"
                WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE -> "Campus network already provisioned"
                WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP -> "Exceeded max network suggestions limit"
                WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED -> "Wi-Fi suggestions disallowed by system"
                else -> "Wi-Fi suggestion returned code: $status"
            }

            CampusDiagnosticsStore.instance.record(
                if (isSuccess) CampusConnectionEvent.Type.WIFI_ASSOCIATED else CampusConnectionEvent.Type.DIRECT_HTTP_FAILURE,
                "Provision Suggestion (${profile.ssid})",
                msg
            )

            return ProvisionResult(isSuccess, status, msg)
        } catch (e: Exception) {
            return ProvisionResult(false, -1, "Exception adding Wi-Fi suggestion: ${e.message}")
        }
    }

    fun removeSuggestions(profile: CampusNetworkProfile = CampusNetworkProfile.DEFAULT_PROFILE) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && wifiManager != null) {
            try {
                val builder = WifiNetworkSuggestion.Builder().setSsid(profile.ssid).build()
                wifiManager.removeNetworkSuggestions(listOf(builder))
            } catch (ignored: Exception) {
            }
        }
    }
}
