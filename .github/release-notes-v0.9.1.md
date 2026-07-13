## OmniTune v0.9.1 — Discord Rich Presence Integration

### New Feature: Discord Rich Presence
- Full Discord Rich Presence integration showing your currently playing song on your Discord profile
- **DiscordSettingsScreen** with OmniGlass design: account connection, activity customization (name/type/status), image selection (large/small), action buttons, update interval
- **DiscordLoginScreen** — WebView-based token extraction with thread-safe state handling
- **Live connection status indicator** — Green/yellow dot showing real-time RPC connection state
- Auto-start on login, start/stop with playback lifecycle
- Configurable activity details with template variables: `{song}`, `{artist}`, `{album}`
- Persistent reconnect with exponential backoff (up to 5 consecutive failures)
- Proper video-ID-based button URLs to share what you're listening to

### Fixes & Improvements
- `resolveButtonUrl` now uses actual YouTube video IDs instead of display names
- Hilt cyclic inheritance error resolved (removed `javax.inject` annotations from kizzy JVM module)
- `HttpClient` (Ktor) binding added to NetworkModule for proper DI
- Discord settings logout + enable toggle now live-starts/stops the RPC via `restartDiscordPresence()`
- Thread-safety fix for Discord login screen (Handler.getMainLooper for Compose state writes)

### Technical
- New `kizzy` module: gateway entities, WebSocket client, KizzyRPC, KizzyRepository, ApiService
- New `KizzyModule` DI providing kizzy dependencies via Hilt
- `DiscordPresenceManager` — lifecycle-aware singleton with auto-reconnect
- `DiscordImageResolver`, `DiscordIntervalUtils` utilities
- Wired into `MusicService`, `PlayerConnection`, `OmniNavGraph`
- Bump version to 0.9.1 (versionCode 38)
