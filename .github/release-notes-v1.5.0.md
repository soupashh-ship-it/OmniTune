OmniTune 1.5.0 is a stable release that rolls up the full prerelease cycle since 1.1.0 with a redesigned interface, stronger discovery controls, safer playback behavior, and broad reliability polish.

## Highlights

- Refreshed OmniTune branding, launcher icon, onboarding text, and in-app presentation for a cleaner stable identity.
- Restored the in-app updater entry in Settings for future stable and prerelease upgrade paths.
- Added Default Music Language under Settings > Discovery, with English as the default plus English, Hindi, Punjabi, Telugu, Tamil, Malayalam, Kannada, Bengali, Marathi, Gujarati, and Automatic options.
- Improved Home, discovery, search suggestions, browse pages, artist radio, autoplay, and smart queue recommendations with real YouTube Music language and region context.
- Completed a major UI refresh across Home, Search, Library, Player, Mini Player, Queue, Lyrics, Playlist Detail, Stats, menus, spacing, typography, semantic colors, touch targets, and directional navigation.
- Fixed playlist playback controls, first-play startup behavior, shuffle and favorite actions, download-start feedback, playlist edit feedback, stale search results, Android Auto voice search, PiP/media metadata, lyrics and mini-player progress sync, theme mode handling, playback settings wiring, SponsorBlock runtime wiring, and update checksum handling.
- Improved queue persistence, autoplay continuation, play-count/history/taste tracking, equalizer handling, Bluetooth handling, and playback service startup.
- Hardened YouTube/InnerTube parsing, trusted media host checks, YouTube import URLs, auth/token redaction, settings error visibility, and Android backup/privacy behavior.
- Removed stale or inactive donor, Discord RPC, AI provider, Last.fm, legacy menu, and duplicate launcher-icon code/resources.

## Stability And Quality

- 60+ bug fixes, reliability updates, and quality-of-life improvements since v1.1.0.
- Major UX polish across playback, playlists, queue, discovery, downloads, settings, and Android Auto flows.
- Backend/service cleanup across playback, recommendations, provider context, diagnostics, update checks, and data handling.
- Expanded unit, lint, Android-test compilation, and UI-test infrastructure coverage.

## Validation

- Local verification passed: `.\gradlew.bat assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`
- Local release workflow mirror passed: `.\gradlew.bat testDebugUnitTest compileDebugAndroidTestKotlin lintRelease`
- GitHub Actions Android Release verifies tests, Android-test compilation, release lint, signs the release APK, verifies it with apksigner, and uploads the APK plus SHA-256 file.

Package: com.omnitune.app  
Version: 1.5.0  
Version code: 150  
Status: Stable release

-- OmniTune
