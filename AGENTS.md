# Agent Instructions

You are an AI coding agent working on **OmniTune**, an open-source Android music player.

## Mandatory First Step
**Read `GEMINI.md`** completely before any work. It contains project constraints, agent workflow rules, error handling protocol, verification commands, and response format.

## Core Rules
- Smallest safe patch — one concern per edit
- Inspect, git status, and diagnose before touching code
- Never rename package, remove GPL, or copy Velune branding
- Preserve all playback/queue/download/notification behavior
- Verify with `testDebugUnitTest`, `lintDebug`, `assembleDebug`

## Response Format
Every completion: what was done, files changed, verification results, known limitations, next step.
