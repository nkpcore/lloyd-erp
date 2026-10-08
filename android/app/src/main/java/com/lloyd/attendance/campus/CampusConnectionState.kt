package com.lloyd.attendance.campus

import android.net.Network
import com.lloyd.attendance.campus.diagnostics.CampusHealthScore

/**
 * Finite State Machine representing the lifecycle of the campus connection.
 */
sealed class CampusConnectionState {

    data object Disconnected : CampusConnectionState()

    data class ConnectingWifi(
        val ssid: String? = null
    ) : CampusConnectionState()

    data class WifiConnected(
        val ssid: String,
        val network: Network
    ) : CampusConnectionState()

    data class CaptiveDetected(
        val ssid: String,
        val portalUrl: String,
        val network: Network
    ) : CampusConnectionState()

    data class Authenticating(
        val ssid: String,
        val method: String = "Direct HTTP",
        val attempt: Int = 1
    ) : CampusConnectionState()

    data class WaitingValidation(
        val ssid: String,
        val network: Network
    ) : CampusConnectionState()

    data class Online(
        val ssid: String,
        val sessionStartTime: Long = System.currentTimeMillis(),
        val healthScore: CampusHealthScore = CampusHealthScore()
    ) : CampusConnectionState()

    data class AuthFailedPermanent(
        val reason: String
    ) : CampusConnectionState()

    data class AuthFailedTransient(
        val reason: String,
        val nextRetrySeconds: Int,
        val attempt: Int
    ) : CampusConnectionState()
}
