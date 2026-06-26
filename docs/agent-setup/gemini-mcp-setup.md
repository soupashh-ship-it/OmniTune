# Gemini CLI — MCP Server Setup for OmniTune

## Recommended MCP Servers

### GitHub MCP
Enables repo inspection, release management, issue tracking, and PR operations.

**Required env:** `GITHUB_MCP_PAT` — a GitHub Personal Access Token with `repo` scope.

```json
{
  "mcpServers": {
    "github": {
      "url": "https://api.github.com/mcp",
      "headers": {
        "Authorization": "Bearer ${GITHUB_MCP_PAT}"
      }
    }
  }
}
```

### Context7 MCP
Provides up-to-date documentation for dependencies (Android SDK, Compose, libraries).

**Required env:** `CONTEXT7_API_KEY`

```json
{
  "mcpServers": {
    "context7": {
      "url": "https://mcp.context7.xyz/sse",
      "headers": {
        "Authorization": "Bearer ${CONTEXT7_API_KEY}"
      }
    }
  }
}
```

## Security Rules
- **Never** commit API keys, tokens, or secrets to the repository
- Use environment variables (`$ENV_VAR`) for all credentials
- **Do not** install MCP servers with file write, shell execution, or agent persistence capabilities

## Verification
After configuring an MCP server, run `/mcp list` in Gemini CLI to confirm it is connected.
