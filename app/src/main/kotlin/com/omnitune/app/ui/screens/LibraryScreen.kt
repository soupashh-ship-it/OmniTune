/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.omnitune.app.R
import com.omnitune.app.constants.OmniLibraryDesign
import com.omnitune.app.constants.OmniLibraryDesignKey
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
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val showLiked by com.omnitune.app.utils.rememberPreference(com.omnitune.app.constants.ShowLikedPlaylistKey, true)
    val showDownloaded by com.omnitune.app.utils.rememberPreference(com.omnitune.app.constants.ShowDownloadedPlaylistKey, true)
    val libraryDesign by rememberEnumPreference(OmniLibraryDesignKey, OmniLibraryDesign.DEFAULT)
    val compactLibrary = libraryDesign == OmniLibraryDesign.COMPACT_LIST
    val uiState by viewModel.uiState.collectAsState()
    val totalCount = uiState.librarySongCount + uiState.libraryAlbumCount + uiState.libraryArtistCount + uiState.playlistCount
    val hasQuickRows = uiState.likedCount > 0 || uiState.downloadCount > 0 || uiState.recentlyPlayed.isNotEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .padding(horizontal = if (compactLibrary) OmniSpacing.medium else OmniSpacing.section),
        verticalArrangement = Arrangement.spacedBy(if (compactLibrary) OmniSpacing.compact else OmniSpacing.medium),
    ) {
        item(contentType = "header") {
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.height(if (compactLibrary) OmniSpacing.medium else OmniSpacing.large))
            LibraryHeader(
                totalCount = totalCount,
                onDownloads = onNavigateToDownloads,
                onSearch = onNavigateToSearch,
            )
        }

        item(contentType = "tabs") {
            LibraryCategoryTabs(
                playlistCount = uiState.playlistCount,
                songCount = uiState.librarySongCount,
                albumCount = uiState.libraryAlbumCount,
                artistCount = uiState.libraryArtistCount,
                compact = compactLibrary,
                onPlaylists = onNavigateToPlaylists,
                onSongs = onNavigateToSongs,
                onAlbums = onNavigateToAlbums,
                onArtists = onNavigateToArtists,
            )
        }

        item(contentType = "quick-title") {
            OmniSectionHeader(title = "Quick access")
        }

        if (hasQuickRows) {
            if (uiState.likedCount > 0) {
                if (showLiked) {
                    item(key = "liked", contentType = "route-row") {
                        LibraryRouteRow(
                            painter = painterResource(R.drawable.ic_favorite),
                            title = "Liked",
                            detail = countLabel(uiState.likedCount, "song"),
                            accent = OmniColors.Hot,
                            compact = compactLibrary,
                            onClick = onNavigateToLiked,
                        )
                    }
                }
            }
            if (uiState.downloadCount > 0) {
                if (showDownloaded) {
                    item(key = "downloaded", contentType = "route-row") {
                        LibraryRouteRow(
                            painter = painterResource(R.drawable.ic_download),
                            title = "Downloaded",
                            detail = countLabel(uiState.downloadCount, "song"),
                            accent = OmniColors.Downloaded,
                            compact = compactLibrary,
                            onClick = onNavigateToDownloads,
                        )
                    }
                }
            }
            if (uiState.recentlyPlayed.isNotEmpty()) {
                item(key = "recent", contentType = "route-row") {
                    LibraryRouteRow(
                        painter = painterResource(R.drawable.ic_history),
                        title = "Recently played",
                        detail = countLabel(uiState.recentlyPlayed.size, "song"),
                        accent = OmniColors.OmniAccentSecondary,
                        compact = compactLibrary,
                        onClick = onNavigateToRecentlyPlayed,
                    )
                }
            }
        } else {
            item(contentType = "empty") {
                LibraryEmptyHub(onSearch = onNavigateToSearch)
            }
        }

        item(contentType = "browse-title") {
            OmniSectionHeader(title = "Browse")
        }

        item(key = "browse-playlists", contentType = "route-row") {
            LibraryRouteRow(
                painter = painterResource(R.drawable.ic_list),
                title = "Playlists",
                detail = countLabel(uiState.playlistCount, "playlist"),
                accent = OmniColors.OmniAccentPrimary,
                compact = compactLibrary,
                onClick = onNavigateToPlaylists,
            )
        }
        item(key = "browse-songs", contentType = "route-row") {
            LibraryRouteRow(
                painter = painterResource(R.drawable.ic_album),
                title = "Songs",
                detail = countLabel(uiState.librarySongCount, "song"),
                accent = OmniColors.OmniAccentSecondary,
                compact = compactLibrary,
                onClick = onNavigateToSongs,
            )
        }
        item(key = "browse-artists", contentType = "route-row") {
            LibraryRouteRow(
                painter = painterResource(R.drawable.ic_artist),
                title = "Artists",
                detail = countLabel(uiState.libraryArtistCount, "artist"),
                accent = OmniColors.OmniAccentTertiary,
                compact = compactLibrary,
                onClick = onNavigateToArtists,
            )
        }
        item(key = "browse-albums", contentType = "route-row") {
            LibraryRouteRow(
                painter = painterResource(R.drawable.ic_album),
                title = "Albums",
                detail = countLabel(uiState.libraryAlbumCount, "album"),
                accent = OmniColors.OmniAccentWarm,
                compact = compactLibrary,
                onClick = onNavigateToAlbums,
            )
        }
        item(key = "browse-search", contentType = "route-row") {
            LibraryRouteRow(
                painter = painterResource(R.drawable.ic_search),
                title = "Find music",
                detail = "Search OmniTune",
                accent = OmniColors.OmniAccentSecondary,
                compact = compactLibrary,
                onClick = onNavigateToSearch,
            )
        }

        item(contentType = "bottom-spacer") { Spacer(modifier = Modifier.height(OmniChrome.BottomContentPaddingWithPlayer)) }
    }
}

@Composable
private fun LibraryHeader(
    totalCount: Int,
    onDownloads: () -> Unit,
    onSearch: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Library",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = if (totalCount == 0) "Saved music will appear here" else countLabel(totalCount, "saved item"),
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextSecondary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact)) {
            IconButtonSurface(
                painter = painterResource(R.drawable.ic_download),
                contentDescription = "Downloads",
                onClick = onDownloads,
            )
            IconButtonSurface(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Search",
                onClick = onSearch,
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
    compact: Boolean,
    onPlaylists: () -> Unit,
    onSongs: () -> Unit,
    onAlbums: () -> Unit,
    onArtists: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
    ) {
        LibraryTabChip("Playlists", playlistCount.toString(), onPlaylists, Modifier.weight(1f), compact)
        LibraryTabChip("Songs", songCount.toString(), onSongs, Modifier.weight(1f), compact)
        LibraryTabChip("Albums", albumCount.toString(), onAlbums, Modifier.weight(1f), compact)
        LibraryTabChip("Artists", artistCount.toString(), onArtists, Modifier.weight(1f), compact)
    }
}

@Composable
private fun LibraryTabChip(
    title: String,
    count: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(
        modifier = modifier
            .clip(OmniShapes.Medium)
            .background(OmniColors.SurfaceRaised)
            .border(
                width = 1.dp,
                color = OmniColors.SurfaceHairline,
                shape = OmniShapes.Medium,
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (compact) OmniSpacing.micro else OmniSpacing.compact,
                vertical = if (compact) OmniSpacing.compact else OmniSpacing.small,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OmniColors.OmniAccentSecondary,
            maxLines = 1,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = OmniColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
            .size(48.dp)
            .clip(OmniShapes.Pill)
            .background(OmniColors.SurfaceQuiet.copy(alpha = 0.58f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = OmniColors.TextPrimary,
            modifier = Modifier.size(22.dp),
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
