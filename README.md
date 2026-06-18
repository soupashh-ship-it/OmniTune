# OmniTune

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="120" alt="OmniTune Logo">
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

## Features

- **Stream from YouTube** — Search and play any song via YouTube's InnerTube API
- **MiniPlayer** — Persistent mini player visible across all screens with live progress
- **Full Player** — Album artwork, seek bar, play/pause/skip, volume, shuffle, and repeat controls
- **Synced Lyrics** — Auto-scrolling lyrics fetched from multiple providers (LrcLib, KuGou, BetterLyrics, SimpMusic)
- **Home Feed** — Recently played songs at a glance
- **Library** — Browse albums and artists
- **Queue Management** — Add to queue, play next, reorder
- **Like & Save** — Favorite songs and manage your library
- **Media Notifications** — Notification controls with custom actions
- **Discord Rich Presence** — Show what you're listening to on Discord
- **Last.fm Scrobbling** — Automatically scrobble plays to Last.fm
- **Dark Theme** — Customizable color palettes with dark mode support

## Tech Stack

| Layer | Technology |
|-------|------------|
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, Hilt DI |
| Media | ExoPlayer (Media3), MediaSession |
| Database | Room |
| Networking | Retrofit, OkHttp |
| Image Loading | Coil |
| Lyrics | LrcLib, KuGou, BetterLyrics, SimpMusic |
| Backend | YouTube InnerTube API |

## Building

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Android SDK 34

### Build

```bash
git clone https://github.com/soupashh-ship-it/OmniTune.git
cd OmniTune
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Download

Grab the latest APK from the [Releases](https://github.com/soupashh-ship-it/OmniTune/releases) page.

> **Note:** This is pre-alpha software. Expect bugs and missing features.

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

Contributions are welcome! Feel free to open issues or submit pull requests.

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
