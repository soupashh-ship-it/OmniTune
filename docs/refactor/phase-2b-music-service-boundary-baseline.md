# Phase 2B MusicService Boundary Baseline

## Project State

* Branch: `refactor/music-service-boundaries`
* Starting commit: `122433d`
* Device/emulator: vivo I2202, Android 14 SDK 34, serial `138898743000055`
* `MusicService.kt` baseline line count: 1346

## Baseline Verification

| Command/check | Result | Notes |
| --- | --- | --- |
| `.\gradlew.bat clean assembleDebug` | PASS | Baseline build completed before edits. |
| `.\gradlew.bat testDebugUnitTest` | PASS | Baseline unit tests completed before edits. |
| `.\gradlew.bat lintDebug` | PASS | Baseline lint completed before edits. |
| `.\gradlew.bat installDebug` | PASS | Debug app installed on attached vivo device. |
| ADB launch | PASS | `com.omnitune.app.debug/com.omnitune.app.MainActivity` resumed. |
| Launch/search/playback/miniplayer/full player | PASS | Song playback from search, miniplayer, full player, play/pause, next, previous worked. |
| Queue/overflow actions | PASS | Queue opened; Play Next and Add to Queue verified through UI/logcat retry. |
| Settings/lyrics | PASS | Settings and lyrics UI opened. |
| Notification/background/reopen | PASS | Playback notification appeared; background media session stayed sane; app reopened. |
| Crash scan | PASS | No `FATAL EXCEPTION` found during baseline smoke. |

## Responsibility Review

| Responsibility | Currently in MusicService? | Existing extracted class? | Extraction needed? | Risk |
| --- | --- | --- | --- | --- |
| Notification channel/fallback/platform notification | PASS | NOT AVAILABLE | PASS | MEDIUM: notification actions and foreground behavior are user-visible. |
| MediaLibrarySession setup/lifecycle/custom layout | PASS | NOT AVAILABLE | PASS | MEDIUM: session and controller compatibility affect playback controls. |
| Lyrics prefetch/cache on item transition | PASS | NOT AVAILABLE | PASS | LOW: background-only if kept non-blocking and failure-safe. |
| Network callback/re-resolution/recovery hints | PASS | NOT AVAILABLE | PASS | MEDIUM: network transition recovery can affect active playback. |
| Player construction | NOT RUN | `PlayerFactory.kt` | NOT RUN | Already extracted in prior phase. |
| Queue persistence helper | PARTIAL | `QueuePersistenceManager.kt` | NOT RUN | Out of Phase 2B scope. |
| Playback event recorder helper | PARTIAL | `PlaybackEventRecorder.kt` | NOT RUN | Out of Phase 2B scope. |

## Notes

Only Phase 2B targets will be changed. DAO/database decomposition, UI redesign, playback rewrite, app identity, signing, license, and attribution are out of scope.
