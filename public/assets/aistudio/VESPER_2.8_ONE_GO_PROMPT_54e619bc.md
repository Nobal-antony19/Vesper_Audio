# VESPER 2.8 — One-Go Stabilisation Prompt (for Google AI Studio)

Baseline: the attached 2.7 project (package `com.vesper.audio`, single `:app` module, Compose + Media3 + Room + DataStore + Coil).
Goal: fix everything below in ONE reply, without rewriting what already works (library cache, playback service, Room, downloader entry point, VESPER burgundy identity).

---

## 0. HOW TO WORK (token-efficient, no drift)

1. **Read before writing.** Open only the files named in §2–§9 plus what they call. Do not summarise the project back to me.
2. **Output only changed or new files**, each as `// FILE: app/src/main/java/com/example/...` followed by the complete file. Never paste unchanged files. Never output diffs or "rest unchanged" placeholders.
3. **One reply, in the order of §10.** If the reply would be cut off, stop after a *complete* file, write `CONTINUE FROM: <next item>`, and I will say "continue".
4. **Before the code, write at most 12 lines**: the verified root cause for each of R1–R8 (name file + line/function). If a suspected cause in this prompt is wrong, say so and use the real one.
5. **Do not**: add libraries beyond what the project already uses, add accounts/analytics/telemetry, change applicationId/namespace/SDK/AGP/signing, init git, remove the Downloader entry, or replace real data with mock or demo data. Keep it offline-first.
6. **Never claim** a build, test or device check you did not run. The ZIP has no `gradlew`/wrapper JAR; if you cannot build, say "NOT BUILT" and give the exact reason.
7. After the code, give the **final report** (§12) in ≤ 25 lines.

---

## 1. EVIDENCE FROM MY PHONE (screen recordings, trust these over assumptions)

| # | What the video shows | Meaning |
|---|---|---|
| E1 | Appearance Studio: I pick a purple accent. Only the selected Light chip, the picker ring and the Background Blur slider fill turn purple. The live preview card, toggles (cream thumb / red-pink track), crossfade slider dots, Now Playing controls (cream thumb, cream icons, burgundy play button, burgundy waveform), mini-player and hearts stay burgundy/cream. | Accent is not reaching most components. Many hard-coded colours. |
| E2 | I select **Light** theme. The whole app stays dark. | Theme mode is saved but not applied to the real theme. |
| E3 | I tap **Custom image**, the system Photos picker opens, I choose a photo and return. Nothing changes. | Picked image is never copied/committed/rendered. |
| E4 | Songs tab, query "Taylor": 96 results. Heart icons stay outline (never pink). Alphabet side-index overlaps the rows' right edge and the ⋮ buttons. | Heart state not read from Room per row; layout misaligned. |
| E5 | Tapping a search result does not start that song. Mini-player kept showing "1989 / Nightly" while Now Playing header said "PLAYING FROM THE VOID". | Click does not reach the player, or queue/source state is stale. Source label and current item disagree. |
| E6 | Now Playing for the second track shows a flat dashed waveform and the heart outline. | Waveform fails silently for some tracks; heart not bound to favourite state. |
| E7 | Home: "Recently Added" shows album/art tiles and "1458 tracks"; "All Songs" still present; nav bar is the old 5-item bar (Home, Songs, Albums, Artists, Discover). | Home needs the rework in §4. |

---

## 2. R1 — SEARCH MUST PLAY THE TAPPED SONG (blocker, do first)

Trace: `SearchScreen` / Songs-tab search → `SonoraMainContent` → `SonoraViewModel.playTrack` → `SonoraPlayerClient.playTrack` → `MediaController` → `SonoraMediaService`.

Find the real break. Check at minimum:
- the row's `clickable` is not swallowed by a parent (`combinedClickable`, `pointerInput`, an overlay such as the alphabet index or a transparent selection layer);
- the index passed is the index **inside the list that was passed**, not in the full library;
- `setMediaItems(items, startIndex, startPositionMs)` + `prepare()` + `play()` all run, on the main thread, with the controller connected (queue the command if not yet connected);
- `MediaItem` has a non-empty `mediaId` and valid `contentUri` (MediaStore `content://` or SAF `content://…/document/…`); handle `SecurityException` / `FileNotFoundException`;
- `Player.Listener.onPlayerError` and `onMediaItemTransition` update UI state; the **source label** ("Playing from …") is derived from the actual queue source set at the same moment as the queue, never from a stale value.

Required behaviour:
- Tapping a result plays exactly that song immediately; Now Playing + mini-player show it.
- Queue = the visible search results (in displayed order); next/previous follow it; "Playing from" = `Search: "<query>"`.
- Rows highlight the current track via `currentTrack.id` and show a play/pause state.
- Works after typing, clearing, changing filter chips and scrolling; keeps query + scroll on return.
- Search runs off the main thread on the cached library, debounced ~200 ms, with `collectLatest`/`flatMapLatest` so old work is cancelled.
- Unreadable file → snackbar "Can't play this file (moved or no permission)" and skip to next; never fail silently.

---

## 3. R2 — SONG ROW, HEART AND ALIGNMENT

Rewrite the shared song row composable (and use it everywhere: Songs, Search, Favorites, Playlist detail, Album detail, Queue):
- Fixed row height 64 dp, 16 dp horizontal padding. Layout: `[art 48dp] [title/artist (weight 1, 1 line ellipsis)] [duration] [heart 40dp] [⋮ 40dp]`.
- The A–Z index is an **overlay** on the far edge: reserve 20 dp end padding on the list's content only while the index is visible; the index must never intercept taps outside its own 20 dp strip.
- Heart: `isFavorite` comes from one `StateFlow<Set<String>>` of favourite track IDs held in the ViewModel (backed by Room). Row reads `id in favouriteIds`; filled pink/accent icon when true, outline when false. Toggle is optimistic, writes Room on `Dispatchers.IO`, and updates every screen at once. Heart tap must not trigger the row click.
- Stable `key = track.id`, `contentType = "track"`.
- Ensure the Now Playing heart uses the same set.

---

## 4. R3 — HOME REWORK (Oto-Music style; keep icons and albums)

Order, top to bottom:
1. Existing VESPER header + search entry.
2. Existing quick-action tiles (Favorites, History, Stats, Shuffle) — keep, with real callbacks.
3. **Recently Added** — horizontal row of cards (art, title, artist), newest first, max 20, "See all" → full list screen sorted newest first.
4. Existing **Albums** carousel (keep).
5. **Favorites** — replaces "All Songs". Compact boxed cards (≈ 140 dp wide, art + title + artist) in a horizontal row or 2-row grid, **oldest-favourited first → newest last** (Room `addedAt ASC`). "View all" opens the full Favorites screen. Empty state: "Tap ♥ on any song to build your favourites."
6. Keep "Recently Played" only if there is real data; otherwise hide it. Never show album placeholders as played songs.

Remove the Home "All Songs" section. The Songs tab stays.

Recently Added correctness:
- Order by real `dateAdded` (MediaStore `DATE_ADDED`) descending. For SAF/downloaded files use the time the file was first indexed/imported (store `addedAt` once at first insert, never overwrite on later syncs).
- No fallback to "whole library" when empty — show an empty state.
- Deduplicate by track id. A newly imported/downloaded file shows up after the incremental sync without a full rescan.
- Fix the label so it shows the count of tracks in the section, not the library total.

---

## 5. R4 — APPEARANCE STUDIO: MAKE EVERY CONTROL REAL

### 5a. Accent everywhere
- Provide one `VesperColors` object via `CompositionLocal` (accent, accentContainer, onAccent, accentMuted, track, surface…) derived from the saved hex. Wrap the **whole** app (including dialogs, bottom sheets, mini-player, Now Playing).
- Run `grep -rn "Color(0x" app/src/main/java` and `grep -rn "Burgundy\|Cream\|Pink"`; replace every interactive use (toggles, sliders, progress, waveform, play button, chips, borders, nav indicator, hearts, FABs, selection, focus rings, dialogs) with theme tokens. Material components must get colours from `MaterialTheme.colorScheme` built from the accent (`primary`, `onPrimary`, `primaryContainer`, `secondary`, `tertiary`, `surfaceTint`) so `Switch`, `Slider`, `Checkbox`, `RadioButton`, `FilterChip`, `ProgressIndicator` follow automatically.
- Readability: choose `onAccent` by luminance. Derive pressed/disabled variants. Do not recolour album art.
- Brand burgundy may stay only in the VESPER logo and splash.

### 5b. Theme mode (E2)
`System / Dark / Light` must actually switch `MaterialTheme` (dynamic light + dark schemes, correct status/navigation bar icon contrast). If a screen is unreadable in Light, fix its colours; do not hide Light.

### 5c. Background image (E3)
- Use `PickVisualMedia(ImageOnly)`. In the callback (cancel = no change): on `Dispatchers.IO`, open stream via `contentResolver`, decode bounds-first with `inSampleSize` to ≤ 1440 px long edge, write JPEG q85 to `filesDir/backgrounds/bg_<epochMillis>.jpg`, verify it decodes, then persist that **path** + increment `backgroundVersion`, then delete the previous file. On any failure keep the old background and show a snackbar.
- Modes: `NONE | ALBUM_ART | CUSTOM` (single source of truth; custom image must win over album-art when mode = CUSTOM).
- **Staged preview with Apply**: changes preview live in the Studio card; Apply writes everything to DataStore and closes; Back/Reset discards (confirm if changed). No Apply button that does nothing.
- Render once at the app root (`VesperBackdrop`: image → blur(radius from slider) → scrim alpha from slider) and make every main screen background transparent so it is visible on Home, Songs, Albums, Artists, Playlists, Search, Settings and Now Playing. Blur via `Modifier.blur` only on API 31+, otherwise pre-blurred downsampled bitmap cached in memory.
- Reset / Remove button returns to default. Survives force-stop and reboot.
- Make the HEX field editable with validation (`#RRGGBB`), updating the preview live.

### 5d. Dynamic artwork backdrop
Optional, off by default, palette cached per track id, never overrides the manual accent unless the user turns on "Use artwork colours for accent".

---

## 6. R5 — SPEED AND SMOOTH SCROLL

Apply, measure nothing you cannot measure, and report only what you actually did:
- `key` and `contentType` on every `LazyColumn/LazyRow/LazyVerticalGrid`; `rememberLazyListState()` hoisted so position survives navigation and Now Playing open/close.
- Collections to UI as `ImmutableList`/`@Immutable` wrappers or stable data classes; mark UI models `@Immutable`.
- Sorting/filtering/grouping/alphabet-index maps computed in the ViewModel/repository with `flowOn(Dispatchers.Default)` and `stateIn(WhileSubscribed(5000))`, never inside composition.
- Rows take plain values + lambdas; no row collects a flow; use `derivedStateOf` for "is current".
- Coil: one shared `ImageLoader` (memory cache ≈ 15 % of heap, disk cache ≤ 64 MB), request size = the displayed size in px (thumbnail 96–128 px, card 320 px, hero 720 px), stable cache keys (`artHash` or track id + version), `crossfade(false)` inside lists, `allowHardware(true)`.
- No per-row infinite animations, big shadows or blur in lists. Glow/animation only on the active nav indicator.
- Enable R8 `minifyEnabled true` + `shrinkResources true` for release (keep rules for Room, Media3, Coil); add a baseline profile only if the project already supports it.
- No main-thread disk or DB I/O. Do not regress the persistent library cache: never rescan on launch, only incremental sync.

---

## 7. R6 — FAST BRANDED LOADING SCREEN

- Use the AndroidX `SplashScreen` API (`installSplashScreen()`): burgundy background + VESPER mark, no custom Activity-level timer, no artificial delay.
- Dismiss as soon as the **cached Room library emits its first value** (or immediately if the DB is empty), cap at 1 s. Do not wait for scanning, artwork or permissions.
- Render Home from cache right away; scanning/refresh runs silently in the background with at most a thin progress indicator, never a blocking "Indexing…" screen.
- Move heavy singletons (Coil loader, EQ, MusicRepository init) off the first frame using lazy init / background coroutine.

---

## 8. R7 — DOWNLOADER THAT WORKS, WITH FREE AND SUPPORTER TIERS

Keep the existing Downloader screen and local SAF importer. Scope = **authorised direct audio URLs only** (links the user owns or that are openly licensed). Do not scrape or rip streaming platforms, bypass DRM or paywalls, and do not label a file "licensed" because it downloaded.

### Core (free, must work)
- `DownloadManager` class owned by the app (not by a screen) using `OkHttp`/`HttpURLConnection` already in the project; a `Foreground`/WorkManager-backed service only if WorkManager is already a dependency, otherwise a foreground service with a notification (progress + cancel).
- Pipeline: validate URL scheme (http/https) → HEAD/GET → check status 2xx, `Content-Type` is audio/* or `application/octet-stream` with a known audio extension, `Content-Length` vs free space → stream to a `.part` file in app cache → on success verify it opens with `MediaMetadataRetriever` and has duration > 0 → publish to MediaStore (`Music/VESPER/`) with `IS_PENDING` flow, unique filename (`name (1).mp3`, never overwrite) → insert into Room incrementally with `addedAt = now` → appears in Songs and Recently Added without full rescan.
- Cancel really cancels the call and deletes the `.part`. Network drop → state `Failed(retryable)` with Retry. HTML-as-audio, empty body, wrong type, low storage → clear error text.
- Persistent history in Room (`downloads` table: id, url, title, state, bytes, total, createdAt, finishedAt, error) with migration; list survives restart; actions: retry, cancel, remove entry, open in player.
- UI: URL field with paste button, queue list with progress ring, speed + size, status chips, clear completed.

### Tiers (design now, billing later)
Create `DownloadEntitlements` (interface + `LocalEntitlements` default) returning:

| Limit | Free | Supporter |
|---|---|---|
| Max parallel downloads | 1 | 4 (configurable 1–6) |
| Queue size | 5 | 200 |
| Batch add (paste many URLs) | no | yes |
| Resume interrupted (HTTP Range + ETag/Last-Modified) | no | yes |
| Wi-Fi-only + schedule | Wi-Fi-only toggle | + schedule |
| Priority reorder | no | yes |

- `isSupporter` is a DataStore boolean. No payment SDK, no accounts. Add a **debug-only** switch (`BuildConfig.DEBUG`) to toggle it for testing. In release, show a neutral "Supporter features — coming soon" card that lists the table, with no fake locked buttons that do nothing: locked actions show a small dialog explaining the limit.
- All limits are enforced in `DownloadManager` (not only in UI) and covered by unit tests.
- Local playback, library, favourites, playlists and basic downloads stay free.

---

## 9. R8 — NAVIGATION

- One navigation state; every bottom-bar destination opens the right screen; Back returns to the previous meaningful destination (not always Home); the logo returns Home.
- Mini-player and bottom bar never cover content: add `contentPadding(bottom = barHeight + miniPlayerHeight)` to every scrollable.
- Keep the current labels (Home, Songs, Albums, Artists, Discover/Search) unless the 2.7 floating dock with glow-trail already exists in the source; if it does, keep it and fix its accent/indicator, otherwise do not redesign the bar in this pass — only make it work and use accent tokens.
- Every drawer item, settings row, dialog button, playlist card and ⋮ menu action has a real callback. Delete dead buttons and toggles that change nothing (check *Low-latency track*, *High-resolution output*, *Gapless*, *Crossfade* — wire each to the player/audio effect or remove it).
- Now Playing: the **Playing from** label always matches the live queue source; waveform failure shows a flat static bar with no error text, but must not break seeking; seek bar thumb and progress use the accent.

---

## 10. ORDER OF WORK (single reply)

1. R1 search playback + queue source (ViewModel, PlayerClient, service glue, search UI).
2. R2 shared song row + favourites StateFlow.
3. R3 Home rework + Recently Added/Favorites data.
4. R4 theme tokens, theme mode, accent audit, background pipeline, Studio draft/Apply.
5. R5 performance edits (lazy keys, Coil sizes, flows off main).
6. R6 splash + cached-first startup.
7. R7 downloader + entitlements + Room migration.
8. R8 navigation fixes, dead toggle cleanup.
9. Version `2.8.0`, `versionCode` +1, `CHANGELOG.md` with only what you actually changed.

Any Room schema change needs a `Migration` (no destructive fallback), version bump and exported schema.

---

## 11. ACCEPTANCE TESTS (state PASS / FAIL / NOT RUN for each)

1. Search "Taylor", tap result 3 → that exact song plays; Now Playing + mini-player match; next goes to result 4; "Playing from" = Search.
2. Heart a song in Songs → pink immediately; same song pink in Search, Now Playing, Home Favorites; unheart removes it everywhere.
3. Favorites on Home: first-hearted leftmost, latest-hearted last.
4. Import/download a file → appears first in Recently Added without a full scan.
5. Pick accent purple → toggles, sliders, waveform, play button, nav indicator, hearts, mini-player all purple; survives force-stop.
6. Choose Light → whole app light and readable.
7. Pick photo → preview → Apply → visible on every tab; force-stop → still there; pick another → old file deleted only after new one works; Reset → default.
8. Cold start with 1458 tracks: Home from cache in ≤ ~1 s, no "Indexing…" screen, no rescan.
9. Scroll Songs (1458) end to end twice: no visible jank, memory stable.
10. Downloader: valid MP3 URL OK; HTML page rejected; cancel removes `.part`; duplicate name gets `(1)`; free tier blocks a 2nd parallel and a 6th queued item with a clear message; history survives restart.
11. Back button and mini-player overlap checks on every tab.

---

## 12. FINAL REPORT FORMAT

1. Root cause per R1–R8 (one line each).
2. Files changed / created / removed.
3. Build command and result, or "NOT BUILT: reason".
4. Tests run with pass/fail counts; the §11 list marked PASS/FAIL/NOT RUN.
5. Known remaining issues.
6. Confirmation that no mock/demo data replaced real library, playback, cache, playlists or downloader.
