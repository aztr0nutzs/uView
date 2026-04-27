package com.sentinel.companion.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.companion.data.model.Alert
import com.sentinel.companion.data.model.DeviceProfile
import com.sentinel.companion.data.model.DeviceState
import com.sentinel.companion.data.model.SystemStatus
import com.sentinel.companion.data.repository.AlertsRepository
import com.sentinel.companion.data.repository.DeviceRepository
import com.sentinel.companion.data.repository.PreferencesRepository
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
    val pinnedDevices: List<DeviceProfile> = emptyList(),
    val recentAlerts: List<Alert> = emptyList(),
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isEmpty: Boolean = false,
    val syncError: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val deviceRepo: DeviceRepository,
    private val alertsRepo: AlertsRepository,
    private val prefsRepo: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Single source of truth: DeviceRepository. Alerts + sync state live in
            // AlertsRepository and don't carry their own device list.
            combine(
                deviceRepo.devices,
                alertsRepo.alerts,
                alertsRepo.syncState,
                prefsRepo.connectionPrefs,
            ) { devices, alerts, sync, conn ->
                val online   = devices.count { it.stateEnum() == DeviceState.ONLINE }
                val offline  = devices.count { it.stateEnum() == DeviceState.OFFLINE }
                val connecting = devices.count { it.stateEnum() == DeviceState.CONNECTING }
                val disabled = devices.count { it.stateEnum() == DeviceState.DISABLED }
                val unread   = alerts.count { !it.isRead }

                DashboardUiState(
                    systemStatus = SystemStatus(
                        isConnected       = sync.lastOutcome?.ok == true,
                        hostAddress       = conn.hostAddress,
                        totalCameras      = devices.size,
                        onlineCameras    = online,
                        offlineCameras   = offline,
                        connectingCameras = connecting,
                        disabledCameras  = disabled,
                        unreadAlerts     = unread,
                        uptimeMs         = 0L,
                        lastSyncMs       = sync.lastOutcome?.finishedAtMs ?: 0L,
                    ),
                    pinnedDevices = devices
                        .sortedWith(
                            compareByDescending<DeviceProfile> { it.isFavorite }
                                .thenByDescending { it.stateEnum() == DeviceState.ONLINE }
                                .thenBy { it.name },
                        )
                        .take(4),
                    recentAlerts  = alerts.take(5),
                    isLoading     = false,
                    isSyncing     = sync.phase == SyncPhase.RUNNING,
                    isEmpty       = devices.isEmpty(),
                    syncError     = sync.lastOutcome?.takeIf { !it.ok }?.error,
                )
            }.collect { _uiState.value = it }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { alertsRepo.refresh() }
    }
}
