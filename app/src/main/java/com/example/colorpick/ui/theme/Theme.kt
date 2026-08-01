package com.example.colorpick.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = iOSSystemBlue,
    onPrimary = iOSWhite,
    background = iOSWhite,
    onBackground = iOSBlack,
    surface = iOSWhite,
    onSurface = iOSBlack,
    surfaceVariant = iOSLightGray,
    onSurfaceVariant = iOSGray,
    error = iOSSystemRed
)

private val DarkColorScheme = darkColorScheme(
    primary = iOSSystemBlue,
    onPrimary = iOSWhite,
    background = iOSBlack,
    onBackground = iOSWhite,
    surface = iOSDarkGray,
    onSurface = iOSWhite,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = iOSGray,
    error = iOSSystemRed
)

@Composable
fun ColorPickTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
