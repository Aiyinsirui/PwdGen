package com.pwdgen.app.data

import android.util.Base64
import com.pwdgen.app.core.SiteEntry
import java.nio.charset.StandardCharsets

/**
 * Encodes/decodes the site list.
 *
 * On-disk / in-cloud format is a single base64 blob (NO_WRAP) containing one
 * UTF-8 entry per line. The raw (decoded) format is intentionally simple so
 * the cloud file stays human-auditable once decoded.
 *
 * Line format (backward compatible):
 *   - legacy: "site"           -> SiteEntry(site, login = null)
 *   - new:    "site\tlogin"    -> SiteEntry(site, login)
 * Only the FIRST tab separates the two fields; anything after it is part of the
 * login (defensive; in practice logins never contain tabs).
 */
object SiteCodec {

    private const val SEP = '\t'

    /** Build the plaintext payload (one entry per line, trailing newline). */
    fun encodeRaw(sites: List<SiteEntry>): String {
        if (sites.isEmpty()) return ""
        val sb = StringBuilder()
        for (s in sites) {
            val name = s.site.trim()
            if (name.isEmpty()) continue
            sb.append(name)
            val login = s.login?.trim()
            if (!login.isNullOrEmpty()) {
                sb.append(SEP).append(login)
            }
            sb.append('\n')
        }
        return sb.toString()
    }

    /** base64(utf8(site\tsite\n...)) with NO_WRAP so it is a single line. */
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
        // De-dup key: "site\rlogin" (both lowercased). Same site may appear once
        // with a login and once without, but obviously-duplicate lines collapse.
        // Order is preserved.
        val seen = LinkedHashSet<String>()
        val out = ArrayList<SiteEntry>()
        raw.split('\n', '\r').forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach
            val entry = parseLine(trimmed)
            if (entry.site.isEmpty()) return@forEach
            val key = entry.site.lowercase() + '\n' + (entry.login ?: "").lowercase()
            if (seen.add(key)) out.add(entry)
        }
        return out
    }

    /** Parse a single raw line into a [SiteEntry]; legacy lines have no tab. */
    private fun parseLine(line: String): SiteEntry {
        val idx = line.indexOf(SEP)
        return if (idx < 0) {
            SiteEntry(line.trim(), null)
        } else {
            val site = line.substring(0, idx).trim()
            val login = line.substring(idx + 1).trim().ifBlank { null }
            SiteEntry(site, login)
        }
    }
}
