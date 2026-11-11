package com.vesper.audio.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vesper.audio.model.Track
import com.vesper.audio.util.VesperArtworkResolver
import com.vesper.audio.ui.theme.LocalVesperAccentColor
import com.vesper.audio.ui.theme.LocalVesperColors
import com.vesper.audio.ui.theme.VesperBackground
import com.vesper.audio.ui.theme.VesperBorderAccent
import com.vesper.audio.ui.theme.VesperDeepBurgundy
import com.vesper.audio.ui.theme.VesperOutline
import com.vesper.audio.ui.theme.VesperSurfaceElevated
import com.vesper.audio.ui.theme.VesperTextMuted
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTextSecondary
import com.vesper.audio.ui.theme.VesperTypography

/**
 * Reusable VesperPrimaryButton conforming to 48-56dp height and 18-22dp corner radius.
 */
@Composable
fun VesperPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String = "vesper_primary_button"
) {
    val accent = LocalVesperAccentColor.current
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        color = if (enabled) accent else VesperDeepBurgundy.copy(alpha = 0.5f),
        contentColor = VesperTextPrimary,
        modifier = modifier
            .heightIn(min = 50.dp)
            .border(1.dp, VesperBorderAccent, RoundedCornerShape(20.dp))
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VesperTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = VesperTypography.labelLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = VesperTextPrimary
                )
            )
        }
    }
}

// Backwards compatibility alias
@Composable
fun GoldPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String = "gold_primary_button"
) {
    VesperPrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        testTag = testTag
    )
}

/**
 * SectionHeader for grouping content with a dynamic accent bar.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val accent = LocalVesperAccentColor.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = VesperTypography.titleLarge.copy(fontWeight = FontWeight.Bold, color = VesperTextPrimary)
            )
        }

        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = VesperTypography.labelLarge.copy(color = VesperTextSecondary),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * MusicTrackRow displays track item with artwork, title, artist, duration, favorite heart and action buttons.
 * Fixed 64dp height, 16dp horizontal padding: [art 48dp (128px Coil size)] [title/artist weight 1] [duration] [heart 40dp] [⋮ 40dp]
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicTrackRow(
    track: Track,
    isPlaying: Boolean,
    isCurrentTrack: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = track.isFavorite,
    leadingNumber: Int? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = LocalVesperColors.current
    val accent = colors.accent
    val rowBg = if (isSelected) accent.copy(alpha = 0.35f)
        else if (isCurrentTrack) colors.surfaceElevated
        else Color.Transparent

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = rowBg,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("track_row_${track.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = accent,
                        uncheckedColor = colors.textMuted,
                        checkmarkColor = colors.textPrimary
                    ),
                    modifier = Modifier.padding(end = 6.dp)
                )
            } else if (leadingNumber != null) {
                Text(
                    text = "$leadingNumber",
                    style = VesperTypography.labelMedium.copy(
                        color = if (isCurrentTrack) colors.textPrimary else colors.textMuted,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Artwork (48dp x 48dp, bounded 128px Coil decode)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceElevated)
                    .border(
                        width = 1.dp,
                        color = if (isCurrentTrack) accent else colors.outline,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                var imageLoadFailed by remember(track.artworkUri) { mutableStateOf(false) }
                val artUri = track.artworkUri
                val hasValidArt = artUri != null &&
                    !imageLoadFailed &&
                    !VesperArtworkResolver.isArtworkUriBroken(artUri.toString())

                if (hasValidArt && artUri != null) {
                    val imageRequest = remember(artUri) {
                        ImageRequest.Builder(context)
                            .data(artUri)
                            .size(128)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "Artwork for ${track.title}",
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
                        tint = if (isCurrentTrack) colors.textPrimary else colors.textMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (isCurrentTrack && isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.background.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        MiniEqualizerIndicator()
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Artist (weight 1, single line ellipsis)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = track.title,
                    style = VesperTypography.titleMedium.copy(
                        color = if (isCurrentTrack) accent else colors.textPrimary,
                        fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    style = VesperTypography.bodyMedium.copy(
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Duration
            Text(
                text = track.formattedDuration,
                style = VesperTypography.bodySmall.copy(color = colors.textMuted),
                maxLines = 1
            )

            if (trailingContent != null) {
                trailingContent()
            } else {
                Spacer(modifier = Modifier.width(4.dp))

                // Favorite Toggle (40dp x 40dp, touch isolated from row click)
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("fav_button_${track.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) accent else colors.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // More Options (40dp x 40dp)
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("more_button_${track.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Track options",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * 3-bar mini visualizer indicator for playing tracks in rows.
 */
@Composable
fun MiniEqualizerIndicator() {
    val accent = LocalVesperAccentColor.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(14.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(VesperTextPrimary)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(accent)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(VesperDeepBurgundy)
        )
    }
}

/**
 * AlbumArtworkCard for albums grid and horizontal carousels (bounded 300px Coil decode).
 */
@Composable
fun AlbumArtworkCard(
    title: String,
    subtitle: String,
    artworkUri: android.net.Uri?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Int = 140,
    trackCount: Int? = null
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .width(sizeDp.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("album_card_$title")
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(VesperSurfaceElevated)
                .border(1.dp, VesperOutline, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            var imageLoadFailed by remember(artworkUri) { mutableStateOf(false) }
            val hasValidArt = artworkUri != null &&
                !imageLoadFailed &&
                !VesperArtworkResolver.isArtworkUriBroken(artworkUri.toString())

            if (hasValidArt && artworkUri != null) {
                val imageRequest = remember(artworkUri) {
                    ImageRequest.Builder(context)
                        .data(artworkUri)
                        .size(300)
                        .crossfade(false)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "Artwork for $title",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = {
                        imageLoadFailed = true
                        VesperArtworkResolver.markArtworkUriBroken(artworkUri.toString())
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = VesperTextMuted,
                    modifier = Modifier.size((sizeDp / 3).dp)
                )
            }

            if (trackCount != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(VesperBackground.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$trackCount tracks",
                        style = VesperTypography.labelSmall.copy(color = VesperTextPrimary, fontSize = 10.sp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = VesperTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = VesperTextPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Custom VesperSlider for playback progress scrubbing and EQ sliders.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VesperSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    testTag: String = "vesper_slider"
) {
    val colors = LocalVesperColors.current
    val accent = colors.accent
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = accent,
            activeTrackColor = accent,
            inactiveTrackColor = colors.outline
        ),
        modifier = modifier.testTag(testTag)
    )
}

// Backwards compatibility alias
@Composable
fun GoldSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    testTag: String = "gold_slider"
) {
    VesperSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        testTag = testTag
    )
}

/**
 * EmptyLibraryState displaying clean message and action button.
 */
@Composable
fun EmptyLibraryState(
    title: String,
    message: String,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accent = LocalVesperAccentColor.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(VesperSurfaceElevated)
                .border(1.dp, VesperBorderAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            style = VesperTypography.titleLarge.copy(fontWeight = FontWeight.Bold, color = VesperTextPrimary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (actionButtonText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            VesperPrimaryButton(
                text = actionButtonText,
                onClick = onActionClick,
                testTag = "empty_state_action"
            )
        }
    }
}

/**
 * LoadingState spinner with dynamic accent.
 */
@Composable
fun LoadingState(
    message: String = "Loading your music collection...",
    modifier: Modifier = Modifier
) {
    val accent = LocalVesperAccentColor.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = accent,
            modifier = Modifier.size(44.dp),
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary)
        )
    }
}
