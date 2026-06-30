package com.omnitune.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import com.omnitune.app.ui.component.OmniTuneLoader
import com.omnitune.app.ui.component.TrackMenuProvider
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import java.time.LocalDate

@Composable
fun HistoryScreen(
    onPlaySong: (Song) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val groupedEvents = remember(uiState.events) { groupHistory(uiState.events) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(OmniColors.BackgroundGradient)
            .padding(horizontal = OmniSpacing.section),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
    ) {
        item(contentType = "header") {
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.height(OmniSpacing.large))
            HistoryHeader()
        }

        when {
            uiState.isLoading -> item(contentType = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    OmniTuneLoader(size = 42.dp)
                }
            }

            uiState.events.isEmpty() -> item(contentType = "empty") {
                HistoryEmptyState()
            }

            else -> groupedEvents.forEach { section ->
                item(key = "header_${section.title}", contentType = "history-section") {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OmniColors.TextSecondary,
                        modifier = Modifier.padding(top = OmniSpacing.medium),
                    )
                }
                items(
                    items = section.events,
                    key = { event -> "history_${event.event.id}_${event.song.id}" },
                    contentType = { "history-row" },
                ) { event ->
                    HistoryRow(
                        event = event,
                        onPlaySong = { onPlaySong(event.song) },
                    )
                }
            }
        }

        item(contentType = "bottom-spacer") { Spacer(modifier = Modifier.height(104.dp)) }
    }
}

@Composable
private fun HistoryHeader() {
    Column {
        Text(
            text = "History",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = OmniColors.TextPrimary,
        )
        Text(
            text = "Recently played tracks from this device",
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextSecondary,
        )
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
            .clip(OmniShapes.Large)
            .background(OmniColors.SurfaceQuiet)
            .border(BorderStroke(1.dp, OmniColors.SurfaceHairline), OmniShapes.Large)
            .clickable(onClick = onPlaySong)
            .padding(OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkBox(thumbnailUrl = event.song.thumbnailUrl)
        Spacer(modifier = Modifier.width(OmniSpacing.small))
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
            .size(56.dp)
            .clip(OmniShapes.ArtworkSmall)
            .background(OmniColors.OmniGlassStrong),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnailUrl.isNullOrBlank()) {
            Icon(
                painterResource(R.drawable.ic_album),
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
private fun HistoryEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.ExtraLarge)
            .background(OmniColors.SurfaceQuiet)
            .border(BorderStroke(1.dp, OmniColors.SurfaceHairline), OmniShapes.ExtraLarge)
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
