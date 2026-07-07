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
import com.omnitune.app.db.entities.Album
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing

private enum class AlbumFilterMode { ALL, SAVED }

@Composable
fun LibraryAlbumsScreen(
    onBack: () -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val allAlbums by viewModel.libraryAlbums.collectAsState()
    val savedAlbums by viewModel.savedAlbums.collectAsState()
    var filterMode by remember { mutableStateOf(AlbumFilterMode.ALL) }

    val albums = when (filterMode) {
        AlbumFilterMode.ALL -> allAlbums
        AlbumFilterMode.SAVED -> savedAlbums
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
            title = "Albums",
            subtitle = countLabel(albums.size, "album"),
            icon = R.drawable.ic_album,
            accent = OmniColors.OmniAccentSecondary,
            onBack = onBack,
        )

        // All / Saved toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = OmniSpacing.small),
            horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
        ) {
            FilterChip(
                selected = filterMode == AlbumFilterMode.ALL,
                onClick = { filterMode = AlbumFilterMode.ALL },
                label = { Text("All (${allAlbums.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OmniColors.OmniAccentSecondary.copy(alpha = 0.2f),
                    selectedLabelColor = OmniColors.OmniAccentSecondary,
                ),
            )
            FilterChip(
                selected = filterMode == AlbumFilterMode.SAVED,
                onClick = { filterMode = AlbumFilterMode.SAVED },
                label = { Text("Saved (${savedAlbums.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OmniColors.Hot.copy(alpha = 0.2f),
                    selectedLabelColor = OmniColors.Hot,
                ),
            )
        }

        if (albums.isEmpty()) {
            val emptyText = when (filterMode) {
                AlbumFilterMode.ALL -> "No albums in your library yet"
                AlbumFilterMode.SAVED -> "No saved albums — like an album to save it here"
            }
            LibraryEmptyState(icon = R.drawable.ic_album, text = emptyText)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            ) {
                items(
                    items = albums,
                    key = { it.id },
                    contentType = { "album" },
                ) { album ->
                    AlbumRow(album = album, onClick = { onNavigateToAlbum(album.id) })
                }
                item(contentType = "bottom-spacer") { Spacer(modifier = Modifier.height(OmniChrome.BottomContentPadding)) }
            }
        }
    }
}

@Composable
private fun AlbumRow(album: Album, onClick: () -> Unit) {
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
        ArtworkBox(
            thumbnail = album.thumbnailUrl,
            fallbackIcon = R.drawable.ic_album,
        )
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = album.artists.joinToString(", ") { it.name }.ifBlank { "Unknown artist" },
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = OmniSpacing.medium, bottom = OmniSpacing.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(OmniColors.OmniGlassMedium)
                .border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), CircleShape),
        ) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back", tint = OmniColors.TextPrimary)
        }
        Spacer(modifier = Modifier.width(OmniSpacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = OmniColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = OmniColors.TextSecondary)
        }
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ArtworkBox(thumbnail: String?, fallbackIcon: Int) {
    Box(
        modifier = Modifier.size(58.dp).clip(OmniShapes.ArtworkSmall).background(OmniColors.OmniGlassStrong),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnail.isNullOrBlank()) {
            Icon(painterResource(fallbackIcon), contentDescription = null, tint = OmniColors.TextTertiary, modifier = Modifier.size(24.dp))
        } else {
            AsyncImage(model = thumbnail, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun LibraryEmptyState(icon: Int, text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.ExtraLarge)
            .background(OmniColors.OmniGlassSubtle)
            .border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), OmniShapes.ExtraLarge)
            .padding(OmniSpacing.screen),
        contentAlignment = Alignment.Center,
    ) {
        EmptyPlaceholder(icon = icon, text = text)
    }
}

private fun countLabel(count: Int, singular: String): String {
    val noun = if (count == 1) singular else "${singular}s"
    return "$count $noun"
}
