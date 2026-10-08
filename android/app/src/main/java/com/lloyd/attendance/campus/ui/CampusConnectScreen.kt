package com.lloyd.attendance.campus.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiBad
import androidx.compose.material.icons.filled.SignalWifiStatusbarConnectedNoInternet4
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.lloyd.attendance.campus.CampusConnectManager
import com.lloyd.attendance.campus.CampusConnectionState
import com.lloyd.attendance.campus.diagnostics.CampusConnectionEvent
import com.lloyd.attendance.campus.diagnostics.CampusHealthScore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusConnectScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val manager = remember { CampusConnectManager.getInstance(context) }
    val connectionState by manager.connectionState.collectAsState()
    val healthScore by manager.healthScore.collectAsState()
    val diagnosticEvents by manager.diagnosticsStore.eventsFlow.collectAsState()

    val scope = rememberCoroutineScope()

    var showEditCredsDialog by remember { mutableStateOf(false) }
    var isSyncingErp by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var showDiagnosticsSection by remember { mutableStateOf(false) }

    var inputUser by remember { mutableStateOf(manager.credentialStore.getWifiUsername().orEmpty()) }
    var inputPass by remember { mutableStateOf(manager.credentialStore.getWifiPassword().orEmpty()) }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Campus Connect",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Smart Campus Wi-Fi & Auto-Login",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    IconButton(
                        onClick = {
                            isSyncingErp = true
                            scope.launch {
                                val result = manager.syncCredentialsFromErp()
                                isSyncingErp = false
                                syncMessage = result.getOrNull() ?: result.exceptionOrNull()?.message
                            }
                        }
                    ) {
                        if (isSyncingErp) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CloudSync,
                                contentDescription = "Sync from ERP"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Campus Status Card
            item {
                CampusStatusHeroCard(
                    state = connectionState,
                    health = healthScore,
                    sessionDuration = manager.sessionManager.formatDuration(),
                    lastLoginTs = manager.credentialStore.lastLoginTimestamp,
                    onConnectNow = { manager.connectNow() },
                    onDisconnect = { manager.disconnect() }
                )
            }

            // 2. Wi-Fi Quality & Metrics Card
            item {
                CampusHealthMetricsCard(health = healthScore)
            }

            // 3. Automation Toggles Card
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Connection Preferences",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Auto-Connect Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Connect via System Suggestion",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Allow Android OS to seamlessly auto-associate with campus APs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = manager.credentialStore.isAutoConnectEnabled,
                                onCheckedChange = { checked ->
                                    manager.credentialStore.isAutoConnectEnabled = checked
                                    if (checked) {
                                        val u = manager.credentialStore.getWifiUsername()
                                        val p = manager.credentialStore.getWifiPassword()
                                        manager.provisioner.provisionCampusNetwork(
                                            manager.credentialStore.savedProfile, u, p
                                        )
                                    } else {
                                        manager.provisioner.removeSuggestions(manager.credentialStore.savedProfile)
                                    }
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Headless Auto-Login Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Headless Auto-Login",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Direct HTTP authentication without opening browsers or WebViews",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = manager.credentialStore.isAutoLoginEnabled,
                                onCheckedChange = { checked ->
                                    manager.credentialStore.isAutoLoginEnabled = checked
                                }
                            )
                        }
                    }
                }
            }

            // 4. Secure Campus Credentials Card
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Campus Wi-Fi Vault",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(onClick = {
                                inputUser = manager.credentialStore.getWifiUsername().orEmpty()
                                inputPass = manager.credentialStore.getWifiPassword().orEmpty()
                                showEditCredsDialog = true
                            }) {
                                Text("Edit")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val user = manager.credentialStore.getWifiUsername()
                        if (!user.isNullOrBlank()) {
                            Text(
                                text = "Username / Student ID: $user",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Password: •••••••• (Hardware Keystore AES-256-GCM)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "No campus Wi-Fi credentials saved. Sync from ERP or enter manually.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                isSyncingErp = true
                                scope.launch {
                                    val result = manager.syncCredentialsFromErp()
                                    isSyncingErp = false
                                    syncMessage = result.getOrNull() ?: result.exceptionOrNull()?.message
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudSync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync Wi-Fi Login from Lloyd ERP")
                        }
                    }
                }
            }

            // 5. Diagnostics & Event Logs Section
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDiagnosticsSection = !showDiagnosticsSection },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Live Diagnostics & Logs (${diagnosticEvents.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (showDiagnosticsSection) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null
                            )
                        }

                        AnimatedVisibility(visible = showDiagnosticsSection) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                if (diagnosticEvents.isEmpty()) {
                                    Text(
                                        text = "No diagnostic events recorded yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    diagnosticEvents.take(15).forEach { event ->
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = event.summary,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp)),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (!event.details.isNullOrBlank()) {
                                                Text(
                                                    text = event.details,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            HorizontalDivider(
                                                modifier = Modifier.padding(top = 4.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Credentials Modal Dialog
    if (showEditCredsDialog) {
        AlertDialog(
            onDismissRequest = { showEditCredsDialog = false },
            title = {
                Text(text = "Campus Wi-Fi Credentials", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your Lloyd campus Internet/Wi-Fi login details. Stored safely in Android Hardware Keystore.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = inputUser,
                        onValueChange = { inputUser = it },
                        label = { Text("Student ID / Wi-Fi Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputPass,
                        onValueChange = { inputPass = it },
                        label = { Text("Wi-Fi Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputUser.isNotBlank() && inputPass.isNotBlank()) {
                            manager.credentialStore.saveCredentials(inputUser.trim(), inputPass.trim())
                            showEditCredsDialog = false
                        }
                    }
                ) {
                    Text("Save to Vault")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditCredsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sync Notification Dialog
    if (syncMessage != null) {
        AlertDialog(
            onDismissRequest = { syncMessage = null },
            title = { Text(text = "ERP Credential Sync", fontWeight = FontWeight.Bold) },
            text = { Text(text = syncMessage ?: "") },
            confirmButton = {
                Button(onClick = { syncMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun CampusStatusHeroCard(
    state: CampusConnectionState,
    health: CampusHealthScore,
    sessionDuration: String,
    lastLoginTs: Long,
    onConnectNow: () -> Unit,
    onDisconnect: () -> Unit
) {
    val (statusTitle, statusColor, statusIcon) = when (state) {
        is CampusConnectionState.Online -> Triple(
            "CAMPUS ONLINE",
            Color(0xFF2E7D32),
            Icons.Filled.CheckCircle
        )
        is CampusConnectionState.CaptiveDetected -> Triple(
            "CAPTIVE BARRIER",
            Color(0xFFED6C02),
            Icons.Filled.SignalWifiStatusbarConnectedNoInternet4
        )
        is CampusConnectionState.Authenticating -> Triple(
            "AUTHENTICATING...",
            Color(0xFF1976D2),
            Icons.Filled.Refresh
        )
        is CampusConnectionState.WaitingValidation -> Triple(
            "VALIDATING...",
            Color(0xFF0288D1),
            Icons.Filled.Refresh
        )
        is CampusConnectionState.ConnectingWifi -> Triple(
            "CONNECTING...",
            Color(0xFFED6C02),
            Icons.Filled.Wifi
        )
        is CampusConnectionState.WifiConnected -> Triple(
            "WIFI ASSOCIATED",
            Color(0xFF0288D1),
            Icons.Filled.Wifi
        )
        is CampusConnectionState.AuthFailedPermanent -> Triple(
            "CREDENTIALS ERROR",
            Color(0xFFD32F2F),
            Icons.Filled.SignalWifiBad
        )
        is CampusConnectionState.AuthFailedTransient -> Triple(
            "RECOVERING (${state.nextRetrySeconds}s)",
            Color(0xFFED6C02),
            Icons.Filled.Refresh
        )
        is CampusConnectionState.Disconnected -> Triple(
            "DISCONNECTED",
            Color(0xFF757575),
            Icons.Filled.WifiOff
        )
    }

    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Status Pill
            Surface(
                shape = CircleShape,
                color = statusColor.copy(alpha = 0.15f),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                }
            }

            // Connection summary
            Text(
                text = health.diagnosticSummary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Session Information Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Session Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = sessionDuration,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Last Login",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (lastLoginTs > 0) SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastLoginTs)) else "--",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Connect / Reconnect Button
            Button(
                onClick = {
                    if (state is CampusConnectionState.Online) {
                        onDisconnect()
                    } else {
                        onConnectNow()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state is CampusConnectionState.Online) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (state is CampusConnectionState.Online) "Disconnect" else "Connect Now",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CampusHealthMetricsCard(health: CampusHealthScore) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Health & Link Diagnostics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    icon = Icons.Filled.SignalWifi4Bar,
                    title = "Wi-Fi Signal",
                    value = if (health.rssiDbm > -127) "${health.rssiDbm} dBm" else "--"
                )
                MetricItem(
                    icon = Icons.Filled.Speed,
                    title = "Link Speed",
                    value = if (health.linkSpeedMbps > 0) "${health.linkSpeedMbps} Mbps" else "--"
                )
                MetricItem(
                    icon = if (health.isErpReachable) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                    title = "ERP Reachable",
                    value = if (health.isErpReachable) "${health.erpLatencyMs} ms" else "Unreachable",
                    tint = if (health.isErpReachable) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                )
            }
        }
    }
}

@Composable
fun MetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
