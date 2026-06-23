# Notification and Lock-Screen Controls

OmniTune uses Android MediaSession and a foreground media playback service for notification shade and lock-screen controls.

OmniTune also includes an OEM compatibility fallback that posts a platform `Notification.MediaStyle` playback notification with the same MediaSession token when the normal Media3 notification path is not visible to the system.

Some OEM Android skins can hide media controls until notification, lock-screen, or battery settings are allowed by the user. This is common on Vivo, iQOO, and Funtouch OS devices.

## Check These Settings

1. Open OmniTune > Settings > Advanced > Fix notification & lock-screen controls.
2. Open notification settings and allow OmniTune notifications.
3. If Android shows notification categories, allow the OmniTune Playback category.
4. Enable lock-screen notifications for OmniTune if your device has a separate lock-screen notification switch.
5. Open app settings and allow background activity if available.
6. Open battery settings and set OmniTune to unrestricted or not optimized if your device allows it.
7. On Vivo/iQOO/Funtouch OS, also allow autostart and keep OmniTune out of aggressive battery cleanup if those options exist.

## What OmniTune Can and Cannot Do

OmniTune can post a real media notification with a MediaSession token and expose play, pause, next, and previous commands.

OmniTune cannot bypass OEM notification, lock-screen, background, or battery restrictions. If the system hides media controls, the user must allow those settings manually.

## ADB Checks

After starting playback, these commands can help confirm the Android system sees OmniTune:

```powershell
adb shell dumpsys notification | findstr /i "omnitune media playback"
adb shell dumpsys media_session | findstr /i "omnitune com.omnitune playback"
adb logcat -d | findstr /i "MediaControls MusicService AndroidRuntime"
```

Expected results:

- A posted notification from `com.omnitune.app` or `com.omnitune.app.debug`.
- A MediaSession named `OmniTune`.
- No `AndroidRuntime` crash.
- Debug builds should log `MediaControls` lines with notification permission, channel importance, session status, player state, and current metadata.
