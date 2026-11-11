package com.vesper.audio.data.remote

import android.net.Uri
import com.vesper.audio.data.local.DiscoverCachedTrackEntity
import com.vesper.audio.model.Album
import com.vesper.audio.model.Artist
import com.vesper.audio.model.Track

/**
 * Provider-neutral remote track representation for VESPER Discover.
 * Uses namespaced IDs (`<providerId>:<providerTrackId>`) to prevent collisions
 * with local device tracks and across multiple remote providers.
 */
data class DiscoverTrack(
    val providerId: String,
    val providerName: String,
    val providerTrackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String?,
    val durationMs: Long,
    val genre: String = "",
    val releaseYear: Int = 0,
    val isExplicit: Boolean = false,
    val streamEndpointUrl: String = ""
) {
    val namespacedId: String
        get() = "${providerId.lowercase()}:$providerTrackId"

    fun toTrack(resolvedArtworkUrl: String? = artworkUrl): Track {
        val cleanArtUri = resolvedArtworkUrl
            ?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
            ?.let { Uri.parse(it) }
        val streamUri = if (streamEndpointUrl.isNotBlank()) {
            Uri.parse(streamEndpointUrl)
        } else {
            Uri.parse("https://discoveryprovider.audius.co/v1/tracks/$providerTrackId/stream?app_name=VESPER")
        }
        return Track(
            id = namespacedId,
            title = title.ifBlank { "Untitled Track" },
            artist = artist.ifBlank { "Unknown Artist" },
            album = album.ifBlank { "$providerName Independent" },
            durationMs = durationMs.coerceAtLeast(0L),
            contentUri = streamUri,
            artworkUri = cleanArtUri,
            year = releaseYear,
            genre = genre,
            albumArtist = artist.ifBlank { "Unknown Artist" },
            mimeType = "audio/mpeg"
        )
    }

    fun toAudiusTrack(): AudiusTrack {
        return AudiusTrack(
            id = providerTrackId,
            title = title,
            artist = artist,
            artworkUrl = artworkUrl,
            durationSec = (durationMs / 1000L).toInt(),
            genre = genre,
            streamUrl = streamEndpointUrl.ifBlank {
                "https://discoveryprovider.audius.co/v1/tracks/$providerTrackId/stream?app_name=VESPER"
            },
            providerId = providerId,
            providerName = providerName,
            albumName = album,
            releaseYear = releaseYear,
            isExplicit = isExplicit
        )
    }

    fun toCachedEntity(
        cacheKey: String,
        sortIndex: Int,
        fetchedAt: Long,
        ttlMs: Long
    ): DiscoverCachedTrackEntity {
        return DiscoverCachedTrackEntity(
            providerId = providerId,
            cacheKey = cacheKey,
            providerTrackId = providerTrackId,
            namespacedId = namespacedId,
            title = title,
            artist = artist,
            album = album,
            artworkUrl = artworkUrl,
            durationMs = durationMs,
            genre = genre,
            releaseYear = releaseYear,
            isExplicit = isExplicit,
            sortIndex = sortIndex,
            fetchedAt = fetchedAt,
            expiresAt = fetchedAt + ttlMs
        )
    }
}

fun DiscoverCachedTrackEntity.toDiscoverTrack(providerName: String, streamEndpointBuilder: (String) -> String): DiscoverTrack {
    return DiscoverTrack(
        providerId = providerId,
        providerName = providerName,
        providerTrackId = providerTrackId,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artworkUrl,
        durationMs = durationMs,
        genre = genre,
        releaseYear = releaseYear,
        isExplicit = isExplicit,
        streamEndpointUrl = streamEndpointBuilder(providerTrackId)
    )
}

data class DiscoverPageResult(
    val tracks: List<DiscoverTrack>,
    val isFromCache: Boolean = false,
    val isStale: Boolean = false,
    val cacheTimestampMs: Long = 0L,
    val errorMessage: String? = null,
    val latencyMs: Long = 0L
)

data class DiscoverArtistDetail(
    val providerId: String,
    val artistId: String,
    val name: String,
    val artworkUrl: String?,
    val tracks: List<DiscoverTrack>,
    val albums: List<Album>
)

data class DiscoverAlbumDetail(
    val providerId: String,
    val albumId: String,
    val name: String,
    val artist: String,
    val artworkUrl: String?,
    val year: Int,
    val tracks: List<DiscoverTrack>
)

/**
 * Provider-neutral contract for online music discovery in VESPER.
 */
interface DiscoverProvider {
    val providerId: String
    val displayName: String
    val isEnabled: Boolean
    val isAuthorized: Boolean
    val statusNote: String

    suspend fun getTrending(limit: Int = 50, offset: Int = 0): DiscoverPageResult
    suspend fun searchTracks(query: String, limit: Int = 30, offset: Int = 0): DiscoverPageResult
    suspend fun getTrackDetail(trackId: String, currentCatalogue: List<DiscoverTrack> = emptyList()): DiscoverTrack?
    suspend fun getArtistDetail(artistName: String, currentCatalogue: List<DiscoverTrack>): DiscoverArtistDetail
    suspend fun getAlbumDetail(albumName: String, artistName: String, currentCatalogue: List<DiscoverTrack>): DiscoverAlbumDetail
    suspend fun resolveStreamUri(track: DiscoverTrack): Uri
    fun buildStreamEndpoint(providerTrackId: String): String
}

/**
 * Authorised public Audius Discovery API provider implementation.
 */
open class AudiusDiscoverProvider(
    private val apiClient: AudiusApiClient = AudiusApiClient()
) : DiscoverProvider {
    override val providerId: String = "audius"
    override val displayName: String = "Audius"
    override val isEnabled: Boolean = true
    override val isAuthorized: Boolean = true
    override val statusNote: String = "Official public Audius Discovery API v1 (Active)"

    override fun buildStreamEndpoint(providerTrackId: String): String {
        return "https://discoveryprovider.audius.co/v1/tracks/$providerTrackId/stream?app_name=VESPER"
    }

    override suspend fun getTrending(limit: Int, offset: Int): DiscoverPageResult {
        val startMs = System.currentTimeMillis()
        val res = apiClient.getTrendingTracksResult(limit = limit, offset = offset)
        val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
        return DiscoverPageResult(
            tracks = res.tracks.map { it.toDiscoverTrack() },
            isFromCache = false,
            isStale = false,
            cacheTimestampMs = System.currentTimeMillis(),
            errorMessage = res.errorMessage,
            latencyMs = elapsed
        )
    }

    override suspend fun searchTracks(query: String, limit: Int, offset: Int): DiscoverPageResult {
        val startMs = System.currentTimeMillis()
        val res = apiClient.searchTracksResult(query = query, limit = limit, offset = offset)
        val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
        return DiscoverPageResult(
            tracks = res.tracks.map { it.toDiscoverTrack() },
            isFromCache = false,
            isStale = false,
            cacheTimestampMs = System.currentTimeMillis(),
            errorMessage = res.errorMessage,
            latencyMs = elapsed
        )
    }

    override suspend fun getTrackDetail(trackId: String, currentCatalogue: List<DiscoverTrack>): DiscoverTrack? {
        val cleanId = trackId.removePrefix("audius:").removePrefix("audius_")
        return currentCatalogue.firstOrNull { it.providerTrackId == cleanId || it.namespacedId == trackId }
    }

    override suspend fun getArtistDetail(artistName: String, currentCatalogue: List<DiscoverTrack>): DiscoverArtistDetail {
        val normKey = DiscoverComparisonUtils.normalizeKey(artistName)
        val artistTracks = currentCatalogue.filter {
            DiscoverComparisonUtils.normalizeKey(it.artist) == normKey
        }
        val albums = DiscoverComparisonUtils.deriveAlbumsFromDiscoverTracks(artistTracks)
        return DiscoverArtistDetail(
            providerId = providerId,
            artistId = "audius:artist:$normKey",
            name = artistTracks.firstOrNull()?.artist ?: artistName,
            artworkUrl = artistTracks.firstNotNullOfOrNull { it.artworkUrl },
            tracks = artistTracks,
            albums = albums
        )
    }

    override suspend fun getAlbumDetail(
        albumName: String,
        artistName: String,
        currentCatalogue: List<DiscoverTrack>
    ): DiscoverAlbumDetail {
        val normAlbum = DiscoverComparisonUtils.normalizeKey(albumName)
        val normArtist = DiscoverComparisonUtils.normalizeKey(artistName)
        val albumTracks = currentCatalogue.filter {
            DiscoverComparisonUtils.normalizeKey(it.album) == normAlbum &&
                (normArtist.isBlank() || DiscoverComparisonUtils.normalizeKey(it.artist) == normArtist)
        }
        return DiscoverAlbumDetail(
            providerId = providerId,
            albumId = "audius:album:$normAlbum|$normArtist",
            name = albumTracks.firstOrNull()?.album ?: albumName,
            artist = albumTracks.firstOrNull()?.artist ?: artistName,
            artworkUrl = albumTracks.firstNotNullOfOrNull { it.artworkUrl },
            year = albumTracks.firstOrNull()?.releaseYear ?: 0,
            tracks = albumTracks
        )
    }

    override suspend fun resolveStreamUri(track: DiscoverTrack): Uri {
        return Uri.parse(buildStreamEndpoint(track.providerTrackId))
    }
}

/**
 * JioSaavn Provider Adapter — Disabled by default per VESPER Provider Policy:
 * Unofficial or reverse-engineered JioSaavn endpoints are strictly prohibited.
 * Remains disabled until official partner API credentials and redistribution permissions are configured.
 */
class JioSaavnDiscoverProvider : DiscoverProvider {
    override val providerId: String = "jiosaavn"
    override val displayName: String = "JioSaavn"
    override val isEnabled: Boolean = false
    override val isAuthorized: Boolean = false
    override val statusNote: String = "Disabled by default (requires authorised partner API credentials)"

    override fun buildStreamEndpoint(providerTrackId: String): String = ""

    override suspend fun getTrending(limit: Int, offset: Int): DiscoverPageResult =
        DiscoverPageResult(emptyList(), errorMessage = "JioSaavn provider is disabled (awaiting authorised partner API credentials).")

    override suspend fun searchTracks(query: String, limit: Int, offset: Int): DiscoverPageResult =
        DiscoverPageResult(emptyList(), errorMessage = "JioSaavn provider is disabled (awaiting authorised partner API credentials).")

    override suspend fun getTrackDetail(trackId: String, currentCatalogue: List<DiscoverTrack>): DiscoverTrack? = null

    override suspend fun getArtistDetail(artistName: String, currentCatalogue: List<DiscoverTrack>): DiscoverArtistDetail =
        DiscoverArtistDetail(providerId, "jiosaavn:artist:$artistName", artistName, null, emptyList(), emptyList())

    override suspend fun getAlbumDetail(albumName: String, artistName: String, currentCatalogue: List<DiscoverTrack>): DiscoverAlbumDetail =
        DiscoverAlbumDetail(providerId, "jiosaavn:album:$albumName", albumName, artistName, null, 0, emptyList())

    override suspend fun resolveStreamUri(track: DiscoverTrack): Uri = Uri.EMPTY
}

/**
 * BitChord Provider Adapter — Disabled by default per VESPER Provider Policy:
 * Remains disabled until an official documented public API with streaming authorization is available.
 */
class BitChordDiscoverProvider : DiscoverProvider {
    override val providerId: String = "bitchord"
    override val displayName: String = "BitChord"
    override val isEnabled: Boolean = false
    override val isAuthorized: Boolean = false
    override val statusNote: String = "Disabled by default (no public authorised streaming API endpoint)"

    override fun buildStreamEndpoint(providerTrackId: String): String = ""

    override suspend fun getTrending(limit: Int, offset: Int): DiscoverPageResult =
        DiscoverPageResult(emptyList(), errorMessage = "BitChord provider is disabled (no authorised public API).")

    override suspend fun searchTracks(query: String, limit: Int, offset: Int): DiscoverPageResult =
        DiscoverPageResult(emptyList(), errorMessage = "BitChord provider is disabled (no authorised public API).")

    override suspend fun getTrackDetail(trackId: String, currentCatalogue: List<DiscoverTrack>): DiscoverTrack? = null

    override suspend fun getArtistDetail(artistName: String, currentCatalogue: List<DiscoverTrack>): DiscoverArtistDetail =
        DiscoverArtistDetail(providerId, "bitchord:artist:$artistName", artistName, null, emptyList(), emptyList())

    override suspend fun getAlbumDetail(albumName: String, artistName: String, currentCatalogue: List<DiscoverTrack>): DiscoverAlbumDetail =
        DiscoverAlbumDetail(providerId, "bitchord:album:$albumName", albumName, artistName, null, 0, emptyList())

    override suspend fun resolveStreamUri(track: DiscoverTrack): Uri = Uri.EMPTY
}

/**
 * Normalized comparison utilities to prevent duplicate artists or albums
 * caused by case differences or inconsistent whitespace while preserving display names.
 */
object DiscoverComparisonUtils {
    private val whitespaceRegex = Regex("\\s+")

    fun normalizeKey(raw: String): String {
        return raw.trim().lowercase().replace(whitespaceRegex, " ")
    }

    /**
     * Partial, case-insensitive, token-aware query matcher.
     * Every token in the user query must match at least one of the provided fields.
     */
    fun matchesTokens(query: String, vararg fields: String): Boolean {
        val normQuery = normalizeKey(query)
        if (normQuery.isBlank()) return true
        val tokens = normQuery.split(" ").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return true
        val normalizedFields = fields.map { normalizeKey(it) }
        return tokens.all { token ->
            normalizedFields.any { field -> field.contains(token) }
        }
    }

    /**
     * Computes a deterministic relevance score (higher = better match) for sorting suggestions and search results.
     */
    fun scoreTrackMatch(title: String, artist: String, album: String, query: String): Int {
        val q = normalizeKey(query)
        if (q.isBlank()) return 0
        val t = normalizeKey(title)
        val a = normalizeKey(artist)
        val al = normalizeKey(album)
        var score = 0
        if (t == q) score += 100
        else if (t.startsWith(q)) score += 70
        else if (t.contains(q)) score += 45

        if (a == q) score += 80
        else if (a.startsWith(q)) score += 55
        else if (a.contains(q)) score += 35

        if (al == q) score += 60
        else if (al.startsWith(q)) score += 40
        else if (al.contains(q)) score += 20

        if (matchesTokens(q, title, artist, album)) {
            score += 15
        }
        return score
    }

    fun deriveAlbumsFromDiscoverTracks(
        tracks: List<DiscoverTrack>,
        brokenArtworkUrls: Set<String> = emptySet()
    ): List<Album> {
        return tracks
            .distinctBy { it.namespacedId }
            .groupBy { "${normalizeKey(it.album)}|${normalizeKey(it.artist)}" }
            .values
            .mapNotNull { group ->
                val first = group.firstOrNull() ?: return@mapNotNull null
                val validArtUrl = group.firstNotNullOfOrNull { trk ->
                    trk.artworkUrl?.takeIf { it.isNotBlank() && it !in brokenArtworkUrls }
                }
                val bestYear = group.map { it.releaseYear }.firstOrNull { it > 0 } ?: 0
                Album(
                    name = first.album.trim().ifBlank { "${first.providerName} Independent" },
                    artist = first.artist.trim().ifBlank { "Unknown Artist" },
                    artworkUri = validArtUrl?.let { Uri.parse(it) },
                    trackCount = group.size,
                    year = bestYear
                )
            }
    }

    fun deriveArtistsFromDiscoverTracks(tracks: List<DiscoverTrack>): List<Artist> {
        return tracks
            .distinctBy { it.namespacedId }
            .groupBy { normalizeKey(it.artist) }
            .values
            .mapNotNull { group ->
                val first = group.firstOrNull() ?: return@mapNotNull null
                val distinctAlbums = group.map { normalizeKey(it.album) }.distinct().size
                Artist(
                    name = first.artist.trim().ifBlank { "Unknown Artist" },
                    trackCount = group.size,
                    albumCount = distinctAlbums.coerceAtLeast(1)
                )
            }
    }
}
