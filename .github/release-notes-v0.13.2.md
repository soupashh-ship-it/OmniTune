# OmniTune v0.13.2

This release focuses on restoring reliable lyrics behavior and cleaning up the launcher icon presentation.

## Fixes

- Fixed lyrics provider results being incorrectly rejected when a provider returned valid plain-text lyrics without timestamps.
- Fixed plain lyrics loading as an empty result, which made the lyrics screen show missing/error states even after a provider returned usable lyrics.
- Fixed auto-scroll not advancing when a plain-text provider result was cached before a synced LRC/TTML result. OmniTune now prefers synced lyrics for playback-following views and only falls back to plain lyrics when no synced lyrics are available.
- Fixed existing plain cached lyrics blocking synced lyrics forever by refreshing unsynced cached lyrics when a synced provider result can be found.
- Fixed lyrics failing with "Cannot access database on the main thread" by moving lyrics repository database reads/writes and refresh work onto the IO dispatcher.
- Preserved synced LRC/TTML timestamps through the lyrics repository so playback-synced lyrics can continue to auto-scroll with the current song position.
- Made the fullscreen lyrics sheet use the synced lyrics renderer consistently so timed lyrics follow playback instead of requiring manual scrolling.
- Restored the fullscreen player lyric preview path by keeping fetched lyrics displayable for both synced and plain lyric formats.
- Updated the app launcher icon assets from the provided OmniTune icon with cleaner internal padding so the icon no longer appears overly zoomed or cramped.
- Updated the adaptive launcher icon background to a dark tone to avoid visible white edges around the padded icon.

## Verification

- Passed focused lyrics unit tests for repository parsing, inline synced lyrics, and lyrics ViewModel retry/current-song behavior.
- Passed `compileDebugKotlin`.
