package com.sentinel.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary          = OrangePrimary,
    onPrimary        = TextOnOrange,
    primaryContainer = OrangeContainer,
    onPrimaryContainer = TextPrimary,

    secondary        = GreenOnline,
    onSecondary      = Color(0xFF0C5C00),
    secondaryContainer = GreenContainer,
    onSecondaryContainer = Color(0xFFE7FFD9),

    tertiary         = CyanTertiary,
    onTertiary       = Color(0xFF005F65),
    tertiaryContainer = CyanFixed,
    onTertiaryContainer = Color(0xFF00555B),

    error            = ErrorRed,
    onError          = Color(0xFF450900),
    errorContainer   = ErrorContainer,
    onErrorContainer = Color(0xFFFFD2C8),

    background       = BackgroundDeep,
    onBackground     = TextPrimary,

    surface          = BackgroundDeep,
    onSurface        = TextPrimary,
    surfaceVariant   = SurfaceHighest,
    onSurfaceVariant = TextSecondary,

    outline          = OutlineColor,
    outlineVariant   = SurfaceStroke,

    inverseSurface   = TextPrimary,
    inverseOnSurface = BackgroundDeep,
    inversePrimary   = Color(0xFFA14100),

    surfaceTint      = OrangePrimary,
    scrim            = FeedScrim
)

private val LightColorScheme = lightColorScheme(
    primary          = Color(0xFFA14100),
    onPrimary        = Color(0xFFFFFFFF),
    background       = Color(0xFFF5F0EE),
    onBackground     = Color(0xFF1A1A1A),
    surface          = Color(0xFFFFFFFF),
    onSurface        = Color(0xFF1A1A1A)
)

@Composable
fun SentinelTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor  = BackgroundDeep.toArgb()
            window.navigationBarColor = BackgroundDeep.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = SentinelTypography,
        content     = content
    )
}
