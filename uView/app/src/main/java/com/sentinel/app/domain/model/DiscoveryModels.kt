package com.sentinel.app.domain.model

// ─────────────────────────────────────────────────────────────────────────────
// Discovery domain models — Phase 4
// ─────────────────────────────────────────────────────────────────────────────

/**
 * How a device was found. Multiple strategies may find the same device.
 * The UI uses this to show the user how confident the detection is.
 */
enum class DiscoveryMethod {
    ARP_TABLE,          // Read from /proc/net/arp — fast, LAN-only
    MDNS,               // Android NsdManager — advertised by IP Webcam, some cameras
    ONVIF_WS_DISCOVERY, // UDP multicast to 239.255.255.250:3702 — real ONVIF devices
    TCP_PORT_PROBE,     // Fallback TCP connect probe
    MANUAL              // User entered manually
}

/**
 * Confidence level for a discovered device being a camera.
 */
enum class DiscoveryConfidence {
    CONFIRMED,    // ONVIF WS-Discovery or mDNS with camera service type
    PROBABLE,     // Known camera port (554, 4747, 8080) open with matching banner
    POSSIBLE,     // Has open ports in range but no camera-specific signal
    UNKNOWN       // Only responds to ping/ARP
}

/**
 * Full discovery result — extends the original [DiscoveredDevice] with
 * richer metadata from Phase 4 discovery methods.
 */
data class DiscoveredDevice(
    val ipAddress: String,
    val hostname: String?,
    val port: Int,
    val probableSourceType: CameraSourceType?,
    val openPorts: List<Int>,
    val banner: String?,
    val isAlreadyAdded: Boolean = false,
    // Phase 4 additions
    val discoveryMethod: DiscoveryMethod = DiscoveryMethod.TCP_PORT_PROBE,
    val confidence: DiscoveryConfidence = DiscoveryConfidence.POSSIBLE,
    val mdnsServiceName: String? = null,     // e.g. "IP Webcam._http._tcp.local."
    val onvifManufacturer: String? = null,   // from WS-Discovery ProbeMatch
    val onvifModel: String? = null,
    val onvifXAddrs: List<String> = emptyList(), // ONVIF device service URLs
    val macAddress: String? = null,          // from ARP table
    val macVendor: String? = null,           // OUI lookup result
    val discoveredAt: Long = System.currentTimeMillis()
)

/**
 * Result of a full network scan — aggregates findings from all strategies.
 */
data class ScanResult(
    val devices: List<DiscoveredDevice>,
    val subnetScanned: String,
    val durationMs: Long,
    val strategiesUsed: Set<DiscoveryMethod>,
    val completedAt: Long = System.currentTimeMillis()
)

/**
 * Configuration for a discovery scan run.
 */
data class ScanConfig(
    val subnet: String? = null,          // null = auto-detect from WiFi
    val timeoutPerHostMs: Int = 500,
    val parallelProbes: Int = 50,        // concurrent TCP probes
    val useArpTable: Boolean = true,
    val useMdns: Boolean = true,
    val useOnvifWsDiscovery: Boolean = true,
    val useTcpProbe: Boolean = true,
    val tcpProbeSubnet: Boolean = true,  // whether to TCP-probe the whole subnet
    val mdnsTimeoutMs: Long = 4_000L,
    val onvifTimeoutMs: Long = 3_000L
)

/**
 * OUI vendor lookup result from MAC address prefix.
 */
data class OuiEntry(val prefix: String, val vendor: String)

/**
 * Known camera-related MAC OUI prefixes for vendor identification.
 * This is a small curated set — a full OUI database would be 50k+ entries.
 */
val KNOWN_CAMERA_OUI_PREFIXES: Map<String, String> = mapOf(
    "00:40:8C" to "Axis Communications",
    "AC:CC:8E" to "Axis Communications",
    "B8:A4:4F" to "Hikvision",
    "C0:56:E3" to "Hikvision",
    "28:57:BE" to "Hikvision",
    "D4:E8:53" to "Dahua Technology",
    "E0:50:8B" to "Dahua Technology",
    "A4:14:37" to "Reolink",
    "EC:71:DB" to "Amcrest",
    "00:12:17" to "Amcrest",
    "00:1A:8A" to "Foscam",
    "C4:D6:55" to "TP-Link (cameras)",
    "B0:BE:76" to "Annke"
)
