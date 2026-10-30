package com.lloyd.attendance.feature.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class ServiceItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesHubScreen(
    onNavigateToSyllabus: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToLatestAttendance: () -> Unit,
    onNavigateToFeeReceipts: () -> Unit,
    onNavigateToSimulation: () -> Unit,
    onNavigateToFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val honeyBronze = Color(0xFFF6BD60)
    val mutedTeal = Color(0xFF84A59D)
    val lightCoral = Color(0xFFF28482)

    val sections = listOf(
        "Academic" to listOf(
            ServiceItem("Syllabus", "AKTU syllabus browser", Icons.AutoMirrored.Filled.MenuBook, honeyBronze, onNavigateToSyllabus),
            ServiceItem("Calendar", "Attendance calendar", Icons.Default.DateRange, honeyBronze, onNavigateToCalendar),
            ServiceItem("Latest Updates", "This week's attendance", Icons.Default.Update, honeyBronze, onNavigateToLatestAttendance)
        ),
        "Finance" to listOf(
            ServiceItem("Fee Receipts", "View & print receipts", Icons.Default.Receipt, mutedTeal, onNavigateToFeeReceipts)
        ),
        "Tools" to listOf(
            ServiceItem("Bunk Planner", "Simulate your attendance", Icons.Default.Calculate, lightCoral, onNavigateToSimulation),
            ServiceItem("Feedback", "Report an issue", Icons.Default.Feedback, lightCoral, onNavigateToFeedback)
        )
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Services",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sections.forEach { (sectionTitle, sectionItems) ->
                item {
                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                // Pair items into rows of 2
                val rows = sectionItems.chunked(2)
                rows.forEach { rowItems ->
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { service ->
                                ElevatedCard(
                                    onClick = service.onClick,
                                    shape = RoundedCornerShape(20.dp),
                                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = service.accentColor.copy(alpha = 0.18f),
                                            modifier = Modifier.size(52.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Icon(
                                                    imageVector = service.icon,
                                                    contentDescription = null,
                                                    tint = service.accentColor,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = service.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = service.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                            // Fill empty slot if odd number of items
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
