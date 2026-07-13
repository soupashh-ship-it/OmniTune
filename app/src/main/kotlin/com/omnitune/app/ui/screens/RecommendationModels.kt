/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.runtime.Immutable
import com.omnitune.app.db.entities.EventWithSong
import com.omnitune.app.db.entities.Song
import com.omnitune.app.db.entities.SongSkipEntity

@Immutable
data class HomeRecommendationInput(
    val events: List<EventWithSong>,
    val quickPickSongs: List<Song>,
    val likedSongs: List<Song>,
    val librarySongs: List<Song>,
    val downloadedSongs: List<Song>,
    val forgottenFavorites: List<Song>,
    val skips: List<SongSkipEntity>,
)

@Immutable
data class HomeRecommendationResult(
    val sections: List<HomeSection>,
    val topSongs: List<Song>,
    val topArtists: List<HomeArtistSeed>,
)

@Immutable
data class HomeArtistSeed(
    val name: String,
    val query: String,
    val score: Double,
    val thumbnailUrl: String? = null,
)
