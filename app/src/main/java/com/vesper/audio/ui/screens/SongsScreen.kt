package com.vesper.audio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import com.vesper.audio.ui.components.AlphabeticalFastScrollRail
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesper.audio.model.Playlist
import com.vesper.audio.model.Track
import com.vesper.audio.ui.SortMode
import com.vesper.audio.ui.components.EmptyLibraryState
import com.vesper.audio.ui.components.MultiSelectActionBar
import com.vesper.audio.ui.components.MusicTrackRow
import com.vesper.audio.ui.components.SectionHeader
import com.vesper.audio.ui.components.VesperLogo
import com.vesper.audio.ui.theme.VesperBackground
import com.vesper.audio.ui.theme.VesperBackgroundSecondary
import com.vesper.audio.ui.theme.VesperBorderAccent
import com.vesper.audio.ui.theme.VesperBurgundyAccent
import com.vesper.audio.ui.theme.VesperDeepBurgundy
import com.vesper.audio.ui.theme.VesperOutline
import com.vesper.audio.ui.theme.VesperSurfaceElevated
import com.vesper.audio.ui.theme.VesperTextMuted
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTextSecondary
import com.vesper.audio.ui.theme.VesperTypography

enum class SongsFilterTab {
    ALL_SONGS,
    PLAYLISTS,
    FOLDERS
}

@Composable
fun SongsScreen(
    tracks: List<Track>,
    playlists: List<Playlist>,
    sortMode: SortMode,
    onSortSelected: (SortMode) -> Unit,
    onTrackClick: (Track) -> Unit,
    onTrackClickWithQueue: ((Track, List<Track>) -> Unit)? = null,
    onShuffleAllClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onPickSafFolderClick: () -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onMoreTrackClick: (Track) -> Unit,
    onOpenDrawer: () -> Unit,
    searchQuery: String = "",
    onQueryChange: (String) -> Unit = {},
    currentTrack: Track? = null,
    isPlaying: Boolean = false,
    favoriteTrackIds: Set<String> = emptySet(),
    onLogoClick: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onEnterMultiSelect: (Track) -> Unit = {},
    onToggleTrackSelection: (String) -> Unit = {},
    onSelectAll: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onBulkAddToQueue: () -> Unit = {},
    onBulkAddToFavorites: () -> Unit = {},
    onBulkAddToPlaylist: () -> Unit = {},
    onBulkDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(SongsFilterTab.ALL_SONGS) }
    val dynamicAccent = com.vesper.audio.ui.theme.LocalVesperAccentColor.current

    val displayedTracks = remember(tracks, searchQuery) {
        if (searchQuery.isBlank()) tracks
        else {
            val q = searchQuery.trim().lowercase()
            tracks.filter {
                it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q)
            }
        }
    }

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Transparent)
    ) {
        val isCompactHeight = maxHeight < 480.dp
        val verticalSpacerDp = if (isCompactHeight) 4.dp else 8.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = if (isCompactHeight) 4.dp else 8.dp)
        ) {
            // Multi-Select Bar or Standard Top Bar
            if (isSelectionMode) {
                MultiSelectActionBar(
                    selectedCount = selectedTrackIds.size,
                    totalCount = displayedTracks.size,
                    onSelectAll = onSelectAll,
                    onClearSelection = onClearSelection,
                    onAddToQueue = onBulkAddToQueue,
                    onAddToFavorites = onBulkAddToFavorites,
                    onAddToPlaylist = onBulkAddToPlaylist,
                    onDelete = onBulkDelete,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            } else {
                // Top Bar: Branding with Logo + Sort + Hamburger Menu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = if (isCompactHeight) 2.dp else 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = if (onLogoClick != null) Modifier.clickable(onClick = onLogoClick) else Modifier
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompactHeight) 32.dp else 38.dp)
                                .clip(CircleShape)
                                .background(VesperDeepBurgundy)
                                .border(1.2.dp, dynamicAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            VesperLogo(
                                size = if (isCompactHeight) 20.dp else 24.dp,
                                animated = false
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Songs",
                            style = VesperTypography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = VesperTextPrimary
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("songs_sort_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort Songs",
                                    tint = VesperTextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier
                                    .background(VesperSurfaceElevated)
                                    .border(1.dp, VesperOutline, RoundedCornerShape(12.dp))
                            ) {
                                SortMode.values().forEach { mode ->
                                    val isSelected = sortMode == mode
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = mode.displayName,
                                                color = if (isSelected) dynamicAccent else VesperTextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            onSortSelected(mode)
                                            showSortMenu = false
                                        },
                                        modifier = Modifier.testTag("sort_${mode.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier
                                .size(44.dp)
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
            }

            Spacer(modifier = Modifier.height(verticalSpacerDp))

            // In-line Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Filter songs, artists, albums...",
                        style = VesperTypography.bodyMedium.copy(color = VesperTextMuted)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = VesperTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = VesperTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = VesperBackgroundSecondary,
                    unfocusedContainerColor = VesperBackgroundSecondary,
                    focusedBorderColor = dynamicAccent,
                    unfocusedBorderColor = VesperOutline,
                    focusedTextColor = VesperTextPrimary,
                    unfocusedTextColor = VesperTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompactHeight) 48.dp else 52.dp)
                    .testTag("songs_search_field")
            )

            Spacer(modifier = Modifier.height(verticalSpacerDp))

            // Horizontal Category Tabs: All, Playlists, Folders
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryFilterChip(
                    label = "All Songs (${tracks.size})",
                    selected = selectedFilter == SongsFilterTab.ALL_SONGS,
                    onClick = { selectedFilter = SongsFilterTab.ALL_SONGS },
                    testTag = "filter_all_songs"
                )
                CategoryFilterChip(
                    label = "Playlists (${playlists.size})",
                    selected = selectedFilter == SongsFilterTab.PLAYLISTS,
                    onClick = { selectedFilter = SongsFilterTab.PLAYLISTS },
                    testTag = "filter_playlists"
                )
                CategoryFilterChip(
                    label = "Folders",
                    selected = selectedFilter == SongsFilterTab.FOLDERS,
                    onClick = { selectedFilter = SongsFilterTab.FOLDERS },
                    testTag = "filter_folders"
                )
            }

            Spacer(modifier = Modifier.height(verticalSpacerDp))

            // Tab Content with bounded weight(1f) so LazyColumn always measures and scrolls to the final item in portrait & landscape
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedFilter) {
                    SongsFilterTab.ALL_SONGS -> {
                        if (displayedTracks.isEmpty()) {
                            EmptyLibraryState(
                                title = if (searchQuery.isBlank()) "No Songs Found" else "No matching tracks",
                                message = if (searchQuery.isBlank()) "Your audio library is empty. Import or scan media to listen." else "No songs match '$searchQuery'",
                                actionButtonText = if (searchQuery.isBlank()) "Import Music" else "Clear Filter",
                                onActionClick = {
                                    if (searchQuery.isNotBlank()) onQueryChange("")
                                    else onPickSafFolderClick()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val listState = rememberLazyListState()
                            val coroutineScope = rememberCoroutineScope()

                            val letterIndexMap = remember(displayedTracks) {
                                val map = mutableMapOf<Char, Int>()
                                displayedTracks.forEachIndexed { index, track ->
                                    val firstChar = track.title.trim().firstOrNull()?.uppercaseChar() ?: '#'
                                    val key = if (firstChar in 'A'..'Z') firstChar else '#'
                                    if (!map.containsKey(key)) {
                                        map[key] = index
                                    }
                                }
                                map
                            }
                            val availableLetters = remember(letterIndexMap) { letterIndexMap.keys.toSet() }

                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp, end = 26.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("all_songs_list")
                                ) {
                                    item {
                                        SectionHeader(
                                            title = "${displayedTracks.size} Tracks",
                                            actionText = if (!isSelectionMode) "Shuffle All" else null,
                                            onActionClick = if (!isSelectionMode) onShuffleAllClick else null
                                        )
                                    }

                                    items(displayedTracks, key = { it.id }, contentType = { "track" }) { track ->
                                        val isSelected = track.id in selectedTrackIds
                                        val isCurrent = currentTrack?.id == track.id
                                        MusicTrackRow(
                                            track = track,
                                            isPlaying = isPlaying && isCurrent,
                                            isCurrentTrack = isCurrent,
                                            isFavorite = track.id in favoriteTrackIds,
                                            isSelected = isSelected,
                                            isSelectionMode = isSelectionMode,
                                            onLongClick = { onEnterMultiSelect(track) },
                                            onClick = {
                                                if (isSelectionMode) {
                                                    onToggleTrackSelection(track.id)
                                                } else if (onTrackClickWithQueue != null) {
                                                    onTrackClickWithQueue(track, displayedTracks)
                                                } else {
                                                    onTrackClick(track)
                                                }
                                            },
                                            onFavoriteClick = { onFavoriteToggle(track) },
                                            onMoreClick = { onMoreTrackClick(track) }
                                        )
                                    }
                                }

                                if (displayedTracks.size >= 10 && !isSelectionMode) {
                                    AlphabeticalFastScrollRail(
                                        availableLetters = availableLetters,
                                        onLetterSelected = { char ->
                                            val targetIndex = letterIndexMap[char] ?: 0
                                            coroutineScope.launch {
                                                listState.scrollToItem((targetIndex + 1).coerceAtMost(displayedTracks.size))
                                            }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    SongsFilterTab.PLAYLISTS -> {
                        LazyColumn(
                            contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("playlists_list")
                        ) {
                            item {
                                Surface(
                                    onClick = onCreatePlaylistClick,
                                    shape = RoundedCornerShape(18.dp),
                                    color = VesperSurfaceElevated,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, VesperBorderAccent, RoundedCornerShape(18.dp))
                                        .testTag("create_playlist_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(dynamicAccent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = VesperTextPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = "Create Playlist",
                                                style = VesperTypography.titleMedium.copy(
                                                    color = VesperTextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Text(
                                                text = "Organise your music collection",
                                                style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary)
                                            )
                                        }
                                    }
                                }
                            }

                            if (playlists.isEmpty()) {
                                item {
                                    EmptyLibraryState(
                                        title = "No Playlists Yet",
                                        message = "Create your first playlist or add songs from your library.",
                                        actionButtonText = "Create Playlist",
                                        onActionClick = onCreatePlaylistClick
                                    )
                                }
                            } else {
                                items(playlists, key = { it.id }) { playlist ->
                                    Surface(
                                        onClick = { onPlaylistClick(playlist) },
                                        shape = RoundedCornerShape(16.dp),
                                        color = VesperSurfaceElevated,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, VesperOutline, RoundedCornerShape(16.dp))
                                            .testTag("playlist_item_${playlist.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(VesperDeepBurgundy)
                                                    .border(1.dp, VesperOutline, RoundedCornerShape(12.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.QueueMusic,
                                                    contentDescription = null,
                                                    tint = VesperTextPrimary
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = playlist.name,
                                                    style = VesperTypography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = VesperTextPrimary
                                                    )
                                                )
                                                Text(
                                                    text = "${playlist.trackCount} tracks",
                                                    style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SongsFilterTab.FOLDERS -> {
                        LazyColumn(
                            contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("folders_list")
                        ) {
                            item {
                                Surface(
                                    onClick = onPickSafFolderClick,
                                    shape = RoundedCornerShape(18.dp),
                                    color = VesperSurfaceElevated,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, VesperBorderAccent, RoundedCornerShape(18.dp))
                                        .testTag("import_saf_folder_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(dynamicAccent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FolderOpen,
                                                contentDescription = null,
                                                tint = VesperTextPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = "Import Music Folder",
                                                style = VesperTypography.titleMedium.copy(
                                                    color = VesperTextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Text(
                                                text = "Select a folder via Storage Access Framework",
                                                style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary)
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
private fun CategoryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val accent = com.vesper.audio.ui.theme.LocalVesperAccentColor.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent else VesperSurfaceElevated,
        modifier = Modifier
            .border(1.dp, if (selected) VesperBorderAccent else VesperOutline, RoundedCornerShape(16.dp))
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = VesperTypography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) VesperTextPrimary else VesperTextSecondary
            ),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
