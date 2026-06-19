**Build Hardening & Safety (Phase 1)**

- **Moved `DebugActivity` out of Release Builds:** Prevented the 14KB debug-only crash handler from being shipped in the production release.
- **Product Flavor Architecture fixes:** Correctly defined and built `universal`, `arm64`, `armeabi`, and `x86` artifacts in `build.gradle.kts`.
- **Linter Rules:** Added a unified `lint.xml` for strict code checks.
- **Preference Keys Added:** Added foundation preference keys for Sleep Timer, Equalizer, and Downloads.
