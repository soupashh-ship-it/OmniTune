# Goals 6–10 completion audit

## Delivered source changes

- **Backup:** truthful library-only copy, v2 logical-format contract, diagnostic Room-schema
  metadata with legacy alias, manifest preflight, safety archive/rollback, selective Merge, and
  complete Replace only.
- **Credentials:** Together bearer and Last.fm signing flow removed from BuildConfig, source,
  Gradle, and release dependency graph. ListenBrainz is a user-token feature with Keystore
  encryption/migration/removal and redacted requests.
- **Discovery/Search:** Search uses generation gating and cancellation propagation; Home loads
  provider endpoints independently, deduplicates sections, surfaces partial failures, uses
  timestamped persisted safe shelf metadata, and labels restart-cache sections as cached.
- **Settings:** direct preference-backed controls have a behavior registry and source-level CI
  guard. Signed-out YouTube Music Sync clears its enabled state and cannot be re-enabled until
  sign-in. Retired Together/Discord/Last.fm preferences receive a one-time scoped cleanup.
- **Dead systems:** Together, Discord RPC, Kizzy, ForYouSuggestionEngine, and the client-side
  Last.fm module were removed. The ordinary community Discord link remains.

## Local evidence

| Check | Result |
| --- | --- |
| `:app:compileDebugKotlin` | Passed |
| `:app:compileDebugUnitTestKotlin` | Passed; unit-test source compiles |
| `:app:assembleRelease` | Passed before the final Home cache-only change; unsigned local artifact inspected as documented in Goal 7 |
| Formatting | `git diff --check` passed |
| USB/device tests | Deliberately not run at the user’s request |

No source claim in this audit substitutes for physical-device playback, provider, or restore
validation. Those remain deferred while USB/device testing is unavailable.
