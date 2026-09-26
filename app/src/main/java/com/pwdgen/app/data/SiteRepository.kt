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
 */
class SiteRepository(private val context: Context) {

    private val file: File get() = File(context.filesDir, FILE_NAME)

    /** Read the current list. Never throws; missing/corrupt file -> empty. */
    suspend fun load(): List<SiteEntry> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList<SiteEntry>()
        try {
            SiteCodec.decode(file.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Overwrite the whole list and return what was persisted. */
    suspend fun save(sites: List<SiteEntry>): List<SiteEntry> = withContext(Dispatchers.IO) {
        val deduped = dedupe(sites)
        file.writeText(SiteCodec.encode(deduped), Charsets.UTF_8)
        deduped
    }

    /**
     * Add a site to the stored list (case-insensitive dedup).
     * @return the new full list, plus whether the site was actually new.
     */
    suspend fun add(site: String): Pair<List<SiteEntry>, Boolean> {
        val name = site.trim()
        if (name.isEmpty()) return load() to false
        val current = load()
        if (current.any { it.site.equals(name, ignoreCase = true) }) {
            return current to false
        }
        val updated = current + SiteEntry(name)
        return save(updated) to true
    }

    suspend fun remove(site: String) {
        val updated = load().filterNot { it.site.equals(site, ignoreCase = true) }
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

        /** Stable order-preserving, case-insensitive de-duplication. */
        fun dedupe(sites: List<SiteEntry>): List<SiteEntry> {
            val seen = HashSet<String>()
            val out = ArrayList<SiteEntry>(sites.size)
            for (s in sites) {
                val name = s.site.trim()
                if (name.isEmpty()) continue
                if (seen.add(name.lowercase())) out.add(s.copy(site = name))
            }
            return out
        }
    }
}