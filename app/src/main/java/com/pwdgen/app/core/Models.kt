package com.pwdgen.app.core

/**
 * A saved site entry.
 *
 * SECURITY: the site (service) name is persisted locally as base64 and,
 * optionally, in the user's private cloud file. When the "save login" option is
 * enabled, the login for that site is also persisted (base64 in the same blob).
 * The master password is NEVER stored in the app or in the cloud.
 */
data class SiteEntry(
    val site: String,
    val login: String? = null,
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
