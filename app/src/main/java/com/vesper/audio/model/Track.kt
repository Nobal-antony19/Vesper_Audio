package com.vesper.audio.model

import android.net.Uri

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val artworkUri: Uri? = null,
    val trackNumber: Int = 0,
    val discNumber: Int = 1,
    val year: Int = 0,
    val genre: String = "",
    val albumArtist: String = "",
    val mimeType: String = "audio/mpeg",
    val dateAdded: Long = 0L,
    val sizeBytes: Long = 0L,
    val isFavorite: Boolean = false
) {
    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }
}

data class Album(
    val name: String,
    val artist: String,
    val artworkUri: Uri? = null,
    val trackCount: Int,
    val year: Int = 0
)

data class Artist(
    val name: String,
    val trackCount: Int,
    val albumCount: Int
)

data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val createdAt: Long
)
