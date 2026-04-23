package com.sentinel.app.data.playback

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.sentinel.app.data.playback.mjpeg.MjpegFrame
import com.sentinel.app.data.playback.mjpeg.MjpegFrameSource
import com.sentinel.app.domain.model.CameraStreamEndpoint
import com.sentinel.app.domain.model.PlayerState
import com.sentinel.app.domain.service.CameraPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CameraPlaybackServiceImpl
 *
 * Manages the full lifecycle of camera stream playback for every active camera.
 *
 * ── ExoPlayer cameras (RTSP / HLS / generic HTTP) ───────────────────────────
 *   1. [StreamUrlResolver] resolves the endpoint URL.
 *   2. [ExoPlayerFactory] creates a configured ExoPlayer.
 *   3. A [Player.Listener] monitors playback state and feeds [PlayerState].
 *   4. On error, [ReconnectScheduler] drives exponential-backoff retries.
 *
 * ── MJPEG cameras (multipart/x-mixed-replace) ───────────────────────────────
 *   1. [StreamUrlResolver] resolves the endpoint URL.
 *   2. [MjpegFrameSource.frames] opens the HTTP connection and emits Bitmaps.
 *   3. Frames are routed to [MjpegStreamView] via [MjpegSessionRegistry].
 *   4. On error, the same reconnect loop applies.
 *
 * Sessions are keyed by cameraId. Multiple cameras can play simultaneously.
 * All sessions are released when [releaseAll] is called (e.g. app backgrounded).
 *
 * Threading:
 *   - ExoPlayer must be created and interacted with on the main thread.
 *   - MJPEG frame pulling runs on Dispatchers.IO inside the session coroutine.
 *   - StateFlow updates are safe to observe from any thread.
 */
@UnstableApi
@Singleton
class CameraPlaybackServiceImpl @Inject constructor(
    private val urlResolver: StreamUrlResolver,
    private val playerFactory: ExoPlayerFactory,
    private val mjpegFrameSource: MjpegFrameSource,
    private val mjpegSessionRegistry: MjpegSessionRegistry
) : CameraPlaybackService {

    // Supervisor scope: individual session failures don't kill sibling sessions
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Active sessions keyed by cameraId
    private val sessions = ConcurrentHashMap<String, PlayerSessionState>()

    // ─────────────────────────────────────────────────────────────────────────
    // CameraPlaybackService API
    // ─────────────────────────────────────────────────────────────────────────

    override fun observePlayerState(cameraId: String): Flow<PlayerState> =
        sessions[cameraId]?.playerState ?: flowOf(PlayerState.Idle)

    override suspend fun play(cameraId: String, endpoint: CameraStreamEndpoint) {
        // Stop any existing session first
        stop(cameraId)

        Timber.d("play: cameraId=$cameraId url=${endpoint.url}")
        val session = PlayerSessionState(cameraId, endpoint)
        sessions[cameraId] = session
        session.setState(PlayerState.Loading)

        if (urlResolver.requiresMjpegRenderer(endpoint)) {
            startMjpegSession(session)
        } else {
            startExoPlayerSession(session)
        }
    }

    override suspend fun pause(cameraId: String) {
        withContext(Dispatchers.Main) {
            sessions[cameraId]?.let { session ->
                session.exoPlayer?.pause()
                session.setState(PlayerState.Paused)
            }
        }
    }

    override suspend fun stop(cameraId: String) {
        withContext(Dispatchers.Main) {
            sessions.remove(cameraId)?.release()
            mjpegSessionRegistry.remove(cameraId)
            Timber.d("stop: released session for cameraId=$cameraId")
        }
    }

    override suspend fun reconnect(cameraId: String) {
        val session = sessions[cameraId] ?: return
        Timber.d("reconnect: cameraId=$cameraId")
        session.setState(PlayerState.Reconnecting)
        session.reconnectScheduler.reset()
        play(cameraId, session.endpoint)
    }

    override suspend fun releaseAll() {
        withContext(Dispatchers.Main) {
            sessions.keys.toList().forEach { id ->
                sessions.remove(id)?.release()
                mjpegSessionRegistry.remove(id)
            }
            Timber.d("releaseAll: all sessions released")
        }
    }

    override fun setVolume(cameraId: String, volume: Float) {
        sessions[cameraId]?.exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }

    override fun toggleMute(cameraId: String): Boolean {
        val session = sessions[cameraId] ?: return false
        session.isMuted = !session.isMuted
        session.exoPlayer?.volume = if (session.isMuted) 0f else 1f
        return session.isMuted
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ExoPlayer session
    // ─────────────────────────────────────────────────────────────────────────

    private fun startExoPlayerSession(session: PlayerSessionState) {
        serviceScope.launch(Dispatchers.Main) {
            launchExoPlayerWithReconnect(session)
        }
    }

    private suspend fun launchExoPlayerWithReconnect(session: PlayerSessionState) {
        val scheduler = session.reconnectScheduler

        while (scheduler.shouldRetry() && sessions.containsKey(session.cameraId)) {
            // Release any previous player from a prior attempt
            session.releasePlayer()

            val player = playerFactory.create(session.endpoint)
            if (player == null) {
                // Shouldn't happen for non-MJPEG paths, but guard anyway
                session.setState(PlayerState.Error("Could not create player"))
                return
            }

            session.exoPlayer = player
            player.volume = if (session.isMuted) 0f else 1f

            // Attach listener that drives PlayerState transitions
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (!sessions.containsKey(session.cameraId)) return
                    when (playbackState) {
                        Player.STATE_BUFFERING -> session.setState(PlayerState.Buffering(0))
                        Player.STATE_READY     -> {
                            scheduler.reset()
                            session.setState(PlayerState.Playing)
                        }
                        Player.STATE_ENDED     -> session.setState(PlayerState.Idle)
                        Player.STATE_IDLE      -> { /* handled by onPlayerError */ }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (!sessions.containsKey(session.cameraId)) return
                    Timber.w("ExoPlayer error on ${session.cameraId}: ${error.message}")
                    // Trigger reconnect by setting Reconnecting state — the loop
                    // below will handle the backoff
                    session.setState(PlayerState.Reconnecting)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (!sessions.containsKey(session.cameraId)) return
                    if (isPlaying) session.setState(PlayerState.Playing)
                }
            })

            player.prepare()

            // Wait for the player to either succeed or error
            // We do this by polling the state — a listener-based approach
            // would need channels, which adds complexity without benefit here.
            var waited = 0
            val maxWaitMs = 12_000
            val pollMs    = 200L
            while (waited < maxWaitMs && sessions.containsKey(session.cameraId)) {
                val state = session.playerState.value
                if (state is PlayerState.Playing) {
                    Timber.d("ExoPlayer playing: cameraId=${session.cameraId}")
                    return   // success — exit reconnect loop
                }
                if (state is PlayerState.Error || state is PlayerState.Reconnecting) {
                    break    // error — fall through to retry
                }
                kotlinx.coroutines.delay(pollMs)
                waited += pollMs.toInt()
            }

            // If we're still here, connection failed this attempt
            if (!sessions.containsKey(session.cameraId)) return   // session was stopped

            val currentState = session.playerState.value
            if (currentState is PlayerState.Playing) return       // raced to success

            if (!scheduler.shouldRetry()) {
                session.setState(
                    PlayerState.Error("Could not connect after ${scheduler.currentAttempt} attempts")
                )
                return
            }

            session.setState(PlayerState.Reconnecting)
            Timber.d("ExoPlayer retry ${scheduler.currentAttempt + 1} for ${session.cameraId}")
            scheduler.waitBeforeNextAttempt()
        }

        if (sessions.containsKey(session.cameraId) &&
            session.playerState.value !is PlayerState.Playing) {
            session.setState(PlayerState.Error("Stream unreachable — retries exhausted"))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MJPEG session
    // ─────────────────────────────────────────────────────────────────────────

    private fun startMjpegSession(session: PlayerSessionState) {
        // MJPEG runs entirely on IO, supervised by the service scope
        val job = serviceScope.launch(Dispatchers.IO) {
            launchMjpegWithReconnect(session)
        }
        session.streamJob = job
    }

    private suspend fun launchMjpegWithReconnect(session: PlayerSessionState) {
        val scheduler = session.reconnectScheduler
        val endpoint  = session.endpoint

        while (scheduler.shouldRetry() && sessions.containsKey(session.cameraId)) {
            session.setState(PlayerState.Loading)

            var receivedFirstFrame = false

            mjpegFrameSource.frames(
                url      = endpoint.url,
                username = "",   // TODO: pass credentials from CameraDevice
                password = ""
            ).collect { frame ->
                if (!sessions.containsKey(session.cameraId)) {
                    return@collect   // session was stopped
                }
                when (frame) {
                    is MjpegFrame.Frame -> {
                        if (!receivedFirstFrame) {
                            receivedFirstFrame = true
                            scheduler.reset()
                            session.setState(PlayerState.Playing)
                        }
                        mjpegSessionRegistry.postFrame(session.cameraId, frame.bitmap)
                    }
                    is MjpegFrame.Error -> {
                        Timber.w("MJPEG error on ${session.cameraId}: ${frame.message}")
                        session.setState(PlayerState.Reconnecting)
                    }
                }
            }

            // Flow completed (stream closed) — check if we should retry
            if (!sessions.containsKey(session.cameraId)) return

            if (!scheduler.shouldRetry()) {
                session.setState(PlayerState.Error("MJPEG stream ended — retries exhausted"))
                return
            }

            session.setState(PlayerState.Reconnecting)
            Timber.d("MJPEG retry ${scheduler.currentAttempt + 1} for ${session.cameraId}")
            scheduler.waitBeforeNextAttempt()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers used by ViewModels
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the active [ExoPlayer] for [cameraId], or null if the session
     * is MJPEG-based or not yet started.
     */
    fun getExoPlayer(cameraId: String): androidx.media3.exoplayer.ExoPlayer? =
        sessions[cameraId]?.exoPlayer

    /**
     * Returns a snapshot of the current [PlayerState] without subscribing.
     * Used by [MultiViewViewModel] to populate the grid state map.
     */
    fun getPlayerStateSnapshot(cameraId: String): PlayerState =
        sessions[cameraId]?.playerState?.value ?: PlayerState.Idle

    /**
     * Emit an error state for a camera that could not be started —
     * used when URL resolution fails or the source is unsupported.
     */
    fun emitError(cameraId: String, message: String) {
        val session = sessions.getOrPut(cameraId) {
            PlayerSessionState(cameraId, com.sentinel.app.domain.model.CameraStreamEndpoint(
                url = "", sourceType = com.sentinel.app.domain.model.CameraSourceType.GENERIC_URL
            ))
        }
        session.setState(PlayerState.Error(message))
    }

    /**
     * Emit [PlayerState.StreamUnsupported] for sources we explicitly cannot handle
     * (e.g. Alfred in LAN-only mode).
     */
    fun emitUnsupported(cameraId: String) {
        val session = sessions.getOrPut(cameraId) {
            PlayerSessionState(cameraId, com.sentinel.app.domain.model.CameraStreamEndpoint(
                url = "", sourceType = com.sentinel.app.domain.model.CameraSourceType.GENERIC_URL
            ))
        }
        session.setState(PlayerState.StreamUnsupported)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Accessors called by PlaybackManager
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the live [ExoPlayer] for [cameraId], or null if:
     *   - the session doesn't exist
     *   - the session uses MJPEG rendering instead of ExoPlayer
     */
    fun getPlayer(cameraId: String): androidx.media3.exoplayer.ExoPlayer? =
        sessions[cameraId]?.exoPlayer

    /**
     * Returns the MJPEG frame [Flow<Bitmap>] for [cameraId] via the registry.
     * The registry always returns a flow — callers decide whether to observe it
     * based on whether the session is MJPEG-backed.
     */
    fun getMjpegFrames(cameraId: String): kotlinx.coroutines.flow.Flow<android.graphics.Bitmap> =
        mjpegSessionRegistry.frames(cameraId)
}
