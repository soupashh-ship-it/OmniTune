/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.PlaylistSong
import com.omnitune.app.db.entities.PlaylistSongMap
import com.omnitune.app.models.AddPlaylistSongResult
import com.omnitune.app.models.PlaylistSuggestion
import com.omnitune.app.models.PlaylistSuggestionPage
import com.omnitune.app.models.PlaylistSuggestionQuery
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.utils.PlaylistSuggestionQueryBuilder
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.SongItem
import com.omnitune.innertube.models.YTItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val database: MusicDatabase,
) : ViewModel() {
    val db: MusicDatabase get() = database
    private val playlistId: String = savedStateHandle["playlistId"] ?: ""
    val getPlaylistId: String get() = playlistId


    val playlist = database.playlist(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val songs: StateFlow<List<PlaylistSong>> = database.playlistSongs(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val rawPlaylistSuggestions = MutableStateFlow<PlaylistSuggestion?>(null)
    val playlistSuggestions = combine(rawPlaylistSuggestions, songs) { suggestions, playlistSongs ->
        val existingIds = playlistSongs.map { it.song.id }.toSet()
        suggestions?.copy(items = suggestions.items.filter { it.id !in existingIds }.take(10))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val isLoadingSuggestions = MutableStateFlow(false)

    private val suggestionQueries = MutableStateFlow<List<PlaylistSuggestionQuery>>(emptyList())
    private var currentSuggestionQueryIndex = 0
    private var currentSuggestionPage: PlaylistSuggestionPage? = null
    private var suggestionsCacheTimestamp = 0L
    private var suggestedSongIds = emptySet<String>()
    private val suggestionMutex = Mutex()

    init {
        viewModelScope.launch {
            combine(playlist, songs) { currentPlaylist, currentSongs -> currentPlaylist to currentSongs }
                .collect { (currentPlaylist, _) ->
                    if (currentPlaylist != null) loadPlaylistSuggestions()
                }
        }
    }


    fun renamePlaylist(newName: String) {
        val current = playlist.value ?: return
        if (!current.playlist.isEditable) return
        viewModelScope.launch(Dispatchers.IO) {
            database.update(current.playlist.copy(name = newName))
        }
    }

    fun deletePlaylist() {
        val current = playlist.value ?: return
        if (!current.playlist.isEditable) return
        viewModelScope.launch(Dispatchers.IO) {
            database.delete(current.playlist)
        }
    }

    fun removeSong(songId: String) {
        val current = playlist.value ?: return
        if (!current.playlist.isEditable) return
        viewModelScope.launch(Dispatchers.IO) {
            database.withTransaction {
                removeSongFromPlaylist(current.playlist.id, songId)
                update(current.playlist.copy(lastUpdateTime = java.time.LocalDateTime.now()))
            }
        }
    }

    suspend fun addSongToPlaylist(song: SongItem, browseId: String? = null): AddPlaylistSongResult {
        val current = playlist.value ?: return AddPlaylistSongResult.Failed("Playlist unavailable")
        if (!current.playlist.isEditable) return AddPlaylistSongResult.Failed("Playlist is not editable")
        return try {
            val duplicate = withContext(Dispatchers.IO) {
                database.playlistDuplicates(current.id, listOf(song.id)).isNotEmpty()
            }
            if (duplicate) {
                markSuggestionAsSeen(song.id)
                return AddPlaylistSongResult.Duplicate
            }

            val remoteResult = browseId?.let { remotePlaylistId ->
                withContext(Dispatchers.IO) { YouTube.addToPlaylist(remotePlaylistId, song.id) }
            }
            if (remoteResult?.isFailure == true) {
                return AddPlaylistSongResult.Failed(remoteResult.exceptionOrNull()?.message)
            }

            withContext(Dispatchers.IO) {
                database.withTransaction {
                    insert(song.toMediaMetadata())
                    val maxPosition = maxPlaylistSongPosition(current.id) ?: -1
                    insert(
                        PlaylistSongMap(
                            playlistId = current.id,
                            songId = song.id,
                            position = maxPosition + 1,
                            setVideoId = song.setVideoId,
                        ),
                    )
                    update(current.playlist.copy(lastUpdateTime = java.time.LocalDateTime.now()))
                }
            }
            markSuggestionAsSeen(song.id)
            AddPlaylistSongResult.Added
        } catch (e: Exception) {
            AddPlaylistSongResult.Failed(e.message)
        }
    }

    fun loadPlaylistSuggestions(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            suggestionMutex.withLock {
                val currentPlaylist = playlist.first() ?: return@withLock
                val currentSongs = songs.first()

                if (!forceRefresh &&
                    rawPlaylistSuggestions.value != null &&
                    !PlaylistSuggestionQueryBuilder.shouldRefreshSuggestions(suggestionsCacheTimestamp)
                ) {
                    return@withLock
                }

                isLoadingSuggestions.value = true
                rawPlaylistSuggestions.value = null
                currentSuggestionQueryIndex = 0
                currentSuggestionPage = null
                suggestedSongIds = suggestedSongIds + currentSongs.map { it.song.id }

                try {
                    val queries = PlaylistSuggestionQueryBuilder.buildSuggestionQueries(
                        playlistName = currentPlaylist.playlist.name,
                        playlistSongs = currentSongs,
                    )
                    suggestionQueries.value = queries
                    suggestionsCacheTimestamp = System.currentTimeMillis()
                    loadNextSuggestionPage()
                } finally {
                    isLoadingSuggestions.value = false
                }
            }
        }
    }

    fun resetAndLoadPlaylistSuggestions() {
        loadPlaylistSuggestions(forceRefresh = true)
    }

    fun loadMoreSuggestions() {
        viewModelScope.launch {
            suggestionMutex.withLock {
                if (isLoadingSuggestions.value) return@withLock
                isLoadingSuggestions.value = true
                try {
                    val continuation = currentSuggestionPage?.continuation
                    if (continuation != null) {
                        loadMoreFromContinuation(continuation)
                    } else {
                        val nextIndex = currentSuggestionQueryIndex + 1
                        if (nextIndex < suggestionQueries.value.size) {
                            currentSuggestionQueryIndex = nextIndex
                            loadNextSuggestionPage()
                        } else {
                            rawPlaylistSuggestions.value = rawPlaylistSuggestions.value?.copy(hasMore = false)
                        }
                    }
                } finally {
                    isLoadingSuggestions.value = false
                }
            }
        }
    }

    private suspend fun loadNextSuggestionPage() {
        val queries = suggestionQueries.value
        val query = queries.getOrNull(currentSuggestionQueryIndex) ?: run {
            rawPlaylistSuggestions.value = PlaylistSuggestion(emptyList(), null, 0, 0, "", hasMore = false)
            return
        }
        val result = withContext(Dispatchers.IO) {
            YouTube.search(query.query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
        } ?: return

        val filtered = filterSuggestionItems(result.items).take(10)
        currentSuggestionPage = PlaylistSuggestionPage(filtered, result.continuation)
        suggestedSongIds = suggestedSongIds + filtered.map { it.id }
        rawPlaylistSuggestions.value = PlaylistSuggestion(
            items = filtered,
            continuation = result.continuation,
            currentQueryIndex = currentSuggestionQueryIndex,
            totalQueries = queries.size,
            query = query.query,
            hasMore = result.continuation != null || currentSuggestionQueryIndex < queries.lastIndex,
        )
        if (filtered.isEmpty() && rawPlaylistSuggestions.value?.hasMore == true) {
            loadMoreSuggestions()
        }
    }

    private suspend fun loadMoreFromContinuation(continuation: String) {
        val result = withContext(Dispatchers.IO) {
            YouTube.searchContinuation(continuation).getOrNull()
        } ?: return

        val filtered = filterSuggestionItems(result.items).take(10)
        currentSuggestionPage = PlaylistSuggestionPage(filtered, result.continuation)
        suggestedSongIds = suggestedSongIds + filtered.map { it.id }
        val current = rawPlaylistSuggestions.value
        rawPlaylistSuggestions.value = current?.copy(
            items = current.items + filtered,
            continuation = result.continuation,
            hasMore = result.continuation != null || current.currentQueryIndex < suggestionQueries.value.lastIndex,
        )
    }

    private fun filterSuggestionItems(items: List<YTItem>): List<YTItem> {
        val existing = songs.value.map { it.song.id }.toSet()
        return items
            .filterIsInstance<SongItem>()
            .filter { it.id !in existing && it.id !in suggestedSongIds }
            .distinctBy { it.id }
            .shuffled()
    }

    private fun markSuggestionAsSeen(songId: String) {
        suggestedSongIds = suggestedSongIds + songId
        rawPlaylistSuggestions.value = rawPlaylistSuggestions.value?.copy(
            items = rawPlaylistSuggestions.value?.items.orEmpty().filter { it.id != songId },
        )
    }


    fun moveSong(fromPosition: Int, toPosition: Int) {
        val current = playlist.value ?: return
        if (!current.playlist.isEditable) return
        viewModelScope.launch(Dispatchers.IO) {
            database.withTransaction {
                move(current.playlist.id, fromPosition, toPosition)
                update(current.playlist.copy(lastUpdateTime = java.time.LocalDateTime.now()))
            }
        }
    }

    fun removeSongs(songIds: List<String>) {
        val current = playlist.value ?: return
        if (!current.playlist.isEditable) return
        viewModelScope.launch(Dispatchers.IO) {
            database.withTransaction {
                songIds.forEach { removeSongFromPlaylist(current.playlist.id, it) }
                update(current.playlist.copy(lastUpdateTime = java.time.LocalDateTime.now()))
            }
        }
    }
}
