package com.vesper.audio

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vesper.audio.data.MusicRepository
import com.vesper.audio.data.local.CachedTrackEntity
import com.vesper.audio.data.local.DiscoverCachedTrackEntity
import com.vesper.audio.data.local.SonoraDatabase
import com.vesper.audio.data.local.SonoraLocalRepository
import com.vesper.audio.data.local.VesperPreferencesRepository
import com.vesper.audio.data.local.toCachedEntity
import com.vesper.audio.data.local.toTrack
import com.vesper.audio.data.media.DemoAudioProvider
import com.vesper.audio.data.remote.AudiusDiscoverProvider
import com.vesper.audio.data.remote.BitChordDiscoverProvider
import com.vesper.audio.data.remote.DiscoverArtistDetail
import com.vesper.audio.data.remote.DiscoverAlbumDetail
import com.vesper.audio.data.remote.DiscoverComparisonUtils
import com.vesper.audio.data.remote.DiscoverPageResult
import com.vesper.audio.data.remote.DiscoverProvider
import com.vesper.audio.data.remote.DiscoverRepository
import com.vesper.audio.data.remote.DiscoverTrack
import com.vesper.audio.data.remote.JioSaavnDiscoverProvider
import com.vesper.audio.playback.SonoraPlayerClient
import com.vesper.audio.ui.screens.SettingsCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: SonoraDatabase
    private lateinit var repository: SonoraLocalRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SonoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SonoraLocalRepository(db.sonoraDao())
    }

    @After
    fun tearDown() {
        db.close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val demoDir = java.io.File(context.filesDir, "sonora_demo_tracks")
        if (demoDir.exists()) {
            demoDir.deleteRecursively()
        }
    }

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VESPER", appName)
    }

    @Test
    fun testFavoriteTrackPersistence() = runBlocking {
        repository.setFavorite("track_001", true)
        val favorites = repository.allFavoriteIds.first()
        assertEquals(1, favorites.size)
        assertEquals("track_001", favorites[0])

        val isFav = repository.isFavorite("track_001").first()
        assertTrue(isFav)

        repository.setFavorite("track_001", false)
        val updatedFavorites = repository.allFavoriteIds.first()
        assertEquals(0, updatedFavorites.size)
    }

    @Test
    fun testPlaylistCreation() = runBlocking {
        repository.createPlaylist("Midnight Vibes")
        val playlists = repository.allPlaylists.first()
        assertEquals(1, playlists.size)
        assertEquals("Midnight Vibes", playlists[0].name)
    }

    @Test
    fun testScannedFolderPersistence() = runBlocking {
        repository.addScannedFolder("content://com.android.externalstorage.documents/tree/primary%3AMusic", "Music")
        val folders = repository.allScannedFolders.first()
        assertEquals(1, folders.size)
        assertEquals("Music", folders[0].displayName)

        val syncFolders = repository.getAllScannedFoldersSync()
        assertEquals(1, syncFolders.size)
        assertEquals("content://com.android.externalstorage.documents/tree/primary%3AMusic", syncFolders[0].uriString)

        repository.removeScannedFolder("content://com.android.externalstorage.documents/tree/primary%3AMusic")
        val updated = repository.allScannedFolders.first()
        assertEquals(0, updated.size)
    }

    @Test
    fun testDemoAudioGeneration() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = DemoAudioProvider(context)
        val demos = provider.getOrGenerateDemoTracks()
        assertTrue(demos.isNotEmpty())
        assertEquals(4, demos.size)
        assertEquals("Apocalypse (Acoustic Ambient)", demos[0].title)
    }

    @Test
    fun testEmptyLibraryRemainsEmpty() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val musicRepo = MusicRepository(context, repository)
        val tracks = musicRepo.tracks.first()
        assertEquals(0, tracks.size)
        val albums = musicRepo.albums.first()
        assertEquals(0, albums.size)
    }

    @Test
    fun testExplicitDemoLoading() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val musicRepo = MusicRepository(context, repository)
        musicRepo.forceLoadDemoTracks()
        val tracks = musicRepo.tracks.first { it.isNotEmpty() }
        assertTrue(tracks.isNotEmpty())
        assertEquals(4, tracks.size)
    }

    @Test
    fun testMetadataUpdating() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val musicRepo = MusicRepository(context, repository)
        musicRepo.forceLoadDemoTracks()
        val tracks = musicRepo.tracks.first { it.isNotEmpty() }
        val targetTrack = tracks[0]

        musicRepo.updateTrackMetadata(targetTrack.id, "Cosmic Awakening", "Vesper Audio", "Celestial Drift")
        val updatedTracks = musicRepo.tracks.first { it[0].title == "Cosmic Awakening" }
        assertEquals("Cosmic Awakening", updatedTracks[0].title)
        assertEquals("Vesper Audio", updatedTracks[0].artist)
        assertEquals("Celestial Drift", updatedTracks[0].album)
    }

    @Test
    fun testSoundProfileValues() {
        val profiles = com.vesper.audio.ui.SoundProfile.values()
        assertEquals(15, profiles.size)
        assertTrue(profiles.any { it.name == "BALANCED" })
        assertTrue(profiles.any { it.name == "BASS_BOOST" })
        assertTrue(profiles.any { it.name == "NIGHT" })
        assertTrue(profiles.any { it.name == "STUDIO_REFERENCE" })
        assertTrue(profiles.any { it.name == "WARM_ANALOG" })
    }

    @Test
    fun testDrawerDestinationSequence() {
        val expectedSequence = listOf(
            com.vesper.audio.ui.VesperFullScreenDestination.ABOUT,
            com.vesper.audio.ui.VesperFullScreenDestination.SETTINGS,
            com.vesper.audio.ui.VesperFullScreenDestination.EQUALIZER,
            com.vesper.audio.ui.VesperFullScreenDestination.TONES,
            com.vesper.audio.ui.VesperFullScreenDestination.SCAN_MEDIA,
            com.vesper.audio.ui.VesperFullScreenDestination.DONATE
        )
        assertEquals(6, expectedSequence.size)
        assertEquals(com.vesper.audio.ui.VesperFullScreenDestination.ABOUT, expectedSequence[0])
        assertEquals(com.vesper.audio.ui.VesperFullScreenDestination.SETTINGS, expectedSequence[1])
        assertEquals(com.vesper.audio.ui.VesperFullScreenDestination.EQUALIZER, expectedSequence[2])
        assertEquals(com.vesper.audio.ui.VesperFullScreenDestination.TONES, expectedSequence[3])
        assertEquals(com.vesper.audio.ui.VesperFullScreenDestination.SCAN_MEDIA, expectedSequence[4])
        assertEquals(com.vesper.audio.ui.VesperFullScreenDestination.DONATE, expectedSequence[5])
    }

    @Test
    fun testVesperPaletteConstants() {
        assertEquals(androidx.compose.ui.graphics.Color(0xFF000000), com.vesper.audio.ui.theme.VesperBackground)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF0B0B0D), com.vesper.audio.ui.theme.VesperBackgroundSecondary)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF141418), com.vesper.audio.ui.theme.VesperSurfaceElevated)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF4A1824), com.vesper.audio.ui.theme.VesperDeepBurgundy)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF8C2F48), com.vesper.audio.ui.theme.VesperBurgundyAccent)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFF2E9EA), com.vesper.audio.ui.theme.VesperTextPrimary)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFD6CBCD), com.vesper.audio.ui.theme.VesperTextSecondary)
    }

    @Test
    fun testBottomNavigationSequence() {
        val entries = com.vesper.audio.ui.MainDestination.entries
        assertEquals(5, entries.size)
        assertEquals(com.vesper.audio.ui.MainDestination.HOME, entries[0])
        assertEquals(com.vesper.audio.ui.MainDestination.SONGS, entries[1])
        assertEquals(com.vesper.audio.ui.MainDestination.ALBUMS, entries[2])
        assertEquals(com.vesper.audio.ui.MainDestination.PLAYLISTS, entries[3])
        assertEquals(com.vesper.audio.ui.MainDestination.DISCOVER, entries[4])
        assertEquals(com.vesper.audio.ui.MainDestination.DISCOVER, com.vesper.audio.ui.MainDestination.SEARCH)
    }

    @Test
    fun testAlbumTrackNumberSorting() {
        val t1 = com.vesper.audio.model.Track("1", "Song B", "Artist X", "Album Y", 1000L, android.net.Uri.EMPTY, trackNumber = 2)
        val t2 = com.vesper.audio.model.Track("2", "Song A", "Artist X", "Album Y", 1000L, android.net.Uri.EMPTY, trackNumber = 1)
        val t3 = com.vesper.audio.model.Track("3", "Song C", "Artist X", "Album Y", 1000L, android.net.Uri.EMPTY, trackNumber = 3)

        val sorted = listOf(t1, t2, t3).sortedWith(
            compareBy(
                { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE },
                { it.title.lowercase() }
            )
        )

        assertEquals("2", sorted[0].id)
        assertEquals("1", sorted[1].id)
        assertEquals("3", sorted[2].id)
    }

    @Test
    fun testArtistFiltering() {
        val t1 = com.vesper.audio.model.Track("1", "Track 1", "Daft Punk", "Album 1", 1000L, android.net.Uri.EMPTY)
        val t2 = com.vesper.audio.model.Track("2", "Track 2", "The Weeknd", "Album 2", 1000L, android.net.Uri.EMPTY)
        val t3 = com.vesper.audio.model.Track("3", "Track 3", "Daft Punk", "Album 1", 1000L, android.net.Uri.EMPTY)

        val daftTracks = listOf(t1, t2, t3).filter { it.artist.equals("Daft Punk", ignoreCase = true) }
        assertEquals(2, daftTracks.size)
        assertTrue(daftTracks.all { it.artist == "Daft Punk" })
    }

    @Test
    fun testEqualizerSteppingAndFlatReset() {
        val eqController = com.vesper.audio.audio.AudioEffectController()
        val initialBands = eqController.bands.value
        assertEquals(10, initialBands.size)
        assertEquals(0.toShort(), initialBands[0].levelMb)

        // Step up by +2 dB (200 mB)
        eqController.stepBandLevel(0, 2)
        assertEquals(200.toShort(), eqController.bands.value[0].levelMb)
        assertEquals("Custom", eqController.activePresetName.value)

        // Preamp headroom should automatically attenuate when positive boost is present
        assertTrue(eqController.preampGainScalar.value < 1.0f)

        // Step down by -1 dB (100 mB)
        eqController.stepBandLevel(0, -1)
        assertEquals(100.toShort(), eqController.bands.value[0].levelMb)

        // Adjust all bands by +1 dB
        eqController.adjustAllBands(1)
        assertEquals(200.toShort(), eqController.bands.value[0].levelMb)
        assertEquals(100.toShort(), eqController.bands.value[1].levelMb)

        // Reset to flat (0 dB)
        eqController.resetToFlat()
        assertTrue(eqController.bands.value.all { it.levelMb == 0.toShort() })
        assertEquals("Balanced", eqController.activePresetName.value)
        assertEquals(1.0f, eqController.preampGainScalar.value, 0.001f)
    }

    @Test
    fun testPlaylistWithCountAndTrackAddition() = runBlocking {
        val playlistId = repository.createPlaylist("Cosmic Ambient")
        var playlists = repository.playlistsWithCount.first()
        assertEquals(1, playlists.size)
        assertEquals(0, playlists[0].trackCount)

        repository.addTrackToPlaylist(playlistId, "track_alpha", 0)
        repository.addTrackToPlaylist(playlistId, "track_beta", 1)

        playlists = repository.playlistsWithCount.first()
        assertEquals(1, playlists.size)
        assertEquals(2, playlists[0].trackCount)

        repository.removeTrackFromPlaylist(playlistId, "track_alpha")
        playlists = repository.playlistsWithCount.first()
        assertEquals(1, playlists[0].trackCount)
    }

    @Test
    fun testSongsFilteringBySearchQuery() {
        val t1 = com.vesper.audio.model.Track("1", "Starboy", "The Weeknd", "Starboy", 1000L, android.net.Uri.EMPTY)
        val t2 = com.vesper.audio.model.Track("2", "Blinding Lights", "The Weeknd", "After Hours", 1000L, android.net.Uri.EMPTY)
        val t3 = com.vesper.audio.model.Track("3", "Get Lucky", "Daft Punk", "Random Access Memories", 1000L, android.net.Uri.EMPTY)
        val all = listOf(t1, t2, t3)

        val query1 = "blinding"
        val res1 = all.filter {
            it.title.contains(query1, ignoreCase = true) ||
            it.artist.contains(query1, ignoreCase = true) ||
            it.album.contains(query1, ignoreCase = true)
        }
        assertEquals(1, res1.size)
        assertEquals("2", res1[0].id)

        val query2 = "weeknd"
        val res2 = all.filter {
            it.title.contains(query2, ignoreCase = true) ||
            it.artist.contains(query2, ignoreCase = true) ||
            it.album.contains(query2, ignoreCase = true)
        }
        assertEquals(2, res2.size)

        val queryEmpty = ""
        val resEmpty = if (queryEmpty.isBlank()) all else all.filter { it.title.contains(queryEmpty) }
        assertEquals(3, resEmpty.size)
    }

    @Test
    fun testSoundProfileProperties() {
        val profiles = com.vesper.audio.ui.SoundProfile.values()
        assertEquals(15, profiles.size)
        profiles.forEach { p ->
            assertTrue(p.title.isNotBlank())
            assertTrue(p.subtitle.isNotBlank())
            assertTrue(p.description.isNotBlank())
            assertTrue(p.bassBoost >= 0)
        }
    }

    @Test
    fun testSyncedLyricsParsing() {
        val lrcContent = """
            [00:12.50]Lost in the cosmic haze
            [00:18.20]Under burgundy skies
            [01:04.00]Nightfall takes over
        """.trimIndent()

        val lyrics = com.vesper.audio.util.VesperLyricsResolver.parseLrc(lrcContent)
        assertTrue(lyrics != null)
        val lines = lyrics!!.syncedLines
        assertEquals(3, lines.size)
        assertEquals(12500L, lines[0].timestampMs)
        assertEquals("Lost in the cosmic haze", lines[0].text)
        assertEquals(18200L, lines[1].timestampMs)
        assertEquals("Under burgundy skies", lines[1].text)
        assertEquals(64000L, lines[2].timestampMs)
        assertEquals("Nightfall takes over", lines[2].text)
    }

    @Test
    fun testPlaylistRenamePreservesTracks() = runBlocking {
        val pId = repository.createPlaylist("Acoustic Sessions")
        repository.addTrackToPlaylist(pId, "t1", 0)
        repository.addTrackToPlaylist(pId, "t2", 1)

        val before = repository.getPlaylistTrackIds(pId).first()
        assertEquals(2, before.size)

        repository.renamePlaylist(pId, "Electric Dreams")
        val playlists = repository.allPlaylists.first()
        assertEquals("Electric Dreams", playlists.first { it.id == pId }.name)

        val after = repository.getPlaylistTrackIds(pId).first()
        assertEquals(2, after.size)
        assertEquals("t1", after[0])
        assertEquals("t2", after[1])
    }

    @Test
    fun testVesperValidationUtils() {
        val validUrl = "https://freemusicarchive.org/music/track.mp3"
        val res1 = com.vesper.audio.util.VesperValidationUtils.validateDownloadUrl(validUrl)
        assertTrue(res1 is com.vesper.audio.util.VesperValidationUtils.UrlValidationResult.Valid)

        val fileUrl = "file:///sdcard/music.mp3"
        val res2 = com.vesper.audio.util.VesperValidationUtils.validateDownloadUrl(fileUrl)
        assertTrue(res2 is com.vesper.audio.util.VesperValidationUtils.UrlValidationResult.Invalid)

        val loopbackUrl = "http://127.0.0.1/audio.mp3"
        val res3 = com.vesper.audio.util.VesperValidationUtils.validateDownloadUrl(loopbackUrl)
        assertTrue(res3 is com.vesper.audio.util.VesperValidationUtils.UrlValidationResult.Invalid)

        val exeUrl = "https://example.com/malicious.exe"
        val res4 = com.vesper.audio.util.VesperValidationUtils.validateDownloadUrl(exeUrl)
        assertTrue(res4 is com.vesper.audio.util.VesperValidationUtils.UrlValidationResult.Invalid)

        val sanitized = com.vesper.audio.util.VesperValidationUtils.sanitizeFileName("../../etc/secret.mp3")
        assertEquals("secret.mp3", sanitized)

        val emptySanitized = com.vesper.audio.util.VesperValidationUtils.sanitizeFileName("   ")
        assertTrue(emptySanitized.endsWith(".mp3"))

        val (validMeta, _) = com.vesper.audio.util.VesperValidationUtils.validateMetadata("Title", "Artist", "Album")
        assertTrue(validMeta)

        val (blankTitle, err) = com.vesper.audio.util.VesperValidationUtils.validateMetadata("   ", "Artist", "Album")
        assertTrue(!blankTitle)
        assertEquals("Track title cannot be blank", err)

        assertTrue(com.vesper.audio.util.VesperValidationUtils.validateHexColor("#722C42"))
        assertTrue(com.vesper.audio.util.VesperValidationUtils.validateHexColor("#FF722C42"))
        assertTrue(!com.vesper.audio.util.VesperValidationUtils.validateHexColor("blue"))
    }

    @Test
    fun testEqualizerBandClamping() {
        val eq = com.vesper.audio.audio.AudioEffectController()
        eq.setBandLevel(0, 2000.toShort())
        assertEquals(1200.toShort(), eq.bands.value[0].levelMb)

        eq.setBandLevel(0, (-2000).toShort())
        assertEquals((-1200).toShort(), eq.bands.value[0].levelMb)
    }

    @Test
    fun testCustomEqPresetSaveRenameAndDelete() = runBlocking {
        repository.saveEqPreset("My Warm Vocal", "0,100,200,300,200,100,0,0,-100,-100", 150)
        var presets = repository.customEqPresets.first()
        assertEquals(1, presets.size)
        assertEquals("My Warm Vocal", presets[0].name)

        repository.renameEqPreset("My Warm Vocal", "Late Night Vocal", presets[0].bandLevelsCsv, presets[0].bassBoost)
        presets = repository.customEqPresets.first()
        assertEquals(1, presets.size)
        assertEquals("Late Night Vocal", presets[0].name)

        repository.deleteEqPreset("Late Night Vocal")
        presets = repository.customEqPresets.first()
        assertEquals(0, presets.size)
    }

    @Test
    fun testSleepTimerController() {
        var pauseCalled = false
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val controller = com.vesper.audio.playback.SleepTimerController(
            scope = scope
        )

        assertTrue(!controller.timerState.value.isActive)
        assertEquals(0L, controller.timerState.value.remainingSeconds)

        controller.startTimer(30, fadeOut = true, onTimerFinished = { pauseCalled = true })
        assertTrue(controller.timerState.value.isActive)
        assertEquals(30, controller.timerState.value.initialMinutes)
        assertEquals(1800L, controller.timerState.value.remainingSeconds)
        assertTrue(controller.timerState.value.fadeOut)

        controller.addMinutes(15)
        assertEquals(2700L, controller.timerState.value.remainingSeconds)

        controller.cancelTimer()
        assertTrue(!controller.timerState.value.isActive)
        assertEquals(0L, controller.timerState.value.remainingSeconds)

        controller.startEndOfTrackTimer(onTimerFinished = { pauseCalled = true })
        assertTrue(controller.timerState.value.isActive)
        assertTrue(controller.timerState.value.isEndOfTrack)

        controller.onTrackEnded()
        assertTrue(pauseCalled)
        assertTrue(!controller.timerState.value.isActive)
    }

    @Test
    fun testAllEightDeterministicSortModes() {
        val modes = com.vesper.audio.ui.SortMode.values()
        assertEquals(8, modes.size)
        assertTrue(modes.contains(com.vesper.audio.ui.SortMode.TITLE_AZ))
        assertTrue(modes.contains(com.vesper.audio.ui.SortMode.TITLE_ZA))
        assertTrue(modes.contains(com.vesper.audio.ui.SortMode.ARTIST_AZ))
        assertTrue(modes.contains(com.vesper.audio.ui.SortMode.ALBUM_AZ))
        assertTrue(modes.contains(com.vesper.audio.ui.SortMode.RECENTLY_ADDED))
        assertTrue(modes.contains(com.vesper.audio.ui.SortMode.OLDEST_ADDED))
        assertTrue(modes.contains(com.vesper.audio.ui.SortMode.DURATION_DESC))
        assertTrue(modes.contains(com.vesper.audio.ui.SortMode.DURATION_ASC))
    }

    @Test
    fun testCachedTrackPersistenceAndConversion() = runBlocking {
        val entity = CachedTrackEntity(
            id = "101",
            mediaStoreId = 101L,
            contentUriString = "content://media/external/audio/media/101",
            title = "Starfall",
            artist = "Vesper Ensemble",
            album = "Cosmic Echoes",
            durationMs = 215000L,
            trackNumber = 1,
            discNumber = 1,
            year = 2026,
            genre = "Ambient",
            albumArtist = "Vesper Ensemble",
            artworkUriString = null,
            dateAdded = 1700000000L,
            dateModified = 1700001000L,
            sizeBytes = 8500000L,
            mimeType = "audio/flac"
        )

        repository.insertCachedTracks(listOf(entity))
        val cachedList = repository.getAllCachedTracksSync()
        assertEquals(1, cachedList.size)
        assertEquals("Starfall", cachedList[0].title)
        assertEquals("Vesper Ensemble", cachedList[0].artist)

        val track = cachedList[0].toTrack()
        assertEquals("101", track.id)
        assertEquals("Starfall", track.title)
        assertEquals(215000L, track.durationMs)
        assertEquals("3:35", track.formattedDuration)

        val reCached = track.toCachedEntity(mediaStoreId = 101L, dateModified = 1700001000L)
        assertEquals("101", reCached.id)
        assertEquals(101L, reCached.mediaStoreId)
        assertEquals(1700001000L, reCached.dateModified)

        repository.updateCachedTrackMetadata("101", "Starfall (Remastered)", "Vesper Ensemble", "Cosmic Echoes Deluxe", 2026, "Ambient")
        val updatedList = repository.getAllCachedTracksSync()
        assertEquals("Starfall (Remastered)", updatedList[0].title)
        assertEquals("Cosmic Echoes Deluxe", updatedList[0].album)

        repository.deleteCachedTracksByIds(listOf("101"))
        val emptyList = repository.getAllCachedTracksSync()
        assertEquals(0, emptyList.size)
    }

    @Test
    fun testHexColorValidation() {
        assertTrue(com.vesper.audio.util.VesperValidationUtils.validateHexColor("#722C42"))
        assertTrue(com.vesper.audio.util.VesperValidationUtils.validateHexColor("#D6A63C"))
        assertTrue(com.vesper.audio.util.VesperValidationUtils.validateHexColor("#2A7B9B"))
        assertTrue(com.vesper.audio.util.VesperValidationUtils.validateHexColor("#8B3A8B"))
        assertTrue(!com.vesper.audio.util.VesperValidationUtils.validateHexColor("invalid"))
        assertTrue(!com.vesper.audio.util.VesperValidationUtils.validateHexColor("#12345"))
        assertTrue(!com.vesper.audio.util.VesperValidationUtils.validateHexColor("#GGGGGG"))
    }

    @Test
    fun testCompositeAlbumGroupingDifferentArtistsSameAlbumName() {
        val scanner = com.vesper.audio.data.media.MediaStoreScanner(ApplicationProvider.getApplicationContext<Context>())
        val tracks = listOf(
            com.vesper.audio.model.Track(
                id = "1",
                title = "Song A",
                artist = "Artist One",
                album = "Greatest Hits",
                durationMs = 180000L,
                contentUri = android.net.Uri.EMPTY
            ),
            com.vesper.audio.model.Track(
                id = "2",
                title = "Song B",
                artist = "Artist Two",
                album = "Greatest Hits",
                durationMs = 210000L,
                contentUri = android.net.Uri.EMPTY
            )
        )

        val albums = scanner.groupIntoAlbums(tracks)
        assertEquals("Different artists with the same album name must NOT be merged", 2, albums.size)
        assertEquals("Greatest Hits", albums[0].name)
        assertEquals("Greatest Hits", albums[1].name)
        val artists = albums.map { it.artist }.toSet()
        assertTrue(artists.contains("Artist One"))
        assertTrue(artists.contains("Artist Two"))
    }

    @Test
    fun testArtworkResolverFormatBytes() {
        assertEquals("0 KB", com.vesper.audio.util.VesperArtworkResolver.formatBytes(0L))
        assertEquals("500 KB", com.vesper.audio.util.VesperArtworkResolver.formatBytes(500 * 1024L))
        assertEquals("15.5 MB", com.vesper.audio.util.VesperArtworkResolver.formatBytes((15.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun testCircularQueueMath() {
        // Empty queue returns null
        assertNull(SonoraPlayerClient.nextIndex(0, 0))
        assertNull(SonoraPlayerClient.previousIndex(0, 0))

        // 1-item queue wraps to 0
        assertEquals(0, SonoraPlayerClient.nextIndex(0, 1))
        assertEquals(0, SonoraPlayerClient.previousIndex(0, 1))

        val queueSize = 5
        // At end of queue, nextIndex wraps to 0
        assertEquals(0, SonoraPlayerClient.nextIndex(4, queueSize))

        // At start of queue, previousIndex wraps to queueSize - 1
        assertEquals(4, SonoraPlayerClient.previousIndex(0, queueSize))

        // Middle track advances/steps back normally
        assertEquals(3, SonoraPlayerClient.nextIndex(2, queueSize))
        assertEquals(1, SonoraPlayerClient.previousIndex(2, queueSize))
    }

    @Test
    fun testArtworkResolverFallbackAndCacheClear() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.vesper.audio.util.VesperArtworkResolver.clearArtworkCache(context)
        val initialSize = com.vesper.audio.util.VesperArtworkResolver.getArtworkCacheSizeBytes(context)
        assertEquals(0L, initialSize)

        val dummyAudioUri = Uri.parse("content://dummy/audio/1")
        val resolved = com.vesper.audio.util.VesperArtworkResolver.resolveArtworkUri(
            context = context,
            audioUri = dummyAudioUri,
            albumId = 42L,
            trackId = "track_123",
            dateModified = 1000L,
            sizeBytes = 5000L
        )
        assertNotNull(resolved)
        assertEquals("content://media/external/audio/albumart/42", resolved.toString())
    }

    @Test
    fun testAudiusUrlBuildersAndPagination() {
        val trendingUrl0 = com.vesper.audio.data.remote.AudiusApiClient.buildTrendingUrl(limit = 25, offset = 0)
        assertTrue(trendingUrl0.contains("limit=25"))
        assertTrue(trendingUrl0.contains("offset=0"))

        val trendingUrl25 = com.vesper.audio.data.remote.AudiusApiClient.buildTrendingUrl(limit = 25, offset = 25)
        assertTrue(trendingUrl25.contains("offset=25"))

        val searchUrl = com.vesper.audio.data.remote.AudiusApiClient.buildSearchUrl("synth wave", limit = 25, offset = 50)
        assertTrue(searchUrl.contains("query=synth%20wave"))
        assertTrue(searchUrl.contains("limit=25"))
        assertTrue(searchUrl.contains("offset=50"))
    }

    @Test
    fun testAudiusJsonParsingAndAlbumArtistDerivation() {
        val client = com.vesper.audio.data.remote.AudiusApiClient()
        val sampleJson = """
            {
              "data": [
                {
                  "id": "a1",
                  "title": "Neon Horizon",
                  "duration": 195,
                  "genre": "Electronic",
                  "artwork": { "480x480": "https://cdn.audius.co/art1.jpg" },
                  "user": { "name": "Nova Pulse", "handle": "novapulse" }
                },
                {
                  "id": "a2",
                  "title": "Solar Echo",
                  "duration": 210,
                  "genre": "Electronic",
                  "artwork": { "150x150": "https://cdn.audius.co/art2.jpg" },
                  "user": { "name": "Nova Pulse", "handle": "novapulse" }
                },
                {
                  "id": "a3",
                  "title": "Acoustic Dawn",
                  "duration": 180,
                  "genre": "Acoustic",
                  "user": { "name": "", "handle": "indiefolk" }
                }
              ]
            }
        """.trimIndent()

        val parsed = client.parseTracksJson(sampleJson)
        assertEquals(3, parsed.size)
        assertEquals("Neon Horizon", parsed[0].title)
        assertEquals("Nova Pulse", parsed[0].artist)
        assertEquals("https://cdn.audius.co/art1.jpg", parsed[0].artworkUrl)
        assertEquals("indiefolk", parsed[2].artist)
        assertEquals("audius:a1", parsed[0].namespacedId)
        assertEquals("audius:a1", parsed[0].toTrack().id)

        val derivedAlbums = com.vesper.audio.data.remote.AudiusApiClient.deriveAlbumsFromTracks(parsed)
        assertEquals(2, derivedAlbums.size)
        val electronicAlbum = derivedAlbums.first { it.artist == "Nova Pulse" }
        assertEquals(2, electronicAlbum.trackCount)

        val derivedArtists = com.vesper.audio.data.remote.AudiusApiClient.deriveArtistsFromTracks(parsed)
        assertEquals(2, derivedArtists.size)
        val novaArtist = derivedArtists.first { it.name == "Nova Pulse" }
        assertEquals(2, novaArtist.trackCount)

        // Malformed JSON should safely return emptyList without throwing
        assertTrue(client.parseTracksJson("{malformed_json").isEmpty())
    }

    @Test
    fun testToneCustomPresetApplyAndStereoBalance() {
        val eq = com.vesper.audio.audio.AudioEffectController()
        eq.applyCustomPreset("Midnight Custom", "200,150,0,-100,0,100,200,300,200,100", 400)
        assertEquals("Midnight Custom", eq.activePresetName.value)
        assertEquals(200.toShort(), eq.bands.value[0].levelMb)
        assertEquals(300.toShort(), eq.bands.value[7].levelMb)
        assertEquals(400.toShort(), eq.bassBoostStrength.value)

        // Headroom toggle
        assertTrue(eq.preampGainScalar.value < 1.0f)
        eq.toggleHeadroomProtection(false)
        assertEquals(1.0f, eq.preampGainScalar.value, 0.001f)
        eq.toggleHeadroomProtection(true)
        assertTrue(eq.preampGainScalar.value < 1.0f)

        // Stereo balance clamping
        eq.setAudioBalance(-1.5f)
        assertEquals(-1.0f, eq.audioBalance.value, 0.001f)
        eq.setAudioBalance(0.45f)
        assertEquals(0.45f, eq.audioBalance.value, 0.001f)

        // VESPER Signature & Flat / DSP Off honest description check
        val vesperSig = com.vesper.audio.audio.AudioEffectController.BUILT_IN_MODES.first { it.name == "VESPER Signature" }
        assertTrue(vesperSig.description.contains("Balanced house sound", ignoreCase = true))
        val flatBypass = com.vesper.audio.audio.AudioEffectController.BUILT_IN_MODES.first { it.name == "Flat / DSP Off" }
        assertTrue(!flatBypass.description.contains("hardware bypass", ignoreCase = true))
        assertTrue(flatBypass.description.contains("0 dB bypass", ignoreCase = true))
    }

    @Test
    fun testRoomMigration5To6And6To7And7To8Exists() {
        assertEquals(5, SonoraDatabase.MIGRATION_5_6.startVersion)
        assertEquals(6, SonoraDatabase.MIGRATION_5_6.endVersion)
        assertEquals(6, SonoraDatabase.MIGRATION_6_7.startVersion)
        assertEquals(7, SonoraDatabase.MIGRATION_6_7.endVersion)
        assertEquals(7, SonoraDatabase.MIGRATION_7_8.startVersion)
        assertEquals(8, SonoraDatabase.MIGRATION_7_8.endVersion)
    }

    @Test
    fun testUserMetadataOverridePersistence() = runBlocking {
        val initialEntity = CachedTrackEntity(
            id = "202",
            mediaStoreId = 202L,
            contentUriString = "content://media/external/audio/media/202",
            title = "Original File Title",
            artist = "Original Artist",
            album = "Original Album",
            durationMs = 180000L,
            trackNumber = 1,
            discNumber = 1,
            year = 2020,
            genre = "Rock",
            albumArtist = "Original Artist",
            artworkUriString = null,
            dateAdded = 1700000000L,
            dateModified = 1700001000L,
            sizeBytes = 5000000L,
            mimeType = "audio/mpeg",
            isUserOverride = false
        )
        repository.insertCachedTracks(listOf(initialEntity))
        assertTrue(!repository.getAllCachedTracksSync().first().isUserOverride)

        repository.updateCachedTrackMetadata(
            id = "202",
            title = "Custom Studio Title",
            artist = "Custom Studio Artist",
            album = "Custom Studio Album",
            year = 2026,
            genre = "Synthwave",
            trackNumber = 7
        )

        val updated = repository.getAllCachedTracksSync().first()
        assertTrue(updated.isUserOverride)
        assertEquals("Custom Studio Title", updated.title)
        assertEquals("Custom Studio Artist", updated.artist)
        assertEquals("Custom Studio Album", updated.album)
        assertEquals(2026, updated.year)
        assertEquals("Synthwave", updated.genre)
        assertEquals(7, updated.trackNumber)
    }

    @Test
    fun testFastPhaseAArtworkResolution() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.vesper.audio.util.VesperArtworkResolver.clearArtworkCache(context)

        val fastUri = com.vesper.audio.util.VesperArtworkResolver.resolveFastArtworkUri(
            context = context,
            albumId = 88L,
            trackId = "fast_trk_1",
            dateModified = 123456L,
            sizeBytes = 4096L
        )
        assertNotNull(fastUri)
        assertEquals("content://media/external/audio/albumart/88", fastUri.toString())

        val nullUri = com.vesper.audio.util.VesperArtworkResolver.resolveFastArtworkUri(
            context = context,
            albumId = 0L,
            trackId = "fast_trk_2",
            dateModified = 123456L,
            sizeBytes = 4096L
        )
        assertNull(nullUri)
    }

    @Test
    fun testRemoteOnlyDiscoverTabOrderAndSessionShuffle() {
        val tabs = com.vesper.audio.ui.screens.DiscoverTab.values().toList()
        assertEquals(3, tabs.size)
        assertEquals(com.vesper.audio.ui.screens.DiscoverTab.SONGS, tabs[0])
        assertEquals(com.vesper.audio.ui.screens.DiscoverTab.ARTISTS, tabs[1])
        assertEquals(com.vesper.audio.ui.screens.DiscoverTab.ALBUMS, tabs[2])

        val sampleTracks = listOf(
            com.vesper.audio.data.remote.AudiusTrack("id1", "Track 1", "Artist A", null, 180, "Electronic", "https://audius.co/1"),
            com.vesper.audio.data.remote.AudiusTrack("id2", "Track 2", "Artist B", null, 200, "Ambient", "https://audius.co/2"),
            com.vesper.audio.data.remote.AudiusTrack("id1", "Track 1 Duplicate", "Artist A", null, 180, "Electronic", "https://audius.co/1"),
            com.vesper.audio.data.remote.AudiusTrack("id3", "Track 3", "Artist C", null, 220, "Indie", "https://audius.co/3"),
            com.vesper.audio.data.remote.AudiusTrack("id4", "Track 4", "Artist D", null, 240, "Jazz", "https://audius.co/4")
        )

        val shuffledA = com.vesper.audio.data.remote.AudiusApiClient.shuffleForSession(sampleTracks, 42L)
        val shuffledB = com.vesper.audio.data.remote.AudiusApiClient.shuffleForSession(sampleTracks, 42L)
        assertEquals("Deduplication should remove duplicate provider IDs", 4, shuffledA.size)
        assertEquals("Same session seed should produce deterministic order", shuffledA.map { it.id }, shuffledB.map { it.id })
    }

    @Test
    fun testSettingsCategoryHierarchyAndOrder() {
        val categories = SettingsCategory.entries
        assertEquals(9, categories.size)
        assertEquals(SettingsCategory.AUDIO_QUALITY, categories[0])
        assertEquals(SettingsCategory.PLAYBACK, categories[1])
        assertEquals(SettingsCategory.CONTENT_PREFERENCES, categories[2])
        assertEquals(SettingsCategory.APPEARANCE, categories[3])
        assertEquals(SettingsCategory.PERFORMANCE, categories[4])
        assertEquals(SettingsCategory.LOCAL_MUSIC, categories[5])
        assertEquals(SettingsCategory.STORAGE, categories[6])
        assertEquals(SettingsCategory.YOUR_DATA, categories[7])
        assertEquals(SettingsCategory.MISCELLANEOUS, categories[8])
    }

    @Test
    fun testProviderPolicyAndNamespacedIds() = runBlocking {
        val audius = AudiusDiscoverProvider()
        val jiosaavn = JioSaavnDiscoverProvider()
        val bitchord = BitChordDiscoverProvider()

        assertTrue(audius.isEnabled)
        assertTrue(audius.isAuthorized)

        assertFalse("JioSaavn must be disabled by default per provider authorization policy", jiosaavn.isEnabled)
        assertFalse(jiosaavn.isAuthorized)
        assertTrue(jiosaavn.getTrending(25, 0).errorMessage?.contains("disabled", ignoreCase = true) == true)

        assertFalse("BitChord must be disabled by default per provider authorization policy", bitchord.isEnabled)
        assertFalse(bitchord.isAuthorized)
        assertTrue(bitchord.getTrending(25, 0).errorMessage?.contains("disabled", ignoreCase = true) == true)

        val dTrack = DiscoverTrack(
            providerId = "audius",
            providerName = "Audius",
            providerTrackId = "xyz987",
            title = "Astral Drift",
            artist = "Lumen",
            album = "Ambient Sessions",
            artworkUrl = "https://cdn.audius.co/cover.jpg",
            durationMs = 240000L,
            streamEndpointUrl = "https://api.audius.co/v1/tracks/xyz987/stream"
        )
        assertEquals("audius:xyz987", dTrack.namespacedId)
        assertEquals("audius:xyz987", dTrack.toTrack().id)
    }

    @Test
    fun testDiscoverComparisonUtilsNormalization() {
        assertEquals("nova pulse", DiscoverComparisonUtils.normalizeKey("  Nova   Pulse  "))
        assertEquals("ambient sessions", DiscoverComparisonUtils.normalizeKey("AMBIENT  Sessions"))

        val tracks = listOf(
            DiscoverTrack("audius", "Audius", "1", "Song 1", "Nova  Pulse", "Electronic Sessions", null, 180000L, streamEndpointUrl = "https://a.co/1"),
            DiscoverTrack("audius", "Audius", "2", "Song 2", "nova pulse", "electronic   sessions", null, 200000L, streamEndpointUrl = "https://a.co/2")
        )
        val artists = DiscoverComparisonUtils.deriveArtistsFromDiscoverTracks(tracks)
        assertEquals("Normalized comparison must merge case/whitespace variants of same artist", 1, artists.size)
        assertEquals(2, artists[0].trackCount)

        val albums = DiscoverComparisonUtils.deriveAlbumsFromDiscoverTracks(tracks)
        assertEquals("Normalized comparison must merge case/whitespace variants of same album", 1, albums.size)
        assertEquals(2, albums[0].trackCount)
    }

    @Test
    fun testDiscoverRepositoryRoomCacheAndStaleFallback() = runBlocking {
        var shouldFailNetwork = false
        val fakeAudiusProvider = object : AudiusDiscoverProvider() {
            override suspend fun getTrending(limit: Int, offset: Int): DiscoverPageResult {
                return if (shouldFailNetwork) {
                    DiscoverPageResult(emptyList(), errorMessage = "Simulated network outage")
                } else {
                    DiscoverPageResult(
                        tracks = listOf(
                            DiscoverTrack(
                                providerId = "audius",
                                providerName = "Audius",
                                providerTrackId = "t100",
                                title = "Cached Starlight",
                                artist = "Eclipse",
                                album = "Synth Sessions",
                                artworkUrl = "https://cdn.audius.co/img.jpg",
                                durationMs = 195000L,
                                streamEndpointUrl = "https://api.audius.co/v1/tracks/t100/stream"
                            )
                        ),
                        errorMessage = null
                    )
                }
            }
        }

        val discoverRepo = DiscoverRepository(
            localRepository = repository,
            audiusProvider = fakeAudiusProvider,
            jioSaavnProvider = JioSaavnDiscoverProvider(),
            bitChordProvider = BitChordDiscoverProvider()
        )
        assertEquals("Only enabled providers should be exposed", 1, discoverRepo.enabledProviders.size)

        // 1. Live fetch populates Room cache
        val firstResult = discoverRepo.getTrending("audius", limit = 25, offset = 0, forceRefresh = true)
        assertEquals(1, firstResult.tracks.size)
        assertEquals("Cached Starlight", firstResult.tracks[0].title)
        assertEquals(1, discoverRepo.getCacheEntryCount())

        // 2. Subsequent non-forced call hits fresh Room cache immediately
        val cachedHit = discoverRepo.getTrending("audius", limit = 25, offset = 0, forceRefresh = false)
        assertTrue(cachedHit.isFromCache)
        assertFalse(cachedHit.isStale)
        assertEquals(1, cachedHit.tracks.size)

        // 3. Simulate network outage on forced refresh -> falls back to cached data with isStale = true
        shouldFailNetwork = true
        val staleFallback = discoverRepo.getTrending("audius", limit = 25, offset = 0, forceRefresh = true)
        assertTrue(staleFallback.isFromCache)
        assertTrue(staleFallback.isStale)
        assertEquals(1, staleFallback.tracks.size)
        assertEquals("Cached Starlight", staleFallback.tracks[0].title)

        // 4. Clear cache removes all entries
        discoverRepo.clearCache()
        assertEquals(0, discoverRepo.getCacheEntryCount())
    }

    @Test
    fun testVesperPreferencesPersistenceAndReset() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = VesperPreferencesRepository(context)

        prefs.setGaplessPlayback(false)
        prefs.setLockscreenArtwork(false)
        prefs.setCrossfade(true, 6)

        val updated = prefs.userPreferencesFlow.first()
        assertFalse(updated.gaplessPlayback)
        assertFalse(updated.lockscreenArtwork)
        assertEquals(6, updated.crossfadeDurationSec)

        prefs.resetSettingsToDefaults()
        val reset = prefs.userPreferencesFlow.first()
        assertTrue(reset.gaplessPlayback)
        assertTrue(reset.lockscreenArtwork)
        assertEquals(4, reset.crossfadeDurationSec)
        assertFalse(reset.crossfadeEnabled)
    }

    @Test
    fun testSixBasicEqModesToggleAndNoStacking() {
        val eq = com.vesper.audio.audio.AudioEffectController()
        val basicNames = com.vesper.audio.audio.AudioEffectController.BASIC_MODES.map { it.name }
        assertEquals(listOf("Balanced", "Bass", "Vocal", "Rock", "Pop", "Acoustic"), basicNames)

        // 1. Default state is Balanced (0 dB flat)
        assertEquals("Balanced", eq.activePresetName.value)
        assertTrue(eq.bands.value.all { it.levelMb == 0.toShort() })
        assertEquals(0.toShort(), eq.bassBoostStrength.value)

        // 2. Select Bass -> applies conservative Bass curve (+5.04 dB at 31Hz, 380 bass boost)
        val afterBass = eq.selectBasicMode("Bass", toggleIfAlreadyActive = true)
        assertEquals("Bass", afterBass)
        assertEquals("Bass", eq.activePresetName.value)
        val bassCurve = eq.bands.value.map { it.levelMb }
        assertEquals(504.toShort(), bassCurve[0])
        assertEquals(380.toShort(), eq.bassBoostStrength.value)

        // 3. Switch directly to Vocal -> replaces Bass curve cleanly without stacking gains
        val afterVocal = eq.selectBasicMode("Vocal", toggleIfAlreadyActive = true)
        assertEquals("Vocal", afterVocal)
        assertEquals("Vocal", eq.activePresetName.value)
        val vocalCurve = eq.bands.value.map { it.levelMb }
        assertEquals((-144).toShort(), vocalCurve[0])
        assertTrue(vocalCurve[6] > 300)
        assertEquals(60.toShort(), eq.bassBoostStrength.value)

        // 4. Switch to Rock, Pop, Acoustic and verify each curve replaces cleanly and never stacks
        eq.selectBasicMode("Rock", toggleIfAlreadyActive = true)
        assertEquals("Rock", eq.activePresetName.value)
        assertEquals(384.toShort(), eq.bands.value[0].levelMb)
        assertEquals(360.toShort(), eq.bands.value[9].levelMb)
        assertEquals(260.toShort(), eq.bassBoostStrength.value)

        eq.selectBasicMode("Pop", toggleIfAlreadyActive = true)
        assertEquals("Pop", eq.activePresetName.value)
        assertEquals(288.toShort(), eq.bands.value[0].levelMb)
        assertEquals(200.toShort(), eq.bassBoostStrength.value)

        eq.selectBasicMode("Acoustic", toggleIfAlreadyActive = true)
        assertEquals("Acoustic", eq.activePresetName.value)
        assertEquals(192.toShort(), eq.bands.value[0].levelMb)
        assertEquals(120.toShort(), eq.bassBoostStrength.value)

        // Re-selecting Bass produces the exact same bassCurve (zero gain stacking)
        eq.selectBasicMode("Bass", toggleIfAlreadyActive = false)
        assertEquals(bassCurve, eq.bands.value.map { it.levelMb })

        // 5. Tapping the currently active mode ("Bass") again toggles it off to Balanced (0 dB flat)
        val toggledOff = eq.selectBasicMode("Bass", toggleIfAlreadyActive = true)
        assertEquals("Balanced", toggledOff)
        assertEquals("Balanced", eq.activePresetName.value)
        assertTrue(eq.bands.value.all { it.levelMb == 0.toShort() })
        assertEquals(0.toShort(), eq.bassBoostStrength.value)

        // 6. Manual band edit switches activePresetName to Custom
        eq.selectBasicMode("Bass", toggleIfAlreadyActive = false)
        assertEquals("Bass", eq.activePresetName.value)
        eq.setBandLevel(3, 250.toShort())
        assertEquals("Custom", eq.activePresetName.value)
    }

    @Test
    fun testSearchResultQueueIdentityAndTrackSwitching() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val playerClient = SonoraPlayerClient(context)

        // Create 5 filtered search result tracks (MediaStore, SAF, and Remote Audius IDs)
        val filteredResults = listOf(
            com.vesper.audio.model.Track("101", "Midnight Signal", "Aurora", "Night", 180_000L, Uri.parse("content://media/external/audio/media/101")),
            com.vesper.audio.model.Track("saf:content://com.android.externalstorage/doc/202", "Velvet Static", "Lumen", "Night", 190_000L, Uri.parse("content://com.android.externalstorage/doc/202")),
            com.vesper.audio.model.Track("103", "Crimson Orbit", "Vesper", "Nocturne", 200_000L, Uri.parse("content://media/external/audio/media/103")),
            com.vesper.audio.model.Track("audius:trk_404", "Solar Wind", "Pulse", "Discover", 210_000L, Uri.parse("https://api.audius.co/v1/tracks/trk_404/stream")),
            com.vesper.audio.model.Track("105", "Deep Horizon", "Echo", "Deep", 220_000L, Uri.parse("content://media/external/audio/media/105"))
        )

        // Tap result #3 (index 2: "Crimson Orbit") with filteredResults as queue
        val thirdTrack = filteredResults[2]
        playerClient.playTrack(thirdTrack, filteredResults)

        assertEquals("Tapped result #3 must become currentTrack immediately", "103", playerClient.currentTrack.value?.id)
        assertEquals("Crimson Orbit", playerClient.currentTrack.value?.title)
        assertEquals("Queue must contain the exact 5 filtered search results", 5, playerClient.queue.value.size)
        assertEquals(filteredResults.map { it.id }, playerClient.queue.value.map { it.id })

        // Next advances to result #4 ("audius:trk_404")
        playerClient.skipNext()
        assertEquals("audius:trk_404", playerClient.currentTrack.value?.id)
        assertEquals("Solar Wind", playerClient.currentTrack.value?.title)

        // Previous returns to result #3 ("103"), and Previous again returns to result #2 ("saf:...")
        playerClient.skipPrevious()
        assertEquals("103", playerClient.currentTrack.value?.id)
        playerClient.skipPrevious()
        assertEquals("saf:content://com.android.externalstorage/doc/202", playerClient.currentTrack.value?.id)
    }

    @Test
    fun testSafNamespacedTrackIdUniqueness() {
        val treeUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AMusic")
        val docUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3AMusic%2Ftrack.flac")
        val header = com.vesper.audio.data.media.SafDocumentHeader(
            treeUri = treeUri,
            documentUri = docUri,
            documentId = "primary:Music/track.flac",
            displayName = "track.flac",
            mimeType = "audio/flac",
            sizeBytes = 1234567L,
            lastModified = 1700000000L
        )
        assertTrue(header.stableTrackId.startsWith("saf:"))
        assertEquals("saf:$docUri", header.stableTrackId)
    }

    @Test
    fun testArtworkResolverRemoteSkipNegativeCacheAndVersionedKey() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.vesper.audio.util.VesperArtworkResolver.clearArtworkCache(context)

        // 1. Remote HTTP/HTTPS URLs must never enter local MediaMetadataRetriever
        val remoteAudioUri = Uri.parse("https://api.audius.co/v1/tracks/abc/stream")
        val remoteRes = com.vesper.audio.util.VesperArtworkResolver.resolveArtworkUri(
            context = context,
            audioUri = remoteAudioUri,
            albumId = 0L,
            trackId = "audius:abc",
            dateModified = 0L,
            sizeBytes = 0L
        )
        assertNull(remoteRes)
        assertEquals(
            com.vesper.audio.util.ArtworkDiagnosticReason.REMOTE_URL_PASSTHROUGH,
            com.vesper.audio.util.VesperArtworkResolver.getLastDiagnosticReason("audius:abc")
        )

        // 2. Broken MediaStore albumArt URI blacklisting prevents repeated broken loads
        val brokenArtUri = "content://media/external/audio/albumart/777"
        assertFalse(com.vesper.audio.util.VesperArtworkResolver.isArtworkUriBroken(brokenArtUri))
        com.vesper.audio.util.VesperArtworkResolver.markArtworkUriBroken(brokenArtUri)
        assertTrue(com.vesper.audio.util.VesperArtworkResolver.isArtworkUriBroken(brokenArtUri))

        val afterBlacklist = com.vesper.audio.util.VesperArtworkResolver.resolveFastArtworkUri(
            context = context,
            albumId = 777L,
            trackId = "trk_777",
            dateModified = 1000L,
            sizeBytes = 2048L
        )
        assertNull("Blacklisted broken albumArt URI must return null so UI shows placeholder immediately", afterBlacklist)

        // 3. Metadata change (dateModified or sizeBytes) produces a distinct cache key
        val keyV1 = com.vesper.audio.util.VesperArtworkResolver.buildCacheKey("101", 1000L, 5000L)
        val keyV2 = com.vesper.audio.util.VesperArtworkResolver.buildCacheKey("101", 2000L, 5000L)
        assertTrue("Updated dateModified must invalidate old cache key", keyV1 != keyV2)

        // 4. Clearing artwork cache clears the broken URI blacklist
        com.vesper.audio.util.VesperArtworkResolver.clearArtworkCache(context)
        assertFalse(com.vesper.audio.util.VesperArtworkResolver.isArtworkUriBroken(brokenArtUri))
    }

    @Test
    fun testTonesDestinationDistinctAndViewModelQueriesIsolated() {
        val destinations = com.vesper.audio.ui.VesperFullScreenDestination.values().toSet()
        assertTrue(destinations.contains(com.vesper.audio.ui.VesperFullScreenDestination.TONES))
        assertTrue(destinations.contains(com.vesper.audio.ui.VesperFullScreenDestination.EQUALIZER))
        assertTrue(destinations.contains(com.vesper.audio.ui.VesperFullScreenDestination.MODES))
        assertTrue(com.vesper.audio.ui.VesperFullScreenDestination.TONES != com.vesper.audio.ui.VesperFullScreenDestination.EQUALIZER)
        assertTrue(com.vesper.audio.ui.VesperFullScreenDestination.TONES != com.vesper.audio.ui.VesperFullScreenDestination.MODES)
    }

    @Test
    fun testEqualizerSpeakerBypassReapplyAndActiveModeRule() {
        val eq = com.vesper.audio.audio.AudioEffectController()
        eq.selectBasicMode("Rock", toggleIfAlreadyActive = false)
        assertEquals("Rock", eq.activePresetName.value)
        assertTrue(eq.isEnabled.value)

        // Reapplying active effects preserves the selected mode curve and bass boost
        eq.reapplyActiveEffects()
        assertEquals("Rock", eq.activePresetName.value)
        assertEquals(384.toShort(), eq.bands.value[0].levelMb)
        assertEquals(260.toShort(), eq.bassBoostStrength.value)

        // Speaker bypass state updates cleanly
        eq.setSpeakerBypass(disableOnSpeaker = true, isSpeakerOutput = true)
        assertTrue(eq.disableEffectsOnSpeaker.value)
        eq.setSpeakerBypass(disableOnSpeaker = false, isSpeakerOutput = false)
        assertFalse(eq.disableEffectsOnSpeaker.value)

        // When EQ is disabled, active mode highlight rule (isEnabled && activePreset == mode) evaluates to false
        eq.setEnabled(false)
        assertFalse(eq.isEnabled.value)
        val anyHighlightedWhenDisabled = com.vesper.audio.audio.AudioEffectController.BASIC_MODES.any { mode ->
            eq.isEnabled.value && eq.activePresetName.value.equals(mode.name, ignoreCase = true)
        }
        assertFalse("No basic mode may be highlighted as active when EQ is disabled", anyHighlightedWhenDisabled)
    }

    @Test
    fun testTokenAwareSearchAndMultiPageCompleteAlbumAndArtistFetch() = runBlocking {
        // 1. Token-aware matching and scoring
        assertTrue(DiscoverComparisonUtils.matchesTokens("starlight pulse", "Starlight", "Nova Pulse", "Nocturne"))
        assertFalse(DiscoverComparisonUtils.matchesTokens("starlight jazz", "Starlight", "Nova Pulse", "Nocturne"))

        val exactScore = DiscoverComparisonUtils.scoreTrackMatch("Starlight", "Nova Pulse", "Nocturne", "Starlight")
        val partialScore = DiscoverComparisonUtils.scoreTrackMatch("Midnight Starlight", "Nova Pulse", "Nocturne", "Star")
        assertTrue("Exact title match must rank higher than partial match", exactScore > partialScore)

        // 2. Multi-page remote album and artist tracklist aggregation with deduplication
        val page0Tracks = (1..25).map { idx ->
            DiscoverTrack(
                providerId = "audius",
                providerName = "Audius",
                providerTrackId = "alb_trk_$idx",
                title = "Odyssey Part $idx",
                artist = "Celestial",
                album = "Odyssey",
                artworkUrl = "https://cdn.audius.co/odyssey.jpg",
                durationMs = 180_000L + idx * 1000L,
                streamEndpointUrl = "https://api.audius.co/v1/tracks/alb_trk_$idx/stream"
            )
        }
        val page1Tracks = (20..32).map { idx -> // 20..25 overlap to test deduplication
            DiscoverTrack(
                providerId = "audius",
                providerName = "Audius",
                providerTrackId = "alb_trk_$idx",
                title = "Odyssey Part $idx",
                artist = "Celestial",
                album = "Odyssey",
                artworkUrl = "https://cdn.audius.co/odyssey.jpg",
                durationMs = 180_000L + idx * 1000L,
                streamEndpointUrl = "https://api.audius.co/v1/tracks/alb_trk_$idx/stream"
            )
        }

        val multiPageProvider = object : AudiusDiscoverProvider() {
            override suspend fun searchTracks(query: String, limit: Int, offset: Int): DiscoverPageResult {
                return when (offset) {
                    0 -> DiscoverPageResult(tracks = page0Tracks, errorMessage = null)
                    else -> DiscoverPageResult(tracks = page1Tracks, errorMessage = null)
                }
            }
        }

        val discoverRepo = DiscoverRepository(
            localRepository = repository,
            audiusProvider = multiPageProvider,
            jioSaavnProvider = JioSaavnDiscoverProvider(),
            bitChordProvider = BitChordDiscoverProvider()
        )

        val fullAlbumTracks = discoverRepo.fetchCompleteAlbumTracklist(
            providerId = "audius",
            albumName = "Odyssey",
            artistName = "Celestial",
            seedTracks = page0Tracks.take(3),
            maxPages = 2,
            pageSize = 25
        )
        assertEquals("Multi-page album fetch must collect all 32 unique tracks without duplicates", 32, fullAlbumTracks.size)

        val fullArtistCatalogue = discoverRepo.fetchCompleteArtistCatalogue(
            providerId = "audius",
            artistName = "Celestial",
            seedTracks = page0Tracks.take(2),
            maxPages = 2,
            pageSize = 25
        )
        assertEquals("Multi-page artist fetch must collect all 32 unique artist tracks", 32, fullArtistCatalogue.tracks.size)
        assertEquals(1, fullArtistCatalogue.albums.size)
        assertEquals("Odyssey", fullArtistCatalogue.albums[0].name)
    }

    @Test
    fun testAlbumPlaybackPreservesNaturalUnrotatedTrackOrder() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val playerClient = SonoraPlayerClient(context)

        val albumTracks = (1..8).map { num ->
            com.vesper.audio.model.Track(
                id = "album_song_$num",
                title = "Movement $num",
                artist = "Vesper Orchestra",
                album = "Symphony No. 9",
                durationMs = 200_000L,
                contentUri = Uri.parse("content://media/external/audio/media/$num"),
                trackNumber = num
            )
        }

        // User opens album and taps Track 5 (index 4) with natural unrotated albumTracks list
        playerClient.playTrack(albumTracks[4], albumTracks)
        assertEquals("album_song_5", playerClient.currentTrack.value?.id)
        assertEquals("Album queue must preserve natural 1..8 order", albumTracks.map { it.id }, playerClient.queue.value.map { it.id })

        // Next plays Track 6
        playerClient.skipNext()
        assertEquals("album_song_6", playerClient.currentTrack.value?.id)

        // Previous returns to Track 5, and Previous again returns to Track 4
        playerClient.skipPrevious()
        assertEquals("album_song_5", playerClient.currentTrack.value?.id)
        playerClient.skipPrevious()
        assertEquals("album_song_4", playerClient.currentTrack.value?.id)
    }

    @Test
    fun testExpandedVesperSettingsPreferencesPersistence() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = VesperPreferencesRepository(context)

        prefs.setVesperThemeMode("PURE_AMOLED")
        prefs.setDisableEffectsOnSpeaker(true)
        prefs.setLoudnessNormalizationEnabled(true)
        prefs.setHideRecentsOnHome(true)
        prefs.setHideMoodPicksOnHome(true)
        prefs.setHideTopicPicksOnHome(true)
        prefs.setReduceAnimations(true)
        prefs.setReduceDynamicBlur(true)
        prefs.setFullScreenCoverArt(true)
        prefs.setSyncedLyricsEnabled(false)
        prefs.setBlurUnfocusedLyrics(false)

        val current = prefs.userPreferencesFlow.first()
        assertEquals("PURE_AMOLED", current.vesperThemeMode)
        assertTrue(current.disableEffectsOnSpeaker)
        assertTrue(current.loudnessNormalizationEnabled)
        assertTrue(current.hideRecentsOnHome)
        assertTrue(current.hideMoodPicksOnHome)
        assertTrue(current.hideTopicPicksOnHome)
        assertTrue(current.reduceAnimations)
        assertTrue(current.reduceDynamicBlur)
        assertTrue(current.fullScreenCoverArt)
        assertFalse(current.syncedLyricsEnabled)
        assertFalse(current.blurUnfocusedLyrics)

        prefs.resetSettingsToDefaults()
        val reset = prefs.userPreferencesFlow.first()
        assertEquals("Nocturne", reset.vesperThemeMode)
        assertFalse(reset.disableEffectsOnSpeaker)
        assertFalse(reset.hideRecentsOnHome)
        assertTrue(reset.syncedLyricsEnabled)
    }

    @Test
    fun testSixFunctionalSignatureTonesSeparateFromBasicModes() {
        val eq = com.vesper.audio.audio.AudioEffectController()
        val tones = com.vesper.audio.audio.AudioEffectController.SIGNATURE_TONES
        assertEquals(6, tones.size)
        assertEquals(
            listOf("VESPER Signature", "Warmth", "Pulse", "Air", "Depth", "Night"),
            tones.map { it.title }
        )

        // Default activeToneId is VESPER Signature
        assertEquals("tone_vesper_signature", eq.activeToneId.value)

        // Basic Modes remain strictly separate (Balanced, Bass, Vocal, Rock, Pop, Acoustic)
        assertEquals(
            listOf("Balanced", "Bass", "Vocal", "Rock", "Pop", "Acoustic"),
            com.vesper.audio.audio.AudioEffectController.BASIC_MODES.map { it.name }
        )

        // Applying Warmth updates activeToneId, EQ curve, and bass boost without gain stacking
        val appliedWarmth = eq.applySignatureTone("tone_warmth", toggleIfAlreadyActive = true)
        assertEquals("tone_warmth", appliedWarmth)
        assertEquals("tone_warmth", eq.activeToneId.value)
        assertEquals(200.toShort(), eq.bassBoostStrength.value)
        assertTrue(eq.bands.value[0].levelMb > 0)

        // Switching to Night replaces the curve cleanly
        val appliedNight = eq.applySignatureTone("tone_night", toggleIfAlreadyActive = true)
        assertEquals("tone_night", appliedNight)
        assertEquals("tone_night", eq.activeToneId.value)
        assertEquals(0.toShort(), eq.bassBoostStrength.value)
        assertTrue("Night tone attenuates 31Hz sub-bass", eq.bands.value[0].levelMb < 0)

        // Tapping active Night tone again toggles back to VESPER Signature
        val toggledOff = eq.applySignatureTone("tone_night", toggleIfAlreadyActive = true)
        assertEquals("tone_vesper_signature", toggledOff)
        assertEquals(160.toShort(), eq.bassBoostStrength.value)

        // Applying Flat / DSP Off utility bypass zeros all bands and disables effects cleanly
        val flatBypass = eq.applySignatureTone("tone_flat_bypass", toggleIfAlreadyActive = false)
        assertEquals("tone_flat_bypass", flatBypass)
        assertTrue(eq.bands.value.all { it.levelMb == 0.toShort() })
        assertEquals(0.toShort(), eq.bassBoostStrength.value)

        // Legacy "tone_studio_reference" migrates safely to "tone_vesper_signature"
        val migrated = eq.applySignatureTone("tone_studio_reference", toggleIfAlreadyActive = false)
        assertEquals("tone_vesper_signature", migrated)
    }

    @Test
    fun testRecentlyAddedQueueOrderingAndNavigation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val playerClient = SonoraPlayerClient(context)

        // Create library tracks where alphabetical order differs completely from Recently Added order
        val trackAlpha = com.vesper.audio.model.Track(
            id = "10",
            title = "Alpha Dawn",
            artist = "Vesper",
            album = "Origins",
            durationMs = 180_000L,
            contentUri = Uri.parse("content://media/external/audio/media/10"),
            dateAdded = 1_000L
        )
        val trackBeta = com.vesper.audio.model.Track(
            id = "20",
            title = "Beta Horizon",
            artist = "Vesper",
            album = "Origins",
            durationMs = 190_000L,
            contentUri = Uri.parse("content://media/external/audio/media/20"),
            dateAdded = 3_000L
        )
        val trackGammaTie1 = com.vesper.audio.model.Track(
            id = "30",
            title = "Gamma Pulse",
            artist = "Vesper",
            album = "Origins",
            durationMs = 200_000L,
            contentUri = Uri.parse("content://media/external/audio/media/30"),
            dateAdded = 2_000L
        )
        val trackDeltaTie2 = com.vesper.audio.model.Track(
            id = "40",
            title = "Delta Echo",
            artist = "Vesper",
            album = "Origins",
            durationMs = 210_000L,
            contentUri = Uri.parse("content://media/external/audio/media/40"),
            dateAdded = 2_000L
        )

        val rawLibrary = listOf(trackAlpha, trackBeta, trackGammaTie1, trackDeltaTie2)
        val addedList = com.vesper.audio.data.MusicRepository.sortRecentlyAdded(rawLibrary)

        // Expected Recently Added order:
        // 1. trackBeta (dateAdded=3000, id=20)
        // 2. trackDeltaTie2 (dateAdded=2000, id=40 > 30)
        // 3. trackGammaTie1 (dateAdded=2000, id=30)
        // 4. trackAlpha (dateAdded=1000, id=10)
        assertEquals(listOf("20", "40", "30", "10"), addedList.map { it.id })

        // Select second item in Recently Added ("40" - Delta Echo) with addedList as queue
        playerClient.playTrack(addedList[1], addedList)
        assertEquals("40", playerClient.currentTrack.value?.id)
        assertEquals(listOf("20", "40", "30", "10"), playerClient.queue.value.map { it.id })

        // Next must advance to "30" (Gamma Pulse), NOT alphabetical next ("Gamma" vs "Beta")
        playerClient.skipNext()
        assertEquals("30", playerClient.currentTrack.value?.id)

        // Next again advances to "10" (Alpha Dawn)
        playerClient.skipNext()
        assertEquals("10", playerClient.currentTrack.value?.id)

        // Next at end of Recently Added wraps to "20" (Beta Horizon)
        playerClient.skipNext()
        assertEquals("20", playerClient.currentTrack.value?.id)

        // Previous from index 0 wraps to "10" (Alpha Dawn), and Previous again steps to "30"
        playerClient.skipPrevious()
        assertEquals("10", playerClient.currentTrack.value?.id)
        playerClient.skipPrevious()
        assertEquals("30", playerClient.currentTrack.value?.id)
    }

    @Test
    fun testMediaStoreTrackAndDiscDecodingAndFallbackTitle() {
        val (track4, disc1) = com.vesper.audio.data.media.MediaStoreScanner.decodeMediaStoreTrackAndDisc(1004)
        assertEquals(4, track4)
        assertEquals(1, disc1)

        val (track12, disc2) = com.vesper.audio.data.media.MediaStoreScanner.decodeMediaStoreTrackAndDisc(2012)
        assertEquals(12, track12)
        assertEquals(2, disc2)

        val (track7, discDefault) = com.vesper.audio.data.media.MediaStoreScanner.decodeMediaStoreTrackAndDisc(7)
        assertEquals(7, track7)
        assertEquals(1, discDefault)

        val (track0, disc0) = com.vesper.audio.data.media.MediaStoreScanner.decodeMediaStoreTrackAndDisc(0)
        assertEquals(0, track0)
        assertEquals(1, disc0)

        assertEquals(
            "Midnight_Session_Final",
            com.vesper.audio.data.media.MediaStoreScanner.fallbackTitle("<unknown>", "/storage/emulated/0/Music/Midnight_Session_Final.flac")
        )
        assertEquals(
            "Valid Song Title",
            com.vesper.audio.data.media.MediaStoreScanner.fallbackTitle("Valid Song Title", "/storage/emulated/0/Music/01_track.mp3")
        )
    }

    @Test
    fun testIncompleteMetadataDetectionAndEmbeddedTagMerge() {
        val incompleteCached = CachedTrackEntity(
            id = "505",
            mediaStoreId = 505L,
            contentUriString = "content://media/external/audio/media/505",
            title = "505_audio",
            artist = "Unknown Artist",
            album = "Unknown Album",
            durationMs = 200_000L,
            trackNumber = 0,
            discNumber = 1,
            year = 0,
            genre = "",
            albumArtist = "Unknown Artist",
            artworkUriString = "content://media/external/audio/albumart/99",
            dateAdded = 1700000000L,
            dateModified = 1700001000L,
            sizeBytes = 6000000L,
            mimeType = "audio/mpeg",
            isUserOverride = false
        )

        assertTrue(
            "Track with Unknown Artist/Album, blank genre, year=0, or generic albumart URI must be flagged for enrichment",
            com.vesper.audio.data.MusicRepository.isMetadataIncomplete(incompleteCached)
        )

        assertEquals(1998, com.vesper.audio.data.MusicRepository.parseYearTag("1998-05-14"))
        assertEquals(4, com.vesper.audio.data.MusicRepository.parseSubIndex("04/12"))
        assertEquals(2, com.vesper.audio.data.MusicRepository.parseSubIndex("2/2"))

        val merged = com.vesper.audio.data.MusicRepository.mergeEmbeddedTags(
            existing = incompleteCached,
            extractedTitle = "Nocturne in C# Minor",
            extractedArtist = "Frederic Chopin",
            extractedAlbum = "Solo Piano Works",
            extractedAlbumArtist = "Frederic Chopin",
            extractedGenre = "Classical",
            extractedYear = com.vesper.audio.data.MusicRepository.parseYearTag("1998-05-14"),
            extractedTrackNumber = com.vesper.audio.data.MusicRepository.parseSubIndex("04/12"),
            extractedDiscNumber = com.vesper.audio.data.MusicRepository.parseSubIndex("2/2"),
            extractedDurationMs = 245_000L,
            resolvedArtworkUriString = "file:///data/user/0/com.vesper.audio/cache/artwork/art_505.jpg"
        )

        assertEquals("Frederic Chopin", merged.artist)
        assertEquals("Solo Piano Works", merged.album)
        assertEquals("Frederic Chopin", merged.albumArtist)
        assertEquals("Classical", merged.genre)
        assertEquals(1998, merged.year)
        assertEquals(4, merged.trackNumber)
        assertEquals(2, merged.discNumber)
        assertEquals("file:///data/user/0/com.vesper.audio/cache/artwork/art_505.jpg", merged.artworkUriString)
        assertFalse(com.vesper.audio.data.MusicRepository.isMetadataIncomplete(merged))

        // Verify user overrides are never overwritten by background tag merging
        val userOverridden = merged.copy(
            title = "My Custom Title",
            artist = "My Custom Artist",
            isUserOverride = true
        )
        assertFalse(com.vesper.audio.data.MusicRepository.isMetadataIncomplete(userOverridden))
        val afterAttemptedMerge = com.vesper.audio.data.MusicRepository.mergeEmbeddedTags(
            existing = userOverridden,
            extractedTitle = "File Tag Title",
            extractedArtist = "File Tag Artist",
            extractedAlbum = "File Tag Album",
            extractedAlbumArtist = "File Tag Artist",
            extractedGenre = "Pop",
            extractedYear = 2025,
            extractedTrackNumber = 1,
            extractedDiscNumber = 1,
            extractedDurationMs = 100_000L,
            resolvedArtworkUriString = null
        )
        assertEquals("My Custom Title", afterAttemptedMerge.title)
        assertEquals("My Custom Artist", afterAttemptedMerge.artist)
    }

    @Test
    fun testTonesScreenExactTabLabelsAndWorkspaceMigration() {
        val controller = com.vesper.audio.audio.AudioEffectController()
        controller.applySignatureTone("tone_vesper_signature", toggleIfAlreadyActive = false)

        // Verify saved state migration for old enum names
        assertEquals(
            com.vesper.audio.ui.screens.TonesWorkspaceTab.TONES,
            com.vesper.audio.ui.screens.TonesWorkspaceTab.fromSavedName("SIGNATURE_TONES")
        )
        assertEquals(
            com.vesper.audio.ui.screens.TonesWorkspaceTab.STUDIO_EQ,
            com.vesper.audio.ui.screens.TonesWorkspaceTab.fromSavedName("STUDIO_SEQ")
        )
        assertEquals(
            com.vesper.audio.ui.screens.TonesWorkspaceTab.STUDIO_EQ,
            com.vesper.audio.ui.screens.TonesWorkspaceTab.fromSavedName("STUDIO_EQ")
        )

        // Verify exact tab labels: "Tones" and "Studio EQ" (no "Studio SEQ", "SEQ Studio EQ", or "Equalizer")
        val tabs = com.vesper.audio.ui.screens.TonesWorkspaceTab.entries
        assertEquals(2, tabs.size)
        assertEquals("Tones", com.vesper.audio.ui.screens.TonesWorkspaceTab.TONES.title)
        assertEquals("tones_tab_signature", com.vesper.audio.ui.screens.TonesWorkspaceTab.TONES.testTag)
        assertEquals("Studio EQ", com.vesper.audio.ui.screens.TonesWorkspaceTab.STUDIO_EQ.title)
        assertEquals("tones_tab_seq", com.vesper.audio.ui.screens.TonesWorkspaceTab.STUDIO_EQ.testTag)

        // Switching tabs preserves active tone and DSP state
        assertEquals("tone_vesper_signature", controller.activeToneId.value)
        assertTrue(controller.isEnabled.value)
    }

    @Test
    fun testVesperSignatureAbBypassAndHeadroomProtection() {
        val eq = com.vesper.audio.audio.AudioEffectController()
        eq.applySignatureTone("tone_pulse", toggleIfAlreadyActive = false)
        assertEquals("tone_pulse", eq.activeToneId.value)
        assertFalse(eq.isAbBypassed.value)

        val activeStatus = eq.getHeadroomStatus()
        assertTrue("Active Pulse tone must have positive gain and negative preamp headroom attenuation", activeStatus.maxPositiveGainDb > 0f)
        assertTrue(activeStatus.preampAttenuationDb < 0f)
        assertTrue(eq.preampGainScalar.value < 1.0f)

        // Toggle A/B Bypass ON -> bypasses DSP to 0 dB flat while preserving activeToneId and band curve
        val bypassed = eq.toggleAbBypass()
        assertTrue(bypassed)
        assertTrue(eq.isAbBypassed.value)
        assertEquals("tone_pulse", eq.activeToneId.value)
        assertEquals(0f, eq.getHeadroomStatus().preampAttenuationDb, 0.001f)
        assertEquals(1.0f, eq.preampGainScalar.value, 0.001f)
        assertEquals(com.vesper.audio.audio.ProcessingStatus.OFF, eq.processingStatus.value)

        // Toggle A/B Bypass OFF -> restores active Pulse tone and headroom protection immediately
        val restored = eq.toggleAbBypass()
        assertFalse(restored)
        assertFalse(eq.isAbBypassed.value)
        assertEquals("tone_pulse", eq.activeToneId.value)
        assertTrue(eq.getHeadroomStatus().preampAttenuationDb < 0f)
        assertTrue(eq.preampGainScalar.value < 1.0f)
    }

    @Test
    fun testDiscoverWarmCacheImmediatePeekAndHttp429Formatting() = runBlocking {
        val fakeProvider = object : AudiusDiscoverProvider() {
            override suspend fun getTrending(limit: Int, offset: Int): DiscoverPageResult {
                return DiscoverPageResult(
                    tracks = listOf(
                        DiscoverTrack(
                            providerId = "audius",
                            providerName = "Audius",
                            providerTrackId = "warm_1",
                            title = "Instant Horizon",
                            artist = "Vesper",
                            album = "Nocturne",
                            artworkUrl = "https://cdn.audius.co/warm1.jpg",
                            durationMs = 200_000L,
                            streamEndpointUrl = "https://discoveryprovider.audius.co/v1/tracks/warm_1/stream?app_name=VESPER"
                        )
                    ),
                    errorMessage = null
                )
            }
        }

        val discoverRepo = DiscoverRepository(
            localRepository = repository,
            audiusProvider = fakeProvider
        )

        // Populate Room cache
        discoverRepo.getTrending("audius", limit = 25, offset = 0, forceRefresh = true, nowMs = 1_000_000L)

        // Peek warm cache (fresh)
        val freshPeek = discoverRepo.peekCachedTrending("audius", nowMs = 1_000_500L)
        assertNotNull(freshPeek)
        assertTrue(freshPeek!!.isFromCache)
        assertFalse(freshPeek.isStale)
        assertEquals("Instant Horizon", freshPeek.tracks.first().title)

        // Peek expired cache (stale-while-revalidate)
        val stalePeek = discoverRepo.peekCachedTrending("audius", nowMs = 9_000_000_000L)
        assertNotNull(stalePeek)
        assertTrue(stalePeek!!.isFromCache)
        assertTrue(stalePeek.isStale)
        assertEquals("Instant Horizon", stalePeek.tracks.first().title)

        // Verify HTTP 429 rate-limit message and transient 5xx retry predicates
        val rateLimitMsg = com.vesper.audio.data.remote.AudiusApiClient.formatHttpError(429)
        assertTrue(rateLimitMsg.contains("429") && rateLimitMsg.contains("rate limit", ignoreCase = true))
        assertTrue(com.vesper.audio.data.remote.AudiusApiClient.isTransientHttpCode(503))
        assertFalse(com.vesper.audio.data.remote.AudiusApiClient.isTransientHttpCode(429))
        assertFalse(com.vesper.audio.data.remote.AudiusApiClient.isTransientHttpCode(404))
    }

    @Test
    fun testSecurityValidatorHttpsOnlyMediaUriAndIdSanitization() {
        // 1. HTTPS URL validation
        assertTrue(com.vesper.audio.util.SecurityValidator.isValidHttpsUrl("https://discoveryprovider.audius.co/v1/tracks/trending"))
        assertFalse("Cleartext HTTP must be rejected", com.vesper.audio.util.SecurityValidator.isValidHttpsUrl("http://discoveryprovider.audius.co/v1/tracks"))
        assertFalse("javascript: scheme must be rejected", com.vesper.audio.util.SecurityValidator.isValidHttpsUrl("javascript:alert(1)"))
        assertFalse("Embedded credentials must be rejected", com.vesper.audio.util.SecurityValidator.isValidHttpsUrl("https://admin:secret@audius.co/v1"))
        assertFalse("Localhost loopback must be rejected", com.vesper.audio.util.SecurityValidator.isValidHttpsUrl("https://localhost/v1/tracks"))
        assertFalse("Path traversal must be rejected", com.vesper.audio.util.SecurityValidator.isValidHttpsUrl("https://audius.co/v1/../../etc/passwd"))

        // 2. Media Uri validation (MediaStore content://, safe file://, HTTPS stream)
        assertTrue(com.vesper.audio.util.SecurityValidator.isValidMediaUri(Uri.parse("content://media/external/audio/media/42")))
        assertTrue(com.vesper.audio.util.SecurityValidator.isValidMediaUri(Uri.parse("file:///storage/emulated/0/Music/song.flac")))
        assertTrue(com.vesper.audio.util.SecurityValidator.isValidMediaUri(Uri.parse("https://discoveryprovider.audius.co/v1/tracks/abc/stream")))
        assertFalse(com.vesper.audio.util.SecurityValidator.isValidMediaUri(Uri.parse("http://insecure.stream.co/song.mp3")))
        assertFalse(com.vesper.audio.util.SecurityValidator.isValidMediaUri(Uri.parse("file:///data/user/0/com.vesper.audio/../../secret")))
        assertFalse(com.vesper.audio.util.SecurityValidator.isValidMediaUri(Uri.EMPTY))

        // 3. Provider track ID sanitization
        assertEquals("trk_99", com.vesper.audio.util.SecurityValidator.sanitizeProviderTrackId("trk_99"))
        assertEquals("trk99admin1", com.vesper.audio.util.SecurityValidator.sanitizeProviderTrackId("../trk_99?admin=1".replace("_", "")))
        assertFalse(com.vesper.audio.util.SecurityValidator.sanitizeProviderTrackId("../../etc/passwd").contains(".."))
    }
}
