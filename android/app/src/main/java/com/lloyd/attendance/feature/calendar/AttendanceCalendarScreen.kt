package com.lloyd.attendance.feature.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lloyd.attendance.api.Models
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── Brand palette tokens ─────────────────────────────────────────────────────
private val ColorPresent      = Color(0xFF84A59D)          // Muted Teal
private val ColorPresentLight = Color(0xFF84A59D).copy(alpha = 0.18f)
private val ColorAbsent       = Color(0xFFF28482)          // Light Coral
private val ColorAbsentLight  = Color(0xFFF28482).copy(alpha = 0.18f)
private val ColorMixed        = Color(0xFFF6BD60)          // Honey Bronze
private val ColorMixedLight   = Color(0xFFF6BD60).copy(alpha = 0.18f)
private val ColorNoLecture    = Color(0xFF475569).copy(alpha = 0.10f)

// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceCalendarScreen(
    viewModel: AttendanceCalendarViewModel,
    onNavigateBack: () -> Unit,
    onDateSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Attendance Calendar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }, enabled = !uiState.isRefreshing) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh data")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // ── Month Navigator ──────────────────────────────────────────────
            item {
                AnimatedContent(
                    targetState = uiState.currentMonth.format(monthFormatter),
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically { -it / 4 })
                            .togetherWith(fadeOut(tween(120)))
                    },
                    label = "month_label"
                ) { monthLabel ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.previousMonth() },
                            modifier = Modifier.semantics {
                                contentDescription = "Previous month"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = monthLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { viewModel.nextMonth() },
                            modifier = Modifier.semantics {
                                contentDescription = "Next month"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // ── Month KPI strip ──────────────────────────────────────────────
            item {
                MonthKpiStrip(
                    present = uiState.monthPresentDays,
                    absent = uiState.monthAbsentDays,
                    mixed = uiState.monthMixedDays
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Day-of-week header ───────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { d ->
                        Text(
                            text = d,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // ── Heatmap calendar grid ────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                            // Fixed height: 6 rows × (cell + gap)
                            .height(268.dp),
                        userScrollEnabled = false,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        items(uiState.days) { dayInfo ->
                            val isSelected = dayInfo.date == uiState.selectedDate
                            HeatmapDayCell(
                                dayInfo = dayInfo,
                                isSelected = isSelected,
                                onClick = {
                                    if (dayInfo.isCurrentMonth) {
                                        viewModel.selectDate(dayInfo.date)
                                        onDateSelected(dayInfo.date.toString())
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                // Legend row below grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = ColorPresent, label = "All present")
                    Spacer(Modifier.width(16.dp))
                    LegendDot(color = ColorMixed, label = "Partial")
                    Spacer(Modifier.width(16.dp))
                    LegendDot(color = ColorAbsent, label = "All absent")
                    Spacer(Modifier.width(16.dp))
                    LegendDot(color = MaterialTheme.colorScheme.outlineVariant, label = "No class")
                }
            }

            // ── Selected day detail panel ────────────────────────────────────
            item { Spacer(modifier = Modifier.height(8.dp)) }

            uiState.selectedDate?.let { date ->
                item {
                    val dayFormat = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.getDefault())
                    val dayLogs = uiState.selectedDateLogs
                    val presentCount = dayLogs.count { it.isPresent }
                    val totalCount = dayLogs.size

                    // Day header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = date.format(dayFormat),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (totalCount > 0) {
                                Text(
                                    text = "$presentCount of $totalCount lectures attended",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // Attendance ratio chip for the day
                        if (totalCount > 0) {
                            val chipColor = when {
                                presentCount == totalCount -> ColorPresent
                                presentCount == 0 -> ColorAbsent
                                else -> ColorMixed
                            }
                            val pct = (presentCount * 100f / totalCount).toInt()
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = chipColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, chipColor.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "$pct%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = chipColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Empty state
                if (uiState.selectedDateLogs.isEmpty()) {
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(300)) + expandVertically(tween(300))
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = "No lectures recorded for this day",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.selectedDateLogs) { log ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(200)) + slideInVertically(tween(220)) { it / 3 }
                        ) {
                            LectureLogItemCard(
                                log = log,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Month KPI strip ──────────────────────────────────────────────────────────

@Composable
private fun MonthKpiStrip(present: Int, absent: Int, mixed: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KpiChip(
            modifier = Modifier.weight(1f),
            value = present.toString(),
            label = "Present",
            color = ColorPresent,
            bgColor = ColorPresentLight
        )
        KpiChip(
            modifier = Modifier.weight(1f),
            value = absent.toString(),
            label = "Absent",
            color = ColorAbsent,
            bgColor = ColorAbsentLight
        )
        KpiChip(
            modifier = Modifier.weight(1f),
            value = mixed.toString(),
            label = "Partial",
            color = ColorMixed,
            bgColor = ColorMixedLight
        )
    }
}

@Composable
private fun KpiChip(
    value: String,
    label: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

// ─── Heatmap day cell ─────────────────────────────────────────────────────────

@Composable
fun HeatmapDayCell(
    dayInfo: CalendarDayInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Heat fill: full color for the cell background (heatmap style)
    val heatFill: Color = when {
        !dayInfo.isCurrentMonth -> Color.Transparent
        dayInfo.status == DayAttendanceStatus.PRESENT -> ColorPresentLight.copy(alpha = 0.35f)
        dayInfo.status == DayAttendanceStatus.ABSENT -> ColorAbsentLight.copy(alpha = 0.35f)
        dayInfo.status == DayAttendanceStatus.MIXED -> ColorMixedLight.copy(alpha = 0.35f)
        else -> Color.Transparent
    }

    val indicatorColor: Color = when (dayInfo.status) {
        DayAttendanceStatus.PRESENT -> ColorPresent
        DayAttendanceStatus.ABSENT -> ColorAbsent
        DayAttendanceStatus.MIXED -> ColorMixed
        DayAttendanceStatus.NO_LECTURES -> Color.Transparent
    }

    // Animated selection scale (spring pop)
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "cell_scale"
    )

    // Animated background
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else heatFill,
        animationSpec = tween(180),
        label = "cell_bg"
    )

    // Corner radius — selected is more rounded
    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 8.dp,
        animationSpec = tween(180),
        label = "cell_corner"
    )

    val textColor = when {
        !dayInfo.isCurrentMonth -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        dayInfo.isToday -> Color(0xFFF6BD60) // Honey Bronze for today
        else -> MaterialTheme.colorScheme.onSurface
    }

    // Accessibility: describe what this cell represents
    val cellDescription = buildString {
        append("${dayInfo.date}")
        if (dayInfo.isCurrentMonth) {
            when (dayInfo.status) {
                DayAttendanceStatus.PRESENT ->
                    append(", all ${dayInfo.totalCount} lectures present")
                DayAttendanceStatus.ABSENT ->
                    append(", all ${dayInfo.totalCount} lectures absent")
                DayAttendanceStatus.MIXED ->
                    append(", ${dayInfo.presentCount} of ${dayInfo.totalCount} lectures present")
                DayAttendanceStatus.NO_LECTURES ->
                    append(", no lectures")
            }
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .background(animatedBg)
            .then(
                if (dayInfo.isToday && !isSelected) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFF6BD60).copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                } else Modifier
            )
            .clickable(
                enabled = dayInfo.isCurrentMonth,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = indicatorColor),
                onClick = onClick
            )
            .semantics { contentDescription = cellDescription },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Today ring
            if (dayInfo.isToday && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF6BD60).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${dayInfo.dayOfMonth}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF6BD60),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = "${dayInfo.dayOfMonth}",
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }

            // Attendance dot — color indicator (not color alone: also size varies by intensity)
            if (indicatorColor != Color.Transparent && dayInfo.isCurrentMonth) {
                Spacer(modifier = Modifier.height(2.dp))
                // Intensity-based dot: larger when data is rich
                val dotSize = when {
                    dayInfo.totalCount >= 4 -> 6.dp
                    dayInfo.totalCount >= 2 -> 5.dp
                    else -> 4.dp
                }
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
            }
        }
    }
}

// ─── Legend ──────────────────────────────────────────────────────────────────

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

// ─── Status badge (kept for external use if any) ─────────────────────────────

@Composable
fun StatusBadge(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ─── Lecture log card ─────────────────────────────────────────────────────────

@Composable
fun LectureLogItemCard(
    log: Models.StudentAttendanceItem,
    modifier: Modifier = Modifier
) {
    val isPresent = log.isPresent
    val badgeColor = if (isPresent) ColorPresent else ColorAbsent
    val badgeText  = if (isPresent) "PRESENT" else "ABSENT"
    val cardBg     = if (isPresent) ColorPresentLight else ColorAbsentLight
    val leftBar    = if (isPresent) ColorPresent else ColorAbsent

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, leftBar.copy(alpha = 0.25f))
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Colored left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        color = leftBar,
                        shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.subjectDisplayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!log.facultyDisplayName.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = log.facultyDisplayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val lectureTime = log.classLecture?.takeIf { it.isNotBlank() }
                        ?: log.lectureTimeFormatted
                    if (lectureTime.isNotBlank() && lectureTime != "—") {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = lectureTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Status pill — icon + label for non-color-only indication (a11y)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isPresent) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = badgeColor
                            )
                        }
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
