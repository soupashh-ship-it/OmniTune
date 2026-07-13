## OmniTune 0.12.8

This release restores reliable in-app updates and improves lyrics loading and synchronization.

### Fixed

- Fixed update checks crashing on some Android versions because of a platform JSON compatibility issue.
- Restricted update checks to the latest public stable release from the official OmniTune GitHub repository.
- Continued APK installation automatically after Android grants permission to install updates from OmniTune.
- Restored inline synchronized lyrics below the song title when tracks change during continuous playback.
- Added automatic retries for temporary lyrics provider and network failures.
- Fixed full lyrics auto-scroll so the active line follows playback consistently.

### Improved

- Added an Auto-scroll synced lyrics option under Lyrics settings.
- Added regression coverage for GitHub release parsing and transient lyrics failures.
- Retained APK package, signature, version, size, and SHA-256 verification before installation.
