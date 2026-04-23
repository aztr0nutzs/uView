package com.sentinel.app.data.recording

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import com.sentinel.app.data.playback.MjpegSessionRegistry
import com.sentinel.app.data.playback.StreamUrlResolver
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.RecordingState
import com.sentinel.app.domain.service.RecordingCapability
import com.sentinel.app.domain.service.RecordingController
import com.sentinel.app.domain.service.RecordingEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LocalRecordingController
 *
 * Truthful Phase 7 recording implementation.
 *
 * What is real:
 * - MJPEG-backed playback sessions are recorded from the same decoded Bitmap
 *   frame bus used by the live UI.
 * - Captured frames are persisted as a multipart Motion JPEG file (`.mjpeg`)
 *   with a sidecar metadata file (`.properties`) containing duration and frame
 *   count.
 * - Recording state only enters RECORDING after a writable output stream and
 *   capture coroutine are created.
 *
 * Current limitation:
 * - RTSP/HLS/ExoPlayer streams are rejected. The current playback architecture
 *   renders those streams through ExoPlayer but does not expose encoded samples,
 *   decoded frames, or a secondary recording Surface/MediaCodec pipeline. Writing
 *   MP4 from those streams would require adding that pipeline first; starting a
 *   fake MP4 session here would be misleading.
 */
@Singleton
class LocalRecordingController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val streamUrlResolver: StreamUrlResolver,
    private val mjpegSessionRegistry: MjpegSessionRegistry
) : RecordingController {

    private data class RecordingSession(
        val cameraId: String,
        val outputFile: File,
        val metadataFile: File,
        val startedAt: Long,
        val outputStream: FileOutputStream,
        val lock: Any = Any(),
        var job: Job? = null,
        var frameCount: Int = 0,
        var bytesWritten: Long = 0L,
        var lastFrameAt: Long = 0L
    )

    private val activeSessions = ConcurrentHashMap<String, RecordingSession>()
    private val recordingStates = ConcurrentHashMap<String, kotlinx.coroutines.flow.MutableStateFlow<RecordingState>>()
    private val recordingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun startRecording(camera: CameraDevice): Result<Unit> =
        withContext(Dispatchers.IO) {
            val cameraId = camera.id
            if (activeSessions.containsKey(cameraId)) {
                return@withContext Result.failure(IllegalStateException("Already recording for $cameraId"))
            }

            val capability = getRecordingCapability(camera)
            if (!capability.supported) {
                return@withContext Result.failure(
                    UnsupportedOperationException(capability.reason ?: "Recording is not supported for this stream")
                )
            }

            if (!hasActiveMjpegFrames(cameraId)) {
                return@withContext Result.failure(
                    IllegalStateException("Recording needs an active MJPEG live stream. Open the live feed first.")
                )
            }

            try {
                val outputFile = createOutputFile(cameraId)
                val metadataFile = metadataFileFor(outputFile)
                outputFile.parentFile?.mkdirs()

                val session = RecordingSession(
                    cameraId = cameraId,
                    outputFile = outputFile,
                    metadataFile = metadataFile,
                    startedAt = System.currentTimeMillis(),
                    outputStream = FileOutputStream(outputFile)
                )

                activeSessions[cameraId] = session
                setState(cameraId, RecordingState.RECORDING)

                session.job = recordingScope.launch {
                    mjpegSessionRegistry.frames(cameraId)
                        .catch { error ->
                            Timber.e(error, "MJPEG recording frame stream failed for $cameraId")
                            failSession(cameraId, session, error)
                        }
                        .collect { bitmap ->
                            writeMjpegPart(session, bitmap)
                        }
                }

                Timber.i("Recording started for $cameraId -> ${outputFile.absolutePath}")
                Result.success(Unit)
            } catch (e: Exception) {
                activeSessions.remove(cameraId)
                setState(cameraId, RecordingState.ERROR)
                Timber.e(e, "Failed to start recording for $cameraId")
                Result.failure(e)
            }
        }

    override suspend fun stopRecording(cameraId: String): Result<RecordingEntry> =
        withContext(Dispatchers.IO) {
            val session = activeSessions.remove(cameraId)
                ?: return@withContext Result.failure(IllegalStateException("No active recording for $cameraId"))

            setState(cameraId, RecordingState.SAVING)

            try {
                session.job?.cancelAndJoin()
                closeSession(session)

                if (session.frameCount == 0) {
                    session.outputFile.delete()
                    session.metadataFile.delete()
                    setState(cameraId, RecordingState.IDLE)
                    return@withContext Result.failure(
                        IOException("Recording stopped without captured frames; no output file was kept.")
                    )
                }

                val endedAt = System.currentTimeMillis()
                val durationSeconds = ((endedAt - session.startedAt) / 1000L).coerceAtLeast(1L)
                writeMetadata(session, endedAt, durationSeconds)

                val entry = RecordingEntry(
                    filePath = session.outputFile.absolutePath,
                    cameraId = cameraId,
                    startedAt = session.startedAt,
                    durationSeconds = durationSeconds,
                    sizeBytes = session.outputFile.length()
                )

                Timber.i(
                    "Recording stopped for $cameraId: frames=${session.frameCount}, " +
                        "duration=${durationSeconds}s, file=${session.outputFile.absolutePath}"
                )
                setState(cameraId, RecordingState.IDLE)
                Result.success(entry)
            } catch (e: Exception) {
                setState(cameraId, RecordingState.ERROR)
                Timber.e(e, "Failed to stop recording for $cameraId")
                Result.failure(e)
            }
        }

    override suspend fun getRecordingCapability(camera: CameraDevice): RecordingCapability {
        val endpoint = streamUrlResolver.resolve(camera)
            ?: return RecordingCapability(
                supported = false,
                requiresActivePlayback = true,
                outputFormat = null,
                reason = "No playable stream endpoint resolved"
            )

        if (!streamUrlResolver.requiresMjpegRenderer(endpoint)) {
            return RecordingCapability(
                supported = false,
                requiresActivePlayback = true,
                outputFormat = null,
                reason = "${camera.sourceType.displayName} uses ExoPlayer playback. Encoded sample capture is not implemented yet."
            )
        }

        return RecordingCapability(
            supported = true,
            requiresActivePlayback = true,
            outputFormat = "multipart-motion-jpeg (.mjpeg + .properties)",
            reason = null
        )
    }

    override fun observeRecordingState(cameraId: String): kotlinx.coroutines.flow.Flow<RecordingState> =
        getStateFlow(cameraId).asStateFlow()

    override suspend fun getRecordings(cameraId: String): List<RecordingEntry> =
        withContext(Dispatchers.IO) {
            val dir = recordingsDir(cameraId)
            if (!dir.exists()) return@withContext emptyList()

            dir.listFiles { file -> file.extension.equals("mjpeg", ignoreCase = true) }
                ?.sortedByDescending { it.lastModified() }
                ?.map { file ->
                    val metadata = readMetadata(metadataFileFor(file))
                    RecordingEntry(
                        filePath = file.absolutePath,
                        cameraId = cameraId,
                        startedAt = metadata.startedAt ?: file.lastModified(),
                        durationSeconds = metadata.durationSeconds ?: 0L,
                        sizeBytes = file.length()
                    )
                }
                ?: emptyList()
        }

    private fun hasActiveMjpegFrames(cameraId: String): Boolean =
        mjpegSessionRegistry.hasActiveSession(cameraId)

    private fun writeMjpegPart(session: RecordingSession, bitmap: Bitmap) {
        val jpegBytes = ByteArrayOutputStream().use { encoded ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, encoded)
            encoded.toByteArray()
        }

        val now = System.currentTimeMillis()
        val header = buildString {
            append("--$BOUNDARY\r\n")
            append("Content-Type: image/jpeg\r\n")
            append("Content-Length: ${jpegBytes.size}\r\n")
            append("X-Timestamp-Ms: $now\r\n")
            append("\r\n")
        }.toByteArray(Charsets.US_ASCII)

        synchronized(session.lock) {
            session.outputStream.write(header)
            session.outputStream.write(jpegBytes)
            session.outputStream.write(CRLF)
            session.frameCount += 1
            session.bytesWritten += header.size + jpegBytes.size + CRLF.size
            session.lastFrameAt = now
        }
    }

    private fun failSession(cameraId: String, session: RecordingSession, error: Throwable) {
        if (activeSessions.remove(cameraId, session)) {
            closeSession(session)
            setState(cameraId, RecordingState.ERROR)
            Timber.e(error, "Recording session failed for $cameraId")
        }
    }

    private fun closeSession(session: RecordingSession) {
        synchronized(session.lock) {
            runCatching {
                session.outputStream.write("--$BOUNDARY--\r\n".toByteArray(Charsets.US_ASCII))
                session.outputStream.flush()
            }
            runCatching { session.outputStream.fd.sync() }
            runCatching { session.outputStream.close() }
        }
    }

    private fun writeMetadata(session: RecordingSession, endedAt: Long, durationSeconds: Long) {
        Properties().apply {
            setProperty("format", "multipart-motion-jpeg")
            setProperty("cameraId", session.cameraId)
            setProperty("startedAt", session.startedAt.toString())
            setProperty("endedAt", endedAt.toString())
            setProperty("durationSeconds", durationSeconds.toString())
            setProperty("frameCount", session.frameCount.toString())
            setProperty("bytesWritten", session.bytesWritten.toString())
            setProperty("boundary", BOUNDARY)
        }.store(FileOutputStream(session.metadataFile), "Sentinel recording metadata")
    }

    private fun readMetadata(file: File): RecordingMetadata {
        if (!file.exists()) return RecordingMetadata()
        return runCatching {
            val props = Properties()
            file.inputStream().use { props.load(it) }
            RecordingMetadata(
                startedAt = props.getProperty("startedAt")?.toLongOrNull(),
                durationSeconds = props.getProperty("durationSeconds")?.toLongOrNull()
            )
        }.getOrDefault(RecordingMetadata())
    }

    private fun createOutputFile(cameraId: String): File {
        val dir = recordingsDir(cameraId)
        dir.mkdirs()
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "REC_${cameraId.take(8).uppercase(Locale.US)}_$ts.mjpeg")
    }

    private fun metadataFileFor(recordingFile: File): File =
        File(recordingFile.parentFile ?: context.filesDir, "${recordingFile.nameWithoutExtension}.properties")

    private fun recordingsDir(cameraId: String): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        return File(base, "SentinelRecordings/${cameraId.take(12)}")
    }

    private fun getStateFlow(cameraId: String): kotlinx.coroutines.flow.MutableStateFlow<RecordingState> =
        recordingStates.getOrPut(cameraId) { kotlinx.coroutines.flow.MutableStateFlow(RecordingState.IDLE) }

    private fun setState(cameraId: String, state: RecordingState) {
        getStateFlow(cameraId).value = state
    }

    private data class RecordingMetadata(
        val startedAt: Long? = null,
        val durationSeconds: Long? = null
    )

    private companion object {
        const val BOUNDARY = "sentinel-mjpeg-frame"
        val CRLF = "\r\n".toByteArray(Charsets.US_ASCII)
    }
}
