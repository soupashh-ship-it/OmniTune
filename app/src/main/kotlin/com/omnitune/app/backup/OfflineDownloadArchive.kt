/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.backup

import android.content.Context
import android.os.Environment
import java.io.File
import java.security.MessageDigest

/**
 * File operations for restore are staged first and only promoted after the
 * incoming archive has passed its manifest check. Promotion keeps a reversible
 * same-filesystem copy of every replaced target until database verification has
 * also completed.
 */
object OfflineDownloadArchive {
    const val LIBRARY_JSON_ENTRY = "library.json"
    const val MANIFEST_JSON_ENTRY = "manifest.json"
    const val DOWNLOAD_FILES_PREFIX = "downloads/files/"
    const val DOWNLOAD_DATABASE_PREFIX = "downloads/databases/"
    const val MEDIA3_DATABASE_NAME = "exoplayer_internal.db"

    private const val PENDING_ROOT_NAME = "pending_download_restore"
    private const val READY_MARKER = "READY"

    fun downloadDirectory(context: Context): File {
        val externalMusicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        return if (externalMusicDir != null) {
            File(externalMusicDir, "downloads")
        } else {
            File(context.filesDir, "downloads")
        }
    }

    fun media3DatabaseFiles(context: Context): List<File> {
        val db = context.getDatabasePath(MEDIA3_DATABASE_NAME)
        return databaseFiles(db.parentFile)
    }

    fun newStagingDirectory(context: Context): File {
        val root = File(context.filesDir, PENDING_ROOT_NAME)
        if (!root.exists() && !root.mkdirs()) throw IllegalStateException("Could not create restore staging storage")
        return File(root, "restore_${System.currentTimeMillis()}_${System.nanoTime()}").also { dir ->
            if (dir.exists() && !dir.deleteRecursively()) throw IllegalStateException("Could not reset restore staging storage")
            if (!dir.mkdirs()) throw IllegalStateException("Could not create restore staging directory")
        }
    }

    /**
     * A committed Room restore writes this marker before media promotion. If the
     * process dies after the database commit, application startup can complete
     * the already-validated staged media restore instead of abandoning it.
     */
    fun markReady(
        stageDir: File,
        replaceExisting: Boolean,
    ) {
        File(stageDir, READY_MARKER).writeText(if (replaceExisting) "replace" else "merge")
    }

    fun clearReadyMarker(stageDir: File) {
        File(stageDir, READY_MARKER).delete()
    }

    fun hasDownloadPayload(stageDir: File): Boolean =
        File(stageDir, "files").walkTopDown().any { it.isFile } ||
            File(stageDir, "databases").walkTopDown().any { it.isFile }

    fun resolveStagingTarget(stageDir: File, relativePath: String): File {
        require(relativePath.isNotBlank()) { "Empty archive entry path" }
        require(!relativePath.contains('\\')) { "Invalid archive entry path" }
        require(relativePath.split('/').none { it == ".." || it.isBlank() }) {
            "Unsafe archive entry path"
        }

        val target = File(stageDir, relativePath)
        val stagePath = stageDir.canonicalFile.toPath()
        val targetPath = target.canonicalFile.toPath()
        require(targetPath.startsWith(stagePath)) { "Archive entry escaped restore directory" }
        target.parentFile?.mkdirs()
        return target
    }

    /**
     * Promotes staged files. For Replace it swaps the entire downloads directory
     * and all Media3 index files. For Merge it overlays only declared files.
     * The returned handle must be committed only after the Room transaction has
     * verified; otherwise call rollback.
     */
    internal fun applyStaged(
        context: Context,
        stageDir: File,
        replaceExisting: Boolean,
    ): AppliedOfflineRestore {
        require(stageDir.isDirectory) { "Restore staging directory is unavailable" }
        val rollback = RollbackRegistry()
        try {
            applyDownloads(stageDir, downloadDirectory(context), replaceExisting, rollback)
            applyDatabases(stageDir, context.getDatabasePath(MEDIA3_DATABASE_NAME).parentFile, replaceExisting, rollback)
            return AppliedOfflineRestore(rollback)
        } catch (error: Exception) {
            rollback.rollback()
            throw error
        }
    }

    /**
     * Old READY markers can only originate from earlier app builds. They are
     * made reversible at the file level and left in place when a retry fails.
     */
    fun applyPending(context: Context): Boolean {
        val root = File(context.filesDir, PENDING_ROOT_NAME)
        if (!root.exists()) return false
        var applied = false
        root.listFiles()
            ?.filter { File(it, READY_MARKER).exists() }
            ?.sortedBy { it.lastModified() }
            ?.forEach { stageDir ->
                try {
                    val replaceExisting = File(stageDir, READY_MARKER).readText().trim() == "replace"
                    applyStaged(context, stageDir, replaceExisting = replaceExisting).commit()
                    if (!stageDir.deleteRecursively()) throw IllegalStateException("Could not clear applied legacy restore stage")
                    applied = true
                } catch (_: Exception) {
                    // Preserve the stage for a later safe retry instead of deleting it.
                }
            }
        return applied
    }

    private fun applyDownloads(
        stageDir: File,
        destination: File,
        replaceExisting: Boolean,
        rollback: RollbackRegistry,
    ) {
        val source = File(stageDir, "files")
        if (replaceExisting) {
            val parent = destination.parentFile ?: throw IllegalStateException("Offline audio storage is unavailable")
            if (!parent.exists() && !parent.mkdirs()) throw IllegalStateException("Could not create offline audio storage")
            val incoming = File(parent, ".${destination.name}.restore-incoming-${stageDir.name}")
            val previous = File(parent, ".${destination.name}.restore-previous-${stageDir.name}")
            ensureAbsent(incoming)
            ensureAbsent(previous)
            if (!incoming.mkdirs()) throw IllegalStateException("Could not prepare offline audio restore")
            if (source.exists()) copyDirectoryContentsChecked(source, incoming)

            if (destination.exists() && !destination.renameTo(previous)) {
                throw IllegalStateException("Could not preserve current offline audio")
            }
            if (!incoming.renameTo(destination)) {
                if (previous.exists()) previous.renameTo(destination)
                throw IllegalStateException("Could not apply restored offline audio")
            }
            rollback.add(
                rollback = {
                    destination.deleteRecursively()
                    if (previous.exists() && !previous.renameTo(destination)) {
                        throw IllegalStateException("Could not roll back offline audio")
                    }
                },
                commit = { previous.deleteRecursively() },
            )
            return
        }

        if (!source.exists()) return
        source.walkTopDown().filter { it.isFile }.forEach { file ->
            val relative = source.toPath().relativize(file.toPath()).toString()
            val target = File(destination, relative)
            replaceFileReversibly(file, target, stageDir, rollback)
        }
    }

    private fun applyDatabases(
        stageDir: File,
        databaseDirectory: File?,
        replaceExisting: Boolean,
        rollback: RollbackRegistry,
    ) {
        val destinationRoot = databaseDirectory ?: throw IllegalStateException("Media3 database storage is unavailable")
        if (!destinationRoot.exists() && !destinationRoot.mkdirs()) {
            throw IllegalStateException("Could not create Media3 database storage")
        }
        val sourceRoot = File(stageDir, "databases")
        val names = databaseFileNames()
        names.filter { replaceExisting || File(sourceRoot, it).isFile }.forEach { name ->
            val source = File(sourceRoot, name).takeIf { it.isFile }
            val target = File(destinationRoot, name)
            replaceFileReversibly(source, target, stageDir, rollback)
        }
    }

    private fun replaceFileReversibly(
        source: File?,
        target: File,
        stageDir: File,
        rollback: RollbackRegistry,
    ) {
        target.parentFile?.let { parent -> if (!parent.exists() && !parent.mkdirs()) throw IllegalStateException("Could not create restore destination") }
        val backup = File(target.parentFile, ".${target.name}.restore-previous-${stageDir.name}")
        ensureAbsent(backup)
        val existed = target.exists()
        if (existed && !target.renameTo(backup)) {
            throw IllegalStateException("Could not preserve ${target.name} before restore")
        }
        try {
            if (source != null) copyFileChecked(source, target)
        } catch (error: Exception) {
            target.delete()
            if (existed) backup.renameTo(target)
            throw error
        }
        rollback.add(
            rollback = {
                target.delete()
                if (existed && !backup.renameTo(target)) {
                    throw IllegalStateException("Could not roll back ${target.name}")
                }
            },
            commit = { backup.delete() },
        )
    }

    private fun copyDirectoryContentsChecked(source: File, target: File) {
        source.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val relativePath = source.toPath().relativize(file.toPath()).toString()
                copyFileChecked(file, File(target, relativePath))
            }
    }

    private fun copyFileChecked(source: File, target: File) {
        target.parentFile?.let { parent -> if (!parent.exists() && !parent.mkdirs()) throw IllegalStateException("Could not create restore file directory") }
        source.inputStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
        if (source.length() != target.length() || sha256(source) != sha256(target)) {
            target.delete()
            throw IllegalStateException("Restored file verification failed for ${source.name}")
        }
    }

    private fun ensureAbsent(file: File) {
        if (file.exists() && !file.deleteRecursively()) throw IllegalStateException("Could not clear previous restore transaction data")
    }

    private fun databaseFiles(parent: File?): List<File> = parent?.let { directory ->
        databaseFileNames().map { File(directory, it) }.filter { it.exists() && it.isFile }
    } ?: emptyList()

    private fun databaseFileNames() = listOf(
        MEDIA3_DATABASE_NAME,
        "$MEDIA3_DATABASE_NAME-wal",
        "$MEDIA3_DATABASE_NAME-shm",
    )

    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256")
        .let { digest ->
            file.inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
}

internal class AppliedOfflineRestore(
    private val rollbackRegistry: RollbackRegistry,
) {
    fun rollback() = rollbackRegistry.rollback()

    fun commit() = rollbackRegistry.commit()
}

internal class RollbackRegistry {
    private val operations = mutableListOf<Pair<() -> Unit, () -> Unit>>()

    fun add(rollback: () -> Unit, commit: () -> Unit) {
        operations += rollback to commit
    }

    fun rollback() {
        var failure: Exception? = null
        operations.asReversed().forEach { (undo, _) ->
            try {
                undo()
            } catch (error: Exception) {
                failure = failure ?: error
            }
        }
        operations.clear()
        failure?.let { throw it }
    }

    fun commit() {
        var failure: Exception? = null
        operations.forEach { (_, finish) ->
            try {
                finish()
            } catch (error: Exception) {
                failure = failure ?: error
            }
        }
        operations.clear()
        failure?.let { throw it }
    }
}
