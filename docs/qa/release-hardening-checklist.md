# Release Hardening QA Checklist

Before marking OmniTune as ready for a stable release, the following checklist must be manually or automatically verified to catch regressions introduced during refactoring or feature development.

## 1. Build Gates
- [ ] `./gradlew clean assembleDebug` passes
- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew lintDebug` passes
- [ ] `./gradlew installDebug` succeeds on a physical device or emulator

## 2. Core Runtime
- [ ] App launches without crashing
- [ ] Search input works and returns results
- [ ] Playback initiates properly
- [ ] MiniPlayer appears and metadata matches the active track
- [ ] Full player opens from the MiniPlayer
- [ ] Queue opens and displays items
- [ ] "Add to Queue" appends tracks correctly
- [ ] "Play Next" inserts tracks correctly
- [ ] Settings screen opens without crashes
- [ ] Lyrics screen opens and fetches (or safely indicates missing) lyrics
- [ ] Downloads screen opens
- [ ] Notification appears and remains active
- [ ] Background playback continues when the app is minimized
- [ ] Reopening the app from the launcher restores the correct state

## 3. Database-Dependent Checks
- [ ] Queue restores after force-closing and reopening the app
- [ ] "Recently Played" updates after the appropriate threshold
- [ ] Search history persists and displays
- [ ] Lyrics cache serves fetched lyrics without network (if previously fetched)
- [ ] Library songs load correctly
- [ ] Albums display correctly
- [ ] Artists display correctly
- [ ] Playlists load and songs remain ordered
- [ ] Downloads/Format cache persists

## 4. Playback Edge Checks
- [ ] Next/Previous buttons work seamlessly
- [ ] Shuffle toggle randomizes the queue
- [ ] Repeat cycles correctly (Off -> All -> One)
- [ ] Seeking works accurately
- [ ] Track transition does not crash the player
- [ ] Crossfade operates smoothly (if enabled in settings)
- [ ] Equalizer alters audio (if enabled)
- [ ] Audio focus is respected (pauses during a call or other media playback)
- [ ] Lock-screen controls reflect the correct state and operate successfully
- [ ] Toggling Wi-Fi/Mobile data handles buffering gracefully

## 5. Downloads / Offline
- [ ] Complete one track download successfully
- [ ] Play the completed download while online
- [ ] Turn off the network (Airplane mode)
- [ ] Verify the completed download still plays offline
- [ ] Verify that a failed or incomplete download fails gracefully without crashing the app

## Release Recommendation Labels
At the end of a QA run, the reviewer must assign one of the following labels:
* **SAFE_TO_PROCEED**: Build, core, database, and offline checks all pass.
* **SAFE_WITH_NOTED_RISKS**: Core behavior passes, but some edge/download checks were NOT RUN or NOT AVAILABLE.
* **NOT_SAFE_TO_PROCEED**: Any failure in build, launch, playback, queue, database, or library basics.