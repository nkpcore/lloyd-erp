package com.lloyd.attendance.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ==========================================
// Brand Seed Colors: Lloyd Academic Navy & Teal
// ==========================================
val LloydPrimaryLight = Color(0xFF1E40AF) // Royal Navy
val LloydOnPrimaryLight = Color(0xFFFFFFFF)
val LloydPrimaryContainerLight = Color(0xFFDBEAFE)
val LloydOnPrimaryContainerLight = Color(0xFF1E3A8A)

val LloydSecondaryLight = Color(0xFF0F766E) // Deep Teal
val LloydOnSecondaryLight = Color(0xFFFFFFFF)
val LloydSecondaryContainerLight = Color(0xFFCCFBF1)
val LloydOnSecondaryContainerLight = Color(0xFF115E59)

val LloydTertiaryLight = Color(0xFF6D28D9) // Violet Accent
val LloydOnTertiaryLight = Color(0xFFFFFFFF)
val LloydTertiaryContainerLight = Color(0xFFEDE9FE)
val LloydOnTertiaryContainerLight = Color(0xFF4C1D95)

val LloydErrorLight = Color(0xFFBA1A1A)
val LloydOnErrorLight = Color(0xFFFFFFFF)
val LloydErrorContainerLight = Color(0xFFFFDAD6)
val LloydOnErrorContainerLight = Color(0xFF410002)

val LloydBackgroundLight = Color(0xFFF8FAFC) // Slate 50
val LloydOnBackgroundLight = Color(0xFF0F172A) // Slate 900
val LloydSurfaceLight = Color(0xFFF8FAFC)
val LloydOnSurfaceLight = Color(0xFF0F172A)
val LloydSurfaceVariantLight = Color(0xFFE2E8F0) // Slate 200
val LloydOnSurfaceVariantLight = Color(0xFF475569) // Slate 600
val LloydOutlineLight = Color(0xFF94A3B8) // Slate 400
val LloydOutlineVariantLight = Color(0xFFCBD5E1) // Slate 300

// Surface Container Hierarchy (Light)
val LloydSurfaceContainerLowestLight = Color(0xFFFFFFFF)
val LloydSurfaceContainerLowLight = Color(0xFFF1F5F9)
val LloydSurfaceContainerLight = Color(0xFFE2E8F0)
val LloydSurfaceContainerHighLight = Color(0xFFCBD5E1)
val LloydSurfaceContainerHighestLight = Color(0xFF94A3B8)
val LloydSurfaceDimLight = Color(0xFFE2E8F0)
val LloydSurfaceBrightLight = Color(0xFFFFFFFF)

// ==========================================
// Dark Theme Tones
// ==========================================
val LloydPrimaryDark = Color(0xFF93C5FD) // Soft Light Blue
val LloydOnPrimaryDark = Color(0xFF1E3A8A)
val LloydPrimaryContainerDark = Color(0xFF1E40AF)
val LloydOnPrimaryContainerDark = Color(0xFFDBEAFE)

val LloydSecondaryDark = Color(0xFF5EEAD4) // Mint Teal
val LloydOnSecondaryDark = Color(0xFF115E59)
val LloydSecondaryContainerDark = Color(0xFF0F766E)
val LloydOnSecondaryContainerDark = Color(0xFFCCFBF1)

val LloydTertiaryDark = Color(0xFFC4B5FD) // Soft Violet
val LloydOnTertiaryDark = Color(0xFF4C1D95)
val LloydTertiaryContainerDark = Color(0xFF6D28D9)
val LloydOnTertiaryContainerDark = Color(0xFFEDE9FE)

val LloydErrorDark = Color(0xFFFFB4AB)
val LloydOnErrorDark = Color(0xFF690005)
val LloydErrorContainerDark = Color(0xFF93000A)
val LloydOnErrorContainerDark = Color(0xFFFFDAD6)

val LloydBackgroundDark = Color(0xFF0B0F19) // Rich Deep Slate
val LloydOnBackgroundDark = Color(0xFFF1F5F9)
val LloydSurfaceDark = Color(0xFF0B0F19)
val LloydOnSurfaceDark = Color(0xFFF1F5F9)
val LloydSurfaceVariantDark = Color(0xFF1E293B) // Slate 800
val LloydOnSurfaceVariantDark = Color(0xFF94A3B8) // Slate 400
val LloydOutlineDark = Color(0xFF475569) // Slate 600
val LloydOutlineVariantDark = Color(0xFF334155) // Slate 700

// Surface Container Hierarchy (Dark)
val LloydSurfaceContainerLowestDark = Color(0xFF030712)
val LloydSurfaceContainerLowDark = Color(0xFF0F172A)
val LloydSurfaceContainerDark = Color(0xFF1E293B)
val LloydSurfaceContainerHighDark = Color(0xFF334155)
val LloydSurfaceContainerHighestDark = Color(0xFF475569)
val LloydSurfaceDimDark = Color(0xFF0B0F19)
val LloydSurfaceBrightDark = Color(0xFF1E293B)

// ==========================================
// Semantic Attendance Health Colors
// ==========================================
object AttendanceColors {
    val HealthyLight = Color(0xFF059669)
    val HealthyContainerLight = Color(0xFFD1FAE5)
    val OnHealthyContainerLight = Color(0xFF065F46)

    val HealthyDark = Color(0xFF34D399)
    val HealthyContainerDark = Color(0xFF064E3B)
    val OnHealthyContainerDark = Color(0xFFA7F3D0)

    val BorderlineLight = Color(0xFFD97706)
    val BorderlineContainerLight = Color(0xFFFEF3C7)
    val OnBorderlineContainerLight = Color(0xFF92400E)

    val BorderlineDark = Color(0xFFFBBF24)
    val BorderlineContainerDark = Color(0xFF78350F)
    val OnBorderlineContainerDark = Color(0xFFFDE68A)

    val CriticalLight = Color(0xFFDC2626)
    val CriticalContainerLight = Color(0xFFFEE2E2)
    val OnCriticalContainerLight = Color(0xFF991B1B)

    val CriticalDark = Color(0xFFF87171)
    val CriticalContainerDark = Color(0xFF7F1D1D)
    val OnCriticalContainerDark = Color(0xFFFECACA)

    val Unrecorded = Color(0xFF64748B)
    val UnrecordedContainer = Color(0xFFF1F5F9)
}

val LloydLightColorScheme = lightColorScheme(
    primary = LloydPrimaryLight,
    onPrimary = LloydOnPrimaryLight,
    primaryContainer = LloydPrimaryContainerLight,
    onPrimaryContainer = LloydOnPrimaryContainerLight,
    secondary = LloydSecondaryLight,
    onSecondary = LloydOnSecondaryLight,
    secondaryContainer = LloydSecondaryContainerLight,
    onSecondaryContainer = LloydOnSecondaryContainerLight,
    tertiary = LloydTertiaryLight,
    onTertiary = LloydOnTertiaryLight,
    tertiaryContainer = LloydTertiaryContainerLight,
    onTertiaryContainer = LloydOnTertiaryContainerLight,
    error = LloydErrorLight,
    onError = LloydOnErrorLight,
    errorContainer = LloydErrorContainerLight,
    onErrorContainer = LloydOnErrorContainerLight,
    background = LloydBackgroundLight,
    onBackground = LloydOnBackgroundLight,
    surface = LloydSurfaceLight,
    onSurface = LloydOnSurfaceLight,
    surfaceVariant = LloydSurfaceVariantLight,
    onSurfaceVariant = LloydOnSurfaceVariantLight,
    outline = LloydOutlineLight,
    outlineVariant = LloydOutlineVariantLight,
    surfaceContainerLowest = LloydSurfaceContainerLowestLight,
    surfaceContainerLow = LloydSurfaceContainerLowLight,
    surfaceContainer = LloydSurfaceContainerLight,
    surfaceContainerHigh = LloydSurfaceContainerHighLight,
    surfaceContainerHighest = LloydSurfaceContainerHighestLight,
    surfaceDim = LloydSurfaceDimLight,
    surfaceBright = LloydSurfaceBrightLight
)

val LloydDarkColorScheme = darkColorScheme(
    primary = LloydPrimaryDark,
    onPrimary = LloydOnPrimaryDark,
    primaryContainer = LloydPrimaryContainerDark,
    onPrimaryContainer = LloydOnPrimaryContainerDark,
    secondary = LloydSecondaryDark,
    onSecondary = LloydOnSecondaryDark,
    secondaryContainer = LloydSecondaryContainerDark,
    onSecondaryContainer = LloydOnSecondaryContainerDark,
    tertiary = LloydTertiaryDark,
    onTertiary = LloydOnTertiaryDark,
    tertiaryContainer = LloydTertiaryContainerDark,
    onTertiaryContainer = LloydOnTertiaryContainerDark,
    error = LloydErrorDark,
    onError = LloydOnErrorDark,
    errorContainer = LloydErrorContainerDark,
    onErrorContainer = LloydOnErrorContainerDark,
    background = LloydBackgroundDark,
    onBackground = LloydOnBackgroundDark,
    surface = LloydSurfaceDark,
    onSurface = LloydOnSurfaceDark,
    surfaceVariant = LloydSurfaceVariantDark,
    onSurfaceVariant = LloydOnSurfaceVariantDark,
    outline = LloydOutlineDark,
    outlineVariant = LloydOutlineVariantDark,
    surfaceContainerLowest = LloydSurfaceContainerLowestDark,
    surfaceContainerLow = LloydSurfaceContainerLowDark,
    surfaceContainer = LloydSurfaceContainerDark,
    surfaceContainerHigh = LloydSurfaceContainerHighDark,
    surfaceContainerHighest = LloydSurfaceContainerHighestDark,
    surfaceDim = LloydSurfaceDimDark,
    surfaceBright = LloydSurfaceBrightDark
)
