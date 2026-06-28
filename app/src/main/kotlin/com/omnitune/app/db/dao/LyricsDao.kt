package com.omnitune.app.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import com.omnitune.app.db.entities.*
import com.omnitune.app.db.models.*

@Dao
interface LyricsDao {
    @Transaction
    @Query("SELECT * FROM lyrics WHERE id = :id")
    fun lyrics(id: String?): Flow<LyricsEntity?>

    @Upsert
    fun upsert(lyrics: LyricsEntity)

    @Delete
    fun delete(lyrics: LyricsEntity)

}
