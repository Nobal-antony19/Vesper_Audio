package com.vesper.audio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesper.audio.model.Playlist
import com.vesper.audio.model.Track
import com.vesper.audio.ui.theme.SonoraBackground
import com.vesper.audio.ui.theme.SonoraBorderGold
import com.vesper.audio.ui.theme.SonoraDivider
import com.vesper.audio.ui.theme.SonoraGoldBright
import com.vesper.audio.ui.theme.SonoraGoldPrimary
import com.vesper.audio.ui.theme.SonoraSurface
import com.vesper.audio.ui.theme.SonoraSurfaceElevated
import com.vesper.audio.ui.theme.SonoraTextPrimary
import com.vesper.audio.ui.theme.SonoraTextSecondary
import com.vesper.audio.ui.theme.Typography

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SonoraSurfaceElevated,
        title = {
            Text(
                text = "New Playlist",
                style = Typography.titleLarge.copy(color = SonoraGoldBright, fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                Text(
                    text = "Give your playlist a name:",
                    style = Typography.bodyMedium.copy(color = SonoraTextSecondary)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text("e.g. Night Drives", color = SonoraTextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SonoraGoldPrimary,
                        unfocusedBorderColor = SonoraDivider,
                        focusedTextColor = SonoraTextPrimary,
                        unfocusedTextColor = SonoraTextPrimary,
                        cursorColor = SonoraGoldBright
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            GoldPrimaryButton(
                text = "Create",
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SonoraTextSecondary)
            }
        }
    )
}

@Composable
fun AddToPlaylistDialog(
    track: Track,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    onCreateNewClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SonoraSurfaceElevated,
        title = {
            Column {
                Text(
                    text = "Add to Playlist",
                    style = Typography.titleLarge.copy(color = SonoraGoldBright, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = track.title,
                    style = Typography.bodyMedium.copy(color = SonoraTextSecondary)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = onCreateNewClick,
                    shape = RoundedCornerShape(12.dp),
                    color = SonoraSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SonoraBorderGold, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = SonoraGoldBright
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Create New Playlist",
                            style = Typography.titleMedium.copy(color = SonoraGoldBright)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (playlists.isEmpty()) {
                    Text(
                        text = "No playlists created yet.",
                        style = Typography.bodyMedium.copy(color = SonoraTextSecondary),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(playlists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onPlaylistSelected(playlist) }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = SonoraGoldPrimary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = playlist.name,
                                    style = Typography.titleMedium.copy(color = SonoraTextPrimary)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = SonoraTextSecondary)
            }
        }
    )
}
