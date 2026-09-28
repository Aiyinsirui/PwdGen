package com.pwdgen.app.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM encryption for the site list.
 *
 * The site list is encrypted before it is written to disk or uploaded to the
 * cloud, so a plain base64 blob is no longer the storage format. The 256-bit
 * key is a random value generated once and persisted in [SecureStore] (which
 * is itself backed by the Android Keystore), keeping the key out of the app's
 * normal storage and out of the uploaded payload.
 *
 * Payload format (versioned, single line, NO_WRAP base64):
 *   "v2:" + base64( iv(12) || ciphertext+tag )
 *
 * [decrypt] returns null when the payload is not in this format, is corrupt, or
 * the wrong key is supplied.
 */
object SiteCrypto {
    private const val PREFIX = "v2:"
    private const val IV_LEN = 12
    private const val TAG_BITS = 128
    private const val KEY_BYTES = 32
    private val rng = SecureRandom()

    /** Generate a new random 256-bit key, base64 encoded for storage. */
    fun generateKeyB64(): String {
        val key = ByteArray(KEY_BYTES).also(rng::nextBytes)
        return Base64.encodeToString(key, Base64.NO_WRAP)
    }

    /** Encrypt [plain] with the base64-encoded 256-bit [keyB64]. */
    fun encrypt(plain: String, keyB64: String): String {
        val key = decodeKey(keyB64)
        val iv = ByteArray(IV_LEN).also(rng::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val out = iv + ciphertext
        return PREFIX + Base64.encodeToString(out, Base64.NO_WRAP)
    }

    /**
     * Decrypt a payload produced by [encrypt]. Returns null for legacy/non-v2
     * payloads, corrupt data, or an incorrect key.
     */
    fun decrypt(payload: String, keyB64: String): String? {
        if (!payload.startsWith(PREFIX)) return null
        val raw = try {
            Base64.decode(payload.removePrefix(PREFIX), Base64.NO_WRAP)
        } catch (e: Exception) {
            return null
        }
        if (raw.size < IV_LEN + TAG_BITS / 8) return null
        val iv = raw.copyOfRange(0, IV_LEN)
        val ciphertext = raw.copyOfRange(IV_LEN, raw.size)
        return try {
            val key = decodeKey(keyB64)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /** Whether [payload] is an encrypted (v2) blob. */
    fun isEncrypted(payload: String): Boolean = payload.startsWith(PREFIX)

    private fun decodeKey(keyB64: String): SecretKeySpec {
        val bytes = Base64.decode(keyB64, Base64.NO_WRAP)
        require(bytes.size == KEY_BYTES) { "Invalid AES key length" }
        return SecretKeySpec(bytes, "AES")
    }
}
