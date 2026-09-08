/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 *
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.screens.wrapped

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.omnitune.app.ui.component.glass.LiquidGlassSurface
import com.omnitune.app.ui.screens.settings.ArtistStats
import com.omnitune.app.ui.screens.settings.ListeningSongStats
import com.omnitune.app.ui.screens.settings.ListeningStatsUiState
import com.omnitune.app.ui.screens.settings.ListeningStatsViewModel
import com.omnitune.app.ui.screens.settings.TimeOfDay
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WrappedScreen(
    onBack: () -> Unit,
    viewModel: ListeningStatsViewModel = hiltViewModel()
) {
    val isDark = isSystemInDarkTheme()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (isDark) {
                        listOf(Color(0xFF1B0030), Color(0xFF002240), Color(0xFF000000))
                    } else {
                        listOf(Color(0xFFFFE4F2), Color(0xFFE4EEFF), Color(0xFFFFFFFF))
                    }
                )
            )
    ) {
        when {
            uiState.isLoading -> WrappedMessage("Loading your wrapped...")
            uiState.totalSongsPlayed == 0 -> WrappedMessage("Not enough listening yet. Play a few more songs to unlock your wrapped.")
            else -> {
                val cards = remember(uiState) { buildCards(uiState) }
                val pagerState = rememberPagerState(pageCount = { cards.size })
                VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    WrappedCard(cards[page], isDark)
                }
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun WrappedMessage(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

private sealed class WrappedCardContent(val title: String) {
    class TotalMinutes(val minutes: Long) : WrappedCardContent("You listened for...")
    class TotalSongs(val uniqueSongs: Int, val plays: Int) : WrappedCardContent("Your library in motion")
    class Personality(val titleText: String, val description: String) : WrappedCardContent("Your music personality")
    class TopSongs(val songs: List<ListeningSongStats>) : WrappedCardContent("Your top songs")
    class TopArtists(val artists: List<ArtistStats>) : WrappedCardContent("Your top artists")
    class TopTimeOfDay(val time: TimeOfDay, val plays: Int) : WrappedCardContent("When your music peaks")
}

private fun buildCards(state: ListeningStatsUiState): List<WrappedCardContent> = buildList {
    add(WrappedCardContent.TotalMinutes(state.totalListeningTimeMs / 60000))
    add(WrappedCardContent.TotalSongs(state.uniqueSongsPlayed, state.totalSongsPlayed))
    add(
        WrappedCardContent.Personality(
            titleText = state.musicPersonality.title,
            description = state.musicPersonality.description
        )
    )
    if (state.topSongs.isNotEmpty()) add(WrappedCardContent.TopSongs(state.topSongs.take(5)))
    if (state.topArtists.isNotEmpty()) add(WrappedCardContent.TopArtists(state.topArtists.take(5)))
    state.timeOfDayStats.maxByOrNull { it.value }
        ?.takeIf { it.value > 0 }
        ?.let { add(WrappedCardContent.TopTimeOfDay(it.key, it.value)) }
}

@Composable
private fun WrappedCard(card: WrappedCardContent, isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassSurface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(36.dp),
            blurAmount = 55f,
            isDarkTheme = isDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                when (card) {
                    is WrappedCardContent.TotalMinutes -> HeadlineNumber("${card.minutes}", "minutes")
                    is WrappedCardContent.TotalSongs -> HeadlineNumber(
                        "${card.uniqueSongs}",
                        "unique songs - ${card.plays} plays"
                    )
                    is WrappedCardContent.Personality -> HeadlineNumber(card.titleText, card.description)
                    is WrappedCardContent.TopSongs -> SongList(card.songs)
                    is WrappedCardContent.TopArtists -> ArtistList(card.artists)
                    is WrappedCardContent.TopTimeOfDay -> HeadlineNumber(
                        card.time.name.lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase() },
                        "${card.plays} plays"
                    )
                }
            }
        }
    }
}

@Composable
private fun HeadlineNumber(headline: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = headline,
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SongList(songs: List<ListeningSongStats>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        songs.forEachIndexed { index, song ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${index + 1}.",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(28.dp)
                )
                if (!song.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(modifier = Modifier.height(48.dp)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.artist} - ${song.playCount} plays",
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

@Composable
private fun ArtistList(artists: List<ArtistStats>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        artists.forEachIndexed { index, artist ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${index + 1}.",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(28.dp)
                )
                Column {
                    Text(
                        text = artist.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${artist.totalPlays} plays",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
