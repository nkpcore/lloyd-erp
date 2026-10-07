package com.lloyd.attendance.core.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryManagerTest {

    @Test
    fun testBuildTelemetryPayload() {
        val payload = TelemetryManager.buildPayload(
            deviceId = "test-uuid-123",
            studentId = 42,
            studentName = "Student A",
            appVersion = "1.0.14",
            versionCode = 14,
            osVersion = "Android 14 (API 34)",
            deviceModel = "Pixel 8"
        )
        assertEquals("test-uuid-123", payload["device_id"])
        assertEquals(42, payload["student_id"])
        assertEquals("Student A", payload["student_name"])
        assertEquals("1.0.14", payload["app_version"])
        assertEquals(14, payload["version_code"])
        assertEquals("Android 14 (API 34)", payload["os_version"])
        assertEquals("Pixel 8", payload["device_model"])
        assertTrue((payload["timestamp"] as? String)?.isNotBlank() == true)
    }

    @Test
    fun testBuildTelemetryPayload_handlesNullsSafely() {
        val payload = TelemetryManager.buildPayload(
            deviceId = "test-uuid-456",
            studentId = null,
            studentName = null,
            appVersion = "1.0.14",
            versionCode = 14,
            osVersion = "Android 15 (API 35)",
            deviceModel = "Samsung S24"
        )
        assertEquals("test-uuid-456", payload["device_id"])
        assertEquals(null, payload["student_id"])
        assertEquals("", payload["student_name"])
    }
}
