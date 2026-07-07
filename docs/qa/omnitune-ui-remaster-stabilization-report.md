# OmniTune UI Remaster Stabilization Report

Date: 2026-07-07
Branch: `ui/omnitune-remaster-foundation`

## Scope

This pass completed the missing stabilization work after the foundation remaster:

- Resolved the lint gate without a baseline.
- Tightened root shell chrome spacing, clipping, and system navigation safety.
- Improved mini player dock responsiveness and narrow-width behavior.
- Completed small-screen full player polish.
- Polished downloads and library surfaces with shared chrome spacing.
- Added conservative motion for stateful content changes.
- Installed and smoke-tested on a connected Redmi device.

## Commits Created

- `97ae2f3 chore: resolve ui remaster lint gate`
- `59c1431 ui: polish app shell and safe areas`
- `46bdee2 ui: remaster mini player dock`
- `e6337cf ui: complete full player polish`
- `580cefe ui: polish downloads and library surfaces`
- `23fdaa7 ui: add motion and responsive polish`
- `4842ecd ui: tighten app shell chrome clipping`

Starting commit: `dfc675c0c842590194e4f63d49762b535d3024d7`
Ending code commit before this report: `4842ecde6b85d6b9b9599d6a59dc5f1462845fe1`

## Lint Strategy

Fixed lint cleanly. No baseline was created.

Primary fixes:

- Replaced Compose `LocalContext.current.getString(...)` resource reads with `LocalResources` or `stringResource`.
- Centralized playlist-add toast/snackbar copy in `PlaylistToastMessages.kt`.
- Removed unnecessary `BoxWithConstraints` usage in lyrics rendering.
- Fixed unremembered fallback state collection in playlist suggestions.

## UI Areas Completed

- App shell: bottom chrome reserve now includes system nav inset, a chrome-safe gap, and content clipping.
- Mini player: keeps text readable on narrow phones, preserves next button, moves previous into overflow on narrow widths, keeps progress stable.
- Full player: adapts artwork/gaps for small heights, allows two-line long titles, clarifies shuffle/repeat active state.
- Downloads: completed/active/failed states remain distinct; list uses shared bottom chrome padding.
- Library: main empty hub now uses a premium framed surface; detail lists use shared bottom chrome padding.
- Motion: subtle `animateContentSize` for download row state changes and library empty hub.

## Behavior Preserved

- Playback source of truth remains `PlayerConnection`/service-backed state.
- Search result playback remains wired through the existing queue path.
- Play Next / Add to Queue callbacks remain unchanged.
- Download playback path remains unchanged.
- Queue route and full-player queue access remain unchanged.

## Verification

- `.\gradlew.bat clean assembleDebug`: PASS
- `.\gradlew.bat testDebugUnitTest`: PASS
- `.\gradlew.bat lintDebug`: PASS

Runtime:

- `adb devices`: device available intermittently as `138898743000055`.
- `.\gradlew.bat installDebug`: initially blocked by device-side USB/install state.
- `adb push ...` + `adb shell pm install -r -d -g -t ...`: PASS.
- App launch via monkey: PASS.
- Home render: PASS.
- Search screen render: PASS.
- Search query returned results: PASS.
- Search result tap started playback and updated mini player: PASS.
- Mini player tap opened full player: PASS.
- Full player rendered and playback controls were available: PASS.
- User confirmed remaining manual flows work.

Screenshots captured:

- `docs/qa/omnitune_home.png`
- `docs/qa/omnitune_search.png`
- `docs/qa/omnitune_player.png`

## Known Issues / Risks

- ADB connection on the test device was unstable and occasionally flipped offline. Install succeeded through `pm install`.
- Search input through `adb shell input text` inserted `%20` literally for the space; this is an adb input limitation, not an app search failure.
- No emulator-based small-width rotation pass was completed in this run.

## Recommendation

This branch is safe to continue building future UI features on. The remaster now has a passing build, unit tests, and lint, with runtime smoke coverage for launch, home, search playback, mini player, and full player.
