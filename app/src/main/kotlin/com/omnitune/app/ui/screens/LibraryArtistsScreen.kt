/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.omnitune.app.R
import com.omnitune.app.db.entities.Artist
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing

private enum class ArtistFilterMode { ALL, SAVED }

@Composable
fun LibraryArtistsScreen(
    onBack: () -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val allArtists by viewModel.libraryArtists.collectAsState()
    val savedArtists by viewModel.savedArtists.collectAsState()
    var filterMode by remember { mutableStateOf(ArtistFilterMode.ALL) }

    val artists = when (filterMode) {
        ArtistFilterMode.ALL -> allArtists
        ArtistFilterMode.SAVED -> savedArtists
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(OmniColors.BackgroundGradient)
            .statusBarsPadding()
            .padding(horizontal = OmniSpacing.section),
    ) {
        LibraryListHeader(
            title = "Artists",
            subtitle = countLabel(artists.size, "artist"),
            icon = R.drawable.ic_artist,
            accent = OmniColors.OmniAccentPrimary,
            onBack = onBack,
        )

        // All / Saved toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = OmniSpacing.small),
            horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
        ) {
            FilterChip(
                selected = filterMode == ArtistFilterMode.ALL,
                onClick = { filterMode = ArtistFilterMode.ALL },
                label = { Text("All (${allArtists.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OmniColors.OmniAccentPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = OmniColors.OmniAccentPrimary,
                ),
            )
            FilterChip(
                selected = filterMode == ArtistFilterMode.SAVED,
                onClick = { filterMode = ArtistFilterMode.SAVED },
                label = { Text("Saved (${savedArtists.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OmniColors.Hot.copy(alpha = 0.2f),
                    selectedLabelColor = OmniColors.Hot,
                ),
            )
        }

        if (artists.isEmpty()) {
            val emptyText = when (filterMode) {
                ArtistFilterMode.ALL -> "No artists in your library yet"
                ArtistFilterMode.SAVED -> "No saved artists — follow an artist to save them here"
            }
            LibraryEmptyState(icon = R.drawable.ic_artist, text = emptyText)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            ) {
                items(
                    items = artists,
                    key = { it.id },
                    contentType = { "artist" },
                ) { artist ->
                    ArtistRow(artist = artist, onClick = { onNavigateToArtist(artist.id) })
                }
                item(contentType = "bottom-spacer") { Spacer(modifier = Modifier.height(OmniChrome.BottomContentPadding)) }
            }
        }
    }
}

@Composable
private fun ArtistRow(artist: Artist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Large)
            .background(OmniColors.OmniGlassSubtle)
            .border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), OmniShapes.Large)
            .clickable(onClick = onClick)
            .padding(OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(58.dp).clip(CircleShape).background(OmniColors.OmniGlassStrong),
            contentAlignment = Alignment.Center,
        ) {
            if (artist.thumbnailUrl.isNullOrBlank()) {
                Icon(painterResource(R.drawable.ic_artist), contentDescription = null, tint = OmniColors.TextTertiary, modifier = Modifier.size(24.dp))
            } else {
                AsyncImage(model = artist.thumbnailUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = countLabel(artist.songCount, "song"),
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LibraryListHeader(
    title: String,
    subtitle: String,
    icon: Int,
    accent: androidx.compose.ui.graphics.Color,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = OmniSpacing.medium, bottom = OmniSpacing.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(48.dp).clip(CircleShape).background(OmniColors.OmniGlassMedium).border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), CircleShape),
        ) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back", tint = OmniColors.TextPrimary)
        }
        Spacer(modifier = Modifier.width(OmniSpacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = OmniColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = OmniColors.TextSecondary)
        }
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun LibraryEmptyState(icon: Int, text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(OmniShapes.ExtraLarge).background(OmniColors.OmniGlassSubtle).border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), OmniShapes.ExtraLarge).padding(OmniSpacing.screen),
        contentAlignment = Alignment.Center,
    ) {
        EmptyPlaceholder(icon = icon, text = text)
    }
}

private fun countLabel(count: Int, singular: String): String {
    val noun = if (count == 1) singular else "${singular}s"
    return "$count $noun"
}
