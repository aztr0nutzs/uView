package com.sentinel.app.data.playback.mjpeg

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.Authenticator
import java.net.HttpURLConnection
import java.net.PasswordAuthentication
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MjpegFrameSource
 *
 * Connects to an MJPEG over HTTP stream (multipart/x-mixed-replace) and emits
 * decoded [Bitmap] frames as a [Flow]. Used by [MjpegPlayerState] and wired
 * into [MjpegStreamView] for rendering.
 *
 * MJPEG streams use HTTP multipart boundaries to delimit JPEG frames:
 *
 *   HTTP/1.1 200 OK
 *   Content-Type: multipart/x-mixed-replace; boundary=--boundary
 *
 *   --boundary
 *   Content-Type: image/jpeg
 *   Content-Length: 12345
 *
 *   <JPEG bytes>
 *   --boundary
 *   ...
 *
 * This class parses that boundary protocol manually because neither ExoPlayer
 * nor Android's MediaPlayer supports it natively.
 *
 * Supported camera apps:
 *   - IP Webcam (Android)    → http://<ip>:8080/video
 *   - DroidCam               → http://<ip>:4747/video
 *   - Most budget IP cameras → http://<ip>:<port>/video  or  /mjpeg
 */
@Singleton
class MjpegFrameSource @Inject constructor() {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS    = 15_000
        private const val SOI_BYTE_1: Byte   = 0xFF.toByte()   // JPEG Start Of Image marker byte 1
        private const val SOI_BYTE_2: Byte   = 0xD8.toByte()   // JPEG SOI marker byte 2
        private const val EOI_BYTE_1: Byte   = 0xFF.toByte()   // JPEG End Of Image marker byte 1
        private const val EOI_BYTE_2: Byte   = 0xD9.toByte()   // JPEG EOI marker byte 2
        private const val BUFFER_SIZE        = 16_384           // 16 KB read buffer
    }

    /**
     * Opens the MJPEG stream at [url] and emits decoded [Bitmap] frames.
     *
     * The flow is cold — it opens the connection only when collected.
     * Cancel the collecting coroutine to close the stream and release resources.
     *
     * @param url      Full HTTP URL of the MJPEG stream
     * @param username Optional HTTP Basic auth username
     * @param password Optional HTTP Basic auth password
     */
    fun frames(
        url: String,
        username: String = "",
        password: String = ""
    ): Flow<MjpegFrame> = flow {
        Timber.d("MjpegFrameSource: connecting to $url")

        val connection = openConnection(url, username, password)

        try {
            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                emit(MjpegFrame.Error("HTTP $responseCode from $url"))
                return@flow
            }

            val contentType = connection.contentType ?: ""
            val boundary    = parseBoundary(contentType)

            val inputStream = BufferedInputStream(connection.inputStream, BUFFER_SIZE)

            // Choose parser strategy based on content-type
            if (boundary != null) {
                parseBoundaryMode(inputStream, boundary)
                    .collect { frame -> emit(frame) }
            } else {
                // Fallback: scan raw bytes for JPEG SOI/EOI markers
                parseMarkerMode(inputStream)
                    .collect { frame -> emit(frame) }
            }
        } catch (e: Exception) {
            if (currentCoroutineContext().isActive) {
                Timber.e(e, "MjpegFrameSource stream error on $url")
                emit(MjpegFrame.Error(e.message ?: "Stream error"))
            }
        } finally {
            connection.disconnect()
            Timber.d("MjpegFrameSource: disconnected from $url")
        }
    }.flowOn(Dispatchers.IO)

    // ─────────────────────────────────────────────────────────────────────────
    // Boundary-based parser
    // Reads headers to find Content-Length, then reads exactly that many bytes.
    // Most reliable when the server sends Content-Length per part.
    // ─────────────────────────────────────────────────────────────────────────

    private fun parseBoundaryMode(
        stream: BufferedInputStream,
        @Suppress("UNUSED_PARAMETER") boundary: String
    ): Flow<MjpegFrame> = flow {
        val reader = stream.bufferedReader(Charsets.ISO_8859_1)
        val buf    = ByteArray(BUFFER_SIZE)

        while (currentCoroutineContext().isActive) {
            // Read past boundary and headers until blank line
            var contentLength = -1
            var line: String
            do {
                line = reader.readLine() ?: break
                if (line.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line.substringAfter(":").trim().toIntOrNull() ?: -1
                }
            } while (line.isNotBlank())

            if (!currentCoroutineContext().isActive) break

            val frameBytes: ByteArray = if (contentLength > 0) {
                // Fast path: read exactly contentLength bytes
                val bytes = ByteArray(contentLength)
                var offset = 0
                while (offset < contentLength && currentCoroutineContext().isActive) {
                    val n = stream.read(bytes, offset, contentLength - offset)
                    if (n < 0) break
                    offset += n
                }
                bytes
            } else {
                // Slow path: read until next boundary marker in the raw stream
                val bos = ByteArrayOutputStream()
                var prev = -1
                while (currentCoroutineContext().isActive) {
                    val b = stream.read()
                    if (b < 0) break
                    bos.write(b)
                    // Detect EOI (0xFF 0xD9) as frame end
                    if (prev == 0xFF && b == 0xD9) break
                    prev = b
                }
                bos.toByteArray()
            }

            val bitmap = decodeBitmap(frameBytes)
            if (bitmap != null) {
                emit(MjpegFrame.Frame(bitmap))
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Marker-based parser (fallback)
    // Scans the raw byte stream for JPEG SOI (0xFF 0xD8) and EOI (0xFF 0xD9)
    // markers and extracts complete JPEG images between them.
    // Works with cameras that don't send proper multipart headers.
    // ─────────────────────────────────────────────────────────────────────────

    private fun parseMarkerMode(stream: BufferedInputStream): Flow<MjpegFrame> = flow {
        val bos       = ByteArrayOutputStream(32_768)
        var inFrame   = false
        var prevByte  = -1

        while (currentCoroutineContext().isActive) {
            val b = stream.read()
            if (b < 0) break

            if (!inFrame) {
                // Look for SOI marker: 0xFF 0xD8
                if (prevByte == 0xFF && b == 0xD8) {
                    inFrame = true
                    bos.reset()
                    bos.write(0xFF)
                    bos.write(0xD8)
                }
            } else {
                bos.write(b)
                // Look for EOI marker: 0xFF 0xD9
                if (prevByte == 0xFF && b == 0xD9) {
                    inFrame = false
                    val bitmap = decodeBitmap(bos.toByteArray())
                    if (bitmap != null) emit(MjpegFrame.Frame(bitmap))
                    bos.reset()
                }
            }
            prevByte = b
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun openConnection(url: String, username: String, password: String): HttpURLConnection {
        if (username.isNotBlank()) {
            Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(username, password.toCharArray())
            })
        }
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout    = READ_TIMEOUT_MS
        conn.requestMethod  = "GET"
        conn.setRequestProperty("User-Agent", "SentinelHome/1.0")
        return conn
    }

    private fun parseBoundary(contentType: String): String? {
        if (!contentType.contains("multipart", ignoreCase = true)) return null
        return contentType
            .split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("boundary=", ignoreCase = true) }
            ?.substringAfter("=")
            ?.trim()
            ?.trimStart('-')
    }

    private fun decodeBitmap(bytes: ByteArray): Bitmap? {
        if (bytes.size < 4) return null
        return try {
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565   // lower memory than ARGB_8888
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (e: Exception) {
            Timber.w("Failed to decode MJPEG frame: ${e.message}")
            null
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MjpegFrame — sealed result type emitted by MjpegFrameSource
// ─────────────────────────────────────────────────────────────────────────────

sealed class MjpegFrame {
    data class Frame(val bitmap: Bitmap) : MjpegFrame()
    data class Error(val message: String) : MjpegFrame()
}
