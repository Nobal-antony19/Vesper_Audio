# OLD VESPER — FINAL STABILITY, SEARCH, PLAYBACK, METADATA & DISCOVER FIX

## Mission
Treat the supplied `vesper-audio (1)(1).zip` as the current OLD VESPER baseline. The goal is to stabilise the complete app without removing its existing peak features. This is a targeted repair and polish pass, not a rewrite.

## Audit findings from this ZIP
The source already contains:
- `SonoraViewModel` local search flow, debounced by 200 ms and filtered on `Dispatchers.Default`.
- Remote Discover search with a separate 350 ms delay and cancellable `audiusSearchJob`.
- Provider-neutral `DiscoverProvider` / `DiscoverRepository`, Audius enabled, JioSaavn and BitChord adapters disabled pending authorised access.
- Discover Room cache with 30-minute TTL, stale fallback, single-flight requests, bounded concurrency and artwork URL blacklist.
- `AudioEffectController.applyPreset()` with several EQ curves and an `activePresetName` StateFlow.
- `SonoraViewModel.selectSoundProfile()` which changes `_activeSoundProfile`, applies band levels, but does not clearly synchronise the controller's active preset name with the selected SoundProfile.
- `MediaStoreScanner` fast header scan and artwork URI resolution helpers.
These are starting points, not proof that runtime behaviour is correct. Reproduce each bug before editing and verify the fix.

## 1. Critical: fix search and song switching

This is the highest-priority reliability issue. Search must never accidentally change the current song, queue, or playback source. Tapping a result must play exactly the selected result and subsequent Next/Previous must follow the intended result list.

### Required behaviour
- Search is read-only until the user explicitly taps a result or a play action.
- Typing, deleting, clearing, switching search tabs, opening Discover, scrolling results, refreshing results, and loading another page must never call `playTrack`, replace the queue, seek, or change the current media item.
- A tap on a local result plays that exact local track and uses the correct local result list as its queue.
- A tap on a remote Discover result plays that exact remote track and uses the correct remote result list as its queue.
- Local and remote IDs must never collide. Keep provider namespaced IDs.
- A local search result must not resolve to a different file just because title/artist/album match another track.
- Next/Previous must move within the queue from the selected index, not restart at the first item or jump to a similarly named track.
- When the user intentionally starts a new queue, select the clicked track's actual index and preserve the correct queue ordering.
- If a track disappears or its URI becomes inaccessible, show a recoverable playback error; do not silently substitute another song.
- Persist queue IDs and queue index consistently. Restore the same selected track and position after process recreation where the underlying item still exists.

### Investigate and repair
Inspect all call sites of:
- `setSearchQuery`, `searchResults`, `searchAudius`, `loadMoreAudius`
- `playTrack`, `playAlbum`, `playArtist`, `playNext`, `playPrevious`, `playQueue`
- `SonoraPlayerClient.playTrack`, `buildMediaItem`, `trackMap`, `onMediaItemTransition`
- all SearchScreen and Discover result click handlers
- all queue construction and queue restoration code

Look for:
- Search result composables calling playback during composition or from `LaunchedEffect`.
- Stale/debounced query results arriving after a newer query and replacing the visible list.
- A stale pagination response appending to a newer search.
- Wrong queue passed to a result click (defaulting to all local tracks instead of current search/Discover results).
- Duplicate IDs across MediaStore, SAF and remote providers.
- Track map entries being overwritten by the same ID.
- Queue index being derived incorrectly after filtering, sorting or deduplication.
- `MediaItem.mediaId` not matching the exact Track ID stored in `trackMap`.
- Search and Discover using the same query state when they should be independent.
- UI selection state accidentally triggering playback or route changes.

### Implementation requirements
- Separate local-library query state from remote Discover query state.
- Use `flatMapLatest`/cancellable jobs for query changes. Apply results only if the request's query and request generation still match the current query.
- Cancel old remote search and pagination requests when query/provider/tab changes.
- Debounce remote search once in one layer only; avoid stacked delays.
- Keep search filtering off the main thread.
- Add stable IDs and explicit source/type for local, SAF and remote tracks. Use namespaced IDs, e.g. `mediastore:<volume>:<id>`, `saf:<tree/document-id>`, `audius:<id>`.
- Do not use title+artist as the primary playback identity.
- On result click, construct the queue from the exact currently displayed result list and pass the selected track's exact index/ID.
- Add an explicit playback intent boundary: only click handlers and transport controls can issue playback commands.
- Do not mutate the player queue from search state collectors.

### Regression tests
- Type a query while music is playing: current track, queue, position and play state remain unchanged.
- Clear query: playback remains unchanged.
- Change query rapidly: only latest query results appear.
- Load more after changing query: old page is discarded.
- Tap result #3: result #3 starts, and Next starts result #4.
- Tap a remote result with same title as a local song: the remote URI plays and local queue is untouched.
- Tap a local result with same title as a remote result: the local URI plays.
- Next/Previous wraps correctly; shuffle/repeat still work.
- Restore queue after app restart and verify track identity/index.
- Test duplicate IDs across MediaStore and SAF.

## 2. Tones navigation placeholder
- Add a real `TONES` full-screen destination and route.
- Tapping Tones opens a clean blank/placeholder section in VESPER style; it must not open Equalizer or crash.
- Add a back arrow; toolbar back and Android system back return to the previous screen.
- Do not fabricate audio processing or show non-functional controls.
- Add a subtle “Tones workspace is coming soon” empty state only if consistent with the current design.
- Keep Tones distinct from the six basic EQ Modes. Tones will later contain the advanced spatial/studio processing experiences.

## 3. EQ Modes: exactly six useful basic modes
The user wants six simple Modes that cooperate with the Equalizer. Keep advanced studio-style sound architectures for Tones.

### Six basic modes
1. **Balanced** — neutral baseline.
2. **Bass** — controlled low-frequency lift.
3. **Vocal** — intelligible, forward midrange.
4. **Rock** — energetic low/high presence without harsh clipping.
5. **Pop** — modest bass and treble lift with clear mids.
6. **Acoustic** — natural presence and gentle warmth.

Remove/hide extra SoundProfile entries from the basic Modes UI, but do not delete custom EQ presets or advanced functionality unless separately approved. Preserve custom EQ functionality in the Equalizer area.

### Required interaction
- Exactly one basic mode is active at a time.
- Selected mode stays visibly highlighted when navigating away and returning.
- Tapping the currently selected mode again toggles it off to a neutral/flat state (Balanced may be the neutral selected state if the UX defines it; choose one consistent behaviour and document it).
- Selecting a different mode immediately replaces the previous curve; it must not stack gains.
- The selected curve must actually be applied to the active playback audio session, not merely update UI state.
- The mode remains active across track changes and queue transitions until the user changes or disables it.
- EQ enabled state, selected mode, active preset, band values and bass boost must stay synchronised.
- Persist the active mode and restore it on app restart.
- Manual band edits switch the active mode to `Custom` or an equivalent explicit custom state; do not continue highlighting a preset whose curve no longer matches.
- Reset/disable restores a known neutral state and updates the UI immediately.
- Respect device effect capability; handle unsupported Equalizer/BassBoost safely without crashing or falsely claiming processing is active.
- Apply headroom protection to prevent clipping. Do not overdrive gains.

### Important source-level issue to resolve
`selectSoundProfile()` currently updates `_activeSoundProfile` and writes individual band levels, while `AudioEffectController` separately owns `activePresetName`. Synchronise these states through one source of truth. Avoid a UI that highlights a mode while the controller reports a different preset. Prefer one controller method such as `applyBasicMode(mode)` that atomically applies the full curve, bass boost, EQ enabled state, active preset name and observable band state. Keep persistence in one path.

### Tests
- Every one of the six modes updates band values and active mode name.
- Selecting a second mode replaces the first curve.
- Re-tapping selected mode returns to flat/disabled state according to the chosen UX.
- Mode highlight survives screen navigation and process restart.
- Mode remains active across next/previous track transitions.
- Manual band edit marks Custom.
- Unsupported hardware reports accurate status and does not crash.

## 4. Metadata and album-art repair

### Requirements
- Album covers must load consistently in Home, Songs, Albums, Artists, Search, Discover, queue, mini-player and Now Playing.
- Prefer MediaStore album-art/content URIs for local files, with a safe fallback to embedded metadata extraction only when required.
- Verify `VesperArtworkResolver` URI construction and permissions on supported Android versions. A syntactically valid URI is not proof that Coil can read it.
- Do not treat a missing/invalid URI as a successfully loaded image.
- For SAF files, use persisted tree permissions and document URIs; do not assume MediaStore album IDs apply to SAF.
- Keep artwork loading off the main thread and use Coil memory/disk caching with size-aware requests.
- Avoid loading full-size artwork for small rows. Use thumbnail sizing and allow larger requests only on Now Playing/detail screens.
- Stable cache keys must include source identity and metadata version (e.g. MediaStore ID + dateModified + size; SAF document ID + lastModified/size). Do not use only album title.
- Invalidate artwork when the underlying file's modified time/size or embedded metadata changes.
- Avoid repeatedly retrying broken artwork URLs/URIs. Add bounded retry/backoff and neutral VESPER placeholder.
- Ensure album grouping selects a usable artwork URI from any track in the album.
- Keep user-edited metadata precedence intact; do not overwrite edits during rescans.
- Fix remote Discover artwork separately from local file artwork; do not pass remote HTTP URLs through local MediaStore resolvers.
- Add diagnostics that distinguish permission denied, missing file, HTTP failure, decode failure and null artwork.

### Tests
- Local MediaStore track with album art.
- Local track with embedded art but no album art URI.
- SAF track with persisted tree permission.
- Same album with some tracks missing artwork.
- Metadata edit and rescan.
- Artwork cache invalidation after file replacement.
- Remote provider artwork and offline cache.
- Large library scroll with no repeated image fetch storm.

## 5. Discover: faster and visually coherent

The current repository already has a 30-minute Room cache, stale fallback, single-flight request deduplication and bounded concurrency. Preserve and verify these features; do not add a second competing cache.

### UI polish
- Make Discover match the rest of VESPER: black background, burgundy/wine accent, warm-white headings, muted secondary text and restrained outlines.
- Keep the Discover title and subtitle compact. Search field, Songs/Artists/Albums tabs and result cards must share consistent horizontal alignment and spacing.
- Avoid oversized pills, uneven card heights, clipped album names and excessive empty space.
- Use one consistent artwork size/aspect ratio and text baseline for each result type.
- Keep provider labels small and subtle.
- Make album/artist cards visually distinct but consistent with track rows.
- Use stable keys and content types in lazy lists.
- Keep mini-player and bottom navigation clear of list content; apply correct insets and bottom content padding.
- Show cached results immediately; show a small refresh indicator rather than blocking the full screen.
- Add polished loading skeletons, empty results, offline/stale-cache and retry states.
- Ensure refresh does not clear visible cached content.

### Performance
- Keep provider calls on IO dispatcher; JSON parsing and heavy mapping off main.
- Cancel obsolete queries and prevent stale responses from overwriting newer state.
- Avoid multiple independent debounce delays.
- Keep page sizes bounded and paginate on demand.
- Deduplicate by namespaced provider ID, not title.
- Do not repeatedly regroup albums/artists on every recomposition.
- Use Coil memory/disk cache with URL normalisation and size-aware image requests.
- Avoid loading artwork for off-screen items.
- Keep playback progress state from recomposing the entire Discover list.
- Measure cache hit rate, time to first visible results, artwork load latency, jank and memory.
- Maintain stale-while-revalidate behaviour; never replace a good cache with an empty error response.

### Provider restrictions
- Audius remains the working public provider if its API and playback permissions remain valid.
- JioSaavn and BitChord remain disabled unless an official/documented API and explicit streaming/catalogue rights are available.
- Do not bypass auth, DRM, signatures, rate limits or private endpoints.
- Never show a disabled provider as if it were working.
- No fake catalogue entries or demo tracks in production.

## 6. Preserve OLD VESPER's existing peak features
Do not remove or regress:
- Local MediaStore and SAF library scanning.
- Existing player, queue, shuffle, repeat, sleep timer and playback restoration.
- Albums, artists, playlists, favorites and folders.
- Metadata Studio, single/batch metadata edits and artwork handling.
- Equalizer, custom EQ presets, audio balance, preamp/headroom and existing audio-effect capability checks.
- Settings, scan-media, drawer, navigation and mini-player.
- Existing database content and user preferences.

## 7. Build, QA and delivery
1. Extract the supplied ZIP and confirm the project root and Gradle configuration.
2. Back up the entire project before changing files.
3. Establish a baseline build and run existing tests.
4. Reproduce search-to-playback switching, mode highlight and artwork failures before patching.
5. Implement in this order: playback/search identity and queue correctness; six-mode state synchronisation; Tones placeholder route; metadata/artwork; Discover performance/UI.
6. Add unit/UI tests for every regression listed above.
7. Run Gradle build and tests using the project's wrapper. If the environment cannot build, state the exact blocker; do not claim success.
8. Install/run on a real device or emulator if available. Confirm playback source switching, queue transitions, artwork and mode persistence.
9. Report changed files, tests/build results, remaining limitations and measured performance. Do not claim unverified runtime fixes.

## Execution prompt
Fix OLD VESPER from the supplied ZIP. Back up first. Prioritise the critical bug where searching or switching result sources can change the wrong song/queue: make search read-only and ensure tapping a result plays that exact track with the correct queue and Next/Previous order. Add a blank Tones subpage with back navigation. Reduce basic EQ Modes to Balanced, Bass, Vocal, Rock, Pop and Acoustic, and make the selected mode genuinely apply, stay highlighted and persist until toggled off or changed. Repair local/SAF/remote metadata and album-art loading. Polish Discover's spacing, artwork, caching and responsiveness. Preserve all existing features and data. Build/test and report only verified results.
