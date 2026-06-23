# OmniTune v0.6.6 Media Controls Compatibility Hardening

## Focus

This release focuses on Android media notification and lock-screen compatibility, especially for OEM Android skins such as Vivo/iQOO/Funtouch OS. It does not redesign the UI or rewrite playback.

## Fixes and Improvements

- Bound Media3 playback notifications to OmniTune's explicit playback channel and stable notification ID.
- Hardened the playback notification channel for media usage: low importance, public lock-screen visibility, no sound, no vibration, and no badge.
- Switched the playback notification small icon to an app-owned vector icon.
- Added a guarded platform `Notification.MediaStyle` fallback for devices where Media3 exposes an active MediaSession but the OEM notification service does not post a visible media notification.
- The fallback uses the same `music_player` channel and notification ID, so it updates the same playback notification instead of creating a duplicate.
- Added debug-only `MediaControls` logs for notification permission, channel importance, MediaSession presence, player state, current title, mediaId, and queue count.
- Added Settings > Advanced > Fix notification & lock-screen controls.
- Added shortcuts to app notification settings, app details, and battery optimization settings.
- Added Vivo/iQOO/Funtouch OS guidance for notifications, lock-screen notifications, background activity, unrestricted battery, autostart, and cleanup exclusions.
- Documented the limitation that some OEM Android skins can hide media controls until the user allows notification, lock-screen, or battery settings.

## Verified

- `clean assembleDebug` before edits.
- `assembleDebug` after the media-controls compatibility patch.
- `assembleRelease lintDebug` after the fallback patch.
- Signed release APK install and launch through ADB.
- Signed release playback from search result.
- `dumpsys media_session` showed OmniTune as the active media button session with `Faded / Alan Walker` metadata.
- `dumpsys notification` showed an active OmniTune `Notification$MediaStyle` record with transport category, actions, public visibility, and MediaSession token.
- Android media key pause/play changed the active OmniTune MediaSession state.
- Network-disabled search still showed `No internet connection.` and `Retry when online.`
- Manifest already includes foreground service and media playback foreground service declarations.
- Media3 notification provider remains the source of playback notification behavior.

## Known Limitations

- Notification shade play/pause/next/previous need one physical tap verification on the target iQOO Neo 6 / Funtouch OS device.
- Lock-screen controls need one physical verification on the target iQOO Neo 6 / Funtouch OS device.
- Some OEM Android skins can hide media controls until notification, lock-screen, autostart, or battery settings are allowed by the user.

## Status

0.6.6 is a compatibility hardening release, not a premium-ready release. Some OEM Android skins can still require notification, lock-screen, or battery settings to be allowed by the user before media controls appear reliably.
