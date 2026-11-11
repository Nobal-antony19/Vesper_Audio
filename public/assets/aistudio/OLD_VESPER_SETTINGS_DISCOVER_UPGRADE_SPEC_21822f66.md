# OLD VESPER — Settings Refresh + Discover Upgrade

## Target
Work on the supplied `vesper-audio (7).zip` project. Keep this work scoped to OLD VESPER. Preserve its existing playback, local library, Room database, preferences, navigation, and Nocturne visual identity.

## 1. Settings redesign

Use the supplied BitChord settings screenshots as a layout reference only. Do not copy their branding, exact colors, or visual assets.

### Visual direction
- Keep VESPER's AMOLED black background, burgundy/wine accent, warm-white primary text, muted secondary text, and subtle dividers.
- Use a simple, boxy layout: grouped rectangular cards with consistent corner radius and restrained borders.
- Avoid excessive gradients, glow, decorative icons, nested cards, and oversized descriptions.
- Settings landing page: one large `Settings` heading, then compact category rows.
- Each row: small clear icon, readable title, one short description only when useful, and a subtle trailing chevron.
- Tapping a category opens a dedicated subpage. Every subpage has a clear back arrow and heading. Android system back and toolbar back must behave identically.
- Keep the existing mini-player and bottom navigation behaviour intact where currently present. Ensure content is not obscured by either.

### Settings landing page categories
Use these categories and order:
1. **Audio quality** — Sources / streaming source preference, Wi-Fi quality, mobile-data quality, spatial-audio availability/status.
2. **Downloads** — download location, quality, Wi-Fi-only, storage behaviour (only expose controls that actually exist or are implemented).
3. **Playback** — gapless playback, crossfade, headphone disconnect, resume behaviour, sleep timer.
4. **Content preferences** — filters and content-related preferences already supported by the app.
5. **Appearance** — theme/accent and appearance controls already supported by VESPER.
6. **Performance** — artwork cache controls, cache size/clear action, library cache rebuild, scan behaviour and other real performance settings.
7. **Local music** — media permissions, scanned folders, scan/rescan and short-track filtering.
8. **Storage** — app/library/cache storage information and safe cleanup actions, if measurable.
9. **Your data** — backup/export/import/reset actions only if genuinely implemented; destructive actions require confirmation.
10. **Miscellaneous** — remaining real, low-frequency settings.
11. **Language** — only if language selection is actually supported.

Do not invent settings or display dead switches. If an item is not implemented, either omit it or show a clearly labelled disabled/coming-later row only if the product owner explicitly wants that.

### Behaviour and implementation
- Replace the current long, dense Settings form with a landing page plus dedicated category subpages.
- Reuse existing preference callbacks and DataStore/Room state. Do not create duplicate preference stores.
- Preserve current defaults and existing saved values. No destructive migrations.
- Keep the page scrollable and responsive on small screens.
- Use stable test tags for category rows and controls.
- Add UI tests for opening each category, changing a setting, navigating back, and confirming the setting remains changed after reopening.
- Keep settings state lifecycle-aware; avoid local UI state that silently resets persisted settings.

## 2. Discover: provider integration

### Current project baseline
The supplied project already contains:
- `data/remote/AudiusApiClient.kt`
- Audius track parsing and trending/search requests.
- A Discover screen with Songs, Artists, and Albums tabs.
- Remote Audius tracks mapped into the existing Track model.

Audit the existing implementation before changing it. Do not replace working code blindly.

### Provider policy
- Audius can remain as an existing provider, subject to its current API requirements.
- Integrate JioSaavn or BitChord only through documented, public, authorised interfaces or with explicit permission from the provider.
- Do not reverse-engineer private endpoints for production use, spoof clients, extract private keys/tokens, bypass authentication, defeat DRM, bypass signed URL expiry, or circumvent rate limits/access controls.
- A public GitHub client or unofficial endpoint is not proof of permission to redistribute or stream catalogue content.
- If an integration is not authorised or stable, keep it behind a provider adapter that is disabled by default and document the limitation. Do not present it as a working source.
- Do not proxy, rehost, cache, or redistribute audio unless the provider's terms explicitly allow it.

### Architecture
Create a provider-neutral discovery layer:
- `DiscoverProvider` interface: provider identity, search, trending/featured, pagination, track detail, artist detail, album detail, artwork metadata, and stream resolution where authorised.
- Provider-specific implementations (e.g. Audius and any approved JioSaavn/BitChord adapter) remain isolated.
- A unified `DiscoverTrack`/remote entity model must preserve provider ID, provider name, canonical metadata, artwork URL, duration, explicit flag if supplied, and playback resolution metadata.
- Use namespaced IDs such as `audius:<id>` and `provider:<id>` to prevent collisions with local tracks and other providers.
- Keep local device tracks, albums and artists completely separate from remote Discover results.
- Provider failure must not crash the screen or replace valid cached data with fake/demo results.

### Discover experience
- Make the screen feel premium but restrained: clear Discover heading, provider filter/chips, Songs / Artists / Albums navigation, consistent artwork tiles, compact metadata, and polished loading/empty/error states.
- Provide a provider selector/filter only for providers that are actually enabled.
- Search should debounce input, cancel obsolete requests, and show provider-aware results.
- Trending/blank-query discovery should use a session seed or provider ranking to vary results; do not hardcode language or artist bias.
- Pagination must append without duplicates and avoid repeated requests for the same page.
- Artist and album taps must open remote detail pages using provider IDs, not accidentally route to local library detail screens.
- Preserve current playback queue behaviour and distinguish remote tracks from local tracks.
- Show clear network/offline states. Cached results may be shown offline, but clearly indicate when data is stale.

## 3. Cache and loading performance

Implement a measured, bounded cache:
- Cache provider responses and normalized metadata in Room (or the project's existing persistence layer) with provider + entity ID as the key.
- Store `fetchedAt`, expiry/TTL, and relevant provider metadata. Use stale-while-revalidate: render valid cached content immediately, refresh in the background, and retain cached content if refresh fails.
- Cache artwork through Coil's disk/memory cache; normalize image sizes and avoid repeatedly downloading full-resolution artwork for small thumbnails.
- Do not persist expiring stream URLs as permanent metadata. Resolve them when playback starts or refresh them when expired.
- Deduplicate concurrent identical requests (single-flight), cancel obsolete searches, apply bounded concurrency, and set sensible connect/read/call timeouts.
- Use pagination and sensible page sizes; never load an entire catalogue into memory.
- Parse JSON off the main thread. Avoid repeated mapping/grouping on every recomposition; memoize or precompute derived artist/album lists.
- Use stable LazyColumn/LazyGrid keys and content types. Keep playback progress updates from triggering whole Discover list recompositions.
- Add cache size/expiry controls only where they can be measured and safely cleared.

### Metadata quality
- Preserve provider-supplied title, artist, album, duration, artwork, genre, release date/year, and identifiers when available.
- Do not invent missing metadata. Use a neutral fallback label where necessary.
- Keep remote metadata separate from user-edited local file tags. Never write remote metadata into local audio files.
- Avoid duplicate artists/albums caused by case differences or inconsistent whitespace; normalize comparison keys while preserving display names.
- Artwork fallback order: provider artwork, cached artwork, then a neutral VESPER placeholder. Do not repeatedly retry known-broken URLs.
- Validate image and stream URLs; handle HTTP errors, redirects, expired links, and missing fields gracefully.

## 4. Reliability, privacy and testing
- No secrets or provider tokens committed to source control. Use only provider-approved client configuration.
- Respect provider attribution, rate limits, caching restrictions, and content rules.
- Add unit tests for parsing, missing fields, ID namespacing, deduplication, pagination, expiry, and provider errors.
- Add tests for offline cached rendering and stale-cache refresh failure.
- Verify remote playback, queue insertion, artist/album navigation, and local-library isolation.
- Run the project's actual Gradle build and tests. Do not claim success unless commands complete successfully.
- Test on a small screen and a large screen; check scrolling, bottom navigation, mini-player overlap, and system back.
- Measure cold start, cached Discover first-render time, uncached response time, artwork cache hit rate, and memory usage before and after. Report actual numbers and device/emulator conditions.

## 5. Safe execution order
1. Extract and inspect the supplied project; confirm this is the intended OLD VESPER baseline.
2. Create a full source backup before edits.
3. Map every existing Settings control to its current state source and callback.
4. Implement the Settings landing page and subpages without changing preference semantics.
5. Audit the current Audius Discover implementation and establish baseline tests/build.
6. Build the provider-neutral repository layer; migrate Audius without changing existing behaviour.
7. Verify JioSaavn and BitChord API permission/documentation. Integrate only approved endpoints; otherwise document and leave the adapter disabled.
8. Implement bounded metadata/response caching, artwork optimisation, cancellation, pagination, and error states.
9. Add tests, run build, fix regressions, and provide a changed-file list plus measured results.

## 6. Non-negotiable preservation rules
- Do not rewrite the app architecture.
- Do not remove or downgrade existing local music features, playback, equalizer/audio effects, metadata editor, playlists, favorites, scan folders, or user data.
- Do not change database schemas without a backward-compatible migration and migration test.
- Do not add fake Dolby Atmos/spatial-audio claims or fake provider results.
- Do not silently change audio quality or playback behaviour.
- Keep the visual language recognisably VESPER: AMOLED black, burgundy/wine accent, warm text, simple boxy surfaces.
- Back up before edits and report any feature that cannot be completed safely.

## Coding-agent execution prompt
Read this specification and implement it in the supplied OLD VESPER project. Back up the project first. Redesign Settings as a simple boxy category page with separate navigable subpages in VESPER's existing black/burgundy style. Then upgrade Discover using a provider-neutral architecture, retaining Audius and adding JioSaavn/BitChord only if their interfaces are authorised and compliant. Improve caching, metadata, artwork loading, pagination and responsiveness. Preserve all existing playback, local-library features, preferences and user data. Build and test the app, list changed files, and report only verified results.
