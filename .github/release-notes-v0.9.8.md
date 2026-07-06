# OmniTune v0.9.8

Massive architectural update bringing 14 new screens and menus from Velune into OmniTune.

### Major Features
* **Playlist Variants:** Added `CachePlaylistScreen` (for viewing offline/downloaded songs), `TopPlaylistScreen` (for charts), and `LibraryMixScreen` (for personalized auto-generated mixes).
* **Playlist Suggestions:** Added `PlaylistSuggestionSection` to suggest similar songs at the bottom of playlists.
* **Context Menus Everywhere:** Replaced the generic track options sheet with 10 specific context menus! We now have dedicated menus for: `AlbumMenu`, `ArtistMenu`, `PlaylistMenu`, `YouTubeSongMenu`, `YouTubeAlbumMenu`, `YouTubeArtistMenu`, `YouTubePlaylistMenu`, and `LyricsMenu`.
* **Selection/Bulk Menus:** Added `SelectionSongsMenu` to handle bulk actions across multiple selected songs outside of the queue.

### Engineering
* Spawned 4 parallel background agents to resolve over 1,100 compilation errors to seamlessly merge Velune's component architecture with OmniTune's `OmniShell` framework.