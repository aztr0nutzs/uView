package com.sentinel.app.core.security

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
 * CryptoManager — Phase 8
 *
 * Encrypts and decrypts camera credential strings using AES/GCM with keys
 * stored in the Android Keystore. The Keystore is hardware-backed on supported
 * devices, ensuring keys never leave the secure element.
 *
 * Wire format:
 *   Base64( IV_12_BYTES ++ CIPHERTEXT ++ AUTH_TAG_128_BIT )
 *
 * What is real:
 *   - AES-256-GCM encryption with a per-app key in the Android Keystore.
 *   - 12-byte random IV prepended to each ciphertext.
 *   - 128-bit authentication tag providing integrity verification.
 *   - No plaintext fallback — callers must handle encryption failures.
 *
 * What is NOT implemented:
 *   - Key rotation. The key alias is fixed for the app's lifetime.
 *     A production app should version keys and re-encrypt on rotation.
 */
@Singleton
class CryptoManager @Inject constructor() {

    companion object {
        private const val KEY_ALIAS = "sentinel_credential_key"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH_BYTES = 12
        private const val TAG_LENGTH_BITS = 128
    }

    /**
     * Encrypt a plaintext credential string.
     *
     * @return Base64-encoded (IV + ciphertext + auth tag), or null on failure.
     *         Callers MUST NOT store the plaintext if this returns null.
     */
    fun encrypt(plaintext: String): String? {
        if (plaintext.isBlank()) return ""
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv // GCM generates a 12-byte IV automatically
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            // Combine: IV || ciphertext (includes auth tag)
            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Timber.e(e, "CryptoManager.encrypt failed")
            null
        }
    }

    /**
     * Decrypt a Base64-encoded credential string produced by [encrypt].
     *
     * @return Decrypted plaintext, or null on failure (tampered data, wrong key, etc.)
     */
    fun decrypt(encryptedBase64: String): String? {
        if (encryptedBase64.isBlank()) return ""
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < IV_LENGTH_BYTES + 1) {
                Timber.w("CryptoManager.decrypt: input too short — likely not encrypted")
                return null
            }
            val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
            val ciphertext = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            Timber.e(e, "CryptoManager.decrypt failed — data may be corrupt or from a different key")
            null
        }
    }

    /**
     * Check whether a string appears to be a CryptoManager-encrypted value.
     * This is heuristic: checks if the string is valid Base64 of sufficient
     * length to contain an IV + at least 1 byte of ciphertext + auth tag.
     */
    fun isEncrypted(value: String): Boolean {
        if (value.isBlank()) return false
        return try {
            val bytes = Base64.decode(value, Base64.NO_WRAP)
            // Minimum: 12 (IV) + 16 (auth tag) + 1 (payload) = 29 bytes
            bytes.size >= 29
        } catch (_: Exception) {
            false
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)

        // Return existing key if present
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        // Generate a new AES-256 key in the Keystore
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true) // Enforces unique IV per encryption
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
