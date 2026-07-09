# OmniTune v0.11.4

This release upgrades Home discovery with a deeper Mood and Genres experience, smoother visual polish, and richer personalized listening rails while preserving the existing playback, search, queue, downloads, playlists, and Settings flows.

## Fixes & Improvements

- Replaced the placeholder Mood and Genres screen with a real provider-backed catalog powered by YouTube Music mood and genre groups.
- Added grouped Mood and Genres sections with polished two-column cards, provider accent colors, loading shimmer, retry handling, and mini-player-safe spacing.
- Connected Home's Mood and Genres `Show all` action to the new deep catalog.
- Opened mood and genre categories through OmniTune's existing collection flow so categories such as Chill, Workout, Focus, and Genres resolve real playable browse content.
- Restored the personalized `Keep listening` shelf from listening history signals.
- Improved recent discovery labeling with `Similar to [artist]` shelves when artist metadata is available.
- Added a subtle dynamic ambient background to Home using OmniTune theme accents.
- Updated the bundled changelog and release notes for version `0.11.4`.

## Verification

- `clean assembleDebug`: passed
- `testDebugUnitTest`: passed
- `lintDebug`: passed

## Build

- Version: `0.11.4`
- Version code: `54`
