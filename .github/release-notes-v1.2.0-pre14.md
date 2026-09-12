OmniTune 1.2.0-pre14 prerelease.

This prerelease adds a real music-content language preference so OmniTune can better control the language and region context used for remote discovery before the next stable release.

Highlights since pre13:
- Added a new Default Music Language setting under Settings > Discovery.
- English is now the default music-content language for new installs, existing upgrades without a saved preference, and missing or corrupted preference values.
- Added selectable music-content priorities for English, Hindi, Punjabi, Telugu, Tamil, Malayalam, Kannada, Bengali, Marathi, Gujarati, and Automatic.
- Added a central MusicContentPreferenceRepository and request-context model exposing selected language, effective language code, region, and YouTube Music locale.
- Updated YouTube Music request context mapping so language choices apply through real hl/gl provider context instead of a cosmetic UI-only toggle.
- Changed the low-level InnerTube fallback locale to English/US so early startup requests no longer inherit device or India/Hindi defaults before preferences load.
- Wired the setting into Home, discovery categories, search suggestions, browse detail pages, mood and genre browsing, artist radio, pick-a-mix discovery, related songs, autoplay, and smart queue fallback discovery.
- Added language-aware Home seed sections from real YouTube Music search responses for songs, playlists, and artists so the selected language can materially influence the feed.
- Reduced clearly competing language-specific Home sections while preserving normal personalized and explicitly opened content.
- Preserved explicit user searches: typing an artist or song name still sends the user query through without hard-filtering results.
- Preserved explicit navigation and personal data: albums, artists, playlists, Library, Downloads, liked songs, history, login cookies, and unrelated settings are not cleared or hidden by language changes.
- Added language-scoped Home state for remote sections and continuations so one language's Home cache cannot populate another language's feed.
- Added request-generation gating for Home so stale responses from a previous language cannot overwrite the active language state.
- Added an Apply & Restart flow that saves the new preference, reconfigures request context, and relaunches OmniTune cleanly without intentionally crashing or killing the process.
- Connected first-run guest language selection to the same Default Music Language preference.

Validation:
- Local debug build passed: `.\gradlew.bat assembleDebug`
- Local unit tests passed: `.\gradlew.bat :app:testDebugUnitTest`
- Local debug lint passed: `.\gradlew.bat :app:lintDebug`
- Local release assemble passed: `.\gradlew.bat :app:assembleRelease`
- No Android device or emulator was attached for manual runtime smoke testing.
- GitHub Actions Android Release verifies tests, Android-test compilation, lintRelease, signs the release APK, verifies it with apksigner, and uploads the APK plus SHA-256 file.
- Package: com.omnitune.app
- Version: 1.2.0-pre14
- Version code: 132

Known limitation:
- YouTube Music account personalization can still influence recommendations, so this setting strongly biases discovery toward the selected language but cannot guarantee perfect language purity.

Manual install note:
This prerelease APK is attached for manual download/install. Android may require allowing installs from the browser or file manager.
