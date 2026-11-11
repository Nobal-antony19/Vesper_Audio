package com.vesper.audio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vesper.audio.model.Track
import com.vesper.audio.util.VesperArtworkResolver
import com.vesper.audio.ui.theme.VesperBackgroundSecondary
import com.vesper.audio.ui.theme.VesperBurgundyAccent
import com.vesper.audio.ui.theme.VesperOutline
import com.vesper.audio.ui.theme.VesperSurfaceElevated
import com.vesper.audio.ui.theme.VesperTextMuted
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTextSecondary
import com.vesper.audio.ui.theme.VesperTypography

@Composable
fun MiniPlayer(
    currentTrack: Track?,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    durationMs: Long,
    onPlayPauseClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onExpandClick: () -> Unit,
    onSkipPreviousClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = currentTrack != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        if (currentTrack == null) return@AnimatedVisibility

        val progress = if (durationMs > 0) {
            (playbackPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        val accentColor = com.vesper.audio.ui.theme.LocalVesperAccentColor.current
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = VesperSurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, VesperOutline, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .clickable(onClick = onExpandClick)
                .testTag("mini_player")
        ) {
            Column {
                // Top Edge Accent Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = accentColor,
                    trackColor = VesperOutline
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Artwork Thumbnail
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(VesperBackgroundSecondary)
                            .border(1.dp, VesperOutline, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        var imageLoadFailed by remember(currentTrack.artworkUri) { mutableStateOf(false) }
                        val artUri = currentTrack.artworkUri
                        val hasValidArt = artUri != null &&
                            !imageLoadFailed &&
                            !VesperArtworkResolver.isArtworkUriBroken(artUri.toString())

                        if (hasValidArt && artUri != null) {
                            AsyncImage(
                                model = artUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                onError = {
                                    imageLoadFailed = true
                                    VesperArtworkResolver.markArtworkUriBroken(artUri.toString())
                                }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title & Artist
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = currentTrack.title,
                            style = VesperTypography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = VesperTextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentTrack.artist,
                            style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Previous Button (when provided)
                    if (onSkipPreviousClick != null) {
                        IconButton(
                            onClick = onSkipPreviousClick,
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("mini_player_previous")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Track",
                                tint = VesperTextMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Play/Pause Button
                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("mini_player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = VesperTextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Next Button
                    IconButton(
                        onClick = onSkipNextClick,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("mini_player_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = VesperTextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
