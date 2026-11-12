package com.lloyd.attendance.ui

import android.content.Intent
import com.lloyd.attendance.widget.NotificationHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class NavigationRequest(
    val tab: Int,
    val date: LocalDate? = null,
    val recordId: String? = null,
    val subjectId: String? = null
) {
    companion object {
        private val SUPPORTED_DATE_FORMATTERS = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
        )

        fun parseAttendanceDate(raw: String?): LocalDate? {
            val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val candidate = if (trimmed.contains("T")) trimmed.substringBefore("T") else trimmed.take(10)
            for (formatter in SUPPORTED_DATE_FORMATTERS) {
                try {
                    return LocalDate.parse(candidate, formatter)
                } catch (_: Exception) {
                }
            }
            return null
        }

        fun fromExtras(
            navTarget: String?,
            attendanceDate: String?,
            recordId: String?,
            subjectId: String?
        ): NavigationRequest? {
            if (navTarget != NotificationHelper.NAV_TARGET_LOGS) return null
            return NavigationRequest(
                tab = MainTab.LOGS.ordinal,
                date = parseAttendanceDate(attendanceDate),
                recordId = recordId?.trim()?.takeIf { it.isNotEmpty() },
                subjectId = subjectId?.trim()?.takeIf { it.isNotEmpty() }
            )
        }

        fun fromIntent(intent: Intent?): NavigationRequest? {
            if (intent == null) return null
            return fromExtras(
                navTarget = intent.getStringExtra(NotificationHelper.EXTRA_NAV_TARGET),
                attendanceDate = intent.getStringExtra(NotificationHelper.EXTRA_ATTENDANCE_DATE),
                recordId = intent.getStringExtra(NotificationHelper.EXTRA_RECORD_ID),
                subjectId = intent.getStringExtra(NotificationHelper.EXTRA_SUBJECT_ID)
            )
        }
    }
}

