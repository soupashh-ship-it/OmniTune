# Goal 7 — Credential inventory and release policy

No credential values were read or printed during this audit.

| Location/category | Classification | Current disposition |
| --- | --- | --- |
| `YOUTUBE_MUSIC_API_KEY` BuildConfig input | Restricted client API key | Still injected for provider requests; it must remain restricted to the Android package/signing certificate and is not treated as a server secret. |
| `TOGETHER_BEARER_TOKEN` BuildConfig input | Privileged bearer token | Removed with the unshipped Together feature; no BuildConfig field or app code remains. |
| `LASTFM_SECRET` BuildConfig input | Privileged API signing secret | Removed with the client-side Last.fm feature and module. |
| `LASTFM_API_KEY` BuildConfig input | Client API identifier paired with retired signing flow | Removed with Last.fm. |
| ListenBrainz token in DataStore | User-provided token | Keystore-encrypted, encrypted legacy values are migrated at startup, removable in Settings, omitted from backups, and never logged by the submission client. |
| YouTube cookie / PO tokens | User credentials/session material | Keystore-encrypted migration already runs in `OmniTuneApp`; excluded from backups. |
| Release keystore password/alias/file | Signing credential | CI/environment-only signing inputs; never emitted to BuildConfig. |
| GitHub workflow token | CI token | Workflow-scoped only; no application packaging path. |

## Existing exposure response

Any release built while the Together bearer or Last.fm signing secret was present may have
exposed those credentials. The owning service administrator must rotate/revoke them and review
distribution history. This repository cannot truthfully claim external rotation has occurred.

The final release artifact check must confirm that no Together/Last.fm BuildConfig fields,
Bearer literals, signing-password text, or plaintext user tokens are recoverable. APK scans are
recorded as identifier/pattern findings only, never with extracted values.

## Local release-artifact inspection

On 2026-07-28, local `:app:assembleRelease` produced the unsigned disposable artifact
`app-release-unsigned.apk` (22,717,126 bytes; SHA-256
`E751779379E6EA8A79399BC8CBAF1771F49F7500D5AC134D6D7B53C424A416DD`). The artifact is unsigned
because local release-signing environment inputs were intentionally unavailable; CI remains the
signed-release authority.

An identifier-only scan of both DEX files found zero exact matches for `TOGETHER_BEARER_TOKEN`,
the Together/Last.fm/Kizzy package names, `LASTFM_API_KEY`, `LASTFM_SECRET`, signing-password
identifiers, and plaintext ListenBrainz-token patterns. One `Bearer` pattern match is the
diagnostic redaction regular expression, not a credential. One `discordRPC` match is the
one-time preference-migration key, not an active RPC client. No values were extracted or logged.
