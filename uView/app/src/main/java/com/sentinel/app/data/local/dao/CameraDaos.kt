package com.sentinel.app.data.local.dao

import androidx.room.*
import com.sentinel.app.data.local.entities.CameraEntity
import com.sentinel.app.data.local.entities.CameraEventEntity
import com.sentinel.app.domain.model.CameraStatus
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// CameraDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface CameraDao {

    @Query("SELECT * FROM cameras ORDER BY isPinned DESC, isFavorite DESC, name ASC")
    fun observeAllCameras(): Flow<List<CameraEntity>>

    @Query("SELECT * FROM cameras WHERE room = :room ORDER BY isPinned DESC, name ASC")
    fun observeCamerasByRoom(room: String): Flow<List<CameraEntity>>

    @Query("SELECT * FROM cameras WHERE isFavorite = 1 ORDER BY name ASC")
    fun observeFavoriteCameras(): Flow<List<CameraEntity>>

    @Query("SELECT * FROM cameras WHERE id = :id LIMIT 1")
    suspend fun getCameraById(id: String): CameraEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCamera(camera: CameraEntity)

    @Update
    suspend fun updateCamera(camera: CameraEntity)

    @Query("UPDATE cameras SET isEnabled = :enabled, updatedAt = :now WHERE id = :id")
    suspend fun setCameraEnabled(id: String, enabled: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE cameras SET isFavorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE cameras SET isPinned = :pinned, updatedAt = :now WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE cameras SET name = :name, updatedAt = :now WHERE id = :id")
    suspend fun renameCamera(id: String, name: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE cameras SET room = :room, updatedAt = :now WHERE id = :id")
    suspend fun assignRoom(id: String, room: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE cameras SET lastKnownStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: CameraStatus)

    @Query("UPDATE cameras SET lastSuccessfulConnectionMs = :ts WHERE id = :id")
    suspend fun updateLastSeen(id: String, ts: Long)

    @Query("DELETE FROM cameras WHERE id = :id")
    suspend fun deleteCamera(id: String)

    @Query("SELECT DISTINCT room FROM cameras ORDER BY room ASC")
    suspend fun getAllRooms(): List<String>

    @Query("SELECT COUNT(*) FROM cameras")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cameras WHERE lastKnownStatus = 'ONLINE' AND isEnabled = 1")
    fun observeOnlineCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cameras WHERE lastKnownStatus = 'OFFLINE' AND isEnabled = 1")
    fun observeOfflineCount(): Flow<Int>
}

// ─────────────────────────────────────────────────────────────────────────────
// CameraEventDao
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface CameraEventDao {

    @Query("SELECT * FROM camera_events ORDER BY timestampMs DESC LIMIT :limit")
    fun observeEvents(limit: Int): Flow<List<CameraEventEntity>>

    @Query("SELECT * FROM camera_events WHERE cameraId = :cameraId ORDER BY timestampMs DESC LIMIT :limit")
    fun observeEventsForCamera(cameraId: String, limit: Int): Flow<List<CameraEventEntity>>

    @Query("SELECT COUNT(*) FROM camera_events WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CameraEventEntity)

    @Query("UPDATE camera_events SET isRead = 1 WHERE id IN (:ids)")
    suspend fun markAsRead(ids: List<String>)

    @Query("UPDATE camera_events SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM camera_events WHERE timestampMs < :threshold")
    suspend fun pruneOldEvents(threshold: Long)

    @Query("DELETE FROM camera_events WHERE cameraId = :cameraId")
    suspend fun deleteEventsForCamera(cameraId: String)
}
