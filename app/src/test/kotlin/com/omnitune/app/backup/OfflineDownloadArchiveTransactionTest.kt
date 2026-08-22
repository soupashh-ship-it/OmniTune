/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.backup

import android.content.Context
import android.os.Environment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.io.FileNotFoundException

class OfflineDownloadArchiveTransactionTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun replacePromotionRollsBackDownloadsAndMedia3Index() {
        val fixture = fixture()
        fixture.downloadTarget.mkdirs()
        File(fixture.downloadTarget, "old.mp3").writeText("old-audio")
        fixture.media3Database.writeText("old-index")
        writeIncomingStage(fixture.stage, audio = "new-audio", index = "new-index")

        val applied = OfflineDownloadArchive.applyStaged(fixture.context, fixture.stage, replaceExisting = true)
        assertEquals("new-audio", File(fixture.downloadTarget, "new.mp3").readText())
        assertEquals("new-index", fixture.media3Database.readText())

        applied.rollback()

        assertEquals("old-audio", File(fixture.downloadTarget, "old.mp3").readText())
        assertEquals("old-index", fixture.media3Database.readText())
    }

    @Test
    fun interruptedPromotionRestoresEarlierFileChanges() {
        val fixture = fixture(databaseParentIsFile = true)
        fixture.downloadTarget.mkdirs()
        File(fixture.downloadTarget, "old.mp3").writeText("old-audio")
        writeIncomingStage(fixture.stage, audio = "new-audio", index = "new-index")

        assertThrows(FileNotFoundException::class.java) {
            OfflineDownloadArchive.applyStaged(fixture.context, fixture.stage, replaceExisting = true)
        }

        assertTrue(File(fixture.downloadTarget, "old.mp3").isFile)
        assertEquals("old-audio", File(fixture.downloadTarget, "old.mp3").readText())
    }

    @Test
    fun readyReplaceStageIsCompletedAtNextApplicationStart() {
        val fixture = fixture()
        fixture.downloadTarget.mkdirs()
        File(fixture.downloadTarget, "old.mp3").writeText("old-audio")
        fixture.media3Database.writeText("old-index")
        writeIncomingStage(fixture.stage, audio = "new-audio", index = "new-index")
        OfflineDownloadArchive.markReady(fixture.stage, replaceExisting = true)

        assertTrue(OfflineDownloadArchive.applyPending(fixture.context))

        assertEquals("new-audio", File(fixture.downloadTarget, "new.mp3").readText())
        assertEquals("new-index", fixture.media3Database.readText())
        assertTrue(!fixture.stage.exists())
    }

    private fun fixture(databaseParentIsFile: Boolean = false): Fixture {
        val filesDir = temporaryFolder.newFolder("files-${System.nanoTime()}")
        val externalMusicDir = temporaryFolder.newFolder("music-${System.nanoTime()}")
        val databaseParent = if (databaseParentIsFile) {
            temporaryFolder.newFile("database-parent-${System.nanoTime()}")
        } else {
            temporaryFolder.newFolder("databases-${System.nanoTime()}")
        }
        val database = File(databaseParent, OfflineDownloadArchive.MEDIA3_DATABASE_NAME)
        val context = mock(Context::class.java)
        `when`(context.filesDir).thenReturn(filesDir)
        `when`(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)).thenReturn(externalMusicDir)
        `when`(context.getDatabasePath(OfflineDownloadArchive.MEDIA3_DATABASE_NAME)).thenReturn(database)
        val stage = OfflineDownloadArchive.newStagingDirectory(context)
        return Fixture(
            context = context,
            stage = stage,
            downloadTarget = File(externalMusicDir, "downloads"),
            media3Database = database,
        )
    }

    private fun writeIncomingStage(stage: File, audio: String, index: String) {
        OfflineDownloadArchive.resolveStagingTarget(stage, "files/new.mp3").writeText(audio)
        OfflineDownloadArchive.resolveStagingTarget(
            stage,
            "databases/${OfflineDownloadArchive.MEDIA3_DATABASE_NAME}",
        ).writeText(index)
    }

    private data class Fixture(
        val context: Context,
        val stage: File,
        val downloadTarget: File,
        val media3Database: File,
    )
}
