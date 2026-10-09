package com.lloyd.attendance.campus

import android.net.Network
import android.net.NetworkCapabilities
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Intelligent Captive Portal Detector.
 *
 * Uses Android's official NetworkCapabilities as the primary source of truth:
 * - Captive: NET_CAPABILITY_CAPTIVE_PORTAL == true || (INTERNET && !VALIDATED)
 * - Online:  NET_CAPABILITY_VALIDATED == true
 *
 * Fallback probe resolves the exact redirect target URL through an isolated network-bound request.
 */
class CampusPortalDetector {

    /**
     * Checks if the capabilities describe a captive portal network.
     */
    fun isCaptivePortal(capabilities: NetworkCapabilities?): Boolean {
        if (capabilities == null) return false
        val hasCaptive = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        return hasCaptive || (hasInternet && !isValidated)
    }

    /**
     * Checks if the network is fully validated and internet-ready.
     */
    fun isValidated(capabilities: NetworkCapabilities?): Boolean {
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    }

    /**
     * Resolves the captive portal login URL by issuing a network-bound probe
     * to Google's generate_204 endpoint and capturing the redirect URL.
     */
    suspend fun resolvePortalUrl(network: Network, fallbackBaseUrl: String? = null): String {
        return try {
            val client = OkHttpClient.Builder()
                .socketFactory(network.socketFactory)
                .dns(object : okhttp3.Dns {
                    override fun lookup(hostname: String): List<java.net.InetAddress> {
                        return network.getAllByName(hostname).toList()
                    }
                })
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(6, TimeUnit.SECONDS)
                .followRedirects(false) // Intercept first redirect to capture exact portal location
                .followSslRedirects(false)
                .build()

            val request = Request.Builder()
                .url(PROBE_URL)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val response = client.newCall(request).execute()
            val redirectLocation = response.header("Location")
            val code = response.code
            response.close()

            if (!redirectLocation.isNullOrBlank()) {
                redirectLocation
            } else if (code in 300..399 && !redirectLocation.isNullOrBlank()) {
                redirectLocation
            } else {
                // If no redirect header was captured, try following redirect to final destination URL
                resolveFinalRedirectUrl(network) ?: fallbackBaseUrl ?: DEFAULT_PORTAL_FALLBACK
            }
        } catch (e: Exception) {
            fallbackBaseUrl ?: DEFAULT_PORTAL_FALLBACK
        }
    }

    private fun resolveFinalRedirectUrl(network: Network): String? {
        return try {
            val client = OkHttpClient.Builder()
                .socketFactory(network.socketFactory)
                .dns(object : okhttp3.Dns {
                    override fun lookup(hostname: String): List<java.net.InetAddress> {
                        return network.getAllByName(hostname).toList()
                    }
                })
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(6, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()

            val request = Request.Builder().url(PROBE_URL).build()
            val response = client.newCall(request).execute()
            val finalUrl = response.request.url.toString()
            response.close()

            if (!finalUrl.contains("generate_204")) finalUrl else null
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val PROBE_URL = "http://connectivitycheck.gstatic.com/generate_204"
        const val DEFAULT_PORTAL_FALLBACK = "http://172.16.16.16:8090/httpclient.html"
    }
}
