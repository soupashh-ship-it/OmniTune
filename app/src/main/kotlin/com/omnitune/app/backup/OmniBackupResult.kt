/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.backup

data class OmniBackupCounts(
    val songs: Int = 0,
    val likedSongs: Int = 0,
    val playlists: Int = 0,
    val playlistEntries: Int = 0,
    val artists: Int = 0,
    val albums: Int = 0,
    val historyItems: Int = 0,
    val statRecords: Int = 0,
    val tags: Int = 0,
    val playlistTags: Int = 0,
    val downloadedAudioFiles: Int = 0,
    val downloadedAudioBytes: Long = 0,
    val skippedDuplicates: Int = 0,
    val skippedInvalidRows: Int = 0,
) {
    operator fun plus(other: OmniBackupCounts) = OmniBackupCounts(
        songs = songs + other.songs,
        likedSongs = likedSongs + other.likedSongs,
        playlists = playlists + other.playlists,
        playlistEntries = playlistEntries + other.playlistEntries,
        artists = artists + other.artists,
        albums = albums + other.albums,
        historyItems = historyItems + other.historyItems,
        statRecords = statRecords + other.statRecords,
        tags = tags + other.tags,
        playlistTags = playlistTags + other.playlistTags,
        downloadedAudioFiles = downloadedAudioFiles + other.downloadedAudioFiles,
        downloadedAudioBytes = downloadedAudioBytes + other.downloadedAudioBytes,
        skippedDuplicates = skippedDuplicates + other.skippedDuplicates,
        skippedInvalidRows = skippedInvalidRows + other.skippedInvalidRows,
    )
}

data class OmniBackupExportResult(
    val counts: OmniBackupCounts,
    val byteCount: Long,
    val createdAtEpochMillis: Long,
)

data class OmniBackupImportResult(
    val counts: OmniBackupCounts,
    val formatVersion: Int,
    val createdAtEpochMillis: Long,
    val offlineAudioRestorePending: Boolean = false,
    val safetyBackup: RestoreSafetyBackup? = null,
)

enum class RestoreFailurePhase(val userLabel: String) {
    ARCHIVE_READ("archive read"),
    PREFLIGHT("backup preflight"),
    SAFETY_BACKUP("safety backup"),
    DATABASE_TRANSACTION("database transaction"),
    MEDIA_RESTORE("offline media restore"),
    ROLLBACK("recovery rollback"),
}

class RestoreFailureException(
    val phase: RestoreFailurePhase,
    message: String,
    cause: Throwable? = null,
) : IllegalStateException("${phase.userLabel}: $message", cause)
