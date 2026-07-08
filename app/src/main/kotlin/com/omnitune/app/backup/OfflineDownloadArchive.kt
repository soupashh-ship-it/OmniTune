/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.backup

import android.content.Context
import android.os.Environment
import java.io.File

object OfflineDownloadArchive {
    const val LIBRARY_JSON_ENTRY = "library.json"
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
        return listOf(
            db,
            File(db.parentFile, "$MEDIA3_DATABASE_NAME-wal"),
            File(db.parentFile, "$MEDIA3_DATABASE_NAME-shm"),
        ).filter { it.exists() && it.isFile }
    }

    fun newStagingDirectory(context: Context): File {
        val root = File(context.filesDir, PENDING_ROOT_NAME)
        root.mkdirs()
        return File(root, "restore_${System.currentTimeMillis()}").also { dir ->
            if (dir.exists()) dir.deleteRecursively()
            dir.mkdirs()
        }
    }

    fun markReady(stageDir: File) {
        File(stageDir, READY_MARKER).writeText("ready")
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

    fun applyPending(context: Context): Boolean {
        val root = File(context.filesDir, PENDING_ROOT_NAME)
        if (!root.exists()) return false

        var applied = false
        root.listFiles()
            ?.filter { File(it, READY_MARKER).exists() }
            ?.sortedBy { it.lastModified() }
            ?.forEach { stageDir ->
                val filesDir = File(stageDir, "files")
                if (filesDir.exists()) {
                    copyDirectoryContents(filesDir, downloadDirectory(context))
                    applied = true
                }

                val dbDir = File(stageDir, "databases")
                if (dbDir.exists()) {
                    val appDbDir = context.getDatabasePath(MEDIA3_DATABASE_NAME).parentFile
                    appDbDir?.mkdirs()
                    dbDir.listFiles()?.filter { it.isFile }?.forEach { file ->
                        file.copyTo(File(appDbDir, file.name), overwrite = true)
                        applied = true
                    }
                }

                stageDir.deleteRecursively()
            }
        return applied
    }

    private fun copyDirectoryContents(source: File, target: File) {
        source.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val relativePath = source.toPath().relativize(file.toPath()).toString()
                val out = File(target, relativePath)
                out.parentFile?.mkdirs()
                file.copyTo(out, overwrite = true)
            }
    }
}
