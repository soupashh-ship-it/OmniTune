# OmniTune v0.6.7 Playback Interaction Polish & Network Reliability

## Focus

This release focuses on refining the playback interaction experience (Queue, Shuffle, Repeat, MiniPlayer controls) and resolving critical network reliability issues, particularly when switching between Cellular Data and Wi-Fi.

## Fixes and Improvements

### Interaction Polish
- **Queue Expansion**: Users can now queue songs smoothly directly from Search results.
- **Repeat Modes**: Fully implemented the Repeat cycle (Off → Repeat All → Repeat One → Off) and added a dedicated `ic_repeat_one` icon for clear visual feedback.
- **Shuffle Feedback**: Improved Shuffle logic. Toggling shuffle on a single-song queue now shows a helpful toast message.
- **Player Controls**: Addressed MiniPlayer touch targets, conditional Next button visibility, and improved full-player Previous/Next boundary handling.

### Network Reliability (Wi-Fi Fixes)
- **Robust HTTP Client**: Replaced `DefaultHttpDataSource` with `OkHttpDataSource` internally for more stable IPv4/IPv6 handling and to prevent YouTube CDN blocking.
- **Intelligent Network Switching**: The app now actively listens to network transport changes via `ConnectivityManager.NetworkCallback`. When switching from Cellular to Wi-Fi, the app instantly flushes the old IP-bound stream URLs from the cache to prevent `403 Forbidden` errors.
- **Seamless Resume**: If a track is actively playing or buffering during a network switch, the player transparently re-resolves a fresh URL bound to the new IP and seamlessly resumes playback.
- **Targeted Error Messages**: If a genuine stream failure occurs specifically on Wi-Fi, the player now surfaces an explicit message (*"Playback failed on this network. Try another Wi-Fi..."*) instead of a generic "No internet" error.

## Status

Version 0.6.7 delivers significant playback stability and polish. The fundamental playback experience (shuffle, repeat, queue manipulation, and dynamic network transitions) is now much closer to premium expectations.
