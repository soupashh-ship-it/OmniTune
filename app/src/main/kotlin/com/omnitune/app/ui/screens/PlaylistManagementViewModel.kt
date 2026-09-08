package com.omnitune.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.PlaylistEntity
import com.omnitune.app.db.entities.PlaylistSongMap
import com.omnitune.app.models.PlaylistDisplayItem
import com.omnitune.app.models.Song
import com.omnitune.app.models.toMediaMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

data class PlaylistManagementUiState(
    val userPlaylists: List<PlaylistDisplayItem> = emptyList(),
    val isLoadingPlaylists: Boolean = false,
    val isCreatingPlaylist: Boolean = false,
    val isAddingSong: Boolean = false,
    val showAddToPlaylistSheet: Boolean = false,
    val showCreatePlaylistDialog: Boolean = false,
    val selectedSongs: List<Song> = emptyList(),
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class PlaylistManagementViewModel @Inject constructor(
    private val database: MusicDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistManagementUiState())
    val uiState: StateFlow<PlaylistManagementUiState> = _uiState.asStateFlow()

    init {
        loadUserPlaylists()
    }

    fun loadUserPlaylists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPlaylists = true) }
            try {
                val dbPlaylists = withContext(Dispatchers.IO) {
                    database.playlists(com.omnitune.app.constants.PlaylistSortType.CREATE_DATE, true).first()
                }
                val displayPlaylists = dbPlaylists.map { p ->
                    PlaylistDisplayItem(
                        id = p.id,
                        name = p.playlist.name,
                        url = "omnitune://playlist/${p.id}",
                        uploaderName = "You",
                        thumbnailUrl = p.thumbnails.firstOrNull(),
                        songCount = p.songCount
                    )
                }
                _uiState.update {
                    it.copy(
                        userPlaylists = displayPlaylists,
                        isLoadingPlaylists = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingPlaylists = false) }
            }
        }
    }

    fun showAddToPlaylistSheet(song: Song) {
        _uiState.update {
            it.copy(
                showAddToPlaylistSheet = true,
                selectedSongs = listOf(song)
            )
        }
        loadUserPlaylists()
    }

    fun showAddToPlaylistSheet(songs: List<Song>) {
        if (songs.isEmpty()) return
        _uiState.update {
            it.copy(
                showAddToPlaylistSheet = true,
                selectedSongs = songs
            )
        }
        loadUserPlaylists()
    }

    fun hideAddToPlaylistSheet() {
        _uiState.update {
            it.copy(
                showAddToPlaylistSheet = false,
                selectedSongs = emptyList()
            )
        }
    }

    fun showCreatePlaylistDialog() {
        _uiState.update { it.copy(showCreatePlaylistDialog = true) }
    }

    fun hideCreatePlaylistDialog() {
        _uiState.update { it.copy(showCreatePlaylistDialog = false) }
    }

    fun createPlaylist(title: String, description: String, isPrivate: Boolean = true, syncWithYt: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingPlaylist = true) }
            try {
                val songs = _uiState.value.selectedSongs
                val newId = withContext(Dispatchers.IO) {
                    val actualId = "PL_" + UUID.randomUUID().toString().take(8)
                    val playlistEntity = PlaylistEntity(
                        id = actualId,
                        name = title,
                        bookmarkedAt = LocalDateTime.now()
                    )
                    database.insert(playlistEntity)
                    songs.forEachIndexed { index, song ->
                        database.insert(song.toMediaMetadata().toSongEntity())
                        database.insert(
                            PlaylistSongMap(
                                playlistId = actualId,
                                songId = song.id,
                                position = index
                            )
                        )
                    }
                    actualId
                }

                val msg = if (songs.isEmpty()) {
                    "Created playlist \"$title\""
                } else {
                    "Created playlist \"$title\" with ${songs.size} songs"
                }

                _uiState.update {
                    it.copy(
                        isCreatingPlaylist = false,
                        showCreatePlaylistDialog = false,
                        showAddToPlaylistSheet = false,
                        selectedSongs = emptyList(),
                        successMessage = msg
                    )
                }
                loadUserPlaylists()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingPlaylist = false,
                        errorMessage = "Failed to create playlist: ${e.message}"
                    )
                }
            }
        }
    }

    fun addSongsToPlaylist(playlistId: String) {
        val songs = _uiState.value.selectedSongs
        if (songs.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAddingSong = true) }
            try {
                withContext(Dispatchers.IO) {
                    val currentSongs = database.playlistSongs(playlistId).first()
                    val startPos = currentSongs.size
                    songs.forEachIndexed { index, song ->
                        database.insert(song.toMediaMetadata().toSongEntity())
                        database.insert(
                            PlaylistSongMap(
                                playlistId = playlistId,
                                songId = song.id,
                                position = startPos + index
                            )
                        )
                    }
                }

                val msg = if (songs.size == 1) "Added ${songs[0].title} to playlist" else "Added ${songs.size} songs to playlist"
                _uiState.update {
                    it.copy(
                        isAddingSong = false,
                        showAddToPlaylistSheet = false,
                        selectedSongs = emptyList(),
                        successMessage = msg
                    )
                }
                loadUserPlaylists()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAddingSong = false,
                        errorMessage = "Failed to add songs: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
