package com.vesper.audio.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vesper_user_preferences")

data class VesperPreferences(
    val sortMode: String = "TITLE_AZ",
    val eqEnabled: Boolean = false,
    val activeSoundProfile: String = "BALANCED",
    val activePreset: String = "Balanced",
    val eqBandLevelsCsv: String = "",
    val bassBoostStrength: Int = 0,
    val preampHeadroomEnabled: Boolean = true,
    val audioBalance: Float = 0f,
    val crossfadeEnabled: Boolean = false,
    val crossfadeDurationSec: Int = 4,
    val gaplessPlayback: Boolean = true,
    val pauseOnHeadphoneDisconnect: Boolean = true,
    val resumeOnLaunch: Boolean = false,
    val lockscreenArtwork: Boolean = true,
    val autoScan: Boolean = true,
    val filterShortTracks: Boolean = true,
    val lastPlayedTrackId: String? = null,
    val lastPlaybackPositionMs: Long = 0L,
    val lastQueueTrackIdsCsv: String = "",
    val lastQueueIndex: Int = 0,
    val lastShuffleMode: Boolean = false,
    val lastRepeatMode: Int = 0,
    val isSupporter: Boolean = false,
    val vesperThemeMode: String = "Nocturne",
    val directSourceStreamEnabled: Boolean = true,
    val wifiStreamingQuality: String = "High (320 kbps)",
    val mobileStreamingQuality: String = "Balanced (192 kbps)",
    val wifiOnlyDownloads: Boolean = false,
    val disableEffectsOnSpeaker: Boolean = false,
    val loudnessNormalizationEnabled: Boolean = true,
    val hideRecentsOnHome: Boolean = false,
    val hideMoodPicksOnHome: Boolean = false,
    val hideTopicPicksOnHome: Boolean = false,
    val reduceAnimations: Boolean = false,
    val reduceDynamicBlur: Boolean = false,
    val fullScreenCoverArt: Boolean = false,
    val syncedLyricsEnabled: Boolean = true,
    val blurUnfocusedLyrics: Boolean = true
)

class VesperPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val SORT_MODE = stringPreferencesKey("sort_mode")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val ACTIVE_SOUND_PROFILE = stringPreferencesKey("active_sound_profile")
        val ACTIVE_PRESET = stringPreferencesKey("active_preset")
        val EQ_BAND_LEVELS_CSV = stringPreferencesKey("eq_band_levels_csv")
        val BASS_BOOST_STRENGTH = intPreferencesKey("bass_boost_strength")
        val PREAMP_HEADROOM_ENABLED = booleanPreferencesKey("preamp_headroom_enabled")
        val AUDIO_BALANCE = floatPreferencesKey("audio_balance")
        val CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        val CROSSFADE_DURATION_SEC = intPreferencesKey("crossfade_duration_sec")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val PAUSE_ON_HEADPHONE_DISCONNECT = booleanPreferencesKey("pause_on_headphone_disconnect")
        val RESUME_ON_LAUNCH = booleanPreferencesKey("resume_on_launch")
        val LOCKSCREEN_ARTWORK = booleanPreferencesKey("lockscreen_artwork")
        val AUTO_SCAN = booleanPreferencesKey("auto_scan")
        val FILTER_SHORT_TRACKS = booleanPreferencesKey("filter_short_tracks")
        val LAST_PLAYED_TRACK_ID = stringPreferencesKey("last_played_track_id")
        val LAST_PLAYBACK_POSITION_MS = longPreferencesKey("last_playback_position_ms")
        val LAST_QUEUE_TRACK_IDS_CSV = stringPreferencesKey("last_queue_track_ids_csv")
        val LAST_QUEUE_INDEX = intPreferencesKey("last_queue_index")
        val LAST_SHUFFLE_MODE = booleanPreferencesKey("last_shuffle_mode")
        val LAST_REPEAT_MODE = intPreferencesKey("last_repeat_mode")
        val IS_SUPPORTER = booleanPreferencesKey("is_supporter")
        val VESPER_THEME_MODE = stringPreferencesKey("vesper_theme_mode")
        val DIRECT_SOURCE_STREAM_ENABLED = booleanPreferencesKey("direct_source_stream_enabled")
        val WIFI_STREAMING_QUALITY = stringPreferencesKey("wifi_streaming_quality")
        val MOBILE_STREAMING_QUALITY = stringPreferencesKey("mobile_streaming_quality")
        val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
        val DISABLE_EFFECTS_ON_SPEAKER = booleanPreferencesKey("disable_effects_on_speaker")
        val LOUDNESS_NORMALIZATION_ENABLED = booleanPreferencesKey("loudness_normalization_enabled")
        val HIDE_RECENTS_ON_HOME = booleanPreferencesKey("hide_recents_on_home")
        val HIDE_MOOD_PICKS_ON_HOME = booleanPreferencesKey("hide_mood_picks_on_home")
        val HIDE_TOPIC_PICKS_ON_HOME = booleanPreferencesKey("hide_topic_picks_on_home")
        val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
        val REDUCE_DYNAMIC_BLUR = booleanPreferencesKey("reduce_dynamic_blur")
        val FULL_SCREEN_COVER_ART = booleanPreferencesKey("full_screen_cover_art")
        val SYNCED_LYRICS_ENABLED = booleanPreferencesKey("synced_lyrics_enabled")
        val BLUR_UNFOCUSED_LYRICS = booleanPreferencesKey("blur_unfocused_lyrics")
    }

    val userPreferencesFlow: Flow<VesperPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            VesperPreferences(
                sortMode = preferences[PreferencesKeys.SORT_MODE] ?: "TITLE_AZ",
                eqEnabled = preferences[PreferencesKeys.EQ_ENABLED] ?: false,
                activeSoundProfile = preferences[PreferencesKeys.ACTIVE_SOUND_PROFILE] ?: "BALANCED",
                activePreset = preferences[PreferencesKeys.ACTIVE_PRESET] ?: "Balanced",
                eqBandLevelsCsv = preferences[PreferencesKeys.EQ_BAND_LEVELS_CSV] ?: "",
                bassBoostStrength = preferences[PreferencesKeys.BASS_BOOST_STRENGTH] ?: 0,
                preampHeadroomEnabled = preferences[PreferencesKeys.PREAMP_HEADROOM_ENABLED] ?: true,
                audioBalance = preferences[PreferencesKeys.AUDIO_BALANCE] ?: 0f,
                crossfadeEnabled = preferences[PreferencesKeys.CROSSFADE_ENABLED] ?: false,
                crossfadeDurationSec = preferences[PreferencesKeys.CROSSFADE_DURATION_SEC] ?: 4,
                gaplessPlayback = preferences[PreferencesKeys.GAPLESS_PLAYBACK] ?: true,
                pauseOnHeadphoneDisconnect = preferences[PreferencesKeys.PAUSE_ON_HEADPHONE_DISCONNECT] ?: true,
                resumeOnLaunch = preferences[PreferencesKeys.RESUME_ON_LAUNCH] ?: false,
                lockscreenArtwork = preferences[PreferencesKeys.LOCKSCREEN_ARTWORK] ?: true,
                autoScan = preferences[PreferencesKeys.AUTO_SCAN] ?: true,
                filterShortTracks = preferences[PreferencesKeys.FILTER_SHORT_TRACKS] ?: true,
                lastPlayedTrackId = preferences[PreferencesKeys.LAST_PLAYED_TRACK_ID],
                lastPlaybackPositionMs = preferences[PreferencesKeys.LAST_PLAYBACK_POSITION_MS] ?: 0L,
                lastQueueTrackIdsCsv = preferences[PreferencesKeys.LAST_QUEUE_TRACK_IDS_CSV] ?: "",
                lastQueueIndex = preferences[PreferencesKeys.LAST_QUEUE_INDEX] ?: 0,
                lastShuffleMode = preferences[PreferencesKeys.LAST_SHUFFLE_MODE] ?: false,
                lastRepeatMode = preferences[PreferencesKeys.LAST_REPEAT_MODE] ?: 0,
                isSupporter = preferences[PreferencesKeys.IS_SUPPORTER] ?: false,
                vesperThemeMode = preferences[PreferencesKeys.VESPER_THEME_MODE] ?: "Nocturne",
                directSourceStreamEnabled = preferences[PreferencesKeys.DIRECT_SOURCE_STREAM_ENABLED] ?: true,
                wifiStreamingQuality = preferences[PreferencesKeys.WIFI_STREAMING_QUALITY] ?: "High (320 kbps)",
                mobileStreamingQuality = preferences[PreferencesKeys.MOBILE_STREAMING_QUALITY] ?: "Balanced (192 kbps)",
                wifiOnlyDownloads = preferences[PreferencesKeys.WIFI_ONLY_DOWNLOADS] ?: false,
                disableEffectsOnSpeaker = preferences[PreferencesKeys.DISABLE_EFFECTS_ON_SPEAKER] ?: false,
                loudnessNormalizationEnabled = preferences[PreferencesKeys.LOUDNESS_NORMALIZATION_ENABLED] ?: true,
                hideRecentsOnHome = preferences[PreferencesKeys.HIDE_RECENTS_ON_HOME] ?: false,
                hideMoodPicksOnHome = preferences[PreferencesKeys.HIDE_MOOD_PICKS_ON_HOME] ?: false,
                hideTopicPicksOnHome = preferences[PreferencesKeys.HIDE_TOPIC_PICKS_ON_HOME] ?: false,
                reduceAnimations = preferences[PreferencesKeys.REDUCE_ANIMATIONS] ?: false,
                reduceDynamicBlur = preferences[PreferencesKeys.REDUCE_DYNAMIC_BLUR] ?: false,
                fullScreenCoverArt = preferences[PreferencesKeys.FULL_SCREEN_COVER_ART] ?: false,
                syncedLyricsEnabled = preferences[PreferencesKeys.SYNCED_LYRICS_ENABLED] ?: true,
                blurUnfocusedLyrics = preferences[PreferencesKeys.BLUR_UNFOCUSED_LYRICS] ?: true
            )
        }

    suspend fun setSortMode(sortMode: String) {
        context.dataStore.edit { it[PreferencesKeys.SORT_MODE] = sortMode }
    }

    suspend fun setEqSettings(
        enabled: Boolean,
        profile: String,
        preset: String,
        bandLevelsCsv: String,
        bassBoost: Int,
        preampHeadroom: Boolean = true,
        balance: Float = 0f
    ) {
        context.dataStore.edit {
            it[PreferencesKeys.EQ_ENABLED] = enabled
            it[PreferencesKeys.ACTIVE_SOUND_PROFILE] = profile
            it[PreferencesKeys.ACTIVE_PRESET] = preset
            it[PreferencesKeys.EQ_BAND_LEVELS_CSV] = bandLevelsCsv
            it[PreferencesKeys.BASS_BOOST_STRENGTH] = bassBoost
            it[PreferencesKeys.PREAMP_HEADROOM_ENABLED] = preampHeadroom
            it[PreferencesKeys.AUDIO_BALANCE] = balance
        }
    }

    suspend fun setPreampHeadroomEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.PREAMP_HEADROOM_ENABLED] = enabled }
    }

    suspend fun setAudioBalance(balance: Float) {
        context.dataStore.edit { it[PreferencesKeys.AUDIO_BALANCE] = balance.coerceIn(-1f, 1f) }
    }

    suspend fun setCrossfade(enabled: Boolean, durationSec: Int) {
        context.dataStore.edit {
            it[PreferencesKeys.CROSSFADE_ENABLED] = enabled
            it[PreferencesKeys.CROSSFADE_DURATION_SEC] = durationSec
        }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.GAPLESS_PLAYBACK] = enabled }
    }

    suspend fun setPauseOnHeadphoneDisconnect(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.PAUSE_ON_HEADPHONE_DISCONNECT] = enabled }
    }

    suspend fun setResumeOnLaunch(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.RESUME_ON_LAUNCH] = enabled }
    }

    suspend fun setLockscreenArtwork(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.LOCKSCREEN_ARTWORK] = enabled }
    }

    suspend fun setAutoScan(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_SCAN] = enabled }
    }

    suspend fun setFilterShortTracks(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.FILTER_SHORT_TRACKS] = enabled }
    }

    suspend fun setVesperThemeMode(themeMode: String) {
        context.dataStore.edit { it[PreferencesKeys.VESPER_THEME_MODE] = themeMode }
    }

    suspend fun setDirectSourceStreamEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DIRECT_SOURCE_STREAM_ENABLED] = enabled }
    }

    suspend fun setWifiStreamingQuality(quality: String) {
        context.dataStore.edit { it[PreferencesKeys.WIFI_STREAMING_QUALITY] = quality }
    }

    suspend fun setMobileStreamingQuality(quality: String) {
        context.dataStore.edit { it[PreferencesKeys.MOBILE_STREAMING_QUALITY] = quality }
    }

    suspend fun setWifiOnlyDownloads(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.WIFI_ONLY_DOWNLOADS] = enabled }
    }

    suspend fun setDisableEffectsOnSpeaker(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DISABLE_EFFECTS_ON_SPEAKER] = enabled }
    }

    suspend fun setLoudnessNormalizationEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[PreferencesKeys.LOUDNESS_NORMALIZATION_ENABLED] = enabled
            it[PreferencesKeys.PREAMP_HEADROOM_ENABLED] = enabled
        }
    }

    suspend fun setHideRecentsOnHome(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HIDE_RECENTS_ON_HOME] = enabled }
    }

    suspend fun setHideMoodPicksOnHome(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HIDE_MOOD_PICKS_ON_HOME] = enabled }
    }

    suspend fun setHideTopicPicksOnHome(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HIDE_TOPIC_PICKS_ON_HOME] = enabled }
    }

    suspend fun setReduceAnimations(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.REDUCE_ANIMATIONS] = enabled }
    }

    suspend fun setReduceDynamicBlur(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.REDUCE_DYNAMIC_BLUR] = enabled }
    }

    suspend fun setFullScreenCoverArt(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.FULL_SCREEN_COVER_ART] = enabled }
    }

    suspend fun setSyncedLyricsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SYNCED_LYRICS_ENABLED] = enabled }
    }

    suspend fun setBlurUnfocusedLyrics(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BLUR_UNFOCUSED_LYRICS] = enabled }
    }

    suspend fun savePlaybackState(
        trackId: String?,
        positionMs: Long,
        queueIdsCsv: String,
        queueIndex: Int,
        shuffleMode: Boolean,
        repeatMode: Int
    ) {
        context.dataStore.edit {
            if (trackId != null) it[PreferencesKeys.LAST_PLAYED_TRACK_ID] = trackId
            it[PreferencesKeys.LAST_PLAYBACK_POSITION_MS] = positionMs
            it[PreferencesKeys.LAST_QUEUE_TRACK_IDS_CSV] = queueIdsCsv
            it[PreferencesKeys.LAST_QUEUE_INDEX] = queueIndex
            it[PreferencesKeys.LAST_SHUFFLE_MODE] = shuffleMode
            it[PreferencesKeys.LAST_REPEAT_MODE] = repeatMode
        }
    }

    suspend fun setIsSupporter(isSupporter: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.IS_SUPPORTER] = isSupporter }
    }

    suspend fun resetSettingsToDefaults() {
        context.dataStore.edit {
            it[PreferencesKeys.SORT_MODE] = "TITLE_AZ"
            it[PreferencesKeys.EQ_ENABLED] = false
            it[PreferencesKeys.ACTIVE_SOUND_PROFILE] = "BALANCED"
            it[PreferencesKeys.ACTIVE_PRESET] = "Balanced"
            it[PreferencesKeys.EQ_BAND_LEVELS_CSV] = ""
            it[PreferencesKeys.BASS_BOOST_STRENGTH] = 0
            it[PreferencesKeys.PREAMP_HEADROOM_ENABLED] = true
            it[PreferencesKeys.AUDIO_BALANCE] = 0f
            it[PreferencesKeys.CROSSFADE_ENABLED] = false
            it[PreferencesKeys.CROSSFADE_DURATION_SEC] = 4
            it[PreferencesKeys.GAPLESS_PLAYBACK] = true
            it[PreferencesKeys.PAUSE_ON_HEADPHONE_DISCONNECT] = true
            it[PreferencesKeys.RESUME_ON_LAUNCH] = false
            it[PreferencesKeys.LOCKSCREEN_ARTWORK] = true
            it[PreferencesKeys.AUTO_SCAN] = true
            it[PreferencesKeys.FILTER_SHORT_TRACKS] = true
            it[PreferencesKeys.VESPER_THEME_MODE] = "Nocturne"
            it[PreferencesKeys.DIRECT_SOURCE_STREAM_ENABLED] = true
            it[PreferencesKeys.WIFI_STREAMING_QUALITY] = "High (320 kbps)"
            it[PreferencesKeys.MOBILE_STREAMING_QUALITY] = "Balanced (192 kbps)"
            it[PreferencesKeys.WIFI_ONLY_DOWNLOADS] = false
            it[PreferencesKeys.DISABLE_EFFECTS_ON_SPEAKER] = false
            it[PreferencesKeys.LOUDNESS_NORMALIZATION_ENABLED] = true
            it[PreferencesKeys.HIDE_RECENTS_ON_HOME] = false
            it[PreferencesKeys.HIDE_MOOD_PICKS_ON_HOME] = false
            it[PreferencesKeys.HIDE_TOPIC_PICKS_ON_HOME] = false
            it[PreferencesKeys.REDUCE_ANIMATIONS] = false
            it[PreferencesKeys.REDUCE_DYNAMIC_BLUR] = false
            it[PreferencesKeys.FULL_SCREEN_COVER_ART] = false
            it[PreferencesKeys.SYNCED_LYRICS_ENABLED] = true
            it[PreferencesKeys.BLUR_UNFOCUSED_LYRICS] = true
        }
    }
}
