package com.sentinel.companion.data.network

import com.sentinel.companion.data.model.AuthType
import com.sentinel.companion.data.model.StreamProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URISyntaxException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

/**
 * Outcome of a real, protocol-aware probe against a stream endpoint.
 * Each case carries a phase + a precise human-readable reason so the
 * setup UI can show *why* a test failed instead of a generic "FAILED".
 */
sealed interface EndpointTestResult {
    val message: String

    data class Ok(
        val protocolLabel: String,
        val responseCode: Int?,
        val contentType: String?,
        val verifiedSignal: String,        // "RTSP 200 OK", "MJPEG content-type", etc.
        override val message: String,
    ) : EndpointTestResult

    /** Server reachable but reports the credentials as bad (or required). */
    data class AuthFailed(
        val responseCode: Int,
        val realm: String?,
        override val message: String,
    ) : EndpointTestResult

    /** Server reachable but the path/route does not exist. */
    data class BadPath(
        val responseCode: Int,
        override val message: String,
    ) : EndpointTestResult

    data class DnsFailed(override val message: String) : EndpointTestResult
    data class Timeout(override val message: String) : EndpointTestResult
    data class ConnectionRefused(override val message: String) : EndpointTestResult
    data class TlsFailed(override val message: String) : EndpointTestResult
    data class ProtocolMismatch(override val message: String) : EndpointTestResult

    /** We don't have a real test for this protocol on-device. Surfaced honestly. */
    data class Unsupported(override val message: String) : EndpointTestResult

    data class Internal(override val message: String) : EndpointTestResult
}

@Singleton
class StreamEndpointTester @Inject constructor() {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(7, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    suspend fun test(
        protocol: StreamProtocol,
        host: String,
        port: Int,
        path: String,
        authType: AuthType,
        username: String,
        password: String,
    ): EndpointTestResult = withContext(Dispatchers.IO) {
        val cleanHost = host.trim()
        val normalizedPath = if (path.isBlank()) "/" else if (path.startsWith("/") || path.contains("://")) path else "/$path"

        if (protocol == StreamProtocol.CUSTOM && normalizedPath.contains("://")) {
            return@withContext testCustom(normalizedPath, cleanHost, port, authType, username, password)
        }

        if (cleanHost.isBlank()) {
            return@withContext EndpointTestResult.DnsFailed("Host address is empty")
        }
        if (port !in 1..65535) {
            return@withContext EndpointTestResult.Internal("Port $port out of range (1-65535)")
        }

        // Resolve once so DNS errors are reported as DNS, not as a TCP failure later.
        val resolvedIp = try {
            InetAddress.getByName(cleanHost).hostAddress ?: cleanHost
        } catch (e: UnknownHostException) {
            return@withContext EndpointTestResult.DnsFailed("Cannot resolve host '$cleanHost' (DNS lookup failed)")
        }

        return@withContext when (protocol) {
            StreamProtocol.RTSP,
            StreamProtocol.ONVIF -> testRtsp(cleanHost, resolvedIp, port, normalizedPath, authType, username, password)

            StreamProtocol.MJPEG,
            StreamProtocol.HLS,
            StreamProtocol.DROIDCAM,
            StreamProtocol.IP_WEBCAM -> testHttp(
                scheme        = "http",
                host          = cleanHost,
                port          = port,
                path          = normalizedPath,
                expectedKinds = expectedKindsFor(protocol),
                authType      = authType,
                username      = username,
                password      = password,
                protocolLabel = protocol.label,
            )

            StreamProtocol.ALFRED -> EndpointTestResult.Unsupported(
                "Alfred uses a proprietary cloud relay — there is no on-device probe. Save and verify in stream viewer."
            )

            StreamProtocol.CUSTOM -> testCustom(normalizedPath, cleanHost, port, authType, username, password)
        }
    }

    // ── RTSP (real OPTIONS handshake, no fake ack) ───────────────────────────

    private fun testRtsp(
        host: String,
        resolvedIp: String,
        port: Int,
        path: String,
        authType: AuthType,
        username: String,
        password: String,
    ): EndpointTestResult {
        val socket = Socket()
        try {
            socket.connect(InetSocketAddress(resolvedIp, port), 3000)
            socket.soTimeout = 3000
        } catch (e: SocketTimeoutException) {
            return EndpointTestResult.Timeout("RTSP TCP timeout — $host:$port did not answer in 3000ms")
        } catch (e: ConnectException) {
            return EndpointTestResult.ConnectionRefused("RTSP refused on $host:$port (${e.message ?: "no service listening"})")
        } catch (e: Exception) {
            return EndpointTestResult.Internal("RTSP socket error: ${e.javaClass.simpleName}: ${e.message ?: "unknown"}")
        }

        return socket.use { sock ->
            try {
                val url = "rtsp://$host:$port$path"
                val authHeader = if (authType == AuthType.BASIC && username.isNotBlank()) {
                    "Authorization: Basic ${android.util.Base64.encodeToString("$username:$password".toByteArray(), android.util.Base64.NO_WRAP)}\r\n"
                } else ""
                val request = buildString {
                    append("OPTIONS $url RTSP/1.0\r\n")
                    append("CSeq: 1\r\n")
                    append("User-Agent: SentinelCompanion/1.0\r\n")
                    append(authHeader)
                    append("\r\n")
                }

                OutputStreamWriter(sock.getOutputStream(), Charsets.US_ASCII).apply {
                    write(request)
                    flush()
                }

                val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.US_ASCII))
                val statusLine = reader.readLine()
                    ?: return@use EndpointTestResult.ProtocolMismatch("RTSP: server closed without sending a response line")

                if (!statusLine.startsWith("RTSP/")) {
                    return@use EndpointTestResult.ProtocolMismatch(
                        "Port $port answered TCP but is not speaking RTSP (got: \"${statusLine.take(60)}\")"
                    )
                }

                val parts = statusLine.split(" ", limit = 3)
                val code = parts.getOrNull(1)?.toIntOrNull()
                    ?: return@use EndpointTestResult.ProtocolMismatch("RTSP: malformed status line \"$statusLine\"")

                // Drain headers so we can pick up the WWW-Authenticate realm if any.
                var realm: String? = null
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) break
                    if (line.startsWith("WWW-Authenticate", ignoreCase = true)) {
                        realm = Regex("""realm="([^"]+)"""", RegexOption.IGNORE_CASE)
                            .find(line)?.groupValues?.getOrNull(1)
                    }
                }

                when (code) {
                    200 -> EndpointTestResult.Ok(
                        protocolLabel  = "RTSP",
                        responseCode   = 200,
                        contentType    = null,
                        verifiedSignal = "RTSP/1.0 200 OK on OPTIONS",
                        message        = "RTSP endpoint reachable — server accepted OPTIONS at $url",
                    )
                    401 -> EndpointTestResult.AuthFailed(
                        responseCode = 401,
                        realm        = realm,
                        message      = "RTSP 401 Unauthorized" +
                            (realm?.let { " (realm: $it)" } ?: "") +
                            if (authType == AuthType.NONE) " — credentials required" else " — credentials rejected",
                    )
                    403 -> EndpointTestResult.AuthFailed(
                        responseCode = 403,
                        realm        = null,
                        message      = "RTSP 403 Forbidden — server reachable but rejecting this user",
                    )
                    404 -> EndpointTestResult.BadPath(404, "RTSP 404 Not Found at $path")
                    in 400..499 -> EndpointTestResult.BadPath(code, "RTSP $code at $path — request rejected")
                    in 500..599 -> EndpointTestResult.ProtocolMismatch("RTSP server error $code at $url")
                    else -> EndpointTestResult.ProtocolMismatch("Unexpected RTSP status $code")
                }
            } catch (e: SocketTimeoutException) {
                EndpointTestResult.Timeout("RTSP read timeout — server connected but never replied to OPTIONS")
            } catch (e: Exception) {
                Timber.w(e, "RTSP probe failed")
                EndpointTestResult.Internal("RTSP probe error: ${e.javaClass.simpleName}: ${e.message ?: "unknown"}")
            }
        }
    }

    // ── HTTP-style streams (MJPEG / HLS / DroidCam / IP Webcam) ──────────────

    private enum class ContentKind { MJPEG, HLS, IMAGE, VIDEO, ANY_2XX }

    private fun expectedKindsFor(protocol: StreamProtocol): Set<ContentKind> = when (protocol) {
        StreamProtocol.MJPEG     -> setOf(ContentKind.MJPEG, ContentKind.IMAGE)
        StreamProtocol.HLS       -> setOf(ContentKind.HLS)
        StreamProtocol.DROIDCAM,
        StreamProtocol.IP_WEBCAM -> setOf(ContentKind.MJPEG, ContentKind.IMAGE, ContentKind.VIDEO, ContentKind.ANY_2XX)
        else                     -> setOf(ContentKind.ANY_2XX)
    }

    private fun classifyContentType(ct: String?): ContentKind? {
        if (ct == null) return null
        val lower = ct.lowercase()
        return when {
            "multipart/x-mixed-replace" in lower             -> ContentKind.MJPEG
            "image/" in lower                                -> ContentKind.IMAGE
            "application/vnd.apple.mpegurl" in lower
                || "application/x-mpegurl" in lower          -> ContentKind.HLS
            "video/" in lower                                -> ContentKind.VIDEO
            else                                             -> null
        }
    }

    private fun testHttp(
        scheme: String,
        host: String,
        port: Int,
        path: String,
        expectedKinds: Set<ContentKind>,
        authType: AuthType,
        username: String,
        password: String,
        protocolLabel: String,
    ): EndpointTestResult {
        val url = "$scheme://$host:$port$path"
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", "SentinelCompanion/1.0")
            .get()

        when (authType) {
            AuthType.BASIC -> if (username.isNotBlank()) {
                builder.header("Authorization", Credentials.basic(username, password))
            }
            AuthType.TOKEN -> if (password.isNotBlank()) {
                builder.header("Authorization", "Bearer $password")
            }
            AuthType.DIGEST -> {
                // OkHttp doesn't ship a Digest authenticator; the probe still goes out
                // unauthenticated and we will report 401 with realm info so the user
                // can see exactly why it failed instead of a fake "OK".
            }
            AuthType.NONE -> {}
        }

        return try {
            httpClient.newCall(builder.build()).execute().use { resp ->
                val code = resp.code
                val ct   = resp.header("Content-Type")
                val kind = classifyContentType(ct)

                when {
                    code == 401 -> {
                        val realm = resp.header("WWW-Authenticate")?.let {
                            Regex("""realm="([^"]+)"""", RegexOption.IGNORE_CASE)
                                .find(it)?.groupValues?.getOrNull(1)
                        }
                        EndpointTestResult.AuthFailed(
                            responseCode = 401,
                            realm        = realm,
                            message      = "HTTP 401 Unauthorized" +
                                (realm?.let { " (realm: $it)" } ?: "") +
                                when {
                                    authType == AuthType.NONE   -> " — credentials required"
                                    authType == AuthType.DIGEST -> " — Digest auth is not supported on-device; cannot verify"
                                    else                        -> " — credentials rejected"
                                },
                        )
                    }
                    code == 403 -> EndpointTestResult.AuthFailed(403, null, "HTTP 403 Forbidden at $path")
                    code == 404 -> EndpointTestResult.BadPath(404, "HTTP 404 Not Found at $path")
                    code == 405 -> EndpointTestResult.BadPath(405, "HTTP 405 Method Not Allowed at $path")
                    code in 500..599 -> EndpointTestResult.ProtocolMismatch("Server error HTTP $code at $url")
                    code in 200..399 -> {
                        val typed = kind != null && kind in expectedKinds
                        if (typed) {
                            EndpointTestResult.Ok(
                                protocolLabel  = protocolLabel,
                                responseCode   = code,
                                contentType    = ct,
                                verifiedSignal = "HTTP $code · $ct",
                                message        = "$protocolLabel endpoint reachable — Content-Type matches ($ct)",
                            )
                        } else if (ContentKind.ANY_2XX in expectedKinds) {
                            EndpointTestResult.Ok(
                                protocolLabel  = protocolLabel,
                                responseCode   = code,
                                contentType    = ct,
                                verifiedSignal = "HTTP $code",
                                message        = "Endpoint responded HTTP $code — content type \"${ct ?: "unknown"}\" not strictly verified",
                            )
                        } else {
                            EndpointTestResult.ProtocolMismatch(
                                "HTTP $code at $url but Content-Type \"${ct ?: "unknown"}\" is not a $protocolLabel stream"
                            )
                        }
                    }
                    else -> EndpointTestResult.ProtocolMismatch("Unexpected HTTP $code at $url")
                }
            }
        } catch (e: SSLException) {
            EndpointTestResult.TlsFailed("TLS handshake failed at $url: ${e.message ?: "invalid certificate"}")
        } catch (e: SocketTimeoutException) {
            EndpointTestResult.Timeout("HTTP timeout — $url did not respond")
        } catch (e: ConnectException) {
            EndpointTestResult.ConnectionRefused("HTTP connect refused on $host:$port")
        } catch (e: UnknownHostException) {
            EndpointTestResult.DnsFailed("Cannot resolve host '$host' (DNS lookup failed)")
        } catch (e: Exception) {
            Timber.w(e, "HTTP probe failed for %s", url)
            EndpointTestResult.Internal("HTTP probe error: ${e.javaClass.simpleName}: ${e.message ?: "unknown"}")
        }
    }

    private fun testCustom(
        rawPath: String,
        host: String,
        port: Int,
        authType: AuthType,
        username: String,
        password: String,
    ): EndpointTestResult {
        // CUSTOM treats `path` as a full URL when it contains a scheme.
        if ("://" !in rawPath) {
            return EndpointTestResult.Unsupported(
                "CUSTOM protocol expects a full URL in the path field (e.g. rtsp://… or https://…)"
            )
        }
        val uri = try {
            URI(rawPath)
        } catch (e: URISyntaxException) {
            return EndpointTestResult.Internal("CUSTOM URL is malformed: ${e.message ?: rawPath}")
        }
        val targetHost = uri.host ?: host
        val targetPort = if (uri.port > 0) uri.port else defaultPortForScheme(uri.scheme) ?: port
        if (targetPort !in 1..65535) {
            return EndpointTestResult.Internal("CUSTOM URL has no valid port for scheme \"${uri.scheme}\"")
        }
        val targetPath = (uri.rawPath ?: "/").ifBlank { "/" } + (uri.rawQuery?.let { "?$it" } ?: "")

        return when (uri.scheme?.lowercase()) {
            "rtsp" -> {
                val resolved = try {
                    InetAddress.getByName(targetHost).hostAddress ?: targetHost
                } catch (_: UnknownHostException) {
                    return EndpointTestResult.DnsFailed("Cannot resolve host '$targetHost' (DNS lookup failed)")
                }
                testRtsp(targetHost, resolved, targetPort, targetPath, authType, username, password)
            }
            "http", "https" -> testHttp(
                scheme        = uri.scheme,
                host          = targetHost,
                port          = targetPort,
                path          = targetPath,
                expectedKinds = setOf(ContentKind.ANY_2XX, ContentKind.MJPEG, ContentKind.IMAGE, ContentKind.VIDEO, ContentKind.HLS),
                authType      = authType,
                username      = username,
                password      = password,
                protocolLabel = "CUSTOM (${uri.scheme})",
            )
            else -> EndpointTestResult.Unsupported(
                "Scheme \"${uri.scheme}\" is not supported by on-device validation"
            )
        }
    }

    private fun defaultPortForScheme(scheme: String?): Int? = when (scheme?.lowercase()) {
        "rtsp" -> 554
        "http" -> 80
        "https" -> 443
        else -> null
    }
}
