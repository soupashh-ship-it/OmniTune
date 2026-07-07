# OmniTune Dynamic Song Colors Verification

Date: 2026-07-07
Branch: `ui/dynamic-song-colors`
Starting commit: `8960d70a9b3f7b479ff95fbdc25de179b1f2e632`

## Dynamic Color Behavior Summary

The dynamic color pass uses Android Palette extraction from current artwork, prefers expressive swatches, enhances saturation, avoids duplicate/similar colors, and clamps large player backgrounds into a comfortable dark range. Raw album colors are not painted directly onto large surfaces; colors are transformed into dark gradients, subtle overlays, slider accents, and glow-like surfaces so text remains readable.

## OmniTune Implementation

OmniTune already had `rememberPlayerGradient`, `PlayerColorExtractor`, `PlayerBackgroundColorUtils`, Palette, and Coil-based artwork loading. This pass kept that architecture and added a reusable `OmniDynamicSongPalette` with:

- `background`
- `backgroundSecondary`
- `surface`
- `surfaceElevated`
- `accent`
- `accentSoft`
- `onAccent`
- `textPrimary`
- `textSecondary`
- `miniPlayerSurface`
- `playerControlSurface`
- `gradientStart`
- `gradientEnd`

Palette extraction now filters unusable artwork colors, prefers vibrant/dark-vibrant before muted/dark-muted and dominant fallbacks, generates a readable accent, and derives dark glass surfaces from the accent instead of using raw artwork color.

## Applied Areas

- Global shell background via the existing `OmniShellBackground` hook.
- Full player background, artwork glow, loader accent, slider accent, previous/next control surfaces, play/pause button, and active shuffle/repeat accents.
- Mini player glass tint, border glow, ripple, artwork glow, play/pause button, and progress indicator.
- Home continue-listening card with a restrained current-song accent.

## Fallbacks And Performance

- Missing artwork or extraction failure falls back to the current OmniTune theme accent and dark fallback gradient.
- Light, dark, gray, and low-saturation artwork colors are clamped or ignored before surface generation.
- Coil image loading runs through the existing image loader; bitmap palette generation runs off the main thread.
- Extracted results are cached in memory by artwork candidate key and URL to avoid duplicate extraction between shell, player, mini player, and home.
- Palette fields are animated with Compose color springs so song changes transition smoothly without wrapping the whole shell in content transitions.

## Verification

- `.\gradlew.bat clean assembleDebug`: PASS
- `.\gradlew.bat testDebugUnitTest`: PASS
- `.\gradlew.bat lintDebug`: PASS
- `adb devices`: device initially reported `offline`; after ADB server restart, no device was connected.
- `.\gradlew.bat installDebug`: NOT RUN because no online device/emulator was available.
- Screenshots: NOT CAPTURED for this pass because no online device/emulator was available.

## Known Issues

- Manual runtime verification for dynamic color transitions still needs an online device/emulator.
- Screenshots for colorful-artwork and song-switch comparison were not captured in this pass.

## Safe To Continue

The branch is safe to continue building on from a build, test, and lint perspective. Runtime QA should be completed once a device or emulator is online.
