package com.sentinel.companion.ui.screens.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.companion.data.model.ConnectionPrefs
import com.sentinel.companion.data.network.HostValidator
import com.sentinel.companion.data.network.ValidationStep
import com.sentinel.companion.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ConnectPhase {
    IDLE,
    RESOLVING_DNS,
    OPENING_SOCKET,
    PROBING_HTTP,
    SUCCESS,
    FAILED,
}

data class ConnectUiState(
    val hostAddress: String = "",
    val port: String = "8080",
    val useHttps: Boolean = false,
    val phase: ConnectPhase = ConnectPhase.IDLE,
    val statusLine: String = "",
    val errorMessage: String? = null,
    val isConnected: Boolean = false,
    val localOnlyMode: Boolean = false,
    val resolvedIp: String? = null,
    val httpCode: Int? = null,
    val isSentinelHost: Boolean = false,
) {
    val isBusy: Boolean get() = phase == ConnectPhase.RESOLVING_DNS ||
        phase == ConnectPhase.OPENING_SOCKET ||
        phase == ConnectPhase.PROBING_HTTP
}

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val prefsRepo: PreferencesRepository,
    private val hostValidator: HostValidator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    private var validationJob: Job? = null

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

    fun onHostChanged(value: String) {
        _uiState.value = _uiState.value.copy(hostAddress = value, errorMessage = null, phase = ConnectPhase.IDLE)
    }
    fun onPortChanged(value: String) {
        _uiState.value = _uiState.value.copy(port = value, errorMessage = null, phase = ConnectPhase.IDLE)
    }
    fun onHttpsToggled(value: Boolean) {
        _uiState.value = _uiState.value.copy(useHttps = value, phase = ConnectPhase.IDLE)
    }
    fun onLocalOnlyToggled(value: Boolean) {
        _uiState.value = _uiState.value.copy(localOnlyMode = value)
    }

    fun connect(onSuccess: () -> Unit) {
        val host = _uiState.value.hostAddress.trim()
        val portInt = _uiState.value.port.toIntOrNull()

        if (host.isBlank()) {
            _uiState.value = _uiState.value.copy(
                phase = ConnectPhase.FAILED,
                errorMessage = "HOST_ADDRESS_REQUIRED",
            )
            return
        }
        if (portInt == null || portInt !in 1..65535) {
            _uiState.value = _uiState.value.copy(
                phase = ConnectPhase.FAILED,
                errorMessage = "PORT_INVALID (must be 1-65535)",
            )
            return
        }

        validationJob?.cancel()
        validationJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                phase = ConnectPhase.RESOLVING_DNS,
                statusLine = "Resolving $host…",
                errorMessage = null,
                resolvedIp = null,
                httpCode = null,
                isSentinelHost = false,
                isConnected = false,
            )

            hostValidator.validate(host, portInt, _uiState.value.useHttps).collect { step ->
                when (step) {
                    is ValidationStep.ResolvingDns ->
                        _uiState.value = _uiState.value.copy(
                            phase = ConnectPhase.RESOLVING_DNS,
                            statusLine = "Resolving DNS for $host…",
                        )
                    is ValidationStep.DnsResolved ->
                        _uiState.value = _uiState.value.copy(
                            resolvedIp = step.ip,
                            statusLine = "DNS → ${step.ip}",
                        )
                    is ValidationStep.OpeningSocket ->
                        _uiState.value = _uiState.value.copy(
                            phase = ConnectPhase.OPENING_SOCKET,
                            statusLine = "Opening TCP $portInt → ${_uiState.value.resolvedIp ?: host}…",
                        )
                    is ValidationStep.SocketOpen ->
                        _uiState.value = _uiState.value.copy(
                            statusLine = "TCP $portInt open",
                        )
                    is ValidationStep.ProbingHttp ->
                        _uiState.value = _uiState.value.copy(
                            phase = ConnectPhase.PROBING_HTTP,
                            statusLine = "Probing ${if (_uiState.value.useHttps) "HTTPS" else "HTTP"}…",
                        )
                    is ValidationStep.HttpResponded ->
                        _uiState.value = _uiState.value.copy(
                            httpCode = step.code,
                            isSentinelHost = step.sentinelHeader,
                            statusLine = "HTTP ${step.code}" +
                                if (step.sentinelHeader) " · Sentinel host detected" else "",
                        )
                    is ValidationStep.Success -> {
                        prefsRepo.saveConnectionPrefs(
                            ConnectionPrefs(
                                hostAddress     = host,
                                port            = portInt,
                                useHttps        = _uiState.value.useHttps,
                                autoConnect     = true,
                                lastConnectedMs = System.currentTimeMillis(),
                            )
                        )
                        if (_uiState.value.localOnlyMode) {
                            persistLocalOnlyFlag(false)
                        }
                        _uiState.value = _uiState.value.copy(
                            phase = ConnectPhase.SUCCESS,
                            statusLine = "LINK_ESTABLISHED · ${step.ip} · HTTP ${step.httpCode}",
                            isConnected = true,
                            errorMessage = null,
                        )
                        onSuccess()
                    }
                    is ValidationStep.Failure -> {
                        _uiState.value = _uiState.value.copy(
                            phase = ConnectPhase.FAILED,
                            statusLine = "FAILED at ${step.phase.name}",
                            errorMessage = "${step.phase.name}: ${step.reason}",
                            isConnected = false,
                        )
                    }
                }
            }
        }
    }

    fun continueLocal(onSuccess: () -> Unit) {
        validationJob?.cancel()
        viewModelScope.launch {
            // Local-only is a real, valid configuration: persist explicitly with no host
            // and flip the localOnlyMode app flag so the rest of the app routes around the
            // network stack instead of pretending we're connected to a remote host.
            prefsRepo.saveConnectionPrefs(
                ConnectionPrefs(
                    hostAddress     = "",
                    port            = _uiState.value.port.toIntOrNull() ?: 8080,
                    useHttps        = false,
                    autoConnect     = false,
                    lastConnectedMs = System.currentTimeMillis(),
                )
            )
            persistLocalOnlyFlag(true)
            _uiState.value = _uiState.value.copy(
                phase = ConnectPhase.SUCCESS,
                statusLine = "LOCAL_ONLY_MODE active",
                errorMessage = null,
                isConnected = true,
                localOnlyMode = true,
            )
            onSuccess()
        }
    }

    private suspend fun persistLocalOnlyFlag(enabled: Boolean) {
        val current = prefsRepo.appPrefs.first()
        if (current.localOnlyMode != enabled) {
            prefsRepo.saveAppPrefs(current.copy(localOnlyMode = enabled))
        }
    }
}
