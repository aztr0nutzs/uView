package com.sentinel.app.data.playback.mjpeg

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.sentinel.app.data.playback.MjpegSessionRegistry
import kotlinx.coroutines.flow.Flow

/**
 * MjpegStreamView
 *
 * Renders a live MJPEG stream by collecting [Bitmap] frames emitted by
 * [MjpegSessionRegistry] and painting them directly to a Compose [Image].
 *
 * This is intentionally lightweight — no AndroidView, no SurfaceHolder,
 * just Compose state updated on every decoded frame.
 *
 * Frame rate: limited by the source camera (typically 5–30 fps for MJPEG).
 * Frames are rendered on the main thread via Compose recomposition.
 * The decoder runs on Dispatchers.IO inside [MjpegFrameSource].
 *
 * Usage:
 *   MjpegStreamView(
 *       frames   = mjpegSessionRegistry.frames(cameraId),
 *       modifier = Modifier.fillMaxSize()
 *   )
 */
@Composable
fun MjpegStreamView(
    frames: Flow<Bitmap>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val bitmap by frames.collectAsState(initial = null)

    Box(
        modifier = modifier.background(Color.Black)
    ) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Live camera feed",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
