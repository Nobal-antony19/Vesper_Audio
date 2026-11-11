# VESPER — Stability Audit, Audio Import Repair & <25 MB Optimization

**Work against the uploaded VESPER project ZIP.** This is an implementation task, not a redesign. Preserve the existing visual language, features, Room data, playback history, playlists, favourites, settings, artwork, lyrics, EQ, appearance studio and all working behaviour except the Downloader feature explicitly being removed.

## Non-negotiable execution order

1. Inspect the entire project and build configuration. Trace the real code paths before editing.
2. Run the available Gradle build and tests first. Record baseline failures. Add/repair focused tests for parser, import, repository and navigation.
3. Audit and fix correctness issues, especially audio parsing/import and playback. Do not start size trimming until the stability gate below passes.
4. Remove Downloader completely, then add Donate navigation and repair the alphabetical fast-scroll/swipe index.
5. Optimize startup, library scanning, scrolling, memory and package size. Rebuild and run tests after each stage.
6. Report exact files changed, commands executed, test/build outcomes, measured APK/AAB sizes and any remaining device-only checks. Never claim a test passed if it was not run.

## A. Full project health audit

Inspect every Kotlin source file, Gradle file, manifest, Room entity/DAO/migration, navigation route, resource and test. Check for:
- Compile errors, unresolved references, broken imports, unused/dead code, duplicate implementations and dependency/version conflicts.
- Runtime crashes, nullability mistakes, unsafe casts, lifecycle leaks, coroutine cancellation/threading mistakes, blocking main-thread work, race conditions and stale state.
- Broken navigation/back handling, missing callbacks, empty/error/loading states, invalid URI handling and permission-denied behaviour.
- Room schema/version/migration correctness. Never clear or destructively recreate the database.
- Media3 service/controller lifecycle, queue updates, playback state, notification, audio focus, headset/Bluetooth route changes and playback after app backgrounding.
- Compose recomposition churn, unstable list keys, nested scrolling issues, expensive sorting/grouping on main thread, image decode/cache pressure and excessive allocations.
- Manifest permissions and API-level compatibility. Remove only permissions made unnecessary by removed features.

Produce a concise defect list with severity and evidence before applying fixes. Fix confirmed defects; do not refactor unrelated code for style alone.

## B. Audio parser, library scan and Import Audio — critical gate

Trace the complete flow:
**user taps Import Audio / grants access → SAF picker or MediaStore permission → scanner → metadata parser → Track model → Room cache → repository StateFlow → UI list → tap track → Media3 playback.**

Verify real files, not demo placeholders. Test MP3 first, then M4A/AAC, FLAC, WAV, OGG/Opus where supported. Validate:
- Title, artist, album, album artist, duration, track/disc number, year, genre, MIME/format, size, modified time, stable ID and artwork.
- Missing/corrupt/partial metadata, ID3v1/v2 tags, Unicode, long filenames, duplicate titles, malformed files, zero-duration files and files with no embedded artwork.
- SAF folder tree recursion, nested folders, persisted URI permissions, revoked permissions, inaccessible documents and duplicate detection.
- MediaStore query projection compatibility by Android version, scoped storage, `IS_MUSIC` filtering (do not silently exclude legitimate audio files), content URI construction and permissions.
- Importing one file and selecting a folder; rescanning; app restart; adding/removing/renaming a file; duplicate avoidance; permission denial/regrant.
- The parser must close `MediaMetadataRetriever` in `finally` (`release()`), catch per-file failures without aborting the entire scan, and never block the main thread.
- Avoid opening a full metadata retriever for every unchanged file. Use stable source IDs and reliable change signatures (MediaStore ID/date modified/size; SAF document URI/modified/size where available). Cache parsed metadata and artwork references.
- Do not assume `DATA` filesystem paths exist or are readable; use content URIs. Do not rely on file extensions alone.
- Ensure scan cancellation, single-flight scan protection, bounded concurrency and progress/error reporting. Do not swallow exceptions silently or report success for partial/failed scans.
- Cached Room tracks should render immediately while a background incremental scan runs. Do not show an empty library while scanning if cache exists. Do not delete cached tracks when permission is temporarily denied or a provider query fails.
- Preserve user-imported SAF folders and persisted grants. Do not auto-import remote streams as local files.

**Stability gate:** Do not proceed to the sub-25 MB optimization stage until parser/import tests and a successful debug build pass. If a real device/emulator is unavailable, state exactly what remains unverified and do not claim full validation.

## C. Remove Downloader completely

The user wants the Downloader feature removed, not hidden:
- Remove Downloader from drawer/side navbar, navigation destinations, routes, menus, buttons, settings and all entry points.
- Remove Downloader screen, download manager, entitlement/supporter tier/debug toggle, download queue/progress UI, related ViewModel state/callbacks, downloader-only workers/services/receivers and downloader-only dependencies/resources.
- Remove download-only Room entities/tables only if safe. Prefer a correct Room migration that preserves all user data and existing tables. Never wipe the database or downgrade schema unsafely. If old download rows are retained for migration compatibility, ensure no UI or worker can use them and document why.
- Keep **Import Audio** and SAF folder import. They are local-library functionality, not Downloader, and must remain easy to find.
- Search the entire project for `Downloader`, `DownloadEntity`, `VesperDownloadManager`, `DownloadEntitlements`, download routes, strings, resources, permissions and dependencies. Explain each remaining reference if it is intentionally retained for migration or unrelated Android playback.
- Ensure no dangling imports, callbacks, routes or tests remain.

## D. Side navbar: Donate

Add one compact **Donate** item in the existing side/drawer navigation:
- Small heart icon immediately next to the word “Donate”; align with existing nav icon/text spacing, typography, touch target and selected/hover styling.
- Keep it subtle and consistent with VESPER’s current theme. Do not add a large banner, modal, payment SDK or intrusive prompt.
- Tapping it must perform a real, safe action. Do not invent a donation URL, merchant, payment account or payment integration. If the project has no confirmed donation destination, add a single clearly named configurable constant/setting and leave it unset; tapping while unset should show a small honest “Donation link not configured yet” message. Document where the owner can insert their verified URL. Validate HTTPS and open externally with an Android chooser/browser safely.
- Do not collect payment details or user data.

## E. Alphabetical fast-scroll / swipe index

There is already `AlphabeticalFastScrollRail.kt` in this project. Inspect and reuse/fix it; do not create a duplicate rail.
- Make the A–Z/# index visible and usable on the relevant long alphabetical lists (at minimum Songs; add Artists only if its list is alphabetically grouped and the interaction makes sense).
- Support both tap-to-jump and press-drag/swipe continuously down/up the rail. Keep the rail reachable without covering row action buttons or the mini-player.
- Map each letter to the first matching item using the actual displayed sort order and locale-aware normalized title/artist. `#` maps to titles that do not begin with a letter. Handle empty sections and unavailable letters gracefully.
- Use the list’s `LazyListState` to scroll to the correct item; account for section headers and any leading content so the target row lands correctly.
- Keep the fast-scroll thumb/letter preview responsive and accessible; touch target should be practical on small phones. Do not make the rail microscopic merely to save space.
- If a normal scrollbar is also useful, add a slim, unobtrusive draggable scrollbar; do not confuse it with or remove the A–Z rail.
- Verify on a large library and small screen, with mini-player visible, during fast fling, and with empty/one-letter datasets. Add Compose tests for letter mapping and jump targets.

## F. Speed and resource optimization

Measure first; optimize measured bottlenecks:
- Cold start, warm start, Room cache render time, MediaStore scan time, SAF scan time, metadata parse time, Room write time, first playable track, list scroll frame drops and memory/bitmap usage.
- Move file I/O, metadata extraction, sorting/grouping and expensive transformations off the main thread. Use structured coroutines, cancellation and bounded work.
- Avoid rescanning/reparsing unchanged audio. Batch Room writes; avoid one transaction per file. Avoid repeated full-list mapping/sorting on every state emission.
- Keep album art decode sizes appropriate to UI; use stable Coil cache keys, bounded memory/disk caches and avoid decoding full-resolution embedded art for thumbnails.
- Use stable Compose keys/content types, `derivedStateOf` where useful, immutable UI models and lifecycle-aware collection. Avoid premature caching that causes stale UI.
- Avoid unnecessary startup work, duplicate repository scopes, repeated observers, unnecessary network requests and expensive initialization.
- Do not sacrifice audio quality, metadata correctness, accessibility, animations essential to the design, or existing user features for benchmark gains.

## G. Reduce install size to below 25 MB

Target **installed/downloadable release APK under 25 MB** if technically achievable without removing core functionality. Report exact artifact type and size; do not compare an APK to an AAB or confuse compressed download size with installed size.
- Measure baseline release APK and AAB before optimizing.
- Inspect dependency tree and APK Analyzer / bundle contents: native `.so` libraries, duplicate ABIs, unused dependencies, fonts, images, raw assets, debug symbols, bundled models and resource duplication.
- Remove downloader-only dependencies and unused resources after code removal.
- Enable release minification and resource shrinking safely (R8 + `shrinkResources`), add narrowly scoped keep rules only where required, and verify Room, Media3, Coil, Retrofit/serialization (if present) and reflection still work.
- Configure ABI splits or Android App Bundle delivery appropriately; preserve supported device coverage. Do not silently drop 32-bit or required ABIs without stating the trade-off.
- Optimize image/vector resources and remove unused assets; do not degrade the UI or remove app icons/essential artwork.
- Inspect native library sizes and transitive dependencies before replacing libraries. Avoid adding large libraries.
- Do not remove Media3, Room, SAF import, audio effects, or core screens just to hit the target.
- If 25 MB cannot be reached safely, report the measured size, biggest contributors and the smallest safe next steps. Never fake or estimate a final size.

## H. Regression and acceptance checklist

Before declaring complete, verify:
- [ ] Project compiles in debug and release.
- [ ] Unit tests and relevant Compose/instrumented tests pass (or clearly report unavailable device tests).
- [ ] MP3 import and metadata parsing work from MediaStore and SAF.
- [ ] Other supported formats are parsed or correctly identified as unsupported.
- [ ] Imported songs persist after restart and appear without waiting for a full scan.
- [ ] Duplicate scan does not duplicate tracks; failed scan does not erase cached library.
- [ ] Song tap plays; pause/resume/seek/next/previous/shuffle/repeat/queue work.
- [ ] Background playback, notification controls and audio focus remain functional.
- [ ] Downloader is absent from all user-facing navigation and no downloader code path remains active.
- [ ] Donate appears in the side navbar with a small heart and behaves honestly if no URL is configured.
- [ ] A–Z tap and swipe/drag fast-scroll jump to correct rows.
- [ ] Search, albums, artists, favourites, playlists, history, lyrics, EQ, sleep timer, appearance and settings still work.
- [ ] No Room data loss or destructive migration.
- [ ] Release artifact size is measured and reported accurately.

## Final response format required

Keep the final report to **exactly five concise lines**:
1. Audit/build/test result and any remaining unverified device checks.
2. Parser/import result (formats and source paths actually tested).
3. Downloader removal + Donate nav result.
4. A–Z swipe/scroll result + performance changes.
5. Measured release APK/AAB size and whether the under-25-MB target was achieved.

If any acceptance item fails, say **NOT COMPLETE** and name the blocker. Do not claim “all fixed” based only on code inspection. Do not stop after writing a plan: implement, build, test, and report.
