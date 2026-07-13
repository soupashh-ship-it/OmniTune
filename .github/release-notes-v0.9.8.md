# OmniTune v0.9.8
**Playlist Variants, Context Menus, Bluetooth Auto-Play & More**

### 🚀 Features
* **Playlist Variants:** Added `CachePlaylistScreen` (offline/downloaded songs), `TopPlaylistScreen` (charts), and `LibraryMixScreen` (personalized auto-generated mixes)
* **Playlist Suggestions:** Suggested similar songs at the bottom of playlists
* **10 Context Menus:** Replaced generic track options with dedicated menus for albums, artists, playlists, YouTube songs/albums/artists/playlists, lyrics, and bulk selection
* **Bluetooth Auto-Play:** Auto-resume playback when a Bluetooth device connects (configurable in settings)
* **Persistent Queue Toggle:** Option to disable persistent queue restoration on app start
* **Permanent Shuffle:** Keep shuffle mode across queue changes (configurable setting)
* **Auto-Download on Like:** Automatically download liked songs (configurable setting)
* **Pause on Device Mute:** Pause playback when device volume reaches zero or is muted
* **Skip Silence & Audio Offload:** Respect user preferences for silent-skipping and audio offloading from player init
* **Tags in Playlists:** View, assign, and remove tags on cached and top playlists, with "Add Tag" chip for quick assignment

### 🛠️ Fixes
* Remove release signing enforcement to unblock development builds
* Updated `TagChip` composable to accept data via parameters instead of requiring direct database access
* Fixed compilation errors in `TopPlaylistScreen` and `LibraryMixScreen`
* Fixed queue crash due to duplicate item keys (QueueScreen crash when same song queued multiple times)
* Fixed artist screen crash from negative padding value
* Fixed notification small icon showing blank square (switched from ic_launcher_foreground to ic_stat_omnitune)
* Added album art to notification large icon

### 📦 Build
* Version: **0.9.8** (code 45)
