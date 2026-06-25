# OmniTune Download State Audit

Date: 2026-06-25
Scope: PRE-1.0 Phase 18, download/offline playback audit only.
Baseline: v0.7.0, versionName 0.7.0, versionCode 26.

## Summary

OmniTune's Downloads screen reads Media3 `Download` records directly from `DownloadManager.downloadIndex`. The UI has no separate domain-level download state model. Completed rows are the only rows made playable. Downloading, queued, stopped/paused, failed, removing, missing-cache, and unknown states are not playable from the row.

The completed-download playback path added before v0.7.0 is still present in code: `DownloadsScreen` emits the selected `Download`, `MainActivity` rejects non-completed downloads, maps the completed download to a DB-backed `MediaItem` when possible, falls back to title-only metadata when needed, and starts a one-item `ListQueue`. Playback resolution then detects the completed Media3 download and routes through the download cache.

The largest hardening gaps are state honesty and observability: missing cache/corrupt cache is not explicitly detected before playback, `STATE_REMOVING` is not explicitly labelled, old/non-DB downloads may only have title metadata, and diagnostics export does not include download-specific events or failures.

## Files Inspected

- `app/src/main/kotlin/com/omnitune/app/ui/screens/DownloadsScreen.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/screens/DownloadsViewModel.kt`
- `app/src/main/kotlin/com/omnitune/app/playback/DownloadUtil.kt`
- `app/src/main/kotlin/com/omnitune/app/playback/ExoDownloadService.kt`
- `app/src/main/kotlin/com/omnitune/app/playback/StreamUrlResolver.kt`
- `app/src/main/kotlin/com/omnitune/app/playback/MusicService.kt`
- `app/src/main/kotlin/com/omnitune/app/playback/PlayerConnection.kt`
- `app/src/main/kotlin/com/omnitune/app/playback/queues/ListQueue.kt`
- `app/src/main/kotlin/com/omnitune/app/extensions/MediaItemExt.kt`
- `app/src/main/kotlin/com/omnitune/app/models/MediaMetadata.kt`
- `app/src/main/kotlin/com/omnitune/app/db/DatabaseDao.kt`
- `app/src/main/kotlin/com/omnitune/app/db/entities/SongEntity.kt`
- `app/src/main/kotlin/com/omnitune/app/diagnostics/DiagnosticReportExporter.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/screens/LibraryViewModel.kt`
- `app/src/main/kotlin/com/omnitune/app/MainActivity.kt`

## Download State Source

`DownloadsViewModel` registers a Media3 `DownloadManager.Listener` and refreshes a `DownloadsUiState(downloads: List<Download>)` from `downloadUtil.downloadManager.downloadIndex.getDownloads()`.

The UI directly consumes Media3 `Download.state`. There is a lightweight composable-only `DownloadPresentation` in `DownloadsScreen.kt`, but there is no shared domain wrapper or sealed UI state model.

## Download State Mapping

| Media3 state | Current label | Current UI treatment | Notes |
| --- | --- | --- | --- |
| `STATE_COMPLETED` | Ready offline | Playable row | Only state where row `clickable` is attached. |
| `STATE_DOWNLOADING` | Downloading / Downloading N% | Non-playable row with progress if percent is known | Observed on device as `Downloading 0%`. |
| `STATE_QUEUED` | Queued | Non-playable row | Code mapped, not reproduced on device. |
| `STATE_STOPPED` | Paused | Non-playable row | Code mapped, no pause/resume UI found. |
| `STATE_FAILED` | Failed - retry available | Non-playable row with Retry and Remove | Code mapped, not reproduced on device. |
| `STATE_REMOVING` | Download state unknown | Non-playable row if visible before removal | Not explicitly mapped. |
| missing cache/corrupt cache | Not represented | Not explicitly detected | A completed index entry with missing cache is not distinguished before playback. |
| unknown other state | Download state unknown | Non-playable row | Safe fallback exists. |

## Playability Table

| State | Currently playable? | Should be playable? | Notes |
| --- | --- | --- | --- |
| completed | YES | YES | UI row clickable; `MainActivity` also guards for completed state before playback. |
| downloading | NO | NO | Observed as non-playable active row. |
| queued | NO | NO | Code maps as non-playable. |
| paused/stopped | NO | NO | Code maps `STATE_STOPPED` as non-playable. |
| failed | NO | NO unless retry action exists | Retry action exists for failed rows; row itself is not playable. |
| cancelled/removing | NO | NO | Removing is not explicitly labelled; falls through to unknown if visible. |
| missing cache | UNKNOWN in current UI | NO | No pre-play cache verification exists; playback may fail later. |
| unknown | NO | NO | Unknown fallback is non-playable. |

## Completed-Download Playback Path

```text
Downloads row tap
-> DownloadItemRow.onPlay, only attached when DownloadPresentation.playable is true
-> DownloadsScreen.onPlayDownload(download)
-> MainActivity Downloads route callback
-> guard download.state == Download.STATE_COMPLETED
-> database.getSongById(download.request.id)?.toMediaItem()
-> fallback MediaMetadata(id, title from download.request.data or id, artists empty, duration -1).toMediaItem()
-> PlayerConnection.playQueue(ListQueue(title = "Downloads", items = listOf(mediaItem)))
-> MusicService.playQueue(...)
-> StreamUrlResolver.resolveMediaItem(mediaItem, streamExtractor, downloadUtil)
-> downloadUtil.downloadManager.downloadIndex.getDownload(videoId)
-> if Media3 state is completed, return media item with download.request.uri and custom cache key
-> ExoPlayer uses MusicService CacheDataSource backed by downloadUtil.downloadCache
-> PlayerConnection observes current metadata for MiniPlayer/full player
```

## Metadata Fallback Behavior

| Scenario | Current behavior |
| --- | --- |
| newly downloaded track | Download request stores title in `DownloadRequest.data`; DB metadata is used if a `Song` row exists for the video id. |
| old completed download | Expected to use stored request title if DB row is unavailable; artist/artwork/duration may be missing. |
| completed download with DB song record | Uses `Song.toMediaItem()`, preserving title, artists, artwork, album, and duration. |
| completed download without DB song record | Uses title-only `MediaMetadata` fallback with empty artists and duration `-1`. |
| missing artist | Empty artist list; UI surfaces may show neutral/blank artist depending on player component fallback. |
| missing artwork | Player surfaces use their existing missing-artwork fallback. No fake artwork is generated. |
| missing duration | Duration `-1`; player progress code must tolerate unknown duration. |
| missing title | Falls back to `download.request.id`. |

Observed device metadata during audit:

- Search playback for `Faded` showed real title and artist in MiniPlayer/full player.
- The new download row showed title `Faded`.
- The new download did not complete during the audit window, so completed DB-backed vs non-DB-backed metadata could not be reverified on this install.

## Online/Offline QA Results

Device: `138898743000055`
Installed package during manual audit: `com.omnitune.app` release package.

| Test | Result | Notes |
| --- | --- | --- |
| App launch | PASS | v0.7.0 release launched. |
| Downloads render | PASS | Downloads screen opened with honest empty state initially. |
| Completed download playback online | NOT AVAILABLE | This installed release instance had zero completed downloads at audit start. |
| Completed download playback while Search active | NOT AVAILABLE | No completed download available. |
| Completed download playback offline | NOT RUN | No completed download available; forcing offline before completion would not test completed-download path. |
| Completed download after force-stop | NOT RUN | No completed download available. |
| Completed download after app reopen | NOT RUN | No completed download available. |
| Completed download after reboot | NOT RUN | Reboot was not practical and no completed download was available. |
| Search playback before download | PASS | Search result `Faded` played and MiniPlayer/full player updated. |
| Create new download | PARTIAL | Full-player Download action queued a real `Faded` download. |
| Active download state | PASS | Downloads showed Active count `1` and row label `Downloading 0%`. |
| New download completion | NOT AVAILABLE | The audit sample remained `Downloading 0%` after 30 seconds. |

## Active/Failed/Paused State Availability

| State | Device availability | Evidence |
| --- | --- | --- |
| active/downloading | AVAILABLE | `Faded` row showed `Downloading 0%`, Active count `1`, progress bar, Remove button. |
| queued | NOT AVAILABLE | Could not reproduce a queued state with one download and current network conditions. |
| paused/stopped | NOT AVAILABLE | No pause/resume UI found; state is code-mapped but not manually produced. |
| failed | NOT AVAILABLE | No safe forced failure was produced in this phase. |
| cancelled/removing | PARTIAL | The audit-created active `Faded` download was removed after testing; the row disappeared and counts returned to zero. No visible `Removing` state was captured. |
| unknown | NOT AVAILABLE | No unknown state encountered. |
| missing cache | NOT TESTED | Simulating cache corruption/removal would require destructive filesystem manipulation. |

## Non-Playable Row Behavior

- Downloading rows are not row-clickable.
- Queued/stopped/failed/unknown rows are not row-clickable by code.
- Failed rows expose Retry and Remove.
- All visible rows expose Remove.
- The active row clearly showed `Downloading 0%`; users are unlikely to interpret it as playable, but the row still uses the same large card shape as completed downloads.
- There is no tap feedback/message for non-completed rows because the row click modifier is omitted. `MainActivity` still contains a defensive completed-state guard if a non-completed download reaches `onPlayDownload`.

## Diagnostics Audit

`DiagnosticReportExporter` exports app/device/network data and recent sanitized logcat lines. It does not explicitly include structured download lifecycle or download playback events.

| Event | Logged/exported now? | Notes |
| --- | --- | --- |
| completed download play requested | PARTIAL | May appear in recent generic logs if Timber/logcat contains enough context, but no structured diagnostic section exists. |
| incomplete download tapped/rejected | NO | Non-completed rows are not clickable; defensive rejection is not diagnostic-exported. |
| failed download | NO | No structured download state export. |
| missing cache | NO | Not detected before playback and not diagnostic-exported. |
| metadata fallback used | NO | No diagnostic event when DB lookup fails and title-only fallback is used. |
| offline playback failure | PARTIAL | Generic playback/network logs may appear; no specific offline-download failure section exists. |

## Bugs Found

1. Missing-cache/corrupt-cache completed downloads are not detected before playback. A Media3 completed index entry is trusted even if cache content may be absent.
2. `STATE_REMOVING` is not explicitly mapped and falls back to `Download state unknown`.
3. Old or non-DB-backed completed downloads can be title-only with no artist, artwork, or duration.
4. Download diagnostics are not structured. Export does not reliably capture completed play requests, incomplete rejections, failed states, missing cache, metadata fallback, or offline failures.
5. New audit sample stayed `Downloading 0%` after 30 seconds, so active download progress/completion needs broader QA on different tracks/networks.
6. Library download count currently counts all Media3 download records, not only completed downloads.

## Non-Blocking Limitations

- No completed download was available on the installed release instance during this audit, so completed playback was code-traced but not reverified manually in Phase 18.
- Queued, stopped, failed, removing, unknown, and missing-cache states were not reproducible without more invasive setup.
- Reboot QA was not practical in this phase.
- Active/failed download state QA remains limited to one real active sample.

## Recommended Phase 19 Fixes

1. Add a strict reusable download playability function used by UI and playback handoff:
   - completed only
   - non-completed rejected
   - missing/corrupt cache rejected when detectable
   - unknown rejected with honest message
2. Add explicit presentation mapping for `STATE_REMOVING` with a label such as `Removing`.
3. Add a missing-cache/corrupt-cache check before completed-download playback where Media3 cache APIs make this safe.
4. Add metadata repair/backfill for completed downloads:
   - DB song metadata first
   - stored request title next
   - title/id fallback only when unavoidable
   - never fake artist/artwork/duration
5. Add user-visible rejection feedback for non-playable rows if they can be tapped through future UI paths.
6. Add structured diagnostics for:
   - completed download play requested
   - non-completed download rejected
   - failed download state
   - missing cache
   - metadata fallback used
   - offline download playback failure
7. Make Library download count honest by deciding whether it should count all records or completed-only records, then label it accordingly.
8. Add focused QA cases for queued, stopped, failed, removing, missing-cache, force-stop, reboot, and old/non-DB-backed downloads.
9. Preserve the v0.7.0 completed-download handoff through `ListQueue`; do not reroute completed downloads through a new playback architecture before 1.0.

## Phase 19 Update: Download State Hardening

Phase 19 hardening has been completed. The following changes were implemented:
1. `DownloadUtil.kt` now features an `isPlayable` method providing safe cache detection and explicit gating for `STATE_REMOVING`.
2. `DownloadsScreen.kt` maps `Download.STATE_REMOVING` to a user-friendly "Removing..." state and correctly handles the `percentDownloaded` 0% edge-cases.
3. Added diagnostic logging in `isPlayable` to catch missing caches and rejected playback attempts.
4. `DownloadsViewModel.kt` includes honest feedback rejection using Android Toasts and uses a robust metadata fallback for non-DB-backed downloads without generating fake data.
5. `LibraryViewModel.kt` counts only `STATE_COMPLETED` downloads, making the count semantic honest. `LibraryScreen.kt` accurately displays this count in its downloads card.
