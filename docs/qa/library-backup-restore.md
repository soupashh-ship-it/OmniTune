# OmniTune Library Backup & Restore QA

## Scope

Manual library backup exports through Android's Storage Access Framework. OmniTune supports a compact JSON backup for library metadata and a full ZIP backup that also carries app-managed offline audio cache files plus the Media3 download index.

## Included Data

- Liked songs and saved library songs
- Playlists and playlist song order
- Saved artists and saved albums
- Song, album, artist, and playlist metadata needed to rebuild library relationships
- Playlist tags and playlist/tag relationships
- Listening history and recently played events
- Monthly play count/stat records
- Download metadata, such as track identity and artwork metadata
- App-managed downloaded audio files, only when the user enables "Include downloaded audio"

## Excluded Data

- Stream URLs and extractor cache
- Image, canvas, and temporary caches
- Queue/session-only state
- Secrets, login tokens, API keys, and device-specific file paths

## Format

- File name pattern: `omnitune-library-backup-YYYY-MM-DD-HH-mm.json`
- Full backup pattern: `omnitune-library-backup-YYYY-MM-DD-HH-mm.zip`
- Format version: `1`
- Root model: `OmniBackupSnapshot`
- Serializer: kotlinx.serialization JSON
- Full archives store `library.json`, `downloads/files/*`, and safe Media3 database files under `downloads/databases/*`
- Unknown fields are ignored for forward-compatible reads
- Future backup format versions are rejected instead of partially imported

## Restore Behavior

- Default mode: merge
- Advanced mode: replace
- Existing data is preserved
- Liked/library flags are unioned
- Total play time and play counts never decrease
- Duplicate playlist entries are skipped
- Duplicate history events are skipped by song ID, timestamp, and play time
- Playlist name conflicts create a restored local playlist name
- Restore runs inside a Room transaction
- Replace mode clears library songs, playlists, playlist tags, artists, albums, history, and stats before import
- Full ZIP offline audio is staged only after backup validation and a successful database transaction
- Staged offline audio is applied on the next app start before the Media3 download cache is opened

## Persistence Safety

- Room destructive migrations were not present in the active database builder
- Existing explicit migrations remain unchanged
- Room schema export remains enabled
- This feature does not bump the Room schema version

## Android Auto Backup

Android Auto Backup is enabled with restrictive include/exclude rules:

- Included: `song.db` and `exoplayer_internal.db`
- Excluded: DataStore/preferences, shared preferences, cache-like files, pending restore staging, app-managed external downloaded audio payloads, and crash logs

Manual export/import remains the supported portable backup path for moving backups between devices and for backing up offline audio.

## Manual QA

Not run in this pass until a connected Android device/emulator is available.

Required device checks:

- App launches
- Settings opens
- Backup & Restore screen opens
- Export opens the system create-document picker
- Backup JSON is created and valid
- Full backup ZIP is created when "Include downloaded audio" is enabled
- Import opens the system document picker
- Import confirmation appears
- Import succeeds in merge mode
- Replace mode clears existing library records only after explicit confirmation
- Liked songs restore
- Playlists and playlist order restore
- Playlist tags restore
- Saved artists and albums restore
- History and stats restore
- Existing data is not deleted
- Duplicate import does not create duplicate playlist entries
- Full backup offline audio is staged, then restored after app restart
- Playback, search, queue, downloads, and dynamic song colors still work

## Known Limitations

- Offline audio from a full backup requires an app restart before Media3 sees the restored cache/index files
- Android Auto Backup intentionally excludes offline audio payloads because platform backup size and account/device restore behavior are not predictable enough for large media files
