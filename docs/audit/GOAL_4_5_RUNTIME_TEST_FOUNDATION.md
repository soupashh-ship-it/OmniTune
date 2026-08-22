# Goals 4–5 — Runtime environment and integration-test foundation

## Current status

Source and build foundation implemented. The connected USB device was not touched, in line with the user's request to leave device testing disabled. Nothing in this report claims a runtime smoke case passed.

`GOAL_4_5_COVERAGE_MATRIX.md` maps every required runtime/test area and completion gate to its current evidence and remaining runtime proof.

## Disposable runtime environment

- `scripts/qa/OmniTuneRuntime.ps1` builds, installs, launches, force-stops, captures logcat/screenshots, pulls an explicitly named backup, records device/version/network metadata, can request controlled Wi-Fi/mobile-data states, and runs only allow-listed isolated instrumentation fixtures with persisted console evidence.
- The script hard-codes `com.omnitune.app.debug`; it refuses to clear data until `-ConfirmResetDebugProfile` is supplied and has no release-package path.
- `RuntimeSmokeDatasetTest` seeds only the debug target profile with deterministic tracks, artists, albums, likes, a local playlist, tag/folder, history/statistics, queue, failed download state, and non-secret playback settings. Completed/partial download checks are intentionally created through the production download flow during RT-07.
- `docs/runtime/OMNITUNE_DISPOSABLE_RUNTIME.md` is the reproducible runbook. `OMNITUNE_RUNTIME_EVIDENCE_TEMPLATE.md` records the full smoke matrix, exact expected/actual results, reproduction rate, evidence locations, and issue IDs.

## Integration-test improvements

- Search now has a request-generation publication boundary in addition to coroutine cancellation. Late provider responses cannot overwrite a newer query/filter state, and a provider `Result.failure` during refresh retains known-good cached results instead of discarding them.
- Search provider, network status, and timing have production Hilt bindings plus deterministic fake seams.
- `SearchViewModelInstrumentedTest` exercises real ViewModel + Room wiring with a fake provider and no live network: filter dedupe, empty query, offline short-circuiting without a provider call, provider-failure error state, cached-result retention after a failed refresh, and continuation-page dedupe. `SearchRequestGateTest` covers rapid-query, filter, and clear-query stale-response suppression.
- Search-result selection now creates a validated `SongPlaybackRequest` before it touches the player. `SongPlaybackRequestTest` verifies the selected item retains `SEARCH_RESULTS` provenance and invalid/stale row indexes are rejected rather than causing a playback-boundary crash.
- `QueuePersistenceManagerTest` now verifies the saved media-ID ordering, selected index, and playback position, plus restored queue ordering and invalid-index clamping with a controlled dispatcher.
- `LibraryPlaylistPersistenceInstrumentedTest` uses real in-memory Room to verify local playlist create/rename/reorder/remove/delete, duplicate prevention, liked-song survival, tag/folder-link cleanup on deletion, and complete rollback of an intentionally failed song/playlist/membership transaction.
- `PlaybackPreferenceObserverInstrumentedTest` uses a temporary DataStore and a real Media3 player to prove persisted Skip silence and Crossfade settings are consumed; its isolated file prevents fixture execution from modifying the normal debug profile.
- Download admission, library availability mapping, and resolved-stream retry limits now share `DownloadLifecyclePolicy`, which is called by the production `DownloadUtil` and `ExoDownloadService`. The policy rejects downloads below 64 MiB free storage before stream resolution and preserves the existing Wi-Fi-only behavior; `DownloadLifecyclePolicyTest` covers those guards, completed-byte requirements, failed/in-progress/removal states, and bounded retry eligibility.
- `StreamUrlResolverInstrumentedTest` now also exercises the resolver timeout branch using a zero-duration internal fixture seam. It verifies a timed-out lookup yields no broken MediaItem and does not cache a failure that would block a later successful resolution; production remains fixed at a ten-second lookup timeout.
- `MusicSessionCallback` now owns and removes its player listener during service teardown, resets its exposed playback state, and initializes its state from a replacement player. The real-ExoPlayer session fixture covers that teardown boundary so an old player cannot publish stale current-track state after a service recreation.
- The UPI support path has existing deterministic `AboutMetadataTest` coverage for URI formation, input validation, and the distinction between no enabled handler and Android accepting a launch. A device smoke remains necessary only to validate the actual payment-app resolver handoff; it must not attempt or assert payment completion.
- `OfflinePlaybackCacheRoutingInstrumentedTest` now distinguishes a deletion from a transient index/cache race. It proves that once the completed-download index record is removed, routing leaves the download cache and cannot remain playable while its stream source is offline; the separate race test still guarantees that a stale completed index never silently turns into a network request.
- `MusicSessionCallbackInstrumentedTest` uses a real Media3 session and controller to verify play/pause/next/previous transport changes plus Like, Shuffle, and Repeat command routing without starting `MusicService` or posting a notification. Event-driven waits avoid arbitrary sleeps.
- `StreamUrlResolverInstrumentedTest` injects a deterministic stream lookup into the real resolver and verifies bare-ID stream replacement, MIME/custom-cache-key preservation, cache reuse and invalidation refresh, unavailable streams, and HTTP-item rejection.
- `PlaybackNotificationContractTest` is a JVM regression test for fallback metadata and Previous/Play-or-Pause/Next/Like/Repeat action order; `PlaybackNotificationManager` consumes the same contract when it creates Android notification actions.
- `OfflinePlaybackCacheRoutingInstrumentedTest` now uses the real routing factory and Media3 cache: a byte-complete download is read from persistent storage, then cache deletion must fail offline rather than silently creating a stream-source request.
- Queue startup now follows one safe path for ordinary and preload queues: it inserts raw queue IDs while paused, resolves the selected item, then replaces it before the player is prepared. The former preload branch prepared a raw YouTube ID and never replaced that current item with the resolved stream, matching the reported immediate pause.
- Existing Goal 2 and Goal 3 test fixtures remain responsible for Replace safety and offline cache routing/deletion boundaries.

## Build evidence

- `./scripts/qa/OmniTuneRuntime.ps1 -Action Build` — passed without requiring ADB.
- `./scripts/qa/OmniTuneRuntime.ps1 -Action BuildTestApk` — passed; no instrumentation was executed.
- `./gradlew.bat :app:testDebugUnitTest --tests "com.omnitune.app.ui.screens.SearchRequestGateTest"` — passed.
- `./gradlew.bat :app:testDebugUnitTest --tests "com.omnitune.app.playback.QueuePersistenceManagerTest"` — passed.
- `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest` — passed, with 131 test cases and zero failures in the current XML results; the current debug and instrumentation APKs were assembled without connecting to a device.
- `./gradlew.bat :app:compileDebugAndroidTestKotlin` — passed after adding the runtime dataset and search integration fixtures.
- `./gradlew.bat :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` — passed after the preload-queue playback correction.
- `./gradlew.bat :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` — passed after adding the isolated playback-preference wiring fixture.
- `./gradlew.bat :app:compileDebugAndroidTestKotlin` — passed after expanding the deterministic Search ViewModel fixture.
- `./gradlew.bat :app:compileDebugAndroidTestKotlin` — passed after adding the real Media3 session/controller fixture.
- `./gradlew.bat :app:compileDebugAndroidTestKotlin` — passed after strengthening the offline no-fallback regression fixture.
- `./gradlew.bat :app:compileDebugAndroidTestKotlin` — passed after adding the Room transaction-rollback fixture.
- `./gradlew.bat :app:compileDebugAndroidTestKotlin` — passed after adding the deterministic provider-failure fixture.
- `./gradlew.bat :app:testDebugUnitTest --tests "com.omnitune.app.playback.PlaybackNotificationContractTest"` — passed after extracting the notification action/metadata contract.
- `./gradlew.bat :app:testDebugUnitTest --tests "com.omnitune.app.ui.navigation.SongPlaybackRequestTest" :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` — passed after validating the search-result-to-playback handoff.
- `./gradlew.bat :app:testDebugUnitTest --tests "com.omnitune.app.playback.DownloadLifecyclePolicyTest" :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` — Gradle completed successfully; the generated XML records 3 tests and zero failures after adding low-storage, download-state, and retry policy coverage.
- `./gradlew.bat :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` — passed after adding deterministic stream-resolution timeout coverage.
- `./gradlew.bat :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` — passed after adding Media3 callback teardown coverage.
- `./gradlew.bat :app:compileDebugAndroidTestKotlin` — passed after expanding the Media3 transport-command fixture.
- `./gradlew.bat :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` — passed after the cached-search refresh fallback correction.
- `./gradlew.bat :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin` — passed after adding the deterministic stream-resolution fixture.
- `./gradlew.bat :app:lintDebug` — current report has 0 errors and 1 existing `QueryPermissionsNeeded` warning in `AboutMetadata.kt`.

## Outstanding work

The smoke matrix and broader end-to-end coverage are still active work. In particular, device execution, notification/media-session verification, full playback/service process recreation, real DownloadManager restart/delete, UPI resolver behaviour, and all disposable Backup Merge/Replace flows must be recorded using the runtime evidence template before the combined goals can be considered complete.
