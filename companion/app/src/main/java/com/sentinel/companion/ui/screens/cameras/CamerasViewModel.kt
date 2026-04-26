package com.sentinel.companion.ui.screens.cameras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.companion.data.model.Camera
import com.sentinel.companion.data.repository.CameraRepository
import com.sentinel.companion.data.sync.SyncPhase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CamerasUiState(
    val cameras: List<Camera> = emptyList(),
    val filteredCameras: List<Camera> = emptyList(),
    val rooms: List<String> = emptyList(),
    val selectedRoom: String = "ALL_NODES",
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isEmpty: Boolean = false,
    val syncError: String? = null,
    val lastSyncMs: Long = 0L,
)

@HiltViewModel
class CamerasViewModel @Inject constructor(
    private val repo: CameraRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CamerasUiState())
    val uiState: StateFlow<CamerasUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repo.cameras, repo.syncState) { cameras, sync ->
                cameras to sync
            }.collect { (cameras, sync) ->
                val rooms = listOf("ALL_NODES") + cameras.map { it.room }.distinct().sorted()
                val filtered = applyFilter(cameras, _uiState.value.selectedRoom, _uiState.value.searchQuery)
                _uiState.value = _uiState.value.copy(
                    cameras         = cameras,
                    filteredCameras = filtered,
                    rooms           = rooms,
                    isLoading       = false,
                    isSyncing       = sync.phase == SyncPhase.RUNNING,
                    isEmpty         = cameras.isEmpty(),
                    syncError       = sync.lastOutcome?.takeIf { !it.ok }?.error,
                    lastSyncMs      = sync.lastOutcome?.finishedAtMs ?: 0L,
                )
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { repo.refresh() }
    }

    fun onRoomSelected(room: String) {
        val filtered = applyFilter(_uiState.value.cameras, room, _uiState.value.searchQuery)
        _uiState.value = _uiState.value.copy(selectedRoom = room, filteredCameras = filtered)
    }

    fun onSearchChanged(query: String) {
        val filtered = applyFilter(_uiState.value.cameras, _uiState.value.selectedRoom, query)
        _uiState.value = _uiState.value.copy(searchQuery = query, filteredCameras = filtered)
    }

    fun toggleFavorite(cameraId: String) {
        viewModelScope.launch { repo.toggleFavorite(cameraId) }
    }

    fun toggleEnabled(cameraId: String) {
        viewModelScope.launch { repo.toggleEnabled(cameraId) }
    }

    fun reconnect(cameraId: String) {
        viewModelScope.launch { repo.reconnectCamera(cameraId) }
    }

    fun deleteCamera(cameraId: String) {
        viewModelScope.launch { repo.deleteCamera(cameraId) }
    }

    private fun applyFilter(cameras: List<Camera>, room: String, query: String): List<Camera> {
        var list = if (room == "ALL_NODES") cameras else cameras.filter { it.room == room }
        if (query.isNotBlank()) {
            val q = query.lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) || it.room.lowercase().contains(q) || it.streamUrl.lowercase().contains(q)
            }
        }
        return list
    }
}
