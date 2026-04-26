package com.sentinel.app.data.playback

import com.sentinel.app.data.remote.adapters.AndroidPhoneSourceAdapterImpl
import com.sentinel.app.data.remote.adapters.GenericStreamAdapterImpl
import com.sentinel.app.data.remote.adapters.MjpegStreamAdapterImpl
import com.sentinel.app.data.remote.adapters.OnvifCameraAdapterImpl
import com.sentinel.app.data.remote.adapters.RtspCameraAdapterImpl
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraSourceType
import com.sentinel.app.domain.model.CameraStreamEndpoint
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * StreamUrlResolver
 *
 * Single entry point for resolving a playable [CameraStreamEndpoint] from any
 * [CameraDevice], regardless of source type.
 *
 * Routing table:
 *   RTSP                    → RtspCameraAdapterImpl
 *   MJPEG                   → MjpegStreamAdapterImpl
 *   ONVIF                   → unavailable until profile stream setup is complete
 *   HLS / GENERIC_URL       → GenericStreamAdapterImpl
 *   ANDROID_DROIDCAM/IPWEBCAM/CUSTOM → AndroidPhoneSourceAdapterImpl
 *   ANDROID_ALFRED / DEMO   → unavailable in ship builds
 *
 * Returns null only if the device's source type is completely unresolvable and
 * every adapter fails. Callers must handle null by showing an error state.
 */
@Singleton
class StreamUrlResolver @Inject constructor(
    private val rtspAdapter: RtspCameraAdapterImpl,
    private val mjpegAdapter: MjpegStreamAdapterImpl,
    private val onvifAdapter: OnvifCameraAdapterImpl,
    private val phoneAdapter: AndroidPhoneSourceAdapterImpl,
    private val genericAdapter: GenericStreamAdapterImpl
) {
    suspend fun resolve(camera: CameraDevice): CameraStreamEndpoint? {
        Timber.d("Resolving stream for camera=${camera.id} type=${camera.sourceType}")
        if (isUnsupportedSource(camera)) {
            Timber.w("Source type ${camera.sourceType} is unavailable for playback in this build")
            return null
        }
        return try {
            when (camera.sourceType) {
                CameraSourceType.RTSP ->
                    rtspAdapter.resolveStreamEndpoint(camera.connectionProfile, camera)

                CameraSourceType.MJPEG ->
                    mjpegAdapter.resolveStreamEndpoint(camera.connectionProfile, camera)

                CameraSourceType.ONVIF -> null

                CameraSourceType.ANDROID_DROIDCAM,
                CameraSourceType.ANDROID_IPWEBCAM,
                CameraSourceType.ANDROID_CUSTOM ->
                    phoneAdapter.resolveStreamEndpoint(camera.connectionProfile, camera)

                CameraSourceType.HLS,
                CameraSourceType.GENERIC_URL ->
                    genericAdapter.resolveStreamEndpoint(camera.connectionProfile, camera)

                CameraSourceType.ANDROID_ALFRED,
                CameraSourceType.DEMO -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to resolve stream URL for camera=${camera.id}")
            null
        }
    }

    /**
     * Determine whether the resolved endpoint should use MJPEG pull-mode
     * rendering (BitmapFactory loop) rather than ExoPlayer.
     *
     * ExoPlayer handles: RTSP, HLS, RTSPS, generic HTTP streams.
     * MJPEG requires our custom [MjpegFrameSource] because ExoPlayer does not
     * natively support multipart/x-mixed-replace boundaries.
     */
    fun requiresMjpegRenderer(endpoint: CameraStreamEndpoint): Boolean =
        endpoint.sourceType == CameraSourceType.MJPEG ||
            endpoint.url.contains("/video", ignoreCase = true) &&
            (endpoint.url.startsWith("http://") || endpoint.url.startsWith("https://")) &&
            endpoint.sourceType != CameraSourceType.HLS

    /**
     * Whether a configured source is unavailable in this ship build.
     * Surfaced to callers so they can show an honest unsupported state.
     */
    fun isUnsupportedSource(camera: CameraDevice): Boolean =
        camera.sourceType == CameraSourceType.ONVIF ||
            camera.sourceType == CameraSourceType.DEMO ||
            camera.sourceType == CameraSourceType.ANDROID_ALFRED
}
