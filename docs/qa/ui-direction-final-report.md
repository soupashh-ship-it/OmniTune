# OmniTune UI Direction Final QA

## Build

- Commit under test before QA evidence: dbb3d74
- Branch: release/phase-5-rc-qa-offline-downloads
- Package: com.omnitune.app.debug
- Device: 138898743000055
- Build commands passed before device verification:
  - ./gradlew.bat assembleDebug
  - ./gradlew.bat testDebugUnitTest
  - ./gradlew.bat lintDebug

## Screenshots

Saved under `docs/qa/ui-direction-final/`:

- `home-top.png`: compact Home header, chips, image hero, Quick Picks area.
- `home-middle.png`: provider/local feed shelves during vertical scroll.
- `home-bottom.png`: lower Home shelves and nav padding.
- `collection.png`: native provider collection page.
- `search.png`: Search screen with result list and category chips.
- `miniplayer-over-home.png`: MiniPlayer over an active app screen after playback.
- `player.png`: full Player screen.
- `library.png`: Library tab.
- `stats.png`: Stats tab with real playback-derived data.
- `history.png`: History tab.
- `settings.png`: in-app Settings screen.
- `logcat.txt`: runtime logcat captured after the QA flow.

## Verified

- Home launches on a clean debug install without showing the old "Home Discovery" title.
- Home uses compact OmniTune branding, top Search/Settings actions, compact chips, image-led hero content, and horizontal feed sections.
- Quick Picks are real playable rows when playback/local data exists; first-run fallback remains compact and is not fake Quick Picks.
- Provider-backed hero/card navigation opens native collection pages instead of generic Search.
- Native collection page opens with artwork/title/actions and playable track rows.
- Track playback works from a provider collection; MiniPlayer and full Player metadata update.
- Bottom nav remains Home / Stats / History / Library.
- Search opens and displays typed results with category chips.
- Library, Stats, History, and Settings open without crash.
- Settings Updates and Diagnostics entries are visible in the debug app.
- No FATAL EXCEPTION was observed in the captured runtime flow.

## UI Direction

- Visual surfaces were moved toward the OmniTune direction: dark base, lower border alpha, fewer glass-heavy repeated rows, lighter Search/collection rows, calmer MiniPlayer/bottom nav/player glow.
- Artwork remains the primary visual emphasis on Home and native collection pages.
- Settings retains its existing functional categories while using calmer rows and spacing.

## Not Run

- Offline/airplane mode regression was not rerun in this final pass.
- Downloads playback was not rerun because no completed download was verified during this pass.
- Lyrics provider behavior was not rerun.
- Notification/background playback controls were not rerun.
- Queue, Play Next, and Add to Queue were not fully rerun in this final pass.
- `adb gfxinfo framestats` was not run for this pass.

## Remaining Risks

- Cold-start skipped-frame/Davey warnings may still appear and need profiler-backed tuning before a strict release gate.
- Native collection pages are improved visually but still carry more controls than Home by design.
- Logcat contains non-fatal media/network noise from normal runtime flows; no crash was observed in the verified path.

## Recommendation

SAFE_WITH_NOTED_RISKS for the UI direction pass. A release gate should still include offline, downloads, lyrics, notification/background playback, queue actions, and gfxinfo/framestats.
