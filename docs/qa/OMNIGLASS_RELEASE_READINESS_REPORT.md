# OmniGlass Release Readiness Report

Date: 2026-06-24
Device: `138898743000055`
Build baseline: `v0.6.11` UI overhaul through Phase 12

## Build And Lint

- `.\gradlew.bat clean assembleDebug`: PASS with JDK 21.
- `.\gradlew.bat lintDebug`: PASS with JDK 21.
- `adb devices`: `138898743000055	device`.

## Completed-Download Playback

Release status is NO-GO because completed-download playback failed during clean Phase 12 retesting.

- Test A, no active Search playback: FAIL. Launching fresh, opening Downloads, and tapping completed `Veridis Quo` did not produce a valid completed-download playback result.
- Test B, while Search playback active: FAIL. After Search playback started `Instant Crush (feat. Julian Casablancas)`, tapping completed `Veridis Quo` did not switch the full player or MiniPlayer metadata away from `Instant Crush`.
- Test C, offline completed-download playback: NOT RUN. Online completed-download playback is already release-blocking, so offline radio toggling was skipped.

Evidence:

- Downloads listed two completed items: `Veridis Quo` and `Instant Crush (feat. Julian Casablancas)`.
- `DownloadsScreen.kt` still marks only `Download.STATE_COMPLETED` rows as playable and calls the passed `onPlayDownload(download)` callback.
- The Phase 12 failure is not isolated to a row click affordance: tapping a completed row left the active player state unchanged or produced invalid playback state.
- The callback passed from `MainActivity.kt` constructs a manual `MediaItem` for completed downloads; that code is not a Phase 12 allowed fix surface and was unchanged from `HEAD`.

No UI-only fix was applied because the failure was not proven to be limited to `DownloadsScreen.kt` or a shared row/card component.

## Regression Results

- App launch: PASS.
- Home render: PASS.
- Search render: PASS.
- Search tap playback: PASS during Phase 12 after `Instant Crush` started and appeared in MiniPlayer/full player.
- MiniPlayer: PASS for active Search playback.
- Full player: PASS for active Search playback.
- Play/pause: PASS for active Search playback.
- Seek/shuffle/repeat: not fully rerun after NO-GO; Phase 11 had passed.
- Queue/Add to Queue: not fully rerun after NO-GO; Phase 11 had passed.
- Library render: PASS.
- Downloads render: PASS.
- Completed-download playback: FAIL.
- Offline completed-download playback: NOT RUN.
- Settings render: PASS screenshot captured.
- Update checker: PASS in Phase 11, returned already latest; not rerun after NO-GO.
- Diagnostics/export: PASS in Phase 11, share sheet opened; not rerun after NO-GO.
- About/Credits/License: PASS in Phase 11; not rerun after NO-GO.
- Playback across tabs: PASS for Search playback in Phase 11; not fully rerun after NO-GO.
- Insets/no overlap: PASS in Phase 11; spot-checked in captured screenshots.

## Screenshots

Captured under `docs/qa/screenshots/omni-glass/`:

- `01_home.png`
- `02_search_results.png`
- `04_full_player_download_failure.png`
- `06_library.png`
- `07_downloads.png`
- `08_settings.png`

The complete release screenshot set was not finished because the release-blocking completed-download playback failure was reproduced.

## Protected Surfaces

Protected runtime surfaces were not changed in Phase 12:

- Playback service/connection files: unchanged.
- Stream resolver: unchanged.
- Download service/util and download implementation logic: unchanged.
- Data/repository implementation files: unchanged.
- Queue implementation and persistence: unchanged.
- Lyrics logic files: unchanged.
- Update-check logic: unchanged.
- Diagnostics/export logic: unchanged.
- Gradle/release/signing files: unchanged.
- License/credits text: unchanged.

## Release-Blocking Issues

- Completed-download playback fails to switch playback from an active Search track to the tapped completed download.
- Offline completed-download playback remains untested and cannot be claimed while online completed-download playback fails.

## Non-Blocking Issues

- Lyrics surface remains unavailable.
- Queue count text may lag after swipe-remove until player state recomposes.
- Queue swipe-remove uses a Material deprecation warning.
- Missing-artwork fallback was not force-tested.
- Active/failed download states need QA with real active/failed downloads.
- OEM notification and lock-screen behavior remains device-dependent.

## Untested Items

- Offline completed-download playback.
- Active/failed download state behavior with real active/failed downloads.
- Full release screenshot set after a clean passing regression.
- Notification/lock-screen controls on multiple OEM builds.
- Missing-artwork fallback on known artwork-less tracks.

## Device-Dependent Risks

- Notification and lock-screen controls can vary by OEM.
- Battery optimization can affect background playback.
- Diagnostics export depends on Android share-sheet availability.
- About/legal links depend on browser or URL-handler availability.

## Final Decision

NO-GO.

The app should not proceed to release preparation until completed-download playback passes both online completed-download playback tests. A separate focused Phase 13 bugfix should investigate the completed-download playback handoff with explicit approval to inspect and, if necessary, modify the correct playback/download integration surface.

## Phase 13 Completed Download Playback Bugfix

Date: 2026-06-24
Device: `138898743000055`

### Root Cause

The completed-download tap callback in `MainActivity.kt` built a manual `MediaItem` with a title-only AndroidX `MediaMetadata`, but without the app-level `com.omnitune.app.models.MediaMetadata` tag that OmniTune uses as the canonical current metadata source. `PlayerConnection` and `MusicService` publish player metadata through the app metadata tag, so a tagless downloaded item could leave the MiniPlayer/full-player state showing the previous Search track even after a completed download was tapped.

### Fix

- `MainActivity.kt` now passes the injected `MusicDatabase` into `OmniTuneMainScreen`.
- The completed-download callback now builds the tapped download item through the same `toMediaItem()` path used by normal app playback.
- If a matching local song exists, full stored metadata is used.
- If the old completed download only has title data, the fallback creates an app metadata tag with the real download id and real stored title, leaving missing artist/artwork as honest fallback UI.
- The callback still routes through `connection.playQueue(ListQueue(title = "Downloads", items = listOf(mediaItem)))`.
- Non-completed download states are guarded and do not start playback.

### Validation

- `.\gradlew.bat clean assembleDebug`: PASS with JDK 21.
- `.\gradlew.bat lintDebug`: PASS with JDK 21.
- `adb devices`: `138898743000055	device`.
- Debug APK install: PASS.

### Completed Download Playback Results

- Test A, clean/no active Search playback: PASS. Tapping completed `Veridis Quo` opened full player with `Veridis Quo` as current title and active playback state.
- Test B, while Search playback active: PASS. Active Search track `Da Funk / Daftendirekt` switched to completed download `Veridis Quo`.
- Test C, offline completed-download playback: PASS. Wi-Fi/mobile data were disabled, app was force-stopped, and completed `Veridis Quo` still opened and played offline with correct title metadata.
- MiniPlayer metadata after completed download: PASS. MiniPlayer showed `Veridis Quo`.
- Full player metadata after completed download: PASS. Full player showed `Veridis Quo`.

### Regression Results

- Search tap playback: PASS.
- MiniPlayer: PASS.
- Full player: PASS.
- Play/pause: PASS.
- Seek: PASS.
- Shuffle: PASS.
- Repeat: PASS.
- Queue current item: PASS.
- Incomplete/failed download false playability: NOT AVAILABLE; device had 0 active and 0 failed downloads, and code still marks those states non-playable.
- Settings render: PASS.
- Update checker: not rerun in Phase 13; Phase 11/12 had passed and Phase 13 did not touch update logic.
- Diagnostics/export: not rerun in Phase 13; Phase 11/12 had passed and Phase 13 did not touch diagnostics logic.
- About/license/credits: not rerun in Phase 13; Phase 11/12 had passed and Phase 13 did not touch legal surfaces.

### Protected Surfaces

- Stream resolver: unchanged.
- Download service/util: unchanged.
- Download architecture: unchanged.
- Repository/data contracts: unchanged.
- Queue implementation: unchanged.
- Gradle/release/signing: unchanged.
- License/credits text: unchanged.

### Release-Readiness Update

CONDITIONAL GO.

The Phase 12 release blocker is fixed: completed-download playback now passes clean, active-search, and offline tests. A separate release packaging phase should still rerun the final release checklist and screenshots before publishing.

## Phase 14 Final Release Verification

Date: 2026-06-24
Device: `138898743000055`

### Build And Packaging

- `.\gradlew.bat clean assembleDebug`: PASS with JDK 21.
- `.\gradlew.bat lintDebug`: PASS with JDK 21.
- `.\gradlew.bat assembleRelease`: NOT RUN - release signing environment variables were not available locally.
- Debug APK install: PASS.

### Full Regression Results

- App launch and bottom navigation: PASS.
- Home render: PASS.
- Search render and result loading: PASS.
- Search tap playback: PASS with `Da Funk / Daftendirekt`.
- MiniPlayer metadata and tap-to-full-player: PASS.
- Full player metadata, play/pause, seek, previous/next, shuffle, and repeat controls: PASS.
- Add to Queue: PASS from Search overflow.
- Queue open/current/upcoming display: PASS.
- Library render/routes: PASS.
- Downloads render: PASS.
- Completed-download Test A, clean/no active Search playback: PASS with completed `Veridis Quo`.
- Completed-download Test B, while Search playback active: PASS. Active `Da Funk / Daftendirekt` switched to `Veridis Quo`.
- Completed-download Test C, offline: PASS after disabling Wi-Fi/mobile data and force-stopping the app.
- Settings render/scroll: PASS.
- Update checker: PASS, reported `Already latest`.
- Diagnostics/export: PASS, Android share sheet opened.
- About/Credits/License/GPL access: PASS.
- Playback across tabs: PASS.
- Insets/no overlap: PASS by device smoke and screenshots.

### Screenshots

Captured under `docs/qa/screenshots/omni-glass-final/`:

- `01_home.png`
- `02_search_results.png`
- `03_miniplayer.png`
- `04_full_player.png`
- `05_queue.png`
- `06_library.png`
- `07_downloads.png`
- `08_settings.png`
- `09_about_license.png`
- `10_update_checker.png`
- `11_diagnostics_share.png`

### Protected Surfaces

- Playback service/connection behavior: unchanged in Phase 14.
- Stream resolver: unchanged.
- Download service/util and download logic: unchanged.
- Update-check logic: unchanged.
- Diagnostics/export logic: unchanged.
- Data/repository contracts: unchanged.
- Queue implementation/persistence: unchanged.
- Lyrics logic: unchanged.
- Gradle/release/signing files: unchanged.
- License/credits text: unchanged.

### Release-Blocking Issues

- None found in Phase 14.

### Remaining Non-Blocking Or Conditional Items

- `assembleRelease` was not run because release signing credentials are not available locally.
- Active/failed download visual-state QA remains unavailable; the device had no active or failed downloads.
- Lyrics display surface remains unavailable and was not invented by the OmniGlass overhaul.
- OEM notification and lock-screen controls remain device-dependent.
- Older downloads may have limited artist/artwork metadata unless the song exists in the local database.

### Final Decision

CONDITIONAL GO.

Core playback, Search playback, MiniPlayer, full player, Queue/Add to Queue, completed-download playback, offline completed-download playback, Settings, update checker, diagnostics/export, and legal access all passed on device. Before public release, run the release packaging task with signing credentials and, if possible, spot-check active/failed download states and OEM notification/lock-screen behavior.
