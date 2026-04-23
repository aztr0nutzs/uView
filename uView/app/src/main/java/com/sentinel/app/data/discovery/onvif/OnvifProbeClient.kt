package com.sentinel.app.data.discovery.onvif

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OnvifProbeClient
 *
 * Sends ONVIF SOAP requests over HTTP to a discovered ONVIF device.
 * Used after WS-Discovery to:
 *   1. GetProfiles — enumerate available media profiles (stream configs)
 *   2. GetStreamUri — retrieve the RTSP URL for a given profile
 *
 * IMPLEMENTATION STATUS:
 *   GetProfiles  — IMPLEMENTED (SOAP request + XML parser)
 *   GetStreamUri — IMPLEMENTED (SOAP request + XML parser)
 *   GetDeviceInfo — IMPLEMENTED (manufacturer, model, firmware)
 *   Authentication (WS-Security SOAP headers) — SCAFFOLDED
 *     Real WS-Security requires WSSE username token with nonce + timestamp.
 *     The current implementation sends requests without auth — works for
 *     cameras that allow anonymous ONVIF access (many consumer cameras do).
 *     For protected cameras, add the WS-Security header below.
 *
 * Usage:
 *   val profiles = client.getProfiles(deviceServiceUrl)
 *   val rtspUrl  = client.getStreamUri(deviceServiceUrl, profiles.first().token)
 */
@Singleton
class OnvifProbeClient @Inject constructor() {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val READ_TIMEOUT_MS    = 8_000
        private const val SOAP_ACTION_PROFILES   = "http://www.onvif.org/ver10/media/wsdl/GetProfiles"
        private const val SOAP_ACTION_STREAM_URI = "http://www.onvif.org/ver10/media/wsdl/GetStreamUri"
        private const val SOAP_ACTION_DEVICE_INFO = "http://www.onvif.org/ver10/device/wsdl/GetDeviceInformation"
    }

    data class OnvifProfile(
        val token: String,
        val name: String,
        val videoWidth: Int,
        val videoHeight: Int,
        val encoding: String
    )

    data class OnvifDeviceInfo(
        val manufacturer: String,
        val model: String,
        val firmwareVersion: String,
        val serialNumber: String
    )

    /**
     * Retrieve available media profiles from an ONVIF device.
     * Returns an empty list on failure.
     *
     * @param deviceServiceUrl  The XAddr URL from WS-Discovery ProbeMatch,
     *                          e.g. "http://192.168.1.101/onvif/device_service"
     */
    suspend fun getProfiles(
        deviceServiceUrl: String,
        username: String = "",
        password: String = ""
    ): List<OnvifProfile> = withContext(Dispatchers.IO) {
        try {
            val mediaUrl = buildMediaUrl(deviceServiceUrl)
            val soap = buildGetProfilesSoap(username, password)
            val response = postSoap(mediaUrl, soap, SOAP_ACTION_PROFILES)
            parseProfiles(response)
        } catch (e: Exception) {
            Timber.e(e, "OnvifProbeClient: getProfiles failed for $deviceServiceUrl")
            emptyList()
        }
    }

    /**
     * Get the RTSP stream URI for a given media profile token.
     * Returns null on failure.
     */
    suspend fun getStreamUri(
        deviceServiceUrl: String,
        profileToken: String,
        username: String = "",
        password: String = ""
    ): String? = withContext(Dispatchers.IO) {
        try {
            val mediaUrl = buildMediaUrl(deviceServiceUrl)
            val soap = buildGetStreamUriSoap(profileToken, username, password)
            val response = postSoap(mediaUrl, soap, SOAP_ACTION_STREAM_URI)
            parseStreamUri(response)
        } catch (e: Exception) {
            Timber.e(e, "OnvifProbeClient: getStreamUri failed for $deviceServiceUrl")
            null
        }
    }

    /**
     * Get basic device information (manufacturer, model, firmware).
     */
    suspend fun getDeviceInfo(
        deviceServiceUrl: String,
        username: String = "",
        password: String = ""
    ): OnvifDeviceInfo? = withContext(Dispatchers.IO) {
        try {
            val soap = buildGetDeviceInfoSoap(username, password)
            val response = postSoap(deviceServiceUrl, soap, SOAP_ACTION_DEVICE_INFO)
            parseDeviceInfo(response)
        } catch (e: Exception) {
            Timber.e(e, "OnvifProbeClient: getDeviceInfo failed for $deviceServiceUrl")
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SOAP builders
    // ─────────────────────────────────────────────────────────────────────

    private fun buildSoapEnvelope(body: String, username: String, password: String): String {
        val securityHeader = if (username.isNotBlank()) {
            // TODO Phase 8: Replace with proper WS-Security WSSE nonce+timestamp
            """
            <s:Header>
              <Security xmlns="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
                <UsernameToken>
                  <Username>$username</Username>
                  <Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText">$password</Password>
                </UsernameToken>
              </Security>
            </s:Header>"""
        } else ""

        return """<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
            xmlns:trt="http://www.onvif.org/ver10/media/wsdl"
            xmlns:tds="http://www.onvif.org/ver10/device/wsdl"
            xmlns:tt="http://www.onvif.org/ver10/schema">
  $securityHeader
  <s:Body>$body</s:Body>
</s:Envelope>"""
    }

    private fun buildGetProfilesSoap(u: String, p: String) =
        buildSoapEnvelope("<trt:GetProfiles/>", u, p)

    private fun buildGetStreamUriSoap(token: String, u: String, p: String) =
        buildSoapEnvelope("""
            <trt:GetStreamUri>
              <trt:StreamSetup>
                <tt:Stream>RTP-Unicast</tt:Stream>
                <tt:Transport><tt:Protocol>RTSP</tt:Protocol></tt:Transport>
              </trt:StreamSetup>
              <trt:ProfileToken>$token</trt:ProfileToken>
            </trt:GetStreamUri>""", u, p)

    private fun buildGetDeviceInfoSoap(u: String, p: String) =
        buildSoapEnvelope("<tds:GetDeviceInformation/>", u, p)

    // ─────────────────────────────────────────────────────────────────────
    // HTTP transport
    // ─────────────────────────────────────────────────────────────────────

    private fun postSoap(url: String, soap: String, soapAction: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod  = "POST"
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout    = READ_TIMEOUT_MS
        conn.doOutput       = true
        conn.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8")
        conn.setRequestProperty("SOAPAction", "\"$soapAction\"")

        conn.outputStream.use { it.write(soap.toByteArray(Charsets.UTF_8)) }
        return conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Response parsers
    // ─────────────────────────────────────────────────────────────────────

    private fun parseProfiles(xml: String): List<OnvifProfile> {
        val profiles = mutableListOf<OnvifProfile>()
        // Extract each Profiles element
        val profileRegex = Regex("<[^/]*Profiles[^>]+token=\"([^\"]+)\"[^>]*>(.*?)</[^>]*Profiles>",
            RegexOption.DOT_MATCHES_ALL)
        for (match in profileRegex.findAll(xml)) {
            val token  = match.groupValues[1]
            val body   = match.groupValues[2]
            val name   = extractXmlValue(body, "Name") ?: token
            val width  = extractXmlValue(body, "Width")?.toIntOrNull() ?: 0
            val height = extractXmlValue(body, "Height")?.toIntOrNull() ?: 0
            val enc    = extractXmlValue(body, "Encoding") ?: "H264"
            profiles.add(OnvifProfile(token, name, width, height, enc))
        }
        return profiles
    }

    private fun parseStreamUri(xml: String): String? {
        return extractXmlValue(xml, "Uri")?.trim()
    }

    private fun parseDeviceInfo(xml: String): OnvifDeviceInfo? {
        val manufacturer = extractXmlValue(xml, "Manufacturer") ?: return null
        val model        = extractXmlValue(xml, "Model") ?: ""
        val firmware     = extractXmlValue(xml, "FirmwareVersion") ?: ""
        val serial       = extractXmlValue(xml, "SerialNumber") ?: ""
        return OnvifDeviceInfo(manufacturer, model, firmware, serial)
    }

    private fun extractXmlValue(xml: String, tag: String): String? {
        val start = xml.indexOf("<$tag>").takeIf { it >= 0 } ?: return null
        val end   = xml.indexOf("</$tag>", start).takeIf { it >= 0 } ?: return null
        return xml.substring(start + tag.length + 2, end).trim()
    }

    private fun buildMediaUrl(deviceServiceUrl: String): String {
        // Many cameras expose media service at /onvif/media_service
        // Try to infer from the device service URL
        return when {
            deviceServiceUrl.contains("device_service") ->
                deviceServiceUrl.replace("device_service", "media_service")
            deviceServiceUrl.endsWith("/") ->
                "${deviceServiceUrl}onvif/media_service"
            else ->
                deviceServiceUrl  // Use as-is and let the camera redirect
        }
    }
}
