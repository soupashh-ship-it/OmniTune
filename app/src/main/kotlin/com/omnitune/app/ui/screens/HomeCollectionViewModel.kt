/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.ui.utils.resize
import com.omnitune.app.utils.isInternetAvailable
import com.omnitune.app.utils.reportException
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.SongItem
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
        collectionCache.remove(collectionId)
        load(forceRefresh = true)
    }

    private fun load(forceRefresh: Boolean = false) {
        val collection = metadata ?: run {
            _uiState.update { it.copy(error = "Collection not found.", isLoading = false) }
            return
        }
        if (_uiState.value.isLoading) return

        if (!forceRefresh) {
            collectionCache[collectionId]?.let { cached ->
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
                        collectionCache[collectionId] = CachedCollection(
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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Couldn't load songs. Check your connection and try again.",
                        )
                    }
                }
        }
    }

    private suspend fun loadCollectionSongs(collection: HomeCollectionMetadata): Result<List<SongItem>> {
        val providerId = collection.providerId
        if (!providerId.isNullOrBlank()) {
            return when (collection.actionType) {
                HomeActionType.OPEN_ALBUM -> YouTube.album(providerId)
                    .map { page -> page.songs.distinctBy { it.id }.take(collection.maxItems) }

                HomeActionType.OPEN_PLAYLIST -> YouTube.playlist(providerId)
                    .map { page -> page.songs.distinctBy { it.id }.take(collection.maxItems) }

                HomeActionType.OPEN_BROWSE -> YouTube.browse(providerId, collection.browseParams)
                    .map { page ->
                        page.items
                            .flatMap { it.items }
                            .filterIsInstance<SongItem>()
                            .distinctBy { it.id }
                            .take(collection.maxItems)
                    }

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

    private suspend fun searchCollectionSongs(collection: HomeCollectionMetadata): Result<List<SongItem>> =
        YouTube.search(collection.query, YouTube.SearchFilter.FILTER_SONG)
            .map { result ->
                result.items
                    .filterIsInstance<SongItem>()
                    .distinctBy { it.id }
                    .take(collection.maxItems)
            }
}
