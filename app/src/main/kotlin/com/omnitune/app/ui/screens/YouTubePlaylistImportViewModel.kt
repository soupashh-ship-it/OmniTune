package com.omnitune.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.sync.syncYouTubePlaylist
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.utils.completed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val YOUTUBE_MUSIC_PLAYLISTS_BROWSE_ID = "FEmusic_liked_playlists"

data class YouTubeLibraryPlaylist(
    val remote: PlaylistItem,
) {
    val id: String get() = remote.id
    val title: String get() = remote.title
    val songCountText: String? get() = remote.songCountText
}

data class YouTubePlaylistImportUiState(
    val isLoading: Boolean = true,
    val isImporting: Boolean = false,
    val playlists: List<YouTubeLibraryPlaylist> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val progressLabel: String? = null,
    val completedSummary: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class YouTubePlaylistImportViewModel @Inject constructor(
    private val database: MusicDatabase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(YouTubePlaylistImportUiState())
    val uiState: StateFlow<YouTubePlaylistImportUiState> = _uiState.asStateFlow()

    init {
        loadPlaylists()
    }

    fun loadPlaylists() {
        if (_uiState.value.isImporting) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    completedSummary = null,
                )
            }
            try {
                val playlists = withContext(Dispatchers.IO) {
                    YouTube.library(YOUTUBE_MUSIC_PLAYLISTS_BROWSE_ID)
                        .completed()
                        .getOrElse { error -> throw error }
                        .items
                        .filterIsInstance<PlaylistItem>()
                        .filter { it.id.isNotBlank() }
                        .map(::YouTubeLibraryPlaylist)
                        .distinctBy(YouTubeLibraryPlaylist::id)
                        .sortedBy { it.title.lowercase() }
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        playlists = playlists,
                        selectedIds = playlists.mapTo(linkedSetOf()) { playlist -> playlist.id },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load your YouTube Music playlists. Sign in again, then retry.",
                    )
                }
            }
        }
    }

    fun setSelected(playlistId: String, selected: Boolean) {
        _uiState.update { state ->
            state.copy(
                selectedIds = state.selectedIds.toMutableSet().apply {
                    if (selected) add(playlistId) else remove(playlistId)
                },
            )
        }
    }

    fun selectAll(selected: Boolean) {
        _uiState.update { state ->
            state.copy(selectedIds = if (selected) state.playlists.mapTo(linkedSetOf()) { it.id } else emptySet())
        }
    }

    fun importSelected() {
        val selected = _uiState.value.playlists.filter { it.id in _uiState.value.selectedIds }
        if (selected.isEmpty() || _uiState.value.isImporting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, errorMessage = null, completedSummary = null) }
            try {
                var importedSongs = 0
                selected.forEachIndexed { index, playlist ->
                    _uiState.update {
                        it.copy(progressLabel = "Importing ${index + 1} of ${selected.size}: ${playlist.title}")
                    }
                    importedSongs += withContext(Dispatchers.IO) {
                        syncYouTubePlaylist(database, playlist.remote)
                    }
                }
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        progressLabel = null,
                        completedSummary = "Imported ${selected.size} playlists and $importedSongs songs.",
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        progressLabel = null,
                        errorMessage = "Playlist import stopped. Check your connection or sign in again, then retry.",
                    )
                }
            }
        }
    }
}
