# OmniTune v0.7.4 — Hardening & Polish Release

This release addresses the most impactful runtime and UX issues reported after v0.7.3, focusing on playback controls reliability, shuffle correctness, offline download robustness, lyrics auto-scroll, and artwork quality.

## What's New

### Playback Controls
- **Fixed MiniPlayer tap targets:** Tapping the MiniPlayer body now opens the full player reliably — but accidentally hitting Play/Pause or Next no longer triggers the body tap. Buttons correctly consume their own click events.
- **Honest disabled states:** Previous and Next buttons now report their true state through ExoPlayer instead of relying on UI-level heuristics. No more "enabled when it shouldn't be."

### Shuffle & Queue
- **Shuffle now correctly reflected in Queue screen:** The queue list now reads from ExoPlayer's actual shuffled timeline order instead of the original linear index. Enabling shuffle immediately updates the visible queue, and the current track stays unchanged.
- **No duplicated or lost tracks during shuffle.**

### Search-Result Queue
- **Full playlist-aware search playback:** Tapping a search result builds a real playable queue from the entire result set. Next/Previous navigate the full list, shuffle works across it, and the current track resolves first while upcoming tracks remain lazy. No UI blocking.

### Downloads Reliability
- **Offline playback fix:** Completed downloads now play reliably with no network connection. The fallback `MediaItem` for non-DB-backed downloads now includes the correct URI and metadata tag, enabling offline cache resolution.
- **Smoother progress:** Poll interval reduced to 300ms with matching animation timing for live, fluid progress bars.
- **Safer deletion:** Delete action now requires a deliberate tap plus confirmation dialog. Accidental partial swipes no longer risk data loss.
- **Offline verification:** Validated by disabling Wi-Fi and mobile data via ADB — completed download plays without network dependency.

### Lyrics
- **Fixed synced lyrics auto-scroll:** Programmatic scrolling (via `animateScrollToItem`) no longer confuses the manual scroll detector. The system now uses `collectIsDraggedAsState()` to distinguish human drags from auto-scroll.
- **Grace period:** Manual scrolling pauses auto-scroll for 3 seconds, then resumes automatically.
- **Clean state handling:** Loading, no-result, provider-error, synced, and plain lyrics states all render properly without crashing.

### Artwork Quality
- **High-resolution YouTube thumbnails:** The full player now uses `maxresdefault.jpg` (1920×1080) as the primary artwork source when available, falling through `sddefault.jpg` (640×480) to the existing thumbnail URL and finally a placeholder. All candidates are cached separately so the MiniPlayer's small thumbnail never pollutes the full-player's cache.
- **Crisp, square album-art layout:** Full-player artwork uses `ContentScale.Crop` on a balanced near-square card. No more letterboxed landscape thumbnails or excessive top/bottom gaps.
- **Background gradient retained** for visual depth behind the artwork.

## Full Changelog

- MiniPlayer: moved clickable body target to outer bounds (#1)
- PlayerScreen: honest disabled states for previous/next buttons
- PlayerExt: added `getQueueIndices()` for shuffle-aware queue ordering
- QueueScreen: bound to `queueIndices` flow — respects ExoPlayer ShuffleOrder
- DownloadsViewModel: added `.setUri()` and `.setTag()` for offline fallback MediaItems
- DownloadsViewModel: reduced poll delay from 1000ms to 300ms
- DownloadsScreen: removed unsafe `SwipeToDismissBox`, restored button+dialog deletion
- LyricsBottomSheet: migrated from `isScrollInProgress` to `collectIsDraggedAsState()`
- PlayerScreen: replaced `resize(1200)` with high-res YouTube `maxresdefault.jpg` fallback chain
- PlayerScreen: restored 330dp near-square artwork card with `ContentScale.Crop`
- MediaItemExt: upgraded artwork URI to 800px resolution
- KNOWN_ISSUES: updated to reflect fixed items

## Verification

- All changes built and tested via `assembleDebug`, `testDebugUnitTest`, and `lintDebug`
- ADB runtime smoke test performed on Android 14 (API 34) — search, playback, MiniPlayer, full-player controls, repeat cycle, shuffle, downloads, offline playback, background playback, and delete safety all PASS
- Artwork and lyrics smoothness require human visual QA — automated screenshots captured for reference

## Download

- `OmniTune-v0.7.4-release.apk` (signed, release-optimized)
- `OmniTune-v0.7.4-release.apk.sha256` (checksum)

## Known Limitations

- Downloaded files use Android 11+ scoped storage and will not survive an app uninstall/reinstall
- OEM notification/lock-screen behavior varies across manufacturers (Xiaomi, Samsung, Oppo, etc.)
- Some YouTube thumbnail images have baked-in letterbox borders — this is a provider limitation, not a layout issue
