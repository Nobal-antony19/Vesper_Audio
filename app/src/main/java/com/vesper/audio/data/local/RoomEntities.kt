package com.vesper.audio.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteTrackEntity(
    @PrimaryKey val trackId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val trackCount: Int
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    indices = [Index("playlistId"), Index("trackId")]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: String,
    val sortOrder: Int = 0
)

@Entity(tableName = "eq_presets")
data class CustomEqPresetEntity(
    @PrimaryKey val name: String,
    val bandLevelsCsv: String,
    val bassBoost: Int = 0
)

@Entity(tableName = "scanned_folders")
data class ScannedFolderEntity(
    @PrimaryKey val uriString: String,
    val displayName: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cached_tracks",
    indices = [
        Index("mediaStoreId"),
        Index("artist"),
        Index("album")
    ]
)
data class CachedTrackEntity(
    @PrimaryKey val id: String,
    val mediaStoreId: Long,
    val contentUriString: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val trackNumber: Int = 0,
    val discNumber: Int = 1,
    val year: Int = 0,
    val genre: String = "",
    val albumArtist: String = "",
    val artworkUriString: String? = null,
    val dateAdded: Long = 0L,
    val dateModified: Long = 0L,
    val sizeBytes: Long = 0L,
    val mimeType: String = "audio/mpeg",
    val isUserOverride: Boolean = false
)

fun CachedTrackEntity.toTrack(): com.vesper.audio.model.Track = com.vesper.audio.model.Track(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    contentUri = android.net.Uri.parse(contentUriString),
    artworkUri = artworkUriString?.let { android.net.Uri.parse(it) },
    trackNumber = trackNumber,
    discNumber = discNumber,
    year = year,
    genre = genre,
    albumArtist = albumArtist,
    mimeType = mimeType,
    dateAdded = dateAdded,
    sizeBytes = sizeBytes,
    isFavorite = false
)

fun com.vesper.audio.model.Track.toCachedEntity(
    mediaStoreId: Long = id.toLongOrNull() ?: 0L,
    dateModified: Long = 0L,
    isUserOverride: Boolean = false
): CachedTrackEntity = CachedTrackEntity(
    id = id,
    mediaStoreId = mediaStoreId,
    contentUriString = contentUri.toString(),
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    trackNumber = trackNumber,
    discNumber = discNumber,
    year = year,
    genre = genre,
    albumArtist = albumArtist,
    artworkUriString = artworkUri?.toString(),
    dateAdded = dateAdded,
    dateModified = dateModified,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    isUserOverride = isUserOverride
)

@Entity(
    tableName = "discover_cache",
    primaryKeys = ["providerId", "cacheKey", "providerTrackId"],
    indices = [
        Index(value = ["providerId", "cacheKey"]),
        Index(value = ["fetchedAt"])
    ]
)
data class DiscoverCachedTrackEntity(
    val providerId: String,
    val cacheKey: String,
    val providerTrackId: String,
    val namespacedId: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String? = null,
    val durationMs: Long = 0L,
    val genre: String = "",
    val releaseYear: Int = 0,
    val isExplicit: Boolean = false,
    val sortIndex: Int = 0,
    val fetchedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 30 * 60 * 1000L
)

