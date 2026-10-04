package com.lloyd.attendance.widget

import org.junit.Assert.assertFalse
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
}
