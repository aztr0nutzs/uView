package com.sentinel.app.ui.components

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.sentinel.app.domain.model.PlayerState
import com.sentinel.app.ui.theme.CyanPrimary
import com.sentinel.app.ui.theme.SurfaceBase
import com.sentinel.app.ui.theme.TextDisabled
import com.sentinel.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * LiveStreamSurface
 *
 * The top-level live feed composable. Routes to the correct renderer:
 *
 *   - [ExoPlayerSurface]   for RTSP / HLS / generic HTTP streams
 *   - [MjpegSurface]       for MJPEG / multipart cameras (IP Webcam, DroidCam)
 *   - [StreamIdleSurface]  when no session is active
 *
 * ViewModels pass the ExoPlayer or MJPEG frame flow here — this composable
 * doesn't create players, it only renders them.
 *
 * @param exoPlayer     Non-null when this camera uses ExoPlayer.
 * @param mjpegFrames   Non-null when this camera uses MJPEG rendering.
 * @param playerState   Current state — drives loading/error overlays.
 * @param showControls  Whether to show ExoPlayer's built-in transport controls.
 */
@OptIn(UnstableApi::class)
@Composable
fun LiveStreamSurface(
    playerState: PlayerState,
    exoPlayer: ExoPlayer?,
    mjpegFrames: Flow<Bitmap>?,
    modifier: Modifier = Modifier,
    showControls: Boolean = false
) {
    Box(modifier = modifier.background(Color.Black)) {
        when {
            exoPlayer != null -> {
                ExoPlayerSurface(
                    player       = exoPlayer,
                    showControls = showControls,
                    modifier     = Modifier.fillMaxSize()
                )
            }
            mjpegFrames != null -> {
                MjpegSurface(
                    frames   = mjpegFrames,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                StreamIdleSurface(
                    state    = playerState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Overlay states — shown on top of any renderer
        when (playerState) {
            is PlayerState.Loading      -> StreamLoadingOverlay()
            is PlayerState.Buffering    -> StreamLoadingOverlay()
            is PlayerState.Reconnecting -> StreamReconnectingOverlay(playerState)
            is PlayerState.Error        -> {} // Error is handled at tile level with retry button
            else                        -> {}
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ExoPlayerSurface
// Binds an ExoPlayer to a Media3 PlayerView inside an AndroidView.
// The PlayerView is created once and reused across recompositions.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerSurface(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
    showControls: Boolean = false
) {
    val context = LocalContext.current

    AndroidView(
        factory = {
            PlayerView(context).apply {
                this.player           = player
                useController         = showControls
                resizeMode            = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setBackgroundColor(android.graphics.Color.BLACK)
                setShutterBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        update = { view ->
            view.player       = player
            view.useController = showControls
        },
        onRelease = { view ->
            // Detach player from view when composable leaves — the player
            // itself is managed by PlaybackManager, not released here.
            view.player = null
        },
        modifier = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// MjpegSurface
// Collects bitmap frames from the MJPEG session and paints them.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MjpegSurface(
    frames: Flow<Bitmap>,
    modifier: Modifier = Modifier
) {
    val bitmap by frames.collectAsState(initial = null)

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { bmp ->
            Image(
                bitmap             = bmp.asImageBitmap(),
                contentDescription = "Live MJPEG feed",
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.fillMaxSize()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StreamIdleSurface — shown when no player is attached
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StreamIdleSurface(state: PlayerState, modifier: Modifier = Modifier) {
    Box(
        modifier          = modifier.background(SurfaceBase),
        contentAlignment  = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = Icons.Default.VideocamOff,
                contentDescription = null,
                tint               = TextDisabled,
                modifier           = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = when (state) {
                    is PlayerState.Idle    -> "Stream not started"
                    is PlayerState.Paused  -> "Paused"
                    is PlayerState.StreamUnsupported -> "Unsupported stream type"
                    else                   -> "No stream"
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextDisabled
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading / Reconnecting overlays — drawn on top of the renderer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StreamLoadingOverlay() {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color       = CyanPrimary,
            strokeWidth = 2.5.dp,
            modifier    = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun StreamReconnectingOverlay(state: PlayerState.Reconnecting) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color       = com.sentinel.app.ui.theme.WarningAmber,
                strokeWidth = 2.5.dp,
                modifier    = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Reconnecting…",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}
