package com.omnitune.app.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import com.omnitune.app.db.entities.*
import com.omnitune.app.db.models.*

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue ORDER BY id DESC LIMIT 1")
    suspend fun getQueue(): com.omnitune.app.db.entities.QueueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveQueue(queue: com.omnitune.app.db.entities.QueueEntity)

    @Query("DELETE FROM queue")
    suspend fun clearQueue()

}
