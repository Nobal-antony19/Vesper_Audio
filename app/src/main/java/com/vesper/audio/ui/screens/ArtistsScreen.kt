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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.vesper.audio.model.Artist
import com.vesper.audio.ui.components.EmptyLibraryState
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

enum class ArtistSortMode {
    NAME,
    TRACK_COUNT,
    ALBUM_COUNT
}

@Composable
fun ArtistsScreen(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit,
    onScanRequest: () -> Unit,
    onOpenDrawer: () -> Unit,
    onLogoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var sortMode by remember { mutableStateOf(ArtistSortMode.NAME) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sortedArtists = remember(artists, sortMode) {
        when (sortMode) {
            ArtistSortMode.NAME -> artists.sortedBy { it.name.lowercase() }
            ArtistSortMode.TRACK_COUNT -> artists.sortedByDescending { it.trackCount }
            ArtistSortMode.ALBUM_COUNT -> artists.sortedByDescending { it.albumCount }
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
                        imageVector = Icons.Default.Person,
                        contentDescription = "Artists Icon",
                        tint = VesperTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Artists",
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
                        modifier = Modifier.testTag("artists_sort_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort Artists",
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
                                    "Artist Name (A-Z)",
                                    color = if (sortMode == ArtistSortMode.NAME) VesperBurgundyAccent else VesperTextPrimary
                                )
                            },
                            onClick = { sortMode = ArtistSortMode.NAME; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Track Count",
                                    color = if (sortMode == ArtistSortMode.TRACK_COUNT) VesperBurgundyAccent else VesperTextPrimary
                                )
                            },
                            onClick = { sortMode = ArtistSortMode.TRACK_COUNT; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Album Count",
                                    color = if (sortMode == ArtistSortMode.ALBUM_COUNT) VesperBurgundyAccent else VesperTextPrimary
                                )
                            },
                            onClick = { sortMode = ArtistSortMode.ALBUM_COUNT; showSortMenu = false }
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

        if (artists.isEmpty()) {
            EmptyLibraryState(
                title = "No Artists Found",
                message = "Scan your local audio files or add a music folder to index artist metadata.",
                actionButtonText = "Scan Media",
                onActionClick = onScanRequest,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("artists_list")
            ) {
                items(sortedArtists, key = { it.name }) { artist ->
                    Surface(
                        onClick = { onArtistClick(artist) },
                        shape = RoundedCornerShape(16.dp),
                        color = VesperSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, VesperOutline, RoundedCornerShape(16.dp))
                            .testTag("artist_card_${artist.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(VesperDeepBurgundy)
                                    .border(1.dp, VesperBorderAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = VesperBurgundyAccent,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = artist.name,
                                    style = VesperTypography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = VesperTextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${artist.trackCount} tracks • ${artist.albumCount} albums",
                                    style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary)
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View Artist",
                                tint = VesperTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
