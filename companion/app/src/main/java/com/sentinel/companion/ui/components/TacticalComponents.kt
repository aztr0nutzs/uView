package com.sentinel.companion.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentinel.companion.data.model.AlertType
import com.sentinel.companion.data.model.DeviceState
import com.sentinel.companion.ui.theme.BackgroundDeep
import com.sentinel.companion.ui.theme.CyanSubtleBg
import com.sentinel.companion.ui.theme.CyanTertiaryDim
import com.sentinel.companion.ui.theme.ErrorContainer
import com.sentinel.companion.ui.theme.ErrorRed
import com.sentinel.companion.ui.theme.GreenContainer
import com.sentinel.companion.ui.theme.GreenOnline
import com.sentinel.companion.ui.theme.OrangeContainer
import com.sentinel.companion.ui.theme.OrangeGlow
import com.sentinel.companion.ui.theme.OrangePrimary
import com.sentinel.companion.ui.theme.OrangeSubtle
import com.sentinel.companion.ui.theme.StatusConnecting
import com.sentinel.companion.ui.theme.StatusDisabled
import com.sentinel.companion.ui.theme.SurfaceBase
import com.sentinel.companion.ui.theme.SurfaceElevated
import com.sentinel.companion.ui.theme.SurfaceHighest
import com.sentinel.companion.ui.theme.SurfaceLow
import com.sentinel.companion.ui.theme.SurfaceLowest
import com.sentinel.companion.ui.theme.SurfaceStroke
import com.sentinel.companion.ui.theme.TextDisabled
import com.sentinel.companion.ui.theme.TextOnOrange
import com.sentinel.companion.ui.theme.TextPrimary
import com.sentinel.companion.ui.theme.TextSecondary

// ─── Carbon dot background texture ───────────────────────────────────────────

fun Modifier.carbonBackground(): Modifier = drawBehind {
    val dotRadius  = 0.45.dp.toPx()
    val gridSize   = 4.dp.toPx()
    val dotColor   = Color(0xFF1A1A1A)
    var y = 0f
    while (y < size.height) {
        var x = 0f
        while (x < size.width) {
            drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
            x += gridSize
        }
        y += gridSize
    }
}

// ─── Chamfer (diagonal-cut) thumbnail clip ───────────────────────────────────

fun Modifier.chamferClip(cutFraction: Float = 0.15f): Modifier = clip(
    object : androidx.compose.ui.graphics.Shape {
        override fun createOutline(
            size: androidx.compose.ui.geometry.Size,
            layoutDirection: androidx.compose.ui.unit.LayoutDirection,
            density: androidx.compose.ui.unit.Density,
        ): androidx.compose.ui.graphics.Outline {
            val cut = (size.width * cutFraction)
            val path = Path().apply {
                moveTo(cut, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height - cut)
                lineTo(size.width - cut, size.height)
                lineTo(0f, size.height)
                lineTo(0f, cut)
                close()
            }
            return androidx.compose.ui.graphics.Outline.Generic(path)
        }
    }
)

// ─── Corner bracket overlay (tactical feed decoration) ───────────────────────

@Composable
fun CornerBrackets(
    color: Color = OrangePrimary,
    size: Dp = 20.dp,
    strokeWidth: Dp = 2.dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // TL
        CornerBracket(color = color, size = size, strokeWidth = strokeWidth,
            modifier = Modifier.align(Alignment.TopStart))
        // TR
        CornerBracket(color = color, size = size, strokeWidth = strokeWidth, flipH = true,
            modifier = Modifier.align(Alignment.TopEnd))
        // BL
        CornerBracket(color = color, size = size, strokeWidth = strokeWidth, flipV = true,
            modifier = Modifier.align(Alignment.BottomStart))
        // BR
        CornerBracket(color = color, size = size, strokeWidth = strokeWidth, flipH = true, flipV = true,
            modifier = Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun CornerBracket(
    color: Color,
    size: Dp,
    strokeWidth: Dp,
    flipH: Boolean = false,
    flipV: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val w = this.size.width
                val h = this.size.height
                val sw = strokeWidth.toPx()
                val x1 = if (flipH) w else 0f
                val x2 = if (flipH) w - w * 0.6f else w * 0.6f
                val y1 = if (flipV) h else 0f
                val y2 = if (flipV) h - h * 0.6f else h * 0.6f
                drawLine(color, Offset(x1, y1), Offset(x2, y1), strokeWidth = sw)
                drawLine(color, Offset(x1, y1), Offset(x1, y2), strokeWidth = sw)
            }
    )
}

// ─── Section header bar ───────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .background(OrangePrimary)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = OrangePrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = 1.5.sp,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) trailing()
    }
}

// ─── Stat card ────────────────────────────────────────────────────────────────

@Composable
fun StatCard(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(SurfaceBase, RoundedCornerShape(14.dp))
            .border(1.dp, SurfaceStroke, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
        )
    }
}

// ─── Status badge ─────────────────────────────────────────────────────────────

@Composable
fun StatusBadge(state: DeviceState, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (state) {
        DeviceState.ONLINE      -> Triple(GreenContainer,   GreenOnline,      "ONLINE")
        DeviceState.OFFLINE     -> Triple(ErrorContainer,   ErrorRed,         "OFFLINE")
        DeviceState.CONNECTING  -> Triple(Color(0xFF2A2000), StatusConnecting, "LINKING")
        DeviceState.DISABLED    -> Triple(SurfaceHighest,   StatusDisabled,   "DISABLED")
        DeviceState.UNKNOWN     -> Triple(SurfaceHighest,   TextDisabled,     "UNKNOWN")
    }
    Row(
        modifier = modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PulseDot(color = fg, size = 5.dp, animate = state == DeviceState.ONLINE)
        Text(
            text = label,
            color = fg,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
        )
    }
}

// ─── Source type badge ────────────────────────────────────────────────────────

@Composable
fun SourceTypeBadge(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        color = CyanTertiaryDim,
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.5.sp,
        modifier = modifier
            .background(CyanSubtleBg, RoundedCornerShape(4.dp))
            .border(1.dp, CyanTertiaryDim.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

// ─── Pulsing status dot ───────────────────────────────────────────────────────

@Composable
fun PulseDot(
    color: Color,
    size: Dp = 8.dp,
    animate: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by if (animate) infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 0.25f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "dotAlpha",
    ) else infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000)),
        label = "dotAlphaStatic",
    )
    Box(
        modifier = modifier
            .size(size)
            .background(color.copy(alpha = alpha), CircleShape)
    )
}

// ─── Tactical label (uppercase, italic, black weight) ────────────────────────

@Composable
fun TacticalLabel(
    text: String,
    color: Color = OrangePrimary,
    size: Float = 10f,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        fontSize = size.sp,
        fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic,
        letterSpacing = 1.sp,
        modifier = modifier,
    )
}

// ─── Section card container ───────────────────────────────────────────────────

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceBase, RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceStroke, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        content()
    }
}

// ─── Alert icon color by type ─────────────────────────────────────────────────

fun alertColor(type: AlertType): Color = when (type) {
    AlertType.MOTION              -> OrangePrimary
    AlertType.CONNECTION_LOST     -> ErrorRed
    AlertType.CONNECTION_RESTORED -> GreenOnline
    AlertType.RECORDING_STARTED   -> ErrorRed
    AlertType.RECORDING_STOPPED   -> TextSecondary
    AlertType.SNAPSHOT            -> CyanTertiaryDim
    AlertType.SYSTEM              -> TextSecondary
}

// ─── Top app bar (tactical style) ────────────────────────────────────────────

@Composable
fun CompanionTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isConnected: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceLow)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OrangePrimary,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onBack() },
                )
                Spacer(Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = OrangePrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(
                            if (isConnected) GreenContainer else ErrorContainer,
                            RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    PulseDot(
                        color  = if (isConnected) GreenOnline else ErrorRed,
                        size   = 5.dp,
                        animate= isConnected,
                    )
                    Text(
                        text  = if (isConnected) "LINKED" else "OFFLINE",
                        color = if (isConnected) GreenOnline else ErrorRed,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                    )
                }
                if (trailing != null) trailing()
            }
        }
        HorizontalDivider(
            modifier  = Modifier.padding(top = 10.dp),
            thickness = 1.dp,
            color     = OrangePrimary.copy(alpha = 0.25f),
        )
    }
}

// ─── Primary / Ghost buttons ──────────────────────────────────────────────────

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .background(
                if (enabled) OrangePrimary else SurfaceHighest,
                RoundedCornerShape(12.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) TextOnOrange else TextDisabled,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = if (enabled) TextOnOrange else TextDisabled,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = OrangePrimary,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .border(1.dp, color, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
        )
    }
}

// ─── Info metric row (key → value) ───────────────────────────────────────────

@Composable
fun MetricRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─── Settings row ─────────────────────────────────────────────────────────────

@Composable
fun SettingsRow(
    icon: ImageVector,
    iconTint: Color = OrangePrimary,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
        }
        if (trailing != null) trailing()
    }
}
