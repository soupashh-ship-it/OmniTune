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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.R
import com.omnitune.app.constants.HasPressedStarKey
import com.omnitune.app.constants.LaunchCountKey
import com.omnitune.app.constants.SupportDialogDismissedKey
import com.omnitune.app.constants.SupportDialogSnoozedUntilKey
import com.omnitune.app.db.entities.EventWithSong
import com.omnitune.app.db.entities.Song
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.ui.component.AccentPill
import com.omnitune.app.ui.component.GlassCard
import com.omnitune.app.ui.component.GlassSurface
import com.omnitune.app.ui.component.GlassTone
import com.omnitune.app.ui.component.OmniSectionHeader
import com.omnitune.app.ui.component.ShimmerBar
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles
import com.omnitune.app.utils.dataStore
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
    onNavigateToSearchQuery: (String) -> Unit,
    onNavigateToCollection: (String, String?) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onResumePlayback: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlaySongs: (List<Song>) -> Unit,
    viewModel: HomeDiscoveryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by (playerConnection?.mediaMetadata ?: flowOf(null)).collectAsState(initial = null)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = OmniSpacing.large),
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.section),
        ) {
            item(contentType = "header") {
                Column {
                    Spacer(modifier = Modifier.statusBarsPadding())
                    Spacer(modifier = Modifier.height(OmniSpacing.medium))
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
                HeroCarousel(
                    items = uiState.carouselItems,
                    isLoading = uiState.isLoading,
                    onPlaySong = onPlaySong,
                    onSearch = onNavigateToSearchQuery,
                    onOpenCollection = onNavigateToCollection,
                    onRequestHydration = viewModel::requestThumbnailHydration,
                )
            }

            mediaMetadata?.let { currentTrack ->
                item(key = "continue_${currentTrack.id}", contentType = "continue") {
                    ContinueCard(mediaMetadata = currentTrack, onClick = onResumePlayback)
                }
            }

            item(contentType = "quick-picks") {
                QuickPicksSection(
                    items = uiState.quickPicks,
                    isLoading = uiState.isLoading,
                    canPlayAll = uiState.playAllSongs.isNotEmpty(),
                    onPlayAll = { onPlaySongs(uiState.playAllSongs) },
                    onPlaySong = onPlaySong,
                    onSearch = onNavigateToSearchQuery,
                    onOpenCollection = onNavigateToCollection,
                    exploreQuery = uiState.quickPicksExploreQuery,
                    onRequestHydration = viewModel::requestThumbnailHydration,
                )
            }

            if (uiState.recentSongs.isNotEmpty()) {
                item(contentType = "recent") {
                    RecentlyPlayedDiscoverySection(
                        events = uiState.recentSongs,
                        isLoading = uiState.isLoading,
                        onPlaySong = onPlaySong,
                        onSearch = onNavigateToSearch,
                    )
                }
            }

            item(contentType = "searches") {
                DiscoveryShelf(
                    section = uiState.searchSection,
                    emptyLabel = "Search seeds appear here until you build history.",
                    onAction = onNavigateToSearch,
                    onItemClick = { item -> item.query?.let(onNavigateToSearchQuery) },
                    onRequestHydration = viewModel::requestThumbnailHydration,
                )
            }

            uiState.shelfSections.forEach { section ->
                item(key = "shelf_${section.id}", contentType = "horizontal-shelf") {
                    HorizontalDiscoveryShelf(
                        section = section,
                        onItemClick = { item ->
                            if (item.source == HomeCatalogSource.CuratedDefault && HomeDefaultCatalog.findCollection(item.id) != null) {
                                onNavigateToCollection(item.id, item.thumbnailUrl)
                            } else {
                                item.query?.let(onNavigateToSearchQuery)
                            }
                        },
                        onRequestHydration = viewModel::requestThumbnailHydration,
                    )
                }
            }

            item(contentType = "mood-grid") {
                MoodGenreGrid(chips = uiState.genreChips, onChipClick = { chip -> onNavigateToCollection(chip.id, null) })
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
                Spacer(modifier = Modifier.height(OmniSpacing.screen))
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "OmniTune",
                style = OmniTextStyles.heroTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Your music, ready when you are",
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HeaderIconButton(icon = R.drawable.ic_search, contentDescription = "Search", onClick = onSearch)
        Spacer(modifier = Modifier.width(OmniSpacing.compact))
        HeaderIconButton(icon = R.drawable.ic_settings, contentDescription = "Settings", onClick = onSettings)
    }
}

@Composable
private fun HeaderIconButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(46.dp)
            .clip(OmniShapes.Pill)
            .background(OmniColors.OmniGlassMedium),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = OmniColors.TextPrimary,
            modifier = Modifier.size(22.dp),
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
            FilterChip(
                selected = false,
                onClick = { onChipClick(chip) },
                label = { Text(chip.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

@Composable
private fun HeroCarousel(
    items: List<HomeCarouselItem>,
    isLoading: Boolean,
    onPlaySong: (Song) -> Unit,
    onSearch: (String) -> Unit,
    onOpenCollection: (String, String?) -> Unit,
    onRequestHydration: (HomeThumbnailRequest) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        OmniSectionHeader(title = "Home Discovery")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
            if (isLoading) {
                items(3, contentType = { "hero-loading" }) { HeroSkeleton() }
            } else {
                items(items, key = { it.id }, contentType = { "hero" }) { item ->
                    HeroCard(
                        item = item,
                        onRequestHydration = onRequestHydration,
                        onClick = {
                            item.song?.let(onPlaySong) ?: if (
                                item.source == HomeCatalogSource.CuratedDefault &&
                                HomeDefaultCatalog.findCollection(item.id) != null
                            ) {
                                onOpenCollection(item.id, item.thumbnailUrl)
                            } else {
                                item.query?.let(onSearch)
                            }
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

    GlassCard(
        modifier = Modifier.width(300.dp),
        onClick = onClick,
        cornerRadius = OmniShapes.ExtraLarge,
        tone = GlassTone.Strong,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp),
        ) {
            DiscoveryArtwork(
                thumbnailUrl = item.thumbnailUrl,
                contentDescription = item.title,
                title = item.title,
                artworkKey = item.artworkKey ?: item.id,
                modifier = Modifier.fillMaxSize(),
                imageSize = HERO_IMAGE_SIZE,
                shape = OmniShapes.ExtraLarge,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(androidx.compose.ui.graphics.Color.Transparent, OmniColors.OmniBackgroundBase.copy(alpha = 0.86f)),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(OmniSpacing.large),
            ) {
                AccentPill(text = if (item.song != null) "Play" else "Search")
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
}

@Composable
private fun HeroSkeleton() {
    GlassSurface(
        modifier = Modifier
            .width(300.dp)
            .height(176.dp),
        cornerRadius = OmniShapes.ExtraLarge,
        tone = GlassTone.Subtle,
    ) {
        ShimmerBar(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun ContinueCard(
    mediaMetadata: MediaMetadata,
    onClick: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        cornerRadius = OmniShapes.Player,
        tone = GlassTone.Player,
    ) {
        Row(
            modifier = Modifier.padding(OmniSpacing.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DiscoveryArtwork(
                thumbnailUrl = mediaMetadata.thumbnailUrl,
                contentDescription = mediaMetadata.title,
                title = mediaMetadata.title,
                artworkKey = mediaMetadata.id,
                modifier = Modifier.size(76.dp),
                imageSize = SHELF_IMAGE_SIZE,
                shape = OmniShapes.ArtworkMedium,
            )
            Spacer(modifier = Modifier.width(OmniSpacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                AccentPill(text = "Continue")
                Spacer(modifier = Modifier.height(OmniSpacing.small))
                Text(
                    text = mediaMetadata.title.ifBlank { "Unknown track" },
                    style = OmniTextStyles.sectionTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = mediaMetadata.artists.joinToString(", ") { it.name }.ifBlank { "Unknown artist" },
                    style = OmniTextStyles.metadata,
                    color = OmniColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun QuickPicksSection(
    items: List<QuickPickItem>,
    isLoading: Boolean,
    canPlayAll: Boolean,
    onPlayAll: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onSearch: (String) -> Unit,
    onOpenCollection: (String, String?) -> Unit,
    exploreQuery: String,
    onRequestHydration: (HomeThumbnailRequest) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        OmniSectionHeader(
            title = "Quick Picks",
            action = if (canPlayAll) "Play all" else "Explore",
            onAction = if (canPlayAll) onPlayAll else { { onSearch(exploreQuery) } },
        )
        if (isLoading) {
            repeat(4) { ShelfSkeletonRow() }
        } else {
            items.take(12).forEach { item ->
                QuickPickRow(
                    item = item,
                    onRequestHydration = onRequestHydration,
                    onClick = {
                        item.song?.let(onPlaySong) ?: if (
                            item.source == HomeCatalogSource.CuratedDefault &&
                            HomeDefaultCatalog.findCollection(item.id) != null
                        ) {
                            onOpenCollection(item.id, item.thumbnailUrl)
                        } else {
                            item.query?.let(onSearch)
                        }
                    },
                )
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

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        cornerRadius = OmniShapes.Medium,
        tone = GlassTone.Subtle,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OmniSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DiscoveryArtwork(
                thumbnailUrl = item.thumbnailUrl,
                contentDescription = item.title,
                title = item.title,
                artworkKey = item.artworkKey ?: item.id,
                modifier = Modifier.size(58.dp),
                imageSize = SHELF_IMAGE_SIZE,
                shape = OmniShapes.ArtworkSmall,
            )
            Spacer(modifier = Modifier.width(OmniSpacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = OmniTextStyles.songTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.subtitle, style = OmniTextStyles.metadata, color = OmniColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                painter = painterResource(if (item.song != null) R.drawable.ic_play_arrow else R.drawable.ic_search),
                contentDescription = null,
                tint = OmniColors.TextSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun RecentlyPlayedDiscoverySection(
    events: List<EventWithSong>,
    isLoading: Boolean,
    onPlaySong: (Song) -> Unit,
    onSearch: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        OmniSectionHeader(title = "Continue Listening")
        when {
            isLoading -> repeat(3) { ShelfSkeletonRow() }
            events.isEmpty() -> EmptyDiscoveryCard(text = "No recent plays yet", action = "Search", onClick = onSearch)
            else -> events.take(5).forEach { event ->
                SongShelfRow(
                    item = PlaylistShelfItem(
                        id = "recent_${event.song.id}",
                        title = event.song.song.title.ifBlank { "Unknown track" },
                        subtitle = event.song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown artist" },
                        thumbnailUrl = event.song.song.thumbnailUrl,
                        song = event.song,
                    ),
                    onClick = { onPlaySong(event.song) },
                )
            }
        }
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

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        cornerRadius = OmniShapes.Medium,
        tone = GlassTone.Subtle,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OmniSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DiscoveryArtwork(
                thumbnailUrl = item.thumbnailUrl,
                contentDescription = item.title,
                title = item.title,
                artworkKey = item.artworkKey ?: item.id,
                modifier = Modifier.size(56.dp),
                imageSize = SHELF_IMAGE_SIZE,
                shape = OmniShapes.ArtworkSmall,
            )
            Spacer(modifier = Modifier.width(OmniSpacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = OmniTextStyles.songTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.subtitle, style = OmniTextStyles.metadata, color = OmniColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                painter = painterResource(if (item.song != null) R.drawable.ic_play_arrow else R.drawable.ic_search),
                contentDescription = null,
                tint = OmniColors.TextSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun HorizontalDiscoveryShelf(
    section: HomeSection,
    onItemClick: (PlaylistShelfItem) -> Unit,
    onRequestHydration: (HomeThumbnailRequest) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        OmniSectionHeader(title = section.title)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
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

    GlassCard(
        modifier = Modifier.width(158.dp),
        onClick = onClick,
        cornerRadius = OmniShapes.Large,
        tone = GlassTone.Subtle,
    ) {
        Column(modifier = Modifier.padding(OmniSpacing.small)) {
            CollageArtwork(
                thumbnailUrls = item.thumbnailUrls,
                contentDescription = item.title,
                title = item.title,
                artworkKey = item.artworkKey ?: item.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                imageSize = SHELF_IMAGE_SIZE,
                shape = OmniShapes.ArtworkMedium,
            )
            Spacer(modifier = Modifier.height(OmniSpacing.small))
            Text(item.title, style = OmniTextStyles.songTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(item.subtitle, style = OmniTextStyles.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MoodGenreGrid(
    chips: List<MoodChip>,
    onChipClick: (MoodChip) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        OmniSectionHeader(title = "Mood and Genres")
        chips.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small), modifier = Modifier.fillMaxWidth()) {
                row.forEach { chip ->
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 72.dp),
                        onClick = { onChipClick(chip) },
                        cornerRadius = OmniShapes.Large,
                        tone = GlassTone.Subtle,
                    ) {
                        Text(
                            text = chip.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = OmniColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(OmniSpacing.large),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EmptyDiscoveryCard(
    text: String,
    action: String,
    onClick: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick, cornerRadius = OmniShapes.Large, tone = GlassTone.Subtle) {
        Row(modifier = Modifier.padding(OmniSpacing.large), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = OmniTextStyles.metadata, color = OmniColors.TextSecondary, modifier = Modifier.weight(1f))
            Text(action, style = MaterialTheme.typography.labelLarge, color = OmniColors.OmniAccentSecondary)
        }
    }
}

@Composable
private fun NewHomeState(
    onSearch: () -> Unit,
    onLibrary: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = OmniShapes.ExtraLarge, tone = GlassTone.Medium) {
        Column(modifier = Modifier.padding(OmniSpacing.section), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Start with your music", style = OmniTextStyles.sectionTitle)
            Spacer(modifier = Modifier.height(OmniSpacing.compact))
            Text(
                text = "Search for a song or add favorites. Home will fill with real listening signals.",
                style = OmniTextStyles.metadata,
                color = OmniColors.TextSecondary,
            )
            Spacer(modifier = Modifier.height(OmniSpacing.large))
            Row(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onSearch, modifier = Modifier.weight(1f)) { Text("Search") }
                TextButton(onClick = onLibrary, modifier = Modifier.weight(1f)) { Text("Library") }
            }
        }
    }
}

@Composable
private fun ShelfSkeletonRow() {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        cornerRadius = OmniShapes.Medium,
        tone = GlassTone.Subtle,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(OmniSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShimmerBar(modifier = Modifier.size(52.dp).clip(OmniShapes.ArtworkSmall))
            Spacer(modifier = Modifier.width(OmniSpacing.small))
            Column(modifier = Modifier.weight(1f)) {
                ShimmerBar(modifier = Modifier.fillMaxWidth(0.72f).height(14.dp))
                Spacer(modifier = Modifier.height(OmniSpacing.compact))
                ShimmerBar(modifier = Modifier.fillMaxWidth(0.44f).height(10.dp))
            }
        }
    }
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
            source == HomeCatalogSource.CuratedDefault &&
            !query.isNullOrBlank() &&
            thumbnailUrl.isNullOrBlank() &&
            state == HomeHydrationState.None
        ) {
            onRequestHydration(HomeThumbnailRequest(id = id, query = query, collage = collage))
        }
    }
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
                .build()
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(OmniColors.OmniGlassStrong),
        contentAlignment = Alignment.Center,
    ) {
        if (model != null) {
            SubcomposeAsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    GeneratedArtwork(title = title, artworkKey = artworkKey, modifier = Modifier.fillMaxSize())
                },
                error = {
                    GeneratedArtwork(title = title, artworkKey = artworkKey, modifier = Modifier.fillMaxSize())
                },
            )
        } else {
            GeneratedArtwork(title = title, artworkKey = artworkKey, modifier = Modifier.fillMaxSize())
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
            .background(OmniColors.OmniGlassStrong),
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
                .build()
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (model != null) {
            SubcomposeAsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { GeneratedArtwork(title = title, artworkKey = artworkKey, modifier = Modifier.fillMaxSize()) },
                error = { GeneratedArtwork(title = title, artworkKey = artworkKey, modifier = Modifier.fillMaxSize()) },
            )
        } else {
            GeneratedArtwork(title = title, artworkKey = artworkKey, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun GeneratedArtwork(
    title: String,
    artworkKey: String,
    modifier: Modifier = Modifier,
) {
    val palettes = listOf(
        listOf(Color(0xFF15B8A6), Color(0xFF234E70)),
        listOf(Color(0xFFEF476F), Color(0xFF5B2A86)),
        listOf(Color(0xFFFFB703), Color(0xFF126782)),
        listOf(Color(0xFF80ED99), Color(0xFF22577A)),
        listOf(Color(0xFFF77F00), Color(0xFF6A040F)),
        listOf(Color(0xFF48CAE4), Color(0xFF3A0CA3)),
        listOf(Color(0xFFE9C46A), Color(0xFF264653)),
        listOf(Color(0xFFF72585), Color(0xFF4361EE)),
    )
    val colors = palettes[kotlin.math.abs(artworkKey.hashCode()) % palettes.size]
    val initials = title
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString(" ") { it.take(1).uppercase() }
        .ifBlank { "OT" }
    val label = title
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString(" ")
        .ifBlank { "OmniTune" }

    Box(
        modifier = modifier.background(Brush.linearGradient(colors)),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_album),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.20f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(78.dp),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .height(28.dp)
                .padding(OmniSpacing.small),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            listOf(0.36f, 0.70f, 0.48f, 0.88f, 0.55f).forEachIndexed { index, height ->
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight(height)
                        .clip(OmniShapes.Pill)
                        .background(Color.White.copy(alpha = 0.34f + index * 0.04f)),
                )
            }
        }
        Text(
            text = initials,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.72f),
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(OmniSpacing.small),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
