/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.SearchHistory
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.AlbumItem
import com.omnitune.innertube.pages.SearchResult
import com.omnitune.innertube.models.ArtistItem
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.models.SongItem
import com.omnitune.app.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val songs: List<SongItem> = emptyList(),
    val artists: List<ArtistItem> = emptyList(),
    val albums: List<AlbumItem> = emptyList(),
    val playlists: List<PlaylistItem> = emptyList(),
    val searchHistory: List<SearchHistory> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
)

private const val SEARCH_DEBOUNCE_MS = 400L

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val database: MusicDatabase,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val lastGoodResults = object : java.util.LinkedHashMap<String, SearchUiState>(10, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SearchUiState>?) = size > 10
    }

    init {
        viewModelScope.launch {
            _query
                .collect { q ->
                    _uiState.value = _uiState.value.copy(query = q)

                    searchJob?.cancel()
                    if (q.isBlank()) {
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            songs = emptyList(),
                            artists = emptyList(),
                            albums = emptyList(),
                            playlists = emptyList(),
                            error = null,
                        )
                        // Observe search history
                        searchJob = viewModelScope.launch {
                            database.searchHistory().collect { history ->
                                _uiState.value = _uiState.value.copy(
                                    searchHistory = history,
                                )
                            }
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            searchHistory = emptyList(),
                            error = null,
                        )
                        searchJob = viewModelScope.launch {
                            delay(SEARCH_DEBOUNCE_MS)
                            performSearch(q.trim())
                        }
                    }
                }
        }
    }

    fun retrySearch() {
        val q = _query.value
        if (q.isNotBlank()) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                performSearch(q.trim())
            }
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = _uiState.value.copy(isSearching = true, error = null)

        var songsList = emptyList<SongItem>()
        var artistsList = emptyList<ArtistItem>()
        var albumsList = emptyList<AlbumItem>()
        var playlistsList = emptyList<PlaylistItem>()
        var searchError: String? = null

        try {
            // Fallback 1: SupervisorScope ensures one failure doesn't cancel siblings
            kotlinx.coroutines.supervisorScope {
                val songDeferred = async { YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull() }
                val albumDeferred = async { YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).getOrNull() }
                val artistDeferred = async { YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).getOrNull() }
                val playlistDeferred = async { YouTube.search(query, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST).getOrNull() }

                try { songDeferred.await()?.let { songsList = it.items.filterIsInstance<SongItem>() } } catch (e: Exception) { reportException(e) }
                try { albumDeferred.await()?.let { albumsList = it.items.filterIsInstance<AlbumItem>() } } catch (e: Exception) { reportException(e) }
                try { artistDeferred.await()?.let { artistsList = it.items.filterIsInstance<ArtistItem>() } } catch (e: Exception) { reportException(e) }
                try { playlistDeferred.await()?.let { playlistsList = it.items.filterIsInstance<PlaylistItem>() } } catch (e: Exception) { reportException(e) }
            }

            // Fallback 2: Mixed-result parser if primary failed
            if (songsList.isEmpty() && artistsList.isEmpty() && albumsList.isEmpty() && playlistsList.isEmpty()) {
                val summaryResult = YouTube.searchSummary(query).getOrNull()
                if (summaryResult != null) {
                    summaryResult.summaries.forEach { summary ->
                        when {
                            summary.title.contains("Song", ignoreCase = true) -> songsList = summary.items.filterIsInstance<SongItem>()
                            summary.title.contains("Artist", ignoreCase = true) -> artistsList = summary.items.filterIsInstance<ArtistItem>()
                            summary.title.contains("Album", ignoreCase = true) -> albumsList = summary.items.filterIsInstance<AlbumItem>()
                            summary.title.contains("Playlist", ignoreCase = true) -> playlistsList = summary.items.filterIsInstance<PlaylistItem>()
                        }
                    }
                }
            }

            // Fallback 3: Last-good cached result
            if (songsList.isEmpty() && artistsList.isEmpty() && albumsList.isEmpty() && playlistsList.isEmpty()) {
                val cached = lastGoodResults[query]
                if (cached != null) {
                    songsList = cached.songs
                    artistsList = cached.artists
                    albumsList = cached.albums
                    playlistsList = cached.playlists
                } else {
                    searchError = "No results found for '$query'"
                }
            }
        } catch (e: Exception) {
            val cached = lastGoodResults[query]
            if (cached != null) {
                songsList = cached.songs
                artistsList = cached.artists
                albumsList = cached.albums
                playlistsList = cached.playlists
            } else {
                searchError = "Search failed: ${e.localizedMessage ?: "Unknown error"}"
                reportException(e)
            }
        }

        val newState = _uiState.value.copy(
            songs = songsList,
            artists = artistsList,
            albums = albumsList,
            playlists = playlistsList,
            isSearching = false,
            error = searchError,
        )

        _uiState.value = newState

        if (searchError == null && (songsList.isNotEmpty() || artistsList.isNotEmpty() || albumsList.isNotEmpty() || playlistsList.isNotEmpty())) {
            saveSearchQuery(query)
            lastGoodResults[query] = newState
        }
    }

    fun onQueryChanged(query: String) {
        _query.value = query
    }

    fun clearQuery() {
        _query.value = ""
    }

    private fun saveSearchQuery(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.insert(SearchHistory(query = query))
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearSearchHistory()
            _uiState.value = _uiState.value.copy(searchHistory = emptyList())
        }
    }
}
