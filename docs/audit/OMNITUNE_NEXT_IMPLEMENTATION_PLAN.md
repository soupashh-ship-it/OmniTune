# OmniTune implementation plan after audit

Do this order. Every task should be a small, reviewable change with its own verification evidence; do not combine the cache fix, backup behavior, and UI redesign.

| Order | Task | Priority | Reason | Dependencies | Main files | Verification | Estimated complexity |
| ---: | --- | --- | --- | --- | --- | --- | --- |
| 1 | Repair the failing UPI URI test and serialization contract | P0 | Current CI/release is blocked. | None | `AboutMetadata.kt`, `AboutMetadataTest.kt` | `:app:testDebugUnitTest`; deterministic `tr` assertion; Android intent smoke test. | XS |
| 2 | Make Replace restore recoverable | P0 | Valid but incomplete backup can clear a library without safety rollback. | Disposable backup fixture | `OmniBackupRepository.kt`, restore UI/ViewModel, models | Safety backup creation, malformed/empty archive rejection, Replace rollback on device. | M |
| 3 | Wire downloaded media into player data source correctly | P1 | Offline playback is statically broken. | Understand Media3 cache keys/request URIs | `PlayerFactory.kt`, `DownloadUtil.kt`, `StreamUrlResolver.kt`, `MusicService.kt` | Download → airplane mode → play/seek/next/relaunch/delete on API 26 and 34+. | M |
| 4 | Establish repeatable device smoke environment | P1 | Core journeys cannot be certified without a disposable profile/device. | Debug APK and test account/provider strategy | `docs/audit` checklist, test harness | Fresh/existing-profile launches, logcat capture, no production data modification. | M |
| 5 | Add playback/search/download integration tests | P1 | Current unit policies cannot catch service/cache/UI wiring failures. | 1, 3, 4 | playback/search/download packages; `androidTest` | Rapid search cancellation, stream failure, queue restore, offline cache routing, notification/service state. | L |
| 6 | Correct backup scope disclosure and add actual selected-state backup | P1 | Current UI promises preferences but export excludes them; queue absent. | 2 | `SettingsScreen.kt`, backup models/repository | Export/import matrix for settings, queue, secrets excluded, old backups preserved. | M |
| 6a | Repair backup compatibility metadata contract | P2 | Snapshot reports schema 5 while Room is v7 and import ignores it. | 2 | `OmniBackupRepository.kt`, `OmniBackupModels.kt` | Old/new snapshot import matrix and documented compatibility behavior. | XS |
| 7 | Remove/move privileged BuildConfig secrets | P1 | Client-distributed secrets are extractable. | Secret rotation/operations authority | `app/build.gradle.kts`, provider configuration | APK inspection, key rotation, request restrictions, no privileged token in BuildConfig. | M |
| 8 | Harden Home provider feed and label cold-start fallback | P2 | Home is real but mixed with static/generic content and all-or-nothing load. | Provider error test doubles | `HomeFeedRepository.kt`, `HomeDiscoveryViewModel.kt` | Partial provider failure retains successful sections; clear discovery labels; restart cache behavior. | M |
| 9 | Make Search cancellation stale-safe | P2 | Fast typing/filter changes can publish stale state. | Search test doubles | `SearchViewModel.kt` | Deterministic cancellation/race unit test plus device manual rapid typing test. | S |
| 10 | Audit each visible setting and retire the 87 orphan keys | P2 | Persisted values without readers make features misleading. | Screen-by-screen product decisions | settings screens, `PreferenceKeys.kt` | Registry test: every visible setting changes persisted/observed behavior; migration for removed keys. | L |
| 11 | Decide Together/Discord/ForYou ownership | P2 | Dead code increases maintenance/security footprint. | Product decision | `together`, `discord`, `ForYouSuggestionEngine` | Either full reachable feature with tests or complete removal/migration. | M |
| 12 | Clarify notification scope or implement product notifications | P2 | Current settings label overstates capability. | Product decision | `NotificationSettings.kt`, root settings, worker/notification code | Permission/channel/media notification and any promised category tests. | S |
| 13 | Remove polling/deprecation debt | P3 | Avoidable 300/500/1000 ms loops and compiler warnings erode performance/upgrade safety. | Core tests first | `DownloadsViewModel.kt`, `PlayerConnection.kt`, affected Compose/Hilt files | Profiler/recomposition check; no lost progress updates. | M |
| 14 | Device resilience matrix | P3 | Provider and service behavior under real network/lifecycle conditions remains unknown. | 3–5 | playback/network/service/diagnostics | Wi-Fi/mobile switch, VPN/DNS, background, process death, Bluetooth/noisy, low storage. | L |
| 15 | Feature enhancements only after P0–P3 | P4 | Prevent new surface area from hiding unresolved core defects. | P0–P3 complete | Product-selected files | Full reusable verification checklist. | Varies |

## Acceptance gates

Do not move to P4 or claim release readiness until all of these are true:

1. `:app:testDebugUnitTest`, `:innertube:test`, debug/release lint, Android test compilation, and signed CI release pass.
2. A physical device verifies sustained online playback, pause/resume/seek, queue restoration, and provider error feedback.
3. A completed download plays with network disabled, survives app restart, and is no longer playable after deletion.
4. Backup Merge/Replace have disposable-device evidence, clear scope wording, integrity checks, and a recoverable failure path.
5. No privileged secret is embedded in a distributable APK.
6. Every visible setting has an observed behavior or is removed/renamed.
