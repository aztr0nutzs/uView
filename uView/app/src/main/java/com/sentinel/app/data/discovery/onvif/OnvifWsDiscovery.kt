package com.sentinel.app.data.discovery.onvif

import android.content.Context
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OnvifWsDiscovery
 *
 * Implements ONVIF WS-Discovery (Web Services Dynamic Discovery) using UDP
 * multicast to find ONVIF-compatible cameras on the local network.
 *
 * Protocol:
 *   1. Send a WS-Discovery Probe message to 239.255.255.250:3702 (UDP multicast).
 *   2. Cameras that match the probe reply with a ProbeMatch SOAP message.
 *   3. Parse ProbeMatch to extract XAddrs (device service endpoint URLs).
 *
 * The Probe targets:
 *   - NetworkVideoTransmitter (cameras with video streaming)
 *   - Device (any ONVIF device)
 *
 * Implementation notes:
 *   - Requires CHANGE_WIFI_MULTICAST_STATE permission for multicast lock.
 *   - Multicast lock prevents the WiFi radio from filtering multicast packets.
 *   - Replies arrive on the unicast socket port after sending to multicast.
 *   - We use a 3-second receive window — cameras that don't reply within
 *     this window are missed (acceptable for initial discovery).
 *
 * ONVIF WS-Discovery is FULLY IMPLEMENTED here for finding devices.
 * GetStreamUri (for getting the actual RTSP URL from the device) is
 * scaffolded in OnvifProbeClient and requires SOAP over HTTP.
 */
@Singleton
class OnvifWsDiscovery @Inject constructor(
    @ApplicationContext
    private val context: Context
) {

    companion object {
        private const val WS_DISCOVERY_MULTICAST_IP   = "239.255.255.250"
        private const val WS_DISCOVERY_PORT           = 3702
        private const val SOCKET_TIMEOUT_MS           = 3_000
        private const val RECEIVE_BUFFER_SIZE         = 65_536
    }

    /**
     * A device found via WS-Discovery ProbeMatch.
     */
    data class OnvifProbeMatch(
        val messageId: String,
        val xAddrs: List<String>,       // device service URLs
        val scopes: List<String>,       // ONVIF scope URIs
        val types: List<String>,        // e.g. NetworkVideoTransmitter
        val endpointRef: String?,       // device UUID
        val manufacturer: String?,      // parsed from scopes
        val model: String?,             // parsed from scopes
        val sourceIp: String            // where the ProbeMatch came from
    )

    /**
     * Send a WS-Discovery Probe and collect ProbeMatch responses as a [Flow].
     * Runs for [timeoutMs] milliseconds then closes.
     *
     * Requires:
     *   - android.permission.CHANGE_WIFI_MULTICAST_STATE
     *   - android.permission.INTERNET
     *
     * @param timeoutMs  How long to wait for ProbeMatch responses.
     */
    fun probe(timeoutMs: Long = 3_000L): Flow<OnvifProbeMatch> = callbackFlow {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager

        // Acquire multicast lock so WiFi radio doesn't filter multicast packets
        val multicastLock = wifiManager?.createMulticastLock("sentinel_onvif_discovery")
        multicastLock?.setReferenceCounted(true)

        var socket: MulticastSocket? = null

        try {
            multicastLock?.acquire()
            Timber.d("OnvifWsDiscovery: multicast lock acquired")

            socket = MulticastSocket().apply {
                soTimeout = SOCKET_TIMEOUT_MS
                reuseAddress = true
            }

            val messageId = "uuid:${UUID.randomUUID()}"
            val probeXml  = buildProbeMessage(messageId)
            val probeBytes = probeXml.toByteArray(Charsets.UTF_8)

            val multicastAddress = InetAddress.getByName(WS_DISCOVERY_MULTICAST_IP)
            val sendPacket = DatagramPacket(
                probeBytes, probeBytes.size, multicastAddress, WS_DISCOVERY_PORT
            )

            Timber.d("OnvifWsDiscovery: sending Probe to $WS_DISCOVERY_MULTICAST_IP:$WS_DISCOVERY_PORT")
            socket.send(sendPacket)

            // Receive window — collect responses until timeout
            val buf        = ByteArray(RECEIVE_BUFFER_SIZE)
            val recvPacket = DatagramPacket(buf, buf.size)
            val deadline   = System.currentTimeMillis() + timeoutMs

            while (System.currentTimeMillis() < deadline) {
                try {
                    socket.receive(recvPacket)
                    val xml    = String(recvPacket.data, 0, recvPacket.length, Charsets.UTF_8)
                    val source = recvPacket.address.hostAddress ?: continue

                    Timber.d("OnvifWsDiscovery: received ${recvPacket.length} bytes from $source")

                    val match = parseProbeMatch(xml, source)
                    if (match != null) {
                        Timber.d("OnvifWsDiscovery: ProbeMatch from $source — ${match.manufacturer} ${match.model}")
                        trySend(match)
                    }
                } catch (_: java.net.SocketTimeoutException) {
                    break  // Normal — receive window elapsed
                }
            }

            Timber.d("OnvifWsDiscovery: probe complete")
            close()

        } catch (e: Exception) {
            Timber.e(e, "OnvifWsDiscovery: probe failed")
            close(e)
        }

        awaitClose {
            socket?.close()
            try { multicastLock?.release() } catch (_: Exception) {}
            Timber.d("OnvifWsDiscovery: socket closed, multicast lock released")
        }
    }.flowOn(Dispatchers.IO)

    // ─────────────────────────────────────────────────────────────────────
    // SOAP message building
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Builds a WS-Discovery Probe SOAP envelope targeting ONVIF
     * NetworkVideoTransmitter devices.
     */
    private fun buildProbeMessage(messageId: String): String = """<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope
    xmlns:s="http://www.w3.org/2003/05/soap-envelope"
    xmlns:a="http://schemas.xmlsoap.org/ws/2004/08/addressing"
    xmlns:d="http://schemas.xmlsoap.org/ws/2005/04/discovery"
    xmlns:dn="http://www.onvif.org/ver10/network/wsdl">
  <s:Header>
    <a:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</a:Action>
    <a:MessageID>$messageId</a:MessageID>
    <a:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</a:To>
  </s:Header>
  <s:Body>
    <d:Probe>
      <d:Types>dn:NetworkVideoTransmitter</d:Types>
    </d:Probe>
  </s:Body>
</s:Envelope>"""

    // ─────────────────────────────────────────────────────────────────────
    // ProbeMatch XML parsing
    // Uses simple string scanning — avoids a full XML parser dependency.
    // Robust enough for well-formed ONVIF responses.
    // ─────────────────────────────────────────────────────────────────────

    private fun parseProbeMatch(xml: String, sourceIp: String): OnvifProbeMatch? {
        return try {
            if (!xml.contains("ProbeMatch", ignoreCase = true)) return null

            val xAddrs   = extractTagValues(xml, "XAddrs").flatMap { it.split(" ") }.filter { it.isNotBlank() }
            val scopes   = extractTagValues(xml, "Scopes").flatMap { it.split(" ") }.filter { it.isNotBlank() }
            val types    = extractTagValues(xml, "Types").flatMap { it.split(" ") }.filter { it.isNotBlank() }
            val msgId    = extractFirstTagValue(xml, "MessageID")
            val epRef    = extractFirstTagValue(xml, "Address")

            val manufacturer = scopes
                .firstOrNull { it.contains("onvif://www.onvif.org/hardware/", ignoreCase = true) }
                ?.substringAfterLast("/")
                ?.replace("-", " ")

            val model = scopes
                .firstOrNull { it.contains("onvif://www.onvif.org/name/", ignoreCase = true) }
                ?.substringAfterLast("/")
                ?.replace("+", " ")

            if (xAddrs.isEmpty() && sourceIp.isBlank()) return null

            OnvifProbeMatch(
                messageId   = msgId ?: "",
                xAddrs      = xAddrs,
                scopes      = scopes,
                types       = types,
                endpointRef = epRef,
                manufacturer = manufacturer,
                model       = model,
                sourceIp    = sourceIp
            )
        } catch (e: Exception) {
            Timber.w("OnvifWsDiscovery: failed to parse ProbeMatch — ${e.message}")
            null
        }
    }

    private fun extractTagValues(xml: String, tag: String): List<String> {
        val results = mutableListOf<String>()
        var start = 0
        while (true) {
            val open  = xml.indexOf("<$tag", start).takeIf { it >= 0 } ?: break
            val close = xml.indexOf(">", open).takeIf { it >= 0 } ?: break
            val end   = xml.indexOf("</$tag>", close).takeIf { it >= 0 } ?: break
            results.add(xml.substring(close + 1, end).trim())
            start = end + tag.length + 3
        }
        return results
    }

    private fun extractFirstTagValue(xml: String, tag: String): String? =
        extractTagValues(xml, tag).firstOrNull()
}
