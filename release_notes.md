# OmniTune v0.5.1

## Fixes & Improvements
* **MiniPlayer Overhaul**: Replaced the performance-heavy circular audio visualizer with a live-synced lyrics display.
* **Persistent Queue via Room**: The playback queue is now persistently saved and seamlessly restored on app launch.
* **Copyright Notice Restored**: Restored the missing copyright attribution headers to CrossfadeAudio.kt.
* **SleepTimer Memory Leak**: Fixed an issue where an orphaned coroutine scope could cause the app to ANR or crash by leaking SleepTimer.
* **Stale YouTube URLs**: Introduced an LruCache with a 4-hour eviction timestamp for signed YouTube streams, eliminating unexpected HTTP 403 errors on resume.
* **Race Conditions Fixed**: Fixed a race condition with 	oggleLike() where DB writes were not properly awaited before UI optimistic updates.
* **Background Playback Tracking**: Implemented background tracking to save fully played songs to the database for playback history.
* **Material You Support**: Integrated dynamic color theming logic via DynamicThemeKey.
* **MusicService Encapsulation**: Refactored internal ExoPlayer states and MutableStateFlows to be cleanly encapsulated.
