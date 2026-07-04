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
