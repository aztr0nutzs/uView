package com.sentinel.companion.data.repository

import com.sentinel.companion.data.db.DeviceDao
import com.sentinel.companion.data.model.DeviceProfile
import com.sentinel.companion.data.model.DeviceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val dao: DeviceDao,
) {
    val devices: Flow<List<DeviceProfile>> = dao.observeAll()
    val favorites: Flow<List<DeviceProfile>> = dao.observeFavorites()
    val online: Flow<List<DeviceProfile>> = dao.observeOnline()
    val locations: Flow<List<String>> = dao.observeLocations()

    fun observeDevice(id: String): Flow<DeviceProfile?> = dao.observeById(id)
    fun observeByLocation(location: String): Flow<List<DeviceProfile>> = dao.observeByLocation(location)

    suspend fun save(device: DeviceProfile) = dao.insert(device)

    suspend fun update(device: DeviceProfile) = dao.update(device)

    suspend fun delete(id: String) = dao.deleteById(id)

    suspend fun setFavorite(id: String, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun setEnabled(id: String, enabled: Boolean) {
        dao.setEnabled(id, enabled)
        if (!enabled) {
            dao.updateState(id, DeviceState.DISABLED.name, 0, System.currentTimeMillis())
        } else {
            dao.updateState(id, DeviceState.CONNECTING.name, 0, System.currentTimeMillis())
        }
    }

    suspend fun reconnect(id: String) {
        dao.updateState(id, DeviceState.CONNECTING.name, 0, System.currentTimeMillis())
        delay(1500)
        dao.updateState(id, DeviceState.ONLINE.name, (20..150).random(), System.currentTimeMillis())
    }

    suspend fun updateState(id: String, state: DeviceState, latencyMs: Int = 0) {
        dao.updateState(id, state.name, latencyMs, System.currentTimeMillis())
    }

    suspend fun count(): Int = dao.count()
    suspend fun countOnline(): Int = dao.countOnline()
}
