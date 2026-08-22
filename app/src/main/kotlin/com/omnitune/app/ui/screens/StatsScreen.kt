package com.omnitune.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onNavigateToYearInMusic: () -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .padding(horizontal = OmniSpacing.screenHorizontalCompact),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(contentType = "header") {
            Spacer(modifier = Modifier.statusBarsPadding())
            StatsHeader(
                uiState = uiState,
                onNavigateToYearInMusic = onNavigateToYearInMusic,
            )
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
                item(contentType = "metrics") {
                    StatsMetricGrid(
                        uiState = uiState,
                        onNavigateToYearInMusic = onNavigateToYearInMusic,
                    )
                }

                item(contentType = "overview") {
                    StatsOverview(uiState = uiState)
                }

                if (uiState.topSongs.isNotEmpty()) {
                item(contentType = "top-songs") {
                    StatsPanel(title = "Top Songs") {
                        val maxPlays = uiState.topSongs.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                        Column(modifier = Modifier.padding(vertical = OmniSpacing.micro)) {
                            uiState.topSongs.forEachIndexed { index, (song, plays) ->
                                    TopSongRow(
                                        rank = index + 1,
                                        song = song,
                                        plays = plays,
                                        maxPlays = maxPlays,
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.topArtists.isNotEmpty()) {
                    item(contentType = "top-artists") {
                        StatsPanel(title = "Top Artists") {
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
private fun StatsHeader(
    uiState: StatsUiState,
    onNavigateToYearInMusic: () -> Unit,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_omnitune_logo),
                contentDescription = null,
                colorFilter = ColorFilter.tint(OmniColors.OmniAccentSecondary),
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(OmniSpacing.compact))
            Text(
                text = "OmniTune",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
            )
            Spacer(modifier = Modifier.weight(1f))
            StatsHeaderIconButton(
                icon = R.drawable.ic_share,
                contentDescription = "Share listening stats",
                onClick = {
                    val summary = if (uiState.hasStats) {
                        "I listened to ${formatListeningMinutes(uiState.minutesListened)} in OmniTune."
                    } else {
                        "My OmniTune listening summary."
                    }
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, summary)
                            },
                            "Share stats",
                        ),
                    )
                },
            )
            Spacer(modifier = Modifier.width(OmniSpacing.compact))
            StatsHeaderIconButton(
                icon = R.drawable.ic_calendar,
                contentDescription = "Open Year in Music",
                onClick = onNavigateToYearInMusic,
            )
        }
        Text(
            text = "Stats",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = OmniColors.TextPrimary,
        )
        Text(
            text = "Your listening insights and journey.",
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextSecondary,
        )
    }
}

@Composable
private fun StatsHeaderIconButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(OmniShapes.Pill)
            .background(OmniColors.SurfaceSubtle.copy(alpha = 0.54f))
            .border(1.dp, OmniColors.OmniAccentPrimary.copy(alpha = 0.28f), OmniShapes.Pill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = OmniColors.TextPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun StatsMetricGrid(
    uiState: StatsUiState,
    onNavigateToYearInMusic: () -> Unit,
) {
    val topArtist = uiState.topArtists.firstOrNull()
    val topSong = uiState.topSongs.firstOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StatsMetricCard(
                label = "Total Plays",
                value = uiState.totalPlayed.toString(),
                detail = "From listening history",
                icon = R.drawable.ic_play_arrow,
                modifier = Modifier.weight(1f),
            )
            StatsMetricCard(
                label = "Minutes Listened",
                value = formatListeningMinutes(uiState.minutesListened),
                detail = "From recorded play time",
                icon = R.drawable.ic_history,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StatsMetricCard(
                label = "Top Artist",
                value = topArtist?.first?.name ?: "Unavailable",
                detail = topArtist?.second?.let { countLabel(it, "play") } ?: "Keep listening to unlock",
                icon = R.drawable.ic_artist,
                modifier = Modifier.weight(1f),
                compactValue = true,
            )
            StatsMetricCard(
                label = "Top Song",
                value = topSong?.first?.title ?: "Unavailable",
                detail = topSong?.second?.let { countLabel(it, "play") } ?: "Keep listening to unlock",
                icon = R.drawable.ic_album,
                modifier = Modifier.weight(1f),
                compactValue = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StatsMetricCard(
                label = "Active this week",
                value = uiState.playedThisWeek.toString(),
                detail = countLabel(uiState.playedThisWeek, "recorded play"),
                icon = R.drawable.ic_history,
                modifier = Modifier.weight(1f),
            )
            StatsYearInMusicCard(
                modifier = Modifier.weight(1f),
                onClick = onNavigateToYearInMusic,
            )
        }
    }
}

@Composable
private fun StatsMetricCard(
    label: String,
    value: String,
    detail: String,
    icon: Int,
    modifier: Modifier = Modifier,
    compactValue: Boolean = false,
) {
    Row(
        modifier = modifier
            .height(60.dp)
            .clip(OmniShapes.Large)
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(OmniColors.OmniAccentPrimary.copy(alpha = 0.16f), OmniColors.SurfaceRaised),
                ),
            )
            .border(1.dp, OmniColors.OmniAccentPrimary.copy(alpha = 0.28f), OmniShapes.Large)
            .padding(horizontal = OmniSpacing.small, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(OmniShapes.Medium)
                .background(OmniColors.OmniAccentPrimary.copy(alpha = 0.28f))
                .border(1.dp, OmniColors.OmniAccentSecondary.copy(alpha = 0.42f), OmniShapes.Medium),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = OmniColors.OmniAccentSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = if (compactValue) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatsYearInMusicCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(60.dp)
            .clip(OmniShapes.Large)
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(OmniColors.OmniAccentPrimary.copy(alpha = 0.34f), OmniColors.SurfaceRaised),
                ),
            )
            .border(1.dp, OmniColors.OmniAccentPrimary.copy(alpha = 0.34f), OmniShapes.Large)
            .clickable(onClick = onClick)
            .padding(horizontal = OmniSpacing.small, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Year in Music ${java.time.LocalDate.now().year}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = "Relive your year",
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = "Open Year in Music",
            tint = OmniColors.OmniAccentSecondary,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer(rotationZ = 180f),
        )
    }
}

@Composable
private fun StatsOverview(uiState: StatsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .clip(OmniShapes.Pill)
                    .border(1.dp, OmniColors.SurfaceHairline, OmniShapes.Pill)
                    .padding(horizontal = OmniSpacing.small, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OmniSpacing.micro),
            ) {
                Text(
                    text = "Last 30 days",
                    style = MaterialTheme.typography.labelSmall,
                    color = OmniColors.TextSecondary,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Last 30 days",
                    tint = OmniColors.TextSecondary,
                    modifier = Modifier
                        .size(12.dp)
                        .graphicsLayer(rotationZ = -90f),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(OmniShapes.Large)
                .background(OmniColors.SurfaceRaised)
                .border(1.dp, OmniColors.SurfaceHairline, OmniShapes.Large)
                .padding(OmniSpacing.small),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.micro)) {
                StatChip(R.drawable.ic_play_arrow, "Songs", uiState.recentlyPlayedCount.toString(), Modifier.weight(1f))
                StatChip(R.drawable.ic_artist, "Artists", uiState.artistCount.toString(), Modifier.weight(1f))
                StatChip(R.drawable.ic_album, "Albums", uiState.albumCount.toString(), Modifier.weight(1f))
                StatChip(R.drawable.ic_favorite, "Likes", uiState.likedCount.toString(), Modifier.weight(1f))
                StatChip(R.drawable.ic_history, "This week", uiState.playedThisWeek.toString(), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(6.dp))
            ListeningTimeChart(
                totalMinutes = uiState.minutesListened,
                dailyListeningMinutes = uiState.dailyListeningMinutes,
            )
        }
    }
}

@Composable
private fun StatsPanel(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.small)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = OmniColors.TextPrimary,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(OmniShapes.Large)
                .background(OmniColors.SurfaceRaised)
                .border(1.dp, OmniColors.OmniAccentPrimary.copy(alpha = 0.22f), OmniShapes.Large)
                .padding(OmniSpacing.small),
        ) {
            content()
        }
    }
}

private fun formatListeningMinutes(minutes: Long): String =
    if (minutes >= 60L) "${minutes / 60L}h ${minutes % 60L}m" else "${minutes}m"

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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(62.dp)
            .clip(OmniShapes.Medium)
            .background(OmniColors.SurfaceRaised)
            .border(1.dp, OmniColors.SurfaceHairline, OmniShapes.Medium)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = OmniColors.OmniAccentSecondary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = OmniColors.TextPrimary,
            maxLines = 1,
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

@Composable
private fun ListeningTimeChart(
    totalMinutes: Long,
    dailyListeningMinutes: List<ListeningDay>,
) {
    val points = dailyListeningMinutes.ifEmpty {
        listOf(
            ListeningDay(java.time.LocalDate.now().minusDays(1), 0L),
            ListeningDay(java.time.LocalDate.now(), 0L),
        )
    }
    val maximum = points.maxOf { it.minutes }.coerceAtLeast(1L)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Medium)
            .background(OmniColors.SurfaceSubtle.copy(alpha = 0.54f))
            .padding(OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(94.dp)) {
            Text(
                text = "Listening Time",
                style = MaterialTheme.typography.labelMedium,
                color = OmniColors.TextSecondary,
            )
            Text(
                text = formatListeningMinutes(totalMinutes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = "Last 30 days",
                style = MaterialTheme.typography.labelSmall,
                color = OmniColors.TextTertiary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                val strokeWidth = 2.dp.toPx()
                val horizontalInset = strokeWidth
                repeat(3) { index ->
                    val y = size.height * (index + 1) / 4f
                    drawLine(
                        color = OmniColors.SurfaceHairline.copy(alpha = 0.64f),
                        start = androidx.compose.ui.geometry.Offset(horizontalInset, y),
                        end = androidx.compose.ui.geometry.Offset(size.width - horizontalInset, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                val path = Path()
                points.forEachIndexed { index, day ->
                    val x = if (points.size == 1) size.width / 2f else {
                        horizontalInset + (size.width - horizontalInset * 2f) * index / (points.size - 1)
                    }
                    val y = size.height - ((day.minutes.toFloat() / maximum.toFloat()) * (size.height * 0.82f)) - horizontalInset
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = OmniColors.OmniAccentSecondary,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(points.first(), points[points.size / 2], points.last()).forEach { point ->
                    Text(
                        text = "${point.date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${point.date.dayOfMonth}",
                        style = MaterialTheme.typography.labelSmall,
                        color = OmniColors.TextTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopSongRow(
    rank: Int,
    song: Song,
    plays: Int,
    maxPlays: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
        .clip(OmniShapes.Medium)
            .background(OmniColors.SurfaceRaised)
            .padding(OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = OmniColors.TextSecondary,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center,
        )
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
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (plays.toFloat() / maxPlays.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(OmniShapes.Pill),
                color = OmniColors.OmniAccentPrimary,
                trackColor = OmniColors.OmniAccentPrimary.copy(alpha = 0.15f),
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
