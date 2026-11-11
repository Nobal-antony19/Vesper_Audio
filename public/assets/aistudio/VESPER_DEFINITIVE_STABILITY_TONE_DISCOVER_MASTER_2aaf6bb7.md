# VESPER — THE DEFINITIVE STABILITY-FIRST MASTER BUILD
## Core repairs → Studio Tone Engine → Discover → premium release

**Document type:** Executable engineering brief for Gemini / Claude / an Android coding agent  
**Project:** VESPER — offline-first Android music player  
**Primary objective:** Turn the existing VESPER project into a stable, fast, premium music player with genuinely working audio controls and a lawful, useful Discover experience.

---

# 0. EXECUTION CONTRACT — READ BEFORE TOUCHING CODE

You are the principal Android/Kotlin engineer, Media3 playback engineer, audio DSP engineer, Compose performance engineer, QA lead and product designer.

This is an **implementation task against the supplied existing project**, not an audit-only report, mock-up, tutorial, or new greenfield app.

## Mandatory workflow

1. Inspect the complete supplied project ZIP and all relevant source files before editing.
2. Read this specification and the companion VESPER engineering reference supplied by the user. Treat the reference as design input, not unquestionable technical truth. Validate every code sample and claim against the real project and Android APIs.
3. Make a complete untouched backup of the original project outside generated build/cache directories.
4. Establish the current build baseline. Record the exact Gradle/SDK/JDK setup, build errors, app ID, version, signing configuration and available tests.
5. Diagnose and repair existing failures first. Do not begin broad Discover implementation while playback, import, navigation or startup is unstable.
6. Implement the phases in order. Build and test after each coherent phase.
7. Do not stop after producing recommendations. Make the actual source changes.
8. Do not ask the user to manually edit Kotlin files. Complete everything possible in the available environment.
9. Do not use Git. Use full-folder backups and deliver a complete project ZIP.
10. Do not claim a test passed, a feature works, an APK is under a size limit, or a performance target was achieved unless it was actually built/measured/tested.
11. If an environment limitation blocks a task (no Android SDK, no device, no API key, unavailable network, missing signing key), finish all independent work and label the blocked test precisely as `NOT RUN` or `BLOCKED`.
12. Never leave placeholder buttons, fake controls, TODO implementations, silent catches, demo data masquerading as live results, or known compile errors.

## Priority order

**Data safety → startup/build stability → local import → playback/queue correctness → UI performance → Tone engine → Discover → visual polish → release optimisation.**

## Required delivery

- Complete modified Android project in a ZIP.
- Release APK if the environment can genuinely build one.
- `IMPLEMENTATION_REPORT.md` listing changed files, architecture decisions, tests with PASS/FAIL/NOT RUN, known limitations, APK size if measured, and exact build command.
- Changelog update.
- No snippets-only delivery.

---

# 1. PRODUCT NORTH STAR

VESPER should feel like a carefully engineered music instrument, not a clone full of decorative toggles.

## Product principles

- Local-first: local library and local playback remain fully useful without internet.
- Privacy-first: no mandatory account, ads, trackers, fingerprinting, behavioural analytics or unnecessary telemetry.
- Fast: predictable scrolling, low memory churn, no UI-thread metadata extraction, no expensive animation work while audio plays.
- Audio-first: playback correctness and sound quality take priority over visual gimmicks.
- Honest: controls must do what their labels claim. Never claim lossless conversion, Dolby Atmos, Hi-Res certification, hardware bypass, or 8D surround unless that exact capability is verifiably implemented and supported.
- Premium: restrained near-black AMOLED surfaces, deep wine/burgundy accents, confident typography, clear hierarchy, refined spacing and responsive motion.
- Lean: no dependency or service should be added without a demonstrated need.

## Feature removals requested

Remove these user-facing features and their dead implementation paths:
- Appearance Studio, dynamic wallpaper/background customisation and appearance-only state.
- Downloader and all downloader-only screens, services, workers, permissions, settings and dependencies.
- Artist Cache UI and artist-scraping/cache workers.
- Playback History, Listening History, Recently Played history and Listening Stats UI/features.

Do not remove:
- Local music files.
- Favourites.
- User playlists.
- Current queue.
- SAF folder access and persisted URI permissions.
- Existing user-created audio presets or audio preferences unless migration is explicitly required and data is preserved.
- Core metadata required for library display and playback.
- Any unrelated feature not explicitly listed for removal.

Remove dead code only after tracing all references. Remove database entities/tables only with a safe migration. **Never use destructive database recreation or `fallbackToDestructiveMigration()` as a shortcut.** Preserve user data and compatible schema history.

---

# PHASE 1 — BASELINE, STARTUP AND PROJECT STABILITY

## 1.1 Project inventory

Inspect and map:
- `settings.gradle(.kts)`, root/app Gradle files, version catalog, wrapper files, `gradle.properties`.
- `AndroidManifest.xml`, application class, activities, services, receivers and permissions.
- Compose navigation, screen routes, ViewModels, state holders and UI components.
- Media3 player/service/controller lifecycle.
- Room database, entities, DAOs, migrations and repository layer.
- DataStore preferences and persisted settings.
- MediaStore scanner, SAF scanner, metadata parser and import flows.
- Artwork resolver/cache, Coil configuration and image requests.
- Existing audio effects / equalizer implementation.
- Existing fast-scroll implementation.
- Tests, changelog and release configuration.

Search for all references to removed features before deleting anything. Identify the actual app ID, namespace, version, SDK levels and dependency versions.

## 1.2 Safe baseline and build

- Preserve `applicationId`, namespace, signing configuration, keystore references, package structure and user data.
- Do not upgrade Gradle, Kotlin, AGP, Compose BOM, Media3 or SDK versions merely for novelty.
- Fix only verified build blockers.
- If Gradle wrapper files are absent, do not pretend `./gradlew` works. Use an available compatible Gradle installation if possible; otherwise report the blocker.
- Capture baseline compile/build failures before modifying code.
- Create an untouched project backup.

## 1.3 Startup and navigation

Verify:
- Cold launch reaches the first usable screen.
- No blocking database scan, artwork decode, network request or metadata parse occurs on the main thread.
- Navigation does not create duplicate player services, duplicate collectors or leaked controllers.
- Back navigation and process recreation behave correctly.
- Empty library, permission denial, revoked SAF access and offline states have clear UI.
- No infinite loading indicator when an operation has failed.

**Phase 1 exit gate:** app compiles (where build environment permits), launches (where device/emulator permits), navigation is stable, and no known startup blocker remains.

---

# PHASE 2 — LOCAL LIBRARY, IMPORT AND METADATA REPAIR

## 2.1 Scanner correctness

Audit MediaStore and SAF independently.

Requirements:
- Query only audio media with valid content URIs.
- Use stable media IDs and stable URI identity; do not use list position as track identity.
- Handle duplicate tracks across MediaStore and SAF without deleting valid entries.
- Handle file moves, renames, deletions and changed `dateModified`/size.
- Handle permission revocation and re-grant.
- Do not rescan the entire library on every screen recomposition or every app resume.
- Use incremental scan/delta logic where possible, with a safe full-rescan fallback.
- Perform I/O and parsing on `Dispatchers.IO`; publish immutable snapshots/state to the UI.
- Batch Room writes to avoid thousands of individual transactions.
- Keep scan progress cancellable and avoid concurrent overlapping scans.
- Ensure imported tracks appear promptly and the displayed count matches the actual library.

## 2.2 Metadata parsing

Support available Android metadata for title, artist, album, album artist, track/disc number, duration, MIME/container, bitrate/sample rate where reliably available, year and embedded artwork.

- Decode UTF-8 and Unicode correctly.
- Handle missing, malformed and blank tags gracefully.
- Prefer embedded artwork over generic album art.
- Never invent metadata.
- Do not add a large custom ID3 parser unless Android's retriever demonstrably fails on real files and a tested parser is justified.
- Do not parse large audio files on the main thread.
- Make parser errors observable in diagnostics without exposing private file paths unnecessarily.

## 2.3 Artwork resolver — use the supplied reference, but harden it

Use the user's `VesperArtworkResolver` sample as a starting design:
- Deterministic per-track cache identity using track ID plus modification time and file size.
- Embedded artwork first.
- Album-art URI fallback only when safe.
- Downsample before full bitmap decode.
- Prefer `RGB_565` for opaque thumbnails only when visual quality is acceptable; preserve alpha when needed.
- Release `MediaMetadataRetriever` in `finally`.
- Cache files must be validated (non-empty, decodable, reasonable dimensions).
- Never return a generic album cover when it is known to belong to a different track/album.
- Use bounded disk cache with LRU/age eviction and a safe cache-clear operation.
- Avoid unbounded cache growth and stale cache files.
- Do not use `Uri.fromFile()` where a file provider/content URI is required by another process. Keep internal artwork URI handling inside the app or expose through a secure provider if needed.
- Do not store artwork in Room as huge bitmap blobs.

### Artwork concurrency and lifecycle

- Resolve artwork asynchronously on `Dispatchers.IO`.
- Deduplicate concurrent extraction for the same cache key (single-flight/in-flight job map).
- Cancel work when no longer needed where safe.
- Avoid re-decoding the same image during recomposition.
- Use appropriately sized requests: small list thumbnails, medium grid art, larger Now Playing art.
- Do not decode 3000px artwork for a 64dp row.
- Handle corrupt, unsupported, missing and revoked URI sources without crashing.
- Add placeholder/error visuals that preserve layout dimensions and do not flash/jump.

### Reference code warning

The user's sample uses `trackId` as a `String`; adapt it to the real Track model. Check cache key sanitisation and path safety. `maxEdge / sampleSize > 1024` is a reasonable upper-bound concept, but use target dimensions appropriate to each UI surface. Do not assume `RGB_565` is suitable for transparent artwork. The album-art content URI may return the wrong image if the album ID or provider mapping is wrong; validate it or omit that fallback.

## 2.4 Library performance

- Use Paging/LazyColumn/LazyVerticalGrid appropriately.
- Use stable keys and content types for lazy items.
- Do not sort/filter a large library inside composable bodies.
- Cache derived sort/index data in repository/ViewModel.
- Avoid repeated full-list copies and repeated `indexOfFirst` in hot paths.
- Preserve scroll state across navigation where sensible.
- Do not preload all full-resolution artwork.
- Ensure the library remains responsive with a large local collection.

**Phase 2 exit gate:** MediaStore and SAF import are reliable; metadata/artwork are correct and asynchronous; no known UI-thread disk or bitmap work remains.

---

# PHASE 3 — MEDIA3 PLAYBACK AND QUEUE: ZERO FREEZES

## 3.1 Establish one source of playback truth

Trace the complete path:
UI action → ViewModel → repository/queue model → Media3 controller/service → ExoPlayer → state flow → mini-player/Now Playing UI.

- Keep one authoritative player instance in the service.
- Do not create a second ExoPlayer for Now Playing or visualizer.
- Keep player operations on the correct application/player thread.
- Keep UI state as a projection of the actual Media3 player state.
- Avoid two competing queue states (one in ViewModel and a different one in Media3).
- Preserve the intended queue when playback starts from Songs, Albums, Search/Discover, Favourites or playlists.
- Ensure selected track, mini-player, Now Playing, notification and MediaSession metadata agree.

## 3.2 Required manual Next/Previous behaviour

The user supplied circular `skipNext()` and `skipPrevious()` examples. Implement the intended **manual skip wrap-around** robustly:

- Next from any non-final queue item selects the next item.
- Next from the final item wraps to queue index `0`.
- Previous from any non-first item selects the previous item.
- Previous from the first item wraps to the final item.
- Previous pressed after more than 3 seconds restarts the current track at position 0; otherwise it selects the previous queue item.
- An empty queue or a current item not found in the queue must safely no-op or recover through an explicitly defined queue-reconciliation path. Never index `-1`.
- A one-item queue must behave deterministically (manual Next/Previous restarts that item without crashing).
- Queue updates and rapid repeated taps must not create stale-index races.

**Important distinction: manual transport vs natural completion.** The user's circular skip sample defines manual button behaviour. Do not automatically restart a finished queue when Repeat is OFF merely because manual Next wraps. Implement natural end-of-queue separately:
- `REPEAT_MODE_ONE`: replay current item on natural completion.
- `REPEAT_MODE_ALL`: continue from first item after final item.
- `REPEAT_MODE_OFF`: stop/ended state after the final item; do not silently restart the queue.
- Manual Next/Previous may still wrap as explicitly requested.
- Keep Media3 repeat mode and app queue semantics consistent. Do not implement two independent repeat engines that fight each other.

## 3.3 Queue controller implementation requirements

Create a small testable queue policy/controller (or improve the existing player client) with:
- Pure index-resolution functions for next/previous.
- Explicit handling for empty queue, missing current ID, one-item queue and boundaries.
- Stable identity comparison using the real track/media ID.
- No blocking locks, disk/network work or expensive allocations on the playback thread.
- No out-of-bounds array access.
- A single place responsible for queue mutations.
- Correct synchronization between app queue and `player.currentMediaItemIndex`.
- Correct queue rebuilding when a user taps a track from a filtered/search result list.
- No accidental fallback to full-library queue after starting from a context queue.

Do not add `prepare()` on every skip if Media3's current playlist/seek operation already supports the transition. Prefer updating/seeking the existing playlist and let Media3 manage preparation. If a custom one-item transition is needed, justify and test it.

### Pseudocode policy (adapt to actual architecture)

```kotlin
internal fun nextIndex(currentIndex: Int, size: Int): Int? {
    if (size <= 0 || currentIndex !in 0 until size) return null
    return (currentIndex + 1) % size
}

internal fun previousIndex(currentIndex: Int, size: Int): Int? {
    if (size <= 0 || currentIndex !in 0 until size) return null
    return (currentIndex - 1 + size) % size
}
```

Use these only for manual transport. Natural completion follows repeat mode separately.

## 3.4 Error handling and audio focus

- Inspect `onPlayerError()` for retry/skip loops.
- Do not endlessly skip through every track when one decoder/source fails.
- Retry only transient failures with a bounded retry count/backoff.
- Mark a failed item and surface a concise error.
- Release MediaController/ExoPlayer resources correctly.
- Handle audio focus loss, ducking, noisy headset unplug, Bluetooth route changes, phone calls and foreground service lifecycle.
- Support notification/media controls and lock-screen metadata.
- Ensure playback continues when navigating between screens.
- Test seek, pause/resume, rapid next/previous, queue replacement and process recreation.

**Phase 3 exit gate:** queue controls never freeze or crash; manual wrap-around is correct; natural completion respects repeat mode; error recovery is bounded.

---

# PHASE 4 — NOW PLAYING, VISUALIZER AND COMPOSE PERFORMANCE

The user reports the Now Playing / artwork / visualizer experience feels laggy. Diagnose before redesigning.

## 4.1 Profile and identify bottlenecks

Inspect:
- Main-thread work and frame timing.
- Artwork decode size and frequency.
- Coil request churn and cache configuration.
- Blur, backdrop and shader costs.
- Visualizer update frequency and Canvas drawing.
- Compose recomposition scope and state collection.
- Nested scroll/gesture competition.
- Allocation/GC pressure.
- Player position polling and coroutine/ticker count.
- Duplicate player/controller/service instances.

Do not assume one component is responsible. Record reproducible observations.

## 4.2 Visualizer

- Visualizer is decorative and must never block audio playback.
- Use a supported audio-session visualizer/effect API only when available and permitted.
- If no valid audio session/effect is available, show a calm idle state rather than fake reactive data.
- Throttle visualizer updates to a sensible frame rate (for example 30–45 fps), not hundreds of state updates per second.
- Isolate visualizer state to its own composable/Canvas.
- Avoid triggering recomposition of the whole Now Playing screen.
- Reuse arrays/buffers; avoid per-frame object allocation.
- Stop visualizer work when the screen is not visible or app is backgrounded, unless the chosen design genuinely needs it.
- Do not create a second player or decode the track a second time for visualization.
- Respect audio recording/visualizer permissions and API restrictions; do not request microphone permission for a playback visualizer unless truly required and justified.

## 4.3 Artwork and backdrop

- Keep a stable layout while images load.
- Prefer one properly sized artwork request, with an optional blurred low-resolution derivative.
- Do not run a large real-time blur every frame.
- Cache a pre-blurred thumbnail if blur is retained.
- Do not animate large full-screen image transformations continuously.
- Use a restrained static gradient or low-cost tint as fallback.
- Preserve contrast and legibility.

## 4.4 Compose rules

- Use stable keys, immutable UI models where valid and narrow state collection.
- Isolate high-frequency playback position and visualizer state from the whole screen.
- Use `derivedStateOf` only where it reduces meaningful recalculation.
- Use lambda-based modifier APIs for frequently changing values where appropriate.
- Do not wrap everything in `remember` blindly.
- Avoid broad state objects that invalidate the full screen every 100ms.
- Avoid nested scroll containers that fight over gestures.
- Keep animations short, purposeful and lifecycle-aware.
- Do not use expensive infinite animations as background decoration.

**Phase 4 exit gate:** Now Playing remains responsive while playback continues; artwork loads without blocking; visualizer updates are isolated and bounded.

---

# PHASE 5 — REMOVE RETIRED FEATURES SAFELY

Remove Appearance Studio, Downloader, Artist Cache, Playback History, Listening History and Listening Stats as requested.

- Remove navigation destinations, settings entries, screens, ViewModels, repositories, workers, services, receivers, notifications and feature-only permissions.
- Remove dependencies only if no remaining feature uses them.
- Remove artist scraping and history event writes.
- Keep core artist/album metadata required to display the library.
- Preserve favourites, playlists, current queue, local library and audio presets.
- Migrate Room safely. If deleting a table, create a versioned migration and test it against an existing database snapshot.
- Never wipe the database.
- Update documentation, strings, menus and changelog so removed features are not still advertised.
- Search for dead references and unused resources after removal.

**Phase 5 exit gate:** no dead navigation or crash paths; user music/favourites/playlists/audio settings remain intact; migrations are safe.

---

# PHASE 6 — STUDIO TONE: REAL AUDIO CONTROL, NOT MARKETING EFFECTS

Build a polished **Tone** section that provides meaningful, measurable audio controls. This is a core product feature, not a decorative screen.

## 6.1 Audio-engine architecture

- Keep Media3/ExoPlayer as the playback engine.
- Attach supported Android audio effects to the correct audio session ID and lifecycle.
- Use platform effects only after checking availability and device support.
- Use a clean abstraction such as `ToneEngine` / `AudioEffectController` with state exposed to the UI.
- Avoid one effect instance per screen; effects belong to the playback session and must be released safely.
- Do not attach duplicate equalizers/compressors after every navigation.
- Persist user presets/settings safely using DataStore or the existing settings repository.
- When playback session ID changes, reattach/reconfigure effects without losing the selected preset.
- If an effect is unsupported, disable that control with a truthful explanation rather than showing a fake active state.
- Audio processing must not block the main thread or playback callback.

## 6.2 Tone screen experience

Create a premium, simple screen with:
- Master Tone enable/disable.
- Preset selector.
- 10-band graphic equalizer (or the actual number of bands exposed by the platform effect).
- Per-band gain sliders with readable frequency labels.
- Preamp/headroom control or automatic headroom mode.
- Bass enhancement only if backed by a real supported effect.
- Balance control (left/right).
- Crossfeed only if genuinely implemented; otherwise mark as a later capability, not a fake toggle.
- Reset to flat.
- Save, rename, duplicate and delete user presets.
- Clear active-state indication and current preset name.
- Accessibility labels and sensible slider increments.
- A compact mini-player/Tone status indicator if it fits the existing design.

Do not pretend every Android device exposes identical EQ band counts, ranges or effects. Query capabilities at runtime and adapt the UI.

## 6.3 Equalizer correctness

- Query `Equalizer` capabilities: band count, band level range and center frequencies.
- Convert milliBel/dB correctly. Validate all gain conversions.
- Clamp every value to hardware-supported ranges.
- Do not hardcode ten frequencies if the device exposes a different band layout. If a software DSP is implemented, it may expose a consistent 10-band layout.
- Keep user-facing gain values in dB.
- Flat preset sets all bands to 0 dB.
- Do not store invalid values or apply settings to a stale audio session.
- Ensure the selected preset actually changes audio output, not merely slider appearance.
- Test with different output routes and devices where available.

## 6.4 Headroom and clipping protection

Use a conservative preamp policy when positive EQ boosts are active. A practical starting rule is:

`preampDb = -max(0 dB, maximumPositiveBandGainDb) - safetyMarginDb`

A safety margin around 1–2 dB can be used as a configurable default, but verify with actual implementation and avoid unnecessary loudness loss when all bands are flat/cut-only.

Important:
- If platform Equalizer does not expose preamp or limiter control, do not claim the platform EQ has implemented an automatic limiter.
- Apply a real software preamp only if the signal path supports it.
- A true look-ahead limiter/compressor requires a real PCM processing stage; do not simulate it with UI state.
- Avoid clipping; test boosted presets with peak-heavy audio.
- Document the actual signal chain and any platform limitations.

## 6.5 DSP options — only implement what can be verified

Possible advanced controls, in order of priority:
1. Graphic EQ and preamp/headroom.
2. Bass enhancement using supported `BassBoost` or tested DSP.
3. Virtualizer/spatial effect only if supported and accurately labelled.
4. Crossfeed only with a real stereo DSP implementation.
5. Optional experimental stereo-width/panning effect only if implemented as actual signal processing and clearly labelled as a creative effect.

Do not ship claims such as:
- Dolby Atmos / Dolby Audio / Dolby Pro.
- Hi-Res certification or bit-perfect output.
- Lossless conversion or restoration of lost MP3 data.
- True 8D, spherical sound or physical 3D audio.
- Vendor hardware bypass or direct DAC access.

A standard Android app cannot promise direct hardware DAC bypass across devices. Android audio effects and vendor processing vary by OEM and output route. Keep product copy honest.

## 6.6 Audio quality and performance validation

- Verify effects attach to the current Media3 audio session.
- Verify enabling/disabling Tone does not interrupt playback.
- Verify presets persist after process death.
- Verify no duplicate effect instances or leaks.
- Compare flat vs preset output at matched loudness where possible.
- Check clipping/harshness and avoid extreme default boosts.
- Test speaker, wired headphones and Bluetooth when available.
- Report physical-device tests as `NOT RUN` if no device is connected.

**Phase 6 exit gate:** every visible Tone control changes a real supported audio parameter or is clearly unavailable; settings persist; no playback regression.

---

# PHASE 7 — DISCOVER: A USEFUL, LAWFUL, PAGINATED MUSIC CATALOG

Discover is optional online functionality. It must never compromise local playback or privacy.

## 7.1 Information architecture

Under the Discover search field, add a clear segmented selector:
- **Songs**
- **Albums**
- **Artists**

Expected behaviour:
- Search query is shared across categories.
- Switching category retains the query and scroll state where sensible.
- Empty state, loading state, no-results state, offline state and provider-error state are distinct.
- Debounce user input.
- Cancel stale in-flight searches when a new query arrives.
- Do not show old results as if they match the new query.
- Support pagination/infinite loading without loading the entire catalog.
- Use stable result IDs and source labels.
- Search results must be playable only when the provider supplies a valid permitted stream URL.
- Tapping an album/artist opens a real detail view populated from the provider; no placeholder details.
- Add source attribution and outbound provider links where required by API terms.
- Clearly distinguish remote results from local library results.
- Do not download/cache audio unless provider terms explicitly permit it.

## 7.2 Provider architecture

Use provider interfaces and a unified domain model. Keep network implementation isolated from Compose UI.

Recommended first release:
- **Audius**: primary source for actual streaming content through its official API, subject to current endpoint terms, availability and rate limits.
- **MusicBrainz**: metadata enrichment/lookup only. It is not a music streaming provider and does not grant rights to recordings.
- **Local library**: always available and independent of network.
- **Jamendo**: optional independent/Creative Commons catalog only after checking current API terms, track-level licence and whether the app's distribution/use qualifies.
- Do not depend on Last.fm for streaming; it is metadata-oriented and default API terms have non-commercial restrictions.
- Do not ship TheAudioDB free API in a public app-store build unless current paid-plan terms explicitly allow that use.
- Do not use NewPipe, Piped, website scraping, undocumented YouTube extraction, stream URL circumvention or third-party download extractors as a production music source.

## 7.3 Legal and catalogue truth

- A large metadata index is not the same as a large playable catalogue.
- MusicBrainz metadata does not provide audio streams or permission to play copyrighted recordings.
- Audius catalog coverage is not guaranteed to include every mainstream/commercial song.
- Every provider's current API terms, branding requirements, rate limits and attribution requirements must be checked before release.
- For Jamendo or any Creative Commons catalog, verify the licence of each track and follow attribution, commercial-use, share-alike and no-derivatives restrictions as applicable.
- Do not describe the app as legally risk-free. No API or open-source licence can guarantee zero legal liability.
- Implement a contact/takedown process and preserve source/rights metadata for remote results.
- Do not assume a non-profit label automatically makes commercial-use restrictions irrelevant.
- Never claim 10M+ playable tracks unless the actual licensed provider catalogue proves it.

## 7.4 API/network implementation

- Use Retrofit/OkHttp or the project's existing HTTP stack.
- Use a single shared HTTP client with explicit timeouts and safe connection reuse.
- Add a meaningful User-Agent where required.
- Respect provider rate limits; MusicBrainz requests must be throttled to no more than one request per second per client.
- Use pagination and bounded page sizes.
- Debounce search (roughly 300–500ms is a sensible starting point; tune based on UX).
- Cancel stale requests.
- Add bounded retries with exponential backoff and jitter for transient failures only.
- Do not silently switch to an unrelated provider and show its results as if they came from the primary provider.
- Failover may occur only between approved providers with clearly identified source attribution and compatible rights.
- Cache lightweight metadata with TTL and size bounds; do not cache protected audio streams.
- Do not store an entire global catalog in the APK or local database.
- Keep API keys out of source control and the APK where possible. If a provider requires a public client key, treat it as extractable and apply restrictions/rate limits; never embed a private secret.
- Offline mode must still show local library results.

## 7.5 Remote playback

- Use provider-approved stream endpoints only.
- Check streamability before showing a Play action.
- Handle expired URLs, HTTP errors, unavailable tracks, network loss and provider throttling.
- Keep remote queue items identifiable by provider + provider track ID.
- Do not persist short-lived signed URLs as durable track identity.
- Keep local and remote queues distinguishable and safely switchable.
- Show source attribution in remote Now Playing.
- Do not add download/offline caching for remote audio unless explicitly permitted.

## 7.6 Discover result layouts

### Songs
Show artwork thumbnail, track title, artist, provider/source, duration only when supplied reliably, and format/bitrate only when verified. Show Play only when streamable.

### Albums
Show artwork, album title, artist, release year/date if supplied, provider/source, and a real album detail page with track list if available.

### Artists
Show artist name, supplied artwork/avatar, provider/source, and a real artist detail page with available releases/tracks.

Do not fabricate album covers, verified badges, format badges or bitrates.

## 7.7 Discover and local A–Z navigation

Inspect and reuse the existing `AlphabeticalFastScrollRail` / `FastScrollRail.kt`. Do not add a duplicate implementation.

- Support local Songs and sufficiently large Discover result lists.
- Use a stable precomputed alphabet index for the currently displayed sorted list.
- Handle `#` and A–Z, empty groups and non-Latin names.
- Use binary search or precomputed first-index maps rather than scanning the entire list on every drag event.
- Keep haptics optional and low intensity.
- Do not force alphabetical sorting onto provider rankings where relevance order is expected. In Discover, use the rail only when the user explicitly selects/switches to alphabetical sort, or provide a clearly labelled sort control.
- Do not claim sub-millisecond performance without measuring it.

**Phase 7 exit gate:** all three tabs perform real provider-backed searches; paging works; streamable results play; provider attribution and rights metadata are retained; local library remains fully functional offline.

---

# PHASE 8 — PREMIUM UI AND INTERACTION DESIGN

## 8.1 Visual identity

Use near-black / AMOLED black base (`#000000` or carefully chosen near-black surface), restrained deep burgundy/wine accent, warm off-white primary text, muted secondary text, clear typography, deliberate spacing, subtle dividers, tonal surfaces and small purposeful transitions.

Avoid excessive blur/glow, competing accent colours, huge decorative artwork, constantly animated backgrounds, overcrowded cards, tiny labels, inconsistent padding and pixel-for-pixel copying of another app.

## 8.2 Screen quality

Polish Home, Songs/library, Albums and Artists if supported, Discover, Now Playing, Tone, Queue and Settings. Every screen needs clear hierarchy, responsive insets, accessible semantics/touch targets, loading/empty/error states, no clipped labels, stable artwork layout and correct contrast.

## 8.3 AMOLED and resolution

- Android apps do not select a user-facing “1.8K rendering mode”. Build density-aware layouts and crisp vector assets.
- Do not force device resolution or density.
- Use vector icons where suitable, but do not remove useful raster artwork or fonts just to satisfy a slogan.
- Pure black can save power on OLED pixels, but do not promise zero display power or infinite real-world contrast in all conditions.

---

# PHASE 9 — DATABASE, PRIVACY, SECURITY AND RELEASE HYGIENE

- No analytics or advertising SDKs.
- No hidden tracking or device fingerprinting.
- Request only permissions required for visible functionality.
- Do not request microphone permission for a playback visualizer unless genuinely required.
- Keep network access limited to Discover/provider features.
- Use HTTPS.
- Validate all remote URLs and provider responses.
- Do not log access tokens, private file paths, personally identifying data or full signed stream URLs.
- Keep API credentials out of source code.
- Preserve user-created data.
- Use Room migrations with tests.
- Ensure SAF URI permissions are persisted and handled when revoked.
- Avoid broad storage permissions where MediaStore/SAF is sufficient.
- Audit manifest exported components and intent filters.
- Remove unused permissions after Downloader removal.
- Ensure clear app identity, attribution and privacy disclosures.

---

# PHASE 10 — PERFORMANCE, SIZE AND QUALITY GATES

## 10.1 Performance

Measure where possible: cold/warm launch, library scan time on representative collection, scroll frame stability, artwork cache hit/miss, Now Playing responsiveness, visualizer CPU, memory/bitmap churn, queue skip latency and Discover search/pagination latency.

Do not invent target results. Record device/emulator, OS version, test dataset and measurement method.

## 10.2 APK size

Target release APK: **under 30 MB**, but this is a measured goal, not a guarantee.

- Measure actual release APK and, if relevant, AAB download size separately.
- Enable R8/minification and resource shrinking only after verifying release behaviour.
- Remove genuinely unused dependencies and resources.
- Exclude unused Media3 modules/codecs only after confirming supported local formats and stream formats still work.
- Do not remove codecs or native libraries blindly.
- Do not add a large DSP library if Android platform effects meet the requirement.
- Do not claim a billion-track catalogue is stored locally. The APK contains the client; remote results are paged.

## 10.3 Test matrix

Run what the environment supports:
- Unit tests for queue index policy and repeat behaviour.
- Unit tests for metadata parsing and cache key creation.
- Tests for empty library and missing current queue item.
- Room migration tests against existing schema/data.
- Playback tests: play, pause, seek, manual next/previous, one-item queue, queue boundaries, natural completion, repeat modes, failed URI.
- Import tests: MediaStore, SAF, duplicates, malformed tags, missing art, corrupt art, revoked permission.
- Tone tests: capability discovery, flat preset, saved preset, effect attach/release, session ID change.
- Discover tests: debounce, cancellation, paging, provider errors, offline mode, stream URL expiry, source identity.
- UI tests for navigation, empty/error/loading states and controls.
- Build debug and release variants if possible.

For every test, report `PASS`, `FAIL` or `NOT RUN` and explain why.

---

# PHASE 11 — CHANGELOG AND VERSIONING

Inspect the existing version before updating. Do not invent historical feature details or dates.

The user wants the release story to reflect:
- 2.6: Downloader removed/dropped.
- 2.7: improvements to hardware-backed audio and sound modes.
- 2.8: only features that were genuinely present and retained.
- 2.9: stability repairs, performance improvements, Tone and Discover upgrades.

Only include a historical statement if supported by existing changelog/source or clearly label it as user-provided history. Do not leave removed Downloader/Appearance Studio features advertised in current release notes.

Increment `versionCode` correctly for a release. Do not change signing identity.

---

# PHASE 12 — FINAL ACCEPTANCE AND HANDOFF

Before delivery:
1. Search for TODOs, placeholders, fake controls, dead routes, stale strings and removed-feature references.
2. Build the project.
3. Run available tests.
4. Inspect the final project tree.
5. Confirm migrations and data preservation strategy.
6. Measure APK size if a release APK was built.
7. Create the complete project ZIP, excluding only generated build/cache folders that can be safely regenerated. Include wrapper files and source.
8. Confirm the ZIP exists and is non-empty.
9. Write `IMPLEMENTATION_REPORT.md`.

## Final report format

### Summary
What was actually implemented.

### Repairs
- Startup/build:
- Import/metadata/artwork:
- Playback/queue:
- Now Playing/visualizer:
- Removed features:
- Tone:
- Discover:
- UI/performance:

### Verification
| Check | Result | Evidence / notes |
|---|---|---|
| Debug build | PASS / FAIL / NOT RUN | |
| Release build | PASS / FAIL / NOT RUN | |
| Unit tests | PASS / FAIL / NOT RUN | |
| Room migration | PASS / FAIL / NOT RUN | |
| Playback device test | PASS / FAIL / NOT RUN | |
| Tone device test | PASS / FAIL / NOT RUN | |
| Discover API test | PASS / FAIL / NOT RUN | |
| APK size | Measured / NOT BUILT | |

### Limitations
State every remaining issue plainly.

### Deliverables
- Full project ZIP path.
- APK path, if built.
- Report path.
- Changelog path.

---

# APPENDIX A — USER-SUPPLIED REFERENCE CODE AND IMPLEMENTATION INTENT

The user supplied two code concepts. Preserve their intent, but adapt them to the actual source architecture.

## A1. Circular manual skip

The intended manual behaviour is circular:
- `skipNext()`: final item → first item.
- `skipPrevious()`: first item → final item.
- Previous pressed after 3 seconds → restart current track.
- Empty queue / unknown current item → safe no-op or controlled reconciliation.
- Use stable IDs, never assume list indices remain valid after asynchronous queue changes.

Recommended pure index logic:

```kotlin
internal fun nextIndex(currentIndex: Int, size: Int): Int? {
    if (size <= 0 || currentIndex !in 0 until size) return null
    return (currentIndex + 1) % size
}

internal fun previousIndex(currentIndex: Int, size: Int): Int? {
    if (size <= 0 || currentIndex !in 0 until size) return null
    return (currentIndex - 1 + size) % size
}
```

Do not confuse manual skip with natural completion. Repeat OFF should end after the final track; Repeat ALL should loop on natural completion; Repeat ONE should replay the current item.

## A2. Artwork resolver concept

The user supplied `VesperArtworkResolver` using `MediaMetadataRetriever`, embedded artwork extraction, deterministic track-specific cache filename using track ID/modification time/size, bounds decode then downsampled decode, RGB_565 for suitable opaque thumbnails, album-art URI fallback, cache-size and clear-cache helpers.

Use the hardened requirements in Phase 2. Do not paste the snippet blindly: adapt model types, URI security, cache validation, target sizes, concurrency and disk eviction. Keep all extraction/decode work off the main thread.

---

# APPENDIX B — ENGINEERING GUARDRAILS

- Keep Media3 unless a verified blocker requires a different playback engine.
- Do not migrate to Oboe/AAudio merely for marketing or speculative performance.
- Do not promise bit-perfect output through the Android audio stack.
- Do not imply EQ can restore information removed by lossy compression.
- Do not hardcode a 10-band hardware EQ layout when device capabilities differ.
- Do not build an imaginary “1B+ song index” or store a global catalogue in the APK.
- Do not use NewPipe/Piped/YouTube extraction as the Discover streaming foundation.
- Use official provider APIs and verify current terms before release.
- Keep source identity and licence/attribution information attached to remote content.
- Do not add a second A–Z rail; repair/reuse the existing one.
- Do not create a second player for visualizer or Now Playing.
- Do not run metadata extraction, image decoding, database scans or network calls on the UI thread.
- Do not silently swallow exceptions. Handle expected failures and log useful, privacy-safe diagnostics.
- Do not remove user data to simplify implementation.
- Do not sacrifice playback stability or supported formats to hit an APK-size target.

**Definition of done:** VESPER launches reliably, imports local music accurately, plays and navigates queues without freezing, renders Now Playing smoothly, exposes real Tone controls, offers genuine provider-backed Discover search/streaming within provider terms, preserves user data, and ships with an honest test/build report.
