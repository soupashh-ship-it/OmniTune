package com.omnitune.app.ui.screens.search

import com.omnitune.app.ui.screens.SearchViewModel
import com.omnitune.app.ui.screens.SearchStatus
import com.omnitune.app.ui.screens.SearchUiState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel


import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import com.omnitune.app.R
import com.omnitune.innertube.models.ArtistItem

fun LazyListScope.artistSearchResults(
    artists: List<ArtistItem>,
    onNavigateToArtist: (String) -> Unit
) {
    if (artists.isNotEmpty()) {
        item(contentType = "section-artists") {
            SectionLabel(title = "Artists", count = artists.size)
        }
        items(
            items = artists,
            key = { "artist-${it.id}" },
            contentType = { "artist" },
        ) { artist ->
            SearchResultRow(
                title = artist.title,
                subtitle = "Artist",
                thumbnailUrl = artist.thumbnail,
                fallbackRes = R.drawable.ic_artist,
                circular = true,
                onClick = { onNavigateToArtist(artist.id) },
            )
        }
    }
}
