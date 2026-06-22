# OmniTune In-App Update System

OmniTune uses a manual, user-confirmed update flow. The app checks GitHub Releases, downloads an APK only after the user taps Download, verifies the downloaded package, and then opens Android's normal package installer. It never performs silent installs.

## Update Source

The app checks only the official latest release endpoint:

`https://api.github.com/repos/soupashh-ship-it/OmniTune/releases/latest`

The checker reads `tag_name`, `name`, `body`, `published_at`, `prerelease`, `draft`, and `assets`.

Draft releases are ignored. Prereleases are ignored in release builds; debug builds may inspect prereleases for testing.

## Release Asset Naming

Attach one installable release APK using one of these names:

- `OmniTune-vX.Y.Z-release.apk`
- `OmniTune-vX.Y.Z-universal-release.apk`

Do not use debug APKs for public updater assets. The updater rejects assets with debug/unsigned names, `.idsig` files, source archives, and unrelated zip files.

Recommended assets:

- `OmniTune-v0.6.3-release.apk`
- `OmniTune-v0.6.3-release.apk.sha256`

## SHA-256 Convention

If GitHub exposes an asset `digest` value with `sha256:...`, OmniTune verifies it.

If no digest is available, publish a companion checksum asset named exactly:

`<apk asset name>.sha256`

The checksum file should contain the 64-character SHA-256 hash, optionally followed by whitespace and the filename.

## Package Verification

After download, OmniTune inspects the APK with `PackageManager` before launching the installer:

- package name must match the installed app package
- versionCode must be greater than the installed app
- signing metadata must be present
- file size must be non-zero
- GitHub asset size must match when available

If package name differs, the app shows:

`This update package does not match OmniTune.`

If versionCode is same or lower, the app shows:

`This update is not newer than your installed version.`

## Install Flow

OmniTune stores update APKs in its private cache `updates/` folder and exposes only that folder through a FileProvider content URI. The install intent uses:

- `application/vnd.android.package-archive`
- `FLAG_GRANT_READ_URI_PERMISSION`

On Android 8+, if unknown-app install permission is missing, OmniTune opens `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` for its own package. The user must grant permission and manually confirm installation.

Silent install is not supported because Android requires user confirmation for normal apps and because bypassing that protection would be unsafe.

## Signing Warning

Android will only update an installed app if the package name and signing certificate are compatible. If a user installed an older test build signed with a different key, Android may block the update. The safe recovery path is to uninstall the old test build once and install the new secure release.

## Test Checklist

1. Build the app with `.\gradlew.bat clean assembleDebug`.
2. Install with `adb install -r app\build\outputs\apk\debug\app-debug.apk`.
3. Launch with `adb shell monkey -p com.omnitune.app.debug 1`.
4. Open Settings > Updates.
5. Tap Check for updates.
6. Confirm same/lower latest release shows no update.
7. Test against a newer release asset when available.
8. Confirm Download starts only after tapping Download.
9. Confirm mobile data requires a second confirmation.
10. Confirm wrong package and same/lower versionCode APKs are rejected before installer launch.
11. Confirm valid APK opens Android's package installer through a content URI.

## Replacing Release Assets

When replacing a bad GitHub APK asset:

1. Delete the old APK asset from the release.
2. Upload a correctly named release APK.
3. Upload the matching `.sha256` file.
4. Confirm the release is not a draft.
5. Confirm the APK package name, versionCode, and signing key are correct before publishing.
