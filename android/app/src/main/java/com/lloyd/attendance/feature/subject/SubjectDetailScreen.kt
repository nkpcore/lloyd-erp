package com.lloyd.attendance.feature.subject

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lloyd.attendance.api.Models
import com.lloyd.attendance.core.designsystem.components.AttendanceHealthBadge
import com.lloyd.attendance.core.designsystem.components.StatusBadge
import com.lloyd.attendance.core.designsystem.theme.AttendanceColors
import com.lloyd.attendance.core.domain.AttendanceHealth
import com.lloyd.attendance.core.domain.AttendancePercentage
import com.lloyd.attendance.core.domain.BunkAdvisor
import com.lloyd.attendance.core.domain.SubjectAttendance
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subject: SubjectAttendance,
    onBack: () -> Unit,
    viewModel: SubjectDetailViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    LaunchedEffect(subject) {
        viewModel.loadSubject(subject)
    }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val currentSubject = uiState.subject ?: subject
    val advice = BunkAdvisor.getAdvice(currentSubject.presentCount, currentSubject.totalClasses)
    val missedCount = (currentSubject.totalClasses - currentSubject.presentCount).coerceAtLeast(0)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentSubject.subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentSubject.subjectCode.isNotBlank()) {
                            Text(
                                text = "Code: ${currentSubject.subjectCode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = buildString {
                            appendLine("📊 Lloyd Attendance — ${currentSubject.subjectName}")
                            appendLine("Percentage: ${currentSubject.percentage.displayValue}")
                            appendLine("Attended: ${currentSubject.presentCount} / ${currentSubject.totalClasses} classes")
                            appendLine("Missed: $missedCount classes")
                            if (!currentSubject.teacherName.isNullOrBlank()) {
                                appendLine("Faculty: ${currentSubject.teacherName}")
                            }
                            appendLine("Advice: ${advice.headline}")
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Subject Attendance")
                        context.startActivity(shareIntent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Attendance"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Gauge Card
            item {
                SubjectHeroCard(
                    subject = currentSubject,
                    missedCount = missedCount,
                    advice = advice
                )
            }

            // 2. Faculty Info Pill Card
            if (!currentSubject.teacherName.isNullOrBlank()) {
                item {
                    FacultyInfoCard(teacherName = currentSubject.teacherName!!)
                }
            }

            // 3. Interactive What-If Simulator Card
            item {
                SubjectSimulatorCard(
                    currentPresent = currentSubject.presentCount,
                    currentTotal = currentSubject.totalClasses,
                    currentPercentage = currentSubject.percentage.numericValue ?: 0.0,
                    simulateAttend = uiState.simulateAttend,
                    simulateMiss = uiState.simulateMiss,
                    simulatedPercentage = uiState.simulatedPercentage,
                    onUpdateSimulation = { attend, miss ->
                        viewModel.updateSimulation(attend, miss)
                    }
                )
            }

            // 4. Section Header: Lecture Ledger
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lecture History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.logsForSubject.size} sessions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 5. Lecture Items
            if (uiState.logsForSubject.isEmpty()) {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Individual Logs Found",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Class-level daily session entries will appear here once published by faculty.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.logsForSubject, key = { it.attendanceDate.orEmpty() + it.createdByName.orEmpty() + it.status.orEmpty() }) { logItem ->
                    LectureLogItemCard(logItem = logItem)
                }
            }
        }
    }
}

@Composable
private fun SubjectHeroCard(
    subject: SubjectAttendance,
    missedCount: Int,
    advice: BunkAdvisor.Advice
) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CURRENT ATTENDANCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subject.percentage.displayValue,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${subject.presentCount} attended · $missedCount missed",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Gauge
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val targetProgress = when (val p = subject.percentage) {
                        is AttendancePercentage.Value -> (p.percentage / 100.0).toFloat().coerceIn(0f, 1f)
                        else -> 0f
                    }
                    val animatedProgress by animateFloatAsState(
                        targetValue = targetProgress,
                        animationSpec = tween(durationMillis = 800),
                        label = "subjectGauge"
                    )

                    val strokeColor = when (subject.health) {
                        AttendanceHealth.HEALTHY -> AttendanceColors.HealthyLight
                        AttendanceHealth.BORDERLINE -> AttendanceColors.BorderlineLight
                        AttendanceHealth.CRITICAL -> AttendanceColors.CriticalLight
                        AttendanceHealth.UNRECORDED -> MaterialTheme.colorScheme.outline
                    }

                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = strokeColor.copy(alpha = 0.15f),
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = strokeColor,
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )
                    AttendanceHealthBadge(
                        health = subject.health,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contextual Bunk Advisor Rule Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (advice.isSafe) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (advice.isSafe) "✓" else "!",
                        fontWeight = FontWeight.Bold,
                        color = if (advice.isSafe) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    Column {
                        Text(
                            text = advice.headline,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (advice.isSafe) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                        Text(
                            text = advice.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (advice.isSafe) {
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FacultyInfoCard(teacherName: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Faculty",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column {
                Text(
                    text = "Course Faculty",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = teacherName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SubjectSimulatorCard(
    currentPresent: Int,
    currentTotal: Int,
    currentPercentage: Double,
    simulateAttend: Int,
    simulateMiss: Int,
    simulatedPercentage: Double,
    onUpdateSimulation: (attend: Int, miss: Int) -> Unit
) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "What-If Simulator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Forecast percentage for this subject",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (simulateAttend > 0 || simulateMiss > 0) {
                    IconButton(
                        onClick = { onUpdateSimulation(0, 0) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Simulation",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Simulation Result Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PROJECTED ATTENDANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f%%", simulatedPercentage),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (simulatedPercentage >= 75.0) {
                                AttendanceColors.HealthyLight
                            } else {
                                AttendanceColors.CriticalLight
                            }
                        )
                    }

                    val delta = simulatedPercentage - currentPercentage
                    if (simulateAttend > 0 || simulateMiss > 0) {
                        val deltaText = if (delta >= 0) "+${String.format(Locale.US, "%.1f", delta)}%" else "${String.format(Locale.US, "%.1f", delta)}%"
                        val deltaColor = if (delta >= 0) AttendanceColors.HealthyLight else AttendanceColors.CriticalLight
                        StatusBadge(
                            text = deltaText,
                            containerColor = deltaColor.copy(alpha = 0.15f),
                            contentColor = deltaColor
                        )
                    } else {
                        Text(
                            text = "Current: ${String.format(Locale.US, "%.1f%%", currentPercentage)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stepper: Classes to Attend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Attend Next Classes",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "+$simulateAttend class${if (simulateAttend == 1) "" else "es"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledIconButton(
                        onClick = { if (simulateAttend > 0) onUpdateSimulation(simulateAttend - 1, simulateMiss) },
                        enabled = simulateAttend > 0,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    Text(
                        text = "$simulateAttend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    FilledIconButton(
                        onClick = { onUpdateSimulation(simulateAttend + 1, simulateMiss) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stepper: Classes to Miss
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Miss Next Classes",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "+$simulateMiss class${if (simulateMiss == 1) "" else "es"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledIconButton(
                        onClick = { if (simulateMiss > 0) onUpdateSimulation(simulateAttend, simulateMiss - 1) },
                        enabled = simulateMiss > 0,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    Text(
                        text = "$simulateMiss",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    FilledIconButton(
                        onClick = { onUpdateSimulation(simulateAttend, simulateMiss + 1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }
        }
    }
}

@Composable
private fun LectureLogItemCard(logItem: Models.StudentAttendanceItem) {
    val isPresent = logItem.status?.equals("present", ignoreCase = true) == true
    val statusColor = if (isPresent) AttendanceColors.HealthyLight else AttendanceColors.CriticalLight
    val statusBg = if (isPresent) AttendanceColors.HealthyContainerLight else AttendanceColors.CriticalContainerLight

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = logItem.attendanceDate ?: "Unknown Date",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!logItem.createdByName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Marked by ${logItem.createdByName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(statusBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPresent) "Present" else "Absent",
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
