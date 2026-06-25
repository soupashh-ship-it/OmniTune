# OmniTune v0.7.0 — OmniGlass UI Overhaul

Draft date: 2026-06-25

## Highlights

- OmniGlass visual overhaul across the app.
- Redesigned Home, Search, MiniPlayer, full player, Queue, Library, Downloads, and Settings surfaces.
- Improved dark-first glass UI with more consistent spacing, accessibility, and responsive layout polish.
- Improved Settings presentation for appearance, updates, diagnostics, notification/lock-screen guidance, About, credits, and license access.
- Fixed completed-download playback so tapping a completed download starts the tapped offline item, updates MiniPlayer/full-player metadata, and replaces an active Search track when needed.
- Verified offline completed-download playback on device `138898743000055`.

## Stability And QA

- Search playback verified.
- MiniPlayer and full player verified.
- Queue and Add to Queue verified.
- Completed downloads verified online and offline.
- Update checker verified.
- Diagnostics/export verified.
- About, credits, license, and GPL access verified.
- Final debug/device regression passed on device `138898743000055`.

## Known Limitations

- Lyrics display surface remains unavailable.
- OEM notification and lock-screen controls may vary by device.
- Active/failed download states need broader QA when those states are available.
- Older completed downloads may have limited artist/artwork metadata unless the song exists in the local database.
- Signed release APK is produced by GitHub Actions using repository secrets.

## Verification Summary Before Tagging

- `clean assembleDebug`: PASS.
- `lintDebug`: PASS.
- Device full regression: PASS on `138898743000055`.
- Completed-download playback clean state: PASS.
- Completed-download playback while Search playback active: PASS.
- Offline completed-download playback: PASS.
- Update checker: PASS, reported already latest.
- Diagnostics/export: PASS, Android share sheet opened.
- About/Credits/License: PASS.

## Release Process Note

This release has not been published by this draft. After explicit approval, create and push the `v0.7.0` tag so the verified GitHub Actions release workflow can build, sign, verify, checksum, and upload the release APK.
