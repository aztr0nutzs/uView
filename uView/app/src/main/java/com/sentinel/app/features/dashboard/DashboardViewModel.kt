package com.sentinel.app.features.dashboard

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.sentinel.app.core.power.PowerAwarenessManager
import com.sentinel.app.core.service.MonitorServiceController
import com.sentinel.app.data.playback.PlaybackManager
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraEvent
import com.sentinel.app.domain.model.DashboardSummary
import com.sentinel.app.domain.model.PlayerState
import com.sentinel.app.domain.model.StreamQualityProfile
import com.sentinel.app.domain.repository.CameraEventRepository
import com.sentinel.app.domain.repository.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val summary: DashboardSummary? = null,
    val pinnedCameras: List<CameraDevice> = emptyList(),
    val recentEvents: List<CameraEvent> = emptyList(),
    val powerState: PowerAwarenessManager.PowerState? = null,
    val isMonitoringActive: Boolean = false,
    val isLoading: Boolean = true
)

@OptIn(UnstableApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val eventRepository: CameraEventRepository,
    private val playbackManager: PlaybackManager,
    private val powerAwarenessManager: PowerAwarenessManager,
    private val monitorServiceController: MonitorServiceController
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        cameraRepository.observeDashboardSummary(),
        cameraRepository.observeAllCameras(),
        eventRepository.observeEvents(limit = 5),
        powerAwarenessManager.powerState
    ) { summary, cameras, events, power ->
        DashboardUiState(
            summary           = summary.copy(recentEventCount = events.size),
            pinnedCameras     = cameras.filter { it.isPinned || it.isFavorite }.take(4),
            recentEvents      = events,
            powerState        = power,
            isMonitoringActive = monitorServiceController.isRunning,
            isLoading         = false
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun observePlayerState(cameraId: String): Flow<PlayerState> =
        playbackManager.observeState(cameraId)

    fun getExoPlayer(cameraId: String) =
        playbackManager.getExoPlayer(cameraId)

    fun getMjpegFrames(cameraId: String) =
        playbackManager.getMjpegFrames(cameraId)
}
