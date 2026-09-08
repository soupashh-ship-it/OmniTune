package com.omnitune.app.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.models.Album
import com.omnitune.app.models.Song
import com.omnitune.app.models.toPresentationSong
import com.omnitune.app.models.toMediaItem
import com.omnitune.app.playback.DownloadUtil
import com.omnitune.app.ui.navigation.Destination
import com.omnitune.innertube.YouTube
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AlbumUiState(
    val album: Album? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val selectedSongIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false
)

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val database: MusicDatabase,
    private val downloadUtil: DownloadUtil,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val albumId: String = savedStateHandle.get<String>(Destination.Album.ARG_ALBUM_ID)
        ?: savedStateHandle.get<String>("albumId")
        ?: ""

    private val initialName: String? = savedStateHandle.get<String>(Destination.Album.ARG_NAME)
    private val initialThumbnail: String? = savedStateHandle.get<String>(Destination.Album.ARG_THUMBNAIL)

    val selectedSongId: String? = savedStateHandle.get<String>(Destination.Album.ARG_SELECTED_SONG_ID)
        ?: savedStateHandle.get<String>("selectedSongId")

    private val _batchProgress = MutableStateFlow<Pair<Int, Int>>(Pair(0, 0))
    val batchProgress: StateFlow<Pair<Int, Int>> = _batchProgress.asStateFlow()

    private var playerConnection: com.omnitune.app.playback.PlayerConnection? = null

    private val _uiState = MutableStateFlow(
        AlbumUiState(
            album = if (albumId.isNotBlank()) Album(id = albumId, title = initialName ?: "", artist = "", thumbnailUrl = initialThumbnail) else null,
            isLoading = true
        )
    )
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    init {
        if (albumId.isNotBlank()) {
            loadAlbum()
            checkLibraryStatus()
        }
    }

    private fun checkLibraryStatus() {
        viewModelScope.launch {
            try {
                database.album(albumId).collect { dbAlbum ->
                    _uiState.update { it.copy(isSaved = dbAlbum?.album?.bookmarkedAt != null) }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun toggleSaveToLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            val album = _uiState.value.album ?: return@launch
            try {
                val dbAlbum = database.album(album.id).first()
                if (dbAlbum != null) {
                    database.update(dbAlbum.album.toggleLike())
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun loadAlbum() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Try online first
                val ytResult = withContext(Dispatchers.IO) {
                    YouTube.album(albumId)
                }

                ytResult.onSuccess { albumPage ->
                    val presentationSongs = albumPage.songs.map { it.toPresentationSong() }
                    val presentationAlbum = Album(
                        id = albumId,
                        title = albumPage.album.title,
                        artist = albumPage.album.artists?.joinToString(", ") { it.name } ?: "",
                        year = albumPage.album.year?.toString(),
                        thumbnailUrl = albumPage.album.thumbnail,
                        songs = presentationSongs
                    )
                    _uiState.update {
                        it.copy(album = presentationAlbum, isLoading = false, error = null)
                    }
                }.onFailure {
                    // Fallback to local DB
                    val local = withContext(Dispatchers.IO) {
                        database.albumWithSongs(albumId).first()
                    }
                    if (local != null) {
                        val presentationSongs = local.songs.map { it.toPresentationSong() }
                        val presentationAlbum = Album(
                            id = local.album.id,
                            title = local.album.title,
                            artist = local.artists.joinToString(", ") { a -> a.name },
                            year = local.album.year?.toString(),
                            thumbnailUrl = local.album.thumbnailUrl,
                            songs = presentationSongs
                        )
                        _uiState.update { it.copy(album = presentationAlbum, isLoading = false, error = null) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = it.error ?: "Failed to load album") }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error loading album") }
            }
        }
    }

    fun retry() {
        loadAlbum()
    }

    fun downloadAlbum(album: Album) {
        viewModelScope.launch(Dispatchers.IO) {
            val total = album.songs.size
            if (total == 0) return@launch
            _batchProgress.value = Pair(0, total)
            var current = 0
            album.songs.forEach { song ->
                downloadUtil.enqueue(song.id, song.title)
                current++
                _batchProgress.value = Pair(current, total)
            }
        }
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            downloadUtil.enqueue(song.id, song.title)
        }
    }

    fun setPlayerConnection(connection: com.omnitune.app.playback.PlayerConnection?) {
        this.playerConnection = connection
    }

    fun playNext(songs: List<Song>, connection: com.omnitune.app.playback.PlayerConnection? = null) {
        val conn = connection ?: playerConnection
        conn?.playNext(songs.map { it.toMediaItem() })
    }

    fun addToQueue(songs: List<Song>, connection: com.omnitune.app.playback.PlayerConnection? = null) {
        val conn = connection ?: playerConnection
        conn?.addToQueue(songs.map { it.toMediaItem() })
    }

    fun playNextSelectedSongs(connection: com.omnitune.app.playback.PlayerConnection? = null) {
        val conn = connection ?: playerConnection
        val selectedIds = _uiState.value.selectedSongIds
        val currentAlbum = _uiState.value.album ?: return
        val selectedSongs = currentAlbum.songs.filter { it.id in selectedIds }
        if (selectedSongs.isNotEmpty()) {
            conn?.playNext(selectedSongs.map { it.toMediaItem() })
            clearSelection()
        }
    }

    fun addToQueueSelectedSongs(connection: com.omnitune.app.playback.PlayerConnection? = null) {
        val conn = connection ?: playerConnection
        val selectedIds = _uiState.value.selectedSongIds
        val currentAlbum = _uiState.value.album ?: return
        val selectedSongs = currentAlbum.songs.filter { it.id in selectedIds }
        if (selectedSongs.isNotEmpty()) {
            conn?.addToQueue(selectedSongs.map { it.toMediaItem() })
            clearSelection()
        }
    }

    fun reorderSong(fromIndex: Int, toIndex: Int) {
        val currentAlbum = _uiState.value.album ?: return
        val songs = currentAlbum.songs.toMutableList()
        if (fromIndex !in songs.indices || toIndex !in songs.indices) return

        val movedSong = songs.removeAt(fromIndex)
        songs.add(toIndex, movedSong)

        _uiState.update {
            it.copy(album = currentAlbum.copy(songs = songs))
        }
    }

    fun toggleSongSelection(song: Song) {
        val current = _uiState.value.selectedSongIds.toMutableSet()
        if (current.contains(song.id)) current.remove(song.id) else current.add(song.id)
        _uiState.update {
            it.copy(
                selectedSongIds = current,
                isSelectionMode = current.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedSongIds = emptySet(), isSelectionMode = false) }
    }
}
