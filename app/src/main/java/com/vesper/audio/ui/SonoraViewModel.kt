package com.vesper.audio.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vesper.audio.SonoraApplication
import com.vesper.audio.data.MusicRepository
import com.vesper.audio.data.local.CustomEqPresetEntity
import com.vesper.audio.data.local.ScannedFolderEntity
import com.vesper.audio.data.local.VesperPreferencesRepository
import com.vesper.audio.data.remote.AudiusApiClient
import com.vesper.audio.data.remote.AudiusTrack
import com.vesper.audio.data.remote.MusicBrainzClient
import com.vesper.audio.model.Album
import com.vesper.audio.model.Artist
import com.vesper.audio.model.Playlist
import com.vesper.audio.model.Track
import com.vesper.audio.playback.SonoraPlayerClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class MainDestination {
    HOME,
    SONGS,
    ALBUMS,
    PLAYLISTS,
    DISCOVER;

    companion object {
        val SEARCH = DISCOVER
        val ARTISTS = PLAYLISTS
    }
}

enum class VesperFullScreenDestination {
    NONE,
    NOW_PLAYING,
    EQUALIZER,
    MODES,
    TONES,
    DONATE,
    METADATA_STUDIO,
    SETTINGS,
    ABOUT,
    SCAN_MEDIA,
    QUEUE,
    ALBUM_DETAILS,
    ARTIST_DETAILS,
    PLAYLIST_DETAILS,
    FAVORITES
}

enum class LibraryTab {
    ALL,
    ALBUMS,
    ARTISTS,
    PLAYLISTS,
    FOLDERS
}

enum class SortMode(val displayName: String) {
    TITLE_AZ("Title (A-Z)"),
    TITLE_ZA("Title (Z-A)"),
    ARTIST_AZ("Artist (A-Z)"),
    ALBUM_AZ("Album (A-Z)"),
    RECENTLY_ADDED("Recently Added"),
    OLDEST_ADDED("Oldest Added"),
    DURATION_DESC("Duration (Longest)"),
    DURATION_ASC("Duration (Shortest)");

    companion object {
        val TITLE get() = TITLE_AZ
        val ARTIST get() = ARTIST_AZ
        val ALBUM get() = ALBUM_AZ
        val DURATION get() = DURATION_DESC
        val DATE_ADDED get() = RECENTLY_ADDED
    }
}

enum class SoundProfile(
    val title: String,
    val subtitle: String,
    val bassBoost: Short,
    val description: String
) {
    BALANCED("Balanced", "A natural, well-rounded sound", 0, "Flat reference response preserving studio master sound"),
    DEEP_BASS("Deep Bass", "Rumbling sub-bass extension", 700, "Emphasizes frequencies below 80Hz for electronic and hip-hop"),
    BASS_BOOST("Bass Boost", "Deeper and more impactful lows", 550, "Amplifies sub-bass rumble and punchy kick drums"),
    VOCAL("Vocal Clarity", "Clear and forward vocals", 100, "Brings mid-range vocals and spoken dialogue to the front"),
    ACOUSTIC("Acoustic", "Warm and natural tones", 150, "Optimized for strings, pianos and organic recordings"),
    CLASSICAL("Classical", "Detailed and dynamic range", 100, "Maximizes dynamic headroom across symphonic movements"),
    ROCK("Rock", "Energetic drive and crunch", 350, "Sharp electric guitar presence with aggressive low-mid punch"),
    JAZZ("Jazz", "Silky warmth and smooth brass", 200, "Full upright bass, lush mids, and delicate cymbal air"),
    POP("Pop", "Bright, radio-ready modern mix", 300, "Polished top-end with controlled modern kick punch"),
    ELECTRONIC("Electronic", "Synthesized lows and crystal highs", 500, "V-shaped curve for synthesizers, club tracks, and EDM"),
    CINEMATIC("Cinematic", "Expansive wide dynamic range", 300, "Wide orchestral staging and deep theatrical low frequencies"),
    NIGHT("Night Listening", "Smoother sound for late listening", 0, "Softens aggressive highs and limits dynamic bursts"),
    STUDIO_REFERENCE("Studio Reference", "Neutral frequency response", 0, "App effects bypass with zero EQ coloration"),
    WARM_ANALOG("Warm Analog", "Gentle warmth and silky highs", 200, "Softened treble with rich low-mid warmth and natural vocals"),
    CUSTOM("Custom", "User tailored audio tuning", 0, "Fully customizable multi-band equalizer adjustments")
}

class SonoraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MusicRepository = (application as SonoraApplication).musicRepository
    val preferencesRepository = VesperPreferencesRepository(application)
    val playerClient = SonoraPlayerClient(application, viewModelScope)

    val isLoading: StateFlow<Boolean> = repository.isLoading
    val isSyncing: StateFlow<Boolean> = repository.isSyncing
    val lastSyncSummary: StateFlow<String> = repository.lastSyncSummary
    val tracks: StateFlow<List<Track>> = repository.tracks
    val albums: StateFlow<List<Album>> = repository.albums
    val artists: StateFlow<List<Artist>> = repository.artists
    val favoriteTracks: StateFlow<List<Track>> = repository.favoriteTracks
    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val scannedFolders: StateFlow<List<ScannedFolderEntity>> = repository.scannedFolders
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val recentlyAddedTracks: StateFlow<List<Track>> = repository.recentlyAddedTracks

    // Playback States from SonoraPlayerClient
    val currentTrack: StateFlow<Track?> = playerClient.currentTrack
    val isPlaying: StateFlow<Boolean> = playerClient.isPlaying
    val isBuffering: StateFlow<Boolean> = playerClient.isBuffering
    val playbackPositionMs: StateFlow<Long> = playerClient.playbackPositionMs
    val durationMs: StateFlow<Long> = playerClient.durationMs
    val shuffleMode: StateFlow<Boolean> = playerClient.shuffleMode
    val repeatMode: StateFlow<Int> = playerClient.repeatMode
    val playbackQueue: StateFlow<List<Track>> = playerClient.queue

    // Queue Source Label
    private val _queueSource = MutableStateFlow("All Songs")
    val queueSource: StateFlow<String> = _queueSource.asStateFlow()

    // Favourites StateFlow
    private val _optimisticFavorites = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val favoriteTrackIds: StateFlow<Set<String>> = combine(repository.favoriteTracks, _optimisticFavorites) { favList, optimistic ->
        val baseSet = favList.map { it.id }.toMutableSet()
        optimistic.forEach { (id, isFav) ->
            if (isFav) baseSet.add(id) else baseSet.remove(id)
        }
        baseSet
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val localRepository = (application as SonoraApplication).localRepository

    val customEqPresets: StateFlow<List<CustomEqPresetEntity>> = localRepository.customEqPresets
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isSupporter = MutableStateFlow(false)
    val isSupporter: StateFlow<Boolean> = _isSupporter.asStateFlow()

    // Navigation state
    private val _currentDestination = MutableStateFlow(MainDestination.HOME)
    val currentDestination: StateFlow<MainDestination> = _currentDestination.asStateFlow()

    private val _fullScreenDestination = MutableStateFlow(VesperFullScreenDestination.NONE)
    val fullScreenDestination: StateFlow<VesperFullScreenDestination> = _fullScreenDestination.asStateFlow()

    // Backward-compatible flags
    val isNowPlayingExpanded: StateFlow<Boolean> = _fullScreenDestination
        .combine(MutableStateFlow(Unit)) { dest, _ -> dest == VesperFullScreenDestination.NOW_PLAYING }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val showEqualizer: StateFlow<Boolean> = _fullScreenDestination
        .combine(MutableStateFlow(Unit)) { dest, _ -> dest == VesperFullScreenDestination.EQUALIZER }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val showQueueSheet: StateFlow<Boolean> = _fullScreenDestination
        .combine(MutableStateFlow(Unit)) { dest, _ -> dest == VesperFullScreenDestination.QUEUE }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    // Audio Effects & EQ
    val audioEffectController = com.vesper.audio.playback.SonoraMediaService.audioEffectController
    val eqEnabled: StateFlow<Boolean> = audioEffectController.isEnabled
    val eqBands: StateFlow<List<com.vesper.audio.audio.EqBand>> = audioEffectController.bands
    val eqBandRange: StateFlow<Pair<Short, Short>> = audioEffectController.bandLevelRange
    val activePreset: StateFlow<String> = audioEffectController.activePresetName
    val bassBoostStrength: StateFlow<Short> = audioEffectController.bassBoostStrength
    val preampHeadroomEnabled: StateFlow<Boolean> = audioEffectController.headroomProtectionEnabled
    val audioBalance: StateFlow<Float> = audioEffectController.audioBalance

    // Active Sound Profile
    private val _activeSoundProfile = MutableStateFlow(SoundProfile.BALANCED)
    val activeSoundProfile: StateFlow<SoundProfile> = _activeSoundProfile.asStateFlow()

    // Metadata Studio State
    private val _trackForMetadata = MutableStateFlow<Track?>(null)
    val trackForMetadata: StateFlow<Track?> = _trackForMetadata.asStateFlow()

    private val _currentLibraryTab = MutableStateFlow(LibraryTab.ALL)
    val currentLibraryTab: StateFlow<LibraryTab> = _currentLibraryTab.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.TITLE)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _discoverSearchQuery = MutableStateFlow("")
    val discoverSearchQuery: StateFlow<String> = _discoverSearchQuery.asStateFlow()

    // Playback & Library Settings
    private val _crossfadeEnabled = MutableStateFlow(false)
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled.asStateFlow()

    private val _crossfadeDurationSec = MutableStateFlow(4)
    val crossfadeDurationSec: StateFlow<Int> = _crossfadeDurationSec.asStateFlow()

    private val _gaplessPlayback = MutableStateFlow(true)
    val gaplessPlayback: StateFlow<Boolean> = _gaplessPlayback.asStateFlow()

    private val _pauseOnHeadphoneDisconnect = MutableStateFlow(true)
    val pauseOnHeadphoneDisconnect: StateFlow<Boolean> = _pauseOnHeadphoneDisconnect.asStateFlow()

    private val _resumeOnLaunch = MutableStateFlow(false)
    val resumeOnLaunch: StateFlow<Boolean> = _resumeOnLaunch.asStateFlow()

    private val _lockscreenArtwork = MutableStateFlow(true)
    val lockscreenArtwork: StateFlow<Boolean> = _lockscreenArtwork.asStateFlow()

    private val _autoScanStartup = MutableStateFlow(true)
    val autoScanStartup: StateFlow<Boolean> = _autoScanStartup.asStateFlow()

    val filterShortTracks: StateFlow<Boolean> = repository.filterShortTracks

    // Extended VESPER Block-Style Settings StateFlows
    private val _vesperThemeMode = MutableStateFlow("Nocturne")
    val vesperThemeMode: StateFlow<String> = _vesperThemeMode.asStateFlow()

    private val _directSourceStreamEnabled = MutableStateFlow(true)
    val directSourceStreamEnabled: StateFlow<Boolean> = _directSourceStreamEnabled.asStateFlow()

    private val _wifiStreamingQuality = MutableStateFlow("High (320 kbps)")
    val wifiStreamingQuality: StateFlow<String> = _wifiStreamingQuality.asStateFlow()

    private val _mobileStreamingQuality = MutableStateFlow("Balanced (192 kbps)")
    val mobileStreamingQuality: StateFlow<String> = _mobileStreamingQuality.asStateFlow()

    private val _wifiOnlyDownloads = MutableStateFlow(false)
    val wifiOnlyDownloads: StateFlow<Boolean> = _wifiOnlyDownloads.asStateFlow()

    private val _disableEffectsOnSpeaker = MutableStateFlow(false)
    val disableEffectsOnSpeaker: StateFlow<Boolean> = _disableEffectsOnSpeaker.asStateFlow()

    private val _loudnessNormalizationEnabled = MutableStateFlow(true)
    val loudnessNormalizationEnabled: StateFlow<Boolean> = _loudnessNormalizationEnabled.asStateFlow()

    private val _hideRecentsOnHome = MutableStateFlow(false)
    val hideRecentsOnHome: StateFlow<Boolean> = _hideRecentsOnHome.asStateFlow()

    private val _hideMoodPicksOnHome = MutableStateFlow(false)
    val hideMoodPicksOnHome: StateFlow<Boolean> = _hideMoodPicksOnHome.asStateFlow()

    private val _hideTopicPicksOnHome = MutableStateFlow(false)
    val hideTopicPicksOnHome: StateFlow<Boolean> = _hideTopicPicksOnHome.asStateFlow()

    private val _reduceAnimations = MutableStateFlow(false)
    val reduceAnimations: StateFlow<Boolean> = _reduceAnimations.asStateFlow()

    private val _reduceDynamicBlur = MutableStateFlow(false)
    val reduceDynamicBlur: StateFlow<Boolean> = _reduceDynamicBlur.asStateFlow()

    private val _fullScreenCoverArt = MutableStateFlow(false)
    val fullScreenCoverArt: StateFlow<Boolean> = _fullScreenCoverArt.asStateFlow()

    private val _syncedLyricsEnabled = MutableStateFlow(true)
    val syncedLyricsEnabled: StateFlow<Boolean> = _syncedLyricsEnabled.asStateFlow()

    private val _blurUnfocusedLyrics = MutableStateFlow(true)
    val blurUnfocusedLyrics: StateFlow<Boolean> = _blurUnfocusedLyrics.asStateFlow()

    // Multi-Selection State
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedTrackIds: StateFlow<Set<String>> = _selectedTrackIds.asStateFlow()

    // Contextual Dialog States
    private val _trackForActionSheet = MutableStateFlow<Track?>(null)
    val trackForActionSheet: StateFlow<Track?> = _trackForActionSheet.asStateFlow()

    private val _trackForRename = MutableStateFlow<Track?>(null)
    val trackForRename: StateFlow<Track?> = _trackForRename.asStateFlow()

    private val _trackForDetails = MutableStateFlow<Track?>(null)
    val trackForDetails: StateFlow<Track?> = _trackForDetails.asStateFlow()

    private val _tracksForDeleteConfirm = MutableStateFlow<List<Track>?>(null)
    val tracksForDeleteConfirm: StateFlow<List<Track>?> = _tracksForDeleteConfirm.asStateFlow()

    // Playlist dialogs
    private val _trackForPlaylist = MutableStateFlow<Track?>(null)
    val trackForPlaylist: StateFlow<Track?> = _trackForPlaylist.asStateFlow()

    private val _showCreatePlaylistDialog = MutableStateFlow(false)
    val showCreatePlaylistDialog: StateFlow<Boolean> = _showCreatePlaylistDialog.asStateFlow()

    // Sorted tracks (8 deterministic sort modes)
    val sortedTracks: StateFlow<List<Track>> = combine(tracks, _sortMode) { trackList, sort ->
        when (sort) {
            SortMode.TITLE_AZ -> trackList.sortedWith(compareBy({ it.title.lowercase() }, { it.id }))
            SortMode.TITLE_ZA -> trackList.sortedWith(compareByDescending<Track> { it.title.lowercase() }.thenBy { it.id })
            SortMode.ARTIST_AZ -> trackList.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }, { it.id }))
            SortMode.ALBUM_AZ -> trackList.sortedWith(compareBy({ it.album.lowercase() }, { it.discNumber }, { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }, { it.title.lowercase() }))
            SortMode.RECENTLY_ADDED -> MusicRepository.sortRecentlyAdded(trackList)
            SortMode.OLDEST_ADDED -> trackList.sortedWith(compareBy<Track> { it.dateAdded }.thenBy { it.id.toLongOrNull() ?: 0L }.thenBy { it.title.lowercase() })
            SortMode.DURATION_DESC -> trackList.sortedWith(compareByDescending<Track> { it.durationMs }.thenBy { it.title.lowercase() })
            SortMode.DURATION_ASC -> trackList.sortedWith(compareBy<Track> { it.durationMs }.thenBy { it.title.lowercase() })
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Local search results (debounced and computed off main thread, strictly read-only)
    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<List<Track>> = combine(
        tracks,
        _searchQuery.debounce(200L)
    ) { trackList, query ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val q = query.trim().lowercase()
            trackList.filter {
                it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q)
            }
        }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Album & Artist Details Selection
    private val _selectedAlbum = MutableStateFlow<Album?>(null)
    val selectedAlbum: StateFlow<Album?> = _selectedAlbum.asStateFlow()

    private val _selectedArtist = MutableStateFlow<Artist?>(null)
    val selectedArtist: StateFlow<Artist?> = _selectedArtist.asStateFlow()

    private val _isRemoteDetailMode = MutableStateFlow(false)
    val isRemoteDetailMode: StateFlow<Boolean> = _isRemoteDetailMode.asStateFlow()

    private val _remoteDetailTracks = MutableStateFlow<List<Track>>(emptyList())
    val remoteDetailTracks: StateFlow<List<Track>> = _remoteDetailTracks.asStateFlow()

    private val _remoteDetailAlbums = MutableStateFlow<List<Album>>(emptyList())
    val remoteDetailAlbums: StateFlow<List<Album>> = _remoteDetailAlbums.asStateFlow()

    private val _isLoadingRemoteDetails = MutableStateFlow(false)
    val isLoadingRemoteDetails: StateFlow<Boolean> = _isLoadingRemoteDetails.asStateFlow()
    private var remoteDetailJob: Job? = null

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    val playlistTracks: StateFlow<List<Track>> = _selectedPlaylist.flatMapLatest { playlist ->
        if (playlist == null) flowOf(emptyList())
        else repository.getTracksForPlaylist(playlist.id)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val audiusApiClient = AudiusApiClient()
    val discoverRepository = com.vesper.audio.data.remote.DiscoverRepository(
        localRepository = localRepository,
        audiusProvider = com.vesper.audio.data.remote.AudiusDiscoverProvider(audiusApiClient)
    )
    val musicBrainzClient = MusicBrainzClient()

    private val _selectedDiscoverProviderId = MutableStateFlow("audius")
    val selectedDiscoverProviderId: StateFlow<String> = _selectedDiscoverProviderId.asStateFlow()

    private val _audiusTrendingTracks = MutableStateFlow<List<AudiusTrack>>(emptyList())
    val audiusTrendingTracks: StateFlow<List<AudiusTrack>> = _audiusTrendingTracks.asStateFlow()

    private val _audiusSearchResults = MutableStateFlow<List<AudiusTrack>>(emptyList())
    val audiusSearchResults: StateFlow<List<AudiusTrack>> = _audiusSearchResults.asStateFlow()

    private val _isSearchingAudius = MutableStateFlow(false)
    val isSearchingAudius: StateFlow<Boolean> = _isSearchingAudius.asStateFlow()

    private val _isLoadingMoreAudius = MutableStateFlow(false)
    val isLoadingMoreAudius: StateFlow<Boolean> = _isLoadingMoreAudius.asStateFlow()

    private val _hasMoreAudius = MutableStateFlow(true)
    val hasMoreAudius: StateFlow<Boolean> = _hasMoreAudius.asStateFlow()

    private val _audiusError = MutableStateFlow<String?>(null)
    val audiusError: StateFlow<String?> = _audiusError.asStateFlow()

    private val _isDiscoverStale = MutableStateFlow(false)
    val isDiscoverStale: StateFlow<Boolean> = _isDiscoverStale.asStateFlow()

    private val _isDiscoverFromCache = MutableStateFlow(false)
    val isDiscoverFromCache: StateFlow<Boolean> = _isDiscoverFromCache.asStateFlow()

    private val _discoverCacheCount = MutableStateFlow(0)
    val discoverCacheCount: StateFlow<Int> = _discoverCacheCount.asStateFlow()

    private var audiusSearchJob: Job? = null
    private var audiusPaginationJob: Job? = null
    private var discoverRequestGen: Long = 0L
    private val audiusPageSize = 50
    private var discoverSessionSeed: Long = System.currentTimeMillis()

    fun selectDiscoverProvider(providerId: String) {
        val enabled = discoverRepository.enabledProviders.firstOrNull { it.providerId.equals(providerId, ignoreCase = true) }
        if (enabled != null) {
            audiusSearchJob?.cancel()
            audiusPaginationJob?.cancel()
            discoverRequestGen++
            _isLoadingMoreAudius.value = false
            _selectedDiscoverProviderId.value = enabled.providerId
            val activeQuery = _discoverSearchQuery.value.trim()
            if (activeQuery.isNotBlank()) {
                searchAudius(activeQuery)
            } else {
                loadAudiusTrending(forceRefresh = false)
            }
        }
    }

    fun refreshDiscoverCacheCount() {
        viewModelScope.launch {
            _discoverCacheCount.value = discoverRepository.getCacheEntryCount()
        }
    }

    fun clearDiscoverCache() {
        viewModelScope.launch {
            discoverRepository.clearCache()
            _discoverCacheCount.value = 0
            _isDiscoverFromCache.value = false
            _isDiscoverStale.value = false
        }
    }

    private fun preloadDiscoverCache() {
        viewModelScope.launch {
            val providerId = _selectedDiscoverProviderId.value
            val cached = discoverRepository.peekCachedTrending(providerId = providerId)
            if (cached != null && cached.tracks.isNotEmpty() && _audiusTrendingTracks.value.isEmpty()) {
                val mapped = cached.tracks.map { it.toAudiusTrack() }
                _audiusTrendingTracks.value = AudiusApiClient.shuffleForSession(mapped, discoverSessionSeed)
                _hasMoreAudius.value = cached.tracks.size >= 25
                _isDiscoverFromCache.value = true
                _isDiscoverStale.value = cached.isStale
            }
            _discoverCacheCount.value = discoverRepository.getCacheEntryCount()
        }
    }

    fun loadAudiusTrending(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val providerId = _selectedDiscoverProviderId.value
            if (!forceRefresh && _audiusTrendingTracks.value.isNotEmpty() && !_isDiscoverStale.value) {
                return@launch
            }
            if (forceRefresh) {
                discoverSessionSeed = System.currentTimeMillis()
            }

            // 1. Emit cached tracks immediately so Discover renders on the first frame without waiting on network
            if (!forceRefresh && _audiusTrendingTracks.value.isEmpty()) {
                val cached = discoverRepository.peekCachedTrending(providerId = providerId)
                if (cached != null && cached.tracks.isNotEmpty() && _selectedDiscoverProviderId.value == providerId) {
                    val mapped = cached.tracks.map { it.toAudiusTrack() }
                    _audiusTrendingTracks.value = AudiusApiClient.shuffleForSession(mapped, discoverSessionSeed)
                    _hasMoreAudius.value = cached.tracks.size >= 25
                    _isDiscoverFromCache.value = true
                    _isDiscoverStale.value = cached.isStale
                    if (!cached.isStale) {
                        _isSearchingAudius.value = false
                        return@launch
                    }
                }
            }

            // 2. Fetch or revalidate in background (only show full loading spinner if list is still empty)
            _isSearchingAudius.value = _audiusTrendingTracks.value.isEmpty() || forceRefresh
            _audiusError.value = null
            val result = discoverRepository.getTrending(
                providerId = providerId,
                limit = audiusPageSize,
                offset = 0,
                forceRefresh = forceRefresh
            )
            if (_selectedDiscoverProviderId.value == providerId) {
                if (result.tracks.isNotEmpty()) {
                    val mapped = result.tracks.map { it.toAudiusTrack() }
                    _audiusTrendingTracks.value = AudiusApiClient.shuffleForSession(mapped, discoverSessionSeed)
                    _hasMoreAudius.value = result.tracks.size >= 25
                    _isDiscoverFromCache.value = result.isFromCache
                    _isDiscoverStale.value = result.isStale
                }
                _audiusError.value = result.errorMessage
            }
            _isSearchingAudius.value = false
            refreshDiscoverCacheCount()
        }
    }

    fun searchAudius(query: String) {
        audiusSearchJob?.cancel()
        audiusPaginationJob?.cancel()
        _isLoadingMoreAudius.value = false
        val myGen = ++discoverRequestGen
        val trimmedQuery = query.trim()
        val providerId = _selectedDiscoverProviderId.value

        if (trimmedQuery.isBlank()) {
            _audiusSearchResults.value = emptyList()
            _isSearchingAudius.value = false
            _audiusError.value = null
            _isDiscoverStale.value = false
            _hasMoreAudius.value = _audiusTrendingTracks.value.size >= 25
            return
        }
        audiusSearchJob = viewModelScope.launch {
            delay(250L) // Debounce in-flight remote search
            if (myGen != discoverRequestGen) return@launch
            _isSearchingAudius.value = true
            _audiusError.value = null
            val result = discoverRepository.searchTracks(
                query = trimmedQuery,
                providerId = providerId,
                limit = 30,
                offset = 0
            )
            if (myGen != discoverRequestGen || _selectedDiscoverProviderId.value != providerId) {
                return@launch
            }
            val mapped = result.tracks.map { it.toAudiusTrack() }
            _audiusSearchResults.value = mapped.distinctBy { it.namespacedId }
            _hasMoreAudius.value = result.tracks.size >= 25
            _isDiscoverFromCache.value = result.isFromCache
            _isDiscoverStale.value = result.isStale
            _audiusError.value = result.errorMessage
            _isSearchingAudius.value = false
            refreshDiscoverCacheCount()
        }
    }

    fun loadMoreAudius() {
        if (_isSearchingAudius.value || _isLoadingMoreAudius.value || !_hasMoreAudius.value) return
        val currentQuery = _discoverSearchQuery.value.trim()
        val providerId = _selectedDiscoverProviderId.value
        val myGen = discoverRequestGen

        audiusPaginationJob?.cancel()
        audiusPaginationJob = viewModelScope.launch {
            _isLoadingMoreAudius.value = true
            if (currentQuery.isNotBlank()) {
                val offset = _audiusSearchResults.value.size
                val result = discoverRepository.searchTracks(
                    query = currentQuery,
                    providerId = providerId,
                    limit = 30,
                    offset = offset
                )
                if (myGen == discoverRequestGen && _discoverSearchQuery.value.trim() == currentQuery && _selectedDiscoverProviderId.value == providerId) {
                    if (result.errorMessage == null) {
                        val mapped = result.tracks.map { it.toAudiusTrack() }
                        val combined = (_audiusSearchResults.value + mapped).distinctBy { it.namespacedId }
                        _audiusSearchResults.value = combined
                        _hasMoreAudius.value = result.tracks.size >= 25
                    } else {
                        _audiusError.value = result.errorMessage
                    }
                }
            } else {
                val offset = _audiusTrendingTracks.value.size
                val result = discoverRepository.getTrending(
                    providerId = providerId,
                    limit = audiusPageSize,
                    offset = offset
                )
                if (myGen == discoverRequestGen && _discoverSearchQuery.value.trim().isEmpty() && _selectedDiscoverProviderId.value == providerId) {
                    if (result.errorMessage == null) {
                        val mapped = result.tracks.map { it.toAudiusTrack() }
                        val moreShuffled = AudiusApiClient.shuffleForSession(mapped, discoverSessionSeed + offset)
                        val combined = (_audiusTrendingTracks.value + moreShuffled).distinctBy { it.namespacedId }
                        _audiusTrendingTracks.value = combined
                        _hasMoreAudius.value = result.tracks.size >= 25
                    } else {
                        _audiusError.value = result.errorMessage
                    }
                }
            }
            _isLoadingMoreAudius.value = false
        }
    }

    private val navBackStack = ArrayDeque<VesperFullScreenDestination>()
    private var isPlaybackRestored = false

    init {
        preloadDiscoverCache()
        // 1. Restore User Preferences from DataStore
        viewModelScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                _isSupporter.value = prefs.isSupporter

                _sortMode.value = try {
                    SortMode.valueOf(prefs.sortMode)
                } catch (_: Exception) {
                    SortMode.TITLE_AZ
                }

                _crossfadeEnabled.value = prefs.crossfadeEnabled
                _crossfadeDurationSec.value = prefs.crossfadeDurationSec
                playerClient.setCrossfade(prefs.crossfadeEnabled, prefs.crossfadeDurationSec)

                _gaplessPlayback.value = prefs.gaplessPlayback
                playerClient.setGaplessPlayback(prefs.gaplessPlayback)
                _pauseOnHeadphoneDisconnect.value = prefs.pauseOnHeadphoneDisconnect
                _resumeOnLaunch.value = prefs.resumeOnLaunch
                _lockscreenArtwork.value = prefs.lockscreenArtwork
                _autoScanStartup.value = prefs.autoScan
                repository.setFilterShortTracks(prefs.filterShortTracks)

                _vesperThemeMode.value = prefs.vesperThemeMode
                _directSourceStreamEnabled.value = prefs.directSourceStreamEnabled
                _wifiStreamingQuality.value = prefs.wifiStreamingQuality
                _mobileStreamingQuality.value = prefs.mobileStreamingQuality
                _wifiOnlyDownloads.value = prefs.wifiOnlyDownloads
                _disableEffectsOnSpeaker.value = prefs.disableEffectsOnSpeaker
                _loudnessNormalizationEnabled.value = prefs.loudnessNormalizationEnabled
                _hideRecentsOnHome.value = prefs.hideRecentsOnHome
                _hideMoodPicksOnHome.value = prefs.hideMoodPicksOnHome
                _hideTopicPicksOnHome.value = prefs.hideTopicPicksOnHome
                _reduceAnimations.value = prefs.reduceAnimations
                _reduceDynamicBlur.value = prefs.reduceDynamicBlur
                _fullScreenCoverArt.value = prefs.fullScreenCoverArt
                _syncedLyricsEnabled.value = prefs.syncedLyricsEnabled
                _blurUnfocusedLyrics.value = prefs.blurUnfocusedLyrics

                // Restore EQ & Basic Mode state without clobbering activePresetName to "Custom"
                audioEffectController.setSpeakerBypass(prefs.disableEffectsOnSpeaker, true)
                audioEffectController.setEnabled(prefs.eqEnabled)
                audioEffectController.toggleHeadroomProtection(prefs.preampHeadroomEnabled)
                audioEffectController.setAudioBalance(prefs.audioBalance)

                val savedPreset = prefs.activePreset.ifBlank { "Balanced" }
                val isBuiltIn = com.vesper.audio.audio.AudioEffectController.BUILT_IN_MODES.any {
                    it.name.equals(savedPreset, ignoreCase = true)
                }
                if (isBuiltIn && !savedPreset.equals("Custom", ignoreCase = true)) {
                    audioEffectController.applyPreset(savedPreset)
                } else if (prefs.eqBandLevelsCsv.isNotBlank()) {
                    audioEffectController.applyCustomPreset(
                        name = savedPreset,
                        bandLevelsCsv = prefs.eqBandLevelsCsv,
                        bassBoostStrength = prefs.bassBoostStrength
                    )
                } else {
                    audioEffectController.applyPreset("Balanced")
                }

                _activeSoundProfile.value = mapPresetToSoundProfile(audioEffectController.activePresetName.value, prefs.activeSoundProfile)
                playerClient.applyPreampScalar(audioEffectController.preampGainScalar.value)
            }
        }

        // 2. Playback Restoration once tracks are loaded
        viewModelScope.launch {
            tracks.collect { allTracks ->
                if (!isPlaybackRestored && allTracks.isNotEmpty()) {
                    isPlaybackRestored = true
                    try {
                        val prefs = preferencesRepository.userPreferencesFlow.first()
                        val lastTrackId = prefs.lastPlayedTrackId
                        if (lastTrackId != null && currentTrack.value == null) {
                            val queueIds = prefs.lastQueueTrackIdsCsv.split(",").filter { it.isNotBlank() }
                            val trackMap = allTracks.associateBy { it.id }
                            val restoredQueue = if (queueIds.isNotEmpty()) {
                                queueIds.mapNotNull { trackMap[it] }
                            } else {
                                allTracks
                            }
                            val targetTrack = restoredQueue.getOrNull(prefs.lastQueueIndex)?.takeIf { it.id == lastTrackId }
                                ?: allTracks.find { it.id == lastTrackId }
                            if (targetTrack != null) {
                                val position = prefs.lastPlaybackPositionMs
                                val shouldPlay = prefs.resumeOnLaunch
                                playerClient.restorePlayback(
                                    track = targetTrack,
                                    restoredQueue = restoredQueue.ifEmpty { listOf(targetTrack) },
                                    positionMs = position,
                                    shouldPlay = shouldPlay
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // 3. Periodic and State-Triggered Playback State Persistence
        viewModelScope.launch {
            while (isActive) {
                delay(4000)
                val track = playerClient.currentTrack.value
                if (track != null) {
                    val pos = playerClient.playbackPositionMs.value
                    val q = playerClient.queue.value
                    val qCsv = q.joinToString(",") { it.id }
                    val qIndex = q.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                    preferencesRepository.savePlaybackState(
                        trackId = track.id,
                        positionMs = pos,
                        queueIdsCsv = qCsv,
                        queueIndex = qIndex,
                        shuffleMode = playerClient.shuffleMode.value,
                        repeatMode = playerClient.repeatMode.value
                    )
                }
            }
        }
    }

    // Navigation Methods
    fun setDestination(dest: MainDestination) {
        _currentDestination.value = dest
        _fullScreenDestination.value = VesperFullScreenDestination.NONE
        navBackStack.clear()
    }

    fun navigateTo(dest: VesperFullScreenDestination) {
        if (_fullScreenDestination.value != VesperFullScreenDestination.NONE) {
            navBackStack.addLast(_fullScreenDestination.value)
        }
        _fullScreenDestination.value = dest
    }

    fun navigateBack(): Boolean {
        if (navBackStack.isNotEmpty()) {
            _fullScreenDestination.value = navBackStack.removeLast()
            return true
        } else if (_fullScreenDestination.value != VesperFullScreenDestination.NONE) {
            _fullScreenDestination.value = VesperFullScreenDestination.NONE
            return true
        }
        return false
    }

    fun openAlbumDetails(album: Album, isRemote: Boolean = false) {
        _isRemoteDetailMode.value = isRemote
        _selectedAlbum.value = album
        remoteDetailJob?.cancel()
        if (isRemote) {
            val seedDiscoverTracks = (_audiusSearchResults.value + _audiusTrendingTracks.value)
                .distinctBy { it.namespacedId }
                .map { it.toDiscoverTrack() }
            val normAlbum = com.vesper.audio.data.remote.DiscoverComparisonUtils.normalizeKey(album.name)
            val normArtist = com.vesper.audio.data.remote.DiscoverComparisonUtils.normalizeKey(album.artist)
            val initialMatches = seedDiscoverTracks.filter {
                com.vesper.audio.data.remote.DiscoverComparisonUtils.normalizeKey(it.album) == normAlbum &&
                    (normArtist.isBlank() || normArtist == "unknown artist" || com.vesper.audio.data.remote.DiscoverComparisonUtils.normalizeKey(it.artist) == normArtist)
            }.map { it.toTrack() }
            _remoteDetailTracks.value = initialMatches
            _isLoadingRemoteDetails.value = true
            remoteDetailJob = viewModelScope.launch {
                val fullDiscoverTracks = discoverRepository.fetchCompleteAlbumTracklist(
                    albumName = album.name,
                    artistName = album.artist,
                    seedTracks = seedDiscoverTracks,
                    providerId = _selectedDiscoverProviderId.value
                )
                val fullMapped = fullDiscoverTracks.map { it.toTrack() }
                if (fullMapped.isNotEmpty()) {
                    _remoteDetailTracks.value = fullMapped
                    val bestYear = fullMapped.firstOrNull { it.year > 0 }?.year ?: album.year
                    _selectedAlbum.value = album.copy(
                        trackCount = fullMapped.size,
                        year = bestYear,
                        artworkUri = album.artworkUri ?: fullMapped.firstNotNullOfOrNull { it.artworkUri }
                    )
                }
                _isLoadingRemoteDetails.value = false
            }
        } else {
            _remoteDetailTracks.value = emptyList()
            _isLoadingRemoteDetails.value = false
        }
        navigateTo(VesperFullScreenDestination.ALBUM_DETAILS)
    }

    fun openArtistDetails(artist: Artist, isRemote: Boolean = false) {
        _isRemoteDetailMode.value = isRemote
        _selectedArtist.value = artist
        remoteDetailJob?.cancel()
        if (isRemote) {
            val seedDiscoverTracks = (_audiusSearchResults.value + _audiusTrendingTracks.value)
                .distinctBy { it.namespacedId }
                .map { it.toDiscoverTrack() }
            val normArtist = com.vesper.audio.data.remote.DiscoverComparisonUtils.normalizeKey(artist.name)
            val initialArtistDiscover = seedDiscoverTracks.filter {
                com.vesper.audio.data.remote.DiscoverComparisonUtils.normalizeKey(it.artist) == normArtist
            }
            _remoteDetailTracks.value = initialArtistDiscover.map { it.toTrack() }
            _remoteDetailAlbums.value = com.vesper.audio.data.remote.DiscoverComparisonUtils.deriveAlbumsFromDiscoverTracks(
                initialArtistDiscover,
                discoverRepository.getBrokenArtworkUrlsSnapshot()
            )
            _isLoadingRemoteDetails.value = true
            remoteDetailJob = viewModelScope.launch {
                val artistDetail = discoverRepository.fetchCompleteArtistCatalogue(
                    artistName = artist.name,
                    seedTracks = seedDiscoverTracks,
                    providerId = _selectedDiscoverProviderId.value
                )
                if (artistDetail.tracks.isNotEmpty()) {
                    val mappedTracks = artistDetail.tracks.map { it.toTrack() }
                    _remoteDetailTracks.value = mappedTracks
                    _remoteDetailAlbums.value = artistDetail.albums
                    _selectedArtist.value = artist.copy(
                        trackCount = mappedTracks.size,
                        albumCount = artistDetail.albums.size.coerceAtLeast(1)
                    )
                }
                _isLoadingRemoteDetails.value = false
            }
        } else {
            _remoteDetailTracks.value = emptyList()
            _remoteDetailAlbums.value = emptyList()
            _isLoadingRemoteDetails.value = false
        }
        navigateTo(VesperFullScreenDestination.ARTIST_DETAILS)
    }

    fun openPlaylistDetails(playlist: Playlist) {
        _selectedPlaylist.value = playlist
        navigateTo(VesperFullScreenDestination.PLAYLIST_DETAILS)
    }

    fun openFavorites() {
        navigateTo(VesperFullScreenDestination.FAVORITES)
    }

    // Sleep Timer Controls
    val sleepTimerState = playerClient.sleepTimerController.timerState
    private val _showSleepTimerDialog = MutableStateFlow(false)
    val showSleepTimerDialog: StateFlow<Boolean> = _showSleepTimerDialog.asStateFlow()

    fun openSleepTimerDialog() {
        _showSleepTimerDialog.value = true
    }

    fun closeSleepTimerDialog() {
        _showSleepTimerDialog.value = false
    }

    fun startSleepTimer(minutes: Int, fadeOut: Boolean = true) {
        playerClient.startSleepTimer(minutes, fadeOut)
    }

    fun startEndOfTrackSleepTimer() {
        playerClient.startEndOfTrackSleepTimer()
    }

    fun cancelSleepTimer() {
        playerClient.cancelSleepTimer()
    }

    fun addSleepTimerMinutes(minutes: Int) {
        playerClient.addSleepTimerMinutes(minutes)
    }

    fun rebuildLibraryCache() {
        clearArtworkCache()
        repository.refreshLibrary()
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
        viewModelScope.launch {
            preferencesRepository.setSortMode(mode.name)
        }
    }

    /**
     * Updates local library search query only.
     * Strictly read-only: never triggers remote network calls or modifies active playback.
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Updates remote Discover search query and triggers debounced, generation-tracked provider search.
     * Strictly read-only with respect to active playback.
     */
    fun setDiscoverSearchQuery(query: String) {
        _discoverSearchQuery.value = query
        searchAudius(query)
    }

    fun setLibraryTab(tab: LibraryTab) {
        _currentLibraryTab.value = tab
    }

    fun setShowNowPlaying(show: Boolean) {
        _fullScreenDestination.value = if (show) VesperFullScreenDestination.NOW_PLAYING else VesperFullScreenDestination.NONE
    }

    fun setShowEqualizer(show: Boolean) {
        _fullScreenDestination.value = if (show) VesperFullScreenDestination.EQUALIZER else VesperFullScreenDestination.NONE
    }

    fun setShowQueueSheet(show: Boolean) {
        _fullScreenDestination.value = if (show) VesperFullScreenDestination.QUEUE else VesperFullScreenDestination.NONE
    }

    // Core Playback Operations
    fun playTrack(track: Track, queue: List<Track> = tracks.value, source: String = "All Songs") {
        _queueSource.value = source
        val cleanQueue = queue.distinctBy { it.id }.let { distinct ->
            if (distinct.any { it.id == track.id }) distinct else listOf(track) + distinct
        }
        playerClient.playTrack(track, cleanQueue)
        viewModelScope.launch {
            val qCsv = cleanQueue.joinToString(",") { it.id }
            val qIdx = cleanQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            preferencesRepository.savePlaybackState(
                trackId = track.id,
                positionMs = 0L,
                queueIdsCsv = qCsv,
                queueIndex = qIdx,
                shuffleMode = playerClient.shuffleMode.value,
                repeatMode = playerClient.repeatMode.value
            )
        }
    }

    fun addToQueue(track: Track) {
        playerClient.addToQueue(track)
    }

    fun playNext(track: Track) {
        playerClient.playNext(track)
    }

    fun togglePlayPause() {
        playerClient.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerClient.seekTo(positionMs)
    }

    fun skipNext() {
        playerClient.skipNext()
        persistCurrentQueueState()
    }

    fun skipPrevious() {
        playerClient.skipPrevious()
        persistCurrentQueueState()
    }

    private fun persistCurrentQueueState() {
        val activeTrack = playerClient.currentTrack.value ?: return
        val activeQueue = playerClient.queue.value
        if (activeQueue.isEmpty()) return
        viewModelScope.launch {
            val qCsv = activeQueue.joinToString(",") { it.id }
            val qIdx = activeQueue.indexOfFirst { it.id == activeTrack.id }.coerceAtLeast(0)
            preferencesRepository.savePlaybackState(
                trackId = activeTrack.id,
                positionMs = playerClient.playbackPositionMs.value,
                queueIdsCsv = qCsv,
                queueIndex = qIdx,
                shuffleMode = playerClient.shuffleMode.value,
                repeatMode = playerClient.repeatMode.value
            )
        }
    }

    fun toggleShuffle() {
        playerClient.toggleShuffle()
    }

    fun cycleRepeatMode() {
        playerClient.cycleRepeatMode()
    }

    fun shuffleAll() {
        val all = tracks.value.shuffled()
        if (all.isNotEmpty()) {
            playTrack(all.first(), all, "Shuffle All")
        }
    }

    fun playAlbum(album: Album, shuffle: Boolean = false) {
        val albumTracks = tracks.value.filter { it.album.equals(album.name, ignoreCase = true) }
            .sortedWith(
                compareBy<Track> { it.discNumber.coerceAtLeast(1) }
                    .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                    .thenBy { it.title.lowercase() }
            )
        if (albumTracks.isNotEmpty()) {
            val playQueue = if (shuffle) albumTracks.shuffled() else albumTracks
            playTrack(playQueue.first(), playQueue, "Album: ${album.name}")
        }
    }

    fun playArtist(artist: Artist, shuffle: Boolean = false) {
        val artistTracks = tracks.value.filter { it.artist.equals(artist.name, ignoreCase = true) }
            .sortedBy { it.title.lowercase() }
        if (artistTracks.isNotEmpty()) {
            val playQueue = if (shuffle) artistTracks.shuffled() else artistTracks
            playTrack(playQueue.first(), playQueue, "Artist: ${artist.name}")
        }
    }

    fun playPlaylist(playlist: Playlist, shuffle: Boolean = false) {
        viewModelScope.launch {
            val resolved = repository.getTracksForPlaylist(playlist.id).first()
            if (resolved.isNotEmpty()) {
                val playQueue = if (shuffle) resolved.shuffled() else resolved
                playTrack(playQueue.first(), playQueue, "Playlist: ${playlist.name}")
            }
        }
    }

    fun addTracksToPlaylist(playlistId: Long, tracksToAdd: List<Track>) {
        if (tracksToAdd.isEmpty()) return
        viewModelScope.launch {
            repository.batchAddToPlaylist(playlistId, tracksToAdd)
        }
    }

    fun removeQueueItem(index: Int) {
        playerClient.removeQueueItem(index)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        playerClient.moveQueueItem(fromIndex, toIndex)
    }

    fun clearQueue() {
        playerClient.clearQueue()
    }

    fun refreshLibrary() {
        repository.refreshLibrary()
    }

    fun syncLibraryIncremental(force: Boolean = false, reason: String = "manual") {
        repository.syncLibraryIncremental(forceFullRescan = force, reason = reason)
    }

    fun fixMissingArtwork() {
        repository.fixMissingArtwork()
    }

    fun importSafFolder(uri: Uri) {
        repository.importSafFolder(uri)
    }

    fun deleteScannedFolder(folder: ScannedFolderEntity) {
        viewModelScope.launch {
            repository.localRepository.deleteScannedFolder(folder)
            repository.refreshLibrary()
        }
    }

    fun loadDemoTracks() {
        repository.forceLoadDemoTracks()
    }

    fun toggleFavorite(track: Track) {
        val currentIsFav = track.id in favoriteTrackIds.value
        val newFav = !currentIsFav
        _optimisticFavorites.value = _optimisticFavorites.value + (track.id to newFav)
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(track)
        }
    }

    // Contextual Track Actions Menu
    fun openTrackActionSheet(track: Track) {
        _trackForActionSheet.value = track
    }

    fun closeTrackActionSheet() {
        _trackForActionSheet.value = null
    }

    fun openRenameTrackDialog(track: Track) {
        _trackForRename.value = track
    }

    fun closeRenameTrackDialog() {
        _trackForRename.value = null
    }

    fun openTrackInfoDialog(track: Track) {
        _trackForDetails.value = track
    }

    fun closeTrackInfoDialog() {
        _trackForDetails.value = null
    }

    fun openDeleteConfirmDialog(tracks: List<Track>) {
        _tracksForDeleteConfirm.value = tracks
    }

    fun closeDeleteConfirmDialog() {
        _tracksForDeleteConfirm.value = null
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            repository.deleteTrack(track)
            closeDeleteConfirmDialog()
        }
    }

    fun deleteTracks(tracks: List<Track>) {
        viewModelScope.launch {
            repository.batchDeleteTracks(tracks)
            closeDeleteConfirmDialog()
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.renamePlaylist(playlistId, newName.trim())
            _selectedPlaylist.value?.let {
                if (it.id == playlistId) {
                    _selectedPlaylist.value = it.copy(name = newName.trim())
                }
            }
        }
    }

    // Multi-Selection Actions
    fun enterMultiSelect(initialTrack: Track) {
        _isSelectionMode.value = true
        _selectedTrackIds.value = setOf(initialTrack.id)
    }

    fun toggleTrackSelection(trackId: String) {
        val current = _selectedTrackIds.value.toMutableSet()
        if (current.contains(trackId)) {
            current.remove(trackId)
            if (current.isEmpty()) {
                _isSelectionMode.value = false
            }
        } else {
            current.add(trackId)
        }
        _selectedTrackIds.value = current
    }

    fun selectAllTracks(allIds: List<String>) {
        _selectedTrackIds.value = allIds.toSet()
    }

    fun clearSelection() {
        _selectedTrackIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun addSelectedToQueue() {
        val selected = tracks.value.filter { it.id in _selectedTrackIds.value }
        selected.forEach { playerClient.addToQueue(it) }
        clearSelection()
    }

    fun addSelectedToFavorites() {
        val selected = tracks.value.filter { it.id in _selectedTrackIds.value }
        viewModelScope.launch {
            repository.batchAddToFavorites(selected)
            clearSelection()
        }
    }

    fun addSelectedToPlaylist(playlist: Playlist) {
        val selected = tracks.value.filter { it.id in _selectedTrackIds.value }
        viewModelScope.launch {
            repository.batchAddToPlaylist(playlist.id, selected)
            clearSelection()
        }
    }

    fun deleteSelectedTracks() {
        val selected = tracks.value.filter { it.id in _selectedTrackIds.value }
        viewModelScope.launch {
            repository.batchDeleteTracks(selected)
            clearSelection()
        }
    }

    // Playback & Library Settings Setters
    fun setCrossfade(enabled: Boolean, durationSec: Int) {
        _crossfadeEnabled.value = enabled
        _crossfadeDurationSec.value = durationSec
        playerClient.setCrossfade(enabled, durationSec)
        viewModelScope.launch {
            preferencesRepository.setCrossfade(enabled, durationSec)
        }
    }

    fun setGaplessPlayback(enabled: Boolean) {
        _gaplessPlayback.value = enabled
        playerClient.setGaplessPlayback(enabled)
        viewModelScope.launch {
            preferencesRepository.setGaplessPlayback(enabled)
        }
    }

    fun setPauseOnHeadphoneDisconnect(enabled: Boolean) {
        _pauseOnHeadphoneDisconnect.value = enabled
        viewModelScope.launch {
            preferencesRepository.setPauseOnHeadphoneDisconnect(enabled)
        }
    }

    fun setResumeOnLaunch(enabled: Boolean) {
        _resumeOnLaunch.value = enabled
        viewModelScope.launch {
            preferencesRepository.setResumeOnLaunch(enabled)
        }
    }

    fun setLockscreenArtwork(enabled: Boolean) {
        _lockscreenArtwork.value = enabled
        viewModelScope.launch {
            preferencesRepository.setLockscreenArtwork(enabled)
        }
    }

    fun resetSettingsToDefaults() {
        viewModelScope.launch {
            preferencesRepository.resetSettingsToDefaults()
            resetEqToFlat()
        }
    }

    fun setFilterShortTracks(enabled: Boolean) {
        repository.setFilterShortTracks(enabled)
        viewModelScope.launch {
            preferencesRepository.setFilterShortTracks(enabled)
        }
    }

    fun setAutoScanStartup(enabled: Boolean) {
        _autoScanStartup.value = enabled
        viewModelScope.launch {
            preferencesRepository.setAutoScan(enabled)
        }
    }

    fun setVesperThemeMode(themeMode: String) {
        _vesperThemeMode.value = themeMode
        viewModelScope.launch {
            preferencesRepository.setVesperThemeMode(themeMode)
        }
    }

    fun setDirectSourceStreamEnabled(enabled: Boolean) {
        _directSourceStreamEnabled.value = enabled
        viewModelScope.launch {
            preferencesRepository.setDirectSourceStreamEnabled(enabled)
        }
    }

    fun setWifiStreamingQuality(quality: String) {
        _wifiStreamingQuality.value = quality
        viewModelScope.launch {
            preferencesRepository.setWifiStreamingQuality(quality)
        }
    }

    fun setMobileStreamingQuality(quality: String) {
        _mobileStreamingQuality.value = quality
        viewModelScope.launch {
            preferencesRepository.setMobileStreamingQuality(quality)
        }
    }

    fun setWifiOnlyDownloads(enabled: Boolean) {
        _wifiOnlyDownloads.value = enabled
        viewModelScope.launch {
            preferencesRepository.setWifiOnlyDownloads(enabled)
        }
    }

    fun setDisableEffectsOnSpeaker(enabled: Boolean) {
        _disableEffectsOnSpeaker.value = enabled
        audioEffectController.setSpeakerBypass(enabled, true)
        playerClient.applyPreampScalar(audioEffectController.preampGainScalar.value)
        viewModelScope.launch {
            preferencesRepository.setDisableEffectsOnSpeaker(enabled)
        }
    }

    fun setLoudnessNormalizationEnabled(enabled: Boolean) {
        _loudnessNormalizationEnabled.value = enabled
        audioEffectController.toggleHeadroomProtection(enabled)
        playerClient.applyPreampScalar(audioEffectController.preampGainScalar.value)
        viewModelScope.launch {
            preferencesRepository.setLoudnessNormalizationEnabled(enabled)
        }
    }

    fun setHideRecentsOnHome(enabled: Boolean) {
        _hideRecentsOnHome.value = enabled
        viewModelScope.launch {
            preferencesRepository.setHideRecentsOnHome(enabled)
        }
    }

    fun setHideMoodPicksOnHome(enabled: Boolean) {
        _hideMoodPicksOnHome.value = enabled
        viewModelScope.launch {
            preferencesRepository.setHideMoodPicksOnHome(enabled)
        }
    }

    fun setHideTopicPicksOnHome(enabled: Boolean) {
        _hideTopicPicksOnHome.value = enabled
        viewModelScope.launch {
            preferencesRepository.setHideTopicPicksOnHome(enabled)
        }
    }

    fun setReduceAnimations(enabled: Boolean) {
        _reduceAnimations.value = enabled
        viewModelScope.launch {
            preferencesRepository.setReduceAnimations(enabled)
        }
    }

    fun setReduceDynamicBlur(enabled: Boolean) {
        _reduceDynamicBlur.value = enabled
        viewModelScope.launch {
            preferencesRepository.setReduceDynamicBlur(enabled)
        }
    }

    fun setFullScreenCoverArt(enabled: Boolean) {
        _fullScreenCoverArt.value = enabled
        viewModelScope.launch {
            preferencesRepository.setFullScreenCoverArt(enabled)
        }
    }

    fun setSyncedLyricsEnabled(enabled: Boolean) {
        _syncedLyricsEnabled.value = enabled
        viewModelScope.launch {
            preferencesRepository.setSyncedLyricsEnabled(enabled)
        }
    }

    fun setBlurUnfocusedLyrics(enabled: Boolean) {
        _blurUnfocusedLyrics.value = enabled
        viewModelScope.launch {
            preferencesRepository.setBlurUnfocusedLyrics(enabled)
        }
    }

    fun setPreampHeadroomEnabled(enabled: Boolean) {
        audioEffectController.toggleHeadroomProtection(enabled)
        playerClient.applyPreampScalar(audioEffectController.preampGainScalar.value)
        viewModelScope.launch {
            preferencesRepository.setPreampHeadroomEnabled(enabled)
        }
    }

    fun setAudioBalance(balance: Float) {
        audioEffectController.setAudioBalance(balance)
        viewModelScope.launch {
            preferencesRepository.setAudioBalance(balance)
        }
    }

    // Playlist Dialog Actions
    fun openAddToPlaylistDialog(track: Track) {
        _trackForPlaylist.value = track
    }

    fun closeAddToPlaylistDialog() {
        _trackForPlaylist.value = null
    }

    fun openCreatePlaylistDialog() {
        _showCreatePlaylistDialog.value = true
    }

    fun closeCreatePlaylistDialog() {
        _showCreatePlaylistDialog.value = false
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.createPlaylist(name.trim())
            _trackForPlaylist.value?.let { track ->
                repository.addTrackToPlaylist(id, track.id)
                _trackForPlaylist.value = null
            }
            _showCreatePlaylistDialog.value = false
        }
    }

    fun addTrackToExistingPlaylist(playlist: Playlist, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlist.id, track.id)
            _trackForPlaylist.value = null
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist.id)
        }
    }

    // Audio Effects & EQ — Single Synchronized Source of Truth
    private fun mapPresetToSoundProfile(presetName: String, fallbackProfileName: String? = null): SoundProfile {
        return when (presetName.trim().lowercase()) {
            "balanced" -> SoundProfile.BALANCED
            "bass" -> SoundProfile.BASS_BOOST
            "vocal" -> SoundProfile.VOCAL
            "rock" -> SoundProfile.ROCK
            "pop" -> SoundProfile.POP
            "acoustic" -> SoundProfile.ACOUSTIC
            "studio reference" -> SoundProfile.STUDIO_REFERENCE
            "warm" -> SoundProfile.WARM_ANALOG
            "classical" -> SoundProfile.CLASSICAL
            "electronic" -> SoundProfile.ELECTRONIC
            "cinematic" -> SoundProfile.CINEMATIC
            "custom" -> SoundProfile.CUSTOM
            else -> try {
                if (!fallbackProfileName.isNullOrBlank()) SoundProfile.valueOf(fallbackProfileName) else SoundProfile.CUSTOM
            } catch (_: Exception) {
                SoundProfile.CUSTOM
            }
        }
    }

    fun toggleEqEnabled(enabled: Boolean) {
        audioEffectController.setEnabled(enabled)
        playerClient.applyPreampScalar(audioEffectController.preampGainScalar.value)
        persistEqSettings()
    }

    fun setEqBandLevel(bandIndex: Short, levelMb: Short) {
        audioEffectController.setBandLevel(bandIndex, levelMb)
        _activeSoundProfile.value = SoundProfile.CUSTOM
        playerClient.applyPreampScalar(audioEffectController.preampGainScalar.value)
        persistEqSettings()
    }

    fun selectPreset(preset: String) {
        val customMatch = customEqPresets.value.firstOrNull { it.name == preset }
        val isBasicMode = com.vesper.audio.audio.AudioEffectController.BASIC_MODES.any {
            it.name.equals(preset, ignoreCase = true)
        }
        if (customMatch != null) {
            audioEffectController.applyCustomPreset(customMatch.name, customMatch.bandLevelsCsv, customMatch.bassBoost)
            _activeSoundProfile.value = SoundProfile.CUSTOM
        } else if (isBasicMode) {
            val resultingMode = audioEffectController.selectBasicMode(preset, toggleIfAlreadyActive = true)
            _activeSoundProfile.value = mapPresetToSoundProfile(resultingMode)
        } else {
            audioEffectController.applyPreset(preset)
            _activeSoundProfile.value = mapPresetToSoundProfile(preset)
        }
        playerClient.applyPreampScalar(audioEffectController.preampGainScalar.value)
        persistEqSettings()
    }

    fun setBassBoostStrength(strength: Short) {
        audioEffectController.setBassBoost(strength)
        playerClient.applyPreampScalar(audioEffectController.preampGainScalar.value)
        persistEqSettings()
    }

    fun saveCustomEqPreset(name: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            val levelsCsv = audioEffectController.bands.value.joinToString(",") { it.levelMb.toString() }
            val boost = audioEffectController.bassBoostStrength.value.toInt()
            repository.localRepository.saveEqPreset(cleanName, levelsCsv, boost)
            audioEffectController.applyCustomPreset(cleanName, levelsCsv, boost)
            _activeSoundProfile.value = SoundProfile.CUSTOM
            persistEqSettings()
        }
    }

    fun renameCustomEqPreset(oldName: String, newName: String) {
        val cleanNew = newName.trim()
        if (cleanNew.isBlank()) return
        viewModelScope.launch {
            val existing = customEqPresets.value.firstOrNull { it.name == oldName } ?: return@launch
            repository.localRepository.renameEqPreset(oldName, cleanNew, existing.bandLevelsCsv, existing.bassBoost)
            if (activePreset.value == oldName) {
                audioEffectController.applyCustomPreset(cleanNew, existing.bandLevelsCsv, existing.bassBoost)
                persistEqSettings()
            }
        }
    }

    fun duplicateCustomEqPreset(sourceName: String) {
        viewModelScope.launch {
            val existing = customEqPresets.value.firstOrNull { it.name == sourceName } ?: return@launch
            val copyName = "$sourceName Copy"
            repository.localRepository.saveEqPreset(copyName, existing.bandLevelsCsv, existing.bassBoost)
        }
    }

    fun deleteCustomEqPreset(name: String) {
        viewModelScope.launch {
            repository.localRepository.deleteEqPreset(name)
            if (activePreset.value == name) {
                resetEqToFlat()
            }
        }
    }

    fun resetEqToFlat() {
        audioEffectController.resetToFlat()
        _activeSoundProfile.value = SoundProfile.BALANCED
        playerClient.applyPreampScalar(audioEffectController.preampGainScalar.value)
        persistEqSettings()
    }

    fun selectSoundProfile(profile: SoundProfile) {
        _activeSoundProfile.value = profile
        if (profile != SoundProfile.CUSTOM) {
            val mappedPreset = when (profile) {
                SoundProfile.BALANCED -> "Balanced"
                SoundProfile.DEEP_BASS, SoundProfile.BASS_BOOST -> "Bass"
                SoundProfile.VOCAL -> "Vocal"
                SoundProfile.ROCK -> "Rock"
                SoundProfile.POP -> "Pop"
                SoundProfile.ACOUSTIC -> "Acoustic"
                SoundProfile.STUDIO_REFERENCE -> "Studio Reference"
                SoundProfile.WARM_ANALOG, SoundProfile.JAZZ, SoundProfile.NIGHT -> "Warm"
                SoundProfile.CLASSICAL -> "Classical"
                SoundProfile.ELECTRONIC -> "Electronic"
                SoundProfile.CINEMATIC -> "Cinematic"
                SoundProfile.CUSTOM -> "Custom"
            }
            audioEffectController.setEnabled(true)
            audioEffectController.applyPreset(mappedPreset)
        }
        playerClient.applyPreampScalar(audioEffectController.preampGainScalar.value)
        persistEqSettings()
    }

    private fun persistEqSettings() {
        viewModelScope.launch {
            val levelsCsv = audioEffectController.bands.value.joinToString(",") { it.levelMb.toString() }
            preferencesRepository.setEqSettings(
                enabled = audioEffectController.isEnabled.value,
                profile = _activeSoundProfile.value.name,
                preset = audioEffectController.activePresetName.value,
                bandLevelsCsv = levelsCsv,
                bassBoost = audioEffectController.bassBoostStrength.value.toInt(),
                preampHeadroom = audioEffectController.headroomProtectionEnabled.value,
                balance = audioEffectController.audioBalance.value
            )
        }
    }

    fun toggleSupporterDebug() {
        viewModelScope.launch {
            val next = !_isSupporter.value
            _isSupporter.value = next
            preferencesRepository.setIsSupporter(next)
        }
    }

    fun setSupporterStatus(isSupporter: Boolean) {
        viewModelScope.launch {
            _isSupporter.value = isSupporter
            preferencesRepository.setIsSupporter(isSupporter)
        }
    }

    // Metadata Studio Controls
    fun openMetadataEditor(track: Track) {
        _trackForMetadata.value = track
        navigateTo(VesperFullScreenDestination.METADATA_STUDIO)
    }

    fun updateTrackMetadata(
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        newYear: Int = 0,
        newTrackNumber: Int = 0,
        newGenre: String? = null
    ) {
        val track = _trackForMetadata.value ?: _trackForRename.value ?: currentTrack.value ?: return
        repository.updateTrackMetadata(track.id, newTitle, newArtist, newAlbum, newYear, newTrackNumber, newGenre)
        val updated = track.copy(
            title = newTitle.trim(),
            artist = newArtist.trim(),
            album = newAlbum.trim(),
            year = if (newYear > 0) newYear else track.year,
            trackNumber = if (newTrackNumber > 0) newTrackNumber else track.trackNumber,
            genre = if (!newGenre.isNullOrBlank()) newGenre.trim() else track.genre
        )
        _trackForMetadata.value = updated
        _trackForRename.value = null
    }

    fun batchUpdateTrackMetadata(trackIds: Set<String>, newArtist: String?, newAlbum: String?, newYear: Int?) {
        repository.batchUpdateMetadata(trackIds, newArtist, newAlbum, newYear)
    }

    fun clearArtworkCache() {
        com.vesper.audio.util.VesperArtworkResolver.clearArtworkCache(getApplication())
        repository.refreshLibrary()
    }

    fun getArtworkCacheSize(): Long {
        return com.vesper.audio.util.VesperArtworkResolver.getArtworkCacheSizeBytes(getApplication())
    }

    override fun onCleared() {
        playerClient.release()
        super.onCleared()
    }
}
