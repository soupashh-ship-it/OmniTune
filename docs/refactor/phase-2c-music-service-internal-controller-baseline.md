# Phase 2C MusicService Internal Controller Baseline

## Goal
Establish a clean verification baseline before extracting remaining internal `MusicService.kt` behavior into dedicated controllers.

## Starting Point
- Branch created from `fa57a78`.
- Working tree had pre-existing unrelated untracked docs:
  - `docs/architecture/`
  - `docs/qa/post-refactor-release-verification.md`
- No tracked code changes existed before Phase 2C edits.

## Environment Notes
- Initial Gradle invocation failed because `JAVA_HOME` was not set and `java` was not on `PATH`.
- Retried successfully with:
  - `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot`
  - `ANDROID_HOME=C:\Users\soupa\AppData\Local\Android\Sdk`

## Baseline Verification
- `.\gradlew.bat clean assembleDebug` -> PASS
- `.\gradlew.bat testDebugUnitTest` -> PASS
- `.\gradlew.bat lintDebug` -> PASS
- `.\gradlew.bat installDebug` -> PASS on device `138898743000055`
- Launch `com.omnitune.app.debug/com.omnitune.app.MainActivity` -> PASS

## Known Baseline Limitations
- Runtime smoke checks beyond launch were not performed before extraction.
- The environment requires explicit Java and Android SDK variables for command-line Gradle runs.
