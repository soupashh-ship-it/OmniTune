/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.SearchHistory
import com.omnitune.app.utils.isInternetAvailable
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.AlbumItem
import com.omnitune.innertube.models.ArtistItem
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.models.SongItem
import com.omnitune.app.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

enum class SearchStatus {
    Idle,
    Loading,
    Success,
    PartialResults,
    Empty,
    NetworkError,
    ParserChanged,
    CachedResultsShown,
}

data class SearchUiState(
    val query: String = "",
    val songs: List<SongItem> = emptyList(),
    val artists: List<ArtistItem> = emptyList(),
    val albums: List<AlbumItem> = emptyList(),
    val playlists: List<PlaylistItem> = emptyList(),
    val searchHistory: List<SearchHistory> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
    val status: SearchStatus = SearchStatus.Idle,
)

private const val SEARCH_DEBOUNCE_MS = 400L

@HiltViewModel
class SearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
                            status = SearchStatus.Idle,
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
        _uiState.value = _uiState.value.copy(
            isSearching = true,
            error = null,
            status = SearchStatus.Loading
        )

        var songsList = emptyList<SongItem>()
        var artistsList = emptyList<ArtistItem>()
        var albumsList = emptyList<AlbumItem>()
        var playlistsList = emptyList<PlaylistItem>()
        var searchError: String? = null
        var status = SearchStatus.Success
        var bucketFailures = 0
        var usedCachedResults = false

        try {
            val networkAvailable = isInternetAvailable(context)
            if (!networkAvailable) {
                val cached = lastGoodResults[query]
                if (cached != null) {
                    usedCachedResults = true
                    songsList = cached.songs
                    artistsList = cached.artists
                    albumsList = cached.albums
                    playlistsList = cached.playlists
                } else {
                    searchError = "No internet connection.\nRetry when online."
                    status = SearchStatus.NetworkError
                }
            }

            // Fallback 1: supervisorScope ensures one failed bucket does not cancel siblings.
            if (networkAvailable && !usedCachedResults) kotlinx.coroutines.supervisorScope {
                val songDeferred = async { YouTube.search(query, YouTube.SearchFilter.FILTER_SONG) }
                val albumDeferred = async { YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM) }
                val artistDeferred = async { YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST) }
                val playlistDeferred = async { YouTube.search(query, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST) }

                songDeferred.await()
                    .onSuccess { songsList = it.items.filterIsInstance<SongItem>() }
                    .onFailure { bucketFailures++; reportException(it) }
                albumDeferred.await()
                    .onSuccess { albumsList = it.items.filterIsInstance<AlbumItem>() }
                    .onFailure { bucketFailures++; reportException(it) }
                artistDeferred.await()
                    .onSuccess { artistsList = it.items.filterIsInstance<ArtistItem>() }
                    .onFailure { bucketFailures++; reportException(it) }
                playlistDeferred.await()
                    .onSuccess { playlistsList = it.items.filterIsInstance<PlaylistItem>() }
                    .onFailure { bucketFailures++; reportException(it) }
            }

            // Fallback 2: Mixed-result parser if primary failed
            if (networkAvailable && searchError == null && songsList.isEmpty() && artistsList.isEmpty() && albumsList.isEmpty() && playlistsList.isEmpty()) {
                YouTube.searchSummary(query)
                    .onSuccess { summaryResult ->
                        summaryResult.summaries.forEach { summary ->
                            when {
                                summary.title.contains("Song", ignoreCase = true) -> songsList = summary.items.filterIsInstance<SongItem>()
                                summary.title.contains("Artist", ignoreCase = true) -> artistsList = summary.items.filterIsInstance<ArtistItem>()
                                summary.title.contains("Album", ignoreCase = true) -> albumsList = summary.items.filterIsInstance<AlbumItem>()
                                summary.title.contains("Playlist", ignoreCase = true) -> playlistsList = summary.items.filterIsInstance<PlaylistItem>()
                            }
                        }
                    }
                    .onFailure { reportException(it) }
            }

            // Fallback 3: Last-good cached result
            if (searchError == null && songsList.isEmpty() && artistsList.isEmpty() && albumsList.isEmpty() && playlistsList.isEmpty()) {
                val cached = lastGoodResults[query]
                if (cached != null) {
                    usedCachedResults = true
                    songsList = cached.songs
                    artistsList = cached.artists
                    albumsList = cached.albums
                    playlistsList = cached.playlists
                } else {
                    searchError = "No results found for '$query'"
                    status = if (bucketFailures >= 4) SearchStatus.NetworkError else SearchStatus.Empty
                }
            }
        } catch (e: Exception) {
            val cached = lastGoodResults[query]
            if (cached != null) {
                usedCachedResults = true
                songsList = cached.songs
                artistsList = cached.artists
                albumsList = cached.albums
                playlistsList = cached.playlists
            } else {
                searchError = "Search failed: ${e.localizedMessage ?: "Unknown error"}"
                status = classifySearchFailure(e)
                reportException(e)
            }
        }

        if (searchError == null) {
            status = when {
                usedCachedResults -> SearchStatus.CachedResultsShown
                bucketFailures > 0 -> SearchStatus.PartialResults
                songsList.isEmpty() && artistsList.isEmpty() && albumsList.isEmpty() && playlistsList.isEmpty() -> SearchStatus.Empty
                else -> SearchStatus.Success
            }
        }

        val newState = _uiState.value.copy(
            songs = songsList,
            artists = artistsList,
            albums = albumsList,
            playlists = playlistsList,
            isSearching = false,
            error = searchError,
            status = status,
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

    private fun classifySearchFailure(throwable: Throwable): SearchStatus {
        val text = "${throwable::class.java.simpleName} ${throwable.message.orEmpty()}".lowercase()
        return when {
            "json" in text || "parse" in text || "serializer" in text || "unexpected" in text -> SearchStatus.ParserChanged
            "timeout" in text || "network" in text || "connect" in text || "unknownhost" in text -> SearchStatus.NetworkError
            else -> SearchStatus.NetworkError
        }
    }
}
