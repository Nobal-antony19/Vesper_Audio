package com.vesper.audio.data.remote

import com.vesper.audio.data.local.SonoraLocalRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.Collections

/**
 * Provider-neutral Discover Repository implementing:
 * - Bounded Room metadata caching (`discover_cache`, max 300 entries, 30-min TTL)
 * - Stale-while-revalidate (renders cached content immediately, retains cached content on network failure)
 * - Single-flight request deduplication for concurrent identical queries
 * - Bounded concurrency (`Semaphore(2)`)
 * - Broken artwork URL blacklisting so known-broken image URLs are not retried repeatedly
 */
class DiscoverRepository(
    private val localRepository: SonoraLocalRepository? = null,
    val audiusProvider: AudiusDiscoverProvider = AudiusDiscoverProvider(),
    val jioSaavnProvider: JioSaavnDiscoverProvider = JioSaavnDiscoverProvider(),
    val bitChordProvider: BitChordDiscoverProvider = BitChordDiscoverProvider(),
    private val cacheTtlMs: Long = DEFAULT_CACHE_TTL_MS,
    private val maxCacheEntries: Int = DEFAULT_MAX_CACHE_ENTRIES
) {
    companion object {
        const val DEFAULT_CACHE_TTL_MS = 30 * 60 * 1000L // 30 minutes
        const val DEFAULT_MAX_CACHE_ENTRIES = 300
    }

    val allProviders: List<DiscoverProvider> = listOf(
        audiusProvider,
        jioSaavnProvider,
        bitChordProvider
    )

    val enabledProviders: List<DiscoverProvider>
        get() = allProviders.filter { it.isEnabled && it.isAuthorized }

    private val requestSemaphore = Semaphore(2)
    private val inFlightMutex = Mutex()
    private val inFlightRequests = mutableMapOf<String, CompletableDeferred<DiscoverPageResult>>()

    private val brokenArtworkUrls = Collections.synchronizedSet( LinkedHashSet<String>() )

    fun markArtworkUrlBroken(url: String?) {
        if (url.isNullOrBlank()) return
        synchronized(brokenArtworkUrls) {
            if (brokenArtworkUrls.size >= 200) {
                val first = brokenArtworkUrls.iterator().next()
                brokenArtworkUrls.remove(first)
            }
            brokenArtworkUrls.add(url)
        }
    }

    fun isArtworkUrlBroken(url: String?): Boolean {
        if (url.isNullOrBlank()) return true
        return url in brokenArtworkUrls
    }

    fun getBrokenArtworkUrlsSnapshot(): Set<String> {
        synchronized(brokenArtworkUrls) {
            return brokenArtworkUrls.toSet()
        }
    }

    fun getProvider(providerId: String): DiscoverProvider {
        return enabledProviders.firstOrNull { it.providerId.equals(providerId, ignoreCase = true) }
            ?: audiusProvider
    }

    /**
     * Immediately reads any cached trending tracks from Room (whether fresh or stale)
     * without waiting for a network round-trip, enabling instant UI rendering while
     * a background refresh updates the list.
     */
    suspend fun peekCachedTrending(
        providerId: String = "audius",
        nowMs: Long = System.currentTimeMillis()
    ): DiscoverPageResult? = withContext(Dispatchers.IO) {
        if (localRepository == null) return@withContext null
        val provider = getProvider(providerId)
        val cachedRows = try {
            localRepository.getDiscoverCache(provider.providerId, "trending")
        } catch (_: Exception) {
            emptyList()
        }
        if (cachedRows.isEmpty()) return@withContext null
        val isFresh = cachedRows.first().expiresAt > nowMs
        val tracks = cachedRows.map {
            it.toDiscoverTrack(provider.displayName, provider::buildStreamEndpoint)
        }
        DiscoverPageResult(
            tracks = sanitizeTracks(tracks),
            isFromCache = true,
            isStale = !isFresh,
            cacheTimestampMs = cachedRows.first().fetchedAt,
            errorMessage = null,
            latencyMs = 1L
        )
    }

    /**
     * Fetches trending tracks with Room Stale-While-Revalidate and Single-Flight deduplication.
     */
    suspend fun getTrending(
        providerId: String = "audius",
        limit: Int = 50,
        offset: Int = 0,
        forceRefresh: Boolean = false,
        nowMs: Long = System.currentTimeMillis()
    ): DiscoverPageResult = withContext(Dispatchers.IO) {
        val provider = getProvider(providerId)
        val cacheKey = "trending"

        // 1. Check Room cache for initial page (offset == 0) when not forcing refresh
        val cachedRows = if (offset == 0 && localRepository != null) {
            try {
                localRepository.getDiscoverCache(provider.providerId, cacheKey)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        if (!forceRefresh && offset == 0 && cachedRows.isNotEmpty()) {
            val isFresh = cachedRows.first().expiresAt > nowMs
            if (isFresh) {
                val tracks = cachedRows.map {
                    it.toDiscoverTrack(provider.displayName, provider::buildStreamEndpoint)
                }
                return@withContext DiscoverPageResult(
                    tracks = sanitizeTracks(tracks),
                    isFromCache = true,
                    isStale = false,
                    cacheTimestampMs = cachedRows.first().fetchedAt,
                    errorMessage = null,
                    latencyMs = 1L
                )
            }
        }

        // 2. Single-flight network fetch
        val flightKey = "${provider.providerId}:trending:$limit:$offset"
        val networkResult = executeSingleFlight(flightKey) {
            requestSemaphore.withPermit {
                provider.getTrending(limit = limit, offset = offset)
            }
        }

        if (networkResult.errorMessage == null && networkResult.tracks.isNotEmpty()) {
            val deduplicated = sanitizeTracks(networkResult.tracks)
            if (offset == 0 && localRepository != null) {
                try {
                    val entities = deduplicated.mapIndexed { idx, trk ->
                        trk.toCachedEntity(
                            cacheKey = cacheKey,
                            sortIndex = idx,
                            fetchedAt = nowMs,
                            ttlMs = cacheTtlMs
                        )
                    }
                    localRepository.saveDiscoverCache(
                        providerId = provider.providerId,
                        cacheKey = cacheKey,
                        items = entities,
                        replaceExistingKey = true,
                        maxEntries = maxCacheEntries
                    )
                } catch (_: Exception) {
                }
            }
            return@withContext networkResult.copy(tracks = deduplicated)
        }

        // 3. Stale-while-revalidate fallback: retain cached data if network refresh fails
        if (offset == 0 && cachedRows.isNotEmpty()) {
            val staleTracks = cachedRows.map {
                it.toDiscoverTrack(provider.displayName, provider::buildStreamEndpoint)
            }
            return@withContext DiscoverPageResult(
                tracks = sanitizeTracks(staleTracks),
                isFromCache = true,
                isStale = true,
                cacheTimestampMs = cachedRows.first().fetchedAt,
                errorMessage = "Offline / network unreachable — showing cached ${provider.displayName} results.",
                latencyMs = networkResult.latencyMs
            )
        }

        networkResult
    }

    /**
     * Searches remote tracks with Room Stale-While-Revalidate and Single-Flight deduplication.
     */
    suspend fun searchTracks(
        query: String,
        providerId: String = "audius",
        limit: Int = 30,
        offset: Int = 0,
        forceRefresh: Boolean = false,
        nowMs: Long = System.currentTimeMillis()
    ): DiscoverPageResult = withContext(Dispatchers.IO) {
        val cleanQuery = DiscoverComparisonUtils.normalizeKey(query)
        if (cleanQuery.isBlank()) {
            return@withContext DiscoverPageResult(emptyList(), errorMessage = null)
        }
        val provider = getProvider(providerId)
        val cacheKey = "search:$cleanQuery"

        val cachedRows = if (offset == 0 && localRepository != null) {
            try {
                localRepository.getDiscoverCache(provider.providerId, cacheKey)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        if (!forceRefresh && offset == 0 && cachedRows.isNotEmpty()) {
            val isFresh = cachedRows.first().expiresAt > nowMs
            if (isFresh) {
                val tracks = cachedRows.map {
                    it.toDiscoverTrack(provider.displayName, provider::buildStreamEndpoint)
                }
                return@withContext DiscoverPageResult(
                    tracks = sanitizeTracks(tracks),
                    isFromCache = true,
                    isStale = false,
                    cacheTimestampMs = cachedRows.first().fetchedAt,
                    errorMessage = null,
                    latencyMs = 1L
                )
            }
        }

        val flightKey = "${provider.providerId}:search:$cleanQuery:$limit:$offset"
        val networkResult = executeSingleFlight(flightKey) {
            requestSemaphore.withPermit {
                provider.searchTracks(query = query.trim(), limit = limit, offset = offset)
            }
        }

        if (networkResult.errorMessage == null && networkResult.tracks.isNotEmpty()) {
            val deduplicated = sanitizeTracks(networkResult.tracks)
            if (offset == 0 && localRepository != null) {
                try {
                    val entities = deduplicated.mapIndexed { idx, trk ->
                        trk.toCachedEntity(
                            cacheKey = cacheKey,
                            sortIndex = idx,
                            fetchedAt = nowMs,
                            ttlMs = cacheTtlMs
                        )
                    }
                    localRepository.saveDiscoverCache(
                        providerId = provider.providerId,
                        cacheKey = cacheKey,
                        items = entities,
                        replaceExistingKey = true,
                        maxEntries = maxCacheEntries
                    )
                } catch (_: Exception) {
                }
            }
            return@withContext networkResult.copy(tracks = deduplicated)
        }

        // Fallback to stale cache when offline
        if (offset == 0 && cachedRows.isNotEmpty()) {
            val staleTracks = cachedRows.map {
                it.toDiscoverTrack(provider.displayName, provider::buildStreamEndpoint)
            }
            return@withContext DiscoverPageResult(
                tracks = sanitizeTracks(staleTracks),
                isFromCache = true,
                isStale = true,
                cacheTimestampMs = cachedRows.first().fetchedAt,
                errorMessage = "Offline — showing cached search results for \"$query\".",
                latencyMs = networkResult.latencyMs
            )
        }

        networkResult
    }

    private fun sanitizeTracks(tracks: List<DiscoverTrack>): List<DiscoverTrack> {
        val brokenSnapshot = getBrokenArtworkUrlsSnapshot()
        return tracks
            .distinctBy { it.namespacedId }
            .map { trk ->
                val validArt = trk.artworkUrl?.takeIf { url ->
                    com.vesper.audio.util.SecurityValidator.isValidHttpsUrl(url) && url !in brokenSnapshot
                }
                trk.copy(artworkUrl = validArt)
            }
    }

    private suspend fun executeSingleFlight(
        key: String,
        block: suspend () -> DiscoverPageResult
    ): DiscoverPageResult {
        val existingOrNew: Pair<CompletableDeferred<DiscoverPageResult>, Boolean> = inFlightMutex.withLock {
            val active = inFlightRequests[key]
            if (active != null && active.isActive) {
                active to false
            } else {
                val created = CompletableDeferred<DiscoverPageResult>()
                inFlightRequests[key] = created
                created to true
            }
        }

        val deferred = existingOrNew.first
        val isCreator = existingOrNew.second

        if (!isCreator) {
            return deferred.await()
        }

        return try {
            val result = block()
            deferred.complete(result)
            result
        } catch (t: Throwable) {
            val fallback = DiscoverPageResult(emptyList(), errorMessage = "Discovery request failed: ${t.localizedMessage ?: "Network error"}")
            deferred.complete(fallback)
            fallback
        } finally {
            inFlightMutex.withLock {
                inFlightRequests.remove(key)
            }
        }
    }

    suspend fun getCacheEntryCount(): Int {
        return try {
            localRepository?.getDiscoverCacheCount() ?: 0
        } catch (_: Exception) {
            0
        }
    }

    suspend fun clearCache() {
        try {
            localRepository?.clearAllDiscoverCache()
            brokenArtworkUrls.clear()
        } catch (_: Exception) {
        }
    }

    /**
     * Assembles the complete available tracklist for a remote album by combining
     * seed tracks with multi-page provider queries for the artist/album,
     * deduplicating by namespacedId and preserving natural track ordering.
     */
    suspend fun fetchCompleteAlbumTracklist(
        albumName: String,
        artistName: String,
        seedTracks: List<DiscoverTrack>,
        providerId: String = "audius",
        maxPages: Int = 3,
        pageSize: Int = 50
    ): List<DiscoverTrack> = withContext(Dispatchers.IO) {
        val normAlbum = DiscoverComparisonUtils.normalizeKey(albumName)
        val normArtist = DiscoverComparisonUtils.normalizeKey(artistName)

        fun matchesAlbum(track: DiscoverTrack): Boolean {
            val tAlbum = DiscoverComparisonUtils.normalizeKey(track.album)
            val tArtist = DiscoverComparisonUtils.normalizeKey(track.artist)
            val artistMatches = normArtist.isBlank() || normArtist == "unknown artist" || tArtist == normArtist
            return tAlbum == normAlbum && artistMatches
        }

        val combined = LinkedHashMap<String, DiscoverTrack>()
        seedTracks.filter(::matchesAlbum).forEach { combined[it.namespacedId] = it }

        val queryTerm = if (artistName.isNotBlank() && !artistName.equals("Unknown Artist", ignoreCase = true)) {
            artistName.trim()
        } else {
            albumName.trim()
        }

        if (queryTerm.isNotBlank()) {
            for (page in 0 until maxPages.coerceIn(1, 5)) {
                val pageResult = searchTracks(
                    query = queryTerm,
                    providerId = providerId,
                    limit = pageSize,
                    offset = page * pageSize
                )
                if (pageResult.tracks.isEmpty()) break
                pageResult.tracks.filter(::matchesAlbum).forEach { trk ->
                    if (!combined.containsKey(trk.namespacedId)) {
                        combined[trk.namespacedId] = trk
                    }
                }
                if (pageResult.tracks.size < pageSize) break
            }
        }

        sanitizeTracks(combined.values.toList())
    }

    /**
     * Assembles the complete available catalogue (tracks + albums) for a remote artist
     * across multiple pages, deduplicating by namespacedId.
     */
    suspend fun fetchCompleteArtistCatalogue(
        artistName: String,
        seedTracks: List<DiscoverTrack>,
        providerId: String = "audius",
        maxPages: Int = 3,
        pageSize: Int = 50
    ): DiscoverArtistDetail = withContext(Dispatchers.IO) {
        val normArtist = DiscoverComparisonUtils.normalizeKey(artistName)

        fun matchesArtist(track: DiscoverTrack): Boolean {
            return DiscoverComparisonUtils.normalizeKey(track.artist) == normArtist
        }

        val combined = LinkedHashMap<String, DiscoverTrack>()
        seedTracks.filter(::matchesArtist).forEach { combined[it.namespacedId] = it }

        if (artistName.isNotBlank()) {
            for (page in 0 until maxPages.coerceIn(1, 5)) {
                val pageResult = searchTracks(
                    query = artistName.trim(),
                    providerId = providerId,
                    limit = pageSize,
                    offset = page * pageSize
                )
                if (pageResult.tracks.isEmpty()) break
                pageResult.tracks.filter(::matchesArtist).forEach { trk ->
                    if (!combined.containsKey(trk.namespacedId)) {
                        combined[trk.namespacedId] = trk
                    }
                }
                if (pageResult.tracks.size < pageSize) break
            }
        }

        val resolvedTracks = sanitizeTracks(combined.values.toList())
        val brokenSnapshot = getBrokenArtworkUrlsSnapshot()
        val resolvedAlbums = DiscoverComparisonUtils.deriveAlbumsFromDiscoverTracks(
            resolvedTracks,
            brokenSnapshot
        )
        DiscoverArtistDetail(
            providerId = providerId,
            artistId = "$providerId:artist:$normArtist",
            name = resolvedTracks.firstOrNull()?.artist ?: artistName,
            artworkUrl = resolvedTracks.firstNotNullOfOrNull { it.artworkUrl },
            tracks = resolvedTracks,
            albums = resolvedAlbums
        )
    }
}
