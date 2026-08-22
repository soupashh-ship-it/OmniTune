# Goals 4–5 — Requirement Coverage Matrix

This matrix is a completion-audit aid, not a claim that runtime testing has passed. It separates deterministic source coverage from Android/device execution. The user has asked to leave USB testing disabled, so every device-dependent row remains deferred.

## Goal 4: disposable runtime environment

| Requirement | Current artifact | Evidence state | Remaining proof |
| --- | --- | --- | --- |
| Debug-only profile and package isolation | `OmniTuneRuntime.ps1` hard-codes `com.omnitune.app.debug`; reset requires `-ConfirmResetDebugProfile` | Source-reviewed and PowerShell-parser verified | Execute on an emulator or disposable device |
| Device/emulator metadata | `Status` writes model, Android version, SDK, package version, commit, network summary, and non-secret provider/account descriptor | Implemented; no device status captured in this goal | Capture one status JSON per runtime run |
| Build/install/test APK | `Build`, `BuildTestApk`, `Install`, `InstallTestApk` actions | Build actions previously passed; install deferred | Install using a disposable target |
| Launch, logcat, screenshot, backup pull | `Launch`, `StartLogcat`, `StopLogcat`, `Screenshot`, `PullBackup` | Implemented; parser verified | Produce evidence artifacts during smoke run |
| Process death/network controls | `ProcessDeath`, guarded `SetNetwork` | Implemented; no network mutation issued | Record actual before/after connectivity evidence |
| Smoke fixture dataset | `RuntimeSmokeDatasetTest` seeds tracks, albums, artists, likes, playlist/folder, history, queue and settings | Compiled; not run | Seed only after resetting a disposable debug profile |
| Runtime evidence record | `OMNITUNE_RUNTIME_EVIDENCE_TEMPLATE.md` | Implemented | Fill RT-01 through RT-15 with artifacts and reproduction rates |

## Goal 5: deterministic wiring coverage

| Area | Current coverage | Evidence state | Explicit remaining gap |
| --- | --- | --- | --- |
| Search query/filter/cancellation/selection | `SearchRequestGateTest`; `SearchViewModelInstrumentedTest`; `SongPlaybackRequestTest` | Request-gate and selected-song JVM tests passed; Android fixture compiles | Exercise the selected result through `MusicService` on an authorized disposable target |
| Search failures/offline/pagination/cache | Deterministic provider covers duplicate results, offline short-circuit, provider failure, cached refresh fallback and continuation dedupe | Android fixture compiles | Device network transition and UI pagination evidence |
| Stream resolution | `StreamUrlResolverInstrumentedTest` uses lookup seam with real MediaItems, including deterministic timeout/no-cache-poisoning coverage | Android fixture compiles | Actual extractor/provider failure and 10-second timeout on a disposable target |
| Playback controls and session | `MusicSessionCallbackInstrumentedTest` uses real Media3 session/controller and verifies callback listener teardown; notification contract JVM test passed | Session fixture compiles; contract JVM test passed | Full `MusicService` lifecycle, notification posting/dismissal, Android 13 permission, real seek audio |
| Queue persistence | `QueuePersistenceManagerTest` verifies order/index/position save and restore | JVM test passed earlier | Service process recreation and resolved-stream restoration |
| Preferences consumed | `PlaybackPreferenceObserverInstrumentedTest` uses real ExoPlayer + isolated DataStore | Android fixture compiles | Run fixture and UI setting persistence smoke |
| Download/offline cache | `OfflinePlaybackCacheRoutingTest`; real-cache `OfflinePlaybackCacheRoutingInstrumentedTest`; `DownloadLifecyclePolicyTest` | Routing JVM test passed earlier; lifecycle policy test passed; Android fixture compiles, including deletion-to-offline-unavailable routing | DownloadManager progress and retry execution plus airplane-mode playback/restart/delete on a disposable target |
| Library/playlists | `LibraryPlaylistPersistenceInstrumentedTest` and existing DB tests | Android fixture compiles | Cross-screen like consistency, relaunch persistence and large-list UI |
| Backup/restore | Archive/preflight/safety/transaction JVM tests; `OmniBackupRepositoryInstrumentedTest` | JVM tests previously passed; Android fixture compiles | Disposable Merge/Replace, backup retrieval, rollback and excluded-secret runtime record |
| Notification/media session | Notification contract plus real Media3 session/controller fixture | JVM contract passed; Android fixture compiles | System notification metadata, dismissal, permission behavior and service restart |
| UPI launch | `AboutMetadataTest` covers UPI URI validation/encoding, amount rules, and no-handler vs launch-initiated classification | JVM tests passed in the complete local suite | Confirm the Android resolver/payment-app handoff on a disposable device without completing a payment |

## Completion-gate traceability

| Gate | Regression artifact | Current evidence |
| --- | --- | --- |
| Playback cache points at wrong cache | `OfflinePlaybackCacheRoutingTest`, `OfflinePlaybackCacheRoutingInstrumentedTest` | Source/JVM and compiled real-cache fixture; runtime deferred |
| Search publishes stale query | `SearchRequestGateTest`, `SearchViewModelInstrumentedTest` | JVM gate passed; fixture compiled |
| Replace Restore clears without safety backup | `RestoreSafetyBackupStoreTest`, `RestoreTransactionBoundaryTest`, backup repository fixture | JVM tests previously passed; runtime deferred |
| Visible persisted setting is unused | `PlaybackPreferenceObserverInstrumentedTest` | Compiled isolated DataStore + real-player fixture; execution deferred |
| Queue restoration loses ordering | `QueuePersistenceManagerTest` | JVM test passed earlier |
| Download deletion leaves offline availability | `OfflinePlaybackCacheRoutingInstrumentedTest` | Compiled real-cache fixture covers both the index/cache-removal route and the cache/index race; execution deferred |

## Authorized next runtime action

When non-USB execution is authorized, prefer an emulator and follow `docs/runtime/OMNITUNE_DISPOSABLE_RUNTIME.md` in order: Build → Install → Reset debug profile → Install test APK → Seed dataset → Status/logcat → selected fixtures → RT-01 through RT-15. Do not mark any deferred row as passed until the evidence template contains its actual result and artifact paths.
