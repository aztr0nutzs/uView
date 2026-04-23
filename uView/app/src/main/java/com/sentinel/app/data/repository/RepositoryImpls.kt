package com.sentinel.app.data.repository

import com.sentinel.app.data.local.dao.CameraDao
import com.sentinel.app.data.local.dao.CameraEventDao
import com.sentinel.app.data.local.toDomain
import com.sentinel.app.data.local.toEntity
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraEvent
import com.sentinel.app.domain.model.DashboardSummary
import com.sentinel.app.domain.repository.CameraEventRepository
import com.sentinel.app.domain.repository.CameraRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// CameraRepositoryImpl
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class CameraRepositoryImpl @Inject constructor(
    private val cameraDao: CameraDao
) : CameraRepository {

    override fun observeAllCameras(): Flow<List<CameraDevice>> =
        cameraDao.observeAllCameras().map { list -> list.map { it.toDomain() } }

    override fun observeCamerasByRoom(room: String): Flow<List<CameraDevice>> =
        cameraDao.observeCamerasByRoom(room).map { list -> list.map { it.toDomain() } }

    override fun observeFavoriteCameras(): Flow<List<CameraDevice>> =
        cameraDao.observeFavoriteCameras().map { list -> list.map { it.toDomain() } }

    override suspend fun getCameraById(id: String): CameraDevice? =
        cameraDao.getCameraById(id)?.toDomain()

    override suspend fun saveCamera(camera: CameraDevice) {
        Timber.d("Saving camera: ${camera.id} — ${camera.name}")
        cameraDao.insertCamera(camera.toEntity())
    }

    override suspend fun setCameraEnabled(cameraId: String, enabled: Boolean) {
        cameraDao.setCameraEnabled(cameraId, enabled)
    }

    override suspend fun toggleFavorite(cameraId: String): Boolean {
        val camera = cameraDao.getCameraById(cameraId) ?: return false
        val newState = !camera.isFavorite
        cameraDao.setFavorite(cameraId, newState)
        return newState
    }

    override suspend fun togglePinned(cameraId: String): Boolean {
        val camera = cameraDao.getCameraById(cameraId) ?: return false
        val newState = !camera.isPinned
        cameraDao.setPinned(cameraId, newState)
        return newState
    }

    override suspend fun renameCamera(cameraId: String, newName: String) {
        cameraDao.renameCamera(cameraId, newName)
    }

    override suspend fun assignRoom(cameraId: String, room: String) {
        cameraDao.assignRoom(cameraId, room)
    }

    override suspend fun deleteCamera(cameraId: String) {
        Timber.d("Deleting camera: $cameraId")
        cameraDao.deleteCamera(cameraId)
    }

    override suspend fun getAllRooms(): List<String> =
        cameraDao.getAllRooms()

    override fun observeDashboardSummary(): Flow<DashboardSummary> =
        combine(
            cameraDao.observeTotalCount(),
            cameraDao.observeOnlineCount(),
            cameraDao.observeOfflineCount()
        ) { total, online, offline ->
            DashboardSummary(
                totalCameras = total,
                onlineCameras = online,
                offlineCameras = offline,
                disabledCameras = (total - online - offline).coerceAtLeast(0),
                recentEventCount = 0,           // populated by joining event repo in ViewModel
                hasActiveRecording = false,     // populated by RecordingController
                storageUsedMb = 0L,             // future: storage manager
                storageCapacityMb = 0L
            )
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// CameraEventRepositoryImpl
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class CameraEventRepositoryImpl @Inject constructor(
    private val eventDao: CameraEventDao
) : CameraEventRepository {

    override fun observeEvents(limit: Int): Flow<List<CameraEvent>> =
        eventDao.observeEvents(limit).map { list -> list.map { it.toDomain() } }

    override fun observeEventsForCamera(cameraId: String, limit: Int): Flow<List<CameraEvent>> =
        eventDao.observeEventsForCamera(cameraId, limit).map { list -> list.map { it.toDomain() } }

    override fun observeUnreadCount(): Flow<Int> =
        eventDao.observeUnreadCount()

    override suspend fun addEvent(event: CameraEvent) {
        eventDao.insertEvent(event.toEntity())
    }

    override suspend fun markAsRead(eventIds: List<String>) {
        eventDao.markAsRead(eventIds)
    }

    override suspend fun markAllAsRead() {
        eventDao.markAllAsRead()
    }

    override suspend fun pruneOldEvents(olderThanMs: Long) {
        eventDao.pruneOldEvents(olderThanMs)
    }

    override suspend fun deleteEventsForCamera(cameraId: String) {
        eventDao.deleteEventsForCamera(cameraId)
    }
}
