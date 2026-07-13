# OmniTune v0.12.0

This release replaces the earlier lyrics, crossfade, and download workarounds with fixes at the Media3 playback and download boundaries.

## Crossfade and Audio

- Fixed crossfade pausing the current song by preventing the overlap player from requesting competing Android audio focus.
- Fixed the next song silently running ahead during preload; it now stays prepared at the beginning and starts only when the fade window opens.
- Disabled audio offload while crossfade is active so both decoded streams can mix without an offload transition gap.
- Replaced the obsolete reflective audio-offload toggle with Media3's current track-selection API, making the Audio offload setting functional again.

## Downloads

- Fixed song, album, playlist, search-result, bulk-selection, and auto-like downloads by resolving a playable stream URL before creating the Media3 request.
- Added shared request deduplication and bounded parallel stream resolution for large playlists.
- Explicitly resumes the download manager after enqueue and retains fresh-URL retry behavior for expired streams.
- Fixed the download service's obsolete restart action so queued downloads can resume when network requirements are restored.

## Lyrics

- Fixed synced lyrics detection when LRC metadata appears before timestamps.
- Added TTML timing support to the standard lyrics screen.
- Fixed auto-follow when opening lyrics mid-song and when lyric text repeats.
- Reset fullscreen lyric tracking and scrolling correctly when the song changes.
- Restored selected-provider priority while retaining parallel provider requests.
- Removed automatic wrong-language YouTube transcript fallback; YouTube subtitles remain available for manual provider selection.
- Refreshed successful lyrics so previously cached mismatches can be replaced.
- Added regression tests for metadata-prefixed LRC and TTML timing.

## Verification

- `testDebugUnitTest`: passed
- `lintDebug`: passed
- `assembleDebug`: passed

## Build

- Version: `0.12.0`
- Version code: `60`
