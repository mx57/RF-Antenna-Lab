package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RfCyanPrimary,
    onPrimary = Color(0xFF002630),
    primaryContainer = RfCyanContainer,
    onPrimaryContainer = Color(0xFFB8F4FF),
    secondary = RfAmberSecondary,
    onSecondary = Color(0xFF2B1B00),
    secondaryContainer = RfAmberContainer,
    onSecondaryContainer = Color(0xFFFFDEA1),
    tertiary = RfEmeraldTertiary,
    onTertiary = Color(0xFF00291B),
    background = RfNavyDark,
    onBackground = Color(0xFFE5EEF8),
    surface = RfSurfaceDark,
    onSurface = Color(0xFFE5EEF8),
    surfaceVariant = RfCardDark,
    onSurfaceVariant = Color(0xFF9FB3C8),
    error = RfCoralError
)

private val LightColorScheme = lightColorScheme(
    primary = RfLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC2F2FF),
    onPrimaryContainer = Color(0xFF001F25),
    secondary = RfLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDEA1),
    onSecondaryContainer = Color(0xFF2A1800),
    tertiary = RfLightTertiary,
    onTertiary = Color.White,
    background = RfLightBackground,
    onBackground = Color(0xFF0F172A),
    surface = RfLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
