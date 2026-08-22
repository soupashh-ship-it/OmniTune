/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.backup

import android.content.Context
import android.annotation.SuppressLint
import com.omnitune.app.BuildConfig
import com.omnitune.app.db.CURRENT_ROOM_DATABASE_SCHEMA_VERSION
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.AlbumArtistMap
import com.omnitune.app.db.entities.AlbumEntity
import com.omnitune.app.db.entities.ArtistEntity
import com.omnitune.app.db.entities.Event
import com.omnitune.app.db.entities.PlayCountEntity
import com.omnitune.app.db.entities.PlaylistEntity
import com.omnitune.app.db.entities.PlaylistSongMap
import com.omnitune.app.db.entities.PlaylistTagMap
import com.omnitune.app.db.entities.SongAlbumMap
import com.omnitune.app.db.entities.SongArtistMap
import com.omnitune.app.db.entities.SongEntity
import com.omnitune.app.db.entities.TagEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.FilterOutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class OmniBackupRepository @Inject constructor(
    private val database: MusicDatabase,
    @ApplicationContext private val context: Context,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun exportBackup(
        outputStream: OutputStream,
        includeDownloadedAudio: Boolean = false,
    ): OmniBackupExportResult = withContext(Dispatchers.IO) {
        val snapshot = database.withTransaction { createSnapshot() }
        outputStream.use { stream -> writeSnapshot(stream, snapshot, includeDownloadedAudio) }
    }

    private suspend fun createSnapshot(): OmniBackupSnapshot {
        val createdAt = System.currentTimeMillis()
        val songs = database.backupSongs()
        val artists = database.backupArtists()
        val albums = database.backupAlbums()
        val playlists = database.backupPlaylists()
        val playlistSongs = database.backupPlaylistSongMaps()
        val songArtists = database.backupSongArtistMaps()
        val songAlbums = database.backupSongAlbumMaps()
        val albumArtists = database.backupAlbumArtistMaps()
        val events = database.backupEvents()
        val playCounts = database.backupPlayCounts()
        val tags = database.backupTags()
        val playlistTags = database.backupPlaylistTagMaps()

        return OmniBackupSnapshot(
            createdAtEpochMillis = createdAt,
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE.toLong(),
            roomSchemaVersion = CURRENT_ROOM_DATABASE_SCHEMA_VERSION,
            library = BackupLibrarySection(
                exportedSongCount = songs.size,
                exportedLikedSongCount = songs.count { it.liked },
                exportedPlaylistCount = playlists.size,
                exportedPlaylistEntryCount = playlistSongs.size,
                exportedArtistCount = artists.size,
                exportedAlbumCount = albums.size,
                exportedHistoryItemCount = events.size,
                exportedStatsRecordCount = playCounts.size,
                exportedTagCount = tags.size,
                exportedPlaylistTagCount = playlistTags.size,
            ),
            songs = songs.map { it.toBackupSong() },
            artists = artists.map { it.toBackupArtist() },
            albums = albums.map { it.toBackupAlbum() },
            playlists = playlists.map { it.toBackupPlaylist() },
            playlistSongs = playlistSongs.map { it.toBackupPlaylistSong() },
            songArtists = songArtists.map { it.toBackupSongArtist() },
            songAlbums = songAlbums.map { it.toBackupSongAlbum() },
            albumArtists = albumArtists.map { it.toBackupAlbumArtist() },
            tags = tags.map { it.toBackupTag() },
            playlistTags = playlistTags.map { it.toBackupPlaylistTag() },
            history = events.map { it.toBackupHistoryItem() },
            stats = playCounts.map { it.toBackupStatsItem() },
            settings = BackupSettingsSection(),
        )
    }

    private fun writeSnapshot(
        outputStream: OutputStream,
        snapshot: OmniBackupSnapshot,
        includeDownloadedAudio: Boolean,
    ): OmniBackupExportResult {
        val libraryBytes = json.encodeToString(snapshot).toByteArray(StandardCharsets.UTF_8)
        if (!includeDownloadedAudio) {
            outputStream.write(libraryBytes)
            outputStream.flush()
            return OmniBackupExportResult(
                counts = snapshot.toPreviewCounts(),
                byteCount = libraryBytes.size.toLong(),
                createdAtEpochMillis = snapshot.createdAtEpochMillis,
            )
        }

        val downloadRoot = OfflineDownloadArchive.downloadDirectory(context)
        val downloadFiles = if (downloadRoot.exists()) {
            downloadRoot.walkTopDown().filter { it.isFile }.sortedBy { it.absolutePath }.toList()
        } else {
            emptyList()
        }
        val media3Files = OfflineDownloadArchive.media3DatabaseFiles(context).sortedBy { it.name }
        require(downloadFiles.isEmpty() || media3Files.any { it.name == OfflineDownloadArchive.MEDIA3_DATABASE_NAME }) {
            "Offline audio cannot be archived without its Media3 download index"
        }

        val counting = CountingOutputStream(outputStream)
        ZipOutputStream(BufferedOutputStream(counting)).use { zip ->
            writeZipEntry(zip, OfflineDownloadArchive.LIBRARY_JSON_ENTRY, libraryBytes)
            val files = mutableListOf<BackupArchiveFile>()
            downloadFiles.forEach { file ->
                val relative = downloadRoot.toPath().relativize(file.toPath()).toString()
                    .replace(File.separatorChar, '/')
                files += writeArchiveFile(zip, OfflineDownloadArchive.DOWNLOAD_FILES_PREFIX + relative, file)
            }
            media3Files.forEach { file ->
                files += writeArchiveFile(zip, OfflineDownloadArchive.DOWNLOAD_DATABASE_PREFIX + file.name, file)
            }
            val manifest = BackupArchiveManifest(
                librarySha256 = sha256(libraryBytes),
                files = files.sortedBy { it.entryName },
            )
            writeZipEntry(
                zip,
                OfflineDownloadArchive.MANIFEST_JSON_ENTRY,
                json.encodeToString(manifest).toByteArray(StandardCharsets.UTF_8),
            )
        }
        return OmniBackupExportResult(
            counts = snapshot.toPreviewCounts().copy(
                downloadedAudioFiles = downloadFiles.size,
                downloadedAudioBytes = downloadFiles.sumOf { it.length() },
            ),
            byteCount = counting.bytesWritten,
            createdAtEpochMillis = snapshot.createdAtEpochMillis,
        )
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content)
        zip.closeEntry()
    }

    private fun writeArchiveFile(zip: ZipOutputStream, entryName: String, file: File): BackupArchiveFile {
        zip.putNextEntry(ZipEntry(entryName))
        val digest = MessageDigest.getInstance("SHA-256")
        var byteCount = 0L
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                byteCount += read
                zip.write(buffer, 0, read)
                digest.update(buffer, 0, read)
            }
        }
        zip.closeEntry()
        return BackupArchiveFile(entryName, byteCount, digest.digest().toHex())
    }

    suspend fun previewBackup(
        inputStream: InputStream,
        mode: OmniRestoreMode,
        selection: OmniRestoreSelection = OmniRestoreSelection.ALL,
    ): OmniBackupPreview = withContext(Dispatchers.IO) {
        selection.requireSupportedFor(mode)
        val packageResult = try {
            readBackupPackage(inputStream, stageDownloads = false)
        } catch (error: Exception) {
            throw RestoreFailureException(RestoreFailurePhase.ARCHIVE_READ, error.message ?: "Could not read backup archive", error)
        }
        val selectedSnapshot = packageResult.snapshot.selectForRestore(selection)
        try {
            OmniBackupPreflight.validate(selectedSnapshot, mode, packageResult.archiveInfo)
                .copy(selection = selection)
        } catch (error: BackupPreflightException) {
            throw RestoreFailureException(RestoreFailurePhase.PREFLIGHT, error.message ?: "Backup validation failed", error)
        }
    }

    fun latestSafetyBackup(): RestoreSafetyBackup? = safetyBackupStore(context).latest()

    suspend fun recoverLatestSafetyBackup(): OmniBackupImportResult = withContext(Dispatchers.IO) {
        val backup = latestSafetyBackup()
            ?: throw RestoreFailureException(RestoreFailurePhase.SAFETY_BACKUP, "No retained Replace safety backup is available")
        FileInputStream(backup.file).use { input -> importBackup(input, OmniRestoreMode.REPLACE) }
    }

    suspend fun importBackup(
        inputStream: InputStream,
        mode: OmniRestoreMode = OmniRestoreMode.MERGE,
        selection: OmniRestoreSelection = OmniRestoreSelection.ALL,
    ): OmniBackupImportResult = withContext(Dispatchers.IO) {
        selection.requireSupportedFor(mode)
        val packageResult = try {
            readBackupPackage(inputStream, stageDownloads = true)
        } catch (error: Exception) {
            throw RestoreFailureException(RestoreFailurePhase.ARCHIVE_READ, error.message ?: "Could not read backup archive", error)
        }
        var mediaRestore: AppliedOfflineRestore? = null
        var readyMediaStage: File? = null
        var safetySnapshot: OmniBackupSnapshot? = null
        var databaseCommitted = false
        try {
            val selectedSnapshot = packageResult.snapshot.selectForRestore(selection)
            try {
                OmniBackupPreflight.validate(selectedSnapshot, mode, packageResult.archiveInfo)
            } catch (error: BackupPreflightException) {
                throw RestoreFailureException(RestoreFailurePhase.PREFLIGHT, error.message ?: "Backup validation failed", error)
            }

            val safetyBackup = if (mode == OmniRestoreMode.REPLACE) {
                safetySnapshot = database.withTransaction { createSnapshot() }
                try {
                    safetyBackupStore(context).create(requireNotNull(safetySnapshot)) { output ->
                        writeSnapshot(output, requireNotNull(safetySnapshot), includeDownloadedAudio = true)
                    }.also { backup ->
                        verifySafetyBackup(backup)
                    }
                } catch (error: Exception) {
                    throw RestoreFailureException(
                        RestoreFailurePhase.SAFETY_BACKUP,
                        error.message ?: "Could not create a verified safety backup",
                        error,
                    )
                }
            } else {
                null
            }

            val hasDownloadPayload = packageResult.archiveInfo.downloadedAudioFiles > 0 ||
                packageResult.archiveInfo.media3DatabaseFiles > 0
            // A Media3 index is a complete database, not a safely mergeable
            // collection. Applying it during Merge can orphan the current
            // profile's audio files, so only Replace restores offline media.
            val restoreOfflineMedia = mode == OmniRestoreMode.REPLACE &&
                selection.downloads && packageResult.archiveInfo.isFullArchive
            val counts = RestoreTransactionBoundary.run {
                database.withTransaction {
                    if (mode == OmniRestoreMode.REPLACE) clearLibraryForReplace()
                    val restored = restoreMerge(
                        snapshot = selectedSnapshot,
                        restoreDownloadedAudioState = restoreOfflineMedia && hasDownloadPayload,
                    )
                    if (mode == OmniRestoreMode.REPLACE) verifyReplaceRestore(selectedSnapshot)
                    restored
                }
            }
            databaseCommitted = true

            if (restoreOfflineMedia) {
                val stage = packageResult.stagedDownloadDir ?: OfflineDownloadArchive.newStagingDirectory(context)
                try {
                    // Persist the post-transaction handoff before touching
                    // media. A process restart will finish only this verified
                    // stage through OfflineDownloadArchive.applyPending.
                    OfflineDownloadArchive.markReady(stage, replaceExisting = true)
                    readyMediaStage = stage
                    val appliedRestore = OfflineDownloadArchive.applyStaged(
                        context = context,
                        stageDir = stage,
                        replaceExisting = mode == OmniRestoreMode.REPLACE,
                    )
                    mediaRestore = appliedRestore
                    appliedRestore.commit()
                    mediaRestore = null
                    readyMediaStage = null
                } catch (error: Exception) {
                    throw RestoreFailureException(
                        RestoreFailurePhase.MEDIA_RESTORE,
                        error.message ?: "Could not restore offline media",
                        error,
                    )
                } finally {
                    if (stage != packageResult.stagedDownloadDir) stage.deleteRecursively()
                }
            }
            packageResult.stagedDownloadDir?.deleteRecursively()

            OmniBackupImportResult(
                counts = counts + if (restoreOfflineMedia) packageResult.downloadCounts else OmniBackupCounts(),
                formatVersion = selectedSnapshot.formatVersion,
                createdAtEpochMillis = selectedSnapshot.createdAtEpochMillis,
                offlineAudioRestorePending = false,
                safetyBackup = safetyBackup,
            )
        } catch (e: Exception) {
            readyMediaStage?.let(OfflineDownloadArchive::clearReadyMarker)
            try {
                mediaRestore?.rollback()
            } catch (rollbackError: Exception) {
                throw RestoreFailureException(
                    RestoreFailurePhase.ROLLBACK,
                    "Media rollback failed; the retained safety backup is available for recovery. ${rollbackError.message}",
                    rollbackError,
                )
            }
            if (databaseCommitted && mode == OmniRestoreMode.REPLACE && safetySnapshot != null) {
                try {
                    database.withTransaction {
                        clearLibraryForReplace()
                        restoreMerge(requireNotNull(safetySnapshot), restoreDownloadedAudioState = true)
                        verifyReplaceRestore(requireNotNull(safetySnapshot))
                    }
                } catch (rollbackError: Exception) {
                    throw RestoreFailureException(
                        RestoreFailurePhase.ROLLBACK,
                        "Database recovery failed; the retained safety backup is available for recovery. ${rollbackError.message}",
                        rollbackError,
                    )
                }
            }
            packageResult.stagedDownloadDir?.deleteRecursively()
            throw e
        }
    }

    private fun verifySafetyBackup(backup: RestoreSafetyBackup) {
        val read = FileInputStream(backup.file).use { input -> readBackupPackage(input, stageDownloads = false) }
        OmniBackupPreflight.validate(
            snapshot = read.snapshot,
            mode = OmniRestoreMode.REPLACE,
            archive = read.archiveInfo,
            allowEmpty = true,
        )
        require(read.snapshot.toPreviewCounts() == backup.counts) { "Safety backup verification counts do not match" }
    }

    private fun readBackupPackage(
        inputStream: InputStream,
        stageDownloads: Boolean,
    ): BackupPackageReadResult {
        val buffered = BufferedInputStream(inputStream)
        buffered.mark(4)
        val header = ByteArray(4)
        val read = buffered.read(header)
        buffered.reset()
        return if (read >= 2 && header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte()) {
            readZipBackup(buffered, stageDownloads)
        } else {
            BackupPackageReadResult(snapshot = readSnapshot(buffered))
        }
    }

    private fun readSnapshot(inputStream: InputStream): OmniBackupSnapshot {
        val text = inputStream.use { it.readBytesLimited(MAX_LIBRARY_JSON_BYTES).toString(StandardCharsets.UTF_8) }
        if (text.isBlank()) {
            throw IllegalArgumentException("Backup file is empty")
        }
        return try {
            json.decodeFromString(text)
        } catch (e: SerializationException) {
            throw IllegalArgumentException("Backup file is not a valid OmniTune JSON backup", e)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Backup file is not a valid OmniTune JSON backup", e)
        }
    }

    @SuppressLint("UsableSpace")
    private fun readZipBackup(
        inputStream: InputStream,
        stageDownloads: Boolean,
    ): BackupPackageReadResult {
        var snapshot: OmniBackupSnapshot? = null
        var libraryBytes: ByteArray? = null
        var manifest: BackupArchiveManifest? = null
        var stagedDir: File? = null
        var audioFileCount = 0
        var audioBytes = 0L
        var media3DatabaseFiles = 0
        var entryCount = 0
        val actualFiles = mutableListOf<BackupArchiveFile>()
        val seenEntries = mutableSetOf<String>()

        try {
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    entryCount++
                    require(entryCount <= MAX_ARCHIVE_ENTRIES) { "Backup archive contains too many entries" }
                    require(seenEntries.add(entry.name)) { "Backup archive contains duplicate entry ${entry.name}" }
                    if (entry.isDirectory) {
                        validateArchiveDirectory(entry.name)
                    } else {
                        when {
                            entry.name == OfflineDownloadArchive.LIBRARY_JSON_ENTRY -> {
                                require(snapshot == null) { "Backup archive contains multiple library.json entries" }
                                libraryBytes = zip.readBytesLimited(MAX_LIBRARY_JSON_BYTES)
                                snapshot = decodeSnapshot(requireNotNull(libraryBytes))
                            }
                            entry.name == OfflineDownloadArchive.MANIFEST_JSON_ENTRY -> {
                                require(manifest == null) { "Backup archive contains multiple manifest.json entries" }
                                manifest = decodeManifest(zip.readBytesLimited(MAX_MANIFEST_JSON_BYTES))
                            }
                            entry.name.startsWith(OfflineDownloadArchive.DOWNLOAD_FILES_PREFIX) -> {
                                val relative = safeArchiveRelativePath(entry.name, OfflineDownloadArchive.DOWNLOAD_FILES_PREFIX)
                                val stage = if (stageDownloads) stagedDir ?: OfflineDownloadArchive.newStagingDirectory(context).also { stagedDir = it } else null
                                val output = stage?.let { current ->
                                    val remaining = (current.usableSpace - MIN_FREE_SPACE_BYTES).coerceAtLeast(0L)
                                    require(remaining > 0) { "Not enough storage to stage downloaded audio" }
                                    OfflineDownloadArchive.resolveStagingTarget(current, "files/$relative").outputStream()
                                }
                                val copied = if (output != null) {
                                    output.use { zip.copyToLimitedAndDigest(it, MAX_AUDIO_ENTRY_BYTES) }
                                } else {
                                    zip.copyToLimitedAndDigest(null, MAX_AUDIO_ENTRY_BYTES)
                                }
                                actualFiles += BackupArchiveFile(entry.name, copied.byteCount, copied.sha256)
                                audioFileCount++
                                audioBytes += copied.byteCount
                            }
                            entry.name.startsWith(OfflineDownloadArchive.DOWNLOAD_DATABASE_PREFIX) -> {
                                val relative = safeArchiveRelativePath(entry.name, OfflineDownloadArchive.DOWNLOAD_DATABASE_PREFIX)
                                require(relative in media3DatabaseNames()) { "Unsupported Media3 database entry" }
                                val stage = if (stageDownloads) stagedDir ?: OfflineDownloadArchive.newStagingDirectory(context).also { stagedDir = it } else null
                                val output = stage?.let { current ->
                                    OfflineDownloadArchive.resolveStagingTarget(current, "databases/$relative").outputStream()
                                }
                                val copied = if (output != null) {
                                    output.use { zip.copyToLimitedAndDigest(it, MAX_DATABASE_ENTRY_BYTES) }
                                } else {
                                    zip.copyToLimitedAndDigest(null, MAX_DATABASE_ENTRY_BYTES)
                                }
                                actualFiles += BackupArchiveFile(entry.name, copied.byteCount, copied.sha256)
                                media3DatabaseFiles++
                            }
                            else -> throw IllegalArgumentException("Backup archive contains unsupported entry ${entry.name}")
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (error: Exception) {
            stagedDir?.deleteRecursively()
            throw error
        }

        val finalSnapshot = snapshot ?: throw IllegalArgumentException("Backup archive is missing library.json")
        return BackupPackageReadResult(
            snapshot = finalSnapshot,
            downloadCounts = OmniBackupCounts(
                downloadedAudioFiles = audioFileCount,
                downloadedAudioBytes = audioBytes,
            ),
            stagedDownloadDir = stagedDir,
            archiveInfo = BackupArchiveInfo(
                isFullArchive = true,
                manifest = manifest,
                actualLibrarySha256 = libraryBytes?.let(::sha256),
                actualFiles = actualFiles.sortedBy { it.entryName },
                downloadedAudioFiles = audioFileCount,
                downloadedAudioBytes = audioBytes,
                media3DatabaseFiles = media3DatabaseFiles,
            ),
        )
    }

    private fun safeArchiveRelativePath(entryName: String, prefix: String): String {
        require(entryName.startsWith(prefix)) { "Archive entry is outside the expected backup section" }

        val relative = entryName.removePrefix(prefix)
        require(relative.isNotBlank()) { "Empty archive entry path" }
        require(!relative.startsWith('/')) { "Absolute archive entry path" }
        require(!relative.contains('\\')) { "Invalid archive entry path" }

        val parts = relative.split('/')
        require(parts.none { it.isBlank() || it == "." || it == ".." || it.contains('\u0000') }) {
            "Unsafe archive entry path"
        }

        return parts.joinToString("/")
    }

    private fun validateArchiveDirectory(entryName: String) {
        require(entryName.endsWith('/')) { "Invalid backup archive directory" }
        when {
            entryName == "downloads/" ||
                entryName == OfflineDownloadArchive.DOWNLOAD_FILES_PREFIX ||
                entryName == OfflineDownloadArchive.DOWNLOAD_DATABASE_PREFIX -> Unit
            entryName.startsWith(OfflineDownloadArchive.DOWNLOAD_FILES_PREFIX) -> {
                safeArchiveRelativePath(
                    entryName.removeSuffix("/"),
                    OfflineDownloadArchive.DOWNLOAD_FILES_PREFIX,
                )
            }
            entryName.startsWith(OfflineDownloadArchive.DOWNLOAD_DATABASE_PREFIX) -> {
                safeArchiveRelativePath(
                    entryName.removeSuffix("/"),
                    OfflineDownloadArchive.DOWNLOAD_DATABASE_PREFIX,
                )
            }
            else -> throw IllegalArgumentException("Backup archive contains unsupported directory $entryName")
        }
    }

    private fun InputStream.readBytesLimited(limit: Long): ByteArray {
        val output = ByteArrayOutputStream()
        copyToLimited(output, limit)
        return output.toByteArray()
    }

    private fun InputStream.copyToLimited(output: OutputStream, limit: Long): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) return total
            total += read
            require(total <= limit) { "Backup entry exceeds the allowed size" }
            output.write(buffer, 0, read)
        }
    }

    private fun InputStream.copyToLimitedAndDigest(output: OutputStream?, limit: Long): CopiedArchiveEntry {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) return CopiedArchiveEntry(total, digest.digest().toHex())
            total += read
            require(total <= limit) { "Backup entry exceeds the allowed size" }
            output?.write(buffer, 0, read)
            digest.update(buffer, 0, read)
        }
    }

    private fun decodeSnapshot(bytes: ByteArray): OmniBackupSnapshot = try {
        json.decodeFromString(bytes.toString(StandardCharsets.UTF_8))
    } catch (error: SerializationException) {
        throw IllegalArgumentException("Backup archive library.json is invalid", error)
    } catch (error: IllegalArgumentException) {
        throw IllegalArgumentException("Backup archive library.json is invalid", error)
    }

    private fun decodeManifest(bytes: ByteArray): BackupArchiveManifest = try {
        json.decodeFromString(bytes.toString(StandardCharsets.UTF_8))
    } catch (error: SerializationException) {
        throw IllegalArgumentException("Backup archive manifest.json is invalid", error)
    } catch (error: IllegalArgumentException) {
        throw IllegalArgumentException("Backup archive manifest.json is invalid", error)
    }

    private fun media3DatabaseNames() = setOf(
        OfflineDownloadArchive.MEDIA3_DATABASE_NAME,
        "${OfflineDownloadArchive.MEDIA3_DATABASE_NAME}-wal",
        "${OfflineDownloadArchive.MEDIA3_DATABASE_NAME}-shm",
    )

    companion object {
        private const val MAX_LIBRARY_JSON_BYTES = 64L * 1024 * 1024
        private const val MAX_MANIFEST_JSON_BYTES = 16L * 1024 * 1024
        private const val MAX_DATABASE_ENTRY_BYTES = 512L * 1024 * 1024
        private const val MAX_AUDIO_ENTRY_BYTES = 2L * 1024 * 1024 * 1024
        private const val MIN_FREE_SPACE_BYTES = 256L * 1024 * 1024
        private const val MAX_ARCHIVE_ENTRIES = 100_000
    }

    private suspend fun MusicDatabase.verifyReplaceRestore(snapshot: OmniBackupSnapshot) {
        val restored = createSnapshot().toPreviewCounts()
        require(restored == snapshot.toPreviewCounts()) {
            "Database verification does not match the selected backup"
        }
    }

    private suspend fun MusicDatabase.restoreMerge(
        snapshot: OmniBackupSnapshot,
        restoreDownloadedAudioState: Boolean,
    ): OmniBackupCounts {
        var counts = OmniBackupCounts()
        val validSongIds = snapshot.songs.map { it.id }.filter { it.isNotBlank() }.toSet()

        snapshot.artists.forEach { artist ->
            if (artist.id.isBlank() || artist.name.isBlank()) {
                counts = counts.copy(skippedInvalidRows = counts.skippedInvalidRows + 1)
                return@forEach
            }
            val existing = backupArtistById(artist.id)
            val merged = artist.toArtistEntity(existing)
            if (existing == null) {
                insert(merged)
                counts = counts.copy(artists = counts.artists + 1)
            } else {
                upsert(merged)
            }
        }

        snapshot.albums.forEach { album ->
            if (album.id.isBlank() || album.title.isBlank()) {
                counts = counts.copy(skippedInvalidRows = counts.skippedInvalidRows + 1)
                return@forEach
            }
            val existing = backupAlbumById(album.id)
            val merged = album.toAlbumEntity(existing)
            if (existing == null) {
                insert(merged)
                counts = counts.copy(albums = counts.albums + 1)
            } else {
                upsert(merged)
            }
        }

        snapshot.songs.forEach { song ->
            if (song.id.isBlank() || song.title.isBlank()) {
                counts = counts.copy(skippedInvalidRows = counts.skippedInvalidRows + 1)
                return@forEach
            }
            val existing = backupSongById(song.id)
            val merged = song.toSongEntity(existing, restoreDownloadedAudioState)
            if (existing == null) {
                insert(merged)
                counts = counts.copy(
                    songs = counts.songs + 1,
                    likedSongs = counts.likedSongs + if (merged.liked) 1 else 0,
                )
            } else {
                upsert(merged)
                if (!existing.liked && merged.liked) {
                    counts = counts.copy(likedSongs = counts.likedSongs + 1)
                }
            }
        }

        snapshot.songArtists.forEach { map ->
            if (map.songId !in validSongIds || map.artistId.isBlank()) {
                counts = counts.copy(skippedInvalidRows = counts.skippedInvalidRows + 1)
            } else {
                upsert(SongArtistMap(map.songId, map.artistId, map.position))
            }
        }
        snapshot.songAlbums.forEach { map ->
            if (map.songId !in validSongIds || map.albumId.isBlank()) {
                counts = counts.copy(skippedInvalidRows = counts.skippedInvalidRows + 1)
            } else {
                upsert(SongAlbumMap(map.songId, map.albumId, map.index))
            }
        }
        snapshot.albumArtists.forEach { map ->
            if (map.albumId.isBlank() || map.artistId.isBlank()) {
                counts = counts.copy(skippedInvalidRows = counts.skippedInvalidRows + 1)
            } else {
                upsert(AlbumArtistMap(map.albumId, map.artistId, map.order))
            }
        }

        val playlistIdMap = mutableMapOf<String, String>()
        snapshot.playlists.forEach { playlist ->
            if (playlist.id.isBlank() || playlist.name.isBlank()) {
                counts = counts.copy(skippedInvalidRows = counts.skippedInvalidRows + 1)
                return@forEach
            }
            val target = resolvePlaylistTarget(playlist)
            playlistIdMap[playlist.id] = target.id
            if (target.isNew) {
                insert(target.entity)
                counts = counts.copy(playlists = counts.playlists + 1)
            } else {
                update(target.entity)
            }
        }

        snapshot.playlistSongs
            .sortedWith(compareBy<BackupPlaylistSong> { it.playlistId }.thenBy { it.position })
            .forEach { entry ->
                val targetPlaylistId = playlistIdMap[entry.playlistId]
                if (targetPlaylistId == null || entry.songId !in validSongIds) {
                    counts = counts.copy(skippedInvalidRows = counts.skippedInvalidRows + 1)
                    return@forEach
                }
                if (checkInPlaylist(targetPlaylistId, entry.songId) > 0) {
                    counts = counts.copy(skippedDuplicates = counts.skippedDuplicates + 1)
                    return@forEach
                }
                val position = (maxPlaylistSongPosition(targetPlaylistId) ?: -1) + 1
                insert(
                    PlaylistSongMap(
                        playlistId = targetPlaylistId,
                        songId = entry.songId,
                        position = position,
                        setVideoId = entry.setVideoId,
                    ),
                )
                counts = counts.copy(playlistEntries = counts.playlistEntries + 1)
            }

        val validTagIds = snapshot.tags.map { it.id }.filter { it.isNotBlank() }.toSet()
        snapshot.tags.forEach { tag ->
            if (tag.id.isBlank() || tag.name.isBlank()) {
                counts = counts.copy(skippedInvalidRows = counts.skippedInvalidRows + 1)
                return@forEach
            }
            val existing = backupTagById(tag.id)
            val entity = tag.toTagEntity(existing)
            if (existing == null) {
                insert(entity)
                counts = counts.copy(tags = counts.tags + 1)
            } else {
                update(entity)
            }
        }

        snapshot.playlistTags.forEach { map ->
            val targetPlaylistId = playlistIdMap[map.playlistId] ?: map.playlistId
            if (targetPlaylistId.isBlank() || map.tagId !in validTagIds) {
                counts = counts.copy(skippedInvalidRows = counts.skippedInvalidRows + 1)
                return@forEach
            }
            insert(
                PlaylistTagMap(
                    playlistId = targetPlaylistId,
                    tagId = map.tagId,
                    createdAt = map.createdAtEpochMillis.toLocalDateTimeOrNull() ?: LocalDateTime.now(),
                ),
            )
            counts = counts.copy(playlistTags = counts.playlistTags + 1)
        }

        snapshot.history.forEach { item ->
            val timestamp = item.timestampEpochMillis.toLocalDateTimeOrNull()
            if (item.songId !in validSongIds || timestamp == null || item.playTime < 0) {
                counts = counts.copy(skippedInvalidRows = counts.skippedInvalidRows + 1)
                return@forEach
            }
            if (backupEventExists(item.songId, timestamp, item.playTime) > 0) {
                counts = counts.copy(skippedDuplicates = counts.skippedDuplicates + 1)
                return@forEach
            }
            insert(Event(songId = item.songId, timestamp = timestamp, playTime = item.playTime))
            counts = counts.copy(historyItems = counts.historyItems + 1)
        }

        snapshot.stats.forEach { item ->
            if (item.songId !in validSongIds || item.year <= 0 || item.month !in 1..12 || item.count < 0) {
                counts = counts.copy(skippedInvalidRows = counts.skippedInvalidRows + 1)
                return@forEach
            }
            val existing = backupPlayCount(item.songId, item.year, item.month)
            if (existing == null) {
                insert(PlayCountEntity(item.songId, item.year, item.month, item.count))
                counts = counts.copy(statRecords = counts.statRecords + 1)
            } else {
                val mergedCount = max(existing.count, item.count)
                if (mergedCount != existing.count) {
                    backupUpdatePlayCount(item.songId, item.year, item.month, mergedCount)
                    counts = counts.copy(statRecords = counts.statRecords + 1)
                } else {
                    counts = counts.copy(skippedDuplicates = counts.skippedDuplicates + 1)
                }
            }
        }

        return counts
    }

    private suspend fun MusicDatabase.clearLibraryForReplace() {
        // Queue, playback metadata, and caches refer to the old library and are
        // not part of a restorable snapshot. Clear them inside the same Room
        // transaction so Replace cannot leave orphaned records behind.
        backupClearQueue()
        backupClearSongSkips()
        backupClearSetVideoIds()
        backupClearRelatedSongMaps()
        backupClearFormats()
        backupClearLyrics()
        backupClearPlaylistTagMaps()
        backupClearTags()
        backupClearEvents()
        backupClearPlayCounts()
        backupClearPlaylistSongMaps()
        backupClearPlaylists()
        backupClearSongArtistMaps()
        backupClearSongAlbumMaps()
        backupClearAlbumArtistMaps()
        backupClearSongs()
        backupClearAlbums()
        backupClearArtists()
    }

    private suspend fun MusicDatabase.resolvePlaylistTarget(playlist: BackupPlaylist): PlaylistTarget {
        backupPlaylistById(playlist.id)?.let { existing ->
            return PlaylistTarget(
                id = existing.id,
                entity = playlist.toPlaylistEntity(existing = existing),
                isNew = false,
            )
        }

        val nameConflict = backupPlaylistByName(playlist.name) != null
        val restoredName = if (nameConflict) nextRestoredPlaylistName(playlist.name) else playlist.name
        val restoredId = if (nameConflict) PlaylistEntity.generatePlaylistId() else playlist.id
        val entity = playlist.toPlaylistEntity(
            existing = null,
            id = restoredId,
            name = restoredName,
            browseId = null,
            forceLocal = true,
        )
        return PlaylistTarget(id = entity.id, entity = entity, isNew = true)
    }

    private suspend fun MusicDatabase.nextRestoredPlaylistName(name: String): String {
        var candidate = "$name (Restored)"
        var suffix = 2
        while (backupPlaylistByName(candidate) != null) {
            candidate = "$name (Restored $suffix)"
            suffix++
        }
        return candidate
    }

    private data class PlaylistTarget(
        val id: String,
        val entity: PlaylistEntity,
        val isNew: Boolean,
    )

    private data class BackupPackageReadResult(
        val snapshot: OmniBackupSnapshot,
        val downloadCounts: OmniBackupCounts = OmniBackupCounts(),
        val stagedDownloadDir: File? = null,
        val archiveInfo: BackupArchiveInfo = BackupArchiveInfo(),
    )

    private data class CopiedArchiveEntry(
        val byteCount: Long,
        val sha256: String,
    )
}

private fun OmniBackupSnapshot.exportedCounts() = toPreviewCounts()

private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes)
    .toHex()

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

private fun SongEntity.toBackupSong() = BackupSong(
    id = id,
    title = title,
    duration = duration,
    thumbnailUrl = thumbnailUrl,
    albumId = albumId,
    albumName = albumName,
    explicit = explicit,
    year = year,
    dateEpochMillis = date.toEpochMillisOrNull(),
    dateModifiedEpochMillis = dateModified.toEpochMillisOrNull(),
    liked = liked,
    likedDateEpochMillis = likedDate.toEpochMillisOrNull(),
    totalPlayTime = totalPlayTime,
    inLibraryEpochMillis = inLibrary.toEpochMillisOrNull(),
    dateDownloadEpochMillis = dateDownload.toEpochMillisOrNull(),
    isLocal = isLocal,
    downloadState = downloadState,
)

private fun BackupSong.toSongEntity(
    existing: SongEntity?,
    restoreDownloadedAudioState: Boolean,
) = SongEntity(
    id = id,
    title = title.ifBlank { existing?.title ?: "Unknown title" },
    duration = if (duration >= 0) duration else existing?.duration ?: -1,
    thumbnailUrl = thumbnailUrl ?: existing?.thumbnailUrl,
    albumId = albumId ?: existing?.albumId,
    albumName = albumName ?: existing?.albumName,
    explicit = explicit || (existing?.explicit == true),
    year = year ?: existing?.year,
    date = dateEpochMillis.toLocalDateTimeOrNull() ?: existing?.date,
    dateModified = latest(dateModifiedEpochMillis.toLocalDateTimeOrNull(), existing?.dateModified),
    liked = liked || (existing?.liked == true),
    likedDate = earliest(existing?.likedDate, likedDateEpochMillis.toLocalDateTimeOrNull()),
    totalPlayTime = max(totalPlayTime, existing?.totalPlayTime ?: 0L),
    inLibrary = earliest(existing?.inLibrary, inLibraryEpochMillis.toLocalDateTimeOrNull()),
    dateDownload = existing?.dateDownload ?: dateDownloadEpochMillis.toLocalDateTimeOrNull(),
    isLocal = isLocal || (existing?.isLocal == true),
    downloadState = existing?.downloadState ?: if (restoreDownloadedAudioState) downloadState else 0,
)

private fun ArtistEntity.toBackupArtist() = BackupArtist(
    id = id,
    name = name,
    thumbnailUrl = thumbnailUrl,
    channelId = channelId,
    lastUpdateTimeEpochMillis = lastUpdateTime.toEpochMillisOrNull(),
    bookmarkedAtEpochMillis = bookmarkedAt.toEpochMillisOrNull(),
    isLocal = isLocal,
)

private fun BackupArtist.toArtistEntity(existing: ArtistEntity?) = ArtistEntity(
    id = id,
    name = name.ifBlank { existing?.name ?: "Unknown artist" },
    thumbnailUrl = thumbnailUrl ?: existing?.thumbnailUrl,
    channelId = channelId ?: existing?.channelId,
    lastUpdateTime = latest(lastUpdateTimeEpochMillis.toLocalDateTimeOrNull(), existing?.lastUpdateTime)
        ?: LocalDateTime.now(),
    bookmarkedAt = earliest(existing?.bookmarkedAt, bookmarkedAtEpochMillis.toLocalDateTimeOrNull()),
    isLocal = isLocal || (existing?.isLocal == true),
)

private fun AlbumEntity.toBackupAlbum() = BackupAlbum(
    id = id,
    playlistId = playlistId,
    title = title,
    year = year,
    thumbnailUrl = thumbnailUrl,
    themeColor = themeColor,
    songCount = songCount,
    duration = duration,
    explicit = explicit,
    lastUpdateTimeEpochMillis = lastUpdateTime.toEpochMillisOrNull(),
    bookmarkedAtEpochMillis = bookmarkedAt.toEpochMillisOrNull(),
    likedDateEpochMillis = likedDate.toEpochMillisOrNull(),
    inLibraryEpochMillis = inLibrary.toEpochMillisOrNull(),
    isLocal = isLocal,
)

private fun BackupAlbum.toAlbumEntity(existing: AlbumEntity?) = AlbumEntity(
    id = id,
    playlistId = playlistId ?: existing?.playlistId,
    title = title.ifBlank { existing?.title ?: "Unknown album" },
    year = year ?: existing?.year,
    thumbnailUrl = thumbnailUrl ?: existing?.thumbnailUrl,
    themeColor = themeColor ?: existing?.themeColor,
    songCount = max(songCount, existing?.songCount ?: 0),
    duration = max(duration, existing?.duration ?: 0),
    explicit = explicit || (existing?.explicit == true),
    lastUpdateTime = latest(lastUpdateTimeEpochMillis.toLocalDateTimeOrNull(), existing?.lastUpdateTime)
        ?: LocalDateTime.now(),
    bookmarkedAt = earliest(existing?.bookmarkedAt, bookmarkedAtEpochMillis.toLocalDateTimeOrNull()),
    likedDate = earliest(existing?.likedDate, likedDateEpochMillis.toLocalDateTimeOrNull()),
    inLibrary = earliest(existing?.inLibrary, inLibraryEpochMillis.toLocalDateTimeOrNull()),
    isLocal = isLocal || (existing?.isLocal == true),
)

private fun PlaylistEntity.toBackupPlaylist() = BackupPlaylist(
    id = id,
    name = name,
    browseId = browseId,
    createdAtEpochMillis = createdAt.toEpochMillisOrNull(),
    lastUpdateTimeEpochMillis = lastUpdateTime.toEpochMillisOrNull(),
    isEditable = isEditable,
    bookmarkedAtEpochMillis = bookmarkedAt.toEpochMillisOrNull(),
    remoteSongCount = remoteSongCount,
    playEndpointParams = playEndpointParams,
    thumbnailUrl = thumbnailUrl,
    shuffleEndpointParams = shuffleEndpointParams,
    radioEndpointParams = radioEndpointParams,
    customOrder = customOrder,
    isLocal = isLocal,
    isAutoSync = isAutoSync,
)

private fun BackupPlaylist.toPlaylistEntity(
    existing: PlaylistEntity?,
    id: String = this.id,
    name: String = this.name,
    browseId: String? = this.browseId,
    forceLocal: Boolean = false,
) = PlaylistEntity(
    id = id,
    name = name.ifBlank { existing?.name ?: "Restored playlist" },
    browseId = browseId ?: existing?.browseId,
    createdAt = existing?.createdAt ?: createdAtEpochMillis.toLocalDateTimeOrNull(),
    lastUpdateTime = latest(lastUpdateTimeEpochMillis.toLocalDateTimeOrNull(), existing?.lastUpdateTime),
    isEditable = existing?.isEditable ?: isEditable,
    bookmarkedAt = earliest(existing?.bookmarkedAt, bookmarkedAtEpochMillis.toLocalDateTimeOrNull()) ?: LocalDateTime.now(),
    remoteSongCount = remoteSongCount ?: existing?.remoteSongCount,
    playEndpointParams = playEndpointParams ?: existing?.playEndpointParams,
    thumbnailUrl = thumbnailUrl ?: existing?.thumbnailUrl,
    shuffleEndpointParams = shuffleEndpointParams ?: existing?.shuffleEndpointParams,
    radioEndpointParams = radioEndpointParams ?: existing?.radioEndpointParams,
    customOrder = existing?.customOrder ?: customOrder,
    isLocal = forceLocal || isLocal || (existing?.isLocal == true),
    isAutoSync = existing?.isAutoSync ?: isAutoSync,
)

private fun PlaylistSongMap.toBackupPlaylistSong() = BackupPlaylistSong(
    playlistId = playlistId,
    songId = songId,
    position = position,
    setVideoId = setVideoId,
)

private fun SongArtistMap.toBackupSongArtist() = BackupSongArtist(
    songId = songId,
    artistId = artistId,
    position = position,
)

private fun SongAlbumMap.toBackupSongAlbum() = BackupSongAlbum(
    songId = songId,
    albumId = albumId,
    index = index,
)

private fun AlbumArtistMap.toBackupAlbumArtist() = BackupAlbumArtist(
    albumId = albumId,
    artistId = artistId,
    order = order,
)

private fun TagEntity.toBackupTag() = BackupTag(
    id = id,
    name = name,
    color = color,
    createdAtEpochMillis = createdAt.toEpochMillisOrNull(),
)

private fun BackupTag.toTagEntity(existing: TagEntity?) = TagEntity(
    id = id,
    name = name.ifBlank { existing?.name ?: "Restored tag" },
    color = color.ifBlank { existing?.color ?: "#FF6B6B" },
    createdAt = existing?.createdAt ?: createdAtEpochMillis.toLocalDateTimeOrNull() ?: LocalDateTime.now(),
)

private fun PlaylistTagMap.toBackupPlaylistTag() = BackupPlaylistTag(
    playlistId = playlistId,
    tagId = tagId,
    createdAtEpochMillis = createdAt.toEpochMillisOrNull(),
)

private fun Event.toBackupHistoryItem() = BackupHistoryItem(
    songId = songId,
    timestampEpochMillis = timestamp.toEpochMillisOrNull() ?: 0L,
    playTime = playTime,
)

private fun PlayCountEntity.toBackupStatsItem() = BackupStatsItem(
    songId = song,
    year = year,
    month = month,
    count = count,
)

private fun LocalDateTime?.toEpochMillisOrNull(): Long? =
    this?.toInstant(ZoneOffset.UTC)?.toEpochMilli()

private fun Long?.toLocalDateTimeOrNull(): LocalDateTime? =
    this?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) }

private fun earliest(a: LocalDateTime?, b: LocalDateTime?): LocalDateTime? = when {
    a == null -> b
    b == null -> a
    a <= b -> a
    else -> b
}

private fun latest(a: LocalDateTime?, b: LocalDateTime?): LocalDateTime? = when {
    a == null -> b
    b == null -> a
    a >= b -> a
    else -> b
}

private class CountingOutputStream(
    outputStream: OutputStream,
) : FilterOutputStream(outputStream) {
    var bytesWritten: Long = 0
        private set

    override fun write(b: Int) {
        out.write(b)
        bytesWritten++
    }

    override fun write(
        b: ByteArray,
        off: Int,
        len: Int,
    ) {
        out.write(b, off, len)
        bytesWritten += len
    }
}
