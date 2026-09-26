package com.pwdgen.app.cloud

/**
 * Generic remote storage abstraction.
 *
 * The cloud only ever holds the base64-encoded *site list*. No master password
 * and no account/login data is uploaded.
 */
interface RemoteStorage {
    /**
     * Fetch the current remote blob.
     * @return the base64 payload, or null if the file does not exist yet.
     */
    suspend fun pull(): String?

    /** Create or overwrite the remote blob. Returns true on success. */
    suspend fun push(content: String): Boolean

    /** Whether the configuration is complete enough to attempt a sync. */
    fun isConfigured(): Boolean

    /** Human readable provider label, e.g. "GitHub". */
    val label: String
}

/** Thrown for non-fatal cloud errors we want to surface to the UI. */
class CloudException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Result of a sync operation, shown as a snackbar text key. */
sealed class SyncResult {
    object Success : SyncResult()
    object NotConfigured : SyncResult()
    data class Failed(val message: String) : SyncResult()
}