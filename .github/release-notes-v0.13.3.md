# OmniTune v0.13.3

This release refreshes OmniTune's dynamic artwork colors so the app reacts more clearly when the currently playing song changes.

## Fixes & Improvements

- Updated the default OmniTune accent seed from the previous muted lavender tone to a warmer, stronger coral tone.
- Improved song-change color extraction so the app uses the artwork color directly instead of over-processing it into duller results.
- Improved fullscreen player and mini-player background palettes with stronger artwork-driven gradients.
- Improved dynamic player color pairing by selecting the most prominent artwork color and a visually distinct companion color.
- Reduced overly dark color clamping in player surfaces while keeping text and controls readable.
- Updated fallback dynamic colors so tracks without artwork still use the refreshed OmniTune palette.

## Verification

- Passed `testDebugUnitTest`.
- Passed `git diff --check`.
- Passed `assembleDebug`.
