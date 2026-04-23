package com.sentinel.app.features.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.app.domain.model.DiscoveredDevice
import com.sentinel.app.domain.model.DiscoveryConfidence
import com.sentinel.app.domain.model.DiscoveryMethod
import com.sentinel.app.domain.model.ScanConfig
import com.sentinel.app.domain.repository.CameraRepository
import com.sentinel.app.domain.service.CameraDiscoveryService
import com.sentinel.app.domain.service.DiscoveryCapabilities
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class DiscoveryUiState(
    val devices: List<DiscoveredDevice> = emptyList(),
    val isScanning: Boolean = false,
    val scanComplete: Boolean = false,
    val scanDurationMs: Long = 0,
    val capabilities: DiscoveryCapabilities? = null,
    val activeStrategies: Set<DiscoveryMethod> = emptySet(),
    val errorMessage: String? = null,
    val addedHostnames: Set<String> = emptySet()   // IPs already configured as cameras
)

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val discoveryService: CameraDiscoveryService,
    private val cameraRepository: CameraRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _state.asStateFlow()

    private var scanJob: Job? = null
    private var scanStart = 0L

    init {
        // Load capabilities and already-added hosts on start
        viewModelScope.launch {
            val caps = discoveryService.checkDiscoveryCapabilities()
            _state.update { it.copy(capabilities = caps) }
        }
        viewModelScope.launch {
            cameraRepository.observeAllCameras().collect { cameras ->
                val hosts = cameras.map { it.connectionProfile.host }.toSet()
                _state.update { s ->
                    s.copy(
                        addedHostnames = hosts,
                        devices = s.devices.map { d ->
                            d.copy(isAlreadyAdded = d.ipAddress in hosts)
                        }
                    )
                }
            }
        }
    }

    fun startScan(customSubnet: String? = null) {
        scanJob?.cancel()
        scanStart = System.currentTimeMillis()

        _state.update { it.copy(
            devices       = emptyList(),
            isScanning    = true,
            scanComplete  = false,
            errorMessage  = null,
            activeStrategies = emptySet()
        )}

        val config = ScanConfig(
            subnet                = customSubnet,
            timeoutPerHostMs      = 500,
            parallelProbes        = 50,
            useArpTable           = true,
            useMdns               = true,
            useOnvifWsDiscovery   = true,
            useTcpProbe           = true,
            tcpProbeSubnet        = true,
            mdnsTimeoutMs         = 4_000L,
            onvifTimeoutMs        = 3_000L
        )

        scanJob = viewModelScope.launch {
            discoveryService.startScan(config)
                .catch { e ->
                    Timber.e(e, "DiscoveryViewModel: scan error")
                    _state.update { it.copy(
                        isScanning   = false,
                        scanComplete = true,
                        errorMessage = e.message ?: "Scan failed"
                    )}
                }
                .collect { device ->
                    val addedHosts = _state.value.addedHostnames
                    val enriched   = device.copy(isAlreadyAdded = device.ipAddress in addedHosts)
                    _state.update { state ->
                        val existing = state.devices.toMutableList()
                        val idx      = existing.indexOfFirst { it.ipAddress == enriched.ipAddress }
                        if (idx >= 0) {
                            // Merge: higher-confidence discovery wins
                            val existing_item = existing[idx]
                            existing[idx] = mergeDevices(existing_item, enriched)
                        } else {
                            existing.add(enriched)
                        }
                        state.copy(
                            devices          = existing.sortedWith(compareByDescending<DiscoveredDevice> {
                                it.confidence.ordinal
                            }.thenBy { it.ipAddress }),
                            activeStrategies = state.activeStrategies + enriched.discoveryMethod
                        )
                    }
                }

            val duration = System.currentTimeMillis() - scanStart
            _state.update { it.copy(
                isScanning    = false,
                scanComplete  = true,
                scanDurationMs = duration
            )}
            Timber.i("DiscoveryViewModel: scan complete in ${duration}ms, found ${_state.value.devices.size} devices")
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        viewModelScope.launch { discoveryService.cancelScan() }
        _state.update { it.copy(isScanning = false, scanComplete = true) }
    }

    fun probeManual(ipAddress: String) {
        viewModelScope.launch {
            val device = discoveryService.probeDevice(ipAddress.trim()) ?: return@launch
            val addedHosts = _state.value.addedHostnames
            val enriched = device.copy(isAlreadyAdded = device.ipAddress in addedHosts)
            _state.update { state ->
                val existing = state.devices.toMutableList()
                val idx = existing.indexOfFirst { it.ipAddress == enriched.ipAddress }
                if (idx >= 0) existing[idx] = enriched else existing.add(enriched)
                state.copy(devices = existing)
            }
        }
    }

    fun clearResults() {
        scanJob?.cancel()
        _state.update { it.copy(devices = emptyList(), scanComplete = false, errorMessage = null) }
    }

    /**
     * Merge two discoveries of the same device.
     * Higher confidence / more specific discovery method wins.
     * Fields are merged so we keep the richest data from each.
     */
    private fun mergeDevices(existing: DiscoveredDevice, incoming: DiscoveredDevice): DiscoveredDevice {
        val winner = if (incoming.confidence.ordinal > existing.confidence.ordinal) incoming else existing
        return winner.copy(
            openPorts         = (existing.openPorts + incoming.openPorts).distinct().sorted(),
            banner            = existing.banner ?: incoming.banner,
            macAddress        = existing.macAddress ?: incoming.macAddress,
            macVendor         = existing.macVendor ?: incoming.macVendor,
            onvifManufacturer = existing.onvifManufacturer ?: incoming.onvifManufacturer,
            onvifModel        = existing.onvifModel ?: incoming.onvifModel,
            onvifXAddrs       = (existing.onvifXAddrs + incoming.onvifXAddrs).distinct(),
            mdnsServiceName   = existing.mdnsServiceName ?: incoming.mdnsServiceName,
            hostname          = existing.hostname ?: incoming.hostname
        )
    }
}
