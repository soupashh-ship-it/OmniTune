/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.backup

import kotlinx.serialization.Serializable

/**
 * Version 2 adds an integrity manifest to full ZIP backups. JSON-only backups
 * intentionally remain supported because they do not contain downloadable files.
 */
const val OMNI_BACKUP_FORMAT_VERSION = 2

@Serializable
data class OmniBackupSnapshot(
    val formatVersion: Int = OMNI_BACKUP_FORMAT_VERSION,
    val appName: String = "OmniTune",
    val createdAtEpochMillis: Long,
    val appVersionName: String? = null,
    val appVersionCode: Long? = null,
    /**
     * Diagnostic Room schema metadata. Logical import compatibility is governed only by
     * [formatVersion], so a different Room version never triggers a destructive migration.
     */
    val roomSchemaVersion: Int? = null,
    /** Legacy spelling accepted from backups written before the metadata contract was clarified. */
    val databaseSchemaVersion: Int? = null,
    val library: BackupLibrarySection = BackupLibrarySection(),
    val songs: List<BackupSong> = emptyList(),
    val artists: List<BackupArtist> = emptyList(),
    val albums: List<BackupAlbum> = emptyList(),
    val playlists: List<BackupPlaylist> = emptyList(),
    val playlistSongs: List<BackupPlaylistSong> = emptyList(),
    val songArtists: List<BackupSongArtist> = emptyList(),
    val songAlbums: List<BackupSongAlbum> = emptyList(),
    val albumArtists: List<BackupAlbumArtist> = emptyList(),
    val tags: List<BackupTag> = emptyList(),
    val playlistTags: List<BackupPlaylistTag> = emptyList(),
    val history: List<BackupHistoryItem> = emptyList(),
    val stats: List<BackupStatsItem> = emptyList(),
    val settings: BackupSettingsSection? = null,
    val queue: BackupQueueSection? = null,
)

/**
 * Queue data is deliberately not restored yet. Keeping the shape here lets the
 * preflight reject malformed queue references instead of silently ignoring them.
 */
@Serializable
data class BackupQueueSection(
    val mediaIds: List<String> = emptyList(),
    val startIndex: Int = 0,
    val positionMillis: Long = 0,
)

/** Integrity data written as manifest.json in format-2 full ZIP archives. */
@Serializable
data class BackupArchiveManifest(
    val manifestVersion: Int = 1,
    val librarySha256: String,
    val files: List<BackupArchiveFile> = emptyList(),
)

@Serializable
data class BackupArchiveFile(
    val entryName: String,
    val byteCount: Long,
    val sha256: String,
)

@Serializable
data class BackupLibrarySection(
    val exportedSongCount: Int = 0,
    val exportedLikedSongCount: Int = 0,
    val exportedPlaylistCount: Int = 0,
    val exportedPlaylistEntryCount: Int = 0,
    val exportedArtistCount: Int = 0,
    val exportedAlbumCount: Int = 0,
    val exportedHistoryItemCount: Int = 0,
    val exportedStatsRecordCount: Int = 0,
    val exportedTagCount: Int = 0,
    val exportedPlaylistTagCount: Int = 0,
)

@Serializable
data class BackupSettingsSection(
    val note: String = "Library backup metadata only. Secrets and device-specific preferences are not exported.",
)

/**
 * Categories available when merging a backup. Related song records are included automatically
 * for playlists and history/statistics so imported relations never point to missing tracks.
 */
data class OmniRestoreSelection(
    val libraryAndLikes: Boolean = true,
    val playlists: Boolean = true,
    val historyAndStats: Boolean = true,
    val downloads: Boolean = true,
) {
    fun requireSupportedFor(mode: OmniRestoreMode) {
        require(libraryAndLikes || playlists || historyAndStats) {
            "Select at least one library category to restore"
        }
        require(mode != OmniRestoreMode.REPLACE || this == ALL) {
            "Replace restores the complete validated library; use Merge for selected categories"
        }
    }

    companion object {
        val ALL = OmniRestoreSelection()
    }
}

internal fun OmniBackupSnapshot.selectForRestore(selection: OmniRestoreSelection): OmniBackupSnapshot {
    val requiredSongIds = buildSet {
        if (selection.libraryAndLikes) addAll(songs.map { it.id })
        if (selection.playlists) addAll(playlistSongs.map { it.songId })
        if (selection.historyAndStats) {
            addAll(history.map { it.songId })
            addAll(stats.map { it.songId })
        }
    }
    val selectedSongs = songs
        .filter { it.id in requiredSongIds }
        .map { song ->
            if (selection.libraryAndLikes) song else song.copy(liked = false, likedDateEpochMillis = null)
        }
    val selectedPlaylists = if (selection.playlists) playlists else emptyList()
    val selectedPlaylistSongs = if (selection.playlists) playlistSongs else emptyList()
    val selectedTags = if (selection.playlists) tags else emptyList()
    val selectedPlaylistTags = if (selection.playlists) playlistTags else emptyList()
    val selectedArtists = if (selection.libraryAndLikes) artists else emptyList()
    val selectedAlbums = if (selection.libraryAndLikes) albums else emptyList()
    val selectedSongArtists = if (selection.libraryAndLikes) songArtists else emptyList()
    val selectedSongAlbums = if (selection.libraryAndLikes) songAlbums else emptyList()
    val selectedAlbumArtists = if (selection.libraryAndLikes) albumArtists else emptyList()
    val selectedHistory = if (selection.historyAndStats) history else emptyList()
    val selectedStats = if (selection.historyAndStats) stats else emptyList()

    return copy(
        library = BackupLibrarySection(
            exportedSongCount = selectedSongs.size,
            exportedLikedSongCount = selectedSongs.count { it.liked },
            exportedPlaylistCount = selectedPlaylists.size,
            exportedPlaylistEntryCount = selectedPlaylistSongs.size,
            exportedArtistCount = selectedArtists.size,
            exportedAlbumCount = selectedAlbums.size,
            exportedHistoryItemCount = selectedHistory.size,
            exportedStatsRecordCount = selectedStats.size,
            exportedTagCount = selectedTags.size,
            exportedPlaylistTagCount = selectedPlaylistTags.size,
        ),
        songs = selectedSongs,
        artists = selectedArtists,
        albums = selectedAlbums,
        playlists = selectedPlaylists,
        playlistSongs = selectedPlaylistSongs,
        songArtists = selectedSongArtists,
        songAlbums = selectedSongAlbums,
        albumArtists = selectedAlbumArtists,
        tags = selectedTags,
        playlistTags = selectedPlaylistTags,
        history = selectedHistory,
        stats = selectedStats,
    )
}

@Serializable
data class BackupSong(
    val id: String,
    val title: String,
    val duration: Int = -1,
    val thumbnailUrl: String? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    val explicit: Boolean = false,
    val year: Int? = null,
    val dateEpochMillis: Long? = null,
    val dateModifiedEpochMillis: Long? = null,
    val liked: Boolean = false,
    val likedDateEpochMillis: Long? = null,
    val totalPlayTime: Long = 0,
    val inLibraryEpochMillis: Long? = null,
    val dateDownloadEpochMillis: Long? = null,
    val isLocal: Boolean = false,
    val downloadState: Int = 0,
)

@Serializable
data class BackupArtist(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val channelId: String? = null,
    val lastUpdateTimeEpochMillis: Long? = null,
    val bookmarkedAtEpochMillis: Long? = null,
    val isLocal: Boolean = false,
)

@Serializable
data class BackupAlbum(
    val id: String,
    val playlistId: String? = null,
    val title: String,
    val year: Int? = null,
    val thumbnailUrl: String? = null,
    val themeColor: Int? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val explicit: Boolean = false,
    val lastUpdateTimeEpochMillis: Long? = null,
    val bookmarkedAtEpochMillis: Long? = null,
    val likedDateEpochMillis: Long? = null,
    val inLibraryEpochMillis: Long? = null,
    val isLocal: Boolean = false,
)

@Serializable
data class BackupPlaylist(
    val id: String,
    val name: String,
    val browseId: String? = null,
    val createdAtEpochMillis: Long? = null,
    val lastUpdateTimeEpochMillis: Long? = null,
    val isEditable: Boolean = true,
    val bookmarkedAtEpochMillis: Long? = null,
    val remoteSongCount: Int? = null,
    val playEndpointParams: String? = null,
    val thumbnailUrl: String? = null,
    val shuffleEndpointParams: String? = null,
    val radioEndpointParams: String? = null,
    val customOrder: Int? = null,
    val isLocal: Boolean = false,
    val isAutoSync: Boolean = false,
)

@Serializable
data class BackupPlaylistSong(
    val playlistId: String,
    val songId: String,
    val position: Int = 0,
    val setVideoId: String? = null,
)

@Serializable
data class BackupSongArtist(
    val songId: String,
    val artistId: String,
    val position: Int = 0,
)

@Serializable
data class BackupSongAlbum(
    val songId: String,
    val albumId: String,
    val index: Int = 0,
)

@Serializable
data class BackupAlbumArtist(
    val albumId: String,
    val artistId: String,
    val order: Int = 0,
)

@Serializable
data class BackupTag(
    val id: String,
    val name: String,
    val color: String = "#FF6B6B",
    val createdAtEpochMillis: Long? = null,
)

@Serializable
data class BackupPlaylistTag(
    val playlistId: String,
    val tagId: String,
    val createdAtEpochMillis: Long? = null,
)

@Serializable
data class BackupHistoryItem(
    val songId: String,
    val timestampEpochMillis: Long,
    val playTime: Long,
)

@Serializable
data class BackupStatsItem(
    val songId: String,
    val year: Int,
    val month: Int,
    val count: Int,
)
