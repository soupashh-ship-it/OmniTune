/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OmniBackupPreflightTest {
    @Test
    fun validMergeAndReplaceProducePreview() {
        val snapshot = validSnapshot()

        val merge = OmniBackupPreflight.validate(snapshot, OmniRestoreMode.MERGE)
        val replace = OmniBackupPreflight.validate(snapshot, OmniRestoreMode.REPLACE)

        assertEquals(1, merge.counts.songs)
        assertFalse(merge.replacesCurrentLibrary)
        assertTrue(replace.replacesCurrentLibrary)
        assertEquals(1, replace.counts.playlistEntries)
    }

    @Test
    fun emptyValidFormatBackupCannotReplace() {
        val error = assertThrows(BackupPreflightException::class.java) {
            OmniBackupPreflight.validate(
                OmniBackupSnapshot(createdAtEpochMillis = 1L),
                OmniRestoreMode.REPLACE,
            )
        }

        assertTrue(error.message.orEmpty().contains("no restorable"))
    }

    @Test
    fun unsupportedVersionIsRejected() {
        val error = assertThrows(BackupPreflightException::class.java) {
            OmniBackupPreflight.validate(validSnapshot().copy(formatVersion = 99), OmniRestoreMode.MERGE)
        }

        assertTrue(error.message.orEmpty().contains("Unsupported backup format"))
    }

    @Test
    fun missingRelationshipIsRejected() {
        val invalid = validSnapshot().copy(
            songArtists = listOf(BackupSongArtist(songId = "song", artistId = "missing")),
        )

        val error = assertThrows(BackupPreflightException::class.java) {
            OmniBackupPreflight.validate(invalid, OmniRestoreMode.REPLACE)
        }

        assertTrue(error.message.orEmpty().contains("song-artist"))
    }

    @Test
    fun duplicateIdentifiersAndPlaylistOrderingAreRejected() {
        val duplicateSong = validSnapshot().copy(
            songs = listOf(
                BackupSong(id = "song", title = "Song"),
                BackupSong(id = "song", title = "Duplicate"),
            ),
            library = validSnapshot().library.copy(exportedSongCount = 2),
        )
        val ordering = validSnapshot().copy(
            songs = listOf(BackupSong(id = "song", title = "Song"), BackupSong(id = "song-2", title = "Two")),
            playlistSongs = listOf(
                BackupPlaylistSong("playlist", "song", position = 0),
                BackupPlaylistSong("playlist", "song-2", position = 0),
            ),
            library = validSnapshot().library.copy(exportedSongCount = 2, exportedPlaylistEntryCount = 2),
        )

        val duplicateError = assertThrows(BackupPreflightException::class.java) {
            OmniBackupPreflight.validate(duplicateSong, OmniRestoreMode.MERGE)
        }
        val orderError = assertThrows(BackupPreflightException::class.java) {
            OmniBackupPreflight.validate(ordering, OmniRestoreMode.MERGE)
        }

        assertTrue(duplicateError.message.orEmpty().contains("Duplicate song"))
        assertTrue(orderError.message.orEmpty().contains("playlist ordering"))
    }

    @Test
    fun missingDeclaredArchiveFileIsRejected() {
        val sha = "a".repeat(64)
        val declared = BackupArchiveFile(
            entryName = OfflineDownloadArchive.DOWNLOAD_DATABASE_PREFIX + OfflineDownloadArchive.MEDIA3_DATABASE_NAME,
            byteCount = 4,
            sha256 = sha,
        )
        val archive = BackupArchiveInfo(
            isFullArchive = true,
            manifest = BackupArchiveManifest(librarySha256 = sha, files = listOf(declared)),
            actualLibrarySha256 = sha,
            actualFiles = emptyList(),
        )

        val error = assertThrows(BackupPreflightException::class.java) {
            OmniBackupPreflight.validate(validSnapshot(), OmniRestoreMode.REPLACE, archive)
        }

        assertTrue(error.message.orEmpty().contains("do not match its manifest"))
    }

    @Test
    fun corruptArchiveLibraryContentIsRejected() {
        val archive = BackupArchiveInfo(
            isFullArchive = true,
            manifest = BackupArchiveManifest(librarySha256 = "a".repeat(64)),
            actualLibrarySha256 = "b".repeat(64),
        )

        val error = assertThrows(BackupPreflightException::class.java) {
            OmniBackupPreflight.validate(validSnapshot(), OmniRestoreMode.MERGE, archive)
        }

        assertTrue(error.message.orEmpty().contains("library content does not match"))
    }

    @Test
    fun invalidQueueAndLargeCollectionAreRejected() {
        val queueError = assertThrows(BackupPreflightException::class.java) {
            OmniBackupPreflight.validate(
                validSnapshot().copy(queue = BackupQueueSection(mediaIds = listOf("missing"))),
                OmniRestoreMode.REPLACE,
            )
        }
        val largeSongs = List(50_001) { index -> BackupSong(id = "song-$index", title = "Song $index") }
        val large = OmniBackupSnapshot(
            createdAtEpochMillis = 1L,
            library = BackupLibrarySection(exportedSongCount = largeSongs.size),
            songs = largeSongs,
        )
        val sizeError = assertThrows(BackupPreflightException::class.java) {
            OmniBackupPreflight.validate(large, OmniRestoreMode.MERGE)
        }

        assertTrue(queueError.message.orEmpty().contains("Queue contains invalid"))
        assertTrue(sizeError.message.orEmpty().contains("too many songs"))
    }

    @Test
    fun oldVersionWithoutSummaryIsAcceptedWithWarning() {
        val legacy = validSnapshot().copy(
            formatVersion = 1,
            library = BackupLibrarySection(),
        )

        val preview = OmniBackupPreflight.validate(legacy, OmniRestoreMode.MERGE)

        assertTrue(preview.warnings.any { it.contains("Legacy") })
    }

    @Test
    fun mergePreviewKeepsExistingOfflineMediaUntouched() {
        val sha = "a".repeat(64)
        val files = listOf(
            BackupArchiveFile("downloads/files/song.mp3", 4, sha),
            BackupArchiveFile(
                OfflineDownloadArchive.DOWNLOAD_DATABASE_PREFIX + OfflineDownloadArchive.MEDIA3_DATABASE_NAME,
                4,
                sha,
            ),
        )
        val archive = BackupArchiveInfo(
            isFullArchive = true,
            manifest = BackupArchiveManifest(librarySha256 = sha, files = files),
            actualLibrarySha256 = sha,
            actualFiles = files,
            downloadedAudioFiles = 1,
            downloadedAudioBytes = 4,
            media3DatabaseFiles = 1,
        )

        val preview = OmniBackupPreflight.validate(validSnapshot(), OmniRestoreMode.MERGE, archive)

        assertTrue(preview.warnings.any { it.contains("only restored with Replace") })
        assertTrue(preview.unavailableItems.contains("Offline audio files and Media3 download index"))
    }

    @Test
    fun selectiveMergeRetainsRequiredTracksWithoutRestoringUnselectedLibraryData() {
        val selection = OmniRestoreSelection(
            libraryAndLikes = false,
            playlists = false,
            historyAndStats = true,
            downloads = false,
        )

        val selected = validSnapshot().selectForRestore(selection)
        val preview = OmniBackupPreflight.validate(selected, OmniRestoreMode.MERGE)

        assertEquals(listOf("song"), selected.songs.map { it.id })
        assertFalse(selected.songs.single().liked)
        assertTrue(selected.playlists.isEmpty())
        assertEquals(1, preview.counts.historyItems)
        assertEquals(1, preview.counts.statRecords)
    }

    @Test
    fun partialSelectionCannotReplaceTheWholeLibrary() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            OmniRestoreSelection(libraryAndLikes = true, playlists = false).requireSupportedFor(OmniRestoreMode.REPLACE)
        }

        assertTrue(error.message.orEmpty().contains("Replace restores"))
    }

    @Test
    fun legacyAndNewRoomSchemaMetadataAreHandledWithoutChangingLogicalCompatibility() {
        val legacy = OmniBackupPreflight.validate(
            validSnapshot().copy(databaseSchemaVersion = 7),
            OmniRestoreMode.MERGE,
        )
        val newerRoom = OmniBackupPreflight.validate(
            validSnapshot().copy(roomSchemaVersion = 8),
            OmniRestoreMode.MERGE,
        )

        assertTrue(legacy.warnings.any { it.contains("Legacy databaseSchemaVersion") })
        assertTrue(newerRoom.warnings.any { it.contains("newer Room schema") })
    }

    private fun validSnapshot(): OmniBackupSnapshot = OmniBackupSnapshot(
        createdAtEpochMillis = 1_725_000_000_000L,
        library = BackupLibrarySection(
            exportedSongCount = 1,
            exportedLikedSongCount = 1,
            exportedPlaylistCount = 1,
            exportedPlaylistEntryCount = 1,
            exportedArtistCount = 1,
            exportedAlbumCount = 1,
            exportedHistoryItemCount = 1,
            exportedStatsRecordCount = 1,
            exportedTagCount = 1,
            exportedPlaylistTagCount = 1,
        ),
        songs = listOf(BackupSong(id = "song", title = "Song", liked = true)),
        artists = listOf(BackupArtist(id = "artist", name = "Artist")),
        albums = listOf(BackupAlbum(id = "album", title = "Album")),
        playlists = listOf(BackupPlaylist(id = "playlist", name = "Playlist")),
        playlistSongs = listOf(BackupPlaylistSong("playlist", "song", position = 0)),
        songArtists = listOf(BackupSongArtist("song", "artist", position = 0)),
        songAlbums = listOf(BackupSongAlbum("song", "album", index = 0)),
        albumArtists = listOf(BackupAlbumArtist("album", "artist", order = 0)),
        tags = listOf(BackupTag("tag", "Tag")),
        playlistTags = listOf(BackupPlaylistTag("playlist", "tag")),
        history = listOf(BackupHistoryItem("song", 1_725_000_000_001L, 100L)),
        stats = listOf(BackupStatsItem("song", 2026, 7, 1)),
    )
}
