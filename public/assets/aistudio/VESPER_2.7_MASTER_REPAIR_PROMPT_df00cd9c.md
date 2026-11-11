# VESPER 2.7 — MASTER REPAIR, REFINEMENT & VERIFICATION PROMPT
**For Google AI Studio | One controlled repair programme | Android / Kotlin / Jetpack Compose**

> **Mission:** Repair the existing VESPER 2.7 project, preserve every working feature, resolve the reported defects at their root causes, and deliver a stable, refined, lightweight music player. This is a source-code repair task, not a redesign-from-scratch task.

---

## 0. HOW TO USE THIS FILE

Attach these items to the same AI Studio conversation:

1. The latest VESPER 2.7 project ZIP.
2. The VESPER 2.7 16:9 reference board image.
3. Relevant screenshots and screen recordings showing the defects.
4. This Markdown file.

Treat the screenshots and recordings as **bug evidence**. Treat the 16:9 board as the intended visual and interaction direction. The board is a design target, not permission to copy another app's branding, assets, source code, or exact proprietary layout.

### Mandatory working protocol

1. First inspect the supplied project. Confirm the package name, actual dependency versions, database version, navigation structure, player architecture and existing feature implementations. Do not assume old line numbers or old file contents are still correct.
2. Reply first with a concise plan of no more than 20 numbered lines, grouped into batches B1–B9. Then begin B1 in the same response if the relevant files are available.
3. Work in small batches. Do not attempt to rewrite the whole project in one enormous code response.
4. For each batch, output **only changed or new files**:
   - Files under 200 lines: provide the complete file.
   - Larger files: provide exact, unambiguous edits with file path, a short surrounding context, and `OLD` → `NEW` blocks.
   - Never reprint untouched files.
   - Never use vague instructions such as “rest unchanged”.
5. Keep explanations to five short lines per batch, excluding code.
6. At the end of every batch, report:
   - `Files changed:`
   - `Risk check:` imports, nullable types, Room schema/migration, lifecycle, permissions and API guards as applicable.
   - `Verify:` exact manual or automated checks for that batch.
7. Do not claim a build, test, device check or performance target passed unless it was actually executed and observed.
8. If required source files are unavailable, identify the exact missing paths and stop that batch. Do not invent replacement implementations.
9. Preserve existing functionality. Do not replace real code with placeholders, mock data, demo tracks or fake success states.
10. Prefer root-cause fixes over visual workarounds.

### Output efficiency rules

- Search for symbols and call sites before editing; do not reread or reproduce the entire project.
- Make the smallest coherent change that fixes the root cause.
- Reuse existing models, repositories, state flows and services when sound.
- Avoid duplicate state, duplicate playback engines, duplicate preference systems and duplicate caches.
- Do not add dependencies unless a required feature cannot reasonably be implemented with the existing stack. Explain and request approval before adding one.
- Do not spend tokens restating this prompt. Report only findings, code changes and verification.
- If a requested feature is unsafe, impossible on the current API level, unsupported by the device or not supported by the existing provider, explain the limitation and implement a truthful fallback.

---

## 1. PROJECT BASELINE — VERIFY BEFORE EDITING

The supplied VESPER project configuration currently indicates:

| Item | Observed project configuration |
|---|---|
| Android namespace | `com.vesper.audio` |
| Application ID | `com.aistudio.sonora.musicaudio` |
| Kotlin | `2.2.10` |
| Android Gradle Plugin | `9.1.1` |
| Compose BOM | `2024.09.00` |
| Room | `2.7.0` |
| Coil | `2.7.0` — Coil v2 API |
| Media3 | `1.5.1` |
| DataStore | `1.1.7` |
| minSdk | `24` |
| targetSdk | `36` |
| Java source/target | `11` |

**Re-read the actual Gradle files in the attached ZIP and treat them as authoritative if they differ.** Do not upgrade or downgrade versions as part of this repair unless the project cannot build without a justified change.

### Protected project areas

- Do not modify the Downloader screen or Downloader business logic.
- Do not modify `signingConfigs`, debug signing or release signing assignments.
- Do not change the application ID or package name.
- Do not remove existing features: favourites, playlists, queue, history, stats, lyrics, metadata editing, downloader, equalizer, sound profiles, notifications or lock-screen playback.
- Do not delete user data or perform destructive Room migrations.
- Do not introduce a second player or a second settings store.

### First inspection checklist

Before coding, locate the current implementations of:

- Application class and dependency creation.
- Main navigation and screen shell.
- ViewModel and state flows.
- Music repository.
- MediaStore scanner.
- SAF folder scanner.
- Room entities, DAO, database and migrations.
- Artwork resolver and all artwork loading call sites.
- DataStore preferences.
- Appearance Studio and theme provider.
- Settings screen.
- Songs, Search, Albums, Artists, Playlists and Playlist Details screens.
- Media3 player service and player client.
- Audio effect controller and sound profiles.
- Mini-player and bottom navigation.
- Tests and build configuration.

Create a short internal file map with real paths and current responsibilities. Do not guess line numbers.

---

## 2. BUGS TO FIX — SOURCE OF TRUTH

The following are the reported VESPER 2.7 problems. Verify each one against the current source and supplied recordings before changing it.

### A. Library re-indexes from scratch on every app launch

Observed:
- Splash is followed by a blank `Indexing audio library…` screen.
- Reopening after force-close or clearing RAM behaves like a first scan.
- The library UI is unavailable while scanning.
- Metadata and artwork appear to be re-extracted repeatedly.

Required outcome:
- Persist the indexed library in Room.
- Display the cached library immediately on later launches.
- Scan only new, changed or genuinely missing media.
- Keep the current UI visible during background sync.
- Only show a full-screen first-scan state when there is no cached library at all.
- Provide an explicit, confirmed manual full rescan in Settings.
- A failed or incomplete scan must never delete the cached library.

### B. Artwork coverage is low or incorrect

Observed:
- Only roughly 30% of tracks show artwork.
- Different tracks sometimes show the same unrelated image.
- Album grouping merges unrelated artists or albums.
- Original artwork already present on the phone is not consistently displayed.

Required outcome:
- Use a deterministic per-track/per-artwork resolution pipeline.
- Prefer embedded artwork, then Android MediaStore thumbnails, then carefully validated sibling album artwork.
- Never trust a shared album ID alone as proof that unrelated files share artwork.
- Correctly group albums using album + album artist (falling back to track artist).
- Cache decoded artwork efficiently and persist stable references.
- Recover automatically if an artwork file disappears or becomes corrupt.
- Show a consistent designed placeholder if no real artwork exists.
- Clearly distinguish real artwork from generated placeholders in Storage settings.
- Do not claim 100% original artwork: some audio files contain no cover art and some system providers expose no usable thumbnail.

### C. Appearance Studio is only partially applied

Observed:
- Accent colour changes some controls but leaves many components in the old burgundy/gold palette.
- Background photo selection does not reliably change the visible app background after Apply.
- Some screens paint opaque backgrounds over the root backdrop.
- Apply/Reset behaviour is confusing or cosmetic.

Required outcome:
- One app-wide source of truth for the selected accent and derived accent colours.
- Apply accent consistently to navigation, selected states, chips, switches, sliders, progress, FABs, EQ curve, waveform, selection, focus and interactive highlights.
- Keep neutral text and surface colours legible in Dark and Light themes.
- Make custom background selection reliably persist and display after Apply and restart.
- Ensure no screen-level opaque background unintentionally hides the root backdrop.
- Keep cards, dialogs and sheets sufficiently opaque for readability.
- Implement real draft / Apply / Reset behaviour. Unsaved changes must be visible and leaving with unsaved changes must prompt Apply or Discard.
- Selecting a new image must use a unique file/cache key so the previous image cannot remain stuck in Coil cache.
- Support crop, focus, blur and dim controls with safe API fallbacks for minSdk 24.
- If a selected image becomes unavailable, fall back safely and explain the issue.

### D. Search state leaks between Search and Songs

Observed:
- Typing in Search also changes the Songs filter.
- Search and Songs do not behave as independent destinations.

Required outcome:
- Independent `searchQuery` and `songsFilterQuery`.
- Independent loading, result, error and pagination state.
- Search state should survive reasonable navigation within Search.
- Songs filter should not inherit Search text.
- Clearing one must not clear the other.
- Online search, if later added, must remain separate from the local library query unless the user explicitly chooses combined search.

### E. Settings are cluttered and some controls are fake

Observed:
- Too many rows and categories.
- Several switches appear to use local `remember` state and reset.
- Some settings do not change actual playback or library behaviour.

Required outcome:
- Keep only settings that are useful, understandable and actually implemented.
- Every visible switch must persist and control real behaviour.
- Remove fake, duplicate or misleading controls.
- Do not remove useful controls simply to reduce row count.
- Organise settings into clear groups:
  1. Appearance
  2. Playback
  3. Sound Studio
  4. Library & Scanning
  5. Downloads (preserve existing Downloader entry and behaviour)
  6. Storage & Cache
  7. Backup & Restore
  8. Privacy
  9. About
- Add only useful settings that can be implemented and tested.
- Explain settings that are device-dependent.
- Keep mandatory Android foreground-service notification behaviour honest; do not offer a fake switch to disable a mandatory notification.
- Do not label platform-supported gapless playback as a user-controlled feature if there is no real control to change it.

### F. Navigation and Playlists need a refined redesign

Observed:
- Current bottom navigation feels heavy and generic.
- Playlists need a proper dedicated destination.
- User wants a refined animated indicator with a minimal glow trail.
- Playlist interaction can take inspiration from Oto Music's smart lists, list density and contextual actions, but VESPER must retain its own visual identity.

Required outcome:
- Create a custom, lightweight bottom dock that fits VESPER.
- Use a short, smooth indicator movement and subtle fading glow trail.
- Keep labels only for the active destination if that improves clarity.
- Respect Android accessibility, touch target sizes, font scaling and reduced-motion preference.
- Do not make glow or animation distract from navigation.
- Preserve scroll positions when navigating between major destinations.
- Ensure mini-player and dock never cover list content.
- Add a dedicated Playlists destination.
- Smart lists: Favourites, Recently Added, Most Played and History, only where the data is real.
- Playlist functions: create, rename, delete with confirmation, reorder, add tracks, remove tracks, play, shuffle and contextual menu.
- Removing a track from a playlist must be an explicit action with Undo where practical. A menu tap must never accidentally delete or remove a track.
- Preserve playlist IDs, order and membership during rename or migration.

### G. Sound Studio needs honest, safe improvements

Observed:
- The app claims a 10-band EQ even though Android devices may expose a different number of hardware bands.
- Some presets sound identical or are mathematically identical.
- Night mode is just a flat volume reduction.
- Crossfade is currently a volume dip rather than a true overlapping crossfade.
- Effects and settings may reset after audio-session changes.

Required outcome:
- Keep one source of truth for presets and custom EQ.
- Derive actual band count and frequency ranges from the device's `Equalizer` effect.
- Display the actual hardware band count. Do not claim a 10-band hardware EQ on a 5-band device.
- Use reference curves and interpolate to the actual device bands in log-frequency space.
- Keep profiles distinct and explain their intended sound.
- Add conservative headroom to reduce clipping when positive EQ/bass gains are applied.
- Do not claim LoudnessEnhancer is a professional mastering limiter.
- Reattach/reapply supported effects when the audio session changes.
- Release effects cleanly.
- Expose capability state and disable unsupported controls rather than pretending they work.
- Keep audio processing lightweight and avoid unnecessary resampling or DSP.
- Crossfade must either be a real two-player overlap with robust lifecycle handling, or be renamed honestly to “Fade between tracks”. Never call a volume dip a crossfade.
- Do not promise studio-grade output, bit depth or sample rate unless the active device path reports it.
- Add an Output/Signal Path view only if it displays real runtime information.

### H. App size, memory and scrolling performance

Observed:
- App data grows substantially after scanning a large library.
- Artwork extraction/cache may be storing full-size images.
- Large song lists may recompose or sort too much.

Required outcome:
- Bound artwork and image caches.
- Resize artwork before persistent storage.
- Avoid duplicate artwork files using content hashes.
- Keep Room metadata compact.
- Use stable LazyColumn keys and content types.
- Move sorting, grouping and expensive parsing off the main thread.
- Avoid scanning or decoding every file on every launch.
- Avoid unbounded in-memory lists and repeated full-list transformations.
- Measure actual APK and app-data sizes; do not promise a target without measuring.
- Preserve responsiveness with at least 1,500 tracks and test with a larger library if available.

---

## 3. IMPLEMENTATION RULES — IMPORTANT TECHNICAL GUARDRAILS

### 3.1 Persistent incremental library index

Use Room as the persistent source of truth for the local library.

Preferred behaviour:
1. First launch: request permission only when necessary, perform a real initial scan and persist results.
2. Later launch: observe Room immediately and render the cached library.
3. After first frame: perform a lightweight incremental check if automatic sync is enabled.
4. Detect new, changed and missing files using stable media identity and available MediaStore version/generation information.
5. If the scan is incomplete, throws, returns a null cursor or lacks permission, do not delete cached rows.
6. Keep the last successful scan timestamp and scan diagnostics.
7. Manual rescan must be explicit and confirmed.

Important:
- Do not call a forced full rescan from a permission callback when permission was already granted.
- Check permission state before launching the permission dialog.
- Do not block the whole app while background sync runs.
- Observe Room flows instead of replacing the entire UI list after every sync.
- Preserve favourites, playlists, history and stats when a scan changes.
- Apply minimum-duration filtering at query time if feasible, not by permanently omitting tracks from the index.
- API-specific MediaStore generation calls must be guarded for the correct Android version.
- Use a conservative missing-file policy. Never delete all cached rows because one scan returned zero or suspiciously few results.
- SAF folder indexing must compare document identity/size/modified time before extracting metadata again. Do not fully extract metadata for every SAF document on every sync.

### 3.2 Artwork pipeline

Implement a staged resolver:
1. Embedded artwork from the actual audio file.
2. Android's per-item thumbnail API where supported.
3. Sibling artwork only when the normalised album + album artist match meaningfully.
4. No-artwork state.

Rules:
- Never use `albumId` alone as a unique artwork identity.
- Never reuse an unrelated cover merely because files share a folder or MediaStore album bucket.
- Store small, bounded, compressed artwork files in app-private persistent storage when appropriate.
- Use a content hash or stable content key.
- Decode with sampling; reject corrupt or unreasonably large images safely.
- Use limited concurrency and batches. Do not decode the entire library simultaneously.
- Persist artwork resolution state and retry count in Room if a schema migration is justified.
- If the referenced file is missing, mark it for repair and backfill it.
- A “Fix missing artwork” action should retry unresolved artwork without forcing a complete library rescan.
- Keep Coil memory and disk caches bounded and use Coil 2.7 APIs.
- Use appropriately sized requests for rows versus hero artwork.
- Keep list crossfade disabled or minimal to avoid scroll jank.
- Album grouping must use a stable composite key: normalised album artist + normalised album title.
- For albums with no meaningful album artist, fall back to track artist.
- Show a deterministic VESPER placeholder for NONE/FAILED. Do not display another song's cover.
- Storage UI must report real counts: actual artwork, system thumbnail, shared album art, generated placeholder and unresolved.

Do not add an online artwork lookup in this repair batch. If later proposed, make it opt-in and use a legitimate metadata/artwork service with its own terms.

### 3.3 Theme and accent architecture

Use one accent state and derive a coherent palette from it. Avoid scattered hard-coded burgundy/gold constants in composable UI.

The derived palette should include:
- Accent
- Accent highlight
- Accent deep
- Subtle accent surface
- Border
- Glow
- Text/icon colour on accent

Requirements:
- Calculate foreground contrast and select a readable on-accent colour.
- Provide neutral Dark and Light surfaces independently from accent.
- Apply accent to all interactive states and selected controls.
- Replace legacy constants systematically; do not use a dangerous global text replacement without checking composable and non-composable contexts.
- Use parameters for colours in non-composable code.
- Saved themes, if retained, must be persisted and capped to a reasonable number.
- Add a test for contrast and accent derivation.

### 3.4 Appearance background

Use a single root backdrop layer beneath app content.

Support:
- None
- Album artwork
- Custom photo

Persist:
- Background mode
- Custom image path/key
- Blur
- Dim
- Crop/focus
- Zoom, if implemented

Requirements:
- Copy selected image into app-private storage using a unique filename.
- Downsample and bound the stored image dimensions.
- Apply the new image with a new Coil cache key.
- Only delete the previous image after the new image is safely copied and persisted.
- The Apply button must commit the complete draft atomically.
- Reset must revert draft changes.
- On process restart, the selected background must be restored.
- Keep cards, menus, sheets and dialogs readable.
- API 31+ blur can use supported platform blur; API 24–30 must have a cheaper fallback or gracefully disable blur. Never crash on older devices.

### 3.5 Settings: real behaviour only

Every setting must have:
- A persistent value.
- A real implementation path.
- A clear label and description.
- A sensible default.
- A test or manual verification step.

If a setting cannot be implemented safely in this pass, remove it from the active UI or mark it clearly as unavailable. Do not ship a decorative toggle.

Suggested useful controls:
- Appearance Studio
- Resume playback
- Pause on headphone/Bluetooth disconnect
- Sleep timer
- Crossfade or honest fade setting
- Sound Studio
- Quick incremental scan
- Minimum track duration
- Music folders
- Manual rescan
- Fix missing artwork
- Storage usage and clear artwork/image cache
- Export/import user library data where safe
- Clear listening history
- About and privacy information

Clear Cache must never clear Room library records, favourites, playlists, user-created files or downloaded tracks. Label exactly what will be removed and confirm destructive operations.

### 3.6 Navigation and playlists

Create a VESPER-specific dock:
- Minimal rounded surface.
- Active icon/label.
- Small animated accent indicator.
- A short glow trail that fades quickly.
- 250–350 ms motion, unless reduced motion is enabled.
- No heavy always-on glow.
- Correct system-bar and gesture-navigation insets.
- Sufficient bottom content padding.

Suggested destinations:
- Home
- Songs
- Playlists
- Library
- Search

Library may contain Albums, Artists and Folders as internal tabs if the current navigation architecture supports this cleanly.

Playlist design may take inspiration from common music-player conventions and Oto Music's information density, smart-list grouping and contextual menus. Do not copy Oto branding, assets or exact screen composition.

### 3.7 Audio engine

Before changing the audio engine, map current ownership of:
- Player volume.
- Audio session ID.
- Equalizer.
- BassBoost.
- Virtualizer.
- LoudnessEnhancer.
- Crossfade/fade logic.
- ReplayGain, if implemented.
- Audio focus and noisy-route handling.

Rules:
- One owner for effective player volume. Combine headroom, fade envelope and any supported gain adjustment in one place.
- Never allow separate components to overwrite `player.volume` unpredictably.
- Restore preferences before attaching effects, then reapply after session changes.
- Clamp gains to platform ranges.
- Provide safe fallbacks for unsupported effects.
- Keep custom EQ values persistent.
- Do not claim that all devices expose the same bands/effects.
- Do not enable effects that the user has disabled.
- Keep presets distinct but moderate. Avoid extreme boosts that cause clipping or listener fatigue.
- Add unit tests for preset differences, interpolation and gain clamping.
- If genuine crossfade cannot be implemented and tested reliably, rename the existing behaviour to “Fade between tracks” and describe it honestly.

---

## 4. UI DIRECTION — VESPER 2.7

### Brand direction

VESPER should feel like a refined, personal music instrument:
- Deep near-black plum background.
- Burgundy accent with warm cream typography.
- Restrained glow, not neon overload.
- Premium spacing and clear hierarchy.
- Compact, information-rich song and playlist lists.
- Smooth but lightweight transitions.
- Strong album artwork presentation.
- Distinct VESPER identity.

Suggested base palette (verify against the current theme and preserve user custom accent):
- Background: `#171316`
- Secondary surface: `#211B20`
- Elevated surface: `#2B2229`
- Deep burgundy: `#4A1728`
- Accent burgundy: `#722C42`
- Primary cream: `#FFF1D5`
- Secondary cream: `#F5E7CC`

### Home

- Compact VESPER brand/header.
- Search entry.
- Continue Listening card only when real playback history exists.
- One featured album/playlist hero with correct image crop.
- Quick actions: Favourites, History, Stats, Shuffle.
- Recently Added horizontal shelf with correct artwork.
- Mini-player above the dock.
- Do not show fake recommendations or demo content.

### Songs

- Compact list rows with consistent artwork sizing.
- Title and artist/duration metadata.
- Contextual menu for queue, Play Next, favourite, playlist, rename where supported, details and delete only when appropriate.
- A–Z fast-scroll rail with readable letters and accessible touch targets.
- Current letter indicator.
- Sorting and filters must persist if intended.
- Preserve scroll position.

### Playlists

- Smart lists at top.
- User playlists below.
- Cover mosaics where multiple tracks exist.
- Track count and duration only when computed from real data.
- Create, rename, reorder, delete and manage tracks safely.
- Contextual actions must be explicit and non-destructive.

### Now Playing

- Large square artwork.
- Clear title/artist hierarchy.
- Favourite and queue actions.
- Seek bar and elapsed/remaining time.
- Shuffle, previous, play/pause, next and repeat.
- Queue, Lyrics, Equalizer and More actions.
- Gesture behaviour must not conflict with horizontal navigation or seek controls.
- Swipe down to minimise only if already supported or can be added safely.
- Keep controls usable on narrow/tall Android screens.

### Appearance Studio

- Live preview.
- Accent swatches and custom HEX input only if validated.
- Background modes: None, Album Art, Custom Photo.
- Blur, dim and crop/focus.
- Unsaved changes indicator.
- Real Apply, Reset and Default actions.

### Settings

- Grouped cards, concise descriptions, no decorative toggles.
- Prioritise common settings.
- Advanced information can be placed in expandable sections.

### Sound Studio

Tabs:
- Equalizer
- Presets/Modes
- Output

Output must display actual device/session capabilities only. Never invent sample rate, bit depth, hardware support or signal-path stages.

---

## 5. IMPLEMENTATION BATCHES — EXECUTE IN ORDER

Do not skip ahead to visual polish while core data correctness is broken.

### B1 — Baseline audit and safe startup
- Confirm current code and project configuration.
- Reproduce the startup scan path.
- Fix permission callback behaviour.
- Keep cached Room library visible.
- Prevent full-screen blank UI during background sync.
- Add a safe sync state model.
- Ensure failed/incomplete scans never delete cached rows.
- Add logging for queried, unchanged, new, updated, missing, failed and elapsed time.
- Do not perform a database migration unless B1 proves one is required.

### B2 — Incremental MediaStore and SAF indexing
- Compare stable IDs and metadata.
- Use MediaStore version/generation checks only with API guards.
- Process new/changed files only.
- Make SAF folder indexing incremental.
- Prevent duplicate rows and stale SAF entries.
- Preserve user data and playlist references.
- Add tests for unchanged, new, changed, missing and incomplete scans.

### B3 — Artwork resolver and album grouping
- Fix incorrect album-art bucket reuse.
- Implement embedded-art and system-thumbnail fallback.
- Persist compact artwork safely.
- Add bounded backfill and retry.
- Add self-healing for missing files.
- Group albums by album artist + album.
- Add honest placeholder and Storage counts.
- Verify the same unrelated cover is no longer reused across unrelated files.

### B4 — Theme and Appearance Studio
- Centralise accent derivation.
- Apply accent across all screens/components.
- Implement reliable custom background copy/cache keys.
- Implement draft/Apply/Reset.
- Make root backdrop visible behind transparent screen surfaces.
- Add API-level fallbacks.
- Verify after process restart.

### B5 — Settings simplification and wiring
- Audit every existing setting.
- Remove fake, duplicate and misleading controls.
- Persist every remaining setting.
- Connect each setting to actual behaviour.
- Add useful library, artwork, storage and privacy controls.
- Ensure Clear Cache never deletes the library or user data.

### B6 — Navigation, Songs, Search and Playlists
- Separate Search and Songs query state.
- Implement refined dock and reduced-motion behaviour.
- Add dedicated Playlists destination.
- Fix playlist actions and safe removal.
- Preserve scroll state and content insets.
- Keep existing A–Z rail working and improve touch targets.

### B7 — Sound Studio and playback integrity
- Unify preset state.
- Derive real EQ band count and frequency centres.
- Improve preset curves conservatively.
- Add safe headroom and one volume owner.
- Reattach effects after audio-session changes.
- Verify crossfade or rename it honestly.
- Preserve queue, audio focus, notification and lock-screen behaviour.

### B8 — Performance, memory and cleanup
- Profile large lists and recomposition.
- Bound artwork and image cache sizes.
- Downsample artwork.
- Avoid duplicate extraction.
- Remove only dependencies proven unused.
- Enable release shrinking only after validating required reflection/manifest/Room/Media3 rules.
- Preserve signing configuration.
- Measure actual APK and app-data size.

### B9 — Regression, build and final report
- Run the build and tests available in the environment.
- Fix regressions caused by these batches.
- Verify Room migration paths.
- Test process death and relaunch.
- Produce an honest report with PASS/PARTIAL/NOT IMPLEMENTED/DEVICE DEPENDENT/FAIL.
- List all changed files by batch.
- List all tests actually run and their actual output.
- List remaining device-only checks.

---

## 6. ACCEPTANCE CRITERIA

A feature is complete only when its acceptance test passes.

### Library and startup
- Second launch displays cached Home/Songs immediately.
- No full-screen indexing state appears when Room already has tracks.
- Background sync does not reset navigation or scroll position.
- Unchanged files are not re-extracted.
- Failed/incomplete scans do not delete cached tracks.
- Revoking permission does not erase cached library or user data.
- Re-granting permission restores incremental sync.

### Artwork
- Each track displays its correct original cover where available.
- Unrelated tracks never inherit a cover solely because of a shared album ID.
- Album grouping separates same-named albums by different artists.
- Missing/corrupt artwork is retried safely.
- Every row has either real artwork or a deliberate VESPER placeholder.
- Storage distinguishes real artwork from generated placeholders.
- No claim of universal original cover availability.

### Appearance
- Changing accent updates all interactive UI consistently.
- Custom background appears immediately after Apply.
- Custom background persists after app restart.
- Changing the custom photo does not display the previous cached image.
- Reset and unsaved-change handling work.
- Dark/Light theme text remains readable.

### Search and navigation
- Search query never leaks into Songs.
- Songs filter never changes Search.
- Dock destinations open the correct screens.
- Indicator animation is smooth and restrained.
- Reduced-motion setting disables or reduces the animation.
- Mini-player/dock do not obscure list content.

### Playlists and library
- Create, rename, delete, reorder, add and remove work.
- Removing a track is explicit.
- No contextual-menu action accidentally deletes or removes a track.
- Favourites, History, Recently Added and Most Played use real data.
- Existing playlist IDs and track membership survive migration.

### Audio
- EQ displays the actual supported band count.
- Presets are measurably different.
- Settings survive app restart and audio-session changes.
- Unsupported effects are labelled unavailable.
- Positive EQ/bass gain is managed with safe headroom.
- Crossfade is genuinely overlapping or honestly renamed.
- Notification, lock-screen and Bluetooth controls remain functional.

### Performance and reliability
- App remains responsive with at least 1,500 tracks.
- Artwork/cache storage is bounded.
- No repeated full scan on normal relaunch.
- No crash from API-level-specific calls on minSdk 24.
- APK and app-data measurements are reported, not guessed.

---

## 7. TESTING REQUIREMENTS

Run only commands supported by the supplied project and environment. The project ZIP may not contain a Gradle wrapper JAR; verify before assuming `./gradlew` is available.

Suggested checks when Gradle is configured:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Useful device checks:

```bash
adb logcat -s VesperScan VesperArt
adb shell am force-stop com.aistudio.sonora.musicaudio
```

Then relaunch the app and verify that Room data appears before background sync.

Add or update tests for:
- Incremental scan comparison.
- Suspicious/incomplete scan protection.
- Artwork resolution state transitions.
- Album grouping keys.
- Accent contrast.
- Appearance draft/commit/reset.
- Search/Songs query separation.
- Playlist mutation safety.
- EQ interpolation.
- Preset distinctness.
- Volume/headroom clamping.
- Room migration preservation.

For Room schema changes:
- Inspect current version and every existing migration.
- Add a forward migration only.
- Never use destructive migration fallback.
- Preserve favourites, playlists, history, stats and metadata.
- Add migration tests from the actual previous schema.
- Do not copy a proposed SQL migration from an older prompt without matching it to the current entities and schema.

---

## 8. DEVICE-DEPENDENT TESTS — DO NOT CLAIM FROM SOURCE REVIEW

The following require a real Android device, ideally the user's Redmi K50i / HyperOS device:

- Lock-screen artwork and media controls.
- Notification behaviour under HyperOS.
- Bluetooth disconnect and audio focus.
- Audio effect support and actual band count.
- Output sample rate and float output.
- Crossfade gaplessness and timing.
- Battery and background playback restrictions.
- Large-library scan speed.
- Image picker behaviour and background persistence.

Mark these `DEVICE DEPENDENT` until tested.

---

## 9. FINAL RESPONSE FORMAT

At the end of B9, report:

### Summary
What is genuinely fixed and what remains.

### Batch report
For each B1–B9:
- Status.
- Files changed.
- Root cause addressed.
- Tests performed.
- Remaining risks.

### Acceptance criteria
Use only:
- `PASS`
- `PARTIAL`
- `NOT IMPLEMENTED`
- `DEVICE DEPENDENT`
- `FAIL`

### Build and measurements
- Exact build command and result.
- Exact test command and result.
- APK size if built.
- App-data size only if measured on device.
- Any warnings or errors.

### Remaining work
List only actual remaining items. Do not say “everything is fixed” if any acceptance criterion remains unverified.

**Final instruction:** Preserve VESPER's identity and working features. Make the smallest reliable set of changes that fixes the actual defects. A stable, honest, fast app is more valuable than a large amount of unfinished UI or fake functionality.
