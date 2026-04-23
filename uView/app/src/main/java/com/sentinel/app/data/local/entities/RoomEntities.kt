package com.sentinel.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sentinel.app.domain.model.CameraSourceType
import com.sentinel.app.domain.model.CameraStatus
import com.sentinel.app.domain.model.CameraEventType
import com.sentinel.app.domain.model.StreamQualityProfile
import com.sentinel.app.domain.model.StreamTransport

// ─────────────────────────────────────────────────────────────────────────────
// CameraEntity
// Persisted representation of a camera configuration.
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "cameras")
data class CameraEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val room: String,
    val sourceType: CameraSourceType,
    val host: String,
    val port: Int,
    val streamPath: String = "",
    val username: String = "",
    val passwordEncrypted: String = "",   // Phase 8: AES-256-GCM via CryptoManager
    val transport: StreamTransport = StreamTransport.AUTO,
    val useTls: Boolean = false,
    val timeoutSeconds: Int = 10,
    val retryCount: Int = 3,
    val preferredQuality: StreamQualityProfile = StreamQualityProfile.AUTO,
    val isEnabled: Boolean = true,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val thumbnailUrl: String? = null,
    val notes: String = "",
    // Android phone specific fields — null for non-phone sources
    val phoneNickname: String? = null,
    val phoneAppMethod: CameraSourceType? = null,
    val phoneEndpointUrl: String? = null,
    val phoneIsLanOnly: Boolean = true,
    val phoneAudioAvailable: Boolean = false,
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Cached status (written by health poller, not user)
    val lastKnownStatus: CameraStatus = CameraStatus.UNKNOWN,
    val lastSuccessfulConnectionMs: Long? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// CameraEventEntity
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "camera_events")
data class CameraEventEntity(
    @PrimaryKey
    val id: String,
    val cameraId: String,
    val cameraName: String,
    val eventType: CameraEventType,
    val timestampMs: Long,
    val description: String = "",
    val thumbnailPath: String? = null,
    val isRead: Boolean = false
)
