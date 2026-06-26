# OmniTune — Gemini Agent Context

## Project Identity
- OmniTune: open-source Android music player
- Jetpack Compose + Material 3 (OmniGlass UI)
- Current release: v0.7.3 (versionCode 29)
- Package: `com.omnitune.app`, minSdk 26, targetSdk 36
- License: GPL-3.0
- Repository root: `O:\code\omnitune`

## Modules
`:app`, `:innertube`, `:kugou`, `:lrclib`, `:lastfm`, `:simpmusic`, `:betterlyrics`, `:kizzy`, `:canvas`

## Non-Negotiable Constraints
- **Do NOT rename** package name or `applicationId` (`com.omnitune.app`)
- **Do NOT remove** GPL license, CREDITS, or attribution files
- **Do NOT copy** Velune branding, identity, icon, exact UI, or proprietary assets
- Velune may only be used as a **reliability/behavior benchmark**
- **Preserve** working playback, search, queue, downloads, notification, background playback, and restore behavior
- **Avoid broad rewrites** unless a small patch is impossible

## Required Agent Workflow
1. **Inspect first** with tools (`Read`, `Glob`, `Grep`, filesystem) before editing
2. **Check `git status`** before any edit to know the working tree state
3. **Use `rg` / `git grep`** for code search over manual browsing
4. **Apply smallest safe patch** — one concern per edit
5. **Verify after every change** with the verification commands below

## Error Handling
When a command fails:
1. **Quote the exact error output** in the response
2. **Diagnose root cause** (read logs, check file content, inspect dependencies)
3. **Apply fix and retry** — try up to 3 distinct approaches before escalating

## Verification Commands
```
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

## Runtime Verification Matrix
| Area | Check |
|------|-------|
| Playback | Start, pause, resume, skip, seek |
| Miniplayer | Shows current track, controls work |
| Queue | Add, reorder, remove, clear |
| Downloads | Initiate, cancel, manage |
| Background | Audio continues on home/power off |
| Notification | Shows artwork/title/controls, dismiss clears |
| Restore | App resumes after process kill |

## Final Response Format
Each completion must include:
1. What was implemented
2. Files changed
3. Build/lint/test results
4. Verification steps taken
5. Any known limitations
6. Next-step recommendation
