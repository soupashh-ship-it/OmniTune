# God Object Prevention

To ensure OmniTune remains maintainable, monitor file sizes and class responsibilities continuously. Future development must respect these thresholds to prevent the re-emergence of God Objects.

## Warning Thresholds (Line Counts)
* **300+ lines**: Inspect for responsibility creep. Are there private helpers that belong in a separate utility or collaborator?
* **500+ lines**: WARNING. The file is likely taking on too much state.
* **800+ lines**: Architecture review recommended. Refactoring is required before adding significant new features.
* **1000+ lines**: GOD OBJECT RISK. No new responsibilities may be added without an extraction plan.

## Examples

### ❌ Bad Architecture (What to Avoid)
* Adding notification construction or update logic directly into `MusicService`.
* Adding SQL queries for unrelated domains (like SearchHistory and Playlists) into one monolithic `DatabaseDao`.
* Putting every settings section (Audio, UI, Storage, Network) into one massive `SettingsScreen.kt` file.
* Adding unrelated navigation, playback control, and database calls directly into `MainActivity.kt`.

### ✅ Good Architecture (What to Encourage)
* **Focused Coordinator Classes**: `CrossfadePlaybackCoordinator`, `PlaybackRecoveryCoordinator`.
* **Focused DAOs**: `SongDao`, `PlaylistDao`.
* **Small UI Components**: Breaking screens into `<Feature>Section.kt` components.
* **Verification**: Running regular phase reports and continuous runtime verification to ensure boundaries aren't crossed.

## Automated Checks
Run `scripts/check-large-files.ps1` to perform a non-blocking scan of the codebase and identify files that breach these thresholds.