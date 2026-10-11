package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = NeonPink,
    tertiary = ArcadeGold,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color(0xFF070B18),
    onSecondary = Color.White,
    onTertiary = Color(0xFF070B18),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
