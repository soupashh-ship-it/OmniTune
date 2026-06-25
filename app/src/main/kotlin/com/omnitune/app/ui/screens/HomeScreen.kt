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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
                Spacer(modifier = Modifier.height(OmniSpacing.medium))
                PremiumHomeHeader()
            }
        }

        item {
            SearchEntryCard(onClick = onNavigateToSearch)
        }

        mediaMetadata?.let { currentTrack ->
            item(key = "continue_listening_${currentTrack.id}") {
                ContinueListeningCard(
                    mediaMetadata = currentTrack,
                    onClick = onResumePlayback,
                )
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
private fun PremiumHomeHeader() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = OmniShapes.ExtraLarge,
        tone = GlassTone.Strong,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OmniSpacing.section),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OmniGlass home",
                    style = MaterialTheme.typography.labelMedium,
                    color = OmniColors.OmniAccentSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(OmniSpacing.compact))
                Text(
                    text = "OmniTune",
                    style = OmniTextStyles.heroTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(OmniSpacing.compact))
                Text(
                    text = "Your music, downloads, and recent plays in one place.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmniColors.TextSecondary,
                )
            }

            Spacer(modifier = Modifier.width(OmniSpacing.medium))
            SignalMark()
        }
    }
}

@Composable
private fun SignalMark() {
    Row(
        modifier = Modifier
            .clip(OmniShapes.Pill)
            .background(OmniColors.OmniGlassSubtle)
            .padding(horizontal = OmniSpacing.small, vertical = OmniSpacing.medium),
        horizontalArrangement = Arrangement.spacedBy(OmniSpacing.micro),
        verticalAlignment = Alignment.Bottom,
    ) {
        val heights = listOf(18.dp, 34.dp, 24.dp, 44.dp)
        heights.forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(height)
                    .clip(OmniShapes.Pill)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                if (index == 3) OmniColors.OmniAccentSecondary else OmniColors.OmniAccentPrimary,
                                OmniColors.OmniAccentMuted,
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun SearchEntryCard(onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        cornerRadius = OmniShapes.Large,
        tone = GlassTone.Medium,
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
private fun ContinueListeningCard(
    mediaMetadata: MediaMetadata,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        OmniSectionHeader(title = "Continue Listening")
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            cornerRadius = OmniShapes.Player,
            tone = GlassTone.Player,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(OmniSpacing.large),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MediaArtwork(
                    thumbnailUrl = mediaMetadata.thumbnailUrl,
                    contentDescription = "Current track artwork",
                    modifier = Modifier.size(92.dp),
                    shape = OmniShapes.ArtworkMedium,
                )
                Spacer(modifier = Modifier.width(OmniSpacing.medium))
                Column(modifier = Modifier.weight(1f)) {
                    AccentPill(text = "Now playing")
                    Spacer(modifier = Modifier.height(OmniSpacing.small))
                    Text(
                        text = mediaMetadata.title.ifBlank { "Unknown track" },
                        style = MaterialTheme.typography.titleLarge,
                        color = OmniColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(OmniSpacing.micro))
                    Text(
                        text = mediaMetadata.artists.joinToString(", ") { it.name }.ifBlank { "Unknown artist" },
                        style = OmniTextStyles.metadata,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(OmniSpacing.small))
                    Text(
                        text = "Tap to open the full player",
                        style = MaterialTheme.typography.labelMedium,
                        color = OmniColors.OmniAccentSecondary,
                    )
                }
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
    GlassCard(
        modifier = modifier.heightIn(min = 132.dp),
        onClick = onClick,
        cornerRadius = OmniShapes.Large,
        tone = GlassTone.Subtle,
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
    GlassCard(
        modifier = Modifier.width(168.dp),
        cornerRadius = OmniShapes.Large,
        tone = GlassTone.Subtle,
    ) {
        Column(modifier = Modifier.padding(OmniSpacing.small)) {
            ShimmerBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(OmniShapes.ArtworkMedium),
            )
            Spacer(modifier = Modifier.height(OmniSpacing.small))
            ShimmerBar(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(14.dp),
            )
            Spacer(modifier = Modifier.height(OmniSpacing.compact))
            ShimmerBar(
                modifier = Modifier
                    .fillMaxWidth(0.56f)
                    .height(10.dp),
            )
        }
    }
}

@Composable
private fun DiscoverySongCard(
    song: Song,
    onClick: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.width(168.dp),
        onClick = onClick,
        cornerRadius = OmniShapes.Large,
        tone = GlassTone.Subtle,
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
            ShimmerBar(
                modifier = Modifier
                    .size(52.dp)
                    .clip(OmniShapes.ArtworkSmall),
            )
            Spacer(modifier = Modifier.width(OmniSpacing.small))
            Column(modifier = Modifier.weight(1f)) {
                ShimmerBar(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(14.dp),
                )
                Spacer(modifier = Modifier.height(OmniSpacing.compact))
                ShimmerBar(
                    modifier = Modifier
                        .fillMaxWidth(0.44f)
                        .height(10.dp),
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
            Icon(
                painter = painterResource(R.drawable.ic_play_arrow),
                contentDescription = "Play",
                tint = OmniColors.OmniAccentSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun RecentEmptyCard(onNavigateToSearch: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onNavigateToSearch,
        cornerRadius = OmniShapes.Large,
        tone = GlassTone.Subtle,
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
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = OmniShapes.ExtraLarge,
        tone = GlassTone.Medium,
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
