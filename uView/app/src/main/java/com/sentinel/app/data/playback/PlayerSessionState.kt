package com.sentinel.app.data.playback

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.sentinel.app.domain.model.CameraStreamEndpoint
import com.sentinel.app.domain.model.PlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PlayerSessionState
 *
 * Holds the mutable runtime state for a single camera's playback session.
 * One instance lives per active camera inside [CameraPlaybackServiceImpl].
 *
 * Lifecycle: Created when play() is called. Released when stop() is called or
 * the service shuts down via releaseAll().
 */
@UnstableApi
internal class PlayerSessionState(
    val cameraId: String,
    val endpoint: CameraStreamEndpoint
) {
    // The ExoPlayer instance — null for MJPEG cameras (which use frameJob instead)
    var exoPlayer: ExoPlayer? = null

    // Running coroutine for MJPEG frame pull + reconnect loop
    var streamJob: Job? = null

    // Reconnect scheduler — fresh instance per session so attempts reset on stop/restart
    var reconnectScheduler: ReconnectScheduler = ReconnectScheduler(DefaultReconnectPolicy)

    // Whether audio is muted for this session
    var isMuted: Boolean = false

    // Published player state — observed by ViewModels
    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Loading)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun setState(state: PlayerState) {
        _playerState.value = state
    }

    /**
     * Release ExoPlayer resources. Safe to call multiple times.
     */
    fun releasePlayer() {
        exoPlayer?.let { player ->
            player.stop()
            player.release()
        }
        exoPlayer = null
    }

    /**
     * Cancel the stream/reconnect coroutine. Safe to call multiple times.
     */
    fun cancelStreamJob() {
        streamJob?.cancel()
        streamJob = null
    }

    /**
     * Full teardown — cancel job + release player.
     */
    fun release() {
        cancelStreamJob()
        releasePlayer()
        _playerState.value = PlayerState.Idle
    }
}
