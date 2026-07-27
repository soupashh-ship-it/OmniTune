/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.omnitune.app.R
import com.omnitune.app.constants.OmniLibraryDesign
import com.omnitune.app.constants.OmniLibraryDesignKey
import com.omnitune.app.db.entities.Song
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.component.OmniSectionHeader
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.utils.rememberEnumPreference

@Composable
fun LibraryScreen(
    onNavigateToSearch: () -> Unit = {},
    onNavigateToLiked: () -> Unit = {},
    onNavigateToSongs: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToRecentlyPlayed: () -> Unit = {},
    onNavigateToArtists: () -> Unit = {},
    onNavigateToAlbums: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onPlaySong: (Song) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val showLiked by com.omnitune.app.utils.rememberPreference(com.omnitune.app.constants.ShowLikedPlaylistKey, true)
    val showDownloaded by com.omnitune.app.utils.rememberPreference(com.omnitune.app.constants.ShowDownloadedPlaylistKey, true)
    val libraryDesign by rememberEnumPreference(OmniLibraryDesignKey, OmniLibraryDesign.DEFAULT)
    val compactLibrary = libraryDesign == OmniLibraryDesign.COMPACT_LIST
    val uiState by viewModel.uiState.collectAsState()
    val totalCount = uiState.librarySongCount + uiState.libraryAlbumCount + uiState.libraryArtistCount + uiState.playlistCount
    val quickSongs = buildList {
        uiState.likedSongs.firstOrNull()?.let(::add)
        uiState.recentlyPlayed.map { it.song }.distinctBy { it.id }.take(3).forEach(::add)
    }.distinctBy { it.id }.take(4)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(OmniColors.BackgroundGradient)
            .padding(horizontal = OmniSpacing.screenHorizontalCompact),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium),
    ) {
        item(contentType = "header") {
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.height(OmniSpacing.small))
            LibraryHeader(
                totalCount = totalCount,
                onSearch = onNavigateToSearch,
                onSettings = onNavigateToSettings,
            )
        }

        item(contentType = "tabs") {
            LibraryCategoryTabs(
                playlistCount = uiState.playlistCount,
                songCount = uiState.librarySongCount,
                albumCount = uiState.libraryAlbumCount,
                artistCount = uiState.libraryArtistCount,
                onPlaylists = onNavigateToPlaylists,
                onSongs = onNavigateToSongs,
                onAlbums = onNavigateToAlbums,
                onArtists = onNavigateToArtists,
            )
        }

        item(contentType = "quick-title") {
            OmniSectionHeader(title = "Quick access", action = "Edit")
        }

        item(contentType = "quick-access") {
            LibraryQuickAccessRail(
                songs = quickSongs,
                likedCount = uiState.likedCount,
                showLiked = showLiked,
                onLiked = onNavigateToLiked,
                onPlaySong = onPlaySong,
            )
        }

        item(contentType = "collections") {
            LibraryCollectionGrid(
                playlistCount = uiState.playlistCount,
                songCount = uiState.librarySongCount,
                albumCount = uiState.libraryAlbumCount,
                artistCount = uiState.libraryArtistCount,
                downloadCount = uiState.downloadCount,
                likedCount = uiState.likedCount,
                recentCount = uiState.recentlyPlayed.size,
                showLiked = showLiked,
                showDownloaded = showDownloaded,
                onRecent = onNavigateToRecentlyPlayed,
                onPlaylists = onNavigateToPlaylists,
                onSongs = onNavigateToSongs,
                onArtists = onNavigateToArtists,
                onAlbums = onNavigateToAlbums,
                onDownloads = onNavigateToDownloads,
                onLiked = onNavigateToLiked,
            )
        }

        item(contentType = "bottom-spacer") { Spacer(modifier = Modifier.height(OmniChrome.BottomContentPaddingWithPlayer)) }
    }
}

@Composable
private fun LibraryHeader(
    totalCount: Int,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_omnitune_logo),
            contentDescription = null,
            modifier = Modifier.size(30.dp),
        )
        Spacer(modifier = Modifier.width(OmniSpacing.compact))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "OmniTune",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = if (totalCount == 0) "Your music, your vibe." else "$totalCount saved items",
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextSecondary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact)) {
            IconButtonSurface(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Search",
                onClick = onSearch,
            )
            IconButtonSurface(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "Settings",
                onClick = onSettings,
            )
        }
    }
}

@Composable
private fun LibraryCategoryTabs(
    playlistCount: Int,
    songCount: Int,
    albumCount: Int,
    artistCount: Int,
    onPlaylists: () -> Unit,
    onSongs: () -> Unit,
    onAlbums: () -> Unit,
    onArtists: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        LibraryTabChip("Playlists", playlistCount.toString(), selected = true, onPlaylists, Modifier.weight(1f))
        LibraryTabChip("Songs", songCount.toString(), selected = false, onSongs, Modifier.weight(1f))
        LibraryTabChip("Albums", albumCount.toString(), selected = false, onAlbums, Modifier.weight(1f))
        LibraryTabChip("Artists", artistCount.toString(), selected = false, onArtists, Modifier.weight(1f))
    }
}

@Composable
private fun LibraryTabChip(
    title: String,
    count: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(OmniShapes.Pill)
            .background(if (selected) OmniColors.OmniAccentPrimary else OmniColors.SurfaceQuiet)
            .border(
                width = 1.dp,
                color = if (selected) OmniColors.OmniAccentPrimary else OmniColors.SurfaceHairline,
                shape = OmniShapes.Pill,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = OmniSpacing.micro),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (selected) OmniColors.TextOnAccent else OmniColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LibraryQuickAccessRail(
    songs: List<Song>,
    likedCount: Int,
    showLiked: Boolean,
    onLiked: () -> Unit,
    onPlaySong: (Song) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact)) {
        if (showLiked) {
            item(key = "liked-songs") {
                LibraryQuickAccessItem(
                    title = "Liked Songs",
                    subtitle = countLabel(likedCount, "song"),
                    thumbnailUrl = null,
                    icon = R.drawable.ic_favorite,
                    accent = OmniColors.Hot,
                    onClick = onLiked,
                )
            }
        }
        items(songs, key = { it.id }) { song ->
            LibraryQuickAccessItem(
                title = song.title,
                subtitle = song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown artist" },
                thumbnailUrl = song.thumbnailUrl,
                icon = R.drawable.ic_play_arrow,
                accent = OmniColors.OmniAccentPrimary,
                onClick = { onPlaySong(song) },
            )
        }
    }
}

@Composable
private fun LibraryQuickAccessItem(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    icon: Int,
    accent: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(OmniShapes.ArtworkMedium)
                .background(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.38f), OmniColors.SurfaceRaised),
                    ),
                )
                .border(1.dp, OmniColors.SurfaceHairline, OmniShapes.ArtworkMedium),
            contentAlignment = Alignment.Center,
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(38.dp),
                )
            }
            if (!thumbnailUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(26.dp)
                        .clip(OmniShapes.Pill)
                        .background(OmniColors.OmniAccentPrimary.copy(alpha = 0.88f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_arrow),
                        contentDescription = "Play $title",
                        tint = OmniColors.TextOnAccent,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextPrimary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = OmniColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LibraryCollectionGrid(
    playlistCount: Int,
    songCount: Int,
    albumCount: Int,
    artistCount: Int,
    downloadCount: Int,
    likedCount: Int,
    recentCount: Int,
    showLiked: Boolean,
    showDownloaded: Boolean,
    onRecent: () -> Unit,
    onPlaylists: () -> Unit,
    onSongs: () -> Unit,
    onArtists: () -> Unit,
    onAlbums: () -> Unit,
    onDownloads: () -> Unit,
    onLiked: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.compact)) {
        Row(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact)) {
            LibraryCollectionCard(
                title = "Recently played",
                detail = countLabel(recentCount, "item"),
                icon = R.drawable.ic_history,
                accent = OmniColors.OmniAccentSecondary,
                modifier = Modifier.weight(1f),
                onClick = onRecent,
            )
            LibraryCollectionCard(
                title = "Playlists",
                detail = countLabel(playlistCount, "playlist"),
                icon = R.drawable.ic_list,
                accent = OmniColors.OmniAccentPrimary,
                modifier = Modifier.weight(1f),
                onClick = onPlaylists,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact)) {
            LibraryCollectionCard(
                title = "Songs",
                detail = countLabel(songCount, "song"),
                icon = R.drawable.ic_album,
                accent = OmniColors.OmniAccentTertiary,
                modifier = Modifier.weight(1f),
                onClick = onSongs,
            )
            LibraryCollectionCard(
                title = "Artists",
                detail = countLabel(artistCount, "artist"),
                icon = R.drawable.ic_artist,
                accent = OmniColors.OmniAccentWarm,
                modifier = Modifier.weight(1f),
                onClick = onArtists,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact)) {
            LibraryCollectionCard(
                title = "Albums",
                detail = countLabel(albumCount, "album"),
                icon = R.drawable.ic_album,
                accent = OmniColors.TextSecondary,
                modifier = Modifier.weight(1f),
                onClick = onAlbums,
            )
            if (showDownloaded) {
                LibraryCollectionCard(
                    title = "Downloads",
                    detail = countLabel(downloadCount, "song"),
                    icon = R.drawable.ic_download,
                    accent = OmniColors.Downloaded,
                    modifier = Modifier.weight(1f),
                    onClick = onDownloads,
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        if (showLiked) {
            LibraryCollectionCard(
                title = "Liked Songs",
                detail = countLabel(likedCount, "song"),
                icon = R.drawable.ic_favorite,
                accent = OmniColors.Hot,
                modifier = Modifier.fillMaxWidth(),
                onClick = onLiked,
            )
        }
    }
}

@Composable
private fun LibraryCollectionCard(
    title: String,
    detail: String,
    icon: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(86.dp)
            .clip(OmniShapes.Medium)
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.20f), OmniColors.SurfaceRaised),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.30f), OmniShapes.Medium)
            .clickable(onClick = onClick)
            .padding(OmniSpacing.compact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(OmniShapes.Pill)
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(OmniSpacing.compact))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
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
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Medium)
            .background(OmniColors.SurfaceSubtle.copy(alpha = 0.46f))
            .clickable(onClick = onClick)
            .padding(if (compact) OmniSpacing.compact else OmniSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LibraryIconTile(painter = painter, accent = accent, size = if (compact) 40.dp else 48.dp)
        Spacer(modifier = Modifier.width(if (compact) OmniSpacing.compact else OmniSpacing.medium))
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
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = null,
            tint = OmniColors.TextTertiary,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer(rotationZ = 180f),
        )
    }
}

@Composable
private fun LibraryEmptyHub(onSearch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Large)
            .background(OmniColors.SurfaceSubtle.copy(alpha = 0.42f))
            .border(
                width = 1.dp,
                color = OmniColors.SurfaceHairline,
                shape = OmniShapes.Large,
            )
            .animateContentSize()
            .padding(OmniSpacing.large),
    ) {
        Text(
            text = "Nothing saved yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = OmniColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(OmniSpacing.micro))
        Text(
            text = "Like songs, download tracks, or play music to build your library.",
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(OmniSpacing.medium))
        LibraryRouteRow(
            painter = painterResource(R.drawable.ic_search),
            title = "Start with Search",
            detail = "Find something to save",
            accent = OmniColors.OmniAccentSecondary,
            onClick = onSearch,
        )
    }
}

@Composable
private fun LibraryIconTile(
    painter: Painter,
    accent: Color,
    size: androidx.compose.ui.unit.Dp = 48.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(OmniShapes.Small)
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun IconButtonSurface(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(OmniShapes.Pill)
            .background(OmniColors.SurfaceQuiet.copy(alpha = 0.58f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = OmniColors.TextPrimary,
            modifier = Modifier.size(18.dp),
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
