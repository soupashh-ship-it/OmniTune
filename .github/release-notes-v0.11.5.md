# OmniTune v0.11.5

This hotfix improves Mood and Genres reliability and cleans up the Home carousel artwork above Quick Picks.

## Fixes & Improvements

- Fixed Mood and Genres `Show all` so it renders already-loaded Home provider categories immediately instead of waiting indefinitely for a fresh provider request.
- Improved Home carousel artwork quality by requesting larger YouTube thumbnails and caching the high-resolution artwork URL.
- Removed the `Play` / `Open` badge from Home carousel artwork so the cards show cleaner cover art above Quick Picks.
- Kept the direct YouTube Browse category flow from 0.11.4 unchanged.

## Verification

- `assembleDebug`: passed
- `testDebugUnitTest`: passed
- `lintDebug`: passed

## Build

- Version: `0.11.5`
- Version code: `55`
