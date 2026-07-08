# Playlist Remaster QA

## Summary

OmniTune playlists now use the existing Room playlist model with a richer list, detail, add/search, suggestion, playback, download, and backup-aware flow. Velune was used as a behavioral reference for playlist affordances only; OmniTune keeps its package names, database, playback queue model, OmniGlass styling, and existing assets.

## Current behavior before fix

- The playlist list was mostly a plain library list with limited artwork context and a basic create dialog.
- Creating a playlist inserted it but did not consistently move the user into the new playlist detail flow.
- Playlist detail had rename, delete, tags, remove, multi-select, and drag reorder foundations, but the main play behavior and add/search experience were incomplete.
- Row taps could start a single song instead of planning a full playlist queue from the selected row.
- `resetAndLoadPlaylistSuggestions()` was effectively a stub, so the "You might like" area was not a reliable real-suggestion flow.
- Playlist backup models already existed and contained playlist-song positions, but tests did not explicitly guard ordered restore/export behavior.

## Velune reference behavior used

- Playlist pages present a clear cover identity, metadata, action row, ordered song list, add/search path, and suggestions.
- Playlist add flow keeps the user in context, shows results with Add controls, and confirms adds with a toast/snackbar.
- Playlist download actions reflect Media3 download state and queue real downloads instead of showing inert controls.
- Shuffle and row playback operate on the whole playlist rather than a single song.

No Velune names, branding, assets, or source code were copied.

## Data model

- No Room schema migration was required.
- `PlaylistEntity` already supports stable ID, name, browse ID, timestamps, editable/sync flags, custom order name, and optional thumbnail URL.
- `PlaylistSongMap` already supports `playlistId`, `songId`, `position`, generated row ID, optional `setVideoId`, and cascade delete when a playlist is deleted.
- Song metadata remains stored in the existing `SongEntity` and relation tables through `insert(song.toMediaMetadata())`.
- Duplicate handling is intentional: same song ID is prevented in the same playlist by checking `playlistDuplicates()` before insert.

## UI changes

- Playlist list rows now show a 1-4 artwork collage/cover, playlist name, song count, and last-updated date when present.
- Create playlist validates non-blank trimmed names and an 80-character limit, then navigates to the created playlist.
- Playlist detail keeps real rename, delete, tag assignment, remove, multi-select, and long-press reorder behavior.
- Playlist detail now shows passive metadata pills, collage artwork, Play All, Shuffle, Add, and Download playlist actions when songs exist.
- Empty playlist detail shows an Add songs CTA and real suggestions when available.
- The add-song flow is a dedicated route: `playlist/{playlistId}/add`.

## Playlist search/add

- The add screen uses `YouTube.search(query, FILTER_SONG)`.
- Each result has a real Add button.
- Add inserts local song metadata, appends a `PlaylistSongMap` at `max(position) + 1`, updates playlist `lastUpdateTime`, and leaves the user on the add screen.
- Duplicate adds show `Already in playlist` and do not append another row.

## Suggestions strategy

- `PlaylistSuggestionQueryBuilder` builds query candidates from playlist name, mood/genre keywords, top artists already in the playlist, and safe discovery fallbacks.
- `PlaylistDetailViewModel` loads real song suggestions with `FILTER_SONG`, filters existing playlist songs and previously suggested IDs, and supports refresh/new pages.
- Suggestions are hidden when no real suggestions are loaded.

## Playback behavior

- `PlaylistPlaybackPlanner` creates ordered or shuffled playlist queue plans.
- Play All queues all playlist songs in saved `position` order.
- Tapping a row queues the whole playlist and starts at that row.
- Shuffle queues every playlist song once and avoids a no-op shuffle when possible.
- Playlist queues include `PlaybackContext(sourceType = PLAYLIST, sourceId, sourceTitle, allowAutoplay = true, sessionItems)`.
- Existing manual queue and Play Next behavior remains owned by `PlayerConnection`.

## Download behavior

- Playlist detail uses the existing `DownloadsViewModel.startDownload()` pipeline.
- Download playlist queues missing, non-active songs through the existing Media3 download service.
- The button reflects completed/downloading states at a playlist level and is disabled once every playlist song is downloaded.

## Backup/restore behavior

- Existing backup DTOs are preserved: `BackupPlaylist`, `BackupPlaylistSong`, and `BackupPlaylistTag`.
- Existing export includes `backupPlaylistSongMaps()`.
- Existing restore sorts playlist entries by `playlistId` then `position` before inserting `PlaylistSongMap` rows.
- Tests now assert that playlist-song order and positions survive backup model serialization.

## Tests

- `PlaylistPlaybackPlannerTest`
  - ordered playback starts from the selected playlist row
  - shuffle includes every song exactly once and avoids no-op ordering when possible
- `PlaylistSuggestionQueryBuilderTest`
  - query builder uses playlist name/artists and excludes existing context appropriately
- `OmniBackupModelsTest`
  - backup serialization includes playlist song entries with preserved positions

## Verification

- `.\gradlew.bat assembleDebug`: PASS
- `.\gradlew.bat clean assembleDebug`: PASS
- `.\gradlew.bat testDebugUnitTest`: PASS
- `.\gradlew.bat lintDebug`: PASS
- `adb devices`: PASS, no attached devices listed
- `.\gradlew.bat installDebug`: NOT RUN, no attached Android device or emulator
- Runtime/manual device QA: NOT RUN, no attached Android device or emulator

## Manual QA checklist

- Create playlist, restart app, confirm it persists: not run, no device attached
- Add songs from playlist search, confirm count, collage, and order update: not run, no device attached
- Attempt duplicate add, confirm it is reported and not appended: not run, no device attached
- Play playlist and tap a middle row, confirm full queue continuation: not run, no device attached
- Shuffle playlist, confirm every song appears once: not run, no device attached
- Rename and delete playlist, confirm mappings only are removed: not run, no device attached
- Download playlist, confirm missing songs are queued: not run, no device attached
- Export/import backup, confirm playlist order: build/unit covered, device flow not run

## Known limitations

- Total playlist duration is not shown yet because the current playlist relation does not expose duration in the list-level `Playlist` model and many provider songs can have missing duration.
- Runtime QA requires a connected Android device or emulator.
- Playlist download queues missing songs but does not yet provide a per-song progress dashboard inside playlist detail; detailed state remains in the existing Downloads surface.
