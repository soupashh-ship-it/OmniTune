# Baseline Audit - v0.5.2

## Current Architecture
- **UI:** Jetpack Compose, directly depends on `MusicService.instance` in many places (violates separation of concerns).
- **Service:** `MusicService` handles ExoPlayer lifecycle, media session, downloads, queue, and playback recovery. It is a monolithic "God object".
- **Database:** Room database with indexed tables for songs, albums, artists, etc.
- **Search:** Heavy reliance on typed filtering, lacking a robust fallback layered search parser.
- **Downloads:** Skeleton exists, but lacks state machine, offline-first resolution, and verified file-handling behavior.

## Identified Architecture Gaps
1. **Error Recovery:** `MusicService` does not smartly handle YouTube API 403/404s with retries before skipping.
2. **Coupling:** UI components bypass ViewModels and interact directly with the Service.
3. **Search Fallbacks:** Fails entirely if the primary typed parser fails.
4. **Queue Persistence:** Needs extraction into a `QueueRepository` and `QueuePersistenceManager`.
5. **Downloads:** Lacks lifecycle and offline-first integration with ExoPlayer.

## Assessment
The codebase has successfully implemented initial premium features, but the underlying scaffolding (error recovery, search layers, and decoupling) is fragile and requires strengthening before any further UI work is attempted.
