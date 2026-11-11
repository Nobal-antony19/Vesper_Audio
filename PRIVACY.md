# Privacy Notice — VESPER AUDIO

**Status:** beta draft; review this notice against every feature enabled in the release build
before publication.  
**Last reviewed:** 2026-11-11

## Data stored on the device

VESPER uses Android media access to display and play music selected from the device. The app
also uses local persistence for library/cache information and user preferences (including
playlists/favourites and audio settings, depending on the feature used).

The app does not need an account to provide its core local music-player functionality.

## Online requests

When online features are used, VESPER may contact:

- **Audius discovery providers** for discovery/search results and audio stream resolution.
- **MusicBrainz** for optional recording metadata.
- **Cover Art Archive** for optional artwork.

Those services receive the network request data necessary to fulfil the request, such as a
search query, recording title/artist, requested resource identifier, IP address and standard
network metadata. Their own privacy policies and terms apply.

Local playback should remain available when online services are disabled or unavailable.

## Permissions

The manifest currently declares internet access, audio-media/storage access for library
scanning, foreground media playback, notification permission, wake lock and audio settings
access. Review the exact permission prompts and platform behaviour on every supported Android
version before release. Remove any permission no longer required by implemented features.

## Analytics, advertising and crash reporting

No advertising or analytics SDK is intentionally documented as part of the current product
design. Re-check the final dependency graph, merged manifest and release APK before making a
public "no trackers" claim. Do not add telemetry or crash-report uploads without an explicit,
understandable disclosure and user choice.

## Backups

The hardened project defaults to excluding app data from Android cloud backup and device
transfer. This avoids silently copying local library metadata and preferences to another
device. If backup is introduced later, document exactly what is included and provide a clear
user-facing choice.

## Retention and deletion

App-private data can be removed by clearing app storage or uninstalling the app, subject to
Android's backup/device-transfer behaviour and any data already sent to third-party providers.
Provider-side request logs are governed by those providers' own retention policies.

## Contact

Before public release, replace this line with a dedicated project privacy/security contact
address controlled by the maintainer. Do not publish a personal email address unless you
intentionally want it to be the public support contact.
