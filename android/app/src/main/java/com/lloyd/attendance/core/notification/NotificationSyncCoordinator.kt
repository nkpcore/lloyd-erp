package com.lloyd.attendance.core.notification

import com.lloyd.attendance.api.Models
import java.util.Date

/**
 * Abstraction over notification state persistence so bootstrap, detection,
 * and delivery tracking can be verified in pure JVM unit tests.
 */
interface NotificationStateStore {
    fun isBootstrapComplete(): Boolean
    fun setBootstrapComplete(timestamp: Long)
    fun getLastEmptyBootstrapAttempt(): Long
    fun setLastEmptyBootstrapAttempt(timestamp: Long)
    fun setLastNotificationCheckTimestamp(timestamp: Long)

    fun getLastSeenAttendanceId(): Long
    fun saveLastSeenAttendanceId(id: Long)

    fun getDetectedIds(): Set<String>
    fun addDetectedIds(ids: Collection<Long>)
    fun addDetectedSignatures(signatures: Collection<String>)

    fun getDeliveredIds(): Set<String>
    fun addDeliveredIds(ids: Collection<Long>)

    fun saveNotificationEvent(
        id: String,
        subject: String,
        status: String,
        timestamp: Long,
        delivered: Boolean,
        date: String = ""
    )
}

data class NotificationSyncSummary(
    val detectedCount: Int,
    val deliveredIds: List<Long>,
    val bootstrappedNow: Boolean,
    val emptyBootstrapAttemptRecorded: Boolean
)

/**
 * Coordinates bootstrap initialization, new attendance mark detection,
 * delivery tracking, and in-app notification event logging.
 */
object NotificationSyncCoordinator {

    fun processLogs(
        store: NotificationStateStore,
        logs: List<Models.StudentAttendanceItem>?,
        shouldNotify: Boolean,
        syncSucceeded: Boolean = true,
        nowMillis: Long = System.currentTimeMillis(),
        referenceDate: Date? = null,
        postNotification: (AttendanceMarkEvent.Marked) -> Boolean
    ): NotificationSyncSummary {
        store.setLastNotificationCheckTimestamp(nowMillis)

        val isBootstrapped = store.isBootstrapComplete()
        if (!isBootstrapped) {
            if (logs.isNullOrEmpty()) {
                if (syncSucceeded) {
                    store.setLastEmptyBootstrapAttempt(nowMillis)
                }
                return NotificationSyncSummary(
                    detectedCount = 0,
                    deliveredIds = emptyList(),
                    bootstrappedNow = false,
                    emptyBootstrapAttemptRecorded = syncSucceeded
                )
            } else if (store.getLastEmptyBootstrapAttempt() <= 0L) {
                // First time ever with existing historical records: mark all as detected without notifying.
                val allLogIds = logs.mapNotNull { if (it.id > 0) it.id else null }
                val allLogSigs = logs.map { AttendanceChangeDetector.getRecordSignature(it) }
                val maxLogId = allLogIds.maxOrNull() ?: 0L
                store.addDetectedIds(allLogIds)
                store.addDetectedSignatures(allLogSigs)
                if (maxLogId > 0L) {
                    store.saveLastSeenAttendanceId(maxLogId)
                }
                store.setBootstrapComplete(nowMillis)
                return NotificationSyncSummary(
                    detectedCount = 0,
                    deliveredIds = emptyList(),
                    bootstrappedNow = true,
                    emptyBootstrapAttemptRecorded = false
                )
            } else {
                // Previously saw an empty response: account started empty, so newly arrived marks
                // are genuine new marks and must NOT be suppressed.
                store.setBootstrapComplete(nowMillis)
            }
        }

        if (logs.isNullOrEmpty()) {
            return NotificationSyncSummary(
                detectedCount = 0,
                deliveredIds = emptyList(),
                bootstrappedNow = false,
                emptyBootstrapAttemptRecorded = false
            )
        }

        val detectedIds = store.getDetectedIds()
        val lastSeenId = store.getLastSeenAttendanceId()
        val allLogIds = logs.mapNotNull { if (it.id > 0) it.id else null }
        val allLogSigs = logs.map { AttendanceChangeDetector.getRecordSignature(it) }
        val maxLogId = allLogIds.maxOrNull() ?: 0L

        val newlyMarkedItems = AttendanceChangeDetector.detectNewMarks(
            logs = logs,
            seenIds = detectedIds,
            isFirstBootstrap = false,
            lastSeenId = lastSeenId,
            enforceDateFilter = true,
            referenceDate = referenceDate
        )

        // Persist all evaluated IDs to detectedIds to prevent re-evaluation on subsequent syncs.
        store.addDetectedIds(allLogIds)
        store.addDetectedSignatures(allLogSigs)
        if (maxLogId > lastSeenId) {
            store.saveLastSeenAttendanceId(maxLogId)
        }

        val deliveredBatch = mutableListOf<Long>()
        for (newMark in newlyMarkedItems) {
            val delivered = if (shouldNotify) {
                try {
                    postNotification(newMark)
                } catch (_: Exception) {
                    false
                }
            } else {
                false
            }
            if (delivered) {
                deliveredBatch.add(newMark.attendanceId)
            }
            store.saveNotificationEvent(
                id = newMark.attendanceId.toString(),
                subject = newMark.subjectName,
                status = if (newMark.isPresent) "Present" else "Absent",
                timestamp = nowMillis,
                delivered = delivered,
                date = newMark.attendanceDate
            )
        }

        if (deliveredBatch.isNotEmpty()) {
            store.addDeliveredIds(deliveredBatch)
        }

        return NotificationSyncSummary(
            detectedCount = newlyMarkedItems.size,
            deliveredIds = deliveredBatch,
            bootstrappedNow = false,
            emptyBootstrapAttemptRecorded = false
        )
    }
}

