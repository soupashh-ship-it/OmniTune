# Phase 31 — Wi-Fi Playback Startup Fix Report

## Issue
When OmniTune is used on Wi-Fi, starting a song can take forever or may never start. On mobile data, playback starts normally. This was caused by an infinite hang when attempting to resolve or buffer streams over a Wi-Fi connection that was connected to the router but lacked a validated internet connection (e.g., captive portal or IPv6 blackhole). 

## Root Cause
1. **Network Connectivity Validation:** `NetworkConnectivityObserver` incorrectly assumed Wi-Fi was fully online the moment it connected (`onAvailable`), ignoring the `NET_CAPABILITY_VALIDATED` flag. This caused `isInternetAvailable()` to return `true` on dead Wi-Fi networks, sending the app into a resolution loop instead of fast-failing.
2. **Resolver Timeout Missing:** `StreamUrlResolver.resolveMediaItem` lacked a bounded timeout around `streamExtractor.extract()`. This allowed the underlying OkHttp/Ktor client (which has a 45s/30s timeout by default) to stall UI and playback queues for a very long time.
3. **Player Buffering Watchdog Missing:** If the URL resolved but ExoPlayer failed to load the chunks over a slow/blackholed Wi-Fi, it would spin forever in `STATE_BUFFERING` because ExoPlayer's default load policy retries extensively without a hard time limit.

## Fixes Applied
1. **`app/src/main/kotlin/com/omnitune/app/utils/NetworkConnectivityObserver.kt`**: 
   - Overrode `onCapabilitiesChanged` to check for `NetworkCapabilities.NET_CAPABILITY_VALIDATED` on API 23+. The app now only considers Wi-Fi online if it actually has validated internet access.
2. **`app/src/main/kotlin/com/omnitune/app/playback/StreamUrlResolver.kt`**: 
   - Wrapped `streamExtractor.extract` in `kotlinx.coroutines.withTimeoutOrNull(10_000L)`. If resolution takes longer than 10 seconds, it aborts cleanly and returns `null` so the error handler takes over.
3. **`app/src/main/kotlin/com/omnitune/app/playback/MusicService.kt`**: 
   - Added a `playbackWatchdogJob` in `onPlaybackStateChanged`. If the player gets stuck in `STATE_BUFFERING` for more than 15 seconds, it cancels the job and forces a `PlaybackException` with `ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT`.
   - Updated `fallbackSkip()` to accept an `errorType` and display a clear user-facing `Toast` message (e.g., "Network error during playback. Please check your connection." or "Playback timed out. Please try again.") when retries are exhausted.

## Privacy/Security
Confirmed no full stream URLs, YouTube URLs, cookies, headers, or tokens are logged. Added standard exception types instead of logging full URL stack traces. Logging uses generic tags (`StreamUrlResolver: no stream found for $videoId or timed out` and `Playback watchdog timeout!`).

## Verification Matrix
*Note: ADB tests marked as PENDING due to `adb.exe: no devices/emulators found` during execution. Requires physical device validation.*

| Test                        | Wi-Fi | Mobile Data | Offline | Result |
| --------------------------- | ----- | ----------- | ------- | ------ |
| Search-to-play              | PENDING | PENDING     | N/A     | PENDING |
| Completed download playback | N/A   | N/A         | PENDING | PENDING |
| No-internet state           | PENDING | PENDING     | N/A     | PENDING |
| Crash check                 | PENDING | PENDING     | PENDING | PENDING |
| URL leak check              | PENDING | PENDING     | PENDING | PENDING |

## Commands Run
- `.\gradlew clean assembleDebug lintDebug testDebugUnitTest`: **PASS**
- `.\gradlew assembleRelease`: **PASS** (with keystore environment variables)
- `adb install -r app/build/outputs/apk/debug/app-debug.apk`: **FAIL** (no devices/emulators found)

## Remaining Issues
- ADB device was disconnected; manual verification matrix needs to be executed by the user on a physical device to confirm real-world behavior on an actual Wi-Fi network.

## Release Decision
CONDITIONAL GO
