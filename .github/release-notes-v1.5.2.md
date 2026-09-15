OmniTune 1.5.2 is a stable playback reliability and branding update.

## Playback Reliability

- Reduced first-play startup work by skipping unnecessary player-JavaScript and stream preflight requests for the normal Android playback route.
- Improved recovery when a DNS provider, VPN, proxy, firewall, or network route cannot reach YouTube.
- Applied shorter connection and read timeouts so blocked routes fail promptly instead of leaving playback stuck waiting.
- Kept the player-JavaScript resolver aligned with proxy changes made in Settings.
- Added clear playback messages for offline use, network blocks, region limits, sign-in requirements, expired stream URLs, and unavailable formats.
- Improved first-play diagnostic timings for future troubleshooting.

## Branding

- Updated OmniTune's default launcher icon to the new music-note mark.
- Updated themed launcher and notification resources to use the matching OmniTune mark.
- Existing installations return to the new default icon after updating.

## Quality

- Added regression coverage for first-play request selection and wrapped DNS/TLS network failures.
- Verified focused playback tests, release lint, and release APK assembly locally.
- GitHub Actions verifies the full test and lint suite, signs the APK, validates its signature, and publishes the APK with a SHA-256 checksum.

Package: com.omnitune.app
Version: 1.5.2
Version code: 153
Status: Stable release

-- OmniTune
