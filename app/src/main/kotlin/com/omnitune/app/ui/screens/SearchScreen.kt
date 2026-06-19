/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.omnitune.app.R
import com.omnitune.app.db.entities.SearchHistory
import com.omnitune.innertube.models.AlbumItem
import com.omnitune.innertube.models.ArtistItem
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.models.SongItem
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.component.OmniSectionHeader
import com.omnitune.app.ui.component.OmniTuneLoader
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes

private val moodChips = listOf(
    "Late Night" to "late night lofi vibes",
    "Gym" to "workout energetic phonk",
    "Rain" to "relaxing rain music",
    "Focus" to "deep focus space ambient",
    "Lofi" to "lofi hip hop beats"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onPlaySong: (SongItem) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val textFieldValue = remember { mutableStateOf(TextFieldValue(uiState.query)) }
    LaunchedEffect(Unit) { snapshotFlow { uiState.query }.collect { q -> if (q != textFieldValue.value.text) textFieldValue.value = TextFieldValue(q) } }

    Column(modifier = Modifier.fillMaxSize().background(OmniColors.Background).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp).clip(OmniShapes.SM).border(1.dp, OmniColors.GlassBorder, OmniShapes.SM).background(OmniColors.GlassSurface)) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back", tint = OmniColors.TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            TextField(value = textFieldValue.value, onValueChange = { textFieldValue.value = it; viewModel.onQueryChanged(it.text) },
                modifier = Modifier.weight(1f).height(48.dp).clip(OmniShapes.MD).background(OmniColors.GlassSurface).border(1.dp, OmniColors.GlassBorder, OmniShapes.MD),
                placeholder = { Text("Search songs, artists, albums...", color = OmniColors.TextMuted, fontSize = 14.sp) },
                leadingIcon = { Icon(painterResource(android.R.drawable.ic_menu_search), contentDescription = null, tint = OmniColors.TextMuted, modifier = Modifier.size(20.dp)) },
                trailingIcon = { if (uiState.query.isNotEmpty()) IconButton(onClick = { viewModel.clearQuery() }, modifier = Modifier.size(32.dp)) { Icon(painterResource(android.R.drawable.ic_menu_close_clear_cancel), contentDescription = "Clear", tint = OmniColors.TextMuted, modifier = Modifier.size(18.dp)) } },
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = OmniColors.Primary, focusedTextColor = OmniColors.TextPrimary, unfocusedTextColor = OmniColors.TextPrimary),
                singleLine = true)
        }
        when {
            uiState.isSearching -> Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { OmniTuneLoader(size = 48.dp) }
            uiState.error != null -> EmptyPlaceholder(icon = android.R.drawable.ic_menu_search, text = uiState.error ?: "Search failed")
            uiState.query.isEmpty() -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    item {
                        OmniSectionHeader(title = "Mood"); Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            moodChips.forEach { (label, query) ->
                                val isActive = uiState.query == query
                                val bgColor by androidx.compose.animation.animateColorAsState(
                                    targetValue = if (isActive) OmniColors.Primary.copy(alpha = 0.2f) else OmniColors.GlassSurface,
                                    label = "bgColor"
                                )
                                val borderColor by androidx.compose.animation.animateColorAsState(
                                    targetValue = if (isActive) OmniColors.Primary.copy(alpha = 0.5f) else OmniColors.GlassBorder,
                                    label = "borderColor"
                                )
                                val textColor by androidx.compose.animation.animateColorAsState(
                                    targetValue = if (isActive) OmniColors.Primary else OmniColors.TextSecondary,
                                    label = "textColor"
                                )
                                Box(modifier = Modifier.clip(OmniShapes.Pill).border(1.dp, borderColor, OmniShapes.Pill).background(bgColor)
                                    .clickable(remember { MutableInteractionSource() }, indication = androidx.compose.material3.ripple(bounded = true, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f))) { 
                                        if (isActive) viewModel.clearQuery() else viewModel.onQueryChanged(query) 
                                    }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    Text(label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    if (uiState.searchHistory.isNotEmpty()) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Recent Searches", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = OmniColors.TextPrimary, modifier = Modifier.weight(1f))
                                Text("Clear all", color = OmniColors.Secondary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable(remember { MutableInteractionSource() }, indication = androidx.compose.material3.ripple(bounded = false, color = OmniColors.Secondary.copy(alpha = 0.2f))) { viewModel.clearSearchHistory() })
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(uiState.searchHistory) { item ->
                            Row(modifier = Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, indication = androidx.compose.material3.ripple(bounded = true, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f))) { viewModel.onQueryChanged(item.query) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(R.drawable.ic_list), contentDescription = null, tint = OmniColors.TextMuted, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(item.query, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, color = OmniColors.TextSecondary)
                            }
                        }
                    }
                    if (uiState.searchHistory.isEmpty()) item { EmptyPlaceholder(icon = android.R.drawable.ic_menu_search, text = "Search for your favorite songs, artists, and albums") }
                }
            }
            uiState.songs.isEmpty() && uiState.artists.isEmpty() && uiState.albums.isEmpty() && uiState.playlists.isEmpty() -> EmptyPlaceholder(icon = android.R.drawable.ic_menu_search, text = "No results for \"${uiState.query}\"")
            else -> SearchResultsContent(uiState.songs, uiState.artists, uiState.albums, uiState.playlists, onNavigateToAlbum, onNavigateToArtist, onPlaySong)
        }
    }
}

@Composable
private fun SearchResultsContent(songs: List<SongItem>, artists: List<ArtistItem>, albums: List<AlbumItem>, playlists: List<PlaylistItem>, onNavigateToAlbum: (String) -> Unit, onNavigateToArtist: (String) -> Unit, onPlaySong: (SongItem) -> Unit = {}) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (songs.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(8.dp)); OmniSectionHeader(title = "Songs", action = "${songs.size} results"); Spacer(modifier = Modifier.height(8.dp)) }
            items(songs) { song -> GlassSearchRow(song.title, song.artists.joinToString(", ") { it.name }, song.thumbnail, { onPlaySong(song) }) }
        }
        if (artists.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(16.dp)); OmniSectionHeader(title = "Artists", action = "${artists.size} results"); Spacer(modifier = Modifier.height(8.dp)) }
            items(artists) { artist -> GlassSearchRow(artist.title, "Artist", artist.thumbnail, { onNavigateToArtist(artist.id) }, circular = true) }
        }
        if (albums.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(16.dp)); OmniSectionHeader(title = "Albums", action = "${albums.size} results"); Spacer(modifier = Modifier.height(8.dp)) }
            items(albums) { album -> GlassSearchRow(album.title, album.artists?.joinToString(", ") { it.name } ?: "", album.thumbnail, { onNavigateToAlbum(album.browseId) }) }
        }
        if (playlists.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(16.dp)); OmniSectionHeader(title = "Playlists", action = "${playlists.size} results"); Spacer(modifier = Modifier.height(8.dp)) }
            items(playlists) { playlist -> GlassSearchRow(playlist.title, playlist.author?.name ?: "", playlist.thumbnail, {}, fallbackRes = R.drawable.ic_list) }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun GlassSearchRow(title: String, subtitle: String, thumbnailUrl: String?, onClick: () -> Unit, circular: Boolean = false, fallbackRes: Int = android.R.drawable.ic_media_play) {
    Row(modifier = Modifier.fillMaxWidth().clip(OmniShapes.SM).clickable(remember { MutableInteractionSource() }, indication = androidx.compose.material3.ripple(bounded = true), onClick = onClick)
        .background(OmniColors.GlassSurface).border(1.dp, OmniColors.GlassBorderLight, OmniShapes.SM).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(44.dp).clip(if (circular) RoundedCornerShape(22.dp) else OmniShapes.SM).background(OmniColors.GlassSurfaceStrong), contentAlignment = Alignment.Center) {
            if (thumbnailUrl != null) AsyncImage(model = thumbnailUrl, contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(if (circular) RoundedCornerShape(22.dp) else OmniShapes.SM), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            else Icon(painterResource(fallbackRes), contentDescription = null, tint = OmniColors.TextMuted, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = OmniColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotEmpty()) Text(subtitle, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = OmniColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
