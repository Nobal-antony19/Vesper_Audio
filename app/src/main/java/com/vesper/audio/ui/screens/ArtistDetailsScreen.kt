package com.vesper.audio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesper.audio.model.Album
import com.vesper.audio.model.Artist
import com.vesper.audio.model.Track
import com.vesper.audio.ui.components.AlbumArtworkCard
import com.vesper.audio.ui.components.MusicTrackRow
import com.vesper.audio.ui.components.TrackActionBottomSheet
import com.vesper.audio.ui.theme.VesperBackground
import com.vesper.audio.ui.theme.VesperBorderAccent
import com.vesper.audio.ui.theme.VesperBurgundyAccent
import com.vesper.audio.ui.theme.VesperDeepBurgundy
import com.vesper.audio.ui.theme.VesperOutline
import com.vesper.audio.ui.theme.VesperSurfaceElevated
import com.vesper.audio.ui.theme.VesperTextMuted
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTextSecondary
import com.vesper.audio.ui.theme.VesperTypography

@Composable
fun ArtistDetailsScreen(
    artist: Artist,
    allAlbums: List<Album>,
    allTracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onTrackClick: (Track, List<Track>) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onPlayAllClick: (List<Track>) -> Unit,
    onShuffleClick: (List<Track>) -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onMoreTrackClick: (Track) -> Unit,
    onBackClick: () -> Unit,
    onTogglePlayPause: (() -> Unit)? = null,
    onAddToQueue: ((Track) -> Unit)? = null,
    onPlayNext: ((Track) -> Unit)? = null,
    onOpenEditMetadata: ((Track) -> Unit)? = null,
    onOpenTrackInfo: ((Track) -> Unit)? = null,
    onOpenDeleteTrack: ((Track) -> Unit)? = null,
    favoriteTrackIds: Set<String> = emptySet(),
    isRemote: Boolean = false,
    isLoadingRemote: Boolean = false,
    modifier: Modifier = Modifier
) {
    var activeActionTrack by remember { mutableStateOf<Track?>(null) }

    val artistTracks = remember(allTracks, artist.name, isRemote) {
        val matching = allTracks.filter { it.artist.equals(artist.name, ignoreCase = true) }
            .distinctBy { it.id }
        if (isRemote) matching else matching.sortedBy { it.title.lowercase() }
    }

    val artistAlbums = remember(allAlbums, artist.name) {
        allAlbums.filter { it.artist.equals(artist.name, ignoreCase = true) }
    }

    val artistArtworkUri = remember(artistTracks, artistAlbums) {
        artistAlbums.firstNotNullOfOrNull { it.artworkUri }
            ?: artistTracks.firstNotNullOfOrNull { it.artworkUri }
    }

    val totalDurationMs = remember(artistTracks) {
        artistTracks.sumOf { it.durationMs }
    }

    val formattedTotalDuration = remember(totalDurationMs) {
        val totalSec = totalDurationMs / 1000
        val mins = totalSec / 60
        val hours = mins / 60
        val remainingMins = mins % 60
        if (hours > 0) "$hours hr $remainingMins min" else "$mins min"
    }

    val isCurrentArtistPlaying = currentTrack != null && artistTracks.any { it.id == currentTrack.id }
    val isPlaybackActive = isCurrentArtistPlaying && isPlaying

    BackHandler(onBack = onBackClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VesperBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Top Navigation Bar with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("artist_details_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to artists",
                    tint = VesperTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Artist",
                style = VesperTypography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = VesperTextPrimary
                )
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("artist_details_list"),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Artist Header: Large Avatar, Name, Stats & Action Buttons
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(VesperDeepBurgundy)
                            .border(2.dp, VesperBorderAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (artistArtworkUri != null) {
                            coil.compose.AsyncImage(
                                model = artistArtworkUri,
                                contentDescription = "Avatar for ${artist.name}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar for ${artist.name}",
                                tint = VesperTextPrimary,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = artist.name,
                        style = VesperTypography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = VesperTextPrimary
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${artistTracks.size} tracks • ${artistAlbums.size} albums • $formattedTotalDuration • ${if (isRemote) "Online" else "Local"}",
                        style = VesperTypography.bodySmall.copy(color = VesperTextMuted),
                        textAlign = TextAlign.Center
                    )

                    if (isLoadingRemote) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = VesperSurfaceElevated,
                            modifier = Modifier
                                .border(1.dp, VesperBorderAccent, RoundedCornerShape(10.dp))
                                .testTag("artist_loading_more_indicator")
                        ) {
                            Text(
                                text = "Loading complete artist catalogue...",
                                style = VesperTypography.labelSmall.copy(
                                    color = VesperTextSecondary,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action Buttons: Working Play/Pause All & Shuffle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isCurrentArtistPlaying && onTogglePlayPause != null) {
                                    onTogglePlayPause()
                                } else {
                                    onPlayAllClick(artistTracks)
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.vesper.audio.ui.theme.LocalVesperAccentColor.current,
                                contentColor = VesperTextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("artist_play_all_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaybackActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPlaybackActive) "Pause" else "Play All",
                                style = VesperTypography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        OutlinedButton(
                            onClick = { onShuffleClick(artistTracks) },
                            shape = RoundedCornerShape(20.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(VesperOutline)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = VesperSurfaceElevated,
                                contentColor = VesperTextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("artist_shuffle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = null,
                                tint = VesperTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Shuffle",
                                style = VesperTypography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Albums by this Artist
            if (artistAlbums.isNotEmpty()) {
                item {
                    Text(
                        text = "Albums (${artistAlbums.size})",
                        style = VesperTypography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = VesperTextSecondary
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(artistAlbums, key = { it.name }) { album ->
                            AlbumArtworkCard(
                                title = album.name,
                                subtitle = "${album.trackCount} tracks",
                                artworkUri = album.artworkUri,
                                onClick = { onAlbumClick(album) },
                                sizeDp = 130
                            )
                        }
                    }
                }
            }

            // All Songs by this Artist
            item {
                Text(
                    text = "Songs (${artistTracks.size})",
                    style = VesperTypography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = VesperTextSecondary
                    ),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            itemsIndexed(artistTracks, key = { _, track -> track.id }) { index, track ->
                val isCurrent = currentTrack?.id == track.id
                MusicTrackRow(
                    track = track,
                    isPlaying = isPlaying,
                    isCurrentTrack = isCurrent,
                    isFavorite = track.id in favoriteTrackIds,
                    leadingNumber = index + 1,
                    onClick = {
                        if (isCurrent && onTogglePlayPause != null) {
                            onTogglePlayPause()
                        } else {
                            onTrackClick(track, artistTracks)
                        }
                    },
                    onFavoriteClick = { onFavoriteToggle(track) },
                    onMoreClick = {
                        activeActionTrack = track
                    }
                )
            }
        }
    }

    // Contextual Track Menu
    activeActionTrack?.let { track ->
        TrackActionBottomSheet(
            track = track,
            onDismiss = { activeActionTrack = null },
            onPlay = {
                onTrackClick(track, artistTracks)
            },
            onPlayNext = { onPlayNext?.invoke(track) },
            onAddToQueue = { onAddToQueue?.invoke(track) },
            onToggleFavorite = { onFavoriteToggle(track) },
            onAddToPlaylist = { onMoreTrackClick(track) },
            onEditMetadata = { onOpenEditMetadata?.invoke(track) },
            onTrackInfo = { onOpenTrackInfo?.invoke(track) },
            onDelete = { onOpenDeleteTrack?.invoke(track) }
        )
    }
}
