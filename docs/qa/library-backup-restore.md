# OmniTune Library Backup & Restore QA

## Scope

Manual library backup now exports a versioned JSON file through Android's Storage Access Framework. Restore uses merge mode by default and does not replace the app database file.

## Included Data

- Liked songs and saved library songs
- Playlists and playlist song order
- Saved artists and saved albums
- Song, album, artist, and playlist metadata needed to rebuild library relationships
- Listening history and recently played events
- Monthly play count/stat records
- Download metadata only, such as track identity and artwork metadata

## Excluded Data

- Actual downloaded audio files
- Stream URLs and extractor cache
- Image, canvas, and temporary caches
- Queue/session-only state
- Secrets, login tokens, API keys, and device-specific file paths

## Format

- File name pattern: `omnitune-library-backup-YYYY-MM-DD-HH-mm.json`
- Format version: `1`
- Root model: `OmniBackupSnapshot`
- Serializer: kotlinx.serialization JSON
- Unknown fields are ignored for forward-compatible reads
- Future backup format versions are rejected instead of partially imported

## Restore Behavior

- Implemented mode: merge
- Existing data is preserved
- Liked/library flags are unioned
- Total play time and play counts never decrease
- Duplicate playlist entries are skipped
- Duplicate history events are skipped by song ID, timestamp, and play time
- Playlist name conflicts create a restored local playlist name
- Restore runs inside a Room transaction

## Persistence Safety

- Room destructive migrations were not present in the active database builder
- Existing explicit migrations remain unchanged
- Room schema export remains enabled
- This feature does not bump the Room schema version

## Android Auto Backup

Android Auto Backup remains disabled in this pass. OmniTune stores account/session preferences and cache-heavy data in app storage, so enabling platform backup safely requires a separate rules audit. Manual JSON export/import is the supported portable backup path.

## Manual QA

Not run in this pass until a connected Android device/emulator is available.

Required device checks:

- App launches
- Settings opens
- Backup & Restore screen opens
- Export opens the system create-document picker
- Backup JSON is created and valid
- Import opens the system document picker
- Import confirmation appears
- Import succeeds in merge mode
- Liked songs restore
- Playlists and playlist order restore
- Saved artists and albums restore
- History and stats restore
- Existing data is not deleted
- Duplicate import does not create duplicate playlist entries
- Playback, search, queue, downloads, and dynamic song colors still work

## Known Limitations

- Replace mode is intentionally deferred
- Actual downloaded audio files are not exported or restored
- Android Auto Backup remains disabled pending a dedicated privacy/cache rules pass
