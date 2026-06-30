package com.omnitune.app.ui.screens.search

import com.omnitune.app.ui.screens.SearchViewModel
import com.omnitune.app.ui.screens.SearchStatus
import com.omnitune.app.ui.screens.SearchUiState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel


import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import com.omnitune.app.R
import com.omnitune.innertube.models.PlaylistItem

fun LazyListScope.playlistSearchResults(
    playlists: List<PlaylistItem>,
    onNavigateToPlaylist: (PlaylistItem) -> Unit,
) {
    if (playlists.isNotEmpty()) {
        item(contentType = "section-playlists") {
            SectionLabel(title = "Playlists", count = playlists.size)
        }
        items(
            items = playlists,
            key = { "playlist-${it.id}" },
            contentType = { "playlist" },
        ) { playlist ->
            SearchResultRow(
                title = playlist.title,
                subtitle = playlist.author?.name ?: "Playlist",
                thumbnailUrl = playlist.thumbnail,
                fallbackRes = R.drawable.ic_list,
                onClick = { onNavigateToPlaylist(playlist) },
                statusText = "Open collection",
            )
        }
    }
}
