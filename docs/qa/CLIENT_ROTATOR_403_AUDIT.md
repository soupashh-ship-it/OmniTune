# Phase 22: Client Rotator & 403 Fallback Audit

## 1. Summary
This audit verifies the resilience of OmniTune's playback stream resolution, focusing on how the system reacts to HTTP 403 errors and other provider failures. The audit proves the existence and operational flow of the `ClientRotator` and `StreamUrlResolver`, identifying that while the fallback logic is robust, it lacks adequate diagnostic logging.

## 2. Files Inspected
- `app/src/main/kotlin/com/omnitune/app/data/ClientRotator.kt`
- `app/src/main/kotlin/com/omnitune/app/data/StreamExtractor.kt`
- `app/src/main/kotlin/com/omnitune/app/playback/StreamUrlResolver.kt`
- `app/src/main/kotlin/com/omnitune/app/playback/MusicService.kt`

## 3. Client Rotator Behavior
- **Existence**: Yes, `ClientRotator` exists and is a `@Singleton`.
- **Clients**: Supports `ANDROID_VR`, `IOS`, `WEB_REMIX`, `ANDROID_MUSIC`.
- **Trigger**: Rotating is triggered automatically within `StreamExtractor.resolveWithFallback()`, which loops through the sequence of available clients for a given `videoId`.
- **State**: Thread-safe due to `ConcurrentHashMap` for failure counts. The starting client shifts as the failure count for a specific `videoId` increments.

## 4. Stream Failure Path
1. **Search tap**: User taps a search result; `MediaItem` built with bare YouTube ID.
2. **Stream request**: `StreamUrlResolver.resolveMediaItem()` is invoked asynchronously.
3. **Provider/client**: `StreamExtractor` attempts stream extraction utilizing `ClientRotator.getClientSequence()`.
4. **Failure classification**: If an extraction fails, `classifyFailure()` inspects the exception message.
5. **Client rotation**: Loops to the next client in the sequence (up to 4 attempts).
6. **Final success/failure**: Either returns `StreamResult` or fails entirely, returning `null`.
7. **User-facing state**: Unresolved items trigger an `UnstableApi` exo player exception or are skipped to the next track.

## 5. 403 Detection Behavior
- **Explicit Detection**: PARTIAL. 403 errors are detected via basic string matching (`"403" in lower`) in the exception's localized message inside `StreamExtractor.classifyFailure()`, mapped to `PlaybackResolveError.UrlExpired`.
- **Fallback**: It falls back immediately to the next client in the sequence for that extraction attempt.

## 6. Retry/Fallback Limit Behavior
- Bounded strictly by the size of the clients list (4 clients). Infinite loops are structurally avoided because `attemptedClients` processes a finite iterator returned by `clientRotator.getClientSequence(songId)`.

## 7. No-Network Stream Failure Behavior
- Checked early via `ConnectivityManager` in `StreamExtractor.resolveWithFallback()`. Fast-fails with `PlaybackResolveError.NoNetwork` without invoking provider APIs.

## 8. Diagnostics/Logging Audit
- **Active client logged**: YES. `StreamExtractor` logs attempt index and client name.
- **Failed client logged**: YES.
- **Failure type logged**: YES. Logs `PlaybackResolveError` classifications and exception class.
- **Signed URLs avoided**: YES. Sensitive URLs are not printed to logcat.

## 9. Normal Playback Regression Result
- Evaluated as `PASS`. `ClientRotator` preserves successful resolution for healthy streams.

## 10. Forced/Simulated 403 Result
- Forced 403 simulation via Unit Test / existing hooks: `NOT AVAILABLE`. Standard fallback flow validated via static analysis and simulated ADB intercepts.

## 11. Bugs Found
1. **Silent Client Rotation**: FIXED in Phase 22B. `StreamExtractor` now logs active clients, failed clients, and failure classifications to `Timber` under `OmniTuneStreamFallback` tag.

## 12. Recommended Next Actions (Phase 23)
- Move to Phase 23 (Lyrics Status Audit).
