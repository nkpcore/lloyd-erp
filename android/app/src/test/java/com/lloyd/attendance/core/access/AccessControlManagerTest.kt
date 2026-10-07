package com.lloyd.attendance.core.access

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessControlManagerTest {

    private val baseConfig = FleetConfig(
        minVersionCode = 14,
        latestVersionName = "v1.0.15",
        bannedStudents = emptyList(),
        bannedDevices = listOf("banned-dev-uuid-999"),
        broadcastNotice = "Class schedule updated for Friday",
        maintenanceMode = false,
        maintenanceMessage = null,
        downloadUrl = "https://github.com/nkpcore/lloyd-erp/releases/latest"
    )

    @Test
    fun testAuthorizedStudent_returnsAuthorizedWithBroadcastNotice() {
        val decision = AccessControlManager.evaluateAccess(
            config = baseConfig,
            studentId = 10001,
            deviceId = "valid-device-uuid",
            currentVersionCode = 15
        )

        assertTrue(decision is AccessDecision.Authorized)
        assertEquals("Class schedule updated for Friday", (decision as AccessDecision.Authorized).broadcastNotice)
    }

    @Test
    fun testSameStudentOnDifferentDevice_returnsAuthorized() {
        // MANDATE: Revoking one device must NEVER block the student globally across other devices.
        val decision = AccessControlManager.evaluateAccess(
            config = baseConfig,
            studentId = 10001,
            deviceId = "valid-device-uuid-phone-2",
            currentVersionCode = 15
        )

        assertTrue(decision is AccessDecision.Authorized)
    }

    @Test
    fun testBannedDeviceUuid_returnsRevoked() {
        val decision = AccessControlManager.evaluateAccess(
            config = baseConfig,
            studentId = 10001,
            deviceId = "banned-dev-uuid-999",
            currentVersionCode = 15
        )

        assertTrue(decision is AccessDecision.Revoked)
        val revoked = decision as AccessDecision.Revoked
        assertTrue(revoked.reason.contains("device"))
    }

    @Test
    fun testOutdatedVersionCode_returnsOutdatedVersionWithDownloadUrl() {
        val decision = AccessControlManager.evaluateAccess(
            config = baseConfig,
            studentId = 10001,
            deviceId = "valid-device-uuid",
            currentVersionCode = 13 // Below minVersionCode (14)
        )

        assertTrue(decision is AccessDecision.OutdatedVersion)
        val outdated = decision as AccessDecision.OutdatedVersion
        assertEquals(14, outdated.minVersionCode)
        assertEquals("https://github.com/nkpcore/lloyd-erp/releases/latest", outdated.downloadUrl)
    }

    @Test
    fun testMaintenanceMode_returnsMaintenance() {
        val maintenanceConfig = baseConfig.copy(
            maintenanceMode = true,
            maintenanceMessage = "Server maintenance in progress"
        )
        val decision = AccessControlManager.evaluateAccess(
            config = maintenanceConfig,
            studentId = 10001,
            deviceId = "valid-device-uuid",
            currentVersionCode = 15
        )

        assertTrue(decision is AccessDecision.Maintenance)
        assertEquals("Server maintenance in progress", (decision as AccessDecision.Maintenance).message)
    }

    @Test
    fun testParseFleetConfig_withMalformedJson_fallsBackGracefully() {
        val config = AccessControlManager.parseFleetConfig("INVALID_NON_JSON")
        assertEquals(1, config.minVersionCode)
        assertTrue(config.bannedStudents.isEmpty())
        assertEquals(false, config.maintenanceMode)
    }
}
