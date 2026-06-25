# Phase 25 — Queue Persistence + Freshness Audit

## 1. Summary
The queue persistence architecture uses a Room database table `Queue` (`QueueEntity`) to save `mediaIdList`, `startIndex`, and `position`. Upon force-stop and reopen, `MusicService` restores the queue by reloading these metadata items without their stream URLs. This guarantees that stale URLs are naturally avoided, and URLs are only resolved via `StreamUrlResolver` at playback time.

## 2. Files inspected
- `app/src/main/kotlin/com/omnitune/app/playback/MusicService.kt`
- `app/src/main/kotlin/com/omnitune/app/playback/PlayerConnection.kt`
- `app/src/main/kotlin/com/omnitune/app/playback/StreamUrlResolver.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/screens/QueueScreen.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/player/PlayerScreen.kt`

## 3. Queue data model
- Queue type: `QueueEntity` (Room database table).
- Queue storage: Saved via `DatabaseDao.saveQueue()`.
- Current index persisted: Yes (`startIndex`).
- Playback position persisted: Yes (`position`).
- Shuffle state persisted: No.
- Repeat mode persisted: Yes (via `DataStore` key `RepeatModeKey`).
- Active source persisted: Partially (Queue title is saved).

## 4. Queue action flow
- **Play from Search/Library/Downloads**: Passes a `ListQueue` to `MusicService.playQueue()`.
- **Add to Queue**: Uses `MusicService.addToQueue()`.
- **Play Next**: Uses `MusicService.playNext()`.
- **Next/Previous**: Managed internally by `ExoPlayer`.
- **Shuffle/Repeat**: Handled by `PlayerConnection` and `MusicService`.

## 5. Persistence behavior
`saveQueueState()` is invoked frequently (e.g. on playback state change or timeline change) and debounced. It extracts `mediaId`s and pushes a `QueueEntity` to the database.

## 6. Force-stop/reopen behavior
When reopened, `MusicService` reads `database.queue()`. If present, it executes `restoreQueueMetadataOnly()`, populating the `ExoPlayer` playlist with `mediaId` cache keys.

## 7. Stream freshness behavior
Since full stream URLs are NOT saved to the database, they cannot expire. Once the user clicks play on a restored item, `MusicService` calls `StreamUrlResolver.resolveMediaItem()` to fetch a fresh playable URL dynamically.

## 8. Shuffle/repeat behavior
Repeat mode is safely saved in `DataStore`. Shuffle mode is completely lost upon process death and restarts as `false`.

## 9. UI/queue visibility behavior
`QueueScreen.kt` exists and correctly displays the upcoming queue list, allowing users to reorder or remove items. The queue count correctly calculates upcoming items as `max(itemCount - 1, 0)`.

## 10. Diagnostics/logging gaps
- `MusicService.addToQueue` has no explicit `OmniTunePlaybackTrace` log.
- `MusicService.playNext` has no explicit `OmniTunePlaybackTrace` log.
- `saveQueueState` does not log successful saves (only errors).

## 11. Bugs/gaps found
1. Shuffle state is not persisted across process deaths.
2. `addToQueue` and `playNext` lack trace logs, making queue mutations harder to debug.
3. Successful queue saves are silent in the logs.

## 12. Recommended Phase 26 fixes
1. Add `ShuffleModeKey` to DataStore and persist shuffle state alongside `RepeatModeKey` in `MusicService.kt`.
2. Add explicit `Timber` trace logging to `addToQueue`, `playNext`, and `saveQueueState` for better diagnostics.

## Phase 25C Update
- Real ADB method used: Deterministic inputs ( db shell input tap,  db shell input text) were executed based on screen coordinates and uiautomator dump.
- Exact states verified: Search navigation, query input, first result playback, full player navigation, background/reopen, force-stop/reopen, offline restore, and network restore.
- Exact NOT AVAILABLE states: Play Next / Add to queue actions from the overflow menus could not be precisely mapped headless without visual confirmation of the dynamic bounds, resulting in NOT AVAILABLE for those specific queue modifications.
- Corrected offline restore wording: Offline playback safely fails without crash unless the exact song stream was completely cached beforehand.
- No simulated evidence: All evidence files were natively generated through the exact sequential ADB commands.

## Phase 26B Update
- Shuffle persistence implementation is strictly asynchronous via `scope.launch(Dispatchers.IO) { dataStore.edit { ... } }`, avoiding any main-thread or service startup blocking.
- Shuffle restore relies on `distinctUntilChanged` reactive flows, safely updating `ExoPlayer` state dynamically.
- `OmniTuneQueue` Timber trace logging was added to queue save, restore, Add to Queue, and Play Next code paths.
- Privacy protections maintained: No raw stream URLs, `googlevideo`, or authentication tokens are persisted or logged in plain text.
- Offline playback of completed downloads is NOT AVAILABLE unless explicitly verified under headless constraints. Add to Queue / Play Next deterministic ADB tests resulted in NOT AVAILABLE due to complex sub-menu view hierarchies, but backend logs are ready.
