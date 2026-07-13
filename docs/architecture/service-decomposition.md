# MusicService Decomposition

## Why MusicService was decomposed
Historically, `MusicService` was a god object that handled everything from playback synchronization to UI notification states, database persistence, radio queues, lyrics prefetching, and network monitoring. This made it brittle, hard to test, and prone to breaking core audio playback when unrelated features were modified.

## Current MusicService Role
The `MusicService` is strictly limited to being a high-level component. Its current roles are:
* Lifecycle coordinator (managing the Android Service bounds)
* Player listener coordinator
* Service entry point
* High-level wiring and dependency injection assembly

## Extracted Collaborators
The following collaborators handle the specific domains previously tangled within `MusicService`:

* **PlayerFactory**: Assembles the ExoPlayer/Media3 instance. Must not own persistence.
* **PlaybackNotificationManager**: Manages the foreground notification. Must not contain playback logic.
* **SessionManager**: MediaSession lifecycle and connection.
* **LyricsPrefetcher**: Caches lyrics for upcoming tracks. Must not delay or block playback transitions.
* **NetworkPlaybackMonitor**: Observes Wi-Fi/Mobile data and pauses/adjusts quality. Must not handle user preferences directly.
* **QueuePersistenceManager**: Restores/saves the queue to the database. Must not modify the active playback state directly.
* **PlaybackEventRecorder**: Records listening history and play counts.
* **PlaybackPreferenceObserver**: Observes DataStore preferences (e.g. skip silence). Must not own ExoPlayer instances.
* **EqualizerController**: Manages AudioFx processing.
* **CrossfadePlaybackCoordinator**: Controls volume ramping during track transitions.
* **RadioQueueManager**: Fetches autoplay/radio items when the queue ends.
* **PlaybackRecoveryCoordinator**: Restores playback position and state after a crash or force-close.

### Required Runtime Checks
After modifying *any* of the above collaborators, the following must be verified:
* **Playback**: Play/pause, next/previous, and track transitions.
* **Background**: App resumes correctly and foreground notification stays active.

## Forbidden Future Changes
* ❌ Adding new unrelated responsibility directly into `MusicService`.
* ❌ Adding network logic directly into `MusicService`.
* ❌ Adding lyrics fetch logic directly into `MusicService`.
* ❌ Adding notification construction directly into `MusicService`.
* ❌ Adding queue persistence directly into `MusicService`.
* ❌ Adding preference DataStore collectors directly into `MusicService`.