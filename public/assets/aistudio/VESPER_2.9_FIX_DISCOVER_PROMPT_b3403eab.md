# VESPER 2.9 — Fix Library Loading + Playback, Remove Downloader, Real Discover

Baseline: the attached project (package `com.vesper.audio`, Kotlin, Compose, Media3, Room, DataStore, Coil, Retrofit/OkHttp/Moshi).
Report from device: splash is fast now, but **songs never appear because the parser/scan is slow**.

## 0. RULES (short)
1. Read first, edit second. Do not rewrite the app. Keep: Room data, favourites, playlists, history, EQ, lyrics, Appearance Studio, Home rework, theme tokens, splash, queue.
2. **Output only changed files** as `// FILE: app/src/main/java/com/example/...` with the full file. No "rest unchanged".
3. Before code, write ≤ 10 lines: the verified root cause for A1–A4 (file + function). If a suspect below is wrong, say so.
4. One reply, in the order A → B → C → D → E. If space runs out stop after a complete file and write `CONTINUE FROM: <item>`.
5. No new dependencies, no accounts/ads/telemetry, **no API keys anywhere in the app**. No Room reset; any schema change needs a `Migration`.
6. Never claim a build/test you did not run. No `gradlew` in the zip: if you cannot build say `NOT BUILT: reason`.

---

## A. LIBRARY LOADS FAST (do first)

### A0. Diagnose cheaply (keep logs, tag `VesperScan`, counts + ms only, one scanId)
Log: time to first Room emission + row count; MediaStore query ms + rows; enrichment ms for new/changed rows; each SAF folder ms; Room write ms; time until `SonoraViewModel.tracks` is non-empty.

### A1. Likely causes to check (verify each in `MusicRepository`, `MediaStoreScanner`, `SafFolderScanner`, `SonoraViewModel`, `SonoraMainContent`)
1. Startup sync runs **before/while** the cache is published, or overwrites `_rawTracks` with an empty list.
2. Full-screen loading is tied to `isLoading` while a background sync runs, so UI waits for the scan even when Room has rows.
3. Per-track work inside the scan loop: `MediaMetadataRetriever`, embedded-art extraction, bitmap decode, `File.exists()`, duration probing, one Room query/insert per track.
4. SAF folders (`DocumentFile`/`DocumentsContract`) walked fully on every start and awaited before the first publish.
5. Scanner filter hides valid MP3s (`IS_MUSIC`, MIME or extension mismatch), or a null cursor/empty result is treated as "all missing".
6. The whole library is re-read from Room and re-sorted/regrouped on the main thread after every change.
7. Two scans run at once (init + permission callback + manual).

### A2. Required behaviour
- **Publish cached Room tracks first, immediately.** Nothing waits on MediaStore, SAF or artwork.
- Replace the blocking spinner: `firstScan` (only when cache is empty: progress + count) vs `isSyncing` (small non-blocking "Updating library…" bar).
- Single-flight scans (`Mutex`/shared Job). Manual rescan and startup sync cannot overlap.
- Header pass only: query minimal columns (`_ID, TITLE, ARTIST, ALBUM, ALBUM_ID, DURATION, DATE_ADDED, DATE_MODIFIED, SIZE, MIME_TYPE, TRACK, YEAR, DISPLAY_NAME`); no art/metadata work in this pass. Diff by `_ID + DATE_MODIFIED + SIZE`; insert/update only new/changed rows in batched transactions (`withTransaction`, chunks of ~500), `Dispatchers.IO`; grouping/sorting on `Default`.
- **Publish partial results during a first scan** every ~500 rows so the user sees songs appear.
- Enrichment (artwork, tags, duration fix) runs afterwards as a low-priority resumable background job, throttled, never blocking list display; art stored once per track under `filesDir/art/` keyed by hash; do not redo for unchanged rows.
- SAF: persisted-URI scan runs after MediaStore publish, per folder, incremental by `documentId + lastModified + size`, non-blocking; never silently skip a user folder; revoked permission → mark folder "needs access" in Settings.
- Keep first-import timestamp (`addedAt`) for existing rows; MediaStore rows use real `DATE_ADDED`.
- Safety: **never delete cached rows** on no permission, null cursor, exception, zero rows, or > 40 % missing. Soft-mark missing, purge only after 2 clean syncs.
- Per-file errors (`SecurityException`, `FileNotFound`, corrupt tags) skip that file only. Missing title/artist/album/art never rejects a file (fallbacks: filename, "Unknown artist", "Unknown album").
- Permission: Android 13+ `READ_MEDIA_AUDIO`, ≤ 12 `READ_EXTERNAL_STORAGE`. Check before scanning; if denied show a "Permission required" state with the existing action, keep the cache.
- UI states are distinct: Permission required · No audio found · Scan failed (Retry) · Updating library.
- Stable keys in all lazy lists; Coil requests sized to the displayed size.

---

## B. PLAYBACK END-TO-END (Media3 stays)

Trace: row click → `SonoraMainContent` → `SonoraViewModel.playTrack(track, queue, source)` → `SonoraPlayerClient.playTrack` → connected `MediaController` → `SonoraMediaService` ExoPlayer → `Player.Listener` → StateFlows → MiniPlayer / NowPlaying / notification.

- Tapped item becomes current immediately; start index = its index in the exact visible list; order `setMediaItems(items, index, 0)` → `prepare()` → `play()`.
- If the controller is not connected yet, **queue the request and run it after connect**; rapid taps keep only the latest.
- `MediaItem`: stable non-empty `mediaId`, audio `contentUri` (never the artwork URI), metadata (title/artist/album/artwork URI).
- UI state comes only from callbacks: `onMediaItemTransition`, `onIsPlayingChanged`, `onPlaybackStateChanged`, `onPlayerError`, `onTimelineChanged`. No optimistic `isPlaying = true` that stays after an error.
- "Playing from" label is set together with the queue, never stale.
- Error → snackbar "Can't play this file — it may have moved or permission was removed", skip to next valid item with a loop guard (stop after one full pass), keep the queue, offer Retry.
- Works from Songs, Search/Discover, Albums, Artists, Playlists, Favorites, Queue, Recently Added. EQ, focus, notification, shuffle/repeat/seek unchanged.

---

## C. REMOVE DOWNLOADER COMPLETELY

- Remove from drawer, menus, routes, `VesperFullScreenDestination.DOWNLOADER`, `SonoraMainContent`, ViewModel state/actions, `Application` init, `DownloadEntitlements`, `VesperDownloadManager`, package `com.vesper.audio.downloader`, `DownloaderScreen`, download Settings rows/strings.
- Remove downloader-only manifest entries (foreground service, notification channel, permissions) and dependencies only after confirming nothing else uses them.
- Downloader Room tables/DAO: drop only with a versioned `Migration` that preserves all other data; if unsure, leave the unused table and say so.
- **Keep** local folder import (SAF) and all local features.
- After removal grep for `Downloader`, `DOWNLOADER`, `Download` and explain any remaining hit. No dead route, no crash.

---

## D. DISCOVER (Audius + MusicBrainz), local search never blocked

### D1. UI (keep burgundy identity and accent tokens)
Title + short subtitle → search field → **chip row directly below: `Songs | Albums | Artists`** (selected chip uses accent; selection remembered) → source filter `All | Audius | Local` → when query is empty: *Trending on Audius*, *Popular genres* (only if the API supports it) → results with Load-more paging → loading / empty / offline / rate-limited / retry states.
Every item shows a small **Local** or **Audius** badge. Remote items never look like local files.

### D2. Behaviour
- Local results appear instantly; remote results stream in after a 300–500 ms debounce (`flatMapLatest`, cancel stale calls, no request per keystroke, no duplicate requests on recomposition).
- Query, chip and scroll survive going to Now Playing and back.
- Albums/Artists tabs show real album/artist/playlist records from the provider; do not invent them from track titles.
- Offline: local search and playback fully work; remote shows a non-blocking "You're offline".

### D3. Audius (read the current official docs first: https://docs.audius.org)
I could not verify the current auth rules; **you must read the docs and follow them**. Do not assume a key is or isn't needed.
- Use only official endpoints for: trending tracks, track search, user/artist search, playlists/albums search, genres (if offered). Select the API host as documented (do not hardcode a single node if the docs say to discover one).
- Pass the documented `app_name` (use `VESPER`). If the docs require an API key for the endpoints you need: **do not put it in the app**; add a tiny configurable `BASE_URL` for a proxy I may host later, and until then show "Discover unavailable" rather than embedding a secret.
- Parse defensively (all nullable): `id`, `title`, `user.name`, `user.profile_picture`, `artwork`, `duration`, `genre`, `play_count`, `favorite_count`, `is_streamable`, `is_downloadable`, stream URL or stream endpoint.
- Play **only** when `is_streamable` is true; otherwise the row is greyed with "Not available to stream". Resolve the stream URL at play time (it can expire); on failure show retry.
- **Streaming ≠ download.** No download button, no caching of remote audio to disk, no use of `is_downloadable` except informational text.
- Show "Audius" attribution in the row, track details and Now Playing.

### D4. MusicBrainz (metadata only, never audio)
- Base `https://musicbrainz.org/ws/2/`, JSON, `User-Agent: VESPER/<version> (<contact URL or email I control>)` — use a placeholder constant `MB_CONTACT` and tell me to fill it; do not invent an email.
- Hard limit **1 request/second** (single shared rate limiter), cache responses (Room or bounded LRU, 24 h+).
- Use for: artist/release info on artist/album detail pages and fuzzy-match enrichment of **local** tracks with missing tags, only on a confident match, never overwriting better local tags, opt-in in Settings.
- Cover art: Cover Art Archive (`coverartarchive.org`) via MBID, sized thumbnails only, for albums that have no local art.
- Never present MusicBrainz items as playable.

### D5. Model and playback
Provider-neutral `DiscoverItem(provider: LOCAL|AUDIUS, providerId, title, artist, album, durationMs, artworkUrl, isStreamable, isDownloadable, genre, attribution)`. Keep remote metadata in a bounded cache table/in-memory cache; **never insert remote tracks into the local songs table**. Source type is an explicit field, never a URI guess.
Queue entries carry source type; remote and local stay distinguishable; Next/Prev work across them; a stream failure never affects local playback.
Artwork: existing Coil, bounded sizes, placeholder + error fallback; one bad URL never crashes a list.

---

## E. SUPPORT SCREEN (no paywall)

Settings → "Support VESPER": short thank-you text, a UPI QR image and a Ko-fi/Buy-Me-a-Coffee link opened with `ACTION_VIEW`. No in-app purchases, no accounts, no feature locks. Put the link/QR in `strings`/assets as placeholders I can edit and tell me where.

---

## F. ACCEPTANCE (mark PASS / FAIL / NOT RUN)
1. Force-stop, reopen with 1,458 tracks: songs visible within ~1 s from cache; no blocking spinner; background bar appears and disappears.
2. Clear app data, grant permission: songs appear progressively during the first scan.
3. No duplicates after 3 relaunches and a manual rescan; nothing deleted when permission is revoked.
4. Tap a song in Songs, Search, Album, Artist, Playlist, Favorites: exact song plays; mini-player = Now Playing = notification.
5. Delete a file then play it: snackbar, no infinite skip loop.
6. Downloader: no UI entry, no route, app builds, existing data intact.
7. Discover offline: local works; online: trending + search work; Songs/Albums/Artists chips behave; badges correct; streamable plays; non-streamable blocked; no download control.
8. MusicBrainz calls never exceed 1/s; no key in source, `BuildConfig`, resources or logs.
9. Appearance, Home, favourites, playlists, EQ, lyrics unchanged.

## G. FINAL REPORT (≤ 25 lines)
Root causes (A, B) · files changed/added/deleted · Downloader remnants · Audius endpoints + auth finding from the docs · security statement (no secret in APK; backend needed? yes/no) · migrations · checks run with PASS/FAIL/NOT RUN · 6-step manual test for the Redmi K50i.
