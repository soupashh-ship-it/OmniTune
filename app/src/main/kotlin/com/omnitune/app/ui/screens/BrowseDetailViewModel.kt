package com.omnitune.app.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.models.Album
import com.omnitune.app.models.Artist
import com.omnitune.app.models.Playlist
import com.omnitune.app.models.Song
import com.omnitune.app.models.toSuvAlbum
import com.omnitune.app.models.toSuvArtist
import com.omnitune.app.models.toSuvPlaylist
import com.omnitune.app.models.toSuvSong
import com.omnitune.app.ui.navigation.Destination
import com.omnitune.app.utils.classifyProviderError
import com.omnitune.app.utils.reportException
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.AlbumItem
import com.omnitune.innertube.models.ArtistItem
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.models.SongItem
import com.omnitune.innertube.models.YTItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

sealed interface BrowseDetailItem {
    val id: String
    val title: String

    data class SongEntry(val song: Song) : BrowseDetailItem {
        override val id: String = "song_${song.id}"
        override val title: String = song.title
    }

    data class AlbumEntry(val album: Album) : BrowseDetailItem {
        override val id: String = "album_${album.id}"
        override val title: String = album.title
    }

    data class ArtistEntry(val artist: Artist) : BrowseDetailItem {
        override val id: String = "artist_${artist.id}"
        override val title: String = artist.name
    }

    data class PlaylistEntry(val playlist: Playlist) : BrowseDetailItem {
        override val id: String = "playlist_${playlist.id}"
        override val title: String = playlist.title
    }
}

data class BrowseDetailSection(
    val title: String,
    val items: List<BrowseDetailItem>,
) {
    val songs: List<Song>
        get() = items.mapNotNull { (it as? BrowseDetailItem.SongEntry)?.song }
}

data class BrowseDetailUiState(
    val title: String = "Explore",
    val sections: List<BrowseDetailSection> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class BrowseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val browseId: String = savedStateHandle[Destination.Explore.ARG_BROWSE_ID] ?: ""
    private val params: String? = savedStateHandle[Destination.Explore.ARG_PARAMS]
    private val initialTitle: String = savedStateHandle[Destination.Explore.ARG_TITLE] ?: "Explore"

    private val _uiState = MutableStateFlow(
        BrowseDetailUiState(
            title = initialTitle.ifBlank { "Explore" },
            isLoading = browseId.isNotBlank(),
            error = if (browseId.isBlank()) "Missing provider destination" else null,
        ),
    )
    val uiState: StateFlow<BrowseDetailUiState> = _uiState.asStateFlow()

    init {
        if (browseId.isNotBlank()) {
            load()
        }
    }

    fun retry() {
        if (browseId.isNotBlank()) {
            load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    withTimeout(10_000L) {
                        YouTube.browse(browseId, params).getOrThrow()
                    }
                }
            }.onSuccess { page ->
                val fallbackTitle = page.title?.takeIf { it.isNotBlank() } ?: initialTitle.ifBlank { "Explore" }
                val sections = page.items.mapIndexedNotNull { index, section ->
                    val items = section.items.mapNotNull(YTItem::toBrowseDetailItem)
                    if (items.isEmpty()) {
                        null
                    } else {
                        BrowseDetailSection(
                            title = section.title?.takeIf { it.isNotBlank() } ?: if (index == 0) fallbackTitle else "More",
                            items = items,
                        )
                    }
                }
                _uiState.update {
                    it.copy(
                        title = fallbackTitle,
                        sections = sections,
                        isLoading = false,
                        error = null,
                    )
                }
            }.onFailure { throwable ->
                reportException(throwable)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = classifyProviderError(throwable).message,
                    )
                }
            }
        }
    }
}

private fun YTItem.toBrowseDetailItem(): BrowseDetailItem? =
    when (this) {
        is SongItem -> BrowseDetailItem.SongEntry(toSuvSong())
        is AlbumItem -> BrowseDetailItem.AlbumEntry(toSuvAlbum())
        is ArtistItem -> BrowseDetailItem.ArtistEntry(toSuvArtist())
        is PlaylistItem -> BrowseDetailItem.PlaylistEntry(toSuvPlaylist())
    }
