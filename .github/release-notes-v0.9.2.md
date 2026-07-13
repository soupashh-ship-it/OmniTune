## OmniTune v0.9.2 — Settings Gaps, Saved Artists & Albums, Playlist Folders

### New Features
- **Saved Artists & Albums** — Bookmark (heart) toggle on artist and album detail screens. Library Artists and Library Albums now have **All / Saved** filter chips to quickly find your bookmarked content
- **Playlist Folders** — Create, rename, and delete folders (via tags). Assign playlists to folders from the playlist row's menu. Filter playlists by folder using chips in the Library Playlists screen

### Settings Improvements
- **Pure Black mode** — Re-added as a real toggle in Appearance Settings. Enables true black backgrounds for OLED screens (mini-player and navigation bar)
- **Last.fm Scrobbling** — Fixed duplicate login logic (extracted shared helper), cleaned up unused imports
- **Library counts** — Fixed race condition where artist, album, and playlist counts briefly showed 0 on screen load

### Fixes & Cleanup
- Library count race condition resolved (combine now reads from hot StateFlows instead of stale values)
- Removed unused imports from ScrobblingSettings

### Technical
- Bump version to 0.9.2 (versionCode 39)
- ArtistDetailViewModel / AlbumDetailViewModel — new HiltViewModels for bookmark toggling
- LibraryViewModel — tag/folder management methods, saved-artist/saved-album flows
- LibraryPlaylistsScreen — folder chips, Manage Folders dialog, Folder Picker dialog
- GlassBottomDock — pureBlack parameter wired end-to-end
