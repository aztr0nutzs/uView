package com.sentinel.companion.ui.screens.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.companion.data.model.ConnectionPrefs
import com.sentinel.companion.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectUiState(
    val hostAddress: String = "",
    val port: String = "8080",
    val useHttps: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val isConnected: Boolean = false,
    val localOnlyMode: Boolean = false,
)

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val prefsRepo: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = prefsRepo.connectionPrefs.first()
            _uiState.value = _uiState.value.copy(
                hostAddress = prefs.hostAddress,
                port        = prefs.port.toString(),
                useHttps    = prefs.useHttps,
            )
        }
    }

    fun onHostChanged(value: String) { _uiState.value = _uiState.value.copy(hostAddress = value, errorMessage = null) }
    fun onPortChanged(value: String) { _uiState.value = _uiState.value.copy(port = value, errorMessage = null) }
    fun onHttpsToggled(value: Boolean) { _uiState.value = _uiState.value.copy(useHttps = value) }
    fun onLocalOnlyToggled(value: Boolean) { _uiState.value = _uiState.value.copy(localOnlyMode = value) }

    fun connect(onSuccess: () -> Unit) {
        val host = _uiState.value.hostAddress.trim()
        val portInt = _uiState.value.port.toIntOrNull() ?: 8080

        if (host.isBlank() && !_uiState.value.localOnlyMode) {
            _uiState.value = _uiState.value.copy(errorMessage = "HOST_ADDRESS_REQUIRED")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true, errorMessage = null)
            delay(1200)
            prefsRepo.saveConnectionPrefs(
                ConnectionPrefs(
                    hostAddress     = host,
                    port            = portInt,
                    useHttps        = _uiState.value.useHttps,
                    autoConnect     = true,
                    lastConnectedMs = System.currentTimeMillis(),
                )
            )
            _uiState.value = _uiState.value.copy(isConnecting = false, isConnected = true)
            onSuccess()
        }
    }

    fun continueLocal(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true)
            delay(600)
            _uiState.value = _uiState.value.copy(isConnecting = false, isConnected = true)
            onSuccess()
        }
    }
}
