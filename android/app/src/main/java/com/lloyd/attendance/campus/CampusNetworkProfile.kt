package com.lloyd.attendance.campus

/**
 * Representation of a campus Wi-Fi network and its captive portal signature.
 * Allows dynamic adaptation to different campus blocks or firewall configurations
 * without hardcoding fixed HTML DOM IDs.
 */
data class CampusNetworkProfile(
    val ssid: String = DEFAULT_CAMPUS_SSID,
    val securityType: SecurityType = SecurityType.OPEN_OR_PORTAL,
    val portalFingerprint: String? = null,
    val portalBaseUrl: String? = null,
    val submitEndpoint: String = "/login",
    val loginMethod: LoginMethod = LoginMethod.DIRECT_HTTP,
    val usernameField: String = "username",
    val passwordField: String = "password",
    val csrfField: String? = null,
    val extraPostFields: Map<String, String> = emptyMap(),
    val successIndicators: List<String> = listOf("logged in", "welcome", "success", "status>1", "keepalive"),
    val failureIndicators: List<String> = listOf("invalid", "incorrect", "denied", "expired", "failed", "error"),
    val sessionCookieNames: List<String> = emptyList()
) {
    enum class SecurityType {
        OPEN_OR_PORTAL,
        WPA2_PSK,
        WPA3_SAE,
        WPA2_ENTERPRISE_PEAP
    }

    enum class LoginMethod {
        DIRECT_HTTP,
        WEBVIEW_FALLBACK,
        EAP_ENTERPRISE
    }

    companion object {
        const val DEFAULT_CAMPUS_SSID = "Lloyd-Student"
        const val DEFAULT_PORTAL_TIMEOUT_SECONDS = 10

        val DEFAULT_PROFILE = CampusNetworkProfile(
            ssid = DEFAULT_CAMPUS_SSID,
            securityType = SecurityType.OPEN_OR_PORTAL,
            loginMethod = LoginMethod.DIRECT_HTTP,
            usernameField = "username",
            passwordField = "password"
        )
    }
}
