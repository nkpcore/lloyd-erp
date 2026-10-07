package com.lloyd.attendance.feature.lock

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lloyd.attendance.R
import com.lloyd.attendance.core.access.AccessDecision

/**
 * Material 3 Expressive lockout screen that completely halts app access
 * when an administrative governance decision is active (Device Revocation,
 * Global Maintenance Mode, or Deprecated App Version).
 */
@Composable
fun AccessLockoutScreen(
    decision: AccessDecision,
    deviceId: String,
    onRetry: () -> Unit,
    onUpdate: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val (badgeBg, iconTint, title, subtitle, desc) = when (decision) {
        is AccessDecision.Revoked -> {
            Tuple5(
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.error,
                "Device Access Restricted",
                "Administrative Revocation",
                decision.reason
            )
        }
        is AccessDecision.Maintenance -> {
            Tuple5(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.primary,
                "Service Under Maintenance",
                "Fleet Maintenance Window",
                decision.message
            )
        }
        is AccessDecision.OutdatedVersion -> {
            Tuple5(
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.tertiary,
                "App Update Required",
                "Unsupported Version",
                "This build is no longer supported (minimum required: v${decision.minVersionCode}). Please update to continue."
            )
        }
        is AccessDecision.Authorized -> {
            Tuple5(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.colorScheme.primary,
                "Access Verified",
                "Authorized",
                ""
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Central Icon Pill
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    when (decision) {
                        is AccessDecision.Revoked -> {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        is AccessDecision.Maintenance -> {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        is AccessDecision.OutdatedVersion -> {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        else -> {
                            Image(
                                painter = painterResource(id = R.drawable.ic_brand_emblem),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = iconTint,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Device UUID support pill (click to copy)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Device UUID", deviceId))
                            Toast.makeText(context, "Device UUID copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DEVICE UUID (TAP TO COPY)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = deviceId,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy UUID",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Primary Action Button
                when (decision) {
                    is AccessDecision.Maintenance -> {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry Connection", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    is AccessDecision.OutdatedVersion -> {
                        Button(
                            onClick = {
                                val url = decision.downloadUrl ?: "https://github.com/nkpcore/lloyd-erp/releases/latest"
                                onUpdate(url)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download Latest Update", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    is AccessDecision.Revoked -> {
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check Access Again", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sign Out Button
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = MaterialTheme.shapes.medium,
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
                    Text("Sign Out", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
