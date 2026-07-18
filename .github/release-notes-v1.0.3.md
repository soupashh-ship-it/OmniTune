OmniTune 1.0.3 is a reliability and privacy release focused on room safety, Discord presence lifecycle cleanup, safer YouTube Music parsing, and more explicit handling of user data.

## Fixes and improvements

- Fixed Discord presence stop/restart behavior so the presence update loop is owned by the tracked job and is cancelled cleanly.
- Enforced Together guest playback and queue permissions on the host/server side, preventing modified or old clients from bypassing room settings.
- Hardened YouTube Music and InnerTube parsing against missing or changed third-party response fields in album, artist, home, history, queue, and transcript flows.
- Removed detached network side effects from Room entity methods and moved YouTube library sync into explicit lifecycle-owned call sites.
- Improved YouTube library sync error handling for songs, playlists, artists, and albums through a shared sync helper.
- Disabled Android automatic cloud backup and excluded app databases from backup and device-transfer rules to avoid silently preserving listening and download metadata.
- Strengthened diagnostic report redaction for bearer tokens, authorization headers, cookies, API keys, session values, PO tokens, and related sensitive fields.
- Added diagnostic redaction regression coverage for common bearer-token and auth formats.

Version: 1.0.3
Status: Released

-- OmniTune
