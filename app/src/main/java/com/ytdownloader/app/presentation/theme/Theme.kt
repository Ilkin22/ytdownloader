package com.ytdownloader.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = OnPrimary,
    secondary = Accent,
    onSecondary = OnPrimary,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = OnSurface,
    background = Secondary,
    onBackground = OnSurface,
    surface = SurfaceCard,
    onSurface = OnSurface,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = OnSurfaceVariant,
    error = Error,
    onError = OnPrimary,
    outline = GlassBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = Color.White,
    secondary = PrimaryDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF0F0),
    onSecondaryContainer = Color(0xFF8B0000),
    background = Color(0xFFF8F8F8),
    onBackground = Color(0xFF1C1C1C),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1C),
    surfaceVariant = Color(0xFFF3F3F3),
    onSurfaceVariant = Color(0xFF666666),
    error = Error,
    onError = Color.White,
    outline = Color(0xFFDDDDDD)
)

@Composable
fun YtDownloaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
