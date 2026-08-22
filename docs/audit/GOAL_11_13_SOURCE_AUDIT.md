# Goals 11–13 source audit

Status: source audit completed on 2026-07-28. No device, USB, emulator, or test-suite execution was performed at the user's request.

## Goal 11 — Playback notification only

OmniTune has one notification capability: the foreground playback notification and its Android system media controls. It has no new-music alert, recommendation-alert, or in-app-message notification backend. Settings therefore use **Playback notification** wording and link directly to Android's notification settings.

The path is:

`MainActivity` requests Android 13+ `POST_NOTIFICATIONS` → `MusicService` owns the single Media3 player and `MediaLibrarySession` → `PlaybackNotificationManager` creates the low-importance public channel and Media3 provider → player listeners refresh widget/notification metadata on state, playing-state, media-item, and metadata changes.

The fallback notification exposes Previous, Play/Pause, Next, Like, Repeat, and Stop. Stop pauses, clears the queue, and stops the service. Active playback is ongoing; a paused notification is dismissible by Android. There is no second player or notification-specific player state.

Source-level coverage:

- `PlaybackNotificationContractTest`: title/artist fallback, transport order, play/pause switch, and Stop action contract.
- `MusicSessionCallbackInstrumentedTest`: Media3 controller/session transport and custom-command propagation.

Physical-device checklist (not run):

| Area | Required check |
| --- | --- |
| Metadata | Artwork, title, artist, track and queue transitions update immediately |
| Controls | Play/Pause, Previous, Next, Stop, Dismiss; app UI follows each action |
| Lifecycle | Service restart, process death, task removal, locked screen |
| Platform | Android 13+ permission denied/granted, Vivo/iQOO battery fallback, Bluetooth, headphone unplug, audio focus, Android Auto where available |

## Goal 12 — Reliability hardening

- Queue setup remains unresolved until the selected item is resolved; `MusicService.playQueue` only prepares after a current-item identity check.
- `StreamResolutionTarget` now identifies the exact media ID and queue index authorized to receive a late resolution. Network-change and error-recovery jobs are cancelled before a replacement starts and discard stale completions.
- Retry policy remains bounded (`PlaybackRecoveryPolicy`), and network availability accepts `NET_CAPABILITY_INTERNET` without falsely requiring validation. Healthy streams are left alone during Wi-Fi/mobile/VPN handover; only a stream that actually failed while offline retries once connectivity returns, on the player's main thread. The UI surfaces actionable offline/network messages.
- `PlayerFactory` owns audio focus and noisy-headset handling; completed downloads are routed before streams. Bluetooth A2DP/ACL and audio-device removal now pause only active playback whose last Bluetooth output disappeared, avoiding accidental speaker playback during disconnects or a Bluetooth handover.
- Existing instrumented migration tests cover every Room version 1–7 and now seed/assert songs, artists, albums, likes, playlist ordering, search history, lyrics, formats, events, play counts, skips, related-song maps, tags, set-video IDs, download state, and legacy queue data where that source version supports it. Startup relation repair and aggregate orphan checks use a single process-scoped background executor, so they neither run on the caller thread nor create a worker for every database open. The checks do not expose IDs or delete data. `SchemaTools` now makes a retained on-disk main/WAL/SHM safety copy before any repair and fails repair if post-repair foreign-key validation fails.

`SchemaToolsRepairInstrumentedTest` now supplies physical-file fixtures for missing tables, missing indices, partial schemas, interrupted/unsupported upgrades, invalid foreign keys, and corrupt bytes; each asserts an on-disk safety copy before a successful repair or fail-closed outcome. A successful repair explicitly resets SQLite's `user_version` to the current Room schema. Device/network migration matrices still require execution before release: Wi-Fi/mobile/VPN/DNS/WARP transitions; provider rate-limit/parser/region/server failures; every supported real migration fixture; corrupted/missing-table/missing-index/invalid-FK repair fixtures.

## Goal 13 — Polling and diagnostics privacy

- Download state now comes from `DownloadManager.Listener`; the redundant 300 ms view-model loop and 500 ms download-flow loop were removed.
- The obsolete, unused `MusicService` connectivity-observer instance was removed; `NetworkPlaybackMonitor` is the only playback-owned network callback and is released with the service. The separate lyric availability helper has no collectors, so it is now a synchronous checker rather than a process-lifetime callback.
- Sleep-timer state is service-owned `StateFlow`; `PlayerConnection` no longer polls it.
- The home view model no longer schedules background 30-minute quick-pick hydration. Provider refresh is explicitly initiated by screen lifecycle/user flows.
- Time-progress UI polling for seek bars, lyrics, crossfade, and active playback thresholds remains intentional and is lifecycle/coroutine-scoped. `BluetoothDisconnectPolicyTest` preserves the disconnect guard independently of Android routing APIs.
- Diagnostic exports redact provider URLs, headers/cookies, tokens, sessions, identifiers, and query text; cap output at 200 lines / 48 KiB; retain only the current cache report; and expose a user-controlled deletion action.
- Crash snapshots retain only the exception class and capped frame locations. Raw exception messages and external raw crash files are no longer written. Production exception logging omits throwable messages.

The changed and pre-existing regression tests were not run in this pass. Compile-only validation was used throughout, including the JVM and Android test source sets. Actionable migration-signature, deprecated-notification, Hilt package-move, coroutine-opt-in, redundant-cast, safe-call, and redundant-conversion warnings were corrected. Media3 1.9.2 remains explicitly annotated at each unstable call site; no dependency upgrade was made. The remaining compatibility suppressions are deliberate: Android `Virtualizer` has no equivalent for the existing user setting, `AudioProcessor.isEnded` is required by the current Media3 interface, the debug-only clipboard screen requires a suspend-callback redesign, and Queue's swipe veto requires an anchored-draggable redesign. The physical checklist remains required before declaring these goals release-verified.
