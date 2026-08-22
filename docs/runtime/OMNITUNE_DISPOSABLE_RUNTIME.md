# OmniTune disposable runtime environment

This runbook verifies OmniTune without touching an owner’s normal application data. It applies to a physical ADB device or emulator. It intentionally targets **only** the debug package, `com.omnitune.app.debug`; the release application ID is never cleared or installed by the helper script.

## Prerequisites

- Android SDK platform-tools (`adb`) on `PATH`, or pass `-AdbPath`.
- One explicitly selected device or emulator. Pass `-Serial` whenever more than one is connected.
- A debug build. The script saves logs, screenshots, status JSON, and pulled backups under `.qa-runtime/`, which is intentionally ignored by git.
- A disposable provider/account state. Do not put cookies, tokens, or account identifiers in evidence. Describe it as `signed out`, `disposable account`, or `provider unavailable`.

## Safe lifecycle

```powershell
.\scripts\qa\OmniTuneRuntime.ps1 -Action Build
.\scripts\qa\OmniTuneRuntime.ps1 -Action Install -Serial emulator-5554
.\scripts\qa\OmniTuneRuntime.ps1 -Action ResetDebugProfile -Serial emulator-5554 -ConfirmResetDebugProfile
.\scripts\qa\OmniTuneRuntime.ps1 -Action StartLogcat -Serial emulator-5554
.\scripts\qa\OmniTuneRuntime.ps1 -Action BuildTestApk
.\scripts\qa\OmniTuneRuntime.ps1 -Action InstallTestApk -Serial emulator-5554
.\scripts\qa\OmniTuneRuntime.ps1 -Action SeedDataset -Serial emulator-5554
.\scripts\qa\OmniTuneRuntime.ps1 -Action Launch -Serial emulator-5554
```

`Install` uses `adb install -r` and preserves the debug profile. `ResetDebugProfile` is the only destructive command; it refuses to run without its explicit confirmation and always calls `pm clear com.omnitune.app.debug`.

Use `ProcessDeath` to force-stop and relaunch while preserving data. Use `ForceStop` when the app must remain closed. End a run with `StopLogcat`.

## Isolated instrumentation fixtures

Build and install the test APK once, then run only an explicitly allow-listed fixture. `RunFixture` never clears the profile or accepts an arbitrary test class. Its result is saved under `.qa-runtime/` alongside the device-status snapshot.

```powershell
.\scripts\qa\OmniTuneRuntime.ps1 -Action BuildTestApk
.\scripts\qa\OmniTuneRuntime.ps1 -Action InstallTestApk -Serial emulator-5554
.\scripts\qa\OmniTuneRuntime.ps1 -Action RunFixture -Fixture PlaybackPreferences -Serial emulator-5554
.\scripts\qa\OmniTuneRuntime.ps1 -Action RunFixture -Fixture SearchViewModel -Serial emulator-5554
.\scripts\qa\OmniTuneRuntime.ps1 -Action RunFixture -Fixture MediaSessionCommands -Serial emulator-5554
.\scripts\qa\OmniTuneRuntime.ps1 -Action RunFixture -Fixture StreamResolver -Serial emulator-5554
```

The allow-list also contains `LibraryPlaylists`, `OfflineCache`, and `BackupRepository`. These fixtures use in-memory databases or uniquely named test/cache files. They complement, but never replace, the smoke matrix: they do not install, launch, seed, clear, or manipulate the debug app profile.

## Controlled network states

The script can request Wi-Fi and mobile-data changes only with explicit authority:

```powershell
.\scripts\qa\OmniTuneRuntime.ps1 -Action SetNetwork -Serial emulator-5554 -NetworkMode Offline -AllowNetworkMutation
```

It saves the resulting connectivity snapshot. Devices can deny these ADB commands or keep a VPN/ethernet transport active, so verify the saved status JSON and the Android system UI. Restore the prior state with `-NetworkMode Online -AllowNetworkMutation`, or use airplane mode manually when the device policy requires it.

## Smoke dataset

Seed the debug profile with the deterministic dataset in `RuntimeSmokeDataset` through its instrumented setup test before a smoke run. It contains:

| Item | Fixture |
| --- | --- |
| Tracks | Four fixed IDs across two artists and two albums |
| Library | Two liked tracks, history/statistics, and a multi-item queue |
| Playlists | One local playlist and one folder/tag where supported |
| Search | `OmniTune runtime fixture` plus a known provider query recorded in the evidence report |
| Downloads | One completed download created during the run; one failed/partial entry created by interrupting a test download |
| Settings | Non-secret playback and appearance settings only |

The completed download is deliberately created through the production `DownloadManager` during setup; it is not faked by writing files into a user profile. After it reaches completion, record its ID and use it for offline/delete checks. To produce a failed or partial item, begin a second disposable download and interrupt network before completion.

## Smoke execution

Use [the evidence template](OMNITUNE_RUNTIME_EVIDENCE_TEMPLATE.md) to run and record each item. Capture status before and after a run, a screenshot at meaningful UI boundaries, and one continuous filtered logcat file. The required matrix covers fresh and existing launches, search, playback controls, queue, likes/playlists, lyrics, download/offline/delete, setting persistence, process death, network loss, notification controls, UPI intent launch, and disposable Backup Merge/Replace.

## Evidence rules

- Record model, Android version, SDK, app version, commit, network state, and provider/account state from `Status` output.
- Store only generated artifacts in `.qa-runtime/`; never commit screenshots, logcat, backups, tokens, cookies, or personal media.
- For a backup artifact, use `PullBackup -RemoteBackupPath /sdcard/...` to copy only the explicitly named file.
- A result is **not verified** until actual runtime evidence records the expected and actual result. A skipped device test remains deferred, not passed.
