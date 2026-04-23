package com.sentinel.app.data.discovery

import android.content.Context
import com.sentinel.app.data.discovery.arp.ArpTableScanner
import com.sentinel.app.data.discovery.mdns.MdnsDiscovery
import com.sentinel.app.data.discovery.onvif.OnvifWsDiscovery
import com.sentinel.app.domain.model.DiscoveredDevice
import com.sentinel.app.domain.model.DiscoveryConfidence
import com.sentinel.app.domain.model.DiscoveryMethod
import com.sentinel.app.domain.service.CameraDiscoveryService
import com.sentinel.app.domain.service.DiscoveryCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CameraDiscoveryServiceImpl — Phase 4
 *
 * Orchestrates all four discovery strategies in parallel using a [channelFlow]:
 *
 *   Strategy 1 — ARP Table (instant)
 *     Reads /proc/net/arp to get live LAN hosts with MAC addresses.
 *     These hosts are then TCP-probed to check for camera ports.
 *
 *   Strategy 2 — mDNS / NSD (fast, ~4 seconds)
 *     Uses Android NsdManager to find devices advertising camera services
 *     (_rtsp._tcp, _http._tcp, _axis-video._tcp, etc).
 *
 *   Strategy 3 — ONVIF WS-Discovery (fast, ~3 seconds)
 *     Sends UDP multicast Probe to 239.255.255.250:3702.
 *     Real ONVIF cameras reply with ProbeMatch containing their XAddr URLs.
 *
 *   Strategy 4 — TCP Port Probe fallback (slower, ~5-10 seconds)
 *     Runs only on hosts NOT already found by strategies 1-3.
 *     Covers cameras that don't use mDNS or ONVIF.
 *
 * All strategies run concurrently. The flow emits each device as soon as
 * any strategy finds it. Duplicates (same IP from multiple strategies)
 * are merged — the higher-confidence discovery method wins.
 *
 * The caller (DiscoveryViewModel) deduplicates by IP address in its state.
 */
@Singleton
class CameraDiscoveryServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subnetDetector: SubnetDetector,
    private val arpScanner: ArpTableScanner,
    private val mdnsDiscovery: MdnsDiscovery,
    private val onvifDiscovery: OnvifWsDiscovery,
    private val portProber: PortProber
) : CameraDiscoveryService {

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning

    // Hosts found by fast strategies — excluded from full subnet TCP probe
    private val fastFoundHosts = mutableSetOf<String>()

    override fun startScan(config: com.sentinel.app.domain.model.ScanConfig): Flow<DiscoveredDevice> =
        channelFlow {
            _isScanning.value = true
            fastFoundHosts.clear()

            val subnet = config.subnet
                ?: subnetDetector.detectSubnet()
                ?: "192.168.1"   // Last-resort fallback

            Timber.i("DiscoveryService: starting scan on $subnet.0/24")
            Timber.d("DiscoveryService: config=$config")

            coroutineScope {

                // ── Strategy 1: ARP Table ─────────────────────────────────
                if (config.useArpTable) {
                    launch(Dispatchers.IO) {
                        Timber.d("DiscoveryService: ARP table scan starting")
                        val arpEntries = arpScanner.readArpTable()
                        Timber.d("DiscoveryService: ARP found ${arpEntries.size} hosts")

                        portProber.probeBatch(
                            ipAddresses = arpEntries.map { it.ipAddress },
                            timeoutMs   = config.timeoutPerHostMs,
                            maxParallel = config.parallelProbes
                        ) { probeResult ->
                            val arpEntry = arpEntries.firstOrNull { it.ipAddress == probeResult.ipAddress }
                            val confidence = if (arpEntry?.vendor != null)
                                DiscoveryConfidence.PROBABLE else DiscoveryConfidence.POSSIBLE

                            val device = DiscoveredDevice(
                                ipAddress         = probeResult.ipAddress,
                                hostname          = null,
                                port              = probeResult.primaryPort,
                                probableSourceType = probeResult.probableSourceType,
                                openPorts         = probeResult.openPorts,
                                banner            = probeResult.banner,
                                discoveryMethod   = DiscoveryMethod.ARP_TABLE,
                                confidence        = confidence,
                                macAddress        = arpEntry?.macAddress,
                                macVendor         = arpEntry?.vendor
                            )
                            fastFoundHosts.add(probeResult.ipAddress)
                            send(device)
                        }
                    }
                }

                // ── Strategy 2: mDNS ──────────────────────────────────────
                if (config.useMdns) {
                    launch(Dispatchers.IO) {
                        Timber.d("DiscoveryService: mDNS scan starting")
                        mdnsDiscovery.discover(config.mdnsTimeoutMs).collect { mdnsCamera ->
                            val device = DiscoveredDevice(
                                ipAddress         = mdnsCamera.host,
                                hostname          = mdnsCamera.serviceName,
                                port              = mdnsCamera.port,
                                probableSourceType = inferTypeFromMdns(mdnsCamera),
                                openPorts         = listOf(mdnsCamera.port),
                                banner            = mdnsCamera.serviceType,
                                discoveryMethod   = DiscoveryMethod.MDNS,
                                confidence        = DiscoveryConfidence.PROBABLE,
                                mdnsServiceName   = mdnsCamera.serviceName
                            )
                            fastFoundHosts.add(mdnsCamera.host)
                            send(device)
                        }
                        Timber.d("DiscoveryService: mDNS scan complete")
                    }
                }

                // ── Strategy 3: ONVIF WS-Discovery ───────────────────────
                if (config.useOnvifWsDiscovery) {
                    launch(Dispatchers.IO) {
                        Timber.d("DiscoveryService: ONVIF WS-Discovery starting")
                        onvifDiscovery.probe(config.onvifTimeoutMs).collect { match ->
                            val ip = match.sourceIp.ifBlank {
                                match.xAddrs.firstOrNull()
                                    ?.removePrefix("http://")
                                    ?.substringBefore(":")
                                    ?.substringBefore("/")
                                    ?: return@collect
                            }

                            val device = DiscoveredDevice(
                                ipAddress         = ip,
                                hostname          = match.model ?: match.endpointRef,
                                port              = 80,
                                probableSourceType = com.sentinel.app.domain.model.CameraSourceType.ONVIF,
                                openPorts         = listOf(80, 554),
                                banner            = listOfNotNull(match.manufacturer, match.model).joinToString(" "),
                                discoveryMethod   = DiscoveryMethod.ONVIF_WS_DISCOVERY,
                                confidence        = DiscoveryConfidence.CONFIRMED,
                                onvifManufacturer = match.manufacturer,
                                onvifModel        = match.model,
                                onvifXAddrs       = match.xAddrs
                            )
                            fastFoundHosts.add(ip)
                            send(device)
                        }
                        Timber.d("DiscoveryService: ONVIF WS-Discovery complete")
                    }
                }

                // ── Strategy 4: TCP Subnet Probe (fallback) ───────────────
                // Only runs hosts not already found by faster strategies
                if (config.useTcpProbe && config.tcpProbeSubnet) {
                    launch(Dispatchers.IO) {
                        // Give fast strategies a head start
                        kotlinx.coroutines.delay(1_000)

                        val allHosts = (1..254).map { "$subnet.$it" }
                        val unprobed = allHosts.filter { it !in fastFoundHosts }
                        Timber.d("DiscoveryService: TCP probe for ${unprobed.size} unprobed hosts")

                        portProber.probeBatch(
                            ipAddresses = unprobed,
                            timeoutMs   = config.timeoutPerHostMs,
                            maxParallel = config.parallelProbes
                        ) { probeResult ->
                            if (probeResult.ipAddress !in fastFoundHosts) {
                                val device = DiscoveredDevice(
                                    ipAddress         = probeResult.ipAddress,
                                    hostname          = null,
                                    port              = probeResult.primaryPort,
                                    probableSourceType = probeResult.probableSourceType,
                                    openPorts         = probeResult.openPorts,
                                    banner            = probeResult.banner,
                                    discoveryMethod   = DiscoveryMethod.TCP_PORT_PROBE,
                                    confidence        = DiscoveryConfidence.POSSIBLE
                                )
                                send(device)
                            }
                        }
                        Timber.d("DiscoveryService: TCP probe complete")
                    }
                }
            }

            _isScanning.value = false
            Timber.i("DiscoveryService: all strategies complete")
        }.flowOn(Dispatchers.IO)

    override suspend fun cancelScan() {
        _isScanning.value = false
        Timber.d("DiscoveryService: scan cancelled")
    }

    override suspend fun probeDevice(ipAddress: String): DiscoveredDevice? {
        val result = portProber.probeHost(ipAddress, timeoutMs = 1_500) ?: return null
        val arpEntry = arpScanner.readArpTable().firstOrNull { it.ipAddress == ipAddress }
        return DiscoveredDevice(
            ipAddress         = ipAddress,
            hostname          = null,
            port              = result.primaryPort,
            probableSourceType = result.probableSourceType,
            openPorts         = result.openPorts,
            banner            = result.banner,
            discoveryMethod   = DiscoveryMethod.TCP_PORT_PROBE,
            confidence        = DiscoveryConfidence.POSSIBLE,
            macAddress        = arpEntry?.macAddress,
            macVendor         = arpEntry?.vendor
        )
    }

    override fun detectLocalSubnet(): String? = subnetDetector.detectSubnet()

    override fun checkDiscoveryCapabilities(): DiscoveryCapabilities {
        val subnet = subnetDetector.detectSubnet()
        val arpReadable = try {
            java.io.File("/proc/net/arp").canRead()
        } catch (_: Exception) { false }

        return DiscoveryCapabilities(
            wifiConnected      = subnetDetector.isWifiConnected(),
            multicastAvailable = subnetDetector.isWifiConnected(), // Multicast requires WiFi
            arpTableReadable   = arpReadable,
            nsdAvailable       = true, // NsdManager always available on Android 4.1+
            detectedSubnet     = subnet
        )
    }

    private fun inferTypeFromMdns(
        camera: MdnsDiscovery.MdnsCamera
    ): com.sentinel.app.domain.model.CameraSourceType = when {
        camera.serviceType.contains("rtsp", ignoreCase = true) ->
            com.sentinel.app.domain.model.CameraSourceType.RTSP
        camera.serviceName.contains("IP Webcam", ignoreCase = true) ->
            com.sentinel.app.domain.model.CameraSourceType.ANDROID_IPWEBCAM
        camera.serviceName.contains("DroidCam", ignoreCase = true) ->
            com.sentinel.app.domain.model.CameraSourceType.ANDROID_DROIDCAM
        camera.serviceType.contains("axis", ignoreCase = true) ->
            com.sentinel.app.domain.model.CameraSourceType.ONVIF
        camera.port == 4747 ->
            com.sentinel.app.domain.model.CameraSourceType.ANDROID_DROIDCAM
        camera.port == 8080 ->
            com.sentinel.app.domain.model.CameraSourceType.ANDROID_IPWEBCAM
        else ->
            com.sentinel.app.domain.model.CameraSourceType.GENERIC_URL
    }
}
