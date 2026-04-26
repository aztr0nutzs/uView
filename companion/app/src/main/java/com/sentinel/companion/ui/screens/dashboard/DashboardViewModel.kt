package com.sentinel.companion.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.companion.data.model.Alert
import com.sentinel.companion.data.model.Camera
import com.sentinel.companion.data.model.CameraStatus
import com.sentinel.companion.data.model.SystemStatus
import com.sentinel.companion.data.repository.CameraRepository
import com.sentinel.companion.data.sync.SyncPhase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val systemStatus: SystemStatus = SystemStatus(),
    val pinnedCameras: List<Camera> = emptyList(),
    val recentAlerts: List<Alert> = emptyList(),
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isEmpty: Boolean = false,
    val syncError: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: CameraRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repo.cameras,
                repo.alerts,
                repo.systemStatus,
                repo.syncState,
            ) { cameras, alerts, status, sync ->
                DashboardUiState(
                    systemStatus  = status,
                    pinnedCameras = cameras.filter { it.isFavorite || it.statusEnum() == CameraStatus.ONLINE }.take(4),
                    recentAlerts  = alerts.take(5),
                    isLoading     = false,
                    isSyncing     = sync.phase == SyncPhase.RUNNING,
                    isEmpty       = cameras.isEmpty(),
                    syncError     = sync.lastOutcome?.takeIf { !it.ok }?.error,
                )
            }.collect { _uiState.value = it }
        }
        // Kick off a real sync as soon as the dashboard appears so first paint
        // reflects observed reality, not just the cached DB state.
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { repo.refresh() }
    }

    fun reconnect(cameraId: String) {
        viewModelScope.launch { repo.reconnectCamera(cameraId) }
    }
}
