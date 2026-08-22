# Test readiness status

Last verified: 2026-08-23 (this document replaces a previous version that
claimed a "232-test READY suite" for an external redesign workspace; that
claim was false for this repository).

## Verified green runs

| Suite | Result | When | Evidence |
| --- | --- | --- | --- |
| `:app:testDebugUnitTest` | 145 tests, 0 failures, 0 skipped | 2026-08-18 | `app/build/test-results/testDebugUnitTest/` |
| Instrumented (`connectedDebugAndroidTest`, physical device) | 32 tests, 0 failures, 0 skipped | 2026-07-29 | `app/build/outputs/androidTest-results/connected/` |
| Manual QA walkthrough (search → playback → process death → recovery) | Passed | 2026-07-29 | `.qa-runtime/` screenshots + uiautomator dumps |

CI runs `testDebugUnitTest` + `lintDebug`/`lintRelease` on every push and PR.
Instrumented tests are **not** executed in CI; they require a manual device run.

## What is genuinely covered

- Playback policy layer: recovery, error classification, queue persistence,
  autoplay continuation, notification contract, download lifecycle
- Backup/restore contract: preflight validation, safety archives,
  transaction boundaries, archive read/write
- Database: schema migrations 1→N with seeded data, integrity repair path
- Stream resolution incl. timeout branch; offline cache routing decisions
- Search stale-response gating; lyrics parsing/quality/caching;
  settings behavior registry

## What is NOT covered (do not assume otherwise)

- Compose UI layer (46 screens, 26 components): no UI tests at all
- Most ViewModels (`BackupRestoreViewModel`, `DownloadsViewModel`,
  `SettingsViewModel`, `PoTokenViewModel`, `TopPlaylistViewModel`, ...)
- End-to-end download flow (`DownloadUtil`, `ExoDownloadService`)
- Full `MusicService` lifecycle and notification posting on device
- The eight lyrics provider implementations as isolated units
- `simpmusic`, `betterlyrics`, `lrclib`, `kugou`, `canvas` modules: zero tests
- No coverage measurement tooling configured

## Outstanding runtime verifications

The RT-01..RT-15 smoke matrix in `docs/runtime/OMNITUNE_RUNTIME_EVIDENCE_TEMPLATE.md`
is unfilled. Device-dependent paths that compile and have unit-level coverage
but lack device proof: airplane-mode playback of completed downloads,
Replace-restore with interrupted media promotion, update install flow,
notification controls across OEM skins.
