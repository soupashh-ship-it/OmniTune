package com.omnitune.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.SearchHistory
import com.omnitune.app.models.*
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.AlbumItem as InnerAlbumItem
import com.omnitune.innertube.models.ArtistItem as InnerArtistItem
import com.omnitune.innertube.models.PlaylistItem as InnerPlaylistItem
import com.omnitune.innertube.models.SongItem as InnerSongItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

sealed class SearchEvent {
    data class ShowAddToPlaylistSheet(val song: Song) : SearchEvent()
}

internal object SearchSourcePolicy {
    const val isHqAudioSearchEnabled: Boolean = false

    val visibleTabs: List<SearchTab> = if (isHqAudioSearchEnabled) {
        listOf(SearchTab.YOUTUBE_MUSIC, SearchTab.REMOTE)
    } else {
        listOf(SearchTab.YOUTUBE_MUSIC)
    }

    fun normalize(tab: SearchTab): SearchTab =
        if (tab in visibleTabs) tab else SearchTab.YOUTUBE_MUSIC

    fun filtersFor(tab: SearchTab): List<Pair<ResultFilter, String>> =
        when (normalize(tab)) {
            SearchTab.YOUTUBE_MUSIC -> youtubeFilters
            SearchTab.REMOTE -> hqAudioFilters
        }

    fun normalizeFilter(tab: SearchTab, filter: ResultFilter): ResultFilter =
        if (filtersFor(tab).any { it.first == filter }) filter else ResultFilter.ALL

    fun toRequestFilter(filter: ResultFilter): SearchFilterTab =
        when (filter) {
            ResultFilter.ALL -> SearchFilterTab.ALL
            ResultFilter.SONGS -> SearchFilterTab.SONGS
            ResultFilter.VIDEOS -> SearchFilterTab.VIDEOS
            ResultFilter.ALBUMS -> SearchFilterTab.ALBUMS
            ResultFilter.ARTISTS -> SearchFilterTab.ARTISTS
            ResultFilter.COMMUNITY_PLAYLISTS -> SearchFilterTab.PLAYLISTS
            ResultFilter.FEATURED_PLAYLISTS -> SearchFilterTab.FEATURED
        }

    private val youtubeFilters = listOf(
        ResultFilter.ALL to "All",
        ResultFilter.SONGS to "Songs",
        ResultFilter.VIDEOS to "Videos",
        ResultFilter.ALBUMS to "Albums",
        ResultFilter.ARTISTS to "Artists",
        ResultFilter.COMMUNITY_PLAYLISTS to "Community",
        ResultFilter.FEATURED_PLAYLISTS to "Featured"
    )

    private val hqAudioFilters = listOf(
        ResultFilter.ALL to "All",
        ResultFilter.SONGS to "Songs",
        ResultFilter.ALBUMS to "Albums",
        ResultFilter.ARTISTS to "Artists",
        ResultFilter.COMMUNITY_PLAYLISTS to "Playlists"
    )
}

data class SearchUiState(
    val query: String = "",
    val results: List<Song> = emptyList(),
    val artistResults: List<Artist> = emptyList(),
    val albumResults: List<Album> = emptyList(),
    val playlistResults: List<Playlist> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val browseCategories: List<BrowseCategory> = emptyList(),
    val recentSearches: List<RecentSearchItem> = emptyList(),
    val selectedTab: SearchTab = SearchTab.YOUTUBE_MUSIC,
    val showSuggestions: Boolean = false,
    val isLoading: Boolean = false,
    val isCategoriesLoading: Boolean = false,
    val isSuggestionsLoading: Boolean = false,
    val isSearchActive: Boolean = false,
    val error: String? = null,
    val resultFilter: ResultFilter = ResultFilter.ALL,
    val trendingSearches: List<String> = listOf(
        "Trending Hits",
        "Top Bollywood",
        "Lo-Fi Beats",
        "Workout Energy",
        "Acoustic Chill",
        "Pop Favorites",
        "Deep Focus"
    )
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val database: MusicDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SearchEvent>()
    val events: SharedFlow<SearchEvent> = _events.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    private var searchJob: Job? = null
    private var suggestionJob: Job? = null
    private val searchGate = SearchRequestGate()
    private val suggestionGate = SearchRequestGate()

    init {
        loadRecentSearches()
        loadBrowseCategories()

        viewModelScope.launch {
            _searchQuery
                .debounce(250)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collect { query -> fetchSuggestions(query) }
        }

        viewModelScope.launch {
            _searchQuery
                .debounce(650)
                .distinctUntilChanged()
                .filter { it.trim().length >= 2 }
                .collect { query -> searchInternal(query, saveToHistory = false) }
        }
    }

    private fun loadRecentSearches() {
        viewModelScope.launch {
            try {
                val dbHistory = withContext(Dispatchers.IO) {
                    database.searchHistory().first()
                }
                val items = dbHistory.map { RecentSearchItem.QueryItem(it.query, it.id.toString()) }
                _uiState.update { it.copy(recentSearches = items) }
            } catch (e: Exception) {
                logFailure(e, "Failed to load recent searches")
            }
        }
    }

    private fun loadBrowseCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCategoriesLoading = true) }
            try {
                val moodResult = withContext(Dispatchers.IO) {
                    YouTube.moodAndGenres()
                }
                moodResult.onSuccess { moodPage ->
                    val categories = moodPage.flatMap { group ->
                        group.items.map { item ->
                            BrowseCategory(
                                id = item.endpoint.browseId,
                                title = item.title,
                                color = item.stripeColor,
                                params = item.endpoint.params
                            )
                        }
                    }
                    _uiState.update {
                        it.copy(
                            browseCategories = categories,
                            isCategoriesLoading = false
                        )
                    }
                }.onFailure { error ->
                    logFailure(error, "Failed to load browse categories")
                    _uiState.update { it.copy(isCategoriesLoading = false) }
                }
            } catch (e: Exception) {
                logFailure(e, "Failed to load browse categories")
                _uiState.update { it.copy(isCategoriesLoading = false) }
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        val trimmedQuery = newQuery.trim()
        if (trimmedQuery.isBlank()) {
            searchGate.invalidate()
            suggestionGate.invalidate()
            searchJob?.cancel()
            suggestionJob?.cancel()
            _searchQuery.value = ""
            _uiState.update {
                it.copy(
                    query = "",
                    showSuggestions = false,
                    isSearchActive = false,
                    isLoading = false,
                    isSuggestionsLoading = false,
                    suggestions = emptyList(),
                    results = emptyList(),
                    artistResults = emptyList(),
                    albumResults = emptyList(),
                    playlistResults = emptyList(),
                    error = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                query = newQuery,
                showSuggestions = true,
                selectedTab = SearchSourcePolicy.normalize(it.selectedTab),
                resultFilter = SearchSourcePolicy.normalizeFilter(it.selectedTab, it.resultFilter)
            )
        }
        if (trimmedQuery.length < 2) {
            searchGate.invalidate()
            searchJob?.cancel()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    results = emptyList(),
                    artistResults = emptyList(),
                    albumResults = emptyList(),
                    playlistResults = emptyList(),
                    error = null
                )
            }
        }
        _searchQuery.value = newQuery
    }

    private fun fetchSuggestions(query: String) {
        val request = suggestionGate.begin(query, SearchFilterTab.ALL)
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            _uiState.update { it.copy(isSuggestionsLoading = true) }
            try {
                val suggestions = withContext(Dispatchers.IO) {
                    YouTube.searchSuggestions(query).getOrNull()?.queries ?: emptyList()
                }
                if (!suggestionGate.accepts(request) || _uiState.value.query != query) return@launch
                _uiState.update {
                    it.copy(
                        suggestions = suggestions,
                        isSuggestionsLoading = false
                    )
                }
            } catch (e: Exception) {
                if (!suggestionGate.accepts(request)) return@launch
                logFailure(e, "Failed to fetch search suggestions")
                _uiState.update { it.copy(isSuggestionsLoading = false) }
            }
        }
    }

    fun search(saveToHistory: Boolean = true) {
        val query = _uiState.value.query
        if (query.isBlank()) return
        searchInternal(query, saveToHistory)
    }

    private fun searchInternal(query: String, saveToHistory: Boolean) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < 2) return

        val normalizedTab = SearchSourcePolicy.normalize(_uiState.value.selectedTab)
        val normalizedFilter = SearchSourcePolicy.normalizeFilter(normalizedTab, _uiState.value.resultFilter)
        val request = searchGate.begin(
            query = normalizedQuery,
            filter = SearchSourcePolicy.toRequestFilter(normalizedFilter),
        )
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    showSuggestions = false,
                    isSearchActive = true,
                    selectedTab = normalizedTab,
                    resultFilter = normalizedFilter
                )
            }

            if (saveToHistory) {
                withContext(Dispatchers.IO) {
                    try {
                        database.insert(SearchHistory(query = normalizedQuery))
                    } catch (e: Exception) {
                        logFailure(e, "Failed to save search history")
                    }
                }
                loadRecentSearches()
            }

            try {
                val summaryResult = withContext(Dispatchers.IO) {
                    YouTube.searchSummary(normalizedQuery)
                }

                if (!searchGate.accepts(request) || _uiState.value.query.trim() != normalizedQuery) {
                    return@launch
                }

                summaryResult.onSuccess { summaryPage ->
                    val songs = mutableListOf<Song>()
                    val artists = mutableListOf<Artist>()
                    val albums = mutableListOf<Album>()
                    val playlists = mutableListOf<Playlist>()

                    summaryPage.summaries.forEach { summary ->
                        summary.items.forEach { item ->
                            when (item) {
                                is InnerSongItem -> songs.add(item.toPresentationSong())
                                is InnerAlbumItem -> albums.add(item.toPresentationAlbum())
                                is InnerArtistItem -> artists.add(item.toPresentationArtist())
                                is InnerPlaylistItem -> playlists.add(item.toPresentationPlaylist())
                            }
                        }
                    }


                    _uiState.update {
                        it.copy(
                            results = songs,
                            artistResults = artists,
                            albumResults = albums,
                            playlistResults = playlists,
                            isLoading = false,
                            error = null
                        )
                    }
                }.onFailure { error ->
                    if (!searchGate.accepts(request)) return@onFailure
                    logFailure(error, "Failed to search")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to search"
                        )
                    }
                }
            } catch (e: Exception) {
                if (!searchGate.accepts(request)) return@launch
                logFailure(e, "Failed to search")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to search"
                    )
                }
            }
        }
    }

    fun onTabChange(tab: SearchTab) {
        val normalizedTab = SearchSourcePolicy.normalize(tab)
        _uiState.update {
            it.copy(
                selectedTab = normalizedTab,
                resultFilter = SearchSourcePolicy.normalizeFilter(normalizedTab, it.resultFilter)
            )
        }
        if (_uiState.value.query.isNotBlank()) {
            search(saveToHistory = false)
        }
    }

    fun setResultFilter(filter: ResultFilter) {
        _uiState.update {
            it.copy(resultFilter = SearchSourcePolicy.normalizeFilter(it.selectedTab, filter))
        }
    }

    fun onTrendingSearchClick(term: String) {
        _uiState.update {
            it.copy(
                query = term,
                showSuggestions = false,
                isSearchActive = true
            )
        }
        searchInternal(term, saveToHistory = true)
    }

    fun onSuggestionClick(suggestion: String) {
        _uiState.update {
            it.copy(
                query = suggestion,
                showSuggestions = false,
                isSearchActive = true
            )
        }
        searchInternal(suggestion, saveToHistory = true)
    }

    fun addToRecentSearches(item: RecentSearchItem) {
        if (item is RecentSearchItem.QueryItem) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    database.insert(SearchHistory(query = item.query))
                    loadRecentSearches()
                } catch (e: Exception) {
                    logFailure(e, "Failed to add recent search")
                }
            }
        }
    }

    fun removeRecentSearch(item: RecentSearchItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (item is RecentSearchItem.QueryItem) {
                    val id = item.id.toLongOrNull()
                    if (id != null) {
                        database.delete(SearchHistory(id = id, query = item.query))
                    }
                }
                loadRecentSearches()
            } catch (e: Exception) {
                logFailure(e, "Failed to remove recent search")
            }
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.clearSearchHistory()
                loadRecentSearches()
            } catch (e: Exception) {
                logFailure(e, "Failed to clear recent searches")
            }
        }
    }


    fun onBackPressed(): Boolean {
        return if (_uiState.value.isSearchActive || _uiState.value.query.isNotBlank()) {
            searchGate.invalidate()
            suggestionGate.invalidate()
            searchJob?.cancel()
            suggestionJob?.cancel()
            _uiState.update {
                it.copy(
                    query = "",
                    isSearchActive = false,
                    showSuggestions = false,
                    results = emptyList(),
                    artistResults = emptyList(),
                    albumResults = emptyList(),
                    playlistResults = emptyList(),
                    suggestions = emptyList(),
                    isLoading = false,
                    isSuggestionsLoading = false,
                    error = null
                )
            }
            true
        } else {
            false
        }
    }

    fun addToPlaylist(song: Song) {
        viewModelScope.launch {
            _events.emit(SearchEvent.ShowAddToPlaylistSheet(song))
        }
    }

    private fun logFailure(error: Throwable, message: String) {
        if (error is CancellationException) throw error
        Timber.w(error, message)
    }
}
