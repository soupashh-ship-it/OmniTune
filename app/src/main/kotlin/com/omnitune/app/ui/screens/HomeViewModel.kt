package com.omnitune.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.EventWithSong
import com.omnitune.app.db.entities.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentSongs: List<EventWithSong> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val database: MusicDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _quickPicks = MutableStateFlow<List<Song>>(emptyList())
    val quickPicks: StateFlow<List<Song>> = _quickPicks.asStateFlow()

    private val _quickPicksLoading = MutableStateFlow(true)
    val quickPicksLoading: StateFlow<Boolean> = _quickPicksLoading.asStateFlow()

    init {
        loadQuickPicks()
        viewModelScope.launch {
            try {
                database.events().collect { events ->
                    _uiState.value = HomeUiState(
                        recentSongs = events.take(20),
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    error = e.localizedMessage,
                )
            }
        }
    }

    private fun loadQuickPicks() {
        viewModelScope.launch {
            try {
                // Use the YouTube InnerTube client to get home feed suggestions
                val result = com.omnitune.innertube.YouTube.home().getOrNull()
                val songs = result?.sections
                    ?.flatMap { section -> section.items }
                    ?.filterIsInstance<com.omnitune.innertube.models.SongItem>()
                    ?.take(20)
                    ?.map { item ->
                        com.omnitune.app.db.entities.Song(
                            song = com.omnitune.app.db.entities.SongEntity(
                                id = item.id,
                                title = item.title,
                                duration = item.duration ?: -1,
                                thumbnailUrl = item.thumbnail,
                                albumId = item.album?.id,
                                albumName = item.album?.name,
                                explicit = item.explicit,
                                year = null,
                                liked = false,
                                likedDate = null,
                                totalPlayTime = 0,
                                inLibrary = null,
                                isLocal = false,
                            ),
                            artists = item.artists.map { artist ->
                                com.omnitune.app.db.entities.ArtistEntity(
                                    id = artist.id ?: "",
                                    name = artist.name,
                                    thumbnailUrl = null,
                                    channelId = null,
                                    bookmarkedAt = null,
                                    isLocal = false
                                )
                            }
                        )
                    } ?: emptyList()
                _quickPicks.value = songs
                _quickPicksLoading.value = false
            } catch (e: Exception) {
                _quickPicksLoading.value = false
                // Silently fail — home feed is best-effort
            }
        }
    }
}
