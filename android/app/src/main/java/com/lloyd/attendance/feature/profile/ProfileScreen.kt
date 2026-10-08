package com.lloyd.attendance.feature.profile

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.OutlinedTextField
import com.lloyd.attendance.data.AppPreferences
import com.lloyd.attendance.core.telemetry.TelemetryManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import java.io.File
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.lloyd.attendance.core.security.BiometricAuthManager
import com.lloyd.attendance.core.security.BiometricCredentialVault
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lloyd.attendance.BuildConfig
import com.lloyd.attendance.api.Models
import com.lloyd.attendance.core.designsystem.components.StatusBadge
import com.lloyd.attendance.core.designsystem.theme.AttendanceColors
import com.lloyd.attendance.core.ota.OtaReleaseInfo
import com.lloyd.attendance.core.ota.OtaUpdateManager
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: Models.UserProfile?,
    studentId: Int,
    stats: Models.CalculatedStats? = null,
    onLogout: () -> Unit,
    onNavigateToCampusConnect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val biometricVault = remember { BiometricCredentialVault.getInstance(context) }
    var isAppLockOn by remember { mutableStateOf(biometricVault.isAppLockEnabled()) }
    var hasSavedCredentials by remember { mutableStateOf(biometricVault.hasSavedCredentials()) }
    var showForgetCredentialsDialog by remember { mutableStateOf(false) }
    val isBiometricReady = remember { BiometricAuthManager.isBiometricReady(context) }

    var showLogoutDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<OtaReleaseInfo?>(null) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var updateDownloadProgress by remember { mutableStateOf(0f) }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(Unit) {
        val existingApk = OtaUpdateManager.getDownloadedUpdateApk(context)
        if (existingApk.exists() && existingApk.length() > 0L) {
            downloadedApkFile = existingApk
        }
    }

    val deviceId = remember { TelemetryManager.getOrCreateDeviceId(context) }
    var showEndpointDialog by remember { mutableStateOf(false) }
    val prefs = remember { AppPreferences.getInstance(context) }
    var endpointInput by remember { mutableStateOf(prefs.telemetryEndpoint ?: "") }
    var isSendingTelemetry by remember { mutableStateOf(false) }

    // Admin authorization: strictly bound to logged-in student ID (260094709), zero hardcoded passwords!
    val adminId = "260094709"
    val isAdminUser = remember(userProfile, prefs.username, studentId) {
        prefs.username.trim() == adminId ||
        userProfile?.admission_no?.trim() == adminId ||
        studentId?.toString() == adminId ||
        biometricVault.getSavedUsername()?.trim() == adminId
    }

    if (showEndpointDialog) {
        AlertDialog(
            onDismissRequest = { showEndpointDialog = false },
            title = {
                Text(
                    text = "Fleet Admin Server Endpoint",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter Fleet Telemetry / Config server base URL (e.g., http://192.168.1.100:8080). Leave blank to disable remote syncing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = endpointInput,
                        onValueChange = { endpointInput = it },
                        label = { Text("Server URL") },
                        placeholder = { Text("http://192.168.1.100:8080") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = endpointInput.trim()
                        val finalUrl = if (trimmed.isBlank()) null else trimmed
                        AppPreferences.getInstance(context).telemetryEndpoint = finalUrl
                        showEndpointDialog = false
                        Toast.makeText(context, "Fleet server updated", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndpointDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showForgetCredentialsDialog) {
        AlertDialog(
            onDismissRequest = { showForgetCredentialsDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Forget Saved Credentials",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will permanently remove your AES-256-GCM encrypted credentials from the hardware Keystore. You will need to enter your password manually upon the next login or session expiry.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        biometricVault.clearCredentials()
                        hasSavedCredentials = false
                        showForgetCredentialsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Forget Credentials")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgetCredentialsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out of Lloyd ERP? Your session and offline attendance data will be safely cleared from this device.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile & Security",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Identity Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            val photoUrl = userProfile?.photo_url?.takeIf { it.isNotBlank() }
                            if (photoUrl != null) {
                                val cleanPhoto = photoUrl.trim()
                                val fullUrl = when {
                                    cleanPhoto.startsWith("http://") || cleanPhoto.startsWith("https://") -> cleanPhoto
                                    cleanPhoto.startsWith("/assets/resource") || cleanPhoto.startsWith("assets/resource") -> {
                                        "https://erp.lloydcollege.in/" + cleanPhoto.removePrefix("/")
                                    }
                                    cleanPhoto.startsWith("/student-photo") || cleanPhoto.startsWith("student-photo") -> {
                                        "https://erp.lloydcollege.in/assets/resource/" + cleanPhoto.removePrefix("/")
                                    }
                                    else -> {
                                        "https://erp.lloydcollege.in" + if (cleanPhoto.startsWith("/")) cleanPhoto else "/$cleanPhoto"
                                    }
                                }
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(fullUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Profile Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val initial = userProfile?.name?.trim()?.firstOrNull()?.uppercaseChar()
                                if (initial != null) {
                                    Text(
                                        text = initial.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            val resolvedName = userProfile?.name?.takeIf { it.isNotBlank() }
                                ?: stats?.studentName?.takeIf { it.isNotBlank() }
                                ?: prefs.cachedStats?.studentName?.takeIf { it.isNotBlank() }
                                ?: "--"
                            Text(
                                text = resolvedName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            val resolvedId = if (studentId > 0) studentId
                            else if ((userProfile?.id ?: 0) > 0) userProfile?.id
                            else "--"
                            Text(
                                text = "Student ID: $resolvedId",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val rollOrAdmission = userProfile?.admission_no?.takeIf { it.isNotBlank() }
                        ?: userProfile?.username?.takeIf { it.isNotBlank() }
                        ?: prefs.username.takeIf { it.isNotBlank() }
                        ?: "--"
                    ProfileField("Roll / Enrollment", rollOrAdmission)
                    ProfileField("Program / Course", userProfile?.course?.takeIf { it.isNotBlank() } ?: "--")
                    ProfileField("Semester", userProfile?.semester?.takeIf { it.isNotBlank() } ?: "--")
                    val resolvedSection = userProfile?.section?.takeIf { it.isNotBlank() }
                        ?: prefs.selectedSection.takeIf { it.isNotBlank() }
                        ?: "--"
                    ProfileField("Assigned Section", resolvedSection)

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Device UUID",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Device UUID", deviceId))
                                    Toast.makeText(context, "Device UUID copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${deviceId.take(8)}...${deviceId.takeLast(4)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy UUID",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Lloyd Campus Connect Card (M3 Expressive)
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCampusConnect() },
                shape = MaterialTheme.shapes.large,
                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Campus Connect",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Smart Campus Wi-Fi & Headless Auto-Login",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Open Campus Connect",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Biometrics & App Lock Card (M3 Expressive)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Biometrics & App Lock",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Hardware Keystore Vault",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        StatusBadge(
                            text = if (isBiometricReady) "ACTIVE" else "UNAVAILABLE",
                            containerColor = if (isBiometricReady) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = if (isBiometricReady) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // App Lock Switch Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lock App on Screen Lock",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Require fingerprint when device screen turns on",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = isAppLockOn,
                            enabled = isBiometricReady,
                            onCheckedChange = { checked ->
                                isAppLockOn = checked
                                biometricVault.setAppLockEnabled(checked)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Saved Credentials Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Saved Credentials",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (hasSavedCredentials) "Stored securely in AES-256-GCM Keystore" else "No saved credentials",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (hasSavedCredentials) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { showForgetCredentialsDialog = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Forget", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Security Hardening Badge & Info
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large),
                shape = MaterialTheme.shapes.large,
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Crafted by Nikhil Pandey",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        StatusBadge(
                            text = if (isAdminUser) "ADMIN" else "v${BuildConfig.VERSION_NAME}",
                            containerColor = if (isAdminUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (isAdminUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isAdminUser) "Fleet Governance & Developer Gateway Active" else "Open Source Lloyd ERP Companion • Material 3 Expressive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Admin Fleet Controls Card (Visible ONLY when logged in as admin 260094709)
            if (isAdminUser) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Fleet Admin Gateway",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Governance & Live Sync",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            StatusBadge(
                                text = "ADMIN ACTIVE",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val currentEndpoint = prefs.telemetryEndpoint?.takeIf { it.isNotBlank() }
                            ?: BuildConfig.DEFAULT_FLEET_URL.takeIf { it.isNotBlank() }
                            ?: "https://lloyd-erp-sand.vercel.app"
                        Text(
                            text = "Server: $currentEndpoint",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    endpointInput = prefs.telemetryEndpoint ?: ""
                                    showEndpointDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Set Server")
                            }

                            Button(
                                onClick = {
                                    isSendingTelemetry = true
                                    coroutineScope.launch {
                                        val res = TelemetryManager.sendTelemetry(
                                            context = context,
                                            endpointUrl = prefs.telemetryEndpoint,
                                            studentId = studentId,
                                            studentName = userProfile?.name
                                        )
                                        isSendingTelemetry = false
                                        val msg = if (res.isSuccess) "Telemetry heartbeat dispatched!" else "Telemetry failed: ${res.exceptionOrNull()?.message}"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isSendingTelemetry,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSendingTelemetry) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ping Server")
                                }
                            }
                        }

                        if (!currentEndpoint.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(currentEndpoint))
                                        context.startActivity(browserIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Fleet Dashboard")
                            }
                        }
                    }
                }
            }

            // In-App OTA Update Checker Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Software Updates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (updateInfo?.hasUpdate == true) {
                            StatusBadge(
                                text = "UPDATE AVAILABLE",
                                containerColor = AttendanceColors.BorderlineContainerLight,
                                contentColor = AttendanceColors.OnBorderlineContainerLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when {
                            updateInfo?.hasUpdate == true -> "New version v${updateInfo?.latestVersion} is available to install!"
                            updateMessage != null -> updateMessage!!
                            else -> "Running version v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isDownloadingUpdate) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(
                                progress = { updateDownloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Downloading update: ${(updateDownloadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (downloadedApkFile != null && downloadedApkFile!!.exists() && downloadedApkFile!!.length() > 0L) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    if (!OtaUpdateManager.canInstallApk(context)) {
                                        Toast.makeText(context, "Please allow Lloyd Attendance to install updates", Toast.LENGTH_LONG).show()
                                        OtaUpdateManager.openInstallPermissionSettings(context)
                                    } else {
                                        OtaUpdateManager.promptInstallApk(context, downloadedApkFile!!)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Install Downloaded Update (v${updateInfo?.latestVersion ?: "Latest"})")
                            }

                            if (updateInfo?.hasUpdate == true && updateInfo?.downloadUrl != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(
                                    onClick = {
                                        isDownloadingUpdate = true
                                        updateDownloadProgress = 0f
                                        coroutineScope.launch {
                                            val result = OtaUpdateManager.downloadApkWithProgress(context, updateInfo!!.downloadUrl!!) { progress ->
                                                updateDownloadProgress = progress
                                            }
                                            isDownloadingUpdate = false
                                            result.onSuccess { apkFile ->
                                                downloadedApkFile = apkFile
                                                if (!OtaUpdateManager.canInstallApk(context)) {
                                                    Toast.makeText(context, "Please allow Lloyd Attendance to install updates", Toast.LENGTH_LONG).show()
                                                    OtaUpdateManager.openInstallPermissionSettings(context)
                                                } else {
                                                    OtaUpdateManager.promptInstallApk(context, apkFile)
                                                }
                                            }.onFailure { err ->
                                                Toast.makeText(context, "Download failed: ${err.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Re-download Package")
                                }
                            }
                        }
                    } else if (updateInfo?.hasUpdate == true && updateInfo?.downloadUrl != null) {
                        Button(
                            onClick = {
                                isDownloadingUpdate = true
                                updateDownloadProgress = 0f
                                coroutineScope.launch {
                                    val result = OtaUpdateManager.downloadApkWithProgress(context, updateInfo!!.downloadUrl!!) { progress ->
                                        updateDownloadProgress = progress
                                    }
                                    isDownloadingUpdate = false
                                    result.onSuccess { apkFile ->
                                        downloadedApkFile = apkFile
                                        if (!OtaUpdateManager.canInstallApk(context)) {
                                            Toast.makeText(context, "Please allow Lloyd Attendance to install updates", Toast.LENGTH_LONG).show()
                                            OtaUpdateManager.openInstallPermissionSettings(context)
                                        } else {
                                            OtaUpdateManager.promptInstallApk(context, apkFile)
                                        }
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Download failed: ${err.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download & Install v${updateInfo?.latestVersion}")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                isCheckingUpdate = true
                                updateMessage = null
                                coroutineScope.launch {
                                    val result = OtaUpdateManager.checkForUpdates(
                                        currentVersion = BuildConfig.VERSION_NAME,
                                        fleetEndpoint = prefs.telemetryEndpoint
                                    )
                                    isCheckingUpdate = false
                                    result.onSuccess { info ->
                                        updateInfo = info
                                        if (!info.hasUpdate) {
                                            updateMessage = "You are on the latest version (v${info.currentVersion})"
                                        }
                                    }.onFailure {
                                        updateMessage = "You are on the latest version (v${BuildConfig.VERSION_NAME})"
                                    }
                                }
                            },
                            enabled = !isCheckingUpdate,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Checking for updates...")
                            } else {
                                Text("Check for Updates")
                            }
                        }
                    }
                }
            }

            // Export / Share Report Action
            Button(
                onClick = {
                    val report = buildString {
                        appendLine("Attendance Report — Lloyd College ERP")
                        if (!userProfile?.name.isNullOrBlank()) appendLine("Student: ${userProfile?.name}")
                        if (!userProfile?.admission_no.isNullOrBlank()) appendLine("Admission No: ${userProfile?.admission_no}")
                        if (!userProfile?.course.isNullOrBlank()) appendLine("Course: ${userProfile?.course}")
                        if (!userProfile?.section.isNullOrBlank()) appendLine("Section: ${userProfile?.section}")
                        appendLine()
                        if (stats != null && stats.totalClasses > 0) {
                            appendLine("Overall Attendance: ${String.format(Locale.US, "%.1f%%", stats.overallPercentage)} (${stats.totalPresent}/${stats.totalClasses} classes attended)")
                            if (stats.bunkAllowance > 0) {
                                appendLine("Safe to bunk: ${stats.bunkAllowance} next classes (>=75% buffer)")
                            } else if (stats.neededToReach75 > 0) {
                                appendLine("Required to attend: ${stats.neededToReach75} consecutive classes to reach 75%")
                            }
                        }
                        appendLine()
                        appendLine("Official Student Attendance Summary")
                    }

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Lloyd ERP Attendance Report")
                        putExtra(Intent.EXTRA_TEXT, report)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Attendance Report"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Attendance Summary")
            }

            // Logout Action
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of Lloyd ERP")
            }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SecurityCheckItem(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = AttendanceColors.HealthyLight,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
