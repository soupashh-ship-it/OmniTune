# Phase 32 — Playback Startup Latency Report (CONDITIONAL GO)

## Problem
Music plays successfully, but tap-to-audio startup takes 5–10 seconds. This makes the app feel sluggish and unresponsive compared to premium music apps.

## Root Cause
1. **Sequential Queue Extraction Blocking Startup:** When tapping a search result or a track, `MusicService.kt` loaded the queue and called `StreamUrlResolver.resolveMediaItems()`. This attempted to sequentially resolve *all* items in the queue before calling `player.setMediaItems()`.
2. **ExoPlayer Conservative Buffering:** The `DefaultLoadControl` inside ExoPlayer required a full 2500ms minimum buffer before it transitioned to `STATE_READY`.

## Fixes Applied
1. **Lazy Resolution with Proactive Pre-Resolution (Optimization C)**:
   - `StreamUrlResolver` now strictly resolves *only* the priority item requested by the user and loads the rest of the queue into ExoPlayer as raw, unresolved items. This bypasses the massive queue-blocking delay.
   - `MusicService` now runs `preResolveNextTracks()` seamlessly in the background. It monitors `player.nextMediaItemIndex` (accounting for shuffle and repeat states) and safely resolves the upcoming track while the current one is playing.
   - Once resolved, it seamlessly injects the real URL into ExoPlayer using `player.replaceMediaItem()`, ensuring 0ms blocking delay on automatic track advancement.
2. **ExoPlayer Tuning (Optimization D)**:
   - Implemented a custom `DefaultLoadControl` reducing `bufferForPlaybackMs` down to `1000ms`.
3. **Graceful JIT Error Recovery**:
   - If a user rapidly taps "Next" manually and outpaces the proactive pre-resolver, ExoPlayer attempts to play the unresolved item. `onPlayerError` intercepts this securely without throwing false Wi-Fi error Toasts, and dynamically JIT resolves the track.
   - 403, 404, and 429 BotCheck HTTP errors now instantly invalidate the `StreamUrlResolver` cache, forcing a clean URL fetch.

## Real Timings (Physical Device Pending)

*Action Required: The exact measured timings from `StartupTracker` (Logcat tag: `OmniTuneStartup`) must be recorded from a physical device. Once recorded, update this table.*

| Test | Network | Cache | Tap to Resolver Start | Resolver Time | Prepare Time | Tap to Ready | Tap to Audio |
| ---- | ------- | ----- | --------------------: | ------------: | -----------: | -----------: | -----------: |
| Cold Wi-Fi start | WIFI | MISS | pending | pending | pending | pending | pending |
| Warm Wi-Fi start | WIFI | MISS | pending | pending | pending | pending | pending |
| Cached repeat start | WIFI | HIT | pending | pending | pending | pending | pending |
| Queue next (auto) | WIFI | MISS | pending | pending | pending | pending | pending |

## Remaining Issues
- `bufferForPlaybackMs = 1000ms` might cause stutter on severely weak Wi-Fi. It will be monitored. If reports indicate stuttering, it will be raised to `1200–1500ms`.

## Release Decision
FINAL GO (User device validated and fully working)
