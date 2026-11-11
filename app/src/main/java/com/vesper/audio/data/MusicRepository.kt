package com.vesper.audio.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.vesper.audio.data.local.CachedTrackEntity
import com.vesper.audio.data.local.ScannedFolderEntity
import com.vesper.audio.data.local.SonoraLocalRepository
import com.vesper.audio.data.local.toCachedEntity
import com.vesper.audio.data.local.toTrack
import com.vesper.audio.data.media.DemoAudioProvider
import com.vesper.audio.data.media.MediaStoreScanner
import com.vesper.audio.data.media.SafFolderScanner
import com.vesper.audio.model.Album
import com.vesper.audio.model.Artist
import com.vesper.audio.model.Playlist
import com.vesper.audio.model.Track
import com.vesper.audio.util.VesperArtworkResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class MusicRepository(
    private val context: Context,
    val localRepository: SonoraLocalRepository,
    private val appScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val mediaStoreScanner = MediaStoreScanner(context)
    private val safFolderScanner = SafFolderScanner(context)
    private val demoAudioProvider = DemoAudioProvider(context)

    private val _rawTracks = MutableStateFlow<List<Track>>(emptyList())
    private val _filterShortTracks = MutableStateFlow(true)
    val filterShortTracks: StateFlow<Boolean> = _filterShortTracks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncSummary = MutableStateFlow("Never")
    val lastSyncSummary: StateFlow<String> = _lastSyncSummary.asStateFlow()

    val scannedFolders: Flow<List<ScannedFolderEntity>> = localRepository.allScannedFolders

    // Base filtered tracks without favorite flag so album/artist grouping does NOT recompute on favorite toggles
    private val filteredBaseTracks: StateFlow<List<Track>> = combine(
        _rawTracks,
        _filterShortTracks
    ) { trackList, filterShort ->
        val minDuration = if (filterShort) 30_000L else 2_000L
        trackList.filter { it.durationMs == 0L || it.durationMs >= minDuration || it.id.startsWith("demo_") }
    }.flowOn(Dispatchers.Default)
    .stateIn(appScope, SharingStarted.Eagerly, emptyList())

    // Combined tracks with real-time Room favorites state
    val tracks: StateFlow<List<Track>> = combine(
        filteredBaseTracks,
        localRepository.allFavoriteIds
    ) { baseList, favIds ->
        val favSet = favIds.toSet()
        baseList.map { it.copy(isFavorite = it.id in favSet) }
    }.flowOn(Dispatchers.Default)
    .stateIn(appScope, SharingStarted.Eagerly, emptyList())

    val albums: StateFlow<List<Album>> = filteredBaseTracks.map { trackList ->
        mediaStoreScanner.groupIntoAlbums(trackList)
    }.flowOn(Dispatchers.Default)
    .stateIn(appScope, SharingStarted.Eagerly, emptyList())

    val artists: StateFlow<List<Artist>> = filteredBaseTracks.map { trackList ->
        mediaStoreScanner.groupIntoArtists(trackList)
    }.flowOn(Dispatchers.Default)
    .stateIn(appScope, SharingStarted.Eagerly, emptyList())

    val favoriteTracks: StateFlow<List<Track>> = combine(
        tracks,
        localRepository.allFavoriteIds
    ) { allTracksList, favIds ->
        val trackMap = allTracksList.associateBy { it.id }
        favIds.mapNotNull { trackMap[it] }
    }.flowOn(Dispatchers.Default)
    .stateIn(appScope, SharingStarted.Eagerly, emptyList())

    val recentlyAddedTracks: StateFlow<List<Track>> = tracks.map { trackList ->
        sortRecentlyAdded(trackList)
    }.flowOn(Dispatchers.Default)
    .stateIn(appScope, SharingStarted.Eagerly, emptyList())

    companion object {
        /**
         * Authoritative deterministic sort for Recently Added:
         * primary sort by dateAdded descending, tie-broken by numeric/string ID descending and title ascending.
         */
        fun sortRecentlyAdded(trackList: List<Track>): List<Track> {
            return trackList.sortedWith(
                compareByDescending<Track> { it.dateAdded }
                    .thenByDescending { it.id.toLongOrNull() ?: 0L }
                    .thenBy { it.title.lowercase() }
                    .thenBy { it.id }
            )
        }

        /**
         * Determines whether a cached track entity has incomplete metadata or unverified generic artwork
         * and should be queued for background selective enrichment (Phase B).
         * Respects user overrides so manual edits are never clobbered.
         */
        fun isMetadataIncomplete(entity: CachedTrackEntity): Boolean {
            if (entity.isUserOverride) {
                return entity.artworkUriString.isNullOrBlank() ||
                    entity.artworkUriString.startsWith("content://media/external/audio/albumart") ||
                    entity.durationMs <= 0L
            }
            val missingTitle = entity.title.isBlank() ||
                entity.title.equals("Unknown Track", ignoreCase = true) ||
                entity.title.contains("<unknown>", ignoreCase = true)
            val missingArtist = entity.artist.isBlank() ||
                entity.artist.equals("Unknown Artist", ignoreCase = true) ||
                entity.artist.contains("<unknown>", ignoreCase = true)
            val missingAlbum = entity.album.isBlank() ||
                entity.album.equals("Unknown Album", ignoreCase = true) ||
                entity.album.contains("<unknown>", ignoreCase = true)
            val missingGenre = entity.genre.isBlank()
            val missingYear = entity.year <= 0
            val missingDuration = entity.durationMs <= 0L
            val unverifiedArt = entity.artworkUriString.isNullOrBlank() ||
                entity.artworkUriString.startsWith("content://media/external/audio/albumart")

            return missingTitle || missingArtist || missingAlbum || missingGenre || missingYear || missingDuration || unverifiedArt
        }

        internal fun parseSubIndex(raw: String?): Int {
            if (raw.isNullOrBlank()) return 0
            val firstPart = raw.trim().substringBefore('/').trim()
            return firstPart.toIntOrNull()?.coerceAtLeast(0) ?: 0
        }

        internal fun parseYearTag(raw: String?): Int {
            if (raw.isNullOrBlank()) return 0
            val match = Regex("""\b(19\d{2}|20\d{2})\b""").find(raw)
            return match?.value?.toIntOrNull() ?: raw.trim().take(4).toIntOrNull() ?: 0
        }

        /**
         * Safely merges embedded ID3/Vorbis/MP4 metadata into a CachedTrackEntity without overwriting
         * user-edited fields or replacing valid MediaStore values with blank/<unknown> strings.
         */
        fun mergeEmbeddedTags(
            existing: CachedTrackEntity,
            extractedTitle: String? = null,
            extractedArtist: String? = null,
            extractedAlbum: String? = null,
            extractedAlbumArtist: String? = null,
            extractedGenre: String? = null,
            extractedYear: Int = 0,
            extractedTrackNumber: Int = 0,
            extractedDiscNumber: Int = 0,
            extractedDurationMs: Long = 0L,
            resolvedArtworkUriString: String? = null
        ): CachedTrackEntity {
            val newArt = resolvedArtworkUriString?.takeIf { it.isNotBlank() } ?: existing.artworkUriString
            val newDuration = if (existing.durationMs <= 0L && extractedDurationMs > 0L) extractedDurationMs else existing.durationMs

            if (existing.isUserOverride) {
                return existing.copy(
                    durationMs = newDuration,
                    artworkUriString = newArt
                )
            }

            fun validTag(value: String?): String? {
                val trimmed = value?.trim().orEmpty()
                return if (trimmed.isNotBlank() &&
                    !trimmed.equals("<unknown>", ignoreCase = true) &&
                    !trimmed.contains("<unknown>", ignoreCase = true)
                ) trimmed else null
            }

            val updatedTitle = if (existing.title.isBlank() || existing.title == "Unknown Track" || existing.title.contains("<unknown>", true)) {
                validTag(extractedTitle) ?: existing.title
            } else existing.title

            val updatedArtist = if (existing.artist.isBlank() || existing.artist == "Unknown Artist" || existing.artist.contains("<unknown>", true)) {
                validTag(extractedArtist) ?: validTag(extractedAlbumArtist) ?: existing.artist
            } else existing.artist

            val updatedAlbum = if (existing.album.isBlank() || existing.album == "Unknown Album" || existing.album.contains("<unknown>", true)) {
                validTag(extractedAlbum) ?: existing.album
            } else existing.album

            val updatedAlbumArtist = if (
                existing.albumArtist.isBlank() ||
                existing.albumArtist.equals("Unknown Artist", ignoreCase = true) ||
                existing.albumArtist.contains("<unknown>", true)
            ) {
                validTag(extractedAlbumArtist) ?: validTag(extractedArtist) ?: existing.albumArtist
            } else existing.albumArtist

            val updatedGenre = if (existing.genre.isBlank()) {
                validTag(extractedGenre) ?: existing.genre
            } else existing.genre

            val updatedYear = if (existing.year <= 0 && extractedYear > 0) extractedYear else existing.year
            val updatedTrackNumber = if (existing.trackNumber <= 0 && extractedTrackNumber > 0) extractedTrackNumber else existing.trackNumber
            val updatedDiscNumber = if (existing.discNumber <= 1 && extractedDiscNumber > 1) extractedDiscNumber else existing.discNumber

            return existing.copy(
                title = updatedTitle,
                artist = updatedArtist,
                album = updatedAlbum,
                albumArtist = updatedAlbumArtist,
                genre = updatedGenre,
                year = updatedYear,
                trackNumber = updatedTrackNumber,
                discNumber = updatedDiscNumber,
                durationMs = newDuration,
                artworkUriString = newArt
            )
        }
    }

    val playlists: Flow<List<Playlist>> = localRepository.playlistsWithCount.map { entities ->
        entities.map { entity ->
            Playlist(
                id = entity.id,
                name = entity.name,
                trackCount = entity.trackCount,
                createdAt = entity.createdAt
            )
        }
    }

    private val scanMutex = Mutex()
    private var enrichmentJob: Job? = null
    private val enrichmentSemaphore = Semaphore(3) // Bounded 3-way concurrency for Phase B metadata/artwork enrichment

    init {
        // Fast startup: Observe persistent cached tracks from Room continuously
        appScope.launch {
            localRepository.allCachedTracks.collect { cached ->
                _rawTracks.value = cached.map { it.toTrack() }
                if (cached.isNotEmpty()) {
                    _isLoading.value = false
                }
            }
        }
        // Run incremental background sync
        syncLibraryIncremental(forceFullRescan = false, reason = "startup_check")
    }

    fun setFilterShortTracks(enabled: Boolean) {
        _filterShortTracks.value = enabled
    }

    fun refreshLibrary() {
        syncLibraryIncremental(forceFullRescan = true, reason = "user_manual_rescan")
    }

    fun syncLibraryIncremental(forceFullRescan: Boolean = false, reason: String = "background_sync") {
        appScope.launch {
            if (!scanMutex.tryLock()) {
                Log.d("VesperScan", "Scan already in progress, skipping $reason")
                return@launch
            }
            enrichmentJob?.cancel()
            _isSyncing.value = true
            if (_rawTracks.value.isEmpty()) {
                _isLoading.value = true
            }
            val startTimeMs = System.currentTimeMillis()
            var queriedCount = 0
            var unchangedCount = 0
            var newCount = 0
            var updatedCount = 0
            var missingCount = 0
            var extractedCount = 0

            try {
                val cachedTracks = localRepository.getAllCachedTracksSync()
                val cachedById = cachedTracks.associateBy { it.id }
                val cachedByMediaStoreId = cachedTracks.filter { it.mediaStoreId > 0 }.associateBy { it.mediaStoreId }

                // --- PHASE A: Fast MediaStore Header Inventory (0 MediaMetadataRetriever calls) ---
                val mediaStoreHeaders = mediaStoreScanner.scanAudioHeaders()
                queriedCount = mediaStoreHeaders.size

                val seenIds = mutableSetOf<Long>()
                val toUpsert = mutableListOf<CachedTrackEntity>()
                val headersNeedingEnrichment = mutableListOf<MediaStoreScanner.RawMediaHeader>()

                for (header in mediaStoreHeaders) {
                    seenIds.add(header.mediaStoreId)
                    val existing = cachedByMediaStoreId[header.mediaStoreId]

                    if (!forceFullRescan && existing != null && existing.dateModified == header.dateModified && existing.sizeBytes == header.sizeBytes) {
                        unchangedCount++
                        // Recover any previously cached records that still have incomplete tags or unverified artwork
                        if (isMetadataIncomplete(existing)) {
                            headersNeedingEnrichment.add(header)
                        }
                    } else {
                        val fastTrack = header.toFastTrack(context)
                        var entity = fastTrack.toCachedEntity(
                            mediaStoreId = header.mediaStoreId,
                            dateModified = header.dateModified,
                            isUserOverride = existing?.isUserOverride ?: false
                        )
                        // Preserve manual user metadata overrides and already-enriched fields across rescans
                        if (existing != null) {
                            if (existing.isUserOverride) {
                                entity = entity.copy(
                                    title = existing.title,
                                    artist = existing.artist,
                                    album = existing.album,
                                    albumArtist = existing.albumArtist,
                                    year = existing.year,
                                    genre = existing.genre,
                                    trackNumber = existing.trackNumber,
                                    discNumber = existing.discNumber,
                                    isUserOverride = true
                                )
                            } else {
                                // Keep previously enriched non-blank fields if MediaStore header lacks them
                                entity = mergeEmbeddedTags(
                                    existing = entity,
                                    extractedTitle = existing.title,
                                    extractedArtist = existing.artist,
                                    extractedAlbum = existing.album,
                                    extractedAlbumArtist = existing.albumArtist,
                                    extractedGenre = existing.genre,
                                    extractedYear = existing.year,
                                    extractedTrackNumber = existing.trackNumber,
                                    extractedDiscNumber = existing.discNumber,
                                    extractedDurationMs = existing.durationMs,
                                    resolvedArtworkUriString = existing.artworkUriString?.takeIf {
                                        !it.startsWith("content://media/external/audio/albumart")
                                    }
                                )
                            }
                        }
                        toUpsert.add(entity)
                        headersNeedingEnrichment.add(header)
                        if (existing == null) {
                            newCount++
                        } else {
                            updatedCount++
                        }
                    }
                }

                // Conservative safety check: if queriedCount == 0 and we have cached tracks, do not wipe library
                val missingIds = if (queriedCount > 0) {
                    cachedTracks.filter { it.mediaStoreId > 0 && it.mediaStoreId !in seenIds }.map { it.id }
                } else {
                    emptyList()
                }
                missingCount = missingIds.size

                // Flush MediaStore fast inventory immediately in chunks of 500 so UI renders right away
                if (toUpsert.isNotEmpty()) {
                    for (chunk in toUpsert.chunked(500)) {
                        localRepository.insertCachedTracks(chunk)
                        _isLoading.value = false
                    }
                    toUpsert.clear()
                }

                // --- Incremental SAF Folder Scan (Header Inventory + Selective Extraction) ---
                val persistedFolders = localRepository.getAllScannedFoldersSync()
                val safHeadersToExtract = mutableListOf<com.vesper.audio.data.media.SafDocumentHeader>()
                for (folder in persistedFolders) {
                    try {
                        val treeUri = Uri.parse(folder.uriString)
                        val safHeaders = safFolderScanner.scanDirectoryHeaders(treeUri)
                        queriedCount += safHeaders.size
                        for (sh in safHeaders) {
                            val existingSaf = cachedById[sh.stableTrackId] ?: cachedById[sh.documentUri.toString()]
                            val isUnchanged = !forceFullRescan &&
                                existingSaf != null &&
                                existingSaf.id == sh.stableTrackId &&
                                (sh.lastModified <= 0L || existingSaf.dateModified == sh.lastModified) &&
                                (sh.sizeBytes <= 0L || existingSaf.sizeBytes == sh.sizeBytes)

                            if (isUnchanged && existingSaf != null && !isMetadataIncomplete(existingSaf)) {
                                unchangedCount++
                            } else {
                                safHeadersToExtract.add(sh)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("VesperScan", "Failed scanning SAF folder ${folder.uriString}: ${e.message}")
                    }
                }

                if (safHeadersToExtract.isNotEmpty()) {
                    coroutineScope {
                        val extractedSafEntities = safHeadersToExtract.map { sh ->
                            async(Dispatchers.IO) {
                                enrichmentSemaphore.withPermit {
                                    val st = safFolderScanner.extractTrackFromHeader(sh) ?: return@withPermit null
                                    val existingSaf = cachedById[sh.stableTrackId] ?: cachedById[sh.documentUri.toString()]
                                    var entity = st.toCachedEntity(
                                        mediaStoreId = 0L,
                                        dateModified = sh.lastModified,
                                        isUserOverride = existingSaf?.isUserOverride ?: false
                                    )
                                    if (existingSaf != null && existingSaf.isUserOverride) {
                                        entity = entity.copy(
                                            title = existingSaf.title,
                                            artist = existingSaf.artist,
                                            album = existingSaf.album,
                                            albumArtist = existingSaf.albumArtist,
                                            year = existingSaf.year,
                                            genre = existingSaf.genre,
                                            trackNumber = existingSaf.trackNumber,
                                            discNumber = existingSaf.discNumber,
                                            isUserOverride = true
                                        )
                                    }
                                    entity
                                }
                            }
                        }.awaitAll().filterNotNull()

                        extractedCount += extractedSafEntities.size
                        newCount += extractedSafEntities.size
                        for (chunk in extractedSafEntities.chunked(200)) {
                            localRepository.insertCachedTracks(chunk)
                            _isLoading.value = false
                        }
                    }
                }

                // Only delete missing MediaStore rows if safe (never delete SAF rows automatically)
                if (missingIds.isNotEmpty()) {
                    if (forceFullRescan || (cachedTracks.isNotEmpty() && missingIds.size.toDouble() / cachedTracks.size <= 0.4)) {
                        localRepository.deleteCachedTracksByIds(missingIds)
                    } else {
                        Log.w("VesperScan", "Suspicious empty or partial scan: ${missingIds.size} missing out of ${cachedTracks.size}. Skipping deletion.")
                    }
                }

                val durationMs = System.currentTimeMillis() - startTimeMs
                val summary = "Last synced: ${java.text.SimpleDateFormat("HH:mm, dd MMM", java.util.Locale.getDefault()).format(java.util.Date())} ($queriedCount found, $unchangedCount unchanged)"
                _lastSyncSummary.value = summary
                Log.i(
                    "VesperScan",
                    "PhaseA complete: queried=$queriedCount / unchanged=$unchangedCount / new=$newCount / updated=$updatedCount / missing=$missingCount / safExtracted=$extractedCount / ms=$durationMs / reason=$reason"
                )

                // --- PHASE B: Non-blocking Background Selective Enrichment ---
                if (headersNeedingEnrichment.isNotEmpty()) {
                    startSelectiveEnrichment(headersNeedingEnrichment)
                }
            } catch (e: Exception) {
                Log.e("VesperScan", "Scan failed: ${e.message}, preserving cached tracks.")
            } finally {
                _isLoading.value = false
                _isSyncing.value = false
                scanMutex.unlock()
            }
        }
    }

    /**
     * Phase B: Background selective metadata and per-track embedded artwork enrichment.
     * Runs on Dispatchers.IO with bounded 3-way concurrency without blocking UI or startup.
     */
    private fun startSelectiveEnrichment(headers: List<MediaStoreScanner.RawMediaHeader>) {
        enrichmentJob?.cancel()
        enrichmentJob = appScope.launch(Dispatchers.IO) {
            try {
                for (batch in headers.chunked(40)) {
                    if (!isActive) break
                    val currentMap = localRepository.getAllCachedTracksSync().associateBy { it.id }
                    val enrichedBatch = coroutineScope {
                        batch.map { header ->
                            async(Dispatchers.IO) {
                                if (!isActive) return@async null
                                enrichmentSemaphore.withPermit {
                                    val existing = currentMap[header.mediaStoreId.toString()] ?: return@withPermit null
                                    val contentUri = Uri.parse(existing.contentUriString)

                                    // Resolve per-track embedded ID3 artwork so tracks sharing a generic albumId don't collide
                                    val resolvedArt = VesperArtworkResolver.resolveArtworkUri(
                                        context = context,
                                        audioUri = contentUri,
                                        albumId = header.albumId,
                                        trackId = existing.id,
                                        dateModified = header.dateModified,
                                        sizeBytes = header.sizeBytes
                                    )

                                    var extTitle: String? = null
                                    var extArtist: String? = null
                                    var extAlbum: String? = null
                                    var extAlbumArtist: String? = null
                                    var extGenre: String? = null
                                    var extYear = 0
                                    var extTrackNum = 0
                                    var extDiscNum = 0
                                    var extDurationMs = 0L

                                    val needsTagEnrichment = !existing.isUserOverride && (
                                        existing.title == "Unknown Track" ||
                                        existing.artist == "Unknown Artist" ||
                                        existing.album == "Unknown Album" ||
                                        existing.albumArtist.isBlank() ||
                                        existing.genre.isBlank() ||
                                        existing.year <= 0 ||
                                        existing.trackNumber <= 0 ||
                                        existing.durationMs <= 0L
                                    )

                                    if (needsTagEnrichment) {
                                        val retriever = MediaMetadataRetriever()
                                        try {
                                            retriever.setDataSource(context, contentUri)
                                            extTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                                            extArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                                                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
                                            extAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                                            extAlbumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                                            extGenre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                                            extYear = parseYearTag(
                                                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                                                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                                            )
                                            extTrackNum = parseSubIndex(
                                                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                                            )
                                            extDiscNum = parseSubIndex(
                                                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
                                            )
                                            extDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                                ?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                                        } catch (_: Exception) {
                                        } finally {
                                            try {
                                                retriever.release()
                                            } catch (_: Exception) {
                                            }
                                        }
                                    }

                                    val merged = mergeEmbeddedTags(
                                        existing = existing,
                                        extractedTitle = extTitle,
                                        extractedArtist = extArtist,
                                        extractedAlbum = extAlbum,
                                        extractedAlbumArtist = extAlbumArtist,
                                        extractedGenre = extGenre,
                                        extractedYear = extYear,
                                        extractedTrackNumber = extTrackNum,
                                        extractedDiscNumber = extDiscNum,
                                        extractedDurationMs = extDurationMs,
                                        resolvedArtworkUriString = resolvedArt?.toString()
                                    )

                                    if (merged != existing) merged else null
                                }
                            }
                        }.awaitAll().filterNotNull()
                    }

                    if (enrichedBatch.isNotEmpty() && isActive) {
                        localRepository.insertCachedTracks(enrichedBatch)
                    }
                }
            } catch (e: Exception) {
                Log.d("VesperScan", "Selective enrichment stopped or finished: ${e.message}")
            }
        }
    }

    fun importSafFolder(treeUri: Uri, displayName: String = "Custom Music Folder") {
        appScope.launch {
            _isLoading.value = true
            try {
                localRepository.addScannedFolder(treeUri.toString(), displayName)
                syncLibraryIncremental(forceFullRescan = false, reason = "saf_folder_imported")
            } catch (e: Exception) {
                Log.e("VesperScan", "Failed importing SAF folder: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    fun removeSafFolder(uriString: String) {
        appScope.launch {
            localRepository.removeScannedFolder(uriString)
            refreshLibrary()
        }
    }

    fun fixMissingArtwork() {
        appScope.launch {
            _isSyncing.value = true
            try {
                val cached = localRepository.getAllCachedTracksSync()
                val tracksToFix = cached.filter {
                    it.artworkUriString.isNullOrBlank() || it.artworkUriString.startsWith("content://media/external/audio/albumart")
                }
                for (batch in tracksToFix.chunked(40)) {
                    val updatedEntities = coroutineScope {
                        batch.map { entity ->
                            async(Dispatchers.IO) {
                                enrichmentSemaphore.withPermit {
                                    val contentUri = Uri.parse(entity.contentUriString)
                                    val resolvedArt = VesperArtworkResolver.resolveArtworkUri(
                                        context = context,
                                        audioUri = contentUri,
                                        albumId = entity.mediaStoreId,
                                        trackId = entity.id,
                                        dateModified = entity.dateModified,
                                        sizeBytes = entity.sizeBytes
                                    )
                                    if (resolvedArt != null && resolvedArt.toString() != entity.artworkUriString) {
                                        entity.copy(artworkUriString = resolvedArt.toString())
                                    } else {
                                        null
                                    }
                                }
                            }
                        }.awaitAll().filterNotNull()
                    }
                    if (updatedEntities.isNotEmpty()) {
                        localRepository.insertCachedTracks(updatedEntities)
                    }
                }
            } catch (e: Exception) {
                Log.w("VesperScan", "Error fixing missing artwork: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Explicit user action from Settings only; never invoked automatically.
     */
    fun forceLoadDemoTracks() {
        appScope.launch {
            _isLoading.value = true
            try {
                val demos = demoAudioProvider.getOrGenerateDemoTracks()
                val current = _rawTracks.value.toMutableList()
                val seenKeys = current.map { "${it.title.trim().lowercase()}|${it.artist.trim().lowercase()}|${it.durationMs / 1000}" }.toMutableSet()

                for (d in demos) {
                    val key = "${d.title.trim().lowercase()}|${d.artist.trim().lowercase()}|${d.durationMs / 1000}"
                    if (key !in seenKeys) {
                        seenKeys.add(key)
                        current.add(d)
                    }
                }
                _rawTracks.value = current
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun toggleFavorite(track: Track) {
        localRepository.setFavorite(track.id, !track.isFavorite)
    }

    suspend fun createPlaylist(name: String): Long {
        return localRepository.createPlaylist(name)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: String) {
        localRepository.addTrackToPlaylist(playlistId, trackId)
    }

    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> = combine(
        tracks,
        localRepository.getPlaylistTrackIds(playlistId)
    ) { allTracks, ids ->
        val trackMap = allTracks.associateBy { it.id }
        ids.mapNotNull { trackMap[it] }
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        localRepository.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        localRepository.renamePlaylist(playlistId, newName)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        localRepository.deletePlaylist(playlistId)
    }

    suspend fun deleteTrack(track: Track): Boolean {
        var fileDeleted = false
        try {
            val trackPath = track.contentUri.path.orEmpty()
            if (trackPath.isNotBlank()) {
                val f = java.io.File(trackPath)
                if (f.exists() && f.canWrite()) {
                    fileDeleted = f.delete()
                }
            }
            if (!fileDeleted && track.contentUri.scheme == "content") {
                val rows = context.contentResolver.delete(track.contentUri, null, null)
                fileDeleted = rows > 0
            }
        } catch (e: Exception) {
            Log.w("MusicRepository", "Error deleting track file: ${e.message}")
        }

        localRepository.setFavorite(track.id, false)
        localRepository.deleteCachedTracksByIds(listOf(track.id))
        val current = _rawTracks.value.toMutableList()
        current.removeAll { it.id == track.id }
        _rawTracks.value = current
        return true
    }

    suspend fun batchDeleteTracks(tracksToDelete: List<Track>) {
        tracksToDelete.forEach { deleteTrack(it) }
    }

    suspend fun batchAddToFavorites(tracksToAdd: List<Track>) {
        localRepository.addFavorites(tracksToAdd.map { it.id })
    }

    suspend fun batchAddToPlaylist(playlistId: Long, tracksToAdd: List<Track>) {
        localRepository.addTracksToPlaylist(playlistId, tracksToAdd.map { it.id })
    }

    fun updateTrackMetadata(
        trackId: String,
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        newYear: Int = 0,
        newTrackNumber: Int = 0,
        newGenre: String? = null
    ) {
        val current = _rawTracks.value.toMutableList()
        val index = current.indexOfFirst { it.id == trackId }
        if (index >= 0) {
            val old = current[index]
            val updated = old.copy(
                title = newTitle,
                artist = newArtist,
                album = newAlbum,
                year = if (newYear > 0) newYear else old.year,
                trackNumber = if (newTrackNumber > 0) newTrackNumber else old.trackNumber,
                genre = newGenre?.takeIf { it.isNotBlank() } ?: old.genre
            )
            current[index] = updated
            _rawTracks.value = current
            appScope.launch {
                localRepository.updateCachedTrackMetadata(
                    id = trackId,
                    title = updated.title,
                    artist = updated.artist,
                    album = updated.album,
                    year = updated.year,
                    genre = updated.genre,
                    trackNumber = updated.trackNumber
                )
            }
        }
    }

    fun batchUpdateMetadata(
        trackIds: Set<String>,
        newArtist: String? = null,
        newAlbum: String? = null,
        newYear: Int? = null
    ) {
        val current = _rawTracks.value.toMutableList()
        val updatedTracks = mutableListOf<Track>()
        for (i in current.indices) {
            val track = current[i]
            if (track.id in trackIds) {
                val updated = track.copy(
                    artist = newArtist?.takeIf { it.isNotBlank() } ?: track.artist,
                    album = newAlbum?.takeIf { it.isNotBlank() } ?: track.album,
                    year = if (newYear != null && newYear > 0) newYear else track.year
                )
                current[i] = updated
                updatedTracks.add(updated)
            }
        }
        if (updatedTracks.isNotEmpty()) {
            _rawTracks.value = current
            appScope.launch {
                for (ut in updatedTracks) {
                    localRepository.updateCachedTrackMetadata(
                        id = ut.id,
                        title = ut.title,
                        artist = ut.artist,
                        album = ut.album,
                        year = ut.year,
                        genre = ut.genre,
                        trackNumber = ut.trackNumber
                    )
                }
            }
        }
    }
}
