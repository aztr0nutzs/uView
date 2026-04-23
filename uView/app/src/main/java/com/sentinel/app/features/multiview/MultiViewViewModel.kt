package com.sentinel.app.features.multiview

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.sentinel.app.data.playback.PlaybackManager
import com.sentinel.app.core.power.PowerAwarenessManager
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraEvent
import com.sentinel.app.domain.repository.CameraEventRepository
import com.sentinel.app.domain.model.PlayerState
import com.sentinel.app.domain.repository.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.sentinel.app.data.events.EventPipeline
import com.sentinel.app.data.motion.MotionMonitorService
import com.sentinel.app.domain.model.MotionSensitivityConfig
import javax.inject.Inject

enum class GridLayout(val label: String, val columns: Int) {
    TWO_BY_TWO("2×2", 2),
    TWO_BY_THREE("2×3", 2),
    SINGLE("Single", 1),
    THREE_BY_THREE("3×3", 3)
}

data class MultiViewUiState(
    val cameras: List<CameraDevice> = emptyList(),
    val selectedCameraId: String? = null,
    val gridLayout: GridLayout = GridLayout.TWO_BY_TWO,
    val isMuted: Boolean = false,
    val isLoading: Boolean = true,
    val recentEvents: List<CameraEvent> = emptyList(),
    val powerState: PowerAwarenessManager.PowerState? = null
)

@OptIn(UnstableApi::class)
@HiltViewModel
class MultiViewViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val eventRepository: CameraEventRepository,
    private val playbackManager: PlaybackManager,
    private val motionMonitorService: MotionMonitorService,
    private val eventPipeline: EventPipeline,
    private val powerAwarenessManager: PowerAwarenessManager
) : ViewModel() {

    private val _uiExtra = MutableStateFlow(MultiViewUiState())

    val uiState: StateFlow<MultiViewUiState> = combine(
        cameraRepository.observeAllCameras(),
        eventRepository.observeEvents(limit = 5),
        powerAwarenessManager.powerState,
        _uiExtra
    ) { cameras, events, power, extra ->
        val enabled = cameras.filter { it.isEnabled }
        extra.copy(cameras = enabled, isLoading = false,
            recentEvents = events, powerState = power)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MultiViewUiState())

    // Start playback for all enabled cameras
    init {
        viewModelScope.launch {
            uiState.collect { state ->
                if (!state.isLoading && state.cameras.isNotEmpty()) {
                    state.cameras.forEach { camera ->
                        val current = playbackManager.observeState(camera.id)
                        // Only start if idle — don't restart already-playing streams
                        if (playbackManager.getExoPlayer(camera.id) == null &&
                            playbackManager.getMjpegFrames(camera.id) == null) {
                            viewModelScope.launch {
                            playbackManager.startCamera(camera)
                            motionMonitorService.startMonitoring(camera, MotionSensitivityConfig.MEDIUM)
                        }
                        }
                    }
                    return@collect
                }
            }
        }
    }

    // ── Player state per camera (observed by screen) ──────────────────────

    fun observePlayerState(cameraId: String): Flow<PlayerState> =
        playbackManager.observeState(cameraId)

    fun getExoPlayer(cameraId: String): ExoPlayer? =
        playbackManager.getExoPlayer(cameraId)

    fun getMjpegFrames(cameraId: String): Flow<Bitmap>? =
        playbackManager.getMjpegFrames(cameraId)

    fun usesExoPlayer(camera: CameraDevice): Boolean =
        playbackManager.usesExoPlayer(camera)

    // ── Actions ───────────────────────────────────────────────────────────

    fun selectCamera(id: String?) = _uiExtra.update { it.copy(selectedCameraId = id) }

    fun setLayout(layout: GridLayout) = _uiExtra.update { it.copy(gridLayout = layout) }

    fun toggleMute() {
        val newMuted = !_uiExtra.value.isMuted
        _uiExtra.update { it.copy(isMuted = newMuted) }
        uiState.value.cameras.forEach { cam ->
            if (newMuted) {
                playbackManager.setVolume(cam.id, 0f)
            } else {
                playbackManager.setVolume(cam.id, 1f)
            }
        }
    }

    fun reconnect(cameraId: String) = viewModelScope.launch {
        playbackManager.reconnect(cameraId)
    }

    fun refreshAll() = viewModelScope.launch {
        uiState.value.cameras.forEach { cam ->
            playbackManager.reconnect(cam.id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Streams are kept alive intentionally when navigating away from multi-view
        // (user may return quickly). The foreground service (Phase 6) will manage
        // long-running sessions. For now, do not stop here.
    }

    fun stopAll() = viewModelScope.launch {
        playbackManager.stopAll()
        uiState.value.cameras.forEach { cam ->
            motionMonitorService.stopMonitoring(cam.id)
        }
    }
}
