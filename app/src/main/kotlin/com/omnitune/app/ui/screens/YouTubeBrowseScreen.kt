/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.R
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.playback.continuation.PlaybackContext
import com.omnitune.app.playback.continuation.PlaybackSourceType
import com.omnitune.app.playback.queues.ListQueue
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.component.OmniSectionHeader
import com.omnitune.app.ui.component.shimmer.ShimmerGridItem
import com.omnitune.app.ui.component.shimmer.ShimmerHost
import com.omnitune.app.ui.theme.LocalOmniAccents
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles
import com.omnitune.app.utils.classifyProviderError
import com.omnitune.app.utils.reportException
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.AlbumItem
import com.omnitune.innertube.models.ArtistItem
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.models.SongItem
import com.omnitune.innertube.models.YTItem
import com.omnitune.innertube.pages.BrowseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class YouTubeBrowseUiState(
    val title: String = "Browse",
    val sections: List<BrowseResult.Item> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class YouTubeBrowseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val browseId: String = Uri.decode(checkNotNull(savedStateHandle["browseId"]))
    private val params: String? = savedStateHandle.get<String?>("params")?.let(Uri::decode)?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(YouTubeBrowseUiState())
    val uiState: StateFlow<YouTubeBrowseUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                withTimeout(25_000L) {
                    YouTube.browse(browseId, params).getOrThrow()
                }
            }
                .onSuccess { result ->
                    val sections = result.items.filter { it.items.isNotEmpty() }
                    _uiState.update {
                        it.copy(
                            title = result.title?.takeIf { title -> title.isNotBlank() } ?: "Browse",
                            sections = sections,
                            isLoading = false,
                            error = if (sections.isEmpty()) "No browse shelves found." else null,
                        )
                    }
                }
                .onFailure { throwable ->
                    reportException(throwable)
                    val providerError = classifyProviderError(throwable)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = providerError.message,
                        )
                    }
                }
        }
    }
}

@Composable
fun YouTubeBrowseScreen(
    navController: NavController,
    viewModel: YouTubeBrowseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerConnection = LocalPlayerConnection.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.BackgroundGradient),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = OmniSpacing.large,
                end = OmniSpacing.large,
                bottom = OmniChrome.BottomContentPaddingWithPlayer,
            ),
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.large),
        ) {
            item(contentType = "header") {
                YouTubeBrowseHeader(
                    title = uiState.title,
                    onBack = { navController.popBackStack() },
                )
            }

            if (uiState.isLoading) {
                item(contentType = "loading") {
                    BrowseLoading()
                }
            }

            uiState.error?.let { error ->
                item(contentType = "error") {
                    BrowseError(message = error, onRetry = viewModel::retry)
                }
            }

            uiState.sections.forEachIndexed { sectionIndex, section ->
                item(key = "section_${sectionIndex}_${section.title}", contentType = "browse-section") {
                    BrowseShelf(
                        section = section,
                        onItemClick = { item ->
                            when (item) {
                                is SongItem -> {
                                    val songs = section.items.filterIsInstance<SongItem>()
                                    val index = songs.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                                    val mediaItems = songs.map { it.toMediaItem() }
                                    playerConnection?.playQueue(
                                        ListQueue(
                                            title = section.title ?: uiState.title,
                                            items = mediaItems,
                                            startIndex = index,
                                            playbackContext = PlaybackContext(
                                                sourceType = PlaybackSourceType.HOME_DISCOVERY,
                                                sourceTitle = section.title ?: uiState.title,
                                                seedSongId = item.id,
                                                artist = item.artists.firstOrNull()?.name,
                                                sessionItems = mediaItems,
                                            ),
                                        ),
                                    )
                                }
                                is AlbumItem -> navController.navigate("album/${item.browseId}")
                                is ArtistItem -> navController.navigate("artist/${item.id}")
                                is PlaylistItem -> {
                                    val collectionId = HomeDefaultCatalog.providerCollectionId(
                                        kind = "playlist",
                                        providerId = item.id,
                                        title = item.title,
                                        subtitle = item.author?.name ?: item.songCountText ?: "Playlist",
                                    )
                                    navController.navigate("homeCollection/${Uri.encode(collectionId)}")
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun YouTubeBrowseHeader(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = OmniSpacing.compact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back",
                tint = OmniColors.TextPrimary,
            )
        }
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "YouTube Music browse",
                style = OmniTextStyles.metadata,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BrowseShelf(
    section: BrowseResult.Item,
    onItemClick: (YTItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        OmniSectionHeader(title = section.title?.takeIf { it.isNotBlank() } ?: "Featured")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(OmniSpacing.large),
            contentPadding = PaddingValues(end = OmniSpacing.large),
        ) {
            items(section.items, key = { "${it::class.simpleName}_${it.id}" }) { item ->
                BrowseCard(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
private fun BrowseCard(
    item: YTItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(152.dp)
            .clip(OmniShapes.Medium)
            .background(OmniColors.SurfaceSubtle.copy(alpha = 0.24f))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(OmniShapes.ArtworkSmall)
                .background(OmniColors.SurfaceQuiet),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = item.thumbnail,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (item is SongItem) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(OmniSpacing.small)
                        .size(34.dp)
                        .clip(OmniShapes.Pill)
                        .background(LocalOmniAccents.current.secondary.copy(alpha = 0.90f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_arrow),
                        contentDescription = null,
                        tint = OmniColors.TextOnAccent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Column(
            modifier = Modifier.padding(
                top = OmniSpacing.small,
                start = OmniSpacing.micro,
                end = OmniSpacing.micro,
                bottom = OmniSpacing.micro,
            ),
        ) {
            Text(
                text = item.title,
                style = OmniTextStyles.songTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = item.subtitleText(),
                style = OmniTextStyles.metadata,
                color = OmniColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BrowseLoading() {
    ShimmerHost {
        repeat(4) {
            Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.small)) {
                Row(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
                    repeat(3) {
                        ShimmerGridItem(width = 132.dp)
                    }
                }
                Spacer(modifier = Modifier.height(OmniSpacing.small))
            }
        }
    }
}

@Composable
private fun BrowseError(
    message: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Medium)
            .background(OmniColors.Warning.copy(alpha = 0.10f))
            .border(1.dp, OmniColors.Warning.copy(alpha = 0.16f), OmniShapes.Medium)
            .clickable(onClick = onRetry)
            .padding(OmniSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
            tint = OmniColors.Warning,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Couldn't load this category",
                style = OmniTextStyles.songTitle,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$message Tap to retry.",
                style = OmniTextStyles.metadata,
                color = OmniColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun YTItem.subtitleText(): String = when (this) {
    is SongItem -> artists.joinToString(", ") { it.name }.ifBlank { "Song" }
    is AlbumItem -> artists?.joinToString(", ") { it.name }.orEmpty().ifBlank { year?.toString() ?: "Album" }
    is PlaylistItem -> author?.name ?: songCountText ?: "Playlist"
    is ArtistItem -> "Artist"
}
