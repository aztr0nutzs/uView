package com.sentinel.app.data.remote.adapters

import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraSourceType
import com.sentinel.app.domain.model.ConnectionTestResult
import com.sentinel.app.domain.model.DiscoveredDevice
import com.sentinel.app.domain.service.CameraConnectionTester
import com.sentinel.app.domain.service.CameraDiscoveryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// CameraDiscoveryServiceImpl
//
// IMPLEMENTATION STATUS:
//   - Basic TCP port probe: implemented
//   - ONVIF WS-Discovery multicast: NOT YET IMPLEMENTED (requires UDP/SOAP)
//   - mDNS / Bonjour discovery: NOT YET IMPLEMENTED
//   - Subnet enumeration: implemented (linear scan — slow, replace with ARP for prod)
//
// NOTE: Real discovery requires CHANGE_WIFI_MULTICAST_STATE and may need
// network thread handling. This implementation uses sequential TCP probes
// as a functional placeholder.
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class CameraDiscoveryServiceImpl @Inject constructor() : CameraDiscoveryService {

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: Flow<Boolean> = _isScanning

    // Well-known camera ports to probe
    private val cameraPortProbes = listOf(
        80 to "HTTP",
        443 to "HTTPS",
        554 to "RTSP",
        8080 to "HTTP-Alt / IP Webcam",
        4747 to "DroidCam",
        8081 to "MJPEG-Alt",
        9000 to "Custom",
        37777 to "Dahua",
        34599 to "Hikvision"
    )

    override fun startScan(): Flow<DiscoveredDevice> = flow {
        _isScanning.value = true
        try {
            // Placeholder: scan 192.168.1.1–254 (real impl should detect subnet)
            val subnet = "192.168.1"
            Timber.d("Starting network scan on $subnet.0/24")
            for (host in 1..254) {
                val ip = "$subnet.$host"
                val device = probeDevice(ip)
                if (device != null) {
                    Timber.d("Found device at $ip")
                    emit(device)
                }
            }
        } finally {
            _isScanning.value = false
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun cancelScan() {
        _isScanning.value = false
    }

    override suspend fun probeDevice(ipAddress: String): DiscoveredDevice? =
        withContext(Dispatchers.IO) {
            val openPorts = mutableListOf<Int>()
            var probableType: CameraSourceType? = null
            var banner: String? = null

            for ((port, _) in cameraPortProbes) {
                if (tcpProbe(ipAddress, port, timeoutMs = 500)) {
                    openPorts.add(port)
                    when (port) {
                        554 -> probableType = CameraSourceType.RTSP
                        4747 -> probableType = CameraSourceType.ANDROID_DROIDCAM
                        8080 -> if (probableType == null) probableType = CameraSourceType.ANDROID_IPWEBCAM
                        80, 443 -> if (probableType == null) probableType = CameraSourceType.MJPEG
                    }
                }
            }

            if (openPorts.isEmpty()) return@withContext null

            DiscoveredDevice(
                ipAddress = ipAddress,
                hostname = null,  // reverse DNS lookup — future
                port = openPorts.first(),
                probableSourceType = probableType,
                openPorts = openPorts,
                banner = banner
            )
        }

    private fun tcpProbe(host: String, port: Int, timeoutMs: Int): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            true
        }
    } catch (e: Exception) {
        false
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CameraConnectionTesterImpl
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class CameraConnectionTesterImpl @Inject constructor() : CameraConnectionTester {

    override suspend fun testConnection(
        camera: CameraDevice,
        timeoutSeconds: Int
    ): ConnectionTestResult = withContext(Dispatchers.IO) {
        val host = camera.connectionProfile.host
        val port = camera.connectionProfile.port
        val resolvedUrl = camera.connectionProfile.toDisplayUrl()

        val start = System.currentTimeMillis()
        val reachable = pingHost(host, port, timeoutSeconds * 1000)
        val latencyMs = System.currentTimeMillis() - start

        if (!reachable) {
            return@withContext ConnectionTestResult(
                cameraId = camera.id,
                host = host,
                resolvedUrl = resolvedUrl,
                success = false,
                latencyMs = null,
                errorMessage = "Host $host:$port unreachable within ${timeoutSeconds}s",
                streamReachable = false
            )
        }

        // NOTE: Stream-level probe (actually opening RTSP session) is not
        // implemented here — requires Media3 / ExoPlayer integration.
        // The following marks stream as "not tested" rather than lying about it.
        ConnectionTestResult(
            cameraId = camera.id,
            host = host,
            resolvedUrl = resolvedUrl,
            success = true,
            latencyMs = latencyMs,
            errorMessage = null,
            streamReachable = false,    // NOTE: set to false — stream test not implemented
            credentialsAccepted = null  // NOTE: not tested at TCP level
        )
    }

    override suspend fun pingHost(host: String, port: Int, timeoutMs: Int): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    true
                }
            } catch (e: Exception) {
                Timber.d("Ping failed for $host:$port — ${e.message}")
                false
            }
        }

    override fun validateStreamUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            val uri = URI(url)
            uri.scheme in listOf("rtsp", "rtsps", "http", "https") && uri.host != null
        } catch (e: Exception) {
            false
        }
    }
}
