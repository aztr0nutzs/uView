package com.sentinel.app.features.cameras.detail

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.sentinel.app.data.events.EventPipeline
import com.sentinel.app.data.recording.LocalRecordingController
import com.sentinel.app.domain.model.RecordingState
import com.sentinel.app.domain.service.RecordingController
import com.sentinel.app.data.motion.MotionMonitorService
import com.sentinel.app.data.playback.PlaybackManager
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraEvent
import com.sentinel.app.domain.model.ConnectionTestResult
import com.sentinel.app.domain.model.MotionDetectorState
import com.sentinel.app.domain.model.MotionSensitivityConfig
import com.sentinel.app.domain.model.PlayerState
import com.sentinel.app.domain.repository.CameraEventRepository
import com.sentinel.app.domain.repository.CameraRepository
import com.sentinel.app.domain.service.CameraConnectionTester
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraDetailUiState(
    val camera: CameraDevice? = null,
    val events: List<CameraEvent> = emptyList(),
    val lastTestResult: ConnectionTestResult? = null,
    val isTestingConnection: Boolean = false,
    val isTakingSnapshot: Boolean = false,
    val isLoading: Boolean = true,
    val selectedTab: DetailTab = DetailTab.LIVE,
    val motionDetectorState: MotionDetectorState = MotionDetectorState.Idle,
    val recordingState: RecordingState = RecordingState.IDLE
)

enum class DetailTab(val label: String) {
    LIVE("Live"), DETAILS("Details"), EVENTS("Events"), DIAGNOSTICS("Diagnostics")
}

@OptIn(UnstableApi::class)
@HiltViewModel
class CameraDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cameraRepository: CameraRepository,
    private val eventRepository: CameraEventRepository,
    private val connectionTester: CameraConnectionTester,
    private val playbackManager: PlaybackManager,
    private val motionMonitorService: MotionMonitorService,
    private val eventPipeline: EventPipeline,
    private val recordingController: RecordingController
) : ViewModel() {

    private val cameraId: String = checkNotNull(savedStateHandle["cameraId"])
    private val _extraState = MutableStateFlow(CameraDetailUiState(isLoading = true))

    val uiState: StateFlow<CameraDetailUiState> = combine(
        cameraRepository.observeAllCameras(),
        eventRepository.observeEventsForCamera(cameraId, 20),
        motionMonitorService.observeState(cameraId),
        _extraState
    ) { cameras, events, motionState, extra ->
        val camera = cameras.firstOrNull { it.id == cameraId }
        extra.copy(
            camera = camera,
            events = events,
            isLoading = camera == null,
            motionDetectorState = motionState
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CameraDetailUiState())

    val recordingState: StateFlow<RecordingState> =
        recordingController.observeRecordingState(cameraId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordingState.IDLE)

    val playerState: StateFlow<PlayerState> =
        playbackManager.observeState(cameraId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerState.Idle)

    init {
        viewModelScope.launch {
            val camera = uiState.first { it.camera != null }.camera ?: return@launch
            playbackManager.startCamera(camera)
            // Start motion monitoring when this detail screen opens
            motionMonitorService.startMonitoring(camera, MotionSensitivityConfig.MEDIUM)
        }
    }

    fun getExoPlayer(): ExoPlayer? = playbackManager.getExoPlayer(cameraId)
    fun getMjpegFrames(): Flow<Bitmap>? = playbackManager.getMjpegFrames(cameraId)

    fun selectTab(tab: DetailTab) = _extraState.update { it.copy(selectedTab = tab) }

    fun reconnect() = viewModelScope.launch { playbackManager.reconnect(cameraId) }

    fun toggleMute(): Boolean = playbackManager.toggleMute(cameraId)

    fun toggleFavorite() = viewModelScope.launch { cameraRepository.toggleFavorite(cameraId) }

    fun testConnection() = viewModelScope.launch {
        val camera = uiState.value.camera ?: return@launch
        _extraState.update { it.copy(isTestingConnection = true) }
        val result = connectionTester.testConnection(camera)
        _extraState.update { it.copy(isTestingConnection = false, lastTestResult = result) }
    }

    fun takeSnapshot() = viewModelScope.launch {
        val camera = uiState.value.camera ?: return@launch
        _extraState.update { it.copy(isTakingSnapshot = true) }
        // Phase 7: real snapshot via SnapshotControllerImpl
        kotlinx.coroutines.delay(300)
        _extraState.update { it.copy(isTakingSnapshot = false) }
        // Log the event via pipeline
        eventPipeline.recordSnapshotTaken(camera, savedPath = null)
    }

    /** Submit an ExoPlayer video frame for motion analysis. */
    fun submitFrameForMotion(bitmap: Bitmap) {
        motionMonitorService.submitExoFrame(cameraId, bitmap)
    }

    fun toggleRecording() = viewModelScope.launch {
        val camera = uiState.value.camera ?: return@launch
        if (recordingState.value == RecordingState.RECORDING) {
            recordingController.stopRecording(cameraId)
            eventPipeline.recordRecordingStopped(camera, durationSeconds = 0)
        } else {
            recordingController.startRecording(cameraId)
            eventPipeline.recordRecordingStarted(camera)
        }
    }

    fun stopPlayback() = viewModelScope.launch {
        playbackManager.stopCamera(cameraId)
        motionMonitorService.stopMonitoring(cameraId)
    }

    override fun onCleared() {
        super.onCleared()
    }
}
