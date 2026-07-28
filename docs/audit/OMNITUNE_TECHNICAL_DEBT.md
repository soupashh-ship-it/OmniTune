# OmniTune technical debt register

Complexity is an implementation estimate, not a schedule. “Blocks” means it should be resolved before the indicated dependent work or release decision.

| Area | Status | Evidence | Risk | Complexity | Recommended resolution | Blocks |
| --- | --- | --- | --- | --- | --- | --- |
| Release engineering | BROKEN | `AboutMetadataTest` fails under `:app:testDebugUnitTest`; both CI workflows require it. | No tag release can pass CI. | XS | Make URI construction/test deterministic and JVM-compatible; assert transaction ref deliberately. | Signed release |
| Downloads/offline | BROKEN | `downloadCache` is distinct from the only cache used in `PlayerFactory`. | Completed downloads cannot serve offline playback. | M | Add cache routing and test with airplane mode/app restart/delete. | Offline claim/release |
| Backup safety | REGRESSION RISK | Replace clears data after app/format-only validation and no safety export. | Conditional but high-impact user data loss. | M | Safety snapshot, count confirmation, schema/content validation, rollback retention. | Reliable restore |
| Backup scope | PARTIALLY IMPLEMENTED | Snapshot includes library entities/history/stats, but no preferences or queue; root copy claims preferences. | Settings/queue loss and misleading documentation. | M | Correct copy now; version supported prefs/queue later with compatibility tests. | Data-retention claim |
| Backup integrity | PARTIALLY IMPLEMENTED | ZIP path/size limits and transaction exist; no checksum/signature or restore E2E test. | Corruption/tampering not independently detected; offline archive restore unproven. | M | Manifest hashes, validation report, fixture tests, disposable-device runs. | Trusted restore |
| Backup compatibility metadata | REGRESSION RISK | Snapshot writes `databaseSchemaVersion = 5` while Room is v7 and validation ignores it. | Version field cannot reliably guide import compatibility. | XS | Define logical-vs-Room schema meaning, update/rename field, and test old/new imports. | Trusted restore |
| Architecture | REGRESSION RISK | `PlayerConnection` and service expose multiple state holders; service owns player but connection polls timer state. | State divergence/battery churn under lifecycle changes. | M | Define service-owned `StateFlow` contract; derive UI state once; remove polling where listener/event exists. | Playback hardening |
| Search concurrency | REGRESSION RISK | `SearchViewModel.kt` cancels jobs then catches `Exception` without rethrowing cancellation. | Stale state/results on rapid query changes. | S | Use `flatMapLatest` or query-generation check and rethrow cancellation. | Reliable search UX |
| Home resilience | PARTIALLY IMPLEMENTED | Sequential provider feed load; default catalog/fixed cold-start queries. | Generic or empty home feed after one provider error. | M | Independent provider requests, cached timestamped feed, explicit fallback labels. | Recommendation quality |
| Networking | IMPLEMENTED BUT UNVERIFIED | Stream extractor has two outer clients and resolver 10s timeout; provider calls rely on upstream. | VPN, rate-limit, DNS, region, expiry and network-switch behavior unknown. | M | Device/network matrix, structured error telemetry, user retry tests. | Stable playback/search |
| Playback | IMPLEMENTED BUT UNVERIFIED | `MusicService` resolves before `prepare`, queue restore and retry policies exist. | Prior symptoms cannot be dismissed without runtime evidence. | L | Instrumented Media3/service tests plus physical-device trace. | Daily-use certification |
| Downloads performance | REGRESSION RISK | `DownloadsViewModel` polls every 300ms despite a DownloadManager listener. | Wasted work/recomposition; large queues can amplify DB reads. | S | Event-driven state; visible-progress-only tick. | Performance |
| Database | IMPLEMENTED BUT UNVERIFIED | Room v7, explicit migrations, WAL and relationship repair; migration test compiles but did not run. | Real upgrade/recovery could differ from in-memory test schema. | M | Run migration Android tests and corrupt-db/upgrade test matrix. | Data-safety claim |
| Persistence/recovery | REGRESSION RISK | `SchemaTools.repairDatabaseFile` is complex and only schema-failure classifier has JVM test. | Repair can behave differently on real files or partial upgrades. | L | Add fixture DB tests for each repair condition and retain an on-disk backup. | Recovery confidence |
| Security | REGRESSION RISK | Secrets become `BuildConfig` fields (`app/build.gradle.kts:62-78`). | Privileged secret leakage from a public APK. | M | Rotate/restrict existing keys; remove bearer/server secrets from client. | Production release |
| Security/privacy | PARTIALLY IMPLEMENTED | Keystore migration encrypts cookie/PoToken/Last.fm values, but crash snapshots are stored in plain SharedPreferences/external app files. | Stack traces may expose URLs or user-derived errors; token flow not integration tested. | S | Redact/scope crash reports, define retention, test migration/logout. | Privacy assessment |
| Testing | REGRESSION RISK | 24 app unit files, 2 Android DB tests, 1 Innertube test; no end-to-end playback/download/UI/process-death suite. | Broken wiring can ship despite policy tests. | L | Build disposable profile, mock provider server, Media3 test player, and device smoke suite. | Stable release |
| Settings maintainability | BACKEND-ONLY | 87/230 preference keys lack any reader outside declarations. | Drift, misleading future UI, confusing migrations. | M | Remove/migrate dormant keys and add settings behavior registry tests. | Settings expansion |
| Dead code | OBSOLETE OR UNUSED | Together has no route/caller; Discord forcibly disabled; `ForYouSuggestionEngine` has no consumer. | Larger attack/maintenance/dependency surface and divergent feature paths. | M | Delete or complete one feature at a time with ownership/tests. | Maintainability |
| Notifications | PARTIALLY IMPLEMENTED | System media settings route only; root text promises alerts/in-app messages. | Product scope confusion; notification behavior unverified. | S | Rename or implement actual application notification domains. | Product claims |
| Build hygiene | REGRESSION RISK | Compiler prints Media3/Compose/Hilt deprecations and an experimental-coroutines opt-in warning; lint XML is clean. | Future dependency upgrades may convert warnings into failures. | S | Triage warnings by API lifecycle; resolve unsafe/moved APIs. | Upgrade work |
| Release signing | IMPLEMENTED BUT UNVERIFIED | Local release is unsigned by design; CI decodes secret keystore and verifies artifact. | Signing pipeline has not passed due unit-test gate. | S | Fix gate, run protected tag workflow, archive verification evidence. | Release |

## Positive technical controls worth preserving

- `MusicDatabase` registers explicit migrations 1→7 and does not call `fallbackToDestructiveMigration`.
- Migration test code preserves seeded songs, artists, albums, playlists and relation maps across supported versions.
- Backup ZIP import limits file size/count and rejects traversal paths before staging.
- `SecurePreferenceCipher` is used for cookie, PoToken and Last.fm sensitive DataStore values, with plaintext migration in `OmniTuneApp`.
- Release build executes R8/resource shrinking successfully; both lint reports are empty.
- Stream resolver has bounded attempts/timeout and logs classified provider failures.

These are implementation controls, not runtime guarantees. Keep them while resolving the risks above.
