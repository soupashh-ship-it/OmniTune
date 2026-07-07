# OmniTune Functional Appearance Customization Report

Branch: `feature/functional-appearance-customization`

Base commit: `2460fcfa04198216f12f88ca00492b0a7fcacbda`

## Reference Findings

The reference app stores appearance customization as persisted preferences, then consumes those preferences directly in the player, mini player, artwork, lyrics, and library composables. The useful pattern for OmniTune is not the exact visual design, but the dependency rule: a setting is visible only when a real component reads the preference and changes UI or behavior immediately.

## Reintroduced Functional Settings

| Setting | Dependency exists? | Implemented? | UI affected | Verified? |
| --- | --- | --- | --- | --- |
| Player design style | Yes | Yes | Full player artwork sizing, spacing, and immersive background treatment | Build verified |
| New mini player design | Yes | Yes | Mini player internal density, artwork size, button size, progress height | Build verified |
| New library design | Yes | Yes | Library hub spacing, route row density, category chip density | Build verified |
| Hide player thumbnail | Yes | Yes | Full player hides artwork and rebalances metadata/control spacing | Build verified |
| Player button colors | Yes | Yes | Full player controls, mini player play button, active controls, progress accent | Build verified |
| Player slider style | Yes | Yes | Full player progress slider stroke/wave and mini player progress indicator | Build verified |
| Mini player swipe sensitivity | Yes | Yes | Existing mini player swipe threshold and velocity behavior | Build verified |
| Lyrics presentation controls | Yes | Yes | Synced lyrics font size and line spacing presets | Build verified |
| Home/library/playlist visibility controls | Partial | Yes for real library/playlist shortcuts | Liked, downloads, top, cached shortcuts; playlist folder chips | Build verified |
| Reset appearance settings | Yes | Yes | Clears Appearance customization preferences back to defaults | Build verified |

## Deferred Settings

| Setting | Reason deferred |
| --- | --- |
| OmniTune Canvas | Existing OmniTune player does not have a safe canvas-artwork playback dependency wired to real song data. Exposing it would be fake or require a larger media/background feature. |
| Thumbnail cropping | OmniTune does not yet have one shared artwork component consumed by search, home, mini player, full player, and library. This should wait for a shared `OmniArtworkImage` pass. |
| Player thumbnail corner radius | Same dependency as thumbnail cropping; needs a shared artwork rendering component to avoid inconsistent behavior. |
| Theme Creator | Current advanced theme creator path is not a real end-to-end theme editor. It should stay hidden until accent/background controls are wired globally. |
| Background customization entries | Existing background customization is not integrated with OmniTune's remastered player/theme system. Exposing it would be misleading. |
| Playlist section visibility beyond existing shortcuts | Only existing real shortcuts were exposed. Additional playlist section controls should wait until the target sections are verified. |

## Preference Models Added

- `OmniPlayerDesignStyleKey` with `OmniPlayerDesignStyle.DEFAULT`, `COMPACT`, `IMMERSIVE`
- `OmniMiniPlayerDesignKey` with `OmniMiniPlayerDesign.DEFAULT`, `COMPACT`
- `OmniLibraryDesignKey` with `OmniLibraryDesign.DEFAULT`, `COMPACT_LIST`
- `OmniPlayerButtonColorModeKey` with `OmniPlayerButtonColorMode.DYNAMIC`, `DEFAULT`, `MONOCHROME`
- `OmniSliderStyleKey` with `OmniSliderStyle.DEFAULT`, `THIN`, `ROUNDED`
- `OmniLyricsPresentationKey` with `OmniLyricsPresentation.DEFAULT`, `COMPACT`, `LARGE`

Existing keys reused:

- `HidePlayerThumbnailKey`
- `SwipeSensitivityKey`
- `ShowLikedPlaylistKey`
- `ShowDownloadedPlaylistKey`
- `ShowTopPlaylistKey`
- `ShowCachedPlaylistKey`
- `ShowTagsInLibraryKey`

## Persistence And Reset

All visible controls read/write DataStore through OmniTune's existing `rememberPreference` and `rememberEnumPreference` helpers. The reset row removes only Appearance customization keys so defaults are restored without touching playback, queue, downloads, account, network, or service state.

## Verification Status

- `.\gradlew.bat assembleDebug`: PASS
- `.\gradlew.bat clean assembleDebug`: PASS
- `.\gradlew.bat testDebugUnitTest`: PASS
- `.\gradlew.bat lintDebug`: PASS
- `adb devices`: PASS, no device/emulator attached
- Runtime/manual device QA: NOT RUN because no device/emulator was connected
- Screenshots: not captured because no device/emulator was connected

## Known Risks

- Manual runtime verification is still needed to inspect exact visual differences on-device.
- Thumbnail cropping, canvas, and advanced theme editing remain intentionally hidden until their shared dependencies exist.
- Library design currently affects the main library hub. Deeper album/artist/playlist list view modes continue to use their existing local controls.
