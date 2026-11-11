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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesper.audio.ui.components.SectionHeader
import com.vesper.audio.ui.theme.VesperBackground
import com.vesper.audio.ui.theme.VesperBackgroundSecondary
import com.vesper.audio.ui.theme.VesperBurgundyAccent
import com.vesper.audio.ui.theme.VesperDeepBurgundy
import com.vesper.audio.ui.theme.VesperOutline
import androidx.activity.compose.BackHandler
import com.vesper.audio.ui.theme.VesperSurfaceElevated
import com.vesper.audio.ui.theme.VesperTextMuted
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTextSecondary
import com.vesper.audio.ui.theme.VesperTypography

@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

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
                modifier = Modifier.size(48.dp).testTag("about_back_button")
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
                text = "About",
                style = VesperTypography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = VesperTextPrimary
                )
            )
        }

        val dynamicAccent = com.vesper.audio.ui.theme.LocalVesperAccentColor.current

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero branding card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = VesperDeepBurgundy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, dynamicAccent.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                 .size(68.dp)
                                 .clip(CircleShape)
                                 .background(VesperDeepBurgundy)
                                 .border(1.5.dp, dynamicAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            com.vesper.audio.ui.components.VesperLogo(
                                size = 48.dp,
                                animated = true
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "VESPER",
                            style = VesperTypography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = VesperTextPrimary,
                                letterSpacing = 2.sp
                            )
                        )

                        Text(
                            text = "PRIVATE • OFFLINE • PREMIUM MUSIC PLAYER",
                            style = VesperTypography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = VesperTextSecondary,
                                letterSpacing = 1.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Version 3.0.0 (Definitive Master Edition)",
                            style = VesperTypography.bodySmall.copy(color = VesperTextMuted)
                        )
                    }
                }
            }

            // Creator Cards: Nobal Antony & Allen JR
            item {
                SectionHeader(title = "Core Team")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Nobal Antony
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = VesperSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, dynamicAccent.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(VesperDeepBurgundy)
                                    .border(1.dp, dynamicAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = VesperTextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Nobal Antony",
                                    style = VesperTypography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = VesperTextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Lead Designer & Architect",
                                    style = VesperTypography.bodySmall.copy(color = dynamicAccent, fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Crafted with passion for offline acoustic precision and cosmic minimalism.",
                                    style = VesperTypography.bodySmall.copy(color = VesperTextMuted, fontSize = 11.sp)
                                )
                            }
                        }
                    }

                    // Allen JR
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = VesperSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, dynamicAccent.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(VesperDeepBurgundy)
                                    .border(1.dp, dynamicAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = VesperTextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Allen JR",
                                    style = VesperTypography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = VesperTextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "SaaS Manager & Strategy Lead",
                                    style = VesperTypography.bodySmall.copy(color = dynamicAccent, fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Shaping product direction, growth strategy and the VESPER experience.",
                                    style = VesperTypography.bodySmall.copy(color = VesperTextMuted, fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }
            }

            // Core Principles
            item {
                SectionHeader(title = "Core Principles")
            }

            item {
                AboutPrincipleItem(
                    icon = Icons.Default.CloudOff,
                    title = "Offline-First Architecture",
                    description = "Music never leaves your device. No cloud dependencies or network requirements for audio playback."
                )
            }

            item {
                AboutPrincipleItem(
                    icon = Icons.Default.Lock,
                    title = "Privacy & Zero Telemetry",
                    description = "No accounts required. No analytics tracking, background telemetry, or silent file uploads."
                )
            }

            item {
                AboutPrincipleItem(
                    icon = Icons.Default.Block,
                    title = "Zero Advertisements",
                    description = "No banner ads, interstitial popups, or sponsored audio interruptions. Pure listening experience."
                )
            }

            item {
                AboutPrincipleItem(
                    icon = Icons.Default.Tune,
                    title = "Hardware Audio Engine",
                    description = "Powered by AndroidX Media3, real Android AudioSession Equalizer, and hardware BassBoost filtering."
                )
            }

            // v3.0.0 Changelog
            item {
                SectionHeader(title = "v3.0.0 Changelog")
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = VesperSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VesperOutline, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "What's New in v3.0.0:",
                            style = VesperTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = VesperTextPrimary)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Deterministic Artwork Cache: Single-flight ID3 extraction with atomic temp-file writes, alpha-aware compression, and a 40 MB LRU cache ceiling.\n• Circular Media3 Queue Engine: Mathematically safe circular wrap on Next/Previous and in-place seek transitions without reloading the MediaController queue.\n• Studio Tone Engine: 10-band EQ, 9 acoustic modes, automatic preamp headroom protection against positive-gain clipping, stereo balance control, and custom preset Save/Rename/Duplicate/Delete.\n• Unified Paginated Discover: Combined Local + Audius search across Songs, Albums, and Artists with offset pagination, retryable offline error handling, and deterministic A–Z fast-scroll indexing.\n• Streamlined Cosmic Burgundy Identity: Removed bloated history, stats, and theme-studio tables via non-destructive Room migration (v5 → v6).",
                            style = VesperTypography.bodySmall.copy(color = VesperTextSecondary, lineHeight = 20.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutPrincipleItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = VesperSurfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VesperOutline, RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(VesperDeepBurgundy),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VesperTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = VesperTypography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = VesperTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                )
            }
        }
    }
}
