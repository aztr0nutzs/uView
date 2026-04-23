package com.sentinel.app.data.local

import com.sentinel.app.data.local.entities.CameraEntity
import com.sentinel.app.data.local.entities.CameraEventEntity
import com.sentinel.app.domain.model.AndroidPhoneSourceConfig
import com.sentinel.app.domain.model.CameraConnectionProfile
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraEvent
import com.sentinel.app.domain.model.CameraHealthStatus

// ─────────────────────────────────────────────────────────────────────────────
// CameraEntity ↔ CameraDevice
// ─────────────────────────────────────────────────────────────────────────────

fun CameraEntity.toDomain(): CameraDevice {
    val connectionProfile = CameraConnectionProfile(
        host = host,
        port = port,
        path = streamPath,
        username = username,
        password = passwordEncrypted,   // NOTE: decrypt before use in real impl
        transport = transport,
        useTls = useTls,
        timeoutSeconds = timeoutSeconds,
        retryCount = retryCount
    )
    val androidConfig = if (phoneNickname != null && phoneAppMethod != null && phoneEndpointUrl != null) {
        AndroidPhoneSourceConfig(
            phoneNickname = phoneNickname,
            appMethod = phoneAppMethod,
            endpointUrl = phoneEndpointUrl,
            streamTransport = transport,
            isLanOnly = phoneIsLanOnly,
            audioAvailable = phoneAudioAvailable,
            qualityProfile = preferredQuality
        )
    } else null

    return CameraDevice(
        id = id,
        name = name,
        room = room,
        sourceType = sourceType,
        connectionProfile = connectionProfile,
        androidPhoneConfig = androidConfig,
        preferredQuality = preferredQuality,
        isEnabled = isEnabled,
        isFavorite = isFavorite,
        isPinned = isPinned,
        thumbnailUrl = thumbnailUrl,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        healthStatus = CameraHealthStatus(
            cameraId = id,
            status = lastKnownStatus,
            lastSuccessfulConnectionMs = lastSuccessfulConnectionMs
        )
    )
}

fun CameraDevice.toEntity(): CameraEntity = CameraEntity(
    id = id,
    name = name,
    room = room,
    sourceType = sourceType,
    host = connectionProfile.host,
    port = connectionProfile.port,
    streamPath = connectionProfile.path,
    username = connectionProfile.username,
    passwordEncrypted = connectionProfile.password,   // NOTE: encrypt in real impl
    transport = connectionProfile.transport,
    useTls = connectionProfile.useTls,
    timeoutSeconds = connectionProfile.timeoutSeconds,
    retryCount = connectionProfile.retryCount,
    preferredQuality = preferredQuality,
    isEnabled = isEnabled,
    isFavorite = isFavorite,
    isPinned = isPinned,
    thumbnailUrl = thumbnailUrl,
    notes = notes,
    phoneNickname = androidPhoneConfig?.phoneNickname,
    phoneAppMethod = androidPhoneConfig?.appMethod,
    phoneEndpointUrl = androidPhoneConfig?.endpointUrl,
    phoneIsLanOnly = androidPhoneConfig?.isLanOnly ?: true,
    phoneAudioAvailable = androidPhoneConfig?.audioAvailable ?: false,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastKnownStatus = healthStatus?.status
        ?: com.sentinel.app.domain.model.CameraStatus.UNKNOWN,
    lastSuccessfulConnectionMs = healthStatus?.lastSuccessfulConnectionMs
)

// ─────────────────────────────────────────────────────────────────────────────
// CameraEventEntity ↔ CameraEvent
// ─────────────────────────────────────────────────────────────────────────────

fun CameraEventEntity.toDomain() = CameraEvent(
    id = id,
    cameraId = cameraId,
    cameraName = cameraName,
    eventType = eventType,
    timestampMs = timestampMs,
    description = description,
    thumbnailPath = thumbnailPath,
    isRead = isRead
)

fun CameraEvent.toEntity() = CameraEventEntity(
    id = id,
    cameraId = cameraId,
    cameraName = cameraName,
    eventType = eventType,
    timestampMs = timestampMs,
    description = description,
    thumbnailPath = thumbnailPath,
    isRead = isRead
)
