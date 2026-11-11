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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.vesper.audio.model.Album
import com.vesper.audio.ui.components.AlbumArtworkCard
import com.vesper.audio.ui.components.EmptyLibraryState
import com.vesper.audio.ui.theme.VesperBackground
import com.vesper.audio.ui.theme.VesperBurgundyAccent
import com.vesper.audio.ui.theme.VesperDeepBurgundy
import com.vesper.audio.ui.theme.VesperSurfaceElevated
import com.vesper.audio.ui.theme.VesperTextMuted
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTextSecondary
import com.vesper.audio.ui.theme.VesperTypography

enum class AlbumSortMode {
    NAME,
    ARTIST,
    TRACK_COUNT
}

@Composable
fun AlbumsScreen(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    onScanRequest: () -> Unit,
    onOpenDrawer: () -> Unit,
    onLogoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var sortMode by remember { mutableStateOf(AlbumSortMode.NAME) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sortedAlbums = remember(albums, sortMode) {
        when (sortMode) {
            AlbumSortMode.NAME -> albums.sortedBy { it.name.lowercase() }
            AlbumSortMode.ARTIST -> albums.sortedBy { it.artist.lowercase() }
            AlbumSortMode.TRACK_COUNT -> albums.sortedByDescending { it.trackCount }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VesperBackground)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Top Bar: Branding with Logo + Sort + Hamburger Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.then(
                    if (onLogoClick != null) Modifier.clickable { onLogoClick() } else Modifier
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(VesperDeepBurgundy)
                        .border(1.2.dp, VesperBurgundyAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = "Albums Icon",
                        tint = VesperTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Albums",
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
                        modifier = Modifier.testTag("albums_sort_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort Albums",
                            tint = VesperTextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(VesperSurfaceElevated)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Album Title (A-Z)",
                                    color = if (sortMode == AlbumSortMode.NAME) VesperBurgundyAccent else VesperTextPrimary
                                )
                            },
                            onClick = { sortMode = AlbumSortMode.NAME; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Artist Name",
                                    color = if (sortMode == AlbumSortMode.ARTIST) VesperBurgundyAccent else VesperTextPrimary
                                )
                            },
                            onClick = { sortMode = AlbumSortMode.ARTIST; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Track Count",
                                    color = if (sortMode == AlbumSortMode.TRACK_COUNT) VesperBurgundyAccent else VesperTextPrimary
                                )
                            },
                            onClick = { sortMode = AlbumSortMode.TRACK_COUNT; showSortMenu = false }
                        )
                    }
                }

                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .size(48.dp)
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

        Spacer(modifier = Modifier.height(8.dp))

        if (albums.isEmpty()) {
            EmptyLibraryState(
                title = "No Albums Found",
                message = "Scan your local audio files or add a music folder to index album metadata.",
                actionButtonText = "Scan Media",
                onActionClick = onScanRequest,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 104.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("albums_grid")
            ) {
                items(sortedAlbums, key = { it.name + it.artist }) { album ->
                    AlbumArtworkCard(
                        title = album.name,
                        subtitle = album.artist,
                        artworkUri = album.artworkUri,
                        onClick = { onAlbumClick(album) },
                        sizeDp = 160,
                        trackCount = album.trackCount,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
