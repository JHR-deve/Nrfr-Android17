package com.github.nrfr.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Color(0xFF073C41),
    primaryContainer = Color(0xFF24584F),
    onPrimaryContainer = Color(0xFFB7EFD2),
    secondary = PurpleGrey80,
    secondaryContainer = Color(0xFF344851),
    onSecondaryContainer = Color(0xFFE0ECF2),
    tertiary = Pink80,
    background = Color(0xFF121719),
    surface = Color(0xFF1C2224),
    surfaceVariant = Color(0xFF283335),
    onSurface = Color(0xFFEAF1F0),
    onSurfaceVariant = Color(0xFFB9C8C7),
    outline = Color(0xFF69787A)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8EBDD),
    onPrimaryContainer = Color(0xFF063D40),
    secondary = PurpleGrey40,
    secondaryContainer = Color(0xFFD8E9EE),
    onSecondaryContainer = Color(0xFF213D46),
    tertiary = Pink40,
    background = Color(0xFFF4FAF8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE4F0ED),
    onSurface = Color(0xFF172B2D),
    onSurfaceVariant = Color(0xFF405D60),
    outline = Color(0xFF70898B)
)

@Composable
fun NrfrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

