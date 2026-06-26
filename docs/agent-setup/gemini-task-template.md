# OmniTune — Task Execution Template

Use this template for every Gemini CLI task session.

---

## 1. Pre-Flight
- [ ] `gemini` started from `O:\code\omnitune`
- [ ] `/memory reload` loaded GEMINI.md + AGENTS.md
- [ ] `git status` — branch, dirty/clean state known
- [ ] Existing test/build state established

## 2. Inspect
- [ ] Read relevant source files with `Read` tool
- [ ] Search code with `rg` / `git grep`
- [ ] Understand the module structure (`settings.gradle.kts`)
- [ ] Check `.gitignore` for any relevant patterns

## 3. Edit
- [ ] Smallest safe patch — one concern per commit
- [ ] Verify constraints against GEMINI.md rules
- [ ] Do not rename package, remove GPL, copy Velune assets

## 4. Verify
- [ ] `.\gradlew.bat testDebugUnitTest --stacktrace`
- [ ] `.\gradlew.bat lintDebug --stacktrace`
- [ ] `.\gradlew.bat assembleDebug --stacktrace`
- [ ] If test fails: quote error, diagnose, fix, retry (max 3 attempts)

## 5. Report
```
## Summary
[1-2 line overview]

## Files Changed
- path/to/file (reason)

## Build Results
- testDebugUnitTest: [PASS/FAIL]
- lintDebug: [PASS/FAIL]
- assembleDebug: [PASS/FAIL]

## Verification
[Steps taken to verify correctness]

## Known Issues
[Any limitations, warnings, or deferred concerns]

## Next Step
[Suggested follow-up action]
```
