package com.sentinel.companion.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-256-GCM cipher whose key never leaves the Android Keystore (StrongBox where
 * available, TEE-backed otherwise). Used to encrypt camera credentials at rest in
 * the Room database.
 *
 * Format on disk: `ENC1:<base64(iv)>:<base64(ciphertext+tag)>`
 *
 * The "ENC1" prefix is a version tag so we can recognize and migrate legacy
 * plaintext rows on read without ambiguity.
 */
@Singleton
class CredentialCipher @Inject constructor() {

    fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        return try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            "$VERSION_PREFIX:${b64(iv)}:${b64(ciphertext)}"
        } catch (e: Exception) {
            // We refuse to silently fall back to plaintext — that would defeat the
            // entire purpose. Surface the error so callers can decide.
            Timber.e(e, "CredentialCipher.encrypt failed")
            throw CredentialCipherException("encrypt failed: ${e.javaClass.simpleName}", e)
        }
    }

    /** Returns the plaintext, or returns the input unchanged if it is already plaintext. */
    fun decrypt(stored: String): String {
        if (stored.isEmpty()) return ""
        if (!stored.startsWith("$VERSION_PREFIX:")) {
            // Legacy plaintext row — caller is expected to re-save through encrypt().
            return stored
        }
        return try {
            val parts = stored.split(":")
            require(parts.size == 3) { "malformed envelope" }
            val iv = unb64(parts[1])
            val ciphertext = unb64(parts[2])
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            }
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            Timber.e(e, "CredentialCipher.decrypt failed")
            throw CredentialCipherException("decrypt failed: ${e.javaClass.simpleName}", e)
        }
    }

    /** True when [stored] is in the encrypted envelope format produced by [encrypt]. */
    fun isEncrypted(stored: String): Boolean = stored.startsWith("$VERSION_PREFIX:")

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        gen.init(spec)
        return gen.generateKey()
    }

    private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun unb64(str: String) = Base64.decode(str, Base64.NO_WRAP)

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "sentinel_companion_credential_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        const val VERSION_PREFIX = "ENC1"
    }
}

class CredentialCipherException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
