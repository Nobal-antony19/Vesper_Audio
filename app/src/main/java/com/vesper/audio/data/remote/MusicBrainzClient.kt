package com.vesper.audio.data.remote

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class MusicBrainzMetadata(
    val mbid: String,
    val title: String,
    val artist: String,
    val album: String?,
    val year: Int?,
    val coverArtUrl: String?
)

class MusicBrainzClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val rateLimitMutex = Mutex()
    private var lastRequestTimeMs = 0L

    companion object {
        private const val BASE_URL = "https://musicbrainz.org/ws/2"
        private const val USER_AGENT = "VESPER/3.0 (nobalantony2192004@gmail.com)"
        private const val MIN_INTERVAL_MS = 1100L // <= 1 request / second rate limit strictly enforced
    }

    suspend fun searchRecording(title: String, artist: String): MusicBrainzMetadata? = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext null

        enforceRateLimit()

        val query = if (artist.isNotBlank()) {
            "recording:\"${title.trim()}\" AND artist:\"${artist.trim()}\""
        } else {
            "recording:\"${title.trim()}\""
        }

        val encodedQuery = Uri.encode(query)
        val url = "$BASE_URL/recording/?query=$encodedQuery&fmt=json&limit=3"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                parseRecordingJson(bodyStr)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun enforceRateLimit() {
        rateLimitMutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTimeMs
            if (elapsed < MIN_INTERVAL_MS) {
                delay(MIN_INTERVAL_MS - elapsed)
            }
            lastRequestTimeMs = System.currentTimeMillis()
        }
    }

    private fun parseRecordingJson(jsonStr: String): MusicBrainzMetadata? {
        return try {
            val root = JSONObject(jsonStr)
            val recordings = root.optJSONArray("recordings") ?: return null
            if (recordings.length() == 0) return null

            val first = recordings.getJSONObject(0)
            val id = first.optString("id", "")
            val title = first.optString("title", "")

            var artistName = ""
            val artistCredit = first.optJSONArray("artist-credit")
            if (artistCredit != null && artistCredit.length() > 0) {
                artistName = artistCredit.getJSONObject(0).optString("name", "")
            }

            var albumName: String? = null
            var releaseId: String? = null
            var releaseYear: Int? = null

            val releases = first.optJSONArray("releases")
            if (releases != null && releases.length() > 0) {
                val firstRelease = releases.getJSONObject(0)
                albumName = firstRelease.optString("title", null)
                releaseId = firstRelease.optString("id", null)
                val date = firstRelease.optString("date", "")
                if (date.length >= 4) {
                    releaseYear = date.substring(0, 4).toIntOrNull()
                }
            }

            val coverArtUrl = releaseId?.let { "https://coverartarchive.org/release/$it/front-250.jpg" }

            MusicBrainzMetadata(
                mbid = id,
                title = title,
                artist = artistName,
                album = albumName,
                year = releaseYear,
                coverArtUrl = coverArtUrl
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
