package com.omnitune.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.constants.ArtistSortType
import com.omnitune.app.constants.SongSortType
import com.omnitune.app.db.entities.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class StatsUiState(
    val songCount: Int = 0,
    val artistCount: Int = 0,
    val albumCount: Int = 0,
    val likedCount: Int = 0,
    val recentlyPlayedCount: Int = 0,
    val playedThisWeek: Int = 0,
    val totalPlayed: Int = 0,
    val minutesListened: Long = 0,
    val topSongs: List<Pair<Song, Int>> = emptyList(),
    val topArtists: List<Pair<String, Int>> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val hasStats: Boolean
        get() = songCount > 0 ||
            likedCount > 0 ||
            recentlyPlayedCount > 0 ||
            totalPlayed > 0 ||
            minutesListened > 0 ||
            topSongs.isNotEmpty() ||
            topArtists.isNotEmpty()
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val database: MusicDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val songs = database.songs(SongSortType.CREATE_DATE, false).first()
                val artists = database.artists(ArtistSortType.CREATE_DATE, false).first()
                val albums = database.albumsBySongCountAsc().first()
                val events = database.events().first()
                val likedCount = database.likedSongsCount().first()
                val oneWeekAgo = LocalDateTime.now().minusDays(7)

                val calculatedTopSongs = events
                    .groupBy { it.song.id }
                    .map { entry -> entry.value.first().song to entry.value.size }
                    .sortedByDescending { it.second }
                    .take(5)
                val calculatedTopArtists = events
                    .flatMap { event -> event.song.artists.map { it.name } }
                    .filter { it.isNotBlank() }
                    .groupingBy { it }
                    .eachCount()
                    .toList()
                    .sortedByDescending { it.second }
                    .take(5)

                _uiState.value = StatsUiState(
                    songCount = songs.size,
                    artistCount = artists.size,
                    albumCount = albums.size,
                    likedCount = likedCount,
                    recentlyPlayedCount = events.map { it.song.id }.distinct().size,
                    playedThisWeek = events.count { it.event.timestamp.isAfter(oneWeekAgo) },
                    totalPlayed = events.size,
                    minutesListened = events.sumOf { it.event.playTime } / 60000,
                    topSongs = calculatedTopSongs,
                    topArtists = calculatedTopArtists,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = StatsUiState(
                    isLoading = false,
                    error = e.localizedMessage,
                )
            }
        }
    }
}
