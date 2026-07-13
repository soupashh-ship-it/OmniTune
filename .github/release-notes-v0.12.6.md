# OmniTune v0.12.6

This release improves code quality with null-safety hardening across the app, fixes compilation issues, and enables ProGuard/R8 optimizations for release builds.

## Fixes & Improvements

### Code Quality & Stability

- Hardened null-safety across many files by replacing unsafe `!!` assertions with safe calls and proper early returns, preventing potential crashes from unexpected null values.
- Fixed incorrect `return` labels introduced during null-safety refactoring in `LyricsV2.kt`, `HomeDiscoveryScreen.kt`, and `QueueScreen.kt` that caused compilation failures.
- Added `READ_MEDIA_AUDIO` permission manifest entry for Android 13+ audio library access.

### Build & Release

- Enabled ProGuard/R8 minification and resource shrinking for release builds, reducing APK size.
- Fixed a potential nullable crash in `TogetherOnlineApi` retry logic.
- Fixed a nullable crash in `DiscordImageResolver` cached image handling.
- Fixed nullable bound handling in `BottomSheet` dismiss/collapse/expand animations.
- Fixed various nullable artist and label references in menu screens and playlist screen.

## Verification

- `testDebugUnitTest`: passed
- `lintDebug`: passed
- `assembleDebug`: passed

## Build

- Version: **0.12.6**
- Version code: **65**
