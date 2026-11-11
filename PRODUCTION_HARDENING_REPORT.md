# VESPER — Production hardening handoff

## Changes applied in this pass

- Replaced the AI Studio starter README with VESPER-specific build, release and feature guidance.
- Added a beta-stage privacy notice, security policy and licence decision document.
- Added keystore, signing-properties, environment-file and local configuration exclusions to `.gitignore`.
- Removed hard-coded debug signing configuration. Debug builds use standard Android debug signing.
- Release signing now requires a maintainer-provided keystore path, store password, key alias and key password.
- Added a release signing verification task so `assembleRelease` / `bundleRelease` fail if release signing is not configured.
- Migrated the source namespace and Kotlin packages from `com.example.*` to `com.vesper.audio.*` and moved source/test files to matching directories.
- Added an explicit Media3 `onConnect` policy: only trusted Android/system/media controllers can connect and control playback.
- Disabled Android cloud backup and device transfer until a deliberate backup/restore feature is designed.

## Important correction to the earlier audit

The supplied backup XML files were not uncustomised: they selectively included the Room database and DataStore preferences while excluding SQLite WAL/SHM files. The current hardening change intentionally disables both cloud backup and device transfer for now. This is a privacy-first product decision and means users will not automatically migrate VESPER preferences/playlists through Android backup.

## Decisions intentionally not made automatically

1. **Permanent application ID:** the supplied app uses `com.aistudio.sonora.musicaudio`; source namespace is `com.vesper.audio`. Changing the application ID can make the new build install as a separate app and prevents it from updating an existing installation under the old ID. Choose the final ID before public beta.
2. **Namespace/package refactor:** the source packages are `com.vesper.audio.*`. A safe refactor must update Kotlin packages/imports, manifest component names, tests and generated references together.
3. **Open-source licence:** no `LICENSE` was added because GPL/MIT/Apache/MPL selection is a legal permission decision for the copyright holder. Review `LICENCE_DECISION_REQUIRED.md`.
4. **Gradle wrapper:** the supplied archive lacks `gradlew`, `gradlew.bat` and `gradle-wrapper.jar`. This environment has no usable Gradle installation or Android SDK, so a verified wrapper could not be generated and a Gradle build could not be run. The wrapper properties now pin the official Gradle 9.3.1 binary distribution checksum, and `docs/RELEASE_HARDENING.md` documents how to generate and verify the wrapper from a trusted Gradle installation.

## Validation performed

- Source-level edits and file presence checked.
- Manifest and backup policy inspected.
- Release signing configuration inspected for absence of committed credentials.
- MediaSession trusted-controller policy added against the Media3 API contract.
- ZIP integrity checked after packaging.

## Not run

- Gradle sync, Kotlin compilation, unit tests, instrumentation/Compose tests, Android Lint.
- Debug/release APK build and APK signature verification.
- Physical-device playback, notification, Bluetooth, Android Auto, permission-denial and backup/restore tests.
- Full dependency vulnerability scan, merged-manifest review and APK secret scan.

Do not call this production-ready until the not-run checks pass and the application ID / licence decisions are resolved.


## Owner decision applied

- Existing `applicationId` `com.aistudio.sonora.musicaudio` was retained as requested.
- No signing keys, passwords or licence choice were invented or committed.
