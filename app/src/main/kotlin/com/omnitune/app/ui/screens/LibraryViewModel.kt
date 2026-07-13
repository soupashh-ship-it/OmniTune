/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.EventWithSong
import com.omnitune.app.db.entities.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val likedCount: Int = 0,
    val likedSongs: List<Song> = emptyList(),
    val recentlyPlayed: List<EventWithSong> = emptyList(),
    val librarySongCount: Int = 0,
    val libraryArtistCount: Int = 0,
    val libraryAlbumCount: Int = 0,
    val playlistCount: Int = 0,
    val downloadCount: Int = 0,
    val isLoading: Boolean = true,
)

private data class LibraryCounts(
    val songs: Int,
    val artists: Int,
    val albums: Int,
    val playlists: Int,
)

/** Default sort type for liked songs */
private val LIKED_SONGS_SORT = com.omnitune.app.constants.SongSortType.CREATE_DATE

@androidx.media3.common.util.UnstableApi
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val database: MusicDatabase,
    private val downloadUtil: com.omnitune.app.playback.DownloadUtil,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val likedSongs: StateFlow<List<Song>>
    val libraryArtists: StateFlow<List<com.omnitune.app.db.entities.Artist>>
    val libraryAlbums: StateFlow<List<com.omnitune.app.db.entities.Album>>
    val playlists: StateFlow<List<com.omnitune.app.db.entities.Playlist>>
    val editablePlaylists: kotlinx.coroutines.flow.Flow<List<com.omnitune.app.db.entities.Playlist>> = database.editablePlaylistsByCreateDateAsc()

    fun song(songId: String): kotlinx.coroutines.flow.Flow<com.omnitune.app.db.entities.Song?> = database.song(songId)

    // Saved-artists and saved-albums flows (bookmarked only)
    val savedArtists: StateFlow<List<com.omnitune.app.db.entities.Artist>>
    val savedAlbums: StateFlow<List<com.omnitune.app.db.entities.Album>>

    private val downloadListener = object : androidx.media3.exoplayer.offline.DownloadManager.Listener {
        override fun onDownloadChanged(manager: androidx.media3.exoplayer.offline.DownloadManager, download: androidx.media3.exoplayer.offline.Download, finalException: Exception?) {
            refreshDownloadCount()
        }
        override fun onDownloadRemoved(manager: androidx.media3.exoplayer.offline.DownloadManager, download: androidx.media3.exoplayer.offline.Download) {
            refreshDownloadCount()
        }
    }

    private fun refreshDownloadCount() {
        var count = 0
        val cursor = downloadUtil.downloadManager.downloadIndex.getDownloads(androidx.media3.exoplayer.offline.Download.STATE_COMPLETED)
        try {
            while (cursor.moveToNext()) {
                count++
            }
        } finally {
            cursor.close()
        }
        _uiState.value = _uiState.value.copy(downloadCount = count)
    }

    init {
        downloadUtil.downloadManager.addListener(downloadListener)
        refreshDownloadCount()
        // Initialize flows
        likedSongs = database.likedSongs(LIKED_SONGS_SORT, true)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            
        libraryArtists = database.artistsByNameAsc()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            
        libraryAlbums = database.albumsByNameAsc()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            
        playlists = database.playlists(com.omnitune.app.constants.PlaylistSortType.CREATE_DATE, false)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        savedArtists = database.artistsBookmarkedByNameAsc()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        savedAlbums = database.albumsLikedByNameAsc()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        viewModelScope.launch {
            val libraryCounts = combine(
                database.songsByRowIdAsc(),
                libraryArtists,
                libraryAlbums,
                playlists,
            ) { songs, artists, albums, playlists ->
                LibraryCounts(
                    songs = songs.size,
                    artists = artists.size,
                    albums = albums.size,
                    playlists = playlists.size,
                )
            }

            combine(
                likedSongs,
                database.recentEvents(),
                libraryCounts,
            ) { likedList, events, counts ->
                LibraryUiState(
                    likedCount = likedList.size,
                    likedSongs = likedList,
                    recentlyPlayed = events,
                    librarySongCount = counts.songs,
                    libraryArtistCount = counts.artists,
                    libraryAlbumCount = counts.albums,
                    playlistCount = counts.playlists,
                    downloadCount = _uiState.value.downloadCount,
                    isLoading = false,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        downloadUtil.downloadManager.removeListener(downloadListener)
    }

    fun toggleLike(songId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val song = database.getSongById(songId)
            if (song != null) {
                database.upsert(song.song.localToggleLike())
                try {
                    com.omnitune.innertube.YouTube.likeVideo(songId, song.song.likedDate == null)
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "Failed to sync like with YouTube")
                }
            }
        }
    }

    fun toggleLibrary(songId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val song = database.getSongById(songId)
            if (song != null) {
                database.upsert(song.song.toggleLibrary())
            }
        }
    }

    suspend fun addToPlaylist(playlist: com.omnitune.app.db.entities.Playlist, songId: String): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val isDuplicate = database.checkInPlaylist(playlist.playlist.id, songId) > 0
            if (!isDuplicate) {
                database.addSongToPlaylist(playlist, listOf(songId))
            }
            !isDuplicate
        }
    }

    fun createPlaylist(
        name: String,
        initialSongId: String? = null,
        onCreated: (String) -> Unit = {},
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val playlist = com.omnitune.app.db.entities.PlaylistEntity(
                name = name,
                bookmarkedAt = java.time.LocalDateTime.now(),
            )
            database.insert(playlist)
            if (initialSongId != null) {
                val savedPlaylist = database.getPlaylistByIdBlocking(playlist.id)
                if (savedPlaylist != null) {
                    database.addSongToPlaylist(savedPlaylist, listOf(initialSongId))
                }
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onCreated(playlist.id)
            }
        }
    }

    fun ensureSongExists(metadata: com.omnitune.app.models.MediaMetadata) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val song = database.getSongById(metadata.id)
            if (song == null) {
                database.upsert(metadata.toSongEntity())
            }
        }
    }

    // ── Playlist folders / tag management ───────────────────────────

    val allTags: kotlinx.coroutines.flow.Flow<List<com.omnitune.app.db.entities.TagEntity>> = database.allTags()

    fun playlistTags(playlistId: String): kotlinx.coroutines.flow.Flow<List<com.omnitune.app.db.entities.TagEntity>> =
        database.playlistTags(playlistId)

    fun createTag(name: String, color: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val tag = com.omnitune.app.db.entities.TagEntity(name = name, color = color)
            database.insert(tag)
        }
    }

    fun updateTag(tag: com.omnitune.app.db.entities.TagEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            database.update(tag)
        }
    }

    fun deleteTag(tag: com.omnitune.app.db.entities.TagEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            database.removeAllTagPlaylists(tag.id)
            database.delete(tag)
        }
    }

    fun assignPlaylistTag(playlistId: String, tagId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            database.addTagToPlaylist(playlistId, tagId)
        }
    }

    fun removePlaylistTag(playlistId: String, tagId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            database.removePlaylistTag(playlistId, tagId)
        }
    }
}
