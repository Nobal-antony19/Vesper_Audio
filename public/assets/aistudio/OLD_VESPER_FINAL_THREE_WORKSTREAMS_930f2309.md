# OLD VESPER — FINAL THREE WORKSTREAMS ONLY

Use the latest supplied OLD VESPER project ZIP as the implementation baseline. The two supplied screenshots and two videos are the visual and behavioural references. This is the final scoped task: implement only (1) Equalizer + Modes, (2) swipe navigation, and (3) the complete Settings experience + Search & Play flow. Do not expand scope.

## Non-negotiable project rules
- Back up the full project before edits.
- Preserve VESPER's existing black / burgundy / warm-text identity and its boxy grouped-card menu style.
- Preserve all existing local music, playback, queue, metadata, playlist, favourite and navigation features.
- Reuse existing DataStore/Room/preferences and audio controller. Do not create parallel state stores.
- Every visible setting/control must work. Hide controls that cannot be implemented; never ship fake switches or fake audio processing.
- Build and test. Report only verified results.

# 1. Equalizer and EQ Modes — make listening better

### Goal
Make the Equalizer reliable and musically useful. Modes are the six simple EQ profiles; advanced studio/spatial architectures belong in Tones, which is outside this task.

### Six basic modes only
1. Balanced — neutral reference.
2. Bass — controlled low-frequency lift without muddying vocals.
3. Vocal — clearer forward midrange and intelligibility.
4. Rock — energetic low/high presence without harsh treble.
5. Pop — modest low/high lift with balanced mids.
6. Acoustic — natural warmth and presence.

Keep custom EQ presets and manual band adjustment available in the Equalizer. Do not expose more than these six basic modes in the basic Modes selector.

### Behaviour
- Exactly one basic mode is active at a time, or none when EQ is explicitly disabled.
- The active mode is visibly highlighted whenever the Modes screen is opened.
- Selecting a different mode replaces the previous curve; never stack gains.
- Tapping the currently active mode again disables that mode and returns to the defined neutral state.
- The selected curve must be applied to the actual active playback audio session, not just reflected in UI state.
- The mode remains active when the user changes songs, moves through the queue, or leaves/reopens the Equalizer.
- Persist the selected mode and EQ enabled state; restore them on app restart.
- Manual band edits change the selection to `Custom` (or an equally clear custom state); do not leave a preset highlighted when its curve is no longer active.
- Reset returns bands, preamp and mode state to a known neutral configuration.
- Keep EQ bands, preamp, bass boost, controller active preset, UI highlight and persisted preferences synchronised through one source of truth.
- Apply safe headroom/preamp attenuation for positive boosts to reduce clipping. Do not simply maximise every band.
- Respect Android audio-effect/session limitations. If an effect is unavailable, show an honest unavailable state and do not claim the mode is applied.

### Technical audit
Inspect `AudioEffectController`, `SonoraViewModel`, Equalizer and Modes screens, audio session creation/release, and track transition callbacks. Ensure effects attach to the current playback session and are not recreated/leaked on every track change. Ensure a controller state update cannot race with a user mode selection.

### Tests
Test all six curves, switching between modes, toggling the active mode off, manual edit → Custom, reset, navigation persistence, app restart persistence, track changes, queue transitions, and unavailable-effect handling.

## Main navigation update — replace Artists with Playlists

This is a required part of the final scope, not an optional follow-up.

- Replace the **Artists** main bottom-navigation tab and its top-level page with **Playlists**.
- Final main navigation order: **Home → Songs → Albums → Playlists → Discover**.
- The Playlists tab must open the existing playlist library, not a placeholder.
- Show the user's existing playlists with artwork/collage, title and track count where available.
- Tapping a playlist opens its detail page and complete tracklist.
- Support the existing playlist actions: play, shuffle, add/remove tracks, rename, delete and create playlist, wherever those capabilities already exist. Wire each visible action to real behaviour.
- Preserve all playlist data and IDs. Do not reset or migrate destructively.
- Keep artist metadata and artist detail support internally and in Search/Discover where relevant; only remove Artists as a main navigation destination.
- Update swipe order, pager state, bottom-nav selection, deep links and back navigation to use Playlists in the fourth position.
- Test that swiping from Albums reaches Playlists and swiping from Playlists reaches Discover, and the reverse directions work.
- Remove stale Artists tab labels/icons from the main navigation, but do not delete artist-related models, repositories or metadata.

# 2. Swipe navigation — move between adjacent sections

### Goal
Allow a horizontal swipe from the centre or anywhere on the main content area to move to the adjacent section, in the direction of the swipe. Apply this to Songs too.

### Behaviour
- Swipe left → next section to the right.
- Swipe right → previous section to the left.
- Use the app's actual section order and wrap/clamp behaviour consistently; choose one and apply it everywhere.
- Enable this on all main top-level sections: Home, Songs, Albums, Playlists and Discover.
- Songs must support the same gesture; it must not be excluded.
- Do not trigger a page change for vertical scrolling, artwork taps, track taps, sliders, text fields, dialogs, bottom sheets, or other horizontal controls.
- A deliberate horizontal gesture should win over vertical scrolling only after a clear touch slop/angle threshold.
- Keep bottom navigation taps and swipe navigation synchronised: selecting a tab updates the current page, and swiping updates the selected bottom-nav item.
- Preserve each section's scroll position and search/filter state when leaving and returning.
- Do not recreate or restart playback when changing sections.
- Avoid nested gesture conflicts with horizontal carousels and seek bars.

### Implementation
Prefer a single shared pager/navigation state for the top-level sections instead of independent gesture detectors on every screen. Use Compose nested-scroll/gesture handling carefully. Keep stable page keys and preserve state. If the app already uses a pager, fix and extend it rather than introducing a second pager.

### Tests
- Swipe left/right from the centre of each section.
- Confirm Songs participates.
- Confirm vertical scroll does not switch pages.
- Confirm tapping a track while swiping is not accidentally triggered.
- Confirm bottom-nav and pager stay in sync.
- Confirm queue, current song, playback position, and each page's scroll/search state remain unchanged.

# 3. Settings — VESPER block menu with deeper, working sections

### Visual direction
Use the supplied Settings video as the functional/content reference and the supplied screenshots as the design reference:
- Retain VESPER's grouped, boxy block/card menu.
- Black AMOLED background, burgundy accent, warm-white primary text and muted secondary text.
- Clear section headings with a small burgundy marker.
- Each setting row has a simple icon, title, concise explanatory text and a trailing control/chevron.
- Separate settings into dedicated subpages with a clear back arrow and consistent system-back behaviour.
- Do not flatten every setting into one endless page.
- Keep the mini-player and bottom navigation visible where the current app uses them; content must respect insets.
- Provide a settings search field only if it filters real settings.

### Settings landing page and subpages
Use the following groups and preserve the reference's overall breadth. Include only controls that can be wired to real app behaviour.

#### A. Account & integrations
- Account status / sign-in state if account functionality exists.
- Listen together only if the feature is implemented; otherwise omit it.
- Never imply a login/account is required for local playback.

#### B. Audio quality → Sources
- Direct Source Stream toggle only if the playback pipeline supports it.
- Wi-Fi streaming quality selector.
- Mobile-data streaming quality selector.
- Dolby Atmos / immersive source preference only when the device, content and playback path genuinely support it. Never fake Atmos.
- Clearly indicate the currently active source/quality where known.

#### C. Downloads
- Download location.
- Download quality.
- Wi-Fi-only download.
- Storage and download management.
- Expose only real download capabilities and permitted provider content.

#### D. Playback
- Bit-perfect output only if genuinely supported by the Android output path.
- Loudness normalisation if implemented.
- Disable effects on phone speaker if implemented.
- Crossfade duration and on/off.
- Automix only if fully implemented; label beta honestly if appropriate.
- Automix performance/quality only if it changes real behaviour.
- Skip silence only if implemented.
- Spatial audio only if a real processing path exists.
- Link to Equalizer.
- Do not show dead controls.

#### E. Content Preferences
Include working controls matching the reference where the app has the corresponding content:
- Music-only preference.
- Hide recents.
- Hide videos from Home.
- Hide topic picks.
- Hide Find Your Mood.
- Square video thumbnails.
These must actually affect the relevant surfaces. Omit unsupported content features rather than displaying inert switches.

#### F. Appearance
- System / Light / Dark theme.
- Font selection if supported.
- Reduce animations.
- Reduce dynamic blur.
- Disable blend effects.
- Liquid Glass effect only if actually implemented; otherwise omit.
- Full-screen cover art.
- Full-screen album/playlist cover behaviour.
- Player theme selector (e.g. Default / Legacy / Simple) only if each style exists.
- Animated cover art and cellular playback preference if animated artwork is supported.
- Spotify Canvas only if an authorised implementation exists; do not scrape or spoof.
- Artist signatures only if available.
- Synced lyrics.
- Blur unfocused lyrics.
- Lyrics sources and translation language only when supported and compliant.
- Every appearance option must visibly change the UI or playback surface it describes.

#### G. Performance
- Reduce animations / visual effects if not already in Appearance; avoid duplicate settings.
- Artwork/cache size and clear action.
- Rebuild library cache and artwork.
- Scan/cache behaviour only if implemented.
- Display real cache sizes and progress; do not use hardcoded sizes.
- Cache clear must not delete user music, playlists, favourites or metadata edits.

#### H. Local Music / Library & Scanning
- Manage Music Folders (SAF), including persisted URI permissions.
- Rebuild Library Cache & Artwork: re-scan local audio tags and rebuild thumbnail cache, with progress and completion/error feedback.
- Filter Short Audio (<30s): ignore ringtones, notification chimes and short voice clips. Unknown-duration files should not be silently discarded.
- Auto-Scan on Startup: incremental local library sync when the app opens.
- Load Atmospheric Demos: only verified, explicitly user-requested demo tracks; never silently add demo content to the user's library.
- Preserve metadata edits and favourites during rescans.

#### I. Storage
- Show measurable app cache/library storage.
- Safe cleanup of app-generated cache only.
- Never delete user audio or offline files without explicit confirmation.

#### J. Your Data / Feature Zero
- Explain offline-first and on-device data handling truthfully.
- Do not make absolute “zero tracking” guarantees unless code, dependencies and network behaviour have been audited to support them.
- Artwork Cache: show actual size and provide a working clear action.
- Support VESPER entry only if the link/action exists.
- Any export/import/reset action must be implemented and guarded with confirmation.

#### K. Miscellaneous and Language
- Include real miscellaneous controls and language selection only if implemented.
- Do not leave empty categories or non-functional placeholders in the final Settings menu.

#### L. About VESPER
- App name, actual installed version/build, edition label if used, credits and creator information.
- Values must come from the project/build metadata, not hardcoded stale version text.

### Settings engineering
- Map each control to the existing preference/controller before implementing it.
- Use one source of truth for each setting.
- Persist settings using the existing DataStore/Room implementation.
- Apply changes immediately where safe and show confirmation/progress for operations.
- Destructive actions require confirmation.
- Add stable test tags and tests for every setting row/control.
- Preserve existing saved values and defaults; do not reset user preferences during migration.

# 4. Search & Play — match the supplied video

### Target experience
Recreate the demonstrated workflow:
1. User types a query such as `Teenage Dream`.
2. Search suggestions appear while typing, including relevant song, artist and album suggestions.
3. Search results are grouped/mixed and identify result type clearly.
4. The album result for `Teenage Dream` opens an album detail page with artwork, artist, year and complete tracklist.
5. User can play the album, play a specific track, or add tracks to the queue.
6. User can browse the album's full tracklist and move through it without being restricted to the first visible results.
7. Tapping an artist opens the correct artist page and its available catalogue.
8. Playback mini-player and Now Playing remain synchronised.

The example names in the video are behaviour references, not a request to hardcode or seed Katy Perry content.

### Search suggestions and results
- Search local library and enabled/authorised remote catalogue sources without mixing their identities. Keep artist results available in Search even though Artists is no longer a main navigation tab.
- Show suggestions for tracks, artists and albums as the query is typed.
- Debounce once; cancel obsolete requests. The latest query alone may update the screen.
- Support partial matching, case-insensitive matching and sensible token matching.
- Group or label result types clearly: Songs, Artists, Albums.
- Search results must not change playback merely because the query changes or a suggestion appears.
- Tapping a suggestion may open the relevant result/detail page; only an explicit Play action starts playback.
- Keep remote and local IDs namespaced. Never resolve a result by title+artist when a stable source ID or URI is available.

### Album detail
- Show album artwork, album title, primary artist, release year/date and track count/duration when known.
- Show the complete available tracklist with stable ordering and track numbers.
- Provide Play and Shuffle actions.
- Tapping a track plays that exact track and creates/uses the album queue with the correct selected index.
- Next/Previous traverse the album tracklist in order.
- Queue insertion must not replace the current queue unless the user explicitly chooses Play/Replace Queue.
- If the provider returns paginated album tracks, fetch all pages needed for the album detail, with progress and deduplication. Do not show an incomplete album as complete.
- If the catalogue does not expose a complete tracklist, clearly show the available count/state rather than inventing missing songs.

### Artist detail
- Show artist identity/artwork where available.
- Display available albums, singles and tracks from the enabled source.
- Pagination and loading states must be clear.
- Clicking an album opens the remote album detail route, not a local album route.
- Clicking a track plays the exact selected source item.

### Queue and playback identity — critical
- Search is read-only until an explicit user playback action.
- A selected result must play exactly that result; no title-based substitution.
- Queue IDs and `MediaItem.mediaId` must match the exact source track IDs.
- Tapping track N in an album starts track N and Next starts N+1.
- Remote tracks must not collide with local MediaStore/SAF tracks.
- Next/Previous, shuffle, repeat and queue restore must retain correct source identity.
- Changing tabs, queries, suggestions, result lists or album pages must not reset the playing song.
- If a remote stream URL expires, refresh it through the provider's authorised resolver; do not silently substitute another track.
- If a source is unavailable, show a recoverable error.

### Catalogue/provider permissions
- Use only currently authorised provider interfaces and streaming/catalogue rights.
- Do not bypass authentication, DRM, signatures, private endpoints, rate limits or provider access controls.
- A public/unofficial API wrapper does not establish rights to stream or redistribute catalogue content.
- Do not hardcode Katy Perry or any other artist/album as sample results.
- If a provider does not permit full album/artist catalogue access, expose only what it authorises and clearly communicate the limitation.

### Performance
- Cache normalized metadata and album/artist detail with bounded TTL and stale-while-revalidate.
- Cache artwork using Coil disk/memory cache with size-aware requests.
- Render cached results immediately while refreshing in the background.
- Deduplicate concurrent requests and paginated items by provider ID.
- Keep parsing and heavy mapping off main.
- Stable LazyColumn/LazyGrid keys; no full-list recomputation on playback progress updates.
- Never replace useful cached results with an empty error response.
- Measure search-to-first-result time, album detail load time, artwork cache hit rate and scrolling jank.

# 5. Implementation order and acceptance
1. Back up project and run baseline build/tests.
2. Repair Equalizer/Modes state and actual audio effect application.
3. Add shared swipe navigation across all main sections, including Songs.
4. Rebuild Settings as the VESPER block menu with dedicated working subpages and the reference's functional breadth.
5. Implement Search suggestions, mixed result types, album detail/full tracklist, artist detail and exact queue behaviour.
6. Add regression tests for all three workstreams.
7. Run build and tests; install/run if possible.
8. Report changed files, exact build/test output, device/emulator conditions and any unsupported provider features.

## Final coding-agent prompt
Implement ONLY these three final OLD VESPER workstreams from this specification: (1) fix the Equalizer and six basic EQ Modes so curves really affect playback and selection persists; (2) add reliable left/right swipe navigation across all main sections including Songs, synchronised with bottom navigation; (3) build the complete VESPER block-style Settings with deeper working subpages matching the supplied settings video, and recreate the Search & Play workflow with suggestions, mixed song/artist/album results, full album tracklists, artist pages and correct queue playback. Back up first, preserve all existing features/data, use only authorised catalogue APIs, and build/test before reporting. Do not implement unrelated features.
