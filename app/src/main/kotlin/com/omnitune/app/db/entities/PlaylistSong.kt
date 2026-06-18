/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 * Licensed Under GPL-3.0
 */



package com.omnitune.app.db.entities

import androidx.room.Embedded
import androidx.room.Relation

data class PlaylistSong(
    @Embedded val map: PlaylistSongMap,
    @Relation(
        parentColumn = "songId",
        entityColumn = "id",
        entity = SongEntity::class,
    )
    val song: Song,
)
