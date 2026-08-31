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
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.omnitune.app.R
import com.omnitune.app.constants.OmniLibraryDesign
import com.omnitune.app.constants.OmniLibraryDesignKey
import com.omnitune.app.db.entities.Song
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.component.OmniSectionHeader
import com.omnitune.app.ui.theme.omniColors
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.omniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.omniColors
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.utils.rememberEnumPreference
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val totalCount = uiState.librarySongCount + uiState.libraryAlbumCount + uiState.libraryArtistCount + uiState.playlistCount
    val quickSongs = buildList {
        uiState.likedSongs.firstOrNull()?.let(::add)
        uiState.recentlyPlayed.map { it.song }.distinctBy { it.id }.take(3).forEach(::add)
    }.distinctBy { it.id }.take(4)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(omniColors().background)
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
                color = omniColors().textPrimary,
            )
            Text(
                text = if (totalCount == 0) "Your music, your vibe." else "$totalCount saved items",
                style = MaterialTheme.typography.bodyMedium,
                color = omniColors().textSecondary,
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = com.omnitune.app.ui.theme.PillShape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
                    accent = omniColors().accent,
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
                accent = omniColors().accent,
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
            .width(112.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(com.omnitune.app.ui.theme.SquircleShape)
                .background(
                    if (thumbnailUrl.isNullOrBlank()) {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFE84142),
                                Color(0xFFB31217),
                                Color(0xFF1E1012),
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF161922),
                                Color(0xFF0F1116),
                            )
                        )
                    }
                )
                .border(0.5.dp, omniColors().hairline, com.omnitune.app.ui.theme.SquircleShape),
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
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(OmniShapes.ArtworkSmall)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            if (!thumbnailUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(OmniShapes.Pill)
                        .background(omniColors().accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_arrow),
                        contentDescription = "Play $title",
                        tint = omniColors().textOnAccent,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = omniColors().textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = omniColors().textSecondary,
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (showLiked) {
            LibraryHeroLikedCard(
                likedCount = likedCount,
                onClick = onLiked,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LibraryCollectionCard(
                title = "Recently played",
                detail = countLabel(recentCount, "item"),
                icon = R.drawable.ic_history,
                badgeTint = Color(0xFF818CF8),
                badgeBg = Color(0xFF1B1E2D),
                modifier = Modifier.weight(1f),
                onClick = onRecent,
            )
            LibraryCollectionCard(
                title = "Playlists",
                detail = countLabel(playlistCount, "playlist"),
                icon = R.drawable.ic_list,
                badgeTint = Color(0xFF38BDF8),
                badgeBg = Color(0xFF14222B),
                modifier = Modifier.weight(1f),
                onClick = onPlaylists,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LibraryCollectionCard(
                title = "Tracks",
                detail = countLabel(songCount, "song"),
                icon = R.drawable.ic_music_note,
                badgeTint = Color(0xFFA78BFA),
                badgeBg = Color(0xFF201B2B),
                modifier = Modifier.weight(1f),
                onClick = onSongs,
            )
            LibraryCollectionCard(
                title = "Artists",
                detail = countLabel(artistCount, "artist"),
                icon = R.drawable.ic_artist,
                badgeTint = Color(0xFFFBBF24),
                badgeBg = Color(0xFF262016),
                modifier = Modifier.weight(1f),
                onClick = onArtists,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LibraryCollectionCard(
                title = "Albums",
                detail = countLabel(albumCount, "album"),
                icon = R.drawable.ic_album,
                badgeTint = Color(0xFF94A3B8),
                badgeBg = Color(0xFF1C2028),
                modifier = Modifier.weight(1f),
                onClick = onAlbums,
            )
            if (showDownloaded) {
                LibraryCollectionCard(
                    title = "Downloads",
                    detail = countLabel(downloadCount, "song"),
                    icon = R.drawable.ic_download,
                    badgeTint = Color(0xFF34D399),
                    badgeBg = Color(0xFF13251D),
                    modifier = Modifier.weight(1f),
                    onClick = onDownloads,
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LibraryHeroLikedCard(
    likedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .clip(OmniShapes.Medium)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF281114),
                        Color(0xFF181B22),
                        Color(0xFF11141A),
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = Color(0xFFE84142).copy(alpha = 0.28f),
                shape = OmniShapes.Medium,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(OmniShapes.Small)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFE84142),
                            Color(0xFFFF5252),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_favorite),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Liked Songs",
                style = MaterialTheme.typography.bodyLarge,
                color = omniColors().textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$likedCount favorite tracks",
                style = MaterialTheme.typography.bodySmall,
                color = omniColors().textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = null,
            tint = Color(0xFFE84142).copy(alpha = 0.70f),
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer(rotationZ = 180f),
        )
    }
}

@Composable
private fun LibraryCollectionCard(
    title: String,
    detail: String,
    icon: Int,
    badgeTint: Color,
    badgeBg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(74.dp)
            .clip(OmniShapes.Medium)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF161A22),
                        Color(0xFF101318),
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = Color(0xFF262B38).copy(alpha = 0.45f),
                shape = OmniShapes.Medium,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(OmniShapes.Small)
                .background(badgeBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = badgeTint,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = omniColors().textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = omniColors().textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = null,
            tint = omniColors().textTertiary.copy(alpha = 0.40f),
            modifier = Modifier
                .size(14.dp)
                .graphicsLayer(rotationZ = 180f),
        )
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
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF161A22),
                        Color(0xFF101318),
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = Color(0xFF262B38).copy(alpha = 0.45f),
                shape = OmniShapes.Medium,
            )
            .clickable(onClick = onClick)
            .padding(if (compact) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LibraryIconTile(painter = painter, accent = accent, size = if (compact) 38.dp else 44.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = omniColors().textPrimary,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = omniColors().textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = null,
            tint = omniColors().textTertiary.copy(alpha = 0.45f),
            modifier = Modifier
                .size(16.dp)
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
            .background(omniColors().surface.copy(alpha = 0.42f))
            .border(
                width = 1.dp,
                color = omniColors().hairline,
                shape = OmniShapes.Large,
            )
            .animateContentSize()
            .padding(OmniSpacing.large),
    ) {
        Text(
            text = "Nothing saved yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = omniColors().textPrimary,
        )
        Spacer(modifier = Modifier.height(OmniSpacing.micro))
        Text(
            text = "Like songs, download tracks, or play music to build your library.",
            style = MaterialTheme.typography.bodyMedium,
            color = omniColors().textSecondary,
        )
        Spacer(modifier = Modifier.height(OmniSpacing.medium))
        LibraryRouteRow(
            painter = painterResource(R.drawable.ic_search),
            title = "Start with Search",
            detail = "Find something to save",
            accent = omniColors().accentSecondary,
            onClick = onSearch,
        )
    }
}

@Composable
private fun LibraryIconTile(
    painter: Painter,
    accent: Color,
    size: androidx.compose.ui.unit.Dp = 44.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(OmniShapes.Small)
            .background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size((size.value * 0.5f).dp),
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
            .background(omniColors().surfaceQuiet.copy(alpha = 0.58f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = omniColors().textPrimary,
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
