# Agent Refactor Rules

Strict rules for Gemini, Codex, and future AI agents working on OmniTune.

## General Rules
* **Inspect before editing**: Do not make assumptions about the architecture. Run `git status`, verify branches, and read existing implementation before changing files.
* **One phase at a time**: Focus strictly on the current instruction.
* **One responsibility per extraction**: Each extracted class or DAO must have a singular, distinct purpose.
* **Continuous Verification**: Build, test, and lint after *every* risky change. Do not wait until the end of the session.
* **Runtime Verification**: Use ADB to test device runtime after playback or database changes.
* **Stop on failure**: If the build fails and the fix is not trivial, stop and report the error.
* **Honest reporting**: Never mark a test or manual check as `PASS` unless actually verified in the current session.
* **Always create a report**: End your task with a Markdown report of the changes and outcomes.

## Forbidden Actions
* ❌ Broad cleanup or unrelated formatting.
* ❌ Package renaming or application ID changes.
* ❌ App identity, signing, license, GPL, or credits changes.
* ❌ Destructive database migrations (dropping tables or columns without explicit request).
* ❌ Schema version bumps without corresponding migration logic.
* ❌ Modifying playback behavior during a UI or DB phase.
* ❌ Modifying DAO behavior during a UI phase.
* ❌ UI redesigns during a refactoring phase.

## Required Final Response Format
When finishing a phase or task, agents MUST respond with exactly this format:

```text
Branch:
Starting commit:
Ending commit:
Files changed:
Build status:
Test status:
Lint status:
Runtime status:
Known failures:
Known not-run checks:
Recommendation:
```