# Known Issues

Status: post-`v0.7.3` baseline truth audit.

## Current Known Issues

1. **Downloads UX, Progress, and Speed**
   - The Downloads screen can feel blank or lifeless.
   - Tapping downloaded music sometimes does not play correctly from the downloads list.
   - Pressing cross/delete removes downloaded music too easily without proper confirmation or management options.
   - Download progress does not update live (users must leave and reopen the screen).
   - Downloading music is generally too slow.
   - Download progress lacks real-time smooth animation.
   - Offline downloads are not safely stored/recovered upon app reinstall yet.

2. **Lyrics Speed and Sync**
   - Lyrics loading is very slow.
   - Synced lyrics do not automatically scroll/move with the song.

3. **Artwork and Thumbnail Quality**
   - Song thumbnails and artwork look blurry, especially in the full player view.

4. **Recently Played and Liked Songs**
   - Recently Played does not currently show what users expect (or behavior is inconsistent).
   - Liked songs do not work globally across all required UI surfaces yet.

5. **Playlists and Library Integration**
   - Playlists need full create/edit/delete functionality.
   - Playlist folders/collections are not currently possible.
   - Songs need to be addable to playlists from all major screens (search, player, downloads, library, recently played).
   - Saved Artists and Saved Albums features are not fully realized.
   - Library sections must be wired together with real data, avoiding fake counts.

6. **Search and Provider Hardening**
   - Search and provider failure states (403, 404, 429, timeout, empty) need extensive hardening and honest user feedback.

7. **Queue Lifecycle and Device QA**
   - Queue lifecycle, shuffle, repeat, rapid skip, and restart behaviors need comprehensive QA.
   - OEM notification/lock-screen behaviors need honest device QA.
   - Accessibility and TalkBack need formal verification.
   - Equalizer, pitch, and tempo controls need device QA or honest experimental labels.

## Recently Fixed

- Proactive next-track resolution added.
- Seamless, zero-latency transitions using native ExoPlayer queue injection.
- Optimized ExoPlayer initial buffering.
- Graceful suppression of false "No Network" errors on track transitions.
- Intelligent HTTP 403/404/429 cache invalidations.
- Signed release APK generation via verified GitHub Actions.

