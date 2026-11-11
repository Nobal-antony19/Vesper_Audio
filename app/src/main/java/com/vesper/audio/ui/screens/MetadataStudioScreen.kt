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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vesper.audio.model.Track
import com.vesper.audio.ui.components.SectionHeader
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
import com.vesper.audio.util.VesperValidationUtils

enum class MetadataMode {
    SINGLE,
    BATCH
}

@Composable
fun MetadataStudioScreen(
    currentTrack: Track?,
    allTracks: List<Track>,
    onSaveMetadata: (title: String, artist: String, album: String, year: Int, trackNumber: Int, genre: String) -> Unit,
    onBatchSaveMetadata: (Set<String>, String?, String?, Int?) -> Unit = { _, _, _, _ -> },
    onSelectTrack: (Track) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val accentColor = com.vesper.audio.ui.theme.LocalVesperAccentColor.current
    var mode by remember { mutableStateOf(MetadataMode.SINGLE) }

    // Single Edit Form Fields
    var titleInput by remember(currentTrack) { mutableStateOf(currentTrack?.title ?: "") }
    var artistInput by remember(currentTrack) { mutableStateOf(currentTrack?.artist ?: "") }
    var albumInput by remember(currentTrack) { mutableStateOf(currentTrack?.album ?: "") }
    var genreInput by remember(currentTrack) { mutableStateOf(currentTrack?.genre?.ifBlank { "Unknown" } ?: "") }
    var yearInput by remember(currentTrack) { mutableStateOf(if ((currentTrack?.year ?: 0) > 0) currentTrack!!.year.toString() else "2024") }
    var trackNumInput by remember(currentTrack) { mutableStateOf(if ((currentTrack?.trackNumber ?: 0) > 0) currentTrack!!.trackNumber.toString() else "1") }

    var saveStatusMessage by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Batch Edit Form Fields
    val selectedTrackIds = remember { mutableStateListOf<String>() }
    var batchArtistInput by remember { mutableStateOf("") }
    var batchAlbumInput by remember { mutableStateOf("") }
    var batchYearInput by remember { mutableStateOf("") }
    var batchStatusMessage by remember { mutableStateOf<String?>(null) }

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
                modifier = Modifier.size(48.dp).testTag("metadata_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = VesperTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Metadata Studio",
                style = VesperTypography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = VesperTextPrimary
                )
            )
        }

        // Mode Segmented Switch: Single Track vs Batch Edit
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = VesperBackgroundSecondary,
                modifier = Modifier.border(1.dp, VesperOutline, RoundedCornerShape(20.dp))
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    Surface(
                        onClick = { mode = MetadataMode.SINGLE },
                        shape = RoundedCornerShape(16.dp),
                        color = if (mode == MetadataMode.SINGLE) accentColor else Color.Transparent,
                        modifier = Modifier.testTag("metadata_mode_single")
                    ) {
                        Text(
                            text = "Single Track",
                            style = VesperTypography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (mode == MetadataMode.SINGLE) VesperTextPrimary else VesperTextSecondary
                            ),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }

                    Surface(
                        onClick = { mode = MetadataMode.BATCH },
                        shape = RoundedCornerShape(16.dp),
                        color = if (mode == MetadataMode.BATCH) accentColor else Color.Transparent,
                        modifier = Modifier.testTag("metadata_mode_batch")
                    ) {
                        Text(
                            text = "Batch Edit (${allTracks.size})",
                            style = VesperTypography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (mode == MetadataMode.BATCH) VesperTextPrimary else VesperTextSecondary
                            ),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Single primary vertically scrollable container
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("metadata_editor_list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (mode == MetadataMode.SINGLE) {
                // Track selector chip row
                if (allTracks.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Select Track to Edit")
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            allTracks.take(8).forEach { track ->
                                val isSelected = track.id == currentTrack?.id
                                Surface(
                                    onClick = {
                                        onSelectTrack(track)
                                        saveStatusMessage = null
                                        validationError = null
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) VesperDeepBurgundy else VesperSurfaceElevated,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            if (isSelected) accentColor else VesperOutline,
                                            RoundedCornerShape(14.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = if (isSelected) accentColor else VesperTextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = track.title,
                                                style = VesperTypography.titleSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = VesperTextPrimary
                                                )
                                            )
                                            Text(
                                                text = "${track.artist} • ${track.album}",
                                                style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = accentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Edit Fields Card
                item {
                    SectionHeader(title = "Embedded Audio Tags")
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = VesperSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, VesperOutline, RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Artwork Preview Row
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(VesperBackgroundSecondary)
                                        .border(1.dp, VesperBorderAccent, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentTrack?.artworkUri != null) {
                                        AsyncImage(
                                            model = currentTrack.artworkUri,
                                            contentDescription = "Cover Artwork",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = currentTrack?.title ?: "Select a Track",
                                        style = VesperTypography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = VesperTextPrimary
                                        )
                                    )
                                    Text(
                                        text = "Duration: ${currentTrack?.formattedDuration ?: "0:00"}",
                                        style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            MetadataInputBox(
                                label = "Track Title *",
                                value = titleInput,
                                onValueChange = { titleInput = it; validationError = null; saveStatusMessage = null },
                                testTag = "meta_title_input"
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            MetadataInputBox(
                                label = "Artist *",
                                value = artistInput,
                                onValueChange = { artistInput = it; validationError = null; saveStatusMessage = null },
                                testTag = "meta_artist_input"
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            MetadataInputBox(
                                label = "Album",
                                value = albumInput,
                                onValueChange = { albumInput = it; validationError = null; saveStatusMessage = null },
                                testTag = "meta_album_input"
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MetadataInputBox(
                                    label = "Genre",
                                    value = genreInput,
                                    onValueChange = { genreInput = it; saveStatusMessage = null },
                                    modifier = Modifier.weight(1f),
                                    testTag = "meta_genre_input"
                                )
                                MetadataInputBox(
                                    label = "Year",
                                    value = yearInput,
                                    onValueChange = { yearInput = it; saveStatusMessage = null },
                                    modifier = Modifier.weight(0.7f),
                                    testTag = "meta_year_input"
                                )
                                MetadataInputBox(
                                    label = "Track #",
                                    value = trackNumInput,
                                    onValueChange = { trackNumInput = it; saveStatusMessage = null },
                                    modifier = Modifier.weight(0.6f),
                                    testTag = "meta_track_num_input"
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Read-only File Properties & Override Persistence Info
                            if (currentTrack != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = VesperBackgroundSecondary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, VesperOutline, RoundedCornerShape(12.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "FILE & PERSISTENCE DETAILS",
                                            style = VesperTypography.labelSmall.copy(
                                                color = accentColor,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.8.sp
                                            )
                                        )
                                        Text(
                                            text = "Format: ${currentTrack.mimeType} • Duration: ${currentTrack.formattedDuration} • Size: ${String.format("%.1f MB", currentTrack.sizeBytes / (1024f * 1024f))}",
                                            style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                                        )
                                        Text(
                                            text = "Source: ${currentTrack.contentUri}",
                                            style = VesperTypography.bodySmall.copy(color = VesperTextMuted, fontSize = 11.sp),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Edits are saved as persistent VESPER Library Overrides and preserved across library rescans.",
                                            style = VesperTypography.bodySmall.copy(color = VesperTextSecondary, fontSize = 11.sp)
                                        )
                                    }
                                }
                            }

                            if (validationError != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = validationError!!,
                                    style = VesperTypography.bodySmall.copy(color = Color(0xFFE57373))
                                )
                            }

                            if (saveStatusMessage != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = saveStatusMessage!!,
                                    style = VesperTypography.bodyMedium.copy(color = accentColor, fontWeight = FontWeight.Bold)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            VesperPrimaryButton(
                                text = "Save Metadata to Library",
                                icon = Icons.Default.Save,
                                enabled = titleInput.isNotBlank() && artistInput.isNotBlank(),
                                onClick = {
                                    val (isValid, err) = VesperValidationUtils.validateMetadata(titleInput, artistInput, albumInput)
                                    if (!isValid) {
                                        validationError = err
                                    } else {
                                        val parsedYear = yearInput.trim().toIntOrNull()?.coerceIn(0, 2100) ?: 0
                                        val parsedTrackNum = trackNumInput.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
                                        onSaveMetadata(
                                            titleInput.trim(),
                                            artistInput.trim(),
                                            albumInput.trim(),
                                            parsedYear,
                                            parsedTrackNum,
                                            genreInput.trim()
                                        )
                                        saveStatusMessage = "Metadata validated and saved as persistent VESPER Library Override."
                                    }
                                },
                                testTag = "save_metadata_button",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                // Batch Mode View
                item {
                    SectionHeader(title = "Select Multiple Tracks")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedTrackIds.size} of ${allTracks.size} tracks selected",
                            style = VesperTypography.labelMedium.copy(color = accentColor, fontWeight = FontWeight.Bold)
                        )

                        Surface(
                            onClick = {
                                if (selectedTrackIds.size == allTracks.size) {
                                    selectedTrackIds.clear()
                                } else {
                                    selectedTrackIds.clear()
                                    selectedTrackIds.addAll(allTracks.map { it.id })
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = VesperSurfaceElevated,
                            modifier = Modifier.border(1.dp, VesperOutline, RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                text = if (selectedTrackIds.size == allTracks.size) "Deselect All" else "Select All",
                                style = VesperTypography.labelSmall.copy(color = VesperTextPrimary),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Batch Selection Track List
                items(allTracks, key = { it.id }) { track ->
                    val isChecked = track.id in selectedTrackIds
                    Surface(
                        onClick = {
                            if (isChecked) selectedTrackIds.remove(track.id)
                            else selectedTrackIds.add(track.id)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isChecked) VesperDeepBurgundy else VesperSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (isChecked) accentColor else VesperOutline, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedTrackIds.add(track.id)
                                    else selectedTrackIds.remove(track.id)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = accentColor,
                                    checkmarkColor = VesperTextPrimary,
                                    uncheckedColor = VesperOutline
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = VesperTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "${track.artist} • ${track.album}",
                                    style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                                )
                            }
                        }
                    }
                }

                // Batch Fields Card
                item {
                    SectionHeader(title = "Apply Values to Selected Tracks")
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = VesperSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, VesperOutline, RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            MetadataInputBox(
                                label = "Batch Artist (Leave empty to keep existing)",
                                value = batchArtistInput,
                                onValueChange = { batchArtistInput = it; batchStatusMessage = null }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            MetadataInputBox(
                                label = "Batch Album (Leave empty to keep existing)",
                                value = batchAlbumInput,
                                onValueChange = { batchAlbumInput = it; batchStatusMessage = null }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            MetadataInputBox(
                                label = "Batch Year",
                                value = batchYearInput,
                                onValueChange = { batchYearInput = it; batchStatusMessage = null }
                            )

                            if (batchStatusMessage != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = batchStatusMessage!!,
                                    style = VesperTypography.bodyMedium.copy(color = accentColor, fontWeight = FontWeight.Bold)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            VesperPrimaryButton(
                                text = "Apply to ${selectedTrackIds.size} Tracks",
                                enabled = selectedTrackIds.isNotEmpty() &&
                                    (batchArtistInput.isNotBlank() || batchAlbumInput.isNotBlank() || batchYearInput.isNotBlank()),
                                onClick = {
                                    val parsedYear = batchYearInput.toIntOrNull()
                                    onBatchSaveMetadata(
                                        selectedTrackIds.toSet(),
                                        batchArtistInput.takeIf { it.isNotBlank() },
                                        batchAlbumInput.takeIf { it.isNotBlank() },
                                        parsedYear
                                    )
                                    batchStatusMessage = "Successfully updated ${selectedTrackIds.size} tracks in library."
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataInputBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val inputAccent = com.vesper.audio.ui.theme.LocalVesperAccentColor.current
    Column(modifier = modifier) {
        Text(
            text = label,
            style = VesperTypography.labelSmall.copy(color = VesperTextSecondary, fontWeight = FontWeight.SemiBold)
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = inputAccent,
                unfocusedBorderColor = VesperOutline,
                focusedTextColor = VesperTextPrimary,
                unfocusedTextColor = VesperTextPrimary,
                cursorColor = inputAccent
            ),
            modifier = (if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier).fillMaxWidth()
        )
    }
}
