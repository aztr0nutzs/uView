package com.sentinel.companion.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

sealed interface ValidationStep {
    data object ResolvingDns : ValidationStep
    data class DnsResolved(val ip: String) : ValidationStep
    data object OpeningSocket : ValidationStep
    data object SocketOpen : ValidationStep
    data object ProbingHttp : ValidationStep
    data class HttpResponded(val code: Int, val sentinelHeader: Boolean) : ValidationStep
    data class Success(val ip: String, val httpCode: Int, val isSentinelHost: Boolean) : ValidationStep
    data class Failure(val phase: Phase, val reason: String) : ValidationStep {
        enum class Phase { DNS, SOCKET, HTTP, INTERNAL }
    }
}

@Singleton
class HostValidator @Inject constructor() {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(6, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    fun validate(
        host: String,
        port: Int,
        useHttps: Boolean,
    ): Flow<ValidationStep> = flow {
        val trimmed = host.trim()
        if (trimmed.isBlank()) {
            emit(ValidationStep.Failure(ValidationStep.Failure.Phase.DNS, "Host address is empty"))
            return@flow
        }
        if (port !in 1..65535) {
            emit(ValidationStep.Failure(ValidationStep.Failure.Phase.SOCKET, "Port $port out of range (1-65535)"))
            return@flow
        }

        // ── 1. DNS ────────────────────────────────────────────────────────────
        emit(ValidationStep.ResolvingDns)
        val resolved = try {
            InetAddress.getByName(trimmed)
        } catch (e: UnknownHostException) {
            emit(ValidationStep.Failure(ValidationStep.Failure.Phase.DNS, "Cannot resolve host '$trimmed' (DNS lookup failed)"))
            return@flow
        } catch (e: SecurityException) {
            emit(ValidationStep.Failure(ValidationStep.Failure.Phase.DNS, "DNS lookup blocked: ${e.message ?: "permission denied"}"))
            return@flow
        }
        val ip = resolved.hostAddress ?: trimmed
        emit(ValidationStep.DnsResolved(ip))

        // ── 2. TCP socket ─────────────────────────────────────────────────────
        emit(ValidationStep.OpeningSocket)
        val socketResult = openSocket(ip, port)
        if (socketResult != null) {
            emit(ValidationStep.Failure(ValidationStep.Failure.Phase.SOCKET, socketResult))
            return@flow
        }
        emit(ValidationStep.SocketOpen)

        // ── 3. HTTP probe ─────────────────────────────────────────────────────
        emit(ValidationStep.ProbingHttp)
        val scheme = if (useHttps) "https" else "http"
        val baseUrl = "$scheme://$trimmed:$port"
        val probe = probeHttp("$baseUrl/api/health")
        val (code, sentinel) = when (probe) {
            is HttpProbeResult.Ok -> probe.code to probe.sentinelHeader
            is HttpProbeResult.Error -> {
                // Fall back to root path — server may not expose /api/health.
                val rootProbe = probeHttp(baseUrl)
                when (rootProbe) {
                    is HttpProbeResult.Ok -> rootProbe.code to rootProbe.sentinelHeader
                    is HttpProbeResult.Error -> {
                        emit(ValidationStep.Failure(ValidationStep.Failure.Phase.HTTP, rootProbe.reason))
                        return@flow
                    }
                }
            }
        }
        emit(ValidationStep.HttpResponded(code, sentinel))

        // 5xx means the server is reachable but broken — surface, do not auto-pass.
        if (code in 500..599) {
            emit(ValidationStep.Failure(ValidationStep.Failure.Phase.HTTP, "Server reachable but returned HTTP $code"))
            return@flow
        }

        emit(ValidationStep.Success(ip = ip, httpCode = code, isSentinelHost = sentinel))
    }.flowOn(Dispatchers.IO)

    private suspend fun openSocket(host: String, port: Int): String? = withContext(Dispatchers.IO) {
        try {
            Socket().use { s ->
                s.connect(InetSocketAddress(host, port), SOCKET_TIMEOUT_MS)
            }
            null
        } catch (e: SocketTimeoutException) {
            "TCP timeout — $host:$port did not respond within ${SOCKET_TIMEOUT_MS}ms"
        } catch (e: ConnectException) {
            "Connection refused on $host:$port (${e.message ?: "no service listening"})"
        } catch (e: SecurityException) {
            "Network access blocked: ${e.message ?: "permission denied"}"
        } catch (e: Exception) {
            "TCP error on $host:$port — ${e.javaClass.simpleName}: ${e.message ?: "unknown"}"
        }
    }

    private sealed interface HttpProbeResult {
        data class Ok(val code: Int, val sentinelHeader: Boolean) : HttpProbeResult
        data class Error(val reason: String) : HttpProbeResult
    }

    private suspend fun probeHttp(url: String): HttpProbeResult = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "SentinelCompanion/1.0")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                val sentinel = resp.header("X-Sentinel-Service") != null ||
                    resp.header("Server")?.contains("sentinel", ignoreCase = true) == true
                HttpProbeResult.Ok(resp.code, sentinel)
            }
        } catch (e: SSLException) {
            HttpProbeResult.Error("TLS handshake failed: ${e.message ?: "invalid certificate"}")
        } catch (e: SocketTimeoutException) {
            HttpProbeResult.Error("HTTP timeout on $url")
        } catch (e: ConnectException) {
            HttpProbeResult.Error("HTTP connect refused on $url")
        } catch (e: Exception) {
            Timber.w(e, "HTTP probe failed for %s", url)
            HttpProbeResult.Error("HTTP probe failed: ${e.javaClass.simpleName}: ${e.message ?: "unknown"}")
        }
    }

    private companion object {
        const val SOCKET_TIMEOUT_MS = 3000
    }
}
