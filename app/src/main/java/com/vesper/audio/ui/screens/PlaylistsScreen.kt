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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesper.audio.model.Playlist
import com.vesper.audio.model.Track
import com.vesper.audio.ui.components.EmptyLibraryState
import com.vesper.audio.ui.components.VesperPrimaryButton
import com.vesper.audio.ui.theme.LocalVesperAccentColor
import com.vesper.audio.ui.theme.VesperBackground
import com.vesper.audio.ui.theme.VesperBackgroundSecondary
import com.vesper.audio.ui.theme.VesperBorderAccent
import com.vesper.audio.ui.theme.VesperBurgundyHighlight
import com.vesper.audio.ui.theme.VesperDeepBurgundy
import com.vesper.audio.ui.theme.VesperOutline
import com.vesper.audio.ui.theme.VesperSurfaceElevated
import com.vesper.audio.ui.theme.VesperTextMuted
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTextSecondary
import com.vesper.audio.ui.theme.VesperTypography

@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    allTracks: List<Track>,
    onPlaylistClick: (Playlist) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlayPlaylist: (Playlist, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val dynamicAccent = LocalVesperAccentColor.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var renameText by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    val filteredPlaylists = remember(playlists, searchQuery) {
        if (searchQuery.isBlank()) {
            playlists
        } else {
            val q = searchQuery.trim().lowercase()
            playlists.filter { it.name.lowercase().contains(q) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VesperBackground)
            .testTag("playlists_screen")
    ) {
        // Header Row with Create Playlist action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your Playlists",
                    style = VesperTypography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = VesperTextPrimary
                    )
                )
                Text(
                    text = "${playlists.size} ${if (playlists.size == 1) "collection" else "collections"}",
                    style = VesperTypography.bodySmall.copy(color = VesperTextMuted)
                )
            }

            Surface(
                onClick = {
                    newPlaylistName = ""
                    showCreateDialog = true
                },
                shape = RoundedCornerShape(16.dp),
                color = dynamicAccent,
                modifier = Modifier
                    .border(1.dp, VesperBorderAccent, RoundedCornerShape(16.dp))
                    .testTag("playlists_create_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Playlist",
                        tint = VesperTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "New Playlist",
                        style = VesperTypography.labelLarge.copy(
                            color = VesperTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // Filter search bar if there are playlists
        if (playlists.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Filter playlists...",
                        style = VesperTypography.bodyMedium.copy(color = VesperTextMuted)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search playlists",
                        tint = VesperTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = VesperTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = VesperSurfaceElevated,
                    unfocusedContainerColor = VesperSurfaceElevated,
                    focusedBorderColor = dynamicAccent,
                    unfocusedBorderColor = VesperOutline,
                    focusedTextColor = VesperTextPrimary,
                    unfocusedTextColor = VesperTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .testTag("playlists_search_input")
            )
        }

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyLibraryState(
                    title = "No Playlists Yet",
                    message = "Create your first personal playlist to organize local and favorite tracks.",
                    actionButtonText = "Create Playlist",
                    onActionClick = {
                        newPlaylistName = ""
                        showCreateDialog = true
                    }
                )
            }
        } else if (filteredPlaylists.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyLibraryState(
                    title = "No Matching Playlists",
                    message = "No playlist matches \"$searchQuery\".",
                    actionButtonText = "Clear Filter",
                    onActionClick = { searchQuery = "" }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("playlists_list"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredPlaylists, key = { it.id }) { playlist ->
                    var menuExpanded by remember { mutableStateOf(false) }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = VesperSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, VesperOutline, RoundedCornerShape(18.dp))
                            .clickable { onPlaylistClick(playlist) }
                            .testTag("playlist_card_${playlist.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Burgundy playlist badge
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(VesperDeepBurgundy)
                                    .border(1.dp, VesperBorderAccent, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = VesperBurgundyHighlight,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = VesperTypography.titleMedium.copy(
                                        color = VesperTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${playlist.trackCount} ${if (playlist.trackCount == 1) "song" else "songs"}",
                                    style = VesperTypography.bodySmall.copy(
                                        color = VesperTextSecondary,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (playlist.trackCount > 0) {
                                IconButton(
                                    onClick = { onPlayPlaylist(playlist, false) },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(VesperDeepBurgundy)
                                        .testTag("playlist_play_${playlist.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play ${playlist.name}",
                                        tint = VesperTextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { onPlayPlaylist(playlist, true) },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(VesperBackgroundSecondary)
                                        .testTag("playlist_shuffle_${playlist.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Shuffle ${playlist.name}",
                                        tint = VesperTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Box {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier.testTag("playlist_more_${playlist.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More options",
                                        tint = VesperTextMuted
                                    )
                                }

                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    modifier = Modifier.background(VesperSurfaceElevated)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Rename", color = VesperTextPrimary) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null, tint = dynamicAccent)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            renameText = playlist.name
                                            playlistToRename = playlist
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = Color(0xFFFF6B6B)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF6B6B))
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            playlistToDelete = playlist
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = VesperSurfaceElevated,
            title = {
                Text(
                    text = "Create Playlist",
                    style = VesperTypography.titleLarge.copy(
                        color = VesperTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Playlist name", color = VesperTextMuted) },
                    singleLine = true,
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
                        .testTag("create_playlist_name_input")
                )
            },
            confirmButton = {
                VesperPrimaryButton(
                    text = "Create",
                    enabled = newPlaylistName.isNotBlank(),
                    onClick = {
                        onCreatePlaylist(newPlaylistName.trim())
                        showCreateDialog = false
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = VesperTextSecondary)
                }
            }
        )
    }

    playlistToRename?.let { target ->
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            containerColor = VesperSurfaceElevated,
            title = {
                Text(
                    text = "Rename Playlist",
                    style = VesperTypography.titleLarge.copy(
                        color = VesperTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
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
                    text = "Save",
                    enabled = renameText.isNotBlank(),
                    onClick = {
                        onRenamePlaylist(target.id, renameText.trim())
                        playlistToRename = null
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { playlistToRename = null }) {
                    Text("Cancel", color = VesperTextSecondary)
                }
            }
        )
    }

    playlistToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = VesperSurfaceElevated,
            title = {
                Text(
                    text = "Delete Playlist",
                    style = VesperTypography.titleLarge.copy(
                        color = Color(0xFFFF6B6B),
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "Delete '${target.name}'? Audio files on your device will not be removed.",
                    style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePlaylist(target.id)
                        playlistToDelete = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancel", color = VesperTextSecondary)
                }
            }
        )
    }
}
