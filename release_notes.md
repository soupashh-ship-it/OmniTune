# OmniTune v0.12.0
**Lyrics, Crossfade, and Download Root-Cause Fixes**

### Fixes & Improvements
* Fixed crossfade pausing the current song by preventing the overlap player from requesting competing Android audio focus.
* Fixed the next song silently running ahead during crossfade preload; it now stays prepared at the beginning and starts only when the fade window opens.
* Disabled audio offload while crossfade is active so Media3 can mix both decoded audio streams without an offload transition gap.
* Replaced the obsolete reflective audio-offload toggle with Media3's current track-selection API, so the Audio offload setting now controls real player behavior.
* Fixed song, album, playlist, search-result, bulk-selection, and auto-like downloads by resolving a playable stream URL before creating the Media3 download request.
* Added shared download request deduplication, bounded parallel stream resolution, explicit download-manager resume, and fresh-URL retry behavior.
* Fixed the download service's obsolete restart action so queued downloads can resume correctly when network requirements become available.
* Fixed synced lyrics detection when LRC metadata appears before timestamps and added TTML timing support to the standard lyrics screen.
* Fixed lyrics auto-follow when the lyrics screen opens mid-song and when multiple lyric lines contain identical text.
* Fixed fullscreen lyrics state carrying across song changes by resetting tracking and scrolling for each lyric document.
* Restored lyrics provider priority while retaining parallel requests, stopped automatic wrong-language YouTube transcript fallback, and kept subtitles available for manual provider selection.
* Refreshed successful lyrics for the current song so previously cached mismatches are replaced without permanently caching temporary provider failures.
* Added regression coverage for metadata-prefixed LRC and TTML timing.

### Verification
* `testDebugUnitTest`: passed
* `lintDebug`: passed
* `assembleDebug`: passed

### Build
* Version: **0.12.0** (code 60)

---

# OmniTune v0.11.9
**Playback, Lyrics, and Download Reliability Fixes**

### Fixes & Improvements
* Fixed crossfade for YouTube-backed tracks by resolving the next stream before starting the overlap player, so transitions no longer fade out into a failed/silent next item.
* Increased crossfade preloading time so the next track has more room to resolve and buffer before the fade begins.
* Fixed Pause when device volume is 0 by moving the behavior into the playback service instead of the UI-bound player connection, so it works while OmniTune is playing in the background.
* Fixed Auto-start on Bluetooth connect by handling both Bluetooth connection broadcasts and audio device callbacks, then resuming through the normal playback resolver.
* Fixed lyrics race conditions so slower results from a previous song cannot replace the lyrics for the currently playing song.
* Improved lyrics provider selection by trying ID-based lyrics before weaker title-search fallbacks and leaving YouTube subtitles as the last fallback to reduce wrong-language subtitle matches.
* Stopped permanently caching temporary lyrics misses, so songs can retry lyrics lookup after provider or network failures.
* Kept successful lyrics cached for fast repeat loads and added a unit test for stale lyrics result handling.
* Added a Clear all failed action in Downloads.
* Fixed failed playlist, album, and song download requests that were queued with raw YouTube IDs by retrying failed downloads with freshly resolved stream URLs.
* Improved live download status refresh for active downloads across download-aware screens.
* Increased local download concurrency from 5 to 8 parallel downloads to reduce queue wait time when the network and providers allow it.

### Verification
* `testDebugUnitTest`: passed
* `lintDebug`: passed
* `assembleDebug`: passed

### Build
* Version: **0.11.9** (code 59)

---

# OmniTune v0.11.8
**Settings Wiring and Download Status Fixes**

### Fixes & Improvements
* Added a Downloads icon beside Search in Library so users can open download status for queued, active, completed, and failed downloads.
* Fixed Playback & Audio settings that were only saved but not applied: network metering now lowers stream quality, Player Client now drives stream resolution, History duration now prunes old listening history, Stop music on task clear now stops active playback, and Progressive seek now changes seek skip increments.
* Added the missing Playback quality selector to Playback & Audio and wired it to the existing stream quality resolver.
* Added the missing Equalizer entry in Playback & Audio so the existing equalizer screen is reachable from Settings.
* Fixed Symbols to split artists so the settings row opens the existing separator editor.
* Aligned Audio normalization and Audio offload setting defaults with the playback engine defaults.
* Fixed Auto download on like so newly liked songs are queued from the playback service instead of only from one player button path.
* Re-audited Appearance settings and confirmed the visible Appearance options are wired to their player, mini-player, library, lyrics, theme, and shortcut consumers.

### Verification
* `testDebugUnitTest`: passed
* `lintDebug`: passed
* `assembleDebug`: passed

### Build
* Version: **0.11.8** (code 58)

---

# OmniTune v0.11.7
**Library, Stats, and YouTube Music Sync Fixes**

### Fixes & Improvements
* Fixed Library Recently Played so repeated plays of the same song show as one latest entry.
* Fixed Library playlist, song, artist, and album counts so they update from the actual library flows.
* Fixed Stats top artist rows so they show artist artwork and open the artist detail page.
* Improved YouTube Music sign-in by saving account profile metadata after login.
* Added a YouTube Music playlist picker in OmniTune Account settings to import selected playlists for sync.
* Fixed YouTube Music playlist sync failing to open when YouTube omits optional header/card `buttons` fields from browse responses.

### Verification
* `testDebugUnitTest`: passed
* `lintDebug`: passed
* `assembleDebug`: passed

### Build
* Version: **0.11.7** (code 57)

---

# OmniTune v0.11.6
**Settings Refresh and Home Artwork Hotfix**

### Fixes & Improvements
* Remastered the Settings hub with a cleaner OmniTune identity row, flatter settings list, lighter section structure, and less bulky card styling.
* Preserved all existing Settings destinations while improving readability, spacing, chevrons, icon treatment, and mini-player-safe bottom padding.
* Fixed Home carousel thumbnail upgrades for `i.ytimg.com` URLs so the large artwork above Quick Picks can load `maxresdefault` images instead of staying on lower-resolution defaults.
* Fixed `yt3.ggpht.com` artwork resizing so the requested size replaces the old size parameter instead of appending a broken suffix.
* Added focused tests for YouTube thumbnail URL upgrades.

### Verification
* `clean assembleDebug`: passed
* `testDebugUnitTest`: passed
* `lintDebug`: passed

### Build
* Version: **0.11.6** (code 56)

---

# OmniTune v0.11.5
**Mood and Genres Hotfix**

### Fixes & Improvements
* Fixed Mood and Genres `Show all` so it renders already-loaded Home provider categories immediately instead of waiting indefinitely for a fresh provider request.
* Improved Home carousel artwork quality by requesting larger YouTube thumbnails and caching the high-resolution artwork URL.
* Removed the `Play` / `Open` badge from Home carousel artwork so the cards show cleaner cover art above Quick Picks.
* Kept the direct YouTube Browse category flow from 0.11.4 unchanged.

### Verification
* `assembleDebug`: passed
* `testDebugUnitTest`: passed
* `lintDebug`: passed

### Build
* Version: **0.11.5** (code 55)

---

# OmniTune v0.11.4
**Home Discovery and Mood Categories**

### Fixes & Improvements
* Replaced the placeholder Mood and Genres screen with a real provider-backed catalog powered by YouTube Music mood and genre groups.
* Added grouped Mood and Genres sections with polished two-column cards, provider accent colors, loading shimmer, retry handling, and mini-player-safe spacing.
* Connected Home's Mood and Genres `Show all` action to the new deep catalog.
* Added a real YouTube Browse screen so categories such as Chill, Workout, Focus, and Genres open provider shelves directly instead of falling back to old search presets.
* Made Home's visible Mood and Genres cards provider-backed so the six-card grid no longer routes to the old static playlist/search behavior when real categories are loaded.
* Added timeout-backed loading and retry states so Mood and Genres cannot sit on an endless spinner when the provider stalls.
* Restored the personalized `Keep listening` shelf from listening history signals.
* Improved recent discovery labeling with `Similar to [artist]` shelves when artist metadata is available.
* Added a subtle dynamic ambient background to Home using OmniTune theme accents.
* Kept playback, search, queue, downloads, playlists, and Settings paths unchanged.

### Verification
* `assembleDebug`: passed
* `testDebugUnitTest`: passed
* `lintDebug`: passed

### Build
* Version: **0.11.4** (code 54)

---

# OmniTune v0.11.3
**Full Player and About Screen Polish**

### Fixes & Improvements
* Fixed the full-screen player header so it shows the current song title instead of the app name.
* Added inline synced lyric subtitles under the main song title when real LRC or TTML lyrics are available.
* Improved inline lyric readability with album-art-matched accent color, smooth color transitions, and a subtle tinted backing on the active lyric.
* Kept artist and album metadata as the fallback when lyrics are missing, loading, failed, or unsynced.
* Added smooth lyric line transitions and a tap target that opens the existing full lyrics sheet.
* Refined album-art-based dynamic song colors with stronger swatch filtering, dark-safe tone mapping, richer player gradients, and readable control surfaces.
* Remastered Settings > About with a premium OmniTune identity card, verified developer and inspiration links, dynamic install/version details, and accurate GPL-3.0 license information.
* Updated Settings > About with real GitHub avatar images and a working UPI support card for users who want to support OmniTune development.

### Verification
* `clean assembleDebug`: passed
* `testDebugUnitTest`: passed
* `lintDebug`: passed

### Build
* Version: **0.11.3** (code 53)

---

# OmniTune v0.11.2
**Playlist Motion and Changelog Fix**

### Fixes & Improvements
* Made playlist detail scrolling feel continuous by moving the hero, actions, custom order list, and suggestions into one scroll surface.
* Added a floating playlist toolbar with back, search, and menu controls that stays available while the playlist scrolls.
* Added a collapsing toolbar title that appears after the playlist header scrolls away.
* Added smooth lazy-list item placement for playlist rows and suggestions.
* Improved playlist detail bottom padding so content is less likely to sit behind system navigation or the mini player area.
* Replaced the Changelog placeholder in Settings with a real release-notes screen.
* Added bundled release notes for the installed app version so changes are visible even before checking for updates.
* Added a GitHub refresh action on the Changelog screen to show the latest published release notes when online.

### Verification
* `assembleDebug`: passed
* `testDebugUnitTest`: passed
* `lintDebug`: passed

### Build
* Version: **0.11.2** (code 52)

---

# OmniTune v0.11.1
**Playlist Detail, Search Actions, and Library Saves**

### Fixes & Improvements
* Added playlist detail metadata chips for song count and total duration, so playlist length is visible before playback.
* Refined playlist detail actions with working round controls for delete, play, shuffle, download, and edit.
* Added a full-width Add/Search songs action on playlist detail for faster playlist building.
* Improved the playlist song overflow sheet with real actions for play next, queue, add to playlist, like, library save/remove, playlist removal, download management, artist/album navigation, details, share, and radio.
* Added album download actions from search album results, queuing every track through the existing download pipeline.
* Added playlist save and playlist download actions from search playlist results.
* Saved provider playlists now appear in Library playlists with preserved song order and duplicate-safe inserts.
* Kept unavailable actions hidden instead of showing buttons that do not perform real work.

### Verification
* `clean assembleDebug`: passed
* `testDebugUnitTest`: passed
* `lintDebug`: passed

### Build
* Version: **0.11.1** (code 51)

---

# OmniTune v0.11.0
**Playlist Remaster**

### Features
* Remastered playlists with real create/edit/delete support, playlist detail pages, artwork collage covers, add-song search, suggestions, shuffle/play actions, playlist downloads, and persistent custom order.
* Added full-playlist playback planning so Play, Shuffle, and row taps queue the whole playlist with playlist playback context.
* Added a dedicated playlist add-song screen with YouTube song search, duplicate prevention, and add confirmation feedback.

### Improvements
* Improved Home mood and genre categories so Chill, Gaming, Workout, Focus, Romantic, Sad, Party, and related categories load more relevant songs instead of generic random results.
* Added category-specific query profiles, relevance scoring, duplicate removal, and safer fallback behavior for Home discovery category pages.
* Hindi/Bollywood searches are now limited to relevant Hindi/Bollywood categories instead of acting as a broad fallback for every mood.
* Removed stale reference-app branding from OmniTune source comments and ListenBrainz submission metadata.

### Build
* Version: **0.11.0** (code 50)

---

# OmniTune v0.9.8
**Playlist Variants, Context Menus, Bluetooth Auto-Play & More**

### 🚀 Features
* **Playlist Variants:** Added `CachePlaylistScreen` (offline/downloaded songs), `TopPlaylistScreen` (charts), and `LibraryMixScreen` (personalized auto-generated mixes)
* **Playlist Suggestions:** Suggested similar songs at the bottom of playlists
* **10 Context Menus:** Replaced generic track options with dedicated menus for albums, artists, playlists, YouTube songs/albums/artists/playlists, lyrics, and bulk selection
* **Bluetooth Auto-Play:** Auto-resume playback when a Bluetooth device connects (configurable in settings)
* **Persistent Queue Toggle:** Option to disable persistent queue restoration on app start
* **Permanent Shuffle:** Keep shuffle mode across queue changes (configurable setting)
* **Auto-Download on Like:** Automatically download liked songs (configurable setting)
* **Pause on Device Mute:** Pause playback when device volume reaches zero or is muted
* **Skip Silence & Audio Offload:** Respect user preferences for silent-skipping and audio offloading from player init
* **Tags in Playlists:** View, assign, and remove tags on cached and top playlists, with "Add Tag" chip for quick assignment

### 🛠️ Fixes
* Remove release signing enforcement to unblock development builds
* Updated `TagChip` composable to accept data via parameters instead of requiring direct database access
* Fixed compilation errors in `TopPlaylistScreen` and `LibraryMixScreen`

### 📦 Build
* Version: **0.9.8** (code 45)

---

# OmniTune v0.9.5
**Default App Icon & System Overhaul**

### 🚀 Features
* New default app icon
* Redesigned 3-dot overflow menu icon (Material Design style)
* New drag handle, volume, and horizontal more icons

### 🛠️ Fixes
* Fixed queue crash on song removal
* Fixed player options sheet layout issues
* Resolved duplicate songs in "Listen Together"

### 📦 Build
* Version: 0.9.5 (42)

# OmniTune v0.9.2

**Last.fm Scrobbling, Recently Played Chronological History, Discord Settings Polish**

### 🚀 Features
* **Last.fm Scrobbling**: Full scrobbling support with login dialog (username/password), now-playing updates, and automatic scrobble submission when the listening threshold is reached. Configurable via Settings → Integrations → Scrobbling.
* **Recently Played Chronological History**: Removed song-level dedup — each play now creates a separate entry, showing a full chronological log instead of just the latest play per song.
* **Discord Settings Polish**: Added editing UIs for all remaining Discord settings — button labels, URL sources, custom URLs, display custom entries (large text custom, large image/small image URLs), update interval value and unit.
* **Discord & Backup Screens Crash Fix**: Fixed nested `verticalScroll` crash that caused both screens to close immediately on open.

### 🧹 Code Cleanup
* **Removed Dead Preferences**: `PureBlackKey` removed. `DisableBlurKey`, `GridItemsSizeKey`, `HideExplicitKey`, `HideVideoKey`, `PauseListenHistoryKey`, and `PauseSearchHistoryKey` all cleaned up in previous sessions.

### 🛠️ Fixes
* **Search/Provider Error Hardening**: Added `ProviderError.kt` — classifies 403/404/429/timeout/network/parser errors with user-visible messages across Search, Home Discovery, and Home Collection screens.
* **HttpClient Cleanup**: Added tracked singleton + `shutdown()` method to `NetworkModule` for resource cleanup.
* **Dialogs Added to Discord Settings**: Activity name (text input), activity type (enum), status (enum), large image type (enum), large text source (enum), small image type (enum) — all added with proper dialogs.

### 📦 Build
* Version: **0.9.2** (code 39)

---

# OmniTune v0.9.1

**Discord Rich Presence Integration**

### 🚀 Features
* **Discord Rich Presence**: Full Discord integration showing your currently playing song on your Discord profile. Includes account login via WebView, activity customization (name/type/status), image selection (large/small), action buttons with custom labels/URLs, and configurable update interval.
* **Discord Settings Screen**: OmniGlass-styled settings with account connect/disconnect, live connection status indicator (green/yellow dot), and full activity/display/button configuration.
* **Auto-Start on Login**: Discord RPC starts immediately after login without requiring a service restart.
* **Lifecycle Wiring**: Presence starts/stops with playback and responds to lifecycle events via `DiscordPresenceManager`.
* **Auto-Reconnect**: Automatic reconnection with exponential backoff (up to 5 failures, 60s wait, then retry).

### 🛠️ Fixes & Improvements
* **Video ID Button URLs**: `resolveButtonUrl` now uses actual YouTube video IDs for accurate song links instead of display names.
* **Thread-Safe Login**: Discord login screen now uses `Handler(Looper.getMainLooper())` for all Compose state writes from WebView callbacks.
* **Hilt Cyclic Inheritance Resolved**: Removed `javax.inject` annotations from kizzy module (JVM-only) which was causing Hilt annotation processing errors.
* **HttpClient DI Binding**: Added Ktor `HttpClient` provider to `NetworkModule` for proper dependency injection.
* **Live Toggle**: Enable/disable Discord RPC and logout now live-start/stop the presence via `restartDiscordPresence()`.

### 📦 Build
* Version: **0.9.1** (code 38)
* APK is automatically signed via GitHub Actions Secrets when the `v0.9.1` tag is pushed.

---

# OmniTune v0.9.0

**OmniGlass UI Overhaul — Lyrics Display, Card-Group Polish, Dead Code Removal**

### 🚀 Features
* **Lyrics Bottom Sheet**: Tap the music-note icon in the full player to open a draggable bottom sheet with synced lyrics, auto-scroll to the current line, and a glowing animated active-line background. The "return to current" button fades in/out smoothly.
* **Custom Inter Font**: Integrated the Inter typeface across all Material3 text styles for a cleaner, more professional reading experience.
* **Shimmer Signal-Bars Loader**: Replaced the plain CircularProgressIndicator with a custom animated 4-bar signal meter featuring a shimmer sweep effect.
* **Player Background Effect**: Added dynamic background gradient extraction from album art in the full player, with blurred backdrop and animated color transitions.
* **Settings Sub-Navigation**: Replaced the single scrolling settings page with a categorized navigation system — 10 dedicated screens (Appearance, Playback & Audio, Content, Storage, Lyrics, Scrobbling, Updates, Diagnostics, About, Notifications) accessible via the main Settings index.
* **Card-Group Polish**: Stats and History screens now use `OmniPreferenceCard` groupings to match the Settings visual language, giving each section (Overview, Top songs, Top artists, Today, Yesterday, Older) a polished card appearance.

### 🧹 Code Cleanup
* **Removed Dead Files**: Deleted the old `GlassComponents.kt`, the legacy 1406-line `SettingsScreen.kt`, `HomeScreen.kt` (821 lines), and `HomeViewModel.kt` — all had zero remaining callers.
* **Stripped Deprecated QA Artifacts**: Removed outdated QA documentation and UI snapshot patches.

### 🛠️ Fixes
* **Completed-Download Playback Fix**: Resolved a regression where downloaded songs would fail to play after the download completed.
* **font_certs.xml Lint Compatibility**: Fixed `Unknown tag <string> in <array>` resource error by correcting the Android resource format, unblocking `lintDebug`.

### 📦 Build
* Version: **0.9.0** (code 37)
* APK is automatically signed via GitHub Actions Secrets when the `v0.9.0` tag is pushed.

---

# OmniTune v0.8.5

**Quick Picks Personalization, Pagination & Deduplication**

### 🚀 Features
* **Personalized Seeds**: Quick Picks now seed from your top artists, recently played, and liked tracks — with a curated default fallback when listening history is sparse.
* **Cross-Source Deduplication**: Duplicate songs from different providers (local vs. YouTube Music) are automatically collapsed using normalized title/artist keys.
* **Auto-Refresh Timer**: Quick Picks automatically refresh every 30 minutes to keep recommendations current.
* **Expanded Collections**: Page limits raised from 50 to 100 across home categories, provider collections, and search history.
* **Looping Pagination**: Album/playlist collections with >100 items now fetch continuation pages for full browsing (up to 100 items).
* **Personalized Song Carousel**: Home screen now shows a horizontal carousel of up to 20 personalized song recommendations.

### 🛠️ Fixes
* **Flow Operator Lint Warning**: Resolved lint warning on flow composition operators.

---

# OmniTune v0.8.4

**Quick Picks Overhaul — Swipeable Pages, Smarter Recommendations, Dead Code Removal**

### 🚀 Features
* **Swipeable Quick Picks**: Browse up to 4 pages of Quick Picks by swiping horizontally. Each page shows 5 song rows with dot indicators for navigation.
* **Play All Button**: A dedicated ▶ icon button on the Quick Picks header plays every song in the section sequentially — supports both local and provider-sourced tracks.
* **Expanded Song Pool**: Quick Picks now draws from up to 80 songs (up from 20), ensuring richer variety across all pages.
* **Quick Picks Mode Setting**: Choose between "Related to your listening" and "Related to last listen" modes in Content Settings.
* **Provider Hydration Fallback**: When local quick picks are unavailable, the app hydrates recommendations from YouTube Music collections to keep the feed fresh.

### 🧹 Code Cleanup
* **Removed Dead UI**: Stripped deprecated `ContinueCard` and `ContinueListeningCard` composables from both Home and Discovery screens, reducing visual clutter.
* **Removed Legacy QA Artifacts**: Cleaned up hundreds of outdated QA documentation and screenshot files.

### 🛠️ Fixes
* **Play All Reliability**: Fixed play-all for provider-sourced Quick Picks — previously only played the first song; now queues and plays all tracks sequentially.

---

# OmniTune v0.8.0

**Architecture & Stability Updates**
* **Database Optimization**: Decomposed the monolithic `DatabaseDao` into domain-specific DAOs (`SongDao`, `AlbumDao`, `PlaylistDao`) for improved maintainability.
* **Service Decoupling**: Extracted playback logic from `MusicService` into focused controllers (`PlaybackRecoveryCoordinator`, `RadioQueueManager`) to enhance playback stability.
* **QA Guardrails**: Established strict architectural boundaries and release testing protocols to prevent regressions.
* **Backward Compatibility**: Fully compatible with v0.7.x. User libraries, queues, playback history, and offline downloads are preserved without schema changes.
---

# OmniTune v0.7.6

### 🚀 Features
* **Global Track Options Menu**: "Like", "Play next", "Add to queue", and "Add to playlist" actions are now available natively on every song row throughout the app (Search, Albums, Artists, History, and Liked Songs).
* **Full Playlist Lifecycle**: You can now create, rename, and delete playlists natively.
* **Playlist Duplicate Prevention**: Seamless duplicate rejection checks are now built right into the local database mappings!
* **Audio Quality Settings**: Take back control of data usage with honest audio quality modes (Data Saver, Balanced, High, Auto) that actually dynamically instruct the stream extractor.

---

# OmniTune v0.7.3

### 🚀 Performance & Playback Latency Improvements
* **Proactive Next-Track Resolution**: Implemented a background pre-resolver (`preResolveNextTracks()`) that securely resolves the upcoming track just-in-time without blocking the UI thread.
* **Seamless Transitions**: Replaced the legacy `omnitune-unresolved://` schema with native ExoPlayer queue injection, enabling instant (<1ms) audio transitions between songs natively!
* **Optimized Buffering Control**: Lowered ExoPlayer's `DefaultLoadControl` initial buffer window to heavily reduce "Tap-to-Audio" wait times on reliable networks.

### 🛠️ Stability & Bug Fixes
* **False Network Errors Suppressed**: Fixed a bug where rapidly skipping tracks manually on Wi-Fi would incorrectly trigger a "Playback failed on this network" Toast notification. Unresolved tracks are now handled gracefully without spamming UI errors.
* **Intelligent Network Invalidations**: Reconfigured the Stream Resolver to actively monitor ExoPlayer's HTTP exception types. Receiving HTTP `403 (Forbidden)`, `404 (Not Found)`, or `429 (Too Many Requests)` now triggers instant background cache invalidation and a fresh URL token generation.

*Note: All APK artifacts are securely signed natively through GitHub Actions Secrets.*
