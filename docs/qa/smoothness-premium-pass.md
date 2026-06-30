# OmniTune Smoothness Premium Pass

Branch: `release/phase-5-rc-qa-offline-downloads`

Starting commit: `9f8d959ec04ecdfe217336002b59dd125d0f5e12`

## Root Causes Audited

- Home thumbnail hydration prewarmed too many curated items at launch and used six concurrent workers.
- Hydration request scheduling wrote loading state into Home UI state, rebuilding the Home model before useful thumbnail data existed.
- Home and collection artwork used `SubcomposeAsyncImage` in repeated list/card content, adding extra composition work.
- Generated fallback artwork rebuilt palette/text values during composition.
- Navigation used simple fades only, making route changes feel abrupt.
- Collection results were fetched again when reopening a collection instead of feeling cached.
- Track rows did not reserve a fixed height, increasing the chance of visual movement while artwork loaded.

## Changes

- Limited Home thumbnail hydration to two workers and delayed a small first-screen prewarm set.
- Switched repeated discovery/collection artwork to regular `AsyncImage` over stable generated fallback artwork.
- Added memory/disk cache keys and short thumbnail crossfade.
- Added shared `OmniMotion` screen and thumbnail timing tokens.
- Added subtle screen fade/vertical motion to the main `NavHost`.
- Added per-session Home collection result cache.
- Reserved fixed heights for Home rows and collection track rows.

## Verification

- `./gradlew assembleDebug`: PASS
- `./gradlew testDebugUnitTest`: PASS
- `./gradlew lintDebug`: PASS
- Device: `138898743000055`
- Fresh install/clear-data launch: PASS
- Home populated immediately: PASS
- Home scroll smoke: PASS
- Quick Pick to native collection: PASS
- Collection track load: PASS
- Search open/query smoke: PASS
- Settings open/category smoke: PASS
- Collection playback smoke: PASS

## Gfxinfo Sample

Package: `com.omnitune.app.debug`

- Total frames rendered: 1622
- Janky frames: 221 (13.63%)
- 90th percentile: 24ms
- 95th percentile: 34ms
- 99th percentile: 125ms
- Missed vsync: 19

## Not Fully Verified

- Android Studio profiler and recomposition counter tooling were not available in this CLI session.
- Full downloads/offline, lyrics provider runtime, and notification/background regression were not rerun in this pass.
- MiniPlayer full-player tap was attempted, but the UI dump did not conclusively identify the full Player screen.
