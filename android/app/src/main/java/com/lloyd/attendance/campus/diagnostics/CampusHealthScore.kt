package com.lloyd.attendance.campus.diagnostics

/**
 * Real-time composite health assessment of the campus connection.
 * Distinguishes "Wi-Fi link healthy vs Portal captive vs ERP reachable".
 */
data class CampusHealthScore(
    val rssiDbm: Int = -127,
    val linkSpeedMbps: Int = 0,
    val isInternetValidated: Boolean = false,
    val isCaptivePortal: Boolean = false,
    val isErpReachable: Boolean = false,
    val erpLatencyMs: Long = -1L,
    val dataStallCount: Int = 0,
    val dnsLatencyMs: Long = -1L,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class HealthRating {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR,
        STALLED,
        CAPTIVE_LOCKED,
        OFFLINE
    }

    val rating: HealthRating
        get() {
            if (!isInternetValidated && isCaptivePortal) return HealthRating.CAPTIVE_LOCKED
            if (rssiDbm <= -90 || linkSpeedMbps <= 0) return HealthRating.OFFLINE
            if (dataStallCount > 0) return HealthRating.STALLED
            if (!isInternetValidated) return HealthRating.POOR

            return when {
                rssiDbm >= -65 && linkSpeedMbps >= 50 && isErpReachable -> HealthRating.EXCELLENT
                rssiDbm >= -75 && linkSpeedMbps >= 20 -> HealthRating.GOOD
                rssiDbm >= -85 -> HealthRating.FAIR
                else -> HealthRating.POOR
            }
        }

    val diagnosticSummary: String
        get() {
            return when (rating) {
                HealthRating.EXCELLENT -> "Optimal Connection (${linkSpeedMbps} Mbps, ${rssiDbm} dBm, ERP Live)"
                HealthRating.GOOD -> "Stable Connection (${linkSpeedMbps} Mbps, ${rssiDbm} dBm)"
                HealthRating.FAIR -> "Fair Signal (${rssiDbm} dBm). Consider moving closer to AP"
                HealthRating.POOR -> "Weak Wi-Fi Link (${rssiDbm} dBm, ${linkSpeedMbps} Mbps)"
                HealthRating.STALLED -> "Data Stall Detected (Packets Dropping / DNS Timeout)"
                HealthRating.CAPTIVE_LOCKED -> "Captive Portal Barrier Active. Authentication Required"
                HealthRating.OFFLINE -> "Disconnected from Campus Wi-Fi"
            }
        }

    companion object {
        val DISCONNECTED = CampusHealthScore()
    }
}
