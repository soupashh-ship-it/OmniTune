# OmniTune v0.11.1

This release refines the playlist detail experience and adds practical playlist and album actions directly from search, while keeping every visible action connected to real app behavior.

## Fixes & Improvements

- Added playlist detail metadata chips for song count and total duration.
- Refined playlist detail controls with working delete, play, shuffle, download, and edit actions.
- Added a full-width Add/Search songs action on playlist detail for faster playlist building.
- Improved the playlist song overflow sheet with real actions for play next, queue, add to playlist, like, library save/remove, playlist removal, download management, artist/album navigation, details, share, and radio.
- Added album download actions from search album results, queuing every track through the existing download pipeline.
- Added playlist save and playlist download actions from search playlist results.
- Saved provider playlists now appear in Library playlists with preserved song order and duplicate-safe inserts.
- Kept unavailable actions hidden instead of showing controls that do not perform real work.

## Verification

- `clean assembleDebug`: passed
- `testDebugUnitTest`: passed
- `lintDebug`: passed

## Build

- Version: `0.11.1`
- Version code: `51`
