OmniTune 1.2.0-pre11 prerelease.

This supersedes v1.2.0-pre10 with a focused polish pass before the stable release.

Highlights since pre10:
- Added a visible App Updates entry back into the main Settings screen, under About & Project.
- Prerelease builds now default the updater to the Prerelease channel, while stable builds keep the Stable channel by default.
- Updated the default launcher, splash, in-app brand mark, and notification logo assets to the new OmniTune black-and-white icon.
- Reworked the first-run welcome screen copy so it presents OmniTune clearly instead of reading like inherited project text.
- Updated the themed monochrome launcher icon layer to use an OmniTune-style mark instead of the old music-note glyph.

Validation:
- Local verification was run before tagging.
- GitHub Actions Android Release verifies tests, Android-test compilation, lintRelease, signs the release APK, verifies it with apksigner, and uploads the APK plus SHA-256 file.
- Package: com.omnitune.app
- Version: 1.2.0-pre11
- Version code: 129

Manual install note:
This prerelease APK is attached for manual download/install. Android may require allowing installs from the browser or file manager.
