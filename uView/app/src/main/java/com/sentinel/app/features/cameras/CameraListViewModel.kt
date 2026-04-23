package com.sentinel.app.features.cameras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.repository.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraListUiState(
    val cameras: List<CameraDevice> = emptyList(),
    val filteredCameras: List<CameraDevice> = emptyList(),
    val selectedRoom: String? = null,
    val allRooms: List<String> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class CameraListViewModel @Inject constructor(
    private val cameraRepository: CameraRepository
) : ViewModel() {

    private val selectedRoom = MutableStateFlow<String?>(null)
    private val searchQuery  = MutableStateFlow("")

    val uiState: StateFlow<CameraListUiState> = combine(
        cameraRepository.observeAllCameras(),
        selectedRoom,
        searchQuery
    ) { cameras, room, query ->
        val rooms = cameras.map { it.room }.distinct().sorted()
        val filtered = cameras.filter { cam ->
            val matchesRoom  = room == null || cam.room == room
            val matchesQuery = query.isBlank() ||
                cam.name.contains(query, ignoreCase = true) ||
                cam.room.contains(query, ignoreCase = true) ||
                cam.sourceType.displayName.contains(query, ignoreCase = true)
            matchesRoom && matchesQuery
        }
        CameraListUiState(
            cameras = cameras,
            filteredCameras = filtered,
            selectedRoom = room,
            allRooms = rooms,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CameraListUiState())

    fun setRoomFilter(room: String?) { selectedRoom.value = room }
    fun setSearchQuery(q: String)    { searchQuery.value = q }

    fun toggleEnabled(cameraId: String, enabled: Boolean) = viewModelScope.launch {
        cameraRepository.setCameraEnabled(cameraId, enabled)
    }
    fun toggleFavorite(cameraId: String) = viewModelScope.launch {
        cameraRepository.toggleFavorite(cameraId)
    }
    fun deleteCamera(cameraId: String) = viewModelScope.launch {
        cameraRepository.deleteCamera(cameraId)
    }
}
