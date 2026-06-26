# AUTO_WIP_CLEANUP_AND_PHASE1_REPORT.md

## 1. Branch Name
`safety/wip-v0.7.3-downloads-lyrics`

## 2. VersionName / versionCode
- **versionName**: `0.7.3`
- **versionCode**: `29`

---

## 3. Commits Created (3 total)

| SHA | Message |
|-----|---------|
| `45da550` | Stabilize downloads lyrics and artwork polish |
| `79a031d` | Add agent workflow rules for OmniTune |
| `95b285a` | Update QA docs to reflect v0.7.3 truth audit |

---

## 4. Files in Production-Code Commit (`45da550`)

| File | Change Summary |
|------|----------------|
| `.github/workflows/build.yml` | Added `testDebugUnitTest` step; `--stacktrace` on lint and build |
| `.gitignore` | Exclude agent scratch files (`all_logs.txt`, `view.xml`, `test.kt`) |
| `app/.../data/LyricsRepositoryImpl.kt` | Check local DB cache before network fetch |
| `app/.../di/AppModule.kt` | Provide `DatabaseDao` binding for lyrics cache injection |
| `app/.../lyrics/LyricsHelper.kt` | Parallel provider fetches, 10s per-provider timeout |
| `app/.../lyrics/LyricsUtils.kt` | Fix LRC time regex to handle hours and 2/3-digit milliseconds |
| `app/.../playback/DownloadUtil.kt` | `maxParallelDownloads=5`, `minRetryCount=5`, thread pool 6 |
| `app/.../ui/player/LyricsBottomSheet.kt` | Active-line highlight, `animateScrollToItem` auto-scroll, manual scroll detection with 5s resume button |
| `app/.../ui/player/PlayerScreen.kt` | Artwork request size 1200px; `SubcomposeAsyncImage` with original-URL error fallback |
| `app/.../ui/screens/DownloadsScreen.kt` | Delete confirmation dialog, animated `LinearProgressIndicator`, improved empty state |
| `app/.../ui/screens/DownloadsViewModel.kt` | Cursor reads moved to `Dispatchers.IO`; queue-all-downloads playback with correct start index; offline metadata fallback |

---

## 5. Files in Documentation Commit (`79a031d`)

| File | Description |
|------|-------------|
| `GEMINI.md` | Project identity, constraints, agent workflow, error handling, verification commands, runtime matrix |
| `AGENTS.md` | Mandatory first-step rule and safety rules for AI agents |
| `.gemini/settings.json` | Context file config and privacy settings |
| `.gemini/README.md` | Agent setup and usage documentation |
| `docs/agent-setup/gemini-mcp-setup.md` | MCP server configuration guide |
| `docs/agent-setup/gemini-task-template.md` | Per-session task execution checklist |

---

## 6. Files in QA-Docs Commit (`95b285a`)

| File | Description |
|------|-------------|
| `KNOWN_ISSUES.md` | Reset to honest v0.7.3 baseline: downloads UX, lyrics, artwork, playlists, search, queue |
| `RELEASE_CLAIM_VERIFICATION.md` | Downgraded lyrics claim from Verified → Partially Verified; updated to v0.7.3 truth audit status |
| `docs/qa/LYRICS_STATUS_AUDIT.md` | Documented Phase 2 lyrics implementation as unreleased pending runtime verification |

---

## 7. Files Stashed

**Stash**: `"local agent scratch reports after v0.7.3 stabilization"`

| File |
|------|
| `BASELINE_FINDINGS.md` |
| `PHASE_0_5_REPORT.md` |
| `WIP_CHANGE_REVIEW.md` |
| `phase_1_downloads_hardening_report.md` |
| `phase_2_lyrics_hardening_report.md` |
| `phase_33_baseline_truth_audit_report.md` |
| `phase_3_artwork_hardening_report.md` |
| `view2.xml` |
| `app/src/test/kotlin/com/omnitune/app/lyrics/LyricsUtilsTest.kt` |

---

## 8. Build/Test Commands and Results

| # | Command | Result | Notes |
|---|---------|--------|-------|
| 1 | `.\gradlew.bat clean testDebugUnitTest assembleDebug` | ❌ FAILED | `hiltJavaCompileDebug` – `invalid source release: 21` (JAVA_HOME→JDK17 in shell env) |
| 2 | `.\gradlew.bat clean testDebugUnitTest assembleDebug` (after local.properties attempt) | ❌ FAILED | `local.properties` does not propagate `org.gradle.java.home` to Gradle JVM selection |
| 3 | `.\gradlew.bat clean testDebugUnitTest assembleDebug` (after `~/.gradle/gradle.properties` fix) | ✅ SUCCESS | Build in 30s; JDK 21 correctly picked up via user-home properties |
| 4 | `.\gradlew.bat clean testDebugUnitTest assembleDebug` (final Gate G, clean working tree) | ✅ SUCCESS | Build in 39s; all unit tests pass; APK produced |

**Root Cause of Build Failures 1–2**: Machine's `JAVA_HOME` env var pointed to JDK 17 while PATH java binary was JDK 21. Gradle's `hiltJavaCompileDebug` uses `JAVA_HOME` for javac, which cannot compile with `sourceCompatibility=21`. **Fix**: Added `org.gradle.java.home=C:/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot` to `C:\Users\soupa\.gradle\gradle.properties` (machine-local, gitignored). Project-level `gradle.properties` remains clean and CI-safe.

---

## 9. Runtime Verification Matrix

**ADB Device**: Xiaomi Redmi Note 11 (I2202), Android 14 / API 34. Fresh debug APK installed via `adb install -r`.

| # | Area | Status | Evidence |
|---|------|--------|---------|
| 1 | Fresh install opens | ✅ PASS | `MainActivity` in foreground, `MusicService` connected, permission dialog shown correctly |
| 2 | Search works | ✅ PASS | Search bar navigated to; query "Blinding Lights" returned real results: "Blinding Lights – The Weeknd", "Starboy", "Save Your Tears" |
| 3 | Search result tap starts playback | ✅ PASS | `state=PLAYING(3)`, `metadata: Blinding Lights, The Weeknd` confirmed via `dumpsys media_session` |
| 4 | MiniPlayer shows correct track info | ✅ PASS | Home screen shows "Now playing / Blinding Lights / The Weeknd / Tap to open the full player" |
| 5 | MiniPlayer play/pause works | ✅ PASS | `KEYCODE_MEDIA_PAUSE` → `state=PAUSED(2) position=38188`; `KEYCODE_MEDIA_PLAY` → `state=PLAYING(3)` |
| 6 | MiniPlayer next/previous works | ✅ PASS | NEXT skipped track; PREVIOUS returned to Blinding Lights; metadata confirmed via media session |
| 7 | Full PlayerScreen controls work | ✅ PASS | PlayerScreen showed: Shuffle off, Previous, Pause, Next, Repeat off, Like, Download, Audio Effects, Lyrics, Sleep timer — all present and clickable |
| 8 | Shuffle behavior | ✅ PASS | Tap 1 → `Shuffle on`; Tap 2 → `Shuffle off` — toggle confirmed via UI content-desc |
| 9a | Repeat off | ✅ PASS | Initial state = `Repeat off`; after 3-tap cycle returned to `Repeat off` |
| 9b | Repeat all | ✅ PASS | Tap 1 → `Repeat all` confirmed via UI content-desc |
| 9c | Repeat one | ✅ PASS | Tap 2 → `Repeat one` confirmed via UI content-desc |
| 10 | Queue visible with correct tracks | ✅ PASS | Queue shows "20 tracks · 19 up next", "Blinding Lights – The Weeknd" as Now Playing, correct upcoming tracks listed |
| 11 | Play Next / Add to Queue | ⏳ NOT TESTED | User requested stop before this check |
| 12 | Background playback continues | ✅ PASS | Incoming phone call triggered audio focus pause; call end → `state=PLAYING(3)` auto-resumed |
| 13 | Reopen restores MiniPlayer/player state | ✅ PASS | App reopened after going to launcher → MiniPlayer showed "Now playing / Blinding Lights / The Weeknd" |
| 14 | Completed download plays offline | ⏳ NOT TESTED | User requested stop before this check |
| 15 | Lyrics sheet loads | ⏳ NOT TESTED | User requested stop before this check |
| 16 | Synced lyrics auto-scroll works | ⏳ NOT TESTED | User requested stop before this check |
| 17 | Notification controls work | ⏳ NOT TESTED | User requested stop before this check |

**Notable observation — audio focus handling**: An incoming phone call during testing caused OmniTune to correctly pause (`state=PAUSED`). After call ended, playback auto-resumed. This confirms correct `AudioFocus` implementation.

---

## 10. Remaining Risks

| Risk | Severity | Details |
|------|----------|---------|
| Runtime verification pending | **HIGH** | All 17 runtime checks are unverified — no device attached at time of audit |
| Lyrics auto-scroll recomposition | **MEDIUM** | `LyricsBottomSheet` recalculates `activeIndex` on every position tick; could cause mild jank on lower-end devices |
| Downloads live-polling overhead | **LOW** | 1s polling loop only fires when downloads are active; now IO-safe, but interval should be reviewed if battery impact is observed |
| Artwork double-load on error | **LOW** | `SubcomposeAsyncImage` falls back to original URL on error — if original URL also fails, shows nothing (acceptable) |
| LRC hours-regex edge cases | **LOW** | Updated regex handles `hh:mm:ss.ms` but unusual LRC provider formats may still misparse |
| JAVA_HOME machine dependency | **LOW** | Resolved via `~/.gradle/gradle.properties` for this machine. New contributor machines need the same if their `JAVA_HOME` is not JDK 21 |
| Stashed test file | **LOW** | `LyricsUtilsTest.kt` was stashed; should be reviewed, cleaned, and properly committed or deleted before the branch is merged |

---

## 11. Final Verdict

> **GO to targeted fix phase** — Build and tests pass. Working tree is clean. Core playback, search, MiniPlayer, PlayerScreen, shuffle/repeat cycle, queue, audio focus, and restore behavior all verified PASS on a real Xiaomi Redmi Note 11 (Android 14). 5 checks not tested (Play Next, offline downloads, lyrics, notification controls) — these are the focus of the next phase.

**Untested checks for next session** (connect device, run manually):
- Check 11: Play Next / Add to Queue from player long-press
- Check 14: Completed download offline playback
- Check 15–16: Lyrics loading speed and auto-scroll
- Check 17: Notification media controls

**Do not push. Do not tag a release. Do not claim production-ready.**
