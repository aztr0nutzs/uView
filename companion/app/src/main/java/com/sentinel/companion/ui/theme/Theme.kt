package com.sentinel.companion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CompanionDarkColors = darkColorScheme(
    primary            = OrangePrimary,
    onPrimary          = TextOnOrange,
    primaryContainer   = OrangeContainer,
    onPrimaryContainer = TextPrimary,

    secondary          = GreenOnline,
    onSecondary        = Color(0xFF0C5C00),
    secondaryContainer = GreenContainer,
    onSecondaryContainer = TextPrimary,

    tertiary           = CyanTertiary,
    onTertiary         = Color(0xFF00363B),
    tertiaryContainer  = CyanSubtleBg,
    onTertiaryContainer= CyanTertiary,

    error              = ErrorRed,
    onError            = Color(0xFF690000),
    errorContainer     = ErrorContainer,
    onErrorContainer   = TextPrimary,

    background         = BackgroundDeep,
    onBackground       = TextPrimary,
    surface            = BackgroundDeep,
    onSurface          = TextPrimary,
    surfaceVariant     = SurfaceHighest,
    onSurfaceVariant   = TextSecondary,

    outline            = OutlineColor,
    outlineVariant     = SurfaceStroke,
    scrim              = FeedScrim,

    inverseSurface     = TextPrimary,
    inverseOnSurface   = BackgroundDeep,
    inversePrimary     = OrangeDim,
)

@Composable
fun SentinelCompanionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CompanionDarkColors,
        typography  = CompanionTypography,
        content     = content,
    )
}
