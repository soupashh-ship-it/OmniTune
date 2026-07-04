package com.omnitune.app.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import com.omnitune.app.db.entities.*
import com.omnitune.app.db.models.*

@Dao
interface EventDao {
    @Transaction
    @Query("SELECT * FROM event ORDER BY rowId DESC")
    fun events(): Flow<List<EventWithSong>>

    @Transaction
    @Query("SELECT * FROM event ORDER BY rowId ASC LIMIT 1")
    fun firstEvent(): Flow<EventWithSong?>

    @Transaction
    @Query("DELETE FROM event")
    fun clearListenHistory()

    @Transaction
    fun insertRecentEvent(songId: String, playTime: Long) {
        insert(Event(songId = songId, timestamp = LocalDateTime.now(), playTime = playTime))
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(event: Event)

    @Delete
    fun delete(event: Event)

}
