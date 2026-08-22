/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.backup

import android.content.Context
import com.omnitune.app.db.MusicDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.Mockito.mock
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class OmniBackupArchiveReadTest {
    @Test
    fun corruptZipIsReportedAsArchiveReadFailure() {
        val error = assertThrows(RestoreFailureException::class.java) {
            runBlocking {
                repository().previewBackup(
                    ByteArrayInputStream(byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 3, 4)),
                    OmniRestoreMode.REPLACE,
                )
            }
        }

        assertEquals(RestoreFailurePhase.ARCHIVE_READ, error.phase)
    }

    @Test
    fun traversalDirectoryInZipIsRejectedBeforePreview() {
        val archive = ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("downloads/files/../escape/"))
                zip.closeEntry()
            }
            output.toByteArray()
        }

        val error = assertThrows(RestoreFailureException::class.java) {
            runBlocking {
                repository().previewBackup(ByteArrayInputStream(archive), OmniRestoreMode.REPLACE)
            }
        }

        assertEquals(RestoreFailurePhase.ARCHIVE_READ, error.phase)
    }

    private fun repository() = OmniBackupRepository(
        database = mock(MusicDatabase::class.java),
        context = mock(Context::class.java),
    )
}
