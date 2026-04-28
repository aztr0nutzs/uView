package com.sentinel.companion.data.pairing

import com.sentinel.companion.data.model.ConnectionPrefs
import com.sentinel.companion.data.repository.PreferencesRepository
import com.sentinel.companion.security.CredentialCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performs the client side of the pairing handshake described in [PairingProtocol].
 * Stores the resulting host/port + (encrypted) auth token via [PreferencesRepository]
 * + [CredentialCipher].
 */
@Singleton
class PairingClient @Inject constructor(
    private val prefsRepo: PreferencesRepository,
    private val credentialCipher: CredentialCipher,
) {

    sealed interface Result {
        data class Success(val host: String, val port: Int, val deviceName: String) : Result
        data class Failure(val reason: String) : Result
    }

    suspend fun pair(qrText: String): Result = withContext(Dispatchers.IO) {
        val payload = try {
            PairingProtocol.decodeQrPayload(qrText)
        } catch (e: Exception) {
            return@withContext Result.Failure("QR not recognized as a Sentinel pairing code")
        }
        if (System.currentTimeMillis() > payload.expiryMs) {
            return@withContext Result.Failure("This pairing code has expired — generate a new one")
        }

        val keyPair = PairingProtocol.newKeyPair()
        val request = PairingProtocol.encodeRequest(
            PairingProtocol.HandshakeRequest(
                token = payload.token,
                clientPubKey = PairingProtocol.encodePublicKey(keyPair.public),
            )
        )

        val responseLine = try {
            Socket().use { s ->
                s.connect(InetSocketAddress(payload.host, payload.port), 4000)
                s.soTimeout = 5000
                s.getOutputStream().write(request)
                s.getOutputStream().flush()
                s.getInputStream().bufferedReader(Charsets.UTF_8).readLine()
                    ?: return@withContext Result.Failure("Hub closed connection without responding")
            }
        } catch (e: Exception) {
            Timber.w(e, "Pairing connection failed")
            return@withContext Result.Failure("Could not reach hub at ${payload.host}:${payload.port} (${e.javaClass.simpleName})")
        }

        val (iv, ct) = try {
            PairingProtocol.decodeResponse(responseLine)
        } catch (e: Exception) {
            return@withContext Result.Failure("Hub response was malformed")
        }

        val plaintext = try {
            val serverPub = PairingProtocol.decodePublicKey(payload.serverPubKey)
            val shared = PairingProtocol.sharedSecret(keyPair.private, serverPub)
            val key = PairingProtocol.hkdf(ikm = shared, salt = payload.token)
            PairingProtocol.aesGcmDecrypt(key, iv, ct)
        } catch (e: Exception) {
            Timber.w(e, "Pairing decrypt failed")
            return@withContext Result.Failure("Hub identity could not be verified — pairing aborted")
        }

        val json = try { JSONObject(String(plaintext, Charsets.UTF_8)) }
        catch (e: Exception) { return@withContext Result.Failure("Pairing payload was not valid JSON") }

        val authToken = json.optString("a", "").ifBlank {
            return@withContext Result.Failure("Pairing payload missing auth token")
        }
        val deviceName = json.optString("n", "Sentinel Hub")
        val host = json.optString("h", payload.host)
        val port = json.optInt("p", payload.port)

        // Persist: host/port go into ConnectionPrefs (same surface manual entry uses).
        // Auth token is encrypted at rest via CredentialCipher under a stable key alias.
        prefsRepo.saveConnectionPrefs(
            ConnectionPrefs(
                hostAddress = host,
                port = port,
                useHttps = false,
                autoConnect = true,
                lastConnectedMs = System.currentTimeMillis(),
            )
        )
        prefsRepo.savePairedHubAuthToken(credentialCipher.encrypt(authToken))

        Result.Success(host = host, port = port, deviceName = deviceName)
    }
}
