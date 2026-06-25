# Regression Tests

To guarantee we do not break OmniTune while fortifying it, the following manual flows must be verified before proceeding to UI phases:

1. **Playback Initialization**
   - Click a searched song. Ensure it starts playing.
   - Click an artist. Ensure their top songs populate and play.

2. **Playback Controls**
   - Play/Pause toggle.
   - Next/Previous track.
   - Seek to a specific timestamp.

3. **Background & Notification**
   - Start a song, press Home. Song should continue playing.
   - Use the Android Media Notification to pause, skip, and resume.

4. **Queue Management**
   - Add song to queue.
   - Verify song appears in queue list.
   - Force close the app, reopen, verify the queue is restored.

5. **Downloads**
   - Click download on a track.
   - Track completes without crashing.
   - Turn off Wi-Fi/Data (Airplane Mode) and play the downloaded track.
   
6. **Search Capabilities**
   - Search a known Hindi song (verifies language support/fallback).
   - Search an English song.
   - Trigger a deliberate typo to test mixed/fallback results.

