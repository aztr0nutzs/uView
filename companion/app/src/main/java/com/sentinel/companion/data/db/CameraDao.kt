package com.sentinel.companion.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sentinel.companion.data.model.Camera
import kotlinx.coroutines.flow.Flow

@Dao
interface CameraDao {

    @Query("SELECT * FROM cameras ORDER BY isFavorite DESC, name ASC")
    fun observeAll(): Flow<List<Camera>>

    @Query("SELECT * FROM cameras WHERE id = :id")
    fun observeById(id: String): Flow<Camera?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(camera: Camera)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(cameras: List<Camera>)

    @Query("UPDATE cameras SET status = :status, latencyMs = :latencyMs, lastSeenMs = :lastSeenMs WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, latencyMs: Int, lastSeenMs: Long)

    @Query("UPDATE cameras SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("UPDATE cameras SET isEnabled = :enabled, status = CASE WHEN :enabled THEN 'CONNECTING' ELSE 'DISABLED' END WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM cameras WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM cameras")
    suspend fun count(): Int

    @Query("SELECT * FROM cameras WHERE isEnabled = 1")
    suspend fun snapshotEnabled(): List<Camera>

    @Query("SELECT * FROM cameras WHERE id = :id")
    suspend fun getById(id: String): Camera?
}
