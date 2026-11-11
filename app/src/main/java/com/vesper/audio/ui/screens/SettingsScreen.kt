package com.vesper.audio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vesper.audio.data.local.ScannedFolderEntity
import com.vesper.audio.model.Track
import com.vesper.audio.ui.SortMode
import com.vesper.audio.ui.theme.LocalVesperAccentColor
import com.vesper.audio.ui.theme.VesperBackground
import com.vesper.audio.ui.theme.VesperBackgroundSecondary
import com.vesper.audio.ui.theme.VesperBurgundyAccent
import com.vesper.audio.ui.theme.VesperBurgundyHighlight
import com.vesper.audio.ui.theme.VesperDeepBurgundy
import com.vesper.audio.ui.theme.VesperOutline
import com.vesper.audio.ui.theme.VesperSurfaceElevated
import com.vesper.audio.ui.theme.VesperTextMuted
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTextSecondary
import com.vesper.audio.ui.theme.VesperTypography
import com.vesper.audio.util.VesperArtworkResolver
import kotlin.math.roundToInt

/**
 * Clean, boxy Settings category hierarchy defined by OLD_VESPER_SETTINGS_DISCOVER_UPGRADE_SPEC.
 * Each category opens its own dedicated subpage so the main Settings screen remains scannable and uncluttered.
 */
enum class SettingsCategory(
    val title: String,
    val description: String,
    val testTag: String
) {
    AUDIO_QUALITY(
        title = "Audio quality",
        description = "Streaming sources, equalizer & headroom protection",
        testTag = "settings_cat_audio_quality"
    ),
    PLAYBACK(
        title = "Playback",
        description = "Gapless, crossfade, headphone disconnect & sleep timer",
        testTag = "settings_cat_playback"
    ),
    CONTENT_PREFERENCES(
        title = "Content preferences",
        description = "Default library sort order & short-track filtering",
        testTag = "settings_cat_content_preferences"
    ),
    APPEARANCE(
        title = "Appearance",
        description = "Nocturne AMOLED surface palette & lockscreen artwork",
        testTag = "settings_cat_appearance"
    ),
    PERFORMANCE(
        title = "Performance",
        description = "Artwork cache, Discover cache, rebuild & startup scan",
        testTag = "settings_cat_performance"
    ),
    LOCAL_MUSIC(
        title = "Local music",
        description = "SAF folders, media rescan, short audio filter & demos",
        testTag = "settings_cat_local_music"
    ),
    STORAGE(
        title = "Storage",
        description = "Library footprint, artwork cache & Discover cache usage",
        testTag = "settings_cat_storage"
    ),
    YOUR_DATA(
        title = "Your data",
        description = "On-device privacy, cache cleanup & preference reset",
        testTag = "settings_cat_your_data"
    ),
    MISCELLANEOUS(
        title = "Miscellaneous",
        description = "Support VESPER, version info & open-source credits",
        testTag = "settings_cat_miscellaneous"
    );

    val icon: ImageVector
        get() = when (this) {
            AUDIO_QUALITY -> Icons.Default.HighQuality
            PLAYBACK -> Icons.Default.PlayCircleOutline
            CONTENT_PREFERENCES -> Icons.Default.Tune
            APPEARANCE -> Icons.Default.Palette
            PERFORMANCE -> Icons.Default.Speed
            LOCAL_MUSIC -> Icons.Default.LibraryMusic
            STORAGE -> Icons.Default.Storage
            YOUR_DATA -> Icons.Default.Security
            MISCELLANEOUS -> Icons.Default.MoreHoriz
        }
}

@Composable
fun SettingsScreen(
    onNavigateToAudioTuning: () -> Unit,
    onNavigateToScanMedia: () -> Unit,
    onNavigateToDonate: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onLoadDemosClick: () -> Unit,
    crossfadeDurationSec: Int,
    onCrossfadeDurationChange: (Int) -> Unit,
    gaplessPlayback: Boolean,
    onGaplessPlaybackChange: (Boolean) -> Unit,
    pauseOnHeadphoneDisconnect: Boolean,
    onPauseOnDisconnectChange: (Boolean) -> Unit,
    resumeOnLaunch: Boolean,
    onResumeOnLaunchChange: (Boolean) -> Unit,
    filterShortTracks: Boolean,
    onFilterShortTracksChange: (Boolean) -> Unit,
    autoScanStartup: Boolean,
    onAutoScanStartupChange: (Boolean) -> Unit,
    preampHeadroomEnabled: Boolean = true,
    onPreampHeadroomChange: (Boolean) -> Unit = {},
    lockscreenArtwork: Boolean = true,
    onLockscreenArtworkChange: (Boolean) -> Unit = {},
    sortMode: SortMode = SortMode.TITLE_AZ,
    onSortModeChange: (SortMode) -> Unit = {},
    vesperThemeMode: String = "Nocturne",
    onVesperThemeModeChange: (String) -> Unit = {},
    directSourceStreamEnabled: Boolean = true,
    onDirectSourceStreamChange: (Boolean) -> Unit = {},
    wifiStreamingQuality: String = "High (320 kbps)",
    onWifiStreamingQualityChange: (String) -> Unit = {},
    mobileStreamingQuality: String = "Balanced (192 kbps)",
    onMobileStreamingQualityChange: (String) -> Unit = {},
    wifiOnlyDownloads: Boolean = false,
    onWifiOnlyDownloadsChange: (Boolean) -> Unit = {},
    disableEffectsOnSpeaker: Boolean = false,
    onDisableEffectsOnSpeakerChange: (Boolean) -> Unit = {},
    loudnessNormalizationEnabled: Boolean = true,
    onLoudnessNormalizationChange: (Boolean) -> Unit = {},
    hideRecentsOnHome: Boolean = false,
    onHideRecentsOnHomeChange: (Boolean) -> Unit = {},
    hideMoodPicksOnHome: Boolean = false,
    onHideMoodPicksOnHomeChange: (Boolean) -> Unit = {},
    hideTopicPicksOnHome: Boolean = false,
    onHideTopicPicksOnHomeChange: (Boolean) -> Unit = {},
    reduceAnimations: Boolean = false,
    onReduceAnimationsChange: (Boolean) -> Unit = {},
    reduceDynamicBlur: Boolean = false,
    onReduceDynamicBlurChange: (Boolean) -> Unit = {},
    fullScreenCoverArt: Boolean = false,
    onFullScreenCoverArtChange: (Boolean) -> Unit = {},
    syncedLyricsEnabled: Boolean = true,
    onSyncedLyricsChange: (Boolean) -> Unit = {},
    blurUnfocusedLyrics: Boolean = true,
    onBlurUnfocusedLyricsChange: (Boolean) -> Unit = {},
    tracks: List<Track> = emptyList(),
    scannedFolders: List<ScannedFolderEntity> = emptyList(),
    discoverCacheCount: Int = 0,
    onClearDiscoverCache: () -> Unit = {},
    onClearArtworkCache: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onRebuildLibraryCache: () -> Unit,
    onResetSettingsToDefaults: () -> Unit = {},
    onBackClick: () -> Unit,
    initialCategory: SettingsCategory? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dynamicAccent = LocalVesperAccentColor.current
    var activeCategory by rememberSaveable { mutableStateOf(initialCategory) }
    var settingsSearchQuery by rememberSaveable { mutableStateOf("") }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var cacheBytes by remember { mutableLongStateOf(VesperArtworkResolver.getArtworkCacheSizeBytes(context)) }

    // Subpage -> Settings Landing Page -> Exit Settings
    BackHandler(enabled = activeCategory != null) {
        activeCategory = null
    }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            containerColor = VesperSurfaceElevated,
            titleContentColor = VesperTextPrimary,
            textContentColor = VesperTextSecondary,
            title = {
                Text(
                    text = "Clear Artwork Cache?",
                    style = VesperTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "This removes cached album art thumbnails (${VesperArtworkResolver.formatBytes(cacheBytes)}). Your audio files, playlists, and favorites will not be affected.",
                    style = VesperTypography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearArtworkCache()
                        cacheBytes = VesperArtworkResolver.getArtworkCacheSizeBytes(context)
                        showClearCacheConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VesperBurgundyAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_clear_cache_button")
                ) {
                    Text("Clear Cache", color = VesperTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) {
                    Text("Cancel", color = VesperTextSecondary)
                }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = VesperSurfaceElevated,
            titleContentColor = VesperTextPrimary,
            textContentColor = VesperTextSecondary,
            title = {
                Text(
                    text = "Reset Preferences to Defaults?",
                    style = VesperTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "This restores playback, sort order, and startup scan toggles to their factory defaults. Your local music, playlists, and favorites are untouched.",
                    style = VesperTypography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetSettingsToDefaults()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VesperBurgundyAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_reset_settings_button")
                ) {
                    Text("Reset Defaults", color = VesperTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel", color = VesperTextSecondary)
                }
            }
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(VesperBackground)
    ) {
        val isCompactHeight = maxHeight < 480.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 16.dp))

            // Top Header Bar (Consistent Back navigation from Subpage -> Landing -> Exit)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
            IconButton(
                onClick = {
                    if (activeCategory != null) {
                        activeCategory = null
                    } else {
                        onBackClick()
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(VesperBackgroundSecondary)
                    .border(1.dp, VesperOutline, RoundedCornerShape(10.dp))
                    .testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (activeCategory != null) "Back to Settings" else "Back",
                    tint = VesperTextPrimary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activeCategory?.title ?: "Settings",
                    style = VesperTypography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = VesperTextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = activeCategory?.description ?: "Configure audio, playback, local library & data",
                    style = VesperTypography.bodySmall.copy(color = VesperTextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

            Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 18.dp))

            val currentCat = activeCategory
            if (currentCat == null) {
            // Search bar on Settings landing page
            OutlinedTextField(
                value = settingsSearchQuery,
                onValueChange = { settingsSearchQuery = it },
                singleLine = true,
                placeholder = { Text("Search settings (equalizer, theme, cache, scan...)", color = VesperTextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search settings",
                        tint = dynamicAccent,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (settingsSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { settingsSearchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear settings search",
                                tint = VesperTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = VesperBackgroundSecondary,
                    unfocusedContainerColor = VesperBackgroundSecondary,
                    focusedBorderColor = dynamicAccent,
                    unfocusedBorderColor = VesperOutline,
                    focusedTextColor = VesperTextPrimary,
                    unfocusedTextColor = VesperTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("settings_search_input")
            )

            val searchableItems = remember {
                listOf(
                    Triple("Equalizer & 6 Basic EQ Modes", "Balanced, Bass, Vocal, Rock, Pop, Acoustic & Custom EQ", SettingsCategory.AUDIO_QUALITY),
                    Triple("Direct Source Stream (Online Discover)", "Enable or disable online discovery streaming", SettingsCategory.AUDIO_QUALITY),
                    Triple("Wi-Fi & Mobile Streaming Quality", "Configure audio stream bitrate on Wi-Fi and mobile data", SettingsCategory.AUDIO_QUALITY),
                    Triple("Disable Effects on Phone Speaker", "Bypass EQ when playing through built-in phone speaker", SettingsCategory.PLAYBACK),
                    Triple("Loudness Normalisation & Headroom", "Prevent clipping and balance perceived loudness", SettingsCategory.PLAYBACK),
                    Triple("Gapless Playback & Crossfade", "Smooth transitions between tracks", SettingsCategory.PLAYBACK),
                    Triple("Sleep Timer", "Countdown timer or stop at end of track", SettingsCategory.PLAYBACK),
                    Triple("Hide Recents / Mood / Topic Picks", "Customize shelves shown on the Home dashboard", SettingsCategory.CONTENT_PREFERENCES),
                    Triple("Default Library Sort Order", "Sort songs by Title, Artist, Album, Recently Added, or Duration", SettingsCategory.CONTENT_PREFERENCES),
                    Triple("VESPER Theme (Nocturne / Cosmic Latte / Pure AMOLED)", "Switch visual surface palette while preserving playback", SettingsCategory.APPEARANCE),
                    Triple("Reduce Animations & Dynamic Blur", "Streamline visual effects and cover art blur", SettingsCategory.APPEARANCE),
                    Triple("Full-Screen Cover Art & Synced Lyrics", "Configure Now Playing artwork and lyrics focus", SettingsCategory.APPEARANCE),
                    Triple("Clear Artwork & Discover Cache", "Manage disk and Room metadata caches", SettingsCategory.STORAGE),
                    Triple("Manage Music Folders & Rescan", "Import SAF folders and run incremental local media scan", SettingsCategory.LOCAL_MUSIC),
                    Triple("Reset Preferences to Defaults", "Restore default VESPER settings without touching your music", SettingsCategory.YOUR_DATA),
                    Triple("Support VESPER & About", "Version info, privacy guarantee & indie supporter options", SettingsCategory.MISCELLANEOUS)
                )
            }

            val q = settingsSearchQuery.trim().lowercase()
            val filteredCategories = remember(q) {
                if (q.isBlank()) {
                    SettingsCategory.entries
                } else {
                    SettingsCategory.entries.filter { cat ->
                        cat.title.lowercase().contains(q) ||
                            cat.description.lowercase().contains(q) ||
                            searchableItems.any { it.third == cat && (it.first.lowercase().contains(q) || it.second.lowercase().contains(q)) }
                    }
                }
            }
            val matchedDirectSettings = remember(q) {
                if (q.isBlank()) emptyList()
                else searchableItems.filter { it.first.lowercase().contains(q) || it.second.lowercase().contains(q) }
            }

            // LANDING PAGE: Clean, boxy category cards + direct matched setting shortcuts
            LazyColumn(
                contentPadding = PaddingValues(bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("settings_list")
            ) {
                items(filteredCategories, key = { it.name }) { category ->
                    BoxySettingsCategoryCard(
                        category = category,
                        onClick = { activeCategory = category }
                    )
                }

                if (matchedDirectSettings.isNotEmpty()) {
                    item(key = "direct_matches_header") {
                        SubpageSectionTitle("MATCHING SETTINGS (${matchedDirectSettings.size})")
                    }
                    items(matchedDirectSettings, key = { it.first }) { (title, subtitle, targetCat) ->
                        SettingsActionRow(
                            icon = targetCat.icon,
                            title = title,
                            subtitle = "${targetCat.title} • $subtitle",
                            onClick = { activeCategory = targetCat },
                            testTag = "settings_search_match_${targetCat.name.lowercase()}"
                        )
                    }
                }
            }
        } else {
            // SUBPAGE CONTENT
            LazyColumn(
                contentPadding = PaddingValues(bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("settings_subpage_${currentCat.name.lowercase()}")
            ) {
                when (currentCat) {
                    SettingsCategory.AUDIO_QUALITY -> {
                        item {
                            SubpageSectionTitle("DSP & EQUALIZER")
                        }
                        item {
                            SettingsActionRow(
                                icon = Icons.Default.Equalizer,
                                title = "Equalizer & 6 Basic EQ Modes",
                                subtitle = "Balanced, Bass, Vocal, Rock, Pop, Acoustic & Custom 10-band EQ",
                                onClick = onNavigateToAudioTuning,
                                testTag = "settings_audio_tuning_button"
                            )
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.GraphicEq,
                                title = "Preamp Headroom Protection",
                                subtitle = "Automatically attenuate gain when EQ bands are boosted above 0 dB",
                                checked = preampHeadroomEnabled,
                                onCheckedChange = onPreampHeadroomChange,
                                testTag = "settings_toggle_headroom"
                            )
                        }
                        item {
                            SubpageSectionTitle("STREAMING & DOWNLOAD QUALITY")
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.CloudQueue,
                                title = "Direct Source Stream (Online Discover)",
                                subtitle = "Allow streaming from authorised public discovery sources",
                                checked = directSourceStreamEnabled,
                                onCheckedChange = onDirectSourceStreamChange,
                                testTag = "settings_toggle_direct_source_stream"
                            )
                        }
                        item {
                            QualityOptionSelectorCard(
                                title = "Wi-Fi Streaming Quality",
                                selectedQuality = wifiStreamingQuality,
                                options = listOf("High (320 kbps)", "Balanced (192 kbps)", "Data Saver (128 kbps)"),
                                onSelect = onWifiStreamingQualityChange,
                                testTagPrefix = "settings_wifi_quality"
                            )
                        }
                        item {
                            QualityOptionSelectorCard(
                                title = "Mobile Data Streaming Quality",
                                selectedQuality = mobileStreamingQuality,
                                options = listOf("High (320 kbps)", "Balanced (192 kbps)", "Data Saver (128 kbps)"),
                                onSelect = onMobileStreamingQualityChange,
                                testTagPrefix = "settings_mobile_quality"
                            )
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.Wifi,
                                title = "Wi-Fi-Only Downloads & Caching",
                                subtitle = "Restrict artwork and track pre-caching to unmetered Wi-Fi",
                                checked = wifiOnlyDownloads,
                                onCheckedChange = onWifiOnlyDownloadsChange,
                                testTag = "settings_toggle_wifi_only_downloads"
                            )
                        }
                        item {
                            SubpageSectionTitle("ONLINE STREAMING SOURCES")
                        }
                        item {
                            BoxyInfoCard(
                                icon = Icons.Default.CloudQueue,
                                title = "Audius Public API • Active",
                                subtitle = "Authorised public indie discovery stream ($wifiStreamingQuality)",
                                badgeText = if (directSourceStreamEnabled) "ENABLED" else "PAUSED"
                            )
                        }
                        item {
                            BoxyInfoCard(
                                icon = Icons.Default.Lock,
                                title = "JioSaavn & BitChord Adapters",
                                subtitle = "Disabled by default per VESPER Provider Policy (requires official partner credentials)",
                                badgeText = "DISABLED"
                            )
                        }
                        item {
                            SubpageSectionTitle("AUDIO OUTPUT")
                        }
                        item {
                            SettingsInfoRow(
                                icon = Icons.Default.Headphones,
                                title = "System Audio Routing",
                                value = "Android Media3 AudioTrack (Automatic)"
                            )
                        }
                    }

                    SettingsCategory.PLAYBACK -> {
                        item {
                            SubpageSectionTitle("ACOUSTIC PROCESSING & ROUTING")
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.Speaker,
                                title = "Disable Effects on Phone Speaker",
                                subtitle = "Automatically bypass EQ & bass boost on built-in phone speaker",
                                checked = disableEffectsOnSpeaker,
                                onCheckedChange = onDisableEffectsOnSpeakerChange,
                                testTag = "settings_toggle_speaker_bypass"
                            )
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.VolumeUp,
                                title = "Loudness Normalisation",
                                subtitle = "Maintain consistent playback headroom across tracks and EQ modes",
                                checked = loudnessNormalizationEnabled,
                                onCheckedChange = onLoudnessNormalizationChange,
                                testTag = "settings_toggle_loudness_norm"
                            )
                        }
                        item {
                            SettingsActionRow(
                                icon = Icons.Default.Equalizer,
                                title = "Open Equalizer & 6 Basic Modes",
                                subtitle = "Configure Balanced, Bass, Vocal, Rock, Pop, Acoustic or Custom EQ",
                                onClick = onNavigateToAudioTuning,
                                testTag = "settings_playback_open_eq_button"
                            )
                        }
                        item {
                            SubpageSectionTitle("TRANSITIONS & CONTINUITY")
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.GraphicEq,
                                title = "Gapless Playback",
                                subtitle = "Eliminate silence between consecutive album tracks",
                                checked = gaplessPlayback,
                                onCheckedChange = onGaplessPlaybackChange,
                                testTag = "settings_toggle_gapless"
                            )
                        }
                        item {
                            CrossfadeSliderCard(
                                durationSec = crossfadeDurationSec,
                                onDurationChange = onCrossfadeDurationChange
                            )
                        }
                        item {
                            SubpageSectionTitle("BEHAVIOUR & TIMERS")
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.Headphones,
                                title = "Pause on Headphone Disconnect",
                                subtitle = "Automatically pause when wired or Bluetooth audio disconnects",
                                checked = pauseOnHeadphoneDisconnect,
                                onCheckedChange = onPauseOnDisconnectChange,
                                testTag = "settings_toggle_pause_unplug"
                            )
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.PlayCircleOutline,
                                title = "Resume Playback on Launch",
                                subtitle = "Restore your last active queue and position on startup",
                                checked = resumeOnLaunch,
                                onCheckedChange = onResumeOnLaunchChange,
                                testTag = "settings_toggle_resume_startup"
                            )
                        }
                        item {
                            SettingsActionRow(
                                icon = Icons.Default.Bedtime,
                                title = "Sleep Timer",
                                subtitle = "Configure countdown timer or stop at end of current track",
                                onClick = onOpenSleepTimer,
                                testTag = "settings_sleep_timer_button"
                            )
                        }
                    }

                    SettingsCategory.CONTENT_PREFERENCES -> {
                        item {
                            SubpageSectionTitle("HOME SCREEN SECTIONS")
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.Tune,
                                title = "Hide Recents on Home",
                                subtitle = "Hide Recently Played & Continue Listening shelves on Home",
                                checked = hideRecentsOnHome,
                                onCheckedChange = onHideRecentsOnHomeChange,
                                testTag = "settings_toggle_hide_recents"
                            )
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.Tune,
                                title = "Hide Mood & Time Picks on Home",
                                subtitle = "Hide For This Hour & Tuned to Right Now cards on Home",
                                checked = hideMoodPicksOnHome,
                                onCheckedChange = onHideMoodPicksOnHomeChange,
                                testTag = "settings_toggle_hide_mood_picks"
                            )
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.Tune,
                                title = "Hide Quick Topic Pills on Home",
                                subtitle = "Hide top quick action pills (Favorites, Shuffle, Tones) on Home",
                                checked = hideTopicPicksOnHome,
                                onCheckedChange = onHideTopicPicksOnHomeChange,
                                testTag = "settings_toggle_hide_topic_picks"
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            SubpageSectionTitle("DEFAULT LIBRARY ORDER")
                        }
                        items(SortMode.entries, key = { it.name }) { mode ->
                            SortModeOptionRow(
                                mode = mode,
                                isSelected = sortMode == mode,
                                onSelect = { onSortModeChange(mode) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            SubpageSectionTitle("CONTENT FILTERING")
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.AudioFile,
                                title = "Filter Short Audio (< 30s)",
                                subtitle = "Exclude ringtones, notification clips, and voice notes",
                                checked = filterShortTracks,
                                onCheckedChange = onFilterShortTracksChange,
                                testTag = "settings_toggle_filter_short"
                            )
                        }
                    }

                    SettingsCategory.APPEARANCE -> {
                        item {
                            SubpageSectionTitle("VESPER THEME PALETTE")
                        }
                        item {
                            BoxyThemeIdentityCard(
                                selectedTheme = vesperThemeMode,
                                onSelectTheme = onVesperThemeModeChange
                            )
                        }
                        item {
                            SubpageSectionTitle("PLAYER & MOTION")
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.Palette,
                                title = "Full-Screen Cover Art in Player",
                                subtitle = "Expand album artwork framing in the Nocturne full player",
                                checked = fullScreenCoverArt,
                                onCheckedChange = onFullScreenCoverArtChange,
                                testTag = "settings_toggle_fullscreen_cover"
                            )
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.Speed,
                                title = "Reduce Dynamic Blur",
                                subtitle = "Use clean dark gradient backdrop instead of heavy artwork blur",
                                checked = reduceDynamicBlur,
                                onCheckedChange = onReduceDynamicBlurChange,
                                testTag = "settings_toggle_reduce_blur"
                            )
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.Speed,
                                title = "Reduce Animations",
                                subtitle = "Minimize decorative motion for maximum responsiveness",
                                checked = reduceAnimations,
                                onCheckedChange = onReduceAnimationsChange,
                                testTag = "settings_toggle_reduce_animations"
                            )
                        }
                        item {
                            SubpageSectionTitle("LYRICS & SYSTEM MEDIA")
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.AudioFile,
                                title = "Synced Lyrics Display",
                                subtitle = "Enable time-aligned lyrics view in the Nocturne player",
                                checked = syncedLyricsEnabled,
                                onCheckedChange = onSyncedLyricsChange,
                                testTag = "settings_toggle_synced_lyrics"
                            )
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.AudioFile,
                                title = "Soft-Focus Unfocused Lyrics",
                                subtitle = "Dim non-active lyric lines to highlight the current vocal line",
                                checked = blurUnfocusedLyrics,
                                onCheckedChange = onBlurUnfocusedLyricsChange,
                                testTag = "settings_toggle_blur_lyrics"
                            )
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.Palette,
                                title = "Lockscreen & Notification Artwork",
                                subtitle = "Expose album cover metadata to Android MediaSession",
                                checked = lockscreenArtwork,
                                onCheckedChange = onLockscreenArtworkChange,
                                testTag = "settings_toggle_lockscreen_art"
                            )
                        }
                    }

                    SettingsCategory.PERFORMANCE -> {
                        item {
                            SubpageSectionTitle("INDEXING & STARTUP")
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.Refresh,
                                title = "Auto-Scan on Startup",
                                subtitle = "Run fast differential MediaStore sync when VESPER launches",
                                checked = autoScanStartup,
                                onCheckedChange = onAutoScanStartupChange,
                                testTag = "settings_toggle_autoscan"
                            )
                        }
                        item {
                            SettingsActionRow(
                                icon = Icons.Default.Refresh,
                                title = "Rebuild Library Cache & Artwork",
                                subtitle = "Clear cached covers and re-index all local tracks while keeping your edits",
                                onClick = {
                                    onRebuildLibraryCache()
                                    cacheBytes = VesperArtworkResolver.getArtworkCacheSizeBytes(context)
                                },
                                testTag = "settings_rebuild_cache_button"
                            )
                        }
                        item {
                            SubpageSectionTitle("CACHES")
                        }
                        item {
                            SettingsActionRow(
                                icon = Icons.Default.DeleteSweep,
                                title = "Clear Artwork Thumbnail Cache",
                                subtitle = "Currently using ${VesperArtworkResolver.formatBytes(cacheBytes)} on disk",
                                onClick = { showClearCacheConfirm = true },
                                testTag = "settings_clear_cache_row"
                            )
                        }
                        item {
                            SettingsActionRow(
                                icon = Icons.Default.CleaningServices,
                                title = "Clear Discover Metadata Cache",
                                subtitle = "$discoverCacheCount remote track entries cached in Room (30-min TTL)",
                                onClick = onClearDiscoverCache,
                                testTag = "settings_clear_discover_cache_row"
                            )
                        }
                    }

                    SettingsCategory.LOCAL_MUSIC -> {
                        item {
                            SubpageSectionTitle("DEVICE FOLDERS & SCANNER")
                        }
                        item {
                            SettingsActionRow(
                                icon = Icons.Default.FolderOpen,
                                title = "Manage Music Folders & Rescan",
                                subtitle = "${tracks.size} indexed tracks • ${scannedFolders.size} SAF folders connected",
                                onClick = onNavigateToScanMedia,
                                testTag = "settings_scan_media_button"
                            )
                        }
                        item {
                            SettingsToggleRow(
                                icon = Icons.Default.AudioFile,
                                title = "Filter Short Audio (< 30s)",
                                subtitle = "Skip short clips and ringtones during library scan",
                                checked = filterShortTracks,
                                onCheckedChange = onFilterShortTracksChange,
                                testTag = "settings_toggle_filter_short"
                            )
                        }
                        item {
                            SubpageSectionTitle("BUILT-IN TEST AUDIO")
                        }
                        item {
                            SettingsActionRow(
                                icon = Icons.Default.LibraryMusic,
                                title = "Load Atmospheric Demos",
                                subtitle = "Generate 4 playable offline ambient tracks for testing",
                                onClick = onLoadDemosClick,
                                testTag = "settings_load_demos_button"
                            )
                        }
                    }

                    SettingsCategory.STORAGE -> {
                        val totalTrackBytes = tracks.sumOf { it.sizeBytes.coerceAtLeast(0L) }
                        item {
                            SubpageSectionTitle("MEASURED FOOTPRINT")
                        }
                        item {
                            SettingsInfoRow(
                                icon = Icons.Default.LibraryMusic,
                                title = "Indexed Local Audio (${tracks.size} tracks)",
                                value = VesperArtworkResolver.formatBytes(totalTrackBytes)
                            )
                        }
                        item {
                            SettingsInfoRow(
                                icon = Icons.Default.Storage,
                                title = "Artwork Thumbnail Cache",
                                value = VesperArtworkResolver.formatBytes(cacheBytes)
                            )
                        }
                        item {
                            SettingsInfoRow(
                                icon = Icons.Default.CloudQueue,
                                title = "Discover Metadata Cache",
                                value = "$discoverCacheCount / 300 entries"
                            )
                        }
                        item {
                            SubpageSectionTitle("STORAGE MAINTENANCE")
                        }
                        item {
                            SettingsActionRow(
                                icon = Icons.Default.DeleteSweep,
                                title = "Clear Artwork Cache",
                                subtitle = "Free ${VesperArtworkResolver.formatBytes(cacheBytes)} of extracted cover thumbnails",
                                onClick = { showClearCacheConfirm = true },
                                testTag = "settings_clear_cache_row"
                            )
                        }
                        item {
                            SettingsActionRow(
                                icon = Icons.Default.CleaningServices,
                                title = "Purge Discover Cache",
                                subtitle = "Remove all $discoverCacheCount cached online discovery rows",
                                onClick = onClearDiscoverCache,
                                testTag = "settings_storage_clear_discover_button"
                            )
                        }
                    }

                    SettingsCategory.YOUR_DATA -> {
                        item {
                            SubpageSectionTitle("PRIVACY GUARANTEE")
                        }
                        item {
                            BoxyInfoCard(
                                icon = Icons.Default.Security,
                                title = "100% On-Device Library & Preferences",
                                subtitle = "VESPER requires no account, embeds no ad SDKs, and never uploads your local library or listening habits.",
                                badgeText = "PRIVATE"
                            )
                        }
                        item {
                            SubpageSectionTitle("DATA CONTROLS")
                        }
                        item {
                            SettingsActionRow(
                                icon = Icons.Default.CleaningServices,
                                title = "Clear Discover Metadata Cache",
                                subtitle = "Remove cached online search and trending results ($discoverCacheCount items)",
                                onClick = onClearDiscoverCache,
                                testTag = "settings_data_clear_discover_button"
                            )
                        }
                        item {
                            SettingsActionRow(
                                icon = Icons.Default.DeleteSweep,
                                title = "Clear Artwork Cache",
                                subtitle = "Remove extracted local artwork thumbnails (${VesperArtworkResolver.formatBytes(cacheBytes)})",
                                onClick = { showClearCacheConfirm = true },
                                testTag = "settings_clear_cache_row"
                            )
                        }
                        item {
                            SettingsActionRow(
                                icon = Icons.Default.RestartAlt,
                                title = "Reset Preferences to Defaults",
                                subtitle = "Restore default playback, sorting, and scanner settings",
                                onClick = { showResetConfirm = true },
                                testTag = "settings_reset_defaults_button"
                            )
                        }
                    }

                    SettingsCategory.MISCELLANEOUS -> {
                        item {
                            SubpageSectionTitle("ABOUT VESPER")
                        }
                        item {
                            SettingsActionRow(
                                icon = Icons.Default.Info,
                                title = "About VESPER Audio",
                                subtitle = "Architecture, privacy philosophy & open-source licences",
                                onClick = onNavigateToAbout,
                                testTag = "settings_about_button"
                            )
                        }
                        item {
                            SettingsInfoRow(
                                icon = Icons.Default.Check,
                                title = "Build Version",
                                value = "v${com.vesper.audio.BuildConfig.VERSION_NAME} (${com.vesper.audio.BuildConfig.VERSION_CODE} • NOCTURNE)"
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
private fun BoxySettingsCategoryCard(
    category: SettingsCategory,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = VesperSurfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VesperOutline, RoundedCornerShape(12.dp))
            .testTag(category.testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VesperDeepBurgundy),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = VesperTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    style = VesperTypography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = VesperTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = category.description,
                    style = VesperTypography.bodySmall.copy(color = VesperTextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = VesperTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SubpageSectionTitle(title: String) {
    val dynamicAccent = LocalVesperAccentColor.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(dynamicAccent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = VesperTypography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = dynamicAccent,
                letterSpacing = 1.2.sp
            )
        )
    }
}

@Composable
private fun QualityOptionSelectorCard(
    title: String,
    selectedQuality: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    testTagPrefix: String
) {
    val dynamicAccent = LocalVesperAccentColor.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = VesperSurfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VesperOutline, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = VesperTypography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = VesperTextPrimary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { option ->
                    val isSelected = selectedQuality == option
                    Surface(
                        onClick = { onSelect(option) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) VesperDeepBurgundy else VesperBackgroundSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (isSelected) {
                                    Modifier.border(1.dp, dynamicAccent, RoundedCornerShape(8.dp))
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("${testTagPrefix}_${option.substringBefore(" ").lowercase()}")
                    ) {
                        Text(
                            text = option.substringBefore(" ("),
                            style = VesperTypography.labelSmall.copy(
                                color = if (isSelected) VesperTextPrimary else VesperTextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxyInfoCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = VesperSurfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VesperOutline, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VesperDeepBurgundy),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VesperTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = VesperTypography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = VesperTextPrimary
                        ),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = VesperDeepBurgundy
                    ) {
                        Text(
                            text = badgeText,
                            style = VesperTypography.labelSmall.copy(
                                color = VesperTextPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                )
            }
        }
    }
}

@Composable
private fun BoxyThemeIdentityCard(
    selectedTheme: String = "Nocturne",
    onSelectTheme: (String) -> Unit = {}
) {
    val themes = listOf(
        Triple("Nocturne", "True AMOLED Black, Graphite & Wine (#07080B • #8E344F)", "settings_theme_nocturne"),
        Triple("Cosmic Latte", "Warm Charcoal, Cream & Muted Burgundy (#100E12 • #C56D88)", "settings_theme_cosmic_latte"),
        Triple("Pure AMOLED", "Pitch Black with Restrained Elevated Surfaces (#000000 • #A3395B)", "settings_theme_pure_amoled")
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        themes.forEach { (themeName, subtitle, tag) ->
            val isSelected = selectedTheme.equals(themeName, ignoreCase = true)
            Surface(
                onClick = { onSelectTheme(themeName) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) VesperDeepBurgundy else VesperSurfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isSelected) VesperBurgundyHighlight else VesperOutline,
                        RoundedCornerShape(12.dp)
                    )
                    .testTag(tag)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = themeName,
                            style = VesperTypography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = VesperTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active Theme",
                            tint = VesperBurgundyHighlight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortModeOptionRow(
    mode: SortMode,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val dynamicAccent = LocalVesperAccentColor.current
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) VesperDeepBurgundy else VesperSurfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isSelected) dynamicAccent else VesperOutline,
                RoundedCornerShape(12.dp)
            )
            .testTag("settings_sort_mode_${mode.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mode.displayName,
                style = VesperTypography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = VesperTextPrimary
                )
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = VesperBurgundyHighlight,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CrossfadeSliderCard(
    durationSec: Int,
    onDurationChange: (Int) -> Unit
) {
    val dynamicAccent = LocalVesperAccentColor.current
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
                        text = "Crossfade Duration",
                        style = VesperTypography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = VesperTextPrimary
                        )
                    )
                    Text(
                        text = if (durationSec == 0) "Off (Instant track transition)" else "Smooth ${durationSec}s overlap between songs",
                        style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = VesperDeepBurgundy
                ) {
                    Text(
                        text = if (durationSec == 0) "OFF" else "${durationSec}s",
                        style = VesperTypography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = VesperTextPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = durationSec.toFloat(),
                onValueChange = { onDurationChange(it.roundToInt()) },
                valueRange = 0f..12f,
                steps = 11,
                colors = SliderDefaults.colors(
                    thumbColor = VesperTextPrimary,
                    activeTrackColor = dynamicAccent,
                    inactiveTrackColor = VesperBackgroundSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_crossfade_slider")
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val dynamicAccent = LocalVesperAccentColor.current
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(10.dp),
        color = VesperSurfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VesperOutline, RoundedCornerShape(10.dp))
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VesperDeepBurgundy),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VesperTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = VesperTypography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = VesperTextPrimary
                    )
                )
                Text(
                    text = subtitle,
                    style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = VesperTextPrimary,
                    checkedTrackColor = dynamicAccent,
                    uncheckedThumbColor = VesperTextSecondary,
                    uncheckedTrackColor = VesperBackgroundSecondary,
                    uncheckedBorderColor = VesperOutline
                )
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = VesperSurfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VesperOutline, RoundedCornerShape(10.dp))
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VesperDeepBurgundy),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VesperTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = VesperTypography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = VesperTextPrimary
                    )
                )
                Text(
                    text = subtitle,
                    style = VesperTypography.bodySmall.copy(color = VesperTextSecondary)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = VesperTextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = VesperSurfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VesperOutline, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(VesperBackgroundSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = VesperTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = title,
                    style = VesperTypography.bodyLarge.copy(color = VesperTextPrimary),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = value,
                style = VesperTypography.bodyMedium.copy(
                    color = VesperBurgundyHighlight,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
