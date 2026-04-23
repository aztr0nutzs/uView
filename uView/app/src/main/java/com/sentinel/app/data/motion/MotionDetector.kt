package com.sentinel.app.data.motion

import android.graphics.Bitmap
import android.graphics.Color
import com.sentinel.app.domain.model.MotionAnalysisResult
import com.sentinel.app.domain.model.MotionRegion
import com.sentinel.app.domain.model.MotionSensitivityConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * MotionDetector
 *
 * Frame-difference based motion detection.
 *
 * Algorithm:
 *   1. Scale the incoming frame down to a small analysis resolution
 *      (default 160×90) to reduce CPU load.
 *   2. Convert to grayscale (luminance only — color changes are ignored,
 *      which reduces false positives from auto white balance).
 *   3. Compare each pixel's luminance to the reference frame.
 *   4. Count pixels where |delta| > pixelThreshold as "changed".
 *   5. If changedPixels / totalPixels > motionRatioThreshold → motion detected.
 *   6. Update the reference frame with a weighted blend of old and new
 *      (α=0.1 for slow drift, handling lighting changes over time).
 *
 * Region support:
 *   When [MotionSensitivityConfig.regions] is non-empty, only pixels within
 *   those normalized rectangles are included in the analysis.
 *
 * Threading:
 *   All analysis runs on [Dispatchers.Default] (CPU-bound). Never called on
 *   the main thread. The MJPEG flow and ExoPlayer frame callbacks feed frames
 *   from IO/main respectively — callers must dispatch to Default before calling.
 *
 * Performance targets:
 *   - 160×90 analysis resolution: ~2–5ms per frame on a mid-range device.
 *   - 320×180: ~8–15ms. Do not go above 320×180 for real-time use.
 */
@Singleton
class MotionDetector @Inject constructor() {

    companion object {
        /** Width of the analysis frame. Lower = faster, less accurate. */
        private const val ANALYSIS_WIDTH  = 160
        private const val ANALYSIS_HEIGHT = 90

        /** Blend factor for reference frame update. 0.0 = never update, 1.0 = full replace. */
        private const val REFERENCE_BLEND_ALPHA = 0.05f
    }

    // Per-camera reference frames (grayscale float array, size = ANALYSIS_WIDTH * ANALYSIS_HEIGHT)
    private val referenceFrames = mutableMapOf<String, FloatArray>()

    /**
     * Analyze a new frame for motion against the reference frame.
     *
     * @param cameraId  Camera identifier — used to look up the per-camera reference.
     * @param frame     Current video frame as a [Bitmap].
     * @param config    Sensitivity configuration for this camera.
     * @return          [MotionAnalysisResult] describing what was detected.
     */
    suspend fun analyze(
        cameraId: String,
        frame: Bitmap,
        config: MotionSensitivityConfig
    ): MotionAnalysisResult = withContext(Dispatchers.Default) {
        val now = System.currentTimeMillis()

        if (!config.enabled) {
            return@withContext MotionAnalysisResult(
                cameraId        = cameraId,
                timestampMs     = now,
                motionDetected  = false,
                motionRatio     = 0f,
                peakDelta       = 0,
                triggeredRegion = null
            )
        }

        // Scale frame down to analysis resolution
        val scaled = Bitmap.createScaledBitmap(frame, ANALYSIS_WIDTH, ANALYSIS_HEIGHT, false)

        // Extract grayscale luminance values
        val current = extractLuminance(scaled)
        scaled.recycle()

        val reference = referenceFrames[cameraId]

        // First frame for this camera — set reference and return no motion
        if (reference == null) {
            referenceFrames[cameraId] = current
            return@withContext MotionAnalysisResult(cameraId, now, false, 0f, 0, null)
        }

        // Determine which pixels to analyze
        val regions = config.regions.ifEmpty { listOf(MotionRegion.FULL_FRAME) }

        var changedPixels = 0
        var totalPixels   = 0
        var peakDelta     = 0
        var triggeredRegion: String? = null

        for (region in regions) {
            val x0 = (region.left   * ANALYSIS_WIDTH).toInt().coerceIn(0, ANALYSIS_WIDTH - 1)
            val y0 = (region.top    * ANALYSIS_HEIGHT).toInt().coerceIn(0, ANALYSIS_HEIGHT - 1)
            val x1 = (region.right  * ANALYSIS_WIDTH).toInt().coerceIn(x0 + 1, ANALYSIS_WIDTH)
            val y1 = (region.bottom * ANALYSIS_HEIGHT).toInt().coerceIn(y0 + 1, ANALYSIS_HEIGHT)

            var regionChanged = 0
            var regionTotal   = 0

            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    val idx   = y * ANALYSIS_WIDTH + x
                    val delta = abs(current[idx] - reference[idx]).toInt()
                    if (delta > config.pixelThreshold) {
                        regionChanged++
                        if (delta > peakDelta) peakDelta = delta
                    }
                    regionTotal++
                }
            }

            changedPixels += regionChanged
            totalPixels   += regionTotal

            val regionRatio = regionChanged.toFloat() / regionTotal.toFloat()
            if (regionRatio >= config.motionRatioThreshold && triggeredRegion == null) {
                triggeredRegion = region.name
            }
        }

        val motionRatio    = changedPixels.toFloat() / totalPixels.toFloat()
        val motionDetected = motionRatio >= config.motionRatioThreshold

        // Update reference frame with slow exponential blend
        // This allows the detector to adapt to gradual lighting changes
        // without resetting on a motion event itself
        if (!motionDetected) {
            blendReference(reference, current, REFERENCE_BLEND_ALPHA)
        }
        // When motion IS detected, freeze the reference so we don't adapt
        // the moving object into the background

        return@withContext MotionAnalysisResult(
            cameraId        = cameraId,
            timestampMs     = now,
            motionDetected  = motionDetected,
            motionRatio     = motionRatio,
            peakDelta       = peakDelta,
            triggeredRegion = triggeredRegion
        )
    }

    /**
     * Reset the reference frame for a camera.
     * Call when the camera reconnects or feed restarts to avoid false positives
     * from transition frames.
     */
    fun resetReference(cameraId: String) {
        referenceFrames.remove(cameraId)
        Timber.d("MotionDetector: reset reference for $cameraId")
    }

    /**
     * Clear all reference frames. Call on app background to free memory.
     */
    fun clearAll() {
        referenceFrames.clear()
    }

    // ─────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Extract a float luminance array from a [Bitmap].
     * Uses the standard luminance formula: Y = 0.299R + 0.587G + 0.114B
     */
    private fun extractLuminance(bitmap: Bitmap): FloatArray {
        val w       = bitmap.width
        val h       = bitmap.height
        val pixels  = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        return FloatArray(pixels.size) { i ->
            val px = pixels[i]
            val r  = Color.red(px)
            val g  = Color.green(px)
            val b  = Color.blue(px)
            0.299f * r + 0.587f * g + 0.114f * b
        }
    }

    /**
     * Blend [current] into [reference] in-place using exponential moving average.
     * reference = reference * (1 - α) + current * α
     */
    private fun blendReference(reference: FloatArray, current: FloatArray, alpha: Float) {
        val inv = 1f - alpha
        for (i in reference.indices) {
            reference[i] = reference[i] * inv + current[i] * alpha
        }
    }
}
