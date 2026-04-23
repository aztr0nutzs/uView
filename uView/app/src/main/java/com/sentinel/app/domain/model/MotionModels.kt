package com.sentinel.app.domain.model

// ─────────────────────────────────────────────────────────────────────────────
// Motion Detection Domain Models — Phase 5
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A rectangular region of interest within a camera frame.
 * Coordinates are normalized 0.0–1.0 (relative to frame dimensions).
 * Used to restrict motion detection to specific areas of the frame.
 *
 * Default (null) means the entire frame is monitored.
 */
data class MotionRegion(
    val id: String,
    val name: String,
    val left: Float,   // 0.0 = left edge
    val top: Float,    // 0.0 = top edge
    val right: Float,  // 1.0 = right edge
    val bottom: Float  // 1.0 = bottom edge
) {
    val width: Float  get() = right - left
    val height: Float get() = bottom - top
    val area: Float   get() = width * height

    companion object {
        /** Full-frame region — monitors everything. */
        val FULL_FRAME = MotionRegion("full", "Full Frame", 0f, 0f, 1f, 1f)
    }
}

/**
 * Sensitivity configuration for the frame-diff motion detector.
 *
 * @param pixelThreshold      Minimum per-pixel luminance delta (0–255) to count
 *                            as "changed". Low values = more sensitive.
 * @param motionRatioThreshold Fraction of frame pixels that must change to trigger
 *                            a motion event (0.0–1.0). Low = more sensitive.
 * @param cooldownMs          Minimum milliseconds between consecutive motion events
 *                            for the same camera. Prevents event spam.
 * @param regions             Restrict detection to these regions. Empty = full frame.
 * @param enabled             Master switch. When false the detector runs but never fires.
 */
data class MotionSensitivityConfig(
    val pixelThreshold: Int = 25,
    val motionRatioThreshold: Float = 0.02f,   // 2% of pixels must change
    val cooldownMs: Long = 10_000L,             // 10 second cooldown
    val regions: List<MotionRegion> = emptyList(),
    val enabled: Boolean = true
) {
    companion object {
        val LOW    = MotionSensitivityConfig(pixelThreshold = 40, motionRatioThreshold = 0.05f)
        val MEDIUM = MotionSensitivityConfig(pixelThreshold = 25, motionRatioThreshold = 0.02f)
        val HIGH   = MotionSensitivityConfig(pixelThreshold = 12, motionRatioThreshold = 0.008f)
        val DISABLED = MotionSensitivityConfig(enabled = false)
    }
}

/**
 * Result of a single frame-diff analysis.
 */
data class MotionAnalysisResult(
    val cameraId: String,
    val timestampMs: Long,
    val motionDetected: Boolean,
    val motionRatio: Float,         // fraction of pixels that changed
    val peakDelta: Int,             // max per-pixel luminance delta in frame
    val triggeredRegion: String?    // which region triggered (or null = full frame)
)

/**
 * State of the motion detector for a single camera.
 */
sealed class MotionDetectorState {
    object Idle        : MotionDetectorState()
    object Running     : MotionDetectorState()
    object Cooldown    : MotionDetectorState()
    object Disabled    : MotionDetectorState()
    data class Error(val message: String) : MotionDetectorState()
}
