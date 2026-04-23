package com.sentinel.app.domain.model

import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────────────────
// Camera Source Type
// Defines what kind of source this camera entry represents.
// ─────────────────────────────────────────────────────────────────────────────

enum class CameraSourceType(val displayName: String, val description: String) {
    RTSP(
        displayName = "RTSP Camera",
        description = "Standard RTSP stream (IP cameras, NVRs, security cameras)"
    ),
    MJPEG(
        displayName = "MJPEG / HTTP Stream",
        description = "Motion JPEG over HTTP — common in budget IP cameras"
    ),
    ONVIF(
        displayName = "ONVIF Camera",
        description = "ONVIF-compatible camera — auto profile discovery (future)"
    ),
    HLS(
        displayName = "HLS Stream",
        description = "HTTP Live Streaming — m3u8 playlist source"
    ),
    ANDROID_DROIDCAM(
        displayName = "DroidCam (Android Phone)",
        description = "Old Android phone using the DroidCam app as a webcam"
    ),
    ANDROID_ALFRED(
        displayName = "Alfred (Android Phone)",
        description = "Old Android phone using the Alfred home security app"
    ),
    ANDROID_IPWEBCAM(
        displayName = "IP Webcam (Android Phone)",
        description = "Old Android phone using the IP Webcam app"
    ),
    ANDROID_CUSTOM(
        displayName = "Android Phone — Custom URL",
        description = "Android phone exposing a custom RTSP/HTTP stream endpoint"
    ),
    GENERIC_URL(
        displayName = "Generic Stream URL",
        description = "Any custom stream URL — RTSP, MJPEG, or HTTP"
    ),
    DEMO(
        displayName = "Demo Camera",
        description = "Local demo entry for testing and UI development"
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Camera Status
// ─────────────────────────────────────────────────────────────────────────────

enum class CameraStatus {
    ONLINE,
    OFFLINE,
    CONNECTING,
    ERROR,
    DISABLED,
    UNKNOWN
}

// ─────────────────────────────────────────────────────────────────────────────
// Transport type for stream delivery
// ─────────────────────────────────────────────────────────────────────────────

enum class StreamTransport {
    TCP,
    UDP,
    HTTP,
    HTTPS,
    AUTO
}

// ─────────────────────────────────────────────────────────────────────────────
// Video quality profile
// ─────────────────────────────────────────────────────────────────────────────

enum class StreamQualityProfile(val label: String) {
    AUTO("Auto"),
    HIGH("High (main stream)"),
    MEDIUM("Medium"),
    LOW("Low (sub stream)"),
    MINIMAL("Minimal (data saver)")
}

// ─────────────────────────────────────────────────────────────────────────────
// Recording state
// ─────────────────────────────────────────────────────────────────────────────

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    SAVING,
    ERROR
}

// ─────────────────────────────────────────────────────────────────────────────
// Camera Connection Profile
// Network and auth details for connecting to a camera source.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class CameraConnectionProfile(
    val host: String,
    val port: Int,
    val path: String = "",
    val username: String = "",
    val password: String = "",
    val transport: StreamTransport = StreamTransport.AUTO,
    val useTls: Boolean = false,
    val timeoutSeconds: Int = 10,
    val retryCount: Int = 3
) {
    val hasCredentials: Boolean get() = username.isNotBlank()

    /** Constructs a display-safe URL (no credentials in string) */
    fun toDisplayUrl(): String {
        val scheme = when {
            useTls -> "rtsps"
            else -> "rtsp"
        }
        val portStr = if (port != 554) ":$port" else ""
        return "$scheme://$host$portStr${path.ifEmpty { "/" }}"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Camera Stream Endpoint
// Resolved, ready-to-play stream URL with metadata.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class CameraStreamEndpoint(
    val url: String,
    val sourceType: CameraSourceType,
    val qualityProfile: StreamQualityProfile = StreamQualityProfile.AUTO,
    val hasAudio: Boolean = false,
    val hasVideo: Boolean = true,
    val isLanOnly: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Android Phone Source Config
// Config specific to using an old Android phone as a camera.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class AndroidPhoneSourceConfig(
    val phoneNickname: String,
    val appMethod: CameraSourceType,
    val endpointUrl: String,
    val streamTransport: StreamTransport = StreamTransport.HTTP,
    val isLanOnly: Boolean = true,
    val audioAvailable: Boolean = false,
    val qualityProfile: StreamQualityProfile = StreamQualityProfile.MEDIUM,
    val batteryLevelPercent: Int? = null,        // populated at runtime if available
    val isCharging: Boolean? = null,              // populated at runtime if available
    val lastSeenOnline: Long? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// Camera Health Status
// Runtime health snapshot for a given camera.
// ─────────────────────────────────────────────────────────────────────────────

data class CameraHealthStatus(
    val cameraId: String,
    val status: CameraStatus,
    val latencyMs: Long? = null,
    val lastSuccessfulConnectionMs: Long? = null,
    val consecutiveFailures: Int = 0,
    val errorMessage: String? = null,
    val streamBitrateKbps: Int? = null,
    val resolvedStreamUrl: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// Camera Device
// The primary domain model for a configured camera.
// ─────────────────────────────────────────────────────────────────────────────

data class CameraDevice(
    val id: String,
    val name: String,
    val room: String,
    val sourceType: CameraSourceType,
    val connectionProfile: CameraConnectionProfile,
    val androidPhoneConfig: AndroidPhoneSourceConfig? = null,
    val streamEndpoints: List<CameraStreamEndpoint> = emptyList(),
    val preferredQuality: StreamQualityProfile = StreamQualityProfile.AUTO,
    val isEnabled: Boolean = true,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val thumbnailUrl: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val healthStatus: CameraHealthStatus? = null
) {
    val isOnline: Boolean get() = healthStatus?.status == CameraStatus.ONLINE
    val displayStatus: CameraStatus get() = when {
        !isEnabled -> CameraStatus.DISABLED
        else -> healthStatus?.status ?: CameraStatus.UNKNOWN
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Camera Event
// Represents a logged event (motion, connection change, snapshot, etc.)
// ─────────────────────────────────────────────────────────────────────────────

enum class CameraEventType(val displayName: String) {
    MOTION_DETECTED("Motion Detected"),
    CONNECTION_LOST("Connection Lost"),
    CONNECTION_RESTORED("Connection Restored"),
    RECORDING_STARTED("Recording Started"),
    RECORDING_STOPPED("Recording Stopped"),
    SNAPSHOT_TAKEN("Snapshot Taken"),
    CAMERA_ADDED("Camera Added"),
    CAMERA_REMOVED("Camera Removed"),
    SETTINGS_CHANGED("Settings Changed"),
    STREAM_ERROR("Stream Error")
}

data class CameraEvent(
    val id: String,
    val cameraId: String,
    val cameraName: String,
    val eventType: CameraEventType,
    val timestampMs: Long,
    val description: String = "",
    val thumbnailPath: String? = null,
    val isRead: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Snapshot Request
// ─────────────────────────────────────────────────────────────────────────────

data class SnapshotRequest(
    val cameraId: String,
    val outputPath: String? = null,    // null = auto-generate path
    val quality: Int = 90              // JPEG quality 1–100
)

data class SnapshotResult(
    val cameraId: String,
    val savedPath: String,
    val timestampMs: Long,
    val success: Boolean,
    val errorMessage: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// Connection Test Result
// ─────────────────────────────────────────────────────────────────────────────

data class ConnectionTestResult(
    val cameraId: String,
    val host: String,
    val resolvedUrl: String,
    val success: Boolean,
    val latencyMs: Long? = null,
    val errorMessage: String? = null,
    val streamReachable: Boolean = false,
    val credentialsAccepted: Boolean? = null,   // null = not tested
    val testedAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────────────────────────────────────
// Dashboard Summary Stats
// Aggregated stats shown on the home dashboard.
// ─────────────────────────────────────────────────────────────────────────────

data class DashboardSummary(
    val totalCameras: Int,
    val onlineCameras: Int,
    val offlineCameras: Int,
    val disabledCameras: Int,
    val recentEventCount: Int,
    val hasActiveRecording: Boolean,
    val storageUsedMb: Long,
    val storageCapacityMb: Long
) {
    val storagePercent: Float get() = if (storageCapacityMb == 0L) 0f
    else storageUsedMb.toFloat() / storageCapacityMb.toFloat()
}

// ─────────────────────────────────────────────────────────────────────────────
// Discovered Device (from network scan)
// ─────────────────────────────────────────────────────────────────────────────

data class DiscoveredDevice(
    val ipAddress: String,
    val hostname: String?,
    val port: Int,
    val probableSourceType: CameraSourceType?,
    val openPorts: List<Int>,
    val banner: String?,                // HTTP banner if present
    val isAlreadyAdded: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Player State Model
// State machine for the stream playback component.
// ─────────────────────────────────────────────────────────────────────────────

sealed class PlayerState {
    object Idle : PlayerState()
    object Loading : PlayerState()
    data class Buffering(val progressPercent: Int) : PlayerState()
    object Playing : PlayerState()
    object Paused : PlayerState()
    data class Error(val message: String, val cause: Throwable? = null) : PlayerState()
    object Reconnecting : PlayerState()
    object StreamUnsupported : PlayerState()
}

// ─────────────────────────────────────────────────────────────────────────────
// Reconnect Policy
// ─────────────────────────────────────────────────────────────────────────────

data class ReconnectPolicy(
    val enabled: Boolean = true,
    val maxAttempts: Int = 5,
    val initialDelayMs: Long = 2_000L,
    val backoffMultiplier: Float = 1.5f,
    val maxDelayMs: Long = 30_000L
)
