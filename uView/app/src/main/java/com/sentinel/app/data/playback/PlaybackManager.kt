package com.sentinel.app.data.playback

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraSourceType
import com.sentinel.app.domain.model.PlayerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PlaybackManager
 *
 * Thin ViewModel-facing facade that wraps [CameraPlaybackServiceImpl] and
 * [StreamUrlResolver].
 *
 * Screens and ViewModels call this instead of the service directly so they
 * don't need to resolve URLs themselves or know whether a camera is MJPEG or
 * ExoPlayer.
 *
 * Typical ViewModel usage:
 *
 *   playbackManager.startCamera(camera)
 *   playbackManager.observeState(camera.id).collectAsState()
 *   playbackManager.getExoPlayer(camera.id)  // null for MJPEG
 *   playbackManager.getMjpegFrames(camera.id) // null for ExoPlayer
 *   playbackManager.stopCamera(camera.id)
 */
@OptIn(UnstableApi::class)
@Singleton
class PlaybackManager @Inject constructor(
    private val playbackService: CameraPlaybackServiceImpl,
    private val streamUrlResolver: StreamUrlResolver,
    private val mjpegSessionRegistry: MjpegSessionRegistry
) {
    /**
     * Resolve the stream URL for [camera] and begin playback.
     * Emits [PlayerState.Loading] immediately, then transitions based on
     * actual connection result.
     */
    suspend fun startCamera(camera: CameraDevice) {
        if (streamUrlResolver.isUnsupportedSource(camera)) {
            Timber.w("startCamera: ${camera.id} is an unsupported source (${camera.sourceType})")
            playbackService.emitUnsupported(camera.id)
            return
        }

        val endpoint = streamUrlResolver.resolve(camera)
        if (endpoint == null) {
            Timber.e("startCamera: could not resolve endpoint for ${camera.id}")
            playbackService.emitError(camera.id, "Stream source is not available in this build")
            return
        }
        playbackService.play(camera.id, endpoint)
    }

    /** Stop and release playback for a single camera. */
    suspend fun stopCamera(cameraId: String) {
        playbackService.stop(cameraId)
    }

    /** Stop all active sessions — call when the screen leaves composition. */
    suspend fun stopAll() {
        playbackService.releaseAll()
    }

    /** Reconnect a specific camera. */
    suspend fun reconnect(cameraId: String) {
        playbackService.reconnect(cameraId)
    }

    /** Observe the [PlayerState] for [cameraId]. */
    fun observeState(cameraId: String): Flow<PlayerState> =
        playbackService.observePlayerState(cameraId)

    /**
     * Returns the raw ExoPlayer for [cameraId] so [StreamView] can bind a PlayerView.
     * Returns null if this camera uses MJPEG rendering instead.
     */
    fun getExoPlayer(cameraId: String): androidx.media3.exoplayer.ExoPlayer? =
        playbackService.getPlayer(cameraId)

    /**
     * Returns the MJPEG bitmap frame flow for [cameraId].
     * Returns null if this camera uses ExoPlayer rendering.
     */
    fun getMjpegFrames(cameraId: String): kotlinx.coroutines.flow.Flow<android.graphics.Bitmap>? {
        // Only expose MJPEG frames if the camera's session is MJPEG-backed.
        // The registry holds frames only when an MJPEG session is active.
        return mjpegSessionRegistry.frames(cameraId)
    }

    /** Toggle mute state. Returns new muted state. */
    fun toggleMute(cameraId: String): Boolean =
        playbackService.toggleMute(cameraId)

    /** Set volume (0.0–1.0). */
    fun setVolume(cameraId: String, volume: Float) {
        playbackService.setVolume(cameraId, volume)
    }

    /**
     * Whether [camera] uses ExoPlayer (true) or MJPEG composable (false).
     * Drives which renderer the UI attaches.
     */
    fun usesExoPlayer(camera: CameraDevice): Boolean =
        camera.sourceType != CameraSourceType.MJPEG &&
        camera.sourceType != CameraSourceType.ANDROID_IPWEBCAM &&
        camera.sourceType != CameraSourceType.ANDROID_DROIDCAM

    /**
     * Expose the raw [CameraPlaybackServiceImpl] for advanced callers that
     * need direct access (e.g. the foreground service in Phase 6).
     */
    fun rawService(): CameraPlaybackServiceImpl = playbackService
}
