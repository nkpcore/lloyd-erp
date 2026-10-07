package com.lloyd.attendance.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lloyd.attendance.core.designsystem.theme.AttendanceColors
import com.lloyd.attendance.core.domain.AttendanceHealth

import androidx.compose.foundation.shape.CircleShape

/**
 * Material 3 Status Badge with semantic tonal container styling and pill shape.
 */
@Composable
fun AttendanceHealthBadge(
    health: AttendanceHealth,
    modifier: Modifier = Modifier,
    customText: String? = null
) {
    val isDark = isSystemInDarkTheme()
    val (bgColor, textColor, defaultText) = when (health) {
        AttendanceHealth.HEALTHY -> Triple(
            if (isDark) AttendanceColors.HealthyContainerDark else AttendanceColors.HealthyContainerLight,
            if (isDark) AttendanceColors.OnHealthyContainerDark else AttendanceColors.OnHealthyContainerLight,
            "Healthy"
        )
        AttendanceHealth.BORDERLINE -> Triple(
            if (isDark) AttendanceColors.BorderlineContainerDark else AttendanceColors.BorderlineContainerLight,
            if (isDark) AttendanceColors.OnBorderlineContainerDark else AttendanceColors.OnBorderlineContainerLight,
            "Borderline"
        )
        AttendanceHealth.CRITICAL -> Triple(
            if (isDark) AttendanceColors.CriticalContainerDark else AttendanceColors.CriticalContainerLight,
            if (isDark) AttendanceColors.OnCriticalContainerDark else AttendanceColors.OnCriticalContainerLight,
            "Critical"
        )
        AttendanceHealth.UNRECORDED -> Triple(
            AttendanceColors.UnrecordedContainer,
            AttendanceColors.Unrecorded,
            "No Records"
        )
    }

    Box(
        modifier = modifier
            .background(bgColor, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = customText ?: defaultText,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun StatusBadge(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(containerColor, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

