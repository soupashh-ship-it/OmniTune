# Home Feed Final Runtime QA

Date: 2026-07-01
Device: vivo I2202
Android: 14
Package: com.omnitune.app.debug
Version: 0.8.0 (33)
Commit under test: 78b62530445d90ccbfc617a1e1e1a20b37ff3bb3 plus native collection polish

## Screenshots and Logs

- Home top, shelves, settings, search, offline Home, offline Search, provider collection, queue attempts, and playback screenshots were captured under `docs/qa/`.
- Gfxinfo framestats saved to `docs/qa/gfxinfo-home-feed-final.txt`.
- Final logcat saved to `docs/qa/home-feed-final-logcat.txt`.

## Home and Navigation

- Home launches without crash.
- Header remains compact with OmniTune branding, Search, and Settings.
- Bottom navigation remains Home / Stats / History / Library.
- Search and Settings are only top actions, not bottom-nav tabs.
- Provider-backed hero and community playlist shelves render with real thumbnails.
- Empty first-run Quick Picks do not show fake songs; Start Exploring is compact.
- Provider playlist hero and community playlist cards open native collection pages rather than generic Search.

## Native Collection

- Provider collection page opens without crash.
- Header artwork, title, count, Play, and Shuffle controls are visible when provider data exists.
- Track rows load from provider content.
- Track tap starts playback and updates MiniPlayer metadata.
- Visual polish was applied to reduce heavy boxed/glass surfaces and make rows more compact.

## Playback and Search

- Search opens from the Home header.
- Search query `arijit` returned provider results.
- Search result playback started successfully and MiniPlayer showed the selected track.
- Provider collection track playback started successfully in prior runtime pass.
- Full queue, Play Next, and Add to Queue were attempted but not conclusively verified.

## Offline and Fallback

- Wi-Fi and mobile data were disabled through ADB.
- App launched offline without crash.
- Offline Search opened without crash and showed an empty/start state rather than crashing.
- Network was restored after the offline pass.
- Completed download playback was not available/not run.

## Stability and Performance

- No `FATAL EXCEPTION` was found in the captured final logcat.
- Gfxinfo framestats ran after Home scroll, collection navigation, Search, and playback interactions.
- Reported frames: 49 total, 10 janky frames (20.41%), 50th percentile 15 ms, 90th percentile 77 ms, 95th percentile 250 ms, 99th percentile 650 ms.
- Slow bitmap uploads were 0.
- Cold-start/skipped-frame warnings remain a performance risk.

## Known Failures and Not-Run Checks

- `assembleRelease` failed because release signing environment variables are missing: `OMNITUNE_KEYSTORE_FILE`, `OMNITUNE_KEYSTORE_PASSWORD`, `OMNITUNE_KEY_ALIAS`, and `OMNITUNE_KEY_PASSWORD`.
- Queue screen verification was inconclusive.
- Play Next and Add to Queue were not fully rerun.
- Downloads playback was not run because no completed download was verified.
- Lyrics were not verified.
- Notification/background playback was not verified.
- Full artist/album/browse tap matrix was not exhausted.
- Gfxinfo shows remaining jank.

## Recommendation

SAFE_WITH_NOTED_RISKS for debug QA continuation. Not full release GO until queue/downloads/lyrics/background checks are completed and the remaining jank is reduced or accepted.
