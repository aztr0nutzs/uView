package com.sentinel.companion.data.repository

import com.sentinel.companion.data.model.Alert
import com.sentinel.companion.data.model.Camera
import com.sentinel.companion.data.model.CameraStatus
import com.sentinel.companion.data.model.MockData
import com.sentinel.companion.data.model.SystemStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraRepository @Inject constructor() {

    private val _cameras = MutableStateFlow(MockData.cameras)
    private val _alerts  = MutableStateFlow(MockData.alerts)

    val cameras: Flow<List<Camera>> = _cameras.asStateFlow()
    val alerts:  Flow<List<Alert>>  = _alerts.asStateFlow()

    val systemStatus: Flow<SystemStatus> = _cameras.map { cams ->
        SystemStatus(
            isConnected      = true,
            hostAddress      = "192.168.1.100",
            totalCameras     = cams.size,
            onlineCameras    = cams.count { it.statusEnum() == CameraStatus.ONLINE },
            offlineCameras   = cams.count { it.statusEnum() == CameraStatus.OFFLINE },
            connectingCameras= cams.count { it.statusEnum() == CameraStatus.CONNECTING },
            disabledCameras  = cams.count { it.statusEnum() == CameraStatus.DISABLED },
            unreadAlerts     = _alerts.value.count { !it.isRead },
            uptimeMs         = 14_400_000L,
            lastSyncMs       = System.currentTimeMillis(),
        )
    }

    fun getCameraById(id: String): Flow<Camera?> =
        _cameras.map { list -> list.firstOrNull { it.id == id } }

    fun getAlertsByCameraId(cameraId: String): Flow<List<Alert>> =
        _alerts.map { list -> list.filter { it.cameraId == cameraId } }

    suspend fun toggleFavorite(cameraId: String) {
        _cameras.value = _cameras.value.map { cam ->
            if (cam.id == cameraId) cam.copy(isFavorite = !cam.isFavorite) else cam
        }
    }

    suspend fun toggleEnabled(cameraId: String) {
        _cameras.value = _cameras.value.map { cam ->
            if (cam.id == cameraId) {
                val nextEnabled = !cam.isEnabled
                cam.copy(
                    isEnabled = nextEnabled,
                    status = if (nextEnabled) CameraStatus.CONNECTING.name else CameraStatus.DISABLED.name,
                )
            } else cam
        }
    }

    suspend fun reconnectCamera(cameraId: String) {
        _cameras.value = _cameras.value.map { cam ->
            if (cam.id == cameraId) cam.copy(status = CameraStatus.CONNECTING.name) else cam
        }
        delay(1500)
        _cameras.value = _cameras.value.map { cam ->
            if (cam.id == cameraId) cam.copy(status = CameraStatus.ONLINE.name, latencyMs = (30..150).random()) else cam
        }
    }

    suspend fun markAlertRead(alertId: String) {
        _alerts.value = _alerts.value.map { a ->
            if (a.id == alertId) a.copy(isRead = true) else a
        }
    }

    suspend fun markAllAlertsRead() {
        _alerts.value = _alerts.value.map { it.copy(isRead = true) }
    }

    suspend fun addCamera(camera: Camera) {
        _cameras.value = _cameras.value + camera
    }

    suspend fun deleteCamera(cameraId: String) {
        _cameras.value = _cameras.value.filter { it.id != cameraId }
    }
}
