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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesper.audio.audio.AudioEffectController
import com.vesper.audio.audio.ProcessingStatus
import com.vesper.audio.audio.SignatureToneProfile
import com.vesper.audio.ui.theme.LocalVesperAccentColor
import com.vesper.audio.ui.theme.VesperBackground
import com.vesper.audio.ui.theme.VesperBackgroundSecondary
import com.vesper.audio.ui.theme.VesperBorderAccent
import com.vesper.audio.ui.theme.VesperBurgundyHighlight
import com.vesper.audio.ui.theme.VesperDeepBurgundy
import com.vesper.audio.ui.theme.VesperOutline
import com.vesper.audio.ui.theme.VesperSurfaceElevated
import com.vesper.audio.ui.theme.VesperTextMuted
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTextSecondary
import com.vesper.audio.ui.theme.VesperTypography
import kotlin.math.roundToInt

enum class TonesWorkspaceTab(
    val title: String,
    val testTag: String
) {
    TONES(title = "Tones", testTag = "tones_tab_signature"),
    STUDIO_EQ(title = "Studio EQ", testTag = "tones_tab_seq");

    companion object {
        fun fromSavedName(raw: String?): TonesWorkspaceTab {
            return when (raw?.trim()?.uppercase()) {
                "STUDIO_EQ", "STUDIO_SEQ", "SEQ" -> STUDIO_EQ
                else -> TONES
            }
        }
    }
}

/**
 * Dedicated Tones workspace screen with two tabs:
 * 1. Tones — 6 Signature Tones led by VESPER Signature, plus Flat / DSP Off utility bypass
 * 2. Studio EQ — 10-band precision equalizer, preamp headroom guard, sub-bass, stage width, and stereo balance
 * Uses genuine, capability-tested Android AudioEffect DSP only — zero fake DAC or high-res claims.
 */
@Composable
fun TonesScreen(
    onBackClick: () -> Unit,
    audioEffectController: AudioEffectController? = null,
    onOpenBasicEqualizer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val controller = remember(audioEffectController) {
        audioEffectController ?: AudioEffectController()
    }
    val dynamicAccent = LocalVesperAccentColor.current
    var activeTabName by rememberSaveable { mutableStateOf(TonesWorkspaceTab.TONES.name) }
    val activeTab = remember(activeTabName) { TonesWorkspaceTab.fromSavedName(activeTabName) }

    val isEnabled by controller.isEnabled.collectAsState()
    val isAbBypassed by controller.isAbBypassed.collectAsState()
    val activeToneId by controller.activeToneId.collectAsState()
    val processingStatus by controller.processingStatus.collectAsState()
    val hardwareCaps by controller.hardwareCapabilities.collectAsState()
    val bands by controller.bands.collectAsState()
    val bassBoostStrength by controller.bassBoostStrength.collectAsState()
    val virtualizerStrength by controller.virtualizerStrength.collectAsState()
    val headroomEnabled by controller.headroomProtectionEnabled.collectAsState()
    val audioBalance by controller.audioBalance.collectAsState()
    val preampScalar by controller.preampGainScalar.collectAsState()

    val headroomStatus = remember(bands, bassBoostStrength, headroomEnabled, isEnabled, isAbBypassed, activeToneId, preampScalar) {
        controller.getHeadroomStatus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VesperBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("tones_placeholder_screen")
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("tones_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VesperTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Tones",
                        style = VesperTypography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = VesperTextPrimary
                        )
                    )
                    Text(
                        text = "VESPER Signature • Capability-Tested DSP",
                        style = VesperTypography.labelSmall.copy(color = VesperTextMuted)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // One-tap A/B Bypass utility pill
                Surface(
                    onClick = { controller.toggleAbBypass() },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isAbBypassed) VesperDeepBurgundy else VesperBackgroundSecondary,
                    modifier = Modifier
                        .then(
                            if (isAbBypassed) {
                                Modifier.border(1.dp, dynamicAccent, RoundedCornerShape(10.dp))
                            } else {
                                Modifier
                            }
                        )
                        .testTag("tones_ab_bypass_button")
                ) {
                    Text(
                        text = if (isAbBypassed) "A/B: Flat" else "A/B Bypass",
                        style = VesperTypography.labelSmall.copy(
                            color = if (isAbBypassed) VesperTextPrimary else VesperTextSecondary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = if (isEnabled && !isAbBypassed) "ON" else "OFF",
                    style = VesperTypography.labelMedium.copy(
                        color = if (isEnabled && !isAbBypassed) dynamicAccent else VesperTextMuted,
                        fontWeight = FontWeight.Bold
                    )
                )
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { controller.setEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VesperTextPrimary,
                        checkedTrackColor = dynamicAccent,
                        uncheckedThumbColor = VesperTextMuted,
                        uncheckedTrackColor = VesperBackgroundSecondary
                    ),
                    modifier = Modifier.testTag("tones_master_switch")
                )
            }
        }

        // Honest DSP Status & Hardware Capability Banner (single outer container border)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = VesperSurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .border(1.dp, VesperOutline, RoundedCornerShape(12.dp))
                .testTag("tones_coming_soon_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAbBypassed) "Processing: A/B Bypass (0 dB Flat)" else processingStatus.label,
                        style = VesperTypography.labelLarge.copy(
                            color = when {
                                isAbBypassed -> VesperTextSecondary
                                processingStatus == ProcessingStatus.DEVICE_EFFECT -> dynamicAccent
                                processingStatus == ProcessingStatus.SOFTWARE -> VesperBurgundyHighlight
                                processingStatus == ProcessingStatus.OFF -> VesperTextSecondary
                                else -> Color(0xFFFFB74D)
                            },
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append("EQ: ${if (hardwareCaps.hasEqualizer) "${hardwareCaps.equalizerBandsCount}-band" else "Software"} • ")
                            append("Bass: ${if (hardwareCaps.hasBassBoost) "Supported" else "Unavailable"} • ")
                            append("Stage: ${if (hardwareCaps.hasVirtualizer) "Supported" else "Unavailable"}")
                        },
                        style = VesperTypography.bodySmall.copy(
                            color = VesperTextMuted,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onOpenBasicEqualizer != null) {
                    Surface(
                        onClick = onOpenBasicEqualizer,
                        shape = RoundedCornerShape(10.dp),
                        color = VesperDeepBurgundy,
                        modifier = Modifier.testTag("tones_open_basic_modes_button")
                    ) {
                        Text(
                            text = "Basic Modes",
                            style = VesperTypography.labelSmall.copy(
                                color = VesperTextPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Workspace Tab Selector: [Tones] vs [Studio EQ] inside one clean segmented bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = VesperBackgroundSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .border(1.dp, VesperOutline, RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TonesTabPill(
                    title = TonesWorkspaceTab.TONES.title,
                    selected = activeTab == TonesWorkspaceTab.TONES,
                    onClick = { activeTabName = TonesWorkspaceTab.TONES.name },
                    modifier = Modifier.weight(1f),
                    testTag = TonesWorkspaceTab.TONES.testTag
                )
                TonesTabPill(
                    title = TonesWorkspaceTab.STUDIO_EQ.title,
                    selected = activeTab == TonesWorkspaceTab.STUDIO_EQ,
                    onClick = { activeTabName = TonesWorkspaceTab.STUDIO_EQ.name },
                    modifier = Modifier.weight(1f),
                    testTag = TonesWorkspaceTab.STUDIO_EQ.testTag
                )
            }
        }

        when (activeTab) {
            TonesWorkspaceTab.TONES -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("signature_tones_list"),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "tones_header_label") {
                        Text(
                            text = "VESPER Signature & Acoustic Voicings",
                            style = VesperTypography.labelMedium.copy(
                                color = VesperTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    items(AudioEffectController.SIGNATURE_TONES, key = { it.id }) { tone ->
                        val isSelected = isEnabled && !isAbBypassed && activeToneId == tone.id
                        val isHardwareLimited = tone.requiresVirtualizer && !hardwareCaps.hasVirtualizer

                        SignatureToneCard(
                            tone = tone,
                            isSelected = isSelected,
                            isHardwareLimited = isHardwareLimited,
                            accentColor = dynamicAccent,
                            onClick = {
                                controller.applySignatureTone(tone.id, toggleIfAlreadyActive = true)
                            }
                        )
                    }

                    item(key = "tones_flat_bypass_header") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Utility & Reference Bypass",
                            style = VesperTypography.labelMedium.copy(
                                color = VesperTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    item(key = AudioEffectController.FLAT_BYPASS_PROFILE.id) {
                        val bypassTone = AudioEffectController.FLAT_BYPASS_PROFILE
                        val isBypassSelected = isAbBypassed || activeToneId == bypassTone.id
                        SignatureToneCard(
                            tone = bypassTone,
                            isSelected = isBypassSelected,
                            isHardwareLimited = false,
                            accentColor = dynamicAccent,
                            onClick = {
                                controller.applySignatureTone(bypassTone.id, toggleIfAlreadyActive = true)
                            }
                        )
                    }
                }
            }

            TonesWorkspaceTab.STUDIO_EQ -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("studio_seq_console"),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Preamp Headroom & Clipping Guard
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = VesperSurfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, VesperOutline, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Headroom Protection (Preamp)",
                                            style = VesperTypography.titleMedium.copy(
                                                color = VesperTextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = "Attenuates digital gain (${String.format("%.1f dB", headroomStatus.preampAttenuationDb)}) when positive band boosts are active.",
                                            style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                                        )
                                    }
                                    Switch(
                                        checked = headroomEnabled,
                                        onCheckedChange = { controller.toggleHeadroomProtection(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = VesperTextPrimary,
                                            checkedTrackColor = dynamicAccent,
                                            uncheckedThumbColor = VesperTextMuted,
                                            uncheckedTrackColor = VesperBackgroundSecondary
                                        ),
                                        modifier = Modifier.testTag("seq_headroom_switch")
                                    )
                                }
                            }
                        }
                    }

                    // 2. 10-Band Precision Studio EQ
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = VesperSurfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, VesperOutline, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = dynamicAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Studio EQ Band Matrix",
                                            style = VesperTypography.titleMedium.copy(
                                                color = VesperTextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    Surface(
                                        onClick = { controller.resetToFlat() },
                                        shape = RoundedCornerShape(10.dp),
                                        color = VesperBackgroundSecondary,
                                        modifier = Modifier.testTag("seq_reset_flat_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Reset Flat",
                                                tint = VesperTextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "0 dB Flat",
                                                style = VesperTypography.labelSmall.copy(
                                                    color = VesperTextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                bands.forEach { band ->
                                    val dbVal = band.levelMb / 100f
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${band.formattedFreq} Hz",
                                            style = VesperTypography.labelMedium.copy(
                                                color = VesperTextSecondary,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            modifier = Modifier.width(56.dp)
                                        )

                                        Slider(
                                            value = dbVal,
                                            onValueChange = { newDb ->
                                                val mb = (newDb * 100f).roundToInt().toShort()
                                                controller.setBandLevel(band.index, mb)
                                            },
                                            valueRange = -12f..12f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = VesperTextPrimary,
                                                activeTrackColor = dynamicAccent,
                                                inactiveTrackColor = VesperOutline
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("seq_band_slider_${band.index}")
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = String.format("%+.1f dB", dbVal),
                                            style = VesperTypography.labelSmall.copy(
                                                color = if (band.levelMb.toInt() != 0) dynamicAccent else VesperTextMuted,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.width(54.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Sub-Bass Harmonic Drive & Spatial Virtualizer & L/R Balance
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = VesperSurfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, VesperOutline, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Acoustic Stage & Dynamics",
                                    style = VesperTypography.titleMedium.copy(
                                        color = VesperTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Bass Boost
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Sub-Bass Contour",
                                        style = VesperTypography.bodyMedium.copy(color = VesperTextPrimary)
                                    )
                                    Text(
                                        text = "${(bassBoostStrength / 10).coerceIn(0, 100)}%",
                                        style = VesperTypography.labelMedium.copy(
                                            color = dynamicAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Slider(
                                    value = bassBoostStrength.toFloat(),
                                    onValueChange = { controller.setBassBoost(it.roundToInt().toShort()) },
                                    valueRange = 0f..1000f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = VesperTextPrimary,
                                        activeTrackColor = dynamicAccent,
                                        inactiveTrackColor = VesperOutline
                                    ),
                                    modifier = Modifier.testTag("seq_bass_slider")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Spatial Virtualizer
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Spatial Stage Width",
                                        style = VesperTypography.bodyMedium.copy(color = VesperTextPrimary)
                                    )
                                    Text(
                                        text = if (hardwareCaps.hasVirtualizer) {
                                            "${(virtualizerStrength / 10).coerceIn(0, 100)}%"
                                        } else {
                                            "Hardware unsupported"
                                        },
                                        style = VesperTypography.labelMedium.copy(
                                            color = if (hardwareCaps.hasVirtualizer) dynamicAccent else VesperTextMuted,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Slider(
                                    value = virtualizerStrength.toFloat(),
                                    onValueChange = { controller.setVirtualizerStrength(it.roundToInt().toShort()) },
                                    enabled = hardwareCaps.hasVirtualizer,
                                    valueRange = 0f..1000f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = VesperTextPrimary,
                                        activeTrackColor = dynamicAccent,
                                        inactiveTrackColor = VesperOutline
                                    ),
                                    modifier = Modifier.testTag("seq_virtualizer_slider")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Stereo L/R Balance
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Stereo Balance (L / R)",
                                        style = VesperTypography.bodyMedium.copy(color = VesperTextPrimary)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = when {
                                                kotlin.math.abs(audioBalance) < 0.03f -> "Center"
                                                audioBalance < 0f -> "L ${(kotlin.math.abs(audioBalance) * 100).roundToInt()}%"
                                                else -> "R ${(audioBalance * 100).roundToInt()}%"
                                            },
                                            style = VesperTypography.labelMedium.copy(
                                                color = dynamicAccent,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        if (kotlin.math.abs(audioBalance) >= 0.03f) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Center",
                                                style = VesperTypography.labelSmall.copy(
                                                    color = VesperTextSecondary,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { controller.setAudioBalance(0f) }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Slider(
                                    value = audioBalance,
                                    onValueChange = { controller.setAudioBalance(it) },
                                    valueRange = -1f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = VesperTextPrimary,
                                        activeTrackColor = dynamicAccent,
                                        inactiveTrackColor = VesperOutline
                                    ),
                                    modifier = Modifier.testTag("seq_balance_slider")
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
private fun TonesTabPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val accent = LocalVesperAccentColor.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accent else Color.Transparent,
        modifier = modifier
            .height(38.dp)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                style = VesperTypography.labelLarge.copy(
                    color = if (selected) VesperTextPrimary else VesperTextSecondary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun SignatureToneCard(
    tone: SignatureToneProfile,
    isSelected: Boolean,
    isHardwareLimited: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) VesperDeepBurgundy else VesperSurfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isSelected) accentColor else VesperOutline,
                shape = RoundedCornerShape(12.dp)
            )
            .testTag("signature_tone_${tone.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) accentColor else VesperBackgroundSecondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Tune,
                    contentDescription = null,
                    tint = VesperTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tone.title,
                        style = VesperTypography.titleMedium.copy(
                            color = VesperTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tone.subtitle,
                        style = VesperTypography.labelSmall.copy(
                            color = if (isSelected) VesperBurgundyHighlight else VesperTextMuted,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tone.description,
                    style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                )
                if (isHardwareLimited) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Note: Hardware Virtualizer unavailable on this output; applying EQ contour only.",
                        style = VesperTypography.labelSmall.copy(
                            color = Color(0xFFFFB74D),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
