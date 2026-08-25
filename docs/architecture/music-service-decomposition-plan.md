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
| `TasteSignalRecorder` | Listening-window state machine + listening-event persistence (**2026-08**) |
| `PlayCountTracker` | Threshold play-count increment + scrobble hand-off (**2026-08**) |
| `PlaybackHistoryTracker` | Previous-track history state holder (unit tested) (**2026-08**) |
| `EqualizerEffectObserver` | Equalizer/bass-boost/virtualizer preference application (**2026-08**) |
| `BluetoothAudioHandler` | BT receiver + audio-device callbacks, auto-start/pause policies (**2026-08**) |
| `AutoplayContinuationManager` | Autoplay state (recent/failed/seed), candidate selection loop (**2026-08**) |

Note: notification fallback logic already lived in `PlaybackNotificationManager`; the service only keeps thin initialized-guards. Liked-songs looping stays inline in the service deliberately: it is pure player mutation (`setMediaItems`/`prepare`) tied to `withOriginalVideoIdUri`, ~24 lines.

## Remaining extraction queue

After the 2026-08 pass, the remaining candidates are done or retired:

| # | Candidate | Status |
| --- | --- | --- |
| 1 | `VolumeNormalizationController` | Done (2026-08): loudness lookup + factor StateFlow extracted; enable switch stays with preference plumbing. `PlaybackPreferenceObserver` now takes a read-only `StateFlow<Float>` for the factor. |
| 2 | Queue persistence inline block → reuse existing `QueuePersistenceManager` | Done (2026-08): manager upgraded to persist full playback context behind the persistent-queue preference gate and debounced/immediate flush modes; service delegates all queue saves to it. Its inline restore path remains (richer: explicit-content filter + context mapper). |

The service is now below the god-object threshold trajectory; verify against
`scripts/check-large-files.ps1` after each future feature addition.
