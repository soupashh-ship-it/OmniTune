/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.backup

import kotlinx.serialization.Serializable

const val OMNI_BACKUP_FORMAT_VERSION = 1

@Serializable
data class OmniBackupSnapshot(
    val formatVersion: Int = OMNI_BACKUP_FORMAT_VERSION,
    val appName: String = "OmniTune",
    val createdAtEpochMillis: Long,
    val appVersionName: String? = null,
    val appVersionCode: Long? = null,
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
    val history: List<BackupHistoryItem> = emptyList(),
    val stats: List<BackupStatsItem> = emptyList(),
    val settings: BackupSettingsSection? = null,
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
)

@Serializable
data class BackupSettingsSection(
    val note: String = "Library backup metadata only. Secrets and device-specific preferences are not exported.",
)

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
