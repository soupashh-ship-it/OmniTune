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
