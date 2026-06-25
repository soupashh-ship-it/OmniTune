# OmniTune v0.7.2 - Network Resilience Update

Released: 2026-06-25

## What's New

- Fixed an issue where the app could hang indefinitely when attempting to start playback on Wi-Fi networks that are connected but lack validated internet access (e.g., captive portals or IPv6 blackholes).
- Added a 10-second timeout for stream URL resolution to prevent the player from stalling on dead connections.
- Added a 15-second playback startup watchdog that intercepts endless buffering and provides a clear retry option.
- Improved error handling to show a clear user-facing toast notification when network retries are exhausted.

# OmniTune v0.7.1 - RC GO Verification

## What's New

- Completed final RC verification for offline and completed download playback (Phase 30C).
- Hardened lyrics, queue, and library flows before 1.0 (Phase 23-28).
- Verified completed download playback online and offline.
- Passed crash and privacy leak checks.

# OmniTune v0.7.0 - OmniGlass UI Overhaul

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
- Added full-player lyrics entry and bottom sheet UI.
- Supports synced lyrics, plain fallback, no-lyrics, and error states.
- Privacy protection: full lyrics are not logged.
- Note: Synced lyric auto-scroll may need tuning for unusual LRC formatting.
- Notification and lock-screen behavior can vary by device/OEM.

## Phase 28 � Library Depth + Dead Route Hardening

- Implemented real PlaylistDetailScreen showing playlist songs from Room database.
- Wired playlist item tap to `playlist/{playlistId}` route (replaced dead "coming soon" Toast).
- Playlist songs load from `playlist_song_map` join table with real playback support.
- Empty state: "No songs in this playlist yet" shown when playlist has no songs.
- Added root-only `/window*.xml` gitignore rule for UI dump files.
- Active and failed download states need broader QA when those states are available.
- Older downloads may show limited metadata unless the song exists in the local database.

