OmniTune 1.5.1 is a stable maintenance release focused on YouTube Music sign-in, account playlist import, update checks, and app icon consistency.

## Changes

- Fixed YouTube Music sign-in persistence by saving session cookies, visitor data, data-sync ID, and account details before closing the login flow.
- Added support for secure YouTube SAPISID cookie variants used by newer Google/YouTube sign-in sessions.
- Improved account-owned and private YouTube Music playlist imports after sign-in.
- Added support for importing playlists from pasted playlist IDs as well as full YouTube and YouTube Music links.
- Added a visible Stable / Prerelease selector to the in-app updater.
- Fixed prerelease update checks so newer stable releases can still be offered when appropriate.
- Reset older launcher icon aliases to the new default OmniTune app icon on first launch after updating.
- Updated default launcher, splash, in-app, and notification resources to the new OmniTune icon set.
- Updated playback notification resources to use the new transparent OmniTune notification mark.

## Quality

- Added regression coverage for secure login cookies, data-sync extraction, playlist ID imports, and updater channel behavior.
- Verified innertube unit tests, app unit tests, Android-test Kotlin compilation, release lint, and release APK assembly locally.

Package: com.omnitune.app
Version: 1.5.1
Version code: 152
Status: Stable release

-- OmniTune
