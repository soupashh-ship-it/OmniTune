package com.omnitune.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.component.shimmer.ShimmerTrackList
import com.omnitune.app.ui.component.TrackMenuProvider
import com.omnitune.app.ui.screens.settings.OmniPreferenceCard
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import java.time.LocalDate

@Composable
fun HistoryScreen(
    onPlaySong: (Song) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val groupedEvents = remember(uiState.events) { groupHistory(uiState.events) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(OmniColors.BackgroundGradient)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
    ) {
        item(contentType = "header") {
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.height(OmniSpacing.large))
            HistoryHeader(onSearch = onNavigateToSearch)
        }

        when {
            uiState.isLoading -> item(contentType = "loading") {
                ShimmerTrackList(rowCount = 5)
            }

            uiState.events.isEmpty() -> item(contentType = "empty") {
                HistoryEmptyState()
            }

            else -> {
                groupedEvents.forEach { section ->
                item(key = "header_${section.title}", contentType = "history-section") {
                    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.small)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = OmniColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${section.events.size} ${if (section.events.size == 1) "track" else "tracks"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = OmniColors.OmniAccentSecondary,
                            )
                        }
                        Column(modifier = Modifier.padding(vertical = OmniSpacing.micro)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(OmniShapes.Large)
                                    .background(OmniColors.SurfaceRaised)
                                    .border(1.dp, OmniColors.OmniAccentPrimary.copy(alpha = 0.26f), OmniShapes.Large)
                                    .padding(horizontal = OmniSpacing.compact),
                            ) {
                                section.events.forEachIndexed { index, event ->
                                    HistoryRow(event = event, onPlaySong = { onPlaySong(event.song) })
                                    if (index < section.events.lastIndex) {
                                        Spacer(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(OmniColors.SurfaceHairline),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "clear-history", contentType = "clear-history") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(OmniShapes.Pill)
                        .border(1.dp, OmniColors.OmniAccentPrimary, OmniShapes.Pill)
                        .clickable { showClearConfirmation = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_trash),
                            contentDescription = null,
                            tint = OmniColors.OmniAccentPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Clear listening history",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = OmniColors.OmniAccentPrimary,
                        )
                    }
                }
            }
            }
        }

        item(contentType = "bottom-spacer") { Spacer(modifier = Modifier.height(OmniChrome.BottomContentPaddingWithPlayer)) }
    }

    if (showClearConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            containerColor = OmniColors.SurfaceRaised,
            title = { Text("Clear listening history?", color = OmniColors.TextPrimary) },
            text = { Text("This removes your local listening history. It cannot be undone.", color = OmniColors.TextSecondary) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.clearListenHistory()
                        showClearConfirmation = false
                    },
                ) { Text("Clear", color = OmniColors.Error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel", color = OmniColors.TextSecondary)
                }
            },
        )
    }
}

@Composable
private fun HistoryHeader(onSearch: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(OmniSpacing.micro)) {
            Text(
                text = "History",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = "Your recently played tracks",
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextSecondary,
            )
        }
        IconButton(
            onClick = onSearch,
            modifier = Modifier
                .size(48.dp)
                .clip(OmniShapes.Pill)
                .border(1.dp, OmniColors.SurfaceHairline, OmniShapes.Pill),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Search",
                tint = OmniColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun HistoryRow(
    event: EventWithSong,
    onPlaySong: () -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlaySong)
            .padding(vertical = 6.dp, horizontal = OmniSpacing.compact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkBox(thumbnailUrl = event.song.thumbnailUrl)
        Spacer(modifier = Modifier.width(OmniSpacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = event.song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown artist" },
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = event.event.timestamp.toLocalTime().toString().take(5),
            style = MaterialTheme.typography.labelMedium,
            color = OmniColors.TextSecondary,
            modifier = Modifier.padding(horizontal = OmniSpacing.compact),
        )
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = "More options",
                    tint = OmniColors.TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            TrackMenuProvider(
                showMenu = showMenu,
                onDismissMenu = { showMenu = false },
                mediaMetadata = event.song.toMediaMetadata(),
                onPlayNext = { playerConnection?.playNext(event.song.toMediaItem()) },
                onAddToQueue = { playerConnection?.addToQueue(event.song.toMediaItem()) },
            )
        }
    }
}

@Composable
private fun ArtworkBox(thumbnailUrl: String?) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(OmniColors.SurfaceQuiet),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnailUrl.isNullOrBlank()) {
            Icon(
                painterResource(R.drawable.ic_album),
                contentDescription = null,
                tint = OmniColors.TextTertiary,
                modifier = Modifier.size(28.dp),
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
private fun HistoryEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.ExtraLarge)
            .padding(OmniSpacing.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_history),
            contentDescription = null,
            tint = OmniColors.TextTertiary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(OmniSpacing.medium))
        Text(
            text = "No history yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = OmniColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(OmniSpacing.micro))
        Text(
            text = "Songs you play will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

private data class HistorySection(
    val title: String,
    val events: List<EventWithSong>,
)

private fun groupHistory(events: List<EventWithSong>): List<HistorySection> {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    return events
        .groupBy { event ->
            when (event.event.timestamp.toLocalDate()) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> "Older"
            }
        }
        .map { (title, sectionEvents) -> HistorySection(title, sectionEvents) }
        .sortedBy { section ->
            when (section.title) {
                "Today" -> 0
                "Yesterday" -> 1
                else -> 2
            }
        }
}
