package com.sentinel.app.data.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.SurfaceView
import androidx.media3.common.util.UnstableApi
import com.sentinel.app.domain.model.SnapshotRequest
import com.sentinel.app.domain.model.SnapshotResult
import com.sentinel.app.domain.service.SnapshotController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Captures still frames from active camera sessions and saves them as JPEGs.
 *
 * What is real:
 * - MJPEG snapshots are persisted from the latest decoded bitmap in
 *   [MjpegSessionRegistry].
 * - ExoPlayer snapshots are attempted only when a real [SurfaceView] has been
 *   registered through [SurfaceCapture].
 *
 * What is deliberately not faked:
 * - No placeholder image is written. If no frame can be read, this returns a
 *   failed [SnapshotResult] and callers must not log a success event.
 */
@UnstableApi
@Singleton
class SnapshotControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mjpegRegistry: MjpegSessionRegistry,
    @Suppress("unused") private val sessions: CameraPlaybackServiceImpl
) : SnapshotController {

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)

    override suspend fun takeSnapshot(request: SnapshotRequest): SnapshotResult =
        withContext(Dispatchers.IO) {
            val cameraId = request.cameraId
            val timestamp = System.currentTimeMillis()
            val file = File(snapshotDir(cameraId), "snap_${dateFormat.format(Date(timestamp))}.jpg")

            try {
                val bitmap = tryCaptureMjpegFrame(cameraId) ?: SurfaceCapture.capture(cameraId)
                if (bitmap == null) {
                    Timber.w("Snapshot failed: no active frame for cameraId=$cameraId")
                    return@withContext SnapshotResult(
                        cameraId = cameraId,
                        savedPath = "",
                        timestampMs = timestamp,
                        success = false,
                        errorMessage = "No active stream frame available"
                    )
                }

                saveBitmap(bitmap, file, request.quality).getOrThrow()
                Timber.d("Snapshot saved: ${file.absolutePath}")
                SnapshotResult(
                    cameraId = cameraId,
                    savedPath = file.absolutePath,
                    timestampMs = timestamp,
                    success = true
                )
            } catch (e: Exception) {
                Timber.e(e, "Snapshot error for cameraId=$cameraId")
                file.delete()
                SnapshotResult(
                    cameraId = cameraId,
                    savedPath = "",
                    timestampMs = timestamp,
                    success = false,
                    errorMessage = e.message
                )
            }
        }

    override suspend fun getSnapshots(cameraId: String): List<SnapshotResult> =
        withContext(Dispatchers.IO) {
            val dir = snapshotDir(cameraId)
            if (!dir.exists()) return@withContext emptyList()

            dir.listFiles { file -> file.extension.equals("jpg", ignoreCase = true) }
                ?.filter { it.length() > 0L }
                ?.sortedByDescending { it.lastModified() }
                ?.map { file ->
                    SnapshotResult(
                        cameraId = cameraId,
                        savedPath = file.absolutePath,
                        timestampMs = file.lastModified(),
                        success = true
                    )
                }
                ?: emptyList()
        }

    private fun snapshotDir(cameraId: String): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "Snapshots/$cameraId")

    private suspend fun tryCaptureMjpegFrame(cameraId: String): Bitmap? =
        withTimeoutOrNull(750L) {
            mjpegRegistry.frames(cameraId).firstOrNull()
        }

    private fun saveBitmap(bitmap: Bitmap, file: File, quality: Int): Result<Unit> =
        runCatching {
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), out)
                out.flush()
                if (!compressed || file.length() <= 0L) {
                    throw IllegalStateException("Bitmap compression produced no JPEG data")
                }
            }
        }
}

/**
 * Registry for active rendered surfaces that can be snapshotted.
 *
 * SurfaceView pixels are not always CPU-readable. This path is best-effort and
 * returns null when no registered surface can provide a frame.
 */
object SurfaceCapture {

    private val surfaces = ConcurrentHashMap<String, SurfaceView>()

    fun register(cameraId: String, view: SurfaceView) {
        surfaces[cameraId] = view
    }

    fun unregister(cameraId: String) {
        surfaces.remove(cameraId)
    }

    fun capture(cameraId: String): Bitmap? {
        val view = surfaces[cameraId] ?: return null
        if (view.width <= 0 || view.height <= 0) return null
        return try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            bitmap
        } catch (e: Exception) {
            Timber.w(e, "SurfaceCapture.capture failed for $cameraId")
            null
        }
    }
}
