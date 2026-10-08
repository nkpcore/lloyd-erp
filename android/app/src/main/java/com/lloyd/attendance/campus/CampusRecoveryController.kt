package com.lloyd.attendance.campus

/**
 * Circuit Breaker and Exponential Backoff Controller.
 *
 * Prevents battery drain and portal flooding during outages:
 * - Attempt 1: 0s (immediate)
 * - Attempt 2: 2s
 * - Attempt 3: 5s
 * - Attempt 4: 15s
 * - Attempt 5: 30s
 * - Trip: Circuit Opens -> Ceases retries until next network transition or user tap.
 * - Permanent Trip: On invalid credentials (HTTP 401 / bad password), stops immediately.
 */
class CampusRecoveryController {

    private val backoffSchedule = listOf(0, 2, 5, 15, 30)

    var attemptCount: Int = 0
        private set

    var isCircuitOpen: Boolean = false
        private set

    var isPermanentFailure: Boolean = false
        private set

    var failureReason: String? = null
        private set

    fun canAttempt(): Boolean {
        return !isCircuitOpen && !isPermanentFailure
    }

    fun getNextBackoffSeconds(): Int {
        val index = (attemptCount - 1).coerceIn(0, backoffSchedule.size - 1)
        return backoffSchedule[index]
    }

    fun recordSuccess() {
        attemptCount = 0
        isCircuitOpen = false
        isPermanentFailure = false
        failureReason = null
    }

    fun recordInvalidCredentials(reason: String) {
        isPermanentFailure = true
        isCircuitOpen = true
        failureReason = reason
    }

    fun recordTransientFailure(reason: String): Int {
        attemptCount++
        failureReason = reason
        if (attemptCount >= backoffSchedule.size) {
            isCircuitOpen = true
            return -1 // Circuit tripped open
        }
        return getNextBackoffSeconds()
    }

    fun resetForNewNetwork() {
        attemptCount = 0
        isCircuitOpen = false
        // Keep permanent failure if it was bad credentials, unless explicitly cleared by reset()
    }

    fun resetAll() {
        attemptCount = 0
        isCircuitOpen = false
        isPermanentFailure = false
        failureReason = null
    }
}
