package com.vesper.audio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SonoraDao {

    // --- Favorites ---
    @Query("SELECT trackId FROM favorites ORDER BY addedAt ASC")
    fun getAllFavoriteIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackId = :trackId)")
    fun isFavorite(trackId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteTrackEntity)

    @Query("DELETE FROM favorites WHERE trackId = :trackId")
    suspend fun removeFavorite(trackId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorites(favorites: List<FavoriteTrackEntity>)

    @Query("DELETE FROM favorites WHERE trackId IN (:trackIds)")
    suspend fun removeFavorites(trackIds: List<String>)

    // --- Playlists ---
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT p.id, p.name, p.createdAt, COUNT(pt.trackId) as trackCount FROM playlists p LEFT JOIN playlist_tracks pt ON p.id = pt.playlistId GROUP BY p.id ORDER BY p.createdAt DESC")
    fun getPlaylistsWithCount(): Flow<List<PlaylistWithCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String)

    // --- Playlist Tracks ---
    @Query("SELECT trackId FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY sortOrder ASC")
    fun getTrackIdsForPlaylist(playlistId: Long): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(crossRef: PlaylistTrackCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTracksToPlaylist(crossRefs: List<PlaylistTrackCrossRef>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String)

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    fun getPlaylistTrackCount(playlistId: Long): Flow<Int>

    // --- Custom EQ Presets ---
    @Query("SELECT * FROM eq_presets ORDER BY name ASC")
    fun getAllEqPresets(): Flow<List<CustomEqPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEqPreset(preset: CustomEqPresetEntity)

    @Query("DELETE FROM eq_presets WHERE name = :name")
    suspend fun deleteEqPreset(name: String)

    // --- Scanned SAF Folders ---
    @Query("SELECT * FROM scanned_folders ORDER BY addedAt DESC")
    fun getAllScannedFolders(): Flow<List<ScannedFolderEntity>>

    @Query("SELECT * FROM scanned_folders ORDER BY addedAt DESC")
    suspend fun getAllScannedFoldersSync(): List<ScannedFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScannedFolder(folder: ScannedFolderEntity)

    @Query("DELETE FROM scanned_folders WHERE uriString = :uriString")
    suspend fun deleteScannedFolder(uriString: String)

    // --- Cached Library Tracks ---
    @Query("SELECT * FROM cached_tracks ORDER BY title ASC")
    fun getAllCachedTracks(): Flow<List<CachedTrackEntity>>

    @Query("SELECT * FROM cached_tracks ORDER BY title ASC")
    suspend fun getAllCachedTracksSync(): List<CachedTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedTracks(tracks: List<CachedTrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedTrack(track: CachedTrackEntity)

    @Query("DELETE FROM cached_tracks WHERE id IN (:ids)")
    suspend fun deleteCachedTracksByIds(ids: List<String>)

    @Query("DELETE FROM cached_tracks")
    suspend fun clearCachedTracks()

    @Query("UPDATE cached_tracks SET title = :title, artist = :artist, album = :album, year = :year, genre = :genre, trackNumber = :trackNumber, isUserOverride = 1 WHERE id = :id")
    suspend fun updateCachedTrackMetadata(id: String, title: String, artist: String, album: String, year: Int, genre: String, trackNumber: Int = 0)

    // --- Bounded Discover Provider Cache ---
    @Query("SELECT * FROM discover_cache WHERE providerId = :providerId AND cacheKey = :cacheKey ORDER BY sortIndex ASC")
    suspend fun getDiscoverCache(providerId: String, cacheKey: String): List<DiscoverCachedTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscoverCache(items: List<DiscoverCachedTrackEntity>)

    @Query("DELETE FROM discover_cache WHERE providerId = :providerId AND cacheKey = :cacheKey")
    suspend fun clearDiscoverCacheByKey(providerId: String, cacheKey: String)

    @Query("DELETE FROM discover_cache WHERE expiresAt < :nowMs")
    suspend fun deleteExpiredDiscoverCache(nowMs: Long)

    @Query("SELECT COUNT(*) FROM discover_cache")
    suspend fun getDiscoverCacheCount(): Int

    @Query("DELETE FROM discover_cache WHERE rowid IN (SELECT rowid FROM discover_cache ORDER BY fetchedAt DESC LIMIT -1 OFFSET :maxEntries)")
    suspend fun pruneDiscoverCache(maxEntries: Int = 300)

    @Query("DELETE FROM discover_cache")
    suspend fun clearAllDiscoverCache()
}

