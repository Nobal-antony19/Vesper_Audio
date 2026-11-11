# VESPER 2.6 — MASTER IMPLEMENTATION PROMPT
### Corrective patch + Songs redesign + persistent library + playback integrity (one integrated task)

---

## 0. ROLE AND OPERATING CONTRACT

You are a senior Android engineer with production experience in Kotlin, Jetpack Compose, Media3 (ExoPlayer + MediaSession), Room, DataStore, Coil, MediaStore scoped storage, and large-library performance tuning. You are repairing and improving **VESPER**, an existing offline-first, privacy-first Android music player (the codebase may still carry the older internal name "Sonora"; the latest ZIP provided is `sonora (4).zip`).

You will receive: this prompt, the project ZIP, screen recordings of the current app, a screenshot of the current Now Playing screen, and a screenshot of Oto Music's action sheet. **Inspect the real code before believing anything in this prompt.** Every "root cause" below is marked as a HYPOTHESIS until you have confirmed it in the source. Where the code disagrees with this prompt, trust the code and say so in the audit.

### Non‑negotiable rules
1. **Fix real behaviour, not appearances.** No mock-ups, no placeholder UI, no decorative menu items, no cosmetic sliders. A control either works or is hidden/disabled with an honest reason.
2. **Keep the architecture.** Reuse existing screens, ViewModels, repositories, Room entities/DAOs, DataStore, Media3 service/controller, navigation and theme. Do not introduce a second player, a second database, a second DI framework, or a parallel "v2" of anything. Extend, don't duplicate.
3. **Do not touch the Downloader module** unless a listed repair cannot be done without it. If you must, state exactly why in the report.
4. **Preserve user data.** Favourites, playlists (IDs, names, track order), play history, listening sessions, settings, EQ, theme. No `fallbackToDestructiveMigration` anywhere (grep for it and remove it, with a safe migration instead). Every schema change ships with an explicit `Migration`, exported schema JSON, and a `MigrationTestHelper` test.
5. **Privacy-first.** No network calls for lyrics, artwork, analytics or crash reporting. No new permissions beyond what is required (see §5.8). Never request `MANAGE_EXTERNAL_STORAGE`.
6. **Small, safe increments.** Work phase by phase (§3). After each phase: build, run unit tests, commit with a clear message. Do not rewrite the app.
7. **Honesty.** Never invent a passing result. Use exactly these statuses: `PASS` (implemented AND verified, with evidence), `PARTIAL` (works with a stated gap), `NOT IMPLEMENTED`, `DEVICE DEPENDENT` (implemented, needs a physical device/OEM to verify; give exact manual steps), `FAIL`. If no emulator/device is available in your environment, you cannot mark runtime behaviour `PASS` — mark it `DEVICE DEPENDENT` and provide the test steps. Compiling is not testing.
8. **Keep a ledger.** Maintain `docs/VESPER_PROGRESS.md` (phase, files touched, decisions, open issues, test evidence) and update it after every phase, so work survives context loss.
9. **Detect, don't assume.** In Phase 0 detect and record: minSdk/targetSdk/compileSdk, Kotlin/AGP/Compose BOM versions, Media3 version, Room version, Coil version (2.x vs 3.x APIs differ), DI approach (Hilt/Koin/manual), navigation library, the tag-writing library (if any), and which sound modes already exist. Write code against what is actually present.

---

## 1. EVIDENCE FROM THE REFERENCE MATERIAL (what is visibly wrong today)

These were observed in the screen recordings/screenshots of the current build. Treat them as reproduction cases and as regression tests.

### 1.1 Search → playback → swipe leaks into the whole library (CRITICAL)
Songs screen, search "Taylor swift", header shows **"91 Tracks"**. First result: *"the alchemy • you are in love"* (vin's mashups). Starting playback and then swiping/pressing Next on the Now Playing screen goes to *"you're so pretty, you're so…"* (Angel), then *#SaamiSaamial | Pushpa S…* (Aditya Music), then *1989 – Nightly* ("THE VOID"), later *21 Savage & Metro Boomin – Gloc…*, *ASAP FERG PLAIN JANE*, *Attack on Titan Season 4 O…* — none of which are search results. The mini-player proves it: it switched from the search result to *21 Savage*. The queue is being built from (or indexed against) the whole library instead of the 91 filtered rows.
HYPOTHESIS: playback is started with the full list + an index computed against a different list, or the Now Playing pager/Next/Previous are driven by a ViewModel library list instead of the player timeline.

### 1.2 Wrong artwork (data-integrity bug, not cosmetic)
In the search results, *Bad Blood* (Taylor Swift – Topic), *Enchanted – Taylor Swift*, *Enchanted x So High School* all show **Jessie J – "Who You Are"** cover art; Now Playing shows the same Jessie J art for *#SaamiSaamial* and *ASAP FERG PLAIN JANE*. *1989 – Nightly* ("THE VOID") shows an empty grey square.
HYPOTHESIS: artwork cache/Coil key is built from `albumId` or album name (files in the same download folder/album bucket such as "YMusic" share one), or a stale disk-cache entry is returned for a different track. Fix by keying on track identity + file `dateModified`/`artworkVersion`, never on a shared album bucket alone, and never falling back to another track's art.

### 1.3 Inconsistent numbers
Home → "Recently Added … **1457 tracks**"; Songs → "All Songs **(1453)**". Both must come from one repository query (same filters). Find out why they differ (hidden/duplicate/short/missing/unplayable rows?) and make them equal by construction.

### 1.4 Bad/unknown durations and unreadable tracks
Several tracks show **0:00 / 0:00** with a flat dashed progress line (e.g. *#SaamiSaamial*, *1989 – Nightly*, *ASAP FERG PLAIN JANE*). MediaStore frequently reports duration 0/null for freshly downloaded files. Read duration via the metadata retriever as a fallback, and write back the real duration when the player reports it. Do not display "0:00" as if it were data; unplayable files must be handled gracefully (skip with a non-blocking message, flagged in the index, never crash).

### 1.5 "PLAYING FROM" label is inconsistent
It currently shows "YMusic" (a folder), "Joseph James" (an artist), "THE VOID", "emma • 131", "Music Is My Medicine"… with no consistent rule. It must come from the **queue source** (§5.2): `All songs`, `Search: "taylor swift"`, `Album · 1989`, `Artist · Taylor Swift`, `Folder · YMusic`, `Playlist · <name>`, `Favourites`, `History`.

### 1.6 Now Playing layout defects
- The **Timer** chip wraps its label vertically ("T / i / m / e / r") because the four action chips (Queue, Lyrics, Equalizer, Timer) share one row and the last gets squeezed. Needs a layout that never wraps single words (equal-weight row with `maxLines = 1`, icon-only fallback on narrow widths, or a horizontally scrollable/Flow row).
- Artwork has a visible outline and rounded corners; the requirement is **square (1:1), borderless** artwork.
- The seek bar draws a vertical bar "thumb" detached from the track end with a stray dot on the right. Redraw it correctly (thumb centred on progress, track fully drawn, 48dp touch target).
- The waveform area shows a dashed placeholder line on tracks without waveform data. Show a proper loading/empty state, never a fake-looking flat waveform. Cache computed waveforms persistently if the existing feature already computes them.
- Long titles are cut with "…" — add marquee on the title while playing (optional polish).

### 1.7 Songs screen defects
- The A–Z rail is ~8sp text with a ~12dp-wide touch area, no active-letter indicator, no drag-scrub, no bubble. Letters with songs are barely distinguishable from empty ones.
- Each row spends ~100dp on two trailing icons (heart + ⋮), which truncates titles ("Ariana Grande & Taylor …", "Enchanted x So High Sc…").
- The first result starts with a quote character and is sorted/sectioned as if it were `#`. Section/sort keys must strip leading punctuation and normalise diacritics.
- Mini-player + bottom navigation + list overlap: the last rows sit under the mini-player.
- Home search bar and Search tab must open the **Search** destination, not Songs (patch item 1).

### 1.8 Appearance / theme
The Appearance screen (accent palette, HEX field, live preview, background blur/opacity, "Use album art as background") must be verified end-to-end: the chosen accent must propagate to every themed surface (nav indicator, seek bar, switches, selected chips, A–Z rail, selection state) and persist across restarts. The **default** remains the burgundy identity in §4.

### 1.9 No immersive "Spotify-style" banner; lock-screen card shows only a small thumbnail
Now Playing, album/artist/playlist headers and Home use a flat `#171316` background with a small framed cover. The lock-screen media card (screenshot, Xiaomi system UI) shows a tiny rounded thumbnail on a teal card whose colour is **not** derived from the (black-and-white) cover — i.e. the OEM is drawing it. Two separate surfaces are involved: (a) **in-app** screens, which VESPER fully controls, and (b) the **lock-screen/notification card**, which the system draws and VESPER can only feed with correct metadata and high-quality artwork. See §5.13.

---

## 2. REFERENCE: OTO MUSIC INTERACTION PATTERNS (inspiration only)

Reference assets you were given: a VESPER Now Playing screenshot (current state) and an Oto Music **action sheet** screenshot. The action sheet shows: a drag handle; **four rounded groups** separated by space, rows divided by hairlines, icon on the left, 56dp rows —
1. *Equalizer · Speed & Pitch · Save playing queue · Clear playing queue*
2. *Go to album · Go to artist · Go to album artist · Go to folder*
3. *Add to playlist · Tag editor · Edit lyrics*
4. *Details · Share · Delete from device*

If song-list screenshots/video of Oto are also attached, they take precedence over the description below; otherwise recreate these commonly-observed patterns **in VESPER's own design language**:
- Dense, calm song list: leading square rounded artwork, title above artist/meta, trailing ⋮ only, generous vertical rhythm, no card borders.
- Alphabetical fast-scroller on the right edge with a large floating letter bubble while dragging.
- Long-press → contextual selection mode with count, select-all, and bulk actions in a top bar.
- ⋮ opens a grouped bottom sheet (above), not a long flat popup.
- Currently-playing row is visibly marked (accent title + animated bars over artwork).
- Scroll position is retained when navigating into a song/album and back.

**Do not copy** Oto's branding, assets, icons, strings, source or distinctive identity. Re-create behaviour only, with VESPER tokens (§4).

---

## 3. PHASES, ORDER AND GATES

The pasted patch said "fix the verified defects first, then the master prompt". The order below respects that intent but sequences by dependency so shared infrastructure is built **once** (selection state, track-actions sheet, queue source model) and reused everywhere, instead of being patched screen by screen. Section 7 maps every original patch item (1–25) and master item (1–9) to a phase so nothing is dropped.

**Phase 0 — Audit (no functional code changes).** Produce `docs/VESPER_AUDIT.md`: stack versions; the exact files/classes responsible for each issue in §1; confirmed vs refuted hypotheses; list of existing sound modes; every `fallbackToDestructiveMigration`/`runBlocking`/`delay(` on startup; where artwork keys are built; how the queue is created from Songs/Search/Album/Artist/Playlist; how Now Playing's pager gets its items; where playback state is (not) persisted. Then continue without waiting unless you hit a blocker.

**Phase 1 — Persistent library index + startup (master §3).** See §5.1.

**Phase 2 — Playback core.** Queue source model, search/filtered-queue fix, Play Next/Add to Queue, shuffle separation, playback restoration + Continue Listening, shared playback state in all screens, Search destination, logo → Home. See §5.2–5.4. (Patch 1, 3, 4, 6, 7, 12; master 2, 5.)

**Phase 3 — Preferences persistence.** Theme, accent, sort (per screen), EQ state, playback settings → DataStore (§5.5). (Patch 5.)

**Phase 4 — Shared building blocks.** One `TrackRow`, one `SelectionState`, one `TrackActionsSheet` + `TrackActions` executor (permission-aware delete/edit/share). Used by Songs, Search, Albums, Artists, Playlists, Favourites, History, Folders. (§5.6, §5.7, §5.8.)

**Phase 5 — Songs screen rebuild + A–Z rail + scroll persistence.** (master §1, §4.) (§6)

**Phase 6 — Apply shared components everywhere.** Playlist three-dot bug, History actions, favourites unification, playlist rename, empty states. (Patch 2, 9, 10, 11, 13, 14.)

**Phase 7 — Now Playing, lyrics, metadata, artwork.** (Patch 15, 16, 17, 23; §1.2, §1.4, §1.6.)

**Phase 7B — Immersive artwork banner (Spotify-style) + lock-screen artwork quality.** Runs **after** Phase 7 because it depends on the corrected artwork pipeline (§1.2) and the borderless square Now Playing artwork (§1.6); a gradient derived from the wrong cover would be worse than none. Reuses the Appearance "Use album art as background / blur / opacity" settings. (New item X1; extends P16, P23, P24; §1.9, §5.13.)

**Phase 8 — Audio engine: EQ, 15 sound modes, crossfade.** (Patch 8, 18, 19.)

**Phase 9 — Verification of existing features + performance.** Clear Cache, Listening Stats, Recently Added/Favourites ordering after rescans, notification/lock-screen controls, large-library performance. (Patch 20–22, 24, 25; master 7.)

**Phase 10 — Full test pass and final report** (§8, §9).

After every phase: `./gradlew assembleDebug testDebugUnitTest lintDebug` (use the project's real task names), fix failures, commit, update the ledger.

---

## 4. DESIGN TOKENS (VESPER identity — keep)

| Token | Value | Use |
|---|---|---|
| background | `#171316` | screen background |
| surface (secondary) | `#211B20` | sheets, mini-player, inputs |
| surfaceElevated | `#2B2229` | grouped menu cards, selected rows, chips |
| burgundyDeep | `#4A1728` | hero/Continue-Listening tint, pressed states |
| accent | `#722C42` | primary buttons, seek bar, selection, rail highlight |
| textPrimary | `#FFF1D5` | titles |
| textSecondary | `#F5E7CC` (use ~70% alpha for meta) | artists, durations |

Rules: define every colour as a theme token and derive hover/pressed/disabled/divider values from them (divider ≈ textPrimary @ 8%). **No hard-coded hex inside composables.** The user-selectable accent (Appearance) must override `accent` and its derived roles everywhere; default is `#722C42`. Maintain WCAG AA contrast for text on `background`/`surface`. Corner radii: 8dp artwork, 12dp chips/selection, 20–28dp sheets. Icons 24dp (20dp inline), minimum touch target 48dp. Avoid borders and nested cards; separate with spacing and tonal difference.

---

## 5. ENGINEERING SPECIFICATION

### 5.1 Persistent library index (high priority)

**Problem:** every cold start/recents-removal/low-memory kill re-scans. HYPOTHESIS: the library is held in ViewModel/Compose state or an in-memory repository and rebuilt from MediaStore on init; "indexed" flag, if any, lives in memory.

**Design (extend the existing Room DB; do not create another):**
- `tracks` table (extend existing entity; migrate, don't drop): `id` (stable PK), `mediaStoreId`, `volumeName`, `contentUri`, `relativePath`, `displayName`, `sizeBytes`, `dateAdded` (from MediaStore, **never overwritten on rescan** — drives Recently Added), `dateModified`, `generationModified` (API 30+), `mimeType`, `title`, `artist`, `albumArtist`, `album`, `albumId`, `genre`, `year`, `trackNo`, `discNo`, `durationMs`, `bitrate`, `sampleRate`, `titleSortKey`, `artistSortKey`, `albumSortKey`, `sectionLetter`, `searchText` (lowercased, diacritic-folded concat of title/artist/album/albumArtist/genre/folder/year), `artworkKey`, `artworkVersion`, `metadataExtractedAt`, `state` (`ACTIVE` / `MISSING` / `UNREADABLE`), `missingSince`, `firstSeenAt`.
- Indices: unique `(volumeName, mediaStoreId)`; plus on `sectionLetter`+sort keys, `albumId`, `artist`, `relativePath`, `state`, `dateAdded`.
- Favourites / playlist entries / history / sessions reference tracks by stable `id`; add a **fingerprint** (`relativePath+displayName+size+duration`) used to re-attach user data if MediaStore re-issues an ID for the same file. Never cascade-delete user data because a file went MISSING.
- `library_state` (DataStore or Room singleton): `schemaIndexVersion`, `firstScanCompleted` (set **only after a successful scan**), `lastSuccessfulScanAt`, `lastSeenMediaVersion`, `lastSeenGeneration`, `lastScanOutcome`.

**First scan:** request `READ_MEDIA_AUDIO` (API 33+) / `READ_EXTERNAL_STORAGE` (≤32) → query MediaStore audio (`IS_MUSIC` or `MIME_TYPE audio/*`, excluding ringtones/notifications/alarms/recordings per existing settings) → batch-extract metadata (title/artist/album/duration/embedded-art reference) on `Dispatchers.IO`/bounded parallelism → upsert in transactions of ~300–500 rows → emit genuine progress `(processed, total)` from the real loop → mark indexed only on success. UI streams rows as they land; never block the main thread.

**Every later launch:**
1. Splash is held by `installSplashScreen().setKeepOnScreenCondition { !startupReady }` where `startupReady` = DataStore settings loaded + first Room emission available. **No fixed delay, no `delay()`.** Add a safety cap (≈1.5–2 s) after which the app proceeds with whatever is cached; the cached library keeps streaming in via Flow. If `firstScanCompleted == false`, route to permission/first-scan flow instead.
2. UI renders from Room immediately (Songs/Albums/Artists/Folders derived from the index).
3. A **lightweight incremental sync** runs in the background (app-scoped coroutine / WorkManager `KEEP` unique work; also trigger on a registered `ContentObserver` for audio and on `onStart` throttled to e.g. ≥15 min):
   - Cheap short-circuit: compare `MediaStore.getVersion()` and (API 30+) `MediaStore.getGeneration()` with stored values; if unchanged → done.
   - Otherwise query only `(_ID, DATE_MODIFIED, SIZE, GENERATION_MODIFIED, DATA/RELATIVE_PATH)` and **diff against Room**: new → extract + insert; changed (modified/size/generation differs) → re-extract + update; absent → mark `MISSING` (soft); unchanged → untouched (no metadata/artwork extraction).
   - Purge a `MISSING` track only after it stays absent on a later successful sync **and** the file is confirmed gone; keep its favourites/playlist/history rows pointing at it (shown as unavailable) so a re-mounted SD card or renamed folder restores cleanly.
4. **Safety guards:** if permission is revoked, the query throws, the cursor is null, or the result is suspiciously empty (e.g. 0 rows while the index has >0 and permission appears granted, or >50% disappearing in one pass) → abort the sync, keep the index untouched, surface a non-destructive banner ("Library couldn't be refreshed") — never wipe. Permission revoked → show cached library read-only plus a "Grant access" action; playback of files that are no longer readable fails gracefully.
5. **Manual full rescan** in Settings: explicit action with confirmation; implemented as a forced re-extraction that still upserts by stable identity (no truncate-and-reload), preserving all user data. Show progress; on failure keep the previous index.
6. De-duplication: unique constraint + upsert; a rescan can never create duplicate rows.
7. Diagnostics (debug builds): log tag `VesperScan` and a debug-only screen/section showing `queried / unchanged / new / updated / missing / extracted / durationMs / reason (cold-start, observer, manual)`. This is how you **prove** the second launch performs no full scan.
8. Process death ≠ data loss: nothing about library correctness may live only in memory.

**Counting:** one repository function supplies track counts to Home, Songs chips, Albums, Artists, Folders.

### 5.2 Queue source model (fixes §1.1 and §1.5)

Introduce (or extend the existing equivalent) a small immutable descriptor:

```
sealed interface QueueSource {
  object Library(sort, order) ; data class Search(query, sort, order)
  data class Album(id) ; data class Artist(id) ; data class Folder(path)
  data class Playlist(id) ; object Favourites ; object History
}
```

- **Starting playback from any list** calls one entry point on the existing playback controller wrapper: `playFrom(orderedTrackIds, startIndex, source, shuffle=false)`. `orderedTrackIds` is exactly the list the user is *looking at* (filtered + sorted), and `startIndex` is the index **within that list**. It calls `setMediaItems(items, startIndex, startPositionMs)` on the shared Media3 controller (one player, one session). Items carry minimal metadata (mediaId = stable track id, URI, title, artist, artwork URI) resolved from Room; do not build 1,500 heavyweight objects needlessly.
- **Next/Previous, swipe, notification, Bluetooth, lock screen** all operate on the player's own timeline — the single source of truth. **The Now Playing artwork pager must be driven by the player timeline/queue, never by a ViewModel library list.** Sync pager ↔ `currentMediaItemIndex` in both directions without feedback loops.
- Searching or filtering **never mutates the global library order or the active queue**. Starting playback from search snapshots the filtered list into the queue; clearing the search afterwards does not alter the playing queue; the queue persists until the user starts something else.
- If a user changes sort while a queue is playing, the queue is not reordered; only a new `playFrom` rebuilds it.
- "PLAYING FROM" and the Queue sheet header derive from `QueueSource`; the source is persisted with the queue (§5.4).
- Same logic for Album, Artist, Folder, Playlist, Favourites, History views.
- **Play Next:** insert after `currentMediaItemIndex` (and after previously inserted "play next" items, keeping their order, like a stack-free FIFO "up next" block); if the queue is empty, start playback. **Add to Queue:** append at the end; never replace/clear the queue; current track and position untouched. Bulk versions preserve the order of the selection. Toasts/snackbars confirm count ("Added 3 to queue") with Undo where cheap.
- Edge cases: removing the currently playing item from the queue, duplicates in queue (items must have unique `mediaItemId` instance keys or use index-based operations), a MISSING/UNREADABLE track in the queue (skip + notify), queue of 10k items.

### 5.3 Shuffle, shared playback state, navigation fixes
- **Shuffle toggle** only flips `shuffleModeEnabled` on the player (and persists). It must not start playback, change the current item, or rebuild the queue. A separate **"Shuffle All / Shuffle"** action builds a queue from the current list, enables shuffle, and starts at a random item. The Home tile that reads "Shuffle On" must reflect actual state and not be a play button in disguise.
- **Shared playback state:** every screen (Home, Songs, Search, Album, Artist, Playlist, Favourites, History, mini-player) observes the same `StateFlow` derived from the Media3 controller (current mediaId, isPlaying, source). Play/Pause controls on album/artist/playlist headers must reflect and toggle the *same* player; a header shows "pause" only if the playing item belongs to that screen's source.
- **Search destination:** Home search bar, any search icon, and the Search tab navigate to the real Search screen (songs, albums, artists, playlists, with its own result sections). The Songs screen keeps its own inline filter. Verify nav graph + deep links; add a UI test.
- **Top-left VESPER logo** → Home (`popUpTo(home)`, `launchSingleTop`).
- Bottom-nav behaviour: `saveState = true / restoreState = true / launchSingleTop = true` so tabs keep their scroll and state.

### 5.4 Playback persistence and restoration
- Persist (Room table for the queue; DataStore for scalars): ordered queue ids (+ source descriptor), current index, position (ms), shuffle, repeat mode, playback speed/pitch, last played track.
- Save triggers: media item transition, pause, stop, app background (`onStop`), service `onTaskRemoved`, and a throttled position write while playing (every ~5–10 s). Write off the main thread, tolerate partial writes.
- Restore on cold start **without auto-playing**: prepare the queue paused at the saved position; the mini-player appears populated. If the MediaSession callback API in the project's Media3 version supports it, implement `onPlaybackResumption` so headset/Bluetooth/notification "play" resumes the last queue after the app was killed.
- **Continue Listening** (Home card): resumes the restored queue at the saved position; hidden if nothing is saved; handles a missing last track gracefully.

### 5.5 Preference persistence (DataStore; Room only for relational data)
Persist and restore: theme mode, accent colour/HEX, background image/blur/opacity/"album art as background", per-screen sort field + order, library filters, EQ enabled, EQ band gains, selected preset/sound mode, bass/virtualizer/loudness values, crossfade enabled + duration, gapless, playback speed/pitch, sleep-timer defaults, shuffle/repeat. Provide typed defaults and a one-time migration from any existing SharedPreferences. Apply EQ/sound-mode at service start **before** first playback.

### 5.6 Selection (shared)
One `SelectionState` (set of stable track ids, `isActive`, `count`), kept in the ViewModel and in `SavedStateHandle` so it survives rotation. Long-press enters selection; tap toggles while active; system Back exits selection first. Top contextual bar: close ✕, **"N selected"**, **Select all**, **Deselect all**, **Add to queue**, **Add to playlist**, **Favourite**, **Delete (confirmed)**, overflow for Play next. Selected row: `surfaceElevated` background + accent check badge replacing the artwork corner/overlay. Select-all means "all currently displayed (filtered) rows". Works in Songs, Search, Albums/Artists detail, Playlist, Favourites, History (where "Delete" there means remove-from-history/playlist, labelled accurately).

### 5.7 Track actions sheet (shared, Oto-style grouping, VESPER-styled)
Bottom sheet (`surface` `#211B20`, groups in `surfaceElevated` `#2B2229`, hairline dividers, 56dp rows, 24dp icons). Header: artwork 48dp + title + artist. Groups:
1. **Play · Play next · Add to queue**
2. **Go to album · Go to artist · Go to album artist (only if different) · Go to folder**
3. **Add to playlist · Favourite / Unfavourite · Tag editor · Edit lyrics**
4. **Details · Share · Delete from device**

Rules: every entry executes a real action or is not shown (e.g. *Go to album artist* hidden when equal/absent; *Tag editor / Edit lyrics* hidden or disabled-with-reason for formats that cannot be written). Navigation entries navigate to the existing Album/Artist/Folder screens with the right arguments; "Go to folder" opens Folders at that path. **Details** = dialog/sheet with title, artist, album, album artist, genre, year, track/disc, duration, format, bitrate, sample rate, size, path, date added/modified. **Share** = `ACTION_SEND`/`SEND_MULTIPLE` with content URIs + `FLAG_GRANT_READ_URI_PERMISSION`. **Add to playlist** = picker with existing playlists + "New playlist"; duplicates handled per existing behaviour (ask/skip).

Now Playing ⋮ uses the same sheet plus the queue group: *Equalizer · Speed & Pitch (via `PlaybackParameters`, persisted) · Save playing queue as playlist · Clear playing queue (confirm)*. Add these only after the core gates pass; each must be real.

### 5.8 Permissions for edit/delete on modern Android
- **Delete:** API 30+ → `MediaStore.createDeleteRequest` + `IntentSenderRequest` result handling (batch uris in one prompt for multi-select); API 29 → catch `RecoverableSecurityException`, launch its intent sender, retry; ≤28 → direct delete with `WRITE_EXTERNAL_STORAGE`. Always show a confirmation dialog first with the count and track name(s). After success: remove from index, current queue (without stopping unrelated playback; if the playing track is deleted, advance), favourites, playlists membership, history; invalidate artwork keys. On failure/cancel: change nothing.
- **Edit tags:** API 30+ → `createWriteRequest`; API 29 → `RecoverableSecurityException`; ≤28 → legacy write. Permissions required in manifest: `READ_MEDIA_AUDIO` (33+), `READ_EXTERNAL_STORAGE` (≤32), `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion=28`), `POST_NOTIFICATIONS` (33+), `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (34+). Nothing else.

---

### 5.9 Audio engine (Phase 8) — requirements

**EQ / effects:** bind `android.media.audiofx.Equalizer` (+ `BassBoost`, `Virtualizer`, `LoudnessEnhancer` where used) to the ExoPlayer `audioSessionId`; **re-create/re-bind when `onAudioSessionIdChanged` fires** (and on service restart/crossfade player swap); release on service destroy. Read the real band count/range from the device — don't assume 5 bands; scale preset gains to the device's bands. Persist everything (§5.5) and apply before first playback. Verify by reading properties back from the effect objects.

**Sound modes — target exactly 15 genuinely functional modes.** Inventory the existing ones in Phase 0 and keep them. Ensure **Studio Reference** (flat/neutral, effects off, optional small loudness trim) and **Warm Analog** (gentle low-shelf lift, soft high-end roll-off, light bass/virtualizer) exist. If the total is below 15, add modes from a documented table (name → band gains in dB at normalised frequencies, bass-boost strength, virtualizer strength, loudness gain) until there are 15. Each mode must differ in actual effect parameters; add a unit test asserting distinct parameter vectors and an instrumented check that applying a mode changes the effect object's readable state. A mode that only changes a label is a defect.

**Crossfade (real):** replace the cosmetic slider. Preferred design: a `CrossfadeController` inside the existing playback service that uses a second `ExoPlayer` instance behind the existing MediaSession (e.g. via `ForwardingPlayer`/player-swap pattern) to pre-prepare the next item and overlap it with the tail of the current one using synchronised volume ramps (equal-power curve). It must handle: pause/resume mid-fade, seek, manual skip (short fade or hard cut), repeat-one (no crossfade), tracks shorter than ~2× fade, gapless same-album transitions (option: skip fade), queue edits during a fade (Play Next/Add/remove), audio focus loss, headset unplug, sleep timer, service lifecycle/process death, and **applying the same EQ/effects to both players' sessions**. Duration setting 0–12 s persisted. If a true overlap cannot be delivered within the existing Media3 architecture without a risky rewrite, implement the best correct version, mark `PARTIAL`, and **disable/hide the control with an honest note rather than leaving a fake slider**.

### 5.10 Lyrics (Phase 7)
Read embedded lyrics (ID3 USLT/SYLT, Vorbis `LYRICS`/`UNSYNCEDLYRICS`, MP4 `©lyr`) and a same-name sidecar `.lrc` if accessible. Parse synced LRC (`[mm:ss.xx]`, multiple timestamps per line, `[offset:]`, optional word-level enhanced tags). Display: synced → highlighted current line, auto-scroll, tap line to seek; unsynced → plain scrollable text; none → "No lyrics available" with **Edit lyrics** (writes a real tag, §5.11). **Never fabricate, generate, or fetch lyrics online.** Cache parsed lyrics in a disposable cache.

### 5.11 Metadata Studio (Phase 7)
Use the project's existing tag library (or justify one maintained Android-compatible library in the audit). Write title/artist/album/album artist/genre/year/track/disc/lyrics/artwork for supported containers (MP3/ID3v2, FLAC & OGG/Opus Vorbis comments, M4A/MP4 where the library supports it); clearly mark unsupported formats read-only. Flow: obtain write access (§5.8) → write to the file → **read back and compare** → only then update Room, bump `artworkVersion`, invalidate Coil memory+disk keys for that track, notify MediaStore (`MediaScannerConnection`/update) → refresh every screen showing the track. On any failure: no partial state, original preserved (write via temp copy + atomic replace if feasible), error shown. Multi-track edits supported only where the fields make sense (album/artist/genre/year/artwork).

### 5.12 Verification-type items (Phase 9)
- **Clear Cache:** clears Coil memory + disk caches, VESPER-managed disposable caches (artwork thumbnails, waveform, lyrics parse cache, temp files). Must **not** delete music, the library index, playlists, favourites, history, sessions or preferences. Show size cleared. After clearing, artwork rebuilds correctly.
- **Listening Stats:** sessions table `(trackId, startedAt, listenedMs)`; accumulate only while `isPlaying` via `Player.Listener` (excludes paused/buffering time); one session per continuous listen, flushed on pause/transition/stop/app-kill (best effort); seeking backward does not re-count; repeat-one counts each full loop once; add a minimum-listen threshold consistent with existing semantics. Unit-test the accumulator with a fake clock.
- **Recently Added / Favourites ordering:** Recently Added uses MediaStore `dateAdded` (fallback `firstSeenAt`) and a rescan never changes it; Favourites keep oldest-first by `favouritedAt`. Test after a forced rescan.
- **Notification/lock screen:** Media3 `MediaSessionService` + media notification with play/pause/previous/next/seek, correct artwork and title for the *current* track, foreground-service type `mediaPlayback`, POST_NOTIFICATIONS flow on 13+, resume after swipe-away behaviour per design. Mark `DEVICE DEPENDENT` unless verified on hardware.

### 5.13 Immersive artwork banner + lock-screen artwork (Phase 7B)

**Scope — two surfaces, treated differently:**
1. **In-app (full control):** Now Playing (full treatment); Album / Artist / Playlist detail headers (hero treatment); Home "Continue Listening" and hero cards (light treatment); mini-player and bottom navigation (subtle tint only).
2. **Lock screen / notification shade (system-rendered):** VESPER cannot change its layout. It can only supply correct metadata and high-quality artwork. Do **not** attempt custom `RemoteViews`, colourised-media-notification hacks, or other tricks: custom media views are ignored by the system media card on Android 11+ and colourised media styling on 12+, and they would break Media3's notification. State this limitation plainly in the report.

**A. In-app layered design (bottom → top):**
1. Base: `background` `#171316`.
2. **Backdrop image:** the current track's artwork, scaled to *fill/crop* the top ~55–65% of the screen, alpha ≈ 0.5–0.7, blurred. Blur via `RenderEffect` (API 31+); on API ≤ 30 use a pre-blurred, heavily downsampled bitmap (≈ 1/8 scale). **Never blur or decode a full-resolution bitmap per frame**; reuse the already-loaded Coil bitmap, blur once, cache by `artworkKey + artworkVersion`. Skip the blur on low-RAM devices (`ActivityManager.isLowRamDevice`) and fall back to the gradient only.
3. **Palette gradient:** extract a colour from the artwork off the main thread (Palette API or Material colour extraction on a ~128px downsample; prefer vibrant → dominant → muted). Harmonise it with the brand: blend ~30–40% with `accent` `#722C42`, clamp saturation/luminance so it never turns neon or washes out, and **guarantee ≥ 4.5:1 contrast** for `textPrimary` against the resulting tint. Vertical gradient: tint @ ≈ 0.85 at the top → fully `background` at ≈ 70–85% of the screen height. For greyscale / near-zero-saturation covers (e.g. the black-and-white Jessie J cover) and for missing art, fall back to a `burgundyDeep` `#4A1728` → `background` gradient rather than a grey one.
4. **Scrim:** bottom-weighted dark gradient (transparent → `background` @ ≈ 0.6) under the title/controls so text is readable on any cover. Text never sits directly on raw artwork.
5. **Foreground:**
   - *Now Playing:* large square (1:1) borderless artwork (≈ 80–88% of width) over the backdrop, then title/artist, seek bar, controls, action chips (Queue / Lyrics / Equalizer / Timer — fixed per §1.6).
   - *Album / Artist / Playlist headers:* full-width hero image (≈ 1:1–4:3) fading into the gradient, title + metadata overlaid bottom-start, Play and Shuffle buttons overlaid, back/overflow icons on a soft top scrim; collapses smoothly into a normal top bar on scroll (collapsing toolbar behaviour, no jank).
   - *Home:* the existing Continue Listening / hero cards get the same palette tint instead of a flat colour.
6. **System bars:** `enableEdgeToEdge()`; transparent status bar and navigation bar; the gradient continues behind the bottom navigation bar (nav surface = `surface` tinted 8–12% by the palette colour, not an opaque slab); correct light/dark system-bar icon colours; all content respects `WindowInsets`. The mini-player gets the same subtle tint.

**Behaviour:**
- Palette extraction runs on a background dispatcher, memoised per `artworkKey + artworkVersion`, optionally persisted as a single `dominantColor` Int on the track/album row so it is not recomputed on every launch (recompute only when `artworkVersion` changes, e.g. after a tag edit). Expose it as a tiny immutable `ArtworkPalette(tint, onTint, isFallback)`.
- On track change, animate the tint and backdrop (`animateColorAsState` / crossfade, ≈ 300–500 ms). While swiping the Now Playing pager, interpolate between neighbouring palettes if cheap; otherwise crossfade after settle. No flicker, no white flash, no layout shift.
- Read colour/blur state in the **draw phase** (`drawBehind`, `graphicsLayer`) so a colour change doesn't recompose the whole screen.
- **Settings:** reuse the existing Appearance controls (*Use Album Art as Background*, *Background Blur*, *Opacity*, custom background image, accent palette). Do not add duplicate settings. If the existing toggle cannot express "immersive banner on/off", add exactly one toggle there, persisted via §5.5. When album-art background is off, show the solid/accent-gradient version of the same layout. The user's chosen accent still drives buttons, seek bar and selection; the album palette only drives the backdrop/gradient.
- Light theme: use a lighter tint + dark scrim-free text variant; verify contrast. Dark theme remains the primary design.
- Reduce-motion / battery: honour the system "remove animations" setting by disabling the crossfade/collapse animation.
- Accessibility: contrast ≥ 4.5:1 for text, ≥ 3:1 for icons over the gradient; don't rely on colour alone to convey state.

**B. Lock-screen / notification artwork (what VESPER controls):**
- Supply, for the *current* track, a **square, high-resolution cover (≈ 512–1024 px)** as Media3 `MediaMetadata.artworkUri` (a content URI the app can serve from its artwork cache, e.g. via a `FileProvider`/`ContentProvider`) or `artworkData` (JPEG kept well under ~1 MB to avoid binder transaction limits). Never hand the system a 128px list thumbnail.
- Configure the Media3 session's `BitmapLoader` (or a Coil-backed one) so the notification and media session use this same cached high-res art; if art is still loading, publish metadata first and **update** it when the bitmap arrives rather than leaving a placeholder or the previous track's cover (§1.2).
- Always set title, artist, album, duration and `mediaType`; keep a monochrome small icon; keep the standard Media3 notification (play/pause/previous/next/seek). Update the session on every media-item transition (including crossfade player swaps, §5.9).
- This lets systems that render art-derived backgrounds (stock Android 13+ and similar) show a big, sharp cover with a matching gradient. Phones whose OEM draws its own card (as in the Xiaomi screenshot) will keep their own style; that is expected and **not** a VESPER defect.

**C. Verification (report honestly):**
- Unit tests: palette clamp/harmonise function (contrast ≥ 4.5 for `textPrimary` across a set of sample colours incl. near-white, near-black, neon, greyscale, null); fallback selection; cache-key invalidation on `artworkVersion` change.
- If a screenshot-testing setup exists (Paparazzi/Compose screenshot tests), add goldens for Now Playing and an album header with a dark cover, a bright cover, and a greyscale cover; otherwise capture emulator screenshots and attach them.
- Performance: no extra dropped frames in Now Playing (Macrobenchmark `FrameTimingMetric` before/after); blur happens once per cover; memory stays flat across 200 track changes.
- Lock screen / notification: `DEVICE DEPENDENT`. Manual steps: play tracks with high-res, low-res, and no embedded art; lock the phone; confirm the card shows the **correct current** cover at good quality and updates on Next; compare against Spotify on the same phone to separate OEM styling from app issues; test on one stock-Android (Pixel/emulator API 33/34) and the user's Xiaomi.

---

## 6. SONGS SCREEN REBUILD (Phase 5)

### 6.1 Structure
Top bar: VESPER logo (→ Home), title "Songs" + track count, sort icon (opens sort sheet), menu. Inline search field (rounded, `surface`, clear ✕). Filter chips: *All Songs (n)*, *Playlists (n)*, *Folders* — counts from the shared repository, never stale. Sub-header: *"N tracks"* + **Shuffle All**. Then the list. Content padding at the bottom must include mini-player + bottom-nav insets so nothing hides underneath.

### 6.2 Sorting and search
- Sort sheet: field (Title, Artist, Album, Date added, Duration, Year, Folder) × order (A→Z / Z→A) with the selection persisted per §5.5. **Search results are displayed in the selected sort** and search covers title, artist, album, album artist, genre, year, folder name, and available tags, via the normalised `searchText` (case/diacritic-insensitive; multi-token AND; debounce ~150–250 ms; runs off the main thread; Unicode-safe for Hindi/Tamil/Japanese titles seen in the library). If FTS4/FTS5 is simple to add via migration, use it; otherwise indexed `LIKE` on `searchText` is acceptable at 10k rows.
- Clearing the search restores the full list instantly with scroll position restored.
- Search within an Album/Artist/Folder screen filters only that scope.

### 6.3 Row spec (compact, consistent)
Height 64dp; horizontal padding 16dp; artwork 48dp, 8dp radius, 12dp gap; title `bodyLarge` Medium 1 line ellipsis in `textPrimary`; subtitle `bodyMedium` 1 line in `textSecondary` as **"Artist • m:ss"** (omit duration if unknown, never "0:00"); trailing **⋮ only** (48dp target) — the per-row heart is removed to give titles ~48dp more width; a favourited track shows a small filled accent heart glyph inline before the duration, and Favourite lives in the ⋮ sheet and the selection bar. Currently playing: accent-coloured title + small animated equaliser bars over the artwork (paused → static). Reserve a right gutter (~28dp) so the A–Z rail never overlaps the ⋮ button or text. Ripple on whole row = play from this list at this index; long-press = select. Placeholder artwork is a deterministic burgundy-tinted tile from the track title (never another track's art).

### 6.4 Sections
Flatten into `LazyColumn` items: `Header(letter)` + songs, with **stable keys** (`"h_$letter"`, `"t_$trackId"`) and `contentType`. Sticky section headers (small letter in accent, `background` fill, 8dp of vertical spacing) provide the clear separation. Section letter rule: take the sort field's text → strip leading punctuation/quotes/whitespace → NFD-fold diacritics → first char; A–Z → that letter; digit, symbol, non-Latin script, empty → `#`. (Optional setting: ignore leading "The/A/An", default off.) When the active sort is not alphabetic (Date added, Duration…), hide the section headers and the rail rather than showing wrong letters.

### 6.5 A–Z navigation rail (master §1)
- Built from the **actually displayed list** (derive a `SectionIndex`: letter → first item index, from the same sorted/filtered list the `LazyColumn` renders; recompute only when that list changes; never a hard-coded list of songs). Order mirrors the list order (Z→A when descending). `#` is first when ascending.
- Layout: vertical column on the right edge, **visual width ~20–24dp, touch width 40–48dp** (hit area extends leftwards over the gutter only; it must not swallow row taps elsewhere), vertically centred, padded off the status/nav insets. Letter text 10–11sp, `labelSmall`. **Responsive:** cell height = available height ÷ 27 clamped; if cells fall below ~14dp (small phones, landscape, split-screen), show a reduced set (every other letter with dots) or let it scroll-scrub only; honour font scale and `WindowInsets`.
- States: letters with results = `textSecondary`; empty letters = 25–30% alpha and not directly tappable (a tap/drag onto an empty letter snaps to the nearest letter that has results, in the direction of travel); **current letter** (derived from `firstVisibleItemIndex` via `derivedStateOf` + binary search) = accent colour, bold, with a small accent pill behind it.
- Interaction: tap → `animateScrollToItem` (smooth); drag/scrub → `scrollToItem` per letter change (fast, no animation queue build-up); **floating bubble** (≈56dp, `surfaceElevated`, large accent letter) appears at the touch point while scrubbing and fades out; light haptic tick on each letter change; the list does not fight the gesture (nested scroll safe). Gesture is `pointerInput` with `detectTapGestures` + `detectVerticalDragGestures`; consume only inside the rail.
- Accessibility: semantics with `contentDescription` ("Jump to B"), TalkBack-operable via custom actions/Slider-like semantics.
- Hidden entirely when the list has fewer than ~20 items or the sort isn't alphabetic. Works identically on the filtered search results (letters reflect the results).

### 6.6 Scroll persistence
Hoist `LazyListState` so it survives navigation and tab switches (NavHost `saveState/restoreState` + `rememberSaveable(saver = LazyListState.Saver)` or a state holder in the ViewModel keyed by screen + search query), and survives process death via `SavedStateHandle` (first visible index + offset). Opening Now Playing, an album, or a menu action and returning must not reset scroll. Playing a song must **not** scroll the list or rebuild it. Clearing a search restores the pre-search position.

### 6.7 Empty, loading, error states
Real states for: permission not granted (explain + Grant button), first scan in progress (progress bar with real counts), no music found (Rescan action), no search results (`No results for "x"` + Clear), library refresh failed (non-destructive banner). Never show "0 tracks" while the index is simply still loading from Room (use a loading state).

### 6.8 Recomposition discipline
Lightweight immutable `SongRow` UI model (id, title, artist, durationText, artworkKey, flags); `@Immutable`/`@Stable`; `collectAsStateWithLifecycle`; per-row `isPlaying/isSelected/isFavourite` supplied as lambdas or derived sets so a single toggle recomposes one row, not the screen. Room queries return projections, not full entities with unused columns.

---

## 7. PATCH & MASTER TRACEABILITY (nothing is dropped)

| # | Item | Phase | Key acceptance criterion |
|---|---|---|---|
| P1 | Search icons/tab open Search, not Songs | 2 | nav UI test |
| P2 | Playlist ⋮ is a real menu; remove requires explicit "Remove" | 6 | tapping ⋮ never removes |
| P3 | Add to Queue / Play Next via shared Media3 controller | 2 | current track + queue preserved |
| P4 | Persist/restore queue, index, position, shuffle, repeat; Continue Listening works | 2 | kill process → restored paused |
| P5 | Persist theme, EQ, preset, sorting, playback settings | 3 | survive restart |
| P6 | Album/artist/playlist/library/search observe one playback state, working Play/Pause | 2 | no stale icons |
| P7 | Shuffle toggle ≠ start playback | 2 | toggling doesn't change current item |
| P8 | Real crossfade | 8 | audible overlap, §5.9 |
| P9 | Long-press multi-select, Select All, count, queue/playlist/favourite/delete | 4–6 | §5.6 |
| P10 | Track ⋮: Play, Play Next, Add to Queue, Favourite, Add to Playlist, Edit metadata, Delete, Info | 4–6 | §5.7 |
| P11 | History ⋮ incl. Remove from History (file untouched) | 6 | file still exists after |
| P12 | Logo → Home | 2 | nav test |
| P13 | Favourites from anywhere → one persistent collection, instantly | 6 | single table/Flow |
| P14 | Playlist rename keeps ID, tracks, order | 6 | `UPDATE name` only; test |
| P15 | Embedded lyrics + synced LRC; never fabricate | 7 | §5.10 |
| P16 | Metadata/artwork audit; fix stale artwork; verify across screens | 7 | §1.2 repro passes |
| P17 | Metadata Studio writes real tags, verifies by read-back | 7 | §5.11 |
| P18 | 15 genuine sound modes incl. Studio Reference + Warm Analog | 8 | §5.9 |
| P19 | EQ persists and applies to active audio session | 8 | rebinding on session change |
| P20 | Clear Cache clears only disposable caches | 9 | user data intact |
| P21 | Listening Stats from persisted sessions, excludes paused time, no double count | 9 | §5.12 |
| P22 | Recently Added timestamps + oldest-first Favourites survive rescans | 9 | `dateAdded` never overwritten |
| P23 | Now Playing square borderless artwork + swipe gestures | 7 | §1.6 |
| P24 | Notification + lock-screen controls | 9 | DEVICE DEPENDENT |
| P25 | Lists/keys/artwork/DB/search tuned for 1,400+ songs (target 10k) | 9 | benchmarks |
| X1 | Immersive Spotify-style banner (backdrop + palette gradient + scrim + edge-to-edge) on Now Playing, album/artist/playlist headers, Home; high-res current-track artwork to lock screen | 7B | §5.13; in-app PASS-able, lock-screen DEVICE DEPENDENT |
| M1 | A–Z rail | 5 | §6.5 |
| M2 | Search/swipe bug | 2 | §5.2 |
| M3 | Persistent index + splash | 1 | §5.1 |
| M4 | Songs screen rebuild + menu | 4–5 | §6 |
| M5 | Queue consistency | 2 | §5.2 |
| M6–M9 | Design rules, performance, testing checklist, final report | all | §4, §9 |

## 8. PERFORMANCE AND RELIABILITY (Phase 9; target 1,457 now, then 5k and 10k)

- All scanning, extraction, DB and sorting work on background dispatchers; main thread only renders.
- Lazy lists everywhere with stable keys + content types; no `Column` + `forEach` over libraries; no `collectAsState` of massive non-stable lists.
- Artwork: Coil with a bounded memory cache and disk cache; request **size-constrained** bitmaps (48dp rows → ~128px, grid cards → ~256px, Now Playing → ~720px); extract embedded art **once** per track/version to a hashed file cache (dedupe identical bytes by content hash — this also protects against §1.2); never decode full-size art in list rows; cancel requests for off-screen rows (Coil does if keyed correctly); graceful placeholder on missing/corrupt art.
- DB: projections for list screens, proper indices, no N+1 queries, transactions for batch writes; `Flow` + `distinctUntilChanged`; avoid re-emitting 10k rows for a single favourite toggle (observe favourites as a separate `Set<Long>`).
- Cold start: splash → first Room emission target < ~600 ms on mid-range hardware for 1.5k tracks; add a **Macrobenchmark** (startup cold/warm, Songs scroll `FrameTimingMetric`) and generate a **Baseline Profile** if the project has none. Debug builds: enable `StrictMode` (thread + VM policy) and LeakCanary; fix violations caused by this work.
- Test data: write a script (`tools/seed_library.sh`) that generates N tiny silent tracks with varied titles (including quotes, digits, emoji, Devanagari/Tamil/Japanese, duplicates, missing tags, same-album-different-art) using `ffmpeg` or a Kotlin/androidTest generator and pushes them to an emulator's Music folder for 1k/5k/10k runs. If emulators/ffmpeg are unavailable, say so and mark runtime performance `DEVICE DEPENDENT`.

---

## 9. TESTING (do not claim anything you didn't run)

### 9.1 Automated (write and run these)
- **Unit:** section-letter/sort-key function (quotes, digits, emoji, non-Latin, diacritics, "The"); `SectionIndex` build + current-letter binary search + empty-letter snapping; search filter (tokens, diacritics, scopes, sort order preserved); queue builder (`playFrom` index mapping on filtered lists; Play Next ordering; Add to Queue non-destructive); playback-persistence mapper; LRC parser; scan differ (new/changed/missing/unchanged/duplicate/permission-error abort); fingerprint re-attach; listening-session accumulator; crossfade ramp math; sound-mode parameter distinctness; DataStore defaults/migration.
- **Room:** `MigrationTestHelper` for every schema bump (old → new keeps favourites/playlists/history/sessions); DAO tests for upsert idempotence and soft-delete.
- **Compose/instrumented:** A–Z rail tap/drag jumps to the correct header; disabled letters behave per §6.5; search → play → Next/Previous/swipe stays in the 91-style filtered list (use a fixture library); clearing search restores browsing; selection mode flows; each ⋮ item triggers its action; playlist ⋮ no longer removes; Search tab/icons → Search screen; logo → Home; scroll position restored after navigation and recreation.
- **Process-death tests:** `adb shell am kill <pkg>` / `am force-stop` / "Don't keep activities" → library visible immediately from Room, **`VesperScan` log shows no full scan**, queue/position restored, favourites/playlists intact.

### 9.2 Required scenario checklist (report each as PASS/PARTIAL/NOT IMPLEMENTED/DEVICE DEPENDENT/FAIL with evidence)
1 A–Z rail jumps to the right section · 2 empty letters behave · 3 search song → Next stays in results · 4 search artist → navigation stays in results · 5 clearing search restores browsing · 6 first scan creates the persistent index · 7 close/reopen restores cached library · 8 removal from recents triggers no full scan · 9 new tracks appear via incremental sync · 10 deleted tracks removed safely · 11 favourites/playlists survive restarts · 12 every song-menu action works · 13 Play Next / Add to Queue correct · 14 scroll position preserved · 15 large libraries don't lag/crash · plus: wrong-artwork repro (§1.2) fixed, count parity (§1.3), duration/unreadable handling (§1.4), "Playing from" labels (§1.5), Timer chip layout + borderless square artwork + seek bar (§1.6), shuffle toggle doesn't start playback, playlist rename keeps ID/order, history remove keeps the file, EQ/modes persist and apply, crossfade, lyrics, tag write read-back, Clear Cache safety, notification controls, **immersive banner (X1): gradient/backdrop correct for dark, bright, greyscale and missing art; text contrast ≥ 4.5:1; transparent system bars; no frame drops; lock-screen card shows the correct high-res current cover (DEVICE DEPENDENT).**

### 9.3 Final report format (end of Phase 10)
1. **Summary** (3–5 lines).
2. **Root cause per issue** (confirmed against code, with file/class names).
3. **Files changed** (grouped by phase; new files marked).
4. **Features implemented** (map to the traceability table).
5. **Tests actually run** (command + result; counts of passing/failing; benchmark numbers if measured).
6. **Needs a physical device** (exact manual steps for each).
7. **Status table** for P1–P25, X1 and M1–M9 using the five statuses.
8. **Known bugs / limitations / follow-ups.**
9. **Build status:** exact command(s) and result; if failing, the verbatim error output and the file/line.

---

## 10. DEFINITION OF DONE
- Searching then playing/Next/Previous/swiping never leaves the displayed result list; clearing search restores normal behaviour; global order untouched.
- Cold start after process death shows the saved library immediately, performs no full scan, and only runs a cheap incremental check; user data intact; permission loss or scan failure never wipes the index.
- Songs screen is compact, aligned, sectioned, selection-capable, with a working responsive A–Z rail driven by the displayed list, persistent scroll, and a grouped ⋮ sheet in which every entry does something real.
- Queue, Play Next, Add to Queue, shuffle, restoration, Continue Listening, notification and mini-player all reflect one Media3 player.
- Artwork is always the correct track's art (or an honest placeholder).
- Now Playing, album/artist/playlist headers and Home show the immersive banner (blurred backdrop + palette-derived gradient + scrim, edge-to-edge system bars, readable text) with safe fallbacks; the lock-screen/notification card receives the correct high-resolution current-track cover, and the report states clearly that its visual style is OEM-controlled.
- No hard-coded colours, no destructive migrations, no cosmetic controls, no new player, no Downloader changes, no fabricated results.

**Start with Phase 0 now. Show your audit, then proceed phase by phase.**
