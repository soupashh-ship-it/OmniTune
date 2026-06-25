/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.omnitune.app.R
import com.omnitune.app.ui.component.GlassCard
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing

@Composable
fun LibraryScreen(
    onNavigateToSearch: () -> Unit = {},
    onNavigateToLiked: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToRecentlyPlayed: () -> Unit = {},
    onNavigateToArtists: () -> Unit = {},
    onNavigateToAlbums: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val realCollectionCount = uiState.likedCount +
        uiState.recentlyPlayed.size +
        uiState.librarySongCount +
        uiState.playlistCount

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(OmniColors.BackgroundGradient)
            .padding(horizontal = OmniSpacing.section),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium),
    ) {
        item {
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.height(OmniSpacing.medium))
            LibraryHero(
                totalCount = realCollectionCount,
            )
        }

        item {
            Text(
                text = "Your shelves",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            ) {
                LibraryDestinationCard(
                    painter = painterResource(R.drawable.ic_favorite),
                    title = "Liked Songs",
                    detail = countLabel(uiState.likedCount, "song"),
                    accent = OmniColors.Hot,
                    onClick = onNavigateToLiked,
                    modifier = Modifier.weight(1f),
                )
                LibraryDestinationCard(
                    painter = painterResource(R.drawable.ic_download),
                    title = "Downloads",
                    detail = "Open offline songs",
                    accent = OmniColors.Downloaded,
                    onClick = onNavigateToDownloads,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            ) {
                LibraryDestinationCard(
                    painter = painterResource(R.drawable.ic_list),
                    title = "Recently Played",
                    detail = countLabel(uiState.recentlyPlayed.size, "song"),
                    accent = OmniColors.OmniAccentPrimary,
                    onClick = onNavigateToRecentlyPlayed,
                    modifier = Modifier.weight(1f),
                )
                LibraryDestinationCard(
                    painter = painterResource(R.drawable.ic_play_arrow),
                    title = "Search",
                    detail = "Find music",
                    accent = OmniColors.OmniAccentSecondary,
                    onClick = onNavigateToSearch,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Text(
                text = "Browse library",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
            )
        }

        item {
            LibraryRouteRow(
                painter = painterResource(R.drawable.ic_artist),
                title = "Artists",
                detail = "Open saved artists",
                accent = OmniColors.OmniAccentPrimary,
                onClick = onNavigateToArtists,
            )
        }
        item {
            LibraryRouteRow(
                painter = painterResource(R.drawable.ic_album),
                title = "Albums",
                detail = "Open saved albums",
                accent = OmniColors.OmniAccentSecondary,
                onClick = onNavigateToAlbums,
            )
        }
        item {
            LibraryRouteRow(
                painter = painterResource(R.drawable.ic_list),
                title = "Playlists",
                detail = countLabel(uiState.playlistCount, "playlist"),
                accent = OmniColors.Hot,
                onClick = onNavigateToPlaylists,
            )
        }

        item { Spacer(modifier = Modifier.height(88.dp)) }
    }
}

@Composable
private fun LibraryHero(
    totalCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.ExtraLarge)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        OmniColors.OmniGlassPlayer,
                        OmniColors.OmniGlassSubtle,
                    )
                )
            )
            .border(
                BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle),
                OmniShapes.ExtraLarge,
            )
            .padding(OmniSpacing.section),
    ) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = OmniColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(OmniSpacing.compact))
        Text(
            text = if (totalCount == 0) {
                "Your saved music will appear here. Downloads are one tap away."
            } else {
                "Saved music, listening history, and playlists."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = OmniColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(OmniSpacing.large))
        Row(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small)) {
            LibraryMetricPill(
                label = "Library",
                value = countLabel(totalCount, "item"),
            )
        }
    }
}

@Composable
private fun LibraryMetricPill(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier
            .clip(OmniShapes.Pill)
            .background(OmniColors.OmniGlassMedium)
            .border(
                BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle),
                OmniShapes.Pill,
            )
            .padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.compact),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OmniColors.TextTertiary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = OmniColors.TextPrimary,
        )
    }
}

@Composable
private fun LibraryDestinationCard(
    painter: Painter,
    title: String,
    detail: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OmniSpacing.medium),
        ) {
            LibraryIconOrb(
                painter = painter,
                accent = accent,
            )
            Spacer(modifier = Modifier.height(OmniSpacing.medium))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LibraryRouteRow(
    painter: Painter,
    title: String,
    detail: String,
    accent: Color,
    onClick: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OmniSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LibraryIconOrb(
                painter = painter,
                accent = accent,
            )
            Spacer(modifier = Modifier.width(OmniSpacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OmniColors.TextPrimary,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = OmniColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LibraryIconOrb(
    painter: Painter,
    accent: Color,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun countLabel(
    count: Int,
    singular: String,
): String {
    val noun = if (count == 1) singular else "${singular}s"
    return "$count $noun"
}
