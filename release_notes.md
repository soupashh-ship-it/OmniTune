# OmniTune v0.7.3

### 🚀 Performance & Playback Latency Improvements
* **Proactive Next-Track Resolution**: Implemented a background pre-resolver (`preResolveNextTracks()`) that securely resolves the upcoming track just-in-time without blocking the UI thread.
* **Seamless Transitions**: Replaced the legacy `omnitune-unresolved://` schema with native ExoPlayer queue injection, enabling instant (<1ms) audio transitions between songs natively!
* **Optimized Buffering Control**: Lowered ExoPlayer's `DefaultLoadControl` initial buffer window to heavily reduce "Tap-to-Audio" wait times on reliable networks.

### 🛠️ Stability & Bug Fixes
* **False Network Errors Suppressed**: Fixed a bug where rapidly skipping tracks manually on Wi-Fi would incorrectly trigger a "Playback failed on this network" Toast notification. Unresolved tracks are now handled gracefully without spamming UI errors.
* **Intelligent Network Invalidations**: Reconfigured the Stream Resolver to actively monitor ExoPlayer's HTTP exception types. Receiving HTTP `403 (Forbidden)`, `404 (Not Found)`, or `429 (Too Many Requests)` now triggers instant background cache invalidation and a fresh URL token generation.

*Note: All APK artifacts are securely signed natively through GitHub Actions Secrets.*
