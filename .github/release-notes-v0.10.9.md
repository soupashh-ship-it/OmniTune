## OmniTune 0.10.9

This release strengthens playback continuation so autoplay remains honest, persistent, and safer across app restarts.

### Fixed

- Restored playback source context now survives persistent queue restore instead of falling back to a generic queue after process recreation.
- Autoplay can now resume with saved source information such as queue source, seed song, artist, verified mood, verified genre, autoplay permission, and shuffled collection state.
- Verified mood and genre continuation no longer stays dormant when OmniTune has reliable source context available.
- Fixed the "Year in Music" listening-time total showing inflated values by formatting milliseconds correctly as minutes/hours.
- Fixed yearly listening totals so they use all listen events for the selected year instead of only the top songs list.
- Fixed listening history recording so future stats use actual listened time instead of recording the full track duration as soon as playback starts.

### Improved

- Added optional genre and mood metadata fields to the playback model for trusted source metadata.
- Explicit mood collections can now guide autoplay through the verified mood/tag fallback branch.
- Provider-backed genre collections can now guide autoplay through the verified genre fallback branch.
- Generic search results, generic Quick Picks, and local library playback still avoid claiming genre/mood when none is actually available.
- Added a safe Room migration for the persistent queue context fields.
- Improved Home mood and genre category results with category-specific query profiles, relevance scoring, duplicate filtering, and safer fallbacks for Chill, Gaming, Workout, Focus, Party, Romantic, Sad, and related categories.

### Quality

- Added tests for queue playback context restore, verified mood/genre seed propagation, candidate filtering, autoplay priority, retry limits, Liked Songs queue behavior, Home category relevance, and Year in Music time formatting.
- Preserved manual queue priority, Play Next, Add to Queue, Liked Songs looping, dynamic song colors, backup/restore, downloads, and search playback behavior.
