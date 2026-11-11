package com.vesper.audio.util

import android.net.Uri
import java.net.URI

/**
 * Security validation utilities for remote URLs, media URIs, and external identifiers.
 * Enforces HTTPS-only remote transport, blocks embedded URL credentials, and prevents
 * path traversal or unsafe URI schemes.
 */
object SecurityValidator {

    private val SAFE_ID_REGEX = Regex("^[A-Za-z0-9._:-]{1,128}$")

    /**
     * Validates that a remote URL uses strict HTTPS, contains a valid host,
     * has no embedded credentials (userInfo), and contains no control characters.
     */
    fun isValidHttpsUrl(rawUrl: String?): Boolean {
        if (rawUrl.isNullOrBlank()) return false
        val trimmed = rawUrl.trim()
        if (trimmed.length > 2048) return false
        if (trimmed.any { it.isISOControl() || it.isWhitespace() }) return false
        if (!trimmed.startsWith("https://", ignoreCase = true)) return false

        return try {
            val jUri = URI(trimmed)
            val scheme = jUri.scheme?.lowercase() ?: return false
            val host = jUri.host?.lowercase() ?: return false
            if (scheme != "https") return false
            if (host.isBlank() || host == "localhost" || host == "127.0.0.1" || host.startsWith("0.")) return false
            if (jUri.userInfo != null) return false
            val path = jUri.rawPath.orEmpty()
            if (path.contains("..")) return false
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Validates that a playback or artwork Uri is safe to pass to Media3 or Coil.
     * - Remote URLs must pass [isValidHttpsUrl] (strict HTTPS, no cleartext HTTP).
     * - Local URIs (`content://`, `file://`, `android.resource://`) must not contain `..` path traversal.
     */
    fun isValidMediaUri(uri: Uri?): Boolean {
        if (uri == null || uri == Uri.EMPTY) return false
        val raw = uri.toString().trim()
        if (raw.isBlank() || raw.length > 2048) return false
        if (raw.any { it.isISOControl() }) return false

        val scheme = uri.scheme?.lowercase() ?: return false
        return when (scheme) {
            "https" -> isValidHttpsUrl(raw)
            "content", "android.resource" -> {
                val path = uri.path.orEmpty()
                !path.contains("..") && !raw.contains("..")
            }
            "file" -> {
                val path = uri.path.orEmpty()
                path.isNotBlank() && !path.contains("..") && !raw.contains("..")
            }
            else -> false
        }
    }

    /**
     * Sanitizes a remote provider track ID so it cannot inject path segments or query parameters.
     */
    fun sanitizeProviderTrackId(rawId: String?): String {
        if (rawId.isNullOrBlank()) return ""
        val trimmed = rawId.trim()
        return if (SAFE_ID_REGEX.matches(trimmed) && !trimmed.contains("..")) {
            trimmed
        } else {
            trimmed
                .replace("..", "")
                .replace(Regex("[^A-Za-z0-9._-]"), "")
                .replace("..", "")
                .take(128)
        }
    }
}
