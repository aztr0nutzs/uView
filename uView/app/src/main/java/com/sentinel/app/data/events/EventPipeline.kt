package com.sentinel.app.data.events

import com.sentinel.app.data.motion.MotionMonitorService
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraEvent
import com.sentinel.app.domain.model.CameraEventType
import com.sentinel.app.domain.model.PlayerState
import com.sentinel.app.domain.repository.CameraEventRepository
import com.sentinel.app.domain.repository.CameraRepository
import com.sentinel.app.domain.service.CameraPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EventPipeline
 *
 * The single source-of-truth for writing camera events to the database.
 * Listens to three event sources:
 *
 *   1. [MotionMonitorService.motionEvents]
 *      → writes [CameraEventType.MOTION_DETECTED] events
 *
 *   2. [CameraPlaybackService] state changes per camera
 *      → writes [CameraEventType.CONNECTION_LOST] and
 *        [CameraEventType.CONNECTION_RESTORED] events
 *
 *   3. Manual calls from ViewModels
 *      → SNAPSHOT_TAKEN, RECORDING_STARTED, RECORDING_STOPPED
 *
 * All writes go through [CameraEventRepository.addEvent] which persists
 * to Room. The Events screen observes the repository directly — no polling.
 *
 * Lifecycle:
 * [start] must be called once on app launch (from MainActivity or
 * SentinelApplication) to wire up the subscriptions. [stop] releases them.
 *
 * The pipeline runs on a [SupervisorJob] scope so individual collection
 * failures don't kill sibling subscriptions.
 */
@Singleton
class EventPipeline @Inject constructor(
    private val motionMonitorService: MotionMonitorService,
    private val playbackService: CameraPlaybackService,
    private val cameraRepository: CameraRepository,
    private val eventRepository: CameraEventRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeMonitoredCameras = mutableSetOf<String>()
    private var started = false

    /**
     * Wire up all event subscriptions. Call once on app start.
     */
    fun start() {
        if (started) return
        started = true
        Timber.i("EventPipeline: started")
        listenToMotionEvents()
        listenToCameraList()
        scheduleEventPruning()
    }

    // ─────────────────────────────────────────────────────────────────────
    // 1. Motion events
    // ─────────────────────────────────────────────────────────────────────

    private fun listenToMotionEvents() {
        scope.launch {
            motionMonitorService.motionEvents.collect { result ->
                val camera = cameraRepository.getCameraById(result.cameraId) ?: return@collect

                val description = buildString {
                    append("Motion detected")
                    result.triggeredRegion?.let { append(" in $it") }
                    append(" — ${(result.motionRatio * 100).toInt()}% of frame")
                }

                writeEvent(
                    camera      = camera,
                    type        = CameraEventType.MOTION_DETECTED,
                    description = description
                )
                Timber.d("EventPipeline: MOTION_DETECTED on ${camera.name}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2. Connection state changes
    // ─────────────────────────────────────────────────────────────────────

    private fun listenToCameraList() {
        scope.launch {
            cameraRepository.observeAllCameras().collect { cameras ->
                cameras.forEach { camera ->
                    if (camera.id !in activeMonitoredCameras) {
                        activeMonitoredCameras.add(camera.id)
                        monitorCameraConnection(camera)
                    }
                }
            }
        }
    }

    private fun monitorCameraConnection(camera: CameraDevice) {
        scope.launch {
            var previousState: PlayerState? = null

            playbackService.observePlayerState(camera.id)
                .distinctUntilChanged()
                .collect { state ->
                    val prev = previousState
                    previousState = state

                    when {
                        // Was previously online, now in error/offline
                        prev is PlayerState.Playing &&
                        (state is PlayerState.Error || state is PlayerState.Reconnecting) -> {
                            writeEvent(
                                camera      = camera,
                                type        = CameraEventType.CONNECTION_LOST,
                                description = when (state) {
                                    is PlayerState.Error       -> "Stream error: ${state.message}"
                                    is PlayerState.Reconnecting -> "Stream lost — attempting reconnect"
                                    else                        -> "Connection lost"
                                }
                            )
                        }

                        // Was previously in error/reconnecting, now playing
                        (prev is PlayerState.Error || prev is PlayerState.Reconnecting) &&
                        state is PlayerState.Playing -> {
                            writeEvent(
                                camera      = camera,
                                type        = CameraEventType.CONNECTION_RESTORED,
                                description = "Stream reconnected successfully"
                            )
                        }

                        else -> { /* Idle, Loading, Buffering transitions — no event */ }
                    }
                }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 3. Manual event writers (called from ViewModels)
    // ─────────────────────────────────────────────────────────────────────

    fun recordSnapshotTaken(camera: CameraDevice, savedPath: String?) {
        scope.launch {
            writeEvent(
                camera      = camera,
                type        = CameraEventType.SNAPSHOT_TAKEN,
                description = savedPath?.let { "Saved to ${it.substringAfterLast("/")}" }
                    ?: "Snapshot captured"
            )
        }
    }

    fun recordRecordingStarted(camera: CameraDevice) {
        scope.launch {
            writeEvent(camera, CameraEventType.RECORDING_STARTED, "Recording started")
        }
    }

    fun recordRecordingStopped(camera: CameraDevice, durationSeconds: Long) {
        scope.launch {
            writeEvent(
                camera      = camera,
                type        = CameraEventType.RECORDING_STOPPED,
                description = "Recording stopped — ${formatDuration(durationSeconds)}"
            )
        }
    }

    fun recordCameraAdded(camera: CameraDevice) {
        scope.launch {
            writeEvent(camera, CameraEventType.CAMERA_ADDED, "Camera added: ${camera.sourceType.displayName}")
        }
    }

    fun recordCameraRemoved(cameraName: String, cameraId: String) {
        scope.launch {
            eventRepository.addEvent(
                CameraEvent(
                    id          = UUID.randomUUID().toString(),
                    cameraId    = cameraId,
                    cameraName  = cameraName,
                    eventType   = CameraEventType.CAMERA_REMOVED,
                    timestampMs = System.currentTimeMillis(),
                    description = "Camera removed from app"
                )
            )
        }
    }

    fun recordStreamError(camera: CameraDevice, errorMessage: String) {
        scope.launch {
            writeEvent(camera, CameraEventType.STREAM_ERROR, errorMessage)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Maintenance
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Prune events older than the configured retention period.
     * Runs once on start and every 24 hours.
     */
    private fun scheduleEventPruning() {
        scope.launch {
            while (true) {
                val retentionMs = 30L * 24 * 60 * 60 * 1000  // 30 days default
                val threshold   = System.currentTimeMillis() - retentionMs
                eventRepository.pruneOldEvents(threshold)
                Timber.d("EventPipeline: pruned events older than 30 days")
                kotlinx.coroutines.delay(24L * 60 * 60 * 1000) // wait 24h
            }
        }
    }

    fun stop() {
        started = false
        activeMonitoredCameras.clear()
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private suspend fun writeEvent(
        camera: CameraDevice,
        type: CameraEventType,
        description: String = ""
    ) {
        eventRepository.addEvent(
            CameraEvent(
                id          = UUID.randomUUID().toString(),
                cameraId    = camera.id,
                cameraName  = camera.name,
                eventType   = type,
                timestampMs = System.currentTimeMillis(),
                description = description
            )
        )
    }

    private fun formatDuration(seconds: Long): String = when {
        seconds < 60   -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else           -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}
