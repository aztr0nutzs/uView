package com.sentinel.companion.ui.screens.devicelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.companion.data.model.DeviceProfile
import com.sentinel.companion.data.model.DeviceState
import com.sentinel.companion.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceListUiState(
    val devices: List<DeviceProfile> = emptyList(),
    val filter: String = "",
    val locationFilter: String = "",
    val availableLocations: List<String> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class DeviceListViewModel @Inject constructor(
    private val repo: DeviceRepository,
) : ViewModel() {

    private val _filter   = MutableStateFlow("")
    private val _location = MutableStateFlow("")

    val state: StateFlow<DeviceListUiState> = combine(
        repo.devices,
        repo.locations,
        _filter,
        _location,
    ) { devices, locations, filter, location ->
        val filtered = devices
            .filter { location.isBlank() || it.location == location }
            .filter { filter.isBlank() || it.name.contains(filter, ignoreCase = true) || it.host.contains(filter) }
        DeviceListUiState(
            devices            = filtered,
            filter             = filter,
            locationFilter     = location,
            availableLocations = locations,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceListUiState(isLoading = true))

    fun onFilterChanged(v: String) { _filter.value = v }
    fun onLocationFilterChanged(v: String) { _location.value = if (_location.value == v) "" else v }

    fun toggleFavorite(device: DeviceProfile) {
        viewModelScope.launch { repo.setFavorite(device.id, !device.isFavorite) }
    }

    fun toggleEnabled(device: DeviceProfile) {
        viewModelScope.launch { repo.setEnabled(device.id, !device.isEnabled) }
    }

    fun reconnect(device: DeviceProfile) {
        viewModelScope.launch { repo.reconnect(device.id) }
    }

    fun delete(device: DeviceProfile) {
        viewModelScope.launch { repo.delete(device.id) }
    }
}
