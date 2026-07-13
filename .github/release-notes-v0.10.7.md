# OmniTune v0.10.7

## Functional Appearance Customization

This release restores Appearance and customization controls only where they are fully wired to real app behavior. Placeholder settings were kept hidden until their supporting UI systems are ready.

### Added

* Player design style selector with Default, Compact, and Immersive full-player layouts.
* Mini player design selector with Default and Compact dock treatments.
* Library design selector with Default and Compact List layouts.
* Player button color mode selector with Dynamic, Default, and Monochrome options.
* Player slider style selector with Default, Thin, and Rounded progress treatments.
* Lyrics presentation selector with Default, Compact, and Large presets.
* Mini player swipe sensitivity control backed by the existing swipe gesture behavior.
* Appearance reset action that restores Appearance defaults without changing playback, queue, downloads, account, or service settings.

### Improved

* Full player now respects the Hide Player Thumbnail setting and rebalances metadata, progress, and controls when artwork is hidden.
* Mini player customization now affects artwork size, control density, progress styling, and swipe threshold behavior.
* Library customization now affects real library spacing, row density, and category chip density.
* Dynamic and static control color choices now apply consistently across the full player and mini player.
* Lyrics presets now feed the existing synced lyrics renderer instead of exposing disconnected controls.
* Appearance settings are reorganized into clearer groups: Appearance, Player, Mini Player, Library & Home, Lyrics, and Advanced.

### Fixed

* Removed dead Appearance rows and replaced them with persisted settings that visibly affect the UI.
* Fixed customization settings that previously appeared clickable but had no effect.
* Prevented unsupported Canvas, thumbnail cropping, and advanced theme editor controls from appearing before their dependencies are complete.
* Preserved playback, search, queue, downloads, mini player state, and dynamic song color behavior.

### Verification

* `clean assembleDebug`: passed
* `testDebugUnitTest`: passed
* `lintDebug`: passed
* Runtime device QA was not run in this pass because no device or emulator was attached.

### Build

* Version: `0.10.7`
* Version code: `47`
* Release APK is built and signed through GitHub Actions using repository secrets.
