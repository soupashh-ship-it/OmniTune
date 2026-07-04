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

private const val SEARCH_DEBOUNCE_MS = 400L

@HiltViewModel
class SearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
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
                        searchJob = viewModelScope.launch {
                            delay(SEARCH_DEBOUNCE_MS)
                            performSearch(q.trim(), selectedFilter)
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
                performSearch(q.trim(), _uiState.value.selectedFilter, forceRefresh = true)
            }
        }
    }

    fun onFilterSelected(filter: SearchFilterTab) {
        if (filter == _uiState.value.selectedFilter) return
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        val q = _query.value.trim()
        if (q.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            performSearch(q, filter)
        }
    }

    fun loadMore() {
        val current = _uiState.value
        val continuation = current.continuation ?: return
        if (current.isSearching || current.isLoadingMore) return
        searchJob = viewModelScope.launch {
            loadMore(current.query.trim(), current.selectedFilter, continuation)
        }
    }

    private suspend fun performSearch(
        query: String,
        filter: SearchFilterTab,
        forceRefresh: Boolean = false,
    ) {
        val cacheKey = cacheKey(query, filter)
        if (!forceRefresh) {
            lastGoodResults[cacheKey]?.let { cached ->
                _uiState.value = _uiState.value.withBucket(
                    query = query,
                    filter = filter,
                    bucket = cached.copy(status = SearchStatus.CachedResultsShown),
                    isSearching = false,
                )
                return
            }
        }

        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            isSearching = true,
            isLoadingMore = false,
            error = null,
            status = SearchStatus.Loading,
            continuation = null,
        )

        Timber.tag("OmniTuneSearch").i("Starting search: length=${query.length}, filter=$filter")

        val bucket = try {
            val networkAvailable = isInternetAvailable(context)
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

        Timber.tag("OmniTuneSearch").i(
            "Search complete: filter=$filter songs=${bucket.songs.size}, artists=${bucket.artists.size}, albums=${bucket.albums.size}, playlists=${bucket.playlists.size}",
        )

        _uiState.value = _uiState.value.withBucket(
            query = query,
            filter = filter,
            bucket = bucket,
            isSearching = false,
        )

        if (bucket.error == null && bucket.hasResults) {
            saveSearchQuery(query)
            lastGoodResults[cacheKey] = bucket
        }
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

        if (songsList.isEmpty() && artistsList.isEmpty() && albumsList.isEmpty() && playlistsList.isEmpty()) {
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
        return YouTube.search(query, providerFilter).fold(
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
        val featured = YouTube.search(query, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
        val featuredItems = featured.getOrNull()?.items?.filterIsInstance<PlaylistItem>().orEmpty()
        val continuation = featured.getOrNull()?.continuation
        if (featuredItems.isNotEmpty()) {
            return SearchBucketState(playlists = featuredItems, continuation = continuation).withResolvedStatus(query)
        }

        return YouTube.search(query, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST).fold(
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

    private suspend fun loadMore(query: String, filter: SearchFilterTab, continuation: String) {
        _uiState.value = _uiState.value.copy(isLoadingMore = true, error = null)
        YouTube.searchContinuation(continuation).fold(
            onSuccess = { result ->
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

    private fun cacheKey(query: String, filter: SearchFilterTab): String = "${query.lowercase()}|${filter.name}"

    private val SearchBucketState.hasResults: Boolean
        get() = songs.isNotEmpty() || artists.isNotEmpty() || albums.isNotEmpty() || playlists.isNotEmpty()

    private fun SearchBucketState.withResolvedStatus(query: String): SearchBucketState =
        copy(
            status = if (hasResults) SearchStatus.Success else SearchStatus.Empty,
            error = if (hasResults) null else "No results found for '$query'",
        ).dedupe()

    private fun SearchBucketState.dedupe(): SearchBucketState = copy(
        songs = songs.distinctBy { it.id },
        artists = artists.distinctBy { it.id },
        albums = albums.distinctBy { it.id },
        playlists = playlists.distinctBy { it.id },
    )

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
