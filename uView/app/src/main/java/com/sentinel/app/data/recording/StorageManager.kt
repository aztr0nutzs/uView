package com.sentinel.app.data.recording

import android.content.Context
import android.os.Environment
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * StorageManager — Phase 7
 *
 * Tracks storage used by Sentinel recordings and manages cleanup.
 *
 * Responsibilities:
 *   - Report total bytes used by recordings
 *   - Report available bytes on the storage volume
 *   - Delete oldest recordings when free space falls below threshold
 *   - List all recordings across all cameras
 *   - Delete individual recordings
 */
@Singleton
class StorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /** Minimum free space to maintain — if below this, prune oldest recordings. */
        private const val MIN_FREE_BYTES = 500L * 1024 * 1024  // 500 MB
    }

    data class StorageStats(
        val usedByRecordingsMb: Long,
        val totalDeviceStorageMb: Long,
        val freeDeviceStorageMb: Long,
        val recordingCount: Int
    )

    data class RecordingFile(
        val path: String,
        val cameraId: String,
        val name: String,
        val sizeBytes: Long,
        val createdAt: Long
    )

    /** Base directory for all Sentinel recordings. */
    val recordingsBaseDir: File
        get() {
            val base = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: context.filesDir
            return File(base, "SentinelRecordings")
        }

    /**
     * Compute current storage statistics.
     */
    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val allFiles = listAllRecordings()
        val usedBytes = allFiles.sumOf { it.sizeBytes }

        val stat = try {
            StatFs(recordingsBaseDir.absolutePath)
        } catch (_: Exception) {
            StatFs(context.filesDir.absolutePath)
        }
        val total = stat.totalBytes
        val free  = stat.availableBytes

        StorageStats(
            usedByRecordingsMb    = usedBytes / (1024 * 1024),
            totalDeviceStorageMb  = total / (1024 * 1024),
            freeDeviceStorageMb   = free / (1024 * 1024),
            recordingCount        = allFiles.size
        )
    }

    /**
     * List all recordings across all cameras, newest first.
     */
    suspend fun listAllRecordings(): List<RecordingFile> = withContext(Dispatchers.IO) {
        if (!recordingsBaseDir.exists()) return@withContext emptyList()

        recordingsBaseDir.walkTopDown()
            .filter { it.isFile && it.extension == "mp4" }
            .map { file ->
                RecordingFile(
                    path      = file.absolutePath,
                    cameraId  = file.parentFile?.name ?: "unknown",
                    name      = file.name,
                    sizeBytes = file.length(),
                    createdAt = file.lastModified()
                )
            }
            .sortedByDescending { it.createdAt }
            .toList()
    }

    /**
     * List recordings for a specific camera.
     */
    suspend fun listRecordingsForCamera(cameraId: String): List<RecordingFile> =
        listAllRecordings().filter { it.cameraId == cameraId.take(12) }

    /**
     * Delete a specific recording file.
     */
    suspend fun deleteRecording(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext false
        val deleted = file.delete()
        Timber.d("StorageManager: deleted $path → $deleted")
        deleted
    }

    /**
     * Prune oldest recordings until free space is above [MIN_FREE_BYTES].
     * Returns how many files were deleted.
     */
    suspend fun pruneIfNeeded(): Int = withContext(Dispatchers.IO) {
        var deleted = 0
        val recordings = listAllRecordings()
        val stat = try { StatFs(recordingsBaseDir.absolutePath) }
                   catch (_: Exception) { return@withContext 0 }

        var free = stat.availableBytes
        val sortedOldest = recordings.sortedBy { it.createdAt }

        for (rec in sortedOldest) {
            if (free >= MIN_FREE_BYTES) break
            val file = File(rec.path)
            if (file.delete()) {
                free += rec.sizeBytes
                deleted++
                Timber.d("StorageManager: pruned ${rec.name} (${rec.sizeBytes / 1024}KB)")
            }
        }

        if (deleted > 0) Timber.i("StorageManager: pruned $deleted recordings to free space")
        deleted
    }

    /**
     * Delete ALL recordings for a specific camera (e.g. when camera is removed).
     */
    suspend fun deleteAllForCamera(cameraId: String): Int = withContext(Dispatchers.IO) {
        val dir = File(recordingsBaseDir, cameraId.take(12))
        if (!dir.exists()) return@withContext 0
        val files = dir.listFiles { f -> f.extension == "mp4" } ?: return@withContext 0
        var count = 0
        files.forEach { if (it.delete()) count++ }
        Timber.d("StorageManager: deleted $count recordings for $cameraId")
        count
    }
}
