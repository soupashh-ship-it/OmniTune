package com.omnitune.app.ui.screens.search

import com.omnitune.app.ui.screens.SearchViewModel
import com.omnitune.app.ui.screens.SearchStatus
import com.omnitune.app.ui.screens.SearchUiState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel


import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import com.omnitune.app.R
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.innertube.models.SongItem
import timber.log.Timber

fun LazyListScope.songSearchResults(
    songs: List<SongItem>,
    onPlaySong: (List<SongItem>, Int) -> Unit,
    onPlayNext: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    sectionTitle: String = "Songs",
) {
    if (songs.isNotEmpty()) {
        item(contentType = "section-songs") {
            SectionLabel(title = sectionTitle, count = songs.size)
        }
        itemsIndexed(
            items = songs,
            key = { index, song -> "song-${song.id.ifBlank { index.toString() }}" },
            contentType = { _, _ -> "song" },
        ) { index, song ->
            SearchResultRow(
                title = song.title,
                subtitle = song.artists.joinToString(", ") { it.name }.ifBlank { "Song" },
                thumbnailUrl = song.thumbnail,
                fallbackRes = R.drawable.ic_play_arrow,
                onClick = {
                    Timber.tag("OmniTunePlaybackTrace").i("Search row clicked: ${song.title}")
                    onPlaySong(songs, index)
                },
                onPlayNext = { 
                    Timber.tag("OmniTuneSearch").i("Play Next clicked: ${song.title}")
                    onPlayNext(song) 
                },
                onAddToQueue = { 
                    Timber.tag("OmniTuneSearch").i("Add to Queue clicked: ${song.title}")
                    onAddToQueue(song) 
                },
                mediaMetadata = song.toMediaMetadata(),
            )
        }
    }
}
