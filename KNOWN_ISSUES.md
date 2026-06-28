# Known Issues

Status: post-`v0.7.3` baseline truth audit.

## Current Known Issues

1. **Recently Played and Liked Songs**
   - Recently Played does not currently show what users expect (or behavior is inconsistent).

2. **Playlists and Library Integration**
   - Playlist folders/collections are not currently possible.
   - Current duplicate prevention is application-level, not database-enforced.
   - Saved Artists and Saved Albums features are not fully realized.
   - Library sections must be wired together with real data, avoiding fake counts.

3. **Search and Provider Hardening**
   - Search and provider failure states (403, 404, 429, timeout, empty) need extensive hardening and honest user feedback.

4. **Device QA & Edge Cases**
   - OEM notification/lock-screen behaviors need honest device QA.
   - Accessibility and TalkBack need formal verification.
   - Equalizer, pitch, and tempo controls need device QA or honest experimental labels.
   - Android 11+ App-specific storage means downloaded tracks will not survive an app uninstall/reinstall.

5. **Settings Limitations**
   - Several settings (e.g., Pure Black mode, Blur toggles, grid layout toggles, explicit/video hiding, and scrobbling) are currently disabled and shown as "Not yet implemented" to maintain UI honesty.
   - History pauses are not supported yet, though users can manually clear search and listen history.

## Recently Fixed


- Global track options menu (Like, Play next, Add to queue, Add to playlist) implemented across all screens.
- Playlist full lifecycle (create, rename, delete, duplicate prevention) implemented.
- Native PlaybackQualityMode implementation (Auto, Data Saver, Balanced, High) with honest real-time stream resolution mappings.
- MiniPlayer tap targets and honest disabled previous/next controls.
- Shuffle correctness and QueueScreen order.
- Offline playback reliability and fallback metadata fixes.
- Downloads progress smoothness and swipe-to-dismiss deletion.
- Synced lyrics auto-scroll logic.
- High-resolution artwork extraction for search and player.

- Proactive next-track resolution added.
- Seamless, zero-latency transitions using native ExoPlayer queue injection.
- Optimized ExoPlayer initial buffering.
- Graceful suppression of false "No Network" errors on track transitions.
- Intelligent HTTP 403/404/429 cache invalidations.
- Signed release APK generation via verified GitHub Actions.

