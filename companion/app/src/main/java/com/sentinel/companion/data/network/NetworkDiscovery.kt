package com.sentinel.companion.data.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.sentinel.companion.data.model.DiscoveredDevice
import com.sentinel.companion.data.model.StreamProtocol
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

private val CAMERA_SERVICE_TYPES = listOf(
    "_rtsp._tcp.",
    "_http._tcp.",
    "_onvif._tcp.",
    "_droidcam._tcp.",
    "_axis-video._tcp.",
)

private val COMMON_CAMERA_PORTS = listOf(554, 8080, 8554, 80, 4747, 1935, 8000)

@Singleton
class NetworkDiscovery @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    // ── mDNS discovery ────────────────────────────────────────────────────────

    fun discoverViaMdns(): Flow<DiscoveredDevice> = callbackFlow {
        val listeners = CAMERA_SERVICE_TYPES.map { serviceType ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Timber.w("mDNS start failed for $serviceType: $errorCode")
                }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
                override fun onDiscoveryStarted(serviceType: String) {
                    Timber.d("mDNS discovery started: $serviceType")
                }
                override fun onDiscoveryStopped(serviceType: String) {}

                override fun onServiceFound(info: NsdServiceInfo) {
                    nsdManager.resolveService(info, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, err: Int) {}
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val host = info.host?.hostAddress ?: return
                            val port = info.port
                            val device = DiscoveredDevice(
                                host             = host,
                                port             = port,
                                name             = info.serviceName,
                                serviceType      = info.serviceType,
                                discoveryMethod  = "MDNS",
                                suggestedProtocol= suggestProtocol(info.serviceType, port),
                                confidence       = 80,
                            )
                            trySend(device)
                        }
                    })
                }

                override fun onServiceLost(info: NsdServiceInfo) {}
            }
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
            listener
        }

        awaitClose {
            listeners.forEach { listener ->
                try { nsdManager.stopServiceDiscovery(listener) } catch (_: Exception) {}
            }
        }
    }.flowOn(Dispatchers.IO)

    // ── Manual IP probe ───────────────────────────────────────────────────────

    suspend fun probeHost(host: String): DiscoveredDevice? = withContext(Dispatchers.IO) {
        val openPorts = COMMON_CAMERA_PORTS.filter { port -> isPortOpen(host, port) }
        if (openPorts.isEmpty()) return@withContext null

        val bestPort = openPorts.first()
        DiscoveredDevice(
            host             = host,
            port             = bestPort,
            name             = host,
            serviceType      = "tcp_probe",
            discoveryMethod  = "TCP_PROBE",
            suggestedProtocol= suggestProtocol("", bestPort),
            openPorts        = openPorts,
            confidence       = 60,
        )
    }

    // ── Subnet sweep ─────────────────────────────────────────────────────────

    fun sweepSubnet(): Flow<DiscoveredDevice> = callbackFlow {
        val subnet = getSubnetBase()
        if (subnet == null) {
            close()
            return@callbackFlow
        }

        // Sweep /24 in parallel batches
        (1..254).chunked(32).forEach { batch ->
            batch.forEach { i ->
                launch(Dispatchers.IO) {
                    val host = "$subnet.$i"
                    try {
                        if (InetAddress.getByName(host).isReachable(300)) {
                            val device = probeHost(host)
                            if (device != null) trySend(device)
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        awaitClose {}
    }.flowOn(Dispatchers.IO)

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isPortOpen(host: String, port: Int, timeoutMs: Int = 400): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            true
        }
    } catch (_: Exception) { false }

    private fun getSubnetBase(): String? {
        val dhcpInfo = wifiManager.dhcpInfo ?: return null
        val ip = dhcpInfo.gateway
        if (ip == 0) return null
        return "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}"
    }

    private fun suggestProtocol(serviceType: String, port: Int): StreamProtocol = when {
        "_rtsp"    in serviceType || port == 554 || port == 8554 -> StreamProtocol.RTSP
        "_onvif"   in serviceType                                -> StreamProtocol.ONVIF
        "_droidcam" in serviceType || port == 4747              -> StreamProtocol.DROIDCAM
        "_axis"    in serviceType                                -> StreamProtocol.RTSP
        port == 8080 || port == 80                               -> StreamProtocol.MJPEG
        else                                                     -> StreamProtocol.CUSTOM
    }

    fun getLocalIpAddress(): String? {
        val dhcpInfo = wifiManager.dhcpInfo ?: return null
        val ip = dhcpInfo.ipAddress
        if (ip == 0) return null
        return "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"
    }
}
