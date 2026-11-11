# VESPER AUDIO — Final Polish, Signature Sound & Security Hardening
**Date:** 2026-11-11  
**Baseline:** Latest uploaded `vesper-audio (5)(1).zip`  
**Priority:** P0 playback/UI defects → P1 Discover startup → P1 Tones/DSP → P0 security and regression validation.

## Product intent

Finish VESPER AUDIO as a stable, responsive, privacy-first Android music player with a recognisable **VESPER Signature** sound. The goal is to reveal detail, texture, ambience, transients and spatial cues that may be masked in familiar tracks—without falsely claiming to recover isolated stems or information that is not present in the source audio.

Use the uploaded screenshots as visual references. The Audio quality screen and Discover screen are the key examples. Preserve VESPER's existing Nocturne/black/burgundy visual identity, typography and product architecture.

---

## 1. Remove UI breakages and duplicated boxes

### Reported symptoms
- Some surfaces look like a card inside another card, creating doubled borders/boxes.
- Discover list rows and controls feel visually over-boxed.
- Audio quality/settings controls have inconsistent borders, outlines, radii and spacing.
- Earlier landscape scrolling and responsive layout issues must not regress.

### Required work
- Inspect the screenshots and trace the actual Compose hierarchy for each affected screen.
- Remove redundant nested `Surface`, `Card`, `border`, background and clickable wrappers when they create duplicate outlines or stacked containers.
- Each logical component should have one clear visual container and, at most, one intentional selected/focus indicator. Do not remove useful separation or accessibility focus indication.
- Standardise card radius, border thickness, internal padding, icon tile size, row height and vertical spacing using the existing VESPER theme tokens.
- Ensure selected segmented controls have one clear burgundy selected state, not multiple competing outlines.
- Keep Discover track rows visually consistent: one row container, one artwork tile, readable title/artist, source badge, and aligned actions. Prevent row content from being clipped or pushed under the mini-player.
- Ensure the mini-player and bottom navigation do not overlap Discover content. Add correct content insets/padding so the final list item remains reachable.
- Audit Settings, Audio quality, Discover, Tones, Studio EQ, Songs and Now Playing for duplicated containers and inconsistent dimensions.
- Preserve touch target sizes and accessibility semantics.

### Responsive and scrolling regression coverage
- Test compact and large phones, portrait and landscape, gesture and three-button navigation, display/font scaling, and keyboard visibility.
- Every long screen/list must scroll to its final item.
- Avoid nested vertical scroll containers with unbounded constraints.
- Use adaptive Compose constraints and window insets; do not add device-specific hard-coded offsets.
- Preserve scroll position and selected tab across recomposition and configuration changes where appropriate.

### Acceptance criteria
- No unintended double borders, double cards, overlapping player/navigation bars, clipped labels, or inaccessible controls.
- Discover and Settings can scroll fully in portrait and landscape.
- No visual regression to the VESPER Nocturne/burgundy identity.

---

## 2. Reduce Discover first-load and first-play latency

### Reported symptom
Discover takes too long to become usable and the first audio playback starts slowly.

### Required work
- Profile the full path from Discover screen entry → cached content → network refresh → artwork → stream resolution → Media3 prepare → first audible output.
- Render valid cached Discover results immediately while refreshing in the background. Clearly mark stale/cache state without blocking the screen.
- Do not wait for artwork, artist/album enrichment, or nonessential metadata before showing playable track rows.
- Avoid sequential API calls where independent requests can safely run concurrently. Add bounded concurrency and cancellation; do not overload the provider.
- Avoid duplicate refreshes on recomposition, repeated navigation, or multiple collectors. Use a single-flight request or equivalent deduplication.
- Reuse a correctly configured HTTP client and connection pool. Apply sensible connect/read/call timeouts, retry only transient failures with bounded backoff, and respect provider rate limits.
- Resolve the stream URL only when needed or pre-resolve the likely first item in a controlled way. Do not download the entire audio file before playback.
- Prepare the selected stream promptly; use Media3 buffering/load control appropriate for streaming and avoid unnecessary player/controller recreation.
- Keep artwork loading asynchronous, size-bounded and cached. Artwork failures must never block audio.
- Cancel stale search/refresh jobs and ignore late responses from old queries or provider selections.
- Provide useful loading, retry, offline and empty states. Do not show an endless spinner.
- Keep the provider policy intact: only authorised public/partner sources. Do not add undocumented endpoints, scraping, DRM bypass, or unauthorised stream extraction.

### Acceptance criteria
- With a warm cache, Discover displays usable tracks immediately while refresh continues.
- With an empty cache, loading state appears immediately and the screen remains interactive.
- First tap starts preparation without avoidable serial metadata/artwork work.
- Test good Wi-Fi, slow network, mobile data, offline mode, provider timeout, HTTP 429 and server errors.
- Measure cold and warm screen-entry time and tap-to-audio time on a physical Android device; report actual measurements rather than claiming a target was met without testing.

---

## 3. Tones workspace tabs and naming

### Required UI change
- Rename the two tabs in the Tones workspace to exactly:
  1. **Tones**
  2. **Studio EQ**
- Keep the screen/workspace identity as Tones.
- Do not label the second tab "Studio SEQ", "Studio Equalizer", or "Equalizer" in the tab control.
- Keep existing saved presets, active tone, band values and user preferences intact during the rename.
- Update accessibility descriptions, test tags, navigation labels and relevant copy consistently.
- Migrate old persisted enum/string values safely if names or keys change. Do not break existing user settings.

### Acceptance criteria
- Both tabs are visible and correctly selected.
- Switching tabs preserves state and does not recreate or reset DSP unexpectedly.
- Back navigation and configuration changes restore the correct workspace/tab.

---

## 4. VESPER Signature audio identity — improve all Tones and modes

### Creative/audio objective
Replace generic "Studio Reference" positioning as the signature experience with **VESPER Signature**. Make the built-in Tones and EQ modes sound intentional, differentiated and musically useful.

The intended experience: help listeners notice more of the texture already encoded in their favourite songs—sub-bass movement, kick/bass separation, vocal articulation, guitar/piano harmonics, reverberation tails, transient detail and stereo ambience—while retaining musical balance.

### Truthful technical boundary
- A conventional stereo EQ, bass boost or virtualizer cannot truly extract hidden stems, isolate instruments, or recreate audio information absent from the mix.
- Do not market EQ as AI stem separation, lossless enhancement, a DAC upgrade, high-resolution conversion, or guaranteed detail recovery.
- "Reveal detail" means carefully adjusting masking frequencies, dynamics and spatial perception where supported—not inventing missing instruments.
- Android device AudioEffect support varies. Show the actual processing status/capabilities and provide graceful software/off states.

### Required work
- Audit every built-in Signature Tone and Basic EQ mode. Remove duplicate-sounding or contradictory profiles and tune each profile by listening tests, not arbitrary extreme boosts.
- Make **VESPER Signature** the default branded tone/profile. It should be the balanced, engaging house sound: controlled low-end weight, clear vocal/instrument presence, smooth treble, stable centre image and safe headroom.
- Rename or migrate the old Studio Reference profile without silently deleting a user's custom settings. If a neutral bypass is still useful, keep it as a clearly named utility option such as **Flat / DSP Off**, not the main signature identity.
- Keep the six basic EQ modes useful and distinguishable: Balanced, Bass, Vocal, Rock, Pop and Acoustic. Tune curves conservatively and document their intent.
- Refine Signature Tones so each has a clear audible purpose. Suggested direction:
  - **VESPER Signature:** balanced house curve; full but controlled bass, natural mids, smooth detail.
  - **Warmth:** gentle low-mid body and relaxed upper treble without muddy vocals.
  - **Pulse:** tight kick/sub-bass impact with transient clarity; avoid boomy bass.
  - **Air:** restrained upper-frequency openness without hiss or harsh sibilance.
  - **Depth:** spatial impression only where the device's supported processing can do this cleanly; avoid phasey mono collapse.
  - **Night:** comfortable low-volume tonal balance and reduced fatigue; do not claim true loudness compensation unless implemented.
- Keep preamp/headroom protection. Calculate attenuation from positive EQ gain and any additional processing; prevent digital clipping and unsafe gain stacking.
- Clamp all DSP parameters to documented device-supported ranges. Handle unsupported effects and invalid session IDs safely.
- Avoid excessive bass boost, treble boost, loudness, virtualizer strength or aggressive compression. Include a one-tap bypass and reset.
- Ensure switching modes updates the active playback audio session without pops, clicks, stale effects or leaked effect instances.
- Make preset changes persistent and deterministic; custom presets must remain editable and survive app restarts.
- Add an A/B bypass control so users can compare processed and unprocessed sound at matched perceived loudness where feasible.
- Do not apply effects twice through both app DSP and device DSP. Detect and avoid duplicate/stacked processing where possible.

### Listening and measurement validation
- Test on multiple Android devices/headphones and the built-in speaker where available.
- Compare each profile against flat/bypass at matched loudness; louder must not automatically seem "better".
- Listen for clipping, distortion, pumping, harsh sibilance, muddy bass, phase cancellation, mono compatibility, channel imbalance and transition clicks.
- Check headroom with full-scale test signals and representative music.
- Verify EQ band frequencies, gain units, session lifecycle, output-route changes and effect release/recreation.
- Document known hardware limitations. Do not claim universal sound quality or identical output on every device.

---

## 5. App security, privacy and owner control

### Goal
Make the app and its release process controlled by the owner, with no hidden access path, secret third-party control, or avoidable data exposure. "Only I own/control the app" must be implemented through signing-key custody, repository/account ownership, transparent permissions and secure release practices—not through claims that an APK can never be copied or reverse engineered.

### Ownership and release integrity
- Confirm the application ID, package identity, versioning and release signing configuration.
- The owner must generate and retain the production signing key and passwords privately, preferably offline or in a secure password manager. Never commit keystores, passwords, tokens or signing secrets.
- Remove any shared/default production signing credentials. Keep debug signing separate from release signing.
- Document a safe release and backup process. Losing the signing key can prevent future updates under the same app identity.
- Ensure release builds are signed by the owner's key and verify the certificate fingerprint.
- Do not add remote kill switches, hidden admin accounts, backdoors, secret commands, undocumented remote configuration, or remote code execution.
- Do not claim that package ownership prevents copying, patching or reverse engineering. Explain realistic Android distribution and signing limits.

### Android attack-surface audit
- Review every manifest permission and remove any permission not required by an implemented feature.
- Audit exported activities, services, receivers and providers. Export only what Android/Media3 requires; add explicit intent filters/permissions and validate incoming intents.
- Check the MediaSessionService exposure and ensure external media controls can only invoke intended playback actions.
- Disable cleartext traffic unless a documented provider exception is essential; prefer HTTPS and validate redirects/hosts.
- Audit WebView use (if any): disable JavaScript/file access unless required, restrict navigation, and never expose privileged bridges to untrusted content.
- Validate all external URLs, provider IDs, media IDs, file paths, SAF URIs and deep-link inputs. Prevent path traversal, unsafe intent handling and malformed URI crashes.
- Use least-privilege storage access. Do not request broad file access when MediaStore/SAF is sufficient.
- Keep private app data in internal storage; use Android Keystore for any app-managed cryptographic keys. Do not invent custom cryptography.
- Ensure backups do not unintentionally export secrets, auth tokens, private user data or sensitive cache contents. Review `allowBackup`, backup rules and data extraction rules deliberately.
- Avoid logging tokens, URLs with credentials, user identifiers, file paths or personal library metadata in production logs.
- No analytics, tracking, crash-report uploads or telemetry without explicit disclosure and user consent. Prefer no collection by default.
- Do not embed long-lived private API keys in the APK. Client-side secrets are extractable. Use public unauthenticated APIs only within their terms; any privileged service credential belongs on a server controlled by the owner.
- Audit dependencies for known vulnerabilities, unnecessary libraries, abandoned packages and risky transitive dependencies. Pin/lock versions where practical and document update policy.
- Enable R8/minification for release only after verifying required Media3, Moshi, Room and reflection rules. Obfuscation is not a security boundary.
- Add a clear privacy/data disclosure and ensure the app works locally if optional online services are unavailable.

### Security tests
- Static review of manifest, exported components, backup rules, network security, secrets and dependency tree.
- Search the repository and generated release artifacts for API keys, passwords, tokens, keystores and debug-only credentials.
- Test malformed/deep-link intents, invalid media/provider IDs, hostile URL schemes, redirect handling and permission denial.
- Verify no cleartext HTTP requests except explicitly documented, justified exceptions.
- Verify release build has no debug logging, test endpoints, debug menu or test credentials.
- Verify no hidden network calls or telemetry occur without consent.
- Run dependency vulnerability checks and Android lint; report tool availability and actual findings.
- Do not run destructive tests against third-party services or real user data.

---

## 6. Regression tests for previous reported fixes

Do not regress any fixes already required by the earlier final-fix pass:

- Recently Added playback must use its own displayed date order; Next/Previous must not fall back to alphabetical order.
- Local and Discover playback must not introduce avoidable gaps or recreate the player unnecessarily. Be honest about network buffering limits.
- Metadata must recover available tags/artwork without overwriting user edits or creating duplicate tracks.
- Songs, Discover, Settings and all long screens must remain scrollable in landscape.
- All layouts must adapt across phone sizes and system insets.

---

## 7. Required test matrix

Run and report actual results for:

| Area | Required cases |
|---|---|
| UI | Portrait, landscape, compact/large screens, font scaling, gesture/3-button nav, no double boxes |
| Discover | Cold cache, warm cache, slow Wi-Fi, mobile data, offline, 429, timeout, retry, rapid search changes |
| Playback | Local, Discover, next/previous, queue transitions, pause/resume, interruptions, output-route change |
| DSP | Every Tone and EQ mode, bypass, custom preset, headroom, unsupported device effects, session recreation |
| Persistence | App restart, tab restore, settings migration, custom preset preservation |
| Metadata | Full/partial/missing/corrupt tags, artwork missing, rescan, user-edited fields preserved |
| Security | Manifest/exported components, secrets scan, backup, network, URL/intent validation, dependency audit |
| Build | Debug build, release build, unit tests, Compose tests, Android lint, release signing verification |

### Test discipline
- Add automated unit/Compose/instrumentation tests for reproducible bugs.
- Use fake/mock provider responses for timeout, error, rate-limit and malformed-data tests.
- Do not mark a test passed unless it actually ran and the result was inspected.
- For audio quality and startup latency, include physical-device listening/measurement notes; emulator success is not enough.
- If the environment cannot run Android SDK/Gradle or a physical-device test, clearly mark it **NOT RUN** and state what is needed.

---

## 8. Definition of done and final report

Do not stop after changing labels or colors. Trace and fix root causes.

The task is complete only when:
1. Duplicate boxes and UI overlaps are resolved in the referenced screens.
2. Discover becomes usable from cache quickly and first playback avoids unnecessary work.
3. Tones tabs read exactly **Tones** and **Studio EQ**.
4. VESPER Signature is the primary branded sound, with all modes/tone profiles tuned safely and honestly.
5. Previous Recently Added, landscape, gapless, responsive and metadata requirements remain intact.
6. Security review is completed with findings and mitigations.
7. Automated tests/builds are run where available, and physical-device tests are explicitly reported.

Final response must include:
- Root causes found.
- Files changed and purpose.
- DSP profiles/curves changed and technical limits.
- Before/after Discover timing measurements, if measured.
- Test commands and actual pass/fail/not-run status.
- Security findings, remaining risks and owner actions (especially signing-key custody).
- Any remaining known issues.

**Never claim "fully secure", "unhackable", "perfectly gapless", "stem separation", or "reveals audio that is not in the source". State measurable facts and limitations.**
