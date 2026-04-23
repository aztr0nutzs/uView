package com.sentinel.app.data.playback

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MjpegSessionRegistry
 *
 * Thread-safe registry that acts as the bus between:
 *   - [CameraPlaybackServiceImpl] (producer) — pushes decoded [Bitmap] frames from IO threads
 *   - [MjpegStreamView] (consumer)            — collects frames on the UI thread to draw
 *
 * Each camera gets its own [MutableSharedFlow] keyed by cameraId.
 * SharedFlow with replay=1 means a new collector immediately gets the last frame,
 * so the view doesn't show a blank frame on first composition.
 */
@Singleton
class MjpegSessionRegistry @Inject constructor() {

    private val frameFlows = ConcurrentHashMap<String, MutableSharedFlow<Bitmap>>()

    /**
     * Push a decoded frame into the flow for [cameraId].
     * Safe to call from any thread.
     */
    fun postFrame(cameraId: String, bitmap: Bitmap) {
        getOrCreate(cameraId).tryEmit(bitmap)
    }

    /**
     * Observe the frame stream for [cameraId].
     * Collect this on the composition side inside [MjpegStreamView].
     */
    fun frames(cameraId: String): Flow<Bitmap> =
        getOrCreate(cameraId).asSharedFlow()

    /**
     * True when an MJPEG session flow exists for [cameraId].
     */
    fun hasActiveSession(cameraId: String): Boolean = frameFlows.containsKey(cameraId)

    /**
     * Remove and discard the flow for [cameraId] when the session stops.
     */
    fun remove(cameraId: String) {
        frameFlows.remove(cameraId)
    }

    private fun getOrCreate(cameraId: String): MutableSharedFlow<Bitmap> =
        frameFlows.getOrPut(cameraId) {
            MutableSharedFlow(replay = 1, extraBufferCapacity = 2)
        }
}
