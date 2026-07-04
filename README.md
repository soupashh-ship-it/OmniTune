# OmniTune

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="120" alt="OmniTune Logo">
</p>

<p align="center">
  <strong>An open-source music player for Android</strong>
</p>

<p align="center">
  <a href="https://github.com/soupashh-ship-it/OmniTune/releases/latest">
    <img src="https://img.shields.io/github/v/release/soupashh-ship-it/OmniTune?label=Latest%20Release" alt="Release">
  </a>
  <a href="https://github.com/soupashh-ship-it/OmniTune/blob/master/LICENSE">
    <img src="https://img.shields.io/badge/License-GPL--3.0-blue.svg" alt="License">
  </a>
  <a href="https://github.com/soupashh-ship-it/OmniTune/releases">
    <img src="https://img.shields.io/badge/Platform-Android%2010%2B-brightgreen" alt="Platform">
  </a>
</p>

---

## About

OmniTune is a modern, lightweight music player for Android that streams music directly from YouTube. Built with Jetpack Compose and Material 3, it delivers a dark-first OmniGlass interface with playback, queue, downloads, update checking, and diagnostics.

Current public release: `v0.9.1 - Discord Rich Presence Integration`.

The `0.9.x` line ships the OmniGlass UI overhaul, Quick Picks personalization, and Discord Rich Presence integration. Work toward `1.0.0` is focused on reliability, claim verification, download/search hardening, and device-specific QA. OmniTune is not claiming universal lock-screen or OEM media-control behavior.

## Features

- **Stream from YouTube** - Search and play songs via YouTube's InnerTube API
- **MiniPlayer** - Persistent mini player visible across app screens with live state
- **Full Player** - Album artwork, seek bar, play/pause/skip, shuffle, repeat, queue, and download controls
- **Lyrics** - User-accessible lyrics via the full player. Supports synced lyrics and plain text fallback. Availability depends on provider coverage and track metadata; some tracks may show a no-results or provider-error state.
- **Home** - OmniGlass landing screen with Search entry, current playback, and real available app state
- **Library** - Browse available liked songs, recently played items, albums, artists, playlists, and downloads where real data exists; Library honesty remains part of pre-1.0 QA
- **Queue Management** - Add to queue and play next
- **Downloads** - Media3-backed completed downloads for offline playback; active/failed states need broader QA
- **MediaSession** - External media controls through Android MediaSession where supported by the device/OEM
- **Media Controls Help** - Settings guidance for notification and lock-screen controls on restrictive OEM Android skins
- **Manual Updates** - Settings-based GitHub release checker with user-confirmed APK install
- **Diagnostics Export** - Share a diagnostic report for playback, downloads, update checks, and device-specific issues
- **Discord Rich Presence** - Full Discord integration: account login, activity customization, live connection status, auto-reconnect, and automatic start/stop with playback
- **Last.fm Scrobbling** - Present in code and settings; needs final claim verification before 1.0
- **Dark Theme** - OmniGlass dark-first theme with controlled glass surfaces

## Tech Stack

| Layer | Technology |
|-------|------------|
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, Hilt DI |
| Media | ExoPlayer (Media3), MediaSession |
| Database | Room |
| Networking | Ktor, OkHttp |
| Image Loading | Coil |
| Lyrics | LrcLib, KuGou, BetterLyrics, SimpMusic |
| Backend | YouTube InnerTube API |

## Building

### Prerequisites

- Android Studio with Android Gradle Plugin 9 support
- JDK 21
- Android SDK 36

### Build

```bash
git clone https://github.com/soupashh-ship-it/OmniTune.git
cd OmniTune
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

Firebase/Crashlytics is optional for local builds and is only enabled with `-PenableFirebase=true`. Signed public release APKs are produced by GitHub Actions from repository secrets. Local release builds require the documented `OMNITUNE_*` signing environment variables and must not fall back to debug signing.

## Download

Grab the latest signed APK from the [Releases](https://github.com/soupashh-ship-it/OmniTune/releases) page. Public release assets are named `OmniTune-vX.Y.Z-release.apk` with a matching `.sha256` checksum.

> **Note:** v0.9.1 ships the OmniGlass UI overhaul, Quick Picks personalization, and Discord Rich Presence integration. Device-specific notification UI, lock-screen controls, active/failed download states, and provider failure edge cases remain under active pre-1.0 QA.

If notification shade or lock-screen controls do not appear, see [Notification and Lock-Screen Controls](docs/troubleshooting/notification-controls.md). Some OEM Android skins can hide media controls until notification, lock-screen, or battery settings are allowed by the user.

## Project Structure

```
app/              # Main application module
  src/main/kotlin/com/omnitune/app/
    ui/           # Compose UI screens and components
    playback/     # Music service, player, queues
    db/           # Room database and entities
    data/         # Stream extraction and repositories
    lyrics/       # Lyrics providers and utilities
    extensions/   # Kotlin extension functions
    di/           # Hilt dependency injection modules
innertube/        # YouTube InnerTube API client
canvas/           # Visual canvas effects
lastfm/           # Last.fm scrobbling integration
kizzy/            # Discord Rich Presence
simpmusic/        # SimpMusic lyrics provider
lrclib/           # LrcLib lyrics provider
betterlyrics/     # BetterLyrics provider
kugou/            # KuGou lyrics provider
```

## Contributing

Contributions are welcome. Feel free to open issues or submit pull requests.

## Credits

This project includes code derived from or inspired by [Velune](https://github.com/nikhilvishwakarma00/Velune). See [CREDITS.md](CREDITS.md).

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
