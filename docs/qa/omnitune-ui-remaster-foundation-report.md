# OmniTune UI Remaster Foundation Report

Date: 2026-07-07
Branch: `ui/omnitune-remaster-foundation`
Starting commit: `f84454ebaf61de754b893f275cc4c3d87ce8d11f`
Ending commit: `dd7f356821c92138df033f104f1ec3e9c522f39f`

## Reference Audit

Velune was used as a UX reference only. No Velune code, branding, assets, names, or exact visuals were copied.

What Velune does better:
- Dense discovery flow with quick access to playable music.
- Strong mini player and player affordances.
- Clear queue/player interaction hierarchy.
- Responsive row and card rhythms that make browsing feel active.

What OmniTune already had:
- A native OmniTune design system with dark glass surfaces, dynamic artwork color extraction, shimmer placeholders, discovery feed models, provider-backed Quick Picks, search actions, mini player, player overlay, queue, downloads, library, and settings.
- Existing stable playback/search/queue/download architecture that should remain the source of truth.

What was redesigned:
- Design foundation tokens for semantic spacing, shapes, motion, icon sizing, row heights, search bar height, and shared chrome dimensions.
- Home discovery presentation, including a current-track resume card and more intentional Quick Picks/shelf rows.
- Search bar and result row surfaces.
- Full player options sheet layout for small-screen safety.
- Queue bulk action index handling.
- Settings category labels.

What was not touched:
- Playback service connection.
- PlayerConnection state ownership.
- Search provider/view model behavior.
- Download manager behavior.
- Mini player playback controls.
- Home feed/provider hydration logic.
- Library/downloads data models.

Risks before changing UI:
- This repo has existing lint debt unrelated to this pass.
- Several large Compose files still contain older patterns and warnings.
- Manual runtime verification requires a connected device or emulator.

## Commits

- `df9346e` - `ui: add OmniTune remaster design foundation`
- `021359f` - `ui: remaster home and quick picks`
- `3a97470` - `ui: remaster search experience`
- `2bcc208` - `ui: polish full player options`
- `dd7f356` - `ui: polish queue and settings`

## Files Changed

- `app/src/main/kotlin/com/omnitune/app/ui/component/OmniComponents.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/player/PlayerScreen.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/screens/HomeDiscoveryScreen.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/screens/QueueScreen.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/screens/search/SearchBar.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/screens/search/SearchComponents.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/screens/settings/SettingsScreen.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/theme/OmniFonts.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/theme/OmniMotion.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/theme/OmniShapes.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/theme/OmniSpacing.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/theme/OmniTypography.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/theme/Theme.kt`

## UI Systems and Components

- Added semantic spacing values for screen, section, row, and card padding.
- Added shape and motion constants for future UI work.
- Added shared chrome sizing for icons, rows, touch targets, and search bar height.
- Pointed OmniTune typography at the app font family instead of the older alias.
- Removed a Velune-specific implementation comment from theme color handling.

## Screens Remastered

- Home / Quick Picks: added resume playback card and denser premium discovery rows/cards.
- Search: upgraded search bar surface, section headers, and result row hierarchy.
- Full Player: replaced fixed option rows with responsive weighted option grid.
- Queue: fixed bulk action index handling and removed composition-time StateFlow value access.
- Settings: replaced uppercase category labels with native title-case labels.

## Behavior Preserved

- Playback callbacks and PlayerConnection ownership were not changed.
- Search result playback, Play Next, and Add to Queue actions remain wired through existing callbacks.
- Home provider feed, thumbnail hydration, Quick Picks hydration, and collection navigation remain intact.
- Queue remove, download, and add-to-playlist actions still use existing PlayerConnection and LibraryViewModel behavior.
- Downloads and library behavior were not changed.

## Verification

- `.\gradlew.bat clean assembleDebug`: PASS
- `.\gradlew.bat assembleDebug`: PASS after final queue lint fix
- `.\gradlew.bat testDebugUnitTest`: PASS
- `.\gradlew.bat lintDebug`: FAIL due to existing project-wide lint errors outside this remaster scope.

Lint notes:
- Initial lint found one error in a changed file, `QueueScreen.kt`, from reading `StateFlow.value` inside composition. That was fixed.
- Rerun lint still fails with 48 errors, starting in `app/src/main/kotlin/com/omnitune/app/ui/menu/AlbumMenu.kt:215`.
- Changed files now show warnings/hints only in the lint report, not blocking errors.

Manual runtime status:
- `adb devices` found no connected device or emulator.
- Manual runtime checklist was not executed.

## Known Issues

- Lint remains blocked by unrelated existing errors in menu/settings/playlist/lyrics files.
- No manual device verification was possible in this session.
- PlayerScreen still has existing warnings around default locale formatting and modifier parameter order.
- QueueScreen still has existing non-blocking warnings/deprecations unrelated to this pass.

## Recommended Next UI Work

- Add a lint baseline or fix existing project-wide lint errors before enforcing lint in CI.
- Run device QA on 360dp width, gesture navigation, and 3-button navigation.
- Continue consolidating older screen-specific rows into shared OmniTune row/card components.
- Consider a focused mini player pass with screenshots and device verification.
- Polish downloads/library detail screens only after lint and runtime QA are stable.

## Future Feature Safety

This foundation is safe to continue building future UI features on from a build and unit-test perspective. The main caveat is project-wide lint debt and the lack of manual runtime verification in this session.
