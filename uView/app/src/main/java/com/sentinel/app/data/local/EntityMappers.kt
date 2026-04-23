package com.sentinel.app.data.local

import com.sentinel.app.core.security.CryptoManager
import com.sentinel.app.data.local.entities.CameraEntity
import com.sentinel.app.data.local.entities.CameraEventEntity
import com.sentinel.app.domain.model.AndroidPhoneSourceConfig
import com.sentinel.app.domain.model.CameraConnectionProfile
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraEvent
import com.sentinel.app.domain.model.CameraHealthStatus

// ─────────────────────────────────────────────────────────────────────────────
// CameraEntity ↔ CameraDevice
//
// Phase 8 — Credential encryption
//   - On read  (toDomain):  `passwordEncrypted` is decrypted via CryptoManager.
//   - On write (toEntity):  `password` is encrypted via CryptoManager.
//   - If CryptoManager is null (preview/test), passwords pass through as-is.
//   - If decryption fails (corrupt data, key rotation), the password field
//     is set to empty — the user must re-enter credentials.
// ─────────────────────────────────────────────────────────────────────────────

fun CameraEntity.toDomain(cryptoManager: CryptoManager? = null): CameraDevice {
    val decryptedPassword = if (passwordEncrypted.isBlank()) {
        ""
    } else if (cryptoManager != null) {
        cryptoManager.decrypt(passwordEncrypted) ?: ""
    } else {
        // Fallback for preview/test contexts without DI — NOT a production path
        passwordEncrypted
    }

    val connectionProfile = CameraConnectionProfile(
        host = host,
        port = port,
        path = streamPath,
        username = username,
        password = decryptedPassword,
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

fun CameraDevice.toEntity(cryptoManager: CryptoManager? = null): CameraEntity {
    val encryptedPassword = if (connectionProfile.password.isBlank()) {
        ""
    } else if (cryptoManager != null) {
        cryptoManager.encrypt(connectionProfile.password) ?: ""
    } else {
        // Fallback for preview/test contexts without DI — NOT a production path
        connectionProfile.password
    }

    return CameraEntity(
        id = id,
        name = name,
        room = room,
        sourceType = sourceType,
        host = connectionProfile.host,
        port = connectionProfile.port,
        streamPath = connectionProfile.path,
        username = connectionProfile.username,
        passwordEncrypted = encryptedPassword,
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
}

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
