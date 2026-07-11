# OmniTune v0.12.2

This release improves offline metadata, Library reliability, and automatic lyrics in the fullscreen player.

## Downloads

- Downloaded songs now retain and display their thumbnail, title, artists, and album metadata.
- The Downloads screen now shows stored artwork and metadata instead of generic entries.
- Offline playback now uses the complete stored song metadata.

## Library

- Fixed saved songs, artists, and albums not appearing or opening correctly.
- Added automatic repair for missing song, album, and artist relationships in existing libraries.
- Album and artist pages now show local content immediately and remain usable offline or when network loading fails.
- Playing a song from Library, an album, or an artist now queues the complete visible collection at the selected song.
- Added working Shuffle and Radio actions to artist pages.

## Lyrics

- Lyrics now load automatically in the fullscreen player without opening the Lyrics sheet first.
- Cached lyrics are displayed immediately without another provider request.
- Lyrics for the next three queued songs are prefetched using the selected provider.
- Prevented lyrics from a previous song replacing the current song's lyrics.

## Verification

- 72 unit tests passed.
- Android instrumentation test sources compiled.
- `lintDebug` passed.
- `assembleDebug` passed.

## Build

- Version: `0.12.2`
- Version code: `62`
