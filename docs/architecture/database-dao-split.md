# Database DAO Split

## Why DatabaseDao was split
The `DatabaseDao.kt` originally spanned over 1600 lines, operating as a god object that handled every single SQL query, insert, and update for the entire application. It became unmaintainable and highly susceptible to merge conflicts and regression bugs.

## Current DAO Files
The persistence layer is now decomposed into focused domain DAOs:
* **QueueDao**: Queue save/restore and clear. Must not alter song entities.
* **LyricsDao**: Lyrics cache get/upsert.
* **SearchHistoryDao**: Search history insert/query/clear.
* **EventDao**: Listen events and history.
* **FormatDao**: Stream/format cache persistence.
* **SongDao**: Song lookups, inserts, liked states, and play count updates.
* **ArtistDao**: Artist queries and library state.
* **AlbumDao**: Album queries and library state.
* **PlaylistDao**: Playlist relations, order, and tag mapping.

### Required Runtime Checks
After changing *any* DAO, verify:
* Database schema builds successfully.
* Playback continues to work (especially `SongDao` and `FormatDao`).
* Library screens load (Albums/Artists/Playlists).
* App reopen state remains sane.

## Remaining DatabaseDao Compatibility Facade
The `DatabaseDao.kt` interface still exists but has been drastically reduced to ~293 lines.
* **Why it remains**: It acts as a compatibility facade for complex, cross-DAO transactions (e.g., `insert(AlbumPage)` which needs to write to Albums, Artists, Songs, and relation maps simultaneously).
* **Why it should not grow**: It represents legacy tight-coupling. New complex transactions should be orchestrated at the `Repository` level rather than defaulting to the DAO facade.
* **Future Retirement**: Phase 3B or later may introduce a dedicated database repository layer that injects specific DAOs and orchestrates them, allowing `DatabaseDao.kt` to be fully deleted.

## Hard Rules
* ❌ Pure DAO splits **must not bump schema version**.
* ❌ Schema version changes require explicit `Migration` steps and detailed reporting.
* ❌ DAO files must contain *persistence access only*, not application business policy.
* ✅ The repository or service layer should own business decisions.