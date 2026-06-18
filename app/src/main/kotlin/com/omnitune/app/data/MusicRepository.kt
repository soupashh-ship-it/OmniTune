package com.omnitune.app.data

import com.omnitune.app.db.entities.Song

interface MusicRepository {
    suspend fun saveSong(song: Song)
}
