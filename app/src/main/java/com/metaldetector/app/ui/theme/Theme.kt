package com.metaldetector.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF0C1017)
val DarkSurface = Color(0xFF161E2E)
val DarkSurfaceElevated = Color(0xFF1E293B)
val DarkBorder = Color(0xFF334155)

val AccentGold = Color(0xFFFFB703)
val AccentCyan = Color(0xFF00E5FF)
val AccentGreen = Color(0xFF10B981)
val AccentOrange = Color(0xFFFB8500)
val AccentRed = Color(0xFFEF4444)

private val DarkColorScheme = darkColorScheme(
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

@Composable
fun MetalDetectorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
