/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
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
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.R
import com.omnitune.app.constants.HasPressedStarKey
import com.omnitune.app.constants.LaunchCountKey
import com.omnitune.app.constants.SupportDialogDismissedKey
import com.omnitune.app.constants.SupportDialogSnoozedUntilKey
import com.omnitune.app.db.entities.EventWithSong
import com.omnitune.app.db.entities.Song
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.component.OmniSectionHeader
import com.omnitune.app.ui.component.shimmer.ShimmerHost
import com.omnitune.app.ui.component.shimmer.ShimmerHeroBanner
import com.omnitune.app.ui.component.shimmer.ShimmerTrackRow
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniMotion
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles
import com.omnitune.app.utils.dataStore
import com.omnitune.innertube.models.SongItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private const val HERO_IMAGE_SIZE = 480
private const val SHELF_IMAGE_SIZE = 160
private const val SUPPORT_REPO_URL = "https://github.com/soupashh-ship-it/OmniTune"
private const val SUPPORT_PROMPT_LAUNCH_THRESHOLD = 3
private const val SUPPORT_SNOOZE_DAYS = 5L

@Composable
fun HomeDiscoveryRoute(
    onNavigateToSearch: () -> Unit,
    onNavigateToCollection: (String, String?) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAllGenres: (() -> Unit)? = null,
    onResumePlayback: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlayProviderSong: (SongItem) -> Unit,
    onPlayProviderSongs: (List<SongItem>) -> Unit = {},
    onPlaySongs: (List<Song>) -> Unit,
    viewModel: HomeDiscoveryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by (playerConnection?.mediaMetadata ?: flowOf(null)).collectAsState(initial = null)
    val hasProviderFeed = uiState.providerSections.isNotEmpty() ||
        uiState.communitySections.isNotEmpty() ||
        uiState.exploreSections.isNotEmpty()
    val onShowAllGenres = remember(uiState.genreChips, onNavigateToAllGenres) {
        if (onNavigateToAllGenres != null) {
            {
                GenreChipsHolder.chips = uiState.genreChips
                onNavigateToAllGenres()
            }
        } else null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = OmniSpacing.large),
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.large),
        ) {
            item(contentType = "header") {
                Column {
                    Spacer(modifier = Modifier.statusBarsPadding())
                    Spacer(modifier = Modifier.height(OmniSpacing.small))
                    HomeTopHeader(
                        onSearch = onNavigateToSearch,
                        onSettings = onNavigateToSettings,
                    )
                }
            }

            item(contentType = "mood-chips") {
                MoodChipRow(chips = uiState.moodChips, onChipClick = { chip -> onNavigateToCollection(chip.id, null) })
            }

            item(contentType = "hero") {
                val heroItems = uiState.carouselItems.filter { it.canLeadHero() }
                if (heroItems.isNotEmpty() || uiState.isLoading || uiState.isProviderLoading) {
                    HeroCarousel(
                        items = heroItems,
                        isLoading = uiState.isLoading || (uiState.isProviderLoading && heroItems.isEmpty()),
                        onPlaySong = onPlaySong,
                        onPlayProviderSong = onPlayProviderSong,
                        onOpenCollection = onNavigateToCollection,
                        onRequestHydration = viewModel::requestThumbnailHydration,
                    )
                }
            }


            if (uiState.providerError != null && !uiState.isProviderLoading) {
                item(key = "provider-error", contentType = "error") {
                    ProviderErrorCard(
                        message = uiState.providerError.orEmpty(),
                    )
                }
            }

            item(contentType = "quick-picks") {
                val localSongs = uiState.quickPicks.mapNotNull { it.song }
                val providerSongs = uiState.quickPicks.mapNotNull { it.providerSong }
                QuickPicksSection(
                    items = uiState.quickPicks,
                    isLoading = uiState.isLoading,
                    onPlayAll = {
                        if (providerSongs.isNotEmpty()) {
                            onPlayProviderSongs(providerSongs)
                        } else if (localSongs.isNotEmpty()) {
                            onPlaySongs(localSongs)
                        }
                    },
                    onPlaySong = onPlaySong,
                    onPlayProviderSong = onPlayProviderSong,
                    onOpenCollection = onNavigateToCollection,
                    onRequestHydration = viewModel::requestThumbnailHydration,
                )
            }

            uiState.personalizedSections.forEach { section ->
                item(key = "personalized_${section.id}", contentType = "personalized-shelf") {
                    HorizontalDiscoveryShelf(
                        section = section,
                        onItemClick = { item ->
                            handleShelfItemClick(item, onPlaySong, onPlayProviderSong, onNavigateToCollection)
                        },
                        onRequestHydration = viewModel::requestThumbnailHydration,
                    )
                }
            }


            if (uiState.quickPicks.isEmpty() && !hasProviderFeed && !uiState.isProviderLoading) {
                item(contentType = "browse-start") {
                    HorizontalDiscoveryShelf(
                        section = uiState.searchSection,
                        onItemClick = { item -> handleShelfItemClick(item, onPlaySong, onPlayProviderSong, onNavigateToCollection) },
                        onRequestHydration = viewModel::requestThumbnailHydration,
                    )
                }
            }

            uiState.providerSections.forEach { section ->
                item(key = "provider_${section.id}", contentType = "provider-shelf") {
                    HorizontalDiscoveryShelf(
                        section = section,
                        onItemClick = { item -> handleShelfItemClick(item, onPlaySong, onPlayProviderSong, onNavigateToCollection) },
                        onRequestHydration = viewModel::requestThumbnailHydration,
                    )
                }
            }

            if (uiState.communitySections.isNotEmpty()) {
                uiState.communitySections.forEach { section ->
                    item(key = "community_${section.id}", contentType = "community-shelf") {
                        HorizontalDiscoveryShelf(
                            section = section,
                            onItemClick = { item -> handleShelfItemClick(item, onPlaySong, onPlayProviderSong, onNavigateToCollection) },
                            onRequestHydration = viewModel::requestThumbnailHydration,
                        )
                    }
                }
            }

            uiState.exploreSections.forEach { section ->
                item(key = "explore_${section.id}", contentType = "explore-shelf") {
                    HorizontalDiscoveryShelf(
                        section = section,
                        onItemClick = { item -> handleShelfItemClick(item, onPlaySong, onPlayProviderSong, onNavigateToCollection) },
                        onRequestHydration = viewModel::requestThumbnailHydration,
                    )
                }
            }

            if (!hasProviderFeed && !uiState.isProviderLoading) {
                item(contentType = "fallback-discovery") {
                    HorizontalDiscoveryShelf(
                        section = uiState.searchSection,
                        onItemClick = { item -> handleShelfItemClick(item, onPlaySong, onPlayProviderSong, onNavigateToCollection) },
                        onRequestHydration = viewModel::requestThumbnailHydration,
                    )
                }
            }

            if (!hasProviderFeed && !uiState.isProviderLoading) {
                uiState.shelfSections.forEach { section ->
                    item(key = "shelf_${section.id}", contentType = "horizontal-shelf") {
                        HorizontalDiscoveryShelf(
                            section = section,
                            onItemClick = { item -> handleShelfItemClick(item, onPlaySong, onPlayProviderSong, onNavigateToCollection) },
                            onRequestHydration = viewModel::requestThumbnailHydration,
                        )
                    }
                }
            }

            item(contentType = "mood-grid") {
                MoodGenreGrid(chips = uiState.genreChips, onChipClick = { chip -> onNavigateToCollection(chip.id, null) }, onShowAll = onShowAllGenres)
            }

            if (uiState.downloadSection.items.isNotEmpty()) {
                item(contentType = "downloads") {
                    DiscoveryShelf(
                        section = uiState.downloadSection,
                        emptyLabel = "",
                        onAction = onNavigateToDownloads,
                        onItemClick = { item -> item.song?.let(onPlaySong) },
                        onRequestHydration = viewModel::requestThumbnailHydration,
                    )
                }
            }

            if (uiState.librarySection.items.isNotEmpty()) {
                item(contentType = "library") {
                    DiscoveryShelf(
                        section = uiState.librarySection,
                        emptyLabel = "",
                        onAction = onNavigateToLibrary,
                        onItemClick = { item -> item.song?.let(onPlaySong) },
                        onRequestHydration = viewModel::requestThumbnailHydration,
                    )
                }
            }

            item(contentType = "bottom-spacer") {
                Spacer(modifier = Modifier.height(OmniChrome.BottomContentPaddingWithPlayer))
            }
        }

        SupportDevelopmentDialog()
    }
}

@Composable
private fun HomeTopHeader(
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left: compact logo mark + OmniTune title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
        ) {
            // Signal bars logo mark
            Row(
                modifier = Modifier
                    .clip(OmniShapes.Small)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                OmniColors.OmniAccentSecondary.copy(alpha = 0.18f),
                                OmniColors.OmniAccentPrimary.copy(alpha = 0.14f),
                            )
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                val heights = listOf(8.dp, 14.dp, 10.dp, 18.dp)
                heights.forEachIndexed { index, height ->
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(height)
                            .clip(OmniShapes.Pill)
                            .background(
                                if (index == 3) OmniColors.OmniAccentSecondary else OmniColors.OmniAccentPrimary
                            )
                    )
                }
            }
            Text(
                text = "OmniTune",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Right: action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderIconButton(icon = R.drawable.ic_search, contentDescription = "Search", onClick = onSearch)
            HeaderIconButton(icon = R.drawable.ic_settings, contentDescription = "Settings", onClick = onSettings)
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(OmniShapes.Pill)
            .background(OmniColors.SurfaceSubtle)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = OmniColors.TextSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun MoodChipRow(
    chips: List<MoodChip>,
    onChipClick: (MoodChip) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small)) {
        items(chips, key = { it.id }, contentType = { "mood-chip" }) { chip ->
            FeedChip(
                label = chip.label,
                onClick = { onChipClick(chip) },
            )
        }
    }
}

@Composable
private fun FeedChip(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(OmniShapes.Pill)
            .background(OmniColors.SurfaceRaised)
            .border(
                width = 1.dp,
                color = OmniColors.SurfaceHairline,
                shape = OmniShapes.Pill,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = OmniSpacing.medium),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HeroCarousel(
    items: List<HomeCarouselItem>,
    isLoading: Boolean,
    onPlaySong: (Song) -> Unit,
    onPlayProviderSong: (SongItem) -> Unit,
    onOpenCollection: (String, String?) -> Unit,
    onRequestHydration: (HomeThumbnailRequest) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.small)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
            if (isLoading) {
                items(3, contentType = { "hero-loading" }) { HeroSkeleton() }
            } else {
                items(items, key = { it.id }, contentType = { "hero" }) { item ->
                    HeroCard(
                        item = item,
                        onRequestHydration = onRequestHydration,
                        onClick = {
                            item.song?.let(onPlaySong)
                                ?: item.providerSong?.let(onPlayProviderSong)
                                ?: onOpenCollection(item.id, item.thumbnailUrl)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    item: HomeCarouselItem,
    onRequestHydration: (HomeThumbnailRequest) -> Unit,
    onClick: () -> Unit,
) {
    RequestHydrationEffect(
        id = item.id,
        query = item.query,
        source = item.source,
        thumbnailUrl = item.thumbnailUrl,
        state = item.hydrationState,
        collage = false,
        onRequestHydration = onRequestHydration,
    )

    Box(
        modifier = Modifier
            .width(286.dp)
            .aspectRatio(1f)
            .clip(OmniShapes.ArtworkLarge)
            .clickable(onClick = onClick),
    ) {
        DiscoveryArtwork(
            thumbnailUrl = item.thumbnailUrl,
            contentDescription = item.title,
            title = item.title,
            artworkKey = item.artworkKey ?: item.id,
            modifier = Modifier.fillMaxSize(),
            imageSize = HERO_IMAGE_SIZE,
            shape = OmniShapes.ArtworkLarge,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, OmniColors.OmniBackgroundBase.copy(alpha = 0.88f)),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(OmniSpacing.large),
        ) {
            Text(
                text = if (item.song != null || item.providerSong != null) "Play" else "Open",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = OmniColors.OmniAccentSecondary,
                modifier = Modifier
                    .clip(OmniShapes.Pill)
                    .background(OmniColors.OmniAccentSecondary.copy(alpha = 0.12f))
                    .padding(horizontal = OmniSpacing.small, vertical = OmniSpacing.micro),
            )
            Spacer(modifier = Modifier.height(OmniSpacing.small))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                color = OmniColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle,
                style = OmniTextStyles.metadata,
                color = OmniColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HeroSkeleton() {
    ShimmerHeroBanner(
        modifier = Modifier.width(286.dp),
    )
}


@Composable
private fun QuickPicksSection(
    items: List<QuickPickItem>,
    isLoading: Boolean,
    onPlayAll: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlayProviderSong: (SongItem) -> Unit,
    onOpenCollection: (String, String?) -> Unit,
    onRequestHydration: (HomeThumbnailRequest) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Quick Picks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (items.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(OmniShapes.Pill)
                        .background(OmniColors.OmniAccentSecondary.copy(alpha = 0.12f))
                        .clickable(onClick = onPlayAll),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_arrow),
                        contentDescription = "Play all",
                        tint = OmniColors.OmniAccentSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        if (isLoading) {
            repeat(4) { ShelfSkeletonRow() }
        } else if (items.isEmpty()) {
            StartExploringRow(onClick = { onOpenCollection(HomeDefaultCatalog.freshDiscovery.items.first().id, null) })
        } else {
            val pageCount = (items.size + 4) / 5
            val pagerState = rememberPagerState(pageCount = { pageCount })

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) { page ->
                val fromIndex = page * 5
                val toIndex = minOf(fromIndex + 5, items.size)
                Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
                    items.subList(fromIndex, toIndex).forEach { item ->
                        QuickPickRow(
                            item = item,
                            onRequestHydration = onRequestHydration,
                            onClick = {
                                item.song?.let(onPlaySong)
                                    ?: item.providerSong?.let(onPlayProviderSong)
                                    ?: onOpenCollection(item.id, item.thumbnailUrl)
                            },
                        )
                    }
                }
            }

            if (pageCount > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(pageCount) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                                .clip(OmniShapes.Pill)
                                .background(
                                    if (pagerState.currentPage == index)
                                        OmniColors.OmniAccentSecondary
                                    else
                                        OmniColors.TextTertiary.copy(alpha = 0.4f)
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPickRow(
    item: QuickPickItem,
    onRequestHydration: (HomeThumbnailRequest) -> Unit,
    onClick: () -> Unit,
) {
    RequestHydrationEffect(
        id = item.id,
        query = item.query,
        source = item.source,
        thumbnailUrl = item.thumbnailUrl,
        state = item.hydrationState,
        collage = false,
        onRequestHydration = onRequestHydration,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(OmniShapes.Small)
            .clickable(onClick = onClick)
            .padding(horizontal = OmniSpacing.compact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DiscoveryArtwork(
            thumbnailUrl = item.thumbnailUrl,
            contentDescription = item.title,
            title = item.title,
            artworkKey = item.artworkKey ?: item.id,
            modifier = Modifier.size(54.dp),
            imageSize = SHELF_IMAGE_SIZE,
            shape = OmniShapes.ArtworkSmall,
        )
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = OmniTextStyles.songTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.subtitle, style = OmniTextStyles.metadata, color = OmniColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            painter = painterResource(R.drawable.ic_more_vert),
            contentDescription = null,
            tint = OmniColors.TextSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}


@Composable
private fun DiscoveryShelf(
    section: HomeSection,
    emptyLabel: String,
    onAction: () -> Unit,
    onItemClick: (PlaylistShelfItem) -> Unit,
    onRequestHydration: (HomeThumbnailRequest) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        OmniSectionHeader(title = section.title, action = section.actionLabel, onAction = section.actionLabel?.let { onAction })
        if (section.items.isEmpty()) {
            if (emptyLabel.isNotBlank()) EmptyDiscoveryCard(text = emptyLabel, action = "Search", onClick = onAction)
        } else {
            section.items.take(6).forEach { item ->
                SongShelfRow(
                    item = item,
                    onRequestHydration = onRequestHydration,
                    onClick = { onItemClick(item) },
                )
            }
        }
    }
}

@Composable
private fun SongShelfRow(
    item: PlaylistShelfItem,
    onRequestHydration: (HomeThumbnailRequest) -> Unit = {},
    onClick: () -> Unit,
) {
    RequestHydrationEffect(
        id = item.id,
        query = item.query,
        source = item.source,
        thumbnailUrl = item.thumbnailUrl,
        state = item.hydrationState,
        collage = false,
        onRequestHydration = onRequestHydration,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(OmniShapes.Small)
            .clickable(onClick = onClick)
            .padding(horizontal = OmniSpacing.compact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DiscoveryArtwork(
            thumbnailUrl = item.thumbnailUrl,
            contentDescription = item.title,
            title = item.title,
            artworkKey = item.artworkKey ?: item.id,
            modifier = Modifier.size(54.dp),
            imageSize = SHELF_IMAGE_SIZE,
            shape = OmniShapes.ArtworkSmall,
        )
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = OmniTextStyles.songTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.subtitle, style = OmniTextStyles.metadata, color = OmniColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            painter = painterResource(actionIconFor(item.actionType)),
            contentDescription = null,
            tint = OmniColors.TextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun HorizontalDiscoveryShelf(
    section: HomeSection,
    onItemClick: (PlaylistShelfItem) -> Unit,
    onRequestHydration: (HomeThumbnailRequest) -> Unit,
) {
    if (!section.hasImageFeedContent()) {
        TextDiscoveryShelf(section = section, onItemClick = onItemClick)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        FeedSectionHeader(title = section.title, action = section.actionLabel)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(OmniSpacing.large),
            contentPadding = PaddingValues(end = OmniSpacing.large),
        ) {
            items(section.items, key = { it.id }, contentType = { "shelf-card" }) { item ->
                ShelfArtworkCard(
                    item = item,
                    onRequestHydration = onRequestHydration,
                    onClick = { onItemClick(item) },
                )
            }
        }
    }
}

@Composable
private fun TextDiscoveryShelf(
    section: HomeSection,
    onItemClick: (PlaylistShelfItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        FeedSectionHeader(title = section.title, action = section.actionLabel)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            contentPadding = PaddingValues(end = OmniSpacing.large),
        ) {
            items(section.items.take(8), key = { it.id }, contentType = { "text-shelf-card" }) { item ->
                Column(
                    modifier = Modifier
                        .width(176.dp)
                        .height(72.dp)
                        .clip(OmniShapes.Small)
                        .clickable { onItemClick(item) }
                        .padding(OmniSpacing.small),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(item.title, style = OmniTextStyles.songTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(OmniSpacing.micro))
                    Text(item.subtitle, style = OmniTextStyles.caption, color = OmniColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ShelfArtworkCard(
    item: PlaylistShelfItem,
    onRequestHydration: (HomeThumbnailRequest) -> Unit,
    onClick: () -> Unit,
) {
    RequestHydrationEffect(
        id = item.id,
        query = item.query,
        source = item.source,
        thumbnailUrl = item.thumbnailUrl,
        state = item.hydrationState,
        collage = true,
        onRequestHydration = onRequestHydration,
    )

    Column(
        modifier = Modifier
            .width(144.dp)
            .clip(OmniShapes.Small)
            .clickable(onClick = onClick),
    ) {
        if (item.thumbnailUrls.size >= 2) {
            CollageArtwork(
                thumbnailUrls = item.thumbnailUrls.ifEmpty { listOfNotNull(item.thumbnailUrl) },
                contentDescription = item.title,
                title = item.title,
                artworkKey = item.artworkKey ?: item.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                imageSize = SHELF_IMAGE_SIZE,
                shape = OmniShapes.ArtworkSmall,
            )
        } else {
            DiscoveryArtwork(
                thumbnailUrl = item.thumbnailUrl,
                contentDescription = item.title,
                title = item.title,
                artworkKey = item.artworkKey ?: item.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                imageSize = SHELF_IMAGE_SIZE,
                shape = OmniShapes.ArtworkSmall,
            )
        }
        Spacer(modifier = Modifier.height(OmniSpacing.small))
        Text(item.title, style = OmniTextStyles.songTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(item.subtitle, style = OmniTextStyles.metadata, color = OmniColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MoodGenreGrid(
    chips: List<MoodChip>,
    onChipClick: (MoodChip) -> Unit,
    onShowAll: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        OmniSectionHeader(
            title = "Mood and Genres",
            action = if (chips.size > 6 && onShowAll != null) "Show all" else null,
            onAction = { onShowAll?.invoke() },
        )
        chips.take(6).chunked(2).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small), modifier = Modifier.fillMaxWidth()) {
                row.forEachIndexed { columnIndex, chip ->
                    MoodGenreCard(
                        chip = chip,
                        index = rowIndex * 2 + columnIndex,
                        onClick = { onChipClick(chip) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MoodGenreCard(
    chip: MoodChip,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accents = listOf(
        OmniColors.OmniAccentSecondary,
        OmniColors.OmniAccentPrimary,
        OmniColors.OmniAccentTertiary,
        OmniColors.Hot,
        OmniColors.Warning,
    )
    val accent = accents[index % accents.size]
    val icons = listOf(
        R.drawable.ic_play_arrow,
        R.drawable.ic_album,
        R.drawable.ic_history,
        R.drawable.ic_search,
        R.drawable.ic_list,
    )
    val icon = icons[index % icons.size]

    Row(
        modifier = modifier
            .heightIn(min = 62.dp)
            .clip(OmniShapes.Medium)
            .background(
                Brush.linearGradient(
                    listOf(
                        OmniColors.SurfaceSubtle.copy(alpha = 0.80f),
                        accent.copy(alpha = 0.08f),
                    ),
                ),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(OmniShapes.Pill)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chip.label,
                style = MaterialTheme.typography.titleMedium,
                color = OmniColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Browse",
                style = OmniTextStyles.caption,
                color = OmniColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyDiscoveryCard(
    text: String,
    action: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Medium)
            .background(OmniColors.SurfaceQuiet)
            .clickable(onClick = onClick)
            .padding(OmniSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = OmniTextStyles.metadata, color = OmniColors.TextSecondary, modifier = Modifier.weight(1f))
        Text(action, style = MaterialTheme.typography.labelLarge, color = OmniColors.OmniAccentSecondary)
    }
}

@Composable
private fun StartExploringRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(OmniShapes.Medium)
            .background(OmniColors.SurfaceQuiet.copy(alpha = 0.62f))
            .clickable(onClick = onClick)
            .padding(horizontal = OmniSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Start exploring to build real playable Quick Picks.",
            style = OmniTextStyles.metadata,
            color = OmniColors.TextSecondary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Browse",
            style = MaterialTheme.typography.labelLarge,
            color = OmniColors.OmniAccentSecondary,
        )
    }
}

@Composable
private fun ProviderErrorCard(
    message: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Medium)
            .background(OmniColors.Warning.copy(alpha = 0.10f))
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
        Text(
            text = message,
            style = OmniTextStyles.metadata,
            color = OmniColors.TextPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ShelfSkeletonRow() {
    ShimmerTrackRow()
}

@Composable
private fun RequestHydrationEffect(
    id: String,
    query: String?,
    source: HomeCatalogSource,
    thumbnailUrl: String?,
    state: HomeHydrationState,
    collage: Boolean,
    onRequestHydration: (HomeThumbnailRequest) -> Unit,
) {
    LaunchedEffect(id, query, source, thumbnailUrl, state, collage) {
        if (
            (source == HomeCatalogSource.Recommended || source == HomeCatalogSource.ProviderBrowse) &&
            !query.isNullOrBlank() &&
            thumbnailUrl.isNullOrBlank() &&
            state == HomeHydrationState.None
        ) {
            onRequestHydration(HomeThumbnailRequest(id = id, query = query, collage = collage))
        }
    }
}

private fun handleShelfItemClick(
    item: PlaylistShelfItem,
    onPlaySong: (Song) -> Unit,
    onPlayProviderSong: (SongItem) -> Unit,
    onOpenCollection: (String, String?) -> Unit,
) {
    item.song?.let(onPlaySong)
        ?: item.providerSong?.let(onPlayProviderSong)
        ?: onOpenCollection(item.id, item.thumbnailUrl)
}

private fun actionIconFor(actionType: HomeActionType): Int = when (actionType) {
    HomeActionType.PLAY_TRACK -> R.drawable.ic_play_arrow
    HomeActionType.OPEN_SEARCH_ONLY_WHEN_EXPLICIT -> R.drawable.ic_search
    HomeActionType.OPEN_ARTIST -> R.drawable.ic_artist
    HomeActionType.OPEN_ALBUM -> R.drawable.ic_album
    HomeActionType.OPEN_BROWSE,
    HomeActionType.OPEN_COLLECTION,
    HomeActionType.OPEN_PLAYLIST -> R.drawable.ic_album
}

private fun HomeCarouselItem.canLeadHero(): Boolean =
    song != null ||
        providerSong != null ||
        source == HomeCatalogSource.ProviderBrowse ||
        !thumbnailUrl.isNullOrBlank() ||
        thumbnailUrls.isNotEmpty()

private fun HomeSection.hasImageFeedContent(): Boolean =
    items.any { item ->
        item.song != null ||
            item.providerSong != null ||
            item.source == HomeCatalogSource.ProviderBrowse ||
            !item.thumbnailUrl.isNullOrBlank() ||
            item.thumbnailUrls.isNotEmpty()
    }

@Composable
private fun DiscoveryArtwork(
    thumbnailUrl: String?,
    contentDescription: String?,
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
                .crossfade(OmniMotion.ThumbnailFadeMillis)
                .build()
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(OmniColors.SurfaceQuiet),
        contentAlignment = Alignment.Center,
    ) {
        StaticArtworkPlaceholder(modifier = Modifier.fillMaxSize())
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CollageArtwork(
    thumbnailUrls: List<String>,
    contentDescription: String?,
    title: String,
    artworkKey: String,
    modifier: Modifier,
    imageSize: Int,
    shape: androidx.compose.ui.graphics.Shape,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(OmniColors.SurfaceQuiet),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            repeat(2) { row ->
                Row(modifier = Modifier.weight(1f)) {
                    repeat(2) { column ->
                        val index = row * 2 + column
                        CollageTile(
                            thumbnailUrl = thumbnailUrls.getOrNull(index),
                            contentDescription = contentDescription,
                            title = title,
                            artworkKey = "${artworkKey}_$index",
                            imageSize = imageSize,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, OmniColors.OmniBackgroundBase.copy(alpha = 0.28f)),
                    ),
                ),
        )
    }
}

@Composable
private fun CollageTile(
    thumbnailUrl: String?,
    contentDescription: String?,
    title: String,
    artworkKey: String,
    imageSize: Int,
    modifier: Modifier,
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

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        StaticArtworkPlaceholder(modifier = Modifier.fillMaxSize())
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun StaticArtworkPlaceholder(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(
                    OmniColors.SurfaceQuiet,
                    OmniColors.OmniBackgroundElevated,
                    OmniColors.OmniAccentSecondary.copy(alpha = 0.10f),
                ),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_album),
            contentDescription = null,
            tint = OmniColors.TextSecondary.copy(alpha = 0.44f),
            modifier = Modifier.size(42.dp),
        )
    }
}

@Composable
private fun FeedSectionHeader(
    title: String,
    action: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = OmniTextStyles.sectionTitle,
            color = OmniColors.TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (action != null) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = null,
                tint = OmniColors.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SupportDevelopmentDialog() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val prefs = context.dataStore.data.first()
        val launchCount = (prefs[LaunchCountKey] ?: 0) + 1
        val dismissed = prefs[SupportDialogDismissedKey] ?: false
        val hasPressedStar = prefs[HasPressedStarKey] ?: false
        val snoozedUntil = prefs[SupportDialogSnoozedUntilKey] ?: 0L

        context.dataStore.edit { mutablePrefs ->
            mutablePrefs[LaunchCountKey] = launchCount
        }

        showDialog = !dismissed &&
            !hasPressedStar &&
            launchCount >= SUPPORT_PROMPT_LAUNCH_THRESHOLD &&
            now >= snoozedUntil
    }

    if (!showDialog) return

    fun snooze() {
        showDialog = false
        coroutineScope.launch {
            context.dataStore.edit { prefs ->
                prefs[SupportDialogSnoozedUntilKey] =
                    System.currentTimeMillis() + SUPPORT_SNOOZE_DAYS * 24L * 60L * 60L * 1000L
            }
        }
    }

    fun dismissAndOpen(markStarred: Boolean) {
        showDialog = false
        coroutineScope.launch {
            context.dataStore.edit { prefs ->
                prefs[SupportDialogDismissedKey] = true
                if (markStarred) prefs[HasPressedStarKey] = true
            }
        }
        openGitHub(context)
    }

    AlertDialog(
        onDismissRequest = ::snooze,
        title = { Text("Support OmniTune") },
        text = {
            Text("OmniTune is an open-source music player built with care for Android. If you enjoy using it, starring the project on GitHub helps the app grow and keeps development moving.")
        },
        dismissButton = {
            TextButton(
                onClick = ::snooze,
            ) {
                Text("Later")
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = { dismissAndOpen(markStarred = true) },
                ) {
                    Text("Star")
                }
                TextButton(
                    onClick = { dismissAndOpen(markStarred = false) },
                ) {
                    Text("GitHub")
                }
            }
        },
    )
}

private fun openGitHub(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_REPO_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure { Toast.makeText(context, "Could not open GitHub", Toast.LENGTH_SHORT).show() }
}
