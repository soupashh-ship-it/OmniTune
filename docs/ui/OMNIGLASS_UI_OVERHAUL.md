# OmniGlass UI Overhaul

## Phase 0 Baseline Lock

Phase 0 locks the current OmniTune baseline before any OmniGlass UI overhaul work begins. This document is the only Phase 0 artifact. Do not change app UI, playback, Gradle, release, or production source files as part of Phase 0.

## Baseline Identity

- Latest public GitHub release: `v0.6.11`
- Release asset: `OmniTune-v0.6.11-release.apk`
- Baseline commit: `9073711c79a4f3794a7af54212149081f0c1c44a`
- Local refs at baseline: `main`, `origin/main`, and `v0.6.11` all point to `9073711c79a4f3794a7af54212149081f0c1c44a`
- App version: `versionName = "0.6.11"`
- App version code: `versionCode = 25`
- Android SDK config: `compileSdk = 36`, `minSdk = 26`, `targetSdk = 36`
- Baseline build environment: JDK 21

## Build Gate

These checks were observed passing under JDK 21 before this docs-only Phase 0 edit:

```powershell
.\gradlew.bat clean assembleDebug
```

Result: PASS

```powershell
.\gradlew.bat lintDebug
```

Result: PASS

No build rerun is required for Phase 0 because this phase only creates this documentation file.

## Current UI Map

- Shell: `app/src/main/kotlin/com/omnitune/app/MainActivity.kt` owns the global Compose shell, navigation host, app-level layout, and bottom dock behavior.
- Home: `app/src/main/kotlin/com/omnitune/app/ui/screens/HomeScreen.kt` and `HomeViewModel.kt`.
- Search: `app/src/main/kotlin/com/omnitune/app/ui/screens/SearchScreen.kt`, `SearchViewModel.kt`, and shared search UI in `ui/component/SearchBar.kt`.
- Library: `LibraryScreen.kt`, `LibraryViewModel.kt`, `LibraryAlbumsScreen.kt`, `LibraryArtistsScreen.kt`, `LibraryPlaylistsScreen.kt`, `LikedSongsScreen.kt`, `RecentlyPlayedScreen.kt`, and shared library UI in `ui/component/Library.kt`.
- Downloads: `app/src/main/kotlin/com/omnitune/app/ui/screens/DownloadsScreen.kt` and `DownloadsViewModel.kt`.
- Settings: `app/src/main/kotlin/com/omnitune/app/ui/screens/SettingsScreen.kt`, `SettingsViewModel.kt`, and settings palette support in `ui/screens/settings/ThemePalettes.kt`.
- MiniPlayer: `app/src/main/kotlin/com/omnitune/app/ui/player/MiniPlayer.kt`.
- Full player: `app/src/main/kotlin/com/omnitune/app/ui/player/PlayerScreen.kt`, with slider support in `ui/component/PlayerSlider.kt` and artwork cache support in `CanvasArtworkPlaybackCache.kt`.
- Queue: `app/src/main/kotlin/com/omnitune/app/ui/screens/QueueScreen.kt`, backed by playback queue classes under `app/src/main/kotlin/com/omnitune/app/playback/queues/`.
- Theme foundation: `app/src/main/kotlin/com/omnitune/app/ui/theme/` contains color, typography, shape, seed palette, and theme tokens.
- Shared glass components: `app/src/main/kotlin/com/omnitune/app/ui/component/GlassComponents.kt` already provides reusable glass-styled UI primitives.

The current UI is already partially glass-styled through theme tokens, shared glass components, the global shell and bottom dock, and the active MiniPlayer and full player surfaces.

## Protected Playback And Data Surfaces

The OmniGlass UI phases must not casually change playback, stream resolution, queue behavior, downloads, lyrics, or persistence. These files and areas are protected because visual changes can easily cause playback regressions if state contracts, side effects, or service integration are touched:

- `app/src/main/kotlin/com/omnitune/app/playback/MusicService.kt`: foreground playback service, media session, notification behavior, and service lifecycle.
- `app/src/main/kotlin/com/omnitune/app/playback/PlayerConnection.kt`: UI-to-player bridge, playback state, controls, metadata, and queue operations.
- `app/src/main/kotlin/com/omnitune/app/playback/StreamUrlResolver.kt`: stream URL resolution path used before playback.
- `app/src/main/kotlin/com/omnitune/app/playback/queues/`: queue implementations and source-specific queue behavior.
- `app/src/main/kotlin/com/omnitune/app/models/PersistQueue.kt` and `app/src/main/kotlin/com/omnitune/app/db/entities/QueueEntity.kt`: queue persistence surfaces.
- `app/src/main/kotlin/com/omnitune/app/playback/DownloadUtil.kt`, `ExoDownloadService.kt`, `ui/screens/DownloadsViewModel.kt`, and `ui/screens/DownloadsScreen.kt`: download state, offline playback, and download UI integration.
- `app/src/main/kotlin/com/omnitune/app/data/LyricsRepository.kt`, `app/src/main/kotlin/com/omnitune/app/lyrics/`, and lyrics database entities/models: lyrics lookup, preload, parsing, and display data.
- `app/src/main/kotlin/com/omnitune/app/data/MusicRepository.kt`, `MusicRepositoryImpl.kt`, `StreamRepository.kt`, and `StreamRepositoryImpl.kt`: data access and stream repository contracts used by UI and playback.

UI phases may read these contracts and consume their state, but behavior changes need explicit justification, focused tests, and regression verification.

## Known Risks

- `KNOWN_ISSUES.md` still lists search fragility, download state gaps, library placeholder/fake sections, and queue persistence limits.
- `AUDIT_RESOLUTION_REPORT.md` exists as a local untracked audit artifact and claims some of those areas were improved. Phase 1 must verify the current code before assuming either document is fully current.
- Physical notification and lock-screen behavior remains OEM-dependent. UI work must not claim universal device behavior unless tested on target devices.

## Do-Not-Break Regression Checklist

- Search tap playback.
- MiniPlayer metadata, artwork, open behavior, and full controls.
- Full player metadata, seek, shuffle, and repeat.
- Queue, Play Next, and Add to Queue.
- Downloads and offline playback.
- Force-stop recovery.
- Notification and lock-screen controls not worsened.
- Update checker.
- Settings stability.
- Network-disabled copy.

## Next Phase

Phase 1 only: design tokens and theme foundation.

Phase 1 must wait for approval after the Phase 0 report. It must not edit playback/data surfaces or broaden scope into app behavior unless explicitly approved.

## Phase 1 Design Tokens And Theme Foundation

Phase 1 strengthens the dark-first OmniGlass design foundation only. It does not redesign screens, navigation, MiniPlayer, full player, playback, downloads, queue behavior, lyrics, persistence, release configuration, or Gradle configuration.

### Files Changed

- `app/src/main/kotlin/com/omnitune/app/ui/theme/OmniColors.kt`: added explicit OmniGlass background, glass, accent, semantic, and text color roles while preserving existing compatibility names.
- `app/src/main/kotlin/com/omnitune/app/ui/theme/OmniShapes.kt`: added semantic shape roles for small, medium, large, extraLarge, artwork, dock, player, and pill shapes while preserving existing aliases.
- `app/src/main/kotlin/com/omnitune/app/ui/theme/OmniSpacing.kt`: added centralized spacing tokens for future phases.
- `app/src/main/kotlin/com/omnitune/app/ui/theme/OmniTypography.kt`: removed negative display letter spacing and added named typography helper roles for future UI phases.
- `app/src/main/kotlin/com/omnitune/app/ui/theme/OmniModifiers.kt`: added safe reusable modifier utilities for glass surfaces, soft borders, premium gradient backgrounds, press scale, static artwork glow, and disabled alpha.
- `app/src/main/kotlin/com/omnitune/app/ui/theme/Theme.kt`: mapped the Material 3 dark color scheme to the refined OmniGlass tokens.
- `docs/ui/OMNIGLASS_UI_OVERHAUL.md`: appended Phase 1 notes and validation results.

### Tokens Added Or Refined

- Background: `OmniBackgroundBase`, `OmniBackgroundElevated`, `OmniBackgroundGradientTop`, `OmniBackgroundGradientBottom`.
- Glass: `OmniGlassSubtle`, `OmniGlassMedium`, `OmniGlassStrong`, `OmniGlassDock`, `OmniGlassPlayer`, `OmniGlassBorderSubtle`, `OmniGlassBorderStrong`.
- Accent: `OmniAccentPrimary`, `OmniAccentSecondary`, `OmniAccentMuted`, `OmniAccentGlow`, `OmniAccentOnPrimary`.
- Semantic: `Success`, `Warning`, `Error`, `Offline`, `Downloaded`, `ActivePlayback`.
- Text: `TextPrimary`, `TextSecondary`, `TextTertiary`, `TextDisabled`, `TextOnAccent`.
- Shapes: `Small`, `Medium`, `Large`, `ExtraLarge`, `ArtworkSmall`, `ArtworkMedium`, `ArtworkLarge`, `Dock`, `Player`, `Pill`.
- Spacing: `micro`, `compact`, `small`, `medium`, `large`, `section`, `hero`, `screen`.
- Typography helpers: `heroTitle`, `screenTitle`, `sectionTitle`, `songTitle`, `metadata`, `caption`.

### Intentional Non-Changes

- Playback and data files were untouched because Phase 1 is a theme-token foundation pass and should not affect playback state, service lifecycle, stream resolution, queues, downloads, lyrics, persistence, or repositories.
- `MainActivity.kt`, navigation graph behavior, MiniPlayer, full player, Search, Downloads, Library, and Settings screens were not redesigned or refactored.
- `GlassComponents.kt` was not changed; existing shared glass components can adopt the new tokens in Phase 2.
- Gradle, release signing, package identity, namespace, application ID, license, credits, and attribution were not changed.

### Validation Results

- `.\gradlew.bat clean assembleDebug`: PASS under JDK 21.
- `.\gradlew.bat lintDebug`: PASS under JDK 21.
- Initial build rerun exposed the local shell using `JAVA_HOME` from JDK 17 while `java` resolved to JDK 21. Rerunning with `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot` fixed the environment issue.
- Device smoke test on `138898743000055`: PASS. Installed debug APK, launched app, searched `daft punk`, tapped a song, confirmed MiniPlayer metadata and controls, opened the full player, and confirmed full player metadata, seekbar, shuffle/repeat, playback controls, queue, download, and audio effects controls rendered without a crash.

### Phase 2 Follow-Up

Phase 2 only should adopt the Phase 1 tokens inside shared premium components, especially existing glass surfaces, cards, buttons, rows, icon buttons, chips, placeholders, and loading states. It should still avoid playback/data behavior changes.

## Phase 2 Shared Premium Components

Phase 2 applies the Phase 1 token foundation to shared reusable components only. It does not redesign individual screens, navigation, MiniPlayer, full player, playback, downloads, queues, lyrics, repositories, Gradle, release signing, package identity, license, credits, or attribution.

### Files Changed

- `app/src/main/kotlin/com/omnitune/app/ui/component/GlassComponents.kt`: tokenized shared glass primitives, added `GlassTone`, and aligned cards, rows, icon buttons, gradient buttons, section headers, pills, and shimmer bars with OmniGlass tokens.
- `app/src/main/kotlin/com/omnitune/app/ui/component/EmptyPlaceholder.kt`: aligned empty states with OmniGlass spacing, shape, text, and subtle glass tokens.
- `app/src/main/kotlin/com/omnitune/app/ui/component/Items.kt`: aligned shared list/grid item typography, active-row background, artwork shape, and selected thumbnail overlay with OmniGlass tokens.
- `app/src/main/kotlin/com/omnitune/app/ui/component/OmniTuneLoader.kt`: changed the default loader accent to the `ActivePlayback` semantic token.
- `docs/ui/OMNIGLASS_UI_OVERHAUL.md`: appended Phase 2 notes and validation results.

### Component Foundation Changes

- Existing shared component function names remain intact for compatibility.
- `GlassTone` provides subtle, medium, strong, dock, and player surface options for later phases.
- Shared glass cards and buttons now use Phase 1 accent glow, glass borders, spacing, and pressed-scale utilities.
- Empty placeholders, list rows, grid item text, active item state, artwork corners, and loader accent are token-backed.
- No LazyColumn item blur or realtime expensive visual effects were added.
- No modifier consumes pointer input unexpectedly or owns playback state.

### Intentional Non-Changes

- No screen-specific redesigns were made.
- `MainActivity.kt`, navigation, MiniPlayer, full player, Search, Downloads, Library, and Settings screens were not directly edited.
- Playback service, player connection, stream resolution, queue classes, downloads, lyrics, persistence, data/repository code, update checker, notification controls, and lock-screen behavior were untouched.

### Validation Results

- `.\gradlew.bat clean assembleDebug`: PASS under JDK 21.
- `.\gradlew.bat lintDebug`: PASS under JDK 21.
- The initial Phase 2 build exposed a missing `dp` import in `EmptyPlaceholder.kt`; the fix was scoped to that component file.
- Device smoke test on `138898743000055`: PASS. Installed debug APK, launched app, searched `daft punk`, started playback, confirmed MiniPlayer metadata and controls, opened the full player, and confirmed full player metadata, seekbar, shuffle/repeat, playback controls, queue, download, and audio effects controls rendered without a crash.

### Phase 3 Follow-Up

Phase 3 only should begin applying the shared component foundation to screen-level layouts in a narrow, reviewable pass. It should still preserve playback/data behavior and avoid navigation or player logic changes unless explicitly approved.

## Phase 3 App Shell, Navigation, Background, And Global Layout

Phase 3 applies the OmniGlass foundation to the global app shell only. It does not redesign Home, Search, Library, Downloads, Settings, MiniPlayer internals, full player internals, playback, downloads, queues, lyrics, repositories, Gradle, release signing, package identity, license, credits, or attribution.

### Files Changed

- `app/src/main/kotlin/com/omnitune/app/MainActivity.kt`: added the root OmniGlass background wrapper, adjusted global content bottom padding, refined the floating glass bottom dock, and placed the MiniPlayer host above the dock with system-bar-aware spacing.
- `docs/ui/OMNIGLASS_UI_OVERHAUL.md`: appended Phase 3 notes and validation results.

### Shell And Navigation Changes

- Root background now uses the Phase 1 background and accent tokens through cheap Compose brush backgrounds.
- The existing `NavHost`, routes, destination semantics, and back behavior were preserved.
- Screen content receives bottom padding based on whether the bottom dock and MiniPlayer are visible.
- The bottom dock uses the existing destination list with a stronger glass surface, rounded dock shape, active destination treatment, and readable inactive icons.
- The MiniPlayer host placement was adjusted only at the shell level; its metadata, artwork, controls, gestures, and click behavior were not changed.
- Bottom system bar insets are still respected with `WindowInsets.systemBars.only(WindowInsetsSides.Bottom)`.

### Intentional Non-Changes

- No Home, Search, Library, Downloads, Settings, Queue, MiniPlayer, or full player screen internals were redesigned.
- Playback service, player connection, stream resolution, queues, downloads, offline playback, lyrics, persistence, data/repository code, update checker, notification controls, and lock-screen behavior were untouched.
- Gradle, release signing, package identity, namespace, application ID, license, credits, and attribution were not changed.

### Validation Results

- `.\gradlew.bat clean assembleDebug`: PASS under JDK 21.
- `.\gradlew.bat lintDebug`: PASS under JDK 21.
- Initial Phase 3 build exposed one compile issue from an invalid `matchParentSize` import; the fix was scoped to `MainActivity.kt` by using `fillMaxSize()`.
- Device smoke test on `138898743000055`: PASS. Installed debug APK, launched app, opened bottom navigation destinations, searched for a song, started playback, confirmed MiniPlayer placement above the dock, opened the full player from MiniPlayer, returned with back navigation, and confirmed playback stayed active while switching tabs.

### Remaining Risks

- The smoke test confirms behavior on one connected Android device only; OEM navigation bar and lock-screen notification surfaces can still vary by device.
- Search and playback remain dependent on current network and stream availability.
- Phase 3 keeps existing screen content as-is, including any known placeholder or library limitations from the baseline audit.

### Phase 4 Follow-Up

Phase 4 only should redesign Home / Discovery content using the shell and shared component foundation. It should still avoid playback/data behavior changes and should not redesign Search, MiniPlayer, full player, queue, downloads, or settings unless explicitly approved for that phase.

## Phase 4 Home / Discovery Redesign

Phase 4 redesigns the Home screen only. It keeps the existing navigation callbacks and playback entry paths, and it does not redesign Search, Library, Downloads, Settings, MiniPlayer internals, full player internals, playback, downloads, queues, lyrics, repositories, Gradle, release signing, package identity, license, credits, or attribution.

### Files Changed

- `app/src/main/kotlin/com/omnitune/app/ui/screens/HomeScreen.kt`: rebuilt Home as a premium OmniGlass discovery entry with a header, search entry, current-track card, quick access, real feed items, real recently played, and empty/loading states.
- `app/src/main/kotlin/com/omnitune/app/ui/screens/HomeViewModel.kt`: exposed quick-pick loading state as a read-only flow and kept the existing best-effort YouTube home feed outside Composables.
- `docs/ui/OMNIGLASS_UI_OVERHAUL.md`: appended Phase 4 notes and validation results.

### Home Sections

- Header: premium OmniTune identity card using Phase 1/2 tokens and the Phase 3 shell background.
- Search entry: large glass action card that calls the existing Home search navigation callback.
- Continue Listening: shown only when existing `LocalPlayerConnection` metadata is available; tapping opens the existing full player route through the existing callback.
- Recently Played: uses existing database event data from `HomeViewModel`; empty state points users to Search.
- Quick Access: includes only working actions wired to existing callbacks: Search, Library, and Player when a current track exists.
- Downloads/offline highlight: no standalone Downloads card was added because Home does not currently receive a direct Downloads navigation callback, and Phase 4 did not change `MainActivity` routing.
- Empty/loading states: shimmer cards for loading, real empty copy for no recent plays or no available feed/history.

### Data Sources Used

- Current playback: existing `LocalPlayerConnection.mediaMetadata`.
- Recently played: existing `MusicDatabase.events()` flow through `HomeViewModel`.
- Discovery feed: existing best-effort `YouTube.home()` loading in `HomeViewModel`, not from a Composable.
- No demo songs, demo playlists, fake trending rows, or fake albums were added.

### Intentional Non-Changes

- Search, Library, Downloads, Settings, Queue, MiniPlayer, and full player screen internals were not edited.
- Playback service, player connection, stream resolution, queues, downloads, offline playback, lyrics, persistence, data/repository code, update checker, notification controls, and lock-screen behavior were untouched.
- Gradle, release signing, package identity, namespace, application ID, license, credits, and attribution were not changed.

### Validation Results

- `.\gradlew.bat clean assembleDebug`: PASS under JDK 21.
- `.\gradlew.bat lintDebug`: PASS under JDK 21.
- Device smoke test on `138898743000055`: PASS. Installed debug APK, launched Home, opened Search from the Home search entry, searched for a real song, started playback through the existing Search result path, returned to Home, confirmed the current-track card used real metadata, confirmed MiniPlayer opened full player, opened Quick Access Search and Library, and confirmed playback continued across route changes.

### Remaining Risks

- The standalone Downloads route was not added to Home because Phase 4 avoided `MainActivity` navigation changes.
- The discovery feed is best-effort and may be empty if network or YouTube home data is unavailable.
- The device smoke test covers one connected Android device; OEM-specific notification and lock-screen behavior remain device-dependent.

### Phase 5 Follow-Up

Phase 5 only should redesign Search using the existing search behavior and playback callbacks. It should not change stream resolution, playback queues, downloads, lyrics, repositories, MiniPlayer, or full player behavior.

## Phase 5 Search Redesign

Phase 5 redesigns the Search screen only. It keeps the existing `SearchViewModel` search semantics, existing Search callbacks from `MainActivity.kt`, and the existing playback/queue entry paths. It does not redesign Home, Library, Downloads, Settings, MiniPlayer internals, full player internals, playback, stream resolution, downloads, queues, lyrics, repositories, Gradle, release signing, package identity, license, credits, or attribution.

### Files Changed

- `app/src/main/kotlin/com/omnitune/app/ui/screens/SearchScreen.kt`: rebuilt Search as a premium OmniGlass search experience with a compact header, glass search field, real search history, honest loading/empty/error states, and refreshed result rows/actions.
- `docs/ui/OMNIGLASS_UI_OVERHAUL.md`: appended Phase 5 notes and validation results.

### Search Sections

- Header: compact premium Search title and subtitle using OmniGlass typography and spacing tokens.
- Search field: glass surface around the existing query state and callbacks, with search icon, clear action, IME search focus handling, and subtle loader while searching.
- Results list: real `SongItem`, `ArtistItem`, `AlbumItem`, and `PlaylistItem` results from `SearchViewModel`; rows use bounded artwork, stable keys, content types, and ellipsized title/metadata text.
- Result actions: song rows preserve the existing tap-to-play callback, Play Next callback, and Add to Queue callback. No new playback path was introduced.
- Loading state: lightweight shimmer-style placeholders that are not tappable fake results.
- Empty state: no-query copy plus real search history when available; no-result copy for real empty responses.
- Offline/error state: existing `SearchStatus` and error messages are shown honestly with retry only.

### Data Sources Used

- Search data: existing `SearchViewModel` and YouTube search flow.
- Recent search terms: existing `MusicDatabase.searchHistory()` flow.
- Playback: existing `onPlaySong` callback from `MainActivity.kt`.
- Queue actions: existing `onPlayNext` and `onAddToQueue` callbacks from `MainActivity.kt`.
- No demo songs, demo playlists, fake trending rows, fake suggested terms, or fake search results were added.

### Intentional Non-Changes

- `SearchViewModel.kt` was not changed; debounce timing, provider semantics, fallback/cache behavior, and repository contracts remain as they were.
- Playlist result rows are informational because Search does not currently receive a safe playlist navigation callback; no dead playlist button was added.
- No download action was added to Search rows because there was no existing Search-screen download callback to preserve.
- Home, Library, Downloads, Settings, Queue, MiniPlayer, and full player screen internals were not edited.
- Playback service, player connection, stream resolution, queues, downloads, offline playback, lyrics, persistence, data/repository code, update checker, notification controls, and lock-screen behavior were untouched.
- Gradle, release signing, package identity, namespace, application ID, license, credits, and attribution were not changed.

### Validation Results

- `.\gradlew.bat clean assembleDebug`: PASS under JDK 21.
- `.\gradlew.bat lintDebug`: PASS under JDK 21.
- Device smoke test on `138898743000055`: PASS. Installed debug APK, launched app, opened Search, typed real queries, confirmed results rendered, started playback from a Search result through the existing path, confirmed MiniPlayer and full player metadata/artwork, used Play Next and Add to Queue from the result overflow menu, cleared the query, switched bottom nav tabs, and confirmed playback continued.

### Remaining Risks

- Offline/no-network UI was not practically toggled during the smoke test; Phase 5 relies on the existing `SearchViewModel` network error state and copy.
- Download actions remain available from existing player/download surfaces, not from Search result rows.
- The device smoke test covers one connected Android device; OEM-specific notification and lock-screen behavior remain device-dependent.

### Phase 6 Follow-Up

Phase 6 only should redesign the MiniPlayer using the existing playback state and controls. It should not change PlayerConnection, MusicService, stream resolution, queues, downloads, lyrics, repositories, or full player behavior.

## Phase 6 MiniPlayer Redesign

Phase 6 redesigns the MiniPlayer only. It keeps the existing `MiniPlayer` public parameters, the existing `MainActivity.kt` host callback, and the existing `PlayerConnection` playback state/control paths. It does not redesign the full player, Library, Downloads, Settings, Queue, playback, stream resolution, downloads, lyrics, repositories, Gradle, release signing, package identity, license, credits, or attribution.

### Files Changed

- `app/src/main/kotlin/com/omnitune/app/ui/player/MiniPlayer.kt`: rebuilt the MiniPlayer as a premium floating OmniGlass playback surface while preserving existing playback controls, tap-to-open, skip controls, and progress behavior.
- `docs/ui/OMNIGLASS_UI_OVERHAUL.md`: appended Phase 6 notes and validation results.

### MiniPlayer Visual Changes

- Container: compact 80dp floating glass capsule using Phase 1/2 shape, glass, border, glow, and spacing tokens.
- Artwork: bounded 54dp artwork with Coil `ImageRequest` sizing, rounded corners, real artwork when available, and a cheap icon fallback when artwork is missing.
- Metadata: title and artist now prioritize real playback metadata from `PlayerConnection.mediaMetadata`; long text ellipsizes inside a stable layout.
- Controls: existing play/pause, previous, and next actions are preserved through `PlayerConnection`; no new playback path was added.
- Progress: existing duration/current-position polling remains, with a slim animated progress line and no fake progress state.

### Tap And Gesture Safety

- The MiniPlayer body and playback controls now use separate clickable zones.
- Tapping artwork/metadata opens the full player through the existing `onClick` callback.
- Play/pause, previous, and next buttons are isolated from the body click, so control taps do not also open the full player.
- The pre-existing horizontal swipe-to-skip gesture was retained on the floating surface and still uses only existing skip commands.
- No swipe-to-dismiss, new drag behavior, stream resolution, or playback restart behavior was added.

### Data Sources Used

- Metadata and artwork: existing `PlayerConnection.mediaMetadata`.
- Playback state: existing `PlayerConnection.isPlaying` and `playbackState`.
- Progress: existing `PlayerConnection.duration` and `currentPosition`.
- Controls: existing `playOrResolveCurrent()`, `pause()`, `seekToPrevious()`, and `seekToNext()`.
- No fake metadata, fake artwork, fake playback state, or dead controls were added.

### Intentional Non-Changes

- `MainActivity.kt` was not changed in Phase 6 because the existing MiniPlayer host spacing still supports the redesigned height.
- `PlayerConnection.kt`, `MusicService.kt`, `StreamUrlResolver.kt`, queues, downloads, offline playback, lyrics, persistence, data/repository code, update checker, notification controls, and lock-screen behavior were untouched.
- Full player, Home, Search, Library, Downloads, Settings, and Queue screen internals were not edited.
- Gradle, release signing, package identity, namespace, application ID, license, credits, and attribution were not changed.

### Validation Results

- `.\gradlew.bat clean assembleDebug`: PASS under JDK 21.
- `.\gradlew.bat lintDebug`: PASS under JDK 21.
- Device smoke test on `138898743000055`: PASS. Installed debug APK, launched app, searched a real song, started playback through the existing Search result path, confirmed the redesigned MiniPlayer showed real title, artist, artwork, and progress, tapped the body to open the full player, returned to the shell, verified play/pause, previous, and next controls stayed isolated from full-player navigation, switched tabs, and confirmed playback continued.

### Remaining Risks

- Missing-artwork fallback was not force-tested on device because the searched track had real artwork.
- OEM-specific notification and lock-screen behavior remain device-dependent and were not changed by Phase 6.
- The pre-existing swipe-to-skip gesture remains; Phase 6 verified simple taps and button taps, but broad gesture QA across device densities remains a later pass.

### Phase 7 Follow-Up

Phase 7 only should redesign the full player using the existing playback state and controls. It should not change PlayerConnection, MusicService, stream resolution, queues, downloads, lyrics, repositories, or MiniPlayer behavior.

## Phase 7 Full Player Redesign

Phase 7 redesigns the full player only. It keeps the existing `PlayerScreen` playback state sources, `PlayerConnection` controls, queue route callback, download view model action, audio-effects launcher, and sleep timer behavior. It does not redesign Queue, Lyrics, Library, Downloads, Settings, MiniPlayer, playback, stream resolution, repositories, Gradle, release signing, package identity, license, credits, or attribution.

### Files Changed

- `app/src/main/kotlin/com/omnitune/app/ui/player/PlayerScreen.kt`: rebuilt the full player as a premium OmniGlass playback screen while preserving existing metadata, seek, playback, shuffle, repeat, queue, download, audio effects, and sleep timer control paths.
- `docs/ui/OMNIGLASS_UI_OVERHAUL.md`: appended Phase 7 notes and validation results.

### Full Player Sections

- Top bar: compact collapse/back action and queue access using existing callbacks.
- Artwork hero: large rounded artwork with a controlled static accent glow, bounded Coil request size, and a cheap missing-artwork fallback.
- Metadata: real title, artist, and album text from `PlayerConnection.mediaMetadata`, with stable wrapping and ellipsis for long values.
- Primary actions: existing like, download, audio effects, sleep timer, and queue actions are preserved; no unwired action was added.
- Progress/seek: elapsed time, duration, and the existing seek path are retained with drag-safe local slider state.
- Playback controls: shuffle, previous, play/pause, next, and repeat continue to use only existing `PlayerConnection` commands and real state.
- Secondary surfaces: audio effects and sleep timer dialogs were visually aligned with OmniGlass without changing behavior.

### Behavior Preserved

- Seek still calls the existing player seek path and keeps playback updates from fighting active slider dragging.
- Shuffle and repeat state remain sourced from `PlayerConnection`; repeat cycles through off, all, and one.
- Queue access still uses the existing `onOpenQueue` callback.
- Download still uses the existing `DownloadsViewModel` action and current media metadata URL.
- Lyrics behavior was not changed. The current full player did not expose a wired lyrics action, so Phase 7 did not add a dead lyrics button or alter lyrics lookup/parsing.
- Metadata and artwork are read from current playback metadata only; no fake metadata, fake artwork, or placeholder masking was added.

### Performance Safeguards

- No heavy blur, dynamic artwork palette extraction, per-frame bitmap processing, stream resolver calls, or network calls were added from Composables.
- Artwork image size is bounded.
- Background effects are static Compose brushes.
- Progress updates are localized to the seek section and use drag-local state to avoid control jitter.

### Intentional Non-Changes

- `PlayerConnection.kt`, `MusicService.kt`, `StreamUrlResolver.kt`, queues, downloads/offline playback, lyrics lookup/parsing, persistence, data/repository code, update checker, notification controls, and lock-screen behavior were untouched.
- MiniPlayer, Home, Search, Library, Downloads, Settings, and Queue screen internals were not edited.
- Gradle, release signing, package identity, namespace, application ID, license, credits, and attribution were not changed.

### Validation Results

- `.\gradlew.bat clean assembleDebug`: PASS under JDK 21.
- `.\gradlew.bat lintDebug`: PASS under JDK 21.
- Device smoke test on `138898743000055`: PASS. Installed debug APK, launched app, searched a real song, started playback through the existing Search result path, opened the redesigned full player from MiniPlayer, confirmed real title/artist/artwork, exercised play/pause, previous, next, seek, shuffle, repeat off/all/one, queue, and download actions, collapsed back to the shell, switched tabs, reopened the full player, and confirmed playback plus metadata continuity.

### Remaining Risks

- Missing-artwork fallback and a dedicated unknown-duration track still need broader device QA.
- Lyrics access remains unchanged and not surfaced in the current full player because Phase 7 avoided adding unwired actions.
- OEM-specific notification and lock-screen behavior remain device-dependent and were not changed by Phase 7.

### Phase 8 Follow-Up

Phase 8 only should polish Queue and Lyrics surfaces using existing queue and lyrics behavior. It should not change playback, stream resolution, downloads, repositories, Gradle, release signing, package identity, license, credits, or attribution.

## Phase 8 Queue And Lyrics Surface Polish

Phase 8 polishes the existing Queue route only. It keeps the existing `QueueScreen` route, `PlayerConnection` queue state reads, queue item tap behavior, and swipe-to-remove behavior. It does not add a lyrics route, full-player lyrics button, queue persistence changes, queue ordering changes, lyrics lookup/parsing changes, playback changes, downloads/offline changes, repositories, Gradle, release signing, package identity, license, credits, or attribution.

### Files Changed

- `app/src/main/kotlin/com/omnitune/app/ui/screens/QueueScreen.kt`: rebuilt the wired Queue surface with OmniGlass styling, current-track card, real upcoming queue rows, bounded artwork, empty states, and preserved tap/remove actions.
- `docs/ui/OMNIGLASS_UI_OVERHAUL.md`: appended Phase 8 notes and validation results.

### Queue Surface Changes

- Header: compact glass-safe Queue header with existing back action, real queue title, total item count, and up-next count.
- Now playing/current item: premium current-track card using real `PlayerConnection.mediaMetadata`, artwork, title, and artists.
- Upcoming queue: real media queue items only, stable keys, bounded artwork loading, readable title/artist hierarchy, and no fake rows.
- Queue actions: existing row tap still calls `seekTo(index, 0)` and `prepare()`; existing swipe remove still calls `removeMediaItem(index)`.
- Queue empty state: honest empty copy for no queue/current item and no upcoming items.

### Lyrics Surface Status

- Lyrics surface: NOT AVAILABLE. The project contains lyrics logic/providers and lyrics settings, but no safely routed lyrics display surface was available in the app shell/full player for Phase 8 polish.
- No lyrics screen, lyrics route, full-player lyrics button, parser changes, provider changes, repository changes, or fake lyrics were added.

### Behavior Preserved

- Search tap playback, Add to Queue, Queue open, queue item tap, queue swipe remove, MiniPlayer, full player, and playback across tab/navigation changes remain on existing paths.
- No fake queue items, fake lyrics, or dead controls were added.

### Performance Safeguards

- No heavy blur, expensive effects, stream resolver calls, network calls from Composables, lyrics parsing changes, or queue implementation changes were added.
- Queue artwork requests are bounded.
- Queue list rows use stable keys and simple static glass surfaces.

### Intentional Non-Changes

- Home, Search, Library, Downloads, Settings, MiniPlayer, and full player were not redesigned again.
- `PlayerConnection.kt`, `MusicService.kt`, `StreamUrlResolver.kt`, playback queue implementation files, queue persistence models/entities, lyrics logic/provider/parser/repository files, downloads/offline playback, persistence, data/repository code, update checker, notification controls, and lock-screen behavior were untouched.
- Gradle, release signing, package identity, namespace, application ID, license, credits, and attribution were not changed.

### Validation Results

- `.\gradlew.bat clean assembleDebug`: PASS under JDK 21.
- `.\gradlew.bat lintDebug`: PASS under JDK 21.
- Device smoke test on `138898743000055`: PASS. Installed debug APK, launched app, searched a real song, started playback through the existing Search result path, added a real result to the queue through the existing overflow Add to Queue action, opened Queue through the existing player queue action, confirmed current and upcoming real queue items rendered, tapped an upcoming item to preserve existing seek behavior, swiped a queued item to preserve existing remove behavior, returned to full player, switched tabs, reopened player, and confirmed playback continuity.

### Remaining Risks

- Lyrics display remains unavailable because Phase 8 did not create new lyrics routing or buttons.
- Swipe-to-remove uses the existing `SwipeToDismissBox` pattern, which currently compiles with a Material deprecation warning but preserves behavior.
- Queue count text may lag until player state emits a recomposition after removal because `mediaItemCount` is read from the existing player connection rather than a dedicated queue-state flow.
- OEM-specific notification and lock-screen behavior remain device-dependent and were not changed by Phase 8.

### Phase 9 Follow-Up

Phase 9 only should redesign Library and Downloads surfaces using existing data and download behavior. It should not change playback, stream resolution, queue behavior, lyrics behavior, repositories, Gradle, release signing, package identity, license, credits, or attribution.

## Phase 9 Library And Downloads Redesign

Phase 9 redesigns the Library and Downloads UI surfaces only. It keeps existing navigation routes, library data sources, completed-download playback behavior, download state reads, retry/remove callbacks, MiniPlayer/full-player behavior, playback, stream resolution, queue behavior, lyrics behavior, repositories, Gradle, release signing, package identity, license, credits, and attribution unchanged.

### Files Changed

- `app/src/main/kotlin/com/omnitune/app/ui/screens/LibraryScreen.kt`: rebuilt the Library hub with OmniGlass header, real aggregate counts, existing destination cards, browse rows, and honest empty handling.
- `app/src/main/kotlin/com/omnitune/app/ui/screens/LikedSongsScreen.kt`: polished the liked songs list, header, missing-artwork fallback, and empty state while preserving existing tap playback.
- `app/src/main/kotlin/com/omnitune/app/ui/screens/RecentlyPlayedScreen.kt`: polished recently played history using real event data and the existing playback callback.
- `app/src/main/kotlin/com/omnitune/app/ui/screens/LibraryAlbumsScreen.kt`: polished album rows and empty state while preserving existing album navigation.
- `app/src/main/kotlin/com/omnitune/app/ui/screens/LibraryArtistsScreen.kt`: polished artist rows and empty state while preserving existing artist navigation.
- `app/src/main/kotlin/com/omnitune/app/ui/screens/LibraryPlaylistsScreen.kt`: polished playlist rows and empty state while preserving existing playlist navigation behavior.
- `app/src/main/kotlin/com/omnitune/app/ui/screens/DownloadsScreen.kt`: rebuilt Downloads with OmniGlass header, real state counts, playable completed rows, non-playable incomplete/failed rows, retry/remove controls where already wired, and honest empty copy.
- `docs/ui/OMNIGLASS_UI_OVERHAUL.md`: appended Phase 9 notes and validation results.

### Library Surface Changes

- Header: premium dark-first Library header with real saved-library scope counts. Download counts are intentionally left to the Downloads screen because it reads the authoritative Media3 download state.
- Hub: existing routes only: Liked Songs, Downloads, Recently Played, Search, Artists, Albums, and Playlists. The Downloads card uses neutral copy instead of a Library-derived count.
- Liked Songs: real liked songs only, bounded artwork, long-title ellipsis, and existing playback path.
- Recently Played: real history events only, stable keys, and existing playback path.
- Albums, Artists, and Playlists: real data only, polished rows, honest empty states, and existing navigation behavior.
- Empty states: no fake songs, fake albums, fake artists, fake playlists, or fake metrics were added.

### Downloads Surface Changes

- Header: real counts for completed, active, and failed downloads derived from existing `DownloadsViewModel` state.
- Completed downloads: visually marked as playable and continue to call the existing completed-download playback callback only when `Download.STATE_COMPLETED`.
- Incomplete and failed downloads: visually distinct and not clickable for playback; failed rows keep the existing retry callback and all rows keep the existing remove callback.
- Download states: completed, downloading, queued, paused/stopped, failed, and unknown states are presented honestly from existing Media3 download state.
- Offline clarity: Downloads copy states that completed items are available offline; no network status or storage metric was invented.

### Behavior Preserved

- Library and Downloads use existing `LibraryViewModel` and `DownloadsViewModel` state only.
- Completed-download playback, retry, remove, and route behavior remain on existing callbacks.
- Playback, stream resolution, download service/util logic, queue behavior, lyrics behavior, repositories, and persistence were untouched.
- No fake library items, fake playlists/albums/artists, fake downloads, or dead controls were added.

### Performance Safeguards

- Lists use stable keys where available.
- Artwork sizes are bounded.
- No heavy blur, artwork extraction, stream resolver calls, network calls from Composables, repository changes, or download logic changes were added.
- Visual effects are static OmniGlass brushes, borders, and surfaces.

### Intentional Non-Changes

- Home, Search, MiniPlayer, full player, Queue, Lyrics, and Settings were not redesigned again.
- `PlayerConnection.kt`, `MusicService.kt`, `StreamUrlResolver.kt`, `ExoDownloadService.kt`, `DownloadUtil.kt`, playback queue files, lyrics files, data/repository files, Gradle, release signing, package identity, namespace, application ID, license, credits, and attribution were not changed.

### Validation Results

- `.\gradlew.bat clean assembleDebug`: PASS under JDK 21.
- `.\gradlew.bat lintDebug`: PASS under JDK 21.
- Device smoke test on `138898743000055`: PASS. Installed debug APK, launched app, opened Library, verified visible Library routes render without route crashes, opened Downloads, searched and played a real song through the existing Search path, confirmed MiniPlayer and full player still work, switched tabs, and confirmed playback continuity plus no bottom dock/MiniPlayer overlap.

### Remaining Risks

- Completed-download and offline completed-download playback could only be fully verified when a completed download is present on the device.
- In-progress and failed download visual states depend on having those real Media3 states present during QA; Phase 9 did not fake or force them.
- Existing playlist detail behavior remains unchanged; Phase 9 only polished the existing playlist list/route surface.
- OEM-specific notification and lock-screen behavior remain device-dependent and were not changed by Phase 9.

### Phase 10 Follow-Up

Phase 10 only should polish Settings, update checker, and diagnostics surfaces. It should not change playback, stream resolution, downloads/offline logic, repositories, Gradle, release signing, package identity, license, credits, or attribution.

## Phase 10 Settings Update Checker Diagnostics And About Polish

Phase 10 redesigns the Settings UI only. It preserves existing preference keys, update checker state and actions, APK download/install UI actions, diagnostic export behavior, notification and battery settings intents, playback behavior, stream resolution, downloads/offline behavior, queue behavior, lyrics behavior, data/repository code, Gradle, release signing, package identity, license files, credits files, and attribution.

### Files Changed

- `app/src/main/kotlin/com/omnitune/app/ui/screens/SettingsScreen.kt`: rebuilt Settings as an OmniGlass utility hub with grouped cards, clearer update/diagnostic/notification/About presentation, and preserved existing callbacks.
- `docs/ui/OMNIGLASS_UI_OVERHAUL.md`: appended Phase 10 notes and validation results.

### Settings Surface Changes

- Header: premium dark-first Settings header with real installed version from `BuildConfig.VERSION_NAME` and `BuildConfig.VERSION_CODE`.
- Groups: Appearance, Playback, Downloads & cache, Notifications & lock screen, Updates, Diagnostics, Content & history, Lyrics providers, Scrobbling, and About.
- Appearance/theme: existing pure black, blur, and grid-size preferences are preserved with clearer selected-state copy.
- Playback: existing stream-quality, equalizer, crossfade, skip-silence, auto-skip, and pause-on-mute controls are preserved.
- Downloads/cache: existing smart cache trimmer is preserved; copy clarifies that completed-download playback and storage/offline behavior are not changed here.
- Notifications/lock screen: existing notification, app settings, and battery settings actions are preserved with honest OEM/device-dependent wording.
- Updates: existing `UpdateViewModel` state and actions are preserved for idle, checking, no update, update available, downloading, downloaded, and error states.
- Diagnostics: existing `DiagnosticReportExporter.createShareIntent` path is preserved with clearer export copy and UI feedback.
- About/legal: app name, real version, GPL-3.0 license statement, and repository links for project, LICENSE, and CREDITS.md are surfaced without editing legal text.

### Behavior Preserved

- Update checker networking, latest-release parsing, version comparison, APK download, package verification, and installer launch logic were not edited.
- Diagnostic report generation/export logic was not edited.
- Notification service code, lock-screen behavior, battery behavior, playback, stream resolution, downloads/offline logic, queue behavior, lyrics behavior, repositories, and persistence were not edited.
- No fake version, fake update state, fake premium feature, or dead setting toggle was added.

### Performance Safeguards

- No heavy blur, network call from Composables beyond existing view-model actions, long blocking work, new diagnostics collection, or expensive list effects were added.
- Settings uses static glass surfaces, bounded icons, and simple expanded sections.

### Intentional Non-Changes

- Home, Search, Library, Downloads, MiniPlayer, full player, Queue, and Lyrics surfaces were not redesigned again.
- `PlayerConnection.kt`, `MusicService.kt`, `StreamUrlResolver.kt`, `ExoDownloadService.kt`, `DownloadUtil.kt`, playback queue files, lyrics files, update-check implementation files, diagnostics/export implementation files, data/repository files, Gradle, release signing, package identity, namespace, application ID, license files, credits files, and attribution were untouched.

### Validation Results

- `.\gradlew.bat clean assembleDebug`: PASS under JDK 21.
- `.\gradlew.bat lintDebug`: PASS under JDK 21.
- Device smoke test: NOT RUN. ADB reported no connected devices after the daemon restarted, so Phase 10 was validated with build and lint only.

### Remaining Risks

- Update checker outcome depends on live network/GitHub availability.
- Diagnostics export depends on Android share-sheet availability.
- Notification and lock-screen behavior remains OEM-dependent.
- About legal links depend on an installed browser or URL handler.

### Phase 11 Follow-Up

Phase 11 only should focus on responsiveness, accessibility, consistency, and final UI polish. It should not change playback, stream resolution, downloads/offline logic, update logic, diagnostics logic, repositories, Gradle, release signing, package identity, license, credits, or attribution.

## Phase 11 Responsiveness Accessibility Consistency And Final UI Polish

Phase 11 is a refinement-only pass across the OmniGlass UI. It preserves playback behavior, search behavior, stream resolution, queue behavior, downloads/offline logic, lyrics behavior, repository/data contracts, update-check logic, diagnostics/export logic, Gradle, release signing, package identity, namespace, application ID, license files, credits files, and attribution.

### Files Changed

- `app/src/main/kotlin/com/omnitune/app/ui/screens/SettingsScreen.kt`: added minimum touch heights for settings rows, action buttons, enum dialog options, and slider rows; refined action arrow content descriptions to include the destination label.
- `app/src/main/kotlin/com/omnitune/app/ui/screens/DownloadsScreen.kt`: added minimum row and button sizes for completed/active/failed download rows without changing download state mapping or playback callbacks.
- `docs/qa/OMNIGLASS_FINAL_QA.md`: added the final QA ledger for completed phases, validation results, smoke tests, remaining QA gaps, release-blocking items, and device-dependent risks.
- `docs/ui/OMNIGLASS_UI_OVERHAUL.md`: appended Phase 11 notes and validation results.

### Consistency And Accessibility Changes

- Settings row and button touch areas now use explicit minimum heights for safer compact-screen and accessibility behavior.
- Download rows keep their existing state-driven playability while using more consistent minimum row/button sizing.
- Settings action arrows now expose destination-specific content descriptions such as `Open Appearance`.
- No decorative icon descriptions were added; decorative icons remain silent.

### Responsiveness And Long Text

- Existing ellipsis behavior for Search, Queue, Downloads, MiniPlayer, full player, and Settings text was retained.
- Device smoke ran on a 1080x2400 / 440 dpi device, approximately 393dp wide.
- A dedicated 360dp emulator/override review was not run in Phase 11.
- Missing-artwork fallback was not force-tested with a known artwork-less track.

### Performance Safeguards

- No heavy blur, per-frame image effects, new animation loops, stream resolver calls, network calls from Composables, repository calls, or expensive list effects were added.
- The code changes are static layout/accessibility modifiers only.

### QA Documentation

- `docs/qa/OMNIGLASS_FINAL_QA.md` records all completed phases, build/lint status, smoke tests run, smoke tests not run, release-blocking issues, non-blocking issues, and device-dependent risks.
- The QA ledger explicitly records offline completed-download playback not tested, active/failed download state QA pending, missing-artwork fallback not force-tested, OEM notification/lock-screen dependency, lyrics surface unavailable, and queue count lag after swipe-remove.

### Validation Results

- First `.\gradlew.bat clean assembleDebug` attempt failed because the shell `JAVA_HOME` pointed to JDK 17 while the project requires source release 21.
- Retried with `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot`.
- `.\gradlew.bat clean assembleDebug`: PASS with JDK 21.
- `.\gradlew.bat lintDebug`: PASS with JDK 21.
- `adb devices`: `138898743000055	device`.
- Device smoke test: FAIL/PARTIAL. App launch, Home, Search, search tap playback, MiniPlayer, full player, play/pause, seek, shuffle, repeat, Queue, Library, Downloads render, Settings render/scroll, update checker, diagnostics share sheet, About/license/credits visibility, playback across tabs, and inset/no-overlap checks passed. Completed-download playback did not visibly switch metadata away from the already-playing Search track during the Phase 11 run and must be re-tested in Phase 12.

### Intentional Non-Changes

- No ViewModels were changed in Phase 11.
- `PlayerConnection.kt`, `MusicService.kt`, `StreamUrlResolver.kt`, `ExoDownloadService.kt`, `DownloadUtil.kt`, playback queue implementation files, queue persistence files, lyrics logic files, update-check logic files, diagnostics/export logic files, data/repository files, Gradle files, release/signing files, package identity, namespace, application ID, license text, credits text, and attribution were not touched.
- No fake songs, playlists, artists, albums, downloads, lyrics, update states, or version data were added.
- No dead controls, new routes, feature toggles, or release-preparation changes were added.

### Remaining Risks

- Completed-download playback needs a clean Phase 12 re-test because Phase 11 did not prove it.
- Offline completed-download playback remains untested.
- Active/failed download state QA remains pending until real active/failed downloads exist.
- Missing-artwork fallback still needs force-testing on device.
- Lyrics display surface remains unavailable.
- Queue count may lag after swipe-remove until player state recomposes.
- OEM notification and lock-screen behavior remains device-dependent.

### Phase 12 Follow-Up

Phase 12 only should perform final regression, release-readiness audit, screenshots, and a go/no-go report. It should not bump versions, create release APKs, change playback, change downloads/offline logic, change update/diagnostics logic, change Gradle/release signing, or alter license/credits text unless explicitly approved.

## Phase 12 Final Regression Release-Readiness Audit Screenshots And Go No-Go

Phase 12 performed the final regression audit with device `138898743000055`. It did not bump versions, create release APKs, publish artifacts, change playback, change stream resolution, change downloads/offline logic, change queue behavior, change lyrics behavior, change update-check logic, change diagnostics/export logic, change repositories, change Gradle/release signing, or alter license/credits text.

### Files Changed

- `docs/qa/OMNIGLASS_RELEASE_READINESS_REPORT.md`: added final release-readiness audit, completed-download playback investigation, screenshot list, protected-surface check, and go/no-go decision.
- `docs/qa/OMNIGLASS_FINAL_QA.md`: updated final QA ledger with Phase 12 NO-GO status.
- `docs/ui/OMNIGLASS_UI_OVERHAUL.md`: appended Phase 12 notes.
- `docs/qa/screenshots/omni-glass/`: captured partial release-readiness screenshots, including the completed-download playback failure evidence.

### Completed-Download Playback Result

- Test A, no active Search playback: FAIL.
- Test B, while Search playback active: FAIL.
- Test C, offline completed-download playback: NOT RUN because online completed-download playback already failed.
- Observed Phase 12 failure: after Search playback started `Instant Crush (feat. Julian Casablancas)`, tapping completed `Veridis Quo` did not switch MiniPlayer/full player metadata to the downloaded track.

### Investigation Summary

- `DownloadsScreen.kt` still limits row playback to `Download.STATE_COMPLETED` and calls the passed `onPlayDownload(download)` callback.
- Incomplete, queued, stopped, failed, and unknown download states remain non-playable in presentation state.
- The failure was not proven to be a `DownloadsScreen.kt` row callback regression.
- The callback passed from `MainActivity.kt` manually builds a download `MediaItem`; changing that handoff was outside the Phase 12 approved UI-only fix surface, so no code fix was applied.

### Validation Results

- `.\gradlew.bat clean assembleDebug`: PASS with JDK 21.
- `.\gradlew.bat lintDebug`: PASS with JDK 21.
- `adb devices`: `138898743000055	device`.
- Device smoke test: FAIL because completed-download playback failed.

### Screenshots

Captured under `docs/qa/screenshots/omni-glass/`:

- `01_home.png`
- `02_search_results.png`
- `04_full_player_download_failure.png`
- `06_library.png`
- `07_downloads.png`
- `08_settings.png`

The full release screenshot set was not completed because the release-blocking completed-download playback failure was reproduced.

### Final Decision

NO-GO.

Completed-download playback is release-blocking. Phase 13 should be a focused bugfix for completed-download playback with explicit approval to inspect and, if required, modify the correct playback/download integration surface.

## Phase 13 Completed Download Playback Bugfix

Phase 13 fixed the release-blocking completed-download playback failure discovered in Phase 12. It did not redesign UI, change Search behavior, change stream resolution, change download service architecture, change queue behavior, change lyrics behavior, change update-check logic, change diagnostics/export logic, change Gradle/release signing, bump version, create release APKs, or alter license/credits text.

### Root Cause

The completed-download callback in `MainActivity.kt` built a manual `MediaItem` with AndroidX title metadata but without OmniTune's app-level `MediaMetadata` tag. `PlayerConnection` and `MusicService` use the app metadata tag as the current metadata source for MiniPlayer and full player. As a result, tapping a completed download could fail to replace the visible current item metadata from the previous Search track.

### Files Changed

- `app/src/main/kotlin/com/omnitune/app/MainActivity.kt`: updated completed-download playback handoff to build the tapped item through the normal app `toMediaItem()` metadata path, with a completed-state guard.
- `docs/qa/OMNIGLASS_RELEASE_READINESS_REPORT.md`: appended Phase 13 root cause, fix summary, validation, and release-readiness update.
- `docs/qa/OMNIGLASS_FINAL_QA.md`: appended Phase 13 final QA status.
- `docs/ui/OMNIGLASS_UI_OVERHAUL.md`: appended Phase 13 notes.

### Fix Summary

- The main composable now receives the injected `MusicDatabase`.
- Completed-download playback looks up the tapped download id in the local database and uses existing real song metadata when available.
- Older downloads that only stored title data use an honest app-metadata fallback with the real download id and stored title, leaving missing artist/artwork as fallback UI.
- Playback still routes through `PlayerConnection.playQueue(ListQueue(title = "Downloads", items = listOf(mediaItem)))`.
- Incomplete, queued, stopped, failed, and unknown downloads remain non-playable.

### Validation Results

- `.\gradlew.bat clean assembleDebug`: PASS with JDK 21.
- `.\gradlew.bat lintDebug`: PASS with JDK 21.
- `adb devices`: `138898743000055	device`.
- Debug APK install: PASS.

### Device Results

- Test A, clean/no active Search playback: PASS. Completed `Veridis Quo` opened full player as current item.
- Test B, while Search playback active: PASS. Active Search track `Da Funk / Daftendirekt` switched to completed `Veridis Quo`.
- Test C, offline completed-download playback: PASS after disabling Wi-Fi/mobile data and force-stopping the app.
- MiniPlayer metadata after completed download: PASS.
- Full player metadata after completed download: PASS.
- Search tap playback after fix: PASS.
- Settings render after fix: PASS.

### Remaining Risks

- Existing old downloads may only have title metadata because historical download requests stored title-only data.
- Artist/artwork for old downloads appears only if the track exists in the local database.
- Active/failed download state QA remains pending until real active/failed downloads exist.
- A separate release packaging phase should rerun full screenshots and final release checklist before publishing.

### Release-Readiness Update

CONDITIONAL GO.

The Phase 12 completed-download playback blocker is fixed. Proceed only to a separate final release packaging/checklist phase, not directly to publishing.

## Phase 14 Final Release Verification

Phase 14 performed final release verification, captured the final screenshot set, updated QA documentation, and drafted release notes. It did not change app behavior, redesign UI, bump version, create a release tag, publish a release, or modify protected runtime, Gradle, signing, package identity, license, credits, or attribution files.

### Files Changed

- `docs/qa/OMNIGLASS_RELEASE_READINESS_REPORT.md`: appended Phase 14 release verification and decision.
- `docs/qa/OMNIGLASS_FINAL_QA.md`: appended Phase 14 final QA ledger.
- `docs/ui/OMNIGLASS_UI_OVERHAUL.md`: appended Phase 14 notes.
- `docs/qa/OMNIGLASS_RELEASE_NOTES_DRAFT.md`: added draft release notes.
- `docs/qa/screenshots/omni-glass-final/`: captured fresh final screenshots.

### Validation Results

- `.\gradlew.bat clean assembleDebug`: PASS with JDK 21.
- `.\gradlew.bat lintDebug`: PASS with JDK 21.
- `.\gradlew.bat assembleRelease`: NOT RUN - release signing environment variables were not available locally.
- Device: `138898743000055`.
- Device full regression: PASS for core playback, Search playback, MiniPlayer, full player, Queue/Add to Queue, Library, Downloads, Settings, update checker, diagnostics/export, About/Credits/License, playback across tabs, and inset/no-overlap checks.

### Completed Download Verification

- Test A, clean/no active Search playback: PASS with completed `Veridis Quo`.
- Test B, Search playback active: PASS. Active `Da Funk / Daftendirekt` switched to `Veridis Quo`.
- Test C, offline: PASS after disabling Wi-Fi/mobile data and force-stopping the app.
- MiniPlayer/full-player metadata after completed download: PASS.

### Screenshots

Captured under `docs/qa/screenshots/omni-glass-final/`:

- `01_home.png`
- `02_search_results.png`
- `03_miniplayer.png`
- `04_full_player.png`
- `05_queue.png`
- `06_library.png`
- `07_downloads.png`
- `08_settings.png`
- `09_about_license.png`
- `10_update_checker.png`
- `11_diagnostics_share.png`

### Remaining Risks

- Signed release packaging was not run locally because release signing credentials were unavailable.
- Active/failed download visual-state QA remains pending until real active/failed downloads exist.
- Lyrics display surface remains unavailable.
- OEM notification/lock-screen behavior remains device-dependent.
- Older downloads may have limited artist/artwork metadata unless backed by a local database song record.

### Release-Readiness Decision

CONDITIONAL GO.

The Phase 13 completed-download playback blocker remained fixed through Phase 14, including offline playback. Run signed release packaging and final release operations only in a separate explicit release task.
