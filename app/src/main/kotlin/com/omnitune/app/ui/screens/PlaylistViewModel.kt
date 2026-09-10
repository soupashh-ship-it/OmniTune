package com.omnitune.app.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.models.*
import com.omnitune.app.playback.DownloadUtil
import com.omnitune.app.ui.navigation.Destination
import com.omnitune.innertube.YouTube
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

data class PlaylistUiState(
    val playlist: Playlist? = null,
    val originalSongs: List<Song> = emptyList(),
    val sortType: SortType = SortType.CUSTOM,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val totalSongCount: Int? = null,
    val error: String? = null,
    val isEditable: Boolean = false,
    val isRenaming: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val isSaved: Boolean = false,
    val selectedSongIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val database: MusicDatabase,
    private val downloadUtil: DownloadUtil,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playlistId: String = savedStateHandle.get<String>(Destination.Playlist.ARG_PLAYLIST_ID)
        ?: savedStateHandle.get<String>("playlistId")
        ?: ""

    private val initialName: String? = savedStateHandle.get<String>(Destination.Playlist.ARG_NAME)
    private val initialThumbnail: String? = savedStateHandle.get<String>(Destination.Playlist.ARG_THUMBNAIL)

    private val _uiState = MutableStateFlow(
        PlaylistUiState(
            playlist = if (playlistId.isNotBlank()) Playlist(id = playlistId, title = initialName ?: "", author = "You", thumbnailUrl = initialThumbnail) else null,
            isLoading = true
        )
    )
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        if (playlistId.isNotBlank()) {
            loadPlaylist()
            checkLibraryStatus()
        }
    }

    private fun checkLibraryStatus() {
        viewModelScope.launch {
            try {
                database.playlist(playlistId).collect { dbPlaylist: com.omnitune.app.db.entities.Playlist? ->
                    _uiState.update { it.copy(isSaved = dbPlaylist?.playlist?.bookmarkedAt != null) }
                }
            } catch (e: Exception) {
                logFailure(e, "Failed to observe playlist library state")
            }
        }
    }

    fun toggleSaveToLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = _uiState.value.playlist ?: return@launch
            try {
                val existing = database.getPlaylistByIdBlocking(playlist.id)?.playlist
                if (existing != null) {
                    if (_uiState.value.isSaved) {
                        database.update(existing.copy(bookmarkedAt = null))
                    } else {
                        database.update(existing.copy(bookmarkedAt = java.time.LocalDateTime.now()))
                    }
                }
            } catch (e: Exception) {
                publishFailure(e, "Failed to update playlist library state")
            }
        }
    }

    fun loadPlaylist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                when (playlistId) {
                    "LM" -> {
                        val liked = withContext(Dispatchers.IO) {
                            database.likedSongs(com.omnitune.app.constants.SongSortType.CREATE_DATE, descending = true).first().map { it.toPresentationSong() }
                        }
                        val playlist = Playlist(
                            id = "LM",
                            title = "Liked Songs",
                            author = "You",
                            thumbnailUrl = liked.firstOrNull()?.thumbnailUrl,
                            totalSongCount = liked.size,
                            songs = liked
                        )
                        _uiState.update { it.copy(playlist = playlist, originalSongs = liked, isLoading = false, isEditable = false) }
                    }
                    "DEVICE_SONGS" -> {
                        val localSongs = withContext(Dispatchers.IO) {
                            database.songsByRowIdAsc().first().map { it.toPresentationSong() }.filter { it.source == SongSource.LOCAL }
                        }
                        val playlist = Playlist(
                            id = "DEVICE_SONGS",
                            title = "Device Files",
                            author = "Local",
                            thumbnailUrl = localSongs.firstOrNull()?.thumbnailUrl,
                            totalSongCount = localSongs.size,
                            songs = localSongs
                        )
                        _uiState.update { it.copy(playlist = playlist, originalSongs = localSongs, isLoading = false, isEditable = false) }
                    }
                    "TOP_50" -> {
                        val allSongs = withContext(Dispatchers.IO) {
                            database.songsByRowIdAsc().first().map { it.toPresentationSong() }.take(50)
                        }
                        val playlist = Playlist(
                            id = "TOP_50",
                            title = "My Top 50",
                            author = "You",
                            thumbnailUrl = allSongs.firstOrNull()?.thumbnailUrl,
                            totalSongCount = allSongs.size,
                            songs = allSongs
                        )
                        _uiState.update { it.copy(playlist = playlist, originalSongs = allSongs, isLoading = false, isEditable = false) }
                    }
                    "CACHED_ALL" -> {
                        val allSongs = withContext(Dispatchers.IO) {
                            database.songsByRowIdAsc().first().map { it.toPresentationSong() }
                        }
                        val playlist = Playlist(
                            id = "CACHED_ALL",
                            title = "Cached Songs",
                            author = "Device",
                            thumbnailUrl = allSongs.firstOrNull()?.thumbnailUrl,
                            totalSongCount = allSongs.size,
                            songs = allSongs
                        )
                        _uiState.update { it.copy(playlist = playlist, originalSongs = allSongs, isLoading = false, isEditable = false) }
                    }
                    else -> {
                        // Check local Room DB first
                        val localDbPlaylist = withContext(Dispatchers.IO) {
                            database.playlist(playlistId).first()
                        }
                        val localSongs = withContext(Dispatchers.IO) {
                            database.playlistSongs(playlistId).first()
                        }

                        if (localDbPlaylist != null) {
                            val presentationSongs = localSongs.map { it.song.toPresentationSong() }
                            val playlist = Playlist(
                                id = localDbPlaylist.id,
                                title = localDbPlaylist.playlist.name,
                                author = "You",
                                thumbnailUrl = localDbPlaylist.thumbnails.firstOrNull() ?: presentationSongs.firstOrNull()?.thumbnailUrl,
                                totalSongCount = presentationSongs.size,
                                songs = presentationSongs
                            )
                            _uiState.update {
                                it.copy(
                                    playlist = playlist,
                                    originalSongs = presentationSongs,
                                    isLoading = false,
                                    isEditable = true
                                )
                            }
                        } else {
                            // Fetch from YouTube online
                            val ytResult = withContext(Dispatchers.IO) {
                                YouTube.playlist(playlistId)
                            }
                            ytResult.onSuccess { playlistPage ->
                                val presentationSongs = playlistPage.songs.map { it.toPresentationSong() }
                                val playlist = Playlist(
                                    id = playlistId,
                                    title = playlistPage.playlist.title,
                                    author = playlistPage.playlist.author?.name ?: "",
                                    thumbnailUrl = playlistPage.playlist.thumbnail,
                                    totalSongCount = presentationSongs.size,
                                    songs = presentationSongs
                                )
                                _uiState.update {
                                    it.copy(
                                        playlist = playlist,
                                        originalSongs = presentationSongs,
                                        isLoading = false,
                                        isEditable = false
                                    )
                                }
                            }.onFailure { error ->
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = error.message ?: "Failed to load playlist"
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error") }
            }
        }
    }
    fun refreshPlaylist() {

        loadPlaylist()
    }

    fun downloadPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlist.songs.forEach { downloadUtil.enqueue(it.id, it.title) }
        }
    }

    fun toggleSongSelection(song: Song) {
        val current = _uiState.value.selectedSongIds.toMutableSet()
        if (current.contains(song.id)) current.remove(song.id) else current.add(song.id)
        _uiState.update { it.copy(selectedSongIds = current, isSelectionMode = current.isNotEmpty()) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedSongIds = emptySet(), isSelectionMode = false) }
    }

    fun setSortType(sortType: SortType) {
        _uiState.update { it.copy(sortType = sortType) }
        applySort()
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _uiState.update { it.copy(sortOrder = sortOrder) }
        applySort()
    }

    private fun applySort() {
        val orig = _uiState.value.originalSongs
        val type = _uiState.value.sortType
        val order = _uiState.value.sortOrder

        val sorted = when (type) {
            SortType.TITLE -> orig.sortedBy { it.title.lowercase() }
            SortType.ARTIST -> orig.sortedBy { it.artist.lowercase() }
            SortType.ALBUM -> orig.sortedBy { it.album.lowercase() }
            SortType.DATE_ADDED, SortType.CUSTOM -> orig
        }

        val finalSongs = if (order == SortOrder.DESCENDING) sorted.reversed() else sorted
        _uiState.update {
            it.copy(playlist = it.playlist?.copy(songs = finalSongs))
        }
    }

    fun renamePlaylist(newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = database.getPlaylistByIdBlocking(playlistId)?.playlist
                if (existing != null) {
                    database.update(existing.copy(name = newName))
                } else {
                    error("Playlist not found")
                }
                _uiState.update {
                    it.copy(
                        playlist = it.playlist?.copy(title = newName),
                        successMessage = "Renamed to \"$newName\""
                    )
                }
            } catch (e: Exception) {
                publishFailure(e, "Failed to rename playlist")
            }
        }
    }

    fun deletePlaylist() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (database.playlistIdsByIdOrBrowseId(playlistId).isEmpty()) {
                    error("Playlist not found")
                }
                database.deletePlaylistById(playlistId)
                _uiState.update { it.copy(deleteSuccess = true) }
            } catch (e: Exception) {
                publishFailure(e, "Failed to delete playlist")
            }
        }
    }


    fun removeSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (database.checkInPlaylist(playlistId, song.id) == 0) {
                    error("Song was not in this playlist")
                }
                database.removeSongFromPlaylist(playlistId, song.id)
                val current = _uiState.value.playlist?.songs.orEmpty()
                val updated = current.filter { it.id != song.id }
                _uiState.update {
                    it.copy(
                        playlist = it.playlist?.copy(songs = updated),
                        originalSongs = updated,
                        successMessage = "Removed ${song.title}"
                    )
                }
            } catch (e: Exception) {
                publishFailure(e, "Failed to remove song from playlist")
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    private fun publishFailure(error: Exception, message: String) {
        logFailure(error, message)
        _uiState.update { it.copy(errorMessage = message) }
    }

    private fun logFailure(error: Exception, message: String) {
        if (error is CancellationException) throw error
        Timber.w(error, message)
    }
}
