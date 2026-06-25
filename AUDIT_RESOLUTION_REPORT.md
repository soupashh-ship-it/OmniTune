# OmniTune Baseline Audit Resolution Report
**Target version:** `v0.6.11`
**Status:** Completed

This document outlines the high-priority (P0/P1) architectural debt, UI decoupling issues, and UX bugs identified in the original baseline audit, and details exactly how each was resolved.

## 1. UI/Service Decoupling
**Issue:** The UI (`PlayerScreen`, `MiniPlayer`) directly mutated the ExoPlayer instance by accessing `MusicService.instance.player`, violating clean architecture principles and risking concurrent state issues.
**How it was fixed:** 
- Implemented `PlayerConnection`, a dedicated intermediary class that wraps ExoPlayer.
- Removed all direct `MusicService.instance` accesses from the UI.
- The UI now exclusively collects state via `StateFlow` (e.g., `playbackState`, `isPlaying`) and issues commands via discrete methods (e.g., `seekToNext()`, `toggleShuffle()`).

## 2. Playback State Machine & Network Recovery
**Issue:** Playback recovery was fragile. YouTube stream URLs bound to a specific IP address would fail with `403 Forbidden` when the device switched from Cellular to Wi-Fi. Additionally, YouTube "Error 2000" was misclassified due to an incorrect error code check.
**How it was fixed:**
- **Network Switching:** Implemented an aggressive cache invalidation mechanism upon detecting network transport changes. `StreamUrlResolver` now flushes IP-bound URLs on network switch.
- **Error 2000 Classifier:** Updated `PlaybackErrorClassifier.kt` to examine the actual `PlaybackException` message string for `"error 2000"` rather than checking for a non-existent integer code.
- **Unresolved Streams:** Added a failsafe to `StreamUrlResolver`. If a YouTube ID cannot be resolved, it is assigned a custom `omnitune-unresolved://` scheme rather than silently failing, allowing the queue to handle it gracefully.

## 3. Queue Boundaries & Shuffle Logic
**Issue:** `PlayerScreen` Next/Previous buttons gave no visual feedback when disabled at the start/end of a queue. "Permanent Shuffle" was a dead setting in the UI.
**How it was fixed:**
- **Boundary Logic:** Wired the `enabled` parameter of `MetroIconButton` to `playerConnection.canSkipNext` and `canSkipPrevious`, visually dimming the buttons at queue boundaries (unless `REPEAT_MODE_ALL` is active).
- **Shuffle Setting:** Removed the non-functional `PermanentShuffleKey` entirely from `SettingsScreen` to prevent user confusion, opting for standard session-based shuffle persistence.

## 4. Widget & Notification Sync
**Issue:** The home screen widget always displayed hardcoded text ("OmniTune / Ready to play"). The custom media notification buttons all used generic Play icons.
**How it was fixed:**
- **Widget:** Wired `OmniTuneWidget` to `PreferencesGlanceStateDefinition`. `MusicService.onMediaItemTransition` now writes the current track's title and artist to standard preferences, which dynamically updates the Glance widget layout.
- **Notification:** Replaced the copy-pasted `CommandButton.ICON_PLAY` constants in `MusicService.kt` with `ICON_UNDEFINED` and explicit `setIconResId` references to ensure semantic icons (Heart, Repeat, Shuffle, Radio) render correctly.

## 5. Library Metrics
**Issue:** The "Downloads" smart card on the Library screen had an empty subtitle.
**How it was fixed:**
- Updated `LibraryScreen.kt` to bind the subtitle string directly to `${uiState.downloadCount} songs`, correctly reflecting the offline storage metric.

## 6. Hardware Integrations (Equalizer)
**Issue:** If a device's OS lacked a system equalizer, `setupEqualizer()` would fail silently in the background.
**How it was fixed:**
- Added a `Toast` notification fallback in the `catch` block of `setupEqualizer()` to proactively inform the user that the system EQ is unavailable on their specific ROM/device.

## 7. App Update Infrastructure
**Issue:** Sideloaded app updates failed with "No APK Asset Found".
**How it was fixed:**
- Conducted root-cause analysis revealing it was a GitHub release timing issue (the API was queried before the APK upload finished). 
- Validated the existing `AppUpdateChecker` and `FileProvider` logic. It correctly requests `REQUEST_INSTALL_PACKAGES` and hands the signed APK seamlessly to the Android Package Installer without any code alterations required.

## 8. App Branding & Assets
**Issue:** The app required a tailored, professional icon.
**How it was fixed:**
- Generated precise square and circular Android Launcher icons (MDPI through XXXHDPI) from the provided master image using a Python Pillow scaling script.
- Embedded a 432x432 `ic_launcher_foreground.png` asset to fully support modern Android Adaptive Icons (squircle, teardrop, round).
- Tagged and deployed this visual overhaul as `v0.6.11`.

## 9. Legal Attribution & Docs
**Issue:** Missing GPL attribution for code derived from Velune. Stale release notes.
**How it was fixed:**
- **CREDITS.md:** Explicitly credited Nikhil Vishwakarma for the original `CrossfadeAudio` implementation and UI layout foundations.
- **Release Docs:** Incrementally maintained `release_notes.md` to perfectly match the GitHub releases, detailing the fixes for versions `0.6.7` through `0.6.11`.
