package com.sentinel.companion.ui.screens.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.companion.data.model.Alert
import com.sentinel.companion.data.model.AlertType
import com.sentinel.companion.data.repository.CameraRepository
import com.sentinel.companion.data.sync.SyncPhase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val allAlerts: List<Alert> = emptyList(),
    val filteredAlerts: List<Alert> = emptyList(),
    val selectedType: String = "ALL",
    val selectedCamera: String = "ALL",
    val cameraNames: List<String> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isEmpty: Boolean = false,
    val syncError: String? = null,
    val lastSyncMs: Long = 0L,
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val repo: CameraRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repo.alerts, repo.syncState) { alerts, sync ->
                alerts to sync
            }.collect { (alerts, sync) ->
                val cameraNames = listOf("ALL") + alerts.map { it.cameraName }.distinct().sorted()
                val filtered = applyFilter(alerts, _uiState.value.selectedType, _uiState.value.selectedCamera)
                _uiState.value = _uiState.value.copy(
                    allAlerts      = alerts,
                    filteredAlerts = filtered,
                    cameraNames    = cameraNames,
                    unreadCount    = alerts.count { !it.isRead },
                    isLoading      = false,
                    isSyncing      = sync.phase == SyncPhase.RUNNING,
                    isEmpty        = alerts.isEmpty(),
                    syncError      = sync.lastOutcome?.takeIf { !it.ok }?.error,
                    lastSyncMs     = sync.lastOutcome?.finishedAtMs ?: 0L,
                )
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { repo.refresh() }
    }

    fun onTypeSelected(type: String) {
        val filtered = applyFilter(_uiState.value.allAlerts, type, _uiState.value.selectedCamera)
        _uiState.value = _uiState.value.copy(selectedType = type, filteredAlerts = filtered)
    }

    fun onCameraSelected(camera: String) {
        val filtered = applyFilter(_uiState.value.allAlerts, _uiState.value.selectedType, camera)
        _uiState.value = _uiState.value.copy(selectedCamera = camera, filteredAlerts = filtered)
    }

    fun markRead(alertId: String) {
        viewModelScope.launch { repo.markAlertRead(alertId) }
    }

    fun markAllRead() {
        viewModelScope.launch { repo.markAllAlertsRead() }
    }

    private fun applyFilter(alerts: List<Alert>, type: String, camera: String): List<Alert> {
        var list = alerts.sortedByDescending { it.timestampMs }
        if (type != "ALL") list = list.filter { it.type == type }
        if (camera != "ALL") list = list.filter { it.cameraName == camera }
        return list
    }
}
