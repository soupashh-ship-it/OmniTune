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
import com.omnitune.app.utils.rememberPreference
import kotlinx.coroutines.flow.flowOf

private const val HERO_IMAGE_SIZE = 480
private const val SHELF_IMAGE_SIZE = 160
private const val SUPPORT_REPO_URL = "https://github.com/soupashh-ship-it/OmniTune"
private const val SUPPORT_PROMPT_LAUNCH_THRESHOLD = 3
private const val SUPPORT_SNOOZE_DAYS = 5L

@Composable
fun HomeDiscoveryRoute(
    onNavigateToSearch: () -> Unit,
    onNavigateToSearchQuery: (String) -> Unit,
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
                MoodChipRow(chips = uiState.moodChips, onChipClick = onNavigateToSearchQuery)
            }

            item(contentType = "hero") {
                HeroCarousel(
                    items = uiState.carouselItems,
                    isLoading = uiState.isLoading,
                    onPlaySong = onPlaySong,
                    onSearch = onNavigateToSearchQuery,
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
                    onSearch = onNavigateToSearch,
                )
            }

            item(contentType = "recent") {
                RecentlyPlayedDiscoverySection(
                    events = uiState.recentSongs,
                    isLoading = uiState.isLoading,
                    onPlaySong = onPlaySong,
                    onSearch = onNavigateToSearch,
                )
            }

            item(contentType = "searches") {
                DiscoveryShelf(
                    section = uiState.searchSection,
                    emptyLabel = "Search seeds appear here until you build history.",
                    onAction = onNavigateToSearch,
                    onItemClick = { item -> item.query?.let(onNavigateToSearchQuery) },
                )
            }

            item(contentType = "mood-grid") {
                MoodGenreGrid(chips = uiState.genreChips, onChipClick = onNavigateToSearchQuery)
            }

            if (uiState.downloadSection.items.isNotEmpty()) {
                item(contentType = "downloads") {
                    DiscoveryShelf(
                        section = uiState.downloadSection,
                        emptyLabel = "",
                        onAction = onNavigateToDownloads,
                        onItemClick = { item -> item.song?.let(onPlaySong) },
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
                    )
                }
            }

            if (!uiState.isLoading && uiState.quickPicks.isEmpty() && uiState.recentSongs.isEmpty()) {
                item(contentType = "empty") {
                    NewHomeState(onSearch = onNavigateToSearch, onLibrary = onNavigateToLibrary)
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
    onChipClick: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small)) {
        items(chips, key = { it.id }, contentType = { "mood-chip" }) { chip ->
            FilterChip(
                selected = false,
                onClick = { onChipClick(chip.query) },
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
                        onClick = {
                            item.song?.let(onPlaySong) ?: item.query?.let(onSearch)
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
    onClick: () -> Unit,
) {
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
    onSearch: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        OmniSectionHeader(title = "Quick Picks", action = if (canPlayAll) "Play all" else "Search", onAction = if (canPlayAll) onPlayAll else onSearch)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
            if (isLoading) {
                items(4, contentType = { "quick-loading" }) { QuickPickSkeleton() }
            } else {
                items(items, key = { it.id }, contentType = { "quick-pick" }) { item ->
                    QuickPickCard(item = item, onClick = { item.song?.let(onPlaySong) })
                }
            }
        }
    }
}

@Composable
private fun QuickPickCard(
    item: QuickPickItem,
    onClick: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.width(156.dp),
        onClick = onClick,
        cornerRadius = OmniShapes.Large,
        tone = GlassTone.Subtle,
    ) {
        Column(modifier = Modifier.padding(OmniSpacing.small)) {
            DiscoveryArtwork(
                thumbnailUrl = item.thumbnailUrl,
                contentDescription = item.title,
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
private fun QuickPickSkeleton() {
    GlassCard(modifier = Modifier.width(156.dp), cornerRadius = OmniShapes.Large, tone = GlassTone.Subtle) {
        Column(modifier = Modifier.padding(OmniSpacing.small)) {
            ShimmerBar(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(OmniShapes.ArtworkMedium))
            Spacer(modifier = Modifier.height(OmniSpacing.small))
            ShimmerBar(modifier = Modifier.fillMaxWidth(0.8f).height(14.dp))
            Spacer(modifier = Modifier.height(OmniSpacing.compact))
            ShimmerBar(modifier = Modifier.fillMaxWidth(0.55f).height(10.dp))
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
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        OmniSectionHeader(title = section.title, action = section.actionLabel, onAction = section.actionLabel?.let { onAction })
        if (section.items.isEmpty()) {
            if (emptyLabel.isNotBlank()) EmptyDiscoveryCard(text = emptyLabel, action = "Search", onClick = onAction)
        } else {
            section.items.take(6).forEach { item ->
                SongShelfRow(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
private fun SongShelfRow(
    item: PlaylistShelfItem,
    onClick: () -> Unit,
) {
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
private fun MoodGenreGrid(
    chips: List<MoodChip>,
    onChipClick: (String) -> Unit,
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
                        onClick = { onChipClick(chip.query) },
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
private fun DiscoveryArtwork(
    thumbnailUrl: String?,
    contentDescription: String?,
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
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_album),
                contentDescription = null,
                tint = OmniColors.TextTertiary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun SupportDevelopmentDialog() {
    val context = LocalContext.current
    val launchCount = rememberPreference(LaunchCountKey, 0)
    val hasPressedStar = rememberPreference(HasPressedStarKey, false)
    val dismissed = rememberPreference(SupportDialogDismissedKey, false)
    val snoozedUntil = rememberPreference(SupportDialogSnoozedUntilKey, 0L)
    var hiddenThisSession by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launchCount.value = launchCount.value + 1
    }

    val now = System.currentTimeMillis()
    val shouldShow = !hiddenThisSession &&
        !dismissed.value &&
        !hasPressedStar.value &&
        launchCount.value >= SUPPORT_PROMPT_LAUNCH_THRESHOLD &&
        now >= snoozedUntil.value

    if (!shouldShow) return

    AlertDialog(
        onDismissRequest = {
            hiddenThisSession = true
            snoozedUntil.value = now + SUPPORT_SNOOZE_DAYS * 24L * 60L * 60L * 1000L
        },
        title = { Text("Support OmniTune") },
        text = {
            Text("OmniTune is an open-source music player built with care for Android. If you enjoy using it, starring the project on GitHub helps the app grow and keeps development moving.")
        },
        dismissButton = {
            TextButton(
                onClick = {
                    hiddenThisSession = true
                    snoozedUntil.value = now + SUPPORT_SNOOZE_DAYS * 24L * 60L * 60L * 1000L
                },
            ) {
                Text("Later")
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = {
                        hiddenThisSession = true
                        hasPressedStar.value = true
                        dismissed.value = true
                        openGitHub(context)
                    },
                ) {
                    Text("Star")
                }
                TextButton(
                    onClick = {
                        hiddenThisSession = true
                        dismissed.value = true
                        openGitHub(context)
                    },
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

