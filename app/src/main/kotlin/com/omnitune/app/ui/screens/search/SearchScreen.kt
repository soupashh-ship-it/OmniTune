package com.omnitune.app.ui.screens.search

import com.omnitune.app.ui.screens.SearchViewModel
import com.omnitune.app.ui.screens.SearchStatus
import com.omnitune.app.ui.screens.SearchUiState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.omnitune.app.R
import com.omnitune.app.db.entities.SearchHistory
import com.omnitune.app.ui.component.GlassCard
import com.omnitune.app.ui.component.GlassSurface
import com.omnitune.app.ui.component.GlassTone
import com.omnitune.app.ui.component.OmniSectionHeader
import com.omnitune.app.ui.component.OmniTuneLoader
import com.omnitune.app.ui.component.ShimmerBar
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.ui.component.TrackMenuProvider
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles
import com.omnitune.app.ui.theme.omniPressScale
import com.omnitune.innertube.models.AlbumItem
import com.omnitune.innertube.models.ArtistItem
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.models.SongItem
import timber.log.Timber

@Composable
fun SearchScreen(
    initialQuery: String? = null,
    onBack: () -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onPlaySong: (List<SongItem>, Int) -> Unit = { _, _ -> },
    onPlayNext: (SongItem) -> Unit = {},
    onAddToQueue: (SongItem) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val textFieldValue = remember { mutableStateOf(TextFieldValue(uiState.query)) }

    LaunchedEffect(initialQuery) {
        val query = initialQuery?.trim().orEmpty()
        if (query.isNotBlank() && query != uiState.query) {
            viewModel.onQueryChanged(query)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { uiState.query }.collect { query ->
            if (query != textFieldValue.value.text) {
                textFieldValue.value = TextFieldValue(query)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .statusBarsPadding()
            .padding(horizontal = OmniSpacing.medium),
    ) {
        SearchTopBar(
            query = textFieldValue.value,
            isSearching = uiState.isSearching,
            onQueryChange = {
                textFieldValue.value = it
                viewModel.onQueryChanged(it.text)
            },
            onClear = {
                textFieldValue.value = TextFieldValue("")
                viewModel.clearQuery()
            },
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(OmniSpacing.medium))

        when {
            uiState.isSearching -> SearchLoadingState()
            uiState.error != null -> SearchErrorState(
                message = uiState.error.orEmpty(),
                status = uiState.status,
                onRetry = viewModel::retrySearch,
            )
            uiState.query.isBlank() -> SearchStartState(
                history = uiState.searchHistory,
                onHistoryClick = viewModel::onQueryChanged,
                onClearHistory = viewModel::clearSearchHistory,
            )
            uiState.hasNoResults -> SearchEmptyResults(query = uiState.query)
            else -> SearchResultsContent(
                songs = uiState.songs,
                artists = uiState.artists,
                albums = uiState.albums,
                playlists = uiState.playlists,
                status = uiState.status,
                onNavigateToAlbum = onNavigateToAlbum,
                onNavigateToArtist = onNavigateToArtist,
                onPlaySong = onPlaySong,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
            )
        }
    }
}

private val SearchUiState.hasNoResults: Boolean
    get() = songs.isEmpty() && artists.isEmpty() && albums.isEmpty() && playlists.isEmpty()


@Composable
fun SearchResultsContent(
    songs: List<SongItem>,
    artists: List<ArtistItem>,
    albums: List<AlbumItem>,
    playlists: List<PlaylistItem>,
    status: SearchStatus,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onPlaySong: (List<SongItem>, Int) -> Unit = { _, _ -> },
    onPlayNext: (SongItem) -> Unit = {},
    onAddToQueue: (SongItem) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
    ) {
        if (status == SearchStatus.PartialResults || status == SearchStatus.CachedResultsShown) {
            item(contentType = "status") {
                val message = if (status == SearchStatus.CachedResultsShown) {
                    "Showing the last available results for this search."
                } else {
                    "Some result groups could not load, but available results are shown."
                }
                SearchStatusPill(message = message)
            }
        }

        songSearchResults(songs, onPlaySong, onPlayNext, onAddToQueue)
        artistSearchResults(artists, onNavigateToArtist)
        albumSearchResults(albums, onNavigateToAlbum)
        playlistSearchResults(playlists)
        item(contentType = "bottom-spacer") {
            Spacer(modifier = Modifier.height(OmniSpacing.section))
        }
    }
}

