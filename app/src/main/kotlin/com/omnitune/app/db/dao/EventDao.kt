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
    @Query("SELECT * FROM event WHERE id IN (SELECT MAX(id) FROM event GROUP BY songId) ORDER BY rowId DESC")
    fun recentEvents(): Flow<List<EventWithSong>>

    @Transaction
    @Query("SELECT * FROM event ORDER BY rowId ASC LIMIT 1")
    fun firstEvent(): Flow<EventWithSong?>

    @Transaction
    @Query("DELETE FROM event")
    fun clearListenHistory()

    @Query("DELETE FROM event WHERE timestamp < :beforeTimestamp")
    fun deleteEventsBefore(beforeTimestamp: Long)

    @Query(
        """
        SELECT IFNULL(SUM(playTime), 0)
        FROM event
        WHERE timestamp > :fromTimeStamp
          AND timestamp <= :toTimeStamp
        """,
    )
    fun totalListeningTime(
        fromTimeStamp: Long,
        toTimeStamp: Long,
    ): Flow<Long>

    @Query(
        """
        SELECT COUNT(1)
        FROM event
        WHERE timestamp > :fromTimeStamp
          AND timestamp <= :toTimeStamp
        """,
    )
    fun totalListenEvents(
        fromTimeStamp: Long,
        toTimeStamp: Long,
    ): Flow<Long>

    @Transaction
    fun insertRecentEvent(songId: String, playTime: Long) {
        insert(Event(songId = songId, timestamp = LocalDateTime.now(), playTime = playTime))
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(event: Event)

    @Delete
    fun delete(event: Event)

}
