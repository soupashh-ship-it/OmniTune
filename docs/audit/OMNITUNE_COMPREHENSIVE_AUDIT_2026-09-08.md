# OmniTune Comprehensive Audit

Audit date: 2026-09-08

Main project: `D:\code\omnitune`

Donor/reference project: `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0`

Scope: source inspection, donor comparison, Gradle task discovery, debug lint, release build, previously completed debug build and unit-test run. No product code was changed during this pass.

Runtime limitation: no Android device or emulator was attached, so findings that require actual touch navigation, PiP entry, Android Auto, media buttons, TalkBack, or visual screenshot comparison are source/build based.

## Executive Summary

Build status:

- `.\gradlew.bat assembleDebug`: passed in the previous verification pass on the current codebase.
- `.\gradlew.bat :app:testDebugUnitTest`: passed in the previous verification pass on the current codebase.
- `.\gradlew.bat :app:assembleRelease`: passed during this audit.
- `.\gradlew.bat :app:lintDebug`: failed during this audit with 184 errors, 55 warnings, and 8 hints.
- Donor Gradle task discovery succeeded after Android NDK 27.0.12077973 was installed by Gradle.

Issue counts:

- P0: 0 confirmed.
- P1: 11 high-priority findings.
- P2: 15 medium-priority findings.
- P3: 10 low-priority findings.
- P4: 10 optional QoL/polish improvements.

Most serious risks:

- The static-quality gate is red: `lintDebug` currently fails on restricted API use, missing translations, Compose modifier/state issues, and other warnings.
- Several ported SuvMusic UI surfaces are visible but backed by fake or no-op OmniTune state: HQ Audio search, related songs, output devices, download retry/refresh, AI player status, and launcher/splash icon switching.
- Android system integration is incomplete: PiP actions, deep links, audio share intents, media browser exposure, package visibility queries, and a boot permission are mismatched with the code.
- Some user secrets are still stored and displayed as plain preferences even though OmniTune already has encrypted preference helpers.

Overall donor parity assessment:

OmniTune now contains many SuvMusic-style UI files and visual components, including the main player styles and mini-player variants, but the port is not yet 1:1 in behavior. The biggest donor parity gaps are not color or branding problems; they are wiring problems where donor UI controls survived but donor state, manifest declarations, media-service contracts, and repository adapters did not.

Overall stability assessment:

Core compile paths are green, but lint failure, incomplete system contracts, fake data providers, and duplicated player progress polling make the app feel pre-release rather than production-stable. Playback fundamentals are better covered than surrounding screens, but player-adjacent integrations need focused cleanup.

Overall QoL/polish assessment:

The fastest wins are to make visible controls honest, wire the existing settings, reduce polling recompositions, tighten update/download/search behavior, and add tests around these contracts. Once those are fixed, polish work should focus on donor-faithful transitions, screenshots, accessibility semantics, and screen-by-screen runtime validation.

## P0 Critical

No confirmed P0 issues were found in this source/build-only pass.

The nearest P0 candidates are PiP/media-session integration and secret storage, but they are currently better classified as P1 because core playback still builds and the secrets are user-entered app tokens rather than confirmed leaked credentials.

## P1 High

### 1. Debug lint gate fails

Priority: P1

Confidence: CONFIRMED

Category: build quality / release readiness

OmniTune location:

- `D:\code\omnitune\app\build\intermediates\lint_intermediate_text_report\debug\lintReportDebug\lint-results-debug.txt`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\MainActivity.kt:296`

Donor location:

- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\MainActivity.kt`

Explanation:

`.\gradlew.bat :app:lintDebug` fails with 184 errors and 55 warnings. The first blocking error is a restricted API override of `ComponentActivity.dispatchKeyEvent` in `MainActivity.kt:296`. Lint also reports 176 missing translations, 21 Compose `ModifierParameter` issues, 12 vector-raster warnings, 7 `AutoboxingStateCreation` issues, 5 more restricted API reports, and other Compose/runtime hygiene warnings.

User impact:

The app can compile, but a normal Android quality gate is red. CI or release checks that include lint will fail, and real issues such as recomposition churn and manifest/query problems are hidden in the noise.

Recommended fix direction:

Create a lint burn-down batch. First resolve or explicitly suppress the donor-matched volume-key restricted API after deciding whether to keep that exact behavior. Then handle missing translations by adding fallback strategy or disabling incomplete locale resources, and fix the Compose warnings that point at measurable runtime risk.

### 2. Downloads screen lists library songs, not completed downloads

Priority: P1

Confidence: CONFIRMED

Category: downloads / data correctness

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\DownloadsViewModel.kt:54`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\db\dao\SongDao.kt:29`

Donor location:

- Donor uses dedicated download repository/state paths rather than treating library membership as a completed download list.

Explanation:

`DownloadsViewModel.downloadedSongs` is sourced from `database.songsByRowIdAsc().first()`. The DAO query behind that call returns songs where `inLibrary IS NOT NULL`, ordered by row id. That means the Downloads screen is actually showing the library, not verified completed offline downloads.

User impact:

Users can see non-downloaded songs in Downloads, believe content is available offline when it is not, and delete or manage the wrong items.

Recommended fix direction:

Drive the Downloads screen from Media3 `DownloadManager` state plus the app database's offline/cache metadata. A song should enter the completed list only when a completed download or verified playable offline cache exists.

### 3. PiP UI is exposed but manifest and action wiring are incomplete

Priority: P1

Confidence: CONFIRMED

Category: lifecycle / player / Android system integration

OmniTune location:

- `D:\code\omnitune\app\src\main\AndroidManifest.xml:27`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\MainActivity.kt:320`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\pip\PipHelper.kt:101`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\pip\PipActionReceiver.kt:9`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\playback\MusicService.kt:199`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\playback\PlaybackNotificationManager.kt:294`

Donor location:

- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\AndroidManifest.xml:91`
- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\AndroidManifest.xml:479`
- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\MainActivity.kt:429`

Explanation:

OmniTune calls `enterPictureInPictureMode`, and `PipHelper` creates broadcast actions for `PipActionReceiver`. However, `MainActivity` lacks `android:supportsPictureInPicture="true"`, and `PipActionReceiver` is not declared in the manifest despite the class comment saying it is. The action strings sent by `PipActionReceiver` use `com.omnitune.app.action.*`, while `MusicService` handles notification action constants under `com.omnitune.app.playback.action.*`.

Donor behavior:

SuvMusic declares PiP support on `MainActivity`, declares a PiP receiver, guards PiP entry by player/settings/config state, updates ViewModel PiP mode, and updates PiP params on lifecycle transitions.

User impact:

PiP may never enter on supported devices, and if actions appear they are unlikely to control playback correctly. This is a high-visibility player regression.

Recommended fix direction:

Make a single PiP contract: manifest support, declared receiver, one shared action namespace, service handling for those actions, settings gating, and lifecycle state updates. Add a manifest/action contract test.

### 4. Deep links and audio file intents are parsed in code but not reachable from Android

Priority: P1

Confidence: HIGH CONFIDENCE

Category: navigation / intents

OmniTune location:

- `D:\code\omnitune\app\src\main\AndroidManifest.xml:27`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\viewmodels\MainViewModel.kt:60`

Donor location:

- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\AndroidManifest.xml:108`
- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\AndroidManifest.xml:127`
- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\MainActivity.kt:370`

Explanation:

`MainViewModel.handleIncomingIntent` can parse YouTube links and audio URIs, but OmniTune's manifest only declares MAIN/LAUNCHER for `MainActivity`. It has no YouTube/YT Music deep-link filters and no `audio/*` VIEW filters. The activity also has no `onNewIntent` override, so repeated single-task intents have no visible update path.

Donor behavior:

SuvMusic declares YouTube/YT Music HTTPS filters, audio content/file filters, custom app scheme filters, and has an `onNewIntent` handler.

User impact:

Opening supported music links or audio files from Android share/open-with flows will not reliably route into OmniTune, even though code exists to handle them.

Recommended fix direction:

Add only the intent filters OmniTune truly supports, implement `onNewIntent` with state that recomposes, and harden YouTube host validation to exact host/suffix matching.

### 5. HQ Audio search tab is visible but not backed by a remote audio repository

Priority: P1

Confidence: CONFIRMED

Category: incomplete port / search

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\viewmodel\SearchViewModel.kt:186`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\viewmodel\SearchViewModel.kt:254`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\SearchScreen.kt:315`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\SearchScreen.kt:595`

Donor location:

- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\ui\screens\viewmodel\SearchViewModel.kt:86`
- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\ui\screens\viewmodel\SearchViewModel.kt:452`

Explanation:

OmniTune displays `YouTube Music` and `HQ Audio` tabs, but `SearchViewModel.searchInternal` always calls `YouTube.searchSummary(query)`. The `SearchTab.REMOTE` branch in the UI renders the same result state with different labels. The donor injects `RemoteAudioRepository` and routes remote searches to that repository.

User impact:

The HQ Audio tab is misleading. Users expect a different source but receive YouTube-backed data.

Recommended fix direction:

Either port/adapt the donor remote audio repository properly, hide the HQ tab until it is functional, or relabel it so it does not promise a separate source.

### 6. Related songs sheet always resolves to empty content

Priority: P1

Confidence: CONFIRMED

Category: incomplete port / player

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\viewmodels\PlayerViewModel.kt:221`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\player\components\RelatedSheet.kt:184`

Donor location:

- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\ui\screens\player\PlayerViewModel.kt:438`
- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\ui\screens\player\PlayerViewModel.kt:861`

Explanation:

`PlayerViewModel.refreshRelatedSongs` sets loading, then assigns `_relatedSongs.value = emptyList()` in the try block. The player sheet therefore has a donor-style Related UI but no data integration.

Donor behavior:

SuvMusic wires related/radio content through recommendation and smart-queue logic.

User impact:

A visible player action consistently ends in "No related tracks available", which feels broken and unfinished.

Recommended fix direction:

Back the sheet with OmniTune's recommendation/autoplay resolver or remove the affordance until the adapter exists. Prefer a small player-related-content adapter that maps OmniTune songs to the donor UI model.

### 7. Output device picker is fake and cannot switch devices

Priority: P1

Confidence: CONFIRMED

Category: incomplete port / player / system integration

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\viewmodels\PlayerViewModel.kt:235`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\player\components\OutputDeviceSheet.kt:65`

Donor location:

- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\ui\screens\player\PlayerViewModel.kt:840`
- Donor music player device routing logic around its `MusicPlayer.refreshDevices` implementation.

Explanation:

`PlayerViewModel.refreshDevices` always emits one selected `OutputDevice("phone_speaker", "This Device", PHONE, true)`. The `OutputDeviceSheet` renders a current device and then filters selected devices out of the available list, so the picker cannot offer or switch to anything.

User impact:

Users see an output-device control that looks real but does not discover Bluetooth/Cast/route devices and cannot switch output.

Recommended fix direction:

Integrate Android media route/device discovery or use system output switcher APIs where available. If OmniTune cannot support switching yet, change the UI to a clear read-only current-device indicator.

### 8. MediaLibrarySession is not exposed like a media library service

Priority: P1

Confidence: HIGH CONFIDENCE

Category: media session / external controls

OmniTune location:

- `D:\code\omnitune\app\src\main\AndroidManifest.xml:48`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\playback\MusicService.kt:82`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\playback\SessionManager.kt:28`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\playback\MusicSessionCallback.kt:27`

Donor location:

- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\AndroidManifest.xml:432`

Explanation:

OmniTune's service extends `MediaLibraryService` and creates a `MediaLibrarySession`, but the manifest declares only `androidx.media3.session.MediaSessionService`, the service is `exported=false`, and the callback does not provide browser tree overrides such as library root/children. Donor declares MediaSession, MediaLibrary, and legacy MediaBrowser actions with an exported service.

User impact:

External clients such as Android Auto, Assistant, media browsers, and some hardware/media integrations may fail to discover or browse the app properly.

Recommended fix direction:

Decide whether OmniTune wants a true browsable media library. If yes, declare the correct service actions, export policy, permissions, and callback tree. If no, downgrade the service/session contract to match actual capabilities.

### 9. In-app volume slider setting is ignored while hardware volume keys are always intercepted during playback

Priority: P1

Confidence: CONFIRMED

Category: settings / interaction

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\MainActivity.kt:136`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\MainActivity.kt:296`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\player\PlayerScreen.kt:647`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\settings\SettingsViewModel.kt:49`

Donor location:

- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\MainActivity.kt:490`
- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\ui\screens\settings\PlaybackSettings.kt:296`

Explanation:

`MainActivity` initializes `isVolumeSliderEnabled = true` and never observes `VolumeSliderEnabledKey`. `PlayerScreen` passes `volumeSliderEnabled = true` into overlays. The settings state exists in `SettingsViewModel`, but the visible behavior is hardcoded.

Donor behavior:

SuvMusic observes the volume slider preference in `MainActivity` and passes it through to player overlays.

User impact:

When music is playing, hardware volume keys are intercepted even if the user expects normal system volume behavior, and the player overlay cannot reflect the saved setting.

Recommended fix direction:

Expose the setting in the appropriate settings screen, collect it lifecycle-aware in `MainActivity`, pass it through `PlayerScreen`, and add a small test that the overlay and key handling follow the preference.

### 10. Logo picker promises launcher and splash changes that the app cannot perform

Priority: P1

Confidence: CONFIRMED

Category: settings / app identity / incomplete port

OmniTune location:

- `D:\code\omnitune\app\src\main\AndroidManifest.xml:14`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\settings\LogoPickerSection.kt:139`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\settings\SettingsViewModel.kt:224`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\models\LogoVariant.kt:14`

Donor location:

- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\AndroidManifest.xml:184`
- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\data\SessionManager.kt:2481`

Explanation:

The settings UI says logo selection affects the launcher icon and splash, but OmniTune has no activity aliases for variants and `SettingsViewModel.setLogoVariant` only writes a preference. Donor has many launcher aliases and toggles them through `PackageManager.setComponentEnabledSetting`.

User impact:

A user-facing personalization feature overpromises and only partially works inside the app.

Recommended fix direction:

Either port the launcher-alias mechanism with OmniTune-branded resources or change the settings copy to say the variant affects in-app surfaces only.

### 11. API tokens and Discord token are stored as plain preferences and displayed as plain text

Priority: P1

Confidence: CONFIRMED

Category: security / privacy

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\settings\AISettingsScreen.kt:107`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\settings\DiscordSettingsScreen.kt:95`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\settings\SettingsViewModel.kt:243`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\utils\SecurePreferenceCipher.kt:17`

Donor location:

- Donor uses encryption for at least some sensitive account/token paths; OmniTune also already encrypts YouTube cookies, PO tokens, and ListenBrainz token during app startup.

Explanation:

AI provider API keys and the Discord token are stored directly via `setPreference` and shown in normal `OutlinedTextField`s. OmniTune already has `SecurePreferenceCipher` and migration code for other sensitive values, so this is inconsistent with the app's own security model.

User impact:

Tokens can be shoulder-surfed in settings and are stored less safely than other app credentials.

Recommended fix direction:

Encrypt these preference values, migrate existing plain values, show masked fields with reveal/clear controls, and avoid writing on every keystroke until apply/save or debounce.

## P2 Medium

### 12. Mini-player and bottom-navigation customization preferences are not applied

Priority: P2

Confidence: CONFIRMED

Category: donor parity / settings

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\MainActivity.kt:485`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\MainActivity.kt:620`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\player\ExpandablePlayerSheet.kt:65`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\settings\SettingsViewModel.kt:102`

Donor location:

- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\MainActivity.kt:490`
- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\MainActivity.kt:851`
- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\MainActivity.kt:1072`

Explanation:

OmniTune has settings state for `navBarAlpha`, `navBarBlur`, and `miniPlayerAlpha`, and the sheet supports `userAlpha`, `glassBlurAmount`, and `artworkShape`, but `MainActivity` does not pass those values to the bottom nav or player sheet. Defaults are used instead.

User impact:

Customization controls do not produce the donor-visible effect users expect.

Recommended fix direction:

Collect the preferences in `MainActivity` and pass them through to `ExpressiveBottomNav` and `ExpandablePlayerSheet`, mirroring donor wiring.

### 13. Force max refresh rate setting is visible but unused

Priority: P2

Confidence: CONFIRMED

Category: settings / performance

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\settings\AppearanceSettingsScreen.kt:138`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\settings\SettingsViewModel.kt:66`

Donor location:

- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\MainActivity.kt:244`

Explanation:

The setting is read and toggled in settings, but `MainActivity` does not apply preferred display mode or refresh-rate behavior. Donor applies and resets max refresh rate based on the preference.

User impact:

The setting has no effect, which makes high-refresh animation polish inconsistent and erodes trust in settings.

Recommended fix direction:

Port donor refresh-rate application in an Activity-scoped effect and reset display mode when disabled.

### 14. Keep screen on setting is visible but unused

Priority: P2

Confidence: CONFIRMED

Category: settings / lifecycle

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\settings\MiscScreen.kt:108`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\settings\SettingsViewModel.kt:60`

Donor location:

- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\MainActivity.kt:556`

Explanation:

The setting persists, but no Activity/window effect adds or clears `FLAG_KEEP_SCREEN_ON`. Donor applies it when the player is expanded.

User impact:

Users who enable it still get normal screen timeout during playback/player use.

Recommended fix direction:

Apply `FLAG_KEEP_SCREEN_ON` based on saved preference and player expansion state, and clear it in disposal/lifecycle transitions.

### 15. PiP state is not integrated into app UI lifecycle

Priority: P2

Confidence: HIGH CONFIDENCE

Category: lifecycle / player

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\MainActivity.kt:333`

Donor location:

- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\MainActivity.kt:447`
- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\ui\screens\player\PlayerScreen.kt:401`

Explanation:

OmniTune overrides `onPictureInPictureModeChanged` but only calls `super`. Donor updates the player ViewModel and renders PiP-specific player content.

User impact:

Even after manifest/action fixes, OmniTune may leave normal full-screen player UI state active while in PiP or fail to restore correctly afterward.

Recommended fix direction:

Add PiP mode state to the player adapter and use it to render/hide appropriate UI.

### 16. App shell and player both poll progress every 500 ms

Priority: P2

Confidence: CONFIRMED

Category: performance / state management

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\MainActivity.kt:446`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\player\PlayerScreen.kt:156`

Donor location:

- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\MainActivity.kt:540`

Explanation:

OmniTune runs a `while (true)` polling loop in the app shell and another inside the expanded player. These update Compose state every 500 ms. Donor separates stable playback info from distinct progress flow and keeps raw progress observation closer to the surfaces that need it.

User impact:

Playback can cause unnecessary recomposition of high-level UI and player surfaces, increasing jank risk on artwork-heavy screens.

Recommended fix direction:

Expose a lifecycle-aware progress flow from `PlayerConnection` or playback adapter, collect it only where needed, and use stable data classes/derived state for the miniplayer.

### 17. Downloads retry, refresh, progress, and failure states are no-op or missing

Priority: P2

Confidence: CONFIRMED

Category: downloads / UX

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\DownloadsViewModel.kt:64`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\DownloadsViewModel.kt:85`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\DownloadsViewModel.kt:120`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\DownloadsScreen.kt:139`

Donor location:

- Donor exposes more complete download repository/state behavior.

Explanation:

`retryDownload` has an empty body, `refreshDownloads` is empty, and `downloadItems` maps songs to default `SongItem` values rather than live `DownloadManager` progress/failure. The screen has a polished list but not the expected downloader state.

User impact:

Failed downloads cannot be retried from the visible control path, progress is not meaningful, and refresh does nothing.

Recommended fix direction:

Model download state explicitly: queued, resolving, downloading with percent, completed, failed with error, paused/waiting-for-Wi-Fi. Wire retry/cancel/delete to `DownloadUtil`.

### 18. Downloads Videos tab likely remains empty even for downloaded videos

Priority: P2

Confidence: HIGH CONFIDENCE

Category: downloads / data mapping

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\DownloadsViewModel.kt:60`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\db\entities\Song.kt:243`

Explanation:

`downloadedVideos` filters `downloadedSongs` by `song.isVideo`, but the database-to-presentation mapper does not preserve `isVideo` in the inspected mapping path. The tab is likely always empty unless another path sets the field later.

User impact:

Downloaded videos are hidden from the Videos tab or mixed into Songs incorrectly.

Recommended fix direction:

Preserve media type through the database/entity mapping and back it with real completed download state.

### 19. Updater cannot discover prereleases or the current prerelease APK asset naming

Priority: P2

Confidence: CONFIRMED

Category: update / release management

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\update\GitHubReleaseApi.kt:19`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\update\GitHubReleaseApi.kt:56`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\update\AppUpdateChecker.kt:11`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\update\AppUpdateChecker.kt:38`

Explanation:

`GitHubReleaseApi` fetches only `/releases/latest`, which GitHub uses for stable releases rather than prereleases. `AppUpdateChecker` accepts prerelease configuration, but it never receives a prerelease list to filter. It also only accepts release asset names like `OmniTune-v$version-release.apk` or `OmniTune-v$version-universal-release.apk`, while the current uploaded prerelease APK is named like `OmniTune-1.2.0-pre4-debug.apk`.

User impact:

Users on nightly/prerelease update channel may not see available prereleases, and even if discovered the app may reject the asset.

Recommended fix direction:

Fetch `/releases` when prereleases are allowed, select the newest compatible release by semver/channel, and define a stable APK naming convention for debug/prerelease/manual assets.

### 20. Home screen treats the user as logged in by default

Priority: P2

Confidence: HIGH CONFIDENCE

Category: auth / personalization

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\viewmodel\HomeViewModel.kt:38`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\HomeScreen.kt:201`

Explanation:

`HomeUiState` defaults `isLoggedIn = true` and `userName = "Music Lover"`. The inspected ViewModel code does not replace that with real account/session state before the personalized banner decision.

User impact:

Guest users can see personalized copy and a "For You" banner even when they are not signed in.

Recommended fix direction:

Derive home account state from the real auth/session source and persist dismissals. Default to guest until a session is confirmed.

### 21. Home fallback recommendations are just the first library rows

Priority: P2

Confidence: CONFIRMED

Category: recommendations / UX

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\viewmodel\HomeViewModel.kt:100`

Donor location:

- Donor routes related/radio/recommendation behavior through recommendation and smart queue engines.

Explanation:

`loadLocalRecommendations` reads `database.songsByRowIdAsc().first()` and uses `localSongs.take(12)`. This is deterministic row order, not a recommendation signal.

User impact:

The home screen can feel stale and misleading, especially after imports or library growth.

Recommended fix direction:

Use existing OmniTune playback history, likes, downloads, and recency to score local recommendations. If there is not enough history, label the section honestly as recently added.

### 22. Home pagination can remain stuck loading after thrown exceptions

Priority: P2

Confidence: HIGH CONFIDENCE

Category: network / loading state

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\viewmodel\HomeViewModel.kt:167`

Explanation:

`loadMore` sets `isLoadingMore = true` and calls `YouTube.home(continuation)`, but the inspected function handles `Result.Success` and `Result.Failure` rather than wrapping thrown exceptions in `try/finally`.

User impact:

A network/parser exception during pagination can leave the feed in a stuck loading-more state.

Recommended fix direction:

Use `try/catch/finally`, keep prior content, set a retryable pagination error, and avoid clobbering the main home error unless initial load fails.

### 23. Stream probing is a stub that always reports success

Priority: P2

Confidence: CONFIRMED

Category: network / playback reliability

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\data\StreamRepositoryImpl.kt:76`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\data\StreamRepository.kt:9`

Explanation:

`probeStream(url: String): Boolean = true`. Any caller asking whether a stream is reachable receives a false positive.

User impact:

The app can choose bad URLs, show playable states for unplayable media, or skip fallback too early.

Recommended fix direction:

Implement a bounded `HEAD`/range probe through the same network stack used for playback resolution, with timeout and host validation, or delete the API until real probing is needed.

### 24. Mini-player route visibility is less precise than donor behavior

Priority: P2

Confidence: HIGH CONFIDENCE

Category: navigation / donor parity

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\MainActivity.kt:619`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\MainActivity.kt:530`

Donor location:

- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\MainActivity.kt:752`
- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\MainActivity.kt:834`

Explanation:

OmniTune shows the mini-player whenever a song exists. Donor hides it on routes such as YouTube login, import, and song info, and calculates snackbar/content padding with route-aware mini-player and bottom-nav offsets. OmniTune also creates `innerPadding` from `Scaffold` but does not use it.

User impact:

The mini-player can overlap sensitive/login/import/detail flows, and snackbar/content placement can feel less donor-faithful.

Recommended fix direction:

Port donor route exclusion and padding policy into one layout-state helper, then apply it consistently to nav content, snackbar host, and player sheet.

### 25. Customization settings screen is only partially ported

Priority: P2

Confidence: CONFIRMED

Category: settings / donor parity

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\settings\CustomizationScreen.kt:98`

Donor location:

- `C:\Users\soupa\Downloads\SuvMusic-2.6.6.0\SuvMusic-2.6.6.0\app\src\main\java\com\suvojeet\suvmusic\ui\screens\settings\CustomizationScreen.kt:271`

Explanation:

OmniTune's customization screen currently exposes a much smaller subset, mainly mini-player style and swipe. Donor exposes preview, transparency/blurs, navigation-bar alpha/blur, mini-player alpha, player background/custom image, and home-section visibility. Some corresponding preference state exists in OmniTune but is not surfaced or wired.

User impact:

The port exposes donor visual systems but does not expose the donor-level customization controls users would expect.

Recommended fix direction:

Port donor customization sections in dependency order, wiring only settings that have real OmniTune behavior. Hide or label unavailable controls instead of storing inert preferences.

### 26. AI player status is hardcoded off despite AI settings existing

Priority: P2

Confidence: CONFIRMED

Category: incomplete port / player / settings

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\player\PlayerScreen.kt:587`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\settings\AISettingsScreen.kt:37`

Explanation:

The Liquid Glass player path passes `isAIEnabled = false` and `aiStatus = null`, while AI settings screens and provider key fields exist. This is either a disconnected feature or leftover donor UI.

User impact:

AI-related settings and player affordances can disagree, making the feature appear broken or absent depending on route.

Recommended fix direction:

Define the AI feature contract: remove player AI indicators until real status exists, or add a ViewModel state adapter that reflects configured provider and current processing availability.

## P3 Low

### 27. Missing translations dominate lint output

Priority: P3

Confidence: CONFIRMED

Category: localization

OmniTune location:

- `D:\code\omnitune\app\src\main\res\values\strings.xml:3`
- `D:\code\omnitune\app\src\main\res\values-hi`
- `D:\code\omnitune\app\src\main\res\values-bn`

Explanation:

Lint reports 176 `MissingTranslation` errors for Hindi and Bengali resources. The app either needs complete translated strings or a deliberate localization-resource strategy.

User impact:

Users selecting these languages can see mixed language UI, and lint remains noisy.

Recommended fix direction:

Complete the translations, mark non-translatable strings, or remove incomplete locale resources until they are supported.

### 28. Manifest lacks package visibility query for package inspection

Priority: P3

Confidence: CONFIRMED

Category: manifest / Android 11 package visibility

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\settings\about\AboutMetadata.kt:206`
- `D:\code\omnitune\app\src\main\AndroidManifest.xml`

Explanation:

Lint reports `QueryPermissionsNeeded` because `AboutMetadata` queries package information without matching `<queries>` declarations.

User impact:

About/dependency metadata can fail or become incomplete on Android versions with package visibility restrictions.

Recommended fix direction:

Add narrow `<queries>` entries for the packages/intents that are actually inspected, or remove the package query path.

### 29. Several formatted strings use the default Locale

Priority: P3

Confidence: CONFIRMED

Category: localization / correctness

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\ListeningStatsScreen.kt:121`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\SongInfoScreen.kt:215`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\settings\StorageScreen.kt:471`

Explanation:

Lint reports `DefaultLocale` on numeric/string formatting.

User impact:

Numbers, decimals, and generated text may vary unexpectedly by device locale.

Recommended fix direction:

Use `Locale.US` for protocol/file sizes where stable formatting is required, and user locale for display-only human text.

### 30. Deprecated Compose and Hilt APIs remain after the port

Priority: P3

Confidence: CONFIRMED

Category: maintainability

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\DownloadsScreen.kt:98`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\SearchScreen.kt:328`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\player\components\QueueScreenUI.kt:408`
- Multiple settings screens importing deprecated `androidx.hilt.navigation.compose.hiltViewModel`

Explanation:

Release build logs show deprecated Material tab APIs, deprecated swipe APIs, deprecated icons, and deprecated Hilt Compose integration.

User impact:

Not immediately user-visible, but it increases future upgrade cost and warning noise.

Recommended fix direction:

Modernize APIs in a contained technical-debt pass after core wiring bugs are fixed.

### 31. Vector assets trigger rasterization and duplicate icon warnings

Priority: P3

Confidence: CONFIRMED

Category: resources / APK polish

OmniTune location:

- Lint reports vector logo drawables with 400dp dimensions and duplicate icon resources.

Explanation:

Large vector drawables can be rasterized and duplicate launcher/icon resources add maintenance noise.

User impact:

Small APK/performance and maintenance issue, especially around launcher/logo customization.

Recommended fix direction:

Normalize icon resources as part of the logo-picker/launcher-alias fix.

### 32. `RECEIVE_BOOT_COMPLETED` permission appears unused

Priority: P3

Confidence: HIGH CONFIDENCE

Category: manifest / permissions

OmniTune location:

- `D:\code\omnitune\app\src\main\AndroidManifest.xml:11`

Explanation:

The manifest requests `RECEIVE_BOOT_COMPLETED`, but no boot receiver is declared in the inspected manifest.

User impact:

The app asks for unnecessary capability and increases review/audit noise.

Recommended fix direction:

Remove the permission unless a boot receiver is intentionally added for downloads/playback restoration.

### 33. `onboardingCompleted` is collected but unused

Priority: P3

Confidence: CONFIRMED

Category: state hygiene

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\MainActivity.kt:376`

Explanation:

`onboardingCompleted` is collected in `MainActivity`, but the inspected code does not use it to gate navigation or UI.

User impact:

Minor recomposition/state noise and a sign that onboarding flow logic may have leftover migration state.

Recommended fix direction:

Either wire onboarding state into startup routing or remove the unused collection.

### 34. Stale comments and placeholder migration notes remain

Priority: P3

Confidence: CONFIRMED

Category: cleanup

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\playback\ExoDownloadService.kt:33`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\component\shimmer\ShimmerEffect.kt`

Explanation:

There are stale implementation comments such as "Add this string resource" and generic placeholder comments left from migration or scaffolding.

User impact:

No direct runtime impact, but it lowers maintainability and makes future audits harder.

Recommended fix direction:

Remove stale comments during the cleanup pass, preserving only comments that explain non-obvious behavior.

### 35. Custom clickable UI lacks some accessibility semantics

Priority: P3

Confidence: LIKELY

Category: accessibility

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\navigation\ExpressiveBottomNav.kt`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\DownloadsScreen.kt`
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\settings\LogoPickerSection.kt`

Explanation:

Several donor-style custom surfaces use `.clickable` rather than higher-level selectable/tab semantics. Many icons do have descriptions, but selected state, role, and traversal behavior need TalkBack verification.

User impact:

Screen-reader users may get less useful navigation and selection announcements.

Recommended fix direction:

Add `Role.Tab`, selected semantics, and state descriptions where appropriate without changing visuals.

### 36. `Scaffold` padding is unused

Priority: P3

Confidence: CONFIRMED

Category: layout / Compose hygiene

OmniTune location:

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\MainActivity.kt:498`

Explanation:

Lint reports unused `innerPadding` from `Scaffold`. This usually indicates that content insets are being managed manually or inconsistently.

User impact:

Content/snackbar/IME behavior can drift from expected Material scaffold behavior.

Recommended fix direction:

Either intentionally ignore scaffold padding with a clear route-specific inset policy, or apply the padding through the main content host.

## Donor Parity Gaps

Visual:

- Customization settings are incomplete: OmniTune lacks donor preview, transparency/blur controls, player background/custom image controls, and home-section visibility controls.
- Logo variant UI has donor-like presentation but cannot switch launcher aliases or splash icons.
- Mini-player and bottom nav alpha/blur settings exist but are not passed into visible components.

Animation:

- Player progress is raw-polled in multiple places rather than exposed through donor-like separated playback/progress state.
- PiP-specific player rendering and lifecycle transitions are missing.
- Runtime screenshot/video comparison was not possible without a device, so detailed animation timing parity remains unverified.

Navigation:

- Deep links, audio file intents, custom schemes, and repeated intents are not manifest/lifecycle wired like donor.
- Mini-player visibility is less route-aware than donor.
- Scaffold/snackbar padding policy is less donor-faithful.

Interaction:

- Volume-slider preference is ignored and hardware volume keys are always intercepted during playback.
- Output-device picker looks interactive but is backed by a single fake device.
- Related songs, download retry, download refresh, and HQ Audio search are visible but not fully functional.

Loading/error states:

- Home pagination can remain stuck if a thrown exception bypasses result handling.
- Downloads do not represent queued/downloading/failed states accurately.
- Related songs always reaches an empty state.

Player:

- PiP support, output devices, related songs, AI status, volume overlay settings, and some style customization are disconnected.
- MediaLibrarySession exposure does not match a true media-library service contract.

Queue:

- Queue persistence and UI are comparatively stronger than other areas.
- Queue filter chips are presentational; donor has the same comment, so this is a QoL opportunity rather than a donor mismatch.
- Runtime queue reorder/restore behavior still needs device validation.

Settings:

- Several settings persist but do not affect runtime behavior: volume slider, force refresh rate, keep screen on, nav bar alpha/blur, mini-player alpha, and some logo behavior.
- Sensitive token settings are not using the same encrypted-storage pattern as other credentials.

## Performance Findings

- `MainActivity.kt:446` and `PlayerScreen.kt:156`: duplicate 500 ms progress polling can drive unnecessary recompositions during playback.
- Lint's `AutoboxingStateCreation` findings indicate primitive Compose state could be tightened to reduce allocation pressure.
- Lint's `FrequentlyChangingValue` findings in `SearchScreen.kt` indicate values that change frequently are read in composition instead of being isolated with `derivedStateOf` or effect flows.
- Lint's `UseOfNonLambdaOffsetOverload` findings indicate offset state can cause broader recomposition than necessary.
- Artwork-heavy donor layouts need runtime screenshot and scroll profiling on low-end devices; no device was available during this audit.
- `DownloadUtil.availableDownloadStorageBytes` checks `context.filesDir` while downloads may live elsewhere, so storage-related UX can be inaccurate.

## Architecture / Technical Debt

- Several donor UI affordances were ported without adapter contracts: HQ Audio, related songs, output devices, AI player status, and logo aliases.
- Activity-level system behavior is split between hardcoded local vars and settings state. Volume, PiP, refresh rate, screen-on, nav blur, and mini-player alpha should come from one lifecycle-aware settings adapter.
- Media session type, manifest service actions, and callback capabilities are not aligned. The code says `MediaLibraryService`, while the manifest and callback behavior are closer to a non-browsable media session.
- Search source selection is a UI enum, not a repository routing decision.
- Downloads screen state is not derived from the downloader state machine.
- Authentication/session state is not cleanly reflected in the home screen's default UI state.
- Update-channel behavior exists in UI/ViewModel, but the release API cannot fetch prereleases.

## Dead / Legacy Code

- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\data\StreamRepositoryImpl.kt:76`: `probeStream` is a false-positive stub.
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\DownloadsViewModel.kt:85`: `retryDownload` is a no-op.
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\screens\DownloadsViewModel.kt:120`: `refreshDownloads` is a no-op.
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\viewmodels\PlayerViewModel.kt:221`: related songs implementation is intentionally empty.
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\viewmodels\PlayerViewModel.kt:235`: output device discovery is fake.
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\ui\player\PlayerScreen.kt:587`: AI player state is hardcoded off.
- `D:\code\omnitune\app\src\main\kotlin\com\omnitune\app\MainActivity.kt:376`: onboarding state collection appears unused.
- Donor/SuvMusic references in comments and About credits appear intentional GPL/credit history, not incorrect runtime branding.

## Quality-of-Life Improvements

### A. Honest download state

Problem being solved: Downloads currently looks like a library list and has no meaningful progress/failure/retry state.

Proposed behavior: Show queued/downloading/completed/failed states with progress, retry, cancel, delete, and waiting-for-Wi-Fi messaging.

Expected benefit: Users understand offline availability and can recover failed downloads.

Implementation complexity: MEDIUM

Regression risk: MEDIUM

Priority: P1

### B. Functional related tracks

Problem being solved: Related sheet always empties out.

Proposed behavior: Use OmniTune recommendation/autoplay data to populate related tracks and allow play-next/add-to-queue.

Expected benefit: Better music discovery from the donor player UI.

Implementation complexity: MEDIUM

Regression risk: MEDIUM

Priority: P1

### C. Route-aware mini-player policy

Problem being solved: Mini-player can appear over login/import/detail contexts.

Proposed behavior: Mirror donor exclusions and compute one shared bottom inset for nav, mini-player, snackbar, and IME.

Expected benefit: Less overlap and more donor-faithful navigation polish.

Implementation complexity: LOW

Regression risk: LOW

Priority: P2

### D. Clear account state on Home

Problem being solved: Guest users can see logged-in/personalized UI by default.

Proposed behavior: Default to guest and promote to signed-in UI only after session confirmation.

Expected benefit: More trustworthy onboarding/account experience.

Implementation complexity: LOW

Regression risk: LOW

Priority: P2

### E. Better local recommendations

Problem being solved: Home recommendations are the first rows in the library.

Proposed behavior: Score songs using recent plays, likes, downloads, artists, and freshness.

Expected benefit: Home feels personal without requiring new remote APIs.

Implementation complexity: MEDIUM

Regression risk: LOW

Priority: P2

### F. Prerelease-aware updater

Problem being solved: Nightly/prerelease channel cannot reliably discover or install prerelease APKs.

Proposed behavior: Fetch release lists for prerelease channels and use a stable naming/hash policy.

Expected benefit: Manual prerelease users can upgrade in-app later if desired.

Implementation complexity: LOW

Regression risk: MEDIUM

Priority: P2

### G. Persisted dismissals and screen tabs

Problem being solved: Some banners and tab choices are session-local only.

Proposed behavior: Persist low-risk UI choices such as home banner dismissal and last search tab.

Expected benefit: Fewer repeated nags and fewer taps.

Implementation complexity: LOW

Regression risk: LOW

Priority: P4

### H. Search source honesty

Problem being solved: HQ tab currently gives YouTube results.

Proposed behavior: Either make HQ real or collapse to a single source until remote source exists.

Expected benefit: Less user confusion.

Implementation complexity: LOW to HIGH depending on whether remote backend is ported.

Regression risk: LOW if hiding, HIGH if adding backend.

Priority: P1

### I. System output integration

Problem being solved: Output-device sheet cannot switch routes.

Proposed behavior: Use Android route/device APIs or launch the system output switcher where available.

Expected benefit: Users can move playback to headphones/speakers from the player.

Implementation complexity: MEDIUM

Regression risk: MEDIUM

Priority: P1

### J. Accessible custom controls

Problem being solved: Donor-styled custom clickables may not announce roles/selected state.

Proposed behavior: Add semantics to bottom nav, tabs, selection rows, and custom setting cards.

Expected benefit: Better TalkBack support without changing visuals.

Implementation complexity: LOW

Regression risk: LOW

Priority: P3

## Polish Improvements

- Add screenshot comparison passes for Home, Search, Downloads, Library, Settings, mini-player, full player, queue, lyrics, and empty/loading/error states.
- Make keyboard dismissal and focus behavior consistent on Search and Login.
- Normalize snackbar placement with mini-player/bottom-nav visibility.
- Replace default Material tab indicators where donor uses custom styling.
- Use one artwork placeholder policy across lists, player, queue, downloads, and empty states.
- Add haptics consistently to high-confidence player actions if donor behavior expects it.
- Audit status/navigation bar icon color on light/dark and dynamic color modes.
- Replace stale placeholder comments and migration notes with real docs or delete them.
- Tighten font scaling and truncation behavior on compact screens.
- Keep donor animation specs, but runtime-profile blur/gradient-heavy surfaces before expanding them to more routes.

## Testing Gaps

Existing coverage is strongest around queue persistence, playback recovery, stream resolution, lyrics, diagnostics, backups, and several model/policy units. Important missing or insufficient tests:

- Downloads ViewModel should prove completed downloads, failed downloads, retry, delete, and Videos tab mapping.
- Search ViewModel should prove tab/source routing, debounce cancellation, and remote tab behavior.
- Manifest contract tests should verify PiP support, PiP receiver, deep-link filters, audio intent filters, media-service actions, and package queries.
- Player ViewModel tests should cover related songs, output devices, video-mode toggling, AI status, and settings passthrough.
- MainActivity/layout tests should cover mini-player route exclusions and bottom inset/snackbar policy.
- Settings behavior tests should verify every visible setting has an effect or is intentionally display-only.
- Security migration tests should verify API/Discord tokens encrypt and existing plain values migrate.
- Update tests should cover stable releases, prereleases, debug/prerelease APK names, missing hash, and wrong package.
- Home ViewModel tests should cover guest state, signed-in state, pagination exceptions, banner persistence, and local recommendation ranking.
- UIAutomator/screenshot tests should cover startup, home, search, player expand/collapse, queue, lyrics, downloads, settings, login, and TalkBack-relevant semantics.

## Security / Privacy Sanity Findings

Positive findings:

- Manifest has `android:usesCleartextTraffic="false"`.
- `SecurePreferenceCipher` uses AES/GCM with Android Keystore.
- App startup migrates YouTube cookies, PO tokens, and ListenBrainz tokens to encrypted values.
- Crash snapshot storage avoids exception messages and caps stored data.
- Update installation verifies SHA-256 and package identity before install.
- FileProvider paths are limited to update and diagnostics cache paths.

Issues and risks:

- P1: AI API keys and Discord token are stored/displayed as plain text.
- P2/P3: YouTube link host validation uses `host.contains(...)`; use exact host or suffix checks before accepting deep links.
- P3: Package visibility queries need manifest `<queries>`.
- P3: `RECEIVE_BOOT_COMPLETED` appears unnecessary without a receiver.
- P4: Backup exclusion files only exclude databases. `allowBackup=false` currently reduces risk, but if backup is enabled later, preferences/secrets should be excluded explicitly.
- LIKELY: Login `WebView` enables JavaScript and DOM storage, which is expected for YouTube login, but the success URL detection should be stricter and cancellation/logout cleanup should be runtime-tested.

## Recommended Implementation Order

Batch 1 - Critical correctness:

- Fix the lint red gate enough that real issues remain visible.
- Make visible-but-fake features honest: HQ Audio, related songs, output devices, downloads retry/refresh.
- Encrypt and mask AI/Discord tokens.

Batch 2 - Playback/player:

- Repair PiP manifest/action/service/ViewModel contract.
- Wire volume-slider setting, mini-player alpha/blur/artwork settings, and AI status.
- Replace duplicate progress polling with a shared lifecycle-aware progress flow.

Batch 3 - Navigation/state:

- Add supported deep-link/audio intent filters and `onNewIntent` handling.
- Apply donor route exclusions for mini-player and shared inset/snackbar policy.
- Fix Home auth defaults, pagination error handling, and persisted banner/tab state.

Batch 4 - Donor parity:

- Complete customization settings parity where OmniTune can support the behavior.
- Port launcher alias switching or correct the logo-picker copy.
- Audit settings screen-by-screen for inert controls.

Batch 5 - Performance:

- Address Compose lint warnings around frequently changing values, primitive state, modifiers, and offset lambdas.
- Profile artwork-heavy player/home/search surfaces on real devices.

Batch 6 - QoL:

- Improve local recommendations.
- Add accurate offline/download feedback.
- Add accessible semantics and better keyboard/focus handling.

Batch 7 - Cleanup/polish:

- Remove no-op methods, stale comments, unused permissions, deprecated APIs, and duplicate resources.
- Add screenshot and UIAutomator regression coverage for donor parity.

# Top 20 Changes Worth Doing

1. Fix Downloads to use real completed download state.
   Impact: Prevents users from mistaking library songs for offline files.
   Difficulty: MEDIUM.
   Risk: MEDIUM.
   Why it is worth doing: Downloads are a core music-app promise; this is the most visible data correctness bug.

2. Repair PiP manifest, receiver, action, service, and ViewModel wiring.
   Impact: Makes a high-profile player feature actually work.
   Difficulty: MEDIUM.
   Risk: MEDIUM.
   Why it is worth doing: The UI already exposes PiP behavior, so broken integration feels like a regression.

3. Make HQ Audio search real or hide it.
   Impact: Removes a misleading source tab.
   Difficulty: LOW if hidden, HIGH if backend is ported.
   Risk: LOW to HIGH.
   Why it is worth doing: Honest source selection matters more than a decorative tab.

4. Populate Related songs from a real recommendation adapter.
   Impact: Turns a dead player affordance into useful discovery.
   Difficulty: MEDIUM.
   Risk: MEDIUM.
   Why it is worth doing: It directly improves everyday listening.

5. Replace fake output-device picker with real route/system output behavior.
   Impact: Users can switch playback devices from the player.
   Difficulty: MEDIUM.
   Risk: MEDIUM.
   Why it is worth doing: A fake picker is worse than no picker.

6. Encrypt and mask AI and Discord tokens.
   Impact: Improves credential privacy.
   Difficulty: LOW.
   Risk: LOW.
   Why it is worth doing: The app already has encryption machinery; this is a focused hygiene win.

7. Wire the volume slider setting into hardware-key interception and player overlays.
   Impact: Restores user control over volume behavior.
   Difficulty: LOW.
   Risk: LOW.
   Why it is worth doing: It fixes both UX and lint-adjacent interaction behavior.

8. Add deep-link, audio intent, and `onNewIntent` support.
   Impact: Android open-with/share flows start working.
   Difficulty: MEDIUM.
   Risk: MEDIUM.
   Why it is worth doing: The parser already exists; the platform contract is missing.

9. Align MediaLibrarySession with manifest/service/browser capabilities.
   Impact: Improves Android Auto/media-client interoperability or removes an incorrect contract.
   Difficulty: MEDIUM to HIGH.
   Risk: MEDIUM.
   Why it is worth doing: Media apps live or die by system integration.

10. Replace duplicate 500 ms progress polling with a shared progress flow.
    Impact: Reduces recompositions and jank risk.
    Difficulty: MEDIUM.
    Risk: MEDIUM.
    Why it is worth doing: It touches the hottest UI path during playback.

11. Wire mini-player and nav alpha/blur/artwork customization.
    Impact: Makes existing settings visibly work.
    Difficulty: LOW.
    Risk: LOW.
    Why it is worth doing: It restores donor customization parity with small code changes.

12. Apply force-refresh-rate and keep-screen-on settings.
    Impact: Fixes two visible inert toggles.
    Difficulty: LOW.
    Risk: LOW.
    Why it is worth doing: Inert settings damage trust quickly.

13. Fix Home guest/signed-in state.
    Impact: Prevents misleading personalized UI for guests.
    Difficulty: LOW.
    Risk: LOW.
    Why it is worth doing: It improves first-run and privacy perception.

14. Fix Home pagination exception handling.
    Impact: Prevents stuck loading-more state.
    Difficulty: LOW.
    Risk: LOW.
    Why it is worth doing: It is a small reliability fix around network failure.

15. Implement or remove `probeStream`.
    Impact: Prevents false-positive stream validation.
    Difficulty: LOW to MEDIUM.
    Risk: LOW.
    Why it is worth doing: Stubbed network truth causes confusing playback failures.

16. Make prerelease updater channel actually fetch prereleases.
    Impact: Supports the repo's current prerelease distribution model.
    Difficulty: LOW.
    Risk: MEDIUM.
    Why it is worth doing: The app already exposes channel logic; the API call undermines it.

17. Add route-aware mini-player visibility and shared bottom inset policy.
    Impact: Reduces UI overlap and donor mismatch.
    Difficulty: LOW.
    Risk: LOW.
    Why it is worth doing: It makes the app feel more intentional immediately.

18. Add manifest/action contract tests.
    Impact: Prevents regressions in PiP, media service, deep links, queries, and exported components.
    Difficulty: LOW.
    Risk: LOW.
    Why it is worth doing: These bugs are easy to reintroduce and hard to catch manually.

19. Burn down Compose performance lint warnings.
    Impact: Smoother player/search/home surfaces and easier future upgrades.
    Difficulty: MEDIUM.
    Risk: LOW.
    Why it is worth doing: The warnings point at real recomposition hotspots.

20. Add screenshot/UIAutomator donor-parity smoke tests.
    Impact: Makes future UI port work safer.
    Difficulty: HIGH.
    Risk: LOW.
    Why it is worth doing: A 1:1 UI port needs visual regression evidence, not only compile success.
