# Settings behavior registry

`SettingsBehaviorRegistry.kt` is the machine-checked source of truth for every direct
preference-backed control in the Settings screens. `SettingsBehaviorRegistryTest` fails in
the JVM test suite when a visible `rememberPreference` control has no registry entry, a key
is registered twice, or an entry has no owner/effect. Controls that launch an action rather
than persist a value (backup export/import, update check, diagnostics, UPI, and community
links) are intentionally documented as actions, not settings.

| Screen | Settings covered | Runtime owner | Persistence / migration |
| --- | --- | --- | --- |
| Behavior | Autoplay, persistent queue, failed-track skip, Bluetooth start, mute pause, shuffle, task-clear | `MusicService`, `QueuePersistenceManager` | DataStore; immediate where supported, otherwise next service lifecycle |
| Playback | Metered policy, history duration, skip silence, normalization, offload, progressive seek, client, artist separators | Playback service, recorder, resolver/factory | DataStore; observers apply immediately or at next player rebuild |
| Appearance / Library / background | Palette, typography, thumbnail, swipe, shortcuts, custom backdrop | Compose theme/player/library surfaces | DataStore; Compose state is read on recomposition |
| Downloads / storage | Auto-download, Wi-Fi only, retry, parallelism, cache trimming | `DownloadUtil`, `ExoDownloadService`, `OmniTuneApp` | DataStore; admission/retry behavior changes on subsequent work |
| Parental controls | Explicit-content restriction and safe search | `SearchViewModel`, Home, playback | DataStore; current search is refreshed after change |
| Lyrics | Provider enablement and auto-scroll | `LyricsHelper`, lyrics screen | DataStore; next lookup/render uses the new value |
| Account / sync | Authenticated browsing, playlist sync, selected playlists, sync status | `OmniTuneApp`, `YouTubePlaylistSync` | Sensitive cookie is Keystore-encrypted; signed-out state forcibly disables sync |
| ListenBrainz | Enablement, now-playing, token, thresholds | `ScrobblingManager`, `MusicService` | Token is Keystore-encrypted; removing it disables scrobbling |
| Updates | Last-check time | Updates screen | Read-only persisted status, updated by the update action |
| Backup / restore | Export type, merge-category selection, Replace mode | `BackupRestoreViewModel`, `OmniBackupRepository` | UI-session selection only; archive contains library data and optional managed downloads, never credentials |

## Orphan-key compatibility policy

Preference keys are not removed merely because a reader is not found. The policy is:

- Current visible controls: registry entry plus runtime owner required.
- Retired Together, Discord RPC, and Last.fm values: one-time, enumerated cleanup in
  `RetiredFeaturePreferenceCleanup`; no unrelated preferences are touched.
- Legacy metadata/cache markers: retained only where a named migration or cache owner still
  reads them; otherwise classify them before deletion in a dedicated migration.
- Test-only or future keys: must not be wired to a visible control until an owner/effect is
  registered.

This is a source-level guard, not a claim of physical-device behavior. Device verification is
still deferred while USB/device testing is unavailable.
