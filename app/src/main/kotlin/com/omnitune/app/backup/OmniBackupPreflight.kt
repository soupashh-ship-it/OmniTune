/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.backup

import com.omnitune.app.db.CURRENT_ROOM_DATABASE_SCHEMA_VERSION

/**
 * The archive reader supplies this after it has staged and hashed a ZIP. It is
 * intentionally platform-free so the destructive Restore gate is JVM-testable.
 */
data class BackupArchiveInfo(
    val isFullArchive: Boolean = false,
    val manifest: BackupArchiveManifest? = null,
    val actualLibrarySha256: String? = null,
    val actualFiles: List<BackupArchiveFile> = emptyList(),
    val downloadedAudioFiles: Int = 0,
    val downloadedAudioBytes: Long = 0,
    val media3DatabaseFiles: Int = 0,
)

data class OmniBackupPreview(
    val mode: OmniRestoreMode,
    val selection: OmniRestoreSelection = OmniRestoreSelection.ALL,
    val counts: OmniBackupCounts,
    val warnings: List<String>,
    val unavailableItems: List<String>,
    val replacesCurrentLibrary: Boolean,
    val includesSettings: Boolean,
    val includesQueue: Boolean,
    val archiveIsFull: Boolean,
)

class BackupPreflightException(message: String) : IllegalArgumentException(message)

/**
 * Validates every relationship before a Replace can reach clearLibraryForReplace.
 * Merge uses the same structural gate; only conflicts with existing database
 * records are resolved by its documented deterministic merge policy.
 */
object OmniBackupPreflight {
    private const val MAX_RECORDS_PER_COLLECTION = 50_000
    private const val MAX_TOTAL_RECORDS = 250_000
    private const val MAX_DOWNLOAD_FILES = 20_000
    private const val MAX_DOWNLOAD_BYTES = 20L * 1024 * 1024 * 1024
    private val SHA_256 = Regex("[a-f0-9]{64}")

    fun validate(
        snapshot: OmniBackupSnapshot,
        mode: OmniRestoreMode,
        archive: BackupArchiveInfo = BackupArchiveInfo(),
        allowEmpty: Boolean = false,
    ): OmniBackupPreview {
        val violations = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (snapshot.appName != "OmniTune") violations += "This backup was not created by OmniTune"
        if (snapshot.formatVersion !in 1..OMNI_BACKUP_FORMAT_VERSION) {
            violations += "Unsupported backup format version ${snapshot.formatVersion}"
        }
        if (snapshot.createdAtEpochMillis <= 0) violations += "Backup creation time is invalid"
        val roomSchemaVersion = snapshot.roomSchemaVersion ?: snapshot.databaseSchemaVersion
        if (roomSchemaVersion != null && roomSchemaVersion <= 0) {
            violations += "Backup has an invalid Room schema metadata value"
        }
        if (snapshot.databaseSchemaVersion != null && snapshot.roomSchemaVersion == null) {
            warnings += "Legacy databaseSchemaVersion metadata was accepted as Room schema information."
        }
        if (roomSchemaVersion != null && roomSchemaVersion > CURRENT_ROOM_DATABASE_SCHEMA_VERSION) {
            warnings += "Backup was created with a newer Room schema; logical backup format validation still applies."
        }

        val collections = listOf(
            "songs" to snapshot.songs.size,
            "artists" to snapshot.artists.size,
            "albums" to snapshot.albums.size,
            "playlists" to snapshot.playlists.size,
            "playlist entries" to snapshot.playlistSongs.size,
            "song artists" to snapshot.songArtists.size,
            "song albums" to snapshot.songAlbums.size,
            "album artists" to snapshot.albumArtists.size,
            "tags" to snapshot.tags.size,
            "playlist tags" to snapshot.playlistTags.size,
            "history items" to snapshot.history.size,
            "statistics records" to snapshot.stats.size,
        )
        collections.filter { it.second > MAX_RECORDS_PER_COLLECTION }
            .forEach { violations += "Backup contains too many ${it.first}" }
        if (collections.sumOf { it.second } > MAX_TOTAL_RECORDS) {
            violations += "Backup contains an unreasonably large number of records"
        }

        validateDeclaredCounts(snapshot, violations, warnings)

        val songIds = validateIds("song", snapshot.songs.map { it.id }, violations)
        val artistIds = validateIds("artist", snapshot.artists.map { it.id }, violations)
        val albumIds = validateIds("album", snapshot.albums.map { it.id }, violations)
        val playlistIds = validateIds("playlist", snapshot.playlists.map { it.id }, violations)
        val tagIds = validateIds("tag", snapshot.tags.map { it.id }, violations)

        snapshot.songs.forEach { song ->
            if (song.title.isBlank()) violations += "Song ${displayId(song.id)} has no title"
            if (song.duration < -1 || song.totalPlayTime < 0 || song.downloadState < 0) {
                violations += "Song ${displayId(song.id)} contains invalid playback metadata"
            }
        }
        snapshot.artists.filter { it.name.isBlank() }
            .forEach { violations += "Artist ${displayId(it.id)} has no name" }
        snapshot.albums.filter { it.title.isBlank() }
            .forEach { violations += "Album ${displayId(it.id)} has no title" }
        snapshot.playlists.forEach { playlist ->
            if (playlist.name.isBlank()) violations += "Playlist ${displayId(playlist.id)} has no name"
            if (playlist.remoteSongCount != null && playlist.remoteSongCount < 0) {
                violations += "Playlist ${displayId(playlist.id)} has an invalid song count"
            }
        }
        snapshot.tags.filter { it.name.isBlank() }
            .forEach { violations += "Tag ${displayId(it.id)} has no name" }

        validateRelations(snapshot, songIds, artistIds, albumIds, playlistIds, tagIds, violations)
        validateArchive(snapshot, archive, violations, warnings)

        val restorableItems = collections.sumOf { it.second }
        if (!allowEmpty && restorableItems == 0) {
            violations += "Backup is structurally valid but contains no restorable library data"
        }
        if (!archive.isFullArchive && snapshot.songs.any { it.downloadState > 0 }) {
            warnings += "Download-state metadata is present, but this JSON backup has no offline audio files."
        }
        if (mode == OmniRestoreMode.MERGE && archive.isFullArchive && archive.actualFiles.isNotEmpty()) {
            warnings += "Offline audio is only restored with Replace. Media3 download indexes cannot be safely merged."
        }
        if (snapshot.settings != null) {
            warnings += "Settings are metadata only and are not restored."
        }
        if (snapshot.queue != null) {
            warnings += "Queue data is not restored in this version."
        }
        if (snapshot.formatVersion == 1) {
            warnings += "Legacy format v1 has no archive integrity manifest; JSON library data was structurally checked."
        }

        if (violations.isNotEmpty()) {
            throw BackupPreflightException(violations.distinct().joinToString(separator = "\n"))
        }

        return OmniBackupPreview(
            mode = mode,
            counts = snapshot.toPreviewCounts().copy(
                downloadedAudioFiles = archive.downloadedAudioFiles,
                downloadedAudioBytes = archive.downloadedAudioBytes,
            ),
            warnings = warnings.distinct(),
            unavailableItems = buildList {
                if (snapshot.settings != null) add("Settings")
                if (snapshot.queue != null) add("Queue")
                if (!archive.isFullArchive && snapshot.songs.any { it.downloadState > 0 }) add("Offline audio files")
                if (mode == OmniRestoreMode.MERGE && archive.isFullArchive && archive.actualFiles.isNotEmpty()) {
                    add("Offline audio files and Media3 download index")
                }
            },
            replacesCurrentLibrary = mode == OmniRestoreMode.REPLACE,
            includesSettings = false,
            includesQueue = false,
            archiveIsFull = archive.isFullArchive,
        )
    }

    private fun validateDeclaredCounts(
        snapshot: OmniBackupSnapshot,
        violations: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        val actual = snapshot.toPreviewCounts()
        val declared = snapshot.library
        val declaredValues = listOf(
            declared.exportedSongCount,
            declared.exportedLikedSongCount,
            declared.exportedPlaylistCount,
            declared.exportedPlaylistEntryCount,
            declared.exportedArtistCount,
            declared.exportedAlbumCount,
            declared.exportedHistoryItemCount,
            declared.exportedStatsRecordCount,
            declared.exportedTagCount,
            declared.exportedPlaylistTagCount,
        )
        val legacyNoSummary = snapshot.formatVersion == 1 && declaredValues.all { it == 0 } &&
            listOf(actual.songs, actual.artists, actual.albums, actual.playlists, actual.historyItems).any { it > 0 }
        if (legacyNoSummary) {
            warnings += "Legacy backup has no collection summary; collection contents were counted directly."
            return
        }
        val matches = declared.exportedSongCount == actual.songs &&
            declared.exportedLikedSongCount == actual.likedSongs &&
            declared.exportedPlaylistCount == actual.playlists &&
            declared.exportedPlaylistEntryCount == actual.playlistEntries &&
            declared.exportedArtistCount == actual.artists &&
            declared.exportedAlbumCount == actual.albums &&
            declared.exportedHistoryItemCount == actual.historyItems &&
            declared.exportedStatsRecordCount == actual.statRecords &&
            declared.exportedTagCount == actual.tags &&
            declared.exportedPlaylistTagCount == actual.playlistTags
        if (!matches) violations += "Declared backup counts do not match the archive contents"
    }

    private fun validateIds(
        label: String,
        ids: List<String>,
        violations: MutableList<String>,
    ): Set<String> {
        ids.filter { it.isBlank() }.forEach { violations += "Backup contains a $label with a blank identifier" }
        ids.groupingBy { it }.eachCount()
            .filter { (id, count) -> id.isNotBlank() && count > 1 }
            .forEach { (id, _) -> violations += "Duplicate $label identifier ${displayId(id)}" }
        return ids.filter { it.isNotBlank() }.toSet()
    }

    private fun validateRelations(
        snapshot: OmniBackupSnapshot,
        songIds: Set<String>,
        artistIds: Set<String>,
        albumIds: Set<String>,
        playlistIds: Set<String>,
        tagIds: Set<String>,
        violations: MutableList<String>,
    ) {
        snapshot.songArtists.forEach { relation ->
            if (relation.songId !in songIds || relation.artistId !in artistIds || relation.position < 0) {
                violations += "Invalid song-artist relationship"
            }
        }
        duplicate(snapshot.songArtists.map { "${it.songId}\u0000${it.artistId}" }, "song-artist relationship", violations)

        snapshot.songAlbums.forEach { relation ->
            if (relation.songId !in songIds || relation.albumId !in albumIds || relation.index < 0) {
                violations += "Invalid song-album relationship"
            }
        }
        duplicate(snapshot.songAlbums.map { "${it.songId}\u0000${it.albumId}" }, "song-album relationship", violations)

        snapshot.albumArtists.forEach { relation ->
            if (relation.albumId !in albumIds || relation.artistId !in artistIds || relation.order < 0) {
                violations += "Invalid album-artist relationship"
            }
        }
        duplicate(snapshot.albumArtists.map { "${it.albumId}\u0000${it.artistId}" }, "album-artist relationship", violations)

        snapshot.playlistSongs.forEach { relation ->
            if (relation.playlistId !in playlistIds || relation.songId !in songIds || relation.position < 0) {
                violations += "Invalid playlist-track relationship"
            }
        }
        duplicate(snapshot.playlistSongs.map { "${it.playlistId}\u0000${it.songId}" }, "playlist-track relationship", violations)
        duplicate(snapshot.playlistSongs.map { "${it.playlistId}\u0000${it.position}" }, "playlist ordering", violations)

        snapshot.playlistTags.forEach { relation ->
            if (relation.playlistId !in playlistIds || relation.tagId !in tagIds) {
                violations += "Invalid playlist-tag relationship"
            }
        }
        duplicate(snapshot.playlistTags.map { "${it.playlistId}\u0000${it.tagId}" }, "playlist-tag relationship", violations)

        snapshot.history.forEach { item ->
            if (item.songId !in songIds || item.timestampEpochMillis <= 0 || item.playTime < 0) {
                violations += "Invalid history record"
            }
        }
        duplicate(snapshot.history.map { "${it.songId}\u0000${it.timestampEpochMillis}\u0000${it.playTime}" }, "history record", violations)

        snapshot.stats.forEach { item ->
            if (item.songId !in songIds || item.year !in 1900..9999 || item.month !in 1..12 || item.count < 0) {
                violations += "Invalid statistics record"
            }
        }
        duplicate(snapshot.stats.map { "${it.songId}\u0000${it.year}\u0000${it.month}" }, "statistics record", violations)

        snapshot.queue?.let { queue ->
            if (queue.startIndex !in 0..queue.mediaIds.lastIndex.coerceAtLeast(0) || queue.positionMillis < 0 ||
                queue.mediaIds.any { it !in songIds }
            ) {
                violations += "Queue contains invalid song references or playback position"
            }
        }
    }

    private fun validateArchive(
        snapshot: OmniBackupSnapshot,
        archive: BackupArchiveInfo,
        violations: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        if (!archive.isFullArchive) return
        if (archive.actualFiles.size > MAX_DOWNLOAD_FILES || archive.downloadedAudioFiles > MAX_DOWNLOAD_FILES ||
            archive.downloadedAudioBytes > MAX_DOWNLOAD_BYTES
        ) {
            violations += "Backup archive contains an unreasonable amount of offline audio"
        }
        val manifest = archive.manifest
        if (snapshot.formatVersion >= 2 && manifest == null) {
            violations += "Backup archive is missing its integrity manifest"
            return
        }
        if (manifest == null) {
            warnings += "Legacy ZIP archive has no file manifest; offline files cannot be completely verified."
            return
        }
        if (manifest.manifestVersion != 1 || !SHA_256.matches(manifest.librarySha256)) {
            violations += "Backup archive has invalid manifest metadata"
        }
        if (manifest.librarySha256 != archive.actualLibrarySha256) {
            violations += "Backup archive library content does not match its manifest"
        }
        if (manifest.files.size > MAX_DOWNLOAD_FILES) violations += "Backup archive manifest declares too many files"
        manifest.files.forEach { file ->
            if (!isAllowedDownloadEntry(file.entryName) || file.byteCount < 0 || !SHA_256.matches(file.sha256)) {
                violations += "Backup archive contains invalid download metadata"
            }
        }
        duplicate(manifest.files.map { it.entryName }, "archive file declaration", violations)
        duplicate(archive.actualFiles.map { it.entryName }, "archive file entry", violations)
        if (manifest.files.toSet() != archive.actualFiles.toSet()) {
            violations += "Backup archive files do not match its manifest"
        }
        val hasAudio = archive.actualFiles.any { it.entryName.startsWith(OfflineDownloadArchive.DOWNLOAD_FILES_PREFIX) }
        val hasIndex = archive.actualFiles.any { it.entryName == OfflineDownloadArchive.DOWNLOAD_DATABASE_PREFIX + OfflineDownloadArchive.MEDIA3_DATABASE_NAME }
        if (hasAudio && !hasIndex) violations += "Offline audio is missing its Media3 download index"
    }

    private fun isAllowedDownloadEntry(name: String): Boolean =
        name.startsWith(OfflineDownloadArchive.DOWNLOAD_FILES_PREFIX) ||
            name == OfflineDownloadArchive.DOWNLOAD_DATABASE_PREFIX + OfflineDownloadArchive.MEDIA3_DATABASE_NAME ||
            name == OfflineDownloadArchive.DOWNLOAD_DATABASE_PREFIX + "${OfflineDownloadArchive.MEDIA3_DATABASE_NAME}-wal" ||
            name == OfflineDownloadArchive.DOWNLOAD_DATABASE_PREFIX + "${OfflineDownloadArchive.MEDIA3_DATABASE_NAME}-shm"

    private fun duplicate(values: List<String>, label: String, violations: MutableList<String>) {
        if (values.groupingBy { it }.eachCount().any { it.value > 1 }) {
            violations += "Duplicate $label"
        }
    }

    private fun displayId(id: String) = id.ifBlank { "<blank>" }
}

internal fun OmniBackupSnapshot.toPreviewCounts() = OmniBackupCounts(
    songs = songs.size,
    likedSongs = songs.count { it.liked },
    playlists = playlists.size,
    playlistEntries = playlistSongs.size,
    artists = artists.size,
    albums = albums.size,
    historyItems = history.size,
    statRecords = stats.size,
    tags = tags.size,
    playlistTags = playlistTags.size,
)
