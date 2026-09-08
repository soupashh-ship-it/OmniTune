/*
 * Adapted from SuvMusic (https://github.com/SuvojeetDev/SuvMusic)
 * Copyright (c) SuvMusic contributors
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.models.Album
import com.omnitune.app.models.Artist
import com.omnitune.app.models.ArtistCreditInfo
import com.omnitune.app.models.ArtistPreview
import com.omnitune.app.models.Playlist
import com.omnitune.app.models.Song
import com.omnitune.app.models.toSuvAlbum
import com.omnitune.app.models.toSuvSong
import com.omnitune.app.ui.navigation.Destination
import com.omnitune.innertube.YouTube
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class ArtistError {
    NETWORK,
    AUTH_REQUIRED,
    UNKNOWN
}

data class ArtistUiState(
    val artist: Artist? = null,
    val isLoading: Boolean = false,
    val error: ArtistError? = null,
    val isSubscribing: Boolean = false,
    val isStartingRadio: Boolean = false,
    val radioStatus: String? = null,
    val showMultipleArtistsDialog: Boolean = false,
    val currentArtistCredits: List<ArtistCreditInfo> = emptyList()
)

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val artistId: String = savedStateHandle.get<String>(Destination.Artist.ARG_ARTIST_ID)
        ?: savedStateHandle.get<String>("artistId")
        ?: ""

    private val _uiState = MutableStateFlow(ArtistUiState(isLoading = true))
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    init {
        if (artistId.isNotBlank()) {
            loadArtist()
        }
    }

    fun toggleMultipleArtistsDialog(show: Boolean, credits: List<ArtistCreditInfo> = emptyList()) {
        _uiState.update {
            it.copy(
                showMultipleArtistsDialog = show,
                currentArtistCredits = if (show) credits else emptyList()
            )
        }
    }

    fun fetchArtistCreditsAndShow(artistString: String) {
        viewModelScope.launch {
            val names = artistString.split(",", "&", " feat.", " ft.", ";")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (names.size > 1) {
                val initialCredits = names.map { ArtistCreditInfo(it, "Artist", null, null) }
                toggleMultipleArtistsDialog(true, initialCredits)

                val updatedCredits = names.map { name ->
                    try {
                        val searchResult = withContext(Dispatchers.IO) {
                            YouTube.search(name, YouTube.SearchFilter.FILTER_ARTIST)
                        }
                        val match = searchResult.getOrNull()?.items?.filterIsInstance<com.omnitune.innertube.models.ArtistItem>()?.firstOrNull()
                        ArtistCreditInfo(name, "Artist", match?.id, match?.thumbnail)
                    } catch (e: Exception) {
                        ArtistCreditInfo(name, "Artist", null, null)
                    }
                }
                _uiState.update { it.copy(currentArtistCredits = updatedCredits) }
            }
        }
    }

    fun loadArtist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val ytResult = withContext(Dispatchers.IO) {
                    YouTube.artist(artistId)
                }

                ytResult.onSuccess { artistPage ->
                    val topSongs = artistPage.sections.find {
                        it.title.contains("Top", ignoreCase = true) || it.title.contains("Song", ignoreCase = true)
                    }?.items?.filterIsInstance<com.omnitune.innertube.models.SongItem>()
                        ?.map { it.toSuvSong() } ?: emptyList()

                    val albums = artistPage.sections.find {
                        it.title.equals("Albums", ignoreCase = true) || it.title.contains("Album", ignoreCase = true)
                    }?.items?.filterIsInstance<com.omnitune.innertube.models.AlbumItem>()
                        ?.map { it.toSuvAlbum() } ?: emptyList()

                    val singles = artistPage.sections.find {
                        it.title.contains("Single", ignoreCase = true) || it.title.contains("EP", ignoreCase = true)
                    }?.items?.filterIsInstance<com.omnitune.innertube.models.AlbumItem>()
                        ?.map { it.toSuvAlbum() } ?: emptyList()

                    val videos = artistPage.sections.find {
                        it.title.contains("Video", ignoreCase = true)
                    }?.items?.filterIsInstance<com.omnitune.innertube.models.SongItem>()
                        ?.map { it.toSuvSong().copy(isVideo = true) } ?: emptyList()

                    val featuredPlaylists = artistPage.sections.find {
                        it.title.contains("Featured", ignoreCase = true)
                    }?.items?.filterIsInstance<com.omnitune.innertube.models.PlaylistItem>()
                        ?.map {
                            Playlist(
                                id = it.id,
                                title = it.title,
                                author = it.author?.name ?: "",
                                thumbnailUrl = it.thumbnail
                            )
                        } ?: emptyList()

                    val relatedArtists = artistPage.sections.find {
                        it.title.contains("Similar", ignoreCase = true) || it.title.contains("Fan", ignoreCase = true)
                    }?.items?.filterIsInstance<com.omnitune.innertube.models.ArtistItem>()
                        ?.map {
                            ArtistPreview(
                                id = it.id,
                                name = it.title,
                                thumbnailUrl = it.thumbnail
                            )
                        } ?: emptyList()

                    val suvArtist = Artist(
                        id = artistId,
                        name = artistPage.artist.title,
                        thumbnailUrl = artistPage.artist.thumbnail,
                        description = artistPage.description,
                        subscribers = if (artistPage.artist.channelId != null) "Verified" else null,
                        songs = topSongs,
                        albums = albums,
                        singles = singles,
                        videos = videos,
                        featuredPlaylists = featuredPlaylists,
                        relatedArtists = relatedArtists,
                        channelId = artistPage.artist.channelId,
                        isSubscribed = false
                    )

                    _uiState.update {
                        it.copy(artist = suvArtist, isLoading = false, error = null)
                    }
                }.onFailure {
                    // Fallback to local DB
                    val local = withContext(Dispatchers.IO) {
                        database.artist(artistId).first()
                    }
                    if (local != null) {
                        val suvArtist = Artist(
                            id = local.id,
                            name = local.artist.name,
                            thumbnailUrl = local.artist.thumbnailUrl
                        )
                        _uiState.update { it.copy(artist = suvArtist, isLoading = false, error = null) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = ArtistError.NETWORK) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ArtistError.UNKNOWN) }
            }
        }
    }

    fun toggleSubscribe() {
        val current = _uiState.value.artist ?: return
        val newSub = !current.isSubscribed
        _uiState.update {
            it.copy(artist = current.copy(isSubscribed = newSub))
        }
    }

    fun startRadio(onPlaylistReady: (List<Song>) -> Unit) {
        val currentArtist = _uiState.value.artist ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isStartingRadio = true,
                    radioStatus = "Connecting with ${currentArtist.name} radio station..."
                )
            }
            try {
                val radioSongs = mutableListOf<Song>()
                radioSongs.addAll(currentArtist.songs)

                val searchResult = withContext(Dispatchers.IO) {
                    YouTube.search("${currentArtist.name} radio", YouTube.SearchFilter.FILTER_SONG)
                }
                searchResult.onSuccess { result ->
                    val songs = result.items.filterIsInstance<com.omnitune.innertube.models.SongItem>().map { it.toSuvSong() }
                    radioSongs.addAll(songs.filter { s -> radioSongs.none { it.id == s.id } })
                }

                _uiState.update { it.copy(radioStatus = "Preparing radio queue...") }
                if (radioSongs.isNotEmpty()) {
                    onPlaylistReady(radioSongs.distinctBy { it.id })
                } else {
                    onPlaylistReady(currentArtist.songs)
                }
            } catch (e: Exception) {
                onPlaylistReady(currentArtist.songs)
            } finally {
                _uiState.update { it.copy(isStartingRadio = false, radioStatus = null) }
            }
        }
    }
}
