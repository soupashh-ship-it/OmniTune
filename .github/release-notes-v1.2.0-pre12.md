OmniTune 1.2.0-pre12 prerelease.

This supersedes v1.2.0-pre11 with a focused playback and playlist-controls fix release.

Highlights since pre11:
- Fixed playlist detail Play button taps not starting playback from Home playlist pages.
- Fixed playlist detail Shuffle button taps by routing them into a shuffled playback queue.
- Fixed first manual song taps being lost while the music service is still connecting; OmniTune now keeps the latest tap and starts it once the player connection is ready.
- Fixed YouTube presentation songs entering playback as regular YouTube watch URLs instead of resolver-ready video IDs, which could leave the first track stuck before skipping.
- Updated mini-player and shared play/pause controls to resume through OmniTune's stream resolver instead of directly calling ExoPlayer play on unresolved/restored items.
- Improved playlist heart behavior for Home/remote playlists so saving and removing library bookmarks works even when a playlist is already cached by its YouTube browse ID.
- Added minimal download feedback from playlist detail pages: tapping the download icon now shows "Download started" while keeping the existing download queue behavior.
- Added regression coverage for presentation song playback mapping.

Validation:
- Local debug build, unit tests, and debug Android-test Kotlin compilation were run before tagging.
- GitHub Actions Android Release verifies tests, Android-test compilation, lintRelease, signs the release APK, verifies it with apksigner, and uploads the APK plus SHA-256 file.
- Package: com.omnitune.app
- Version: 1.2.0-pre12
- Version code: 130

Manual install note:
This prerelease APK is attached for manual download/install. Android may require allowing installs from the browser or file manager.
