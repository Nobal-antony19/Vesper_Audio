# VESPER release hardening runbook

## Stable package identity

The Android `applicationId` is intentionally retained as
`com.aistudio.sonora.musicaudio` to preserve update compatibility. The Kotlin/Gradle namespace
is `com.vesper.audio`. Do not change `applicationId` without a separately planned migration.

## Restore the Gradle Wrapper

The supplied source archive omitted `gradlew`, `gradlew.bat` and `gradle-wrapper.jar`.
Generate them from a trusted Gradle 9.3.1 installation, not by running an unverified wrapper:

```bash
gradle wrapper --gradle-version 9.3.1 --distribution-type bin
```

The expected official wrapper JAR SHA-256 for Gradle 9.3.1 is:

`b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13`

Verify after generation:

```bash
sha256sum gradle/wrapper/gradle-wrapper.jar
```

The expected Gradle 9.3.1 binary distribution SHA-256 is:

`b266d5ff6b90eada6dc3b20cb090e3731302e553a27c5d3e4df1f0d76beaff06`

`gradle/wrapper/gradle-wrapper.properties` pins this value using
`distributionSha256Sum`. Do not remove it. Compare checksums against Gradle's official release
checksum reference before trusting a wrapper update:
https://gradle.org/release-checksums/

## Build verification

After restoring the wrapper and installing the Android SDK required by `compileSdk 36.1`:

```bash
./gradlew --version
./gradlew clean
./gradlew lintDebug
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Only after configuring the owner's release keystore through environment variables:

```bash
./gradlew assembleRelease
```

Do not use a debug-signed APK for public distribution. Verify the release APK with Android
Build Tools `apksigner verify --print-certs` and compare its certificate fingerprint with the
owner's securely recorded fingerprint.

## Release signing environment

Set these in the local build environment or protected CI secrets, never in source files:

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

## Release gate

A release is not production-ready until the above commands pass, the APK is installed and
tested on physical devices, the release signature is verified, and the open-source licence
decision has been made and recorded in the repository.
