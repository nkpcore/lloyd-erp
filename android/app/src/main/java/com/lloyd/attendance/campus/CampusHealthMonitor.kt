package com.lloyd.attendance.campus

import android.content.Context
import android.net.ConnectivityDiagnosticsManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.lloyd.attendance.campus.diagnostics.CampusConnectionEvent
import com.lloyd.attendance.campus.diagnostics.CampusDiagnosticsStore
import com.lloyd.attendance.campus.diagnostics.CampusHealthScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Health Diagnostics Monitor.
 *
 * Distinguishes between:
 * 1. Physical Wi-Fi radio degradation (low RSSI / low link speed)
 * 2. Captive portal roadblock
 * 3. Data stalls (TCP retransmissions / DNS timeouts via ConnectivityDiagnosticsManager)
 * 4. ERP cloud unreachable while general internet is live
 */
class CampusHealthMonitor(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val dataStallCounter = AtomicInteger(0)

    init {
        registerDiagnosticsCallbackIfSupported()
    }

    private fun registerDiagnosticsCallbackIfSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val diagManager = context.getSystemService(ConnectivityDiagnosticsManager::class.java)
                diagManager?.registerConnectivityDiagnosticsCallback(
                    android.net.NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build(),
                    context.mainExecutor,
                    object : ConnectivityDiagnosticsManager.ConnectivityDiagnosticsCallback() {
                        override fun onDataStallSuspected(report: ConnectivityDiagnosticsManager.DataStallReport) {
                            super.onDataStallSuspected(report)
                            dataStallCounter.incrementAndGet()
                            CampusDiagnosticsStore.instance.record(
                                CampusConnectionEvent.Type.DATA_STALL,
                                "Data Stall Suspected",
                                "DNS/TCP stall detected by Android OS"
                            )
                        }
                    }
                )
            } catch (ignored: Exception) {
            }
        }
    }

    /**
     * Samples the active network to construct a comprehensive [CampusHealthScore].
     */
    suspend fun evaluateHealth(network: Network?): CampusHealthScore = withContext(Dispatchers.IO) {
        if (network == null) {
            return@withContext CampusHealthScore.DISCONNECTED
        }

        val capabilities = connectivityManager.getNetworkCapabilities(network)
        if (capabilities == null || !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return@withContext CampusHealthScore.DISCONNECTED
        }

        // 1. Resolve RSSI and Link Speed
        var rssi = -127
        var linkSpeed = 0

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val transportInfo = capabilities.transportInfo
            if (transportInfo is WifiInfo) {
                rssi = transportInfo.rssi
                linkSpeed = transportInfo.linkSpeed
            }
        }

        if (rssi == -127 && wifiManager != null) {
            try {
                @Suppress("DEPRECATION")
                val connectionInfo = wifiManager.connectionInfo
                if (connectionInfo != null) {
                    rssi = connectionInfo.rssi
                    linkSpeed = connectionInfo.linkSpeed
                }
            } catch (ignored: Exception) {
            }
        }

        val isInternetValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isCaptive = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)

        // 2. Measure ERP Cloud Reachability via Network-Bound socket
        var isErpReachable = false
        var erpLatencyMs = -1L

        if (isInternetValidated) {
            try {
                val client = OkHttpClient.Builder()
                    .socketFactory(network.socketFactory)
                    .dns(object : okhttp3.Dns {
                        override fun lookup(hostname: String): List<java.net.InetAddress> {
                            return network.getAllByName(hostname).toList()
                        }
                    })
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url("https://erp.lloydcollege.in/api/health")
                    .get()
                    .build()

                val start = System.currentTimeMillis()
                val response = client.newCall(request).execute()
                val latency = System.currentTimeMillis() - start
                val code = response.code
                response.close()

                if (code in 200..404) {
                    isErpReachable = true
                    erpLatencyMs = latency
                }
            } catch (ignored: Exception) {
                isErpReachable = false
            }
        }

        CampusHealthScore(
            rssiDbm = rssi,
            linkSpeedMbps = linkSpeed,
            isInternetValidated = isInternetValidated,
            isCaptivePortal = isCaptive,
            isErpReachable = isErpReachable,
            erpLatencyMs = erpLatencyMs,
            dataStallCount = dataStallCounter.get(),
            timestamp = System.currentTimeMillis()
        )
    }

    fun resetStalls() {
        dataStallCounter.set(0)
    }
}
