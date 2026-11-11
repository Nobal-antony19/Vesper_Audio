package com.vesper.audio.data.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.vesper.audio.model.Track
import com.vesper.audio.util.VesperArtworkResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SafDocumentHeader(
    val treeUri: Uri,
    val documentUri: Uri,
    val documentId: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val lastModified: Long
) {
    val stableTrackId: String
        get() = if (documentUri.toString().startsWith("saf:")) documentUri.toString() else "saf:$documentUri"
}

class SafFolderScanner(private val context: Context) {

    private val supportedAudioExtensions = setOf("mp3", "m4a", "flac", "wav", "aac", "ogg", "opus")

    /**
     * Phase A: Fast, non-blocking SAF directory inventory scan using an explicit traversal queue
     * (prevents stack overflow on deep trees) and zero MediaMetadataRetriever allocations.
     */
    suspend fun scanDirectoryHeaders(treeUri: Uri): List<SafDocumentHeader> = withContext(Dispatchers.IO) {
        val headers = mutableListOf<SafDocumentHeader>()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        try {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val queue = ArrayDeque<String>()
            val visitedDirs = mutableSetOf<String>()
            queue.addLast(rootDocId)

            while (queue.isNotEmpty()) {
                val currentDirDocId = queue.removeFirst()
                if (!visitedDirs.add(currentDirDocId)) continue

                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentDirDocId)
                try {
                    context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                        val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                        val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                        val modifiedCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                        while (cursor.moveToNext()) {
                            val childDocId = cursor.getString(idCol) ?: continue
                            val name = cursor.getString(nameCol) ?: "Unknown"
                            if (name.startsWith(".")) continue // Ignore hidden dotfiles
                            val mime = cursor.getString(mimeCol) ?: ""
                            val size = cursor.getLong(sizeCol)
                            val modified = cursor.getLong(modifiedCol)

                            if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                                queue.addLast(childDocId)
                            } else {
                                val ext = name.substringAfterLast('.', "").lowercase()
                                if (ext in supportedAudioExtensions || mime.startsWith("audio/")) {
                                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                                    headers.add(
                                        SafDocumentHeader(
                                            treeUri = treeUri,
                                            documentUri = fileUri,
                                            documentId = childDocId,
                                            displayName = name,
                                            mimeType = mime.ifBlank { "audio/mpeg" },
                                            sizeBytes = size,
                                            lastModified = modified
                                        )
                                    )
                                }
                            }
                        }
                    }
                } catch (se: SecurityException) {
                    Log.w("SafFolderScanner", "SAF child query permission denied for $childrenUri: ${se.message}")
                } catch (e: Exception) {
                    Log.w("SafFolderScanner", "Error querying SAF children $childrenUri: ${e.message}")
                }
            }
        } catch (se: SecurityException) {
            Log.w("SafFolderScanner", "SAF permission revoked or unavailable for $treeUri: ${se.message}")
        } catch (e: Exception) {
            Log.w("SafFolderScanner", "Error scanning SAF directory $treeUri: ${e.message}")
        }
        headers
    }

    /**
     * Phase B: Selective metadata & artwork extraction for a single new or modified SAF document.
     */
    suspend fun extractTrackFromHeader(header: SafDocumentHeader): Track? = withContext(Dispatchers.IO) {
        extractTrack(
            uri = header.documentUri,
            fileName = header.displayName,
            size = header.sizeBytes,
            modified = header.lastModified
        )
    }

    suspend fun scanDirectory(treeUri: Uri): List<Track> = withContext(Dispatchers.IO) {
        val headers = scanDirectoryHeaders(treeUri)
        headers.mapNotNull { extractTrackFromHeader(it) }
    }

    private fun extractTrack(uri: Uri, fileName: String, size: Long, modified: Long): Track? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
            val title = if (!rawTitle.isNullOrBlank()) rawTitle else fileName.substringBeforeLast('.')
            val rawArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
            val artist = if (rawArtist.isNullOrBlank() || rawArtist.contains("<unknown>", true)) "Unknown Artist" else rawArtist
            val rawAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()
            val album = if (rawAlbum.isNullOrBlank() || rawAlbum.contains("<unknown>", true)) "Unknown Album" else rawAlbum
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore('/')?.trim()?.toIntOrNull() ?: 0
            val discNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
                ?.substringBefore('/')?.trim()?.toIntOrNull() ?: 1
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?.take(4)?.toIntOrNull() ?: 0
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.trim() ?: ""
            val albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)?.trim() ?: ""
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "audio/mpeg"

            val rawUriStr = uri.toString()
            val stableId = if (rawUriStr.startsWith("saf:")) rawUriStr else "saf:$rawUriStr"
            val artworkUri = VesperArtworkResolver.resolveArtworkUri(
                context = context,
                audioUri = uri,
                albumId = 0L,
                trackId = stableId,
                dateModified = modified,
                sizeBytes = size
            )

            Track(
                id = stableId,
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                contentUri = uri,
                artworkUri = artworkUri,
                trackNumber = trackNumber,
                discNumber = discNumber,
                year = year,
                genre = genre,
                albumArtist = albumArtist,
                mimeType = mimeType,
                dateAdded = if (modified > 0L) modified / 1000L else System.currentTimeMillis() / 1000L,
                sizeBytes = size
            )
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
