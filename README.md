# VESPER AUDIO

VESPER is an Android music player focused on local music playback, library organisation,
audio tuning and optional online discovery.

> **Development status:** beta / active development. Features, compatibility and privacy
> behaviour must be validated on supported physical devices before a stable release.

## Project principles

- Local-first music library and playback.
- Optional online discovery; online features are not required for local playback.
- No account is required by the current app flow.
- Clear disclosure of permissions and third-party network services.
- No claim of Dolby Atmos, studio mastering, stem separation or recovery of audio information
  that is not present in the source recording.

## Current capabilities

The codebase includes AndroidX Media3 playback, Jetpack Compose UI, Room persistence,
MediaStore / SAF library scanning, audio effects, and Audius-based discovery.

Some features may be incomplete or change during beta. Check the in-app screens and the
[CHANGELOG](CHANGELOG.md) rather than assuming every planned feature is production-ready.

## Requirements

- Android Studio compatible with the Gradle and Android Gradle Plugin versions declared in
  `gradle/libs.versions.toml`.
- Android SDK platforms/build tools required by the project (`compileSdk` is declared in
  `app/build.gradle.kts`).
- JDK version supported by the declared Android Gradle Plugin.

## Open the project

1. Open the repository root in Android Studio.
2. Install the SDK platform requested by Gradle.
3. Sync Gradle and resolve any dependency or SDK warnings.
4. Run the `app` configuration on an emulator or physical Android device.

**Gradle wrapper note:** the supplied archive did not contain `gradlew`, `gradlew.bat`, or
`gradle/wrapper/gradle-wrapper.jar`. Until those are restored, open the project in Android
Studio with a compatible local Gradle installation or generate a wrapper using the Gradle
version declared in `gradle/wrapper/gradle-wrapper.properties`. Do not download a wrapper JAR
from an untrusted mirror.

## Release signing

Never commit production signing material.

Configure these environment variables for a release build:

- `KEYSTORE_PATH` — path to the owner's private keystore.
- `STORE_PASSWORD` — keystore password.
- `KEY_ALIAS` — production key alias.
- `KEY_PASSWORD` — key password.

Release packaging is intentionally blocked when signing material is not configured. Debug
builds use Android's standard debug signing.

Keep at least two secure backups of the production keystore and its passwords. Losing the
signing key can prevent future updates under the same Android application ID.

## Online services

- **Audius:** optional discovery/search/streaming, subject to Audius API terms, creator settings,
  availability and applicable content permissions.
- **MusicBrainz / Cover Art Archive:** optional metadata and artwork enrichment. Respect API
  rate limits, attribution and applicable commercial-use terms.

Online provider failure must not prevent local library playback.

## Privacy and security

Read [PRIVACY.md](PRIVACY.md) and [SECURITY.md](SECURITY.md) before distributing builds.
Report vulnerabilities privately using the process in `SECURITY.md`.

## Testing

Before each public beta, run:

- `testDebugUnitTest`
- `lintDebug`
- `assembleDebug`
- `assembleRelease` after configuring the owner's release keystore
- Physical-device checks for library access, playback, background media controls,
  output-route changes, offline behaviour, data migration and app updates.

Do not publish a release until the actual build/test results have been reviewed.

## License

**License selection is pending the copyright holder's decision.** No open-source licence is
granted by this repository until a `LICENSE` file is deliberately selected and added.
See `LICENCE_DECISION_REQUIRED.md`.
