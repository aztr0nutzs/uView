package com.sentinel.app.ui.components

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentinel.app.domain.model.CameraStatus
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.CyanAccent
import com.sentinel.app.ui.theme.CyanSubtleBg
import com.sentinel.app.ui.theme.CyanTertiaryDim
import com.sentinel.app.ui.theme.ErrorContainer
import com.sentinel.app.ui.theme.ErrorRed
import com.sentinel.app.ui.theme.GreenGlow
import com.sentinel.app.ui.theme.GreenOnline
import com.sentinel.app.ui.theme.OrangePrimary
import com.sentinel.app.ui.theme.OrangeSubtle
import com.sentinel.app.ui.theme.StatusConnecting
import com.sentinel.app.ui.theme.StatusDisabled
import com.sentinel.app.ui.theme.StatusUnknown
import com.sentinel.app.ui.theme.SurfaceBase
import com.sentinel.app.ui.theme.SurfaceElevated
import com.sentinel.app.ui.theme.SurfaceHighest
import com.sentinel.app.ui.theme.SurfaceLowest
import com.sentinel.app.ui.theme.SurfaceStroke
import com.sentinel.app.ui.theme.TextDisabled
import com.sentinel.app.ui.theme.TextPrimary
import com.sentinel.app.ui.theme.TextSecondary

// ─────────────────────────────────────────────────────────────────────────────
// ChamferThumbnail
// 56×56dp camera thumbnail with the chamfer (diagonal-cut corner) clip-path
// from the HTML design. Matches: clip-path: polygon(10% 0, 100% 0, 100% 90%, 90% 100%, 0 100%, 0 10%)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ChamferThumbnail(
    status: CameraStatus,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    content: @Composable () -> Unit
) {
    val dotColor = when (status) {
        CameraStatus.ONLINE      -> GreenOnline
        CameraStatus.OFFLINE,
        CameraStatus.ERROR       -> ErrorRed
        CameraStatus.CONNECTING  -> StatusConnecting
        CameraStatus.DISABLED    -> StatusDisabled
        CameraStatus.UNKNOWN     -> StatusUnknown
    }
    val borderColor = when (status) {
        CameraStatus.ONLINE      -> OrangePrimary.copy(alpha = 0.4f)
        CameraStatus.OFFLINE,
        CameraStatus.ERROR       -> Color(0xFF3A3A3A)
        else                     -> Color(0xFF3A3A3A)
    }

    Box(modifier = modifier.size(size)) {
        // Chamfer-clipped container
        Box(
            modifier = Modifier
                .size(size)
                .drawBehind {
                    val w = this.size.width
                    val h = this.size.height
                    val cut = w * 0.15f
                    val path = Path().apply {
                        moveTo(cut, 0f)
                        lineTo(w, 0f)
                        lineTo(w, h - cut)
                        lineTo(w - cut, h)
                        lineTo(0f, h)
                        lineTo(0f, cut)
                        close()
                    }
                    clipPath(path) {
                        drawRect(Color(0xFF1C1C1C))
                    }
                    // Draw border around chamfer path
                    drawPath(path, borderColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
                }
        ) {
            // Clip content to chamfer shape
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val w = this.size.width
                        val h = this.size.height
                        val cut = w * 0.15f
                        val path = Path().apply {
                            moveTo(cut, 0f); lineTo(w, 0f); lineTo(w, h - cut)
                            lineTo(w - cut, h); lineTo(0f, h); lineTo(0f, cut); close()
                        }
                        clipPath(path) { drawRect(Color(0xFF1C1C1C)) }
                    }
            ) {
                content()
            }
        }

        // Status dot — top-left corner, outside the chamfer clip
        val glowAlpha by rememberInfiniteTransition(label = "dot").animateFloat(
            initialValue = 1f, targetValue = if (status == CameraStatus.CONNECTING) 0.3f else 1f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "glow"
        )
        Box(
            modifier = Modifier
                .size(9.dp)
                .offset((-2).dp, (-2).dp)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = glowAlpha))
                .align(Alignment.TopStart)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FastenerCard
// Card with corner "fastener" dots (1.5×1.5dp squares at each corner) and an
// optional left-border accent bar — matches the HTML card design.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FastenerCard(
    modifier: Modifier = Modifier,
    leftBorderColor: Color = CyanTertiaryDim,
    leftBorderWidth: Dp = 4.dp,
    backgroundColor: Color = SurfaceBase,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .drawBehind {
                // Left accent border
                drawRect(
                    color  = leftBorderColor,
                    topLeft = Offset.Zero,
                    size   = androidx.compose.ui.geometry.Size(leftBorderWidth.toPx(), this.size.height)
                )
            }
    ) {
        content()

        // Corner fastener dots
        val dotColor = SurfaceStroke
        val dotSize  = 5.dp
        Box(Modifier.size(dotSize).align(Alignment.TopStart).offset(2.dp, 2.dp).background(dotColor))
        Box(Modifier.size(dotSize).align(Alignment.TopEnd).offset((-2).dp, 2.dp).background(dotColor))
        Box(Modifier.size(dotSize).align(Alignment.BottomStart).offset(2.dp, (-2).dp).background(dotColor))
        Box(Modifier.size(dotSize).align(Alignment.BottomEnd).offset((-2).dp, (-2).dp).background(dotColor))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TacticalBadge
// Compact status badge — thin border, small italic uppercase label.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TacticalBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    Box(
        modifier = modifier
            .background(if (filled) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, color.copy(alpha = 0.4f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text       = text,
            fontSize   = 8.sp,
            fontWeight = FontWeight.Black,
            fontStyle  = FontStyle.Italic,
            color      = color,
            letterSpacing = 0.5.sp,
            maxLines   = 1
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TacticalSectionHeader
// Orange horizontal line + italic uppercase section title
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TacticalSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = OrangePrimary
) {
    Row(
        modifier          = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(2.dp)
                .background(color)
        )
        Text(
            text       = title,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Black,
            fontStyle  = FontStyle.Italic,
            color      = color,
            letterSpacing = 0.5.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LiveRecBadge — the pulsing REC indicator from screen 3
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LiveRecBadge(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "recAlpha"
    )
    Row(
        modifier = modifier
            .border(1.dp, ErrorRed.copy(alpha = alpha))
            .background(ErrorRed.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment       = Alignment.CenterVertically,
        horizontalArrangement   = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(ErrorRed.copy(alpha = alpha))
        )
        Text(
            "REC",
            fontSize   = 9.sp,
            fontWeight = FontWeight.Black,
            fontStyle  = FontStyle.Italic,
            color      = ErrorRed,
            letterSpacing = 0.8.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HudStatRow — key/value row for the stream performance and config cards
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HudStatRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .drawBehind {
                // Bottom divider line
                drawLine(
                    color       = SurfaceStroke.copy(alpha = 0.5f),
                    start       = Offset(0f, this.size.height),
                    end         = Offset(this.size.width, this.size.height),
                    strokeWidth = 1.dp.toPx()
                )
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold,
            color      = TextSecondary,
            letterSpacing = 0.3.sp
        )
        Text(
            value,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Black,
            fontStyle  = FontStyle.Italic,
            color      = valueColor,
            letterSpacing = 0.2.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TacticalActionButton — from screen 3 action strip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TacticalActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconColor: Color = TextPrimary,
    isActive: Boolean = false,
    activePulse: Boolean = false
) {
    val bgAlpha by if (activePulse) {
        rememberInfiniteTransition(label = "pulse_bg").animateFloat(
            initialValue = 0.1f, targetValue = 0.25f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "bg"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(if (isActive) 0.12f else 0f) }
            .let { state ->
                // Return a State<Float> directly
                androidx.compose.runtime.remember { state }
            }
    }

    Column(
        modifier = modifier
            .background(
                if (activePulse) iconColor.copy(alpha = bgAlpha)
                else if (isActive) iconColor.copy(alpha = 0.10f)
                else SurfaceBase
            )
            .border(
                width = 0.dp,
                color = Color.Transparent,
                // 4dp bottom border matching HTML border-b-4
            )
            .drawBehind {
                // Bottom accent border (4dp thick) — matches HTML border-b-4
                val borderColor = if (activePulse || isActive) iconColor else SurfaceStroke
                drawRect(
                    color    = borderColor,
                    topLeft  = Offset(0f, this.size.height - 4.dp.toPx()),
                    size     = androidx.compose.ui.geometry.Size(this.size.width, 4.dp.toPx())
                )
            }
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = iconColor,
            modifier           = Modifier.size(22.dp)
        )
        Text(
            text       = label,
            fontSize   = 9.sp,
            fontWeight = FontWeight.Black,
            fontStyle  = FontStyle.Italic,
            color      = if (isActive || activePulse) iconColor else TextSecondary,
            letterSpacing = 0.3.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TacticalOnlineBar — the "X/Y ONLINE" progress bar from screen 2 header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TacticalOnlineBar(
    online: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier          = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Segmented progress bar
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceHighest)
        ) {
            val ratio = if (total > 0) online.toFloat() / total.toFloat() else 0f
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(ratio)
                    .background(GreenOnline)
            )
        }
        Text(
            text       = "$online/$total ONLINE",
            fontSize   = 9.sp,
            fontWeight = FontWeight.Black,
            fontStyle  = FontStyle.Italic,
            color      = GreenOnline,
            letterSpacing = 0.8.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CornerBracket — the L-shaped corner decoration from screen 3 feed overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CornerBrackets(
    modifier: Modifier = Modifier,
    color: Color = OrangePrimary.copy(alpha = 0.5f),
    size: Dp = 24.dp,
    stroke: Dp = 3.dp
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Top-left
        Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).size(size)
            .drawBehind {
                drawLine(color, Offset(0f, stroke.toPx()/2), Offset(this.size.width, stroke.toPx()/2), stroke.toPx())
                drawLine(color, Offset(stroke.toPx()/2, 0f), Offset(stroke.toPx()/2, this.size.height), stroke.toPx())
            })
        // Top-right
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(size)
            .drawBehind {
                drawLine(color, Offset(0f, stroke.toPx()/2), Offset(this.size.width, stroke.toPx()/2), stroke.toPx())
                drawLine(color, Offset(this.size.width - stroke.toPx()/2, 0f), Offset(this.size.width - stroke.toPx()/2, this.size.height), stroke.toPx())
            })
        // Bottom-left
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).size(size)
            .drawBehind {
                drawLine(color, Offset(0f, this.size.height - stroke.toPx()/2), Offset(this.size.width, this.size.height - stroke.toPx()/2), stroke.toPx())
                drawLine(color, Offset(stroke.toPx()/2, 0f), Offset(stroke.toPx()/2, this.size.height), stroke.toPx())
            })
        // Bottom-right
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(size)
            .drawBehind {
                drawLine(color, Offset(0f, this.size.height - stroke.toPx()/2), Offset(this.size.width, this.size.height - stroke.toPx()/2), stroke.toPx())
                drawLine(color, Offset(this.size.width - stroke.toPx()/2, 0f), Offset(this.size.width - stroke.toPx()/2, this.size.height), stroke.toPx())
            })
    }
}
