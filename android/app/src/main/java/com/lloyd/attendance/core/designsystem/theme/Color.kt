package com.lloyd.attendance.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ==========================================
// Warm Designer Palette (User Specified)
// Honey Bronze, Linen, Cotton Rose, Muted Teal, Light Coral
// ==========================================
val HoneyBronze = Color(0xFFF6BD60)
val LinenSurface = Color(0xFFF7EDE2)
val CottonRoseContainer = Color(0xFFF5CAC3)
val MutedTealPrimary = Color(0xFF84A59D)
val LightCoralError = Color(0xFFF28482)

// Light Theme Surface & Text
val LightBackground = Color(0xFFFDFBF7)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF2B2D42)
val LightOnSurfaceVariant = Color(0xFF6C757D)
val LightOutline = Color(0xFFD8D2C2)

// Dark Theme Variants
val DarkBackground = Color(0xFF161B22)
val DarkSurface = Color(0xFF21262D)
val DarkSurfaceVariant = Color(0xFF30363D)
val HoneyBronzeDark = Color(0xFFF8C87A)
val MutedTealDark = Color(0xFF9DC0B8)
val LightCoralDark = Color(0xFFF59D9B)
val DarkOnSurface = Color(0xFFF0F6FC)
val DarkOnSurfaceVariant = Color(0xFF8B949E)
val DarkOutline = Color(0xFF484F58)
val DarkOutlineVariant = Color(0xFF30363D)

// Surface Container Hierarchy (Light)
val LloydSurfaceContainerLowestLight = Color(0xFFFFFFFF)
val LloydSurfaceContainerLowLight = Color(0xFFFBF7F0)
val LloydSurfaceContainerLight = Color(0xFFF7EDE2) // Linen
val LloydSurfaceContainerHighLight = Color(0xFFEFE2D3)
val LloydSurfaceContainerHighestLight = Color(0xFFE4D5C4)
val LloydSurfaceDimLight = Color(0xFFEBE0D3)
val LloydSurfaceBrightLight = Color(0xFFFFFFFF)

// Surface Container Hierarchy (Dark)
val LloydSurfaceContainerLowestDark = Color(0xFF0D1117)
val LloydSurfaceContainerLowDark = Color(0xFF161B22)
val LloydSurfaceContainerDark = Color(0xFF21262D)
val LloydSurfaceContainerHighDark = Color(0xFF2C323B)
val LloydSurfaceContainerHighestDark = Color(0xFF373E47)
val LloydSurfaceDimDark = Color(0xFF161B22)
val LloydSurfaceBrightDark = Color(0xFF30363D)

// ==========================================
// Semantic Attendance Health Colors
// ==========================================
object AttendanceColors {
    val HealthyLight = Color(0xFF4D8B7D)
    val HealthyContainerLight = Color(0xFFD4EAE5)
    val OnHealthyContainerLight = Color(0xFF1B4D43)

    val HealthyDark = MutedTealDark
    val HealthyContainerDark = Color(0xFF204840)
    val OnHealthyContainerDark = Color(0xFFCBE8E1)

    val BorderlineLight = Color(0xFFE5A638)
    val BorderlineContainerLight = Color(0xFFFFF2D6)
    val OnBorderlineContainerLight = Color(0xFF634100)

    val BorderlineDark = HoneyBronzeDark
    val BorderlineContainerDark = Color(0xFF5E4300)
    val OnBorderlineContainerDark = Color(0xFFFFECC4)

    val CriticalLight = LightCoralError
    val CriticalContainerLight = Color(0xFFFFE0DF)
    val OnCriticalContainerLight = Color(0xFF6B1D1D)

    val CriticalDark = LightCoralDark
    val CriticalContainerDark = Color(0xFF631F1F)
    val OnCriticalContainerDark = Color(0xFFFFD5D4)

    val Unrecorded = Color(0xFF8C929D)
    val UnrecordedContainer = LinenSurface
}

// Light Color Scheme
val LloydLightColorScheme = lightColorScheme(
    primary = MutedTealPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = CottonRoseContainer,
    onPrimaryContainer = Color(0xFF442B28),
    secondary = HoneyBronze,
    onSecondary = Color(0xFF3E2C00),
    secondaryContainer = Color(0xFFFFE6BC),
    onSecondaryContainer = Color(0xFF533B00),
    tertiary = Color(0xFF708D81),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD9E7E0),
    onTertiaryContainer = Color(0xFF1A352C),
    error = LightCoralError,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LinenSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = Color(0xFFE7DEC8),
    surfaceContainerLowest = LloydSurfaceContainerLowestLight,
    surfaceContainerLow = LloydSurfaceContainerLowLight,
    surfaceContainer = LloydSurfaceContainerLight,
    surfaceContainerHigh = LloydSurfaceContainerHighLight,
    surfaceContainerHighest = LloydSurfaceContainerHighestLight,
    surfaceDim = LloydSurfaceDimLight,
    surfaceBright = LloydSurfaceBrightLight
)

// Dark Color Scheme
val LloydDarkColorScheme = darkColorScheme(
    primary = MutedTealDark,
    onPrimary = Color(0xFF113832),
    primaryContainer = Color(0xFF28544D),
    onPrimaryContainer = Color(0xFFD4EAE5),
    secondary = HoneyBronzeDark,
    onSecondary = Color(0xFF422F00),
    secondaryContainer = Color(0xFF5F4400),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = CottonRoseContainer,
    onTertiary = Color(0xFF4E2623),
    tertiaryContainer = Color(0xFF693C38),
    onTertiaryContainer = Color(0xFFFFDAD6),
    error = LightCoralDark,
    onError = Color(0xFF561E1E),
    errorContainer = Color(0xFF732827),
    onErrorContainer = Color(0xFFFFDAD6),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    surfaceContainerLowest = LloydSurfaceContainerLowestDark,
    surfaceContainerLow = LloydSurfaceContainerLowDark,
    surfaceContainer = LloydSurfaceContainerDark,
    surfaceContainerHigh = LloydSurfaceContainerHighDark,
    surfaceContainerHighest = LloydSurfaceContainerHighestDark,
    surfaceDim = LloydSurfaceDimDark,
    surfaceBright = LloydSurfaceBrightDark
)
