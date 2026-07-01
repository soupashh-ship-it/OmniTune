# OmniTune v0.8.1

This release focuses on Home feed polish, native navigation, and settings reliability after v0.8.0.

## Changes

- Refined Home into a provider-backed, image-led music feed with compact header, chips, hero content, shelves, and native item actions.
- Kept Quick Picks honest: playable local/provider tracks only, with a compact start-exploring fallback when no playable history exists.
- Improved native collection pages with lighter presentation, compact track rows, stable artwork sizing, and smoother fallback artwork.
- Fixed Explore moods and Mood and Genres pages so provider browse results resolve playlists, albums, and artists into playable tracks instead of opening empty collections.
- Fixed Settings quick actions so Updates and Diagnostics panels open correctly in the debug and release app.
- Verified update checks and diagnostic report export on-device.

## Download

- `OmniTune-v0.8.1-release.apk`
- `OmniTune-v0.8.1-release.apk.sha256`

## Notes

- The release APK is built and signed by GitHub Actions using the repository release secrets.
- The APK filename is compatible with OmniTune's in-app updater.
