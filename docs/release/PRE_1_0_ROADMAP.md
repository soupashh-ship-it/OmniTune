# OmniTune Pre-1.0 Roadmap

Status: post-`v0.7.0`.

This roadmap keeps pre-1.0 work focused on reliability, honesty, QA, and release trust. Large architecture modernization is intentionally deferred until after 1.5.

## Release Path

- `0.7.1`: Post-release hygiene and documentation truth patch.
- `0.8.0`: Downloads and search/provider hardening.
- `0.9.0`: Lyrics, Library, and Queue correctness.
- `0.9.5`: Device/feature verification and migration QA.
- `1.0.0`: Trust release freeze.

## Priorities Before 1.0

1. Keep Search playback, MiniPlayer, full player, Queue/Add to Queue, completed downloads, offline completed downloads, Settings, update checker, diagnostics/export, About, credits, and license access stable.
2. Audit download states before changing download behavior.
3. Audit search/provider failures before hardening failure states.
4. Verify or downgrade lyrics, Library, widget, accessibility, tempo/pitch, equalizer, Discord, Last.fm, Firebase, and migration claims.
5. Build device/OEM media-control evidence without promising universal notification or lock-screen behavior.
6. Add focused automated tests for mapping, queue, download playability, repeat/shuffle state, and failure classification.
7. Keep public docs aligned with verified behavior.

## Explicit Non-Goals Before 1.0

- No package name, namespace, applicationId, license, credits, or attribution changes.
- No large playback/download architecture rewrite.
- No fake lyrics, playlists, downloads, library data, or dead buttons.
- No new premium feature push before reliability gaps are closed.
