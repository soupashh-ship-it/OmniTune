# Release Claim Verification

Status: post-`v0.7.3` baseline truth audit.

This file tracks public-facing claims that must be verified or downgraded before `1.0.0`.

## Verified

### Lyrics

- Status: Partially Verified (Needs Fixes).
- Evidence: UI entry is present, and plain text fallback, no-lyrics, and error states are verified. However, lyrics loading is currently very slow and synced lyrics do not automatically scroll/move with the song.
- Risk: High (Core UX issue).
- Next action: Fix lyrics loading speed, implement auto-scroll, and verify offline/cache behavior in Phase 2.

### Home Screen Widget

- Status: Verified based on existing evidence.
- Evidence: `OmniTuneWidget.kt`, `OmniTuneWidgetReceiver.kt`, and `AndroidManifest.xml` define the Glance widget and receiver. Prior audit evidence says it receives playback-state updates from `MusicService`.
- Risk: Low.
- Next action: Re-test on a physical device during Phase 27 to ensure the claim remains true after recent UI work.

### Signed Release Pipeline

- Status: Verified.
- Evidence: `.github/workflows/release.yml` built `v0.7.0`, verified the APK with `apksigner`, generated `.sha256`, and uploaded both assets.
- Risk: Low.
- Next action: For every release, confirm workflow success and asset presence.

### Library Playlist Detail

- Status: Code path verified (Runtime NOT AVAILABLE).
- Evidence: Phase 28 implemented real PlaylistDetailScreen backed by Room `playlistSongs()` DAO query. Empty state is honest ("No songs in this playlist yet"). Dead Toast placeholder removed. Playback route is wired. Runtime verification of detail screen was NOT AVAILABLE due to empty test library.
- Risk: Low.
- Next action: Keep verified. Playlist create/edit/delete management remains limited.

### Core v0.7.0 Playback Paths

- Status: Verified for v0.7.0.
- Evidence: Phase 14/16 QA passed Search playback, MiniPlayer, full player, Queue/Add to Queue, completed-download playback, offline completed-download playback, update checker, diagnostics/export, and legal access.
- Risk: Medium because playback depends on external provider availability.
- Next action: Re-run focused smoke tests before every release.

## Partially Verified Or Device-Dependent

### Wake Mode / Background Playback

- Status: Partially verified.
- Evidence: `MusicService.kt` configures playback wake behavior. Short release QA passed playback across tabs, but long background playback and battery-restriction behavior need device matrix testing.
- Risk: Medium.
- Next action: Run Phase 28 OEM/background playback matrix.

### Equalizer

- Status: Not fully verified.
- Evidence: System equalizer integration exists in `MusicService.kt`.
- Risk: Medium because system equalizer availability is device-dependent.
- Next action: Test on devices with and without a system equalizer.

### Discord Rich Presence

- Status: Not fully verified.
- Evidence: Kizzy integration module exists.
- Risk: Medium.
- Next action: Verify login/session behavior, privacy wording, and failure states before continuing to advertise the feature.

### Last.fm Scrobbling

- Status: Not fully verified.
- Evidence: Last.fm module and settings exist.
- Risk: Medium.
- Next action: Verify auth, scrobble submission, disabled state, and diagnostics privacy before 1.0.

## Not Tested

### Metro Player Layout

- Status: Not Tested.
- Evidence: Full player UI exists and was redesigned for OmniGlass, but a separate "Metro" layout claim has not been independently verified.
- Risk: Low.
- Next action: Verify the exact public claim or remove the wording.

### Custom Animations Performance

- Status: Not Tested.
- Evidence: `OmniTuneLoader` and OmniGlass UI animations exist.
- Risk: Low to Medium.
- Next action: Test jank/recomposition on a low-end device and compact screen.

### Accessibility / TalkBack Semantics

- Status: Not Tested.
- Evidence: Some content descriptions and semantics exist.
- Risk: Medium.
- Next action: Enable TalkBack and verify Home, Search, MiniPlayer, full player, Queue, Downloads, and Settings.

### Tempo And Pitch Adjustment

- Status: Not Tested.
- Evidence: Playback settings code paths exist.
- Risk: Medium.
- Next action: Test tempo/pitch extremes on device and verify no playback crash or stuck state.

### Client Rotator / 403 Fallback

- Status: Not Tested.
- Evidence: `ClientRotator.kt` and `StreamUrlResolver.kt` contain fallback behavior.
- Risk: High.
- Next action: Force or simulate 403, all-client failure, timeout, and offline cases.

### Room Migration From Older Versions

- Status: Not Tested for the current release line.
- Evidence: Room migrations and schemas exist, but upgrade paths from older public APKs need an install-over test.
- Risk: Medium.
- Next action: Install older signed APK, upgrade to current signed APK, and verify library/download data.

### Firebase Readiness

- Status: Not Tested / optional.
- Evidence: Firebase is gated behind `-PenableFirebase=true`.
- Risk: Low for default builds.
- Next action: Keep public wording clear that Firebase/Crashlytics is optional and not active by default.

