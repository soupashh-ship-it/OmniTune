# OmniTune v0.13.0

This release focuses on stability work needed before a future stable 1.0 release.

## Playback

- Fixed network playback recovery so unresolved YouTube stream IDs no longer bypass offline/network handling.
- Reduced playback startup work by resolving the current track first instead of blocking startup on full queue resolution.
- Fixed queue job cancellation so rapid playback requests cannot leave older queue loads racing newer selections.
- Added a bounded playback stream cache separate from the offline download cache.

## Lyrics

- Fixed fullscreen player inline lyrics so the current and next lyric lines can appear immediately from the active lyrics loader.
- Fixed lyrics page/V2 fallback rendering so loaded lyrics can render and auto-scroll before the database cache flow updates.
- Added regression coverage for inline lyric state from freshly loaded lyric lines.

## Library

- Fixed Artist and Album All views so bookmarked-only items remain visible across sort modes.

## Database And QA

- Added seeded migration coverage for songs, artists, albums, playlists, and relationship maps.
- Expanded branch CI to run debug tests, debug and release lint, Android-test compilation, and debug APK assembly.
- Release CI now passes the YouTube Music API key through GitHub Secrets for verification and signed builds.

## Security And Repository Hygiene

- Removed committed diagnostic/build logs that contained historical request URLs.
- Moved the YouTube Music API key out of source and into `YOUTUBE_MUSIC_API_KEY`.
- Closed historical GitHub secret-scanning alerts after revocation/cleanup.

## Verification

- `testDebugUnitTest`
- `compileDebugAndroidTestKotlin`
- `lintRelease`
- `assembleRelease`
