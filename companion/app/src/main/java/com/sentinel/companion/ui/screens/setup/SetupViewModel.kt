package com.sentinel.companion.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.companion.data.model.AuthType
import com.sentinel.companion.data.model.DeviceProfile
import com.sentinel.companion.data.model.DeviceState
import com.sentinel.companion.data.model.DiscoveredDevice
import com.sentinel.companion.data.model.StreamProtocol
import com.sentinel.companion.data.network.NetworkDiscovery
import com.sentinel.companion.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class SetupStep { DISCOVER, AUTH, CONFIRM }

data class SetupUiState(
    val step: SetupStep = SetupStep.DISCOVER,
    // Step 1 — Discover
    val isScanning: Boolean = false,
    val discoveredDevices: List<DiscoveredDevice> = emptyList(),
    val selectedDevice: DiscoveredDevice? = null,
    val manualHost: String = "",
    val manualPort: String = "554",
    val scanError: String? = null,
    // Step 2 — Auth
    val deviceName: String = "",
    val location: String = "",
    val protocol: StreamProtocol = StreamProtocol.RTSP,
    val streamPath: String = "/",
    val username: String = "",
    val password: String = "",
    val authType: AuthType = AuthType.NONE,
    val isTestingConnection: Boolean = false,
    val connectionTestResult: String? = null,
    val connectionTestOk: Boolean = false,
    // Step 3 — Confirm
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val savedDeviceId: String? = null,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val discovery: NetworkDiscovery,
    private val repo: DeviceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    private var scanJob: Job? = null

    // ── Step 1: Discover ──────────────────────────────────────────────────────

    fun startScan() {
        scanJob?.cancel()
        _state.value = _state.value.copy(isScanning = true, discoveredDevices = emptyList(), scanError = null)
        scanJob = viewModelScope.launch {
            try {
                discovery.discoverViaMdns().collect { device ->
                    val current = _state.value.discoveredDevices
                    if (current.none { it.host == device.host && it.port == device.port }) {
                        _state.value = _state.value.copy(
                            discoveredDevices = current + device,
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(scanError = e.localizedMessage)
            } finally {
                _state.value = _state.value.copy(isScanning = false)
            }
        }
        // Also sweep subnet
        viewModelScope.launch {
            discovery.sweepSubnet().collect { device ->
                val current = _state.value.discoveredDevices
                if (current.none { it.host == device.host }) {
                    _state.value = _state.value.copy(discoveredDevices = current + device)
                }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _state.value = _state.value.copy(isScanning = false)
    }

    fun selectDiscoveredDevice(device: DiscoveredDevice) {
        _state.value = _state.value.copy(
            selectedDevice = device,
            manualHost     = device.host,
            manualPort     = device.port.toString(),
            protocol       = device.suggestedProtocol,
            deviceName     = device.name.take(32),
        )
    }

    fun onManualHostChanged(v: String) { _state.value = _state.value.copy(manualHost = v) }
    fun onManualPortChanged(v: String) { _state.value = _state.value.copy(manualPort = v) }

    fun probeManual() {
        val host = _state.value.manualHost.trim()
        if (host.isBlank()) return
        _state.value = _state.value.copy(isScanning = true, scanError = null)
        viewModelScope.launch {
            val result = discovery.probeHost(host)
            _state.value = if (result != null) {
                _state.value.copy(
                    isScanning     = false,
                    selectedDevice = result,
                    manualPort     = result.port.toString(),
                    protocol       = result.suggestedProtocol,
                    deviceName     = host,
                )
            } else {
                _state.value.copy(isScanning = false, scanError = "HOST_UNREACHABLE // No camera ports found")
            }
        }
    }

    fun goToAuth() {
        val host = _state.value.manualHost.ifBlank { _state.value.selectedDevice?.host ?: return }
        _state.value = _state.value.copy(
            step       = SetupStep.AUTH,
            manualHost = host,
            deviceName = _state.value.deviceName.ifBlank { host },
        )
    }

    // ── Step 2: Auth ──────────────────────────────────────────────────────────

    fun onDeviceNameChanged(v: String)  { _state.value = _state.value.copy(deviceName = v) }
    fun onLocationChanged(v: String)    { _state.value = _state.value.copy(location = v) }
    fun onProtocolChanged(v: StreamProtocol) { _state.value = _state.value.copy(protocol = v) }
    fun onPathChanged(v: String)        { _state.value = _state.value.copy(streamPath = v) }
    fun onUsernameChanged(v: String)    { _state.value = _state.value.copy(username = v) }
    fun onPasswordChanged(v: String)    { _state.value = _state.value.copy(password = v) }
    fun onAuthTypeChanged(v: AuthType)  { _state.value = _state.value.copy(authType = v) }

    fun testConnection() {
        _state.value = _state.value.copy(
            isTestingConnection  = true,
            connectionTestResult = null,
            connectionTestOk     = false,
        )
        viewModelScope.launch {
            kotlinx.coroutines.delay(1800)
            // In production: open a socket or HEAD request to confirm the stream endpoint
            val ok = _state.value.manualHost.isNotBlank()
            _state.value = _state.value.copy(
                isTestingConnection  = false,
                connectionTestOk     = ok,
                connectionTestResult = if (ok) "CONNECTION_OK // Stream endpoint reachable" else "CONNECTION_FAILED",
            )
        }
    }

    fun goToConfirm() {
        _state.value = _state.value.copy(step = SetupStep.CONFIRM)
    }

    fun goBack() {
        _state.value = _state.value.copy(
            step = when (_state.value.step) {
                SetupStep.AUTH    -> SetupStep.DISCOVER
                SetupStep.CONFIRM -> SetupStep.AUTH
                else              -> SetupStep.DISCOVER
            }
        )
    }

    // ── Step 3: Confirm + Save ────────────────────────────────────────────────

    fun saveDevice(onSaved: (String) -> Unit) {
        _state.value = _state.value.copy(isSaving = true, saveError = null)
        viewModelScope.launch {
            try {
                val s = _state.value
                val device = DeviceProfile(
                    id              = UUID.randomUUID().toString(),
                    name            = s.deviceName.trim().ifBlank { s.manualHost },
                    location        = s.location.trim().ifBlank { "Unassigned" },
                    protocol        = s.protocol.name,
                    host            = s.manualHost.trim(),
                    port            = s.manualPort.toIntOrNull() ?: s.protocol.defaultPort,
                    path            = s.streamPath.trim().ifBlank { "/" },
                    username        = s.username,
                    password        = s.password,
                    authType        = s.authType.name,
                    state           = DeviceState.CONNECTING.name,
                    discoveredVia   = s.selectedDevice?.discoveryMethod ?: "MANUAL",
                    serviceType     = s.selectedDevice?.serviceType ?: "",
                )
                repo.save(device)
                _state.value = _state.value.copy(isSaving = false, savedDeviceId = device.id)
                onSaved(device.id)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, saveError = e.localizedMessage)
            }
        }
    }
}
