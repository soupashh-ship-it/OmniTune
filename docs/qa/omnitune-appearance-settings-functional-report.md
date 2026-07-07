# OmniTune Appearance Settings Verification

Date: 2026-07-07
Branch: `fix/settings-appearance-functional`
Starting commit: `03753c1007f7c1b126e638701666bfa4ffe84e00`

## Summary

Settings -> Appearance was reduced to controls that are supported, persisted, and wired to real UI behavior. Placeholder rows were removed instead of keeping inactive switches or dead navigation targets.

## Functional Settings Kept

- Dynamic song colors: enables or disables artwork-reactive colors across the app shell, full player, mini player, progress controls, and now-playing surfaces.
- Player background: switches the full player between dynamic gradient and solid dark modes.
- Pure black mode: deepens MaterialTheme and OmniTune surface/background tokens for OLED-friendly dark UI.
- Use system font: applies the device font through the existing app theme path.

## Placeholder Settings Removed From Appearance

- Dynamic theme toggle
- Theme Creator entry
- Customize Background entry
- Player design style
- New mini player design
- New library design
- Hide Player Thumbnail
- OmniTune Canvas
- Thumbnail cropping
- Player button colors
- Player slider style
- Mini player swipe sensitivity
- Lyrics presentation controls
- Home/library/auto-playlist visibility controls

Underlying preferences were not deleted where other screens still consume them; they were only removed from Appearance because they are not supported as Appearance controls in the current remastered UI.

## Preference Model

- Added `DynamicSongColorsKey`
- Added `OmniPlayerBackgroundStyleKey`
- Added `OmniPlayerBackgroundStyle`
  - `DYNAMIC_GRADIENT`
  - `SOLID_DARK`

## Applied Behavior

- `MainActivity` now uses `DynamicSongColorsKey` for artwork-based root theme accents and disables hidden system dynamic-color behavior for this setting.
- `rememberPlayerGradient` returns the static OmniTune palette immediately when dynamic song colors are disabled.
- `PlayerScreen` applies `OmniPlayerBackgroundStyleKey` to choose dynamic gradient or solid dark background.
- `OmniColors.updateFromTheme` now applies pure-black background and surface tokens when pure black mode is enabled.
- `SettingsScreen` Appearance copy now matches the smaller supported setting set.

## Verification

- `.\gradlew.bat clean assembleDebug`: PASS
- `.\gradlew.bat testDebugUnitTest`: PASS
- `.\gradlew.bat lintDebug`: PASS
- `adb devices`: no online device/emulator detected
- Runtime install/manual QA: NOT RUN
- Screenshots: NOT CAPTURED

## Known Issues

- Manual runtime verification still needs an online Android device/emulator.
- Older internal preference keys remain in the codebase where other screens still read them.

## Safe To Continue

The branch is safe to continue building on from build, unit test, and lint results. Runtime QA should be completed when a device or emulator is available.
