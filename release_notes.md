# OmniTune v0.6.10

## Final Validations and Polish

This release finalizes the current milestone by confirming that the in-app over-the-air updater flow works flawlessly, and adds some minor polish to the system equalizer handling.

## Fixes and Changes
- **Updater Verification**: Confirmed that the GitHub-backed AppUpdateChecker seamlessly discovers, downloads, and hands off the signed APK to the Android Package Installer without requiring any code changes.
- **Equalizer Handling**: Added a clear, user-facing toast message if the device's system equalizer is unavailable or cannot be initialized, providing better feedback than silently failing.
- **Version Bump**: Incrementally bumped to version 0.6.10 (code 24).
