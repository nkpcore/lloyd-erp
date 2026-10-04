package com.lloyd.attendance.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lloyd.attendance.core.designsystem.components.AttendanceCard
import com.lloyd.attendance.core.designsystem.components.AttendanceCardSkeleton
import com.lloyd.attendance.core.designsystem.components.AttendanceHealthBadge
import com.lloyd.attendance.core.designsystem.components.OfflineBanner
import com.lloyd.attendance.core.designsystem.components.StatusBadge
import com.lloyd.attendance.core.designsystem.theme.AttendanceColors
import com.lloyd.attendance.core.domain.AttendanceHealth
import com.lloyd.attendance.core.domain.AttendancePercentage
import com.lloyd.attendance.core.domain.BunkAdvisor
import com.lloyd.attendance.core.domain.SubjectAttendance
import com.lloyd.attendance.core.schedule.DayScheduleResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSubjectSimulation: (SubjectAttendance) -> Unit,
    onNavigateToOverallSimulation: (present: Int, total: Int) -> Unit,
    onNavigateToSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val overall = uiState.overall

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (uiState.studentName.isNotBlank()) uiState.studentName else "Lloyd ERP",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (uiState.section.isNotBlank()) {
                            Text(
                                text = "Section ${uiState.section}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !uiState.isRefreshing
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Data"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AttendanceCardSkeleton()
                AttendanceCardSkeleton()
                AttendanceCardSkeleton()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Offline Notice Banner
                if (uiState.isOffline) {
                    item {
                        OfflineBanner(
                            lastUpdatedMillis = uiState.lastSyncMillis,
                            onRetry = { viewModel.refresh() }
                        )
                    }
                }

                // Hero Attendance Summary Card
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "OVERALL ATTENDANCE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = overall.percentage.displayValue,
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${overall.totalPresent} / ${overall.totalClasses} classes attended",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Large Gauge
                                Box(
                                    modifier = Modifier.size(76.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val targetProgress = when (val p = overall.percentage) {
                                        is AttendancePercentage.Value -> (p.percentage / 100.0).toFloat().coerceIn(0f, 1f)
                                        else -> 0f
                                    }
                                    val animatedProgress by animateFloatAsState(
                                        targetValue = targetProgress,
                                        animationSpec = tween(durationMillis = 800),
                                        label = "overallProgress"
                                    )

                                    val strokeColor = when (overall.health) {
                                        AttendanceHealth.HEALTHY -> AttendanceColors.HealthyLight
                                        AttendanceHealth.BORDERLINE -> AttendanceColors.BorderlineLight
                                        AttendanceHealth.CRITICAL -> AttendanceColors.CriticalLight
                                        AttendanceHealth.UNRECORDED -> MaterialTheme.colorScheme.outlineVariant
                                    }

                                    CircularProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier = Modifier.size(76.dp),
                                        color = strokeColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeWidth = 7.dp,
                                        strokeCap = StrokeCap.Round
                                    )

                                    AttendanceHealthBadge(health = overall.health)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Contextual Advice from BunkAdvisor
                            val advice = BunkAdvisor.getAdvice(overall.totalPresent, overall.totalClasses)
                            Text(
                                text = advice.headline,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (advice.isSafe) MaterialTheme.colorScheme.onSurface else AttendanceColors.CriticalLight
                            )
                            Text(
                                text = advice.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Simulate Button
                            FilledTonalButton(
                                onClick = { onNavigateToOverallSimulation(overall.totalPresent, overall.totalClasses) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Interactive Bunk Simulator")
                            }
                        }
                    }
                }

                // Live Class Quick Glance
                val schedule = uiState.scheduleResult
                if (schedule is DayScheduleResult.Success && (schedule.activePeriod != null || schedule.nextPeriod != null)) {
                    item {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onNavigateToSchedule),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        if (schedule.activePeriod != null) {
                                            Text(
                                                text = "HAPPENING NOW",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = schedule.activePeriod.subjectName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${schedule.activePeriod.remainingMinutes}m remaining in ${schedule.activePeriod.roomNumber ?: "class"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else if (schedule.nextPeriod != null) {
                                            Text(
                                                text = "UPCOMING NEXT",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = schedule.nextPeriod.subjectName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Starts at ${schedule.nextPeriod.startTime}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                StatusBadge(text = "View Routine")
                            }
                        }
                    }
                }

                // Section Header: Subject Breakdown
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Course Subjects",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${overall.subjects.size} subjects",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Subject Items
                if (overall.subjects.isEmpty()) {
                    item {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No Subject Records Found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Subject data will appear here once attendance records are published by faculty.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(overall.subjects, key = { it.subjectCode.ifBlank { it.subjectName } }) { subject ->
                        AttendanceCard(
                            subject = subject,
                            onClick = { onNavigateToSubjectSimulation(subject) }
                        )
                    }
                }
            }
        }
    }
}
