package com.sentinel.app.domain.service

import com.sentinel.app.domain.model.CameraConnectionProfile
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraStreamEndpoint
import com.sentinel.app.domain.model.ConnectionTestResult
import com.sentinel.app.domain.model.DiscoveredDevice
import com.sentinel.app.domain.model.PlayerState
import com.sentinel.app.domain.model.ReconnectPolicy
import com.sentinel.app.domain.model.SnapshotRequest
import com.sentinel.app.domain.model.SnapshotResult
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// CameraDiscoveryService
// Scans the local network for camera-compatible devices.
// Implementation NOTE: real ONVIF WS-Discovery and port scanning are scaffolded.
// ─────────────────────────────────────────────────────────────────────────────

interface CameraDiscoveryService {

    /** Begin a local network scan. Emits discovered devices as found. */
    fun startScan(): Flow<DiscoveredDevice>

    /** Cancel any in-progress scan. */
    suspend fun cancelScan()

    /** Whether a scan is currently running. */
    val isScanning: Flow<Boolean>

    /** Probe a specific IP address for known camera ports. */
    suspend fun probeDevice(ipAddress: String): DiscoveredDevice?
}

// ─────────────────────────────────────────────────────────────────────────────
// CameraConnectionTester
// Tests whether a camera endpoint is reachable and streams correctly.
// ─────────────────────────────────────────────────────────────────────────────

interface CameraConnectionTester {

    /** Full connection test — ping, port check, stream probe. */
    suspend fun testConnection(
        camera: CameraDevice,
        timeoutSeconds: Int = 10
    ): ConnectionTestResult

    /** Lightweight reachability check (ping / TCP connect only). */
    suspend fun pingHost(
        host: String,
        port: Int,
        timeoutMs: Int = 3_000
    ): Boolean

    /** Validate stream URL is formatted correctly without connecting. */
    fun validateStreamUrl(url: String): Boolean
}

// ─────────────────────────────────────────────────────────────────────────────
// CameraPlaybackService
// Manages player lifecycle for a given stream endpoint.
// The actual player (Media3/ExoPlayer) is wired in the implementation.
// ─────────────────────────────────────────────────────────────────────────────

interface CameraPlaybackService {

    /** Observe playback state for a camera. */
    fun observePlayerState(cameraId: String): Flow<PlayerState>

    /** Begin playback of a stream endpoint. */
    suspend fun play(cameraId: String, endpoint: CameraStreamEndpoint)

    /** Pause playback. */
    suspend fun pause(cameraId: String)

    /** Stop and release player resources for a camera. */
    suspend fun stop(cameraId: String)

    /** Force reconnect — stop then replay. */
    suspend fun reconnect(cameraId: String)

    /** Release all players. Call on app background or screen dismiss. */
    suspend fun releaseAll()

    /** Set volume for a stream (0.0 – 1.0). */
    fun setVolume(cameraId: String, volume: Float)

    /** Toggle mute state. Returns new muted state. */
    fun toggleMute(cameraId: String): Boolean
}

// ─────────────────────────────────────────────────────────────────────────────
// RecordingController
// Controls local recording state for camera streams.
// ─────────────────────────────────────────────────────────────────────────────

interface RecordingController {

    /** Start recording the stream for a camera. */
    suspend fun startRecording(camera: CameraDevice): Result<Unit>

    /** Stop recording. */
    suspend fun stopRecording(cameraId: String): Result<RecordingEntry>

    /** Observe recording state for a camera. */
    fun observeRecordingState(cameraId: String): Flow<com.sentinel.app.domain.model.RecordingState>

    /** Returns truthful per-camera recording support. */
    suspend fun getRecordingCapability(camera: CameraDevice): RecordingCapability

    /** List recorded files for a camera. */
    suspend fun getRecordings(cameraId: String): List<RecordingEntry>
}

data class RecordingCapability(
    val supported: Boolean,
    val requiresActivePlayback: Boolean,
    val outputFormat: String?,
    val reason: String? = null
)

data class RecordingEntry(
    val filePath: String,
    val cameraId: String,
    val startedAt: Long,
    val durationSeconds: Long,
    val sizeBytes: Long
)

// ─────────────────────────────────────────────────────────────────────────────
// SnapshotController
// Captures still frames from a camera stream.
// ─────────────────────────────────────────────────────────────────────────────

interface SnapshotController {

    /** Capture a snapshot from the current stream frame. */
    suspend fun takeSnapshot(request: SnapshotRequest): SnapshotResult

    /** List saved snapshots for a camera. */
    suspend fun getSnapshots(cameraId: String): List<SnapshotResult>
}

// ─────────────────────────────────────────────────────────────────────────────
// Stream Adapter interfaces
// Each CameraSourceType gets its own adapter for URL resolution and auth.
// ─────────────────────────────────────────────────────────────────────────────

interface CameraStreamAdapter {
    /** Source type(s) this adapter handles. */
    val supportedTypes: List<com.sentinel.app.domain.model.CameraSourceType>

    /** Resolve the playable stream URL from a connection profile. */
    suspend fun resolveStreamEndpoint(
        profile: CameraConnectionProfile,
        camera: CameraDevice
    ): CameraStreamEndpoint?

    /** Whether this adapter is fully implemented (vs scaffolded). */
    val isImplemented: Boolean
}

/** Handles RTSP/RTSPS camera streams via Media3 ExoPlayer. */
interface RtspCameraAdapter : CameraStreamAdapter

/** Handles MJPEG over HTTP. */
interface MjpegStreamAdapter : CameraStreamAdapter

/**
 * Handles ONVIF-compatible cameras.
 * NOTE: ONVIF WS-Discovery and profile enumeration are NOT yet implemented.
 * This interface is scaffolded for future integration.
 */
interface OnvifCameraAdapter : CameraStreamAdapter {
    /** Discover available media profiles from an ONVIF device. */
    suspend fun discoverProfiles(profile: CameraConnectionProfile): List<OnvifMediaProfile>
}

data class OnvifMediaProfile(
    val token: String,
    val name: String,
    val streamUri: String,
    val videoWidth: Int,
    val videoHeight: Int
)

/**
 * Handles Android phone camera sources (DroidCam, IP Webcam, Alfred, custom).
 * NOTE: Each app uses different port/path conventions.
 */
interface AndroidPhoneSourceAdapter : CameraStreamAdapter {
    /** Build the stream URL for a specific Android phone source app. */
    fun buildStreamUrl(config: com.sentinel.app.domain.model.AndroidPhoneSourceConfig): String
}

/** Generic URL adapter — no protocol-specific handling, direct URL passthrough. */
interface GenericStreamAdapter : CameraStreamAdapter
