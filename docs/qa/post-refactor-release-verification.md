# Post-Refactor Release Verification (v0.7.6-refactor)

Due to the massive scale of the God-object decompositions across `MusicService.kt`, `DatabaseDao.kt`, `SettingsScreen.kt`, `SearchScreen.kt`, and `MainActivity.kt`, a full manual regression sweep is REQUIRED prior to release.

## 1. Playback & Service (MusicService.kt decomposition)
- [ ] **Cold Start**: Force stop the app. Tap a song in the library. Audio must begin immediately without crashing.
- [ ] **Crossfade & EQ**: Enable crossfade and EQ in settings. Transition between two songs and verify seamless blending.
- [ ] **Scrobbling/Play Tracking**: Play a track past the 50% threshold. Verify the play count increments locally (`PlaybackEventRecorder.kt`).
- [ ] **Notification Lifecycle**: Background the app during playback. Ensure the media notification appears, updates track metadata upon skips, and disappears when playback is stopped.
- [ ] **Queue Restoration**: Play a queue, pause, force-kill the app, and reopen. The queue, current track, and exact playback position must be fully restored.

## 2. Persistence & Library (DatabaseDao.kt decomposition)
- [ ] **Lyrics Caching**: Open a song with lyrics. Disconnect network, reopen the same song. Lyrics must load instantly from local storage.
- [ ] **Search History**: Perform a new search. Force kill the app. Reopen the search screen. The previous search terms must persist.
- [ ] **Playlist Operations**: Create a playlist, add songs, rearrange them via drag-and-drop, and delete the playlist. Verify no metadata constraints are violated.

## 3. User Interface (Search & Settings Split)
- [ ] **Settings Sub-navigation**: Open Settings. Ensure all 11 sub-screens (Appearance, Playback, Updates, etc.) load correctly without crashing. Toggle "Skip Silence" and verify state persists on reopening.
- [ ] **Search Results Integrity**: Search for a popular term. Verify all 4 sections (Songs, Artists, Albums, Playlists) populate, and the `LazyListScope` extensions render without visual jitter.
- [ ] **Theme Application**: Toggle Dark Theme / Pure Black. Ensure the `OmniShellBackground` in `MainActivity.kt` respects the dynamic theme instantly.

## 4. Network & Recovery
- [ ] **Offline Recovery**: Disconnect the network while playing a streaming track. Verify the `NetworkPlaybackMonitor` correctly transitions the UI to a waiting state, and auto-resumes once network is restored.

**Status**: PENDING MANUAL QA
**Tester**: _____________
**Date**: _____________