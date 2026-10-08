package com.lloyd.attendance.campus

import com.lloyd.attendance.campus.storage.SecureCampusCredentialStore
import java.util.concurrent.TimeUnit

/**
 * Manages active campus session lifecycle, duration tracking, and keepalive metrics.
 */
class CampusSessionManager(
    private val credentialStore: SecureCampusCredentialStore
) {
    var sessionStartTime: Long = 0L
        private set

    var isSessionActive: Boolean = false
        private set

    fun startSession() {
        sessionStartTime = System.currentTimeMillis()
        isSessionActive = true
        credentialStore.lastLoginTimestamp = sessionStartTime
    }

    fun endSession() {
        if (isSessionActive && sessionStartTime > 0L) {
            val duration = System.currentTimeMillis() - sessionStartTime
            credentialStore.lastSessionDurationMs = duration
        }
        isSessionActive = false
        sessionStartTime = 0L
    }

    val currentDurationMs: Long
        get() = if (isSessionActive && sessionStartTime > 0L) {
            System.currentTimeMillis() - sessionStartTime
        } else {
            credentialStore.lastSessionDurationMs
        }

    fun formatDuration(durationMs: Long = currentDurationMs): String {
        if (durationMs <= 0L) return "0m"
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }
}
