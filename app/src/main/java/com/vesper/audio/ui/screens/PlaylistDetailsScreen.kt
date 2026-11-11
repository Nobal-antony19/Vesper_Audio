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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.vesper.audio.ui.components.EmptyLibraryState
import com.vesper.audio.ui.components.MusicTrackRow
import com.vesper.audio.ui.components.TrackActionBottomSheet
import com.vesper.audio.ui.components.VesperPrimaryButton
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
fun PlaylistDetailsScreen(
    playlist: Playlist,
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onTrackClick: (Track, List<Track>) -> Unit,
    onPlayAllClick: (List<Track>) -> Unit,
    onShuffleClick: (List<Track>) -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onRemoveTrack: (Long, String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onBackClick: () -> Unit,
    onTogglePlayPause: (() -> Unit)? = null,
    onAddToQueue: ((Track) -> Unit)? = null,
    onPlayNext: ((Track) -> Unit)? = null,
    onOpenEditMetadata: ((Track) -> Unit)? = null,
    onOpenTrackInfo: ((Track) -> Unit)? = null,
    onOpenDeleteTrack: ((Track) -> Unit)? = null,
    allTracks: List<Track> = emptyList(),
    onAddTracksToPlaylist: ((Long, List<Track>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }
    var addSongsQuery by remember { mutableStateOf("") }
    var selectedTrackIdsToAdd by remember { mutableStateOf<Set<String>>(emptySet()) }
    var newPlaylistName by remember { mutableStateOf(playlist.name) }
    var activeActionTrack by remember { mutableStateOf<Track?>(null) }
    val dynamicAccent = com.vesper.audio.ui.theme.LocalVesperAccentColor.current

    val totalDurationMs = remember(tracks) { tracks.sumOf { it.durationMs } }
    val formattedDuration = remember(totalDurationMs) {
        val totalSec = totalDurationMs / 1000
        val mins = totalSec / 60
        val hours = mins / 60
        val remainingMins = mins % 60
        if (hours > 0) "$hours hr $remainingMins min" else "$mins min"
    }

    val isCurrentPlaylistPlaying = currentTrack != null && tracks.any { it.id == currentTrack.id }
    val isPlaybackActive = isCurrentPlaylistPlaying && isPlaying

    BackHandler(onBack = onBackClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VesperBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("playlist_details_back_button")
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
                    text = "Playlist",
                    style = VesperTypography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = VesperTextPrimary
                    )
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.testTag("playlist_options_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = VesperTextPrimary
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(VesperSurfaceElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename Playlist", color = VesperTextPrimary) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = dynamicAccent)
                        },
                        onClick = {
                            showMenu = false
                            newPlaylistName = playlist.name
                            showRenameDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Playlist", color = androidx.compose.ui.graphics.Color(0xFFFF6B6B)) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFFFF6B6B))
                        },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Section
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = VesperSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VesperOutline, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(VesperDeepBurgundy)
                                .border(1.5.dp, VesperBorderAccent, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = VesperTextPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = playlist.name,
                            style = VesperTypography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = VesperTextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${tracks.size} tracks • $formattedDuration",
                            style = VesperTypography.bodyMedium.copy(color = VesperTextMuted)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Working Play/Pause All and Shuffle Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (isCurrentPlaylistPlaying && onTogglePlayPause != null) {
                                        onTogglePlayPause()
                                    } else {
                                        onPlayAllClick(tracks)
                                    }
                                },
                                enabled = tracks.isNotEmpty(),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = dynamicAccent,
                                    contentColor = VesperTextPrimary,
                                    disabledContainerColor = VesperOutline,
                                    disabledContentColor = VesperTextMuted
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("playlist_play_all_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaybackActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPlaybackActive) "Pause" else "Play All",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = { onShuffleClick(tracks) },
                                enabled = tracks.isNotEmpty(),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = VesperTextPrimary,
                                    disabledContentColor = VesperTextMuted
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, VesperBorderAccent),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("playlist_shuffle_button")
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shuffle", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (onAddTracksToPlaylist != null && allTracks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    addSongsQuery = ""
                                    selectedTrackIdsToAdd = emptySet()
                                    showAddSongsDialog = true
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = VesperTextPrimary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, VesperOutline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("playlist_add_songs_button")
                            ) {
                                Text("+ Add Songs to Playlist", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            if (tracks.isEmpty()) {
                item {
                    EmptyLibraryState(
                        title = "Playlist is Empty",
                        message = "Add songs to '${playlist.name}' using the button below or from any track menu.",
                        actionButtonText = if (onAddTracksToPlaylist != null && allTracks.isNotEmpty()) "Add Songs" else "Back to Playlists",
                        onActionClick = {
                            if (onAddTracksToPlaylist != null && allTracks.isNotEmpty()) {
                                addSongsQuery = ""
                                selectedTrackIdsToAdd = emptySet()
                                showAddSongsDialog = true
                            } else {
                                onBackClick()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            } else {
                items(tracks, key = { it.id }) { track ->
                    val isCurrent = currentTrack?.id == track.id
                    MusicTrackRow(
                        track = track,
                        isCurrentTrack = isCurrent,
                        isPlaying = isPlaying,
                        onClick = {
                            if (isCurrent && onTogglePlayPause != null) {
                                onTogglePlayPause()
                            } else {
                                onTrackClick(track, tracks)
                            }
                        },
                        onFavoriteClick = { onFavoriteToggle(track) },
                        onMoreClick = {
                            activeActionTrack = track
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Contextual Track Menu BottomSheet (Explicit Remove from Playlist option)
    activeActionTrack?.let { track ->
        TrackActionBottomSheet(
            track = track,
            onDismiss = { activeActionTrack = null },
            onPlay = { onTrackClick(track, tracks) },
            onPlayNext = { onPlayNext?.invoke(track) },
            onAddToQueue = { onAddToQueue?.invoke(track) },
            onToggleFavorite = { onFavoriteToggle(track) },
            onAddToPlaylist = { /* opened via parent */ },
            onEditMetadata = { onOpenEditMetadata?.invoke(track) },
            onTrackInfo = { onOpenTrackInfo?.invoke(track) },
            onDelete = { onOpenDeleteTrack?.invoke(track) },
            onRemoveFromPlaylist = {
                onRemoveTrack(playlist.id, track.id)
            }
        )
    }

    // Rename Playlist Dialog (Preserves playlist ID and track relationships)
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = VesperSurfaceElevated,
            title = {
                Text(
                    text = "Rename Playlist",
                    style = VesperTypography.titleLarge.copy(color = VesperTextPrimary, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VesperBackgroundSecondary,
                        unfocusedContainerColor = VesperBackgroundSecondary,
                        focusedBorderColor = dynamicAccent,
                        unfocusedBorderColor = VesperOutline,
                        focusedTextColor = VesperTextPrimary,
                        unfocusedTextColor = VesperTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                VesperPrimaryButton(
                    text = "Rename",
                    enabled = newPlaylistName.isNotBlank(),
                    onClick = {
                        onRenamePlaylist(playlist.id, newPlaylistName.trim())
                        showRenameDialog = false
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = VesperTextSecondary)
                }
            }
        )
    }

    // Delete Playlist Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = VesperSurfaceElevated,
            title = {
                Text(
                    text = "Delete Playlist",
                    style = VesperTypography.titleLarge.copy(
                        color = androidx.compose.ui.graphics.Color(0xFFFF6B6B),
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${playlist.name}'? Audio files on your device will NOT be deleted.",
                    style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePlaylist(playlist.id)
                        showDeleteDialog = false
                        onBackClick()
                    }
                ) {
                    Text("Delete", color = androidx.compose.ui.graphics.Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = VesperTextSecondary)
                }
            }
        )
    }

    if (showAddSongsDialog && onAddTracksToPlaylist != null) {
        val existingIds = remember(tracks) { tracks.map { it.id }.toSet() }
        val candidateTracks = remember(allTracks, existingIds, addSongsQuery) {
            val available = allTracks.filter { it.id !in existingIds }
            if (addSongsQuery.isBlank()) {
                available
            } else {
                val q = addSongsQuery.trim().lowercase()
                available.filter {
                    it.title.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q) ||
                        it.album.lowercase().contains(q)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showAddSongsDialog = false },
            containerColor = VesperSurfaceElevated,
            title = {
                Text(
                    text = "Add Songs to ${playlist.name}",
                    style = VesperTypography.titleMedium.copy(
                        color = VesperTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = addSongsQuery,
                        onValueChange = { addSongsQuery = it },
                        placeholder = { Text("Search songs...", color = VesperTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = VesperBackgroundSecondary,
                            unfocusedContainerColor = VesperBackgroundSecondary,
                            focusedBorderColor = dynamicAccent,
                            unfocusedBorderColor = VesperOutline,
                            focusedTextColor = VesperTextPrimary,
                            unfocusedTextColor = VesperTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(candidateTracks, key = { it.id }) { candidate ->
                            val isPicked = candidate.id in selectedTrackIdsToAdd
                            Surface(
                                onClick = {
                                    selectedTrackIdsToAdd = if (isPicked) {
                                        selectedTrackIdsToAdd - candidate.id
                                    } else {
                                        selectedTrackIdsToAdd + candidate.id
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPicked) VesperDeepBurgundy else VesperBackgroundSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isPicked) VesperBorderAccent else VesperOutline,
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = candidate.title,
                                            style = VesperTypography.bodyMedium.copy(
                                                color = VesperTextPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = candidate.artist,
                                            style = VesperTypography.bodySmall.copy(
                                                color = VesperTextSecondary,
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = if (isPicked) "Selected" else "Add",
                                        style = VesperTypography.labelSmall.copy(
                                            color = if (isPicked) VesperTextPrimary else dynamicAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                VesperPrimaryButton(
                    text = "Add (${selectedTrackIdsToAdd.size})",
                    enabled = selectedTrackIdsToAdd.isNotEmpty(),
                    onClick = {
                        val tracksToAdd = allTracks.filter { it.id in selectedTrackIdsToAdd }
                        onAddTracksToPlaylist(playlist.id, tracksToAdd)
                        showAddSongsDialog = false
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddSongsDialog = false }) {
                    Text("Cancel", color = VesperTextSecondary)
                }
            }
        )
    }
}
