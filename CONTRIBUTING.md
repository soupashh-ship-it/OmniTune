# Contributing To OmniTune

Thanks for helping improve OmniTune. Keep changes focused and verify behavior before opening a pull request.

## Setup

Requirements:

- JDK 21
- Android SDK 36
- Android Studio with current Android Gradle Plugin support, or the Gradle wrapper

Build locally:

```powershell
.\gradlew.bat assembleDebug
```

## Pull Requests

- Work from a feature branch.
- Keep one concern per pull request.
- Do not rename the package, namespace, or `applicationId`.
- Do not remove `LICENSE`, `CREDITS.md`, or required attribution.
- Do not commit signing files, keystores, `local.properties`, `google-services.json`, APKs, logs, or local reports.
- For playback, queue, downloads, notification, database, or update changes, include the verification you ran.

## Verification

Run the strongest relevant checks for your change:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

For runtime-sensitive changes, manually test playback start/pause/resume, queue operations, downloads, background playback, notification controls, and app restore behavior.

## Issues

When reporting a bug, include:

- App version
- Android version and device model
- Steps to reproduce
- Expected behavior
- Actual behavior
- Logs only when they do not contain private account, token, path, or device-identifying data
