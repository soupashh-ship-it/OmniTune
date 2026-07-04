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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.R
import com.omnitune.app.db.entities.EventWithSong
import com.omnitune.app.db.entities.Song
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.ui.component.OmniFloatingSurface
import com.omnitune.app.ui.component.OmniSectionHeader
import com.omnitune.app.ui.component.TrackMenuProvider
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles
import kotlinx.coroutines.flow.flowOf

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onResumePlayback: () -> Unit = {},
    onPlaySong: (Song) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val quickPicks by viewModel.quickPicks.collectAsState()
    val quickPicksLoading by viewModel.quickPicksLoading.collectAsState()
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by (playerConnection?.mediaMetadata ?: flowOf(null)).collectAsState(initial = null)
    val hasNewUserState = mediaMetadata == null &&
        !uiState.isLoading &&
        !quickPicksLoading &&
        uiState.recentSongs.isEmpty() &&
        quickPicks.isEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OmniSpacing.large),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.section),
    ) {
        item {
            Column {
                Spacer(modifier = Modifier.statusBarsPadding())
                Spacer(modifier = Modifier.height(OmniSpacing.small))
                PremiumHomeHeader(onNavigateToSearch = onNavigateToSearch)
            }
        }

        item {
            QuickAccessSection(
                hasCurrentTrack = mediaMetadata != null,
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToLibrary = onNavigateToLibrary,
                onResumePlayback = onResumePlayback,
            )
        }

        if (quickPicksLoading || quickPicks.isNotEmpty()) {
            item {
                DiscoveryFeedSection(
                    quickPicks = quickPicks,
                    isLoading = quickPicksLoading,
                    onPlaySong = onPlaySong,
                )
            }
        }

        item {
            RecentlyPlayedSection(
                isLoading = uiState.isLoading,
                recentSongs = uiState.recentSongs,
                onPlaySong = onPlaySong,
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToLibrary = onNavigateToLibrary,
            )
        }

        if (hasNewUserState) {
            item {
                EmptyHomeState(
                    onNavigateToSearch = onNavigateToSearch,
                    onNavigateToLibrary = onNavigateToLibrary,
                )
            }
        }

        item { Spacer(modifier = Modifier.height(OmniSpacing.screen)) }
    }
}

@Composable
private fun PremiumHomeHeader(onNavigateToSearch: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left: Logo mark + app name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
        ) {
            // Signal bars logo mark
            SignalMark()
            Column {
                Text(
                    text = "OmniTune",
                    style = MaterialTheme.typography.headlineMedium,
                    color = OmniColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                )
            }
        }

        // Right: action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(OmniShapes.Pill)
                    .background(OmniColors.SurfaceSubtle)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = androidx.compose.material3.ripple(
                            bounded = true,
                            color = OmniColors.OmniAccentSecondary.copy(alpha = 0.14f),
                        ),
                        onClick = onNavigateToSearch,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = "Search",
                    tint = OmniColors.TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SignalMark() {
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
}

@Composable
private fun SearchEntryCard(onClick: () -> Unit) {
    OmniFloatingSurface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = OmniShapes.Large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OmniSpacing.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(OmniShapes.Pill)
                    .background(Brush.linearGradient(OmniColors.PrimaryGradientColors)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = OmniColors.TextOnAccent,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(OmniSpacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Search for a song",
                    style = OmniTextStyles.sectionTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Open the existing search screen",
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
private fun QuickAccessSection(
    hasCurrentTrack: Boolean,
    onNavigateToSearch: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onResumePlayback: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        OmniSectionHeader(title = "Quick Access")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
        ) {
            HomeActionCard(
                icon = R.drawable.ic_search,
                title = "Search",
                subtitle = "Find music",
                onClick = onNavigateToSearch,
                modifier = Modifier.weight(1f),
            )
            HomeActionCard(
                icon = R.drawable.ic_list,
                title = "Library",
                subtitle = "Saved music",
                onClick = onNavigateToLibrary,
                modifier = Modifier.weight(1f),
            )
            if (hasCurrentTrack) {
                HomeActionCard(
                    icon = R.drawable.ic_play_arrow,
                    title = "Player",
                    subtitle = "Now playing",
                    onClick = onResumePlayback,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OmniFloatingSurface(
        modifier = modifier.heightIn(min = 132.dp).clickable(onClick = onClick),
        shape = OmniShapes.Large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OmniSpacing.medium),
            horizontalAlignment = Alignment.Start,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(OmniShapes.Small)
                    .background(OmniColors.OmniGlassStrong),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = OmniColors.OmniAccentSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.height(OmniSpacing.medium))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = OmniColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = OmniTextStyles.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DiscoveryFeedSection(
    quickPicks: List<Song>,
    isLoading: Boolean,
    onPlaySong: (Song) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        OmniSectionHeader(title = "Explore")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
            if (isLoading) {
                items(4) {
                    DiscoverySkeletonCard()
                }
            } else {
                items(
                    items = quickPicks,
                    key = { song -> song.song.id },
                ) { song ->
                    DiscoverySongCard(song = song, onClick = { onPlaySong(song) })
                }
            }
        }
    }
}

@Composable
private fun DiscoverySkeletonCard() {
    OmniFloatingSurface(
        modifier = Modifier.width(168.dp),
        shape = OmniShapes.Large,
    ) {
        Column(modifier = Modifier.padding(OmniSpacing.small)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(OmniShapes.ArtworkMedium)
                    .background(OmniColors.SurfaceQuiet),
            )
            Spacer(modifier = Modifier.height(OmniSpacing.small))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(14.dp)
                    .clip(OmniShapes.Pill)
                    .background(OmniColors.OmniGlassStrong),
            )
            Spacer(modifier = Modifier.height(OmniSpacing.compact))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.56f)
                    .height(10.dp)
                    .clip(OmniShapes.Pill)
                    .background(OmniColors.OmniGlassMedium),
            )
        }
    }
}

@Composable
private fun DiscoverySongCard(
    song: Song,
    onClick: () -> Unit,
) {
    OmniFloatingSurface(
        modifier = Modifier.width(168.dp).clickable(onClick = onClick),
        shape = OmniShapes.Large,
    ) {
        Column(modifier = Modifier.padding(OmniSpacing.small)) {
            MediaArtwork(
                thumbnailUrl = song.song.thumbnailUrl,
                contentDescription = song.song.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = OmniShapes.ArtworkMedium,
            )
            Spacer(modifier = Modifier.height(OmniSpacing.small))
            Text(
                text = song.song.title,
                style = OmniTextStyles.songTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(OmniSpacing.micro))
            Text(
                text = song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown artist" },
                style = OmniTextStyles.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RecentlyPlayedSection(
    isLoading: Boolean,
    recentSongs: List<EventWithSong>,
    onPlaySong: (Song) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToLibrary: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        OmniSectionHeader(
            title = "Recently Played",
            action = if (recentSongs.isNotEmpty()) "Library" else null,
            onAction = if (recentSongs.isNotEmpty()) onNavigateToLibrary else null,
        )

        when {
            isLoading -> repeat(3) { RecentSkeletonRow() }
            recentSongs.isEmpty() -> RecentEmptyCard(onNavigateToSearch = onNavigateToSearch)
            else -> recentSongs.take(6).forEach { event ->
                RecentSongRow(
                    event = event,
                    onClick = { onPlaySong(event.song) },
                )
            }
        }
    }
}

@Composable
private fun RecentSkeletonRow() {
    OmniFloatingSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        shape = OmniShapes.Medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(OmniSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(OmniShapes.ArtworkSmall)
                    .background(OmniColors.SurfaceQuiet),
            )
            Spacer(modifier = Modifier.width(OmniSpacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(14.dp)
                        .clip(OmniShapes.Pill)
                        .background(OmniColors.OmniGlassStrong),
                )
                Spacer(modifier = Modifier.height(OmniSpacing.compact))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.44f)
                        .height(10.dp)
                        .clip(OmniShapes.Pill)
                        .background(OmniColors.OmniGlassMedium),
                )
            }
        }
    }
}

@Composable
private fun RecentSongRow(
    event: EventWithSong,
    onClick: () -> Unit,
) {
    OmniFloatingSurface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = OmniShapes.Medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OmniSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MediaArtwork(
                thumbnailUrl = event.song.song.thumbnailUrl,
                contentDescription = event.song.song.title,
                modifier = Modifier.size(56.dp),
                shape = OmniShapes.ArtworkSmall,
            )
            Spacer(modifier = Modifier.width(OmniSpacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.song.song.title,
                    style = OmniTextStyles.songTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = event.song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown artist" },
                    style = OmniTextStyles.metadata,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            var menuExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            val playerConnection = LocalPlayerConnection.current
            
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = "More options",
                        tint = OmniColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                
                TrackMenuProvider(
                    showMenu = menuExpanded,
                    onDismissMenu = { menuExpanded = false },
                    mediaMetadata = event.song.toMediaMetadata(),
                    onPlayNext = { playerConnection?.playNext(event.song.toMediaItem()) },
                    onAddToQueue = { playerConnection?.addToQueue(event.song.toMediaItem()) }
                )
            }
        }
    }
}

@Composable
private fun RecentEmptyCard(onNavigateToSearch: () -> Unit) {
    OmniFloatingSurface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToSearch),
        shape = OmniShapes.Large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OmniSpacing.section),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_history),
                contentDescription = null,
                tint = OmniColors.TextTertiary,
                modifier = Modifier.size(34.dp),
            )
            Spacer(modifier = Modifier.height(OmniSpacing.small))
            Text(
                text = "No recent plays yet",
                style = MaterialTheme.typography.titleMedium,
                color = OmniColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Use Search to start listening, then real history appears here.",
                style = OmniTextStyles.metadata,
                color = OmniColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EmptyHomeState(
    onNavigateToSearch: () -> Unit,
    onNavigateToLibrary: () -> Unit,
) {
    OmniFloatingSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = OmniShapes.ExtraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OmniSpacing.section),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Start with a search",
                style = OmniTextStyles.sectionTitle,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(OmniSpacing.compact))
            Text(
                text = "No history or feed items are available yet. Search for a song or open your library.",
                style = OmniTextStyles.metadata,
                color = OmniColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(OmniSpacing.large))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            ) {
                HomeActionCard(
                    icon = R.drawable.ic_search,
                    title = "Search",
                    subtitle = "Find a song",
                    onClick = onNavigateToSearch,
                    modifier = Modifier.weight(1f),
                )
                HomeActionCard(
                    icon = R.drawable.ic_list,
                    title = "Library",
                    subtitle = "Open saved music",
                    onClick = onNavigateToLibrary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MediaArtwork(
    thumbnailUrl: String?,
    contentDescription: String?,
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(OmniColors.OmniGlassStrong),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (thumbnailUrl.isNullOrBlank()) {
            Icon(
                painter = painterResource(R.drawable.ic_album),
                contentDescription = null,
                tint = OmniColors.TextTertiary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
