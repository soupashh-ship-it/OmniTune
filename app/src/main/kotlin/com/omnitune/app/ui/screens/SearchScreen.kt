/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

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
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles
import com.omnitune.app.ui.theme.omniPressScale
import com.omnitune.innertube.models.AlbumItem
import com.omnitune.innertube.models.ArtistItem
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.models.SongItem
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
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
private fun SearchTopBar(
    query: TextFieldValue,
    isSearching: Boolean,
    onQueryChange: (TextFieldValue) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.small)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = OmniSpacing.compact),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchIconButton(
                icon = R.drawable.ic_arrow_back,
                contentDescription = "Back",
                onClick = onBack,
            )
            Spacer(modifier = Modifier.width(OmniSpacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Search",
                    style = OmniTextStyles.screenTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Find songs, artists, albums, and playlists",
                    style = OmniTextStyles.metadata,
                    color = OmniColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        GlassSurface(
            cornerRadius = OmniShapes.Large,
            tone = GlassTone.Strong,
            backgroundAlpha = 0.14f,
            borderAlpha = 0.28f,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxSize(),
                placeholder = {
                    Text(
                        text = "Search songs, artists, albums...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OmniColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = OmniColors.OmniAccentSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                },
                trailingIcon = {
                    when {
                        isSearching -> OmniTuneLoader(size = 22.dp, color = OmniColors.ActivePlayback)
                        query.text.isNotEmpty() -> IconButton(
                            onClick = {
                                focusManager.clearFocus(force = true)
                                onClear()
                            },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = "Clear search",
                                tint = OmniColors.TextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = OmniColors.OmniAccentPrimary,
                    focusedTextColor = OmniColors.TextPrimary,
                    unfocusedTextColor = OmniColors.TextPrimary,
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = OmniColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus(force = true) },
                ),
                singleLine = true,
            )
        }
    }
}

@Composable
private fun SearchLoadingState() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
    ) {
        item {
            OmniSectionHeader(title = "Searching", action = "Working")
            Spacer(modifier = Modifier.height(OmniSpacing.compact))
        }
        items(5, key = { "search-loading-$it" }, contentType = { "loading" }) {
            GlassSurface(
                cornerRadius = OmniShapes.Large,
                tone = GlassTone.Subtle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(OmniSpacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShimmerBar(
                        modifier = Modifier.size(54.dp),
                        tone = GlassTone.Medium,
                    )
                    Spacer(modifier = Modifier.width(OmniSpacing.small))
                    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.compact)) {
                        ShimmerBar(
                            modifier = Modifier
                                .fillMaxWidth(0.66f)
                                .height(14.dp),
                        )
                        ShimmerBar(
                            modifier = Modifier
                                .fillMaxWidth(0.42f)
                                .height(10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchStartState(
    history: List<SearchHistory>,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
    ) {
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = OmniShapes.ExtraLarge,
                tone = GlassTone.Medium,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    OmniColors.OmniAccentPrimary.copy(alpha = 0.18f),
                                    OmniColors.OmniAccentSecondary.copy(alpha = 0.08f),
                                    Color.Transparent,
                                )
                            )
                        )
                        .padding(OmniSpacing.large),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.compact)) {
                        Text(
                            text = "Start with a song",
                            style = OmniTextStyles.sectionTitle,
                        )
                        Text(
                            text = "Search for real tracks and play them through the existing OmniTune player.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmniColors.TextSecondary,
                        )
                    }
                }
            }
        }

        if (history.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(OmniSpacing.compact))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OmniSectionHeader(
                        title = "Recent searches",
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onClearHistory) {
                        Text(
                            text = "Clear",
                            color = OmniColors.OmniAccentSecondary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            items(
                items = history,
                key = { "history-${it.query}" },
                contentType = { "history" },
            ) { item ->
                SearchHistoryRow(
                    query = item.query,
                    onClick = { onHistoryClick(item.query) },
                )
            }
        } else {
            item {
                SearchMessageCard(
                    icon = R.drawable.ic_search,
                    title = "Search for a song to start listening",
                    message = "Results will appear here as songs, artists, albums, and playlists when real data is available.",
                )
            }
        }
    }
}

@Composable
private fun SearchErrorState(
    message: String,
    status: SearchStatus,
    onRetry: () -> Unit,
) {
    val title = when (status) {
        SearchStatus.NetworkError -> "Search needs a connection"
        SearchStatus.ParserChanged -> "Search could not read results"
        else -> "Search failed"
    }

    SearchMessageCard(
        icon = R.drawable.ic_search,
        title = title,
        message = message.ifBlank { "Try again in a moment." },
        actionLabel = if (status == SearchStatus.NetworkError) "Retry when online" else "Retry",
        onAction = onRetry,
    )
}

@Composable
private fun SearchEmptyResults(query: String) {
    SearchMessageCard(
        icon = R.drawable.ic_search,
        title = "No results found",
        message = "No songs, artists, albums, or playlists matched \"$query\".",
    )
}

@Composable
private fun SearchResultsContent(
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

        if (songs.isNotEmpty()) {
            item(contentType = "section-songs") {
                SectionLabel(title = "Songs", count = songs.size)
            }
            itemsIndexed(
                items = songs,
                key = { index, song -> "song-${song.id.ifBlank { index.toString() }}" },
                contentType = { _, _ -> "song" },
            ) { index, song ->
                SearchResultRow(
                    title = song.title,
                    subtitle = song.artists.joinToString(", ") { it.name }.ifBlank { "Song" },
                    thumbnailUrl = song.thumbnail,
                    fallbackRes = R.drawable.ic_play_arrow,
                    onClick = {
                        Timber.tag("OmniTunePlaybackTrace").i("Search row clicked: ${song.title}")
                        onPlaySong(songs, index)
                    },
                    onPlayNext = { onPlayNext(song) },
                    onAddToQueue = { onAddToQueue(song) },
                )
            }
        }

        if (artists.isNotEmpty()) {
            item(contentType = "section-artists") {
                SectionLabel(title = "Artists", count = artists.size)
            }
            items(
                items = artists,
                key = { "artist-${it.id}" },
                contentType = { "artist" },
            ) { artist ->
                SearchResultRow(
                    title = artist.title,
                    subtitle = "Artist",
                    thumbnailUrl = artist.thumbnail,
                    fallbackRes = R.drawable.ic_artist,
                    circular = true,
                    onClick = { onNavigateToArtist(artist.id) },
                )
            }
        }

        if (albums.isNotEmpty()) {
            item(contentType = "section-albums") {
                SectionLabel(title = "Albums", count = albums.size)
            }
            items(
                items = albums,
                key = { "album-${it.browseId}" },
                contentType = { "album" },
            ) { album ->
                SearchResultRow(
                    title = album.title,
                    subtitle = album.artists?.joinToString(", ") { it.name }.orEmpty().ifBlank { "Album" },
                    thumbnailUrl = album.thumbnail,
                    fallbackRes = R.drawable.ic_album,
                    onClick = { onNavigateToAlbum(album.browseId) },
                )
            }
        }

        if (playlists.isNotEmpty()) {
            item(contentType = "section-playlists") {
                SectionLabel(title = "Playlists", count = playlists.size)
            }
            items(
                items = playlists,
                key = { "playlist-${it.id}" },
                contentType = { "playlist" },
            ) { playlist ->
                SearchResultRow(
                    title = playlist.title,
                    subtitle = playlist.author?.name ?: "Playlist",
                    thumbnailUrl = playlist.thumbnail,
                    fallbackRes = R.drawable.ic_list,
                    onClick = null,
                )
            }
        }

        item(contentType = "bottom-spacer") {
            Spacer(modifier = Modifier.height(OmniSpacing.section))
        }
    }
}

@Composable
private fun SectionLabel(
    title: String,
    count: Int,
) {
    OmniSectionHeader(
        title = title,
        action = "$count",
        modifier = Modifier.padding(top = OmniSpacing.compact),
    )
}

@Composable
private fun SearchHistoryRow(
    query: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Medium)
            .background(OmniColors.OmniGlassSubtle)
            .omniPressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(
                    bounded = true,
                    color = OmniColors.OmniAccentSecondary.copy(alpha = 0.14f),
                ),
                onClick = onClick,
            )
            .padding(OmniSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_history),
            contentDescription = null,
            tint = OmniColors.TextTertiary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Text(
            text = query,
            style = MaterialTheme.typography.bodyLarge,
            color = OmniColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    fallbackRes: Int,
    onClick: (() -> Unit)?,
    circular: Boolean = false,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    var menuExpanded by remember { mutableStateOf(false) }
    val artworkShape = if (circular) RoundedCornerShape(999.dp) else OmniShapes.ArtworkSmall
    val thumbnailModel = remember(thumbnailUrl) {
        thumbnailUrl?.let {
            ImageRequest.Builder(context)
                .data(it)
                .size(144, 144)
                .memoryCacheKey(it)
                .build()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Large)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        OmniColors.OmniGlassMedium,
                        OmniColors.OmniGlassSubtle,
                    )
                )
            )
            .then(
                if (onClick != null) {
                    Modifier
                        .omniPressScale(interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = androidx.compose.material3.ripple(
                                bounded = true,
                                color = OmniColors.OmniAccentSecondary.copy(alpha = 0.12f),
                            ),
                        ) {
                            focusManager.clearFocus(force = true)
                            onClick()
                        }
                } else {
                    Modifier
                }
            )
            .padding(OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(artworkShape)
                .background(OmniColors.OmniGlassStrong),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnailModel != null) {
                AsyncImage(
                    model = thumbnailModel,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    painter = painterResource(fallbackRes),
                    contentDescription = null,
                    tint = OmniColors.TextTertiary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(OmniSpacing.small))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = OmniTextStyles.songTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = OmniTextStyles.metadata,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (onPlayNext != null || onAddToQueue != null) {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = "More options",
                        tint = OmniColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(OmniColors.OmniBackgroundElevated),
                ) {
                    if (onPlayNext != null) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_skip_next),
                                    contentDescription = null,
                                )
                            },
                            text = { Text("Play next") },
                            onClick = {
                                menuExpanded = false
                                onPlayNext()
                            },
                        )
                    }
                    if (onAddToQueue != null) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_list),
                                    contentDescription = null,
                                )
                            },
                            text = { Text("Add to queue") },
                            onClick = {
                                menuExpanded = false
                                onAddToQueue()
                            },
                        )
                    }
                }
            }
        } else if (onClick == null) {
            Text(
                text = "Info",
                style = OmniTextStyles.caption,
                color = OmniColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun SearchStatusPill(message: String) {
    GlassSurface(
        cornerRadius = OmniShapes.Medium,
        tone = GlassTone.Subtle,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(OmniSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                tint = OmniColors.Warning,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(OmniSpacing.compact))
            Text(
                text = message,
                style = OmniTextStyles.metadata,
                color = OmniColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun SearchMessageCard(
    icon: Int,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = OmniShapes.ExtraLarge,
        tone = GlassTone.Medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OmniSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(OmniShapes.Large)
                    .background(OmniColors.OmniAccentPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = OmniColors.OmniAccentSecondary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Text(
                text = title,
                style = OmniTextStyles.sectionTitle,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(
                        text = actionLabel,
                        color = OmniColors.OmniAccentSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchIconButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(OmniShapes.Medium)
            .background(OmniColors.OmniGlassMedium)
            .omniPressScale(interactionSource),
        interactionSource = interactionSource,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = OmniColors.TextPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}
