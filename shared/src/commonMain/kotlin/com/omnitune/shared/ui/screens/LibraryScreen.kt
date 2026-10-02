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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omnitune.shared.domain.models.AlbumItem
import com.omnitune.shared.domain.models.PlaylistItem
import com.omnitune.shared.domain.models.SongItem
import com.omnitune.shared.ui.components.BounceButton
import com.omnitune.shared.ui.components.OmniArtwork
import com.omnitune.shared.ui.components.SquircleShape
import com.omnitune.shared.ui.theme.PillShape

enum class LibraryFilter(val displayName: String) {
    PLAYLISTS("Playlists"),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    DOWNLOADED("Downloaded")
}

enum class LibrarySortCriteria(val displayName: String) {
    TITLE("Title"),
    DATE_ADDED("Date Added"),
    ARTIST("Artist"),
    PLAY_COUNT("Play Count"),
    DURATION("Duration")
}

enum class LibraryViewMode {
    GRID,
    LIST
}

/**
 * Authentic SuvMusic LibraryScreen with pill filter chips, Grid/List view toggle,
 * sort criteria menu, and empty state CTA ("Discover Music").
 */
@Composable
fun LibraryScreen(
    onSongClick: (SongItem) -> Unit,
    onPlaylistClick: (PlaylistItem) -> Unit = {},
    onAlbumClick: (AlbumItem) -> Unit = {},
    onExploreMusicClick: () -> Unit = {},
    songs: List<SongItem> = emptyList(),
    playlists: List<PlaylistItem> = emptyList(),
    albums: List<AlbumItem> = emptyList(),
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    var selectedFilter by remember { mutableStateOf(LibraryFilter.PLAYLISTS) }
    var viewMode by remember { mutableStateOf(LibraryViewMode.LIST) }
    var sortCriteria by remember { mutableStateOf(LibrarySortCriteria.TITLE) }
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Library Title Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Library",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // View Mode Toggle (Grid vs List)
                IconButton(
                    onClick = {
                        viewMode = if (viewMode == LibraryViewMode.GRID) LibraryViewMode.LIST else LibraryViewMode.GRID
                    }
                ) {
                    Icon(
                        imageVector = if (viewMode == LibraryViewMode.GRID) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                        contentDescription = "Toggle View Mode",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Sort Menu
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        LibrarySortCriteria.entries.forEach { criteria ->
                            DropdownMenuItem(
                                text = { Text(criteria.displayName) },
                                onClick = {
                                    sortCriteria = criteria
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Pill Filter Chips (Playlists, Songs, Albums, Artists, Downloaded)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(LibraryFilter.entries) { filter ->
                val isSelected = (selectedFilter == filter)
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.displayName) },
                    shape = PillShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor,
                        selectedLabelColor = MaterialTheme.colorScheme.background,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Content Display or Empty State
        when (selectedFilter) {
            LibraryFilter.SONGS -> {
                if (songs.isEmpty()) {
                    EmptyLibraryState(onExploreClick = onExploreMusicClick, accentColor = accentColor)
                } else if (viewMode == LibraryViewMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(songs) { song ->
                            Column(modifier = Modifier.clickable { onSongClick(song) }) {
                                OmniArtwork(
                                    url = song.thumbnailUrl,
                                    size = 150.dp,
                                    shape = SquircleShape(cornerRadius = 14.dp, cornerSmoothing = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(songs) { song ->
                            MusicCard(song = song, onClick = { onSongClick(song) })
                        }
                    }
                }
            }
            LibraryFilter.PLAYLISTS -> {
                if (playlists.isEmpty()) {
                    EmptyLibraryState(onExploreClick = onExploreMusicClick, accentColor = accentColor)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(playlists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPlaylistClick(playlist) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OmniArtwork(
                                    url = playlist.thumbnailUrl,
                                    size = 56.dp,
                                    shape = SquircleShape(cornerRadius = 12.dp, cornerSmoothing = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = playlist.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "${playlist.songCount} songs",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                EmptyLibraryState(onExploreClick = onExploreMusicClick, accentColor = accentColor)
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(
    onExploreClick: () -> Unit,
    accentColor: Color
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Your library is empty",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Discover and save songs, albums, and playlists",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            ElevatedButton(
                onClick = onExploreClick,
                shape = PillShape,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = accentColor,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text(
                    text = "Discover Music",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
