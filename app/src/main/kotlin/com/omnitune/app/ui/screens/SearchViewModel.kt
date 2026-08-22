/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.data.SearchProvider
import com.omnitune.app.db.entities.SearchHistory
import com.omnitune.app.constants.RestrictExplicitContentKey
import com.omnitune.app.constants.SafeSearchKey
import com.omnitune.app.utils.PreferenceStore
import com.omnitune.app.utils.dataStore
import com.omnitune.app.utils.ProviderErrorType
import com.omnitune.app.utils.classifyProviderError
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.AlbumItem
import com.omnitune.innertube.models.ArtistItem
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.models.SongItem
import com.omnitune.app.utils.reportException
import timber.log.Timber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

enum class SearchFilterTab(val label: String) {
    All("All"),
    Songs("Songs"),
    Albums("Albums"),
    Artists("Artists"),
    Playlists("Playlists"),
    Videos("Videos"),
}

data class SearchUiState(
    val query: String = "",
    val selectedFilter: SearchFilterTab = SearchFilterTab.All,
    val songs: List<SongItem> = emptyList(),
    val artists: List<ArtistItem> = emptyList(),
    val albums: List<AlbumItem> = emptyList(),
    val playlists: List<PlaylistItem> = emptyList(),
    val searchHistory: List<SearchHistory> = emptyList(),
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val status: SearchStatus = SearchStatus.Idle,
    val continuation: String? = null,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val searchProvider: SearchProvider,
    private val networkStatus: SearchNetworkStatus,
    private val searchTiming: SearchTiming,
) : ViewModel() {
    private data class SearchBucketState(
        val songs: List<SongItem> = emptyList(),
        val artists: List<ArtistItem> = emptyList(),
        val albums: List<AlbumItem> = emptyList(),
        val playlists: List<PlaylistItem> = emptyList(),
        val error: String? = null,
        val status: SearchStatus = SearchStatus.Idle,
        val continuation: String? = null,
    )

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val requestGate = SearchRequestGate()
    private val lastGoodResults = object : java.util.LinkedHashMap<String, SearchBucketState>(24, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SearchBucketState>?) = size > 24
    }

    init {
        viewModelScope.launch {
            _query
                .collect { q ->
                    val selectedFilter = _uiState.value.selectedFilter
                    _uiState.value = _uiState.value.copy(query = q)

                    searchJob?.cancel()
                    if (q.isBlank()) {
                        requestGate.invalidate()
                        _uiState.value = _uiState.value.copy(
                            isSearching = false,
                            songs = emptyList(),
                            artists = emptyList(),
                            albums = emptyList(),
                            playlists = emptyList(),
                            continuation = null,
                            isLoadingMore = false,
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
                        val request = requestGate.begin(q.trim(), selectedFilter)
                        searchJob = viewModelScope.launch {
                            delay(searchTiming.debounceMillis)
                            performSearch(request)
                        }
                    }
                }
        }
        viewModelScope.launch {
            context.dataStore.data
                .map { preferences ->
                    (preferences[SafeSearchKey] ?: true) to
                        (preferences[RestrictExplicitContentKey] ?: false)
                }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    val currentQuery = _query.value.trim()
                    if (currentQuery.isNotBlank()) {
                        searchJob?.cancel()
                        val request = requestGate.begin(currentQuery, _uiState.value.selectedFilter)
                        searchJob = viewModelScope.launch {
                            performSearch(request, forceRefresh = true)
                        }
                    }
                }
        }
    }

    fun retrySearch() {
        val q = _query.value
        if (q.isNotBlank()) {
            searchJob?.cancel()
            val request = requestGate.begin(q.trim(), _uiState.value.selectedFilter)
            searchJob = viewModelScope.launch {
                performSearch(request, forceRefresh = true)
            }
        }
    }

    fun onFilterSelected(filter: SearchFilterTab) {
        if (filter == _uiState.value.selectedFilter) return
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        val q = _query.value.trim()
        if (q.isBlank()) return
        searchJob?.cancel()
        val request = requestGate.begin(q, filter)
        searchJob = viewModelScope.launch {
            performSearch(request)
        }
    }

    fun loadMore() {
        val current = _uiState.value
        val continuation = current.continuation ?: return
        if (current.isSearching || current.isLoadingMore) return
        val request = requestGate.currentFor(current.query.trim(), current.selectedFilter) ?: return
        searchJob = viewModelScope.launch {
            loadMore(request, continuation)
        }
    }

    private suspend fun performSearch(
        request: SearchRequest,
        forceRefresh: Boolean = false,
    ) {
        if (!requestGate.accepts(request)) return
        val query = request.query
        val filter = request.filter
        val cacheKey = cacheKey(query, filter)
        if (!forceRefresh) {
            lastGoodResults[cacheKey]?.let { cached ->
                if (!requestGate.accepts(request)) return
                _uiState.value = _uiState.value.withBucket(
                    query = query,
                    filter = filter,
                    bucket = cached.copy(status = SearchStatus.CachedResultsShown),
                    isSearching = false,
                )
                return
            }
        }

        if (!requestGate.accepts(request)) return
        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            isSearching = true,
            isLoadingMore = false,
            error = null,
            status = SearchStatus.Loading,
            continuation = null,
        )

        Timber.tag("OmniTuneSearch").i("Starting search: length=${query.length}, filter=$filter")

        val rawBucket = try {
            val networkAvailable = networkStatus.isOnline()
            if (!networkAvailable) {
                val cached = lastGoodResults[cacheKey]
                if (cached != null) {
                    cached.copy(status = SearchStatus.CachedResultsShown)
                } else {
                    SearchBucketState(
                        error = "No internet connection.\nRetry when online.",
                        status = SearchStatus.NetworkError,
                    )
                }
            } else {
                searchFilter(query, filter)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val cached = lastGoodResults[cacheKey]
            if (cached != null) {
                cached.copy(status = SearchStatus.CachedResultsShown)
            } else {
                reportException(e)
                SearchBucketState(
                    error = "Search failed: ${e.localizedMessage ?: "Unknown error"}",
                    status = classifySearchFailure(e),
                )
            }
        }
        val visibleBucket = rawBucket.visibleResults(query)
        // Provider APIs represent many failures as Result.failure rather than throwing. Treat
        // those the same as thrown failures so a refresh never discards known-good results.
        val bucket = if (visibleBucket.error != null) {
            lastGoodResults[cacheKey]?.copy(status = SearchStatus.CachedResultsShown) ?: visibleBucket
        } else {
            visibleBucket
        }

        if (!requestGate.accepts(request)) return

        Timber.tag("OmniTuneSearch").i(
            "Search complete: filter=$filter songs=${bucket.songs.size}, artists=${bucket.artists.size}, albums=${bucket.albums.size}, playlists=${bucket.playlists.size}",
        )

        if (bucket.error == null && bucket.hasResults) {
            saveSearchQuery(query)
            lastGoodResults[cacheKey] = bucket
        }

        _uiState.value = _uiState.value.withBucket(
            query = query,
            filter = filter,
            bucket = bucket,
            isSearching = false,
        )
    }

    private suspend fun searchFilter(query: String, filter: SearchFilterTab): SearchBucketState =
        when (filter) {
            SearchFilterTab.All -> searchAll(query)
            SearchFilterTab.Songs -> searchTyped(query, YouTube.SearchFilter.FILTER_SONG, filter)
            SearchFilterTab.Albums -> searchTyped(query, YouTube.SearchFilter.FILTER_ALBUM, filter)
            SearchFilterTab.Artists -> searchTyped(query, YouTube.SearchFilter.FILTER_ARTIST, filter)
            SearchFilterTab.Playlists -> searchPlaylists(query)
            SearchFilterTab.Videos -> searchTyped(query, YouTube.SearchFilter.FILTER_VIDEO, filter)
        }

    private suspend fun searchAll(query: String): SearchBucketState {
        var songsList = emptyList<SongItem>()
        var artistsList = emptyList<ArtistItem>()
        var albumsList = emptyList<AlbumItem>()
        var playlistsList = emptyList<PlaylistItem>()
        var bucketFailures = 0

        kotlinx.coroutines.supervisorScope {
            val songDeferred = async { searchProvider.search(query, YouTube.SearchFilter.FILTER_SONG) }
            val albumDeferred = async { searchProvider.search(query, YouTube.SearchFilter.FILTER_ALBUM) }
            val artistDeferred = async { searchProvider.search(query, YouTube.SearchFilter.FILTER_ARTIST) }
            val playlistDeferred = async { searchProvider.search(query, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST) }

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

        if (songsList.isEmpty() && artistsList.isEmpty() && albumsList.isEmpty() && playlistsList.isEmpty()) {
            searchProvider.searchSummary(query)
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

        return SearchBucketState(
            songs = songsList,
            artists = artistsList,
            albums = albumsList,
            playlists = playlistsList,
            status = bucketStatus(
                hasResults = songsList.isNotEmpty() || artistsList.isNotEmpty() || albumsList.isNotEmpty() || playlistsList.isNotEmpty(),
                bucketFailures = bucketFailures,
            ),
            error = if (songsList.isEmpty() && artistsList.isEmpty() && albumsList.isEmpty() && playlistsList.isEmpty()) "No results found for '$query'" else null,
        )
    }

    private suspend fun searchTyped(
        query: String,
        providerFilter: YouTube.SearchFilter,
        tab: SearchFilterTab,
    ): SearchBucketState {
        return searchProvider.search(query, providerFilter).fold(
            onSuccess = { result ->
                val songs = result.items.filterIsInstance<SongItem>()
                val albums = result.items.filterIsInstance<AlbumItem>()
                val artists = result.items.filterIsInstance<ArtistItem>()
                val playlists = result.items.filterIsInstance<PlaylistItem>()
                val bucket = when (tab) {
                    SearchFilterTab.Songs,
                    SearchFilterTab.Videos -> SearchBucketState(songs = songs, continuation = result.continuation)
                    SearchFilterTab.Albums -> SearchBucketState(albums = albums, continuation = result.continuation)
                    SearchFilterTab.Artists -> SearchBucketState(artists = artists, continuation = result.continuation)
                    else -> SearchBucketState(
                        songs = songs,
                        albums = albums,
                        artists = artists,
                        playlists = playlists,
                        continuation = result.continuation,
                    )
                }
                bucket.withResolvedStatus(query)
            },
            onFailure = { throwable ->
                reportException(throwable)
                SearchBucketState(
                    error = "Search failed: ${throwable.localizedMessage ?: "Unknown error"}",
                    status = classifySearchFailure(throwable),
                )
            },
        )
    }

    private suspend fun searchPlaylists(query: String): SearchBucketState {
        val featured = searchProvider.search(query, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
        val featuredItems = featured.getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
        val continuation = featured.getOrNull()?.continuation
        if (featuredItems.isNotEmpty()) {
            return SearchBucketState(playlists = featuredItems, continuation = continuation).withResolvedStatus(query)
        }

        return searchProvider.search(query, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST).fold(
            onSuccess = { result ->
                SearchBucketState(
                    playlists = result.items.filterIsInstance<PlaylistItem>(),
                    continuation = result.continuation,
                ).withResolvedStatus(query)
            },
            onFailure = { throwable ->
                reportException(throwable)
                if (featured.isFailure) reportException(featured.exceptionOrNull() ?: throwable)
                SearchBucketState(
                    error = "Search failed: ${throwable.localizedMessage ?: "Unknown error"}",
                    status = classifySearchFailure(throwable),
                )
            },
        )
    }

    private suspend fun loadMore(request: SearchRequest, continuation: String) {
        if (!requestGate.accepts(request)) return
        val query = request.query
        val filter = request.filter
        _uiState.value = _uiState.value.copy(isLoadingMore = true, error = null)
        searchProvider.searchContinuation(continuation).fold(
            onSuccess = { result ->
                if (!requestGate.accepts(request)) return@fold
                val current = _uiState.value
                val bucket = when (filter) {
                    SearchFilterTab.Songs,
                    SearchFilterTab.Videos -> SearchBucketState(
                        songs = current.songs + result.items.filterIsInstance<SongItem>(),
                        continuation = result.continuation,
                    )
                    SearchFilterTab.Albums -> SearchBucketState(
                        albums = current.albums + result.items.filterIsInstance<AlbumItem>(),
                        continuation = result.continuation,
                    )
                    SearchFilterTab.Artists -> SearchBucketState(
                        artists = current.artists + result.items.filterIsInstance<ArtistItem>(),
                        continuation = result.continuation,
                    )
                    SearchFilterTab.Playlists -> SearchBucketState(
                        playlists = current.playlists + result.items.filterIsInstance<PlaylistItem>(),
                        continuation = result.continuation,
                    )
                    SearchFilterTab.All -> SearchBucketState(
                        songs = current.songs,
                        artists = current.artists,
                        albums = current.albums,
                        playlists = current.playlists,
                        continuation = null,
                    )
                }.dedupe().withResolvedStatus(query)

                _uiState.value = current.withBucket(query, filter, bucket, isSearching = false).copy(isLoadingMore = false)
                if (bucket.hasResults) lastGoodResults[cacheKey(query, filter)] = bucket
            },
            onFailure = { throwable ->
                if (!requestGate.accepts(request)) return@fold
                reportException(throwable)
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    status = SearchStatus.PartialResults,
                    error = null,
                )
            },
        )
    }

    private fun bucketStatus(hasResults: Boolean, bucketFailures: Int = 0): SearchStatus = when {
        !hasResults -> SearchStatus.Empty
        bucketFailures > 0 -> SearchStatus.PartialResults
        else -> SearchStatus.Success
    }

    private fun cacheKey(query: String, filter: SearchFilterTab): String {
        val safeSearch = PreferenceStore.get(SafeSearchKey) ?: true
        val restrictExplicit = PreferenceStore.get(RestrictExplicitContentKey) ?: false
        return "${query.lowercase()}|${filter.name}|safe=$safeSearch|restricted=$restrictExplicit"
    }

    private val SearchBucketState.hasResults: Boolean
        get() = songs.isNotEmpty() || artists.isNotEmpty() || albums.isNotEmpty() || playlists.isNotEmpty()

    private fun SearchBucketState.withResolvedStatus(query: String): SearchBucketState =
        visibleResults(query)

    private fun SearchBucketState.visibleResults(query: String): SearchBucketState {
        val visible = dedupe()
        if (visible.hasResults) {
            return visible.copy(
                status = when (status) {
                    SearchStatus.PartialResults,
                    SearchStatus.CachedResultsShown -> status
                    else -> SearchStatus.Success
                },
                error = null,
            )
        }
        if (error != null && status !in setOf(SearchStatus.Success, SearchStatus.PartialResults, SearchStatus.CachedResultsShown)) {
            return visible
        }
        return visible.copy(
            status = SearchStatus.Empty,
            error = "No safe results found for '$query'",
        )
    }

    private fun SearchBucketState.dedupe(): SearchBucketState {
        val hideExplicit = (PreferenceStore.get(SafeSearchKey) ?: true) ||
            (PreferenceStore.get(RestrictExplicitContentKey) ?: false)
        return copy(
            songs = songs.distinctBy { it.id }.filterNot { hideExplicit && it.explicit },
            artists = artists.distinctBy { it.id }.filterNot { hideExplicit && it.explicit },
            albums = albums.distinctBy { it.id }.filterNot { hideExplicit && it.explicit },
            playlists = playlists.distinctBy { it.id }.filterNot { hideExplicit && it.explicit },
        )
    }

    private fun SearchUiState.withBucket(
        query: String,
        filter: SearchFilterTab,
        bucket: SearchBucketState,
        isSearching: Boolean,
    ): SearchUiState = copy(
        query = query,
        selectedFilter = filter,
        songs = bucket.songs,
        artists = bucket.artists,
        albums = bucket.albums,
        playlists = bucket.playlists,
        isSearching = isSearching,
        isLoadingMore = false,
        error = bucket.error,
        status = bucket.status,
        continuation = bucket.continuation,
        searchHistory = emptyList(),
    )

    fun onQueryChanged(query: String) {
        _query.value = query
    }

    fun clearQuery() {
        _query.value = ""
    }

    private suspend fun saveSearchQuery(query: String) {
        try {
            withContext(Dispatchers.IO) {
                database.insert(SearchHistory(query = query))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Search results remain useful if local history storage is unavailable.
            Timber.tag("OmniTuneSearch").w(e, "Could not save search history")
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearSearchHistory()
            _uiState.value = _uiState.value.copy(searchHistory = emptyList())
        }
    }

    private fun classifySearchFailure(throwable: Throwable): SearchStatus {
        val error = classifyProviderError(throwable)
        return when (error.type) {
            ProviderErrorType.ParserChanged -> SearchStatus.ParserChanged
            ProviderErrorType.Timeout,
            ProviderErrorType.NetworkUnavailable,
            ProviderErrorType.Unknown -> SearchStatus.NetworkError
            ProviderErrorType.Forbidden403,
            ProviderErrorType.NotFound404,
            ProviderErrorType.TooManyRequests429,
            ProviderErrorType.ServerError -> SearchStatus.NetworkError
        }
    }
}
