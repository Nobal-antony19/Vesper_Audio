# VESPER Progress Ledger

## Phase 5–7 & Targeted Repair Milestone: Appearance Studio, Now Playing, Responsive Design, Library Persistence

### Focus Areas Completed
1. **Appearance Studio & Dynamic Accent Color**:
   - Replaced static `#722C42` across UI components with `LocalVesperAccentColor` dynamic provider.
   - Built 3-card background selection: None, Album art, Custom image.
   - User photo picker via `PickVisualMedia` with durable persistent storage in `context.filesDir/vesper_custom_bg.jpg`.
   - Global backdrop layer with configurable blur (0-100%) and opacity (0-100%).
   - Action buttons: "Apply" (with checkmark) and "Reset" (with restore icon).
2. **Now Playing UI & Song Overflow Menu**:
   - Top bar: Collapse, PLAYING FROM, 3-dot overflow.
   - 1:1 square borderless album art (16dp rounded corner radius, no stroke).
   - Single line title with ellipsis, artist, and favorite heart toggle.
   - Centered seek bar with 48dp minimum touch target.
   - 4 equal-width action chips: Queue, Lyrics, Equalizer, Timer (never wrap on compact widths).
   - 4-group overflow bottom sheet: Playback, Navigate, Organize, Track.
3. **Responsive Design**:
   - Compact phones (320-411dp) and wide/tablet screens (>600dp with centered `widthIn(max = 680.dp)`).
   - Bottom padding (`80.dp` - `96.dp`) on all LazyColumns to prevent mini-player occlusion.
4. **Music Library Persistence & Fast Startup**:
   - Added `CachedTrackEntity` to Room Database with `MIGRATION_3_4`.
   - Cold startup loads from Room immediately in `<50ms`.
   - Background incremental sync checks `_ID`, `DATE_MODIFIED`, and `SIZE` headers, skipping re-processing for unchanged tracks.
   - Diagnostic logging under tag `VesperScan`.

### Test Status
- `compile_applet`: PASS
- `gradle :app:testDebugUnitTest`: PASS (All tests passing)


## VESPER 2.8.0 Upgrade (One-Go Upgrade Milestone)

### Implemented & Repaired
1. **R1: Search Playback & Live Queue Source**:
   - `SearchScreen.kt`: Debounced ~200ms `_searchQuery` running on `Dispatchers.Default`.
   - Result clicking initiates immediate playback from that list in displayed order.
   - Sets `queueSource = "Search: \"<query>\""`.
   - Now Playing header dynamically reflects `"Search: \"<query>\""`.
   - Current playing track highlighted in search results with animated playing indicator.
2. **R2: Shared Song Row, Single Heart Source of Truth & Alignment**:
   - Fixed 64dp height, 16dp horizontal padding.
   - Exact layout: `[art 48dp] [title/artist weight 1] [duration] [heart 40dp] [⋮ 40dp]`.
   - Unified `favoriteTrackIds: StateFlow<Set<String>>` with optimistic updates across all screens.
   - Heart button isolated from row click.
   - A–Z rail restricted to 20dp touch strip with end gutter padding preventing overlap.
3. **R3: Oto-Music Style Home Screen**:
   - Removed legacy "All Songs" block from Home.
   - Real section item counts on header pills.
   - Quick Action Tiles (Favorites, History, Stats, Shuffle).
   - Recently Added horizontal row (`dateAdded DESC`, max 20, "See all").
   - Albums Carousel.
   - Favorites compact cards (`addedAt ASC`, oldest first, "View all").
   - Recently Played shown conditionally when listening history exists.
4. **R4: Appearance Studio Theming, Modes, and Backdrop**:
   - Centralized `LocalVesperColors` and `LocalVesperAccentColor`.
   - Dynamic luminance-aware `onAccent`.
   - Real Light / Dark / System theme switching via Material 3 `ColorScheme`.
   - Staged Draft architecture with Apply and Reset buttons.
   - Photo Picker downsamples to $\le 1440$px JPEG q85 in `filesDir/backgrounds/bg_<epoch>.jpg` with decode verification.
   - Root `VesperBackdrop` with blur, transparency scrim, and transparent container screens.
5. **R5: Speed, Smooth Scrolling & Memory**:
   - Lazy list keys and content types (`contentType = { "track" }`).
   - Sizing constraints on Coil image loading.
   - ViewModel background thread dispatchers.
6. **R6: Startup & Splash Dismissal**:
   - Dismisses immediately upon first Room track emission without fixed artificial delays (capped at 600ms).
7. **R7: Downloader Engine with Free and Supporter Tiers**:
   - Room `DownloadEntity` and `downloads` table with `MIGRATION_4_5`.
   - `VesperDownloadManager` streaming to `.part`, verifying with `MediaMetadataRetriever`, publishing to MediaStore `Music/VESPER/`.
   - `DownloadEntitlements` interface: Free (1 concurrent, 5 queue) vs Supporter (4 concurrent, 200 queue, batch, resume, schedule).
   - Developer toggle for supporter mode in debug builds.
8. **R8: Navigation & Overlap**:
   - 96dp bottom padding on list content preventing mini-player and bottom dock occlusion.
   - Complete callbacks for all menus and dialogs.
