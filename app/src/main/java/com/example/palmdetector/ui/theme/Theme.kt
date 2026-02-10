package com.example.palmdetector.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Black = Color(0xFF000000)
val DarkGray = Color(0xFF121212)
val LightGray = Color(0xFFF5F5F5)
val White = Color(0xFFFFFFFF)
val GrayBorder = Color(0xFFE0E0E0)

val SuccessGreen = Color(0xFF2E7D32)
val ErrorRed = Color(0xFFD32F2F)
val WarningYellow = Color(0xFFF9A825)

val TextBlack = Color(0xFF1C1B1F)
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFF757575)


private val DarkColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    secondary = DarkGray,
    onSecondary = White,
    background = Black,
    surface = DarkGray,
    onSurface = White,
    error = ErrorRed,
    outline = GrayBorder
)


private val LightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    secondary = LightGray,
    onSecondary = Black,
    background = White,
    surface = White,
    onSurface = TextBlack,
    error = ErrorRed,
    outline = GrayBorder
)

@Composable
fun PalmDetectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
