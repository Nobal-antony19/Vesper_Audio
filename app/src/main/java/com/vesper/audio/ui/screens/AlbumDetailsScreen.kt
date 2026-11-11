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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vesper.audio.model.Album
import com.vesper.audio.model.Track
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
fun AlbumDetailsScreen(
    album: Album,
    allTracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onTrackClick: (Track, List<Track>) -> Unit,
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
    onQueueAllClick: ((List<Track>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var activeActionTrack by remember { mutableStateOf<Track?>(null) }

    val albumTracks = remember(allTracks, album.name, album.artist, isRemote) {
        val normAlbum = album.name.trim().lowercase()
        val normArtist = album.artist.trim().lowercase()
        val matching = allTracks.filter {
            it.album.trim().lowercase() == normAlbum &&
                (normArtist.isBlank() || normArtist == "unknown artist" || it.artist.trim().lowercase() == normArtist || !isRemote)
        }.ifEmpty {
            allTracks.filter { it.album.equals(album.name, ignoreCase = true) }
        }
        if (isRemote) {
            matching.distinctBy { it.id }
        } else {
            matching.distinctBy { it.id }.sortedWith(
                compareBy(
                    { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE },
                    { it.title.lowercase() }
                )
            )
        }
    }

    val resolvedYear = remember(album.year, albumTracks) {
        if (album.year > 0) album.year else albumTracks.firstOrNull { it.year > 0 }?.year ?: 0
    }

    val totalDurationMs = remember(albumTracks) {
        albumTracks.sumOf { it.durationMs }
    }

    val formattedTotalDuration = remember(totalDurationMs) {
        val totalSec = totalDurationMs / 1000
        val mins = totalSec / 60
        val hours = mins / 60
        val remainingMins = mins % 60
        if (hours > 0) "$hours hr $remainingMins min" else "$mins min"
    }

    val isCurrentAlbumPlaying = currentTrack != null && albumTracks.any { it.id == currentTrack.id }
    val isPlaybackActive = isCurrentAlbumPlaying && isPlaying

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
                    .testTag("album_details_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to albums",
                    tint = VesperTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Album",
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
                .testTag("album_details_list"),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 48.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Section: Artwork, Title, Artist, Stats & Action Buttons
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Large Artwork Card
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(VesperDeepBurgundy)
                            .border(1.5.dp, VesperBorderAccent, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (album.artworkUri != null) {
                            AsyncImage(
                                model = album.artworkUri,
                                contentDescription = "Artwork for ${album.name}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = VesperTextPrimary,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = album.name,
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
                        text = album.artist,
                        style = VesperTypography.titleMedium.copy(
                            color = VesperTextSecondary,
                            fontWeight = FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = buildString {
                            if (resolvedYear > 0) {
                                append("$resolvedYear • ")
                            }
                            append("${albumTracks.size} ${if (albumTracks.size == 1) "track" else "tracks"} • $formattedTotalDuration")
                            append(if (isRemote) " • Online" else " • Local")
                        },
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
                                .testTag("album_loading_more_indicator")
                        ) {
                            Text(
                                text = "Loading complete album tracklist...",
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
                                if (isCurrentAlbumPlaying && onTogglePlayPause != null) {
                                    onTogglePlayPause()
                                } else {
                                    onPlayAllClick(albumTracks)
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
                                .testTag("album_play_all_button")
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
                            onClick = { onShuffleClick(albumTracks) },
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
                                .testTag("album_shuffle_button")
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

                    if (onQueueAllClick != null && albumTracks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { onQueueAllClick(albumTracks) },
                            shape = RoundedCornerShape(16.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(VesperOutline)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = VesperSurfaceElevated,
                                contentColor = VesperTextSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("album_queue_all_button")
                        ) {
                            Text(
                                text = "+ Add Entire Album to Queue (${albumTracks.size})",
                                style = VesperTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            // Divider / Section Heading
            item {
                Text(
                    text = "Tracklist (${albumTracks.size})",
                    style = VesperTypography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = VesperTextSecondary
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Track Rows in Album Track-Number Order
            itemsIndexed(albumTracks, key = { _, track -> track.id }) { index, track ->
                val trackNum = if (track.trackNumber > 0) track.trackNumber else index + 1
                val isCurrent = currentTrack?.id == track.id
                MusicTrackRow(
                    track = track,
                    isPlaying = isPlaying,
                    isCurrentTrack = isCurrent,
                    isFavorite = track.id in favoriteTrackIds,
                    leadingNumber = trackNum,
                    onClick = {
                        if (isCurrent && onTogglePlayPause != null) {
                            onTogglePlayPause()
                        } else {
                            // Pass natural album track order so Track N plays N, Next plays N+1, Previous plays N-1
                            onTrackClick(track, albumTracks)
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
                onTrackClick(track, albumTracks)
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
