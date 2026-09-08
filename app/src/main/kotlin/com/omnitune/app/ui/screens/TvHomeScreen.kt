/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 *
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.omnitune.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Interests
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.omnitune.app.models.Album
import com.omnitune.app.models.HomeItem
import com.omnitune.app.models.HomeSection
import com.omnitune.app.models.PlaylistDisplayItem
import com.omnitune.app.models.Song
import com.omnitune.app.ui.theme.SquircleShape
import com.omnitune.app.ui.utils.ImageUtils
import com.omnitune.app.ui.utils.dpadFocusable

/**
 * Dedicated Home Screen for Android TV / Desktop mode.
 */
@Composable
fun TvHomeScreen(
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (String) -> Unit = {},
    onExploreClick: (String, String) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading && uiState.homeSections.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val heroItem = remember(uiState) {
        uiState.recommendations.firstOrNull()
            ?: (uiState.homeSections.firstOrNull()?.items?.firstOrNull() as? HomeItem.SongItem)?.song
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 50.dp)
    ) {
        item(key = "tv_hero", contentType = "hero") {
            if (heroItem != null) {
                TvHeroSection(
                    song = heroItem,
                    onPlayClick = {
                        onSongClick(listOf(heroItem), 0)
                    }
                )
            }
        }

        if (uiState.recommendations.isNotEmpty()) {
            item(key = "tv_quick_access", contentType = "quick_access") {
                TvSectionTitle("Quick Access")
                TvQuickAccessGrid(
                    items = uiState.recommendations.take(8),
                    onItemClick = { song ->
                        onSongClick(uiState.recommendations, uiState.recommendations.indexOf(song))
                    }
                )
            }
        }

        items(
            items = uiState.homeSections,
            key = { it.title },
            contentType = { "section" }
        ) { section ->
            if (section.items.isNotEmpty()) {
                TvHorizontalSection(
                    section = section,
                    onSongClick = onSongClick,
                    onPlaylistClick = onPlaylistClick,
                    onAlbumClick = onAlbumClick,
                    onArtistClick = onArtistClick,
                    onExploreClick = onExploreClick
                )
            }
        }
    }
}

@Composable
private fun TvHeroSection(
    song: Song,
    onPlayClick: () -> Unit
) {
    val context = LocalContext.current
    val highResImage = ImageUtils.getHighResThumbnailUrl(song.thumbnailUrl) ?: song.thumbnailUrl

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(highResImage)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.9f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = 1500f
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 400f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 56.dp, bottom = 48.dp)
                .widthIn(max = 600.dp)
        ) {
            Text(
                text = "FEATURED",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = song.title,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onPlayClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                modifier = Modifier.dpadFocusable(
                    focusedScale = 1.05f,
                    shape = RoundedCornerShape(12.dp)
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Play Now", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun TvSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 56.dp, top = 32.dp, bottom = 16.dp)
    )
}

@Composable
private fun TvQuickAccessGrid(
    items: List<Song>,
    onItemClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 56.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val rows = items.chunked(4)
        rows.forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { item ->
                    TvQuickAccessCard(
                        song = item,
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp),
                        onClick = { onItemClick(item) }
                    )
                }
                repeat(4 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TvQuickAccessCard(
    song: Song,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .dpadFocusable(
                onClick = onClick,
                focusedScale = 1.05f,
                shape = SquircleShape,
                borderWidth = 2.dp,
                borderColor = MaterialTheme.colorScheme.primary
            )
            .clip(SquircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .background(Color.DarkGray),
            contentScale = ContentScale.Crop
        )
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun TvHorizontalSection(
    section: HomeSection,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (String) -> Unit,
    onExploreClick: (String, String) -> Unit
) {
    TvSectionTitle(section.title)

    LazyRow(
        contentPadding = PaddingValues(horizontal = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(
            items = section.items,
            key = { "${section.title}_${it.id}" },
            contentType = { it::class.simpleName ?: "item" }
        ) { item ->
            when (item) {
                is HomeItem.SongItem -> {
                    TvSongCard(
                        song = item.song,
                        onClick = { onSongClick(listOf(item.song), 0) }
                    )
                }
                is HomeItem.PlaylistItem -> {
                    TvPlaylistCard(
                        playlist = item.playlist,
                        onClick = { onPlaylistClick(item.playlist) }
                    )
                }
                is HomeItem.AlbumItem -> {
                    TvAlbumCard(
                        album = item.album,
                        onClick = { onAlbumClick(item.album) }
                    )
                }
                is HomeItem.ArtistItem -> {
                    TvArtistCard(
                        title = item.artist.name,
                        subtitle = item.artist.subscribers ?: "Artist",
                        imageUrl = item.artist.thumbnailUrl,
                        onClick = { onArtistClick(item.artist.id) }
                    )
                }
                is HomeItem.ExploreItem -> {
                    TvExploreCard(
                        title = item.title,
                        onClick = { onExploreClick(item.browseId, item.title) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvSongCard(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .dpadFocusable(
                onClick = onClick,
                shape = SquircleShape,
                focusedScale = 1.1f,
                borderColor = MaterialTheme.colorScheme.primary
            )
            .clip(SquircleShape)
            .padding(8.dp)
    ) {
        AsyncImage(
            model = ImageUtils.getHighResThumbnailUrl(song.thumbnailUrl) ?: song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(184.dp)
                .clip(SquircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TvPlaylistCard(playlist: PlaylistDisplayItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .dpadFocusable(
                onClick = onClick,
                shape = SquircleShape,
                focusedScale = 1.1f
            )
            .clip(SquircleShape)
            .padding(8.dp)
    ) {
        AsyncImage(
            model = ImageUtils.getHighResThumbnailUrl(playlist.thumbnailUrl) ?: playlist.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(184.dp)
                .clip(SquircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Playlist",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TvAlbumCard(album: Album, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .dpadFocusable(
                onClick = onClick,
                shape = SquircleShape,
                focusedScale = 1.1f
            )
            .clip(SquircleShape)
            .padding(8.dp)
    ) {
        AsyncImage(
            model = ImageUtils.getHighResThumbnailUrl(album.thumbnailUrl) ?: album.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(184.dp)
                .clip(SquircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Album",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TvArtistCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    onClick: () -> Unit
) {
    TvArtworkCard(
        title = title,
        subtitle = subtitle,
        imageUrl = imageUrl,
        onClick = onClick
    )
}

@Composable
private fun TvExploreCard(
    title: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .dpadFocusable(
                onClick = onClick,
                shape = SquircleShape,
                focusedScale = 1.1f
            )
            .clip(SquircleShape)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(184.dp)
                .clip(SquircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Interests,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Explore",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TvArtworkCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .dpadFocusable(
                onClick = onClick,
                shape = SquircleShape,
                focusedScale = 1.1f
            )
            .clip(SquircleShape)
            .padding(8.dp)
    ) {
        AsyncImage(
            model = ImageUtils.getHighResThumbnailUrl(imageUrl) ?: imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(184.dp)
                .clip(SquircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
