OmniTune 1.0.2 is a reliability and polish update focused on playback startup, network compatibility, lyrics quality, account security, and release hygiene.

## Fixes and improvements

- Improved first-track playback startup by placing the requested queue into the player immediately, then resolving the current stream before preparing playback.
- Reduced repeated stream-resolution work and added bounded stream validation timeouts to avoid long delays on unstable or international networks.
- Improved Private DNS, VPN, WARP, and DNS-filter compatibility by treating usable internet-capable networks as online even when Android network validation is unreliable.
- Updated playback failure messaging for Wi-Fi, DNS, VPN, and mobile-data cases so users get clearer recovery guidance.
- Improved lyrics selection by scoring providers, preferring synced trusted results, and rejecting likely wrong-script or wrong-language lyric matches.
- Reduced fullscreen lyrics polling frequency to lower UI and battery overhead.
- Added focused regression coverage for lyrics quality filtering.
- Fixed Saved/Liked artist and album play-time sorting so bookmarked-only artists and albums are not dropped.
- Added YouTube Music Sync status tracking with last sync time, status, and error details.
- Encrypted stored YouTube cookies, PO tokens, Last.fm sessions, and ListenBrainz tokens with Android Keystore-backed secure preference storage, including migration from existing plain values.
- Cleaned release lint down to no reported issues, converted app logging to Timber, removed duplicate launcher icon assets, and updated adaptive launcher icon metadata.
- Kept release automation compatible with the existing GitHub Actions signing pipeline and repository secrets.

Version: 1.0.2
Status: Released

-- OmniTune
