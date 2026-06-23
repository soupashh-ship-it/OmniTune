# Known Issues (Phase 2)

1. **Search Fragility**: The search logic heavily depends on primary filters. If one category fails to load from the InnerTube API, it can blank out the entire result screen. 
2. **Download State Incomplete**: Downloads lack a robust state machine. Files are downloaded but not cleanly integrated into offline-first playback without remote resolution.
3. **Library Fakes**: Several Library UI sections ("Artists", "Albums") display placeholder text or fake counts without fully implemented screens.
4. **Coupled Architecture**: Composables across the app invoke `MusicService.instance` directly instead of passing intentions through ViewModels, hindering testability and creating lifecycle risks.
5. **Queue Persistence Limits**: Queue and current metadata do not perfectly survive aggressive app death/restarts or rotation consistently without edge cases.
