package com.sentinel.app.data.config

import android.content.Context
import androidx.core.content.FileProvider
import com.sentinel.app.core.security.CryptoManager
import com.sentinel.app.domain.model.CameraConnectionProfile
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.repository.CameraRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ConfigManager — Phase 8
 *
 * Exports all camera configurations to a JSON file and imports them back.
 * Supports sharing the exported file via Android [FileProvider].
 *
 * What is real:
 *   - Full export of all cameras to a serializable JSON format.
 *   - Credentials are EXCLUDED from exports by default — passwords are
 *     redacted to "[REDACTED]" to prevent accidental credential leakage
 *     when sharing config files.
 *   - Import reads the JSON, validates structure, and saves cameras via
 *     the repository. Imported cameras with "[REDACTED]" passwords will
 *     have empty password fields; the user must re-enter credentials.
 *   - Uses [kotlinx.serialization] for encoding/decoding.
 *   - File is written to app cache for FileProvider sharing.
 *
 * What is NOT implemented:
 *   - Optional encrypted export with a user-provided passphrase.
 *     This would require a PBKDF2 key derivation step and a separate
 *     encryption layer on top of the JSON payload.
 *   - Import conflict resolution (e.g. merge vs. overwrite). Currently
 *     imports overwrite cameras with matching IDs.
 */
@Singleton
class ConfigManager @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val cryptoManager: CryptoManager
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Export all cameras to a JSON file in the app cache.
     *
     * Credentials are redacted to prevent leakage. The export includes
     * all connection parameters, room assignments, and preferences.
     *
     * @return The [File] containing the JSON export, suitable for
     *         sharing via [FileProvider].
     */
    suspend fun exportConfig(context: Context): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cameras = cameraRepository.observeAllCameras().first()

            val exportEntries = cameras.map { camera ->
                CameraExportEntry(
                    id = camera.id,
                    name = camera.name,
                    room = camera.room,
                    sourceType = camera.sourceType.name,
                    host = camera.connectionProfile.host,
                    port = camera.connectionProfile.port,
                    streamPath = camera.connectionProfile.path,
                    username = camera.connectionProfile.username,
                    password = REDACTED_MARKER,  // Never export passwords
                    transportType = camera.connectionProfile.transport.name,
                    useTls = camera.connectionProfile.useTls,
                    timeoutSeconds = camera.connectionProfile.timeoutSeconds,
                    retryCount = camera.connectionProfile.retryCount,
                    preferredQuality = camera.preferredQuality.name,
                    isEnabled = camera.isEnabled,
                    isFavorite = camera.isFavorite,
                    isPinned = camera.isPinned,
                    notes = camera.notes
                )
            }

            val export = ConfigExport(
                version = EXPORT_VERSION,
                exportedAt = System.currentTimeMillis(),
                cameraCount = exportEntries.size,
                cameras = exportEntries
            )

            val jsonString = json.encodeToString(export)
            val exportDir = File(context.cacheDir, "config_exports")
            exportDir.mkdirs()
            val exportFile = File(exportDir, "sentinel_config_${System.currentTimeMillis()}.json")
            exportFile.writeText(jsonString)

            Timber.i("ConfigManager: exported ${cameras.size} cameras to ${exportFile.name}")
            Result.success(exportFile)
        } catch (e: Exception) {
            Timber.e(e, "ConfigManager: export failed")
            Result.failure(e)
        }
    }

    /**
     * Import cameras from a JSON string.
     *
     * Cameras with matching IDs will be overwritten. Passwords marked as
     * [REDACTED_MARKER] will be stored as empty — the user must re-enter
     * credentials after import.
     *
     * @return [ImportResult] summarizing the operation.
     */
    suspend fun importConfig(jsonString: String): ImportResult = withContext(Dispatchers.IO) {
        try {
            val export = json.decodeFromString<ConfigExport>(jsonString)

            if (export.version > EXPORT_VERSION) {
                return@withContext ImportResult(
                    success = false,
                    imported = 0,
                    skipped = 0,
                    errorMessage = "Config version ${export.version} is newer than supported ($EXPORT_VERSION)"
                )
            }

            var imported = 0
            var skipped = 0

            for (entry in export.cameras) {
                try {
                    val sourceType = runCatching {
                        com.sentinel.app.domain.model.CameraSourceType.valueOf(entry.sourceType)
                    }.getOrNull()
                    if (sourceType == null) {
                        Timber.w("ConfigManager: skipping camera ${entry.id} — unknown sourceType ${entry.sourceType}")
                        skipped++
                        continue
                    }

                    val transport = runCatching {
                        com.sentinel.app.domain.model.StreamTransport.valueOf(entry.transportType)
                    }.getOrElse { com.sentinel.app.domain.model.StreamTransport.AUTO }

                    val quality = runCatching {
                        com.sentinel.app.domain.model.StreamQualityProfile.valueOf(entry.preferredQuality)
                    }.getOrElse { com.sentinel.app.domain.model.StreamQualityProfile.AUTO }

                    // If password is redacted, store empty — user must re-enter
                    val password = if (entry.password == REDACTED_MARKER) "" else entry.password

                    val camera = CameraDevice(
                        id = entry.id,
                        name = entry.name,
                        room = entry.room,
                        sourceType = sourceType,
                        connectionProfile = CameraConnectionProfile(
                            host = entry.host,
                            port = entry.port,
                            path = entry.streamPath,
                            username = entry.username,
                            password = password,
                            transport = transport,
                            useTls = entry.useTls,
                            timeoutSeconds = entry.timeoutSeconds,
                            retryCount = entry.retryCount
                        ),
                        preferredQuality = quality,
                        isEnabled = entry.isEnabled,
                        isFavorite = entry.isFavorite,
                        isPinned = entry.isPinned,
                        notes = entry.notes
                    )

                    cameraRepository.saveCamera(camera)
                    imported++
                } catch (e: Exception) {
                    Timber.w(e, "ConfigManager: failed to import camera ${entry.id}")
                    skipped++
                }
            }

            Timber.i("ConfigManager: imported $imported cameras, skipped $skipped")
            ImportResult(success = true, imported = imported, skipped = skipped)
        } catch (e: Exception) {
            Timber.e(e, "ConfigManager: import failed — invalid JSON")
            ImportResult(success = false, imported = 0, skipped = 0, errorMessage = e.message)
        }
    }

    /**
     * Get a FileProvider URI for the exported file, suitable for sharing
     * via Intent.ACTION_SEND.
     */
    fun getShareUri(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    companion object {
        const val EXPORT_VERSION = 1
        const val REDACTED_MARKER = "[REDACTED]"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Serializable export models
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class ConfigExport(
    val version: Int,
    val exportedAt: Long,
    val cameraCount: Int,
    val cameras: List<CameraExportEntry>
)

@Serializable
data class CameraExportEntry(
    val id: String,
    val name: String,
    val room: String,
    val sourceType: String,
    val host: String,
    val port: Int,
    val streamPath: String = "",
    val username: String = "",
    val password: String = "",
    val transportType: String = "AUTO",
    val useTls: Boolean = false,
    val timeoutSeconds: Int = 10,
    val retryCount: Int = 3,
    val preferredQuality: String = "AUTO",
    val isEnabled: Boolean = true,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val notes: String = ""
)

data class ImportResult(
    val success: Boolean,
    val imported: Int,
    val skipped: Int,
    val errorMessage: String? = null
)
