package com.sentinel.app.data.motion

import android.graphics.Bitmap
import com.sentinel.app.data.playback.MjpegSessionRegistry
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.MotionAnalysisResult
import com.sentinel.app.domain.model.MotionDetectorState
import com.sentinel.app.domain.model.MotionSensitivityConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MotionMonitorService
 *
 * Runs motion detection on live camera frames and emits [MotionAnalysisResult]
 * events when motion is detected.
 *
 * ── MJPEG cameras ────────────────────────────────────────────────────────────
 * Subscribes to [MjpegSessionRegistry] for decoded Bitmap frames and runs
 * [MotionDetector.analyze] on each one. This works immediately since MJPEG
 * frames are already decoded to Bitmap.
 *
 * ── ExoPlayer cameras (RTSP / HLS) ───────────────────────────────────────────
 * ExoPlayer does not easily expose video frames as Bitmaps during live
 * playback without using a [SurfaceTexture] + [ImageReader] pipeline.
 * The approach used here is a periodic snapshot strategy:
 *   - Register a [FrameExtractCallback] with [registerFrameExtractor]
 *   - The UI layer (ExoPlayerSurface) calls back with captured bitmaps
 *     at a configurable interval (default 2 fps for motion analysis)
 *   - This avoids needing a second codec for frame extraction
 *
 * Cooldown:
 * Each camera has an independent cooldown. Once motion is fired, the
 * detector enters [MotionDetectorState.Cooldown] for [cooldownMs] before
 * it can fire again.
 *
 * Frame sampling:
 * Not every frame is analyzed — configurable via [frameSkip].
 * Default is every 3rd frame (~10 fps analysis at 30 fps source).
 */
@Singleton
class MotionMonitorService @Inject constructor(
    private val motionDetector: MotionDetector,
    private val mjpegRegistry: MjpegSessionRegistry
) {
    /** Callback type for ExoPlayer frame extraction. */
    fun interface FrameExtractCallback {
        fun onFrame(bitmap: Bitmap)
    }

    private val scope          = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mjpegJobs      = ConcurrentHashMap<String, Job>()
    private val frameExtractors = ConcurrentHashMap<String, FrameExtractCallback>()
    private val configs        = ConcurrentHashMap<String, MotionSensitivityConfig>()
    private val lastEventMs    = ConcurrentHashMap<String, Long>()
    private val frameCounters  = ConcurrentHashMap<String, Int>()

    // How many frames to skip between analyses (0 = every frame, 2 = every 3rd)
    private val frameSkip = 2

    // Shared flow for all motion detection results — consumers filter by cameraId
    private val _motionEvents = MutableSharedFlow<MotionAnalysisResult>(extraBufferCapacity = 64)
    val motionEvents: Flow<MotionAnalysisResult> = _motionEvents.asSharedFlow()

    // Per-camera detector state
    private val _states = ConcurrentHashMap<String, MutableStateFlow<MotionDetectorState>>()

    /**
     * Start monitoring motion for a camera.
     * For MJPEG cameras, subscribes to the frame flow automatically.
     * For ExoPlayer cameras, waits for frame callbacks via [registerFrameExtractor].
     *
     * @param camera  The camera device to monitor.
     * @param config  Sensitivity configuration. Defaults to MEDIUM.
     */
    fun startMonitoring(
        camera: CameraDevice,
        config: MotionSensitivityConfig = MotionSensitivityConfig.MEDIUM
    ) {
        val id = camera.id
        configs[id] = config
        motionDetector.resetReference(id)
        setState(id, MotionDetectorState.Running)

        // Subscribe to MJPEG frames if this is an MJPEG source
        if (isMjpegSource(camera)) {
            mjpegJobs[id]?.cancel()
            mjpegJobs[id] = scope.launch {
                mjpegRegistry.frames(id).collect { bitmap ->
                    processFrame(id, bitmap)
                }
            }
            Timber.d("MotionMonitor: started MJPEG monitoring for $id")
        }
        // ExoPlayer cameras use frame extractor callbacks registered separately
    }

    /**
     * Stop monitoring motion for a camera.
     */
    fun stopMonitoring(cameraId: String) {
        mjpegJobs.remove(cameraId)?.cancel()
        frameExtractors.remove(cameraId)
        configs.remove(cameraId)
        lastEventMs.remove(cameraId)
        frameCounters.remove(cameraId)
        motionDetector.resetReference(cameraId)
        setState(cameraId, MotionDetectorState.Idle)
        Timber.d("MotionMonitor: stopped monitoring for $cameraId")
    }

    /**
     * Register a frame extractor for an ExoPlayer camera.
     * The UI layer calls [FrameExtractCallback.onFrame] periodically with
     * captured video frames.
     *
     * Typically called from the composable that hosts the ExoPlayerSurface.
     */
    fun registerFrameExtractor(cameraId: String, callback: FrameExtractCallback) {
        frameExtractors[cameraId] = callback
        Timber.d("MotionMonitor: registered frame extractor for $cameraId")
    }

    /**
     * Submit a frame from an ExoPlayer camera for motion analysis.
     * Call this from the UI frame extraction callback.
     */
    fun submitExoFrame(cameraId: String, bitmap: Bitmap) {
        if (configs.containsKey(cameraId)) {
            scope.launch { processFrame(cameraId, bitmap) }
        }
    }

    /**
     * Update the sensitivity configuration for an active camera.
     */
    fun updateConfig(cameraId: String, config: MotionSensitivityConfig) {
        configs[cameraId] = config
        if (!config.enabled) {
            setState(cameraId, MotionDetectorState.Disabled)
        } else if (getState(cameraId).value !is MotionDetectorState.Cooldown) {
            setState(cameraId, MotionDetectorState.Running)
        }
    }

    /**
     * Observe the detector state for a camera.
     */
    fun observeState(cameraId: String): StateFlow<MotionDetectorState> =
        getState(cameraId).asStateFlow()

    /**
     * Observe motion events for a specific camera.
     */
    fun observeMotionForCamera(cameraId: String): Flow<MotionAnalysisResult> =
        kotlinx.coroutines.flow.filter(motionEvents) { it.cameraId == cameraId }

    // ─────────────────────────────────────────────────────────────────────
    // Core frame processing
    // ─────────────────────────────────────────────────────────────────────

    private suspend fun processFrame(cameraId: String, bitmap: Bitmap) {
        val config = configs[cameraId] ?: return

        // Skip frames to reduce CPU load
        val count = (frameCounters[cameraId] ?: 0) + 1
        frameCounters[cameraId] = count
        if (count % (frameSkip + 1) != 0) return

        // Skip if in cooldown
        val lastEvent = lastEventMs[cameraId] ?: 0L
        val now       = System.currentTimeMillis()
        if (now - lastEvent < config.cooldownMs) return

        val result = motionDetector.analyze(cameraId, bitmap, config)

        if (result.motionDetected) {
            Timber.d("MotionMonitor: MOTION on $cameraId — ratio=${result.motionRatio}, peak=${result.peakDelta}")
            lastEventMs[cameraId] = now
            _motionEvents.tryEmit(result)
            enterCooldown(cameraId, config.cooldownMs)
        }
    }

    private fun enterCooldown(cameraId: String, cooldownMs: Long) {
        setState(cameraId, MotionDetectorState.Cooldown)
        scope.launch {
            delay(cooldownMs)
            if (configs.containsKey(cameraId)) {
                setState(cameraId, MotionDetectorState.Running)
            }
        }
    }

    private fun getState(cameraId: String): MutableStateFlow<MotionDetectorState> =
        _states.getOrPut(cameraId) { MutableStateFlow(MotionDetectorState.Idle) }

    private fun setState(cameraId: String, state: MotionDetectorState) {
        getState(cameraId).value = state
    }

    private fun isMjpegSource(camera: CameraDevice): Boolean =
        camera.sourceType in listOf(
            com.sentinel.app.domain.model.CameraSourceType.MJPEG,
            com.sentinel.app.domain.model.CameraSourceType.ANDROID_IPWEBCAM,
            com.sentinel.app.domain.model.CameraSourceType.ANDROID_DROIDCAM
        )
}

// Helper extension — not available in Kotlin stdlib for SharedFlow
private fun <T> kotlinx.coroutines.flow.filter(
    flow: Flow<T>,
    predicate: (T) -> Boolean
): Flow<T> = kotlinx.coroutines.flow.flow {
    flow.collect { if (predicate(it)) emit(it) }
}
