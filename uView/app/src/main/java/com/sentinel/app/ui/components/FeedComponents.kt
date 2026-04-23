package com.sentinel.app.ui.components

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.PlayerState
import com.sentinel.app.ui.theme.FeedBorder
import com.sentinel.app.ui.theme.FeedBorderActive
import com.sentinel.app.ui.theme.FeedScrim
import com.sentinel.app.ui.theme.RecordingRed
import com.sentinel.app.ui.theme.SurfaceBase
import com.sentinel.app.ui.theme.SurfaceElevated
import com.sentinel.app.ui.theme.SurfaceHighest
import com.sentinel.app.ui.theme.TextDisabled
import com.sentinel.app.ui.theme.TextPrimary
import com.sentinel.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// CameraFeedTile
//
// 16:9 tile used in dashboard pinned row and discovery cards.
// Accepts either an ExoPlayer (RTSP/HLS) or an MJPEG frame flow, both optional.
// When both are null the idle/offline placeholder renders.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
@Composable
fun CameraFeedTile(
    camera: CameraDevice,
    playerState: PlayerState,
    isSelected: Boolean = false,
    exoPlayer: ExoPlayer? = null,
    mjpegFrames: Flow<Bitmap>? = null,
    onTileClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    onReconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) FeedBorderActive else FeedBorder

    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(SurfaceBase)
            .clickable(onClick = onTileClick)
    ) {
        // ── Stream renderer ───────────────────────────────────────────────
        LiveStreamSurface(
            playerState = playerState,
            exoPlayer   = exoPlayer,
            mjpegFrames = mjpegFrames,
            modifier    = Modifier.fillMaxSize()
        )

        // ── Bottom gradient scrim + camera info ───────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, FeedScrim)))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    camera.name,
                    style      = MaterialTheme.typography.labelLarge,
                    color      = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    camera.room,
                    style   = MaterialTheme.typography.labelSmall,
                    color   = TextSecondary,
                    maxLines = 1
                )
            }
        }

        // ── Top-right: status chip ────────────────────────────────────────
        StatusChip(
            status   = camera.displayStatus,
            compact  = true,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        )

        // ── Top-left: LIVE badge ──────────────────────────────────────────
        if (playerState is PlayerState.Playing) {
            LiveBadge(modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
        }

        // ── Bottom-right: fullscreen button when playing ──────────────────
        if (playerState is PlayerState.Playing) {
            IconButton(
                onClick  = onFullscreenClick,
                modifier = Modifier.align(Alignment.BottomEnd).size(36.dp).padding(4.dp)
            ) {
                Icon(
                    Icons.Default.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint     = TextPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LiveBadge — pulsing LIVE indicator
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LiveBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(RecordingRed.copy(alpha = 0.88f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment       = Alignment.CenterVertically,
        horizontalArrangement   = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier.size(5.dp).clip(CircleShape).background(Color.White)
        )
        Text("LIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.8.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EmptyStateView
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier              = modifier.padding(40.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center
    ) {
        Box(
            modifier          = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(SurfaceElevated),
            contentAlignment  = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TextDisabled, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PrimaryButton
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) com.sentinel.app.ui.theme.CyanPrimary else com.sentinel.app.ui.theme.CyanPrimary.copy(alpha = 0.3f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Color.Black, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = Color.Black, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GhostButton
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, com.sentinel.app.ui.theme.CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, null, tint = com.sentinel.app.ui.theme.CyanPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = com.sentinel.app.ui.theme.CyanPrimary, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// InfoRow
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier              = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
