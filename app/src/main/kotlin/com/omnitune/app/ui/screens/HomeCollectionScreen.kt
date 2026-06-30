/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.omnitune.app.ui.component.AccentPill
import com.omnitune.app.ui.component.GlassCard
import com.omnitune.app.ui.component.GlassSurface
import com.omnitune.app.ui.component.OmniWaveformLoader
import com.omnitune.app.ui.component.GlassTone
import com.omnitune.app.ui.component.OmniTrackLoadingRow
import com.omnitune.app.ui.component.TrackMenuProvider
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles
import com.omnitune.app.ui.theme.omniPressScale
import com.omnitune.innertube.models.SongItem

private const val COLLECTION_ARTWORK_SIZE = 544
private const val TRACK_ARTWORK_SIZE = 144
private const val COLLECTION_THUMBNAIL_CROSSFADE_MS = 180

private val CollectionArtworkPalettes = listOf(
    listOf(Color(0xFF15B8A6), Color(0xFF234E70)),
    listOf(Color(0xFFEF476F), Color(0xFF5B2A86)),
    listOf(Color(0xFFFFB703), Color(0xFF126782)),
    listOf(Color(0xFF80ED99), Color(0xFF22577A)),
    listOf(Color(0xFFF77F00), Color(0xFF6A040F)),
    listOf(Color(0xFF48CAE4), Color(0xFF3A0CA3)),
)

@Composable
fun HomeCollectionRoute(
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onPlaySongs: (List<SongItem>, Int) -> Unit,
    onPlayNext: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    viewModel: HomeCollectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    HomeCollectionScreen(
        uiState = uiState,
        onBack = onBack,
        onSearch = onSearch,
        onRetry = viewModel::retry,
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
    onPlaySongs: (List<SongItem>, Int) -> Unit,
    onPlayNext: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
) {
    val metadata = uiState.metadata
    val title = metadata?.title ?: "Collection"
    val subtitle = metadata?.subtitle ?: "Made for exploring"
    val query = metadata?.query.orEmpty()
    val artworkKey = metadata?.artworkKey ?: title
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
                    artworkKey = artworkKey,
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
                    style = OmniTextStyles.sectionTitle,
                    color = OmniColors.TextPrimary,
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
                )
            }
        }

        item(contentType = "collection-bottom-spacer") {
            Spacer(modifier = Modifier.height(OmniSpacing.screen))
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
    artworkKey: String,
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = OmniShapes.ExtraLarge,
        tone = GlassTone.Strong,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(OmniColors.OmniGlassMedium, OmniColors.OmniBackgroundBase.copy(alpha = 0.90f)),
                    ),
                )
                .padding(OmniSpacing.large),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CollectionArtwork(
                    thumbnailUrl = artworkUrl,
                    title = title,
                    artworkKey = artworkKey,
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
                        AccentPill(text = collectionLabel)
                        AccentPill(text = countLabel)
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = OmniColors.TextPrimary,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = OmniTextStyles.metadata,
                        color = OmniColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
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
            style = OmniTextStyles.sectionTitle,
            color = OmniColors.TextPrimary,
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
    GlassCard(
        modifier = Modifier.width(148.dp),
        onClick = onClick,
        cornerRadius = OmniShapes.Medium,
        tone = GlassTone.Subtle,
    ) {
        Column(modifier = Modifier.padding(OmniSpacing.small)) {
            CollectionArtwork(
                thumbnailUrl = song.thumbnail,
                title = song.title,
                artworkKey = song.id,
                modifier = Modifier.size(124.dp),
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
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CollectionTrackRow(
    song: SongItem,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    var menuExpanded by remember { mutableStateOf(false) }
    val subtitle = listOfNotNull(
        song.artists.joinToString(", ") { it.name }.ifBlank { null },
        song.duration?.let(::formatDurationSeconds),
    ).joinToString(" • ").ifBlank { "Song" }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .omniPressScale(interactionSource),
        onClick = onClick,
        cornerRadius = OmniShapes.Large,
        tone = GlassTone.Subtle,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OmniSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CollectionArtwork(
                thumbnailUrl = song.thumbnail,
                title = song.title,
                artworkKey = song.id,
                modifier = Modifier.size(58.dp),
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
                    color = OmniColors.TextSecondary,
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
                        tint = OmniColors.TextSecondary,
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
                )
            }
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
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = OmniShapes.Large,
        tone = GlassTone.Medium,
    ) {
        Column(
            modifier = Modifier.padding(OmniSpacing.large),
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium),
        ) {
            Text(text, style = OmniTextStyles.metadata, color = OmniColors.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small)) {
                Button(onClick = onRetry) { Text("Retry") }
                if (canSearch) {
                    Button(onClick = onSearch) { Text("Open Search") }
                }
            }
        }
    }
}

@Composable
private fun CollectionSkeletonRow() {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp),
        cornerRadius = OmniShapes.Large,
        tone = GlassTone.Subtle,
    ) {
        Row(
            modifier = Modifier.padding(OmniSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OmniTrackLoadingRow(artworkSize = 58.dp)
        }
    }
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
            .size(46.dp)
            .clip(OmniShapes.Pill)
            .background(OmniColors.OmniGlassMedium),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (enabled) OmniColors.TextPrimary else OmniColors.TextTertiary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun CollectionArtwork(
    thumbnailUrl: String?,
    title: String,
    artworkKey: String,
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
                .crossfade(COLLECTION_THUMBNAIL_CROSSFADE_MS)
                .build()
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(OmniColors.OmniGlassStrong),
        contentAlignment = Alignment.Center,
    ) {
        CollectionFallbackArtwork(title, artworkKey, Modifier.fillMaxSize())
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
    title: String,
    artworkKey: String,
    modifier: Modifier,
) {
    val colors = remember(artworkKey) {
        CollectionArtworkPalettes[kotlin.math.abs(artworkKey.hashCode()) % CollectionArtworkPalettes.size]
    }
    val label = remember(title) {
        title.split(" ").filter { it.isNotBlank() }.take(2).joinToString(" ").ifBlank { "OmniTune" }
    }
    Box(modifier = modifier.background(Brush.linearGradient(colors))) {
        Icon(
            painter = painterResource(R.drawable.ic_album),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.22f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(72.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(OmniSpacing.small),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
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
