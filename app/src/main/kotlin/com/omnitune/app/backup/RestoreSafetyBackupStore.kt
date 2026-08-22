/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.backup

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class RestoreSafetyBackup(
    val file: File,
    val createdAtEpochMillis: Long,
    val formatVersion: Int,
    val counts: OmniBackupCounts,
) {
    val locationDescription: String
        get() = "OmniTune app storage / ${file.parentFile?.name}/${file.name}"
}

/**
 * Retained, application-owned recovery archives created before Replace. No
 * preference, account, cookie, API-key, or token data belongs in a snapshot.
 */
internal class RestoreSafetyBackupStore(
    private val rootDirectory: File,
) {
    fun create(
        snapshot: OmniBackupSnapshot,
        writeArchive: (OutputStream) -> Unit,
    ): RestoreSafetyBackup {
        if (!rootDirectory.exists() && !rootDirectory.mkdirs()) {
            throw IllegalStateException("Could not create the safety backup directory")
        }
        if (!rootDirectory.isDirectory || !rootDirectory.canWrite()) {
            throw IllegalStateException("Safety backup storage is unavailable")
        }

        val stamp = TIMESTAMP.format(Instant.ofEpochMilli(snapshot.createdAtEpochMillis))
        val name = "omnitune-replace-safety-$stamp-v${snapshot.formatVersion}.zip"
        val destination = File(rootDirectory, name)
        val temporary = File(rootDirectory, ".$name.pending")
        temporary.delete()
        try {
            FileOutputStream(temporary).use(writeArchive)
            if (temporary.length() <= 0L) throw IllegalStateException("Safety backup is empty")
            moveAtomically(temporary, destination)
        } catch (error: Exception) {
            temporary.delete()
            throw error
        }

        return RestoreSafetyBackup(
            file = destination,
            createdAtEpochMillis = snapshot.createdAtEpochMillis,
            formatVersion = snapshot.formatVersion,
            counts = snapshot.toPreviewCounts(),
        )
    }

    fun latest(): RestoreSafetyBackup? = rootDirectory.listFiles()
        ?.filter { it.isFile && it.name.startsWith("omnitune-replace-safety-") && it.name.endsWith(".zip") }
        ?.maxByOrNull { it.lastModified() }
        ?.let { file ->
            RestoreSafetyBackup(
                file = file,
                createdAtEpochMillis = file.lastModified(),
                formatVersion = OMNI_BACKUP_FORMAT_VERSION,
                counts = OmniBackupCounts(),
            )
        }

    private fun moveAtomically(source: File, destination: File) {
        try {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: Exception) {
            if (destination.exists() && !destination.delete()) {
                throw IllegalStateException("Could not replace existing safety backup")
            }
            if (!source.renameTo(destination)) {
                throw IllegalStateException("Could not finalize safety backup")
            }
        }
    }

    private companion object {
        val TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(ZoneOffset.UTC)
    }
}

internal fun safetyBackupStore(context: Context): RestoreSafetyBackupStore =
    RestoreSafetyBackupStore(File(context.filesDir, "restore_safety_backups"))
