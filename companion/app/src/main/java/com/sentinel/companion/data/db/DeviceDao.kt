package com.sentinel.companion.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sentinel.companion.data.model.DeviceProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    @Query("SELECT * FROM devices ORDER BY isFavorite DESC, addedMs DESC")
    fun observeAll(): Flow<List<DeviceProfile>>

    @Query("SELECT * FROM devices WHERE id = :id")
    fun observeById(id: String): Flow<DeviceProfile?>

    @Query("SELECT * FROM devices WHERE location = :location ORDER BY name")
    fun observeByLocation(location: String): Flow<List<DeviceProfile>>

    @Query("SELECT * FROM devices WHERE isFavorite = 1 ORDER BY name")
    fun observeFavorites(): Flow<List<DeviceProfile>>

    @Query("SELECT * FROM devices WHERE isEnabled = 1 AND state = 'ONLINE' ORDER BY name")
    fun observeOnline(): Flow<List<DeviceProfile>>

    @Query("SELECT DISTINCT location FROM devices ORDER BY location")
    fun observeLocations(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceProfile)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(devices: List<DeviceProfile>)

    @Update
    suspend fun update(device: DeviceProfile)

    @Delete
    suspend fun delete(device: DeviceProfile)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE devices SET state = :state, latencyMs = :latencyMs, lastSeenMs = :lastSeenMs WHERE id = :id")
    suspend fun updateState(id: String, state: String, latencyMs: Int, lastSeenMs: Long)

    @Query("UPDATE devices SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("UPDATE devices SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("SELECT COUNT(*) FROM devices")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM devices WHERE state = 'ONLINE'")
    suspend fun countOnline(): Int
}
