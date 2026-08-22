package com.omnitune.app.ui.screens.search

import com.omnitune.app.ui.screens.SearchViewModel
import com.omnitune.app.ui.screens.SearchStatus
import com.omnitune.app.ui.screens.SearchFilterTab
import com.omnitune.app.ui.screens.SearchUiState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel


import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.omnitune.app.R
import com.omnitune.app.db.entities.SearchHistory
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.component.OmniSectionHeader
import com.omnitune.app.ui.component.OmniTuneLoader
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
    onNavigateToPlaylist: (PlaylistItem) -> Unit = {},
    onPlaySong: (List<SongItem>, Int) -> Unit = { _, _ -> },
    onPlayNext: (SongItem) -> Unit = {},
    onAddToQueue: (SongItem) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val textFieldValue = remember { mutableStateOf(searchTextFieldValue(uiState.query)) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(initialQuery) {
        val query = initialQuery?.trim().orEmpty()
        if (query.isNotBlank() && query != uiState.query) {
            viewModel.onQueryChanged(query)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { uiState.query }.collect { query ->
            if (query != textFieldValue.value.text) {
                textFieldValue.value = searchTextFieldValue(query)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .statusBarsPadding()
            .padding(horizontal = OmniSpacing.medium),
    ) {
        SearchTopBar(
            query = textFieldValue.value,
            isSearching = uiState.isSearching,
            focusRequester = searchFocusRequester,
            onQueryChange = {
                textFieldValue.value = it
                viewModel.onQueryChanged(it.text)
            },
            onClear = {
                textFieldValue.value = searchTextFieldValue("")
                viewModel.clearQuery()
            },
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(OmniSpacing.medium))

        SearchFilterChips(
            selectedFilter = uiState.selectedFilter,
            compactResults = uiState.query.isNotBlank(),
            onFilterSelected = viewModel::onFilterSelected,
        )

        Spacer(modifier = Modifier.height(OmniSpacing.small))

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
                onStartSearch = { searchFocusRequester.requestFocus() },
            )
            uiState.hasNoResults -> SearchEmptyResults(query = uiState.query)
            else -> SearchResultsContent(
                songs = uiState.songs,
                artists = uiState.artists,
                albums = uiState.albums,
                playlists = uiState.playlists,
                selectedFilter = uiState.selectedFilter,
                status = uiState.status,
                continuation = uiState.continuation,
                isLoadingMore = uiState.isLoadingMore,
                onNavigateToAlbum = onNavigateToAlbum,
                onNavigateToArtist = onNavigateToArtist,
                onNavigateToPlaylist = onNavigateToPlaylist,
                onPlaySong = onPlaySong,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onLoadMore = viewModel::loadMore,
            )
        }
    }
}

private fun searchTextFieldValue(query: String): TextFieldValue =
    TextFieldValue(
        text = query,
        selection = TextRange(query.length),
    )

private val SearchUiState.hasNoResults: Boolean
    get() = songs.isEmpty() && artists.isEmpty() && albums.isEmpty() && playlists.isEmpty()


@Composable
fun SearchResultsContent(
    songs: List<SongItem>,
    artists: List<ArtistItem>,
    albums: List<AlbumItem>,
    playlists: List<PlaylistItem>,
    selectedFilter: SearchFilterTab,
    status: SearchStatus,
    continuation: String?,
    isLoadingMore: Boolean,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToPlaylist: (PlaylistItem) -> Unit,
    onPlaySong: (List<SongItem>, Int) -> Unit = { _, _ -> },
    onPlayNext: (SongItem) -> Unit = {},
    onAddToQueue: (SongItem) -> Unit = {},
    onLoadMore: () -> Unit = {},
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

        songSearchResults(
            songs = songs,
            onPlaySong = onPlaySong,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            sectionTitle = if (selectedFilter == SearchFilterTab.Videos) "Videos" else "Songs",
        )
        artistSearchResults(artists, onNavigateToArtist)
        albumSearchResults(albums, onNavigateToAlbum)
        playlistSearchResults(playlists, onNavigateToPlaylist)
        if (continuation != null && selectedFilter != SearchFilterTab.All) {
            item(contentType = "load-more") {
                TextButton(
                    enabled = !isLoadingMore,
                    onClick = onLoadMore,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isLoadingMore) {
                        OmniTuneLoader(size = 20.dp)
                        Spacer(modifier = Modifier.width(OmniSpacing.compact))
                    }
                    Text(if (isLoadingMore) "Loading more" else "Load more")
                }
            }
        }
        item(contentType = "bottom-spacer") {
            Spacer(modifier = Modifier.height(OmniChrome.BottomContentPaddingWithPlayer))
        }
    }
}

@Composable
private fun SearchFilterChips(
    selectedFilter: SearchFilterTab,
    compactResults: Boolean,
    onFilterSelected: (SearchFilterTab) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(
            items = SearchFilterTab.values().toList(),
            key = { it.name },
            contentType = { "search-filter" },
        ) { filter ->
            val selected = filter == selectedFilter
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .clip(OmniShapes.Pill)
                    .background(if (selected) OmniColors.OmniAccentPrimary else OmniColors.SurfaceRaised)
                    .border(
                        1.dp,
                        if (selected) OmniColors.OmniAccentPrimary else OmniColors.SurfaceHairline,
                        OmniShapes.Pill,
                    )
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(searchFilterIcon(filter)),
                    contentDescription = null,
                    tint = if (selected) OmniColors.TextOnAccent else OmniColors.TextSecondary,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = if (compactResults && filter == SearchFilterTab.All) "Top" else filter.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) OmniColors.TextOnAccent else OmniColors.TextPrimary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

private fun searchFilterIcon(filter: SearchFilterTab): Int = when (filter) {
    SearchFilterTab.All -> R.drawable.ic_grid
    SearchFilterTab.Songs, SearchFilterTab.Videos -> R.drawable.ic_play_arrow
    SearchFilterTab.Albums -> R.drawable.ic_album
    SearchFilterTab.Artists -> R.drawable.ic_artist
    SearchFilterTab.Playlists -> R.drawable.ic_list
}
