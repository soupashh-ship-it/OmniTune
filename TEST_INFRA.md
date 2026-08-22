# Test infrastructure

This document describes the **actual** test infrastructure in this repository.
It replaces an earlier version that incorrectly described a "232-test E2E suite"
belonging to a separate redesign workspace that does not exist here.

## Layers

| Layer | Location | Count | Notes |
| --- | --- | --- | --- |
| JVM unit tests | `app/src/test/` | 39 files, ~145 `@Test` methods | JUnit 4, kotlinx-coroutines-test, Mockito |
| Instrumented tests | `app/src/androidTest/` | 11 files, ~32 `@Test` methods | Real Room, real Media3 session, migration helper against `app/schemas` |
| Module tests | `innertube/src/test/` | 1 file | Live-network smoke test (see caveats) |
| QA runtime harness | `scripts/qa/OmniTuneRuntime.ps1` | - | Safety-gated ADB driver for disposable-device verification |

Other library modules (`kugou`, `lrclib`, `simpmusic`, `betterlyrics`, `canvas`) currently have no test sources.

## Running

```powershell
# JVM unit tests (CI gate)
.\gradlew.bat :app:testDebugUnitTest

# Lint
.\gradlew.bat :app:lintDebug :app:lintRelease

# Compile instrumented tests without a device
.\gradlew.bat :app:compileDebugAndroidTestKotlin

# Execute instrumented tests (requires a connected device/emulator)
.\gradlew.bat :app:connectedDebugAndroidTest
```

### Known caveat: live-network test

`:innertube` contains a single test that performs a real YouTube search.
It is excluded from the default unit-test run via a JUnit `live-network` tag;
run it explicitly when you want it:

```powershell
.\gradlew.bat :innertube:test -Dtest.excludeTags=live-network   # default CI behaviour
.\gradlew.bat :innertube:test -Dtest.includeTags=live-network   # opt-in
```

## QA runtime harness

`scripts/qa/OmniTuneRuntime.ps1` drives a disposable debug build
(`com.omnitune.app.debug`) through allow-listed instrumentation fixtures:
status evidence, install, seeded datasets, logcat capture, screenshots,
process-death and recovery checks. Destructive actions require explicit
confirmation flags (`-ConfirmResetDebugProfile`, `-AllowNetworkMutation`).
Artifacts land in `.qa-runtime/` (gitignored).

The runbook lives at `docs/runtime/OMNITUNE_DISPOSABLE_RUNTIME.md`; the
RT-01..RT-15 smoke matrix template is in
`docs/runtime/OMNITUNE_RUNTIME_EVIDENCE_TEMPLATE.md`.

## Honest coverage picture

Well covered (behavioral tests): playback policies (recovery, error
classification, queue persistence, autoplay continuation, notification
contract, download lifecycle), backup/restore contracts (preflight,
safety store, transaction boundaries), database schema + migrations +
repair (instrumented), stream resolution, search request gating,
lyrics parsing/quality/repository caching, settings behavior registry.

Not covered: Compose UI layer (no UI-test dependency yet), most
ViewModels, end-to-end download flow, full `MusicService` lifecycle,
the eight lyrics providers as units, and all companion modules except
`innertube`. There is no coverage measurement tooling configured yet.
