package com.sentinel.app.data.remote.adapters

import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.ConnectionTestResult
import com.sentinel.app.domain.service.CameraConnectionTester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// CameraConnectionTesterImpl
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class CameraConnectionTesterImpl @Inject constructor() : CameraConnectionTester {

    override suspend fun testConnection(
        camera: CameraDevice,
        timeoutSeconds: Int
    ): ConnectionTestResult = withContext(Dispatchers.IO) {
        val host = camera.connectionProfile.host
        val port = camera.connectionProfile.port
        val resolvedUrl = camera.connectionProfile.toDisplayUrl()

        val start = System.currentTimeMillis()
        val reachable = pingHost(host, port, timeoutSeconds * 1000)
        val latencyMs = System.currentTimeMillis() - start

        if (!reachable) {
            return@withContext ConnectionTestResult(
                cameraId = camera.id,
                host = host,
                resolvedUrl = resolvedUrl,
                success = false,
                latencyMs = null,
                errorMessage = "Host $host:$port unreachable within ${timeoutSeconds}s",
                streamReachable = false
            )
        }

        // NOTE: Stream decode (actually opening RTSP session) is not
        // implemented here — requires Media3 / ExoPlayer integration.
        // The following marks stream as "not tested" rather than lying about it.
        ConnectionTestResult(
            cameraId = camera.id,
            host = host,
            resolvedUrl = resolvedUrl,
            success = true,
            latencyMs = latencyMs,
            errorMessage = null,
            streamReachable = false,    // NOTE: set to false — stream test not implemented
            credentialsAccepted = null  // NOTE: not tested at TCP level
        )
    }

    override suspend fun pingHost(host: String, port: Int, timeoutMs: Int): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    true
                }
            } catch (e: Exception) {
                Timber.d("Ping failed for $host:$port — ${e.message}")
                false
            }
        }

    override fun validateStreamUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            val uri = URI(url)
            uri.scheme in listOf("rtsp", "rtsps", "http", "https") && uri.host != null
        } catch (e: Exception) {
            false
        }
    }
}
