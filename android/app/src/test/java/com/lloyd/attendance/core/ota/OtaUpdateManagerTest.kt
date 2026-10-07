package com.lloyd.attendance.core.ota

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtaUpdateManagerTest {

    @Test
    fun testIsNewerVersion() {
        assertTrue(OtaUpdateManager.isNewerVersion("v1.0.15", "1.0.14"))
        assertTrue(OtaUpdateManager.isNewerVersion("1.1.0", "1.0.99"))
        assertFalse(OtaUpdateManager.isNewerVersion("v1.0.14", "1.0.14"))
        assertFalse(OtaUpdateManager.isNewerVersion("v1.0.13", "1.0.14"))
        assertFalse(OtaUpdateManager.isNewerVersion("1.0.14", "1.0.14"))
    }

    @Test
    fun testCleanVersionString() {
        assertTrue(OtaUpdateManager.cleanVersion("v1.2.3") == "1.2.3")
        assertTrue(OtaUpdateManager.cleanVersion("  V2.0.0 ") == "2.0.0")
        assertTrue(OtaUpdateManager.cleanVersion("1.0.0") == "1.0.0")
    }
}
