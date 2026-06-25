# OmniTune v0.7.0 - OmniGlass UI Overhaul

Released: 2026-06-25

## What's New

- New OmniGlass visual design across Home, Search, Library, Downloads, Queue, Settings, MiniPlayer, and full player.
- Refined dark-first layout with improved spacing, accessibility, and compact-screen polish.
- Cleaner Settings experience for updates, diagnostics, notification guidance, About, credits, and license access.

## Fixes

- Fixed completed-download playback so tapping a downloaded track switches playback correctly, including when another Search track is already active.
- Verified completed-download playback works offline.
- Preserved Search playback, MiniPlayer/full-player controls, Queue/Add to Queue, update checker, diagnostics export, and legal access.

## Notes

- Signed release assets were produced by GitHub Actions using repository secrets.
- Release assets include `OmniTune-v0.7.0-release.apk` and `OmniTune-v0.7.0-release.apk.sha256`.
- Lyrics display is not available in this release.
- Notification and lock-screen behavior can vary by device/OEM.
- Active and failed download states need broader QA when those states are available.
- Older downloads may show limited metadata unless the song exists in the local database.
