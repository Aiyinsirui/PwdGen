package com.pwdgen.app.core

/**
 * A saved site entry.
 *
 * SECURITY: only the site (service) name is persisted -- locally as base64 and,
 * optionally, in the user's private cloud file. The master password and the
 * account/login are NEVER stored in the app or in the cloud.
 */
data class SiteEntry(
    val site: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** Result of a generate request, fed into the algorithm. */
data class GenerateRequest(
    val site: String,
    val login: String,
    val master: String,
    val length: Int = 10,
    val counter: Int = 1,
    val useSymbols: Boolean = true
)
