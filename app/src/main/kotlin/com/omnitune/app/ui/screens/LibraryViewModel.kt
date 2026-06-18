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
    val playlistCount: Int = 0,
    val isLoading: Boolean = true,
)

/** Default sort type for liked songs */
private val LIKED_SONGS_SORT = com.omnitune.app.constants.SongSortType.CREATE_DATE

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val database: MusicDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val likedSongs: StateFlow<List<Song>>

    init {
        // Initialize liked songs flow once
        likedSongs = database.likedSongs(LIKED_SONGS_SORT, true)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        viewModelScope.launch {
            combine(
                database.likedSongsCount(),
                database.events(),
                database.songsByRowIdAsc(),
            ) { likedCount, events, librarySongs ->
                LibraryUiState(
                    likedCount = likedCount,
                    likedSongs = emptyList(),
                    recentlyPlayed = events,
                    librarySongCount = librarySongs.size,
                    playlistCount = 0,
                    isLoading = false,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
        // Fetch playlist count separately
        viewModelScope.launch {
            database.playlists(com.omnitune.app.constants.PlaylistSortType.CREATE_DATE, false).collect { playlists ->
                _uiState.value = _uiState.value.copy(playlistCount = playlists.size)
            }
        }
    }
}
