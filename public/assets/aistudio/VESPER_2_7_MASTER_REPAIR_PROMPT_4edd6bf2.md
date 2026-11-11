# VESPER 2.7 — MASTER REPAIR & POLISH PROMPT
## Library intelligence • Metadata and artwork • Accent system • Custom background • A–Z navigation • Feature Zero • Discover • Performance

**Project:** VESPER Android music player  
**Target:** Existing VESPER 2.7 codebase  
**Platform:** Android (Kotlin / Jetpack Compose; preserve the project's actual architecture and dependency versions)  
**Mode:** Repair and improve the existing app. Do not rebuild it from scratch.  
**Priority:** Correctness first, then performance, then visual polish.

---

# 0. HOW TO USE THIS PROMPT

Attach the latest complete VESPER project ZIP, this Markdown file, the 16:9 reference board, and the supplied screenshots/videos to the same Google AI Studio conversation.

Use the current project source as the only source of truth. The screenshots and videos show the intended design and observed problems; they do not override the existing architecture.

Read the entire prompt before editing. Then implement the work in the ordered batches B1–B9 below. Do not attempt a giant unreviewed rewrite. For every batch:
1. Inspect the relevant existing files and trace the current implementation.
2. Explain the exact files and functions that need changing.
3. Make the smallest coherent implementation.
4. Run available compilation, lint and tests.
5. Report changed files, completed behaviour, test results, remaining limitations and next batch.
6. Preserve all unrelated working features.

Do not claim that a feature is fixed unless its code path and relevant test/device behaviour support that claim.

# 1. PRODUCT RULES — NON-NEGOTIABLE

- Keep VESPER offline-first, private, account-free and free of ads and telemetry.
- Local playback must continue to work without internet.
- Do not upload, transmit, fingerprint or remotely analyse the user's music library.
- Do not introduce paid services, API keys, cloud accounts or unnecessary dependencies.
- Do not modify or remove the existing Downloader / Importer module unless a directly necessary integration bug is proven. Preserve its current behaviour.
- Preserve playlists, favourites, history, queue, lyrics, equalizer, playback controls, metadata editor and all existing working features.
- Do not copy Oto Music assets, source code, branding or exact proprietary screen composition. Use the supplied references for interaction patterns only. VESPER must retain its own identity.
- Do not fabricate metadata, artwork, statistics, playback formats or technical capabilities.
- Never overwrite original audio files or embedded tags as part of scanning or artwork repair.
- Never request broad storage access if the existing MediaStore / SAF permissions are sufficient.
- Keep compatibility with the project's current minSdk, targetSdk, Kotlin, Compose, Media3, Room and Gradle versions. Inspect versions before changing anything.
- Do not add dependencies merely for convenience. Prefer existing libraries and Android platform APIs.

# 2. PRIORITY ORDER

**P0 — Metadata and artwork correctness.** This is the highest priority. VESPER and Oto Music are reading the same folders on the same phone, but VESPER displays far fewer tags and covers. Investigate the real cause; do not simply add placeholder images.

**P1 — Accent colour consistency.** The selected accent must apply across the entire app. Burgundy should not remain as an accidental hard-coded dominant colour.

**P2 — Custom background image.** The user must be able to choose an image, preview it, apply it, and still see it after closing and reopening the app.

**P3 — A–Z bookmark side rail.** Implement a real narrow vertical alphabet index, not a letter list.

**P4 — Speed, incremental scanning and file handling.** Keep the already-improved cache behaviour. Improve it without regressing instant startup or cached library display.

**P5 — Settings consolidation.** Remove the separate visible “Storage & Cache” section and combine its useful controls into one “Feature Zero” entry alongside privacy information.

**P6 — About page and changelog.** Add Allen JR and update the displayed release to 2.7. Remove the Tech and License sections as requested.

**P7 — Navigation glow trail.** Add a minimal, elegant, low-cost animated trail that follows the active bottom navigation item and adapts to the chosen accent.

**P8 — Discover.** Rename Search to Discover and redesign its screen with the requested title, search and centered category navigation.

# 3. B1 — METADATA AND ARTWORK ENGINE (P0)

## 3.1 Diagnose before changing
Trace the complete pipeline from Android MediaStore / SAF permission through scan, metadata extraction, Room persistence, cache lookup, URI construction and Compose image rendering.

Compare:
- Total supported audio files visible to VESPER.
- Total audio files returned by MediaStore.
- Tracks with title, artist, album, album artist, genre, year, track number and disc number.
- Tracks with embedded artwork.
- Tracks with folder-level artwork.
- Tracks with MediaStore album artwork.
- Tracks with no discoverable artwork.
- Tracks whose artwork exists but fails to decode or render.

Produce a diagnostic summary with counts and representative failure reasons. Do not assume Oto Music has access to files VESPER cannot access; verify permissions, folder scope, file types, URI handling and extraction differences.

## 3.2 Metadata extraction
Implement robust, defensive metadata extraction for formats supported by the current project and Android APIs. Preserve:
- Title
- Artist
- Album
- Album artist
- Genre
- Year / release date
- Track number
- Disc number
- Duration
- MIME type / container
- Bitrate and sample rate only when reliably available
- Embedded lyrics only if the current implementation supports safe extraction

Use the best available source for each field, with a documented precedence order. Example: embedded file tags where readable, then reliable MediaStore columns, then filename-derived fallback only for missing fields. Do not replace valid embedded values with weaker guessed values.

Handle ID3 variants, MP4/M4A metadata, FLAC Vorbis comments, OGG/Opus tags and other formats only where supported by the current extractor/library. If a format is unsupported, report that limitation honestly instead of returning invented values.

Avoid blocking the main thread. Run extraction on an appropriate IO dispatcher or background worker. Bound concurrency so scanning does not overload the phone.

## 3.3 Artwork discovery and precedence
Implement a deterministic artwork resolver. Check sources in a sensible order:
1. Embedded artwork extracted from the audio file.
2. Folder artwork such as `cover`, `folder`, `album`, `front` image names, including supported JPG/JPEG/PNG/WebP formats.
3. MediaStore album-art/content URI where available and valid.
4. Existing app artwork cache.
5. A neutral generated placeholder only when no real artwork exists.

Do not treat a placeholder as recovered original artwork. Keep the artwork source type in diagnostic state so the user can distinguish embedded, folder, MediaStore and fallback artwork.

Important:
- Do not assume a `content://` URI can be converted to a filesystem path.
- Use `ContentResolver` streams or file descriptors for content URIs.
- Handle SAF tree URIs and persisted URI permissions correctly.
- Handle files with no readable parent directory.
- Support case-insensitive image filenames.
- Avoid loading full-resolution images into list rows.
- Decode appropriate thumbnail sizes for list/grid use and larger images for Now Playing.
- Apply EXIF orientation where relevant.
- Close streams, cursors, descriptors and retrievers reliably.
- Catch corrupt, unsupported and permission-denied files without crashing the scan.

## 3.4 Artwork cache
Use stable cache keys based on the best available file identity and change markers, such as MediaStore ID / URI plus size and modified timestamp. Avoid using only the track title or album name as a key.

Requirements:
- Cache successful artwork and negative results with a short-lived/recheckable policy.
- Invalidate only when the underlying file or artwork source changes.
- Do not re-extract every image on every app launch.
- Keep thumbnails and full-size artwork separate if the current architecture needs both.
- Ensure the same album does not decode the same image repeatedly for every track.
- Use memory and disk caches appropriately; do not retain huge bitmaps in memory.
- Ensure cache clearing removes only disposable derived artwork data, not the indexed music library or original audio.
- Prevent stale album art when a folder image is replaced.
- Add robust fallback when a cached file is missing or corrupt.

## 3.5 Artwork display and coverage
Ensure all relevant UI surfaces use the same resolver and cache:
- Songs list
- Albums list and album detail
- Artists list and artist detail
- Playlists
- Favourites
- Recently Added
- History
- Continue Listening
- Now Playing
- Queue
- Discover results

Do not allow one screen to show artwork while another screen silently uses a different broken path.

**Acceptance criteria**
- The app reports a real coverage breakdown, not a fake “100% loaded” label.
- Every track with readable embedded or folder artwork displays that artwork.
- Albums use consistent artwork selection across screens.
- Tracks with no actual image use a clear fallback.
- Scrolling a large library does not repeatedly decode full-resolution images.
- Missing/corrupt artwork does not crash the app.
- Add tests for source precedence, URI handling, cache key invalidation and fallback behaviour.

# 4. B2 — ACCENT COLOUR SYSTEM (P1)

Create one central theme/accent source of truth. Remove scattered hard-coded burgundy values from reusable components where they represent selection, interaction or accent states.

The selected accent must consistently drive:
- Bottom navigation selected icon and label
- Active navigation indicator and glow
- Buttons and primary actions
- Selected tabs and chips
- Sliders and progress indicators
- Switches and checkboxes
- Focus and pressed states
- Selected list rows and cards
- Playlist controls and playlist detail highlights
- A–Z active letter and letter bubble
- Discover category selection
- Equalizer controls and selected preset
- Dialog confirmation actions
- Artwork overlay/tint elements where intended

Keep the brand palette as the default. Burgundy may remain in the VESPER logo or explicitly branded decorative elements, but it must not override a user's selected accent in functional UI.

Requirements:
- Persist the selected accent.
- Apply it immediately without app restart.
- Ensure every screen observes the same state.
- Use accessible contrast for text and icons.
- Avoid tinting original album artwork itself; artwork should remain faithful to the source.
- Do not create a separate theme instance per screen.
- Test at least the default burgundy, blue, green, purple and amber accent choices.

**Acceptance criteria:** changing the accent updates all relevant screens immediately and remains selected after force-stop/relaunch.

# 5. B3 — CUSTOM BACKGROUND IMAGE (P2)

Add a real background image selection feature to Appearance Studio.

Required flow:
1. User taps “Choose background image”.
2. Open Android Photo Picker or a suitable SAF document picker.
3. User selects an image.
4. Display a preview with crop / fit positioning controls.
5. User taps Apply.
6. Save a durable reference or copy the selected image into app-private storage. Do not save a temporary picker URI unless persistable permission is successfully obtained.
7. Apply it across the intended app background surfaces.
8. Retain it after process death, force-stop and device restart.

Controls:
- Enable/disable custom background.
- Choose image.
- Preview.
- Crop / fit / center positioning.
- Background dim level or overlay strength.
- Reset to default.
- Clear selected image.

Implementation:
- Use lifecycle-safe image loading.
- Validate file type and decodeability.
- Resize large images for background use without destroying the original selected file.
- Avoid reading the full image on the main thread.
- Ensure text and controls remain legible with dark overlay / scrim.
- Respect system bars and edge-to-edge layout.
- If the selected file becomes unavailable, recover gracefully and offer reselection.

**Acceptance criteria:** selection, Apply, persistence and reset all work on a physical Android device. A toast alone is not a valid implementation.

# 6. B4 — SPEED, INCREMENTAL INDEXING AND FILE HANDLING (P4)

The existing cache behaviour is now good. Preserve it. Do not replace it with a full rescan or a new cache architecture without proof of a defect.

Target behaviour:
- On launch, display the last valid Room-backed library immediately.
- Run a lightweight change check in the background.
- Process only added, modified or deleted media.
- Do not re-parse every track merely because the app process was killed or RAM was cleared.
- Keep playback and navigation responsive during updates.
- Show a subtle scan/update indicator only when work is happening.

Use MediaStore generation/version markers where supported by the target Android API, or a reliable equivalent. Use file size, modified time and stable media identity as secondary change markers. Handle MediaStore resets, permission changes, missing records, removable storage and SAF tree changes.

Requirements:
- Separate library indexing from metadata/artwork enrichment.
- Use transactions / batches for Room writes.
- Avoid N+1 database queries.
- Add indexes to frequently queried columns only where justified.
- Use Paging or lazy list strategies appropriate to the current project.
- Cancel obsolete search and scan jobs.
- Debounce text search.
- Avoid blocking IO or bitmap decode on the UI thread.
- Avoid unnecessary recomposition and unstable list keys.
- Preserve list scroll position and playback state during refresh.
- Ensure file descriptors, streams, cursors and retrievers are closed.
- Avoid loading all full-size artwork or all track objects into memory.
- Handle low-storage and permission-revoked conditions.
- Do not delete valid cached library data just because a scan temporarily fails.

Provide a small diagnostics panel or log summary for:
- Last successful scan time
- Number of tracks indexed
- Number of changed tracks processed
- Number of deleted / inaccessible tracks
- Artwork extraction successes and failures
- Scan duration

**Acceptance criteria**
- Relaunch shows cached library without a full scan.
- A single new song is added without reprocessing the entire library.
- A modified song refreshes its metadata/artwork.
- A deleted song is removed from the index safely.
- Revoking and restoring permission does not corrupt the database.
- Large libraries remain scrollable and playback is not interrupted.

# 7. B5 — A–Z BOOKMARK SIDE NAVIGATION (P3)

Create a real vertical alphabet rail beside the Songs list, inspired by the supplied bookmark-style reference.

Visual requirements:
- Very narrow vertical rail aligned to the right edge of the Songs list.
- Letters A–Z in a compact, readable column.
- A small active-letter marker or bookmark highlight.
- On touch, show a floating letter bubble near the user's finger.
- Minimal, premium VESPER styling.
- Accent-aware, not hard-coded burgundy.
- Do not let the rail cover song titles, overflow menus or system gesture area.

Interaction:
- Tap a letter to jump to that alphabetical section.
- Drag continuously up/down to scrub through letters.
- Scroll the Songs list to the first matching item for the selected letter.
- Handle sections with no tracks gracefully.
- Include `#` for numeric/symbol-prefixed titles if the existing sorting model supports it.
- Use locale-aware alphabetical ordering where practical.
- Maintain stable sort order for tracks with identical titles.
- Keep the rail synchronized when the user manually scrolls.
- When search is active, do not force the entire library's A–Z index onto filtered results unless the filtered result set can support it correctly.
- For search results, next/previous swipe and scrolling must follow the current filtered result order, not the full library order.

Scope:
- Implement for Songs first.
- Add to Albums and Artists only if their current list architecture can support it cleanly and consistently.
- Do not introduce a separate full-screen alphabet list.
- Ensure accessibility semantics and an alternative accessible way to navigate.

**Acceptance criteria:** tap and drag both jump accurately; the letter bubble tracks the finger; the list and rail remain synchronized; normal vertical scrolling still works.

# 8. B6 — SETTINGS: MERGE STORAGE & CACHE INTO “FEATURE ZERO” (P5)

Remove the separate visible “Storage & Cache” section from the Settings screen.

Create one clean entry called **Feature Zero** (or “Feature Zero — Privacy & Local Data”) that combines the useful content of:
- Zero Cloud Tracking / privacy explanation
- Local-only storage and cache explanation
- Artwork cache size and clear-cache action
- Playback history and clear-history action
- Downloader / Importer entry, if it currently lives in the removed section

Do not delete useful functionality when removing the section. Move it into the Feature Zero screen.

Feature Zero screen:
- Explain in friendly language that music, library index, preferences and derived artwork cache stay on-device.
- Explain that clearing artwork cache does not delete music files or the indexed library.
- Show current artwork cache size only if measured from actual cache files.
- “Clear artwork cache” must clear only the derived artwork cache and refresh artwork safely.
- “Clear playback history” must clear only history records after confirmation.
- Keep Downloader / Importer accessible.
- Show privacy and permission information without making unsupported absolute claims.
- Do not say “100% private” if any current feature makes network requests. Explain actual behaviour accurately.
- Avoid duplicated sections or repeated privacy cards.

Settings should be simplified into useful groups:
1. Appearance
2. Sound Studio
3. Playback
4. Library & Scanning
5. Feature Zero
6. About VESPER

Retain important working settings. Do not remove features just because they are less common. Remove only duplicate, misleading, broken or redundant entries after tracing their code paths.

# 9. B7 — ABOUT PAGE AND VERSION 2.7 CHANGELOG (P6)

Update the About screen to VESPER 2.7.

Keep the existing VESPER logo and premium offline-first visual style.

Contributor cards:
- **Nobal Antony** — Lead Designer & Architect
  - Tagline: “Crafted with passion for offline acoustic precision and cosmic minimalism.”
- **Allen JR** — SaaS Manager & Strategy Lead
  - Add a concise, professional tagline such as: “Shaping product direction, growth strategy and the VESPER experience.”
  - Do not invent employment history, qualifications or claims.

Display both contributor cards in a balanced layout. On narrow devices, stack them vertically. On wider devices, place them side by side if readable.

Remove the “Tech” and “License” sections from the visible About UI as requested. Do not remove legal notices or third-party license files that must remain in the source distribution. This is a UI cleanup, not permission to violate software licenses.

Update version display and changelog to **2.7** using the actual app version configuration. Do not hard-code a display version that disagrees with Gradle `versionName`.

Suggested changelog entries (include only items actually implemented):
- Improved metadata extraction and artwork resolution.
- More reliable artwork caching and cache invalidation.
- Consistent app-wide accent colour.
- Custom background image support.
- A–Z bookmark navigation.
- Faster incremental library updates.
- Simplified Feature Zero settings.
- Redesigned Discover experience.
- Refined navigation animation.
- About page contributor update.

# 10. B8 — BOTTOM NAVIGATION GLOW TRAIL (P7)

Refine the bottom navigation bar with a minimal animated glow trail inspired by a flowing energy pulse, but keep it subtle and premium.

Requirements:
- Trail follows the active navigation item.
- Smoothly animates between selected destinations.
- Use the currently selected accent colour.
- Use a soft glow with restrained blur and opacity.
- Keep the icon and label readable.
- Do not create a large particle system, full-screen shader, continuous expensive animation or battery-draining effect.
- Pause or reduce animation when app is backgrounded.
- Respect Android reduced-motion / animation scale preferences.
- Do not animate every icon continuously.
- Avoid layout shifts and touch-target changes during animation.
- Ensure it works at different screen widths and with system gesture navigation.

**Acceptance criteria:** active item transition is smooth, subtle, accent-aware, low overhead and does not interfere with taps.

# 11. B9 — DISCOVER REDESIGN (P8)

Rename the bottom navigation destination **Search** to **Discover**.

When selected, display a screen with:
- Top-left title: **Discover**
- Subtitle or heading: **Discover music and albums**
- A centered category navigation row below the search bar.
- A search field that searches the user's local songs and albums.
- Category navigation that can include Songs, Albums, Artists and Genres where those datasets exist.
- Clear selected state using the global accent system.
- Empty, loading and no-results states.

Important distinction:
- This request is a redesign of the local music discovery/search experience.
- Do not silently add online streaming or remote catalogue APIs as part of this task.
- Keep it fully functional offline.
- If a separate online discovery feature already exists, preserve it and keep its network behaviour opt-in and clearly labelled.
- Search must cancel obsolete requests/jobs, debounce input, and use the correct current result ordering.
- Search results must not accidentally navigate to the Songs tab.
- Selecting a song or album must open the correct detail/playback action.
- Do not fabricate trending charts, popularity or recommendations from local data.

# 12. RESPONSIVE DESIGN AND ACCESSIBILITY

- Test small Android phones, standard phones and larger screens.
- Respect display cutouts, status bar and navigation bar insets.
- Avoid clipped labels, overflowing buttons and hidden controls.
- Use sensible minimum touch targets.
- Keep contrast readable with all accent colours and custom backgrounds.
- Support font scaling.
- Ensure the A–Z rail does not overlap system gestures.
- Preserve portrait usability; landscape should not break layouts.
- Do not use fixed pixel dimensions where responsive Compose constraints are appropriate.

# 13. TESTING AND REGRESSION REQUIREMENTS

Create or update tests using the existing project test setup. Do not add a new test framework unless required.

Minimum checks:
1. Metadata precedence and missing-field fallback.
2. Embedded artwork extraction and invalid image handling.
3. Folder artwork discovery with case-insensitive names.
4. Content URI and SAF URI handling.
5. Artwork cache hit, miss and invalidation.
6. Album artwork consistency across screens.
7. Accent persistence and propagation to all shared components.
8. Custom background picker, Apply, persistence and reset.
9. Incremental scan: unchanged, added, modified and deleted files.
10. Permission revoked/restored behaviour.
11. A–Z tap, drag and section mapping.
12. Search-result ordering and navigation.
13. Feature Zero clear-cache and clear-history boundaries.
14. About page version and contributor display.
15. Bottom navigation animation reduced-motion behaviour.
16. Discover search across songs and albums.
17. Existing playback, queue, playlists, favourites, history, lyrics, EQ and downloader regression tests.

If emulator or physical-device testing is unavailable, clearly state that. Do not claim device verification from source inspection alone.

# 14. REQUIRED IMPLEMENTATION BATCHES

Follow this order. Do not skip ahead to visual polish while P0 is broken.

| Batch | Work | Exit condition |
|---|---|---|
| B1 | Audit metadata/artwork pipeline and reproduce failure | Root cause and measurable baseline documented |
| B2 | Repair metadata extraction, artwork discovery, cache and rendering | Tests pass; coverage breakdown available |
| B3 | Centralize accent system | Accent applies across all relevant components |
| B4 | Implement custom background image selection and persistence | Choose, preview, apply, relaunch and reset verified |
| B5 | Implement A–Z bookmark rail | Tap, drag, letter bubble and scroll synchronization work |
| B6 | Improve incremental indexing, performance and file handling | Cached startup and delta scan verified |
| B7 | Merge Settings into Feature Zero | No separate Storage & Cache section; controls preserved |
| B8 | Update About, version and changelog | Both contributors shown; 2.7 version consistent |
| B9 | Add glow trail and Discover redesign; run full regression | UI, performance and regression checks complete |

If a batch reveals a dependency, explain it and make the smallest prerequisite change. Do not silently reorder or combine unrelated work.

# 15. STRICT RULES FOR AI STUDIO OUTPUT

- Do not regenerate the whole app.
- Do not replace working architecture with a new architecture.
- Do not create duplicate repositories, duplicate screens or parallel implementations.
- Do not change package names, application ID, signing configuration or Gradle versions unless absolutely necessary and explicitly explained.
- Do not modify Downloader internals unless a proven direct dependency requires it.
- Do not remove existing user data or Room tables.
- Do not reset the database to solve a migration issue.
- Do not overwrite original music files or tags.
- Do not add dummy artwork, fake recommendations, fake statistics or fake metadata.
- Do not hard-code the 2.7 version separately from the actual build configuration.
- Do not report a successful build unless the build command completed successfully.
- If compilation fails, show the exact error and fix it before continuing.
- If a required Android permission is missing, explain the minimum permission and implement a user-friendly recovery flow.
- Keep code changes focused and explain any unavoidable trade-off.
- Preserve the user's current working features.

# 16. REQUIRED REPORT AFTER EACH BATCH

Use this exact structure:

## Batch Bx — [name]
**Root cause:** What was actually wrong.  
**Files inspected:** List.  
**Files changed:** List.  
**Implementation:** What changed and how it works.  
**Tests run:** Exact commands and results.  
**Device verification:** Verified / not available / still required.  
**Known limitations:** Be honest.  
**Regression risks:** What could be affected.  
**Next batch:** The next planned step.

At the end of B9, provide:
- Full changed-file list.
- Build command and exact result.
- Unit/instrumentation test command and exact result.
- APK output location if a build succeeded.
- Manual physical-device test checklist.
- Remaining known bugs.
- Confirmation that Downloader and unrelated features were preserved.
- A short VESPER 2.7 changelog containing only verified work.

# 17. FINAL ACCEPTANCE CHECKLIST

Do not call the project finished until every applicable item is checked:

- [ ] Metadata extraction is measurably improved and missing fields are explained.
- [ ] Original embedded and folder artwork appears wherever available.
- [ ] Artwork source and fallback are distinguishable in diagnostics.
- [ ] Artwork cache is stable, invalidates correctly and avoids repeated decoding.
- [ ] Accent colour applies across all relevant screens and persists.
- [ ] Burgundy no longer unintentionally overrides selected accents.
- [ ] Custom background can be selected, previewed, applied and persisted.
- [ ] A–Z bookmark rail supports tap and drag.
- [ ] Search results retain their own correct ordering.
- [ ] Cached library appears immediately after relaunch.
- [ ] Incremental scans process only changed media under normal conditions.
- [ ] Settings has no separate Storage & Cache section.
- [ ] Feature Zero contains the useful privacy, cache and history controls.
- [ ] About shows Nobal Antony and Allen JR with the requested titles.
- [ ] Tech and License sections are removed from the visible About page without deleting required legal files.
- [ ] Version and changelog accurately show 2.7.
- [ ] Bottom navigation glow trail is subtle, responsive and low overhead.
- [ ] Search is renamed Discover and the redesigned local search works.
- [ ] Existing playback, playlists, favourites, queue, history, lyrics, EQ and Downloader still work.
- [ ] Build and tests are reported honestly.

**Start with B1 only. Audit the existing project, identify the actual metadata/artwork failure, and report the root cause before making changes. Then proceed batch by batch.**
