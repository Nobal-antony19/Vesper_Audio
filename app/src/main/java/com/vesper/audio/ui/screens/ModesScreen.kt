package com.vesper.audio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesper.audio.ui.SoundProfile
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
fun ModesScreen(
    activeProfile: SoundProfile,
    onProfileSelected: (SoundProfile) -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    // Remember list scroll position across composition switches
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VesperBackground)
    ) {
        // Fixed Top App Bar with statusBars insets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("modes_back_button")
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
                text = "Modes",
                style = VesperTypography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = VesperTextPrimary
                )
            )
        }

        // Single primary vertically scrollable profile collection filling available space between header and bottom switch
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("modes_profile_list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Curated acoustic profiles calibrated for hardware audio sessions.",
                    style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(SoundProfile.values(), key = { it.name }) { profile ->
                val isSelected = profile == activeProfile
                val icon = when (profile) {
                    SoundProfile.BALANCED -> Icons.Default.GraphicEq
                    SoundProfile.DEEP_BASS -> Icons.Default.Speaker
                    SoundProfile.BASS_BOOST -> Icons.Default.Speaker
                    SoundProfile.VOCAL -> Icons.Default.Mic
                    SoundProfile.ACOUSTIC -> Icons.Default.Headphones
                    SoundProfile.CLASSICAL -> Icons.Default.Piano
                    SoundProfile.ROCK -> Icons.Default.MusicNote
                    SoundProfile.JAZZ -> Icons.Default.MusicNote
                    SoundProfile.POP -> Icons.Default.GraphicEq
                    SoundProfile.ELECTRONIC -> Icons.Default.Tune
                    SoundProfile.CINEMATIC -> Icons.Default.Movie
                    SoundProfile.NIGHT -> Icons.Default.NightsStay
                    SoundProfile.STUDIO_REFERENCE -> Icons.Default.GraphicEq
                    SoundProfile.WARM_ANALOG -> Icons.Default.Headphones
                    SoundProfile.CUSTOM -> Icons.Default.Tune
                }

                SoundProfileCard(
                    profile = profile,
                    icon = icon,
                    isSelected = isSelected,
                    onClick = { onProfileSelected(profile) }
                )
            }
        }

        // Fixed Equalizer/Modes Switch at the bottom with navigationBars insets
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VesperBackground)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 12.dp)
                .testTag("modes_bottom_navigation"),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = VesperBackgroundSecondary,
                modifier = Modifier
                    .border(1.dp, VesperOutline, RoundedCornerShape(24.dp))
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    Surface(
                        onClick = onNavigateToEqualizer,
                        shape = RoundedCornerShape(20.dp),
                        color = VesperBackgroundSecondary,
                        modifier = Modifier.testTag("modes_switch_to_eq")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = null,
                                tint = VesperTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Equalizer",
                                style = VesperTypography.labelLarge.copy(color = VesperTextSecondary)
                            )
                        }
                    }

                    val dynamicAccent = com.vesper.audio.ui.theme.LocalVesperAccentColor.current
                    Surface(
                        onClick = { /* already in modes */ },
                        shape = RoundedCornerShape(20.dp),
                        color = dynamicAccent,
                        modifier = Modifier.testTag("modes_active_tab")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = VesperTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Modes",
                                style = VesperTypography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = VesperTextPrimary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundProfileCard(
    profile: SoundProfile,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val accent = com.vesper.audio.ui.theme.LocalVesperAccentColor.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) VesperSurfaceElevated else VesperBackgroundSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accent else VesperOutline,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .testTag(if (profile == SoundProfile.BASS_BOOST) "modes_profile_bass" else "modes_profile_${profile.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) VesperDeepBurgundy else VesperSurfaceElevated)
                    .border(
                        1.dp,
                        if (isSelected) accent else VesperOutline,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) VesperTextPrimary else VesperTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.title,
                    style = VesperTypography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) VesperTextPrimary else VesperTextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = profile.subtitle,
                    style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile.description,
                    style = VesperTypography.bodySmall.copy(color = VesperTextMuted)
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
