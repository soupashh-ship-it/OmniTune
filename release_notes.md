# OmniTune v0.6.5 Hardening Candidate

## Focus

This release candidate focuses on stability over new features: playback recovery, stream resolution, queue behavior, downloads/offline playback, search list responsiveness, and release hygiene.

## Fixes and Improvements

- Updated NewPipeExtractor usage to the current `v0.26.3` artifact and aligned Rhino to `1.8.1` to reduce YouTube stream resolution failures.
- Verified force-stop/process-death recovery: reopening the app and tapping a new search result starts fresh playback instead of reusing stale stream URLs.
- Kept stream cache clearing on service startup so expired direct stream URLs are not trusted after process death.
- Improved search result scrolling with stable list keys/content types and bounded thumbnail requests.
- Added a completed-download playback handoff from Library > Downloads.
- Verified completed Media3 downloads can play after force-stop with Wi-Fi and mobile data disabled.
- Added a manual Settings > Updates checker backed by GitHub Releases with package/version verification before install.
- Fixed a Settings crash caused by using framework drawable resources in Compose `painterResource`.
- Removed corrupt packaged font resources and returned typography to Android's system font.
- Added a sanitized diagnostic report export from Settings > Advanced.
- Preserved release hygiene: Firebase remains opt-in, release signing requires real secrets, and known secret/artifact files are ignored and untracked.

## Verified

- Debug build and clean debug build.
- ADB install and launch.
- Search result playback.
- Force-stop/reopen/new playback recovery.
- MiniPlayer and full player smoke tests.
- Queue/Add to Queue/Play Next from prior 0.6.x QA.
- MediaSession pause/play through Android media keys.
- Offline playback from completed Media3 download.
- Signed GitHub release workflow for `v0.6.4`.
- Settings > Updates no-update path against the public latest release.

## Still Partial

- Notification shade visual controls need physical phone verification.
- Lock-screen visual controls need physical phone verification.
- Network-disabled search and non-downloaded playback error copy needs a manual UI pass.
- Download rows currently preserve title and completion state; richer artist/album/artwork metadata should be improved in a future patch.

## Status

0.6.5 is a hardening candidate, not a final stable or premium-ready release.
