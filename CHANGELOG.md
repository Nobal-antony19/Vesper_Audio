# VESPER — Changelog

## v3.0.0 — The Definitive Stability-First Master Build
- **Deterministic Artwork Cache & Single-Flight ID3 Extraction (`VesperArtworkResolver`)**:
  - Replaced generic `content://media/external/audio/albumart` cache collisions with deterministic per-track cache keys (`trk_<trackId>_<dateModified>_<sizeBytes>.jpg/.png`).
  - Added single-flight extraction locks (`ConcurrentHashMap`) to prevent duplicate concurrent decoding, atomic `.tmp` writes, file-size integrity validation (`> 64B`), alpha-aware encoding (`ARGB_8888` PNG when alpha is present, `RGB_565` JPEG 85% for opaque covers), and a bounded 40 MB disk cache ceiling (`trimCacheIfNeeded`).
  - Connected `SafFolderScanner` directly to `VesperArtworkResolver` with dotfile filtering, safe `track/total` & `disc/total` parsing, and graceful `SecurityException` recovery for revoked SAF tree URIs.
- **Media3 Circular Queue & In-Place Transition Hardening (`SonoraPlayerClient`)**:
  - Added pure, unit-tested circular queue index helpers (`nextIndex`, `previousIndex`) handling empty, single-item, and boundary wrap queues safely.
  - Optimized `playTrack`, `skipNext`, and `skipPrevious` to use `player.seekToDefaultPosition(targetIndex)` when the active queue is already loaded in `MediaController`, avoiding redundant `setMediaItems`/`prepare` rebuilds.
  - Added bounded `onPlayerError` recovery (skips up to 3 consecutive unplayable items before surfacing `playbackError` cleanly).
- **Studio Tone Engine (`AudioEffectController`, `AudioTuningScreen`)**:
  - Added automatic Preamp Headroom Protection (`10^(-maxPositiveGainDb / 20)`) wired into `SonoraPlayerClient` volume scaling to prevent digital clipping on positive EQ boosts.
  - Added full Custom EQ Preset management: Save, Rename, Duplicate, and Delete persisted in Room.
  - Added Stereo Balance (`-1.0f..1.0f`) with Center reset and hardware `Virtualizer` support where device capabilities report availability.
  - Replaced all exaggerated audio marketing copy ("true hardware bypass", "bit-perfect", fabricated bitrates) with acoustically honest descriptions ("App Effects Bypass", "Android Audio Session • Active Route").
- **Lawful Paginated Discover (`AudiusApiClient`, `SearchScreen`)**:
  - Added `offset` and `limit` pagination (`Load More from Audius`) and structured error states (`AudiusResult`) with an inline retry banner when offline.
  - Unified `Songs`, `Albums`, and `Artists` tabs across `All`, `Audius`, and `Local` filters using `deriveAlbumsFromTracks` and `deriveArtistsFromTracks`, with clear `[Audius]` / `[Local]` source badges.
  - Added `Relevance` vs `A–Z` sort toggle on Discover Songs; restricted `AlphabeticalFastScrollRail` to `A–Z` mode using a precomputed `O(1)` letter-to-index map.
- **Safe Non-Destructive Room Migration (`MIGRATION_5_6`) & Streamlined Scope**:
  - Retired `playback_history`, `listening_events`, `downloads`, and Appearance Studio tables/keys via explicit `MIGRATION_5_6` (`version = 6`) while preserving all user Favorites, Playlists, Scanned Folders, Custom EQ Presets, and Cached Tracks.
  - Removed unbacked Settings toggles (`hiResAudio`, `lowLatencyMode`, `replayGain`, `excludeHiddenFiles`) and wired `filterShortTracks`, `autoScanStartup`, and `preampHeadroomEnabled` to persisted DataStore preferences.

---

## v2.9.0 — Discover & Incremental Sync Refinement
- Unified Online + Local Discover hub powered by Audius public endpoints and local `MediaStore`/`SAF` library search.
- Added incremental library synchronization (`syncLibraryIncremental`) comparing `(id, dateModified, sizeBytes)` against `CachedTrackEntity` in Room.
- Added debounced search queries and background coroutine dispatching (`Dispatchers.Default`) for responsive filtering.

---

## v2.8.0 — Cosmic Architecture & Navigation Stabilization
- Established the 5-tab bottom navigation (`Home`, `Songs`, `Albums`, `Artists`, `Discover`) and 5-item navigation drawer (`Donate`, `About`, `Settings`, `Equalizer`, `Scan Media`).
- Added `NowPlayingScreen` with synced `.lrc` lyrics resolution (`VesperLyricsResolver`), sleep timer (`SleepTimerController`), and `AudioTuningScreen` segmented tabs (`Equalizer` and `Modes`).

---

## v2.7.0 — Metadata, Artwork & Fast-Scroll Rail
- Added composite album grouping (`album + albumArtist`) so compilations and identically named albums by different artists never collide.
- Added `AlphabeticalFastScrollRail` for rapid library navigation and `MetadataStudioScreen` for single and batch track tag updates.

---

## v2.6.0 — Foundation & Media3 Playback Core
- Built core offline-first Android music player architecture with Jetpack Compose, Material 3, AndroidX Media3 `MediaSessionService`, Room database, and DataStore preferences.
