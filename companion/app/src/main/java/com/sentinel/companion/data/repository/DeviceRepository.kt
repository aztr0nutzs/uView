package com.sentinel.companion.data.repository

import com.sentinel.companion.data.db.DeviceDao
import com.sentinel.companion.data.model.DeviceProfile
import com.sentinel.companion.data.model.DeviceState
import com.sentinel.companion.security.CredentialCipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for camera devices. Passwords are stored as
 * Keystore-encrypted ciphertext in the DB; this repository transparently
 * encrypts on write and decrypts on read so consumers never see ciphertext.
 *
 * Legacy plaintext rows (from before encryption shipped) are detected by the
 * absence of the `ENC1:` envelope marker and rewritten through [migrateLegacyCredentials].
 */
@Singleton
class DeviceRepository @Inject constructor(
    private val dao: DeviceDao,
    private val cipher: CredentialCipher,
) {
    val devices: Flow<List<DeviceProfile>> = dao.observeAll().map { list -> list.map(::decryptForRead) }
    val favorites: Flow<List<DeviceProfile>> = dao.observeFavorites().map { list -> list.map(::decryptForRead) }
    val online: Flow<List<DeviceProfile>> = dao.observeOnline().map { list -> list.map(::decryptForRead) }
    val locations: Flow<List<String>> = dao.observeLocations()

    fun observeDevice(id: String): Flow<DeviceProfile?> =
        dao.observeById(id).map { it?.let(::decryptForRead) }

    fun observeByLocation(location: String): Flow<List<DeviceProfile>> =
        dao.observeByLocation(location).map { list -> list.map(::decryptForRead) }

    suspend fun save(device: DeviceProfile) = dao.insert(encryptForWrite(device))
    suspend fun update(device: DeviceProfile) = dao.update(encryptForWrite(device))
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
    }

    suspend fun updateState(id: String, state: DeviceState, latencyMs: Int = 0) {
        dao.updateState(id, state.name, latencyMs, System.currentTimeMillis())
    }

    suspend fun count(): Int = dao.count()
    suspend fun countOnline(): Int = dao.countOnline()

    /**
     * One-shot pass that finds any device row whose password is still stored as
     * plaintext (no `ENC1:` envelope) and re-saves it through the cipher. Safe to
     * call repeatedly — encrypted rows are skipped.
     */
    suspend fun migrateLegacyCredentials(): Int {
        val all = dao.observeAll().first()
        var migrated = 0
        for (raw in all) {
            if (raw.password.isNotEmpty() && !cipher.isEncrypted(raw.password)) {
                try {
                    dao.update(raw.copy(password = cipher.encrypt(raw.password)))
                    migrated++
                } catch (e: Exception) {
                    Timber.w(e, "Skipping legacy credential migration for device %s", raw.id)
                }
            }
        }
        if (migrated > 0) Timber.i("Migrated %d legacy plaintext credentials", migrated)
        return migrated
    }

    private fun encryptForWrite(device: DeviceProfile): DeviceProfile {
        if (device.password.isEmpty() || cipher.isEncrypted(device.password)) return device
        return device.copy(password = cipher.encrypt(device.password))
    }

    private fun decryptForRead(device: DeviceProfile): DeviceProfile {
        if (device.password.isEmpty() || !cipher.isEncrypted(device.password)) return device
        return try {
            device.copy(password = cipher.decrypt(device.password))
        } catch (e: Exception) {
            // If decryption fails (e.g. Keystore key was wiped on factory reset), surface
            // the device with an empty password rather than crash. UI will treat as
            // "credentials required" and prompt the user to re-enter.
            Timber.w(e, "Decrypt failed for device %s — clearing in-memory password", device.id)
            device.copy(password = "")
        }
    }
}
