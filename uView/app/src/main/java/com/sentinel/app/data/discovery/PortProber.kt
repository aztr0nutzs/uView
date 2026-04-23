package com.sentinel.app.data.discovery

import com.sentinel.app.domain.model.CameraSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PortProber
 *
 * Performs parallel TCP port probing on a list of IP addresses.
 *
 * Strategy:
 *   1. For each host, probe all [CAMERA_PORTS] concurrently.
 *   2. If any port is open, fetch the HTTP banner (if applicable).
 *   3. Infer the probable camera type from the open ports and banner.
 *
 * Parallelism:
 *   - Up to [maxParallel] concurrent probes across all hosts.
 *   - Per-host probes are concurrent within coroutineScope.
 *   - Uses Dispatchers.IO for all blocking socket operations.
 *
 * Performance:
 *   - With 50 parallel probes and 500ms timeout, a /24 subnet (254 hosts)
 *     completes in roughly 3–5 seconds (vs 127 seconds sequentially).
 */
@Singleton
class PortProber @Inject constructor() {

    companion object {
        /**
         * Camera-relevant ports to probe, ordered by detection priority.
         * Each entry: port → (label, source type hint, is HTTP)
         */
        val CAMERA_PORTS: List<CameraPort> = listOf(
            CameraPort(554,   "RTSP",              CameraSourceType.RTSP,             false),
            CameraPort(4747,  "DroidCam",           CameraSourceType.ANDROID_DROIDCAM, true),
            CameraPort(8080,  "IP Webcam / HTTP",   CameraSourceType.ANDROID_IPWEBCAM, true),
            CameraPort(8081,  "MJPEG-Alt",          CameraSourceType.MJPEG,            true),
            CameraPort(80,    "HTTP",               CameraSourceType.MJPEG,            true),
            CameraPort(443,   "HTTPS",              CameraSourceType.GENERIC_URL,      false),
            CameraPort(37777, "Dahua DVR",          CameraSourceType.RTSP,             false),
            CameraPort(8000,  "Hikvision",          CameraSourceType.RTSP,             true),
            CameraPort(34599, "Hikvision Stream",   CameraSourceType.RTSP,             false),
            CameraPort(9000,  "Generic Camera",     CameraSourceType.GENERIC_URL,      true),
            CameraPort(2020,  "Foscam",             CameraSourceType.RTSP,             false),
            CameraPort(1935,  "RTMP",               CameraSourceType.GENERIC_URL,      false)
        )

        /** HTTP banner paths to try when an HTTP port is open. */
        val HTTP_BANNER_PATHS = listOf("/", "/video", "/index.html")
    }

    data class CameraPort(
        val port: Int,
        val label: String,
        val probableType: CameraSourceType,
        val isHttp: Boolean
    )

    data class ProbeResult(
        val ipAddress: String,
        val openPorts: List<Int>,
        val primaryPort: Int,
        val probableSourceType: CameraSourceType?,
        val banner: String?,
        val isHttpServer: Boolean
    )

    /**
     * Probe a single host across all [CAMERA_PORTS] concurrently.
     * Returns null if no camera-relevant ports are open.
     *
     * @param timeoutMs  Per-port connect timeout in milliseconds.
     */
    suspend fun probeHost(ipAddress: String, timeoutMs: Int = 500): ProbeResult? =
        withContext(Dispatchers.IO) {
            coroutineScope {
                // Probe all ports in parallel
                val portResults = CAMERA_PORTS.map { cameraPort ->
                    async {
                        val open = tcpConnect(ipAddress, cameraPort.port, timeoutMs)
                        Pair(cameraPort, open)
                    }
                }.awaitAll()

                val openPorts = portResults.filter { it.second }.map { it.first }
                if (openPorts.isEmpty()) return@coroutineScope null

                val primary   = openPorts.first()
                val openNums  = openPorts.map { it.port }

                // Try to fetch HTTP banner from first open HTTP port
                val banner = openPorts
                    .firstOrNull { it.isHttp }
                    ?.let { fetchHttpBanner(ipAddress, it.port) }

                val probableType = inferSourceType(openPorts, banner)

                ProbeResult(
                    ipAddress         = ipAddress,
                    openPorts         = openNums,
                    primaryPort       = primary.port,
                    probableSourceType = probableType,
                    banner            = banner,
                    isHttpServer      = openPorts.any { it.isHttp }
                )
            }
        }

    /**
     * Probe a batch of IP addresses in parallel, bounded by [maxParallel].
     * Emits results as they arrive via the callback.
     */
    suspend fun probeBatch(
        ipAddresses: List<String>,
        timeoutMs: Int = 500,
        maxParallel: Int = 50,
        onResult: suspend (ProbeResult) -> Unit
    ) = withContext(Dispatchers.IO) {
        val semaphore = kotlinx.coroutines.sync.Semaphore(maxParallel)
        coroutineScope {
            ipAddresses.map { ip ->
                async {
                    semaphore.acquire()
                    try {
                        val result = probeHost(ip, timeoutMs)
                        if (result != null) onResult(result)
                    } finally {
                        semaphore.release()
                    }
                }
            }.awaitAll()
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // TCP connect probe
    // ─────────────────────────────────────────────────────────────────────

    private fun tcpConnect(host: String, port: Int, timeoutMs: Int): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            true
        }
    } catch (_: Exception) {
        false
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTTP banner fetch
    // ─────────────────────────────────────────────────────────────────────

    private fun fetchHttpBanner(host: String, port: Int): String? {
        for (path in HTTP_BANNER_PATHS) {
            try {
                val url  = "http://$host:$port$path"
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 1_500
                conn.readTimeout    = 1_500
                conn.requestMethod  = "GET"
                conn.instanceFollowRedirects = false

                val responseCode = conn.responseCode
                val server       = conn.getHeaderField("Server")
                val contentType  = conn.contentType ?: ""
                conn.disconnect()

                // Build banner string from HTTP headers
                val parts = mutableListOf<String>()
                server?.let { parts.add(it) }
                if (contentType.contains("multipart", ignoreCase = true)) {
                    parts.add("MJPEG")
                }
                if (responseCode == 200 || responseCode == 401) {
                    return parts.joinToString(" | ").ifBlank { "HTTP $responseCode" }
                }
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    // ─────────────────────────────────────────────────────────────────────
    // Source type inference from probe results
    // ─────────────────────────────────────────────────────────────────────

    private fun inferSourceType(
        openPorts: List<CameraPort>,
        banner: String?
    ): CameraSourceType? {
        // Check banner for strong signals
        banner?.let { b ->
            when {
                b.contains("IP Webcam", ignoreCase = true)  -> return CameraSourceType.ANDROID_IPWEBCAM
                b.contains("DroidCam", ignoreCase = true)   -> return CameraSourceType.ANDROID_DROIDCAM
                b.contains("Hikvision", ignoreCase = true)  -> return CameraSourceType.RTSP
                b.contains("Dahua", ignoreCase = true)      -> return CameraSourceType.RTSP
                b.contains("Axis", ignoreCase = true)       -> return CameraSourceType.ONVIF
                b.contains("MJPEG", ignoreCase = true)      -> return CameraSourceType.MJPEG
            }
        }

        // Fall back to port-based inference — use most specific first
        return openPorts.firstOrNull()?.probableType
    }
}
