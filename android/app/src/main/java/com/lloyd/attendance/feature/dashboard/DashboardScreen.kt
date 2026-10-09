package com.lloyd.attendance.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SignalWifiBad
import androidx.compose.material.icons.filled.SignalWifiStatusbarConnectedNoInternet4
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.lloyd.attendance.campus.CampusConnectManager
import com.lloyd.attendance.campus.CampusConnectionState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lloyd.attendance.core.designsystem.components.AttendanceCard
import com.lloyd.attendance.core.designsystem.components.AttendanceCardSkeleton
import com.lloyd.attendance.core.designsystem.components.AttendanceHealthBadge
import com.lloyd.attendance.core.designsystem.components.OfflineBanner
import com.lloyd.attendance.core.designsystem.components.StatusBadge
import com.lloyd.attendance.core.designsystem.theme.AttendanceColors
import com.lloyd.attendance.core.designsystem.theme.ExpressiveShapes
import com.lloyd.attendance.core.domain.AttendanceHealth
import com.lloyd.attendance.core.domain.AttendancePercentage
import com.lloyd.attendance.core.domain.BunkAdvisor
import com.lloyd.attendance.core.domain.SubjectAttendance
import com.lloyd.attendance.core.export.AttendanceReportExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSubjectDetail: (SubjectAttendance) -> Unit,
    onNavigateToOverallSimulation: (present: Int, total: Int) -> Unit,
    onNavigateToCampusConnect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val overall = uiState.overall
    val context = LocalContext.current

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
                    // Smart Campus Connect Wi-Fi status indicator with M3 Expressive micro-interaction
                    CampusWifiHeaderIndicator(onClick = onNavigateToCampusConnect)

                    IconButton(
                        onClick = {
                            val report = AttendanceReportExporter.generateTextReport(
                                studentName = uiState.studentName,
                                section = uiState.section,
                                overallPercentage = overall.percentage.displayValue,
                                totalPresent = overall.totalPresent,
                                totalClasses = overall.totalClasses,
                                subjects = overall.subjects
                            )
                            AttendanceReportExporter.shareReport(context, report)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Attendance Report"
                        )
                    }
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
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
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

                // Broadcast Notice from Admin
                if (!uiState.broadcastNotice.isNullOrBlank()) {
                    item {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Share,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = uiState.broadcastNotice!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Hero Attendance Summary Card (M3 Expressive Compact Header)
                item {
                    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                    val heroShape = ExpressiveShapes.largeIncreased
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isDark) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, heroShape) else Modifier),
                        shape = heroShape,
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "OVERALL ATTENDANCE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            letterSpacing = 1.sp
                                        )
                                        AttendanceHealthBadge(health = overall.health)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
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

                                // Compact Gauge — Percentage centered with NO badge overlap
                                Box(
                                    modifier = Modifier.size(68.dp),
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
                                        AttendanceHealth.HEALTHY -> if (isDark) AttendanceColors.HealthyDark else AttendanceColors.HealthyLight
                                        AttendanceHealth.BORDERLINE -> if (isDark) AttendanceColors.BorderlineDark else AttendanceColors.BorderlineLight
                                        AttendanceHealth.CRITICAL -> if (isDark) AttendanceColors.CriticalDark else AttendanceColors.CriticalLight
                                        AttendanceHealth.UNRECORDED -> MaterialTheme.colorScheme.outlineVariant
                                    }

                                    CircularProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier = Modifier.size(68.dp),
                                        color = strokeColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeWidth = 6.dp,
                                        strokeCap = StrokeCap.Round
                                    )

                                    Text(
                                        text = overall.percentage.displayValue,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Contextual Advice from BunkAdvisor
                            val isDark = isSystemInDarkTheme()
                            val advice = BunkAdvisor.getAdvice(overall.totalPresent, overall.totalClasses)
                            Text(
                                text = advice.headline,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (advice.isSafe) MaterialTheme.colorScheme.onSurface else if (isDark) AttendanceColors.CriticalDark else AttendanceColors.CriticalLight
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
                            shape = MaterialTheme.shapes.medium
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
                    itemsIndexed(
                        items = overall.subjects,
                        key = { index, subject ->
                            val code = subject.subjectCode.ifBlank { subject.subjectName }
                            if (code.isNotBlank()) "${code}_$index" else "subj_$index"
                        }
                    ) { _, subject ->
                        AttendanceCard(
                            subject = subject,
                            onClick = { onNavigateToSubjectDetail(subject) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Material 3 Expressive Header Indicator for Lloyd Campus Wi-Fi.
 * Displays real-time pulse and connection state, navigating to CampusConnectScreen on tap.
 */
@Composable
fun CampusWifiHeaderIndicator(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val manager = remember { CampusConnectManager.getInstance(context) }
    val connectionState by manager.connectionState.collectAsState()

    val icon = when (connectionState) {
        is CampusConnectionState.Online -> Icons.Default.Wifi
        is CampusConnectionState.CaptiveDetected -> Icons.Default.SignalWifiStatusbarConnectedNoInternet4
        is CampusConnectionState.WifiConnected,
        is CampusConnectionState.Authenticating,
        is CampusConnectionState.WaitingValidation,
        is CampusConnectionState.ConnectingWifi -> Icons.Default.Wifi
        is CampusConnectionState.AuthFailedPermanent,
        is CampusConnectionState.AuthFailedTransient -> Icons.Default.SignalWifiBad
        is CampusConnectionState.Disconnected -> Icons.Default.WifiOff
    }

    val tint = when (connectionState) {
        is CampusConnectionState.Online -> Color(0xFF2E7D32)
        is CampusConnectionState.CaptiveDetected -> Color(0xFFED6C02)
        is CampusConnectionState.WifiConnected,
        is CampusConnectionState.Authenticating,
        is CampusConnectionState.WaitingValidation,
        is CampusConnectionState.ConnectingWifi -> Color(0xFF1976D2)
        is CampusConnectionState.AuthFailedPermanent,
        is CampusConnectionState.AuthFailedTransient -> Color(0xFFD32F2F)
        is CampusConnectionState.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val backgroundTint = when (connectionState) {
        is CampusConnectionState.Online -> Color(0xFF2E7D32).copy(alpha = 0.12f)
        is CampusConnectionState.CaptiveDetected -> Color(0xFFED6C02).copy(alpha = 0.15f)
        is CampusConnectionState.WifiConnected,
        is CampusConnectionState.Authenticating,
        is CampusConnectionState.WaitingValidation,
        is CampusConnectionState.ConnectingWifi -> Color(0xFF1976D2).copy(alpha = 0.15f)
        is CampusConnectionState.AuthFailedPermanent,
        is CampusConnectionState.AuthFailedTransient -> Color(0xFFD32F2F).copy(alpha = 0.15f)
        is CampusConnectionState.Disconnected -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val isPulsing = when (connectionState) {
        is CampusConnectionState.WifiConnected,
        is CampusConnectionState.CaptiveDetected,
        is CampusConnectionState.Authenticating,
        is CampusConnectionState.WaitingValidation,
        is CampusConnectionState.ConnectingWifi -> true
        else -> false
    }

    val infiniteTransition = rememberInfiniteTransition(label = "wifi_pulse")
    val alphaAnim by if (isPulsing) {
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = backgroundTint,
        tonalElevation = 2.dp,
        modifier = modifier
            .padding(end = 4.dp)
            .size(38.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Campus Wi-Fi Status",
                tint = tint.copy(alpha = alphaAnim),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
