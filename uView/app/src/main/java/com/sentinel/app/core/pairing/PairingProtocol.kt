package com.sentinel.app.core.pairing

import android.util.Base64
import org.json.JSONObject
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Protocol used by the main app's [PairingHost] and the companion's `PairingClient`.
 *
 * Wire format (one TCP round-trip on host:port from the QR):
 *
 *   client → server   `{"t":"<b64 token>","k":"<b64 X.509 client pubkey>"}\n`
 *   server → client   `{"iv":"<b64>","ct":"<b64>"}\n`
 *
 * Where:
 *   - `t` matches the token embedded in the QR (binds the handshake to one window)
 *   - `k` is the companion's ephemeral ECDH-P256 public key (X.509 SubjectPublicKeyInfo)
 *   - The server replies with AES-256-GCM ciphertext over `{"a":"<authToken>", ...}`
 *   - Key derived via HKDF-SHA256(shared, salt=token, info="sentinel-pair-v1") → 32 bytes
 *
 * MITM safety: the companion uses the server pubkey from the QR for ECDH. A network
 * attacker who swaps the response will fail GCM verification — they cannot read or
 * forge the credential without one party's private key, neither of which is on the wire.
 */
object PairingProtocol {

    const val VERSION = 1
    private const val CURVE = "secp256r1"
    private const val HKDF_INFO = "sentinel-pair-v1"

    fun newKeyPair(): KeyPair {
        val gen = KeyPairGenerator.getInstance("EC")
        gen.initialize(ECGenParameterSpec(CURVE))
        return gen.generateKeyPair()
    }

    fun randomBytes(n: Int): ByteArray = ByteArray(n).also { SecureRandom().nextBytes(it) }

    fun encodePublicKey(key: PublicKey): ByteArray = key.encoded // X.509 SubjectPublicKeyInfo

    fun decodePublicKey(x509: ByteArray): PublicKey =
        KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(x509))

    /** ECDH on P-256 → 32-byte shared secret. */
    fun sharedSecret(ourPrivate: java.security.PrivateKey, theirPublic: PublicKey): ByteArray {
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(ourPrivate)
        ka.doPhase(theirPublic, true)
        return ka.generateSecret()
    }

    /** HKDF-SHA256 extract-then-expand → [length] bytes. */
    fun hkdf(ikm: ByteArray, salt: ByteArray, info: String = HKDF_INFO, length: Int = 32): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(if (salt.isEmpty()) ByteArray(32) else salt, "HmacSHA256"))
        val prk = mac.doFinal(ikm)

        val out = ByteArray(length)
        var t = ByteArray(0)
        var pos = 0
        var counter = 1
        val infoBytes = info.toByteArray(Charsets.UTF_8)
        while (pos < length) {
            mac.init(SecretKeySpec(prk, "HmacSHA256"))
            mac.update(t)
            mac.update(infoBytes)
            mac.update(byteArrayOf(counter.toByte()))
            t = mac.doFinal()
            val take = minOf(t.size, length - pos)
            System.arraycopy(t, 0, out, pos, take)
            pos += take
            counter++
        }
        return out
    }

    fun aesGcmEncrypt(key: ByteArray, plaintext: ByteArray): Pair<ByteArray, ByteArray> {
        val iv = randomBytes(12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return iv to cipher.doFinal(plaintext)
    }

    fun aesGcmDecrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    fun b64(b: ByteArray): String = Base64.encodeToString(b, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    fun unb64(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)

    /** QR payload: a single base64url-encoded JSON envelope so the QR text is alphanumeric. */
    fun encodeQrPayload(host: String, port: Int, serverPubKey: ByteArray, token: ByteArray, expiryMs: Long): String {
        val json = JSONObject()
            .put("v", VERSION)
            .put("h", host)
            .put("p", port)
            .put("k", b64(serverPubKey))
            .put("t", b64(token))
            .put("x", expiryMs)
        return b64(json.toString().toByteArray(Charsets.UTF_8))
    }

    fun decodeQrPayload(encoded: String): QrPayload {
        val raw = unb64(encoded.trim())
        val json = JSONObject(String(raw, Charsets.UTF_8))
        require(json.getInt("v") == VERSION) { "Unsupported pairing version" }
        return QrPayload(
            host = json.getString("h"),
            port = json.getInt("p"),
            serverPubKey = unb64(json.getString("k")),
            token = unb64(json.getString("t")),
            expiryMs = json.getLong("x"),
        )
    }

    data class QrPayload(
        val host: String,
        val port: Int,
        val serverPubKey: ByteArray,
        val token: ByteArray,
        val expiryMs: Long,
    )

    data class HandshakeRequest(val token: ByteArray, val clientPubKey: ByteArray)

    fun encodeRequest(req: HandshakeRequest): ByteArray {
        val s = JSONObject()
            .put("t", b64(req.token))
            .put("k", b64(req.clientPubKey))
            .toString() + "\n"
        return s.toByteArray(Charsets.UTF_8)
    }

    fun decodeRequest(line: String): HandshakeRequest {
        val json = JSONObject(line)
        return HandshakeRequest(
            token = unb64(json.getString("t")),
            clientPubKey = unb64(json.getString("k")),
        )
    }

    fun encodeResponse(iv: ByteArray, ciphertext: ByteArray): ByteArray {
        val s = JSONObject()
            .put("iv", b64(iv))
            .put("ct", b64(ciphertext))
            .toString() + "\n"
        return s.toByteArray(Charsets.UTF_8)
    }

    fun decodeResponse(line: String): Pair<ByteArray, ByteArray> {
        val json = JSONObject(line)
        return unb64(json.getString("iv")) to unb64(json.getString("ct"))
    }

    /** Constant-time byte comparison. */
    fun bytesEqual(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}
