package com.omnitune.app.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queue")
data class QueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String?,
    val mediaIdList: String, // Comma-separated media IDs
    val startIndex: Int,
    val position: Long,
    val playbackSourceType: String? = null,
    val playbackSourceId: String? = null,
    val playbackSourceTitle: String? = null,
    val playbackSeedSongId: String? = null,
    val playbackGenre: String? = null,
    val playbackMood: String? = null,
    val playbackArtist: String? = null,
    @ColumnInfo(defaultValue = "1")
    val playbackAllowAutoplay: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val playbackShuffledCollection: Boolean = false,
)
