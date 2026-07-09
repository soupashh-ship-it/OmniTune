# OmniTune v0.11.3
**Full Player and About Screen Polish**

### Fixes & Improvements
* Fixed the full-screen player header so it shows the current song title instead of the app name.
* Added inline synced lyric subtitles under the main song title when real LRC or TTML lyrics are available.
* Kept artist and album metadata as the fallback when lyrics are missing, loading, failed, or unsynced.
* Added smooth lyric line transitions and a tap target that opens the existing full lyrics sheet.
* Refined album-art-based dynamic song colors with stronger swatch filtering, dark-safe tone mapping, richer player gradients, and readable control surfaces.
* Remastered Settings > About with a premium OmniTune identity card, verified developer and inspiration links, dynamic install/version details, and accurate GPL-3.0 license information.

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
