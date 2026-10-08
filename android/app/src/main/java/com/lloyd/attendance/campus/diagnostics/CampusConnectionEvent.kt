package com.lloyd.attendance.campus.diagnostics

/**
 * Diagnostic log entry recorded during network transitions and authentication attempts.
 */
data class CampusConnectionEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val type: Type,
    val summary: String,
    val details: String? = null
) {
    enum class Type {
        WIFI_ASSOCIATED,
        WIFI_DISCONNECTED,
        CAPTIVE_DETECTED,
        DIRECT_HTTP_ATTEMPT,
        DIRECT_HTTP_SUCCESS,
        DIRECT_HTTP_FAILURE,
        VALIDATION_CONFIRMED,
        CIRCUIT_BREAKER_TRIGGERED,
        RECOVERY_SCHEDULED,
        DATA_STALL,
        CREDENTIALS_SYNCED
    }
}
