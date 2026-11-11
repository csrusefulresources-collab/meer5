package com.baseerat.vision.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// High-contrast, calm palette
private val Primary = Color(0xFF1B4F72)
private val PrimaryDark = Color(0xFF0E2F44)
private val Secondary = Color(0xFFB9770E)
private val BackgroundLight = Color(0xFFF7F9FC)
private val SurfaceLight = Color(0xFFFFFFFF)
private val OnSurfaceLight = Color(0xFF1A252F)

private val BackgroundDark = Color(0xFF0F1419)
private val SurfaceDark = Color(0xFF1A2332)
private val OnSurfaceDark = Color(0xFFE8EEF4)

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E6F1),
    onPrimaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFDEBD0),
    onSecondaryContainer = Color(0xFF5D3A00),
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFE8EEF4),
    onSurfaceVariant = Color(0xFF4A5C6A),
    error = Color(0xFFC0392B),
    outline = Color(0xFFA0B0C0)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5DADE2),
    onPrimary = Color(0xFF0A1929),
    primaryContainer = PrimaryDark,
    onPrimaryContainer = Color(0xFFD4E6F1),
    secondary = Color(0xFFF5B041),
    onSecondary = Color(0xFF3D2A00),
    secondaryContainer = Color(0xFF5D3A00),
    onSecondaryContainer = Color(0xFFFDEBD0),
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF2A3544),
    onSurfaceVariant = Color(0xFFB0C0D0),
    error = Color(0xFFE74C3C),
    outline = Color(0xFF5A6A7A)
)

@Composable
fun BaseeratTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
