# Known Issues

Status: post-`v0.7.0` OmniGlass release.

## Current Known Issues

1. **Lyrics display surface unavailable**
   - Lyrics provider code exists, but a safe user-facing lyrics display surface is not available in the current release.
   - Public docs should not claim shipped lyrics UI support until Phase 23/24 verifies or downgrades the feature.

2. **Active, paused, failed, and unknown download states need broader QA**
   - Completed-download playback was fixed and verified online/offline for `v0.7.0`.
   - Active/failed/incomplete download states still need real-state QA before stronger public claims.

3. **Older completed downloads may have limited metadata**
   - Completed downloads can play, but older downloads may only have title metadata unless the song also exists in the local database.
   - Artist/artwork fallback must remain honest and must not fake metadata.

4. **OEM notification and lock-screen behavior varies**
   - Android media controls depend on device/OEM notification, lock-screen, and battery policies.
   - Settings guidance exists, but universal behavior is not guaranteed.

5. **Search/provider failure handling needs hardening**
   - Normal Search playback passed release QA.
   - No-network, provider partial-failure, timeout, and empty-result states need focused pre-1.0 audit and hardening.

6. **Client rotator and 403 fallback need forced QA**
   - Client fallback behavior exists in code, but forced 403/all-client-failure scenarios still need verification.
   - Retry limits and diagnostics should be proven before 1.0.

7. **Queue persistence and freshness need lifecycle QA**
   - Queue/Add to Queue passed v0.7.0 release QA.
   - Force-stop/restart, rotation, swipe-remove freshness, and mutation edge cases need a dedicated pass.

8. **Library sections and playlists need honesty audit**
   - Library UI is redesigned and uses real available data where present.
   - Albums, artists, playlists, liked songs, and recently played routes need a feature-by-feature honesty audit before 1.0.

9. **TalkBack/accessibility not fully verified**
   - Accessibility labels and touch targets were improved during OmniGlass, but TalkBack has not been fully tested across the app.

10. **Tempo, pitch, and equalizer need device QA**
    - Related code paths exist, but device coverage is incomplete.
    - Equalizer behavior is especially device-dependent.

11. **Architecture coupling remains long-term technical debt**
    - Playback, downloads, queue, and UI state are still tightly coupled in places.
    - A large architecture refactor is intentionally deferred until after 1.5; pre-1.0 work should stay focused on reliability and public-claim honesty.

## Recently Fixed

- Completed-download playback now switches to the tapped completed download.
- Completed-download playback works while a Search track is active.
- Offline completed-download playback was verified before `v0.7.0`.
- Signed release APK and checksum generation are handled by the verified GitHub Actions release workflow.
