# Phase 2C MusicService Internal Controller Extraction Report

## Goal
Extract the remaining internal `MusicService.kt` behavior controllers without changing public service APIs or playback/queue/recovery behavior.

## Implemented
- Added `PlaybackPreferenceObserver` for DataStore-backed playback preference collection:
  - skip silence
  - audio offload
  - player volume
  - combined fade volume
  - repeat mode
  - shuffle mode
  - crossfade duration
  - audio normalization
  - auto-skip-on-error
- Added `EqualizerController` for system equalizer setup, band application, and release.
- Added `CrossfadePlaybackCoordinator` for `CrossfadeAudio` construction, overlap-player creation, listener forwarding, and release.
- Added `RadioQueueManager` for seamless radio queue creation, stream resolution, queue trimming, insertion, and current queue updates.
- Added `PlaybackRecoveryCoordinator` for playback error classification, recovery retry policy, cache invalidation, stream re-resolution, fallback skip/toast behavior, and the buffering watchdog.
- Kept compatibility wrappers on `MusicService`, including:
  - `applyEqualizerBands(...)`
  - `startRadioSeamlessly()`
  - `playOrResolveCurrent()`
  - `playNext(...)`
  - `addToQueue(...)`

## Commits
- `d39f0cb` Add phase 2C MusicService baseline report
- `ec15822` Extract playback preference observer
- `dc3a1e6` Extract equalizer controller
- `44bb1c3` Extract crossfade playback coordinator
- `00108d3` Extract radio queue manager
- `6311b34` Extract playback recovery coordinator

## Verification
- Baseline before extraction:
  - `.\gradlew.bat clean assembleDebug` -> PASS
  - `.\gradlew.bat testDebugUnitTest` -> PASS
  - `.\gradlew.bat lintDebug` -> PASS
  - `.\gradlew.bat installDebug` -> PASS
  - Launch `com.omnitune.app.debug/com.omnitune.app.MainActivity` -> PASS
- After each completed extraction, static gates were run and passed:
  - `.\gradlew.bat clean assembleDebug` -> PASS
  - `.\gradlew.bat testDebugUnitTest` -> PASS
  - `.\gradlew.bat lintDebug` -> PASS
- Final verification:
  - `.\gradlew.bat clean assembleDebug` -> PASS
  - `.\gradlew.bat testDebugUnitTest` -> PASS
  - `.\gradlew.bat lintDebug` -> PASS

## Runtime Verification Limitation
Runtime verification after the later slices was delayed because ADB repeatedly lost the connected device during `installDebug`.

Observed failures included:
- `Skipping device '138898743000055' ... Device is OFFLINE.`
- `DeviceException: No online devices found.`
- `InstallException: device '138898743000055' not found`
- direct `adb install -r app\build\outputs\apk\debug\app-debug.apk` returning `adb.exe: device offline`
- transient `unauthorized` state requiring device-side confirmation.

After the device connection stabilized, direct ADB install and launch succeeded:
- `adb install -r app\build\outputs\apk\debug\app-debug.apk` -> PASS
- Launch `com.omnitune.app.debug/com.omnitune.app.MainActivity` -> PASS
- Focus confirmed on `com.omnitune.app.debug/com.omnitune.app.MainActivity`

The earlier issue occurred after successful baseline install/launch and after successful install/launch for the preference and equalizer slices. It appears to have been an ADB/device connectivity authorization problem, not a build or app packaging failure.

## Known Limitations
- Full interactive runtime smoke pass was not completed after all extractions.
- Edge checks were not completed:
  - Wi-Fi/mobile toggle during playback: NOT VERIFIED
  - force-stop/reopen: NOT VERIFIED
  - lock-screen controls: NOT VERIFIED
  - audio-focus/call interruption: NOT AVAILABLE
- Unrelated pre-existing untracked docs were intentionally left untouched:
  - `docs/architecture/`
  - `docs/qa/post-refactor-release-verification.md`

## Recommendation
`SAFE_WITH_NOTED_RISKS`: the refactor is structurally narrow and all Gradle build/test/lint gates pass, but a final runtime smoke pass should be rerun once ADB is stable.
