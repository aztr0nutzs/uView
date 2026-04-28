package com.sentinel.app.core.pairing

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hosts a short-lived pairing window: opens a TCP listener, broadcasts host/port +
 * ephemeral pubkey via [PairingSession.qrPayload], and accepts a single companion
 * handshake using the [PairingProtocol]. Lifetime is bounded by [WINDOW_MS]; the
 * server stops as soon as one companion completes pairing or the window expires.
 *
 * The auth token returned to the companion is *not* yet checked by the main app's
 * stream/recording surfaces — there is no remote API to authenticate against today.
 * The token is generated and stored anyway so that wiring authenticated endpoints
 * later is a header-injection change rather than a protocol change. Callers should
 * not pretend authenticated control already exists.
 */
@Singleton
class PairingHost @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    sealed interface State {
        object Idle : State
        data class Listening(val session: PairingSession, val remainingMs: Long) : State
        data class Paired(val deviceLabel: String, val finishedAtMs: Long) : State
        data class Failed(val reason: String) : State
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private var activeSession: PairingSession? = null

    fun start(deviceName: String) {
        stop()
        val session = try {
            buildSession(deviceName)
        } catch (e: Exception) {
            Timber.e(e, "PairingHost.start: could not bind socket / pick host")
            _state.value = State.Failed("Could not start pairing: ${e.javaClass.simpleName}")
            return
        }
        activeSession = session
        _state.value = State.Listening(session, WINDOW_MS)
        serverJob = scope.launch { runAcceptLoop(session) }
        scope.launch { tickCountdown(session) }
    }

    fun stop() {
        serverJob?.cancel()
        serverJob = null
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        activeSession = null
        if (_state.value is State.Listening) _state.value = State.Idle
    }

    private fun buildSession(deviceName: String): PairingSession {
        val keyPair = PairingProtocol.newKeyPair()
        val token = PairingProtocol.randomBytes(16)
        val host = pickLanIpv4() ?: throw IllegalStateException("No LAN IPv4 address available")
        val sock = ServerSocket(0).apply { soTimeout = ACCEPT_TIMEOUT_MS } // 0 = OS-assigned port
        serverSocket = sock
        val port = sock.localPort
        val expiryMs = System.currentTimeMillis() + WINDOW_MS
        return PairingSession(
            keyPair = keyPair,
            token = token,
            host = host,
            port = port,
            expiryMs = expiryMs,
            deviceName = deviceName,
        )
    }

    private suspend fun runAcceptLoop(session: PairingSession) = withContext(Dispatchers.IO) {
        val ss = serverSocket ?: return@withContext
        try {
            while (isActive && System.currentTimeMillis() < session.expiryMs) {
                val client = try { ss.accept() } catch (_: SocketTimeoutException) { continue }
                val ok = try { handleClient(client, session) } catch (e: Exception) {
                    Timber.w(e, "Pairing handshake failed")
                    false
                }
                client.runCatching { close() }
                if (ok) {
                    _state.value = State.Paired(session.deviceName, System.currentTimeMillis())
                    break
                }
            }
            if (_state.value is State.Listening) _state.value = State.Failed("Pairing window expired")
        } finally {
            try { ss.close() } catch (_: Exception) {}
            serverSocket = null
        }
    }

    private fun handleClient(client: Socket, session: PairingSession): Boolean {
        client.soTimeout = 4000
        val reader = client.getInputStream().bufferedReader(Charsets.UTF_8)
        val line = reader.readLine() ?: return false
        val req = PairingProtocol.decodeRequest(line)

        if (!PairingProtocol.bytesEqual(req.token, session.token)) {
            Timber.w("Pairing rejected: token mismatch")
            return false
        }
        if (System.currentTimeMillis() > session.expiryMs) return false

        val clientPub = PairingProtocol.decodePublicKey(req.clientPubKey)
        val shared = PairingProtocol.sharedSecret(session.keyPair.private, clientPub)
        val key = PairingProtocol.hkdf(ikm = shared, salt = session.token)

        val authToken = PairingProtocol.b64(PairingProtocol.randomBytes(32))
        val plaintext = JSONObject()
            .put("a", authToken)
            .put("n", session.deviceName)
            .put("h", session.host)
            .put("p", session.port)
            .put("i", System.currentTimeMillis())
            .toString()
            .toByteArray(Charsets.UTF_8)

        val (iv, ct) = PairingProtocol.aesGcmEncrypt(key, plaintext)
        client.getOutputStream().write(PairingProtocol.encodeResponse(iv, ct))
        client.getOutputStream().flush()
        // The auth token is generated and discarded for now — see class comment.
        return true
    }

    private suspend fun tickCountdown(session: PairingSession) {
        while (true) {
            val now = System.currentTimeMillis()
            val remaining = session.expiryMs - now
            val current = _state.value
            if (current !is State.Listening || current.session !== session) return
            if (remaining <= 0) {
                if (_state.value is State.Listening) _state.value = State.Failed("Pairing window expired")
                stop()
                return
            }
            _state.value = current.copy(remainingMs = remaining)
            kotlinx.coroutines.delay(500)
        }
    }

    private fun pickLanIpv4(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .map { it.hostAddress }
                .firstOrNull { it != null && it != "0.0.0.0" }
        } catch (e: Exception) {
            Timber.w(e, "pickLanIpv4 failed")
            null
        }
    }

    fun shutdown() {
        stop()
        scope.cancel()
    }

    companion object {
        private const val WINDOW_MS = 120_000L         // 2 min — fits a "scan now" UX
        private const val ACCEPT_TIMEOUT_MS = 750      // poll for cancellation
    }
}
