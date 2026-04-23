package com.sentinel.app.features.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraEvent
import com.sentinel.app.domain.model.CameraEventType
import com.sentinel.app.domain.repository.CameraEventRepository
import com.sentinel.app.domain.repository.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventsUiState(
    val events: List<CameraEvent> = emptyList(),
    val cameras: List<CameraDevice> = emptyList(),
    val selectedCameraId: String? = null,
    val selectedEventType: CameraEventType? = null,
    val isLoading: Boolean = true
) {
    val filteredEvents: List<CameraEvent> get() = events.filter { ev ->
        (selectedCameraId == null || ev.cameraId == selectedCameraId) &&
        (selectedEventType == null || ev.eventType == selectedEventType)
    }
}

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventRepository: CameraEventRepository,
    private val cameraRepository: CameraRepository
) : ViewModel() {

    private val _filters = MutableStateFlow(Pair<String?, CameraEventType?>(null, null))

    val uiState: StateFlow<EventsUiState> = combine(
        eventRepository.observeEvents(limit = 300),
        cameraRepository.observeAllCameras(),
        _filters
    ) { events, cameras, (camId, evType) ->
        EventsUiState(
            events = events,
            cameras = cameras,
            selectedCameraId = camId,
            selectedEventType = evType,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventsUiState())

    fun setCameraFilter(id: String?)         = _filters.update { it.copy(first = id) }
    fun setEventTypeFilter(t: CameraEventType?) = _filters.update { it.copy(second = t) }
    fun clearFilters()                        = _filters.value = Pair(null, null)

    fun markAllRead() = viewModelScope.launch { eventRepository.markAllAsRead() }

    fun markRead(eventId: String) = viewModelScope.launch {
        eventRepository.markAsRead(listOf(eventId))
    }
}
