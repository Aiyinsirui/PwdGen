package com.pwdgen.app.core

import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Deterministic password generator.
 *
 * Faithful Kotlin port of the pwdgen.py reference implementation:
 *   algorithm : PBKDF2-HMAC-SHA256
 *   iterations: 100_000
 *   salt      : UTF-8("site:login:counter")
 *   mapping   : raw byte -> charset[b % charset.length], then forced
 *               char-class distribution using a separate offset in the
 *               same deterministic byte stream.
 *
 * IMPORTANT: This is byte-for-byte compatible with the Termux/python tool.
 * It is NOT compatible with upstream LessPass (different salt composition).
 *
 * Fully deterministic: no random, no time, no environment reads.
 */
object PasswordGenerator {

    const val PBKDF2_ITERATIONS = 100_000
    const val DEFAULT_LENGTH = 10
    const val MIN_LENGTH = 8
    const val SPECIAL_CHARS = "!@#\$%^&*"

    const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    const val DIGITS = "0123456789"

    class GenException(message: String) : Exception(message)

    /** Deterministically derive [dklen] bytes from (site, login, master, counter). */
    private fun pbkdf2Derive(
        site: String,
        login: String,
        master: String,
        counter: Int,
        length: Int,
    ): ByteArray {
        val salt = "$site:$login:$counter".toByteArray(Charsets.UTF_8)
        val dklen = maxOf(length * 4, 128)
        val spec = PBEKeySpec(master.toCharArray(), salt, PBKDF2_ITERATIONS, dklen * 8)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val bytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        // Guard: some providers may return a slightly different length; trim to dklen.
        return if (bytes.size == dklen) bytes else bytes.copyOf(dklen)
    }

    /** Map raw bytes into a string using only the given charset. */
    private fun bytesToCharset(raw: ByteArray, charset: String): String {
        val sb = StringBuilder(raw.size)
        for (b in raw) {
            val idx = (b.toInt() and 0xFF) % charset.length
            sb.append(charset[idx])
        }
        return sb.toString()
    }

    /**
     * Generate a deterministic password.
     *
     * Mirrors pwdgen.py::deterministic_password exactly, including the
     * order of the incoming byte stream and the position-selection loop.
     */
    fun generate(
        site: String,
        login: String,
        master: String,
        length: Int = DEFAULT_LENGTH,
        useSpecial: Boolean = true,
        counter: Int = 1,
    ): String {
        if (length < MIN_LENGTH) throw GenException("length must be >= $MIN_LENGTH")
        if (counter < 1) throw GenException("counter must be >= 1")

        val raw = pbkdf2Derive(site, login, master, counter, length)

        // NOTE: charset order MUST be lowercase + uppercase + digits (matching
        // python's string.ascii_letters which is lowercase-first). Changing the
        // order breaks byte-compatibility with the reference tool.
        val charset = StringBuilder(LOWER).append(UPPER).append(DIGITS).apply {
            if (useSpecial) append(SPECIAL_CHARS)
        }.toString()

        // primary = bytes_to_charset(raw[:length*2], charset)[:length]
        val primary = bytesToCharset(raw.copyOfRange(0, length * 2), charset).substring(0, length)
        val pwd = primary.toCharArray()

        val classes = ArrayList<String>(4).apply {
            add(UPPER); add(LOWER); add(DIGITS)
            if (useSpecial) add(SPECIAL_CHARS)
        }

        val usedPositions = HashSet<Int>()
        var streamIdx = length * 2

        for (charsetCls in classes) {
            var attempts = 0
            var pos = 0
            while (attempts < 256) {
                pos = (raw[streamIdx % raw.size].toInt() and 0xFF) % length
                streamIdx += 1
                if (pos !in usedPositions) break
                attempts += 1
            }
            val ch = charsetCls[(raw[streamIdx % raw.size].toInt() and 0xFF) % charsetCls.length]
            streamIdx += 1
            pwd[pos] = ch
            usedPositions.add(pos)
        }

        for (i in 0 until length) {
            if (pwd[i] == '\u0000') {
                pwd[i] = charset[(raw[streamIdx % raw.size].toInt() and 0xFF) % charset.length]
                streamIdx += 1
            }
        }

        return String(pwd.copyOfRange(0, length))
    }

    /** Strength metrics for UI display. */
    fun entropyBits(length: Int, useSpecial: Boolean): Double {
        val pool = 26 + 26 + 10 + if (useSpecial) SPECIAL_CHARS.length else 0
        return length * (kotlin.math.ln(pool.toDouble()) / kotlin.math.ln(2.0))
    }

    /** SHA-256 fingerprint of a master password (for the "saved master" sanity check). */
    fun masterFingerprint(master: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val d = md.digest(master.toByteArray(Charsets.UTF_8))
        return d.joinToString("") { "%02x".format(it) }.take(16)
    }
}
