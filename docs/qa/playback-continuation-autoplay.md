# Playback Continuation and Autoplay QA

## Summary

OmniTune now keeps playback moving after user-selected queues finish without inventing unavailable metadata. Liked Songs starts as a real collection queue, supports Shuffle Play, and loops when the collection reaches the end. Autoplay Radio only starts after explicit Play Next, user queue, and collection items are exhausted.

## Playback Priority

1. Manual Play Next item
2. Existing user queue
3. Current collection queue, including Liked Songs, Search Results, Quick Picks, and Home discovery lists
4. Autoplay Radio, if enabled
5. Stop safely

Autoplay is guarded by the `Autoplay similar songs` playback setting and never runs when a next queue item exists.

## Recommendation Source Chain

The resolver uses this order:

1. Verified genre, only when `PlaybackContext.genre` is non-blank
2. Verified mood/tag, only when `PlaybackContext.mood` is non-blank
3. Artist-based continuation
4. Provider related/title search continuation
5. Current session pool
6. Quick Picks/general discovery
7. Stop safely

Current OmniTune song metadata does not include genre or mood fields, so current runtime behavior does not claim same-genre recommendations. Those branches are implemented for future verified metadata only.

## Candidate Validation

Candidates must have a stable media ID, title, and, for artist continuation, an artist value. The resolver filters out the current track, failed candidates, and recently autoplayed songs. Recently autoplayed songs can be reused only after the current candidate pool is exhausted.

Stream resolution is attempted before playback. Failed candidates are marked and skipped. Autoplay stops after 3 failed stream-resolution attempts.

## Taste Learning

Autoplay songs produce session taste signals:

- Positive signal: listened for at least 60 seconds or at least 40% of the track duration
- Quick skip: skipped before 15 seconds

Positive autoplay songs become the next seed. Quick skips are added to the existing skip table and avoided in the active session.

## Liked Songs

- Row taps start the full Liked Songs queue at the tapped song.
- Play all starts the ordered liked queue.
- Shuffle Play persists the liked shuffle preference and starts a shuffled queue with the selected song first.
- End of the liked queue loops to the beginning, or creates a fresh shuffled order when shuffle is enabled.

## Settings

Added `Settings -> Playback -> Autoplay similar songs`.

Default: enabled.

## Verification

- `.\gradlew.bat assembleDebug`: PASS
- `.\gradlew.bat testDebugUnitTest`: PASS
- `.\gradlew.bat lintDebug`: PASS
- Runtime/device QA: NOT RUN, no Android device or emulator was attached

## Scenario Table

| Scenario | Metadata available | Fallback source used | Candidate selected | Verified result |
| --- | --- | --- | --- | --- |
| Track with genre | Verified context genre | VERIFIED_GENRE | Unit-test fake genre candidate | PASS |
| Track without genre but with artist | Artist only | SAME_ARTIST | Unit-test fake artist candidate | PASS |
| Track without genre and without artist | Title/current track only | RELATED_TITLE_SEARCH, then CURRENT_SESSION_POOL when related is empty | Unit-test fake related/session candidate | PASS |
| Quick Picks track | Source context and provider metadata | Existing queue first, then artist/related/discovery after queue end | Runtime pending | Build-covered |
| Search result track | Search source context and session items | Existing search queue first, then artist/related/session/discovery | Runtime pending | Build-covered |
| Autoplay disabled | Setting disabled | NONE | None | Unit policy PASS |
| Manual queue present | Next item exists | NONE | None | Unit policy PASS |
| Candidate stream resolve failure | Candidate ID available, stream fails | Retry next candidate, stop after 3 failures | Unit retry policy PASS |

## Known Limitations

- Genre and mood continuation are dormant until OmniTune receives reliable genre or mood/tag metadata.
- Runtime verification requires a connected Android device or emulator.
- Persistent queue restore does not restore old playback source context after process death; it resumes as a normal queue to avoid stale autoplay assumptions.
