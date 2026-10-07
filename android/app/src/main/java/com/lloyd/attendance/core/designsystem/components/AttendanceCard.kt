package com.lloyd.attendance.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lloyd.attendance.core.designsystem.theme.AttendanceColors
import com.lloyd.attendance.core.domain.AttendanceHealth
import com.lloyd.attendance.core.domain.AttendancePercentage
import com.lloyd.attendance.core.domain.SubjectAttendance

@Composable
fun AttendanceCard(
    subject: SubjectAttendance,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cardShape = MaterialTheme.shapes.large
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(if (isDark) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape) else Modifier),
        shape = cardShape,
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                    if (!subject.teacherName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subject.teacherName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Circular Progress Indicator with Percentage Display
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (val pct = subject.percentage) {
                        is AttendancePercentage.Value -> {
                            val targetProgress = (pct.percentage / 100.0).toFloat().coerceIn(0f, 1f)
                            val animatedProgress by animateFloatAsState(
                                targetValue = targetProgress,
                                animationSpec = tween(durationMillis = 800),
                                label = "progressAnimation"
                            )

                            val strokeColor = when (subject.health) {
                                AttendanceHealth.HEALTHY -> if (isDark) AttendanceColors.HealthyDark else AttendanceColors.HealthyLight
                                AttendanceHealth.BORDERLINE -> if (isDark) AttendanceColors.BorderlineDark else AttendanceColors.BorderlineLight
                                AttendanceHealth.CRITICAL -> if (isDark) AttendanceColors.CriticalDark else AttendanceColors.CriticalLight
                                AttendanceHealth.UNRECORDED -> MaterialTheme.colorScheme.outline
                            }

                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.size(56.dp),
                                color = strokeColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeWidth = 5.dp,
                                strokeCap = StrokeCap.Round
                            )

                            Text(
                                text = String.format("%.0f%%", pct.percentage),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        is AttendancePercentage.NoData -> {
                            CircularProgressIndicator(
                                progress = { 0f },
                                modifier = Modifier.size(56.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeWidth = 5.dp
                            )
                            Text(
                                text = "--%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Attendance Count Fraction and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${subject.presentCount} / ${subject.totalClasses} classes attended",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AttendanceHealthBadge(health = subject.health)
            }

            // Target Advisory Chips / Guidance
            val t75 = subject.thresholds[75]
            if (t75 != null && subject.totalClasses > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (t75.canBunkWhileMaintaining > 0) {
                        StatusBadge(
                            text = "Can bunk ${t75.canBunkWhileMaintaining} class(es)",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    } else if (t75.classesNeededToReach > 0) {
                        StatusBadge(
                            text = "Need next ${t75.classesNeededToReach} class(es)",
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    } else {
                        StatusBadge(
                            text = "On the line (75%)",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}
