package com.lloyd.attendance.core.notification

import com.lloyd.attendance.api.Models
import com.lloyd.attendance.data.NotificationEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class NotificationSyncCoordinatorTest {

    private class FakeNotificationStateStore : NotificationStateStore {
        var bootstrapDone = false
        var bootstrapTimestamp = 0L
        var emptyAttempt = 0L
        var lastCheck = 0L
        var lastSeenId = 0L
        val detected = LinkedHashSet<String>()
        val delivered = LinkedHashSet<String>()
        val events = mutableListOf<NotificationEvent>()

        override fun isBootstrapComplete(): Boolean = bootstrapDone
        override fun setBootstrapComplete(timestamp: Long) {
            bootstrapDone = true
            bootstrapTimestamp = timestamp
        }
        override fun getLastEmptyBootstrapAttempt(): Long = emptyAttempt
        override fun setLastEmptyBootstrapAttempt(timestamp: Long) {
            emptyAttempt = timestamp
        }
        override fun setLastNotificationCheckTimestamp(timestamp: Long) {
            lastCheck = timestamp
        }
        override fun getLastSeenAttendanceId(): Long = lastSeenId
        override fun saveLastSeenAttendanceId(id: Long) {
            lastSeenId = id
        }
        override fun getDetectedIds(): Set<String> = detected
        override fun addDetectedIds(ids: Collection<Long>) {
            ids.filter { it > 0 }.forEach { detected.add(it.toString()) }
        }
        override fun addDetectedSignatures(signatures: Collection<String>) {
            signatures.filter { it.isNotBlank() }.forEach { detected.add(it.trim()) }
        }
        override fun getDeliveredIds(): Set<String> = delivered
        override fun addDeliveredIds(ids: Collection<Long>) {
            ids.filter { it > 0 }.forEach { delivered.add(it.toString()) }
        }
        override fun saveNotificationEvent(
            id: String,
            subject: String,
            status: String,
            timestamp: Long,
            delivered: Boolean,
            date: String
        ) {
            events.add(NotificationEvent(id, subject, status, timestamp, delivered, date))
            while (events.size > 50) {
                events.removeAt(0)
            }
        }
    }

    private fun todayStr(now: Date): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }.format(now)
    }

    private fun createItem(
        id: Long,
        subject: String,
        status: String,
        date: String,
        subjectId: Int? = 42
    ): Models.StudentAttendanceItem {
        return Models.StudentAttendanceItem().apply {
            this.id = id
            this.subjectName = subject
            this.status = status
            this.attendanceDate = date
            this.createdByName = "Faculty Member"
            this.classLecture = "1"
            this.subjectId = subjectId
        }
    }

    @Test
    fun emptyFirstSync_doesNotCompleteBootstrap_andFirstRealMarkIsNotSuppressed() {
        val store = FakeNotificationStateStore()
        val now = Date()
        val today = todayStr(now)

        // 1. Initial sync returns empty logs
        val firstSync = NotificationSyncCoordinator.processLogs(
            store = store,
            logs = emptyList(),
            shouldNotify = true,
            syncSucceeded = true,
            nowMillis = 1000L,
            referenceDate = now
        ) { true }

        assertFalse("Empty first sync must not complete bootstrap", store.isBootstrapComplete())
        assertEquals(1000L, store.getLastEmptyBootstrapAttempt())
        assertEquals(1000L, store.lastCheck)
        assertTrue(firstSync.emptyBootstrapAttemptRecorded)
        assertEquals(0, firstSync.detectedCount)

        // 2. Faculty marks the first real class of the semester
        val firstMark = createItem(id = 101L, subject = "Data Structures", status = "Present", date = today, subjectId = 77)
        val postedEvents = mutableListOf<AttendanceMarkEvent.Marked>()
        val secondSync = NotificationSyncCoordinator.processLogs(
            store = store,
            logs = listOf(firstMark),
            shouldNotify = true,
            syncSucceeded = true,
            nowMillis = 2000L,
            referenceDate = now
        ) { ev ->
            postedEvents.add(ev)
            true
        }

        assertTrue("Bootstrap should now complete", store.isBootstrapComplete())
        assertEquals(1, secondSync.detectedCount)
        assertEquals(listOf(101L), secondSync.deliveredIds)
        assertEquals(1, postedEvents.size)
        assertEquals(77, postedEvents[0].subjectId)
        assertTrue(store.getDeliveredIds().contains("101"))
        assertTrue(store.getDetectedIds().contains("101"))
    }

    @Test
    fun nonEmptyFirstSync_bootstrapsHistoryWithoutNotifying() {
        val store = FakeNotificationStateStore()
        val now = Date()
        val today = todayStr(now)
        val logs = listOf(
            createItem(id = 200L, subject = "OS", status = "Present", date = today),
            createItem(id = 199L, subject = "DBMS", status = "Absent", date = today)
        )

        var postedCount = 0
        val summary = NotificationSyncCoordinator.processLogs(
            store = store,
            logs = logs,
            shouldNotify = true,
            syncSucceeded = true,
            nowMillis = 5000L,
            referenceDate = now
        ) {
            postedCount++
            true
        }

        assertTrue(summary.bootstrappedNow)
        assertTrue(store.isBootstrapComplete())
        assertEquals(0, postedCount)
        assertTrue(store.getDetectedIds().containsAll(listOf("199", "200")))
        assertTrue(store.getDeliveredIds().isEmpty())
    }

    @Test
    fun whenPermissionDeniedOrNotificationsDisabled_recordsDetectedAndEventLog_butNotDelivered() {
        val store = FakeNotificationStateStore()
        store.setBootstrapComplete(1000L)
        store.addDetectedIds(listOf(100L))
        store.saveLastSeenAttendanceId(100L)

        val now = Date()
        val today = todayStr(now)
        val newMark = createItem(id = 105L, subject = "Computer Networks", status = "Absent", date = today)

        // Post callback returns false (e.g. POST_NOTIFICATIONS permission denied)
        val summary = NotificationSyncCoordinator.processLogs(
            store = store,
            logs = listOf(newMark),
            shouldNotify = true,
            syncSucceeded = true,
            nowMillis = 6000L,
            referenceDate = now
        ) { false }

        assertEquals(1, summary.detectedCount)
        assertTrue("Delivered IDs must remain empty when notify returns false", summary.deliveredIds.isEmpty())
        assertTrue(store.getDeliveredIds().isEmpty())
        assertTrue("Detected IDs must still record evaluated ID to avoid re-evaluation", store.getDetectedIds().contains("105"))
        assertEquals(1, store.events.size)
        assertFalse(store.events[0].delivered)
        assertEquals("Computer Networks", store.events[0].subject)
        assertEquals("Absent", store.events[0].status)
    }
}

