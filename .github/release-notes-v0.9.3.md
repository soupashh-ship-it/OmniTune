# OmniTune v0.9.3

This release introduces significant performance optimizations and major functional updates to align the app's settings and player interface with user expectations.

### Changes & Fixes
* **Performance Optimizations:** Dramatically reduced time-to-first-frame for audio playback by lowering Exoplayer buffer thresholds. Added global connection pooling and caching to OkHttpClient to reuse TLS connections, and quadrupled the memory cache for resolved stream URLs to eliminate stuttering during rapid skips.
* **History Deduplication:** Fixed an issue where playing a song directly from the history screen caused it to duplicate in the list. The history UI now filters and surfaces only the single most recent playback event for each unique song.
* **Player Redesign:** Completely overhauled the main Player screen to match the Velune layout. Metadata (Title/Artist) is now left-aligned with "Share" and "Like" buttons adjacent. Rebuilt the bottom bar to cleanly display Queue, Sleep Timer, Lyrics, and a new 3-dot "More options" menu.
* **Player Options Bottom Sheet:** Added a new fluid bottom sheet for player options (Start Radio, Add to Playlist, Copy Link, Listen Together) tightly integrated with the system's clipboard, player queue, and radio logic.
* **Settings Revamp:** Stripped non-functional Velune placeholder options to ensure total feature honesty. Actively wired up remaining options, including dynamically shrinking the "Slim navigation bar" and toggling visibility of the "Liked," "Downloaded," and "Tags" rows within the Library based on user preference.
* **Restored Backend Architecture:** Restored the Ktor server and websocket dependencies stripped out in earlier milestones. Re-imported the true networking engine for "Listen Together" directly from the Velune source and wired it up to the UI so "Start session" successfully binds the local host and "Join session" handles correct client handshakes.
* **Account Connectivity:** Fixed the YouTube Music login flow. The "Sign in" button in Settings now launches a fully functioning Google Auth WebView that captures and seamlessly bridges the `SAPISID` cookie into OmniTune's backend.