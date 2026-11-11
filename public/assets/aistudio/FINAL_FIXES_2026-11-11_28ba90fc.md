# VESPER AUDIO — Final Fixes
**Date:** 2026-11-11  
**Scope:** Recently Added playback, responsive layouts, gapless playback, and metadata reliability.

## 1. Recently Added playback and queue ordering

### Problems
- Tracks in Recently Added can fail to start playback.
- Pressing Next can jump to alphabetical order instead of the next recently added track.

### Required fixes
- Treat the Recently Added screen's displayed ordering as the authoritative playback order.
- When a track is selected, build the playback queue from the current Recently Added list, preserving its exact sort direction and order.
- Set the queue's current index to the selected track.
- Ensure Next and Previous navigate within that same queue, not the global library or an alphabetically sorted list.
- Keep the queue stable while playback is active. Refresh it only when the user explicitly changes the queue or starts playback from another collection.
- Handle missing, unavailable, or deleted media gracefully without leaving the player in a broken state.
- Apply the same queue contract to other collection screens where appropriate: Songs, Albums, Playlists, Favorites, and Discover.

### Acceptance checks
- Start playback from the first, middle, and last Recently Added track.
- Next and Previous follow the visible Recently Added order.
- Verify ordering with tracks whose titles sort differently from their added dates.
- Confirm playback still works after navigating away from Recently Added.

## 2. Landscape scrolling and screen measurement

### Problems
- Songs and Discover cannot reliably scroll in landscape orientation.
- Some screens clip content or become inaccessible when vertical space is limited.

### Required fixes
- Audit `LazyColumn`, `LazyRow`, `Column`, nested scroll containers, and parent layout constraints.
- Give scrollable content a bounded, usable height using the appropriate Compose layout modifiers.
- Avoid placing a vertically scrolling child inside an unbounded or competing vertical scroll container.
- Ensure bottom navigation, mini-player, and system insets do not cover the final list items.
- Preserve sensible touch targets and avoid forcing portrait-only dimensions.
- Check Songs, Discover, Home, Albums, Playlists, Search, Settings, and detail screens in both orientations.
- Use adaptive arrangements where landscape width allows a better layout, without making content too small.

### Acceptance checks
- Rotate to landscape on Songs and scroll from the first item to the final item.
- Repeat on Discover and Settings.
- Confirm the last item and bottom actions remain reachable.
- Rotate repeatedly between portrait and landscape without crashes, lost state, or clipped content.

## 3. Playback and loading continuity — local and Discover

### Problems
- A small audible or perceived gap occurs during playback/loading.
- The issue is present for both local music and Discover playback.

### Required fixes
- Trace the complete playback path for local and remote/Discover tracks.
- Configure Media3/ExoPlayer for gapless transitions where the source and format support it.
- Avoid unnecessary player recreation, release/reinitialization, or UI-driven delays between tracks.
- Pre-prepare the next item when appropriate and preserve the existing playback session.
- Handle remote buffering, source resolution, and loading states without inserting avoidable pauses.
- Keep loading indicators accurate; do not report a track as ready before the player is ready.
- Do not claim that network-stream transitions can be perfectly gapless when buffering or server-side encoding prevents it.
- Respect audio focus, interruptions, and the user's playback settings.

### Acceptance checks
- Test consecutive local tracks from the same album.
- Test local tracks with different formats and sample rates.
- Test consecutive Discover tracks on stable Wi-Fi and mobile data.
- Confirm no unnecessary player restart occurs between queue items.
- Confirm buffering is communicated clearly and playback recovers from temporary network loss.

## 4. Responsive UI across phones and orientations

### Problems
- Several screens do not fit consistently across devices.
- Settings has awkward sizing/spacing.
- Fixed dimensions and assumptions cause layout breakages.

### Required fixes
- Audit hard-coded widths, heights, offsets, paddings, and spacer values throughout the UI.
- Prefer responsive constraints, `weight`, `fillMaxWidth`, `fillMaxSize`, `wrapContentHeight`, window size classes, and adaptive layouts where suitable.
- Account for status/navigation bars, display cutouts, gesture navigation, and IME insets.
- Ensure dialogs, sheets, menus, cards, sliders, and settings rows fit smaller screens.
- Allow long labels and values to wrap or ellipsize appropriately instead of overflowing.
- Make Settings visually consistent: aligned section headings, readable row spacing, consistent control alignment, and no clipped controls.
- Preserve the VESPER visual identity, typography, colors, and interaction patterns.
- Avoid device-specific hacks or arbitrary minimum dimensions that break other phones.

### Acceptance checks
- Test compact and larger phone widths.
- Test portrait and landscape.
- Test system font/display scaling where practical.
- Confirm Settings, playback controls, dialogs, and Discover remain usable without horizontal overflow or clipped controls.

## 5. Metadata completeness and recovery

### Problems
- Metadata is still incomplete for some local tracks.
- Previously cached incomplete metadata may remain incomplete indefinitely.

### Required fixes
- Audit MediaStore scanning, embedded-tag parsing, metadata caching, and remote enrichment.
- Extract available fields reliably, including:
  - Title
  - Artist
  - Album
  - Album artist
  - Genre
  - Release year/date
  - Track number
  - Disc number
  - Duration
  - Embedded artwork
- Use MediaStore as the primary local source where appropriate and embedded metadata as a complementary source.
- Add safe fallbacks for missing or malformed tags; do not replace valid metadata with blank or lower-quality values.
- Revisit incomplete cached records during rescans or a controlled enrichment pass.
- Ensure metadata enrichment does not overwrite user-edited metadata.
- Avoid duplicate tracks and avoid repeatedly downloading or parsing metadata that is already complete.
- Handle missing, corrupt, oversized, or unsupported artwork safely.
- Keep artwork loading asynchronous and cached; avoid blocking the main thread.
- Where remote metadata providers are used, handle rate limits, timeouts, no-match responses, and conflicting results gracefully.
- Do not invent metadata. Clearly retain unknown values when no trustworthy source exists.

### Acceptance checks
- Test files with complete tags, partial tags, missing tags, and malformed tags.
- Test MP3, FLAC, M4A/AAC, OGG, and other supported formats.
- Confirm embedded artwork loads when present.
- Rescan a library with incomplete cached records and verify they are reconsidered.
- Confirm manually edited metadata survives scans and enrichment.
- Confirm scanning does not create duplicate tracks or freeze the UI.

## 6. Regression and quality requirements

- Preserve existing VESPER AUDIO features, navigation, design language, and user data.
- Keep local playback functional when Discover/network services are unavailable.
- Do not introduce new dependencies unless necessary; justify any addition.
- Keep work scoped to root-cause fixes rather than superficial visual patches.
- Add or update tests for queue ordering, metadata merging, and relevant playback behavior where practical.
- Run the available Gradle build and tests. If the environment lacks the Android SDK or build tooling, state that clearly and provide the exact validation that was completed.
- Inspect logs for crashes, coroutine errors, lifecycle issues, and Media3 state errors.
- Do not leave TODOs, placeholder implementations, or claims of tests that were not run.

## 7. Completion report

When finished, provide:
1. Root causes identified.
2. Files changed and why.
3. Fixes implemented for each section above.
4. Build and test commands run, with actual results.
5. Remaining limitations or issues requiring a physical-device test.

**Definition of done:** All five reported problem areas are addressed at their root causes, the app builds successfully (or build limitations are explicitly documented), and the acceptance checks are completed or clearly marked as pending.
