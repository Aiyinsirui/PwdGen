package com.pwdgen.app.data

import android.content.Context
import com.pwdgen.app.core.SiteEntry
import com.pwdgen.app.crypto.SecureStore
import com.pwdgen.app.crypto.SiteCrypto
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

    private val secure = SecureStore(context)
    private val file: File get() = File(context.filesDir, FILE_NAME)

    /** Lazily-created random 256-bit key, persisted in [SecureStore]. */
    private fun cryptoKey(): String {
        secure.getString(SecureStore.KEY_SITES_CRYPTO)?.let { return it }
        val key = SiteCrypto.generateKeyB64()
        secure.putString(SecureStore.KEY_SITES_CRYPTO, key)
        return key
    }

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

    /**
     * Encode [sites] for on-disk storage and cloud upload: AES-256-GCM with a
     * device key, so the plain base64 blob is never written or uploaded.
     */
    fun encodePayload(sites: List<SiteEntry>): String =
        SiteCrypto.encrypt(SiteCodec.encode(sites), cryptoKey())

    /**
     * Decode a payload from disk or cloud. If a blob is the legacy plain
     * base64 format (still produced by previous versions), migrate it in place
     * and return its sites.
     */
    fun decodePayload(payload: String): List<SiteEntry> {
        if (payload.isBlank()) return emptyList()
        if (SiteCrypto.isEncrypted(payload)) {
            val plain = SiteCrypto.decrypt(payload, cryptoKey()) ?: return emptyList()
            return SiteCodec.decode(plain)
        }
        val legacy = SiteCodec.decode(payload)
        if (legacy.isEmpty()) return emptyList()
        file.writeText(encodePayload(legacy), Charsets.UTF_8)
        return legacy
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
        val idx = current.indexOfFirst { it.site.equals(name, ignoreCase = true) }
        if (idx >= 0) {
            val existing = current[idx]
            if (existing.login == null && log != null) {
                val updated = current.toMutableList().apply {
                    set(idx, existing.copy(site = name, login = log))
                }
                return save(updated) to true
            }
            return current to false
        }
        val updated = current + SiteEntry(name, log)
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

        /**
         * Stable, order-preserving, case-insensitive de-duplication.
         *
         * The site name is the dedup key. When the same site appears more than
         * once, the entry that carries a login wins over one that does not; a
         * later more-complete entry also keeps its position.
         */
        fun dedupe(sites: List<SiteEntry>): List<SiteEntry> {
            val indexBySite = HashMap<String, Int>()
            val out = ArrayList<SiteEntry>(sites.size)
            for (s in sites) {
                val name = s.site.trim()
                if (name.isEmpty()) continue
                val normalized = s.copy(site = name)
                val key = name.lowercase()
                val existingIdx = indexBySite[key]
                if (existingIdx == null) {
                    indexBySite[key] = out.size
                    out.add(normalized)
                } else {
                    val existing = out[existingIdx]
                    if (existing.login == null && normalized.login != null) {
                        out[existingIdx] = normalized
                    }
                }
            }
            return out
        }
    }
}