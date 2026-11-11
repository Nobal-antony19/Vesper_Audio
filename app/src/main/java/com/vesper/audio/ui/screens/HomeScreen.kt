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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vesper.audio.model.Album
import com.vesper.audio.model.Track
import com.vesper.audio.ui.components.AlbumArtworkCard
import com.vesper.audio.ui.components.EmptyLibraryState
import com.vesper.audio.ui.components.SectionHeader
import com.vesper.audio.ui.theme.LocalVesperAccentColor
import com.vesper.audio.ui.theme.LocalVesperColors
import com.vesper.audio.ui.theme.VesperTypography

@Composable
fun HomeScreen(
    tracks: List<Track>,
    albums: List<Album>,
    favoriteTracks: List<Track>,
    recentlyAddedTracks: List<Track> = emptyList(),
    currentTrack: Track? = null,
    isPlaying: Boolean = false,
    shuffleMode: Boolean = false,
    favoriteTrackIds: Set<String> = emptySet(),
    onTrackClick: (Track) -> Unit,
    onTrackClickWithQueue: ((Track, List<Track>, String) -> Unit)? = null,
    onContinueListeningClick: ((Track) -> Unit)? = null,
    onAlbumClick: (Album) -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onMoreTrackClick: (Track) -> Unit,
    onShuffleAllClick: () -> Unit,
    onToggleShuffle: () -> Unit = {},
    onTogglePlayPause: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onScanRequest: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenFavorites: () -> Unit,
    onLogoClick: (() -> Unit)? = null,
    onPlayAlbumDirectly: ((Album) -> Unit)? = null,
    onSeeAllRecentlyAdded: (() -> Unit)? = null,
    hideRecentsOnHome: Boolean = false,
    hideMoodPicksOnHome: Boolean = false,
    hideTopicPicksOnHome: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalVesperColors.current
    val dynamicAccent = LocalVesperAccentColor.current

    if (tracks.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            com.vesper.audio.ui.components.VesperTopBar(
                title = "Vesper",
                onOpenDrawer = onOpenDrawer,
                onLogoClick = onLogoClick
            )
            EmptyLibraryState(
                title = "Your Library is Quiet",
                message = "Scan your local audio files or add a music folder to begin listening in pure cosmic fidelity.",
                actionButtonText = "Scan Media",
                onActionClick = onScanRequest,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
        return
    }

    val addedList = remember(recentlyAddedTracks, tracks) {
        val source = if (recentlyAddedTracks.isNotEmpty()) {
            recentlyAddedTracks
        } else {
            com.vesper.audio.data.MusicRepository.sortRecentlyAdded(tracks)
        }
        source.distinctBy { it.id }.take(20)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .testTag("home_lazy_column"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Bar: Branding with Logo + Top-Right Hamburger Menu
        item {
            com.vesper.audio.ui.components.VesperTopBar(
                title = "Vesper",
                onOpenDrawer = onOpenDrawer,
                onLogoClick = onLogoClick,
                modifier = Modifier.padding(horizontal = 0.dp)
            )
        }

        // Quick Search Bar Pill
        item {
            Surface(
                onClick = onOpenSearch,
                shape = RoundedCornerShape(24.dp),
                color = colors.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, colors.outline, RoundedCornerShape(24.dp))
                    .testTag("home_search_pill")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Search songs, albums, artists...",
                        style = VesperTypography.bodyMedium.copy(color = colors.textMuted)
                    )
                }
            }
        }

        // 2. Quick Action Cards: Favorites & Shuffle (governed by hideTopicPicksOnHome)
        if (!hideTopicPicksOnHome) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Favorites",
                        icon = Icons.Default.Favorite,
                        onClick = onOpenFavorites,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_favorites"
                    )
                    QuickActionCard(
                        title = if (shuffleMode) "Shuffle On" else "Shuffle All",
                        icon = Icons.Default.Shuffle,
                        iconTint = if (shuffleMode) dynamicAccent else colors.textSecondary,
                        onClick = onShuffleAllClick,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_shuffle"
                    )
                }
            }
        }

        // Functional "Continue Listening" Card when a track was restored or active
        if (currentTrack != null && !hideRecentsOnHome) {
            item {
                Surface(
                    onClick = {
                        if (onContinueListeningClick != null) {
                            onContinueListeningClick(currentTrack)
                        } else {
                            onTogglePlayPause()
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, dynamicAccent.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .testTag("continue_listening_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.accentContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentTrack.artworkUri != null) {
                                val req = remember(currentTrack.artworkUri) {
                                    ImageRequest.Builder(context)
                                        .data(currentTrack.artworkUri)
                                        .size(128)
                                        .crossfade(false)
                                        .build()
                                }
                                AsyncImage(
                                    model = req,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CONTINUE LISTENING",
                                style = VesperTypography.labelSmall.copy(
                                    color = dynamicAccent,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentTrack.title,
                                style = VesperTypography.titleMedium.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentTrack.artist,
                                style = VesperTypography.bodySmall.copy(color = colors.textSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Previous / Play-Pause / Next controls on Home Continue Listening card
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onSkipPrevious,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("home_skip_previous_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous Track",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(dynamicAccent)
                                    .testTag("home_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Continue",
                                    tint = colors.onAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            IconButton(
                                onClick = onSkipNext,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("home_skip_next_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Track",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Section: Recently Added (governed by hideRecentsOnHome)
        if (!hideRecentsOnHome) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recently Added",
                        style = VesperTypography.titleLarge.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${addedList.size} tracks",
                            style = VesperTypography.labelSmall.copy(color = colors.textMuted)
                        )
                        if (onSeeAllRecentlyAdded != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "See all",
                                style = VesperTypography.labelMedium.copy(color = dynamicAccent, fontWeight = FontWeight.Bold),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onSeeAllRecentlyAdded() }
                                    .padding(4.dp)
                                    .testTag("home_see_all_recently_added")
                            )
                        }
                    }
                }
            }

            item {
                if (addedList.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceElevated,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "No newly added tracks. Scan your library or import music files.",
                            style = VesperTypography.bodyMedium.copy(color = colors.textMuted),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier.testTag("recently_added_row")
                    ) {
                        items(addedList, key = { "added_${it.id}" }, contentType = { "track" }) { track ->
                            AlbumArtworkCard(
                                title = track.title,
                                subtitle = track.artist,
                                artworkUri = track.artworkUri,
                                onClick = {
                                    if (onTrackClickWithQueue != null) {
                                        onTrackClickWithQueue(track, addedList, "Recently Added")
                                    } else {
                                        onTrackClick(track)
                                    }
                                },
                                sizeDp = 120,
                                modifier = Modifier.testTag("recently_added_item_${track.id}")
                            )
                        }
                    }
                }
            }
        }

        // 4. Section: Albums Carousel
        if (albums.isNotEmpty()) {
            item {
                SectionHeader(title = "Albums (${albums.size})")
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(albums.take(12), key = { "album_${it.name}_${it.artist}" }) { album ->
                        AlbumArtworkCard(
                            title = album.name,
                            subtitle = album.artist,
                            artworkUri = album.artworkUri,
                            trackCount = album.trackCount,
                            onClick = { onAlbumClick(album) },
                            sizeDp = 130
                        )
                    }
                }
            }
        }

        // 5. Section: Favorites
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Favorites (${favoriteTracks.size})",
                    style = VesperTypography.titleLarge.copy(fontWeight = FontWeight.Bold, color = colors.textPrimary)
                )
                Text(
                    text = "View all",
                    style = VesperTypography.labelMedium.copy(color = dynamicAccent, fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenFavorites() }
                        .padding(4.dp)
                )
            }
        }

        item {
            if (favoriteTracks.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceElevated,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = dynamicAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Tap ♥ on any song to build your favourites.",
                            style = VesperTypography.bodyMedium.copy(color = colors.textMuted)
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(favoriteTracks, key = { "fav_card_${it.id}" }, contentType = { "track" }) { track ->
                        AlbumArtworkCard(
                            title = track.title,
                            subtitle = track.artist,
                            artworkUri = track.artworkUri,
                            onClick = {
                                if (onTrackClickWithQueue != null) {
                                    onTrackClickWithQueue(track, favoriteTracks, "Favorites")
                                } else {
                                    onTrackClick(track)
                                }
                            },
                            sizeDp = 130
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = LocalVesperAccentColor.current,
    testTag: String
) {
    val colors = LocalVesperColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceElevated,
        modifier = modifier
            .height(72.dp)
            .border(1.dp, colors.outline, RoundedCornerShape(16.dp))
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = VesperTypography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
