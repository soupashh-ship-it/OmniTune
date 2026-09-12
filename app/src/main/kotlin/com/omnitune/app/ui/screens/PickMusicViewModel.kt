/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 *
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.content.MusicContentDiscoveryPolicy
import com.omnitune.app.content.MusicContentPreferenceRepository
import com.omnitune.app.models.Artist
import com.omnitune.app.models.Song
import com.omnitune.app.models.toPresentationSong
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.ArtistItem
import com.omnitune.innertube.models.SongItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PickMusicViewModel @Inject constructor(
    private val musicContentPreferenceRepository: MusicContentPreferenceRepository,
) : ViewModel() {
    private val _searchResults = MutableStateFlow<List<Artist>>(emptyList())
    val searchResults: StateFlow<List<Artist>> = _searchResults.asStateFlow()

    private val _selectedArtists = MutableStateFlow<List<Artist>>(emptyList())
    val selectedArtists: StateFlow<List<Artist>> = _selectedArtists.asStateFlow()

    private val _uiState = MutableStateFlow<PickMusicUiState>(PickMusicUiState.Selection)
    val uiState: StateFlow<PickMusicUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadPopularArtists()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        when {
            query.length > 2 -> searchArtists(query)
            query.isBlank() -> loadPopularArtists()
        }
    }

    fun toggleSelection(artist: Artist) {
        val currentSelection = _selectedArtists.value.toMutableList()
        val existing = currentSelection.find { it.id == artist.id }
        if (existing != null) {
            currentSelection.remove(existing)
        } else {
            currentSelection.add(artist)
        }
        _selectedArtists.value = currentSelection
    }

    fun createMix(onMixReady: (List<Song>) -> Unit) {
        viewModelScope.launch {
            val artists = _selectedArtists.value
            if (artists.isEmpty()) {
                _uiState.value = PickMusicUiState.Selection
                return@launch
            }

            _uiState.value = PickMusicUiState.Loading

            val songs = withContext(Dispatchers.IO) {
                buildMixForArtists(artists)
            }

            if (songs.isEmpty()) {
                _uiState.value = PickMusicUiState.Selection
                return@launch
            }

            onMixReady(songs)
            _uiState.value = PickMusicUiState.Success(
                message = randomSuccessMessage(),
                playlistName = mixName(artists),
                thumbnailUrl = songs.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl,
                songCount = songs.size,
                artistNames = artists.joinToString { it.name },
            )
        }
    }

    fun resetState() {
        _uiState.value = PickMusicUiState.Selection
    }

    private fun loadPopularArtists() {
        searchArtists("Trending Artists", languageWeighted = true)
    }

    private fun searchArtists(
        query: String,
        languageWeighted: Boolean = false,
    ) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                withContext(Dispatchers.IO) {
                    val language = musicContentPreferenceRepository.applyCurrentPreferenceToYouTube()
                    val requestQuery = if (languageWeighted) {
                        MusicContentDiscoveryPolicy.languageWeightedQuery(language, query)
                    } else {
                        query
                    }
                    YouTube.search(requestQuery, YouTube.SearchFilter.FILTER_ARTIST)
                        .getOrThrow()
                        .items
                        .filterIsInstance<ArtistItem>()
                        .map { item ->
                            Artist(
                                id = item.id,
                                name = item.title,
                                thumbnailUrl = item.thumbnail,
                                channelId = item.channelId,
                            )
                        }
                        .distinctBy { it.id }
                }
            }.onSuccess { artists ->
                _searchResults.value = artists
            }
            _isLoading.value = false
        }
    }

    private suspend fun buildMixForArtists(artists: List<Artist>): List<Song> {
        val targetTotal = 500
        val songsPerArtist = (targetTotal / artists.size).coerceAtLeast(10)
        val mixPlaylist = mutableListOf<Song>()
        val language = musicContentPreferenceRepository.applyCurrentPreferenceToYouTube()

        artists.forEach { artist ->
            val collectedForArtist = mutableListOf<Song>()

            runCatching {
                musicContentPreferenceRepository.applyLanguage(language)
                YouTube.artist(artist.id).getOrThrow()
            }.onSuccess { artistPage ->
                artistPage.sections
                    .firstOrNull {
                        it.title.contains("Top", ignoreCase = true) ||
                            it.title.contains("Song", ignoreCase = true)
                    }
                    ?.items
                    ?.filterIsInstance<SongItem>()
                    ?.map { it.toPresentationSong() }
                    ?.let(collectedForArtist::addAll)
            }

            if (collectedForArtist.size < songsPerArtist) {
                runCatching {
                    val query = MusicContentDiscoveryPolicy.languageWeightedQuery(
                        language = language,
                        rawQuery = "${artist.name} songs",
                    )
                    YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                        .getOrThrow()
                        .items
                        .filterIsInstance<SongItem>()
                        .map { it.toPresentationSong() }
                }.onSuccess { searchSongs ->
                    collectedForArtist.addAll(searchSongs)
                }
            }

            mixPlaylist.addAll(collectedForArtist.distinctBy { it.id }.take(songsPerArtist))
        }

        return mixPlaylist
            .distinctBy { it.id }
            .shuffled()
    }

    private fun mixName(artists: List<Artist>): String =
        "OmniTune Mix ${artists.take(3).joinToString(", ") { it.name }}"

    private fun randomSuccessMessage(): String {
        val messages = listOf(
            "Your playlist is ready to vibe!",
            "Curated just for you!",
            "Music to your ears, literally.",
            "Ready to rock and roll!",
            "Your soundtrack is served.",
            "Beats tailored to your taste.",
            "Excellent choice, Maestro!",
            "Your ears will thank you.",
        )
        return messages.random()
    }
}

sealed class PickMusicUiState {
    data object Selection : PickMusicUiState()
    data object Loading : PickMusicUiState()
    data class Success(
        val message: String,
        val playlistName: String,
        val thumbnailUrl: String? = null,
        val songCount: Int,
        val artistNames: String,
    ) : PickMusicUiState()
}
