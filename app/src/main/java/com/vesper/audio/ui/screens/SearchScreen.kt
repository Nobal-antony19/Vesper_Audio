package com.vesper.audio.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vesper.audio.data.remote.AudiusApiClient
import com.vesper.audio.data.remote.AudiusTrack
import com.vesper.audio.data.remote.DiscoverProvider
import com.vesper.audio.model.Album
import com.vesper.audio.model.Artist
import com.vesper.audio.model.Track
import com.vesper.audio.ui.components.AlphabeticalFastScrollRail
import com.vesper.audio.ui.components.EmptyLibraryState
import com.vesper.audio.ui.components.SectionHeader
import com.vesper.audio.ui.theme.LocalVesperAccentColor
import com.vesper.audio.ui.theme.VesperBackground
import com.vesper.audio.ui.theme.VesperBackgroundSecondary
import com.vesper.audio.ui.theme.VesperBorderAccent
import com.vesper.audio.ui.theme.VesperBurgundyAccent
import com.vesper.audio.ui.theme.VesperBurgundyHighlight
import com.vesper.audio.ui.theme.VesperDeepBurgundy
import com.vesper.audio.ui.theme.VesperOutline
import com.vesper.audio.ui.theme.VesperSurfaceElevated
import com.vesper.audio.ui.theme.VesperTextMuted
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTextSecondary
import com.vesper.audio.ui.theme.VesperTypography
import kotlinx.coroutines.launch

/**
 * Discover content tabs in exact specification order:
 * 1. Songs
 * 2. Artists
 * 3. Album
 */
enum class DiscoverTab {
    SONGS,
    ARTISTS,
    ALBUMS
}

@Composable
fun SearchScreen(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<Track> = emptyList(),
    allTracks: List<Track> = emptyList(),
    allAlbums: List<Album> = emptyList(),
    allArtists: List<Artist> = emptyList(),
    audiusTracks: List<AudiusTrack> = emptyList(),
    audiusTrendingTracks: List<AudiusTrack> = emptyList(),
    isSearchingAudius: Boolean = false,
    isLoadingMoreAudius: Boolean = false,
    hasMoreAudius: Boolean = false,
    audiusError: String? = null,
    isFromCache: Boolean = false,
    isStale: Boolean = false,
    enabledProviders: List<DiscoverProvider> = emptyList(),
    selectedProviderId: String = "audius",
    onSelectProvider: (String) -> Unit = {},
    onArtworkError: (String?) -> Unit = {},
    onLoadTrending: () -> Unit = {},
    onLoadMoreAudius: () -> Unit = {},
    onRetryAudius: () -> Unit = {},
    currentTrack: Track? = null,
    isPlaying: Boolean = false,
    favoriteTrackIds: Set<String> = emptySet(),
    onTrackClick: (Track, List<Track>) -> Unit,
    onAlbumClick: (Album) -> Unit = {},
    onArtistClick: (Artist) -> Unit = {},
    onLocalAlbumClick: ((Album) -> Unit)? = null,
    onLocalArtistClick: ((Artist) -> Unit)? = null,
    onFavoriteToggle: (Track) -> Unit,
    onMoreTrackClick: (Track) -> Unit,
    onOpenDrawer: () -> Unit,
    onLogoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(DiscoverTab.SONGS) }
    var sortAlphabetically by rememberSaveable { mutableStateOf(false) }
    var showSuggestions by rememberSaveable { mutableStateOf(true) }

    val dynamicAccent = LocalVesperAccentColor.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        onLoadTrending()
    }

    val query = searchQuery.trim()

    // Strictly remote-only catalogue: never mix local device tracks, albums, or artists
    val activeRemoteAudiusTracks = remember(query, audiusTracks, audiusTrendingTracks) {
        if (query.isNotBlank()) {
            audiusTracks.distinctBy { it.namespacedId }
        } else {
            audiusTrendingTracks.distinctBy { it.namespacedId }
        }
    }

    val remoteSongs: List<Track> = remember(activeRemoteAudiusTracks, sortAlphabetically) {
        val mapped = activeRemoteAudiusTracks.map { it.toTrack() }
        if (sortAlphabetically) {
            mapped.sortedBy { it.title.lowercase() }
        } else {
            mapped
        }
    }

    val remoteArtists: List<Artist> = remember(activeRemoteAudiusTracks) {
        AudiusApiClient.deriveArtistsFromTracks(activeRemoteAudiusTracks)
    }

    val remoteAlbums: List<Album> = remember(activeRemoteAudiusTracks) {
        AudiusApiClient.deriveAlbumsFromTracks(activeRemoteAudiusTracks)
    }

    // Token-aware local library matches when a search query is active (identities kept strictly distinct)
    val localMatchedSongs: List<Track> = remember(query, allTracks) {
        if (query.isBlank()) {
            emptyList()
        } else {
            allTracks
                .filter { com.vesper.audio.data.remote.DiscoverComparisonUtils.matchesTokens(query, it.title, it.artist, it.album) }
                .sortedByDescending { com.vesper.audio.data.remote.DiscoverComparisonUtils.scoreTrackMatch(it.title, it.artist, it.album, query) }
        }
    }

    val localMatchedArtists: List<Artist> = remember(query, allArtists) {
        if (query.isBlank()) {
            emptyList()
        } else {
            allArtists.filter { com.vesper.audio.data.remote.DiscoverComparisonUtils.matchesTokens(query, it.name) }
        }
    }

    val localMatchedAlbums: List<Album> = remember(query, allAlbums) {
        if (query.isBlank()) {
            emptyList()
        } else {
            allAlbums.filter { com.vesper.audio.data.remote.DiscoverComparisonUtils.matchesTokens(query, it.name, it.artist) }
        }
    }

    // Precompute O(1) letter -> index map only when sorted alphabetically
    val letterIndexMap: Map<Char, Int> = remember(remoteSongs, sortAlphabetically) {
        if (!sortAlphabetically) {
            emptyMap()
        } else {
            val map = mutableMapOf<Char, Int>()
            remoteSongs.forEachIndexed { index, track ->
                val rawChar = track.title.firstOrNull()?.uppercaseChar() ?: '#'
                val keyChar = if (rawChar in 'A'..'Z') rawChar else '#'
                if (!map.containsKey(keyChar)) {
                    map[keyChar] = index
                }
            }
            map
        }
    }

    val availableLetters = remember(letterIndexMap) {
        letterIndexMap.keys
    }

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(VesperBackground)
    ) {
        val isCompactHeight = maxHeight < 480.dp
        val verticalSpacerDp = if (isCompactHeight) 4.dp else 10.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = if (isCompactHeight) 4.dp else 8.dp)
        ) {
            // Top Header: Branding with Logo + Title "Discover" + Hamburger Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isCompactHeight) 2.dp else 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.then(
                        if (onLogoClick != null) Modifier.clickable { onLogoClick() } else Modifier
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isCompactHeight) 32.dp else 38.dp)
                            .clip(CircleShape)
                            .background(VesperDeepBurgundy)
                            .border(1.2.dp, dynamicAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Discover Logo",
                            tint = VesperTextPrimary,
                            modifier = Modifier.size(if (isCompactHeight) 18.dp else 20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Discover",
                                style = VesperTypography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = VesperTextPrimary
                                )
                            )
                            if (isFromCache) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isStale) Color(0xFF2D141B) else VesperDeepBurgundy,
                                    modifier = Modifier
                                        .border(
                                            0.8.dp,
                                            if (isStale) VesperBurgundyAccent else VesperOutline,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .testTag("discover_cache_badge")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isStale) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = VesperBurgundyHighlight,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isStale) "Offline Cache" else "Cached",
                                            style = VesperTypography.labelSmall.copy(
                                                color = VesperTextSecondary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        if (!isCompactHeight) {
                            Text(
                                text = "Online indie music, artists & albums",
                                style = VesperTypography.bodySmall.copy(
                                    color = VesperTextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRetryAudius,
                        modifier = Modifier
                            .size(if (isCompactHeight) 38.dp else 42.dp)
                            .testTag("discover_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Discover",
                            tint = VesperTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(if (isCompactHeight) 42.dp else 48.dp)
                            .testTag("vesper_hamburger_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open navigation menu",
                            tint = VesperTextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(verticalSpacerDp))

            // Remote Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = { Text("Search online songs, artists, albums...", color = VesperTextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = dynamicAccent
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSearchingAudius) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = dynamicAccent,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = VesperTextSecondary
                                )
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = VesperBackgroundSecondary,
                    unfocusedContainerColor = VesperBackgroundSecondary,
                    focusedBorderColor = dynamicAccent,
                    unfocusedBorderColor = VesperOutline,
                    focusedTextColor = VesperTextPrimary,
                    unfocusedTextColor = VesperTextPrimary,
                    cursorColor = dynamicAccent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_field")
            )

            Spacer(modifier = Modifier.height(verticalSpacerDp))

            // Enabled Provider Filter Row (Only shows genuinely enabled & authorized providers)
            if (enabledProviders.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    enabledProviders.forEach { provider ->
                        val isSelected = provider.providerId == selectedProviderId
                        Surface(
                            onClick = { onSelectProvider(provider.providerId) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) VesperDeepBurgundy else VesperBackgroundSecondary,
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (isSelected) dynamicAccent else VesperOutline,
                                    RoundedCornerShape(10.dp)
                                )
                                .testTag("discover_provider_${provider.providerId}")
                        ) {
                            Text(
                                text = provider.displayName,
                                style = VesperTypography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) VesperTextPrimary else VesperTextSecondary
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 3 Content Tabs in Exact Requested Order: Songs | Artists | Album (+ optional A-Z toggle for Songs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DiscoverCategoryChip(
                        label = "Songs",
                        selected = selectedTab == DiscoverTab.SONGS,
                        onClick = { selectedTab = DiscoverTab.SONGS },
                        testTag = "discover_tab_songs"
                    )
                    DiscoverCategoryChip(
                        label = "Artists",
                        selected = selectedTab == DiscoverTab.ARTISTS,
                        onClick = { selectedTab = DiscoverTab.ARTISTS },
                        testTag = "discover_tab_artists"
                    )
                    DiscoverCategoryChip(
                        label = "Album",
                        selected = selectedTab == DiscoverTab.ALBUMS,
                        onClick = { selectedTab = DiscoverTab.ALBUMS },
                        testTag = "discover_tab_album"
                    )
                }

                if (selectedTab == DiscoverTab.SONGS && remoteSongs.isNotEmpty()) {
                    Surface(
                        onClick = { sortAlphabetically = !sortAlphabetically },
                        shape = RoundedCornerShape(10.dp),
                        color = if (sortAlphabetically) VesperDeepBurgundy else Color.Transparent,
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (sortAlphabetically) VesperBurgundyHighlight else VesperOutline,
                                RoundedCornerShape(10.dp)
                            )
                            .testTag("discover_sort_az_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SortByAlpha,
                                contentDescription = "Sort A to Z",
                                tint = if (sortAlphabetically) VesperBurgundyHighlight else VesperTextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (sortAlphabetically) "A–Z" else "Mix",
                                style = VesperTypography.labelSmall.copy(
                                    fontWeight = if (sortAlphabetically) FontWeight.Bold else FontWeight.Normal,
                                    color = if (sortAlphabetically) VesperBurgundyHighlight else VesperTextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            // Provider / Network Error Banner
            if (audiusError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF2D141B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VesperBurgundyAccent, RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = VesperBurgundyHighlight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = audiusError,
                                style = VesperTypography.bodySmall.copy(color = VesperTextSecondary, fontSize = 11.sp),
                                maxLines = if (isCompactHeight) 1 else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            onClick = onRetryAudius,
                            shape = RoundedCornerShape(8.dp),
                            color = VesperBurgundyAccent,
                            modifier = Modifier.testTag("discover_retry_audius_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry Discover",
                                    tint = VesperTextPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Retry",
                                    style = VesperTypography.labelSmall.copy(
                                        color = VesperTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(verticalSpacerDp))

            val hasSuggestions = query.isNotBlank() && (
                localMatchedSongs.isNotEmpty() ||
                    localMatchedArtists.isNotEmpty() ||
                    localMatchedAlbums.isNotEmpty() ||
                    remoteSongs.isNotEmpty() ||
                    remoteArtists.isNotEmpty() ||
                    remoteAlbums.isNotEmpty()
                )

            @Composable
            fun SuggestionsBannerCard() {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = VesperSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .border(1.dp, VesperOutline, RoundedCornerShape(14.dp))
                        .testTag("search_suggestions_section")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SUGGESTIONS FOR \"${query.uppercase()}\"",
                                style = VesperTypography.labelSmall.copy(
                                    color = VesperBurgundyHighlight,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 10.sp
                                )
                            )
                            Text(
                                text = if (showSuggestions) "Hide" else "Show",
                                style = VesperTypography.labelSmall.copy(
                                    color = VesperTextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier
                                    .clickable { showSuggestions = !showSuggestions }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .testTag("toggle_search_suggestions")
                            )
                        }

                        if (showSuggestions) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val topLocalArtist = localMatchedArtists.firstOrNull()
                            val topRemoteArtist = remoteArtists.firstOrNull {
                                com.vesper.audio.data.remote.DiscoverComparisonUtils.matchesTokens(query, it.name)
                            } ?: remoteArtists.firstOrNull()

                            if (topLocalArtist != null) {
                                SearchSuggestionRow(
                                    title = topLocalArtist.name,
                                    subtitle = "${topLocalArtist.trackCount} songs • ${topLocalArtist.albumCount} albums",
                                    typeBadge = "Artist",
                                    sourceBadge = "Local",
                                    onClick = { (onLocalArtistClick ?: onArtistClick).invoke(topLocalArtist) },
                                    testTag = "suggestion_local_artist_${topLocalArtist.name.lowercase()}"
                                )
                            } else if (topRemoteArtist != null) {
                                SearchSuggestionRow(
                                    title = topRemoteArtist.name,
                                    subtitle = "${topRemoteArtist.trackCount} online songs • ${topRemoteArtist.albumCount} albums",
                                    typeBadge = "Artist",
                                    sourceBadge = "Audius",
                                    onClick = { onArtistClick(topRemoteArtist) },
                                    testTag = "suggestion_remote_artist_${topRemoteArtist.name.lowercase()}"
                                )
                            }

                            val topLocalAlbum = localMatchedAlbums.firstOrNull()
                            val topRemoteAlbum = remoteAlbums.firstOrNull {
                                com.vesper.audio.data.remote.DiscoverComparisonUtils.matchesTokens(query, it.name, it.artist)
                            } ?: remoteAlbums.firstOrNull()

                            if (topLocalAlbum != null) {
                                SearchSuggestionRow(
                                    title = topLocalAlbum.name,
                                    subtitle = "${topLocalAlbum.artist} • ${topLocalAlbum.trackCount} tracks",
                                    typeBadge = "Album",
                                    sourceBadge = "Local",
                                    onClick = { (onLocalAlbumClick ?: onAlbumClick).invoke(topLocalAlbum) },
                                    testTag = "suggestion_local_album_${topLocalAlbum.name.lowercase()}"
                                )
                            } else if (topRemoteAlbum != null) {
                                SearchSuggestionRow(
                                    title = topRemoteAlbum.name,
                                    subtitle = "${topRemoteAlbum.artist} • ${topRemoteAlbum.trackCount} tracks",
                                    typeBadge = "Album",
                                    sourceBadge = "Audius",
                                    onClick = { onAlbumClick(topRemoteAlbum) },
                                    testTag = "suggestion_remote_album_${topRemoteAlbum.name.lowercase()}"
                                )
                            }

                            val topLocalSong = localMatchedSongs.firstOrNull()
                            val topRemoteSong = remoteSongs.firstOrNull()
                            if (topLocalSong != null) {
                                SearchSuggestionRow(
                                    title = topLocalSong.title,
                                    subtitle = "${topLocalSong.artist} • ${topLocalSong.album}",
                                    typeBadge = "Song",
                                    sourceBadge = "Local",
                                    onClick = { onTrackClick(topLocalSong, localMatchedSongs) },
                                    testTag = "suggestion_local_song_${topLocalSong.id}"
                                )
                            }
                            if (topRemoteSong != null) {
                                SearchSuggestionRow(
                                    title = topRemoteSong.title,
                                    subtitle = "${topRemoteSong.artist} • ${topRemoteSong.formattedDuration}",
                                    typeBadge = "Song",
                                    sourceBadge = "Audius",
                                    onClick = { onTrackClick(topRemoteSong, remoteSongs) },
                                    testTag = "suggestion_remote_song_${topRemoteSong.id}"
                                )
                            }
                        }
                    }
                }
            }

            // In portrait when the active tab's list is empty, show suggestions above the empty state;
            // otherwise render suggestions inside the LazyColumn so landscape & portrait scroll smoothly from top to bottom!
            val activeTabHasListItems = when (selectedTab) {
                DiscoverTab.SONGS -> remoteSongs.isNotEmpty() || localMatchedSongs.isNotEmpty()
                DiscoverTab.ARTISTS -> remoteArtists.isNotEmpty() || localMatchedArtists.isNotEmpty()
                DiscoverTab.ALBUMS -> remoteAlbums.isNotEmpty() || localMatchedAlbums.isNotEmpty()
            }
            if (hasSuggestions && !activeTabHasListItems) {
                SuggestionsBannerCard()
            }

            // Main Tab Content (Local Matches + Remote Discover)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    DiscoverTab.SONGS -> {
                        if (remoteSongs.isEmpty() && localMatchedSongs.isEmpty()) {
                            if (isSearchingAudius) {
                                LazyColumn(
                                    contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("discover_skeleton_list")
                                ) {
                                    items(6, key = { "skeleton_$it" }, contentType = { "skeleton" }) {
                                        DiscoverSkeletonRow()
                                    }
                                }
                            } else {
                                EmptyLibraryState(
                                    title = if (query.isNotBlank()) "No Online Songs Found" else "Online Discovery Unavailable",
                                    message = if (query.isNotBlank()) {
                                        "No remote tracks matched \"$searchQuery\". Try another search term."
                                    } else {
                                        audiusError ?: "Connect to the internet and tap Retry to browse independent online music."
                                    },
                                    actionButtonText = "Retry Discover",
                                    onActionClick = onRetryAudius,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            val showSectionHeader = !sortAlphabetically && query.isBlank()
                            val headerOffset = (if (showSectionHeader) 1 else 0) + (if (hasSuggestions) 1 else 0)

                            Row(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .testTag("discover_songs_list")
                                ) {
                                    if (hasSuggestions) {
                                        item(key = "discover_suggestions_card", contentType = "suggestions") {
                                            SuggestionsBannerCard()
                                        }
                                    }

                                    if (localMatchedSongs.isNotEmpty()) {
                                        item(key = "header_local_songs", contentType = "header") {
                                            SectionHeader(title = "In Your Library (${localMatchedSongs.size})")
                                        }
                                        items(
                                            items = localMatchedSongs,
                                            key = { "local_song_${it.id}" },
                                            contentType = { "local_track" }
                                        ) { track ->
                                            val isCurrent = currentTrack?.id == track.id
                                            DiscoverTrackRow(
                                                track = track,
                                                isPlaying = isPlaying && isCurrent,
                                                isCurrentTrack = isCurrent,
                                                isFavorite = track.id in favoriteTrackIds,
                                                sourceLabel = "Local",
                                                onArtworkError = onArtworkError,
                                                onClick = { onTrackClick(track, localMatchedSongs) },
                                                onFavoriteClick = { onFavoriteToggle(track) },
                                                onMoreClick = { onMoreTrackClick(track) }
                                            )
                                        }
                                    }

                                    if (showSectionHeader && remoteSongs.isNotEmpty()) {
                                        item(key = "header_trending", contentType = "header") {
                                            SectionHeader(title = "Fresh on Audius")
                                        }
                                    } else if (query.isNotBlank() && remoteSongs.isNotEmpty()) {
                                        item(key = "header_online_songs", contentType = "header") {
                                            SectionHeader(title = "Online Songs (${remoteSongs.size})")
                                        }
                                    }

                                    items(
                                        items = remoteSongs,
                                        key = { it.id },
                                        contentType = { "discover_track" }
                                    ) { track ->
                                        val isCurrent = currentTrack?.id == track.id
                                        DiscoverTrackRow(
                                            track = track,
                                            isPlaying = isPlaying && isCurrent,
                                            isCurrentTrack = isCurrent,
                                            isFavorite = track.id in favoriteTrackIds,
                                            onArtworkError = onArtworkError,
                                            onClick = { onTrackClick(track, remoteSongs) },
                                            onFavoriteClick = { onFavoriteToggle(track) },
                                            onMoreClick = { onMoreTrackClick(track) }
                                        )
                                    }

                                    if (hasMoreAudius) {
                                        item(key = "load_more_audius", contentType = "load_more") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isLoadingMoreAudius) {
                                                    CircularProgressIndicator(
                                                        strokeWidth = 2.dp,
                                                        color = dynamicAccent,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                } else {
                                                    Surface(
                                                        onClick = onLoadMoreAudius,
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = VesperSurfaceElevated,
                                                        modifier = Modifier
                                                            .border(1.dp, VesperBorderAccent, RoundedCornerShape(10.dp))
                                                            .testTag("discover_load_more_button")
                                                    ) {
                                                        Text(
                                                            text = "Load More Online Tracks",
                                                            style = VesperTypography.labelMedium.copy(
                                                                color = VesperTextPrimary,
                                                                fontWeight = FontWeight.SemiBold
                                                            ),
                                                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (sortAlphabetically && availableLetters.isNotEmpty()) {
                                    AlphabeticalFastScrollRail(
                                        availableLetters = availableLetters,
                                        onLetterSelected = { char ->
                                            val targetIndex = letterIndexMap[char]
                                            if (targetIndex != null) {
                                                coroutineScope.launch {
                                                    listState.scrollToItem(targetIndex + headerOffset)
                                                }
                                            }
                                        },
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                DiscoverTab.ARTISTS -> {
                    if (remoteArtists.isEmpty() && localMatchedArtists.isEmpty()) {
                        if (isSearchingAudius) {
                            LazyColumn(
                                contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (hasSuggestions) {
                                    item(key = "discover_suggestions_card", contentType = "suggestions") {
                                        SuggestionsBannerCard()
                                    }
                                }
                                items(5, key = { "artist_skel_$it" }, contentType = { "skeleton" }) {
                                    DiscoverSkeletonRow()
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(bottom = 104.dp)
                            ) {
                                if (hasSuggestions) {
                                    SuggestionsBannerCard()
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                EmptyLibraryState(
                                    title = "No Artists Found",
                                    message = if (query.isNotBlank()) "No artists matched \"$searchQuery\"." else "Connect to the internet to discover independent artists.",
                                    actionButtonText = "Retry Discover",
                                    onActionClick = onRetryAudius
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (hasSuggestions) {
                                item(key = "discover_suggestions_card", contentType = "suggestions") {
                                    SuggestionsBannerCard()
                                }
                            }
                            if (localMatchedArtists.isNotEmpty()) {
                                item(key = "header_local_artists") {
                                    SectionHeader(title = "Artists in Your Library (${localMatchedArtists.size})")
                                }
                                items(
                                    items = localMatchedArtists,
                                    key = { artist -> "local_artist_${artist.name.lowercase()}" },
                                    contentType = { "local_artist" }
                                ) { artist ->
                                    Surface(
                                        onClick = { (onLocalArtistClick ?: onArtistClick).invoke(artist) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = VesperSurfaceElevated,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, VesperOutline, RoundedCornerShape(12.dp))
                                            .testTag("local_artist_row_${artist.name.lowercase()}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(CircleShape)
                                                    .background(VesperDeepBurgundy),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = VesperTextPrimary,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = artist.name,
                                                        style = VesperTypography.bodyLarge.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = VesperTextPrimary
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    AudiusSourceBadge(label = "Local")
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${artist.trackCount} local tracks • ${artist.albumCount} albums",
                                                    style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = VesperTextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (remoteArtists.isNotEmpty() && localMatchedArtists.isNotEmpty()) {
                                item(key = "header_remote_artists") {
                                    SectionHeader(title = "Online Artists (${remoteArtists.size})")
                                }
                            }
                            items(
                                items = remoteArtists,
                                key = { artist -> "remote_artist_${artist.name.lowercase()}" },
                                contentType = { "discover_artist" }
                            ) { artist ->
                                Surface(
                                    onClick = { onArtistClick(artist) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = VesperSurfaceElevated,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, VesperOutline, RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(VesperDeepBurgundy),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = VesperTextPrimary,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = artist.name,
                                                    style = VesperTypography.bodyLarge.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = VesperTextPrimary
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                AudiusSourceBadge()
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${artist.trackCount} online tracks • ${artist.albumCount} collections",
                                                style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = VesperTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                DiscoverTab.ALBUMS -> {
                    if (remoteAlbums.isEmpty() && localMatchedAlbums.isEmpty()) {
                        if (isSearchingAudius) {
                            LazyColumn(
                                contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (hasSuggestions) {
                                    item(key = "discover_suggestions_card", contentType = "suggestions") {
                                        SuggestionsBannerCard()
                                    }
                                }
                                items(5, key = { "album_skel_$it" }, contentType = { "skeleton" }) {
                                    DiscoverSkeletonRow()
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(bottom = 104.dp)
                            ) {
                                if (hasSuggestions) {
                                    SuggestionsBannerCard()
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                EmptyLibraryState(
                                    title = "No Albums Found",
                                    message = if (query.isNotBlank()) "No albums matched \"$searchQuery\"." else "Connect to the internet to browse online collections.",
                                    actionButtonText = "Retry Discover",
                                    onActionClick = onRetryAudius
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (hasSuggestions) {
                                item(key = "discover_suggestions_card", contentType = "suggestions") {
                                    SuggestionsBannerCard()
                                }
                            }
                            if (localMatchedAlbums.isNotEmpty()) {
                                item(key = "header_local_albums") {
                                    SectionHeader(title = "Albums in Your Library (${localMatchedAlbums.size})")
                                }
                                items(
                                    items = localMatchedAlbums,
                                    key = { album -> "local_album_${album.name.lowercase()}_${album.artist.lowercase()}" },
                                    contentType = { "local_album" }
                                ) { album ->
                                    Surface(
                                        onClick = { (onLocalAlbumClick ?: onAlbumClick).invoke(album) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = VesperSurfaceElevated,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, VesperOutline, RoundedCornerShape(12.dp))
                                            .testTag("local_album_row_${album.name.lowercase()}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(VesperDeepBurgundy),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Album,
                                                    contentDescription = null,
                                                    tint = VesperTextPrimary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = album.name,
                                                        style = VesperTypography.bodyLarge.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = VesperTextPrimary
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    AudiusSourceBadge(label = "Local")
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${album.artist} • ${album.trackCount} tracks",
                                                    style = VesperTypography.bodySmall.copy(color = VesperTextSecondary),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = VesperTextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (remoteAlbums.isNotEmpty() && localMatchedAlbums.isNotEmpty()) {
                                item(key = "header_remote_albums") {
                                    SectionHeader(title = "Online Albums (${remoteAlbums.size})")
                                }
                            }
                            items(
                                items = remoteAlbums,
                                key = { album -> "remote_album_${album.name.lowercase()}_${album.artist.lowercase()}" },
                                contentType = { "discover_album" }
                            ) { album ->
                                Surface(
                                    onClick = { onAlbumClick(album) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = VesperSurfaceElevated,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, VesperOutline, RoundedCornerShape(12.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(VesperDeepBurgundy)
                                        ) {
                                            if (album.artworkUri != null && album.artworkUri != Uri.EMPTY) {
                                                val imageModel = remember(context, album.artworkUri) {
                                                    ImageRequest.Builder(context)
                                                        .data(album.artworkUri)
                                                        .size(144)
                                                        .crossfade(false)
                                                        .build()
                                                }
                                                AsyncImage(
                                                    model = imageModel,
                                                    contentDescription = null,
                                                    onError = { onArtworkError(album.artworkUri.toString()) },
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Album,
                                                    contentDescription = null,
                                                    tint = VesperTextPrimary,
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .align(Alignment.Center)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = album.name,
                                                    style = VesperTypography.bodyLarge.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = VesperTextPrimary
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                AudiusSourceBadge()
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${album.artist} • ${album.trackCount} tracks",
                                                style = VesperTypography.bodySmall.copy(color = VesperTextSecondary),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = VesperTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun DiscoverSkeletonRow() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = VesperSurfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VesperOutline, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VesperDeepBurgundy.copy(alpha = 0.55f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(VesperBackgroundSecondary)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(VesperBackgroundSecondary)
                )
            }
        }
    }
}

@Composable
private fun AudiusSourceBadge(label: String = "Audius") {
    val isLocal = label.equals("Local", ignoreCase = true)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isLocal) VesperBackgroundSecondary else VesperBurgundyAccent
    ) {
        Text(
            text = label,
            style = VesperTypography.labelSmall.copy(
                color = if (isLocal) VesperBurgundyHighlight else VesperTextPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun SearchSuggestionRow(
    title: String,
    subtitle: String,
    typeBadge: String,
    sourceBadge: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = VesperBackgroundSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = VesperDeepBurgundy
                ) {
                    Text(
                        text = typeBadge,
                        style = VesperTypography.labelSmall.copy(
                            color = VesperBurgundyHighlight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = VesperTypography.bodyMedium.copy(
                            color = VesperTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = VesperTypography.bodySmall.copy(
                            color = VesperTextSecondary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            AudiusSourceBadge(label = sourceBadge)
        }
    }
}

@Composable
private fun DiscoverTrackRow(
    track: Track,
    isPlaying: Boolean,
    isCurrentTrack: Boolean,
    isFavorite: Boolean,
    sourceLabel: String = "Audius",
    onArtworkError: (String?) -> Unit = {},
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val dynamicAccent = LocalVesperAccentColor.current
    val context = LocalContext.current

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrentTrack) VesperDeepBurgundy else VesperSurfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isCurrentTrack) dynamicAccent else VesperOutline,
                RoundedCornerShape(12.dp)
            )
            .testTag("discover_track_${track.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VesperBackgroundSecondary)
            ) {
                if (track.artworkUri != null && track.artworkUri != Uri.EMPTY) {
                    val imageModel = remember(context, track.artworkUri) {
                        ImageRequest.Builder(context)
                            .data(track.artworkUri)
                            .size(128)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = imageModel,
                        contentDescription = null,
                        onError = { onArtworkError(track.artworkUri.toString()) },
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = VesperTextPrimary,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.title,
                        style = VesperTypography.bodyLarge.copy(
                            fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrentTrack) dynamicAccent else VesperTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    AudiusSourceBadge(label = sourceLabel)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${track.artist} • ${track.formattedDuration}",
                    style = VesperTypography.bodySmall.copy(color = VesperTextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) VesperBurgundyHighlight else VesperTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = VesperTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DiscoverCategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String = ""
) {
    val dynamicAccent = LocalVesperAccentColor.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) dynamicAccent else VesperBackgroundSecondary,
        modifier = Modifier
            .border(
                1.dp,
                if (selected) dynamicAccent else VesperOutline,
                RoundedCornerShape(10.dp)
            )
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
    ) {
        Text(
            text = label,
            style = VesperTypography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) VesperTextPrimary else VesperTextSecondary
            ),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}
