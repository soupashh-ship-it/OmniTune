## OmniTune 0.10.8

This release adds a reliable Library Backup & Restore system designed to protect user libraries across app updates, reinstalls, and device moves.

### New

- Added Settings -> Backup & Restore with Android system file picker support.
- Added versioned JSON library backups for liked songs, saved library songs, playlists, playlist order, saved artists, saved albums, playlist tags, listening history, and play statistics.
- Added optional full ZIP backups that include app-managed offline audio and the Media3 download index.
- Added merge restore mode so existing data is preserved by default.
- Added advanced replace restore mode with explicit confirmation for rebuilding the local library from a backup.
- Added Android Auto Backup rules for safe backup of the library database and Media3 download index.

### Improved

- Restores now run transactionally to avoid partially applied library data.
- Duplicate playlist entries and history records are skipped during merge restores.
- Play counts and total play time are merged without reducing existing stats.
- Offline audio restore is staged and applied safely on next app start to avoid corrupting active playback/download state.
- Backup summaries now show restored/exported counts directly in Settings.

### Safety

- Cache files, stream URLs, temporary resolver data, secrets, API keys, login tokens, and device-specific paths are excluded from manual backups.
- Android Auto Backup uses restrictive include-only rules to avoid backing up private preferences or cache-heavy data.
- Existing playback, search, queue, downloads, dynamic colors, and settings behavior are preserved.
