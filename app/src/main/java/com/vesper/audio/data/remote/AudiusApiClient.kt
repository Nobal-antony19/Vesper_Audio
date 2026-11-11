package com.vesper.audio.data.remote

import android.net.Uri
import com.vesper.audio.model.Album
import com.vesper.audio.model.Artist
import com.vesper.audio.model.Track
import com.vesper.audio.util.SecurityValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

data class AudiusTrack(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val durationSec: Int,
    val genre: String,
    val streamUrl: String,
    val providerId: String = "audius",
    val providerName: String = "Audius",
    val albumName: String = "",
    val releaseYear: Int = 0,
    val isExplicit: Boolean = false
) {
    val namespacedId: String
        get() = "${providerId.lowercase()}:$id"

    fun toDiscoverTrack(): DiscoverTrack {
        val resolvedAlbum = albumName.ifBlank {
            if (genre.isNotBlank()) "$providerName · $genre" else "$providerName Independent"
        }
        return DiscoverTrack(
            providerId = providerId,
            providerName = providerName,
            providerTrackId = id,
            title = title.ifBlank { "Untitled Track" },
            artist = artist.ifBlank { "Unknown Artist" },
            album = resolvedAlbum,
            artworkUrl = artworkUrl?.takeIf { SecurityValidator.isValidHttpsUrl(it) },
            durationMs = durationSec.coerceAtLeast(0) * 1000L,
            genre = genre,
            releaseYear = releaseYear,
            isExplicit = isExplicit,
            streamEndpointUrl = streamUrl
        )
    }

    fun toTrack(): Track {
        return toDiscoverTrack().toTrack()
    }
}

data class AudiusResult(
    val tracks: List<AudiusTrack>,
    val errorMessage: String? = null
)

class AudiusApiClient {

    private val client: OkHttpClient = SHARED_HTTP_CLIENT

    companion object {
        private const val BASE_URL = "https://discoveryprovider.audius.co/v1"
        private const val APP_NAME = "VESPER"
        private const val MAX_TRANSIENT_RETRIES = 1

        private val SHARED_CONNECTION_POOL = ConnectionPool(5, 5, TimeUnit.MINUTES)

        val SHARED_HTTP_CLIENT: OkHttpClient = OkHttpClient.Builder()
            .connectionPool(SHARED_CONNECTION_POOL)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .callTimeout(9, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        internal fun buildTrendingUrl(limit: Int = 50, offset: Int = 0): String {
            return "$BASE_URL/tracks/trending?app_name=$APP_NAME&limit=${limit.coerceIn(1, 100)}&offset=${offset.coerceAtLeast(0)}"
        }

        internal fun buildSearchUrl(query: String, limit: Int = 30, offset: Int = 0): String {
            val encodedQuery = Uri.encode(query.trim())
            return "$BASE_URL/tracks/search?query=$encodedQuery&app_name=$APP_NAME&limit=${limit.coerceIn(1, 100)}&offset=${offset.coerceAtLeast(0)}"
        }

        internal fun formatHttpError(code: Int): String {
            return when (code) {
                429 -> "Audius rate limit reached (HTTP 429). Please wait a moment and retry."
                in 500..599 -> "Audius service temporarily unavailable (HTTP $code)."
                else -> "Audius returned HTTP $code"
            }
        }

        internal fun isTransientHttpCode(code: Int): Boolean {
            return code == 502 || code == 503 || code == 504
        }

        /**
         * Deduplicates remote discovery tracks by provider track ID and applies
         * session-seeded randomisation when query is blank so discovery feels fresh
         * on each launch without any hardcoded language bias.
         */
        fun shuffleForSession(tracks: List<AudiusTrack>, sessionSeed: Long): List<AudiusTrack> {
            val deduplicated = tracks.distinctBy { it.namespacedId }
            if (deduplicated.size <= 1) return deduplicated
            return deduplicated.shuffled(java.util.Random(sessionSeed))
        }

        fun deriveAlbumsFromTracks(tracks: List<AudiusTrack>, brokenArtworkUrls: Set<String> = emptySet()): List<Album> {
            return DiscoverComparisonUtils.deriveAlbumsFromDiscoverTracks(
                tracks.map { it.toDiscoverTrack() },
                brokenArtworkUrls
            )
        }

        fun deriveArtistsFromTracks(tracks: List<AudiusTrack>): List<Artist> {
            return DiscoverComparisonUtils.deriveArtistsFromDiscoverTracks(
                tracks.map { it.toDiscoverTrack() }
            )
        }
    }

    suspend fun getTrendingTracks(limit: Int = 50, offset: Int = 0): List<AudiusTrack> =
        getTrendingTracksResult(limit, offset).tracks

    suspend fun getTrendingTracksResult(limit: Int = 50, offset: Int = 0): AudiusResult = withContext(Dispatchers.IO) {
        val url = buildTrendingUrl(limit, offset)
        fetchTracksResultWithRetry(url)
    }

    suspend fun searchTracks(query: String, limit: Int = 25, offset: Int = 0): List<AudiusTrack> =
        searchTracksResult(query, limit, offset).tracks

    suspend fun searchTracksResult(query: String, limit: Int = 25, offset: Int = 0): AudiusResult = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext AudiusResult(emptyList(), null)
        val url = buildSearchUrl(query, limit, offset)
        fetchTracksResultWithRetry(url)
    }

    private suspend fun fetchTracksResultWithRetry(url: String): AudiusResult {
        if (!SecurityValidator.isValidHttpsUrl(url)) {
            return AudiusResult(emptyList(), "Blocked non-HTTPS discovery endpoint.")
        }

        var attempt = 0
        var lastError = "Unable to connect to Audius discovery. Check your internet connection."

        while (attempt <= MAX_TRANSIENT_RETRIES) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "VESPER/3.0 (Android; Offline-First Music Player)")
                    .header("Accept", "application/json")
                    .build()

                val result = client.newCall(request).execute().use { response ->
                    val code = response.code
                    if (!response.isSuccessful) {
                        if (isTransientHttpCode(code) && attempt < MAX_TRANSIENT_RETRIES) {
                            lastError = formatHttpError(code)
                            null
                        } else {
                            AudiusResult(emptyList(), formatHttpError(code))
                        }
                    } else {
                        val bodyStr = response.body?.string()
                            ?: return@use AudiusResult(emptyList(), "Empty response from Audius")
                        AudiusResult(parseTracksJson(bodyStr), null)
                    }
                }
                if (result != null) return result
            } catch (e: SocketTimeoutException) {
                lastError = "Audius request timed out. Check your connection and try again."
                if (attempt >= MAX_TRANSIENT_RETRIES) {
                    return AudiusResult(emptyList(), lastError)
                }
            } catch (_: Exception) {
                return AudiusResult(emptyList(), lastError)
            }

            attempt++
            delay(200L)
        }

        return AudiusResult(emptyList(), lastError)
    }

    internal fun parseTracksJson(jsonStr: String): List<AudiusTrack> {
        val list = mutableListOf<AudiusTrack>()
        val seenIds = HashSet<String>()
        try {
            val root = JSONObject(jsonStr)
            val dataArray = root.optJSONArray("data") ?: return emptyList()

            for (i in 0 until dataArray.length()) {
                val item = dataArray.optJSONObject(i) ?: continue
                val rawId = item.optString("id", "").trim()
                val id = SecurityValidator.sanitizeProviderTrackId(rawId)
                if (id.isBlank() || !seenIds.add(id)) continue

                val title = item.optString("title", "").trim().ifBlank { "Untitled Track" }
                val userObj = item.optJSONObject("user")
                val artist = userObj?.optString("name", "")?.trim()?.ifBlank {
                    userObj.optString("handle", "").trim()
                }?.ifBlank { "Unknown Artist" } ?: "Unknown Artist"

                val duration = item.optInt("duration", 0).coerceAtLeast(0)
                val genre = item.optString("genre", "").trim()
                val releaseDate = item.optString("release_date", "")
                val releaseYear = releaseDate.take(4).toIntOrNull() ?: 0

                val artworkObj = item.optJSONObject("artwork")
                val rawArtUrl = artworkObj?.optString("480x480", null)?.takeIf { it.isNotBlank() }
                    ?: artworkObj?.optString("150x150", null)?.takeIf { it.isNotBlank() }
                    ?: artworkObj?.optString("1000x1000", null)?.takeIf { it.isNotBlank() }
                val artworkUrl = rawArtUrl?.takeIf { SecurityValidator.isValidHttpsUrl(it) }

                val streamUrl = "$BASE_URL/tracks/$id/stream?app_name=$APP_NAME"
                if (!SecurityValidator.isValidHttpsUrl(streamUrl)) continue

                list.add(
                    AudiusTrack(
                        id = id,
                        title = title,
                        artist = artist,
                        artworkUrl = artworkUrl,
                        durationSec = duration,
                        genre = genre,
                        streamUrl = streamUrl,
                        providerId = "audius",
                        providerName = "Audius",
                        releaseYear = releaseYear
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }
}

