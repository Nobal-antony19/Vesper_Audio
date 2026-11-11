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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesper.audio.model.Track
import com.vesper.audio.ui.components.EmptyLibraryState
import com.vesper.audio.ui.components.MusicTrackRow
import com.vesper.audio.ui.components.SectionHeader
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
fun FavoritesScreen(
    favoriteTracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onTrackClick: (Track, List<Track>) -> Unit,
    onPlayAllClick: (List<Track>) -> Unit,
    onShuffleClick: (List<Track>) -> Unit,
    onAddAllToQueue: (List<Track>) -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onMoreTrackClick: (Track) -> Unit,
    onBackClick: () -> Unit,
    onExploreSongsClick: () -> Unit,
    favoriteTrackIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val dynamicAccent = com.vesper.audio.ui.theme.LocalVesperAccentColor.current
    val totalDurationMs = remember(favoriteTracks) {
        favoriteTracks.sumOf { it.durationMs }
    }
    val formattedDuration = remember(totalDurationMs) {
        val totalSec = totalDurationMs / 1000
        val mins = totalSec / 60
        val hours = mins / 60
        val remMins = mins % 60
        if (hours > 0) "${hours}h ${remMins}m" else "${mins}m"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VesperBackground)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("favorites_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = VesperTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(VesperDeepBurgundy)
                    .border(1.2.dp, dynamicAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = dynamicAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Favourites",
                    style = VesperTypography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = VesperTextPrimary
                    )
                )
                Text(
                    text = if (favoriteTracks.isNotEmpty()) "${favoriteTracks.size} tracks • $formattedDuration" else "No saved tracks",
                    style = VesperTypography.labelSmall.copy(color = VesperTextMuted)
                )
            }
        }

        if (favoriteTracks.isEmpty()) {
            EmptyLibraryState(
                title = "No Favourites Yet",
                message = "Tap the heart icon on any track to save your favorite songs here for instant offline playback.",
                actionButtonText = "Explore Songs",
                onActionClick = onExploreSongsClick,
                modifier = Modifier.padding(top = 40.dp)
            )
        } else {
            // Action bar: Play All, Shuffle, Add All to Queue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onPlayAllClick(favoriteTracks) },
                    colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("favorites_play_all_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = VesperTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Play All",
                        style = VesperTypography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = VesperTextPrimary
                        )
                    )
                }

                OutlinedButton(
                    onClick = { onShuffleClick(favoriteTracks) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VesperTextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(VesperOutline)),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("favorites_shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = VesperTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Shuffle",
                        style = VesperTypography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = VesperTextPrimary
                        )
                    )
                }

                IconButton(
                    onClick = { onAddAllToQueue(favoriteTracks) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(VesperSurfaceElevated)
                        .border(1.dp, VesperOutline, RoundedCornerShape(16.dp))
                        .testTag("favorites_add_all_queue_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Add all to queue",
                        tint = VesperTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("favorites_track_list")
            ) {
                item {
                    SectionHeader(title = "All Saved Tracks (${favoriteTracks.size})")
                }

                items(favoriteTracks, key = { it.id }, contentType = { "track" }) { track ->
                    val isCurrent = currentTrack?.id == track.id
                    MusicTrackRow(
                        track = track,
                        isPlaying = isPlaying && isCurrent,
                        isCurrentTrack = isCurrent,
                        isFavorite = if (favoriteTrackIds.isNotEmpty()) track.id in favoriteTrackIds else true,
                        onClick = { onTrackClick(track, favoriteTracks) },
                        onFavoriteClick = { onFavoriteToggle(track) },
                        onMoreClick = { onMoreTrackClick(track) }
                    )
                }
            }
        }
    }
}
