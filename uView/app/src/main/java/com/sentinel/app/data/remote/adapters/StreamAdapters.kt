package com.sentinel.app.data.remote.adapters

import com.sentinel.app.domain.model.AndroidPhoneSourceConfig
import com.sentinel.app.domain.model.CameraConnectionProfile
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraSourceType
import com.sentinel.app.domain.model.CameraStreamEndpoint
import com.sentinel.app.domain.model.StreamQualityProfile
import com.sentinel.app.domain.service.AndroidPhoneSourceAdapter
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AndroidPhoneSourceAdapterImpl
 *
 * Builds stream URLs for old Android phones acting as IP camera sources.
 * Each supported app (DroidCam, IP Webcam, Alfred, custom) has distinct
 * URL conventions on different default ports.
 *
 * IMPLEMENTATION STATUS:
 * - DroidCam: URL schema implemented (MJPEG/video endpoint).
 * - IP Webcam: URL schema implemented.
 * - Alfred: Relay stream URL not available via direct LAN — requires Alfred's
 *   own backend. Marked as not supported for LAN mode.
 * - Custom / Generic URL: passthrough implemented.
 *
 * Actual stream playback depends on Media3 ExoPlayer wired to the returned URL.
 */
@Singleton
class AndroidPhoneSourceAdapterImpl @Inject constructor() : AndroidPhoneSourceAdapter {

    override val supportedTypes: List<CameraSourceType> = listOf(
        CameraSourceType.ANDROID_DROIDCAM,
        CameraSourceType.ANDROID_ALFRED,
        CameraSourceType.ANDROID_IPWEBCAM,
        CameraSourceType.ANDROID_CUSTOM
    )

    override val isImplemented: Boolean = true  // URL building is implemented; playback requires ExoPlayer wiring

    override suspend fun resolveStreamEndpoint(
        profile: CameraConnectionProfile,
        camera: CameraDevice
    ): CameraStreamEndpoint? {
        val config = camera.androidPhoneConfig ?: run {
            Timber.w("resolveStreamEndpoint called on non-phone camera: ${camera.id}")
            return null
        }
        val url = buildStreamUrl(config)
        return CameraStreamEndpoint(
            url = url,
            sourceType = camera.sourceType,
            qualityProfile = config.qualityProfile,
            hasAudio = config.audioAvailable,
            hasVideo = true,
            isLanOnly = config.isLanOnly
        )
    }

    /**
     * Build the stream URL for each supported Android phone camera app.
     *
     * Port conventions (defaults, user-overridable):
     *   DroidCam    — HTTP MJPEG on :4747/video   (or RTSP :4747/video)
     *   IP Webcam   — HTTP MJPEG on :8080/video   (or :8080/shot.jpg for snapshot)
     *   Alfred      — No direct LAN stream — requires Alfred cloud relay
     *   Custom      — Use endpoint URL directly
     */
    override fun buildStreamUrl(config: AndroidPhoneSourceConfig): String {
        return when (config.appMethod) {
            CameraSourceType.ANDROID_DROIDCAM -> {
                // DroidCam serves MJPEG at /video on port 4747 by default
                val base = config.endpointUrl.trimEnd('/')
                if (base.startsWith("http") || base.startsWith("rtsp")) {
                    // User provided full URL — use as-is
                    base
                } else {
                    "http://$base:4747/video"
                }
            }

            CameraSourceType.ANDROID_IPWEBCAM -> {
                // IP Webcam serves MJPEG at /video on port 8080 by default
                val base = config.endpointUrl.trimEnd('/')
                if (base.startsWith("http") || base.startsWith("rtsp")) {
                    base
                } else {
                    "http://$base:8080/video"
                }
            }

            CameraSourceType.ANDROID_ALFRED -> {
                // Alfred does not provide a direct LAN stream endpoint.
                // It routes through Alfred's cloud relay. Not supported for local LAN mode.
                // The UI should surface this limitation clearly.
                Timber.w("Alfred LAN stream not supported. Returning placeholder URL.")
                "alfred://unsupported-lan-stream"
            }

            CameraSourceType.ANDROID_CUSTOM, CameraSourceType.GENERIC_URL -> {
                // Trust the user-provided endpoint URL directly
                config.endpointUrl
            }

            else -> {
                Timber.w("Unexpected source type in AndroidPhoneSourceAdapter: ${config.appMethod}")
                config.endpointUrl
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RtspCameraAdapterImpl
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class RtspCameraAdapterImpl @Inject constructor() :
    com.sentinel.app.domain.service.RtspCameraAdapter {

    override val supportedTypes: List<CameraSourceType> = listOf(CameraSourceType.RTSP)
    override val isImplemented: Boolean = true  // URL building implemented; ExoPlayer wiring in PlaybackService

    override suspend fun resolveStreamEndpoint(
        profile: CameraConnectionProfile,
        camera: CameraDevice
    ): CameraStreamEndpoint {
        val scheme = if (profile.useTls) "rtsps" else "rtsp"
        val portStr = if (profile.port != 554) ":${profile.port}" else ""
        val credentials = if (profile.hasCredentials) {
            "${profile.username}:${profile.password}@"
        } else ""
        val path = profile.path.ifEmpty { "/" }.let {
            if (it.startsWith("/")) it else "/$it"
        }
        val url = "$scheme://$credentials${profile.host}$portStr$path"
        return CameraStreamEndpoint(
            url = url,
            sourceType = CameraSourceType.RTSP,
            qualityProfile = camera.preferredQuality,
            hasAudio = true,    // assume audio present; confirmed at runtime
            hasVideo = true
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MjpegStreamAdapterImpl
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class MjpegStreamAdapterImpl @Inject constructor() :
    com.sentinel.app.domain.service.MjpegStreamAdapter {

    override val supportedTypes: List<CameraSourceType> = listOf(CameraSourceType.MJPEG)
    override val isImplemented: Boolean = true  // URL building implemented

    override suspend fun resolveStreamEndpoint(
        profile: CameraConnectionProfile,
        camera: CameraDevice
    ): CameraStreamEndpoint {
        val scheme = if (profile.useTls) "https" else "http"
        val portStr = ":${profile.port}"
        val path = profile.path.ifEmpty { "/video" }
        val url = "$scheme://${profile.host}$portStr$path"
        return CameraStreamEndpoint(
            url = url,
            sourceType = CameraSourceType.MJPEG,
            qualityProfile = camera.preferredQuality,
            hasAudio = false,   // MJPEG is video-only
            hasVideo = true
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OnvifCameraAdapterImpl
// SCAFFOLDED — ONVIF WS-Discovery and profile enumeration not yet implemented.
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class OnvifCameraAdapterImpl @Inject constructor() :
    com.sentinel.app.domain.service.OnvifCameraAdapter {

    override val supportedTypes: List<CameraSourceType> = listOf(CameraSourceType.ONVIF)

    /**
     * NOT YET IMPLEMENTED.
     * ONVIF requires SOAP/WS-Security which is not trivial to implement.
     * This adapter is scaffolded for future integration.
     * Consider the `onvif4android` library or a custom SOAP client.
     */
    override val isImplemented: Boolean = false

    override suspend fun resolveStreamEndpoint(
        profile: CameraConnectionProfile,
        camera: CameraDevice
    ): CameraStreamEndpoint? {
        Timber.w("ONVIF adapter not yet implemented. Falling back to RTSP guess.")
        // Fallback: attempt standard RTSP URL which many ONVIF cameras also expose
        val url = "rtsp://${profile.host}:${profile.port}/stream1"
        return CameraStreamEndpoint(
            url = url,
            sourceType = CameraSourceType.ONVIF,
            qualityProfile = camera.preferredQuality,
            hasAudio = true,
            hasVideo = true
        )
    }

    override suspend fun discoverProfiles(
        profile: CameraConnectionProfile
    ): List<com.sentinel.app.domain.service.OnvifMediaProfile> {
        Timber.w("ONVIF profile discovery not implemented.")
        return emptyList()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GenericStreamAdapterImpl
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class GenericStreamAdapterImpl @Inject constructor() :
    com.sentinel.app.domain.service.GenericStreamAdapter {

    override val supportedTypes: List<CameraSourceType> = listOf(
        CameraSourceType.GENERIC_URL,
        CameraSourceType.HLS,
        CameraSourceType.DEMO
    )
    override val isImplemented: Boolean = true

    override suspend fun resolveStreamEndpoint(
        profile: CameraConnectionProfile,
        camera: CameraDevice
    ): CameraStreamEndpoint {
        // For generic sources the host field contains the full URL
        val url = if (profile.host.startsWith("http") || profile.host.startsWith("rtsp")) {
            profile.host
        } else {
            val scheme = if (profile.useTls) "https" else "http"
            "$scheme://${profile.host}:${profile.port}${profile.path}"
        }
        return CameraStreamEndpoint(
            url = url,
            sourceType = camera.sourceType,
            qualityProfile = camera.preferredQuality,
            hasAudio = false,
            hasVideo = true
        )
    }
}
