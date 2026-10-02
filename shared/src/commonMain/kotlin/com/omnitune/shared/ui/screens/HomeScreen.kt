package com.omnitune.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omnitune.shared.domain.models.AlbumItem
import com.omnitune.shared.domain.models.SongItem
import com.omnitune.shared.ui.components.BounceButton
import com.omnitune.shared.ui.components.MeshGradientBackground
import com.omnitune.shared.ui.components.OmniArtwork
import com.omnitune.shared.ui.components.SquircleShape
import com.omnitune.shared.ui.theme.LocalDominantColors
import com.omnitune.shared.ui.theme.PillShape

/**
 * Calculates time-of-day greeting based on current local hour.
 */
fun calculateTimeOfDayGreeting(hour: Int): String {
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}

val STANDARD_MOODS: List<String> = listOf("Energize", "Relax", "Workout", "Focus", "Party")

val SAMPLE_GENRES: List<String> = listOf("Pop", "Hip-Hop", "Rock", "Electronic", "R&B", "Jazz", "Classical")

/**
 * SuvMusic authentic HomeScreen with profile greeting header, 2-column quick access grid,
 * mood filter chips, recommendations carousel, mixes, and genre discovery cards.
 */
@Composable
fun HomeScreen(
    onSongClick: (SongItem) -> Unit,
    onAlbumClick: (String) -> Unit = {},
    onHistoryClick: () -> Unit = {},
    quickAccessItems: List<SongItem> = emptyList(),
    quickPicks: List<SongItem> = emptyList(),
    currentHour: Int = 10,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    val dominantColors = LocalDominantColors.current
    val greeting = calculateTimeOfDayGreeting(currentHour)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MeshGradientBackground(
            dominantColors = dominantColors,
            backgroundColor = MaterialTheme.colorScheme.background
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
        ) {
            // Profile & Greeting Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Welcome to OmniTune",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onHistoryClick) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "O",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.background
                            )
                        }
                    }
                }
            }

            // Quick Access 2-Column Grid (Recently Played)
            if (quickAccessItems.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        val rowCount = (quickAccessItems.size + 1) / 2
                        for (rowIndex in 0 until rowCount) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val firstIdx = rowIndex * 2
                                val secondIdx = firstIdx + 1

                                if (firstIdx < quickAccessItems.size) {
                                    val item = quickAccessItems[firstIdx]
                                    QuickAccessCard(
                                        item = item,
                                        onClick = { onSongClick(item) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (secondIdx < quickAccessItems.size) {
                                    val item = quickAccessItems[secondIdx]
                                    QuickAccessCard(
                                        item = item,
                                        onClick = { onSongClick(item) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Mood Chips Section
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(STANDARD_MOODS) { mood ->
                            val isSelected = (selectedMood == mood)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    // Tapping second time deselects
                                    selectedMood = if (isSelected) null else mood
                                },
                                label = {
                                    Text(
                                        text = mood,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                },
                                shape = PillShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor,
                                    selectedLabelColor = MaterialTheme.colorScheme.background,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // Quick Picks Recommendations Carousel
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Text(
                        text = "Quick picks",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )

                    if (quickPicks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "Start listening to see recommendations",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(quickPicks) { song ->
                                Column(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clickable { onSongClick(song) }
                                ) {
                                    OmniArtwork(
                                        url = song.thumbnailUrl,
                                        size = 130.dp,
                                        shape = SquircleShape(cornerRadius = 16.dp, cornerSmoothing = 0.6f),
                                        elevation = 4.dp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Genres Carousel
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Text(
                        text = "Explore Genres",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(SAMPLE_GENRES) { genre ->
                            Card(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(70.dp),
                                shape = SquircleShape(cornerRadius = 14.dp, cornerSmoothing = 0.6f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(10.dp),
                                    contentAlignment = Alignment.BottomStart
                                ) {
                                    Text(
                                        text = genre,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    item: SongItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick),
        shape = SquircleShape(cornerRadius = 10.dp, cornerSmoothing = 0.6f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OmniArtwork(
                url = item.thumbnailUrl,
                size = 56.dp,
                shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
