package com.lloyd.attendance.campus.auth

/**
 * Result of a captive portal authentication attempt.
 */
sealed class PortalAuthResult {
    data class Success(
        val message: String = "Captive authentication succeeded",
        val cookies: Map<String, String> = emptyMap(),
        val landingUrl: String? = null
    ) : PortalAuthResult()

    data class InvalidCredentials(
        val message: String = "Incorrect campus Wi-Fi username or password"
    ) : PortalAuthResult()

    data class TransientFailure(
        val message: String,
        val statusCode: Int? = null,
        val retryAfterSeconds: Int = 5
    ) : PortalAuthResult()

    data class RequiresWebView(
        val reason: String,
        val portalUrl: String
    ) : PortalAuthResult()
}
