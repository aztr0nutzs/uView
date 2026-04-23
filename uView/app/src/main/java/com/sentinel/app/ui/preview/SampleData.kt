package com.sentinel.app.ui.preview

import com.sentinel.app.domain.model.AndroidPhoneSourceConfig
import com.sentinel.app.domain.model.CameraConnectionProfile
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraEvent
import com.sentinel.app.domain.model.CameraEventType
import com.sentinel.app.domain.model.CameraHealthStatus
import com.sentinel.app.domain.model.CameraSourceType
import com.sentinel.app.domain.model.CameraStatus
import com.sentinel.app.domain.model.ConnectionTestResult
import com.sentinel.app.domain.model.DashboardSummary
import com.sentinel.app.domain.model.DiscoveredDevice
import com.sentinel.app.domain.model.StreamQualityProfile
import com.sentinel.app.domain.model.StreamTransport

// ─────────────────────────────────────────────────────────────────────────────
// SampleData — used in Compose @Preview and ViewModel dev-mode seeding.
// These are realistic-looking mock values only. No real network calls.
// ─────────────────────────────────────────────────────────────────────────────

object SampleData {

    // ── Sample Cameras ───────────────────────────────────────────────────────

    val frontDoorCamera = CameraDevice(
        id = "cam_001",
        name = "Front Door",
        room = "Exterior",
        sourceType = CameraSourceType.RTSP,
        connectionProfile = CameraConnectionProfile(
            host = "192.168.1.101",
            port = 554,
            path = "/stream1",
            username = "admin",
            password = "••••••••",
            transport = StreamTransport.TCP
        ),
        preferredQuality = StreamQualityProfile.HIGH,
        isEnabled = true,
        isFavorite = true,
        isPinned = true,
        healthStatus = CameraHealthStatus(
            cameraId = "cam_001",
            status = CameraStatus.ONLINE,
            latencyMs = 42,
            lastSuccessfulConnectionMs = System.currentTimeMillis() - 120_000,
            consecutiveFailures = 0,
            streamBitrateKbps = 2048
        )
    )

    val backYardCamera = CameraDevice(
        id = "cam_002",
        name = "Back Yard",
        room = "Exterior",
        sourceType = CameraSourceType.MJPEG,
        connectionProfile = CameraConnectionProfile(
            host = "192.168.1.102",
            port = 8080,
            path = "/video",
            transport = StreamTransport.HTTP
        ),
        preferredQuality = StreamQualityProfile.MEDIUM,
        isEnabled = true,
        isFavorite = false,
        healthStatus = CameraHealthStatus(
            cameraId = "cam_002",
            status = CameraStatus.ONLINE,
            latencyMs = 95,
            lastSuccessfulConnectionMs = System.currentTimeMillis() - 30_000,
            streamBitrateKbps = 800
        )
    )

    val garageCamera = CameraDevice(
        id = "cam_003",
        name = "Garage",
        room = "Garage",
        sourceType = CameraSourceType.RTSP,
        connectionProfile = CameraConnectionProfile(
            host = "192.168.1.103",
            port = 554,
            path = "/live",
            username = "admin",
            password = "••••••••",
            transport = StreamTransport.TCP
        ),
        preferredQuality = StreamQualityProfile.MEDIUM,
        isEnabled = true,
        isFavorite = false,
        healthStatus = CameraHealthStatus(
            cameraId = "cam_003",
            status = CameraStatus.OFFLINE,
            latencyMs = null,
            lastSuccessfulConnectionMs = System.currentTimeMillis() - 3_600_000,
            consecutiveFailures = 5,
            errorMessage = "Connection refused — host unreachable"
        )
    )

    val livingRoomPhone = CameraDevice(
        id = "cam_004",
        name = "Living Room Phone",
        room = "Living Room",
        sourceType = CameraSourceType.ANDROID_IPWEBCAM,
        connectionProfile = CameraConnectionProfile(
            host = "192.168.1.115",
            port = 8080,
            path = "/video",
            transport = StreamTransport.HTTP
        ),
        androidPhoneConfig = AndroidPhoneSourceConfig(
            phoneNickname = "Old Pixel 3a",
            appMethod = CameraSourceType.ANDROID_IPWEBCAM,
            endpointUrl = "192.168.1.115",
            streamTransport = StreamTransport.HTTP,
            isLanOnly = true,
            audioAvailable = true,
            qualityProfile = StreamQualityProfile.MEDIUM,
            batteryLevelPercent = 67,
            isCharging = true
        ),
        preferredQuality = StreamQualityProfile.MEDIUM,
        isEnabled = true,
        isFavorite = true,
        healthStatus = CameraHealthStatus(
            cameraId = "cam_004",
            status = CameraStatus.ONLINE,
            latencyMs = 120,
            streamBitrateKbps = 600
        )
    )

    val basementCamera = CameraDevice(
        id = "cam_005",
        name = "Basement",
        room = "Basement",
        sourceType = CameraSourceType.ANDROID_DROIDCAM,
        connectionProfile = CameraConnectionProfile(
            host = "192.168.1.120",
            port = 4747,
            path = "/video",
            transport = StreamTransport.HTTP
        ),
        androidPhoneConfig = AndroidPhoneSourceConfig(
            phoneNickname = "Samsung S8 (old)",
            appMethod = CameraSourceType.ANDROID_DROIDCAM,
            endpointUrl = "192.168.1.120",
            streamTransport = StreamTransport.HTTP,
            isLanOnly = true,
            audioAvailable = false,
            qualityProfile = StreamQualityProfile.LOW,
            batteryLevelPercent = 23,
            isCharging = false
        ),
        preferredQuality = StreamQualityProfile.LOW,
        isEnabled = true,
        isFavorite = false,
        healthStatus = CameraHealthStatus(
            cameraId = "cam_005",
            status = CameraStatus.CONNECTING,
            latencyMs = null,
            consecutiveFailures = 1
        )
    )

    val officeCamera = CameraDevice(
        id = "cam_006",
        name = "Office",
        room = "Office",
        sourceType = CameraSourceType.ONVIF,
        connectionProfile = CameraConnectionProfile(
            host = "192.168.1.130",
            port = 80,
            path = "/onvif/device_service",
            username = "admin",
            password = "••••••••"
        ),
        preferredQuality = StreamQualityProfile.AUTO,
        isEnabled = false,
        isFavorite = false,
        healthStatus = CameraHealthStatus(
            cameraId = "cam_006",
            status = CameraStatus.DISABLED
        )
    )

    val allCameras: List<CameraDevice> = listOf(
        frontDoorCamera,
        backYardCamera,
        garageCamera,
        livingRoomPhone,
        basementCamera,
        officeCamera
    )

    // ── Dashboard Summary ────────────────────────────────────────────────────

    val dashboardSummary = DashboardSummary(
        totalCameras = 6,
        onlineCameras = 3,
        offlineCameras = 1,
        disabledCameras = 1,
        recentEventCount = 14,
        hasActiveRecording = false,
        storageUsedMb = 2_340,
        storageCapacityMb = 32_768
    )

    // ── Sample Events ────────────────────────────────────────────────────────

    val sampleEvents: List<CameraEvent> = listOf(
        CameraEvent(
            id = "evt_001",
            cameraId = "cam_001",
            cameraName = "Front Door",
            eventType = CameraEventType.MOTION_DETECTED,
            timestampMs = System.currentTimeMillis() - 300_000,
            description = "Motion detected near entrance",
            isRead = false
        ),
        CameraEvent(
            id = "evt_002",
            cameraId = "cam_003",
            cameraName = "Garage",
            eventType = CameraEventType.CONNECTION_LOST,
            timestampMs = System.currentTimeMillis() - 3_600_000,
            description = "Camera went offline unexpectedly",
            isRead = false
        ),
        CameraEvent(
            id = "evt_003",
            cameraId = "cam_001",
            cameraName = "Front Door",
            eventType = CameraEventType.SNAPSHOT_TAKEN,
            timestampMs = System.currentTimeMillis() - 7_200_000,
            description = "Manual snapshot captured",
            isRead = true
        ),
        CameraEvent(
            id = "evt_004",
            cameraId = "cam_004",
            cameraName = "Living Room Phone",
            eventType = CameraEventType.CONNECTION_RESTORED,
            timestampMs = System.currentTimeMillis() - 10_800_000,
            description = "Camera back online after 4 minute outage",
            isRead = true
        ),
        CameraEvent(
            id = "evt_005",
            cameraId = "cam_002",
            cameraName = "Back Yard",
            eventType = CameraEventType.MOTION_DETECTED,
            timestampMs = System.currentTimeMillis() - 14_400_000,
            description = "Motion in back yard zone",
            isRead = true
        ),
        CameraEvent(
            id = "evt_006",
            cameraId = "cam_001",
            cameraName = "Front Door",
            eventType = CameraEventType.MOTION_DETECTED,
            timestampMs = System.currentTimeMillis() - 86_400_000,
            description = "Motion detected — package delivery",
            isRead = true
        )
    )

    // ── Discovered Devices (network scan preview) ────────────────────────────

    val discoveredDevices: List<DiscoveredDevice> = listOf(
        DiscoveredDevice(
            ipAddress          = "192.168.1.101",
            hostname           = "ipcam-front",
            port               = 554,
            probableSourceType = CameraSourceType.RTSP,
            openPorts          = listOf(80, 554),
            banner             = "Hikvision DVR",
            isAlreadyAdded     = true,
            discoveryMethod    = com.sentinel.app.domain.model.DiscoveryMethod.ONVIF_WS_DISCOVERY,
            confidence         = com.sentinel.app.domain.model.DiscoveryConfidence.CONFIRMED,
            onvifManufacturer  = "Hikvision",
            onvifModel         = "DS-2CD2143G2-I",
            macAddress         = "B8:A4:4F:12:34:56",
            macVendor          = "Hikvision"
        ),
        DiscoveredDevice(
            ipAddress          = "192.168.1.108",
            hostname           = null,
            port               = 8080,
            probableSourceType = CameraSourceType.MJPEG,
            openPorts          = listOf(8080),
            banner             = "HTTP 200 | MJPEG",
            isAlreadyAdded     = false,
            discoveryMethod    = com.sentinel.app.domain.model.DiscoveryMethod.ARP_TABLE,
            confidence         = com.sentinel.app.domain.model.DiscoveryConfidence.POSSIBLE,
            macAddress         = "A0:B1:C2:D3:E4:F5"
        ),
        DiscoveredDevice(
            ipAddress          = "192.168.1.115",
            hostname           = "android-pixel3a",
            port               = 8080,
            probableSourceType = CameraSourceType.ANDROID_IPWEBCAM,
            openPorts          = listOf(8080, 8081),
            banner             = "IP Webcam/1.14.37.737",
            isAlreadyAdded     = true,
            discoveryMethod    = com.sentinel.app.domain.model.DiscoveryMethod.MDNS,
            confidence         = com.sentinel.app.domain.model.DiscoveryConfidence.PROBABLE,
            mdnsServiceName    = "IP Webcam._http._tcp.local."
        ),
        DiscoveredDevice(
            ipAddress          = "192.168.1.142",
            hostname           = null,
            port               = 554,
            probableSourceType = CameraSourceType.RTSP,
            openPorts          = listOf(554),
            banner             = null,
            isAlreadyAdded     = false,
            discoveryMethod    = com.sentinel.app.domain.model.DiscoveryMethod.TCP_PORT_PROBE,
            confidence         = com.sentinel.app.domain.model.DiscoveryConfidence.POSSIBLE
        )
    )

    // ── Connection test result preview ───────────────────────────────────────

    val sampleConnectionTestResult = ConnectionTestResult(
        cameraId = "cam_001",
        host = "192.168.1.101",
        resolvedUrl = "rtsp://192.168.1.101:554/stream1",
        success = true,
        latencyMs = 42,
        errorMessage = null,
        streamReachable = false,   // stream-level test not yet implemented
        credentialsAccepted = null
    )

    // ── Room list ────────────────────────────────────────────────────────────

    val sampleRooms = listOf(
        "Exterior", "Living Room", "Kitchen", "Bedroom",
        "Garage", "Basement", "Office", "Hallway"
    )
}
