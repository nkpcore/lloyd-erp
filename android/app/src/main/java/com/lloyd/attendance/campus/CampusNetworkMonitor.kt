package com.lloyd.attendance.campus

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.lloyd.attendance.campus.diagnostics.CampusConnectionEvent
import com.lloyd.attendance.campus.diagnostics.CampusDiagnosticsStore

/**
 * Monitors Wi-Fi network connectivity and capabilities changes using official [ConnectivityManager.NetworkCallback].
 */
class CampusNetworkMonitor(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    interface NetworkListener {
        fun onWifiAvailable(network: Network)
        fun onWifiCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities)
        fun onWifiLost(network: Network)
    }

    private var listener: NetworkListener? = null
    private var isRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            CampusDiagnosticsStore.instance.record(
                CampusConnectionEvent.Type.WIFI_ASSOCIATED,
                "Wi-Fi Network Interface Available",
                "Network handle: ${network.networkHandle}"
            )
            listener?.onWifiAvailable(network)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            listener?.onWifiCapabilitiesChanged(network, networkCapabilities)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            CampusDiagnosticsStore.instance.record(
                CampusConnectionEvent.Type.WIFI_DISCONNECTED,
                "Wi-Fi Network Disconnected",
                "Network handle: ${network.networkHandle}"
            )
            listener?.onWifiLost(network)
        }
    }

    fun startMonitoring(listener: NetworkListener) {
        if (isRegistered) return
        this.listener = listener

        try {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

            connectivityManager.registerNetworkCallback(request, networkCallback)
            isRegistered = true
        } catch (e: Exception) {
            CampusDiagnosticsStore.instance.record(
                CampusConnectionEvent.Type.DIRECT_HTTP_FAILURE,
                "NetworkCallback Registration Error",
                e.message
            )
        }
    }

    fun stopMonitoring() {
        if (!isRegistered) return
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (ignored: Exception) {
        }
        isRegistered = false
        listener = null
    }

    fun getActiveWifiNetwork(): Network? {
        val active = connectivityManager.activeNetwork ?: return null
        val caps = connectivityManager.getNetworkCapabilities(active) ?: return null
        return if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) active else null
    }

    fun getNetworkCapabilities(network: Network): NetworkCapabilities? {
        return connectivityManager.getNetworkCapabilities(network)
    }
}
