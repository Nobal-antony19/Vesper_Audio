package com.vesper.audio.util

import android.net.Uri
import java.net.URI

object VesperValidationUtils {

    private val SUPPORTED_AUDIO_EXTENSIONS = setOf("mp3", "m4a", "wav", "flac", "ogg", "aac", "opus")
    private val HEX_COLOR_REGEX = Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$")

    sealed class UrlValidationResult {
        object Valid : UrlValidationResult()
        data class Invalid(val reason: String) : UrlValidationResult()
    }

    /**
     * Validates an audio download URL for security, scheme, host safety, and valid audio formats.
     */
    fun validateDownloadUrl(rawUrl: String): UrlValidationResult {
        if (rawUrl.isBlank()) {
            return UrlValidationResult.Invalid("URL cannot be empty")
        }

        val trimmed = rawUrl.trim()
        val uri = try {
            URI(trimmed)
        } catch (e: Exception) {
            return UrlValidationResult.Invalid("Malformed URL syntax")
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return UrlValidationResult.Invalid("Only HTTP and HTTPS protocols are permitted")
        }

        val host = uri.host?.lowercase()
        if (host.isNullOrBlank()) {
            return UrlValidationResult.Invalid("Missing URL host")
        }

        // Prevent SSRF / internal network probing
        if (host == "localhost" ||
            host == "127.0.0.1" ||
            host.startsWith("192.168.") ||
            host.startsWith("10.") ||
            host.startsWith("169.254.") ||
            host.endsWith(".internal") ||
            host.endsWith(".local")
        ) {
            return UrlValidationResult.Invalid("Private and loopback addresses are not permitted")
        }

        // Disallow embedded user credentials
        if (uri.userInfo != null) {
            return UrlValidationResult.Invalid("Embedded user credentials in URL are prohibited")
        }

        val path = uri.path ?: ""
        val extension = path.substringAfterLast('.', "").lowercase()
        if (extension.isNotBlank() && extension !in SUPPORTED_AUDIO_EXTENSIONS) {
            return UrlValidationResult.Invalid("Unsupported audio file extension: .$extension (Allowed: ${SUPPORTED_AUDIO_EXTENSIONS.joinToString()})")
        }

        return UrlValidationResult.Valid
    }

    /**
     * Sanitizes file names to prevent path traversal, control character injection, and invalid characters.
     */
    fun sanitizeFileName(rawName: String, fallbackExtension: String = "mp3"): String {
        var cleaned = rawName.replace("\\", "/").substringAfterLast('/')
        cleaned = cleaned.replace(Regex("[^a-zA-Z0-9._-]"), "_").trim()
        cleaned = cleaned.replace(Regex("\\.{2,}"), ".") // Prevent double-dot traversal

        if (cleaned.isBlank() || cleaned == "." || cleaned == "..") {
            cleaned = "vesper_audio_${System.currentTimeMillis()}.$fallbackExtension"
        }

        if (!cleaned.contains('.')) {
            cleaned = "$cleaned.$fallbackExtension"
        }

        // Enforce max length of 100 characters while preserving extension
        val ext = cleaned.substringAfterLast('.', fallbackExtension)
        val nameWithoutExt = cleaned.substringBeforeLast('.')
        val truncatedName = if (nameWithoutExt.length > 80) nameWithoutExt.substring(0, 80) else nameWithoutExt
        return "$truncatedName.$ext"
    }

    /**
     * Validates track metadata inputs for editing.
     */
    fun validateMetadata(title: String, artist: String, album: String): Pair<Boolean, String?> {
        val t = title.trim()
        val a = artist.trim()
        val al = album.trim()

        if (t.isEmpty()) {
            return false to "Track title cannot be blank"
        }
        if (t.length > 200) {
            return false to "Track title exceeds 200 characters"
        }
        if (a.length > 200) {
            return false to "Artist name exceeds 200 characters"
        }
        if (al.length > 200) {
            return false to "Album name exceeds 200 characters"
        }

        return true to null
    }

    /**
     * Validates hex color code format (#RRGGBB or #AARRGGBB).
     */
    fun validateHexColor(hex: String): Boolean {
        return HEX_COLOR_REGEX.matches(hex.trim())
    }
}
