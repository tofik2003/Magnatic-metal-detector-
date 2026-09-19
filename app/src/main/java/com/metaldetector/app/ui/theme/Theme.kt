package com.metaldetector.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette options
enum class AppThemeMode(val displayName: String) {
    TACTICAL_CYBER("Tactical Cyber (Default)"),
    HIGH_CONTRAST_GOLD("High-Contrast Gold"),
    MILITARY_EMERALD("Military Emerald"),
    CRIMSON_RADAR("Crimson Radar")
}

// Background & Surface neutrals
val DarkBackground = Color(0xFF0A0E17)
val DarkSurface = Color(0xFF131A29)
val DarkSurfaceElevated = Color(0xFF1B2438)
val DarkBorder = Color(0xFF24324A)

// Theme 1: Tactical Cyber (Gold + Cyan)
val AccentGold = Color(0xFFFFB703)
val AccentCyan = Color(0xFF00E5FF)
val AccentGreen = Color(0xFF10B981)
val AccentOrange = Color(0xFFFB8500)
val AccentRed = Color(0xFFEF4444)

// Theme 2: High-Contrast Gold (Amber + Bright White)
val ThemeGoldPrimary = Color(0xFFFFD000)
val ThemeGoldSecondary = Color(0xFFFFA000)

// Theme 3: Military Emerald
val ThemeEmeraldPrimary = Color(0xFF00E676)
val ThemeEmeraldSecondary = Color(0xFF69F0AE)

// Theme 4: Crimson Radar
val ThemeCrimsonPrimary = Color(0xFFFF1744)
val ThemeCrimsonSecondary = Color(0xFFFF5252)

fun getThemeColorScheme(themeMode: AppThemeMode) = when (themeMode) {
    AppThemeMode.TACTICAL_CYBER -> darkColorScheme(
        primary = AccentGold,
        onPrimary = Color.Black,
        secondary = AccentCyan,
        onSecondary = Color.Black,
        tertiary = AccentGreen,
        background = DarkBackground,
        surface = DarkSurface,
        onBackground = Color(0xFFF1F5F9),
        onSurface = Color(0xFFF1F5F9),
        outline = DarkBorder
    )
    AppThemeMode.HIGH_CONTRAST_GOLD -> darkColorScheme(
        primary = ThemeGoldPrimary,
        onPrimary = Color.Black,
        secondary = ThemeGoldSecondary,
        onSecondary = Color.Black,
        tertiary = AccentGreen,
        background = Color(0xFF050505),
        surface = Color(0xFF141414),
        onBackground = Color.White,
        onSurface = Color.White,
        outline = Color(0xFF383838)
    )
    AppThemeMode.MILITARY_EMERALD -> darkColorScheme(
        primary = ThemeEmeraldPrimary,
        onPrimary = Color.Black,
        secondary = ThemeEmeraldSecondary,
        onSecondary = Color.Black,
        tertiary = AccentGold,
        background = Color(0xFF07110A),
        surface = Color(0xFF0E1F14),
        onBackground = Color(0xFFE8F5E9),
        onSurface = Color(0xFFE8F5E9),
        outline = Color(0xFF1B3824)
    )
    AppThemeMode.CRIMSON_RADAR -> darkColorScheme(
        primary = ThemeCrimsonPrimary,
        onPrimary = Color.White,
        secondary = ThemeCrimsonSecondary,
        onSecondary = Color.White,
        tertiary = AccentGold,
        background = Color(0xFF120507),
        surface = Color(0xFF210B0E),
        onBackground = Color(0xFFFFEBEE),
        onSurface = Color(0xFFFFEBEE),
        outline = Color(0xFF3E161C)
    )
}

@Composable
fun MetalDetectorTheme(
    themeMode: AppThemeMode = AppThemeMode.TACTICAL_CYBER,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = getThemeColorScheme(themeMode),
        content = content
    )
}
