package com.lloyd.attendance.campus

import com.lloyd.attendance.campus.diagnostics.CampusHealthScore
import com.lloyd.attendance.campus.storage.SecureCampusCredentialStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Lloyd Campus Connect core logic:
 * - Circuit breaker & exponential backoff transitions
 * - Campus Health Score calculation & rating states
 * - Duration calculation & string formatting
 */
class CampusConnectEngineTest {

    private lateinit var recoveryController: CampusRecoveryController

    @Before
    fun setUp() {
        recoveryController = CampusRecoveryController()
    }

    @Test
    fun testCircuitBreaker_BackoffSequenceAndTrip() {
        assertTrue("Initially can attempt", recoveryController.canAttempt())
        assertEquals(0, recoveryController.attemptCount)

        // Attempt 1 -> transient fail
        val delay1 = recoveryController.recordTransientFailure("Timeout")
        assertEquals(1, recoveryController.attemptCount)
        assertEquals(0, delay1) // Immediate retry

        // Attempt 2 -> transient fail
        val delay2 = recoveryController.recordTransientFailure("Timeout")
        assertEquals(2, recoveryController.attemptCount)
        assertEquals(2, delay2) // 2s

        // Attempt 3 -> transient fail
        val delay3 = recoveryController.recordTransientFailure("Timeout")
        assertEquals(3, recoveryController.attemptCount)
        assertEquals(5, delay3) // 5s

        // Attempt 4 -> transient fail
        val delay4 = recoveryController.recordTransientFailure("Timeout")
        assertEquals(4, recoveryController.attemptCount)
        assertEquals(15, delay4) // 15s

        // Attempt 5 -> circuit trips open!
        val delay5 = recoveryController.recordTransientFailure("Timeout")
        assertEquals(-1, delay5)
        assertTrue("Circuit must be tripped open", recoveryController.isCircuitOpen)
        assertFalse("Cannot attempt when circuit is open", recoveryController.canAttempt())
    }

    @Test
    fun testCircuitBreaker_PermanentFailureOnInvalidCredentials() {
        recoveryController.recordInvalidCredentials("Incorrect credentials")
        assertTrue("Must be marked permanent failure", recoveryController.isPermanentFailure)
        assertTrue("Circuit must be open", recoveryController.isCircuitOpen)
        assertFalse("Cannot attempt after bad credentials", recoveryController.canAttempt())
        assertEquals("Incorrect credentials", recoveryController.failureReason)

        // New network shouldn't clear bad credentials
        recoveryController.resetForNewNetwork()
        assertFalse("Must still remain blocked after network change", recoveryController.canAttempt())

        // User explicit reset clears it
        recoveryController.resetAll()
        assertTrue("Reset all should allow new attempt", recoveryController.canAttempt())
    }

    @Test
    fun testCampusHealthScore_Ratings() {
        // Disconnected
        val disconnected = CampusHealthScore.DISCONNECTED
        assertEquals(CampusHealthScore.HealthRating.OFFLINE, disconnected.rating)

        // Captive Locked
        val captive = CampusHealthScore(
            rssiDbm = -60,
            linkSpeedMbps = 100,
            isInternetValidated = false,
            isCaptivePortal = true
        )
        assertEquals(CampusHealthScore.HealthRating.CAPTIVE_LOCKED, captive.rating)

        // Data Stall
        val stalled = CampusHealthScore(
            rssiDbm = -60,
            linkSpeedMbps = 100,
            isInternetValidated = true,
            dataStallCount = 2
        )
        assertEquals(CampusHealthScore.HealthRating.STALLED, stalled.rating)

        // Excellent
        val excellent = CampusHealthScore(
            rssiDbm = -55,
            linkSpeedMbps = 150,
            isInternetValidated = true,
            isErpReachable = true
        )
        assertEquals(CampusHealthScore.HealthRating.EXCELLENT, excellent.rating)

        // Good
        val good = CampusHealthScore(
            rssiDbm = -70,
            linkSpeedMbps = 35,
            isInternetValidated = true,
            isErpReachable = false
        )
        assertEquals(CampusHealthScore.HealthRating.GOOD, good.rating)

        // Fair
        val fair = CampusHealthScore(
            rssiDbm = -82,
            linkSpeedMbps = 15,
            isInternetValidated = true
        )
        assertEquals(CampusHealthScore.HealthRating.FAIR, fair.rating)

        // Poor
        val poor = CampusHealthScore(
            rssiDbm = -88,
            linkSpeedMbps = 5,
            isInternetValidated = true
        )
        assertEquals(CampusHealthScore.HealthRating.POOR, poor.rating)
    }

    @Test
    fun testCampusNetworkProfile_Defaults() {
        val defaultProfile = CampusNetworkProfile.DEFAULT_PROFILE
        assertEquals("Lloyd-Student", defaultProfile.ssid)
        assertEquals(CampusNetworkProfile.LoginMethod.DIRECT_HTTP, defaultProfile.loginMethod)
        assertEquals("username", defaultProfile.usernameField)
        assertEquals("password", defaultProfile.passwordField)
    }
}
