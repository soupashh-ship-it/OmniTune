/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.runtime.Immutable
import com.omnitune.app.db.entities.Song

@Immutable
data class MoodChip(
    val id: String,
    val label: String,
    val query: String,
)

@Immutable
data class HomeCarouselItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String? = null,
    val song: Song? = null,
    val query: String? = null,
)

@Immutable
data class QuickPickItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String? = null,
    val song: Song? = null,
    val query: String? = null,
)

@Immutable
data class PlaylistShelfItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String? = null,
    val song: Song? = null,
    val query: String? = null,
)

@Immutable
data class HomeSection(
    val id: String,
    val title: String,
    val actionLabel: String? = null,
    val items: List<PlaylistShelfItem> = emptyList(),
)

