package com.lloyd.attendance.widget

import com.lloyd.attendance.api.Models
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AttendanceWidgetProviderTest {

    @Before
    fun setUp() {
        AttendanceWidgetProvider.resetThrottleForTesting()
    }

    @Test
    fun `canRefresh allows initial refresh`() {
        assertTrue("Initial refresh attempt should succeed", AttendanceWidgetProvider.canRefresh())
    }

    @Test
    fun `canRefresh throttles rapid consecutive calls within debounce window`() {
        assertTrue("First call succeeds", AttendanceWidgetProvider.canRefresh())
        assertFalse("Immediate second call within 15 seconds must be throttled", AttendanceWidgetProvider.canRefresh())
        assertFalse("Immediate third call must also be throttled", AttendanceWidgetProvider.canRefresh())
    }

    @Test
    fun `resetThrottleForTesting clears throttle window`() {
        assertTrue("First call succeeds", AttendanceWidgetProvider.canRefresh())
        assertFalse("Immediate second call throttled", AttendanceWidgetProvider.canRefresh())

        AttendanceWidgetProvider.resetThrottleForTesting()
        assertTrue("Call after throttle reset must succeed", AttendanceWidgetProvider.canRefresh())
    }

    private fun sampleStats(present: Int, total: Int): Models.CalculatedStats {
        return Models.CalculatedStats().apply {
            studentName = "Student #101"
            totalPresent = present
            totalClasses = total
            totalAbsent = (total - present).coerceAtLeast(0)
            overallPercentage = if (total > 0) (present * 100.0) / total else 100.0
            isSafe = overallPercentage >= 75.0
            lastUpdatedMillis = 1700000000000L
        }
    }

    @Test
    fun `refreshing preserves main attendance badge and sets secondary sync status`() {
        val stats = sampleStats(present = 16, total = 20)
        val model = AttendanceWidgetProvider.resolveWidgetUiModel(
            stats = stats,
            isRefreshing = true,
            customStatus = null
        )

        assertEquals(WidgetAttendanceStatus.SAFE, model.attendanceStatus)
        assertEquals(WidgetSyncStatus.REFRESHING, model.syncStatus)
        assertEquals("SAFE (>= 75%)", model.badgeText)
        assertEquals("↻ Syncing", model.syncIndicatorText)
        assertEquals("Syncing...", model.updatedText)
    }

    @Test
    fun `offline failure with cached stats keeps shortage badge and shows cached indicator`() {
        val stats = sampleStats(present = 10, total = 20)
        val model = AttendanceWidgetProvider.resolveWidgetUiModel(
            stats = stats,
            isRefreshing = false,
            customStatus = "Offline: Network error"
        )

        assertEquals(WidgetAttendanceStatus.SHORTAGE, model.attendanceStatus)
        assertEquals(WidgetSyncStatus.OFFLINE, model.syncStatus)
        assertEquals("SHORTAGE (< 75%)", model.badgeText)
        assertEquals("⚠ Cached", model.syncIndicatorText)
    }

    @Test
    fun `customStatus shown only when stats are null`() {
        val model = AttendanceWidgetProvider.resolveWidgetUiModel(
            stats = null,
            isRefreshing = false,
            customStatus = "Session expired"
        )

        assertEquals(WidgetAttendanceStatus.UNKNOWN, model.attendanceStatus)
        assertEquals("SESSION EXPIRED", model.badgeText)
        assertEquals("Session expired", model.classesText)
        assertNull(model.syncIndicatorText)
    }
}
