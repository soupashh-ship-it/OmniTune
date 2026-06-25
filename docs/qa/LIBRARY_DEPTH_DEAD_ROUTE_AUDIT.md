# Phase 27 — Library Depth + Dead Route Audit

**Date:** 2026-06-25
**Auditor:** Mimo (Codebuff execution agent)
**Repo:** O:\code\omnitune
**Branch:** main (HEAD: 9b12ca1, 1 commit ahead of origin/main)
**Base version:** v0.7.0

---

## 1. Summary

Phase 27 is an audit-only phase. No code was changed. This audit inspects OmniTune's Library area for missing depth, dead routes, placeholder behavior, stale counts, and release-risk UI paths before 1.0.

**Phase 28 update:** BUG-1 (dead playlist route) and BUG-2 (no PlaylistDetailScreen) have been fixed. See Phase 28 report.

**Overall finding:** The Library is well-structured and mostly data-backed with real Room queries. One significant dead route exists: **Playlist detail navigation is a Toast placeholder** ("Playlist details coming soon"). All other Library destinations are functional with real data, proper empty states, and working playback.

---

## 2. Files Inspected

### Source Files
- `app/src/main/kotlin/com/omnitune/app/ui/screens/LibraryScreen.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/screens/LibraryViewModel.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/screens/LibraryAlbumsScreen.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/screens/LibraryArtistsScreen.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/screens/LibraryPlaylistsScreen.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/screens/LikedSongsScreen.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/screens/RecentlyPlayedScreen.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/screens/DownloadsScreen.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/screens/DownloadsViewModel.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/screens/HistoryScreen.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/screens/StatsScreen.kt`
- `app/src/main/kotlin/com/omnitune/app/ui/component/Library.kt`
- `app/src/main/kotlin/com/omnitune/app/MainActivity.kt`
- `app/src/main/kotlin/com/omnitune/app/db/DatabaseDao.kt`
- `app/src/main/kotlin/com/omnitune/app/playback/DownloadUtil.kt`
- `app/src/main/kotlin/com/omnitune/app/db/entities/Song.kt`
- `app/src/main/kotlin/com/omnitune/app/db/entities/Event.kt`
- `app/src/main/kotlin/com/omnitune/app/db/entities/Playlist.kt`
- `app/src/main/kotlin/com/omnitune/app/db/entities/Album.kt`
- `app/src/main/kotlin/com/omnitune/app/db/entities/Artist.kt`

### Documentation Files
- `README.md`
- `release_notes.md`
- `KNOWN_ISSUES.md`
- `RELEASE_CLAIM_VERIFICATION.md`
- `docs/release/PRE_1_0_ROADMAP.md`

---

## 3. Library Structure

The Library screen is organized into two sections:

### Hero Section
- Displays total count (liked + recently played + library songs + playlists)
- Shows contextual empty/populated message

### "Your shelves" — 2x2 Card Grid
| Card | Destination | Route | Navigation |
|------|-------------|-------|------------|
| Liked Songs | LikedSongsScreen | `liked_songs` | Real screen, playback wired |
| Downloads | DownloadsScreen | `ROUTE_DOWNLOADS` | Real screen, offline playback wired |
| Recently Played | RecentlyPlayedScreen | `recently_played` | Real screen, playback wired |
| Search | SearchScreen | `Screens.Search.route` | Real screen, existing search |

### "Browse library" — Route Rows
| Row | Destination | Route | Navigation |
|-----|-------------|-------|------------|
| Artists | LibraryArtistsScreen | `library_artists` | Real screen → ArtistScreen |
| Albums | LibraryAlbumsScreen | `library_albums` | Real screen → AlbumScreen |
| Playlists | LibraryPlaylistsScreen | `library_playlists` | Real list screen → **TOAST PLACEHOLDER** |

---

## 4. Data Backing

| Section | Data Source | Count Source | Refresh | Stale Risk |
|---------|------------|-------------|---------|------------|
| Liked Songs | Room DB `likedSongs()` flow | `likedList.size` via combine | Reactive Flow | Low |
| Downloads | DownloadManager `getDownloads(STATE_COMPLETED)` | Cursor count | DownloadManager.Listener callback | Low |
| Recently Played | Room DB `events()` flow | `events.size` via combine | Reactive Flow | Low |
| Artists | Room DB `artistsByNameAsc()` flow | Via LibraryArtistsScreen | Reactive Flow | Low |
| Albums | Room DB `albumsByNameAsc()` flow | Via LibraryAlbumsScreen | Reactive Flow | Low |
| Playlists | Room DB `playlists()` flow | `list.size` collected separately | Reactive Flow | Low |
| Library Total | Sum of liked + events + library songs + playlists | Computed in combine | Reactive | Low |

**Stale count assessment:** Low risk. All counts are backed by Room Flows that reactively update. Download count uses DownloadManager.Listener to refresh on change/removal. No hardcoded counts found.

---

## 5. Route/Tap Behavior

| Tap Target | Tap Action | Result | Honest |
|------------|-----------|--------|--------|
| Liked Songs card | Navigate to `liked_songs` | LikedSongsScreen opens with real Room data | Yes |
| Downloads card | Navigate to `ROUTE_DOWNLOADS` | DownloadsScreen opens with real DownloadManager data | Yes |
| Recently Played card | Navigate to `recently_played` | RecentlyPlayedScreen opens with real Room event data | Yes |
| Search card | Navigate to `Screens.Search.route` | SearchScreen opens | Yes |
| Artists row | Navigate to `library_artists` | LibraryArtistsScreen opens with real Room artists | Yes |
| Albums row | Navigate to `library_albums` | LibraryAlbumsScreen opens with real Room albums | Yes |
| Playlists row | Navigate to `library_playlists` | LibraryPlaylistsScreen opens with real Room playlists | Yes (list only) |
| Playlist item tap | `onNavigateToPlaylist` | **Toast: "Playlist details coming soon"** | **NO — DEAD** |
| Artist item tap | Navigate to `artist/{id}` | ArtistScreen opens with YouTube artist data | Yes |
| Album item tap | Navigate to `album/{id}` | AlbumScreen opens with YouTube album data | Yes |
| Song tap (Liked) | `onPlaySong(song)` | Playback via playerConnection.playQueue() | Yes |
| Song tap (Recent) | `onPlaySong(event.song)` | Playback via playerConnection.playQueue() | Yes |
| Song tap (Download) | `viewModel.playDownload()` | Offline playback if isPlayable, else rejected | Yes |

**Broken taps:** 1 — Playlist item tap in LibraryPlaylistsScreen
**Placeholder taps:** 1 — Playlist item tap shows Toast only
**Crash risk:** Low. No null-pointer or crash-prone patterns found in Library taps.

---

## 6. Downloads/Offline Library Behavior

- **Completed downloads visible:** Yes, DownloadsScreen shows them with green accent and "Ready" label
- **Active downloads counted:** Yes, counted as "Active" in DownloadsScreen header (downloading, queued, stopped, removing)
- **Failed downloads counted:** Yes, counted as "Failed" in DownloadsScreen header
- **Completed-only count:** Library card shows completed count via `downloadCount` from ViewModel
- **Offline-ready state accurate:** DownloadUtil.isPlayable() gates on STATE_COMPLETED + non-empty cachedSpans
- **Completed row playback from Library:** Yes, tapping completed download triggers playDownload()
- **Offline playback from Library:** Yes, when download is completed and cache is present
- **Download state listener:** DownloadManager.Listener refreshes count on download change/remove

---

## 7. Favorites/History/Playlists/Albums/Artists Status

| Feature | Exists | Real Data | Navigation | Empty State | Playback |
|---------|--------|-----------|------------|-------------|----------|
| Favorites/Liked Songs | Yes | Room DB liked songs | → LikedSongsScreen | LibraryEmptyState component | Yes, via onPlaySong |
| History/Recently Played | Yes | Room DB events | → RecentlyPlayedScreen | LibraryEmptyState component | Yes, via onPlaySong |
| Playlists (list) | Yes | Room DB playlists | → LibraryPlaylistsScreen | LibraryEmptyState component | N/A (list only) |
| Playlists (detail) | **NO** | N/A | **Toast placeholder** | N/A | N/A |
| Albums (list) | Yes | Room DB albums | → LibraryAlbumsScreen | LibraryEmptyState component | N/A (list only) |
| Albums (detail) | Yes | YouTube API | → AlbumScreen | Error/loading states | Yes, via onPlaySong |
| Artists (list) | Yes | Room DB artists | → LibraryArtistsScreen | LibraryEmptyState component | N/A (list only) |
| Artists (detail) | Yes | YouTube API | → ArtistScreen | Error/loading states | Yes, via onPlaySong |
