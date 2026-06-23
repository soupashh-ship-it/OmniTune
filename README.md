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

OmniTune is a modern, lightweight music player for Android that streams music directly from YouTube. Built with Jetpack Compose and Material 3, it delivers a clean, responsive experience.

Current release status: 0.6.x is a signed hardening line focused on playback recovery, queue behavior, offline downloads, release hygiene, in-app updates, diagnostics, and OEM media-control compatibility. It is not being presented as premium-ready until notification shade and lock-screen controls are fully verified on physical target devices.

## Features

- **Stream from YouTube** - Search and play songs via YouTube's InnerTube API
- **MiniPlayer** - Persistent mini player visible across app screens with live state
- **Full Player** - Album artwork, seek bar, play/pause/skip, shuffle, repeat, queue, and download controls
- **Synced Lyrics** - Auto-scrolling lyrics fetched from multiple providers
- **Home Feed** - Recently played songs at a glance
- **Library** - Browse albums, artists, playlists, liked songs, and downloads
- **Queue Management** - Add to queue and play next
- **Downloads** - Media3-backed downloads for offline playback
- **MediaSession** - External media controls through Android MediaSession
- **Media Controls Help** - Settings guidance for notification and lock-screen controls on restrictive OEM Android skins
- **Manual Updates** - Settings-based GitHub release checker with user-confirmed APK install
- **Discord Rich Presence** - Show what you're listening to on Discord
- **Last.fm Scrobbling** - Automatically scrobble plays to Last.fm
- **Dark Theme** - Customizable color palettes with dark mode support

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

Firebase/Crashlytics is optional for local builds and is only enabled with `-PenableFirebase=true`. Release signing must be configured with signing properties or environment variables; release builds must not fall back to debug signing.

## Download

Grab the latest signed stabilization-candidate APK from the [Releases](https://github.com/soupashh-ship-it/OmniTune/releases) page. Public release assets are named `OmniTune-vX.Y.Z-release.apk` with a matching `.sha256` checksum.

> **Note:** 0.6.x is focused on stabilization. Device-specific notification UI, lock-screen controls, and network-disabled edge cases remain under active QA.

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
