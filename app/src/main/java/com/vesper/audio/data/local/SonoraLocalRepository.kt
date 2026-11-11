package com.vesper.audio.data.local

import kotlinx.coroutines.flow.Flow

class SonoraLocalRepository(private val dao: SonoraDao) {

    val allFavoriteIds: Flow<List<String>> = dao.getAllFavoriteIds()
    val allPlaylists: Flow<List<PlaylistEntity>> = dao.getAllPlaylists()
    val playlistsWithCount: Flow<List<PlaylistWithCount>> = dao.getPlaylistsWithCount()
    val customEqPresets: Flow<List<CustomEqPresetEntity>> = dao.getAllEqPresets()
    val allScannedFolders: Flow<List<ScannedFolderEntity>> = dao.getAllScannedFolders()

    fun isFavorite(trackId: String): Flow<Boolean> = dao.isFavorite(trackId)

    suspend fun setFavorite(trackId: String, isFav: Boolean) {
        if (isFav) {
            dao.addFavorite(FavoriteTrackEntity(trackId = trackId))
        } else {
            dao.removeFavorite(trackId)
        }
    }

    suspend fun addFavorites(trackIds: List<String>) {
        val now = System.currentTimeMillis()
        val entities = trackIds.map { FavoriteTrackEntity(trackId = it, addedAt = now) }
        dao.addFavorites(entities)
    }

    suspend fun removeFavorites(trackIds: List<String>) {
        dao.removeFavorites(trackIds)
    }

    suspend fun createPlaylist(name: String): Long {
        return dao.createPlaylist(PlaylistEntity(name = name))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        dao.renamePlaylist(playlistId, newName)
    }

    fun getPlaylistTrackIds(playlistId: Long): Flow<List<String>> {
        return dao.getTrackIdsForPlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: String, sortOrder: Int = 0) {
        dao.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId, trackId, sortOrder))
    }

    suspend fun addTracksToPlaylist(playlistId: Long, trackIds: List<String>) {
        val refs = trackIds.mapIndexed { idx, id -> PlaylistTrackCrossRef(playlistId, id, idx) }
        dao.addTracksToPlaylist(refs)
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        dao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun removeTracksFromPlaylist(playlistId: Long, trackIds: List<String>) {
        trackIds.forEach { dao.removeTrackFromPlaylist(playlistId, it) }
    }

    suspend fun saveEqPreset(name: String, bandLevelsCsv: String, bassBoost: Int = 0) {
        dao.saveEqPreset(CustomEqPresetEntity(name, bandLevelsCsv, bassBoost))
    }

    suspend fun renameEqPreset(oldName: String, newName: String, bandLevelsCsv: String, bassBoost: Int = 0) {
        if (oldName != newName) {
            dao.deleteEqPreset(oldName)
        }
        dao.saveEqPreset(CustomEqPresetEntity(newName, bandLevelsCsv, bassBoost))
    }

    suspend fun deleteEqPreset(name: String) {
        dao.deleteEqPreset(name)
    }

    suspend fun getAllScannedFoldersSync(): List<ScannedFolderEntity> {
        return dao.getAllScannedFoldersSync()
    }

    suspend fun addScannedFolder(uriString: String, displayName: String) {
        dao.insertScannedFolder(ScannedFolderEntity(uriString = uriString, displayName = displayName))
    }

    suspend fun removeScannedFolder(uriString: String) {
        dao.deleteScannedFolder(uriString)
    }

    suspend fun deleteScannedFolder(folder: ScannedFolderEntity) {
        dao.deleteScannedFolder(folder.uriString)
    }

    // --- Cached Library Tracks ---
    val allCachedTracks: Flow<List<CachedTrackEntity>> = dao.getAllCachedTracks()

    suspend fun getAllCachedTracksSync(): List<CachedTrackEntity> {
        return dao.getAllCachedTracksSync()
    }

    suspend fun insertCachedTracks(tracks: List<CachedTrackEntity>) {
        dao.insertCachedTracks(tracks)
    }

    suspend fun insertCachedTrack(track: CachedTrackEntity) {
        dao.insertCachedTrack(track)
    }

    suspend fun deleteCachedTracksByIds(ids: List<String>) {
        dao.deleteCachedTracksByIds(ids)
    }

    suspend fun clearCachedTracks() {
        dao.clearCachedTracks()
    }

    suspend fun updateCachedTrackMetadata(
        id: String,
        title: String,
        artist: String,
        album: String,
        year: Int,
        genre: String,
        trackNumber: Int = 0
    ) {
        dao.updateCachedTrackMetadata(id, title, artist, album, year, genre, trackNumber)
    }

    // --- Bounded Discover Provider Cache ---
    suspend fun getDiscoverCache(providerId: String, cacheKey: String): List<DiscoverCachedTrackEntity> {
        return dao.getDiscoverCache(providerId, cacheKey)
    }

    suspend fun saveDiscoverCache(
        providerId: String,
        cacheKey: String,
        items: List<DiscoverCachedTrackEntity>,
        replaceExistingKey: Boolean = true,
        maxEntries: Int = 300
    ) {
        if (replaceExistingKey) {
            dao.clearDiscoverCacheByKey(providerId, cacheKey)
        }
        dao.insertDiscoverCache(items)
        dao.pruneDiscoverCache(maxEntries)
    }

    suspend fun getDiscoverCacheCount(): Int {
        return dao.getDiscoverCacheCount()
    }

    suspend fun clearAllDiscoverCache() {
        dao.clearAllDiscoverCache()
    }
}

