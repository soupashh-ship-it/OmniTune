# Goal 3 — Offline Playback Cache Routing Validation

## Result

Implemented at source level. Physical device and USB testing were intentionally not run because the user asked to leave testing that requires a connected device disabled.

## Routing contract

| Playback condition | Data source route | Network allowed |
| --- | --- | --- |
| DownloadManager entry is completed and its cache key has a known, contiguous full byte range | Persistent `downloadCache` | No |
| Download is queued, downloading, failed, incomplete, missing, or has unknown total length | Bounded `playbackCache` then signed-stream upstream | Yes, if available |
| Completed entry has partial/missing cache bytes | Removed from cache/index and marked unavailable | No offline claim |

`OfflineDownloadIdentity` maintains the request ID as the library/database identity and propagates the exact custom cache key through enqueue, stream resolution, player routing, stale-cache cleanup, and Media3 download removal. Legacy requests without a custom key use their request ID.

The main player and the crossfade overlap player both obtain their data source from `PlayerFactory.createCacheDataSourceFactory`, so they use the same routing policy.

## Automated evidence

- `./gradlew.bat :app:testDebugUnitTest --tests "com.omnitune.app.playback.OfflinePlaybackCacheRoutingTest"` — passed.
- `OfflinePlaybackCacheRoutingTest` covers completed full-cache routing, partial and unknown-length rejection, non-completed rejection, custom-key preservation, and request-ID fallback.
- `OfflinePlaybackCacheRoutingInstrumentedTest` provides an isolated-cache fixture for cache reopen persistence and deletion. It was compiled only; it was not executed on the USB device.

## Deferred device evidence

Before claiming a device-verified release, run on a disposable profile:

1. Download a track and confirm it remains listed as available after app restart.
2. Enable airplane mode, then play, seek, skip next/previous, and exercise crossfade if enabled.
3. Disable network before first play of a completed download to confirm no upstream fallback.
4. Delete the download and confirm cache bytes, `DownloadManager` entry, Room availability state, and offline playback availability are all removed.

This remains deferred rather than failed; no USB/device command was run for this goal.
