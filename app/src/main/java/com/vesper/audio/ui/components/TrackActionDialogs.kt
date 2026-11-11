package com.vesper.audio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesper.audio.model.Track
import com.vesper.audio.ui.theme.VesperBackgroundSecondary
import com.vesper.audio.ui.theme.VesperBurgundyAccent
import com.vesper.audio.ui.theme.VesperBurgundyHighlight
import com.vesper.audio.ui.theme.VesperOutline
import com.vesper.audio.ui.theme.VesperSurfaceElevated
import com.vesper.audio.ui.theme.VesperTextMuted
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTextSecondary
import com.vesper.audio.ui.theme.VesperTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionBottomSheet(
    track: Track,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEditMetadata: () -> Unit,
    onTrackInfo: () -> Unit,
    onDelete: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onOpenEqualizer: (() -> Unit)? = null,
    onClearQueue: (() -> Unit)? = null,
    onGoToAlbum: (() -> Unit)? = null,
    onGoToArtist: (() -> Unit)? = null,
    onGoToFolder: (() -> Unit)? = null,
    onShareTrack: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VesperSurfaceElevated,
        contentColor = VesperTextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(VesperOutline)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(scrollState)
        ) {
            // Track Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = VesperTypography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = VesperTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${track.artist} • ${track.album}",
                        style = VesperTypography.bodySmall.copy(color = VesperTextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Group 1: Playback
            ActionGroupCard(title = "Playback") {
                TrackActionRow(
                    icon = Icons.Default.PlayArrow,
                    title = "Play Now",
                    onClick = {
                        onDismiss()
                        onPlay()
                    }
                )
                ActionHairlineDivider()
                TrackActionRow(
                    icon = Icons.Default.SkipNext,
                    title = "Play Next",
                    onClick = {
                        onDismiss()
                        onPlayNext()
                    }
                )
                ActionHairlineDivider()
                TrackActionRow(
                    icon = Icons.Default.QueueMusic,
                    title = "Add to Queue",
                    onClick = {
                        onDismiss()
                        onAddToQueue()
                    }
                )
                if (onOpenEqualizer != null) {
                    ActionHairlineDivider()
                    TrackActionRow(
                        icon = Icons.Default.Tune,
                        title = "Equalizer",
                        onClick = {
                            onDismiss()
                            onOpenEqualizer()
                        }
                    )
                }
                if (onClearQueue != null) {
                    ActionHairlineDivider()
                    TrackActionRow(
                        icon = Icons.Default.ClearAll,
                        title = "Clear playing queue",
                        onClick = {
                            onDismiss()
                            onClearQueue()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Group 2: Navigate
            if (onGoToAlbum != null || onGoToArtist != null || onGoToFolder != null) {
                ActionGroupCard(title = "Navigate") {
                    if (onGoToAlbum != null) {
                        TrackActionRow(
                            icon = Icons.Default.Album,
                            title = "Go to album",
                            onClick = {
                                onDismiss()
                                onGoToAlbum()
                            }
                        )
                    }
                    if (onGoToArtist != null) {
                        ActionHairlineDivider()
                        TrackActionRow(
                            icon = Icons.Default.Person,
                            title = "Go to artist",
                            onClick = {
                                onDismiss()
                                onGoToArtist()
                            }
                        )
                    }
                    if (onGoToFolder != null) {
                        ActionHairlineDivider()
                        TrackActionRow(
                            icon = Icons.Default.Folder,
                            title = "Go to folder",
                            onClick = {
                                onDismiss()
                                onGoToFolder()
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Group 3: Organize
            ActionGroupCard(title = "Organize") {
                TrackActionRow(
                    icon = Icons.Default.PlaylistAdd,
                    title = "Add to playlist",
                    onClick = {
                        onDismiss()
                        onAddToPlaylist()
                    }
                )
                ActionHairlineDivider()
                TrackActionRow(
                    icon = if (track.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    title = if (track.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (track.isFavorite) VesperBurgundyHighlight else VesperTextPrimary,
                    onClick = {
                        onDismiss()
                        onToggleFavorite()
                    }
                )
                ActionHairlineDivider()
                TrackActionRow(
                    icon = Icons.Default.Edit,
                    title = "Tag editor / Rename",
                    onClick = {
                        onDismiss()
                        onEditMetadata()
                    }
                )
                if (onRemoveFromPlaylist != null) {
                    ActionHairlineDivider()
                    TrackActionRow(
                        icon = Icons.Default.Close,
                        title = "Remove from Playlist",
                        tint = Color(0xFFFF6E6E),
                        onClick = {
                            onDismiss()
                            onRemoveFromPlaylist()
                        }
                    )
                }

            }

            Spacer(modifier = Modifier.height(8.dp))

            // Group 4: Track
            ActionGroupCard(title = "Track") {
                TrackActionRow(
                    icon = Icons.Default.Info,
                    title = "Details",
                    onClick = {
                        onDismiss()
                        onTrackInfo()
                    }
                )
                if (onShareTrack != null) {
                    ActionHairlineDivider()
                    TrackActionRow(
                        icon = Icons.Default.Share,
                        title = "Share",
                        onClick = {
                            onDismiss()
                            onShareTrack()
                        }
                    )
                }
                ActionHairlineDivider()
                TrackActionRow(
                    icon = Icons.Default.Delete,
                    title = "Delete from device",
                    tint = Color(0xFFFF6B6B),
                    onClick = {
                        onDismiss()
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun ActionGroupCard(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = VesperTypography.labelSmall.copy(
                color = VesperTextMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = VesperBackgroundSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, VesperOutline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun ActionHairlineDivider() {
    HorizontalDivider(
        color = VesperOutline.copy(alpha = 0.35f),
        thickness = 0.8.dp,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@Composable
private fun TrackActionRow(
    icon: ImageVector,
    title: String,
    tint: Color = VesperTextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = VesperTypography.bodyLarge.copy(
                color = tint,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun RenameTrackDialog(
    track: Track,
    onDismiss: () -> Unit,
    onConfirm: (title: String, artist: String, album: String) -> Unit
) {
    var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artist) }
    var album by remember { mutableStateOf(track.album) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VesperSurfaceElevated,
        title = {
            Text(
                text = "Edit Track Information",
                style = VesperTypography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = VesperTextPrimary
                )
            )
        },
        text = {
            val fieldAccent = com.vesper.audio.ui.theme.LocalVesperAccentColor.current
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", color = VesperTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VesperBackgroundSecondary,
                        unfocusedContainerColor = VesperBackgroundSecondary,
                        focusedBorderColor = fieldAccent,
                        unfocusedBorderColor = VesperOutline,
                        focusedTextColor = VesperTextPrimary,
                        unfocusedTextColor = VesperTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist", color = VesperTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VesperBackgroundSecondary,
                        unfocusedContainerColor = VesperBackgroundSecondary,
                        focusedBorderColor = fieldAccent,
                        unfocusedBorderColor = VesperOutline,
                        focusedTextColor = VesperTextPrimary,
                        unfocusedTextColor = VesperTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album", color = VesperTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VesperBackgroundSecondary,
                        unfocusedContainerColor = VesperBackgroundSecondary,
                        focusedBorderColor = fieldAccent,
                        unfocusedBorderColor = VesperOutline,
                        focusedTextColor = VesperTextPrimary,
                        unfocusedTextColor = VesperTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            VesperPrimaryButton(
                text = "Save",
                enabled = title.isNotBlank(),
                onClick = {
                    onConfirm(title.trim(), artist.trim(), album.trim())
                    onDismiss()
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VesperTextSecondary)
            }
        }
    )
}

@Composable
fun TrackInfoDialog(
    track: Track,
    onDismiss: () -> Unit
) {
    val durationSeconds = track.durationMs / 1000
    val durationStr = String.format("%d:%02d", durationSeconds / 60, durationSeconds % 60)
    val sizeMb = if (track.sizeBytes > 0) String.format("%.2f MB", track.sizeBytes / (1024.0 * 1024.0)) else "Unknown"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VesperSurfaceElevated,
        title = {
            Text(
                text = "Track Information",
                style = VesperTypography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = VesperTextPrimary
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoItem(label = "Title", value = track.title)
                InfoItem(label = "Artist", value = track.artist)
                InfoItem(label = "Album", value = track.album)
                InfoItem(label = "Duration", value = durationStr)
                InfoItem(label = "File Format", value = track.mimeType.ifBlank { "Audio" })
                InfoItem(label = "File Size", value = sizeMb)
                InfoItem(label = "Location", value = track.contentUri.path?.ifBlank { track.contentUri.toString() } ?: track.contentUri.toString())
            }
        },
        confirmButton = {
            VesperPrimaryButton(
                text = "Close",
                onClick = onDismiss
            )
        }
    )
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(),
            style = VesperTypography.labelSmall.copy(
                color = VesperBurgundyHighlight,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = VesperTypography.bodyMedium.copy(color = VesperTextPrimary),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ConfirmDeleteDialog(
    itemCount: Int = 1,
    itemName: String = "",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VesperSurfaceElevated,
        title = {
            Text(
                text = if (itemCount > 1) "Delete $itemCount Tracks?" else "Delete Track?",
                style = VesperTypography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B6B)
                )
            )
        },
        text = {
            Text(
                text = if (itemCount > 1) {
                    "Are you sure you want to permanently delete $itemCount selected tracks from your device storage? This cannot be undone."
                } else {
                    "Are you sure you want to permanently delete '$itemName' from your device storage? This cannot be undone."
                },
                style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text("Delete", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VesperTextSecondary)
            }
        }
    )
}

@Composable
fun MultiSelectActionBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToFavorites: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = VesperSurfaceElevated,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, com.vesper.audio.ui.theme.LocalVesperAccentColor.current, RoundedCornerShape(20.dp))
            .testTag("multi_select_action_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onClearSelection,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Selection",
                        tint = VesperTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$selectedCount / $totalCount",
                    style = VesperTypography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = VesperTextPrimary
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onSelectAll,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = "Select All",
                        tint = VesperTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onAddToQueue,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Add to Queue",
                        tint = VesperTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onAddToFavorites,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Add to Favorites",
                        tint = VesperBurgundyHighlight,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onAddToPlaylist,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Add to Playlist",
                        tint = VesperTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Selected",
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
