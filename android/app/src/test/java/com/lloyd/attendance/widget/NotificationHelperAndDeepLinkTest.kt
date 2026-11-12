package com.lloyd.attendance.widget

import android.app.NotificationManager
import com.lloyd.attendance.ui.MainTab
import com.lloyd.attendance.ui.NavigationRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class NotificationHelperAndDeepLinkTest {

    @Test
    fun channelLabels_doNotOverpromiseAndUseHonestCopy() {
        assertEquals("Attendance Updates", NotificationHelper.CHANNEL_NAME)
        assertEquals(
            "Background attendance checks (checks every 60 minutes, delivery not guaranteed due to Android battery optimization)",
            NotificationHelper.CHANNEL_DESCRIPTION
        )
        assertFalse(NotificationHelper.CHANNEL_NAME.contains("Live", ignoreCase = true))
        assertFalse(NotificationHelper.CHANNEL_DESCRIPTION.contains("Instant", ignoreCase = true))
    }

    @Test
    fun channelImportance_isHighOnlyForSameDayMarks_andDefaultOtherwise() {
        val now = Date()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }.format(now)

        assertEquals(
            NotificationManager.IMPORTANCE_HIGH,
            NotificationHelper.computeNotificationImportance(today, isAttendanceMark = true, referenceDate = now)
        )
        assertEquals(
            NotificationManager.IMPORTANCE_DEFAULT,
            NotificationHelper.computeNotificationImportance("2024-01-15", isAttendanceMark = true, referenceDate = now)
        )
        assertEquals(
            NotificationManager.IMPORTANCE_DEFAULT,
            NotificationHelper.computeNotificationImportance(null, isAttendanceMark = true, referenceDate = now)
        )
        assertEquals(
            NotificationManager.IMPORTANCE_DEFAULT,
            NotificationHelper.computeNotificationImportance(today, isAttendanceMark = false, referenceDate = now)
        )
    }

    @Test
    fun navigationRequest_parsesAllDeepLinkExtras() {
        val req = NavigationRequest.fromExtras(
            navTarget = NotificationHelper.NAV_TARGET_LOGS,
            attendanceDate = "2026-11-12T09:30:00",
            recordId = "9876",
            subjectId = "42"
        )

        assertEquals(MainTab.LOGS.ordinal, req?.tab)
        assertEquals(LocalDate.of(2026, 11, 12), req?.date)
        assertEquals("9876", req?.recordId)
        assertEquals("42", req?.subjectId)
    }

    @Test
    fun navigationRequest_returnsNullForUnknownTarget_andHandlesInvalidDateGracefully() {
        assertNull(
            NavigationRequest.fromExtras(
                navTarget = "UNKNOWN",
                attendanceDate = "2026-11-12",
                recordId = "1",
                subjectId = "2"
            )
        )

        val partial = NavigationRequest.fromExtras(
            navTarget = NotificationHelper.NAV_TARGET_LOGS,
            attendanceDate = "not-a-date",
            recordId = " ",
            subjectId = null
        )
        assertEquals(MainTab.LOGS.ordinal, partial?.tab)
        assertNull(partial?.date)
        assertNull(partial?.recordId)
        assertNull(partial?.subjectId)
    }
}

