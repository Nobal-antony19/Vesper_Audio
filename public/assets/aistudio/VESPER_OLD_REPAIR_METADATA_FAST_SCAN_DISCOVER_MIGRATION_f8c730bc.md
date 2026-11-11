# VESPER OLD — PERFORMANCE, METADATA, NAVIGATION & DISCOVER MIGRATION SPEC
## Repair the existing OLD VESPER first; port only the New VESPER Discover feature

**Target codebase:** `OLD1(1).zip` (the currently supplied old VESPER project)  
**Feature donor:** New VESPER Audio project / its unified Discover implementation  
**Priority order:** 1) scan/parser speed, 2) metadata restoration, 3) smoothness/stability, 4) hamburger navigation, 5) Discover feature migration  
**Navigation drawer order:** About → Settings → Equalizer → Tones → Scan media → Donate  
**Product constraints:** Preserve old VESPER's existing UI identity and working local playback. Keep audio architecture semantics correct: Tones are immersive audio architectures; EQ Modes are frequency presets.

---

# 1. Mission and non-negotiable rules

Repair the supplied OLD VESPER Android app. Do not rebuild it from scratch and do not replace its UI with a generic template.

The objective is to:
- Make initial library loading feel immediate.
- Stop expensive metadata extraction from blocking the first usable library view.
- Restore and preserve metadata features already present in the old project.
- Eliminate avoidable UI jank, recomposition churn and unstable scrolling.
- Rebuild the side-scroll / hamburger drawer in the exact requested order.
- Port the **Discover feature only** from New VESPER Audio into Old VESPER.
- Keep local music and remote Discover catalogues strictly separate.
- Preserve the existing Media3/player, Room, DataStore, favourites, playlists, queue, audio settings and user data.
- Avoid adding heavy dependencies or large bundled assets.
- Build and test the actual Android project before claiming completion.

## 1.1 Absolute safety rules

1. Make a full backup of the supplied project before edits.
2. Never delete or recreate the Room database to fix a UI or parser problem.
3. Do not change application ID, namespace, signing config or package structure without a demonstrated need.
4. Do not replace existing metadata fields with defaults during a scan.
5. Never block the main thread with MediaStore, SAF, file I/O, artwork decoding or metadata extraction.
6. Do not instantiate `MediaMetadataRetriever` for every track on every scan.
7. Do not rescan every SAF tree on every ordinary startup if its contents have not changed.
8. Do not put remote Discover results into the local `tracks`, `albums`, `artists`, Songs or local Library flows.
9. Do not use mock songs to hide an empty remote result.
10. Do not create a second ExoPlayer or MediaSession for Discover.
11. No fake success: report exactly what built, what was tested, what failed and what remains unverified.

---

# 2. Actual OLD project audit — observed source structure

The following findings are based on the supplied `OLD1(1).zip`.

## 2.1 Relevant files found

| File | Responsibility |
|---|---|
| `app/src/main/java/com/example/data/MusicRepository.kt` | Local track flows, startup sync, MediaStore/SAF reconciliation and Room cache |
| `app/src/main/java/com/example/data/media/MediaStoreScanner.kt` | MediaStore audio scanning and grouping |
| `app/src/main/java/com/example/data/media/SafFolderScanner.kt` | Recursive SAF tree traversal and metadata extraction |
| `app/src/main/java/com/example/data/local/SonoraLocalRepository.kt` | Room-backed local persistence |
| `app/src/main/java/com/example/data/local/VesperPreferencesRepository.kt` | DataStore preferences |
| `app/src/main/java/com/example/ui/SonoraViewModel.kt` | UI state, navigation destinations, playback and library actions |
| `app/src/main/java/com/example/ui/screens/SearchScreen.kt` | Existing Discover/Search screen |
| `app/src/main/java/com/example/ui/screens/MetadataStudioScreen.kt` | Existing single/batch metadata editing UI |
| `app/src/main/java/com/example/model/Track.kt` | Track model |
| `app/src/main/java/com/example/audio/AudioEffectController.kt` | Audio effect handling |

The project already has a `MetadataStudioScreen`; metadata functionality must be repaired and reconnected, not discarded or replaced by a simplified screen.

## 2.2 Existing navigation state

`SonoraViewModel.kt` currently defines:

```kotlin
enum class MainDestination {
    HOME,
    SONGS,
    ALBUMS,
    ARTISTS,
    DISCOVER;

    companion object {
        val SEARCH = DISCOVER
    }
}
```

It also defines full-screen destinations including:
- `EQUALIZER`
- `MODES`
- `DONATE`
- `METADATA_STUDIO`
- `SETTINGS`
- `ABOUT`
- `SCAN_MEDIA`
- `QUEUE`
- `ALBUM_DETAILS`
- `ARTIST_DETAILS`
- `PLAYLIST_DETAILS`
- `FAVORITES`

Do not move these into the bottom navigation. The user's requested six drawer entries are navigation actions to existing destinations/features.

## 2.3 Existing scan behaviour observed

`MusicRepository`:
- Observes Room cached tracks into `_rawTracks`.
- Starts `syncLibraryIncremental(forceFullRescan = false, reason = "startup_check")` during repository initialisation.
- Uses a `Mutex` to prevent overlapping scans.
- Queries MediaStore headers and compares `dateModified` and `sizeBytes` against cached rows.
- Inserts changed/new tracks in chunks of 500.
- Conservatively avoids deleting cached tracks when MediaStore returns zero results.
- Also scans all persisted SAF folders during the sync.
- Uses `SafFolderScanner.scanDirectory()` for each persisted tree.
- The current SAF reconciliation checks a composite title/artist/duration key.

`MediaStoreScanner` already has a header scanning path. Preserve and build on it.

`SafFolderScanner` recursively enumerates SAF documents and calls `MediaMetadataRetriever` for each supported audio file. This is a likely source of slow SAF rescans, especially for large trees or cloud-backed document providers.

The current SAF scanner extracts title, artist, album, duration, track number, disc number, year, genre, album artist, MIME type and artwork URI. Do not lose these fields.

---

# 3. Fix the parser/scanner speed — highest priority

## 3.1 Define “fast startup” correctly

Do not make startup wait for every track's complete metadata and artwork.

Required user experience:
1. App opens.
2. Existing Room-cached library appears immediately.
3. User can scroll, search, play and open the player immediately.
4. A small non-blocking status indicator says `Updating library…` only while background sync is active.
5. New/changed files are progressively added.
6. Metadata enrichment continues off the main thread.
7. A scan failure leaves the previous usable library intact.

Target behaviour:
- Cached library first frame must not depend on network.
- No full MediaMetadataRetriever pass on every launch.
- No full SAF recursion on every ordinary launch unless invalidation requires it.
- No large synchronous mapping/grouping operation in a Composable.

## 3.2 Split scanning into two phases

### Phase A — fast inventory scan

Use MediaStore's indexed metadata wherever possible.

Query only the columns needed for inventory:
- `_ID`
- `TITLE`
- `ARTIST`
- `ALBUM`
- `ALBUM_ID`
- `DURATION`
- `TRACK`
- `YEAR`
- `DATE_ADDED`
- `DATE_MODIFIED`
- `SIZE`
- `MIME_TYPE`
- `ALBUM_ARTIST` where supported

Build `Track`/cache entities directly from this header information.

Do not call `MediaMetadataRetriever` for MediaStore rows whose metadata is already available in the provider index.

Use a stable MediaStore content URI:
`content://media/external/audio/media/{id}` or the correct volume URI for the device.

Do not use deprecated filesystem paths as the primary identity.

### Phase B — selective enrichment

Only enrich a file when:
- A required metadata field is missing/unknown.
- The file is new or has changed size/dateModified.
- The user explicitly requests metadata refresh.
- A metadata operation is being performed in Metadata Studio.

Enrichment fields may include:
- Embedded title/artist/album
- Album artist
- Genre
- Year
- Track/disc numbers
- Composer where available
- Embedded artwork or artwork extraction
- Bitrate/sample rate/channel information only if the product actually displays it and the extraction is efficient

Run enrichment on `Dispatchers.IO` with a bounded concurrency limit. Do not launch one coroutine per file without a cap.

Suggested worker design:
- One bounded queue.
- 2–4 concurrent metadata jobs initially; tune with profiling.
- Cancellation when a scan is superseded.
- A single retriever instance per active job; always release it in `finally`.
- Per-file timeout/failure handling.
- Continue after corrupt or unsupported files.
- Persist each completed enrichment in small batches.
- Do not re-enrich unchanged files without a reason.

Do not assume 2–4 is universally optimal; profile on a low/mid-range Android device.

## 3.3 SAF scanning must become incremental

Current `SafFolderScanner.scanDirectory()` recursively queries the whole tree and opens every audio file through `MediaMetadataRetriever`.

Replace the unconditional full-tree extraction with a two-step SAF scan:

**SAF inventory**
- Enumerate document ID, display name, MIME type, size and last-modified.
- Build a stable key from persisted tree URI + document ID.
- Skip hidden files and unsupported types.
- Do not open the file's media stream during inventory.
- Preserve folder hierarchy if the app exposes folder views.

**SAF metadata extraction**
- Extract metadata only for new or changed documents.
- Use document ID + size + lastModified for incremental comparison.
- Persist a SAF-specific stable source key; do not use a fragile title/artist/duration composite as the sole identity.
- Keep the persisted URI permission.
- Handle providers that return null/zero lastModified or size conservatively.
- For providers with unreliable timestamps, use an explicit user-triggered full rescan rather than rescanning everything at startup.

Do not delete cached SAF rows because a cloud provider temporarily returned an incomplete tree.

## 3.4 Stable IDs and deduplication

Current SAF track IDs are derived from `uri.toString().hashCode()` in the track extraction path, while the track `id` itself uses the URI string. Standardise stable identity.

Preferred identity:
- MediaStore: volume + MediaStore ID.
- SAF: persisted tree URI + document ID.
- Imported external file: canonical content URI if stable.

Do not deduplicate different songs only because title, artist and duration match. Two legitimate recordings can share these fields.

For cross-source duplicates, use a conservative optional duplicate suggestion, not automatic destructive deletion.

## 3.5 Avoid expensive work in repository flow transforms

Current repository builds `albums` and `artists` by grouping the entire `tracks` list whenever the tracks flow emits.

Improve:
- Keep grouping off the main dispatcher.
- Use `mapLatest`/`flowOn(Dispatchers.Default)` where appropriate.
- Cache grouping results by library revision.
- Avoid recomputing groups for favourite-only changes.
- Keep favourite state separate from expensive album/artist grouping where practical.
- Use stable IDs and immutable result lists.
- Avoid repeated `associateBy`/full-list copies in UI recomposition paths.

## 3.6 Avoid duplicate scans

- Keep a single scan coordinator.
- Preserve mutex protection.
- Distinguish startup check, manual rescan, SAF permission change and metadata refresh.
- Debounce repeated manual scan taps.
- Cancel stale work safely.
- Do not start a second scan because a screen recomposed.
- Do not scan from a Composable `LaunchedEffect` unless the event is explicitly user-driven and keyed correctly.
- Show scan progress from repository state, not local fake timers.

## 3.7 Correct short-track filtering

Current `MusicRepository.tracks` filters short tracks using a minimum duration of 30 seconds when `filterShortTracks` is enabled, but retains unknown-duration rows and demo rows. Make this preference visible and testable.

Requirements:
- Preserve user preference.
- Unknown duration must not cause a valid track to disappear.
- Do not hide short music tracks silently if the user disabled the filter.
- Keep spoken-word/audio files if the app claims to support them.
- Do not classify solely by duration as music vs non-music.

---

# 4. Restore metadata features — do not lose existing Metadata Studio

## 4.1 Preserve the existing Metadata Studio screen

The supplied project contains:
`app/src/main/java/com/example/ui/screens/MetadataStudioScreen.kt`

It supports:
- Single-track metadata editing.
- Batch metadata editing.
- Selecting a track.
- Saving title, artist and album.
- Batch operations over selected track IDs.
- Genre/year/track-number inputs in its form.

Keep this feature available through the existing app route and track context menus.

## 4.2 Restore metadata access points

Metadata actions should be available from:
- Track three-dot menu.
- Now Playing overflow menu.
- Album/track details where relevant.
- Metadata Studio drawer/settings route if one already exists.
- Multi-select action bar for batch metadata editing.

Track menu action label: `Edit metadata`.

## 4.3 Metadata fields

Support fields already represented by the model or UI:
- Title
- Artist
- Album
- Album artist
- Genre
- Year
- Track number
- Disc number
- Artwork
- MIME type / file type (read-only)
- Duration (read-only unless file rewriting is explicitly implemented)
- File size (read-only)
- Source URI (read-only)

Do not expose fields as editable if the save path cannot persist them.

## 4.4 Save behaviour

Metadata edits must:
1. Validate inputs.
2. Save to the actual source file only when Android permissions and format support allow it.
3. Otherwise save a VESPER library override in Room and clearly label it as an app-side metadata override.
4. Update Room cache immediately after a successful save.
5. Update track, album and artist flows.
6. Preserve favourite, playlist, history and queue references.
7. Refresh artwork cache keys when artwork changes.
8. Never silently claim the embedded file tags changed if only the app database was updated.

## 4.5 Metadata provider/source distinction

Clearly distinguish:
- Embedded metadata from file tags.
- MediaStore-indexed metadata.
- VESPER user override.
- Online metadata lookup result.

Priority recommendation:
1. User override.
2. Embedded/MediaStore metadata.
3. Optional online enrichment.
4. Filename fallback.

Do not overwrite a user's manual metadata override during the next library scan.

## 4.6 Artwork recovery

- Use existing `VesperArtworkResolver` before inventing a new artwork pipeline.
- Prefer MediaStore album art and embedded artwork already available.
- Cache thumbnails at the size required by the UI.
- Avoid repeatedly extracting the same embedded image.
- Use a cache key incorporating stable track identity and modification timestamp.
- Invalidate only when the underlying file changes or user changes artwork.
- Avoid decoding full-size covers in list rows.
- Keep fallback artwork visually consistent with VESPER.
- Do not block scrolling while artwork loads.

## 4.7 Metadata Studio QA

- [ ] Single edit saves and persists after restart.
- [ ] Batch edit changes only selected tracks.
- [ ] Metadata override survives a rescan.
- [ ] Favourites and playlists remain intact.
- [ ] Artwork refreshes without stale cache.
- [ ] Unsupported file writes are explained.
- [ ] Corrupt file metadata does not crash scan.
- [ ] Empty artist/album values are handled consistently.
- [ ] Unknown metadata does not overwrite valid existing data.

---

# 5. Smooth scrolling and UI performance across the app

## 5.1 Main-thread rule

No disk, provider, database, artwork decode, remote network or audio metadata extraction on the main thread.

Use:
- `Dispatchers.IO` for provider/file/database work.
- `Dispatchers.Default` for CPU-heavy grouping, sorting and transformations.
- Main dispatcher only for UI state delivery.

Do not wrap every operation in `withContext(IO)` blindly; put the work at the correct layer.

## 5.2 Compose list rules

For every `LazyColumn`, `LazyRow` and grid:
- Provide stable `key` values.
- Provide `contentType` where mixed item types exist.
- Avoid using list index as identity.
- Keep item Composables small.
- Do not pass the entire library into each row.
- Avoid nested vertical scrolling unless designed and measured.
- Use `rememberLazyListState` and preserve state per destination.
- Avoid resetting scroll position on unrelated state changes.
- Do not create new sorted lists in every recomposition.
- Precompute/filter with `remember` or ViewModel state where appropriate.
- Avoid expensive artwork blur inside each list item.
- Avoid unbounded `animate*AsState` across hundreds of rows.

## 5.3 Playback state isolation

Playback position can update frequently. Do not combine a 100ms playback ticker with the entire library UI state if it causes all rows/screens to recompose.

- Keep current track ID separate.
- Keep playback position confined to the player/progress component.
- Use `derivedStateOf` for formatted time where useful.
- Update list rows only when their own playing/favourite state changes.
- Do not write playback position to Room every tick.

## 5.4 Artwork performance

- Request artwork at display size.
- Use Coil's memory and disk cache.
- Avoid repeated `ImageRequest` creation from unstable parameters.
- Use stable artwork URIs/cache keys.
- Avoid loading full-resolution art for 48dp rows.
- Precompute ambient blur only for the active player track.
- Do not keep all decoded album images in memory.
- Show placeholders with fixed dimensions to prevent layout jumps.

## 5.5 Navigation and animation

- Avoid recomposing the full app for drawer open/close.
- Use one drawer state owner.
- Animate drawer offset/alpha with bounded properties.
- Avoid multiple nested infinite transitions.
- Respect system window insets.
- Prevent drawer animation from changing the underlying screen's scroll state.
- Avoid expensive blur/backdrop effects over large full-screen content on low-end devices.
- Prefer restrained alpha/gradient/border effects over multiple real-time blur layers.

## 5.6 Profile before and after

Use Android Studio:
- Layout Inspector.
- Compose recomposition counts.
- System Trace / Perfetto.
- Memory Profiler.
- CPU Profiler.
- Macrobenchmark if project setup permits.

Measure:
- Cold launch to first usable Home.
- First library load from Room.
- Full MediaStore scan.
- SAF scan for a large folder.
- Scroll through 1,000+ tracks.
- Artwork-heavy album grid scroll.
- Drawer open/close.
- Track tap to playback.
- Search typing latency.

Do not claim smooth 60 FPS without a measured trace on a named device.

---

# 6. Hamburger / side-scroll navigation redesign

## 6.1 Exact requested order

The drawer must contain exactly these primary actions in this order:

1. **About**
2. **Settings**
3. **Equalizer**
4. **Tones**
5. **Scan media**
6. **Donate**

Do not reorder these based on frequency or feature grouping. Do not insert other items between them.

## 6.2 Recommended visual structure

Top:
- VESPER wordmark.
- Small `NOCTURNE` subtitle.
- Optional current app version.
- Restrained burgundy glow, not a giant bright banner.

Then six menu rows in exact order.

Bottom:
- Small privacy/local-first note only if space allows.
- No seventh navigation action.
- Use a separate close affordance if needed; it is not a menu entry.

## 6.3 Destination mapping

| Drawer item | Existing destination/action |
|---|---|
| About | `VesperFullScreenDestination.ABOUT` |
| Settings | `VesperFullScreenDestination.SETTINGS` |
| Equalizer | `VesperFullScreenDestination.EQUALIZER` |
| Tones | Existing tone destination / `MODES` only if it is truly the Tone screen; correct the route name if needed |
| Scan media | `VesperFullScreenDestination.SCAN_MEDIA` |
| Donate | `VesperFullScreenDestination.DONATE` |

Important: `MODES` currently exists as a separate full-screen destination. Inspect its screen and do not route Tones to an EQ-only screen. Tones must remain immersive audio architectures; EQ modes belong under Equalizer.

If the project already has a dedicated Tone screen, route directly to it. If not, create/restore it without changing the requested drawer order.

## 6.4 Drawer interaction

- Opens from the hamburger icon.
- Closes on outside scrim tap.
- Closes after a destination is selected.
- Supports Android back.
- Has clear selected/current destination state.
- Uses accessible content descriptions.
- Each row has at least 48dp touch height.
- No horizontal clipping on small phones.
- No nested scroll if the six entries fit.
- If vertical scrolling is required, keep the header fixed and menu list scrollable.
- Avoid side-scroll if it makes labels hard to read; user asked to revamp the side-scroll navbar/hamburger menu, so preserve the side-drawer interaction but make its content vertical and readable.

## 6.5 Visual direction

- AMOLED black background.
- Deep burgundy selected row.
- Warm white labels.
- Muted secondary descriptions only where helpful.
- Thin separators or generous spacing.
- One consistent icon size.
- Small selected indicator.
- No excessive neon glow on every row.
- Drawer width around 280–320dp, respecting screen width and insets.

---

# 7. Port Discover from NEW VESPER — feature only

## 7.1 Scope boundary

Port only the New VESPER Audio Discover feature into OLD VESPER.

Do not copy:
- New app shell/navigation.
- New app theme wholesale.
- New database schema wholesale.
- New playback service/player wholesale.
- New settings or library implementation.
- New audio effects system wholesale.

Keep OLD VESPER's existing:
- Home, Songs, Albums, Artists navigation.
- `MusicRepository`.
- `SonoraViewModel`.
- `SonoraPlayerClient`.
- Room persistence.
- Local playback and queue.
- Current visual identity.

The Discover screen can be updated to match New VESPER's richer catalogue experience while using OLD VESPER's existing `Track` and playback pipeline.

## 7.2 Existing old Discover structure

`SearchScreen.kt` currently includes:
- `DiscoverTab`: SONGS, ALBUMS, ARTISTS.
- `DiscoverSourceFilter`: ALL, AUDIUS, LOCAL.
- Audius track/trending state.
- Audius-derived albums and artists.
- Conversion from `AudiusTrack` to the existing `Track` model.
- Track click callback using the existing playback path.
- Local album/artist lists supplied as parameters.

The existing screen can therefore accidentally mix local and remote content because it explicitly has an `ALL` and `LOCAL` source filter and receives local albums/artists/tracks.

## 7.3 Required Discover behaviour

Discover must be a remote-only surface.

- Do not show any local song, local album or local artist in Discover.
- Remove the `ALL` and `LOCAL` source filter controls.
- Do not derive remote results from local `allTracks`.
- Do not call local `MusicRepository.tracks` as a fallback for remote empty results.
- Do not use local album/artist data in Discover.
- Keep local Songs/Albums/Artists pages unchanged.
- If remote catalogue is empty, show a clear empty/error state and retry button.
- Search remote catalogue only.
- Remote results must be playable through existing player only when the source provides a valid playable stream URL and usage is permitted.
- Remote favourites/playlists must not corrupt local MediaStore identity or create fake local file URIs.

## 7.4 Match New VESPER's richer Discover experience

Use the feature set from the New VESPER implementation as the donor:
- Remote trending songs.
- Remote song search.
- Remote artist search.
- Remote album/playlist discovery.
- Load more / pagination.
- Loading state.
- Retry/error state.
- Search debounce.
- Result randomisation/mixed discovery when query is blank.
- No language bias in client-side filtering.
- A single catalogue surface, not a provider chooser.

Do not invent API endpoints. Inspect the New VESPER source and migrate the actual supported calls and parsing models. If the new project uses Audius, retain the exact official Audius API contract from that code and its response models. Do not silently substitute another source.

## 7.5 Random and language-neutral discovery

When query is blank:
- Fetch a sufficiently large valid remote batch.
- Shuffle the batch deterministically for the current session or use a seeded shuffle.
- Avoid repeatedly showing the same first 10 items after every recomposition.
- Keep a session history to reduce immediate duplicates.
- Mix genres/languages only if source metadata actually provides those fields.
- Do not hardcode Tamil, English, Hindi or any other language as preferred.
- Do not infer language from artist names or track titles.
- Do not claim balanced language distribution if the provider does not expose language metadata.
- Randomise remote playlists/collections independently from songs where the API supports them.
- Keep pagination and deduplication by remote provider ID.

When user searches:
- Debounce 300–500ms.
- Cancel stale requests.
- Show query results, not shuffled trending content.
- Preserve the last successful remote results during a temporary refresh if appropriate, but mark them stale.
- Do not append results from a previous query to the new query.

## 7.6 Remote model and local model boundary

Use distinct types for remote provider objects and local files.

Example:

```kotlin
data class RemoteTrack(
    val provider: String,
    val providerTrackId: String,
    val title: String,
    val artistName: String?,
    val albumName: String?,
    val artworkUrl: String?,
    val streamUrl: String?,
    val durationMs: Long?,
    val language: String?,
    val isExplicit: Boolean?
)
```

Use actual fields available from the donor API. Do not add unsupported metadata as if known.

Map a remote item into the playback model only at the playback boundary, with:
- A namespaced ID, e.g. `audius:{providerTrackId}`.
- A valid HTTP(S) stream URI.
- Explicit source/provider field if model supports it.
- No MediaStore ID.
- No fake local path.
- No assumption that the remote stream is downloadable/offline.

## 7.7 Provider/legal boundaries

- Use official/authorised APIs and permitted stream URLs.
- Do not bypass app-bound authentication, DRM, signed URLs or access controls.
- Do not copy private BitChord endpoints or reverse-engineered auth flows.
- Do not treat an open-source client or wrapper licence as a licence to stream a music catalogue.
- If a provider does not authorise third-party streaming, do not integrate it as a production source.
- Keep provider access behind an interface so a lawful provider can be added later.

The New VESPER Discover feature should be migrated as a working feature, not used as a reason to import unrelated backend code.

## 7.8 Suggested Discover architecture

Use the existing package structure. A small separation is enough:

- `DiscoverRepository` — remote API access and paging.
- `DiscoverViewModel` or existing `SonoraViewModel` state — query, tab, loading, error, result state.
- `SearchScreen` — presentation only.
- Existing `SonoraPlayerClient` — playback.
- Existing favourites/playlist repository — only if remote track persistence is deliberately supported.

Do not create a new app-wide repository or player if existing classes can own the responsibility.

## 7.9 Discover UI requirements

Top:
- Search field.
- Exactly three content tabs: Songs, Artists, Album.
- No local/remote source filter.
- No provider selection.

Songs:
- Remote-only trending/search results.
- Artwork, title, artist, duration.
- More menu only for actions that work for remote content.
- Play through existing player when stream URL is valid.

Artists:
- Remote artist results only.
- Artwork and name.
- Artist details should query remote artist catalogue rather than opening local ArtistDetails with a local artist ID.

Album:
- Remote album/collection results only.
- Do not mislabel generic playlists as albums unless the provider marks them as albums.
- Album detail route must resolve remote collection items.

Empty/error:
- No local fallback.
- Explain offline/unavailable state.
- Retry button.
- Keep the local Songs tab accessible independently.

---

# 8. Crash and stability investigation

Do not guess at crash fixes. First reproduce and collect stack traces.

## 8.1 Required crash evidence

Capture:
- `adb logcat` around crash.
- Full fatal exception and `Caused by`.
- Device model.
- Android version.
- App version/build variant.
- Screen/action immediately before crash.
- Whether it occurs during scan, playback, Discover, artwork load, metadata edit or navigation.

## 8.2 High-risk areas to audit from current code

- SAF permission revoked or cloud provider unavailable.
- Recursive SAF folder depth.
- MediaMetadataRetriever failure/release.
- Missing or unsupported MediaStore columns.
- Null/zero dateModified and size values.
- Concurrent Room writes during scan.
- UI state changing while track list is updated.
- Remote response parse failures.
- Invalid/missing stream URLs.
- Artist/album details for remote objects.
- Coil loading of invalid artwork URLs.
- Audio effect creation on unsupported devices.
- Player released while a screen callback still references it.
- Drawer/navigation state during destination transition.

## 8.3 Crash handling requirements

- Catch provider/file exceptions at repository boundaries.
- Do not swallow `CancellationException`.
- Do not catch `Throwable` as a generic recovery mechanism.
- Always release `MediaMetadataRetriever`.
- Validate remote URLs before playback.
- Handle malformed JSON and missing optional fields.
- Use safe defaults only for display, not to overwrite persisted metadata.
- Avoid recursive stack overflow on extremely deep SAF trees; use an explicit traversal queue if needed.
- Keep last good cached library after scan failure.
- Add diagnostic logs without logging private file contents or unnecessary personal paths.

---

# 9. Data migration and backward compatibility

- Preserve Room database version and migrations.
- Add columns only with explicit migration and safe defaults.
- Preserve favourites, playlists, playlist-track order, history and custom EQ presets.
- Preserve DataStore keys for audio settings.
- Preserve persisted SAF URI permissions.
- Keep remote Discover cache in a separate table or separate namespace if caching is needed.
- Never insert remote Discover tracks into the local MediaStore track table.
- Do not delete local cache to make remote Discover look cleaner.
- Test upgrade from the existing installed database, not only a clean install.

---

# 10. Performance targets and measurement

These are acceptance goals, not claims that the current app already meets them.

| Area | Target / acceptance |
|---|---|
| Cold start | Show cached library without waiting for full scan |
| Startup scan | Header inventory first; metadata enrichment in background |
| SAF scan | Incremental; no unconditional full metadata extraction on every launch |
| Scroll | Stable item keys, no repeated full-list work per frame |
| Artwork | Thumbnail-sized loads; cache; no full-resolution decode in rows |
| Search | Debounced; cancel stale requests |
| Discover | Remote-only; no local-list mapping |
| Playback | Existing single player remains alive across navigation |
| Crash rate | No reproducible crash in tested flows |
| Memory | No unbounded bitmap/retriever/queue retention |

Test at least:
- 100 tracks.
- 1,000 tracks.
- 5,000 tracks if available.
- A large SAF tree.
- Missing artwork.
- Corrupt metadata file.
- Offline startup.
- Remote API timeout.
- Invalid remote stream.
- Repeated tab/drawer navigation.
- Playback while scanning.
- Scan while opening Metadata Studio.

---

# 11. Implementation sequence

Do not attempt all changes in one giant refactor. Implement in this order.

## Phase 0 — Backup and baseline
- Extract supplied OLD project.
- Create full backup ZIP.
- Record Gradle/AGP/Kotlin/Media3 versions.
- Build baseline debug.
- Run existing tests.
- Capture baseline APK size and crash log if reproducible.
- Do not edit source until baseline is understood.

## Phase 1 — Fast library startup and scan
- Verify cached Room state appears immediately.
- Separate header inventory from metadata enrichment.
- Fix SAF incremental scan.
- Prevent unnecessary full rescans.
- Keep chunked Room writes.
- Move expensive grouping off main.
- Add scan metrics and logs.
- Test large library.

## Phase 2 — Metadata restoration
- Audit Metadata Studio callbacks in `SonoraViewModel`.
- Restore track menu entry and route.
- Ensure single and batch save callbacks persist correctly.
- Add user override precedence.
- Fix artwork cache invalidation.
- Test rescan persistence.

## Phase 3 — Smoothness and crashes
- Add stable Lazy list keys/content types.
- Isolate playback position state.
- Optimise artwork.
- Profile recomposition.
- Fix reproduced crash stack traces.
- Add regression tests.

## Phase 4 — Drawer
- Implement exact order:
  1. About
  2. Settings
  3. Equalizer
  4. Tones
  5. Scan media
  6. Donate
- Verify routes map to the correct existing screens.
- Confirm Tones are not accidentally mapped to EQ Modes.
- Test back/scrim/selection/small screens.

## Phase 5 — Discover feature migration
- Inspect New VESPER Discover source and API contract.
- Port only remote repository/state/presentation feature.
- Remove local content from Discover.
- Keep local pages untouched.
- Preserve existing player.
- Add remote artist/album details.
- Add session randomisation and pagination.
- Test offline and malformed responses.

## Phase 6 — Release QA
- Build debug and release.
- Run migration tests.
- Test local playback and remote playback.
- Test scan + playback concurrently.
- Profile scrolling and startup.
- Inspect APK/AAB size.
- Produce final ZIP and change report.

---

# 12. Required tests

## Scan/parser
- [ ] Cached Room tracks appear before scan finishes.
- [ ] MediaStore headers do not require per-track MediaMetadataRetriever.
- [ ] Unchanged files are skipped.
- [ ] Changed files are selectively enriched.
- [ ] SAF unchanged files are skipped.
- [ ] SAF cloud-provider failure does not erase cached tracks.
- [ ] Retriever is always released.
- [ ] Corrupt files do not stop the scan.
- [ ] Scan can be cancelled safely.
- [ ] Duplicate scan request does not start another full scan.

## Metadata
- [ ] Metadata Studio is reachable.
- [ ] Single metadata save persists.
- [ ] Batch metadata save persists.
- [ ] Manual override survives scan.
- [ ] Artwork updates correctly.
- [ ] Playlists/favourites remain attached to tracks.
- [ ] Metadata fields do not silently disappear.

## UI performance
- [ ] 1,000-row Songs list scrolls without obvious jank.
- [ ] Stable keys are used.
- [ ] Playback ticker does not recompose the entire list.
- [ ] Artwork does not cause layout jumps.
- [ ] Drawer open/close is smooth.
- [ ] Navigation preserves scroll state.
- [ ] No blocking work on main thread.

## Drawer
- [ ] About
- [ ] Settings
- [ ] Equalizer
- [ ] Tones
- [ ] Scan media
- [ ] Donate
- [ ] Exact order verified.
- [ ] Every route opens the intended screen.
- [ ] Tones and EQ Modes remain distinct.

## Discover
- [ ] No local tracks appear.
- [ ] No local artists appear.
- [ ] No local albums appear.
- [ ] Exactly Songs, Artists, Album content tabs.
- [ ] Blank query loads remote discovery.
- [ ] Results randomise without repeated duplicates.
- [ ] No language is hardcoded as preferred.
- [ ] Search cancels stale requests.
- [ ] Pagination deduplicates by provider ID.
- [ ] Remote album detail opens remote collection.
- [ ] Remote artist detail opens remote catalogue.
- [ ] Offline state has no local fallback.
- [ ] Existing local playback/library remains unchanged.

## Stability
- [ ] No crash on scan.
- [ ] No crash on corrupt file.
- [ ] No crash on missing artwork.
- [ ] No crash on malformed remote response.
- [ ] No crash on invalid stream URL.
- [ ] No crash when SAF permission is revoked.
- [ ] No crash when navigating during playback.

---

# 13. Coding-agent master prompt

You are maintaining the supplied OLD VESPER Android project. This is a repair and targeted feature migration, not a greenfield rewrite.

**First priority: fix scan/parser speed and restore metadata.**  
**Second: smooth scrolling and stability.**  
**Third: rebuild the drawer in the exact requested order.**  
**Fourth: port only New VESPER's Discover feature.**

Before editing:
1. Inspect the whole project.
2. Read `MusicRepository`, `MediaStoreScanner`, `SafFolderScanner`, `SonoraViewModel`, `SearchScreen`, `MetadataStudioScreen`, `Track`, Room entities/DAO and playback client.
3. Inspect the New VESPER Discover implementation and identify its exact API contract and data models.
4. Make a full backup.
5. Write a file-by-file change plan.

Implementation rules:
- Preserve existing project architecture and user data.
- Do not replace local playback or create another player.
- Do not drop metadata fields.
- Do not run `MediaMetadataRetriever` on every file during every scan.
- Use fast MediaStore header inventory and selective enrichment.
- Make SAF scanning incremental.
- Keep cached library available while background scan runs.
- Keep expensive work off the main thread.
- Use stable Lazy list keys and isolate playback progress state.
- Preserve Metadata Studio single and batch editing.
- Drawer order must be About, Settings, Equalizer, Tones, Scan media, Donate.
- Tones are immersive audio architectures; EQ Modes are frequency presets.
- Discover is remote-only. It must never display local tracks/albums/artists.
- Migrate only Discover from New VESPER. Do not transplant the new app shell, database or player.
- Use the existing player for valid remote streams.
- Do not bypass authentication, DRM, signed URLs or provider restrictions.
- Never invent API endpoints or fake catalogue results.
- Do not claim build/test success without actual output.

After implementation, provide:
- Complete updated project ZIP.
- Full backup or clear backup location.
- Files changed and why.
- Scan architecture before/after.
- Metadata features restored.
- Drawer route mapping.
- Discover source/API and remote-only proof.
- Crash fixes with stack traces or reproduction evidence.
- Build command and actual result.
- Device/Android version used for runtime testing.
- Scroll/startup measurements.
- Known limitations.

**Do not stop after editing the Markdown or drawing the UI. Implement, build, run and verify the changes.**

---

# 14. Final expected result

The repaired OLD VESPER should:
- Open quickly using its Room cache.
- Scan local media incrementally.
- Avoid expensive full SAF metadata extraction at startup.
- Retain rich track metadata and Metadata Studio.
- Scroll smoothly through large local libraries.
- Keep playback stable while scanning and navigating.
- Present the exact six drawer actions in the requested order.
- Offer a richer, remote-only Discover experience migrated from New VESPER.
- Keep local Songs/Albums/Artists independent from online catalogue results.
- Preserve the existing Nocturne visual identity and all user data.

**The guiding principle: make OLD VESPER faster and more reliable without removing what made it feature-rich.**
