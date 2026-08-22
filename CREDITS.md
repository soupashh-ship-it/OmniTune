# Credits

This project includes code derived from or inspired by the following open-source projects. All are licensed under GPL-3.0, which is compatible with this project's license.

## InnerTune

The `innertube` module (YouTube InnerTube API client: `InnerTube.kt`, `YouTube.kt`, the `models/` renderer hierarchy, client switching, and related playback plumbing) is derived from [InnerTune](https://github.com/z-huang/InnerTune) by Zion Huang and contributors, carrying forward work from its downstream forks (including [Metrolist](https://github.com/mostafaalagamy/Metrolist) and [OuterTune](https://github.com/OuterTune/Outertune)). The original upstream headers were replaced during rebranding; upstream git history remains the authoritative record of those authors.

## Velune

This project includes code derived from or inspired by [Velune](https://github.com/nikhilvishwakarma00/Velune) by Nikhil Vishwakarma.

### Copied/Adapted Files
- `CrossfadeAudio.kt` - Originally authored by Nikhil.
- UI Layouts & Components (e.g. `PlayerScreen.kt`, `LibraryScreen.kt`, etc.) - Ported and adapted from Velune UI.
- Playback & Queue Architecture (e.g. `MusicService.kt` queue semantics, Error Classification logic) - Inspired by and adapted from Velune's playback logic.

## Other attributions

- PoToken generation (`PoTokenGenerator.kt`): Kòi Natsuko (github.com/koiverse).
- Lyrics providers: data sourced from [LrcLib](https://lrclib.net), [KuGou](https://kugou.com), BetterLyrics, and SimpMusic community endpoints via the respective modules.
- [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) is used as a declared dependency for stream extraction.

All included code is Licensed under GPL-3.0. If you are an author of derived code and believe attribution is incomplete, please open an issue or pull request.
