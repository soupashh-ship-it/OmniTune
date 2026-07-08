## OmniTune 0.10.9

This release strengthens playback continuation so autoplay remains honest, persistent, and safer across app restarts.

### Fixed

- Restored playback source context now survives persistent queue restore instead of falling back to a generic queue after process recreation.
- Autoplay can now resume with saved source information such as queue source, seed song, artist, verified mood, verified genre, autoplay permission, and shuffled collection state.
- Verified mood and genre continuation no longer stays dormant when OmniTune has reliable source context available.

### Improved

- Added optional genre and mood metadata fields to the playback model for trusted source metadata.
- Explicit mood collections can now guide autoplay through the verified mood/tag fallback branch.
- Provider-backed genre collections can now guide autoplay through the verified genre fallback branch.
- Generic search results, generic Quick Picks, and local library playback still avoid claiming genre/mood when none is actually available.
- Added a safe Room migration for the persistent queue context fields.

### Quality

- Added tests for queue playback context restore, verified mood/genre seed propagation, candidate filtering, autoplay priority, retry limits, and Liked Songs queue behavior.
- Preserved manual queue priority, Play Next, Add to Queue, Liked Songs looping, dynamic song colors, backup/restore, downloads, and search playback behavior.

