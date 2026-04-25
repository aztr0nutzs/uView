package com.sentinel.companion.data.model

import java.util.UUID

object MockData {

    val cameras = listOf(
        Camera(
            id = "cam-001",
            name = "FRONT_DOOR",
            room = "Entrance",
            sourceType = SourceType.RTSP.name,
            streamUrl = "rtsp://192.168.1.101:554/stream1",
            status = CameraStatus.ONLINE.name,
            latencyMs = 42,
            isFavorite = true,
        ),
        Camera(
            id = "cam-002",
            name = "GARAGE_NODE",
            room = "Garage",
            sourceType = SourceType.MJPEG.name,
            streamUrl = "http://192.168.1.102:8080/video",
            status = CameraStatus.ONLINE.name,
            latencyMs = 78,
            isFavorite = true,
        ),
        Camera(
            id = "cam-003",
            name = "BACKYARD_UNIT",
            room = "Exterior",
            sourceType = SourceType.ONVIF.name,
            streamUrl = "rtsp://192.168.1.103:554/live",
            status = CameraStatus.OFFLINE.name,
            latencyMs = 0,
        ),
        Camera(
            id = "cam-004",
            name = "LIVING_ROOM",
            room = "Living Room",
            sourceType = SourceType.HLS.name,
            streamUrl = "http://192.168.1.104:8080/stream.m3u8",
            status = CameraStatus.ONLINE.name,
            latencyMs = 120,
        ),
        Camera(
            id = "cam-005",
            name = "OFFICE_RECON",
            room = "Office",
            sourceType = SourceType.DROIDCAM.name,
            streamUrl = "http://192.168.1.105:4747/video",
            status = CameraStatus.CONNECTING.name,
            latencyMs = 0,
        ),
        Camera(
            id = "cam-006",
            name = "BASEMENT_UNIT",
            room = "Basement",
            sourceType = SourceType.IP_WEBCAM.name,
            streamUrl = "http://192.168.1.106:8080/video",
            status = CameraStatus.DISABLED.name,
            latencyMs = 0,
            isEnabled = false,
        ),
    )

    val alerts = listOf(
        Alert(
            id = UUID.randomUUID().toString(),
            cameraId = "cam-003",
            cameraName = "BACKYARD_UNIT",
            type = AlertType.CONNECTION_LOST.name,
            message = "Connection to BACKYARD_UNIT lost. Attempting reconnect.",
            timestampMs = System.currentTimeMillis() - 5 * 60_000,
            isRead = false,
        ),
        Alert(
            id = UUID.randomUUID().toString(),
            cameraId = "cam-001",
            cameraName = "FRONT_DOOR",
            type = AlertType.MOTION.name,
            message = "Motion detected at FRONT_DOOR.",
            timestampMs = System.currentTimeMillis() - 12 * 60_000,
            isRead = false,
        ),
        Alert(
            id = UUID.randomUUID().toString(),
            cameraId = "cam-002",
            cameraName = "GARAGE_NODE",
            type = AlertType.SNAPSHOT.name,
            message = "Snapshot captured from GARAGE_NODE.",
            timestampMs = System.currentTimeMillis() - 38 * 60_000,
            isRead = true,
        ),
        Alert(
            id = UUID.randomUUID().toString(),
            cameraId = "cam-001",
            cameraName = "FRONT_DOOR",
            type = AlertType.RECORDING_STARTED.name,
            message = "Recording started on FRONT_DOOR (triggered by motion).",
            timestampMs = System.currentTimeMillis() - 2 * 3600_000,
            isRead = true,
        ),
        Alert(
            id = UUID.randomUUID().toString(),
            cameraId = "cam-003",
            cameraName = "BACKYARD_UNIT",
            type = AlertType.CONNECTION_RESTORED.name,
            message = "BACKYARD_UNIT came back online.",
            timestampMs = System.currentTimeMillis() - 5 * 3600_000,
            isRead = true,
        ),
    )
}
