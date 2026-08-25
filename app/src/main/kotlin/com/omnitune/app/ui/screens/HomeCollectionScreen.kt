/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.omnitune.app.R
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.component.OmniWaveformLoader
import com.omnitune.app.ui.component.TrackMenuProvider
import com.omnitune.app.ui.theme.omniColors
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.omniColors
import com.omnitune.app.ui.theme.OmniMotion
import com.omnitune.app.ui.theme.omniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.omniColors
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.omniColors
import com.omnitune.app.ui.theme.OmniTextStyles
import com.omnitune.app.ui.theme.omniColors
import com.omnitune.app.ui.theme.omniPressScale
import com.omnitune.app.ui.component.shimmer.ShimmerTrackRow
import com.omnitune.innertube.models.SongItem
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val COLLECTION_ARTWORK_SIZE = 544
private const val TRACK_ARTWORK_SIZE = 144

@Composable
fun HomeCollectionRoute(
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onOpenRelated: (SongItem) -> Unit,
    onPlaySongs: (List<SongItem>, Int) -> Unit,
    onPlayNext: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    viewModel: HomeCollectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeCollectionScreen(
        uiState = uiState,
        onBack = onBack,
        onSearch = onSearch,
        onRetry = viewModel::retry,
        onOpenRelated = onOpenRelated,
        onPlaySongs = onPlaySongs,
        onPlayNext = onPlayNext,
        onAddToQueue = onAddToQueue,
    )
}

@Composable
private fun HomeCollectionScreen(
    uiState: HomeCollectionUiState,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenRelated: (SongItem) -> Unit,
    onPlaySongs: (List<SongItem>, Int) -> Unit,
    onPlayNext: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
) {
    val metadata = uiState.metadata
    val title = metadata?.title ?: "Collection"
    val subtitle = metadata?.subtitle ?: "Made for exploring"
    val query = metadata?.query.orEmpty()
    val collectionLabel = collectionKindLabel(metadata?.collectionType)
    val trackSectionTitle = trackSectionTitle(metadata?.collectionType)
    val showArtistProfileSections = metadata?.collectionType == HomeCollectionType.ArtistMix

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OmniSpacing.large),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium),
    ) {
        item(contentType = "collection-header") {
            Column {
                Spacer(modifier = Modifier.statusBarsPadding())
                Spacer(modifier = Modifier.height(OmniSpacing.small))
                CollectionTopBar(
                    onBack = onBack,
                    onSearch = { if (query.isNotBlank()) onSearch(query) },
                )
                Spacer(modifier = Modifier.height(OmniSpacing.large))
                CollectionHeader(
                    title = title,
                    subtitle = subtitle,
                    collectionLabel = collectionLabel,
                    countLabel = uiState.countLabel,
                    artworkUrl = uiState.headerArtworkUrl,
                )
            }
        }

        item(contentType = "collection-actions") {
            CollectionActions(
                canPlay = uiState.canPlay,
                isLoading = uiState.isLoading,
                onPlay = { if (uiState.songs.isNotEmpty()) onPlaySongs(uiState.songs, 0) },
                onShuffle = {
                    if (uiState.songs.isNotEmpty()) onPlaySongs(uiState.songs.shuffled(), 0)
                },
                onSearch = { if (query.isNotBlank()) onSearch(query) },
                onRetry = onRetry,
            )
        }

        if (uiState.isLoading && uiState.songs.isEmpty()) {
            items(6, contentType = { "collection-loading" }) {
                CollectionSkeletonRow()
            }
        }

        uiState.error?.let { error ->
            item(contentType = "collection-error") {
                CollectionErrorCard(
                    text = error,
                    canSearch = query.isNotBlank(),
                    onRetry = onRetry,
                    onSearch = { onSearch(query) },
                )
            }
        }

        if (uiState.songs.isNotEmpty()) {
            if (showArtistProfileSections) {
                item(contentType = "artist-featured") {
                    ArtistFeaturedStrip(
                        songs = uiState.songs.take(4),
                        onSongClick = { song ->
                            val index = uiState.songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                            onPlaySongs(uiState.songs, index)
                        },
                    )
                }
            }

            item(contentType = "tracks-label") {
                Text(
                    text = trackSectionTitle,
                    style = OmniTextStyles.sectionHeader,
                    color = omniColors().textPrimary,
                )
            }
            itemsIndexed(
                items = uiState.songs,
                key = { index, song -> "collection_${song.id.ifBlank { index.toString() }}" },
                contentType = { _, _ -> "collection-track" },
            ) { index, song ->
                CollectionTrackRow(
                    song = song,
                    onClick = { onPlaySongs(uiState.songs, index) },
                    onPlayNext = { onPlayNext(song) },
                    onAddToQueue = { onAddToQueue(song) },
                    onMoreLikeThis = { onOpenRelated(song) },
                )
            }
        }

        item(contentType = "collection-bottom-spacer") {
            Spacer(modifier = Modifier.height(OmniChrome.BottomContentPaddingWithPlayer))
        }
    }
}

@Composable
private fun CollectionTopBar(
    onBack: () -> Unit,
    onSearch: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CollectionIconButton(icon = R.drawable.ic_arrow_back, contentDescription = "Back", onClick = onBack)
        Spacer(modifier = Modifier.weight(1f))
        CollectionIconButton(icon = R.drawable.ic_search, contentDescription = "Search", onClick = onSearch)
    }
}

@Composable
private fun CollectionHeader(
    title: String,
    subtitle: String,
    collectionLabel: String,
    countLabel: String,
    artworkUrl: String?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.ExtraLarge)
            .padding(OmniSpacing.large),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CollectionArtwork(
                thumbnailUrl = artworkUrl,
                title = title,
                modifier = Modifier.size(128.dp),
                imageSize = COLLECTION_ARTWORK_SIZE,
                shape = OmniShapes.ArtworkMedium,
            )
            Spacer(modifier = Modifier.width(OmniSpacing.large))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact)) {
                    CollectionMetaPill(text = collectionLabel)
                    CollectionMetaPill(text = countLabel)
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = omniColors().textPrimary,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = OmniTextStyles.metadata,
                    color = omniColors().textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CollectionMetaPill(text: String) {
    Box(
        modifier = Modifier
            .clip(OmniShapes.Pill)
            .background(omniColors().accentSecondary.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = omniColors().textPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CollectionActions(
    canPlay: Boolean,
    isLoading: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
) {
    var actionMenuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onPlay,
            enabled = canPlay,
            modifier = Modifier.weight(1f),
        ) {
            if (isLoading) {
                OmniWaveformLoader(modifier = Modifier.size(18.dp), size = 18.dp)
                Spacer(modifier = Modifier.width(OmniSpacing.compact))
            } else {
                Icon(painter = painterResource(R.drawable.ic_play_arrow), contentDescription = null)
                Spacer(modifier = Modifier.width(OmniSpacing.compact))
            }
            Text("Play")
        }
        CollectionIconButton(
            icon = R.drawable.ic_shuffle,
            contentDescription = "Shuffle",
            enabled = canPlay,
            onClick = onShuffle,
        )
        CollectionIconButton(
            icon = R.drawable.ic_search,
            contentDescription = "Search",
            onClick = onSearch,
        )
        Box {
            CollectionIconButton(
                icon = R.drawable.ic_more_vert,
                contentDescription = "More collection options",
                onClick = { actionMenuExpanded = true },
            )
            DropdownMenu(
                expanded = actionMenuExpanded,
                onDismissRequest = { actionMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Retry loading") },
                    onClick = {
                        actionMenuExpanded = false
                        onRetry()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Open Search") },
                    onClick = {
                        actionMenuExpanded = false
                        onSearch()
                    },
                )
            }
        }
    }
}

@Composable
private fun ArtistFeaturedStrip(
    songs: List<SongItem>,
    onSongClick: (SongItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        Text(
            text = "Featured",
            style = OmniTextStyles.sectionHeader,
            color = omniColors().textPrimary,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
            itemsIndexed(
                items = songs,
                key = { index, song -> "artist_featured_${song.id.ifBlank { index.toString() }}" },
                contentType = { _, _ -> "artist-featured-card" },
            ) { _, song ->
                ArtistFeaturedCard(
                    song = song,
                    onClick = { onSongClick(song) },
                )
            }
        }
    }
}

@Composable
private fun ArtistFeaturedCard(
    song: SongItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(148.dp)
            .clip(OmniShapes.Small)
            .clickable(onClick = onClick),
    ) {
        CollectionArtwork(
            thumbnailUrl = song.thumbnail,
            title = song.title,
            modifier = Modifier.size(148.dp),
            imageSize = TRACK_ARTWORK_SIZE,
            shape = OmniShapes.ArtworkSmall,
        )
        Spacer(modifier = Modifier.height(OmniSpacing.small))
        Text(
            text = song.title.ifBlank { "Unknown track" },
            style = OmniTextStyles.songTitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.artists.joinToString(", ") { it.name }.ifBlank { "Song" },
            style = OmniTextStyles.caption,
            color = omniColors().textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CollectionTrackRow(
    song: SongItem,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onMoreLikeThis: () -> Unit,
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    var menuExpanded by remember { mutableStateOf(false) }
    val subtitle = listOfNotNull(
        song.artists.joinToString(", ") { it.name }.ifBlank { null },
        song.duration?.let(::formatDurationSeconds),
    ).joinToString(" • ").ifBlank { "Song" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(OmniShapes.Medium)
            .background(omniColors().surface.copy(alpha = 0.42f))
            .omniPressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CollectionArtwork(
            thumbnailUrl = song.thumbnail,
            title = song.title,
            modifier = Modifier.size(52.dp),
            imageSize = TRACK_ARTWORK_SIZE,
            shape = OmniShapes.ArtworkSmall,
        )
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title.ifBlank { "Unknown track" },
                style = OmniTextStyles.songTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = OmniTextStyles.metadata,
                color = omniColors().textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = "More options",
                    tint = omniColors().textSecondary,
                )
            }
            TrackMenuProvider(
                showMenu = menuExpanded,
                onDismissMenu = { menuExpanded = false },
                mediaMetadata = song.toMediaMetadata(),
                onPlayNext = {
                    menuExpanded = false
                    onPlayNext()
                },
                onAddToQueue = {
                    menuExpanded = false
                    onAddToQueue()
                },
                onMoreLikeThis = {
                    menuExpanded = false
                    onMoreLikeThis()
                },
            )
        }
    }
}

@Composable
private fun CollectionErrorCard(
    text: String,
    canSearch: Boolean,
    onRetry: () -> Unit,
    onSearch: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Medium)
            .padding(OmniSpacing.large),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium),
    ) {
        Text(text, style = OmniTextStyles.metadata, color = omniColors().textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small)) {
            Button(onClick = onRetry) { Text("Retry") }
            if (canSearch) {
                Button(onClick = onSearch) { Text("Open Search") }
            }
        }
    }
}

@Composable
private fun CollectionSkeletonRow() {
    ShimmerTrackRow(artworkSize = 52.dp)
}

@Composable
private fun CollectionIconButton(
    icon: Int,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(42.dp)
            .clip(OmniShapes.Pill)
            .background(omniColors().surface.copy(alpha = if (enabled) 0.78f else 0.38f)),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (enabled) omniColors().textPrimary else omniColors().textTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun CollectionArtwork(
    thumbnailUrl: String?,
    title: String,
    modifier: Modifier,
    imageSize: Int,
    shape: androidx.compose.ui.graphics.Shape,
) {
    val context = LocalContext.current
    val model = remember(thumbnailUrl, imageSize) {
        thumbnailUrl?.takeIf { it.isNotBlank() }?.let {
            ImageRequest.Builder(context)
                .data(it)
                .size(imageSize, imageSize)
                .memoryCacheKey(it)
                .diskCacheKey(it)
                .crossfade(OmniMotion.ThumbnailFadeMillis)
                .build()
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(omniColors().surfaceQuiet),
        contentAlignment = Alignment.Center,
    ) {
        CollectionFallbackArtwork(Modifier.fillMaxSize())
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CollectionFallbackArtwork(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(
                    omniColors().surfaceQuiet,
                    omniColors().backgroundElevated,
                    omniColors().accentSecondary.copy(alpha = 0.06f),
                ),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_album),
            contentDescription = null,
            tint = omniColors().textSecondary.copy(alpha = 0.44f),
            modifier = Modifier.size(42.dp),
        )
    }
}

private fun formatDurationSeconds(durationSeconds: Int): String {
    if (durationSeconds <= 0) return ""
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun collectionKindLabel(type: HomeCollectionType?): String = when (type) {
    HomeCollectionType.ArtistMix -> "Artist mix"
    HomeCollectionType.Mood -> "Mood"
    HomeCollectionType.Genre -> "Genre"
    HomeCollectionType.Playlist -> "Playlist"
    HomeCollectionType.TrendingSearch -> "Trending"
    HomeCollectionType.QuickPick -> "Quick pick"
    HomeCollectionType.NewReleases -> "New music"
    HomeCollectionType.ForYou -> "For you"
    HomeCollectionType.Related -> "Related"
    null -> "Collection"
}

private fun trackSectionTitle(type: HomeCollectionType?): String = when (type) {
    HomeCollectionType.ArtistMix -> "Top songs"
    HomeCollectionType.Mood -> "Songs for this mood"
    HomeCollectionType.Genre -> "Songs in this genre"
    HomeCollectionType.TrendingSearch,
    HomeCollectionType.NewReleases -> "Fresh results"
    else -> "Tracks"
}
