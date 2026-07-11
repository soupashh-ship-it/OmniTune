# OmniTune 0.12.7

This release focuses on playback reliability, offline downloads, data integrity, account synchronization, and release safety.

## Playback

- Restored Media3's native audio rendering pipeline to resolve infinite loading and device-specific playback failures.
- Prevented rapid play requests from racing and replacing the active queue.
- Limited background stream resolution to the validated next track after playback starts.
- Stopped transient network and unresolved-stream errors from incorrectly skipping multiple songs.
- Improved player listener cleanup and corrected the progressive-seek description.

## Downloads and Offline Library

- Restored completed downloads to the Downloads screen and offline playback queue.
- Added database-backed queued, downloading, completed, failed, and removed state tracking.
- Downloaded albums and playlists are marked complete only after every mapped song finishes downloading.
- Preserved downloaded collection names and contents while preventing premature completion status.
- Fixed retry-listener accumulation and serialized state updates to prevent stale progress events.
- Corrected song metadata so ordinary library songs are no longer treated as downloaded by default.

## Library, Lyrics, and Audio

- Prioritized exact video-ID lyric providers before search-based providers to reduce wrong-song matches.
- Passed album metadata to BetterLyrics for more accurate matching.
- Added persistent equalizer enablement, presets, and custom band levels with device-supported gain clamping.
- Removed misleading cache-limit information and unused stream-cache resources.

## YouTube Music Sync

- Persisted selected YouTube Music playlist IDs.
- Added network-constrained periodic synchronization with retry handling.
- Kept manual playlist sync and scheduled future updates after a successful selection.
- Disabled scheduled sync automatically when signing out.

## Data Integrity and Security

- Added Room migration schemas and migration validation coverage for every supported database version.
- Made emergency schema repair transactional and guaranteed foreign-key restoration.
- Merged unique tracks before removing duplicate playlists.
- Added bounded backup JSON reads, archive entry limits, storage safeguards, and cleanup after failed restores.
- Removed the WebView-based Discord session-token extraction flow and erased previously stored Discord tokens.
- Removed the nonfunctional Listen Together surface until a secure authenticated service is available.
- Removed unused placeholder screens, routes, components, and permissions.

## App and Release Quality

- Replaced the broken recursive launcher resources with the new OmniTune icon across adaptive and legacy launchers.
- Added unit coverage for equalizer persistence and Android migration-test compilation.
- Release automation now requires unit tests, Android-test compilation, and release lint before producing an APK.
- Added Dependabot and CodeQL workflows.

## Verification

- Unit tests pass.
- Android instrumentation test sources compile.
- Release lint completes with zero errors.
- Minified release APK assembly completes successfully.
