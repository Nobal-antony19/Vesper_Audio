package com.vesper.audio.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vesper.audio.model.Track
import com.vesper.audio.ui.components.VesperSlider
import com.vesper.audio.ui.theme.VesperBackground
import com.vesper.audio.ui.theme.VesperBorderAccent
import com.vesper.audio.ui.theme.VesperBurgundyAccent
import com.vesper.audio.ui.theme.VesperBurgundyHighlight
import com.vesper.audio.ui.theme.VesperDeepBurgundy
import com.vesper.audio.ui.theme.VesperOutline
import com.vesper.audio.ui.theme.VesperSurfaceElevated
import com.vesper.audio.ui.theme.VesperTextMuted
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTextSecondary
import com.vesper.audio.ui.theme.VesperTypography
import com.vesper.audio.util.LyricLine
import com.vesper.audio.util.TrackLyrics
import com.vesper.audio.util.VesperLyricsResolver
import kotlin.math.abs

@Composable
fun NowPlayingScreen(
    currentTrack: Track?,
    isPlaying: Boolean,
    playbackPositionMs: Long,
    durationMs: Long,
    shuffleMode: Boolean,
    repeatMode: Int,
    onPlayPauseClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onSkipPreviousClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onFavoriteToggle: (Track) -> Unit,
    onCollapseClick: () -> Unit,
    onOpenEqualizerClick: () -> Unit,
    onOpenQueueClick: () -> Unit,
    onOpenSleepTimerClick: (() -> Unit)? = null,
    onOpenMoreOptions: (() -> Unit)? = null,
    queueSource: String = "All Songs",
    favoriteTrackIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    if (currentTrack == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(VesperBackground)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {}
                .windowInsetsPadding(WindowInsets.statusBars),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No track currently playing",
                style = VesperTypography.titleMedium.copy(color = VesperTextSecondary)
            )
        }
        return
    }

    val context = LocalContext.current
    val dynamicAccent = com.vesper.audio.ui.theme.LocalVesperAccentColor.current
    val colors = com.vesper.audio.ui.theme.LocalVesperColors.current
    val isFavorite = currentTrack.id in favoriteTrackIds || currentTrack.isFavorite

    var showLyrics by remember { mutableStateOf(false) }
    val totalDur = durationMs.coerceAtLeast(1L)

    // Lyrics resolution
    val trackLyrics = remember(currentTrack.id) {
        VesperLyricsResolver.resolveLyrics(context, currentTrack)
    }

    BackHandler(onBack = onCollapseClick)

    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 56.dp.toPx() }
    val verticalThresholdPx = with(density) { 72.dp.toPx() }

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(VesperBackground)
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        VesperDeepBurgundy.copy(alpha = 0.72f),
                        VesperBackground,
                        VesperBackground
                    )
                )
            )
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {}
            .testTag("now_playing_full_screen")
    ) {
        val isLandscape = maxWidth > maxHeight
        val artModifier = if (isLandscape) {
            Modifier
                .size((maxHeight * 0.44f).coerceIn(140.dp, 220.dp))
                .clip(RoundedCornerShape(16.dp))
        } else {
            Modifier
                .fillMaxWidth(0.86f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCollapseClick,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("now_playing_collapse")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "PLAYING FROM",
                    style = VesperTypography.labelSmall.copy(
                        color = colors.textMuted,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = queueSource.ifBlank { currentTrack.album.ifBlank { "My Library" } },
                    style = VesperTypography.titleMedium.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = { onOpenMoreOptions?.invoke() },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = VesperTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Center Artwork / Lyrics Area with Side Indicators and Gesture Detection
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(currentTrack.id) {
                    detectDragGestures(
                        onDragStart = {
                            totalDragX = 0f
                            totalDragY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y
                        },
                        onDragEnd = {
                            if (abs(totalDragX) > abs(totalDragY) && abs(totalDragX) > swipeThresholdPx) {
                                if (totalDragX < 0) {
                                    onSkipNextClick()
                                } else {
                                    onSkipPreviousClick()
                                }
                            } else if (totalDragY > verticalThresholdPx) {
                                onCollapseClick()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Center Panel: Clean 1:1 Square Borderless Artwork Frame OR Real Synced/Unsynced Lyrics Card
            if (showLyrics) {
                LyricsCard(
                    lyrics = trackLyrics,
                    playbackPositionMs = playbackPositionMs,
                    onSeekTo = onSeekTo,
                    modifier = artModifier
                )
            } else {
                Surface(
                    modifier = artModifier,
                    shape = RoundedCornerShape(16.dp),
                    color = VesperSurfaceElevated
                ) {
                    if (currentTrack.artworkUri != null && currentTrack.artworkUri != Uri.EMPTY) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(currentTrack.artworkUri)
                                .size(720)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Album artwork for ${currentTrack.title}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(VesperDeepBurgundy),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = VesperTextPrimary,
                                modifier = Modifier.size(if (isLandscape) 56.dp else 96.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 18.dp))

        // Title, Artist & Favorite Heart
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentTrack.title,
                    style = VesperTypography.headlineSmall.copy(
                        color = VesperTextPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentTrack.artist,
                    style = VesperTypography.titleMedium.copy(color = VesperTextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = { onFavoriteToggle(currentTrack) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("now_playing_favorite")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) VesperBurgundyHighlight else VesperTextMuted,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Seekbar & Timestamps (Isolated leaf component to prevent full-screen recomposition)
        NowPlayingSeekBar(
            playbackPositionMs = playbackPositionMs,
            durationMs = totalDur,
            onSeekTo = onSeekTo,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Playback Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("now_playing_shuffle")
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleMode) dynamicAccent else colors.textMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Previous
            IconButton(
                onClick = onSkipPreviousClick,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("now_playing_prev")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Big Burgundy / Dynamic Accent Play/Pause Button
            Surface(
                onClick = onPlayPauseClick,
                shape = CircleShape,
                color = dynamicAccent,
                modifier = Modifier
                    .size(68.dp)
                    .border(1.5.dp, dynamicAccent.copy(alpha = 0.5f), CircleShape)
                    .testTag("now_playing_play_pause")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = colors.onAccent,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            // Next
            IconButton(
                onClick = onSkipNextClick,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("now_playing_next")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Repeat
            IconButton(
                onClick = onCycleRepeat,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("now_playing_repeat")
            ) {
                val icon = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                }
                val tint = if (repeatMode != Player.REPEAT_MODE_OFF) dynamicAccent else colors.textMuted

                Icon(
                    imageVector = icon,
                    contentDescription = "Repeat",
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons: Queue, Lyrics, Tone & Sleep Timer (Equal width, never wrapping)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NowPlayingActionButton(
                icon = Icons.Default.QueueMusic,
                label = "Queue",
                onClick = onOpenQueueClick,
                testTag = "now_playing_open_queue",
                modifier = Modifier.weight(1f)
            )
            NowPlayingActionButton(
                icon = Icons.Default.Description,
                label = if (showLyrics) "Artwork" else "Lyrics",
                onClick = { showLyrics = !showLyrics },
                testTag = "now_playing_open_lyrics",
                modifier = Modifier.weight(1f)
            )
            NowPlayingActionButton(
                icon = Icons.Default.Equalizer,
                label = "Tone",
                onClick = onOpenEqualizerClick,
                testTag = "now_playing_open_tone",
                modifier = Modifier.weight(1f)
            )
            NowPlayingActionButton(
                icon = Icons.Default.Schedule,
                label = "Sleep timer",
                onClick = { onOpenSleepTimerClick?.invoke() },
                testTag = "now_playing_open_timer",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom Gesture Pill: Swipe down to minimize
        Row(
            modifier = Modifier
                .clickable { onCollapseClick() }
                .padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = VesperTextMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Swipe down to minimize",
                style = VesperTypography.labelSmall.copy(
                    color = VesperTextMuted,
                    letterSpacing = 0.5.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LyricsCard(
    lyrics: TrackLyrics?,
    playbackPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = VesperSurfaceElevated,
        modifier = modifier
    ) {
        if (lyrics == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = VesperTextMuted,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Lyrics unavailable for this track",
                        style = VesperTypography.bodyMedium.copy(color = VesperTextMuted),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (lyrics.isSynced && lyrics.syncedLines.isNotEmpty()) {
            val listState = rememberLazyListState()
            val activeIndex = remember(lyrics.syncedLines, playbackPositionMs) {
                val idx = lyrics.syncedLines.indexOfLast { it.timestampMs <= playbackPositionMs }
                if (idx >= 0) idx else 0
            }

            LaunchedEffect(activeIndex) {
                if (activeIndex >= 0 && activeIndex < lyrics.syncedLines.size) {
                    listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
                }
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(lyrics.syncedLines) { index, line ->
                    val isCurrentLine = index == activeIndex
                    Text(
                        text = line.text.ifBlank { "..." },
                        style = if (isCurrentLine) {
                            VesperTypography.titleMedium.copy(
                                color = VesperTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        } else {
                            VesperTypography.bodyMedium.copy(
                                color = VesperTextMuted.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSeekTo(line.timestampMs) }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        } else {
            // Unsynced Lyrics Text
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = lyrics.rawText,
                    style = VesperTypography.bodyMedium.copy(
                        color = VesperTextSecondary,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun NowPlayingActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = VesperSurfaceElevated,
        modifier = modifier
            .border(1.dp, VesperOutline, RoundedCornerShape(14.dp))
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VesperTextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = VesperTypography.labelSmall.copy(
                    color = VesperTextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.5.sp
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

/**
 * Isolated leaf composable for seekbar & time stamps.
 * Encapsulates scrubbing state to prevent re-composing the entire NowPlayingScreen.
 */
@Composable
fun NowPlayingSeekBar(
    playbackPositionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }

    val currentPos = if (isScrubbing) scrubPosition.toLong() else playbackPositionMs
    val totalDur = durationMs.coerceAtLeast(1L)
    val sliderValue = (currentPos.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f)

    Column(modifier = modifier) {
        VesperSlider(
            value = sliderValue,
            onValueChange = { frac ->
                isScrubbing = true
                scrubPosition = frac * totalDur
            },
            onValueChangeFinished = {
                isScrubbing = false
                onSeekTo(scrubPosition.toLong())
            },
            modifier = Modifier.fillMaxWidth(),
            testTag = "now_playing_seek_slider"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPos),
                style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary)
            )
            Text(
                text = formatTime(totalDur),
                style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary)
            )
        }
    }
}
