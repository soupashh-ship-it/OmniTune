/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.omnitune.app.innertube.pages

import com.omnitune.app.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
