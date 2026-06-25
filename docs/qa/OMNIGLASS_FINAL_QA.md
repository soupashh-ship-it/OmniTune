# OmniGlass Final QA Ledger

## Scope

This QA ledger covers the OmniGlass UI overhaul through Phase 11:

- Phase 0: Baseline Lock
- Phase 1: Design Tokens and Theme Foundation
- Phase 2: Shared Premium Components
- Phase 3: App Shell, Navigation, Background, and Global Layout Polish
- Phase 4: Home / Discovery Redesign
- Phase 5: Search Redesign
- Phase 6: MiniPlayer Redesign
- Phase 7: Full Player Redesign
- Phase 8: Queue and Lyrics Surface Polish
- Phase 9: Library and Downloads Redesign
- Phase 10: Settings, Update Checker, Diagnostics, and About Polish
- Phase 11: Responsiveness, Accessibility, Consistency, and Final UI Polish

## Build And Lint

- Phase 0 baseline build/lint passed before UI changes.
- Phases 1 through 10 each reported `.\gradlew.bat clean assembleDebug`: PASS.
- Phases 1 through 10 each reported `.\gradlew.bat lintDebug`: PASS.
- Phase 11 `.\gradlew.bat clean assembleDebug`: PASS with JDK 21.
- Phase 11 `.\gradlew.bat lintDebug`: PASS with JDK 21.

## Smoke Tests Run

- Phase 2 device smoke test passed on device `138898743000055`.
- Phase 3 device smoke test passed.
- Phase 4 device smoke test passed.
- Phase 5 device smoke test passed for search playback, MiniPlayer, full player, Play Next, Add to Queue, and playback across tabs.
- Phase 6 device smoke test passed for MiniPlayer metadata, artwork, tap-to-open, controls, and tab switching.
- Phase 7 device smoke test passed for full player render, controls, seek, shuffle, repeat, queue action, download action, collapse/back, and playback continuity.
- Phase 8 device smoke test passed for Search playback, Add to Queue, Queue open/render/current/upcoming/actions, MiniPlayer, full player, and playback across tabs.
- Phase 9 device smoke test passed for Library, Downloads, completed download playback when available, MiniPlayer, full player, and playback across tabs.
- Phase 11 device smoke test ran on `138898743000055`. Home, Search, search tap playback, MiniPlayer, full player, play/pause, seek, shuffle, repeat, Queue, Library, Downloads render, Settings render/scroll, update checker, diagnostics share sheet, About/license/credits visibility, playback across tabs, and inset/no-overlap checks passed.

## Smoke Tests Not Run Or Still Pending

- Phase 10 device smoke test was not run because ADB reported no connected devices.
- Phase 11 completed-download playback smoke was inconclusive/failed: tapping a completed download did not visibly switch the full player metadata away from the already-playing Search track during the run.
- Offline completed-download playback has not been tested.
- Active/failed download state QA is pending until real active or failed downloads exist.
- Missing-artwork fallback has not been force-tested on device for MiniPlayer or full player.
- Phase 5 offline/no-network search state was not tested.

## Device-Dependent Risks

- OEM notification and lock-screen behavior remains device-dependent.
- Battery optimization and background playback behavior can vary by device/OEM.
- Android share sheet availability affects diagnostics export validation.
- Browser or URL-handler availability affects About/legal link validation.
- Offline completed-download playback depends on real completed downloads being present on the device.

## Remaining QA Gaps

- Lyrics surface remains unavailable; Phase 8 intentionally did not invent a lyrics route or surface.
- Queue count text may lag after swipe-remove until player state recomposes.
- Swipe-remove in Queue still uses a Material deprecation warning.
- Active, paused, queued, failed, and unknown download visual states need QA with real Media3 states.
- Missing-artwork fallback should be force-tested with tracks that genuinely lack artwork.
- Long-title behavior should be spot-checked on 360dp and 393dp device widths.
- Completed-download playback should be re-tested in Phase 12 from a clean state because Phase 11 did not prove it.

## Release-Blocking Issues

- Phase 12 completed the final regression audit and produced a NO-GO decision.
- Completed-download playback is release-blocking: tapping completed `Veridis Quo` did not switch playback away from active Search playback for `Instant Crush (feat. Julian Casablancas)`.
- Offline completed-download playback was not run because online completed-download playback failed.
- Release preparation must wait for a focused completed-download playback bugfix and retest.

## Non-Blocking Issues

- Lyrics surface unavailable is non-blocking for the OmniGlass UI polish unless lyrics is promoted as a release feature.
- Queue count lag after swipe-remove is a known UI-state freshness issue; queue behavior and persistence were not changed.
- OEM notification and lock-screen variance is expected and should be documented rather than claimed as universally fixed.

## Phase 12 Final Status

- `.\gradlew.bat clean assembleDebug`: PASS with JDK 21.
- `.\gradlew.bat lintDebug`: PASS with JDK 21.
- `adb devices`: `138898743000055	device`.
- Screenshots captured under `docs/qa/screenshots/omni-glass/`.
- Release readiness report created at `docs/qa/OMNIGLASS_RELEASE_READINESS_REPORT.md`.
- Final decision: NO-GO.
- Required next step: a separate focused bugfix phase for completed-download playback, with explicit approval before touching playback/download integration surfaces.

## Phase 13 Completed Download Playback Bugfix Status

- `.\gradlew.bat clean assembleDebug`: PASS with JDK 21.
- `.\gradlew.bat lintDebug`: PASS with JDK 21.
- `adb devices`: `138898743000055	device`.
- Debug APK install: PASS.
- Root cause: completed-download playback used a manual tagless `MediaItem`, while OmniTune publishes current MiniPlayer/full-player metadata from the app-level metadata tag.
- Fix: completed-download playback now creates the tapped item through the same app metadata `toMediaItem()` path as normal playback, using local database metadata when available and title-only honest fallback for older downloads.
- Test A, clean/no active Search playback: PASS with completed `Veridis Quo`.
- Test B, Search playback active: PASS. Active `Da Funk / Daftendirekt` switched to completed `Veridis Quo`.
- Test C, offline completed-download playback: PASS after disabling Wi-Fi/mobile data and force-stopping the app.
- MiniPlayer metadata after completed download: PASS.
- Full player metadata after completed download: PASS.
- Search tap playback after fix: PASS.
- Settings render after fix: PASS.
- Incomplete/failed download false-playability: NOT AVAILABLE on device; no active/failed downloads existed, and presentation state still marks non-completed downloads non-playable.
- Protected stream resolver, download service/util, repository/data contracts, queue implementation, Gradle/release files, and license/credits text were not changed.
- Release-readiness update: CONDITIONAL GO. The Phase 12 release blocker is fixed, but a separate release packaging/final checklist phase should rerun final screenshots and the full release checklist before publishing.

## Phase 14 Final Release Verification Status

- `.\gradlew.bat clean assembleDebug`: PASS with JDK 21.
- `.\gradlew.bat lintDebug`: PASS with JDK 21.
- `.\gradlew.bat assembleRelease`: NOT RUN - release signing environment variables were not available locally.
- `adb devices`: `138898743000055	device`.
- Debug APK install: PASS.
- App launch, Home, Search, Library, Downloads, Settings, and bottom navigation: PASS.
- Search tap playback: PASS.
- MiniPlayer metadata and tap-to-full-player: PASS.
- Full player metadata, play/pause, seek, previous/next, shuffle, and repeat: PASS.
- Add to Queue and Queue current/upcoming display: PASS.
- Completed-download Test A, clean/no active Search playback: PASS with `Veridis Quo`.
- Completed-download Test B, Search playback active: PASS. `Da Funk / Daftendirekt` switched to `Veridis Quo`.
- Completed-download Test C, offline: PASS after disabling Wi-Fi/mobile data and force-stopping the app.
- Update checker: PASS, reported `Already latest`.
- Diagnostics/export: PASS, Android share sheet opened.
- About/Credits/License/GPL access: PASS.
- Screenshots captured under `docs/qa/screenshots/omni-glass-final/`.
- Protected playback, stream resolver, download service/util, update-check logic, diagnostics/export logic, data/repository, queue, lyrics, Gradle/release, and license/credits surfaces were not changed in Phase 14.

### Phase 14 Remaining QA Gaps

- `assembleRelease` still needs a signed release environment.
- Active/failed download states were not available on the device.
- OEM notification/lock-screen behavior remains device-dependent.
- Lyrics display surface remains unavailable.
- Older completed downloads may have limited artist/artwork metadata if no local database song record exists.

### Phase 14 Decision

CONDITIONAL GO. Core release paths passed on device after the Phase 13 completed-download playback fix. Complete signed release packaging and any available device/OEM spot checks in a separate explicit release task.
