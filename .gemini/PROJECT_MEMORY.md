# OmniTune Project Memory

Last updated: 2026-07-10

## Purpose
This is a compact working memory file for future tasks in this repo.
Use it as the first shortcut after `GEMINI.md` when figuring out where code lives and what must not break.

## Hard Rules
- Read `GEMINI.md` first.
- Do not rename `com.omnitune.app` or change GPL/credits/attribution.
- Preserve playback, queue, downloads, notification, background playback, and restore behavior.
- Prefer the smallest safe patch.
- Verify changes with:
  - `.\gradlew.bat testDebugUnitTest`
  - `.\gradlew.bat lintDebug`
  - `.\gradlew.bat assembleDebug`

## Working Tree Snapshot
- Branch: `feature/playlist-remaster`
- Pre-existing modified files:
  - `gradle/libs.versions.toml`
  - `gradle/wrapper/gradle-wrapper.jar`
  - `gradle/wrapper/gradle-wrapper.properties`
  - `gradlew`
- Pre-existing untracked file:
  - `gradle/gradle-daemon-jvm.properties`

Treat the above as user or in-progress work unless told otherwise.

## Repo Shape
- Multi-module Android project.
- Modules from `settings.gradle.kts`:
  - `:app`
  - `:innertube`
  - `:kugou`
  - `:lrclib`
  - `:lastfm`
  - `:simpmusic`
  - `:betterlyrics`
  - `:kizzy`
  - `:canvas`
- Kotlin file count snapshot:
  - `app/src/main/kotlin/com/omnitune/app`: about 369 files
  - supporting modules combined: about 129 files
  - test files under `app` + `innertube`: 20 files

## Version Reality Check
- `app/build.gradle.kts` currently says:
  - `versionCode = 60`
  - `versionName = "0.12.0"`
- Some docs still mention older releases or pre-1.0 states.
- Trust build files and code over README/release-roadmap wording when they disagree.

## Main Runtime Flow
1. App process starts in `app/src/main/kotlin/com/omnitune/app/OmniTuneApp.kt`.
2. `OmniTuneApp` sets global prefs/logging, initializes YouTube locale/auth-related observers, Last.fm, theme/cache behavior, and crash capture.
3. `MainActivity` starts and binds `MusicService`, requests notification permission, and builds the Compose tree.
4. `MainActivity` exposes `PlayerConnection`, `MusicDatabase`, `DownloadUtil`, and `SyncUtils` through composition locals.
5. `OmniTuneMainScreen` in `ui/navigation/OmniNavGraph.kt` owns navigation, shell layout, mini-player visibility, and most screen wiring.
6. `MusicService` owns playback state, queue state, MediaSession, notifications, autoplay continuation, recovery, downloads integration, and persistence hooks.
7. `PlayerConnection` is the UI-facing wrapper around `MusicService` + ExoPlayer state.

## Core Files To Know
- App/bootstrap:
  - `app/src/main/kotlin/com/omnitune/app/OmniTuneApp.kt`
  - `app/src/main/kotlin/com/omnitune/app/MainActivity.kt`
  - `app/src/main/AndroidManifest.xml`
- Navigation and shell:
  - `app/src/main/kotlin/com/omnitune/app/ui/navigation/OmniNavGraph.kt`
  - `app/src/main/kotlin/com/omnitune/app/ui/shell/OmniShell.kt`
  - `app/src/main/kotlin/com/omnitune/app/ui/player/MiniPlayer.kt`
  - `app/src/main/kotlin/com/omnitune/app/ui/player/PlayerScreen.kt`
- Playback:
  - `app/src/main/kotlin/com/omnitune/app/playback/MusicService.kt`
  - `app/src/main/kotlin/com/omnitune/app/playback/PlayerConnection.kt`
  - `app/src/main/kotlin/com/omnitune/app/playback/StreamUrlResolver.kt`
  - `app/src/main/kotlin/com/omnitune/app/playback/DownloadUtil.kt`
  - `app/src/main/kotlin/com/omnitune/app/playback/PlaybackNotificationManager.kt`
  - `app/src/main/kotlin/com/omnitune/app/playback/QueuePersistenceManager.kt`
  - `app/src/main/kotlin/com/omnitune/app/playback/PlaybackRecoveryCoordinator.kt`
  - `app/src/main/kotlin/com/omnitune/app/playback/RadioQueueManager.kt`
- Data/network:
  - `app/src/main/kotlin/com/omnitune/app/data/StreamExtractor.kt`
  - `app/src/main/kotlin/com/omnitune/app/data/MusicRepositoryImpl.kt`
  - `app/src/main/kotlin/com/omnitune/app/data/LyricsRepositoryImpl.kt`
  - `app/src/main/kotlin/com/omnitune/app/di/NetworkModule.kt`
  - `innertube/src/main/kotlin/com/omnitune/innertube/YouTube.kt`
- Storage:
  - `app/src/main/kotlin/com/omnitune/app/db/MusicDatabase.kt`
  - `app/src/main/kotlin/com/omnitune/app/db/DatabaseDao.kt`
  - `app/src/main/kotlin/com/omnitune/app/db/entities/`
- Settings and prefs:
  - `app/src/main/kotlin/com/omnitune/app/constants/PreferenceKeys.kt`
  - `app/src/main/kotlin/com/omnitune/app/utils/DataStore.kt`
  - `app/src/main/kotlin/com/omnitune/app/ui/screens/settings/`

## Architecture Summary

### UI
- Jetpack Compose + Material 3, dark-first OmniGlass styling.
- Navigation is centralized in one large `NavHost`.
- Screens are spread across:
  - `ui/screens/`
  - `ui/screens/search/`
  - `ui/screens/settings/`
  - `ui/screens/playlist/`
  - `ui/player/`
  - `ui/component/`
- `OmniNavGraph.kt` is a high-traffic file and one of the easiest places to create regressions.

### Playback
- Playback is Media3/ExoPlayer based.
- `MusicService` is the runtime center of gravity.
- It currently coordinates:
  - player setup
  - queue restore/save
  - notification/widget refresh
  - network monitoring
  - playback recovery
  - crossfade/equalizer observers
  - autoplay continuation
  - Bluetooth auto-start
  - history/play-count/scrobble recording
  - Discord presence integration
- Even though there are extracted helpers, `MusicService.kt` is still large and change-sensitive.

### Data + Persistence
- Room database wrapper is `MusicDatabase`, backed by `InternalDatabase`.
- DB version is 6.
- Queue state is persisted in the database, not only in memory.
- Important entities for playback/library work:
  - `SongEntity`
  - `PlaylistEntity`
  - `QueueEntity`
  - `LyricsEntity`
  - `FormatEntity`
  - `Event`
  - `SongSkipEntity`
- There is both normal migration code and schema-repair logic in `MusicDatabase.kt`.

### Networking
- App uses OkHttp and Ktor.
- YouTube Music access is through the local `innertube` module, mostly via the `YouTube` object.
- Stream resolution is intentionally resilient:
  - `StreamExtractor` rotates clients and classifies failures.
  - playback/download code depends on that fallback behavior.

### Lyrics
- Provider modules:
  - `lrclib`
  - `kugou`
  - `betterlyrics`
  - `simpmusic`
- App-side orchestration lives in:
  - `app/src/main/kotlin/com/omnitune/app/lyrics/`
  - `LyricsRepositoryImpl`
  - `LyricsViewModel`
- Lyrics cache and DB fallback matter; avoid replacing provider flow casually.

### Side Integrations
- Discord Rich Presence:
  - app-side manager in `app/.../discord/`
  - protocol/client module in `:kizzy`
- Last.fm:
  - module `:lastfm`
  - app hooks in playback/settings/app init
- Canvas effects:
  - `:canvas`
- Backup/restore:
  - `app/.../backup/`

## Where To Edit By Task
- Search results, search UX, search playback handoff:
  - `ui/screens/search/`
  - `SearchViewModel.kt`
  - `OmniNavGraph.kt`
  - `innertube/YouTube.kt`
- Player UI / mini-player / queue screen:
  - `ui/player/`
  - `ui/screens/QueueScreen.kt`
  - `PlayerConnection.kt`
- Playback bugs, stream failures, resume behavior:
  - `MusicService.kt`
  - `StreamUrlResolver.kt`
  - `StreamExtractor.kt`
  - `PlaybackRecoveryCoordinator.kt`
  - `NetworkPlaybackMonitor.kt`
- Download behavior:
  - `DownloadUtil.kt`
  - `ExoDownloadService.kt`
  - `DownloadsScreen.kt`
  - DB entities for cached formats/songs if metadata is involved
- Library/playlists/history/stats:
  - `db/dao/`
  - `db/entities/`
  - `ui/screens/Library*.kt`
  - `PlaylistDetailViewModel.kt`
  - `StatsViewModel.kt`
- Lyrics:
  - `lyrics/`
  - `LyricsRepositoryImpl.kt`
  - provider modules
- Settings/prefs toggles:
  - `constants/PreferenceKeys.kt`
  - `utils/DataStore.kt`
  - matching screen under `ui/screens/settings/`
  - observer/consumer in playback/app init if behavior is runtime-driven

## High-Risk Regression Areas
- `MusicService.kt`
- `OmniNavGraph.kt`
- queue persistence and restore
- stream resolution fallback
- notification/media session behavior
- completed-download playback
- lyrics bottom sheet during track changes
- DataStore preference collectors that affect playback

If touching any of those, assume manual verification needs extra attention.

## Existing Architecture Guidance Worth Reusing
- `docs/architecture/service-decomposition.md`
  - Keep new responsibilities out of `MusicService` when possible.
- `docs/architecture/god-object-prevention.md`
- `docs/architecture/database-dao-split.md`
- `docs/refactor/phase-2*.md`
  - useful background for playback/service extraction
- `docs/review/codex-post-gemini-refactor-review.md`
  - useful for environment/build/runtime history

## Current Code Reality Notes
- `MainActivity` still does dynamic theme extraction work tied to current artwork.
- `OmniNavGraph.kt` is large and contains repeated route wiring; prefer surgical edits.
- `MusicService.kt` still contains a lot of coordination logic despite decomposition docs.
- README and roadmap are useful orientation, but some claims/version numbers are stale.
- The app has meaningful automated unit coverage around:
  - playback continuation
  - queue persistence
  - stream URL resolution
  - diagnostics export
  - inline lyrics
  - theme/palette helpers

## Fast Mental Model
- `OmniTuneApp` = global init and preference observers.
- `MainActivity` = service binding + Compose root.
- `OmniNavGraph` = user flow router.
- `MusicService` = playback brain.
- `PlayerConnection` = UI adapter for the playback brain.
- `MusicDatabase` = library/history/queue persistence.
- `YouTube` (`:innertube`) = external content/search/browse/queue source.
- lyrics provider modules = fetchers, app module = orchestration.

## Suggested Refresh Rule
Update this file after any task that changes:
- playback architecture
- navigation structure
- module layout
- database schema
- verification commands
- branch/worktree assumptions that future tasks should know
