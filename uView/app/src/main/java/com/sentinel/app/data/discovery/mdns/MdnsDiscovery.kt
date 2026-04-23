package com.sentinel.app.data.discovery.mdns

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MdnsDiscovery
 *
 * Uses Android's [NsdManager] (Network Service Discovery) to find devices
 * advertising camera-related mDNS services on the local network.
 *
 * Camera apps and devices that advertise via mDNS:
 *
 *   IP Webcam (Android)  → _http._tcp  (path /video)
 *   Some IP cameras      → _rtsp._tcp
 *   Some ONVIF cameras   → _onvif._tcp  (non-standard, rare)
 *   Axis cameras         → _axis-video._tcp
 *   Bosch cameras        → _nvp._tcp
 *
 * NsdManager behaviour notes:
 *   - Discovery callbacks arrive on arbitrary threads — we marshal results
 *     back to the Flow safely.
 *   - NsdManager.resolveService is serialized on older Android versions.
 *     We queue resolve requests to avoid "listener already in use" errors.
 *   - The flow completes when [stopDiscovery] is called or timeout elapses.
 *   - IMPLEMENTATION: NsdManager is fully functional. IP Webcam and any
 *     device using _http._tcp or _rtsp._tcp will be detected.
 */
@Singleton
class MdnsDiscovery @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        /** Service types to search for, in order of priority. */
        val CAMERA_SERVICE_TYPES = listOf(
            "_rtsp._tcp.",
            "_http._tcp.",
            "_axis-video._tcp.",
            "_onvif._tcp.",
            "_nvp._tcp."
        )

        /** HTTP service paths that indicate an MJPEG or camera source. */
        val CAMERA_HTTP_PATHS = setOf("/video", "/mjpeg", "/stream", "/cam", "/live")

        /** Default ports for each service type. */
        val SERVICE_TYPE_DEFAULT_PORTS = mapOf(
            "_rtsp._tcp."        to 554,
            "_http._tcp."        to 8080,
            "_axis-video._tcp."  to 80,
            "_onvif._tcp."       to 80,
            "_nvp._tcp."         to 80
        )
    }

    /**
     * Resolved mDNS service information — camera-specific enrichment over
     * the raw [NsdServiceInfo].
     */
    data class MdnsCamera(
        val serviceName: String,
        val serviceType: String,
        val host: String,
        val port: Int,
        val txtRecords: Map<String, String>,
        val probableCameraPath: String?
    )

    /**
     * Start mDNS discovery for all [CAMERA_SERVICE_TYPES] simultaneously.
     * Emits [MdnsCamera] instances as they are discovered and resolved.
     *
     * The flow is cold — discovery starts on collection and stops on
     * cancellation.
     *
     * @param timeoutMs  How long to listen before the flow completes.
     *                   Pass [Long.MAX_VALUE] to listen indefinitely.
     */
    fun discover(timeoutMs: Long = 4_000L): Flow<MdnsCamera> = callbackFlow {
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
        if (nsdManager == null) {
            Timber.w("MdnsDiscovery: NsdManager not available")
            close()
            return@callbackFlow
        }

        val listeners = mutableListOf<NsdManager.DiscoveryListener>()
        // Resolve requests must be serialized on some Android versions
        val resolveQueue = java.util.concurrent.LinkedBlockingQueue<NsdServiceInfo>()
        var isResolving  = false

        fun processResolveQueue() {
            if (isResolving) return
            val next = resolveQueue.poll() ?: return
            isResolving = true

            nsdManager.resolveService(next, object : NsdManager.ResolveListener {
                override fun onServiceResolved(info: NsdServiceInfo) {
                    isResolving = false
                    val camera = buildMdnsCamera(info)
                    if (camera != null) {
                        trySend(camera)
                        Timber.d("MdnsDiscovery: resolved ${info.serviceName} → ${info.host?.hostAddress}:${info.port}")
                    }
                    processResolveQueue()
                }

                override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                    isResolving = false
                    Timber.w("MdnsDiscovery: resolve failed for ${info.serviceName} (error $errorCode)")
                    processResolveQueue()
                }
            })
        }

        // Start a discovery listener for each service type
        CAMERA_SERVICE_TYPES.forEach { serviceType ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(type: String, errorCode: Int) {
                    Timber.w("MdnsDiscovery: start failed for $type (error $errorCode)")
                }
                override fun onStopDiscoveryFailed(type: String, errorCode: Int) {
                    Timber.w("MdnsDiscovery: stop failed for $type (error $errorCode)")
                }
                override fun onDiscoveryStarted(type: String) {
                    Timber.d("MdnsDiscovery: started for $type")
                }
                override fun onDiscoveryStopped(type: String) {
                    Timber.d("MdnsDiscovery: stopped for $type")
                }
                override fun onServiceFound(info: NsdServiceInfo) {
                    Timber.d("MdnsDiscovery: found ${info.serviceName} (${info.serviceType})")
                    resolveQueue.offer(info)
                    processResolveQueue()
                }
                override fun onServiceLost(info: NsdServiceInfo) {
                    Timber.d("MdnsDiscovery: lost ${info.serviceName}")
                }
            }
            try {
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
                listeners.add(listener)
            } catch (e: Exception) {
                Timber.e(e, "MdnsDiscovery: failed to start discovery for $serviceType")
            }
        }

        // Auto-close after timeout
        if (timeoutMs != Long.MAX_VALUE) {
            kotlinx.coroutines.delay(timeoutMs)
            close()
        }

        awaitClose {
            listeners.forEach { listener ->
                try { nsdManager.stopServiceDiscovery(listener) } catch (_: Exception) {}
            }
            Timber.d("MdnsDiscovery: all listeners stopped")
        }
    }.flowOn(Dispatchers.IO)

    private fun buildMdnsCamera(info: NsdServiceInfo): MdnsCamera? {
        val host = info.host?.hostAddress ?: return null
        if (host.isBlank() || host == "0.0.0.0") return null

        val txtRecords = parseTxtRecords(info)
        val path = inferCameraPath(info.serviceType, txtRecords)

        return MdnsCamera(
            serviceName      = info.serviceName ?: "",
            serviceType      = info.serviceType ?: "",
            host             = host,
            port             = info.port,
            txtRecords       = txtRecords,
            probableCameraPath = path
        )
    }

    private fun parseTxtRecords(info: NsdServiceInfo): Map<String, String> {
        return try {
            @Suppress("UNCHECKED_CAST")
            val raw = info.attributes as? Map<String, ByteArray> ?: return emptyMap()
            raw.mapValues { (_, v) -> String(v, Charsets.UTF_8) }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun inferCameraPath(serviceType: String, txt: Map<String, String>): String? {
        // Check TXT record for path hint
        txt["path"]?.let { return it }
        txt["resource"]?.let { return it }
        // Infer from service type
        return when {
            serviceType.contains("rtsp", ignoreCase = true) -> "/"
            serviceType.contains("http", ignoreCase = true) -> "/video"
            serviceType.contains("axis", ignoreCase = true) -> "/axis-media/media.amp"
            else -> null
        }
    }
}
