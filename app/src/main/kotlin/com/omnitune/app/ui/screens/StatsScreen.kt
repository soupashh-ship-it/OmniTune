package com.omnitune.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.omnitune.app.R
import com.omnitune.app.db.entities.ArtistEntity
import com.omnitune.app.db.entities.Song
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.component.shimmer.ShimmerTrackList
import com.omnitune.app.ui.screens.settings.OmniPreferenceCard
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onNavigateToYearInMusic: () -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .padding(horizontal = OmniSpacing.section),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium),
    ) {
        item(contentType = "header") {
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.height(OmniSpacing.large))
            StatsHeader(
                minutesListened = uiState.minutesListened,
                totalPlayed = uiState.totalPlayed,
            )
        }

        item(contentType = "year-in-music") {
            com.omnitune.app.ui.screens.settings.OmniPreferenceCard(title = null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToYearInMusic() }
                        .padding(OmniSpacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_history), // Use any available icon
                        contentDescription = null,
                        tint = com.omnitune.app.ui.theme.OmniColors.OmniAccentPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(OmniSpacing.medium))
                    Column {
                        androidx.compose.material3.Text(
                            text = "Year in Music",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = com.omnitune.app.ui.theme.OmniColors.TextPrimary
                        )
                        androidx.compose.material3.Text(
                            text = "View your personal music recap",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = com.omnitune.app.ui.theme.OmniColors.TextSecondary
                        )
                    }
                }
            }
        }

        when {
            uiState.isLoading -> item(contentType = "loading") {
                ShimmerTrackList(rowCount = 5)
            }

            uiState.error != null -> item(contentType = "error") {
                StatsEmptyState(
                    icon = R.drawable.ic_info,
                    title = "Stats unavailable",
                    body = uiState.error ?: "Could not read listening stats.",
                )
            }

            !uiState.hasStats -> item(contentType = "empty") {
                StatsEmptyState(
                    icon = R.drawable.ic_history,
                    title = "No insights yet",
                    body = "Listen to a few songs to see your stats here.",
                )
            }

            else -> {
                item(contentType = "summary") {
                    OmniPreferenceCard(title = "Overview") {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(OmniSpacing.small),
                            horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
                            verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
                        ) {
                            if (uiState.recentlyPlayedCount > 0) {
                                StatChip(R.drawable.ic_history, "Recently played", uiState.recentlyPlayedCount.toString())
                            }
                            if (uiState.totalPlayed > 0) {
                                StatChip(R.drawable.ic_play_arrow, "Plays", uiState.totalPlayed.toString())
                            }
                            if (uiState.playedThisWeek > 0) {
                                StatChip(R.drawable.ic_calendar, "This week", uiState.playedThisWeek.toString())
                            }
                            if (uiState.minutesListened > 0) {
                                StatChip(R.drawable.ic_history, "Minutes", uiState.minutesListened.toString())
                            }
                            if (uiState.likedCount > 0) {
                                StatChip(R.drawable.ic_favorite, "Liked", uiState.likedCount.toString())
                            }
                            if (uiState.songCount > 0) {
                                StatChip(R.drawable.ic_album, "Library songs", uiState.songCount.toString())
                            }
                            if (uiState.artistCount > 0) {
                                StatChip(R.drawable.ic_artist, "Artists", uiState.artistCount.toString())
                            }
                            if (uiState.albumCount > 0) {
                                StatChip(R.drawable.ic_album, "Albums", uiState.albumCount.toString())
                            }
                        }
                    }
                }

                if (uiState.topSongs.isNotEmpty()) {
                    item(contentType = "top-songs") {
                        OmniPreferenceCard(title = "Top songs") {
                            Column(modifier = Modifier.padding(vertical = OmniSpacing.micro)) {
                                uiState.topSongs.forEach { (song, plays) ->
                                    TopSongRow(song = song, plays = plays)
                                }
                            }
                        }
                    }
                }

                if (uiState.topArtists.isNotEmpty()) {
                    item(contentType = "top-artists") {
                        OmniPreferenceCard(title = "Top artists") {
                            Column(modifier = Modifier.padding(vertical = OmniSpacing.micro)) {
                                uiState.topArtists.forEach { (artist, plays) ->
                                    TopArtistRow(
                                        artist = artist,
                                        plays = plays,
                                        onClick = { onNavigateToArtist(artist.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item(contentType = "bottom-spacer") { Spacer(modifier = Modifier.height(OmniChrome.BottomContentPaddingWithPlayer)) }
    }
}

@Composable
private fun StatsHeader(minutesListened: Long, totalPlayed: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.compact)) {
        Text(
            text = "Stats",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = OmniColors.TextPrimary,
        )
        Text(
            text = "Listening insights from your real activity",
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextSecondary,
        )
        if (totalPlayed > 0 || minutesListened > 0) {
            Spacer(modifier = Modifier.height(OmniSpacing.small))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            ) {
                if (totalPlayed > 0) {
                    StatsHeroPill(
                        value = totalPlayed.toString(),
                        label = "total plays",
                        icon = R.drawable.ic_play_arrow,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (minutesListened > 0) {
                    StatsHeroPill(
                        value = if (minutesListened >= 60) "${minutesListened / 60}h ${minutesListened % 60}m"
                               else "${minutesListened}m",
                        label = "listened",
                        icon = R.drawable.ic_history,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsHeroPill(
    value: String,
    label: String,
    icon: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(OmniShapes.Medium)
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(
                        OmniColors.OmniAccentSecondary.copy(alpha = 0.14f),
                        OmniColors.OmniAccentPrimary.copy(alpha = 0.10f),
                    )
                )
            )
            .border(1.dp, OmniColors.SurfaceHairline, OmniShapes.Medium)
            .padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = OmniColors.OmniAccentSecondary,
            modifier = Modifier.size(20.dp),
        )
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = OmniColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun StatChip(
    icon: Int,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .clip(OmniShapes.Medium)
            .background(OmniColors.SurfaceRaised)
            .border(1.dp, OmniColors.SurfaceHairline, OmniShapes.Medium)
            .padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = OmniColors.OmniAccentSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OmniColors.OmniAccentSecondary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TopSongRow(
    song: Song,
    plays: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Medium)
            .background(OmniColors.SurfaceRaised)
            .padding(OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkBox(thumbnailUrl = song.thumbnailUrl)
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown artist" },
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = countLabel(plays, "play"),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = OmniColors.OmniAccentSecondary,
        )
    }
}

@Composable
private fun TopArtistRow(
    artist: ArtistEntity,
    plays: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Medium)
            .background(OmniColors.SurfaceSubtle.copy(alpha = 0.44f))
            .clickable(onClick = onClick)
            .padding(OmniSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(OmniShapes.Pill)
                .background(OmniColors.OmniAccentSecondary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            if (artist.thumbnailUrl.isNullOrBlank()) {
                Icon(
                    painter = painterResource(R.drawable.ic_artist),
                    contentDescription = null,
                    tint = OmniColors.OmniAccentSecondary,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                AsyncImage(
                    model = artist.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(modifier = Modifier.width(OmniSpacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = countLabel(plays, "play"),
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun ArtworkBox(thumbnailUrl: String?) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(OmniShapes.ArtworkSmall)
            .background(OmniColors.SurfaceQuiet),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnailUrl.isNullOrBlank()) {
            Icon(
                painter = painterResource(R.drawable.ic_album),
                contentDescription = null,
                tint = OmniColors.TextTertiary,
                modifier = Modifier.size(24.dp),
            )
        } else {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun StatsEmptyState(
    icon: Int,
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.ExtraLarge)
            .padding(OmniSpacing.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = OmniColors.TextTertiary,
            modifier = Modifier.size(44.dp),
        )
        Spacer(modifier = Modifier.height(OmniSpacing.medium))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = OmniColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(OmniSpacing.micro))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextSecondary,
            textAlign = TextAlign.Center,
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
