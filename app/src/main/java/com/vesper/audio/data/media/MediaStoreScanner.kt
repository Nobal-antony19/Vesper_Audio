package com.vesper.audio.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.vesper.audio.model.Album
import com.vesper.audio.model.Artist
import com.vesper.audio.model.Track
import com.vesper.audio.util.VesperArtworkResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreScanner(private val context: Context) {

    companion object {
        /**
         * Decodes Android MediaStore's combined TRACK integer (e.g. 1004 -> Disc 1, Track 4)
         * into a Pair(trackNumber, discNumber).
         */
        fun decodeMediaStoreTrackAndDisc(rawTrack: Int): Pair<Int, Int> {
            if (rawTrack <= 0) return 0 to 1
            return if (rawTrack >= 1000) {
                val disc = (rawTrack / 1000).coerceAtLeast(1)
                val track = (rawTrack % 1000).coerceAtLeast(0)
                track to disc
            } else {
                rawTrack to 1
            }
        }

        fun sanitizeTag(raw: String?, fallback: String): String {
            val trimmed = raw?.trim().orEmpty()
            return if (trimmed.isBlank() || trimmed.equals("<unknown>", ignoreCase = true) || trimmed.contains("<unknown>", ignoreCase = true)) {
                fallback
            } else {
                trimmed
            }
        }

        fun fallbackTitle(rawTitle: String?, filePath: String?): String {
            val cleaned = rawTitle?.trim().orEmpty()
            if (cleaned.isNotBlank() && !cleaned.equals("<unknown>", ignoreCase = true) && !cleaned.equals("Unknown Track", ignoreCase = true)) {
                return cleaned
            }
            val fileName = filePath?.substringAfterLast('/')?.substringBeforeLast('.')?.trim().orEmpty()
            return if (fileName.isNotBlank()) fileName else "Unknown Track"
        }
    }

    suspend fun scanAudioTracks(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val contentResolver: ContentResolver = context.contentResolver

        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projectionList = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projectionList.add(MediaStore.Audio.Media.ALBUM_ARTIST)
        }

        val projection = projectionList.toTypedArray()
        val selection = "(${MediaStore.Audio.Media.DURATION} >= 2000 OR ${MediaStore.Audio.Media.DURATION} IS NULL OR ${MediaStore.Audio.Media.DURATION} = 0)"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackNumberColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val albumArtistColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val filePath = if (dataColumn >= 0) cursor.getString(dataColumn) else null
                    val title = fallbackTitle(cursor.getString(titleColumn), filePath)
                    val artist = sanitizeTag(cursor.getString(artistColumn), "Unknown Artist")
                    val album = sanitizeTag(cursor.getString(albumColumn), "Unknown Album")
                    val albumId = cursor.getLong(albumIdColumn)
                    val duration = cursor.getLong(durationColumn).coerceAtLeast(0L)
                    val rawTrack = cursor.getInt(trackNumberColumn)
                    val (trackNumber, discNumber) = decodeMediaStoreTrackAndDisc(rawTrack)
                    val year = cursor.getInt(yearColumn).coerceAtLeast(0)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val dateModified = if (dateModifiedColumn >= 0) cursor.getLong(dateModifiedColumn) else 0L
                    val size = cursor.getLong(sizeColumn).coerceAtLeast(0L)
                    val mime = if (mimeColumn >= 0) cursor.getString(mimeColumn) ?: "audio/mpeg" else "audio/mpeg"
                    val rawAlbumArtist = if (albumArtistColumn >= 0) cursor.getString(albumArtistColumn) ?: "" else ""
                    val albumArtist = sanitizeTag(rawAlbumArtist, "")

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val artworkUri = VesperArtworkResolver.resolveArtworkUri(
                        context = context,
                        audioUri = contentUri,
                        albumId = albumId,
                        trackId = id.toString(),
                        dateModified = dateModified,
                        sizeBytes = size
                    )

                    tracks.add(
                        Track(
                            id = id.toString(),
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            contentUri = contentUri,
                            artworkUri = artworkUri,
                            trackNumber = trackNumber,
                            discNumber = discNumber,
                            year = year,
                            albumArtist = albumArtist,
                            mimeType = mime,
                            dateAdded = dateAdded,
                            sizeBytes = size
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        tracks
    }

    data class RawMediaHeader(
        val mediaStoreId: Long,
        val title: String,
        val artist: String,
        val album: String,
        val albumId: Long,
        val durationMs: Long,
        val trackNumber: Int,
        val discNumber: Int = 1,
        val year: Int,
        val dateAdded: Long,
        val dateModified: Long,
        val sizeBytes: Long,
        val mimeType: String,
        val filePath: String? = null,
        val albumArtist: String = ""
    ) {
        fun toFastTrack(context: Context): Track {
            val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId)
            val artworkUri = VesperArtworkResolver.resolveFastArtworkUri(
                context = context,
                albumId = albumId,
                trackId = mediaStoreId.toString(),
                dateModified = dateModified,
                sizeBytes = sizeBytes
            )

            return Track(
                id = mediaStoreId.toString(),
                title = fallbackTitle(title, filePath),
                artist = sanitizeTag(artist, "Unknown Artist"),
                album = sanitizeTag(album, "Unknown Album"),
                durationMs = durationMs.coerceAtLeast(0L),
                contentUri = contentUri,
                artworkUri = artworkUri,
                trackNumber = trackNumber,
                discNumber = discNumber,
                year = year.coerceAtLeast(0),
                albumArtist = sanitizeTag(albumArtist, ""),
                mimeType = mimeType,
                dateAdded = dateAdded,
                sizeBytes = sizeBytes
            )
        }

        fun toTrack(context: Context): Track {
            val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaStoreId)
            val artworkUri = VesperArtworkResolver.resolveArtworkUri(
                context = context,
                audioUri = contentUri,
                albumId = albumId,
                trackId = mediaStoreId.toString(),
                dateModified = dateModified,
                sizeBytes = sizeBytes
            )

            return Track(
                id = mediaStoreId.toString(),
                title = fallbackTitle(title, filePath),
                artist = sanitizeTag(artist, "Unknown Artist"),
                album = sanitizeTag(album, "Unknown Album"),
                durationMs = durationMs.coerceAtLeast(0L),
                contentUri = contentUri,
                artworkUri = artworkUri,
                trackNumber = trackNumber,
                discNumber = discNumber,
                year = year.coerceAtLeast(0),
                albumArtist = sanitizeTag(albumArtist, ""),
                mimeType = mimeType,
                dateAdded = dateAdded,
                sizeBytes = sizeBytes
            )
        }
    }

    suspend fun scanAudioHeaders(): List<RawMediaHeader> = withContext(Dispatchers.IO) {
        val headers = mutableListOf<RawMediaHeader>()
        val contentResolver: ContentResolver = context.contentResolver

        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projectionList = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projectionList.add(MediaStore.Audio.Media.ALBUM_ARTIST)
        }

        val projection = projectionList.toTypedArray()
        val selection = "(${MediaStore.Audio.Media.DURATION} >= 2000 OR ${MediaStore.Audio.Media.DURATION} IS NULL OR ${MediaStore.Audio.Media.DURATION} = 0)"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackNumberColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val albumArtistColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val filePath = if (dataColumn >= 0) cursor.getString(dataColumn) else null
                    val title = fallbackTitle(cursor.getString(titleColumn), filePath)
                    val artist = sanitizeTag(cursor.getString(artistColumn), "Unknown Artist")
                    val album = sanitizeTag(cursor.getString(albumColumn), "Unknown Album")
                    val albumId = cursor.getLong(albumIdColumn)
                    val duration = cursor.getLong(durationColumn).coerceAtLeast(0L)
                    val rawTrack = cursor.getInt(trackNumberColumn)
                    val (trackNumber, discNumber) = decodeMediaStoreTrackAndDisc(rawTrack)
                    val year = cursor.getInt(yearColumn).coerceAtLeast(0)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val dateModified = if (dateModifiedColumn >= 0) cursor.getLong(dateModifiedColumn) else 0L
                    val size = cursor.getLong(sizeColumn).coerceAtLeast(0L)
                    val mime = if (mimeColumn >= 0) cursor.getString(mimeColumn) ?: "audio/mpeg" else "audio/mpeg"
                    val rawAlbumArtist = if (albumArtistColumn >= 0) cursor.getString(albumArtistColumn) ?: "" else ""
                    val albumArtist = sanitizeTag(rawAlbumArtist, "")

                    headers.add(
                        RawMediaHeader(
                            mediaStoreId = id,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = albumId,
                            durationMs = duration,
                            trackNumber = trackNumber,
                            discNumber = discNumber,
                            year = year,
                            dateAdded = dateAdded,
                            dateModified = dateModified,
                            sizeBytes = size,
                            mimeType = mime,
                            filePath = filePath,
                            albumArtist = albumArtist
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        headers
    }

    fun groupIntoAlbums(tracks: List<Track>): List<Album> {
        return tracks.groupBy { track ->
            val artistKey = track.albumArtist.ifBlank { track.artist }.trim().lowercase()
            val albumKey = track.album.trim().lowercase()
            "$artistKey|$albumKey"
        }.map { (_, albumTracks) ->
            val firstTrack = albumTracks.first()
            val bestArtwork = albumTracks.firstOrNull { it.artworkUri != null }?.artworkUri
            val albumArtist = firstTrack.albumArtist.ifBlank { firstTrack.artist }
            Album(
                name = firstTrack.album,
                artist = albumArtist,
                artworkUri = bestArtwork,
                trackCount = albumTracks.size,
                year = albumTracks.maxOfOrNull { it.year } ?: 0
            )
        }.sortedBy { it.name.lowercase() }
    }

    fun groupIntoArtists(tracks: List<Track>): List<Artist> {
        return tracks.groupBy { it.artist.trim() }
            .map { (artistName, artistTracks) ->
                val uniqueAlbums = artistTracks.map { it.album.trim().lowercase() }.distinct().size
                Artist(
                    name = artistName,
                    trackCount = artistTracks.size,
                    albumCount = uniqueAlbums
                )
            }.sortedBy { it.name.lowercase() }
    }
}
