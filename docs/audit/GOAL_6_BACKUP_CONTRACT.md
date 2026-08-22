# Goal 6 — Backup contract

## Scope

Manual OmniTune backups include library songs and metadata, artists, albums, liked state,
playlists and ordering, tags, listening history, and listening statistics. A full ZIP can also
contain app-managed offline audio plus its Media3 download index. JSON backups never contain
offline files.

They exclude YouTube cookies, PO tokens, Last.fm/ListenBrainz credentials, signing material,
privileged API credentials, device-specific paths, temporary caches, crash reports, and the
active queue. Non-secret app preferences are not currently exported. The settings copy names
these exclusions explicitly and does not claim to back up preferences.

## Versions and integrity

- `formatVersion` is the **logical backup format** and is the import compatibility gate. The
  app writes v2 and accepts v1/v2.
- `roomSchemaVersion` is diagnostic metadata only. It records the actual Room schema used to
  create a backup and never causes a destructive Room migration. The older
  `databaseSchemaVersion` spelling remains accepted for legacy imports.
- Full ZIP archives contain `manifest.json` with a manifest version, per-entry byte counts,
  SHA-256 digests, and the `library.json` digest. Missing, duplicate, changed, unlisted, unsafe,
  or oversized entries fail preflight before Replace can clear any data.
- The manifest is a corruption/integrity checksum, **not** a signature or proof of archive
  authenticity.

## Restore modes

- **Merge** supports selecting library/likes, playlists, and/or history/statistics. Playlist and
  history selections automatically include their required song references, with no unrelated
  library rows. Offline media is deliberately not merged because the Media3 index is a whole
  database.
- **Replace** restores the complete validated library only. It creates and re-reads a verified
  private safety archive before replacing records; a partial category selection is rejected.
  Full archives stage Media3 data and audio before promotion, with rollback/recovery handling.

## Compatibility matrix

| Input | Expected behavior |
| --- | --- |
| Current v2 JSON/ZIP | Preflight validates counts/relations; ZIP manifest is required and verified. |
| v1 logical backup | Imports if structurally valid; warning explains lack of ZIP manifest/summary as applicable. |
| Legacy `databaseSchemaVersion` | Accepted as Room metadata with a warning; logical format remains the gate. |
| Missing optional fields | Serialization defaults provide safe empty/default values. |
| Newer logical format | Rejected before any database or offline-media change. |
| Missing/corrupt manifest or hash | Full v2 archive is rejected before Replace. |
| Bad queue reference | Rejected; queue is not restored in this version. |
| Merge full archive | Library merge only; existing Media3 files/index remain untouched. |
| Replace full archive | Safety backup, validated database restore, then verified Media3/audio promotion. |

The source and fixture tests cover these rules. Physical-device restore remains deliberately
unverified while USB/device testing is paused.
