OmniTune 1.0.1 is a focused playback stability update.

This release addresses inconsistent playback starts, shuffle behavior, and previous-track navigation after the OmniTune 1.0 stable launch.

## Fixes and improvements

- Improved playback startup reliability by normalizing queue items before stream resolution.
- Reduced cases where the first selected song could remain stuck buffering until another track was chosen.
- Resolved explicit next and previous targets before seeking, avoiding unresolved stream URLs during manual navigation.
- Fixed shuffle being turned off when manually starting another song or queue.
- Preserved the user's current shuffle state when starting new playback.
- Added actual playback history for previous-track navigation, so shuffle mode goes back to the song that really played before.
- Fixed previous behavior for shuffled playlists, where it could jump to playlist order instead of the actual playback order.
- Routed widget next and previous controls through the same playback service path for consistent behavior.

Version: 1.0.1
Status: Released

-- OmniTune
