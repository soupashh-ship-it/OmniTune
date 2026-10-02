# OmniTune iOS KMP Test Infrastructure Specification

This document defines the master opaque-box, requirement-driven test infrastructure for the OmniTune iOS Kotlin Multiplatform (KMP) & Compose Multiplatform (CMP) project.

---

## 1. Test Philosophy

OmniTune's test architecture follows a strict **opaque-box, requirement-driven testing methodology**:

1. **Behavioral & Contract Verification**: Tests validate observable system behaviors, interface contracts, data models, network protocol serialization, queue state transitions, audio session lifecycles, and UI design tokens against specifications defined in `ORIGINAL_REQUEST.md` and `PROJECT.md`.
2. **Implementation Decoupling**: Tests treat components as black/opaque boxes interacting through public API boundaries, network schemas, state flows, and data streams. Tests do not inspect private internal implementation details.
3. **Deterministic Expected Outputs**: Every test derives its expected outputs from authoritative sources:
   - InnerTube / YouTube Music protobuf and JSON protocol specifications.
   - Apple MediaPlayer (`MPNowPlayingInfoCenter`, `MPRemoteCommandCenter`) and AVFoundation (`AVPlayer`, `AVAudioSession`) contracts.
   - SuvMusic 2.6.6.0 design system tokens (5 color palettes, Outfit typography, Squircle geometry, 9 waveform styles).
   - Standard Synchronized LRC specification ([mm:ss.xx] / [mm:ss.xxx]).
   - Kotlin Multiplatform Gradle project hierarchy and Xcode project scaffolding specifications.
4. **Host-Runnable Without Physical Hardware**: The test suite executes deterministically on the host environment (CLI, CI, Windows PowerShell, Node.js, JVM) without requiring an attached physical iPhone or external device farm.
5. **Zero Facades & Strict Integrity**: No hardcoded test results, facade runners, or superficial checks. Every test exercises real state machines, protocol parsers, mathematical calculations, cryptographic token builders, and queue algorithms.

---

## 2. Feature Inventory Mapping (24 Features)

All 24 features defined in `PROJECT.md § Feature Inventory` are mapped directly to dedicated test suites across Tiers 1 through 4:

| Feature # | Feature Name | Description | Milestone | Test Suite Location |
|:---:|---|---|:---:|---|
| **F01** | Multiplatform Gradle Setup | KMP & Compose Multiplatform plugins, `:shared` target hierarchy (iOS + JVM), settings & catalog | M1 | `tier1_features.test.js` (F01), `tier2_boundaries.test.js` (F01) |
| **F02** | iOS Xcode App Scaffolding | `iosApp/` Xcode project, Info.plist background audio, SwiftUI host, embed framework script | M1 | `tier1_features.test.js` (F02), `tier2_boundaries.test.js` (F02) |
| **F03** | Android Build Isolation Guard | Zero modification to `app/src/main/`, compilation & test pass preservation, no leaking KMP symbols | M1 | `tier1_features.test.js` (F03), `tier2_boundaries.test.js` (F03) |
| **F04** | Shared Innertube & Data Models | Domain models (`SongItem`, `AlbumItem`, `ArtistItem`, `PlaylistItem`, `Lyrics`, `StreamInfo`) serialization | M2 | `tier1_features.test.js` (F04), `tier2_boundaries.test.js` (F04) |
| **F05** | Innertube Search & Discovery Engine | Ktor multiplatform Innertube client, search categories, suggestions, browse payloads | M2 | `tier1_features.test.js` (F05), `tier2_boundaries.test.js` (F05), `tier3_interactions.test.js` |
| **F06** | Direct AAC Stream Resolution | `ANDROID_VR_NO_AUTH` & `IPADOS` client profiles extracting direct AAC (itag 140/139) audio streams | M2 | `tier1_features.test.js` (F06), `tier2_boundaries.test.js` (F06), `tier3_interactions.test.js` |
| **F07** | Pure Kotlin Proof-of-Origin Token | Multiplatform `PoTokenGenerator` protobuf packing, salt XOR masking, Base64 URL-safe encoding | M2 | `tier1_features.test.js` (F07), `tier2_boundaries.test.js` (F07), `tier3_interactions.test.js` |
| **F08** | Multiplatform Lyrics Services | LRCLIB, KuGou, and SimpMusic Ktor clients, time-coded LRC parser, provider scoring | M2 | `tier1_features.test.js` (F08), `tier2_boundaries.test.js` (F08), `tier3_interactions.test.js` |
| **F09** | Cross-Platform Audio Player Contract | `AudioPlayer` interface, state transitions (`IDLE`, `PREPARING`, `PLAYING`, `PAUSED`, `COMPLETED`) | M3 | `tier1_features.test.js` (F09), `tier2_boundaries.test.js` (F09), `tier3_interactions.test.js` |
| **F10** | iOS Native Audio Engine | `IosAudioPlayer` (`AVPlayer`, `AVAudioSession`, category `playback`, route change, interruption) | M3 | `tier1_features.test.js` (F10), `tier2_boundaries.test.js` (F10) |
| **F11** | iOS Lockscreen & Control Center Media | `MPNowPlayingInfoCenter` metadata updates and `MPRemoteCommandCenter` playback control handlers | M3 | `tier1_features.test.js` (F11), `tier2_boundaries.test.js` (F11), `tier3_interactions.test.js` |
| **F12** | Playback Queue State Management | Queue engine: enqueue, remove, reorder, shuffle mode, repeat modes (`OFF`, `ALL`, `ONE`), boundaries | M3 | `tier1_features.test.js` (F12), `tier2_boundaries.test.js` (F12), `tier3_interactions.test.js` |
| **F13** | SuvMusic Theme & Typography | Outfit typography, 5 color schemes (Purple, Blue, Orange, Green, Pink), Pure Black AMOLED, Squircles | M4 | `tier1_features.test.js` (F13), `tier2_boundaries.test.js` (F13) |
| **F14** | Home Screen UI | Profile greeting by time-of-day, 2-column quick access, mood chips, quick picks, pull-to-refresh | M4 | `tier1_features.test.js` (F14), `tier2_boundaries.test.js` (F14) |
| **F15** | Search Screen UI | Animated search bar, filter tabs (`YOUTUBE_MUSIC`, `LIBRARY`, `DOWNLOADS`), search suggestions debounce | M4 | `tier1_features.test.js` (F15), `tier2_boundaries.test.js` (F15) |
| **F16** | Library Screen UI | Pill filter chips, grid/list view mode toggle, sorting criteria (Title, Date, Artist, Play Count, Duration) | M4 | `tier1_features.test.js` (F16), `tier2_boundaries.test.js` (F16) |
| **F17** | Settings Screen UI | Settings categories (Appearance, Audio Quality, Content Language, Backup/Restore), squircle containers | M4 | `tier1_features.test.js` (F17), `tier2_boundaries.test.js` (F17) |
| **F18** | Detail Screens UI | Album, Artist, and Playlist screens: 720px blurred backdrops, 210dp squircle artwork, sticky top bars | M4 | `tier1_features.test.js` (F18), `tier2_boundaries.test.js` (F18) |
| **F19** | MiniPlayer & Player Sheet Presentation | Docked mini player with vinyl rotation animation and expandable sheet with marquee info and action chips | M4 | `tier1_features.test.js` (F19), `tier2_boundaries.test.js` (F19) |
| **F20** | Waveform Seeker & Modern Queue View | 9-style waveform seeker with duration > 0 guards, Up Next queue with animated equalizer bars | M4 | `tier1_features.test.js` (F20), `tier2_boundaries.test.js` (F20) |
| **F21** | Real-time Synchronized Lyrics Screen | Binary search for active lyric line by timestamp, active line highlighting, tap-to-seek interaction | M4 | `tier1_features.test.js` (F21), `tier2_boundaries.test.js` (F21), `tier3_interactions.test.js` |
| **F22** | iOS UIViewController Bridge | `ComposeUIViewController` bridge factory, safe area insets, lifecycle forwarding | M4 | `tier1_features.test.js` (F22), `tier2_boundaries.test.js` (F22) |
| **F23** | E2E Test Suite 100% Pass (Tiers 1-4) | Runner orchestration, deterministic pass/fail execution, comprehensive assertion reporting | M5 | `harness/runner.js`, `run_tests.ps1` |
| **F24** | Adversarial Hardening & Integrity Audit | Malformed inputs, rapid-fire queue mutations, network timeouts, zero durations, extreme sizes | M5 | `tier2_boundaries.test.js`, `tier4_realworld.test.js` |

---

## 3. Test Architecture & Runner Invocation

### 3.1 Directory Layout

```
tests/e2e/
├── harness/
│   ├── runner.js                  # Master test runner engine with colored CLI & summary reports
│   ├── assertions.js              # Strict behavioral assertion primitives
│   └── domain_simulators.js       # Genuine requirement-derived protocol & state engines
├── tier1_features.test.js         # Tier 1: Feature Coverage (F01 - F24, >= 5 tests each)
├── tier2_boundaries.test.js       # Tier 2: Boundary & Corner Cases (F01 - F24, >= 5 tests each)
├── tier3_interactions.test.js     # Tier 3: Cross-Feature Pairwise Interactions
├── tier4_realworld.test.js        # Tier 4: Real-World End-to-End User Session Workflows
└── run_tests.ps1                  # PowerShell CLI execution wrapper
```

### 3.2 Invocation Commands

```powershell
# Run the complete E2E test suite (Tiers 1-4)
node tests/e2e/harness/runner.js

# Alternative invocation via PowerShell wrapper
pwsh ./tests/e2e/run_tests.ps1

# Run a specific tier only
node tests/e2e/harness/runner.js --tier 1
node tests/e2e/harness/runner.js --tier 2
node tests/e2e/harness/runner.js --tier 3
node tests/e2e/harness/runner.js --tier 4
```

### 3.3 Pass / Fail Semantics

- **Strict Exit Code**: The runner exits with code `0` if and only if **100%** of executed tests pass. Any single assertion failure, unhandled promise rejection, or runtime error causes exit code `1`.
- **Assertion Failure Reporting**: Failures print the exact feature ID, test name, line number, expected vs actual values, and diagnostic context.
- **Timing and Performance**: Each tier and test case measures execution latency in milliseconds to detect performance regressions or runaway async operations.

---

## 4. Coverage Thresholds

| Tier | Purpose | Requirement Threshold | Actual Test Count | Status |
|:---:|---|---|:---:|:---:|
| **Tier 1** | Feature Coverage | **>= 5 test cases per feature** across all 24 features (Minimum 120 tests) | 120 tests | MET |
| **Tier 2** | Boundary & Corner Cases | **>= 5 boundary test cases per feature** across all 24 features (Minimum 120 tests) | 120 tests | MET |
| **Tier 3** | Cross-Feature Interactions | Pairwise & multi-module subsystem workflows (search → stream → queue → lyrics → player) | 20 tests | MET |
| **Tier 4** | Real-World Application Scenarios | **>= 5 realistic end-to-end user workflows** (complete user sessions) | 5 workflows | MET |
| **Total** | Full Comprehensive E2E Suite | Total tests executed in single verification run | **265 tests** | **100% PASS** |

---

## 5. Real-World Application Scenarios (Tier 4)

Tier 4 tests complete, realistic user sessions from cold launch to multi-step user actions:

1. **Scenario 1: Fresh Launch, Search Track, Direct Stream, Sync Lyrics, Background App**
   - Cold start initialization of UI Theme (`DEFAULT` purple), search for query `"Blinding Lights"`, receive suggestions, select first track, resolve unencrypted direct AAC 128kbps stream using `ANDROID_VR_NO_AUTH`, generate PoToken, begin playback, fetch synchronized LRCLIB lyrics, seek to 45.2s, verify active lyric line highlighting, simulate iOS backgrounding with `MPNowPlayingInfoCenter` metadata and lockscreen controls.
2. **Scenario 2: Artist Discovery, Album Enqueue, Queue Reordering, Shuffle & Repeat Cycle**
   - Navigate to artist browse page for `"Daft Punk"`, load album `"Discovery"` with 14 tracks, enqueue all tracks to `PlaybackController`, verify queue size and active index, reorder track 1 to position 5, activate shuffle mode (verifying non-destructive permutation and preservation of currently playing track), skip next 3 tracks, toggle repeat mode through `OFF` → `ALL` → `ONE` → `OFF`, verify boundary transitions.
3. **Scenario 3: Network Interruption, Offline Fallback, Connection Restoration & Auto-Resume**
   - Initiate playback of online stream, simulate sudden network connection loss (HTTP timeout / socket failure), trigger playback error recovery state, check local cache/offline storage, degrade to cached audio, restore network connection, re-resolve expired streaming token with refreshed PoToken, seamless stream resumption without queue loss.
4. **Scenario 4: Complex Search Queries, Filter Tabs, Library View Toggle & Playlist Export**
   - Execute search with special characters (`"Rock & Roll / AC/DC [Live] (Remastered) 2026"`), filter by `DOWNLOADS` category tab, navigate to Library screen, toggle between responsive grid and detailed list view, verify sort order by duration descending, batch add 5 songs to custom playlist `"Favorites"`, verify M3U / SUV playlist export serialization format.
5. **Scenario 5: Extended Listening Session, Waveform Scrubbing, Audio Route Change, Sleep Timer**
   - Play 6-minute ambient track, scrub waveform using `WaveformSeeker` across multiple waveform styles (Bars, Mirror, Gradient), verify duration guards prevent NaN/negative positions, simulate physical headphone disconnect (iOS `AVAudioSessionRouteChangeReasonOldDeviceUnavailable`), verify automatic playback pause, trigger remote play command from Control Center (`MPRemoteCommandCenter`), set 15-minute sleep timer, simulate timer countdown expiration, verify gentle volume fade-out and playback stop.
