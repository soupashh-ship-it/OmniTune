# Gemini CLI — OmniTune Setup

## Quick Start
```
cd O:\code\omnitune
gemini
```

## Useful Commands

| Command | Purpose |
|---------|---------|
| `/memory reload` | Reload GEMINI.md + AGENTS.md into context |
| `/memory show` | Display active project context |
| `/tools desc` | List available tools |
| `/mcp list` | List configured MCP servers |
| `/model manage` | Switch Gemini model |

## Workflow
1. One issue → one branch
2. Inspect repo state with tools
3. Apply smallest safe patch
4. Verify: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --stacktrace`
5. Report results in the 7-section format

## settings.json Compatibility
If the installed Gemini CLI rejects `.gemini/settings.json`:
1. Run the command that failed and copy the exact error
2. Update this file with the error details below

**Error record (leave blank if accepted):**
_No error — schema accepted on creation._

## MCP Configuration
See `docs/agent-setup/gemini-mcp-setup.md` for MCP server setup instructions.
