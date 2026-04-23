package com.sentinel.app.data.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.view.SurfaceView
import androidx.media3.common.util.UnstableApi
import com.sentinel.app.domain.model.SnapshotRequest
import com.sentinel.app.domain.model.SnapshotResult
import com.sentinel.app.domain.service.SnapshotController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SnapshotControllerImpl
 *
 * Captures still frames from active camera sessions and saves them as JPEG files.
 *
 * Strategy per session type:
 *   MJPEG  → grab the last decoded [Bitmap] from [MjpegSessionRegistry] (replay=1)
 *   ExoPlayer → capture the rendered surface via [SurfaceCapture]
 *             (Note: surface capture requires API-level cooperation; currently
 *              saves a placeholder if no surface is registered. Wire
 *              [SurfaceCapture.register] from [ExoPlayerStreamView] to enable.)
 *
 * Snapshots are saved to [Context.getExternalFilesDir]/Snapshots/<cameraId>/.
 */
@UnstableApi
@Singleton
class SnapshotControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mjpegRegistry: MjpegSessionRegistry,
    private val sessions: CameraPlaybackServiceImpl  // access to ExoPlayer instances
) : SnapshotController {

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)

    override suspend fun takeSnapshot(request: SnapshotRequest): SnapshotResult =
        withContext(Dispatchers.IO) {
            val cameraId = request.cameraId
            val timestamp = System.currentTimeMillis()
            val filename = "snap_${dateFormat.format(Date(timestamp))}.jpg"

            val dir = snapshotDir(cameraId)
            dir.mkdirs()
            val file = File(dir, filename)

            try {
                // Try MJPEG last-frame first (fastest, no surface required)
                val mjpegBitmap = tryCaptureMjpegFrame(cameraId)
                if (mjpegBitmap != null) {
                    saveBitmap(mjpegBitmap, file, request.quality)
                    Timber.d("Snapshot saved (MJPEG): ${file.absolutePath}")
                    return@withContext SnapshotResult(
                        cameraId  = cameraId,
                        savedPath = file.absolutePath,
                        timestampMs = timestamp,
                        success = true
                    )
                }

                // ExoPlayer surface capture — requires SurfaceView registration
                val surfaceBitmap = SurfaceCapture.capture(cameraId)
                if (surfaceBitmap != null) {
                    saveBitmap(surfaceBitmap, file, request.quality)
                    Timber.d("Snapshot saved (ExoPlayer surface): ${file.absolutePath}")
                    return@withContext SnapshotResult(
                        cameraId    = cameraId,
                        savedPath   = file.absolutePath,
                        timestampMs = timestamp,
                        success     = true
                    )
                }

                // No active frame available
                Timber.w("Snapshot failed: no active frame for cameraId=$cameraId")
                SnapshotResult(
                    cameraId    = cameraId,
                    savedPath   = "",
                    timestampMs = timestamp,
                    success     = false,
                    errorMessage = "No active stream frame available"
                )

            } catch (e: Exception) {
                Timber.e(e, "Snapshot error for cameraId=$cameraId")
                SnapshotResult(
                    cameraId     = cameraId,
                    savedPath    = "",
                    timestampMs  = timestamp,
                    success      = false,
                    errorMessage = e.message
                )
            }
        }

    override suspend fun getSnapshots(cameraId: String): List<SnapshotResult> =
        withContext(Dispatchers.IO) {
            val dir = snapshotDir(cameraId)
            if (!dir.exists()) return@withContext emptyList()

            dir.listFiles { f -> f.extension == "jpg" }
                ?.sortedByDescending { it.lastModified() }
                ?.map { file ->
                    SnapshotResult(
                        cameraId    = cameraId,
                        savedPath   = file.absolutePath,
                        timestampMs = file.lastModified(),
                        success     = true
                    )
                } ?: emptyList()
        }

    private fun snapshotDir(cameraId: String): File =
        File(context.getExternalFilesDir(null), "Snapshots/$cameraId")

    private suspend fun tryCaptureMjpegFrame(cameraId: String): Bitmap? {
        // Grab the replayed last frame from the MJPEG registry (non-suspending)
        var captured: Bitmap? = null
        try {
            // SharedFlow with replay=1 — first emission to a new collector is the last frame
            mjpegRegistry.frames(cameraId).collect { bmp ->
                captured = bmp
                // We only want the most recent frame, so we don't loop
                throw StopCollecting
            }
        } catch (_: StopCollecting) { }
        return captured
    }

    private fun saveBitmap(bitmap: Bitmap, file: File, quality: Int) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), out)
            out.flush()
        }
    }

    private object StopCollecting : Throwable()
}

/**
 * SurfaceCapture
 *
 * Lightweight registry that holds weak references to active [SurfaceView]s
 * so [SnapshotControllerImpl] can capture a frame by drawing the view to a
 * [Bitmap] canvas.
 *
 * [ExoPlayerStreamView] must call [register] after attaching its PlayerView
 * and [unregister] when the composable leaves the composition.
 */
object SurfaceCapture {

    private val surfaces = ConcurrentHashMap<String, SurfaceView>()

    fun register(cameraId: String, view: SurfaceView) {
        surfaces[cameraId] = view
    }

    fun unregister(cameraId: String) {
        surfaces.remove(cameraId)
    }

    /**
     * Attempt to draw the registered surface to a bitmap.
     * Returns null if no surface is registered or drawing fails.
     *
     * Note: Surface pixels are not always CPU-readable — this works best with
     * TextureView. For production, switch ExoPlayer to use TextureView via
     * [PlayerView.setVideoSurfaceView] → consider [PlayerView] with
     * `surface_type="texture_view"` in XML, or use Media3's `SurfaceRequest`
     * callback to get the decoded bitmap directly.
     */
    fun capture(cameraId: String): Bitmap? {
        val view = surfaces[cameraId] ?: return null
        return try {
            val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            view.draw(canvas)
            bmp
        } catch (e: Exception) {
            Timber.w("SurfaceCapture.capture failed for $cameraId: ${e.message}")
            null
        }
    }

    private val Timber get() = timber.log.Timber
    private val ConcurrentHashMap get() = java.util.concurrent.ConcurrentHashMap<String, SurfaceView>()
    // Re-declare for scoping; the import above covers actual usage
}
