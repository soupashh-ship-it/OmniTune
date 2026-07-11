# OmniTune v0.12.1

This release improves download management, organizes downloaded collections, and avoids device-specific playback timing artifacts at song boundaries.

## Downloads and Library

- Added **Remove all queued** to the Library downloads screen.
- Album and playlist downloads now create or update a local playlist with the original collection name and artwork.
- Added an **All / Downloaded** filter to Library playlists.
- Kept individually downloaded songs in the regular Downloads list without creating extra playlists.
- Prevented duplicate downloaded playlists when the same album or playlist is downloaded again.
- Fixed YouTube album downloads occasionally doing nothing while album songs were still loading.
- Added a non-destructive database migration for downloaded-playlist metadata.

## Playback

- Made audio offload opt-in after updating to avoid device-specific accelerated intros and outros.
- Automatically disables audio offload while crossfade or silence processing is active, including during initial player startup.
- Preserved normal speed and pitch controls for users who intentionally change playback parameters.

## Verification

- `testDebugUnitTest`: passed
- `lintDebug`: passed
- `assembleDebug`: passed
- Room schema migration generation: passed

## Build

- Version: `0.12.1`
- Version code: `61`
