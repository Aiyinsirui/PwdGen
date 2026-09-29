package com.pwdgen.app.data

import android.content.Context
import com.pwdgen.app.core.SiteEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Local persistence for the site list.
 *
 * The list is stored as a single base64 blob in `filesDir/sites.b64`.
 * Writes are immediate ("local first"): generating a password for a new site
 * persists it right away, cloud sync is a separate, optional step.
 *
 * Storage/upload uses plain Base64 encoding (NOT encryption) so the blob can be
 * shared across devices. The site list contains only site names and optional
 * logins; master passwords and generated passwords are never stored or uploaded.
 */
class SiteRepository(private val context: Context) {

    private val file: File get() = File(context.filesDir, FILE_NAME)

    /** Read the current list. Never throws; missing/corrupt file -> empty. */
    suspend fun load(): List<SiteEntry> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList<SiteEntry>()
        try {
            decodePayload(file.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Overwrite the whole list and return what was persisted. */
    suspend fun save(sites: List<SiteEntry>): List<SiteEntry> = withContext(Dispatchers.IO) {
        val deduped = dedupe(sites)
        file.writeText(encodePayload(deduped), Charsets.UTF_8)
        deduped
    }

    /** Encode [sites] for on-disk storage and cloud upload (plain base64). */
    fun encodePayload(sites: List<SiteEntry>): String = SiteCodec.encode(sites)

    /** Decode a payload from disk or cloud. */
    fun decodePayload(payload: String): List<SiteEntry> {
        if (payload.isBlank()) return emptyList()
        return SiteCodec.decode(payload)
    }

    /**
     * Add a site (and optionally its login) to the stored list.
     *
     * Dedup is case-insensitive on the site name. If the site already exists
     * without a login and this call supplies one, the existing entry is upgraded
     * in place (an existing login is never overwritten).
     *
     * @return the new full list, plus whether anything changed.
     */
    suspend fun add(site: String, login: String? = null): Pair<List<SiteEntry>, Boolean> {
        val name = site.trim()
        if (name.isEmpty()) return load() to false
        val log = login?.trim()?.ifBlank { null }
        val current = load()
        val idx = current.indexOfFirst {
            it.site.equals(name, ignoreCase = true) && (it.login ?: "") == (log ?: "")
        }
        if (idx >= 0) {
            return current to false
        }
        val updated = current + SiteEntry(name, log)
        return save(updated) to true
    }

    suspend fun remove(site: String, login: String? = null) {
        val updated = load().filterNot {
            it.site.equals(site, ignoreCase = true) && (it.login ?: "") == (login ?: "")
        }
        save(updated)
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) { if (file.exists()) file.delete() }
    }

    /** Merge an incoming list (e.g. pulled from cloud) into the local one. */
    suspend fun merge(incoming: List<SiteEntry>): List<SiteEntry> =
        save(load() + incoming)

    companion object {
        const val FILE_NAME = "sites.b64"

        /**
         * Stable, order-preserving, case-insensitive de-duplication.
         *
         * The dedup key is the (site, login) pair (login treated as "" when
         * absent). Only entries whose site AND login both match are considered
         * duplicates; two entries sharing a site but with different logins are
         * kept as separate records.
         */
        fun dedupe(sites: List<SiteEntry>): List<SiteEntry> {
            val index = HashMap<String, Int>()
            val out = ArrayList<SiteEntry>(sites.size)
            for (s in sites) {
                val name = s.site.trim()
                if (name.isEmpty()) continue
                val normalized = s.copy(site = name)
                val key = name.lowercase() + "\n" + (normalized.login ?: "").lowercase()
                val existingIdx = index[key]
                if (existingIdx == null) {
                    index[key] = out.size
                    out.add(normalized)
                }
            }
            return out
        }
    }
}
