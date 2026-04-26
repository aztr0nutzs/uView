package com.sentinel.app.domain.model

enum class SourceSupportLevel {
    SUPPORTED,
    ADVANCED,
    UNAVAILABLE
}

data class SourceSupportInfo(
    val level: SourceSupportLevel,
    val badge: String,
    val summary: String,
    val detail: String
) {
    val isSelectable: Boolean get() = level != SourceSupportLevel.UNAVAILABLE
}

val CameraSourceType.supportInfo: SourceSupportInfo
    get() = when (this) {
        CameraSourceType.RTSP -> SourceSupportInfo(
            level = SourceSupportLevel.SUPPORTED,
            badge = "SUPPORTED",
            summary = "Direct RTSP ingest",
            detail = "Media3 playback is wired for direct RTSP/RTSPS stream URLs."
        )

        CameraSourceType.MJPEG -> SourceSupportInfo(
            level = SourceSupportLevel.SUPPORTED,
            badge = "SUPPORTED",
            summary = "Direct MJPEG ingest",
            detail = "Multipart MJPEG over HTTP uses the app's MJPEG renderer."
        )

        CameraSourceType.HLS -> SourceSupportInfo(
            level = SourceSupportLevel.SUPPORTED,
            badge = "SUPPORTED",
            summary = "Direct HLS ingest",
            detail = "HLS playlist URLs are passed to the Media3 playback path."
        )

        CameraSourceType.ANDROID_IPWEBCAM -> SourceSupportInfo(
            level = SourceSupportLevel.SUPPORTED,
            badge = "SUPPORTED",
            summary = "IP Webcam LAN stream",
            detail = "IP Webcam is supported when the phone exposes a local /video endpoint."
        )

        CameraSourceType.ANDROID_DROIDCAM -> SourceSupportInfo(
            level = SourceSupportLevel.SUPPORTED,
            badge = "SUPPORTED",
            summary = "DroidCam LAN stream",
            detail = "DroidCam is supported for direct local HTTP/RTSP endpoints."
        )

        CameraSourceType.ANDROID_CUSTOM -> SourceSupportInfo(
            level = SourceSupportLevel.ADVANCED,
            badge = "DIRECT_URL",
            summary = "Custom phone URL",
            detail = "Use only when the app exposes a direct RTSP, MJPEG, HTTP, or HLS URL."
        )

        CameraSourceType.GENERIC_URL -> SourceSupportInfo(
            level = SourceSupportLevel.ADVANCED,
            badge = "DIRECT_URL",
            summary = "Direct URL passthrough",
            detail = "Accepts direct stream URLs only. Cloud portals and app-only relays are not supported."
        )

        CameraSourceType.ONVIF -> SourceSupportInfo(
            level = SourceSupportLevel.UNAVAILABLE,
            badge = "UNAVAILABLE",
            summary = "Profile setup unavailable",
            detail = "ONVIF discovery can identify devices, but ONVIF profile/stream setup is not complete."
        )

        CameraSourceType.ANDROID_ALFRED -> SourceSupportInfo(
            level = SourceSupportLevel.UNAVAILABLE,
            badge = "UNAVAILABLE",
            summary = "Cloud relay unsupported",
            detail = "Alfred does not expose a direct LAN stream for this app to ingest."
        )

        CameraSourceType.DEMO -> SourceSupportInfo(
            level = SourceSupportLevel.UNAVAILABLE,
            badge = "INTERNAL_ONLY",
            summary = "Demo source hidden",
            detail = "Demo cameras are for development fixtures and are not a ship-build source."
        )
    }

val CameraSourceType.isSelectableInShipBuild: Boolean
    get() = supportInfo.isSelectable

val CameraSourceType.canBeSuggestedFromDiscovery: Boolean
    get() = this in setOf(
        CameraSourceType.RTSP,
        CameraSourceType.MJPEG,
        CameraSourceType.HLS,
        CameraSourceType.ANDROID_IPWEBCAM,
        CameraSourceType.ANDROID_DROIDCAM,
        CameraSourceType.ANDROID_CUSTOM,
        CameraSourceType.GENERIC_URL
    )
