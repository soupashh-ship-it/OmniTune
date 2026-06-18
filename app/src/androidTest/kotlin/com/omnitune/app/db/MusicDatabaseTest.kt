package com.omnitune.app.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.omnitune.app.db.entities.SongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MusicDatabaseTest {

    @Test
    fun databaseIsCreated() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val internal = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java)
            .build()
        val database = MusicDatabase(internal)
        assertNotNull(database)

        val song = SongEntity(
            id = "test123",
            title = "Test Song",
            duration = 240,
            thumbnailUrl = "https://example.com/thumb.jpg"
        )
        database.insert(song)

        val retrieved = database.song("test123").first()
        assertNotNull(retrieved)
        assert(retrieved?.title == "Test Song")

        internal.close()
    }
}
