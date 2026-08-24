# MusicService decomposition plan

`MusicService` remains the largest class in the app (~1,600 lines) and accumulates every new
playback concern. This document tracks the coordinator-extraction roadmap. Rules for each step:

1. One extraction per change set; no behavior changes beyond the move.
2. Gate on `:app:compileDebugKotlin`, `testDebugUnitTest`, and lint.
3. Run the disposable-device smoke (`scripts/qa/OmniTuneRuntime.ps1`) before the next release.
4. Respect the thresholds in `docs/architecture/god-object-prevention.md`.

## Already extracted

| Coordinator | Responsibility |
| --- | --- |
| `CrossfadePlaybackCoordinator` | Dual-player crossfade mixing |
| `PlaybackRecoveryCoordinator` | Error recovery / playback restart policy |
| `QueuePersistenceManager` | Debounced queue save + restore payloads |
| `ScrobblingManager` | Last.fm/ListenBrainz submission |
| `PlaybackPreferenceObserver` | Reactive player settings from DataStore |
| `NetworkPlaybackMonitor` | Metered-network playback gating |
| `AutoDownloadOnLikeCoordinator` | Auto-download newly liked songs (**extracted 2026-08**) |

## Remaining extraction queue (ranked)

| # | Candidate | Service lines (approx) | Notes |
| --- | --- | --- | --- |
| 1 | `TasteSignalRecorder` | beginTasteWindow / recordTasteSignalForPreviousTransition / recordListeningEventIfNeeded / startPlaybackTracker | Self-contained state machine over play/pause/transition events; feeds stats + recommendations |
| 2 | `PlaybackHistoryUpdater` | updatePlaybackHistory / resetPlaybackHistory / findHistoryIndex | Pure DB coordination; easy unit tests |
| 3 | `VolumeNormalizationController` | updateVolumeNormalizationFactor + loudness cache | Needs loudness DB reads; keep behind existing StateFlow seam |
| 4 | `EqualizerEffectObserver` | startEqualizerObserver + startBassBoostVirtualizerObserver + decodeEqualizerBands | Mirrors PlaybackPreferenceObserver style |
| 5 | `BluetoothAudioHandler` | handleBluetoothConnected/Disconnected + receiver + AudioDeviceCallback registration | Includes BluetoothDisconnectPolicy already extracted |
| 6 | `AutoplayContinuationManager` | rememberAutoplayCandidate + loopLikedSongsIfNeeded + radio hand-off into existing `RadioQueueManager` | Largest remaining block after #1 |
| 7 | Notification fallback | postMediaNotificationFallback + logMediaControlState → move into `PlaybackNotificationManager` | Small but reduces service surface |

After items 1–6 land, the service should be under the 1,000-line guideline with only lifecycle,
queue command routing, and Player.Listener forwarding left inline.
