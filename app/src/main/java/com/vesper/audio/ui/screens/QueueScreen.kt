package com.vesper.audio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vesper.audio.model.Track
import com.vesper.audio.ui.components.EmptyLibraryState
import com.vesper.audio.ui.components.MiniEqualizerIndicator
import com.vesper.audio.ui.components.MusicTrackRow
import com.vesper.audio.ui.components.TrackActionBottomSheet
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

@Composable
fun QueueScreen(
    currentTrack: Track?,
    isPlaying: Boolean,
    queue: List<Track>,
    favoriteTracks: List<Track>,
    onTrackSelect: (Track) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onClearQueue: () -> Unit,
    onBackClick: () -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onAddToQueue: ((Track) -> Unit)? = null,
    onPlayNext: ((Track) -> Unit)? = null,
    onAddToPlaylist: ((Track) -> Unit)? = null,
    onOpenEditMetadata: ((Track) -> Unit)? = null,
    onOpenTrackInfo: ((Track) -> Unit)? = null,
    onDeleteTrack: ((Track) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var activeActionTrack by remember { mutableStateOf<Track?>(null) }
    var actionSheetSourceTab by remember { mutableIntStateOf(0) }

    val tabTitles = listOf("Up Next", "Favorites")

    BackHandler(onBack = onBackClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VesperBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("queue_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VesperTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Playback Queue",
                    style = VesperTypography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = VesperTextPrimary
                    )
                )
            }

            if (selectedTab == 0 && queue.isNotEmpty()) {
                IconButton(
                    onClick = { showClearConfirmation = true },
                    modifier = Modifier.testTag("queue_clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Queue",
                        tint = VesperTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Navigation Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(VesperBackgroundSecondary)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            tabTitles.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) VesperBurgundyAccent else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(vertical = 10.dp)
                        .testTag("queue_tab_$index"),
                    contentAlignment = Alignment.Center
                ) {
                    val count = when (index) {
                        0 -> queue.size
                        1 -> favoriteTracks.size
                        else -> 0
                    }
                    Text(
                        text = "$title ($count)",
                        style = VesperTypography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) VesperTextPrimary else VesperTextMuted
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content
        when (selectedTab) {
            0 -> {
                // Up Next Queue
                if (queue.isEmpty() && currentTrack == null) {
                    EmptyLibraryState(
                        title = "Queue is Empty",
                        message = "Play a song or album to populate your queue.",
                        actionButtonText = "Back",
                        onActionClick = onBackClick
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentTrack?.let { track ->
                            item {
                                Text(
                                    text = "Now Playing",
                                    style = VesperTypography.labelLarge.copy(
                                        color = VesperTextMuted,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = VesperDeepBurgundy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.5.dp, VesperBorderAccent, RoundedCornerShape(16.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(VesperSurfaceElevated),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (track.artworkUri != null) {
                                                AsyncImage(
                                                    model = track.artworkUri,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    tint = VesperTextPrimary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = track.title,
                                                style = VesperTypography.titleMedium.copy(
                                                    color = VesperTextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = track.artist,
                                                style = VesperTypography.bodySmall.copy(color = VesperTextSecondary),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (isPlaying) {
                                            MiniEqualizerIndicator()
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Up Next (${queue.size})",
                                    style = VesperTypography.labelLarge.copy(
                                        color = VesperTextMuted,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        itemsIndexed(queue, key = { index, track -> "queue_${track.id}_$index" }) { index, track ->
                            val isCurrent = currentTrack?.id == track.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isCurrent) VesperSurfaceElevated else VesperBackgroundSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, if (isCurrent) VesperBurgundyAccent else VesperOutline, RoundedCornerShape(14.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clickable { onTrackSelect(track) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = VesperTypography.labelMedium.copy(color = VesperTextMuted),
                                        modifier = Modifier.width(24.dp)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            style = VesperTypography.titleMedium.copy(
                                                color = if (isCurrent) VesperTextPrimary else VesperTextPrimary.copy(alpha = 0.9f),
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = track.artist,
                                            style = VesperTypography.bodySmall.copy(color = VesperTextSecondary),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Move Up
                                    if (index > 0) {
                                        IconButton(
                                            onClick = { onMoveQueueItem(index, index - 1) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Move Up",
                                                tint = VesperTextMuted,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // Move Down
                                    if (index < queue.size - 1) {
                                        IconButton(
                                            onClick = { onMoveQueueItem(index, index + 1) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Move Down",
                                                tint = VesperTextMuted,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // Remove
                                    IconButton(
                                        onClick = { onRemoveFromQueue(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove from Queue",
                                            tint = VesperTextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Favorites with fully functioning 3-dot actions
                if (favoriteTracks.isEmpty()) {
                    EmptyLibraryState(
                        title = "No Favorites Yet",
                        message = "Tap the heart icon on any song to add it to your favorites."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(favoriteTracks, key = { "fav_${it.id}" }) { track ->
                            MusicTrackRow(
                                track = track,
                                isPlaying = currentTrack?.id == track.id && isPlaying,
                                isCurrentTrack = currentTrack?.id == track.id,
                                onClick = { onTrackSelect(track) },
                                onFavoriteClick = { onFavoriteToggle(track) },
                                onMoreClick = {
                                    activeActionTrack = track
                                    actionSheetSourceTab = 1
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Contextual Action Bottom Sheet for History & Favorites
    activeActionTrack?.let { track ->
        TrackActionBottomSheet(
            track = track,
            onDismiss = { activeActionTrack = null },
            onPlay = { onTrackSelect(track) },
            onPlayNext = { onPlayNext?.invoke(track) },
            onAddToQueue = { onAddToQueue?.invoke(track) },
            onToggleFavorite = { onFavoriteToggle(track) },
            onAddToPlaylist = { onAddToPlaylist?.invoke(track) },
            onEditMetadata = { onOpenEditMetadata?.invoke(track) },
            onTrackInfo = { onOpenTrackInfo?.invoke(track) },
            onDelete = { onDeleteTrack?.invoke(track) }
        )
    }

    // Clear queue confirmation dialog
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            containerColor = VesperSurfaceElevated,
            title = {
                Text(
                    text = "Clear Queue?",
                    style = VesperTypography.titleLarge.copy(color = VesperTextPrimary, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to clear all upcoming songs from the playback queue?",
                    style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearQueue()
                        showClearConfirmation = false
                    }
                ) {
                    Text("Clear All", color = VesperBurgundyAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel", color = VesperTextSecondary)
                }
            }
        )
    }
}
