package com.sentinel.companion.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// NOTE: The legacy `Camera` / `CameraStatus` / `SourceType` types lived here.
// They were removed once DeviceProfile / DeviceDao / DeviceRepository became
// the single source of truth for companion devices — see DeviceProfile.kt.

// ─── Alert ───────────────────────────────────────────────────────────────────

enum class AlertType(val label: String, val icon: String) {
    MOTION("Motion Detected", "motion"),
    CONNECTION_LOST("Connection Lost", "wifi_off"),
    CONNECTION_RESTORED("Connection Restored", "wifi"),
    RECORDING_STARTED("Recording Started", "fiber_manual_record"),
    RECORDING_STOPPED("Recording Stopped", "stop"),
    SNAPSHOT("Snapshot Captured", "photo_camera"),
    SYSTEM("System", "info"),
}

@Entity(tableName = "alerts")
@Serializable
data class Alert(
    @PrimaryKey val id: String,
    val cameraId: String,
    val cameraName: String,
    val type: String,
    val message: String,
    val timestampMs: Long,
    val isRead: Boolean = false,
) {
    fun typeEnum(): AlertType =
        AlertType.entries.firstOrNull { it.name == type } ?: AlertType.SYSTEM
}

// ─── System Status ───────────────────────────────────────────────────────────

data class SystemStatus(
    val isConnected: Boolean = false,
    val hostAddress: String = "",
    val totalCameras: Int = 0,
    val onlineCameras: Int = 0,
    val offlineCameras: Int = 0,
    val connectingCameras: Int = 0,
    val disabledCameras: Int = 0,
    val unreadAlerts: Int = 0,
    val uptimeMs: Long = 0L,
    val lastSyncMs: Long = 0L,
)

// ─── Connection Preferences ──────────────────────────────────────────────────

data class ConnectionPrefs(
    val hostAddress: String = "",
    val port: Int = 8080,
    val useHttps: Boolean = false,
    val autoConnect: Boolean = true,
    val lastConnectedMs: Long = 0L,
)

// ─── App Preferences ─────────────────────────────────────────────────────────

data class AppPrefs(
    val darkTheme: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val motionAlertsEnabled: Boolean = true,
    val connectionAlertsEnabled: Boolean = true,
    val dataSaverMode: Boolean = false,
    val streamQuality: StreamQuality = StreamQuality.AUTO,
    val biometricLock: Boolean = false,
    val localOnlyMode: Boolean = false,
)

enum class StreamQuality(val label: String) {
    AUTO("Auto"),
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low"),
}
