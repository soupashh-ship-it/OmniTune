/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RestoreSafetyBackupStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun safetyBackupIsWrittenAtomicallyAndRetained() {
        val root = temporaryFolder.newFolder("safety")
        val store = RestoreSafetyBackupStore(root)
        val backup = store.create(snapshot()) { output -> output.write("safe".encodeToByteArray()) }

        assertTrue(backup.file.isFile)
        assertTrue(backup.file.name.contains("-v2.zip"))
        assertEquals(backup.file, store.latest()?.file)
        assertFalse(root.listFiles().orEmpty().any { it.name.endsWith(".pending") })
    }

    @Test
    fun safetyBackupFailureLeavesNoPartialArchive() {
        val root = temporaryFolder.newFolder("safety")
        val store = RestoreSafetyBackupStore(root)

        assertThrows(IllegalStateException::class.java) {
            store.create(snapshot()) { throw IllegalStateException("forced safety failure") }
        }

        assertTrue(root.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun rollbackRegistryRestoresOperationsInReverseOrder() {
        val events = mutableListOf<String>()
        val registry = RollbackRegistry()
        registry.add(rollback = { events += "undo-first" }, commit = { events += "commit-first" })
        registry.add(rollback = { events += "undo-second" }, commit = { events += "commit-second" })

        registry.rollback()

        assertEquals(listOf("undo-second", "undo-first"), events)
    }

    private fun snapshot() = OmniBackupSnapshot(
        createdAtEpochMillis = 1_725_000_000_000L,
        songs = listOf(BackupSong(id = "song", title = "Song")),
    )
}
