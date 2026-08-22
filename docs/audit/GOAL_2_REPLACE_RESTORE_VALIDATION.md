# Goal 2 — Replace Restore Safety Validation

## Implemented transaction

```text
Select archive
  → read ZIP or JSON
  → stage ZIP media + verify manifest/hash/path/limits
  → validate snapshot and generate preview
  → user chooses Merge or confirms Replace
  → for Replace: create + re-read retained safety ZIP
  → Room merge/replace transaction + database count verification
  → persist post-commit media handoff marker
  → reversible offline-media promotion + file hash verification
  → commit media transaction, retain safety ZIP, show result

Failure after Room commit
  → roll back media promotion
  → restore the in-memory safety snapshot in a Room transaction
  → retain safety ZIP and show the failed phase
```

## Safety contract

- Replace never reaches `clearLibraryForReplace()` unless a full, app-owned safety ZIP has been created and independently re-read.
- Safety archives are named with UTC timestamp and backup format version in `filesDir/restore_safety_backups`.
- They contain only the existing backup model plus managed offline media and Media3 index. Preferences, credentials, cookies, API keys, and session data are excluded.
- Safety archives are intentionally retained after successful verification and can be recovered from Backup & Restore with an additional confirmation. Recovery itself first safeguards the currently active library.
- A failed import reports its failed phase and shows **Retry safely**. Retry reopens the selected archive and repeats preflight, safety backup, staging, and verification from the beginning; it never resumes a partial restore.
- If the process dies after the verified Room commit but before media promotion completes, the staged Replace payload is marked ready and is completed at next application start. A failed in-process restore clears that marker before rolling the database back.

## Preflight and preview

The preview displays songs, likes, playlists and entries, artists, albums, history, statistics, tags, offline-file count/size, warnings, unsupported settings/queue items, and whether the archive is full ZIP or JSON.

Offline audio is deliberately **Replace-only**. A Media3 download index is a whole database and cannot be merged without risking current-profile orphan files. Merge previews incoming offline media as not restored and leaves the current download directory/index unchanged.

Replace rejects empty archives, unsupported formats, inconsistent declared counts, duplicate IDs/relations, invalid relation references/order, invalid queue references, bad timestamps/statistics, oversized collections, unsafe ZIP paths, unsupported ZIP entries, missing or mismatched manifest entries, bad hashes, excess media, and offline files without a Media3 index. Legacy v1 JSON remains readable with a warning; a v1 ZIP has a prominent integrity warning because it has no file manifest.

## Automated source-level evidence

`app/src/test/kotlin/com/omnitune/app/backup/OmniBackupPreflightTest.kt`

- valid Merge and Replace preview
- empty valid-format backup
- unsupported version
- missing relationship
- duplicate IDs and invalid playlist ordering
- missing manifest-declared file and corrupt library hash
- invalid queue reference
- oversized archive collection
- legacy v1 backup handling

`app/src/test/kotlin/com/omnitune/app/backup/RestoreSafetyBackupStoreTest.kt`

- atomic retained safety archive
- forced safety archive write failure leaves no partial file
- rollback ordering primitive

`app/src/test/kotlin/com/omnitune/app/backup/OfflineDownloadArchiveTransactionTest.kt`

- reversible Replace promotion for offline audio and Media3 index
- forced Media3 destination failure restores the already-promoted downloads
- a committed ready-stage is completed safely on the next app start

`app/src/test/kotlin/com/omnitune/app/backup/RestoreTransactionBoundaryTest.kt`

- forced database-transaction failure is phase-labelled and cannot continue to media promotion

`app/src/androidTest/kotlin/com/omnitune/app/backup/OmniBackupRepositoryInstrumentedTest.kt`

- disposable in-memory Room Merge preserves current records and adds incoming ones
- disposable in-memory Room Replace removes old restorable records and produces a verified safety archive
- source compiles; execution is deferred because USB/device testing was explicitly disabled

## Verification status

- `:app:compileDebugKotlin` — passed.
- `:app:testDebugUnitTest` — passed.
- `:app:compileDebugAndroidTestKotlin` — passed.
- USB/emulator destructive testing — **INTENTIONALLY NOT RUN**. The user asked to leave testing because USB-connected testing was not available. No user/device data was modified for this goal.

Remaining disposable-profile checks are process interruption between media staging and promotion and a complete recovery run. These remain explicitly unverified rather than being reported as passed.
