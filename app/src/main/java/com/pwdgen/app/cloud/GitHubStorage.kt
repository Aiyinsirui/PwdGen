package com.pwdgen.app.cloud

import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * Minimal GitHub Contents API client (no third-party HTTP dependency).
 *
 * Reads/writes a single file in a user-owned repository. A private repo is
 * recommended; the app never bundles or transmits a token anywhere except to
 * api.github.com.
 *
 * Create/update require the existing file SHA, so [push] first probes with a
 * GET to obtain it.
 */
class GitHubStorage(
    private val owner: String,
    private val repo: String,
    private val path: String,
    private val branch: String,
    private val token: String
) : RemoteStorage {

    override val label: String get() = "GitHub"

    override fun isConfigured(): Boolean =
        owner.isNotBlank() && repo.isNotBlank() && path.isNotBlank() && token.isNotBlank()

    override suspend fun pull(): String? {
        val resp = request("GET", contentsUrl(), null)
        if (resp.code == 404) return null
        if (resp.code !in 200..299) {
            throw CloudException("GitHub pull failed: HTTP ${resp.code} ${resp.body.take(200)}")
        }
        // The Contents API returns base64 with newlines; strip and re-decode.
        val encoded = JSONObject(resp.body).optString("content", "")
        if (encoded.isBlank()) return null
        val cleaned = encoded.replace("\n", "").replace("\r", "").trim()
        val decodedBytes = try {
            Base64.getMimeDecoder().decode(cleaned)
        } catch (e: Exception) {
            throw CloudException("GitHub content is not valid base64", e)
        }
        return String(decodedBytes, Charsets.UTF_8)
    }

    override suspend fun push(content: String): Boolean {
        val existingSha = currentSha()
        val payload = JSONObject().apply {
            put("message", "pwdgen: update site list")
            put("branch", branch)
            put("content", Base64.getEncoder().encodeToString(content.toByteArray(Charsets.UTF_8)))
            if (existingSha != null) put("sha", existingSha)
        }
        val resp = request("PUT", contentsUrl(), payload.toString())
        if (resp.code !in 200..299) {
            throw CloudException("GitHub push failed: HTTP ${resp.code} ${resp.body.take(200)}")
        }
        return true
    }

    /** Get the blob SHA of the current file, or null if it doesn't exist. */
    private fun currentSha(): String? {
        val resp = request("GET", contentsUrl(), null)
        if (resp.code == 404) return null
        if (resp.code !in 200..299) return null
        return try {
            JSONObject(resp.body).optString("sha", "").ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }

    private fun contentsUrl(): String {
        // Defensive: the Contents API rejects paths with a leading slash or
        // empty segments, which produce a confusing 404 on PUT.
        val cleanPath = path.trim().trimStart('/')
        return "https://api.github.com/repos/$owner/$repo/contents/$cleanPath?ref=$branch"
    }

    private class Resp(val code: Int, val body: String)

    private fun request(method: String, urlStr: String, body: String?): Resp {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "PwdGen-Android")
                doInput = true
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }
            if (body != null) {
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let { s ->
                BufferedReader(s.reader(Charsets.UTF_8)).use { r -> r.readText() }
            } ?: ""
            Resp(code, text)
        } catch (e: Exception) {
            throw CloudException("Network error: ${e.message}", e)
        } finally {
            conn?.disconnect()
        }
    }
}