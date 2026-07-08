/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.discovery.MoodGenreCategories
import com.omnitune.app.discovery.MoodGenreResolver
import com.omnitune.app.ui.utils.resize
import com.omnitune.app.utils.classifyProviderError
import com.omnitune.app.utils.isInternetAvailable
import com.omnitune.app.utils.reportException
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.AlbumItem
import com.omnitune.innertube.models.ArtistItem
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.models.SongItem
import com.omnitune.innertube.models.YTItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

data class HomeCollectionUiState(
    val metadata: HomeCollectionMetadata? = null,
    val songs: List<SongItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val headerArtworkUrl: String? = null,
) {
    val canPlay: Boolean get() = songs.isNotEmpty() && !isLoading
    val countLabel: String
        get() = when {
            isLoading && songs.isEmpty() -> "Loading songs"
            songs.isEmpty() -> "Songs"
            songs.size == 1 -> "1 song"
            else -> "${songs.size} songs"
        }
}

@HiltViewModel
class HomeCollectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private data class CachedCollection(
        val songs: List<SongItem>,
        val headerArtworkUrl: String?,
    )

    private companion object {
        private val collectionCache = ConcurrentHashMap<String, CachedCollection>()
    }

    private val collectionId: String = checkNotNull(savedStateHandle["collectionId"])
    private val initialArtworkUrl: String? = savedStateHandle["artworkUrl"]
    private val metadata = HomeDefaultCatalog.findCollection(collectionId)
    private val moodGenreResolver = MoodGenreResolver()

    private val _uiState = MutableStateFlow(
        HomeCollectionUiState(
            metadata = metadata,
            headerArtworkUrl = initialArtworkUrl?.takeIf { it.isNotBlank() },
        ),
    )
    val uiState: StateFlow<HomeCollectionUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        metadata?.let { collectionCache.remove(it.cacheKey()) } ?: collectionCache.remove(collectionId)
        load(forceRefresh = true)
    }

    private fun load(forceRefresh: Boolean = false) {
        val collection = metadata ?: run {
            _uiState.update { it.copy(error = "Collection not found.", isLoading = false) }
            return
        }
        if (_uiState.value.isLoading) return

        if (!forceRefresh) {
            collectionCache[collection.cacheKey()]?.let { cached ->
                _uiState.update { current ->
                    current.copy(
                        songs = cached.songs,
                        isLoading = false,
                        error = null,
                        headerArtworkUrl = current.headerArtworkUrl ?: cached.headerArtworkUrl,
                    )
                }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            if (!isInternetAvailable(context)) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Couldn't load songs. Check your connection and try again.",
                    )
                }
                return@launch
            }

            loadCollectionSongs(collection)
                .onSuccess { result ->
                    val songs = result
                    val headerArtworkUrl = songs.firstOrNull()?.thumbnail?.resize(544, 544)
                    if (songs.isNotEmpty()) {
                        collectionCache[collection.cacheKey()] = CachedCollection(
                            songs = songs,
                            headerArtworkUrl = headerArtworkUrl,
                        )
                    }
                    _uiState.update { current ->
                        current.copy(
                            songs = songs,
                            isLoading = false,
                            error = if (songs.isEmpty()) "No songs found. Try searching again." else null,
                            headerArtworkUrl = current.headerArtworkUrl ?: headerArtworkUrl,
                        )
                    }
                }
                .onFailure { throwable ->
                    reportException(throwable)
                    val pe = classifyProviderError(throwable)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = pe.message,
                        )
                    }
                }
        }
    }

    private suspend fun loadCollectionSongs(collection: HomeCollectionMetadata): Result<List<SongItem>> {
        val category = MoodGenreCategories.forCollection(collection)
        if (category != null) {
            return moodGenreResolver.loadCategorySongs(category, collection.maxItems)
                .map { result -> result.songs }
        }

        val providerId = collection.providerId
        if (!providerId.isNullOrBlank()) {
            return when (collection.actionType) {
                HomeActionType.OPEN_ALBUM -> YouTube.album(providerId)
                    .map { page -> page.songs.distinctBy { it.id }.take(collection.maxItems) }

                HomeActionType.OPEN_PLAYLIST -> runCatching {
                    val firstPage = YouTube.playlist(providerId).getOrThrow()
                    val allSongs = firstPage.songs.toMutableList()
                    var continuation = firstPage.songsContinuation
                    val seenContinuations = mutableSetOf<String>()
                    var requestCount = 0
                    val maxRequests = 50

                    while (continuation != null && allSongs.size < collection.maxItems && requestCount < maxRequests) {
                        if (continuation in seenContinuations) break
                        seenContinuations.add(continuation)
                        requestCount++
                        val nextPage = YouTube.playlistContinuation(continuation).getOrNull() ?: break
                        if (nextPage.songs.isEmpty()) break
                        allSongs.addAll(nextPage.songs)
                        continuation = nextPage.continuation
                    }

                    allSongs.distinctBy { it.id }.take(collection.maxItems)
                }

                HomeActionType.OPEN_BROWSE -> loadBrowseSongs(collection, providerId)

                HomeActionType.OPEN_ARTIST -> YouTube.artist(providerId)
                    .map { page ->
                        page.sections
                            .flatMap { it.items }
                            .filterIsInstance<SongItem>()
                            .distinctBy { it.id }
                            .take(collection.maxItems)
                    }

                else -> searchCollectionSongs(collection)
            }
        }

        return searchCollectionSongs(collection)
    }

    private suspend fun loadBrowseSongs(
        collection: HomeCollectionMetadata,
        providerId: String,
    ): Result<List<SongItem>> = runCatching {
        val browseItems = YouTube.browse(providerId, collection.browseParams)
            .getOrThrow()
            .items
            .flatMap { it.items }
            .distinctBy { item -> "${item::class.simpleName}:${item.id}" }

        val songs = LinkedHashMap<String, SongItem>()

        fun addSongs(items: List<SongItem>) {
            items.forEach { song ->
                if (songs.size < collection.maxItems) {
                    songs.putIfAbsent(song.id, song)
                }
            }
        }

        addSongs(browseItems.filterIsInstance<SongItem>())

        for (item in browseItems) {
            if (songs.size >= collection.maxItems) break
            addSongs(item.resolveBrowseSongs(collection.maxItems - songs.size))
        }

        songs.values.take(collection.maxItems)
    }

    private suspend fun YTItem.resolveBrowseSongs(limit: Int): List<SongItem> {
        if (limit <= 0) return emptyList()
        return when (this) {
            is SongItem -> listOf(this)
            is PlaylistItem -> {
                val firstPage = YouTube.playlist(id).getOrNull() ?: return emptyList()
                val allSongs = firstPage.songs.toMutableList()
                var continuation = firstPage.songsContinuation
                val seenContinuations = mutableSetOf<String>()
                var requestCount = 0
                val maxRequests = 50

                while (continuation != null && allSongs.size < limit && requestCount < maxRequests) {
                    if (continuation in seenContinuations) break
                    seenContinuations.add(continuation)
                    requestCount++
                    val nextPage = YouTube.playlistContinuation(continuation).getOrNull() ?: break
                    if (nextPage.songs.isEmpty()) break
                    allSongs.addAll(nextPage.songs)
                    continuation = nextPage.continuation
                }

                allSongs.distinctBy { it.id }.take(limit)
            }
            is AlbumItem -> YouTube.album(browseId)
                .getOrNull()
                ?.songs
                .orEmpty()
            is ArtistItem -> YouTube.artist(id)
                .getOrNull()
                ?.sections
                .orEmpty()
                .flatMap { it.items }
                .filterIsInstance<SongItem>()
        }.distinctBy { it.id }.take(limit)
    }

    private suspend fun searchCollectionSongs(collection: HomeCollectionMetadata): Result<List<SongItem>> =
        runCatching {
            val allSongs = mutableListOf<SongItem>()
            val seenContinuations = mutableSetOf<String>()

            val firstResult = YouTube.search(collection.query, YouTube.SearchFilter.FILTER_SONG).getOrThrow()
            allSongs.addAll(firstResult.items.filterIsInstance<SongItem>())
            var continuation = firstResult.continuation

            var requestCount = 0
            val maxRequests = 50

            while (continuation != null && allSongs.size < collection.maxItems && requestCount < maxRequests) {
                if (continuation in seenContinuations) break
                seenContinuations.add(continuation)
                requestCount++
                val nextResult = YouTube.searchContinuation(continuation).getOrNull() ?: break
                val newSongs = nextResult.items.filterIsInstance<SongItem>()
                if (newSongs.isEmpty()) break
                allSongs.addAll(newSongs)
                continuation = nextResult.continuation
            }

            allSongs.distinctBy { it.id }.take(collection.maxItems)
        }

    private fun HomeCollectionMetadata.cacheKey(): String {
        val category = MoodGenreCategories.forCollection(this)
        return if (category != null) {
            "category:${category.id}:v${category.queryVersion}"
        } else {
            id
        }
    }
}
