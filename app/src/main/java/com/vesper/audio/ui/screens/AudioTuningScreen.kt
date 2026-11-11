package com.vesper.audio.ui.screens

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesper.audio.audio.AudioEffectController
import com.vesper.audio.audio.EqBand
import com.vesper.audio.audio.ProcessingStatus
import com.vesper.audio.data.local.CustomEqPresetEntity
import com.vesper.audio.ui.SoundProfile
import com.vesper.audio.ui.components.VesperPrimaryButton
import com.vesper.audio.ui.theme.VesperBackground
import com.vesper.audio.ui.theme.VesperBackgroundSecondary
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
import kotlin.math.abs

enum class AudioTuningTab {
    EQUALIZER,
    MODES
}

@Composable
fun AudioTuningScreen(
    audioEffectController: AudioEffectController,
    isEnabled: Boolean,
    bands: List<EqBand>,
    bandRange: Pair<Short, Short>,
    activePreset: String,
    bassBoost: Short,
    onToggleEnable: (Boolean) -> Unit,
    onBandLevelChange: (Short, Short) -> Unit,
    onPresetSelect: (String) -> Unit,
    onBassBoostChange: (Short) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onBackClick: () -> Unit,
    customPresets: List<CustomEqPresetEntity> = emptyList(),
    onRenameCustomPreset: (String, String) -> Unit = { _, _ -> },
    onDuplicateCustomPreset: (String) -> Unit = {},
    onDeleteCustomPreset: (String) -> Unit = {},
    preampHeadroomEnabled: Boolean = true,
    onToggleHeadroomProtection: (Boolean) -> Unit = {},
    audioBalance: Float = 0f,
    onAudioBalanceChange: (Float) -> Unit = {},
    onResetToFlat: () -> Unit = {},
    activeProfile: SoundProfile = SoundProfile.BALANCED,
    onProfileSelect: (SoundProfile) -> Unit = {},
    onNavigateToModes: (() -> Unit)? = null,
    initialTab: AudioTuningTab = AudioTuningTab.EQUALIZER,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    var currentTab by rememberSaveable { mutableStateOf(initialTab) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetToRename by remember { mutableStateOf<String?>(null) }
    var showAboutToneDialog by remember { mutableStateOf(false) }

    val processingStatus by audioEffectController.processingStatus.collectAsState()
    val headroomProtected by audioEffectController.headroomProtectionEnabled.collectAsState()
    val headroomStatus = audioEffectController.getHeadroomStatus()

    val outputRoute = remember(context) { getAudioOutputRoute(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VesperBackground)
    ) {
        // Top App Bar with statusBars insets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("tone_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VesperTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tone",
                            style = VesperTypography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = VesperTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // About Tone Truth Button
                        IconButton(
                            onClick = { showAboutToneDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "About Tone Truth Sheet",
                                tint = VesperTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = "Studio Acoustic Engine",
                        style = VesperTypography.labelSmall.copy(color = VesperTextMuted)
                    )
                }
            }

            val isAbBypassed by audioEffectController.isAbBypassed.collectAsState()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // One-tap A/B Bypass pill
                Surface(
                    onClick = { audioEffectController.toggleAbBypass() },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isAbBypassed) VesperDeepBurgundy else VesperBackgroundSecondary,
                    modifier = Modifier.testTag("tone_ab_bypass_button")
                ) {
                    Text(
                        text = if (isAbBypassed) "A/B: Flat" else "A/B",
                        style = VesperTypography.labelSmall.copy(
                            color = if (isAbBypassed) VesperBurgundyHighlight else VesperTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Processing status pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when (processingStatus) {
                        ProcessingStatus.OFF -> VesperSurfaceElevated
                        ProcessingStatus.DEVICE_EFFECT -> VesperDeepBurgundy
                        ProcessingStatus.SOFTWARE -> VesperBackgroundSecondary
                        ProcessingStatus.UNAVAILABLE -> VesperBackgroundSecondary
                    },
                    modifier = Modifier.testTag("tone_processing_status_pill")
                ) {
                    Text(
                        text = processingStatus.label,
                        style = VesperTypography.labelSmall.copy(
                            color = when (processingStatus) {
                                ProcessingStatus.OFF -> VesperTextMuted
                                ProcessingStatus.DEVICE_EFFECT -> VesperBurgundyHighlight
                                ProcessingStatus.SOFTWARE -> VesperTextPrimary
                                ProcessingStatus.UNAVAILABLE -> Color(0xFFFF8B8B)
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleEnable,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VesperTextPrimary,
                        checkedTrackColor = VesperBurgundyAccent,
                        uncheckedThumbColor = VesperTextMuted,
                        uncheckedTrackColor = VesperBackgroundSecondary
                    ),
                    modifier = Modifier.testTag("tone_master_power_switch")
                )
            }
        }

        // Output Route & Headroom Bar
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = VesperSurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .border(1.dp, VesperOutline, RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio Route
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (outputRoute.contains("Bluetooth")) Icons.Default.Headphones else Icons.Default.Speaker,
                        contentDescription = null,
                        tint = VesperBurgundyHighlight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = outputRoute,
                            style = VesperTypography.labelMedium.copy(
                                color = VesperTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "Android Audio Session • Active Route",
                            style = VesperTypography.labelSmall.copy(color = VesperTextMuted, fontSize = 10.sp)
                        )
                    }
                }

                // Headroom Preamp Pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (headroomStatus.hasClipRisk) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF5A1A1A),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFF8B8B),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Clip Risk",
                                    style = VesperTypography.labelSmall.copy(color = Color(0xFFFF8B8B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Preamp: ${if (headroomProtected && headroomStatus.preampAttenuationDb < 0f) "%.1f dB".format(headroomStatus.preampAttenuationDb) else "0.0 dB"}",
                        style = VesperTypography.labelSmall.copy(
                            color = if (headroomStatus.hasClipRisk) Color(0xFFFF8B8B) else VesperTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // Centred Top Segmented Navigation: [ Equalizer ] [ Modes ]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = VesperBackgroundSecondary,
                modifier = Modifier
                    .border(1.dp, VesperOutline, RoundedCornerShape(24.dp))
                    .testTag("tone_segmented_tabs")
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    // Equalizer Tab
                    val eqSelected = currentTab == AudioTuningTab.EQUALIZER
                    Surface(
                        onClick = { currentTab = AudioTuningTab.EQUALIZER },
                        shape = RoundedCornerShape(20.dp),
                        color = if (eqSelected) VesperBurgundyAccent else Color.Transparent,
                        modifier = Modifier.testTag("tone_tab_equalizer")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = null,
                                tint = if (eqSelected) VesperTextPrimary else VesperTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Equalizer",
                                style = VesperTypography.labelLarge.copy(
                                    fontWeight = if (eqSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (eqSelected) VesperTextPrimary else VesperTextSecondary
                                )
                            )
                        }
                    }

                    // Modes Tab
                    val modesSelected = currentTab == AudioTuningTab.MODES
                    Surface(
                        onClick = { currentTab = AudioTuningTab.MODES },
                        shape = RoundedCornerShape(20.dp),
                        color = if (modesSelected) VesperBurgundyAccent else Color.Transparent,
                        modifier = Modifier.testTag("tone_tab_modes")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (modesSelected) VesperTextPrimary else VesperTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Modes (6)",
                                style = VesperTypography.labelLarge.copy(
                                    fontWeight = if (modesSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (modesSelected) VesperTextPrimary else VesperTextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Tab Content Switching
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ToneTabTransition",
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { tab ->
            when (tab) {
                AudioTuningTab.EQUALIZER -> {
                    EqualizerTabView(
                        audioEffectController = audioEffectController,
                        isEnabled = isEnabled,
                        bands = bands,
                        bandRange = bandRange,
                        activePreset = activePreset,
                        bassBoost = bassBoost,
                        customPresets = customPresets,
                        preampHeadroomEnabled = preampHeadroomEnabled,
                        audioBalance = audioBalance,
                        onBandLevelChange = onBandLevelChange,
                        onPresetSelect = onPresetSelect,
                        onBassBoostChange = onBassBoostChange,
                        onToggleHeadroomProtection = onToggleHeadroomProtection,
                        onAudioBalanceChange = onAudioBalanceChange,
                        onResetToFlat = onResetToFlat,
                        onOpenSavePresetDialog = { showSavePresetDialog = true },
                        onOpenRenamePresetDialog = { presetToRename = it },
                        onDuplicateCustomPreset = onDuplicateCustomPreset,
                        onDeleteCustomPreset = onDeleteCustomPreset
                    )
                }
                AudioTuningTab.MODES -> {
                    ModesTabView(
                        isEnabled = isEnabled,
                        activeProfile = activeProfile,
                        activePreset = activePreset,
                        onPresetSelect = onPresetSelect,
                        onProfileSelect = onProfileSelect,
                        onSwitchToEqualizer = { currentTab = AudioTuningTab.EQUALIZER }
                    )
                }
            }
        }
    }

    // Save Custom Preset Dialog
    if (showSavePresetDialog) {
        var presetName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            containerColor = VesperSurfaceElevated,
            title = {
                Text(
                    text = "Save Custom Preset",
                    style = VesperTypography.titleLarge.copy(color = VesperTextPrimary, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a name for your custom equalizer profile:",
                        style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = { presetName = it },
                        placeholder = { Text("e.g. Midnight Acoustic", color = VesperTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VesperBurgundyAccent,
                            unfocusedBorderColor = VesperOutline,
                            focusedTextColor = VesperTextPrimary,
                            unfocusedTextColor = VesperTextPrimary,
                            cursorColor = VesperBurgundyAccent
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("save_preset_input")
                    )
                }
            },
            confirmButton = {
                VesperPrimaryButton(
                    text = "Save Preset",
                    enabled = presetName.isNotBlank(),
                    onClick = {
                        if (presetName.isNotBlank()) {
                            onSaveCustomPreset(presetName.trim())
                            showSavePresetDialog = false
                        }
                    },
                    testTag = "save_preset_confirm_button"
                )
            },
            dismissButton = {
                Surface(
                    onClick = { showSavePresetDialog = false },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = VesperTypography.labelLarge.copy(color = VesperTextSecondary),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        )
    }

    // Rename Custom Preset Dialog
    presetToRename?.let { oldName ->
        var newName by remember(oldName) { mutableStateOf(oldName) }
        AlertDialog(
            onDismissRequest = { presetToRename = null },
            containerColor = VesperSurfaceElevated,
            title = {
                Text(
                    text = "Rename Custom Preset",
                    style = VesperTypography.titleLarge.copy(color = VesperTextPrimary, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Update preset name for \"$oldName\":",
                        style = VesperTypography.bodyMedium.copy(color = VesperTextSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VesperBurgundyAccent,
                            unfocusedBorderColor = VesperOutline,
                            focusedTextColor = VesperTextPrimary,
                            unfocusedTextColor = VesperTextPrimary,
                            cursorColor = VesperBurgundyAccent
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("rename_preset_input")
                    )
                }
            },
            confirmButton = {
                VesperPrimaryButton(
                    text = "Rename",
                    enabled = newName.isNotBlank() && newName.trim() != oldName,
                    onClick = {
                        onRenameCustomPreset(oldName, newName.trim())
                        presetToRename = null
                    },
                    testTag = "rename_preset_confirm_button"
                )
            },
            dismissButton = {
                Surface(
                    onClick = { presetToRename = null },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = VesperTypography.labelLarge.copy(color = VesperTextSecondary),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        )
    }

    // About Tone Truth Sheet Dialog
    if (showAboutToneDialog) {
        AlertDialog(
            onDismissRequest = { showAboutToneDialog = false },
            containerColor = VesperSurfaceElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = VesperBurgundyHighlight,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "About Tone Engine",
                        style = VesperTypography.titleLarge.copy(color = VesperTextPrimary, fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    ToneTruthItem(
                        title = "Why No Fake Atmos or 3D Upscaler?",
                        description = "Stereo music is mastered specifically for two transducers. Artificial 3D upscaling smears phase relationships, hollows out the vocal core, and introduces comb filtering."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ToneTruthItem(
                        title = "Lossy Codecs (MP3, AAC, OGG)",
                        description = "Lossy compression permanently discards high-frequency perceptual data. Equalization can sculpt and balance existing frequencies, but cannot resurrect information discarded during encoding."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ToneTruthItem(
                        title = "Headroom Preamp Protection",
                        description = "Boosting any frequency band increases total output amplitude. Vesper's automatic headroom calculates negative preamp attenuation to prevent digital clipping on positive EQ gains."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ToneTruthItem(
                        title = "VESPER Signature & Flat / DSP Off",
                        description = "VESPER Signature is our balanced house voicing with controlled low-end weight, clear vocal presence, smooth treble, and automatic headroom protection. Use Flat / DSP Off or A/B Bypass at any time for 0 dB uncolored comparison."
                    )
                }
            },
            confirmButton = {
                VesperPrimaryButton(
                    text = "Understood",
                    onClick = { showAboutToneDialog = false },
                    testTag = "about_tone_dismiss"
                )
            }
        )
    }
}

@Composable
private fun ToneTruthItem(title: String, description: String) {
    Column {
        Text(
            text = title,
            style = VesperTypography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = VesperBurgundyHighlight
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = VesperTypography.bodySmall.copy(
                color = VesperTextSecondary,
                lineHeight = 18.sp
            )
        )
    }
}

@Composable
private fun EqualizerTabView(
    audioEffectController: AudioEffectController,
    isEnabled: Boolean,
    bands: List<EqBand>,
    bandRange: Pair<Short, Short>,
    activePreset: String,
    bassBoost: Short,
    customPresets: List<CustomEqPresetEntity>,
    preampHeadroomEnabled: Boolean,
    audioBalance: Float,
    onBandLevelChange: (Short, Short) -> Unit,
    onPresetSelect: (String) -> Unit,
    onBassBoostChange: (Short) -> Unit,
    onToggleHeadroomProtection: (Boolean) -> Unit,
    onAudioBalanceChange: (Float) -> Unit,
    onResetToFlat: () -> Unit,
    onOpenSavePresetDialog: () -> Unit,
    onOpenRenamePresetDialog: (String) -> Unit,
    onDuplicateCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    var showPresetMenu by remember { mutableStateOf(false) }

    val builtInPresets = listOf(
        "Balanced", "Bass", "Vocal", "Rock", "Pop", "Acoustic", "Custom"
    )
    val activeCustomPreset = customPresets.firstOrNull { it.name == activePreset }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        // Preset Card & Save / Manage
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = VesperSurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, VesperOutline, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Surface(
                            onClick = { showPresetMenu = true },
                            shape = RoundedCornerShape(16.dp),
                            color = VesperBackgroundSecondary,
                            modifier = Modifier
                                .border(1.dp, VesperBorderAccent, RoundedCornerShape(16.dp))
                                .testTag("preset_dropdown_trigger")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Mode: $activePreset",
                                    style = VesperTypography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = VesperTextPrimary
                                    )
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showPresetMenu,
                            onDismissRequest = { showPresetMenu = false },
                            modifier = Modifier
                                .background(VesperSurfaceElevated)
                                .border(1.dp, VesperOutline, RoundedCornerShape(12.dp))
                        ) {
                            builtInPresets.forEach { preset ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = preset,
                                            style = VesperTypography.bodyMedium.copy(
                                                color = if (preset == activePreset) VesperBurgundyAccent else VesperTextPrimary,
                                                fontWeight = if (preset == activePreset) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    },
                                    onClick = {
                                        onPresetSelect(preset)
                                        showPresetMenu = false
                                    },
                                    trailingIcon = {
                                        if (preset == activePreset) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = VesperBurgundyAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )
                            }

                            if (customPresets.isNotEmpty()) {
                                HorizontalDivider(color = VesperOutline)
                                customPresets.forEach { custom ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "★ ${custom.name}",
                                                style = VesperTypography.bodyMedium.copy(
                                                    color = if (custom.name == activePreset) VesperBurgundyAccent else VesperTextPrimary,
                                                    fontWeight = if (custom.name == activePreset) FontWeight.Bold else FontWeight.Normal
                                                )
                                            )
                                        },
                                        onClick = {
                                            onPresetSelect(custom.name)
                                            showPresetMenu = false
                                        },
                                        trailingIcon = {
                                            if (custom.name == activePreset) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = VesperBurgundyAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onResetToFlat,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(VesperBackgroundSecondary)
                                .border(1.dp, VesperOutline, CircleShape)
                                .testTag("eq_reset_flat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset (0 dB Flat)",
                                tint = VesperTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onOpenSavePresetDialog,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(VesperBackgroundSecondary)
                                .border(1.dp, VesperOutline, CircleShape)
                                .testTag("eq_save_preset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = "Save Preset",
                                tint = VesperBurgundyAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Custom Preset Management Row (Rename / Duplicate / Delete)
                if (activeCustomPreset != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { onOpenRenamePresetDialog(activeCustomPreset.name) },
                            shape = RoundedCornerShape(10.dp),
                            color = VesperBackgroundSecondary,
                            modifier = Modifier
                                .border(1.dp, VesperOutline, RoundedCornerShape(10.dp))
                                .testTag("eq_rename_custom_preset_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rename Preset",
                                    tint = VesperTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Rename",
                                    style = VesperTypography.labelSmall.copy(color = VesperTextSecondary)
                                )
                            }
                        }

                        Surface(
                            onClick = { onDuplicateCustomPreset(activeCustomPreset.name) },
                            shape = RoundedCornerShape(10.dp),
                            color = VesperBackgroundSecondary,
                            modifier = Modifier
                                .border(1.dp, VesperOutline, RoundedCornerShape(10.dp))
                                .testTag("eq_duplicate_custom_preset_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Duplicate Preset",
                                    tint = VesperTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Duplicate",
                                    style = VesperTypography.labelSmall.copy(color = VesperTextSecondary)
                                )
                            }
                        }

                        Surface(
                            onClick = { onDeleteCustomPreset(activeCustomPreset.name) },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF3A161E),
                            modifier = Modifier
                                .border(1.dp, VesperBurgundyAccent, RoundedCornerShape(10.dp))
                                .testTag("eq_delete_custom_preset_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Preset",
                                    tint = VesperBurgundyHighlight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Delete",
                                    style = VesperTypography.labelSmall.copy(color = VesperBurgundyHighlight)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Visual Frequency Curve
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = VesperSurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, VesperOutline, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACOUSTIC SPECTRUM",
                        style = VesperTypography.labelSmall.copy(
                            color = VesperTextMuted,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (isEnabled) "Active Filter Curve" else "Bypass (0 dB Flat)",
                        style = VesperTypography.labelSmall.copy(
                            color = if (isEnabled) VesperBurgundyAccent else VesperTextMuted
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                FrequencyCurveView(
                    bands = bands,
                    minLevel = bandRange.first,
                    maxLevel = bandRange.second,
                    isEnabled = isEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Multi-Band Sliders Column
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = VesperSurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, VesperOutline, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EQUALIZER BANDS",
                        style = VesperTypography.labelSmall.copy(
                            color = VesperTextMuted,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "${bands.size} Hardware Bands",
                        style = VesperTypography.labelSmall.copy(color = VesperTextSecondary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(bands) { band ->
                        BandColumn(
                            band = band,
                            minLevel = bandRange.first,
                            maxLevel = bandRange.second,
                            isEnabled = isEnabled,
                            onLevelChange = { newLevel ->
                                onBandLevelChange(band.index, newLevel)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bass Enhancement Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = VesperSurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, VesperOutline, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BASS ENHANCEMENT",
                            style = VesperTypography.labelSmall.copy(
                                color = VesperTextMuted,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Sub-harmonic acoustic density",
                            style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                        )
                    }

                    Text(
                        text = "${(bassBoost.toFloat() / 10f).toInt()}%",
                        style = VesperTypography.titleMedium.copy(
                            color = if (isEnabled) VesperBurgundyAccent else VesperTextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = bassBoost.toFloat(),
                    onValueChange = { onBassBoostChange(it.toInt().toShort()) },
                    valueRange = 0f..1000f,
                    enabled = isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = VesperTextPrimary,
                        activeTrackColor = VesperBurgundyAccent,
                        inactiveTrackColor = VesperBackgroundSecondary,
                        disabledThumbColor = VesperTextMuted,
                        disabledActiveTrackColor = VesperOutline
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("bass_boost_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Headroom Protection & Channel Balance Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = VesperSurfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, VesperOutline, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HEADROOM PROTECTION",
                            style = VesperTypography.labelSmall.copy(
                                color = VesperTextMuted,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Automatic preamp attenuation prevents positive EQ clipping",
                            style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                        )
                    }
                    Switch(
                        checked = preampHeadroomEnabled,
                        onCheckedChange = onToggleHeadroomProtection,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VesperTextPrimary,
                            checkedTrackColor = VesperBurgundyAccent,
                            uncheckedThumbColor = VesperTextMuted,
                            uncheckedTrackColor = VesperBackgroundSecondary
                        ),
                        modifier = Modifier.testTag("headroom_protection_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = VesperOutline)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "STEREO BALANCE",
                            style = VesperTypography.labelSmall.copy(
                                color = VesperTextMuted,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val balanceText = when {
                            abs(audioBalance) < 0.05f -> "Centered (0%)"
                            audioBalance < 0f -> "Left ${(abs(audioBalance) * 100).toInt()}%"
                            else -> "Right ${(audioBalance * 100).toInt()}%"
                        }
                        Text(
                            text = balanceText,
                            style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                        )
                    }

                    if (abs(audioBalance) >= 0.05f) {
                        Surface(
                            onClick = { onAudioBalanceChange(0f) },
                            shape = RoundedCornerShape(10.dp),
                            color = VesperBackgroundSecondary,
                            modifier = Modifier.border(1.dp, VesperOutline, RoundedCornerShape(10.dp))
                        ) {
                            Text(
                                text = "Center",
                                style = VesperTypography.labelSmall.copy(color = VesperBurgundyHighlight),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Slider(
                    value = audioBalance,
                    onValueChange = onAudioBalanceChange,
                    valueRange = -1f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = VesperTextPrimary,
                        activeTrackColor = VesperBurgundyAccent,
                        inactiveTrackColor = VesperBackgroundSecondary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("audio_balance_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ModesTabView(
    isEnabled: Boolean,
    activeProfile: SoundProfile,
    activePreset: String,
    onPresetSelect: (String) -> Unit,
    onProfileSelect: (SoundProfile) -> Unit,
    onSwitchToEqualizer: () -> Unit
) {
    val modes = AudioEffectController.BASIC_MODES

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!isEnabled) {
            item(key = "eq_disabled_notice") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = VesperBackgroundSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VesperOutline, RoundedCornerShape(14.dp))
                        .testTag("modes_eq_disabled_notice")
                ) {
                    Text(
                        text = "EQ is currently off. Tap any mode below to enable EQ and apply its curve.",
                        style = VesperTypography.bodySmall.copy(color = VesperTextSecondary),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
        items(modes, key = { it.name }) { mode ->
            val isSelected = isEnabled && activePreset.equals(mode.name, ignoreCase = true)

            Surface(
                onClick = { onPresetSelect(mode.name) },
                shape = RoundedCornerShape(18.dp),
                color = if (isSelected) VesperDeepBurgundy else VesperSurfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isSelected) VesperBurgundyHighlight else VesperOutline,
                        RoundedCornerShape(18.dp)
                    )
                    .testTag("basic_eq_mode_${mode.name.lowercase()}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = mode.name,
                                    style = VesperTypography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = VesperTextPrimary
                                    )
                                )
                                if (isSelected && !mode.name.equals("Balanced", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = VesperBackgroundSecondary
                                    ) {
                                        Text(
                                            text = "TAP AGAIN TO RESET",
                                            style = VesperTypography.labelSmall.copy(
                                                color = VesperBurgundyHighlight,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = mode.description,
                                style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = VesperBurgundyHighlight,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (isSelected) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                onClick = onSwitchToEqualizer,
                                shape = RoundedCornerShape(12.dp),
                                color = VesperBackgroundSecondary,
                                modifier = Modifier.border(1.dp, VesperBorderAccent, RoundedCornerShape(12.dp))
                            ) {
                                Text(
                                    text = "Fine Tune in Equalizer →",
                                    style = VesperTypography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = VesperTextPrimary
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FrequencyCurveView(
    bands: List<EqBand>,
    minLevel: Short,
    maxLevel: Short,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (bands.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        val range = (maxLevel - minLevel).coerceAtLeast(1)

        // Draw 0 dB center reference line
        val zeroY = h * (1f - (0 - minLevel).toFloat() / range)
        drawLine(
            color = Color(0x33FFFFFF),
            start = Offset(0f, zeroY),
            end = Offset(w, zeroY),
            strokeWidth = 1.dp.toPx()
        )

        // Build curve path
        val stepX = w / (bands.size - 1).coerceAtLeast(1)
        val path = Path()

        bands.forEachIndexed { i, band ->
            val gainFraction = ((band.levelMb - minLevel).toFloat() / range).coerceIn(0f, 1f)
            val y = h * (1f - gainFraction)
            val x = i * stepX

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                val prevGainFraction = ((bands[i - 1].levelMb - minLevel).toFloat() / range).coerceIn(0f, 1f)
                val prevY = h * (1f - prevGainFraction)
                val prevX = (i - 1) * stepX
                val controlX1 = prevX + (x - prevX) / 2
                val controlX2 = controlX1
                path.cubicTo(controlX1, prevY, controlX2, y, x, y)
            }
        }

        drawPath(
            path = path,
            color = if (isEnabled) Color(0xFF8C2F48) else Color(0x55A89895),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun BandColumn(
    band: EqBand,
    minLevel: Short,
    maxLevel: Short,
    isEnabled: Boolean,
    onLevelChange: (Short) -> Unit
) {
    val dbValue = (band.levelMb.toFloat() / 100f)
    val formattedDb = if (dbValue > 0) "+%.1f dB".format(dbValue) else "%.1f dB".format(dbValue)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(62.dp)
    ) {
        // Gain dB readout
        Text(
            text = formattedDb,
            style = VesperTypography.labelSmall.copy(
                fontSize = 10.sp,
                color = if (isEnabled) VesperBurgundyAccent else VesperTextMuted,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // +1 dB Step Button
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isEnabled) VesperDeepBurgundy else VesperOutline.copy(alpha = 0.5f))
                .border(1.dp, if (isEnabled) VesperBurgundyAccent else VesperOutline, CircleShape)
                .clickable(enabled = isEnabled) {
                    val newLevel = (band.levelMb + 100).coerceAtMost(maxLevel.toInt()).toShort()
                    onLevelChange(newLevel)
                }
                .testTag("eq_plus_band_${band.index}"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                style = VesperTypography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled) VesperTextPrimary else VesperTextMuted,
                    fontSize = 16.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Vertical Slider Track
        val totalRange = (maxLevel - minLevel).coerceAtLeast(1)
        val fraction = ((band.levelMb - minLevel).toFloat() / totalRange).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .width(28.dp)
                .height(130.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(VesperBackgroundSecondary)
                .border(1.dp, VesperOutline, RoundedCornerShape(14.dp))
                .pointerInput(isEnabled) {
                    if (!isEnabled) return@pointerInput
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaMb = (-dragAmount / 130.dp.toPx() * totalRange).toInt()
                        val newLevel = (band.levelMb + deltaMb).coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                        onLevelChange(newLevel)
                    }
                }
                .pointerInput(isEnabled) {
                    if (!isEnabled) return@pointerInput
                    detectTapGestures { offset ->
                        val ratio = 1f - (offset.y / 130.dp.toPx()).coerceIn(0f, 1f)
                        val newLevel = (minLevel + ratio * totalRange).toInt().toShort()
                        onLevelChange(newLevel)
                    }
                }
        ) {
            // Fill level from bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((130.dp * fraction))
                    .align(Alignment.BottomCenter)
                    .background(if (isEnabled) VesperBurgundyAccent else VesperOutline)
            )

            // Slider Thumb Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (110.dp * fraction).coerceAtLeast(0.dp))
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isEnabled) VesperTextPrimary else VesperTextMuted)
                        .align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // -1 dB Step Button
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isEnabled) VesperDeepBurgundy else VesperOutline.copy(alpha = 0.5f))
                .border(1.dp, if (isEnabled) VesperBurgundyAccent else VesperOutline, CircleShape)
                .clickable(enabled = isEnabled) {
                    val newLevel = (band.levelMb - 100).coerceAtLeast(minLevel.toInt()).toShort()
                    onLevelChange(newLevel)
                }
                .testTag("eq_minus_band_${band.index}"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "-",
                style = VesperTypography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled) VesperTextPrimary else VesperTextMuted,
                    fontSize = 16.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Frequency Label
        Text(
            text = band.formattedFreq,
            style = VesperTypography.labelSmall.copy(
                fontSize = 11.sp,
                color = VesperTextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

private fun getAudioOutputRoute(context: Context): String {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "Speaker (Built-in)"
    val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    for (device in devices) {
        when (device.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> {
                val name = device.productName?.toString().orEmpty()
                return if (name.isNotBlank()) "Bluetooth ($name)" else "Bluetooth Audio"
            }
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> return "Wired Headphones (3.5mm)"
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> {
                val name = device.productName?.toString().orEmpty()
                return if (name.isNotBlank()) "USB DAC ($name)" else "USB Audio DAC"
            }
        }
    }
    return "Speaker (Built-in)"
}

@Composable
fun ToneScreen(
    audioEffectController: AudioEffectController,
    isEnabled: Boolean,
    bands: List<EqBand>,
    bandRange: Pair<Short, Short>,
    activePreset: String,
    bassBoost: Short,
    onToggleEnable: (Boolean) -> Unit,
    onBandLevelChange: (Short, Short) -> Unit,
    onPresetSelect: (String) -> Unit,
    onBassBoostChange: (Short) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onBackClick: () -> Unit,
    onResetToFlat: () -> Unit = {},
    activeProfile: SoundProfile = SoundProfile.BALANCED,
    onProfileSelect: (SoundProfile) -> Unit = {},
    onNavigateToModes: (() -> Unit)? = null,
    initialTab: AudioTuningTab = AudioTuningTab.EQUALIZER,
    modifier: Modifier = Modifier
) {
    AudioTuningScreen(
        audioEffectController = audioEffectController,
        isEnabled = isEnabled,
        bands = bands,
        bandRange = bandRange,
        activePreset = activePreset,
        bassBoost = bassBoost,
        onToggleEnable = onToggleEnable,
        onBandLevelChange = onBandLevelChange,
        onPresetSelect = onPresetSelect,
        onBassBoostChange = onBassBoostChange,
        onSaveCustomPreset = onSaveCustomPreset,
        onBackClick = onBackClick,
        onResetToFlat = onResetToFlat,
        activeProfile = activeProfile,
        onProfileSelect = onProfileSelect,
        onNavigateToModes = onNavigateToModes,
        initialTab = initialTab,
        modifier = modifier
    )
}
