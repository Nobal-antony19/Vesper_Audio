package com.vesper.audio.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

enum class ArtworkDiagnosticReason {
    CACHE_HIT,
    EXTRACTED_EMBEDDED,
    MEDIASTORE_FALLBACK,
    REMOTE_URL_PASSTHROUGH,
    PERMISSION_DENIED,
    MISSING_FILE,
    DECODE_FAILURE,
    NULL_ARTWORK
}

object VesperArtworkResolver {

    private const val TAG = "VesperArtwork"
    private const val CACHE_DIR_NAME = "vesper_album_art"
    private const val MAX_CACHE_BYTES = 40L * 1024L * 1024L // 40 MB bounded disk cache
    private const val MAX_NEGATIVE_CACHE_ENTRIES = 1000

    private val inFlightLocks = ConcurrentHashMap<String, Any>()
    private val negativeCacheKeys = ConcurrentHashMap.newKeySet<String>()
    private val brokenAlbumArtUris = ConcurrentHashMap.newKeySet<String>()
    private val lastDiagnosticByTrack = ConcurrentHashMap<String, ArtworkDiagnosticReason>()

    fun buildCacheKey(trackId: String, dateModified: Long, sizeBytes: Long): String {
        val safeTrackId = trackId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return "trk_${safeTrackId}_${dateModified}_${sizeBytes}"
    }

    fun getLastDiagnosticReason(trackId: String): ArtworkDiagnosticReason? {
        return lastDiagnosticByTrack[trackId]
    }

    fun markArtworkUriBroken(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        if (brokenAlbumArtUris.size >= MAX_NEGATIVE_CACHE_ENTRIES) {
            brokenAlbumArtUris.clear()
        }
        brokenAlbumArtUris.add(uriString)
    }

    fun isArtworkUriBroken(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return true
        return brokenAlbumArtUris.contains(uriString)
    }

    /**
     * Fast, non-blocking artwork resolution for Phase A inventory scans.
     * Checks existing deterministic per-track disk cache first, then falls back to MediaStore's
     * indexed album art URI without opening MediaMetadataRetriever.
     */
    fun resolveFastArtworkUri(
        context: Context,
        albumId: Long,
        trackId: String,
        dateModified: Long,
        sizeBytes: Long
    ): Uri? {
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        val cacheKey = buildCacheKey(trackId, dateModified, sizeBytes)
        val jpgFile = File(cacheDir, "$cacheKey.jpg")
        if (isValidCacheFile(jpgFile)) {
            lastDiagnosticByTrack[trackId] = ArtworkDiagnosticReason.CACHE_HIT
            return Uri.fromFile(jpgFile)
        }
        val pngFile = File(cacheDir, "$cacheKey.png")
        if (isValidCacheFile(pngFile)) {
            lastDiagnosticByTrack[trackId] = ArtworkDiagnosticReason.CACHE_HIT
            return Uri.fromFile(pngFile)
        }

        if (albumId > 0) {
            val albumArtUriStr = "content://media/external/audio/albumart/$albumId"
            if (!brokenAlbumArtUris.contains(albumArtUriStr)) {
                lastDiagnosticByTrack[trackId] = ArtworkDiagnosticReason.MEDIASTORE_FALLBACK
                return Uri.parse(albumArtUriStr)
            }
        }
        lastDiagnosticByTrack[trackId] = ArtworkDiagnosticReason.NULL_ARTWORK
        return null
    }

    fun resolveArtworkUri(
        context: Context,
        audioUri: Uri,
        albumId: Long,
        trackId: String,
        dateModified: Long,
        sizeBytes: Long
    ): Uri? {
        // Guard: Never pass remote HTTP/HTTPS URLs through local MediaMetadataRetriever or MediaStore
        val scheme = audioUri.scheme?.lowercase(Locale.ROOT)
        if (scheme == "http" || scheme == "https") {
            lastDiagnosticByTrack[trackId] = ArtworkDiagnosticReason.REMOTE_URL_PASSTHROUGH
            return null
        }

        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val cacheKey = buildCacheKey(trackId, dateModified, sizeBytes)
        val jpgFile = File(cacheDir, "$cacheKey.jpg")
        val pngFile = File(cacheDir, "$cacheKey.png")

        // 1. Fast path: check existing valid cached file
        if (isValidCacheFile(jpgFile)) {
            lastDiagnosticByTrack[trackId] = ArtworkDiagnosticReason.CACHE_HIT
            return Uri.fromFile(jpgFile)
        }
        if (isValidCacheFile(pngFile)) {
            lastDiagnosticByTrack[trackId] = ArtworkDiagnosticReason.CACHE_HIT
            return Uri.fromFile(pngFile)
        }

        // 2. If this exact (trackId, dateModified, sizeBytes) already failed embedded extraction, skip re-extracting
        if (!negativeCacheKeys.contains(cacheKey)) {
            val lock = inFlightLocks.getOrPut(cacheKey) { Any() }
            synchronized(lock) {
                try {
                    if (isValidCacheFile(jpgFile)) {
                        lastDiagnosticByTrack[trackId] = ArtworkDiagnosticReason.CACHE_HIT
                        return Uri.fromFile(jpgFile)
                    }
                    if (isValidCacheFile(pngFile)) {
                        lastDiagnosticByTrack[trackId] = ArtworkDiagnosticReason.CACHE_HIT
                        return Uri.fromFile(pngFile)
                    }

                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, audioUri)
                        val embeddedArt = retriever.embeddedPicture
                        if (embeddedArt != null && embeddedArt.isNotEmpty()) {
                            val savedUri = saveToCache(embeddedArt, cacheDir, cacheKey)
                            if (savedUri != null) {
                                trimCacheIfNeeded(cacheDir)
                                lastDiagnosticByTrack[trackId] = ArtworkDiagnosticReason.EXTRACTED_EMBEDDED
                                return savedUri
                            } else {
                                lastDiagnosticByTrack[trackId] = ArtworkDiagnosticReason.DECODE_FAILURE
                            }
                        } else {
                            lastDiagnosticByTrack[trackId] = ArtworkDiagnosticReason.NULL_ARTWORK
                        }
                        recordNegativeCache(cacheKey)
                    } catch (se: SecurityException) {
                        lastDiagnosticByTrack[trackId] = ArtworkDiagnosticReason.PERMISSION_DENIED
                        Log.w(TAG, "Permission denied extracting artwork for $trackId: ${se.message}")
                    } catch (fnf: FileNotFoundException) {
                        lastDiagnosticByTrack[trackId] = ArtworkDiagnosticReason.MISSING_FILE
                        recordNegativeCache(cacheKey)
                    } catch (e: Exception) {
                        lastDiagnosticByTrack[trackId] = ArtworkDiagnosticReason.MISSING_FILE
                        recordNegativeCache(cacheKey)
                    } finally {
                        try {
                            retriever.release()
                        } catch (_: Exception) {
                        }
                    }
                } finally {
                    inFlightLocks.remove(cacheKey, lock)
                }
            }
        }

        // 3. Fallback to MediaStore Generic URI if no embedded ID3 picture exists and URI is not marked broken
        if (albumId > 0) {
            val albumArtUriStr = "content://media/external/audio/albumart/$albumId"
            if (!brokenAlbumArtUris.contains(albumArtUriStr)) {
                lastDiagnosticByTrack[trackId] = ArtworkDiagnosticReason.MEDIASTORE_FALLBACK
                return Uri.parse(albumArtUriStr)
            }
        }

        return null
    }

    private fun recordNegativeCache(cacheKey: String) {
        if (negativeCacheKeys.size >= MAX_NEGATIVE_CACHE_ENTRIES) {
            negativeCacheKeys.clear()
        }
        negativeCacheKeys.add(cacheKey)
    }

    private fun isValidCacheFile(file: File): Boolean {
        return file.exists() && file.isFile && file.length() > 64L
    }

    private fun saveToCache(imageBytes: ByteArray, cacheDir: File, cacheKey: String): Uri? {
        return try {
            // 1. Decode bounds and mime type without allocating pixel memory
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, boundsOptions)
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

            var sampleSize = 1
            val maxEdge = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
            while ((maxEdge / sampleSize) > 1024) {
                sampleSize *= 2
            }

            val isPng = boundsOptions.outMimeType?.contains("png", ignoreCase = true) == true
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                // Use RGB_565 for opaque JPEG covers (saves 50% RAM), ARGB_8888 only when PNG may contain alpha
                inPreferredConfig = if (isPng) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
            }

            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, decodeOptions) ?: return null
            val hasAlpha = isPng && bitmap.hasAlpha()
            val targetFile = File(cacheDir, if (hasAlpha) "$cacheKey.png" else "$cacheKey.jpg")
            val tempFile = File(cacheDir, "$cacheKey.tmp")

            FileOutputStream(tempFile).use { out ->
                if (hasAlpha) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                } else {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                out.flush()
            }
            bitmap.recycle()

            if (tempFile.exists() && tempFile.length() > 64L) {
                if (targetFile.exists()) targetFile.delete()
                if (tempFile.renameTo(targetFile)) {
                    return Uri.fromFile(targetFile)
                }
            }
            tempFile.delete()
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun trimCacheIfNeeded(cacheDir: File) {
        try {
            val files = cacheDir.listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") } ?: return
            var totalBytes = files.sumOf { it.length() }
            if (totalBytes <= MAX_CACHE_BYTES) return

            // Evict oldest files first until under 80% of MAX_CACHE_BYTES
            val targetBytes = (MAX_CACHE_BYTES * 0.8).toLong()
            val sortedByOldest = files.sortedBy { it.lastModified() }
            for (file in sortedByOldest) {
                val len = file.length()
                if (file.delete()) {
                    totalBytes -= len
                }
                if (totalBytes <= targetBytes) break
            }
        } catch (_: Exception) {
        }
    }

    fun getArtworkCacheSizeBytes(context: Context): Long {
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        return cacheDir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
    }

    fun clearArtworkCache(context: Context) {
        negativeCacheKeys.clear()
        brokenAlbumArtUris.clear()
        lastDiagnosticByTrack.clear()
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            else -> String.format(Locale.US, "%.0f KB", kb)
        }
    }
}
