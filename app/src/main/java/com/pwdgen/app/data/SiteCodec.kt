package com.pwdgen.app.data

import android.util.Base64
import com.pwdgen.app.core.SiteEntry
import java.nio.charset.StandardCharsets

/**
 * Encodes/decodes the site list.
 *
 * On-disk / in-cloud format is a single base64 blob (NO_WRAP) containing one
 * UTF-8 "site" per line. The raw (decoded) format is intentionally simple so
 * the cloud file stays human-auditable once decoded.
 */
object SiteCodec {

    /** Build the plaintext payload (one site per line, trailing newline). */
    fun encodeRaw(sites: List<SiteEntry>): String {
        if (sites.isEmpty()) return ""
        val sb = StringBuilder()
        for (s in sites) {
            val name = s.site.trim()
            if (name.isEmpty()) continue
            sb.append(name).append('\n')
        }
        return sb.toString()
    }

    /** base64(utf8(site\nsite\n...)) with NO_WRAP so it is a single line. */
    fun encode(sites: List<SiteEntry>): String {
        val raw = encodeRaw(sites)
        return if (raw.isEmpty()) "" else
            Base64.encodeToString(raw.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
    }

    /**
     * Decode a base64 payload back into entries. Tolerant: invalid base64 or
     * empty input yields an empty list rather than throwing.
     */
    fun decode(encoded: String?): List<SiteEntry> {
        if (encoded.isNullOrBlank()) return emptyList()
        val raw = try {
            val bytes = Base64.decode(encoded.trim(), Base64.DEFAULT)
            String(bytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            return emptyList()
        }
        return parseRaw(raw)
    }

    /** Parse the plaintext payload (used when the user edits the raw form). */
    fun parseRaw(raw: String): List<SiteEntry> {
        val out = LinkedHashSet<String>()
        raw.split('\n', '\r').forEach { line ->
            val name = line.trim()
            if (name.isNotEmpty()) out.add(name)
        }
        return out.map { SiteEntry(it) }
    }
}
