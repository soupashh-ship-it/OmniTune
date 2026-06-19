package com.omnitune.app.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queue")
data class QueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String?,
    val mediaIdList: String, // Comma-separated media IDs
    val startIndex: Int,
    val position: Long
)
