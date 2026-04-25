package com.sentinel.companion.ui.screens.devicesettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.companion.data.model.AuthType
import com.sentinel.companion.data.model.DeviceProfile
import com.sentinel.companion.data.model.StreamProtocol
import com.sentinel.companion.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceSettingsUiState(
    val device: DeviceProfile? = null,
    // editable fields
    val name: String = "",
    val location: String = "",
    val host: String = "",
    val port: String = "",
    val path: String = "",
    val protocol: String = StreamProtocol.RTSP.name,
    val authType: String = AuthType.NONE.name,
    val username: String = "",
    val password: String = "",
    val isFavorite: Boolean = false,
    val isEnabled: Boolean = true,
    // state
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    val isLoaded: Boolean = false,
)

@HiltViewModel
class DeviceSettingsViewModel @Inject constructor(
    private val repo: DeviceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceSettingsUiState())
    val state: StateFlow<DeviceSettingsUiState> = _state.asStateFlow()

    fun load(deviceId: String) {
        viewModelScope.launch {
            repo.observeDevice(deviceId).collect { device ->
                if (device != null && !_state.value.isLoaded) {
                    _state.value = DeviceSettingsUiState(
                        device    = device,
                        name      = device.name,
                        location  = device.location,
                        host      = device.host,
                        port      = device.port.toString(),
                        path      = device.path,
                        protocol  = device.protocol,
                        authType  = device.authType,
                        username  = device.username,
                        password  = device.password,
                        isFavorite = device.isFavorite,
                        isEnabled  = device.isEnabled,
                        isLoaded   = true,
                    )
                }
            }
        }
    }

    fun onNameChanged(v: String)      { _state.value = _state.value.copy(name = v) }
    fun onLocationChanged(v: String)  { _state.value = _state.value.copy(location = v) }
    fun onHostChanged(v: String)      { _state.value = _state.value.copy(host = v) }
    fun onPortChanged(v: String)      { _state.value = _state.value.copy(port = v) }
    fun onPathChanged(v: String)      { _state.value = _state.value.copy(path = v) }
    fun onProtocolChanged(v: String)  { _state.value = _state.value.copy(protocol = v) }
    fun onAuthTypeChanged(v: String)  { _state.value = _state.value.copy(authType = v) }
    fun onUsernameChanged(v: String)  { _state.value = _state.value.copy(username = v) }
    fun onPasswordChanged(v: String)  { _state.value = _state.value.copy(password = v) }

    fun toggleFavorite() {
        val next = !_state.value.isFavorite
        _state.value = _state.value.copy(isFavorite = next)
        _state.value.device?.let { d ->
            viewModelScope.launch { repo.setFavorite(d.id, next) }
        }
    }

    fun toggleEnabled() {
        val next = !_state.value.isEnabled
        _state.value = _state.value.copy(isEnabled = next)
        _state.value.device?.let { d ->
            viewModelScope.launch { repo.setEnabled(d.id, next) }
        }
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        val original = s.device ?: return
        _state.value = s.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                val updated = original.copy(
                    name     = s.name.trim().ifBlank { original.name },
                    location = s.location.trim().ifBlank { original.location },
                    host     = s.host.trim(),
                    port     = s.port.toIntOrNull() ?: original.port,
                    path     = s.path.trim().ifBlank { "/" },
                    protocol = s.protocol,
                    authType = s.authType,
                    username = s.username,
                    password = s.password,
                )
                repo.update(updated)
                _state.value = _state.value.copy(isSaving = false, saveSuccess = true)
                onDone()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.localizedMessage)
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = _state.value.device?.id ?: return
        viewModelScope.launch {
            repo.delete(id)
            onDone()
        }
    }
}
