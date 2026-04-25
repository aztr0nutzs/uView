package com.sentinel.companion.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class StreamProtocol(val label: String, val defaultPort: Int) {
    RTSP("RTSP", 554),
    MJPEG("MJPEG/HTTP", 8080),
    HLS("HLS", 8080),
    ONVIF("ONVIF", 80),
    DROIDCAM("DroidCam", 4747),
    IP_WEBCAM("IP Webcam", 8080),
    ALFRED("Alfred", 0),
    CUSTOM("Custom URL", 0),
}

enum class DeviceState(val label: String) {
    ONLINE("ONLINE"),
    OFFLINE("OFFLINE"),
    CONNECTING("CONNECTING"),
    DISABLED("DISABLED"),
    UNKNOWN("UNKNOWN"),
}

enum class AuthType { NONE, BASIC, DIGEST, TOKEN }

@Entity(tableName = "devices")
@Serializable
data class DeviceProfile(
    @PrimaryKey val id: String,
    val name: String,
    val location: String,           // room/zone label
    val protocol: String,           // StreamProtocol.name
    val host: String,               // IP or hostname
    val port: Int,
    val path: String = "/",         // stream path
    val username: String = "",
    val password: String = "",
    val authType: String = AuthType.NONE.name,
    val state: String = DeviceState.UNKNOWN.name,
    val latencyMs: Int = 0,
    val isFavorite: Boolean = false,
    val isEnabled: Boolean = true,
    val snapshotUrl: String = "",
    val lastSeenMs: Long = 0L,
    val addedMs: Long = System.currentTimeMillis(),
    // mDNS discovery metadata
    val discoveredVia: String = "MANUAL",  // "MDNS", "ONVIF", "TCP_PROBE", "MANUAL"
    val serviceType: String = "",
    val notes: String = "",
) {
    fun protocolEnum(): StreamProtocol =
        StreamProtocol.entries.firstOrNull { it.name == protocol } ?: StreamProtocol.CUSTOM

    fun stateEnum(): DeviceState =
        DeviceState.entries.firstOrNull { it.name == state } ?: DeviceState.UNKNOWN

    fun authTypeEnum(): AuthType =
        AuthType.entries.firstOrNull { it.name == authType } ?: AuthType.NONE

    fun streamUrl(): String = when (protocolEnum()) {
        StreamProtocol.RTSP    -> "rtsp://${credentials()}${host}:${port}${path}"
        StreamProtocol.MJPEG,
        StreamProtocol.HLS,
        StreamProtocol.DROIDCAM,
        StreamProtocol.IP_WEBCAM -> "http://${host}:${port}${path}"
        StreamProtocol.ONVIF   -> "rtsp://${credentials()}${host}:${port}${path}"
        StreamProtocol.ALFRED  -> "http://${host}:${port}${path}"
        StreamProtocol.CUSTOM  -> path // treat path as full URL for custom
    }

    private fun credentials(): String =
        if (username.isNotBlank()) "$username:$password@" else ""
}

// Lightweight result from a network scan
data class DiscoveredDevice(
    val host: String,
    val port: Int,
    val name: String,
    val serviceType: String,         // "_rtsp._tcp", "_http._tcp", etc.
    val discoveryMethod: String,     // "MDNS", "ONVIF_WS", "TCP_PROBE"
    val suggestedProtocol: StreamProtocol,
    val openPorts: List<Int> = emptyList(),
    val confidence: Int = 0,         // 0-100
)
