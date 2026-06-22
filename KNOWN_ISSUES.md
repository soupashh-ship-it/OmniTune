# Known Issues (Phase 0)

1. **Error Recovery Missing**: `MusicService` fails to retry streams automatically if a 403/404 error occurs. It currently reacts too simply or freezes.
2. **Search Fragility**: The search logic heavily depends on primary filters. If one category fails to load from the InnerTube API, it can blank out the entire result screen. 
3. **Download State Incomplete**: Downloads lack a robust state machine. Files are downloaded but not cleanly integrated into offline-first playback without remote resolution.
4. **Library Fakes**: Several Library UI sections ("Downloads", "Artists", "Albums") display placeholder text or fake counts without fully implemented screens.
5. **Coupled Architecture**: Composables across the app invoke `MusicService.instance` directly instead of passing intentions through ViewModels, hindering testability and creating lifecycle risks.
6. **Queue Persistence Limits**: Queue and current metadata do not perfectly survive aggressive app death/restarts or rotation consistently without edge cases.

7. **Lint Errors**: lintDebug fails with 31 errors. Example: `WrongConstant` in `MusicSessionCallback.kt` using `SessionResult.RESULT_ERROR_BAD_VALUE` instead of `SessionError.ERROR_BAD_VALUE`.
