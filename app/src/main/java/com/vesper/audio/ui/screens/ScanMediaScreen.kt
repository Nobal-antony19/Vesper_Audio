package com.vesper.audio.ui.screens

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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesper.audio.data.local.ScannedFolderEntity
import com.vesper.audio.ui.components.GoldPrimaryButton
import com.vesper.audio.ui.components.SectionHeader
import com.vesper.audio.ui.theme.VesperBackground
import com.vesper.audio.ui.theme.VesperBackgroundSecondary
import com.vesper.audio.ui.theme.VesperBurgundyAccent
import com.vesper.audio.ui.theme.VesperDeepBurgundy
import com.vesper.audio.ui.theme.VesperOutline
import com.vesper.audio.ui.theme.VesperSurfaceElevated
import com.vesper.audio.ui.theme.VesperTextMuted
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTextSecondary
import com.vesper.audio.ui.theme.VesperTypography

@Composable
fun ScanMediaScreen(
    isLoading: Boolean,
    trackCount: Int,
    albumCount: Int,
    artistCount: Int,
    scannedFolders: List<ScannedFolderEntity>,
    onRescanClick: () -> Unit,
    onAddFolderClick: () -> Unit,
    onDeleteFolder: (ScannedFolderEntity) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.activity.compose.BackHandler(onBack = onBackClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VesperBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(48.dp).testTag("scan_media_back_button")
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
                text = "Scan Media",
                style = VesperTypography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = VesperTextPrimary
                )
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats summary card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = VesperDeepBurgundy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VesperBurgundyAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Library Telemetry",
                                style = VesperTypography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = VesperTextPrimary
                                )
                            )
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = VesperTextPrimary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatPill(count = trackCount, label = "Songs")
                            StatPill(count = albumCount, label = "Albums")
                            StatPill(count = artistCount, label = "Artists")
                            StatPill(count = scannedFolders.size, label = "Folders")
                        }
                    }
                }
            }

            // Scanner Actions
            item {
                SectionHeader(title = "Scan Actions")
            }

            item {
                Surface(
                    onClick = onRescanClick,
                    shape = RoundedCornerShape(18.dp),
                    color = VesperSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VesperOutline, RoundedCornerShape(18.dp))
                        .testTag("rescan_mediastore_action")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(VesperDeepBurgundy),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = VesperTextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rescan MediaStore",
                                style = VesperTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Scan device-wide indexed audio and update tags",
                                style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    onClick = onAddFolderClick,
                    shape = RoundedCornerShape(18.dp),
                    color = VesperSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VesperOutline, RoundedCornerShape(18.dp))
                        .testTag("add_saf_folder_action")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(VesperDeepBurgundy),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = VesperTextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Add Music Folder (SAF)",
                                style = VesperTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Grant persistent Storage Access Framework permission",
                                style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                            )
                        }
                    }
                }
            }

            // Persisted Scanned Folders
            item {
                SectionHeader(title = "Persisted SAF Folders (${scannedFolders.size})")
            }

            if (scannedFolders.isEmpty()) {
                item {
                    Text(
                        text = "No custom folders added yet. You can add specific folders to scan custom audio paths.",
                        style = VesperTypography.bodySmall.copy(color = VesperTextMuted),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            } else {
                items(scannedFolders, key = { it.uriString }) { folder ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = VesperBackgroundSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, VesperOutline, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = VesperBurgundyAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.displayName,
                                    style = VesperTypography.titleMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                Text(
                                    text = folder.uriString,
                                    style = VesperTypography.bodySmall.copy(color = VesperTextMuted),
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = { onDeleteFolder(folder) },
                                modifier = Modifier.size(44.dp).testTag("delete_folder_${folder.displayName}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove folder permission",
                                    tint = VesperTextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = VesperTypography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = VesperTextPrimary
            )
        )
        Text(
            text = label,
            style = VesperTypography.labelSmall.copy(color = VesperTextSecondary)
        )
    }
}
