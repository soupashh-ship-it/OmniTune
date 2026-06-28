package com.omnitune.app.ui.screens.search

import com.omnitune.app.ui.screens.SearchViewModel
import com.omnitune.app.ui.screens.SearchStatus
import com.omnitune.app.ui.screens.SearchUiState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel


import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import com.omnitune.app.R
import com.omnitune.innertube.models.AlbumItem

fun LazyListScope.albumSearchResults(
    albums: List<AlbumItem>,
    onNavigateToAlbum: (String) -> Unit
) {
    if (albums.isNotEmpty()) {
        item(contentType = "section-albums") {
            SectionLabel(title = "Albums", count = albums.size)
        }
        items(
            items = albums,
            key = { "album-${it.browseId}" },
            contentType = { "album" },
        ) { album ->
            SearchResultRow(
                title = album.title,
                subtitle = album.artists?.joinToString(", ") { it.name }.orEmpty().ifBlank { "Album" },
                thumbnailUrl = album.thumbnail,
                fallbackRes = R.drawable.ic_album,
                onClick = { onNavigateToAlbum(album.browseId) },
            )
        }
    }
}
