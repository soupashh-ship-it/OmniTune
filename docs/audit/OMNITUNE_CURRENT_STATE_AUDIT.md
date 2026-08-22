# OmniTune current-state audit

> Historical baseline note (2026-07-28): findings in this document describe the pre-Goals-6–10
> checkout unless explicitly marked otherwise. The implemented resolutions and current contracts
> are recorded in `GOAL_6_BACKUP_CONTRACT.md`, `GOAL_7_CREDENTIAL_INVENTORY.md`, and
> `SETTINGS_BEHAVIOR_REGISTRY.md`; do not treat superseded historical findings as current state.

**Audit date:** 2026-07-28
**Repository state audited:** `main` at `360387a3f9b138b523cf40326a303e76a90b3745`
**Scope:** forensic audit and planning only. No application source, UI, generated file, user data, or Git history was changed.

## Executive summary

OmniTune has a substantial implementation: a Compose UI, Media3 playback service, Room database with migrations, provider-backed search/home flows, downloads, backup/restore, integrations, and release automation. `assembleDebug`, both lint variants, Android-test compilation, `assembleRelease`, and the separate `innertube` test all completed successfully. That is useful build evidence, but not proof that user journeys work.

The project is **not ready to call a stable daily-use release** today. Two proven defects block that conclusion:

1. `testDebugUnitTest`, which is required by both CI workflows, fails 1/90 tests. The failing UPI test calls Android's stub `Uri.Builder` from a plain JVM unit test and also expects an obsolete URI shape. A tag release stops before it produces a signed APK.
2. Completed downloads are stored in `DownloadUtil.downloadCache`, while both playback players use `DownloadUtil.playbackCache`. The offline resolver preserves the completed download's original stream URI and custom cache key, but the player never reads the download cache. The advertised completed-download/offline path therefore cannot supply the cached bytes to Media3.

The strongest areas are static build hygiene (both lint reports contain zero issues), explicit Room migrations 1 through 7 with Android migration tests, and a reasonably complete provider/search/playback architecture. The weakest areas are release verification, offline playback, backup completeness/safety, dead feature surfaces, preference drift, and missing device/UI integration coverage.

**User-data assessment:** ordinary Room upgrade paths have explicit migrations and seed-data migration tests, so there is positive evidence for standard schema upgrades. It is still **not safe to claim full data safety**: Replace restore clears the library without an automatic safety backup, backup does not export settings or queue state despite UI wording, and the recovery/restore/download archive paths have not run on a device.

**Runtime limitation:** `adb devices -l` returned no attached devices on 2026-07-28. No install, data clear, account action, network toggle, playback, download, UPI, or backup/restore action was performed. All runtime-dependent paths are explicitly `IMPLEMENTED BUT UNVERIFIED` unless a defect is proven statically.

## Baseline

| Area | Result | Evidence | Blocking |
| --- | --- | --- | --- |
| Working tree | VERIFIED WORKING | `main`, commit `360387a`; `git status --short`, `git diff --check` were empty before audit docs. | No |
| Debug build | VERIFIED WORKING | `./gradlew.bat clean`; `./gradlew.bat :app:assembleDebug --no-daemon --max-workers=1 --console=plain` completed. | No |
| App unit tests | BROKEN | `./gradlew.bat test --no-daemon --max-workers=1 --console=plain`: 90 tests, 1 failure. | **Yes — CI** |
| Separate Innertube test | VERIFIED WORKING | `./gradlew.bat :innertube:test ...`: 1 test, 0 failures. | No |
| Android tests | IMPLEMENTED BUT UNVERIFIED | `:app:compileDebugAndroidTestKotlin` completed; `MusicDatabaseMigrationTest` and `MusicDatabaseTest` were not executed because no device/emulator is attached. | Release confidence |
| Lint | VERIFIED WORKING | `:app:lintDebug :app:lintRelease` completed; both XML reports have zero issues. | No |
| Release assembly | IMPLEMENTED BUT UNVERIFIED | `:app:assembleRelease` completed with R8/resource shrinking, producing `app-release-unsigned.apk`. | Signing/runtime not verified |
| Local signing | IMPLEMENTED BUT UNVERIFIED | All four `OMNITUNE_KEYSTORE_*` environment variables were absent; `apksigner verify` correctly rejected the local `app-release-unsigned.apk`. | CI must verify |
| CI | BROKEN | `.github/workflows/build.yml` and `release.yml` both run `testDebugUnitTest`; current test failure stops them. | **Yes** |
| Device runtime | IMPLEMENTED BUT UNVERIFIED | No ADB device listed; no destructive or user-data-changing actions taken. | Yes for release qualification |

Relevant configuration: Android Gradle Plugin 9.2.1, Kotlin 2.3.10, JDK 21, compile/target SDK 36, min SDK 26, app ID `com.omnitune.app`, version `1.1.0` / code `110`, debug suffix `.debug`. Release minification and shrinking are enabled. The CI tag workflow requires a decoded keystore and uses `apksigner verify` before upload.

## Goal 0 baseline revalidation — 2026-07-28

This revalidation used the same audited commit (`360387a`) with no subsequent commits, no source/configuration changes, and no changed CI workflows. The only worktree content before the run was this intentional, untracked `docs/audit/` package.

| Required command | Fresh result | Evidence status | Notes |
| --- | --- | --- | --- |
| `./gradlew.bat clean --no-daemon --max-workers=1 --console=plain` | Passed | VERIFIED WORKING | All ten module clean tasks completed. |
| `./gradlew.bat :app:assembleDebug --no-daemon --max-workers=1 --console=plain` | Passed | VERIFIED WORKING | Debug APK assembled; native-symbol stripping warning was non-fatal. |
| `./gradlew.bat :app:testDebugUnitTest --no-daemon --max-workers=1 --console=plain` | Failed | BROKEN | 90 tests, 1 failure: `AboutMetadataTest.upiPaymentUriEncodesPayeeAndNote` NPE at line 71. |
| `./gradlew.bat :innertube:test --no-daemon --max-workers=1 --console=plain` | Passed | VERIFIED WORKING | Module test task completed. |
| `./gradlew.bat :app:lintDebug :app:lintRelease --no-daemon --max-workers=1 --console=plain` | Passed | VERIFIED WORKING | Both variants completed. |
| `./gradlew.bat :app:compileDebugAndroidTestKotlin --no-daemon --max-workers=1 --console=plain` | Passed | IMPLEMENTED BUT UNVERIFIED | Android test code compiles; no device was available to execute it. |
| `./gradlew.bat :app:assembleRelease --no-daemon --max-workers=1 --console=plain` | Passed | IMPLEMENTED BUT UNVERIFIED | Local artifact remains unsigned without CI secrets. |

| Audited finding | Goal 0 evidence status | Fresh classification/evidence |
| --- | --- | --- |
| Failing UPI test | BROKEN | Still present: JVM unit test calls `Uri.Builder`; production always appends `tr`, while expected string omits it. |
| Download cache vs. playback cache | BROKEN | Still present: downloads use `downloadCache`; `PlayerFactory` selects only `playbackCache`. |
| Unsafe Replace restore | PARTIALLY IMPLEMENTED | Still present: `validate` checks app/format only, then Replace clears library without a safety export. |
| Inaccurate backup wording | PARTIALLY IMPLEMENTED | Still present: UI says preferences; snapshot stores a metadata-only settings section and no queue. |
| Backup schema metadata mismatch | PARTIALLY IMPLEMENTED | Still present: backup writes schema version 5 while Room is version 7; import validation ignores the field. |
| Privileged BuildConfig secrets | PARTIALLY IMPLEMENTED | Still present: Last.fm secret and Together bearer are still emitted as `BuildConfig` fields. |
| Search cancellation risk | PARTIALLY IMPLEMENTED | Still present: cancellation is followed by an `Exception` catch without cancellation rethrow/generation validation. |
| Together, Discord, and For You systems | PARTIALLY IMPLEMENTED | Still present: no Together route; For You has no consumer; service clears/disables Discord at startup. |
| Orphaned preference keys | PARTIALLY IMPLEMENTED | Still present: 230 keys declared, 87 have no Kotlin source reader outside declarations. |
| Notification scope mismatch | PARTIALLY IMPLEMENTED | Still present: root promises alerts/in-app messages; route renders only system media-controls help. |

No production behavior changed during Goal 0. Goal 1 is the next programme goal and remains intentionally unstarted in this baseline section.

## Goal 1 UPI payment contract — 2026-07-28

Goal 1 resolves the P0 UPI/JVM-test CI blocker. `buildUpiPaymentUri` is now a pure Kotlin serializer with an injectable transaction reference, locale-independent `BigDecimal` amount handling, complete percent encoding, and deliberate invalid/empty-amount behaviour. Production references use a UUID-derived `OMNI…` value; tests inject fixed references instead of depending on clock or randomness.

The About support flow now accepts valid dot-decimal INR amounts, performs an enabled-activity preflight before dispatching `ACTION_VIEW`, handles invalid/no-handler failures, and offers a clipboard-based manual-pay fallback. A launch result means only that Android accepted the activity launch: the UI does not claim that a payment was completed, and a user cancelling the external app simply returns to OmniTune.

| Goal 1 validation | Result | Evidence |
| --- | --- | --- |
| Focused UPI JVM contract test | Passed | `:app:testDebugUnitTest --tests com.omnitune.app.ui.screens.settings.AboutMetadataTest`; covers deterministic URI, Unicode/reserved characters, decimal formatting, invalid/empty amounts, invalid destination/reference, and missing-handler classification. |
| CI-equivalent application gate | Passed | `:app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:compileDebugAndroidTestKotlin :app:assembleDebug :app:assembleRelease --no-daemon --max-workers=1 --console=plain`; final run completed successfully in 8m 05s. |
| Debug install/update | Passed | `adb -s 138898743000055 install -r app/build/outputs/apk/debug/app-debug.apk` succeeded without clearing data. |
| About/support UI | Passed | Amount presets, selected ₹100, truthful completion disclosure, payment action, and manual copy action displayed on the connected I2202 device. Screenshot: `docs/audit/device-qa/goal1-upi-fallback.png`. |
| External intent launch | Passed, limited | Android launched the UPI resolver and listed Navi, WhatsApp, Amazon Pay UPI, GPay, BHIM, and FamApp for the generated URI. GPay required the device encrypted-app fingerprint gate; BHIM refused to show payment details while USB debugging was connected. No payment was approved. |
| No-handler/copy fallback device test | Not run | The user asked to stop USB/device testing. The no-handler branch remains directly covered by JVM classification tests; manual clipboard action was displayed but clipboard contents cannot be inspected safely through this device session. |

The final source-level runtime result is intentionally narrow: handler launch is verified, but correct payee/amount presentation inside a UPI app and the physical no-handler fallback require a later non-USB, disposable-device session. This supersedes the Goal 0 UPI-test portion of AUD-001; unrelated audit findings remain open.

## Feature status matrix

| Feature | Status | UI | Backend | Persistence | Tests | Runtime verified | Evidence | Main gap |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| App launch/navigation | IMPLEMENTED BUT UNVERIFIED | Compose routes are registered. | `MainActivity`, `MusicService`, Hilt. | DataStore/Room injected. | Build only. | No device. | `MainActivity.kt`, `OmniNavGraph.kt`. | Fresh/existing user launch not exercised. |
| Home feed | PARTIALLY IMPLEMENTED | Shelves, chips, retry/error state exist. | `HomeFeedRepository` calls provider home/explore/moods. | Local history/likes/downloads inform engine. | Engine unit coverage only. | No. | `HomeDiscoveryViewModel.kt`, `HomeFeedRepository.kt`. | Static catalog and generic fallback queries remain in the live path. |
| Search | IMPLEMENTED BUT UNVERIFIED | Query, tabs, history, retry, pagination UI. | Debounced provider requests, generation gate, provider fallback summary. | Room search history. | Request-gate unit test and compiled fake-provider ViewModel fixture. | No. | `SearchViewModel.kt`, `SearchViewModelInstrumentedTest.kt`. | Manual device/network/pagination evidence remains deferred. |
| Online playback | IMPLEMENTED BUT UNVERIFIED | Queue/player controls call `PlayerConnection`. | `MusicService` resolves streams and prepares Media3. Preload queues now wait for the resolved source instead of preparing a raw YouTube ID. | Queue/history/format persisted. | Policy/resolver tests, compile-only verification of the corrected service path; no service integration run. | No. | `MusicService.kt`, `StreamExtractor.kt`. | Play/pause/seek/recovery still require disposable-device evidence. |
| Queue restore | IMPLEMENTED BUT UNVERIFIED | Queue screen exists. | Service restores a saved `QueueEntity`. | Room queue table. | Queue unit tests verify ordering/index/position save and restore; Android process fixture compiles. | No. | `MusicService.kt:319-352`, `QueuePersistenceManagerTest.kt`. | Full service process recreation and stream re-resolution remain unverified. |
| Downloads | PARTIALLY IMPLEMENTED | Queue/progress/retry/remove UI. | Media3 `DownloadManager` and `ExoDownloadService`. | Media3 DB/cache plus Room state. | No end-to-end test. | No. | `DownloadUtil.kt`, `ExoDownloadService.kt`. | Download content, retry, storage and resume path unverified. |
| Offline playback | IMPLEMENTED BUT UNVERIFIED | Completed downloads can be selected. | Key-aware router selects completed download cache without upstream fallback. | Persistent download cache plus bounded stream cache. | Unit + compiled cache persistence/deletion fixture. | No. | `OfflinePlaybackCacheRouting.kt`, `DownloadUtil.kt`, `PlayerFactory.kt`. | Airplane-mode playback/restart/delete evidence remains deferred. |
| Lyrics | IMPLEMENTED BUT UNVERIFIED | Now-playing lyrics UI and retry state. | Multi-provider repository and quality filtering. | Room lyrics entity. | Repository/quality/inline/ViewModel tests. | No. | `LyricsRepositoryImpl.kt`, `LyricsQuality.kt`, `LyricsViewModelTest.kt`. | Real provider, sync, language rejection and scroll behavior unverified. |
| Liked songs | IMPLEMENTED BUT UNVERIFIED | Like controls appear in menus/player/library. | `PlayerConnection.toggleLike`, library sync. | Song `liked` data in Room. | No user-journey test. | No. | `PlayerConnection.kt:190-220`. | Cross-screen consistency and sync failures unverified. |
| Local playlists | IMPLEMENTED BUT UNVERIFIED | Create/edit/detail screens/routes exist. | DAO/playlist planner/menu paths. | Playlist maps and foreign keys. | Planner test only. | No. | `PlaylistEntity.kt`, `PlaylistSongMap.kt`, `PlaylistPlaybackPlannerTest.kt`. | CRUD, reorder, bulk operations, restore and large lists unverified. |
| YouTube Music playlist sync | IMPLEMENTED BUT UNVERIFIED | Account screen exposes select/sync/status. | Worker/manual sync use provider and transactions. | Preferences, Room playlist maps. | No worker/account test. | No. | `OmniTuneAccountSettingsScreen.kt`, `YouTubePlaylistSync.kt`. | Toggle can be enabled while signed out; credentials/network sync unverified. |
| History/statistics/year review | IMPLEMENTED BUT UNVERIFIED | History/stats/year routes exist. | Playback event recorder and DB queries. | Event/play-count/skip tables. | Recorder/year-screen tests. | No. | `PlaybackEventRecorder.kt`, `StatsViewModel.kt`. | Real listening thresholds, clear/restore and accuracy unverified. |
| Recommendations | PARTIALLY IMPLEMENTED | Home "Keep listening" and related cards. | Local scoring plus provider searches. | Events/likes/downloads/skips feed scoring. | Engine/mood tests. | No. | `HomeRecommendationEngine.kt`, `HomeDiscoveryViewModel.kt:532-608`. | Cold start uses six fixed popular-artist queries; no feedback loop validation. |
| Explore/moods/genres | PARTIALLY IMPLEMENTED | Browse/chips/routes present. | Provider explore/mood calls. | No durable personalization state. | `MoodGenreResolverTest`. | No. | `HomeFeedRepository.kt`. | Static reference chips precede provider chips; provider failure/region behavior unverified. |
| Playback notification/media session | IMPLEMENTED BUT UNVERIFIED | System-facing; settings links to Android pages. | `MusicService` is a `MediaSessionService`. | Service state/queue. | No notification test. | No. | manifest; `MusicService.kt`. | Notification permission, lock screen, Bluetooth, Android Auto and Vivo behavior unverified. |
| Notification settings/product alerts | PARTIALLY IMPLEMENTED | "Notifications" route is visible. | Opens OS notification/battery/app details. | OS-owned. | No. | `NotificationSettings.kt`, `OmniNavGraph.kt:704`. | No app update/new-music/recommendation preferences or delivery system despite root wording. |
| Backup/restore | PARTIALLY IMPLEMENTED | Export/import and Replace/Merge UX. | JSON/ZIP parser, staged offline archive. | Room transaction; Media3 files staged. | Model tests only. | No. | `OmniBackupRepository.kt`. | No checksum/safety backup; no settings or queue backup; no device restore test. |
| Database migrations | IMPLEMENTED BUT UNVERIFIED | N/A | Explicit 1→7 Room migrations and repair path. | Room schema/foreign keys/WAL. | Android migration tests compile only. | No. | `MusicDatabase.kt:119-243`, `MusicDatabaseMigrationTest.kt`. | Migrations and recovery fallback not executed on a device. |
| Account/token encryption | IMPLEMENTED BUT UNVERIFIED | Login/PoToken/scrobble screens. | Android Keystore cipher plus migration on app start. | Encrypted DataStore values. | No encryption integration test. | No. | `SecurePreferenceCipher.kt`, `OmniTuneApp.kt:278-337`. | Invalid/expired token and logout flows unverified. |
| Last.fm/ListenBrainz | IMPLEMENTED BUT UNVERIFIED | Scrobbling settings UI. | Managers and Media3 playback integration. | Encrypted session/token preferences. | No integration test. | No. | `ScrobblingSettings.kt`, `ScrobblingManager.kt`. | Real authentication/scrobble/error recovery unverified. |
| UPI donation | IMPLEMENTED BUT UNVERIFIED | Amount and copy fallback UI. | External `ACTION_VIEW` UPI intent. | No app persistence required. | JVM URI-contract tests pass. | Device resolver launch previously observed; not rerun in this goal. | `AboutSettings.kt`, `AboutMetadata.kt`. | Installed-handler variations remain device-specific. |
| Updates/install | IMPLEMENTED BUT UNVERIFIED | Update settings UI. | GitHub API, verified APK download/install intent. | Version preferences. | API test. | No. | `ApkDownloadManager.kt`, `UpdatesSettings.kt`. | Real signature/update/unknown-sources flow unverified. |
| Together listening party | BACKEND-ONLY | No screen or route. | Local/online server/client classes exist. | Dead preference declarations. | No tests. | No. | `together/*.kt`; no UI/nav caller. | Entire feature is unreachable. |
| For You suggestion engine | BACKEND-ONLY | No screen uses it. | `@Singleton` can query related songs. | Reads DB. | No tests. | No. | `utils/ForYouSuggestionEngine.kt`; only declaration reference. | Dead duplicate recommendation implementation. |
| Discord presence | OBSOLETE OR UNUSED | Only a Discord community link is exposed. | Service injects manager then clears token/disables it. | Legacy preference keys remain. | No tests. | No. | `MusicService.kt:397-404`, `discord/*.kt`. | Retained dead code and settings data. |
| Release pipeline | IMPLEMENTED BUT CI-UNVERIFIED | N/A | CI builds/tests/lints/signs. | GitHub Actions. | `:app:testDebugUnitTest` currently passes 124/124 locally. | N/A | `.github/workflows/build.yml`, `release.yml`. | A remote CI and signed-release run remain required. |

## Detailed findings

### AUD-001 — CI unit-test gate is restored locally

- **Status:** IMPLEMENTED — local unit gate passed; remote CI remains unverified.
- **Severity:** P0 release gate (mitigated locally)
- **Affected feature:** release engineering and UPI metadata validation
- **User impact:** The earlier unit failure no longer blocks the local debug test gate. A remote CI run is still needed before making a release-pipeline claim.
- **Evidence:** `./gradlew.bat :app:testDebugUnitTest` passed on 2026-07-28; current XML reports 124 tests and zero failures. `buildUpiPaymentUri` now serializes the URI as a plain string and tests pass deterministic transaction references.
- **File paths:** `app/src/test/kotlin/com/omnitune/app/ui/screens/settings/AboutMetadataTest.kt:62-72`; `app/src/main/kotlin/com/omnitune/app/ui/screens/settings/AboutMetadata.kt:94-113`; `.github/workflows/build.yml`; `.github/workflows/release.yml`.
- **Relevant symbols:** `buildUpiPaymentUri`, `upiPaymentUriEncodesPayeeAndNote`.
- **Verification required:** local `:app:testDebugUnitTest` passed. CI run and Android intent checks with a UPI handler remain pending.

### AUD-002 — Offline downloads are not connected to playback cache

- **Status:** IMPLEMENTED — source/unit-test verified; device airplane-mode verification intentionally deferred.
- **Severity:** P1 core functionality
- **Affected feature:** downloaded/offline playback
- **Resolution:** `OfflineCacheRoutingDataSourceFactory` selects the persistent `downloadCache` only for a byte-complete `DownloadManager` entry using its exact custom cache key. That source has no upstream and is read-only, so a completed download cannot fall through to the network. All other media remains on the bounded `playbackCache` plus the existing signed-stream upstream. Both `createPlayer` and `createOverlapPlayer` share this factory.
- **Stale-state handling:** completion now requires a known content length and a contiguous cached range from byte zero. A completed index record without those bytes is marked unavailable in Room, its cache resource is removed, and its download entry is removed instead of being offered as offline media.
- **File paths:** `app/src/main/kotlin/com/omnitune/app/playback/OfflinePlaybackCacheRouting.kt`, `DownloadUtil.kt`, `StreamUrlResolver.kt`, `PlayerFactory.kt`, `DownloadsViewModel.kt`.
- **Fixture coverage:** completed/byte-complete routing, partial and unknown-length rejection, in-progress rejection, custom-key preservation, and legacy request-id fallback in `OfflinePlaybackCacheRoutingTest`.
- **Verification required:** static/unit verification is complete. A disposable device/emulator check remains required before this can be labelled device-verified: download → airplane mode → play/seek/next → app restart → delete download. It was not run because the user requested that USB/device testing be left disabled.

### AUD-003 — Replace restore safety and recovery

- **Status:** IMPLEMENTED — source/unit-test verified; disposable-device verification remains pending.
- **Severity:** P0 data-loss risk (mitigated in source)
- **Affected feature:** backup/restore
- **Resolution:** `OmniBackupPreflight` validates counts, identifiers, relationships, ordering, queue references, archive paths, limits, and full-archive manifests before any Replace operation. `OmniBackupRepository` creates and re-reads a retained app-private full safety archive before clearing Room data. Replace stages archive files first, verifies its Room transaction, promotes media through a reversible filesystem transaction, and restores the safety snapshot if media promotion fails.
- **Recovery:** the Backup & Restore screen previews counts/warnings before confirmation and exposes a confirmation-gated recovery action for the latest retained Replace safety archive. Safety archives exclude preferences, account data, cookies, tokens, and API keys.
- **File paths:** `app/src/main/kotlin/com/omnitune/app/backup/OmniBackupPreflight.kt`, `RestoreSafetyBackupStore.kt`, `OmniBackupRepository.kt`, `OfflineDownloadArchive.kt`, `BackupRestoreScreen.kt`.
- **Fixture coverage:** valid Merge/Replace preflight, empty/unsupported/corrupt-manifest/missing-file/missing-relation/duplicate/invalid-order/invalid-queue/large/legacy input, safety-store failure, database-transaction failure boundary, reversible media promotion, interrupted media promotion rollback, and reverse-order rollback primitive.
- **Verification required:** run the full flow on a disposable local profile, including interrupted file staging, Room transaction failure injection, and successful recovery. This was not run on the USB device because the user requested that device testing be left disabled.

### AUD-004 — Backup claim exceeds exported data

- **Status:** PARTIALLY IMPLEMENTED
- **Severity:** P1 data-retention expectation
- **Affected feature:** backup/restore
- **User impact:** Settings and current queue can be lost while the settings root describes Backup & Restore as backing up “your library and preferences.”
- **Evidence:** `SettingsScreen.kt:118` says “Backup your library and preferences.” Snapshot creation writes `settings = BackupSettingsSection()` only (`OmniBackupRepository.kt:123-168`); `BackupSettingsSection` states that secrets and device-specific preferences are not exported (`OmniBackupModels.kt:51-53`). No queue model appears in `OmniBackupSnapshot`.
- **Recommended fix:** Correct the copy immediately; then decide and document which non-secret preferences and queue state are included, version them, and test selective restore.
- **Verification required:** export/import on a disposable profile and assert settings/queue outcomes.

### AUD-005 — Home discovery mixes provider data with static/generic fallback content

- **Status:** PARTIALLY IMPLEMENTED
- **Severity:** P2
- **Affected feature:** home/recommendations
- **User impact:** A polished personalised surface can show generic artist searches or reference catalog cards, especially on cold start/provider failure.
- **Evidence:** Initial state uses `HomeDefaultCatalog.freshDiscovery`, `shelves`, moods and genres (`HomeDiscoveryViewModel.kt:54-63`, `401-406`). On no local signals, `searchSongsForSeeds` uses fixed Taylor Swift, Ed Sheeran, The Weeknd, Dua Lipa and “top english hits 2025” queries (`568-608`). Provider feed loading is real but one thrown provider call fails the whole sequential repository load (`HomeFeedRepository.kt:27-132`; `HomeDiscoveryViewModel.kt:477-492`).
- **Recommended fix:** Label generic fallback as discovery, make provider sections independently fault-tolerant, persist successful feed cache with timestamps, and add cold-start/provider-failure tests.

### AUD-006 — No end-to-end playback/download/search test coverage

- **Status:** REGRESSION RISK
- **Severity:** P1
- **Affected feature:** core daily-use journeys
- **User impact:** Compilation and isolated policy tests can pass while playback, offline cache, notification controls, network recovery, or search selection fail on a device.
- **Evidence:** only two Android test files exist and neither was runnable without a device. App unit tests include 24 files but no `MusicService`+Media3 integration, UI test, offline test, process-death test, or backup restore test. Runtime ADB list was empty.
- **Recommended fix:** Add a disposable test profile and instrumented/UI suite covering the release checklist before expanding features.

### AUD-007 — Preference declarations have drifted from behavior

- **Status:** BACKEND-ONLY
- **Severity:** P2
- **Affected feature:** settings
- **User impact:** Settings/feature values can survive in DataStore while no runtime code reads them, creating misleading future UI or migration behavior.
- **Evidence:** static identifier inventory found 230 preference declarations, of which 87 have no Kotlin reference outside `PreferenceKeys.kt`; the complete list is in `OMNITUNE_UNWIRED_FEATURES.md`.
- **Recommended fix:** Delete/migrate dormant keys, or add a registry test that requires every visible setting to have a reader and observable behavior.

### AUD-008 — Together and legacy Discord code are unreachable/dead

- **Status:** OBSOLETE OR UNUSED
- **Severity:** P2
- **Affected feature:** collaboration/presence maintenance surface
- **User impact:** Dead code increases dependency, security, and maintenance footprint without a user path.
- **Evidence:** `ForYouSuggestionEngine`, `TogetherOnlineApi`, `TogetherOnlineHost`, `TogetherServer`, and `TogetherClient` have only their declarations/references within their own packages; no app UI route refers to Together. `MusicService.kt:397-404` removes the Discord token, forces RPC off, and stops its injected manager on every service creation.
- **Recommended fix:** Either implement and test a product-owned flow, or remove feature modules, keys, dependencies, and dead `restartDiscordPresence` surface in a dedicated cleanup.

### AUD-009 — Search cancellation may publish stale failure/results

- **Status:** IMPLEMENTED — source/unit-test verified; Android integration fixture compiled but not device-run.
- **Severity:** P3
- **Affected feature:** search responsiveness
- **Resolution:** `SearchRequestGate` assigns a generation to each query/filter request and `SearchViewModel` checks it before publishing cached, loading, success, pagination, or failure state. Cancellation is rethrown rather than classified as a provider failure. `SearchProvider`, `SearchNetworkStatus`, and `SearchTiming` are injected seams, with the production Hilt bindings preserving the YouTube and Android implementations.
- **Fixture coverage:** `SearchRequestGateTest` proves stale query, stale filter, and blank-query invalidation behaviour. `SearchViewModelInstrumentedTest` uses an in-memory Room database and a deterministic provider fake to exercise the real ViewModel's filter, deduplication, and empty-query wiring without live network.
- **Verification required:** Run the instrumented fixture and a manual rapid-typing/network-loss smoke case on a disposable profile. This has not been run on the USB device because device testing remains disabled by user instruction.

### AUD-010 — Polling and long-lived work add avoidable background churn

- **Status:** REGRESSION RISK
- **Severity:** P3
- **Affected feature:** downloads/player performance
- **User impact:** View models wake and rebuild state repeatedly even when listener events already exist.
- **Evidence:** `DownloadsViewModel.kt:52-60` loops forever with a 300 ms delay; `PlayerConnection.kt:81-93` emits sleep timer state every 500 ms and remaining time every 1000 ms. `DownloadsViewModel` also owns a `DownloadManager.Listener`.
- **Recommended fix:** expose event/state flows from the owning service and poll only while an actual visible/active progress consumer needs it.

### AUD-011 — Release secrets are compiled into the client

- **Status:** REGRESSION RISK
- **Severity:** P1 security
- **Affected feature:** release security
- **User impact:** Any non-empty Last.fm secret or Together bearer token supplied at build time is recoverable from a distributed APK despite minification.
- **Evidence:** `app/build.gradle.kts:62-78` writes Last.fm API key, Last.fm secret, YouTube key and Together bearer token to `BuildConfig`; `OmniTuneApp.kt:139-147` consumes two of them. The value contents were not printed or inspected.
- **Recommended fix:** never embed bearer/server secrets in a public client; use public/restricted client keys where unavoidable and move privileged calls behind a server.
- **Verification required:** inspect a disposable release APK for generated fields, rotate any shipped privileged secret, and enable provider restrictions.

### AUD-012 — Notification route does not implement the notifications its label suggests

- **Status:** PARTIALLY IMPLEMENTED
- **Severity:** P2
- **Affected feature:** notifications/settings
- **User impact:** The user can manage Android media notification permission/channel/battery settings, but there are no application update/new music/recommendation notification controls or delivery logic.
- **Evidence:** route `settings/notifications` renders only `MediaControlsHelp` (`OmniNavGraph.kt:704`); `NotificationSettings.kt` checks system notification/channel/battery state and launches Android settings. The root label says “Manage alerts and in-app messages” (`SettingsScreen.kt:94`).
- **Recommended fix:** narrow the label to media controls/system notification access, or implement the promised notification categories and persistence.

### AUD-013 — Backup metadata advertises an older schema number and it is not validated

- **Status:** REGRESSION RISK
- **Severity:** P2
- **Affected feature:** backup compatibility
- **User impact:** Backup compatibility decisions cannot safely use the `databaseSchemaVersion` field because new snapshots are stamped `5` while the Room database is version `7`, and import validation ignores that field.
- **Evidence:** `MusicDatabase.kt:44` defines `CURRENT_VERSION = 7`; `OmniBackupRepository.kt:138-168` writes `databaseSchemaVersion = 5`; `validate` checks only `appName` and `formatVersion` (`351-356`).
- **Likely root cause:** Backup model version and Room schema version diverged without a documented compatibility contract.
- **Recommended fix:** clarify whether this is a logical backup-schema version. If it is Room schema metadata, write/validate the current version; otherwise rename it and add an explicit migration/compatibility policy.
- **Verification required:** import old/new backups, then assert all supported fields survive.

## Architecture and persistence observations

- **Single player ownership:** `MusicService` owns the ExoPlayer; `PlayerConnection` forwards UI calls and observes player callbacks. This is a coherent direction, but its runtime behavior remains unverified.
- **Persistence:** Room v7 defines songs, artists, albums, playlists/maps, history, play counts, skips, lyrics, formats, search history, tags and queue. Explicit migrations 1→7 are registered; no destructive migration fallback was found. The Android test seeds core library data across each supported migration, but it was only compiled in this audit.
- **Database recovery:** a schema-integrity failure triggers `SchemaTools.repairDatabaseFile`. This is deliberately non-destructive in intent, but complex repair code and the asynchronous open callback require device/corrupt-file coverage before relying on it for data safety.
- **Backup safety:** ZIP path validation, entry limits, byte limits and a database transaction are positive controls. There is no cryptographic checksum/signature, no pre-replace safety backup, no settings snapshot, and no queue snapshot.
- **Network/provider resilience:** stream extraction has bounded client fallback; playback resolver has a 10-second timeout and memory cache. Search and home remain provider-dependent and lack full integration tests under VPN/rate-limit/network switch conditions.

## Compiler and release notes

Lint is clean, but the builds print non-fatal Kotlin warnings: Android/Media3/Compose deprecations, migration parameter-name warnings, an experimental coroutine opt-in warning, and unneeded safe calls/casts. R8 assembled successfully. The local release APK is intentionally unsigned because no signing environment was present; CI is the signing authority, but is currently blocked by AUD-001.
