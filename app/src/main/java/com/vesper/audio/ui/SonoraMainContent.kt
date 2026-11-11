package com.vesper.audio.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vesper.audio.ui.components.AddToPlaylistDialog
import com.vesper.audio.ui.components.ConfirmDeleteDialog
import com.vesper.audio.ui.components.CreatePlaylistDialog
import com.vesper.audio.ui.components.LoadingState
import com.vesper.audio.ui.components.MiniPlayer
import com.vesper.audio.ui.components.RenameTrackDialog
import com.vesper.audio.ui.components.SleepTimerBottomSheet
import com.vesper.audio.ui.components.TrackActionBottomSheet
import com.vesper.audio.ui.components.TrackInfoDialog
import com.vesper.audio.ui.components.VesperLogo
import com.vesper.audio.ui.screens.AboutScreen
import com.vesper.audio.ui.screens.AlbumDetailsScreen
import com.vesper.audio.ui.screens.AlbumsScreen
import com.vesper.audio.ui.screens.ArtistDetailsScreen
import com.vesper.audio.ui.screens.ArtistsScreen
import com.vesper.audio.ui.screens.AudioTuningScreen
import com.vesper.audio.ui.screens.AudioTuningTab
import com.vesper.audio.ui.screens.FavoritesScreen
import com.vesper.audio.ui.screens.HomeScreen
import com.vesper.audio.ui.screens.MetadataStudioScreen
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import com.vesper.audio.ui.screens.NowPlayingScreen
import com.vesper.audio.ui.screens.PlaylistDetailsScreen
import com.vesper.audio.ui.screens.PlaylistsScreen
import com.vesper.audio.ui.screens.QueueScreen
import com.vesper.audio.ui.screens.ScanMediaScreen
import com.vesper.audio.ui.screens.SearchScreen
import com.vesper.audio.ui.screens.SettingsScreen
import com.vesper.audio.ui.screens.SongsScreen
import com.vesper.audio.ui.screens.SupportVesperScreen
import com.vesper.audio.ui.screens.TonesScreen
import com.vesper.audio.ui.theme.LocalVesperAccentColor
import com.vesper.audio.ui.theme.VesperBackground
import com.vesper.audio.ui.theme.VesperBackgroundSecondary
import com.vesper.audio.ui.theme.VesperDeepBurgundy
import com.vesper.audio.ui.theme.VesperOutline
import com.vesper.audio.ui.theme.VesperTextMuted
import com.vesper.audio.ui.theme.VesperTextPrimary
import com.vesper.audio.ui.theme.VesperTextSecondary
import com.vesper.audio.ui.theme.VesperTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SonoraMainContent(
    viewModel: SonoraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var isSplashVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (isSplashVisible && (System.currentTimeMillis() - startTime) < 600) {
            if (viewModel.tracks.value.isNotEmpty()) {
                break
            }
            delay(40)
        }
        isSplashVisible = false
    }

    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val fullScreenDest by viewModel.fullScreenDestination.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val sortedTracks by viewModel.sortedTracks.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val recentlyAddedTracks by viewModel.recentlyAddedTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val scannedFolders by viewModel.scannedFolders.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val discoverSearchQuery by viewModel.discoverSearchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val audiusSearchResults by viewModel.audiusSearchResults.collectAsStateWithLifecycle()
    val audiusTrendingTracks by viewModel.audiusTrendingTracks.collectAsStateWithLifecycle()
    val isSearchingAudius by viewModel.isSearchingAudius.collectAsStateWithLifecycle()
    val isLoadingMoreAudius by viewModel.isLoadingMoreAudius.collectAsStateWithLifecycle()
    val hasMoreAudius by viewModel.hasMoreAudius.collectAsStateWithLifecycle()
    val audiusError by viewModel.audiusError.collectAsStateWithLifecycle()
    val isDiscoverFromCache by viewModel.isDiscoverFromCache.collectAsStateWithLifecycle()
    val isDiscoverStale by viewModel.isDiscoverStale.collectAsStateWithLifecycle()
    val discoverCacheCount by viewModel.discoverCacheCount.collectAsStateWithLifecycle()
    val selectedDiscoverProviderId by viewModel.selectedDiscoverProviderId.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // Playback States
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackPositionMs by viewModel.playbackPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val shuffleMode by viewModel.shuffleMode.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val queue by viewModel.playbackQueue.collectAsStateWithLifecycle()

    // Audio Effects & EQ States
    val eqEnabled by viewModel.eqEnabled.collectAsStateWithLifecycle()
    val eqBands by viewModel.eqBands.collectAsStateWithLifecycle()
    val eqBandRange by viewModel.eqBandRange.collectAsStateWithLifecycle()
    val activePreset by viewModel.activePreset.collectAsStateWithLifecycle()
    val bassBoostStrength by viewModel.bassBoostStrength.collectAsStateWithLifecycle()
    val activeSoundProfile by viewModel.activeSoundProfile.collectAsStateWithLifecycle()
    val preampHeadroomEnabled by viewModel.preampHeadroomEnabled.collectAsStateWithLifecycle()
    val audioBalance by viewModel.audioBalance.collectAsStateWithLifecycle()
    val customEqPresets by viewModel.customEqPresets.collectAsStateWithLifecycle()

    val remoteTracksForDetails = remember(audiusTrendingTracks, audiusSearchResults) {
        (audiusTrendingTracks + audiusSearchResults)
            .distinctBy { it.id }
            .map { it.toTrack() }
    }
    val remoteAlbumsForDetails = remember(audiusTrendingTracks, audiusSearchResults) {
        com.vesper.audio.data.remote.AudiusApiClient.deriveAlbumsFromTracks(
            (audiusTrendingTracks + audiusSearchResults).distinctBy { it.id }
        )
    }

    val favoriteTrackIds by viewModel.favoriteTrackIds.collectAsStateWithLifecycle()
    val queueSource by viewModel.queueSource.collectAsStateWithLifecycle()
    val isSupporter by viewModel.isSupporter.collectAsStateWithLifecycle()

    val dynamicAccent = LocalVesperAccentColor.current
    val trackForMetadata by viewModel.trackForMetadata.collectAsStateWithLifecycle()
    val selectedAlbum by viewModel.selectedAlbum.collectAsStateWithLifecycle()
    val selectedArtist by viewModel.selectedArtist.collectAsStateWithLifecycle()
    val isRemoteDetailMode by viewModel.isRemoteDetailMode.collectAsStateWithLifecycle()
    val remoteDetailTracks by viewModel.remoteDetailTracks.collectAsStateWithLifecycle()
    val remoteDetailAlbums by viewModel.remoteDetailAlbums.collectAsStateWithLifecycle()
    val isLoadingRemoteDetails by viewModel.isLoadingRemoteDetails.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val playlistTracks by viewModel.playlistTracks.collectAsStateWithLifecycle()

    val trackForPlaylist by viewModel.trackForPlaylist.collectAsStateWithLifecycle()
    val showCreatePlaylistDialog by viewModel.showCreatePlaylistDialog.collectAsStateWithLifecycle()

    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedTrackIds by viewModel.selectedTrackIds.collectAsStateWithLifecycle()
    val trackForActionSheet by viewModel.trackForActionSheet.collectAsStateWithLifecycle()
    val trackForRename by viewModel.trackForRename.collectAsStateWithLifecycle()
    val trackForDetails by viewModel.trackForDetails.collectAsStateWithLifecycle()
    val tracksForDeleteConfirm by viewModel.tracksForDeleteConfirm.collectAsStateWithLifecycle()

    val crossfadeDurationSec by viewModel.crossfadeDurationSec.collectAsStateWithLifecycle()
    val gaplessPlayback by viewModel.gaplessPlayback.collectAsStateWithLifecycle()
    val pauseOnHeadphoneDisconnect by viewModel.pauseOnHeadphoneDisconnect.collectAsStateWithLifecycle()
    val resumeOnLaunch by viewModel.resumeOnLaunch.collectAsStateWithLifecycle()
    val filterShortTracks by viewModel.filterShortTracks.collectAsStateWithLifecycle()
    val autoScanStartup by viewModel.autoScanStartup.collectAsStateWithLifecycle()
    val lockscreenArtwork by viewModel.lockscreenArtwork.collectAsStateWithLifecycle()
    val vesperThemeMode by viewModel.vesperThemeMode.collectAsStateWithLifecycle()
    val directSourceStreamEnabled by viewModel.directSourceStreamEnabled.collectAsStateWithLifecycle()
    val wifiStreamingQuality by viewModel.wifiStreamingQuality.collectAsStateWithLifecycle()
    val mobileStreamingQuality by viewModel.mobileStreamingQuality.collectAsStateWithLifecycle()
    val wifiOnlyDownloads by viewModel.wifiOnlyDownloads.collectAsStateWithLifecycle()
    val disableEffectsOnSpeaker by viewModel.disableEffectsOnSpeaker.collectAsStateWithLifecycle()
    val loudnessNormalizationEnabled by viewModel.loudnessNormalizationEnabled.collectAsStateWithLifecycle()
    val hideRecentsOnHome by viewModel.hideRecentsOnHome.collectAsStateWithLifecycle()
    val hideMoodPicksOnHome by viewModel.hideMoodPicksOnHome.collectAsStateWithLifecycle()
    val hideTopicPicksOnHome by viewModel.hideTopicPicksOnHome.collectAsStateWithLifecycle()
    val reduceAnimations by viewModel.reduceAnimations.collectAsStateWithLifecycle()
    val reduceDynamicBlur by viewModel.reduceDynamicBlur.collectAsStateWithLifecycle()
    val fullScreenCoverArt by viewModel.fullScreenCoverArt.collectAsStateWithLifecycle()
    val syncedLyricsEnabled by viewModel.syncedLyricsEnabled.collectAsStateWithLifecycle()
    val blurUnfocusedLyrics by viewModel.blurUnfocusedLyrics.collectAsStateWithLifecycle()
    val showSleepTimerDialog by viewModel.showSleepTimerDialog.collectAsStateWithLifecycle()
    val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()

    // SAF Folder Picker launcher
    val safPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    takeFlags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                )
            } catch (_: Exception) {
            }
            viewModel.importSafFolder(uri)
        }
    }

    // Audio storage permission launcher
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.syncLibraryIncremental(force = false, reason = "permission_granted")
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            permissionLauncher.launch(audioPermission)
        }
    }

    // Back button routing
    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    BackHandler(enabled = !drawerState.isOpen && fullScreenDest != VesperFullScreenDestination.NONE) {
        viewModel.navigateBack()
    }

    BackHandler(enabled = !drawerState.isOpen && fullScreenDest == VesperFullScreenDestination.NONE && currentDestination != MainDestination.HOME) {
        viewModel.setDestination(MainDestination.HOME)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = VesperBackgroundSecondary,
                drawerContentColor = VesperTextPrimary,
                modifier = Modifier
                    .width(300.dp)
                    .border(width = 1.dp, color = VesperOutline, shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                    .testTag("vesper_nav_drawer")
            ) {
                // Drawer Header: VESPER wordmark, NOCTURNE subtitle, and version pill
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(VesperDeepBurgundy)
                            .border(1.5.dp, dynamicAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        VesperLogo(
                            size = 36.dp,
                            animated = false
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "VESPER",
                            style = VesperTypography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = VesperTextPrimary,
                                letterSpacing = 1.5.sp
                            )
                        )
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VesperDeepBurgundy,
                            modifier = Modifier.border(1.dp, dynamicAccent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "v3.0.0",
                                style = VesperTypography.labelSmall.copy(
                                    color = VesperTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "NOCTURNE • Cosmic Minimalism Audio",
                        style = VesperTypography.bodySmall.copy(color = VesperTextMuted)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(VesperOutline)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 1. About
                NavigationDrawerItem(
                    label = { Text("About", style = VesperTypography.labelLarge) },
                    icon = { Icon(Icons.Default.Info, contentDescription = null, tint = VesperTextPrimary) },
                    selected = fullScreenDest == VesperFullScreenDestination.ABOUT,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.navigateTo(VesperFullScreenDestination.ABOUT)
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = dynamicAccent,
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = VesperTextPrimary,
                        unselectedTextColor = VesperTextSecondary
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).testTag("drawer_item_about")
                )

                // 2. Settings
                NavigationDrawerItem(
                    label = { Text("Settings", style = VesperTypography.labelLarge) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = VesperTextPrimary) },
                    selected = fullScreenDest == VesperFullScreenDestination.SETTINGS,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.navigateTo(VesperFullScreenDestination.SETTINGS)
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = dynamicAccent,
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = VesperTextPrimary,
                        unselectedTextColor = VesperTextSecondary
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).testTag("drawer_item_settings")
                )

                // 3. Equalizer
                NavigationDrawerItem(
                    label = { Text("Equalizer", style = VesperTypography.labelLarge) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, tint = VesperTextPrimary) },
                    selected = fullScreenDest == VesperFullScreenDestination.EQUALIZER,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.navigateTo(VesperFullScreenDestination.EQUALIZER)
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = dynamicAccent,
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = VesperTextPrimary,
                        unselectedTextColor = VesperTextSecondary
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).testTag("drawer_item_equalizer")
                )

                // 4. Tones
                NavigationDrawerItem(
                    label = { Text("Tones", style = VesperTypography.labelLarge) },
                    icon = { Icon(Icons.Default.Album, contentDescription = null, tint = VesperTextPrimary) },
                    selected = fullScreenDest == VesperFullScreenDestination.TONES,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.navigateTo(VesperFullScreenDestination.TONES)
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = dynamicAccent,
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = VesperTextPrimary,
                        unselectedTextColor = VesperTextSecondary
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).testTag("drawer_item_tones")
                )

                // 5. Scan media
                NavigationDrawerItem(
                    label = { Text("Scan media", style = VesperTypography.labelLarge) },
                    icon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = VesperTextPrimary) },
                    selected = fullScreenDest == VesperFullScreenDestination.SCAN_MEDIA,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.navigateTo(VesperFullScreenDestination.SCAN_MEDIA)
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = dynamicAccent,
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = VesperTextPrimary,
                        unselectedTextColor = VesperTextSecondary
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).testTag("drawer_item_scan_media")
                )
            }
        }
    ) {
        Box(modifier = modifier.fillMaxSize().background(VesperBackground)) {

            Scaffold(
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    Column {
                        // Persistent MiniPlayer docked directly above NavigationBar
                        MiniPlayer(
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            playbackPositionMs = playbackPositionMs,
                            durationMs = durationMs,
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onSkipNextClick = { viewModel.skipNext() },
                            onSkipPreviousClick = { viewModel.skipPrevious() },
                            onExpandClick = { viewModel.navigateTo(VesperFullScreenDestination.NOW_PLAYING) }
                        )

                        // Main Music Navigation: Home -> Songs -> Albums -> Playlists -> Discover
                        val navTargetIndex = when (currentDestination) {
                            MainDestination.HOME -> 0
                            MainDestination.SONGS -> 1
                            MainDestination.ALBUMS -> 2
                            MainDestination.PLAYLISTS -> 3
                            MainDestination.DISCOVER -> 4
                        }
                        val animatedNavIndex by animateFloatAsState(
                            targetValue = navTargetIndex.toFloat(),
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                            label = "nav_glow_trail"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(width = 1.dp, color = VesperOutline, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .background(VesperBackgroundSecondary)
                        ) {
                            NavigationBar(
                                containerColor = Color.Transparent,
                                tonalElevation = 8.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                    .testTag("vesper_bottom_nav")
                            ) {
                                // 1. Home
                                NavigationBarItem(
                                    selected = currentDestination == MainDestination.HOME,
                                    onClick = { viewModel.setDestination(MainDestination.HOME) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = "Home",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("Home", style = VesperTypography.labelMedium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = VesperTextPrimary,
                                        selectedTextColor = VesperTextPrimary,
                                        indicatorColor = dynamicAccent,
                                        unselectedIconColor = VesperTextMuted,
                                        unselectedTextColor = VesperTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_item_home")
                                )

                                // 2. Songs
                                NavigationBarItem(
                                    selected = currentDestination == MainDestination.SONGS,
                                    onClick = { viewModel.setDestination(MainDestination.SONGS) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.LibraryMusic,
                                            contentDescription = "Songs",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("Songs", style = VesperTypography.labelMedium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = VesperTextPrimary,
                                        selectedTextColor = VesperTextPrimary,
                                        indicatorColor = dynamicAccent,
                                        unselectedIconColor = VesperTextMuted,
                                        unselectedTextColor = VesperTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_item_songs")
                                )

                                // 3. Albums
                                NavigationBarItem(
                                    selected = currentDestination == MainDestination.ALBUMS,
                                    onClick = { viewModel.setDestination(MainDestination.ALBUMS) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Album,
                                            contentDescription = "Albums",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("Albums", style = VesperTypography.labelMedium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = VesperTextPrimary,
                                        selectedTextColor = VesperTextPrimary,
                                        indicatorColor = dynamicAccent,
                                        unselectedIconColor = VesperTextMuted,
                                        unselectedTextColor = VesperTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_item_albums")
                                )

                                // 4. Playlists (Replaces Artists in main navigation)
                                NavigationBarItem(
                                    selected = currentDestination == MainDestination.PLAYLISTS,
                                    onClick = { viewModel.setDestination(MainDestination.PLAYLISTS) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                            contentDescription = "Playlists",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("Playlists", style = VesperTypography.labelMedium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = VesperTextPrimary,
                                        selectedTextColor = VesperTextPrimary,
                                        indicatorColor = dynamicAccent,
                                        unselectedIconColor = VesperTextMuted,
                                        unselectedTextColor = VesperTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_item_playlists")
                                )

                                // 5. Discover
                                NavigationBarItem(
                                    selected = currentDestination == MainDestination.DISCOVER,
                                    onClick = { viewModel.setDestination(MainDestination.DISCOVER) },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Discover",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("Discover", style = VesperTypography.labelMedium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = VesperTextPrimary,
                                        selectedTextColor = VesperTextPrimary,
                                        indicatorColor = dynamicAccent,
                                        unselectedIconColor = VesperTextMuted,
                                        unselectedTextColor = VesperTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_item_search")
                                )
                            }

                            // Minimal animated glowing trail line following active tab
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopStart)
                                    .height(3.dp)
                            ) {
                                val itemWidth = maxWidth / 5
                                val pillWidth = 36.dp
                                val pillOffset = (itemWidth * animatedNavIndex) + (itemWidth - pillWidth) / 2
                                Box(
                                    modifier = Modifier
                                        .offset(x = pillOffset)
                                        .width(pillWidth)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(dynamicAccent)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .padding(innerPadding)
                ) {
                    if (isLoading && tracks.isEmpty()) {
                        LoadingState(message = "Indexing audio library...")
                    } else {
                        val pagerState = rememberPagerState(
                            initialPage = MainDestination.entries.indexOf(currentDestination).coerceIn(0, 4),
                            pageCount = { MainDestination.entries.size }
                        )

                        LaunchedEffect(currentDestination) {
                            val targetPage = MainDestination.entries.indexOf(currentDestination).coerceIn(0, 4)
                            if (pagerState.currentPage != targetPage) {
                                pagerState.animateScrollToPage(targetPage)
                            }
                        }

                        LaunchedEffect(pagerState.settledPage) {
                            val settledDest = MainDestination.entries.getOrElse(pagerState.settledPage) { MainDestination.HOME }
                            if (currentDestination != settledDest) {
                                viewModel.setDestination(settledDest)
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            beyondViewportPageCount = 1,
                            key = { page -> MainDestination.entries.getOrElse(page) { MainDestination.HOME }.name },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("main_horizontal_pager")
                        ) { page ->
                            val pageDestination = MainDestination.entries.getOrElse(page) { MainDestination.HOME }
                            when (pageDestination) {
                                MainDestination.HOME -> HomeScreen(
                                    tracks = tracks,
                                    albums = albums,
                                    favoriteTracks = favoriteTracks,
                                    recentlyAddedTracks = recentlyAddedTracks,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    shuffleMode = shuffleMode,
                                    favoriteTrackIds = favoriteTrackIds,
                                    onTrackClick = { viewModel.playTrack(it, recentlyAddedTracks.ifEmpty { tracks }, "Recently Added") },
                                    onTrackClickWithQueue = { clickedTrack, activeQueue, source ->
                                        viewModel.playTrack(clickedTrack, activeQueue, source)
                                    },
                                    onContinueListeningClick = { viewModel.setShowNowPlaying(true) },
                                    onAlbumClick = { viewModel.openAlbumDetails(it) },
                                    onPlayAlbumDirectly = { viewModel.playAlbum(it) },
                                    onSeeAllRecentlyAdded = {
                                        viewModel.setSortMode(SortMode.RECENTLY_ADDED)
                                        viewModel.setDestination(MainDestination.SONGS)
                                    },
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                    onMoreTrackClick = { viewModel.openTrackActionSheet(it) },
                                    onShuffleAllClick = { viewModel.shuffleAll() },
                                    onToggleShuffle = { viewModel.toggleShuffle() },
                                    onTogglePlayPause = { viewModel.togglePlayPause() },
                                    onSkipPrevious = { viewModel.skipPrevious() },
                                    onSkipNext = { viewModel.skipNext() },
                                    onScanRequest = { viewModel.navigateTo(VesperFullScreenDestination.SCAN_MEDIA) },
                                    onOpenSearch = { viewModel.setDestination(MainDestination.DISCOVER) },
                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                    onOpenFavorites = { viewModel.openFavorites() },
                                    onLogoClick = { viewModel.setDestination(MainDestination.HOME) },
                                    hideRecentsOnHome = hideRecentsOnHome,
                                    hideMoodPicksOnHome = hideMoodPicksOnHome,
                                    hideTopicPicksOnHome = hideTopicPicksOnHome
                                )

                                MainDestination.SONGS -> SongsScreen(
                                    tracks = sortedTracks,
                                    playlists = playlists,
                                    sortMode = sortMode,
                                    onSortSelected = { viewModel.setSortMode(it) },
                                    onTrackClick = { viewModel.playTrack(it, sortedTracks) },
                                    onTrackClickWithQueue = { clickedTrack, activeQueue ->
                                        val src = when {
                                            searchQuery.isNotBlank() -> "Search: \"$searchQuery\""
                                            sortMode == SortMode.RECENTLY_ADDED -> "Recently Added"
                                            else -> "All Songs"
                                        }
                                        viewModel.playTrack(clickedTrack, activeQueue, src)
                                    },
                                    onShuffleAllClick = { viewModel.shuffleAll() },
                                    onPlaylistClick = { playlist -> viewModel.openPlaylistDetails(playlist) },
                                    onCreatePlaylistClick = { viewModel.openCreatePlaylistDialog() },
                                    onPickSafFolderClick = { safPickerLauncher.launch(null) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                    onMoreTrackClick = { viewModel.openTrackActionSheet(it) },
                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                    searchQuery = searchQuery,
                                    onQueryChange = { viewModel.setSearchQuery(it) },
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    favoriteTrackIds = favoriteTrackIds,
                                    onLogoClick = { viewModel.setDestination(MainDestination.HOME) },
                                    isSelectionMode = isSelectionMode,
                                    selectedTrackIds = selectedTrackIds,
                                    onEnterMultiSelect = { viewModel.enterMultiSelect(it) },
                                    onToggleTrackSelection = { viewModel.toggleTrackSelection(it) },
                                    onSelectAll = { viewModel.selectAllTracks(sortedTracks.map { it.id }) },
                                    onClearSelection = { viewModel.clearSelection() },
                                    onBulkAddToQueue = { viewModel.addSelectedToQueue() },
                                    onBulkAddToFavorites = { viewModel.addSelectedToFavorites() },
                                    onBulkAddToPlaylist = {
                                        val selected = sortedTracks.filter { it.id in selectedTrackIds }
                                        if (selected.isNotEmpty()) {
                                            viewModel.openAddToPlaylistDialog(selected.first())
                                        }
                                    },
                                    onBulkDelete = { viewModel.deleteSelectedTracks() }
                                )

                                MainDestination.ALBUMS -> AlbumsScreen(
                                    albums = albums,
                                    onAlbumClick = { viewModel.openAlbumDetails(it) },
                                    onScanRequest = { viewModel.navigateTo(VesperFullScreenDestination.SCAN_MEDIA) },
                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                    onLogoClick = { viewModel.setDestination(MainDestination.HOME) }
                                )

                                MainDestination.PLAYLISTS -> PlaylistsScreen(
                                    playlists = playlists,
                                    allTracks = tracks,
                                    onPlaylistClick = { viewModel.openPlaylistDetails(it) },
                                    onCreatePlaylist = { viewModel.createPlaylist(it) },
                                    onRenamePlaylist = { id, newName -> viewModel.renamePlaylist(id, newName) },
                                    onDeletePlaylist = { id -> viewModel.deletePlaylist(id) },
                                    onPlayPlaylist = { playlist, shuffle -> viewModel.playPlaylist(playlist, shuffle) }
                                )

                                MainDestination.DISCOVER -> SearchScreen(
                                    searchQuery = discoverSearchQuery,
                                    onQueryChange = { viewModel.setDiscoverSearchQuery(it) },
                                    allTracks = tracks,
                                    allAlbums = albums,
                                    allArtists = artists,
                                    audiusTracks = if (directSourceStreamEnabled) audiusSearchResults else emptyList(),
                                    audiusTrendingTracks = if (directSourceStreamEnabled) audiusTrendingTracks else emptyList(),
                                    isSearchingAudius = isSearchingAudius,
                                    isLoadingMoreAudius = isLoadingMoreAudius,
                                    hasMoreAudius = directSourceStreamEnabled && hasMoreAudius,
                                    audiusError = if (!directSourceStreamEnabled) "Online discovery stream is paused in Settings → Audio quality." else audiusError,
                                    isFromCache = isDiscoverFromCache,
                                    isStale = isDiscoverStale,
                                    enabledProviders = viewModel.discoverRepository.enabledProviders,
                                    selectedProviderId = selectedDiscoverProviderId,
                                    onSelectProvider = { viewModel.selectDiscoverProvider(it) },
                                    onArtworkError = { viewModel.discoverRepository.markArtworkUrlBroken(it) },
                                    onLoadTrending = { if (directSourceStreamEnabled) viewModel.loadAudiusTrending() },
                                    onLoadMoreAudius = { if (directSourceStreamEnabled) viewModel.loadMoreAudius() },
                                    onRetryAudius = {
                                        if (!directSourceStreamEnabled) {
                                            viewModel.setDirectSourceStreamEnabled(true)
                                        }
                                        if (discoverSearchQuery.isNotBlank()) {
                                            viewModel.searchAudius(discoverSearchQuery)
                                        } else {
                                            viewModel.loadAudiusTrending(forceRefresh = true)
                                        }
                                    },
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    favoriteTrackIds = favoriteTrackIds,
                                    onTrackClick = { track, list ->
                                        val source = if (discoverSearchQuery.isNotBlank()) "Discover: \"$discoverSearchQuery\"" else "Discover"
                                        viewModel.playTrack(track, list, source)
                                    },
                                    onAlbumClick = { viewModel.openAlbumDetails(it, isRemote = true) },
                                    onArtistClick = { viewModel.openArtistDetails(it, isRemote = true) },
                                    onLocalAlbumClick = { viewModel.openAlbumDetails(it, isRemote = false) },
                                    onLocalArtistClick = { viewModel.openArtistDetails(it, isRemote = false) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                    onMoreTrackClick = { viewModel.openTrackActionSheet(it) },
                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                    onLogoClick = { viewModel.setDestination(MainDestination.HOME) }
                                )
                            }
                        }
                    }

                    // Playlist creation dialog
                    if (showCreatePlaylistDialog) {
                        CreatePlaylistDialog(
                            onDismiss = { viewModel.closeCreatePlaylistDialog() },
                            onConfirm = { name -> viewModel.createPlaylist(name) }
                        )
                    }

                    // Add to playlist selection dialog
                    trackForPlaylist?.let { track ->
                        AddToPlaylistDialog(
                            track = track,
                            playlists = playlists,
                            onDismiss = { viewModel.closeAddToPlaylistDialog() },
                            onPlaylistSelected = { playlist -> viewModel.addTrackToExistingPlaylist(playlist, track) },
                            onCreateNewClick = {
                                viewModel.closeAddToPlaylistDialog()
                                viewModel.openCreatePlaylistDialog()
                            }
                        )
                    }
                }
            }

            // Full-Screen Destination Modals / Overlays
            AnimatedVisibility(
                visible = fullScreenDest == VesperFullScreenDestination.NOW_PLAYING,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                NowPlayingScreen(
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    playbackPositionMs = playbackPositionMs,
                    durationMs = durationMs,
                    shuffleMode = shuffleMode,
                    repeatMode = repeatMode,
                    queueSource = queueSource,
                    favoriteTrackIds = favoriteTrackIds,
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onSkipNextClick = { viewModel.skipNext() },
                    onSkipPreviousClick = { viewModel.skipPrevious() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onCycleRepeat = { viewModel.cycleRepeatMode() },
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    onCollapseClick = { viewModel.navigateBack() },
                    onOpenEqualizerClick = { viewModel.navigateTo(VesperFullScreenDestination.EQUALIZER) },
                    onOpenQueueClick = { viewModel.navigateTo(VesperFullScreenDestination.QUEUE) },
                    onOpenSleepTimerClick = { viewModel.openSleepTimerDialog() },
                    onOpenMoreOptions = { currentTrack?.let { viewModel.openTrackActionSheet(it) } }
                )
            }

            AnimatedVisibility(
                visible = fullScreenDest == VesperFullScreenDestination.QUEUE,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                QueueScreen(
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    queue = queue,
                    favoriteTracks = favoriteTracks,
                    onTrackSelect = { viewModel.playTrack(it, queue.ifEmpty { tracks }) },
                    onRemoveFromQueue = { viewModel.removeQueueItem(it) },
                    onMoveQueueItem = { from, to -> viewModel.moveQueueItem(from, to) },
                    onClearQueue = { viewModel.clearQueue() },
                    onBackClick = { viewModel.navigateBack() },
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    onAddToQueue = { viewModel.addToQueue(it) },
                    onPlayNext = { viewModel.playNext(it) },
                    onAddToPlaylist = { viewModel.openAddToPlaylistDialog(it) },
                    onOpenEditMetadata = { viewModel.openMetadataEditor(it) },
                    onOpenTrackInfo = { viewModel.openTrackInfoDialog(it) },
                    onDeleteTrack = { viewModel.openDeleteConfirmDialog(listOf(it)) }
                )
            }

            AnimatedVisibility(
                visible = fullScreenDest == VesperFullScreenDestination.EQUALIZER || fullScreenDest == VesperFullScreenDestination.MODES,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                AudioTuningScreen(
                    audioEffectController = viewModel.audioEffectController,
                    isEnabled = eqEnabled,
                    bands = eqBands,
                    bandRange = eqBandRange,
                    activePreset = activePreset,
                    bassBoost = bassBoostStrength,
                    activeProfile = activeSoundProfile,
                    customPresets = customEqPresets,
                    preampHeadroomEnabled = preampHeadroomEnabled,
                    audioBalance = audioBalance,
                    onToggleEnable = { viewModel.toggleEqEnabled(it) },
                    onBandLevelChange = { index, level -> viewModel.setEqBandLevel(index, level) },
                    onPresetSelect = { viewModel.selectPreset(it) },
                    onBassBoostChange = { viewModel.setBassBoostStrength(it) },
                    onSaveCustomPreset = { viewModel.saveCustomEqPreset(it) },
                    onRenameCustomPreset = { oldName, newName -> viewModel.renameCustomEqPreset(oldName, newName) },
                    onDuplicateCustomPreset = { viewModel.duplicateCustomEqPreset(it) },
                    onDeleteCustomPreset = { viewModel.deleteCustomEqPreset(it) },
                    onToggleHeadroomProtection = { viewModel.setPreampHeadroomEnabled(it) },
                    onAudioBalanceChange = { viewModel.setAudioBalance(it) },
                    onProfileSelect = { viewModel.selectSoundProfile(it) },
                    onResetToFlat = { viewModel.resetEqToFlat() },
                    initialTab = if (fullScreenDest == VesperFullScreenDestination.MODES) AudioTuningTab.MODES else AudioTuningTab.EQUALIZER,
                    onBackClick = { viewModel.navigateBack() }
                )
            }

            AnimatedVisibility(
                visible = fullScreenDest == VesperFullScreenDestination.TONES,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                TonesScreen(
                    audioEffectController = viewModel.audioEffectController,
                    onOpenBasicEqualizer = { viewModel.navigateTo(VesperFullScreenDestination.EQUALIZER) },
                    onBackClick = { viewModel.navigateBack() }
                )
            }

            AnimatedVisibility(
                visible = fullScreenDest == VesperFullScreenDestination.DONATE,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                SupportVesperScreen(
                    onBack = { viewModel.navigateBack() },
                    isSupporter = isSupporter,
                    onSupporterStatusToggled = { viewModel.setSupporterStatus(it) }
                )
            }

            AnimatedVisibility(
                visible = fullScreenDest == VesperFullScreenDestination.METADATA_STUDIO,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                MetadataStudioScreen(
                    currentTrack = trackForMetadata ?: currentTrack,
                    allTracks = tracks,
                    onSaveMetadata = { title, artist, album, year, trackNumber, genre ->
                        viewModel.updateTrackMetadata(title, artist, album, year, trackNumber, genre)
                    },
                    onBatchSaveMetadata = { ids, artist, album, year -> viewModel.batchUpdateTrackMetadata(ids, artist, album, year) },
                    onSelectTrack = { viewModel.openMetadataEditor(it) },
                    onBackClick = { viewModel.navigateBack() }
                )
            }

            AnimatedVisibility(
                visible = fullScreenDest == VesperFullScreenDestination.SETTINGS,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                SettingsScreen(
                    onNavigateToAudioTuning = { viewModel.navigateTo(VesperFullScreenDestination.EQUALIZER) },
                    onNavigateToScanMedia = { viewModel.navigateTo(VesperFullScreenDestination.SCAN_MEDIA) },
                    onNavigateToDonate = { viewModel.navigateTo(VesperFullScreenDestination.DONATE) },
                    onNavigateToAbout = { viewModel.navigateTo(VesperFullScreenDestination.ABOUT) },
                    onLoadDemosClick = { viewModel.loadDemoTracks() },
                    crossfadeDurationSec = crossfadeDurationSec,
                    onCrossfadeDurationChange = { viewModel.setCrossfade(it > 0, it) },
                    gaplessPlayback = gaplessPlayback,
                    onGaplessPlaybackChange = { viewModel.setGaplessPlayback(it) },
                    pauseOnHeadphoneDisconnect = pauseOnHeadphoneDisconnect,
                    onPauseOnDisconnectChange = { viewModel.setPauseOnHeadphoneDisconnect(it) },
                    resumeOnLaunch = resumeOnLaunch,
                    onResumeOnLaunchChange = { viewModel.setResumeOnLaunch(it) },
                    filterShortTracks = filterShortTracks,
                    onFilterShortTracksChange = { viewModel.setFilterShortTracks(it) },
                    autoScanStartup = autoScanStartup,
                    onAutoScanStartupChange = { viewModel.setAutoScanStartup(it) },
                    preampHeadroomEnabled = preampHeadroomEnabled,
                    onPreampHeadroomChange = { viewModel.setPreampHeadroomEnabled(it) },
                    lockscreenArtwork = lockscreenArtwork,
                    onLockscreenArtworkChange = { viewModel.setLockscreenArtwork(it) },
                    sortMode = sortMode,
                    onSortModeChange = { viewModel.setSortMode(it) },
                    vesperThemeMode = vesperThemeMode,
                    onVesperThemeModeChange = { viewModel.setVesperThemeMode(it) },
                    directSourceStreamEnabled = directSourceStreamEnabled,
                    onDirectSourceStreamChange = { viewModel.setDirectSourceStreamEnabled(it) },
                    wifiStreamingQuality = wifiStreamingQuality,
                    onWifiStreamingQualityChange = { viewModel.setWifiStreamingQuality(it) },
                    mobileStreamingQuality = mobileStreamingQuality,
                    onMobileStreamingQualityChange = { viewModel.setMobileStreamingQuality(it) },
                    wifiOnlyDownloads = wifiOnlyDownloads,
                    onWifiOnlyDownloadsChange = { viewModel.setWifiOnlyDownloads(it) },
                    disableEffectsOnSpeaker = disableEffectsOnSpeaker,
                    onDisableEffectsOnSpeakerChange = { viewModel.setDisableEffectsOnSpeaker(it) },
                    loudnessNormalizationEnabled = loudnessNormalizationEnabled,
                    onLoudnessNormalizationChange = { viewModel.setLoudnessNormalizationEnabled(it) },
                    hideRecentsOnHome = hideRecentsOnHome,
                    onHideRecentsOnHomeChange = { viewModel.setHideRecentsOnHome(it) },
                    hideMoodPicksOnHome = hideMoodPicksOnHome,
                    onHideMoodPicksOnHomeChange = { viewModel.setHideMoodPicksOnHome(it) },
                    hideTopicPicksOnHome = hideTopicPicksOnHome,
                    onHideTopicPicksOnHomeChange = { viewModel.setHideTopicPicksOnHome(it) },
                    reduceAnimations = reduceAnimations,
                    onReduceAnimationsChange = { viewModel.setReduceAnimations(it) },
                    reduceDynamicBlur = reduceDynamicBlur,
                    onReduceDynamicBlurChange = { viewModel.setReduceDynamicBlur(it) },
                    fullScreenCoverArt = fullScreenCoverArt,
                    onFullScreenCoverArtChange = { viewModel.setFullScreenCoverArt(it) },
                    syncedLyricsEnabled = syncedLyricsEnabled,
                    onSyncedLyricsChange = { viewModel.setSyncedLyricsEnabled(it) },
                    blurUnfocusedLyrics = blurUnfocusedLyrics,
                    onBlurUnfocusedLyricsChange = { viewModel.setBlurUnfocusedLyrics(it) },
                    tracks = tracks,
                    scannedFolders = scannedFolders,
                    discoverCacheCount = discoverCacheCount,
                    onClearDiscoverCache = { viewModel.clearDiscoverCache() },
                    onClearArtworkCache = { viewModel.clearArtworkCache() },
                    onOpenSleepTimer = { viewModel.openSleepTimerDialog() },
                    onRebuildLibraryCache = { viewModel.rebuildLibraryCache() },
                    onResetSettingsToDefaults = { viewModel.resetSettingsToDefaults() },
                    onBackClick = { viewModel.navigateBack() }
                )
            }

            AnimatedVisibility(
                visible = fullScreenDest == VesperFullScreenDestination.ABOUT,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                AboutScreen(
                    onBackClick = { viewModel.navigateBack() }
                )
            }

            AnimatedVisibility(
                visible = fullScreenDest == VesperFullScreenDestination.SCAN_MEDIA,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                ScanMediaScreen(
                    isLoading = isLoading,
                    trackCount = tracks.size,
                    albumCount = albums.size,
                    artistCount = artists.size,
                    scannedFolders = scannedFolders,
                    onRescanClick = { viewModel.refreshLibrary() },
                    onAddFolderClick = { safPickerLauncher.launch(null) },
                    onDeleteFolder = { viewModel.deleteScannedFolder(it) },
                    onBackClick = { viewModel.navigateBack() }
                )
            }

            AnimatedVisibility(
                visible = fullScreenDest == VesperFullScreenDestination.ALBUM_DETAILS && selectedAlbum != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                selectedAlbum?.let { album ->
                    val detailTracks = if (isRemoteDetailMode) {
                        remoteDetailTracks.ifEmpty { remoteTracksForDetails }
                    } else {
                        tracks
                    }
                    AlbumDetailsScreen(
                        album = album,
                        allTracks = detailTracks,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        favoriteTrackIds = favoriteTrackIds,
                        isRemote = isRemoteDetailMode,
                        isLoadingRemote = isRemoteDetailMode && isLoadingRemoteDetails,
                        onTrackClick = { track, aQueue -> viewModel.playTrack(track, aQueue, album.name) },
                        onPlayAllClick = { aQueue ->
                            if (aQueue.isNotEmpty()) viewModel.playTrack(aQueue.first(), aQueue, album.name)
                        },
                        onShuffleClick = { aQueue ->
                            val shuffled = aQueue.shuffled()
                            if (shuffled.isNotEmpty()) viewModel.playTrack(shuffled.first(), shuffled, album.name)
                        },
                        onQueueAllClick = { aQueue ->
                            aQueue.forEach { viewModel.addToQueue(it) }
                        },
                        onFavoriteToggle = { viewModel.toggleFavorite(it) },
                        onMoreTrackClick = { viewModel.openTrackActionSheet(it) },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onAddToQueue = { viewModel.addToQueue(it) },
                        onPlayNext = { viewModel.playNext(it) },
                        onBackClick = { viewModel.navigateBack() }
                    )
                }
            }

            AnimatedVisibility(
                visible = fullScreenDest == VesperFullScreenDestination.ARTIST_DETAILS && selectedArtist != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                selectedArtist?.let { artist ->
                    val detailTracks = if (isRemoteDetailMode) {
                        remoteDetailTracks.ifEmpty { remoteTracksForDetails }
                    } else {
                        tracks
                    }
                    val detailAlbums = if (isRemoteDetailMode) {
                        remoteDetailAlbums.ifEmpty { remoteAlbumsForDetails }
                    } else {
                        albums
                    }
                    ArtistDetailsScreen(
                        artist = artist,
                        allAlbums = detailAlbums,
                        allTracks = detailTracks,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        favoriteTrackIds = favoriteTrackIds,
                        isRemote = isRemoteDetailMode,
                        isLoadingRemote = isRemoteDetailMode && isLoadingRemoteDetails,
                        onTrackClick = { track, aQueue -> viewModel.playTrack(track, aQueue, artist.name) },
                        onAlbumClick = { album -> viewModel.openAlbumDetails(album, isRemote = isRemoteDetailMode) },
                        onPlayAllClick = { aQueue ->
                            if (aQueue.isNotEmpty()) viewModel.playTrack(aQueue.first(), aQueue, artist.name)
                        },
                        onShuffleClick = { aQueue ->
                            val shuffled = aQueue.shuffled()
                            if (shuffled.isNotEmpty()) viewModel.playTrack(shuffled.first(), shuffled, artist.name)
                        },
                        onFavoriteToggle = { viewModel.toggleFavorite(it) },
                        onMoreTrackClick = { viewModel.openTrackActionSheet(it) },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onAddToQueue = { viewModel.addToQueue(it) },
                        onPlayNext = { viewModel.playNext(it) },
                        onBackClick = { viewModel.navigateBack() }
                    )
                }
            }

            AnimatedVisibility(
                visible = fullScreenDest == VesperFullScreenDestination.PLAYLIST_DETAILS && selectedPlaylist != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                selectedPlaylist?.let { playlist ->
                    PlaylistDetailsScreen(
                        playlist = playlist,
                        tracks = playlistTracks,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onTrackClick = { track, pQueue -> viewModel.playTrack(track, pQueue, playlist.name) },
                        onPlayAllClick = { pQueue ->
                            if (pQueue.isNotEmpty()) viewModel.playTrack(pQueue.first(), pQueue, playlist.name)
                        },
                        onShuffleClick = { pQueue ->
                            val shuffled = pQueue.shuffled()
                            if (shuffled.isNotEmpty()) viewModel.playTrack(shuffled.first(), shuffled, playlist.name)
                        },
                        onFavoriteToggle = { viewModel.toggleFavorite(it) },
                        onRemoveTrack = { pId, tId -> viewModel.removeTrackFromPlaylist(pId, tId) },
                        onRenamePlaylist = { pId, newName -> viewModel.renamePlaylist(pId, newName) },
                        onDeletePlaylist = { pId -> viewModel.deletePlaylist(pId) },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onAddToQueue = { viewModel.addToQueue(it) },
                        onPlayNext = { viewModel.playNext(it) },
                        allTracks = tracks,
                        onAddTracksToPlaylist = { pId, toAdd -> viewModel.addTracksToPlaylist(pId, toAdd) },
                        onBackClick = { viewModel.navigateBack() }
                    )
                }
            }

            AnimatedVisibility(
                visible = fullScreenDest == VesperFullScreenDestination.FAVORITES,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                FavoritesScreen(
                    favoriteTracks = favoriteTracks,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    favoriteTrackIds = favoriteTrackIds,
                    onTrackClick = { track, fQueue -> viewModel.playTrack(track, fQueue, "Favorites") },
                    onPlayAllClick = { fQueue ->
                        if (fQueue.isNotEmpty()) viewModel.playTrack(fQueue.first(), fQueue, "Favorites")
                    },
                    onShuffleClick = { fQueue ->
                        val shuffled = fQueue.shuffled()
                        if (shuffled.isNotEmpty()) viewModel.playTrack(shuffled.first(), shuffled, "Favorites")
                    },
                    onAddAllToQueue = { fQueue ->
                        fQueue.forEach { viewModel.addToQueue(it) }
                    },
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    onMoreTrackClick = { viewModel.openTrackActionSheet(it) },
                    onBackClick = { viewModel.navigateBack() },
                    onExploreSongsClick = {
                        viewModel.navigateBack()
                        viewModel.setDestination(MainDestination.SONGS)
                    }
                )
            }

            // Branded Vesper Cosmic Startup Splash
            AnimatedVisibility(
                visible = isSplashVisible,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VesperBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(VesperDeepBurgundy)
                                .border(2.dp, dynamicAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            VesperLogo(
                                size = 68.dp,
                                animated = true
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "VESPER",
                            style = VesperTypography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = VesperTextPrimary,
                                letterSpacing = 3.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "PRIVATE • OFFLINE • PREMIUM MUSIC PLAYER",
                            style = VesperTypography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = VesperTextSecondary,
                                letterSpacing = 1.2.sp
                            )
                        )
                    }
                }
            }

            // Global Track Action Bottom Sheet
            trackForActionSheet?.let { track ->
                TrackActionBottomSheet(
                    track = track,
                    onDismiss = { viewModel.closeTrackActionSheet() },
                    onPlay = {
                        viewModel.playTrack(track, tracks)
                        viewModel.closeTrackActionSheet()
                    },
                    onPlayNext = {
                        viewModel.playNext(track)
                        viewModel.closeTrackActionSheet()
                    },
                    onAddToQueue = {
                        viewModel.addToQueue(track)
                        viewModel.closeTrackActionSheet()
                    },
                    onToggleFavorite = {
                        viewModel.toggleFavorite(track)
                        viewModel.closeTrackActionSheet()
                    },
                    onAddToPlaylist = {
                        viewModel.closeTrackActionSheet()
                        viewModel.openAddToPlaylistDialog(track)
                    },
                    onEditMetadata = {
                        viewModel.closeTrackActionSheet()
                        viewModel.openMetadataEditor(track)
                    },
                    onTrackInfo = {
                        viewModel.closeTrackActionSheet()
                        viewModel.openTrackInfoDialog(track)
                    },
                    onDelete = {
                        viewModel.closeTrackActionSheet()
                        viewModel.openDeleteConfirmDialog(listOf(track))
                    },
                    onOpenEqualizer = {
                        viewModel.closeTrackActionSheet()
                        viewModel.navigateTo(VesperFullScreenDestination.EQUALIZER)
                    },
                    onClearQueue = {
                        viewModel.closeTrackActionSheet()
                        viewModel.clearQueue()
                    },
                    onGoToAlbum = {
                        viewModel.closeTrackActionSheet()
                        val album = albums.find { it.name.equals(track.album, ignoreCase = true) }
                        if (album != null) {
                            viewModel.openAlbumDetails(album)
                        }
                    },
                    onGoToArtist = {
                        viewModel.closeTrackActionSheet()
                        val artist = artists.find { it.name.equals(track.artist, ignoreCase = true) }
                        if (artist != null) {
                            viewModel.openArtistDetails(artist)
                        }
                    },
                    onGoToFolder = {
                        viewModel.closeTrackActionSheet()
                        viewModel.setDestination(MainDestination.SONGS)
                    },
                    onShareTrack = {
                        viewModel.closeTrackActionSheet()
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "audio/*"
                                putExtra(Intent.EXTRA_STREAM, track.contentUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share ${track.title}"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }

            // Global Rename / Edit Metadata Dialog
            trackForRename?.let { track ->
                RenameTrackDialog(
                    track = track,
                    onDismiss = { viewModel.closeRenameTrackDialog() },
                    onConfirm = { newTitle, newArtist, newAlbum ->
                        viewModel.updateTrackMetadata(newTitle, newArtist, newAlbum)
                    }
                )
            }

            // Global Track Info Dialog
            trackForDetails?.let { track ->
                TrackInfoDialog(
                    track = track,
                    onDismiss = { viewModel.closeTrackInfoDialog() }
                )
            }

            // Global Delete Confirmation Dialog
            tracksForDeleteConfirm?.let { deleteTracks ->
                ConfirmDeleteDialog(
                    itemCount = deleteTracks.size,
                    itemName = deleteTracks.firstOrNull()?.title.orEmpty(),
                    onDismiss = { viewModel.closeDeleteConfirmDialog() },
                    onConfirm = {
                        viewModel.deleteTracks(deleteTracks)
                    }
                )
            }

            // Sleep Timer Bottom Sheet Modal
            if (showSleepTimerDialog) {
                SleepTimerBottomSheet(
                    timerState = sleepTimerState,
                    onSetTimerMinutes = { min, fade -> viewModel.startSleepTimer(min, fade) },
                    onSetEndOfTrack = { viewModel.startEndOfTrackSleepTimer() },
                    onCancelTimer = { viewModel.cancelSleepTimer() },
                    onAddMinutes = { viewModel.addSleepTimerMinutes(it) },
                    onDismiss = { viewModel.closeSleepTimerDialog() }
                )
            }
        }
    }
}
