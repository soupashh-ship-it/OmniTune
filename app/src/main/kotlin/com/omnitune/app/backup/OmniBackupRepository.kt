/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.backup

import com.omnitune.app.BuildConfig
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.AlbumArtistMap
import com.omnitune.app.db.entities.AlbumEntity
import com.omnitune.app.db.entities.ArtistEntity
import com.omnitune.app.db.entities.Event
import com.omnitune.app.db.entities.PlayCountEntity
import com.omnitune.app.db.entities.PlaylistEntity
import com.omnitune.app.db.entities.PlaylistSongMap
import com.omnitune.app.db.entities.SongAlbumMap
import com.omnitune.app.db.entities.SongArtistMap
import com.omnitune.app.db.entities.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class OmniBackupRepository @Inject constructor(
    private val database: MusicDatabase,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun exportBackup(outputStream: OutputStream): OmniBackupExportResult = withContext(Dispatchers.IO) {
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

        val snapshot = OmniBackupSnapshot(
            createdAtEpochMillis = createdAt,
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE.toLong(),
            databaseSchemaVersion = 5,
            library = BackupLibrarySection(
                exportedSongCount = songs.size,
                exportedLikedSongCount = songs.count { it.liked },
                exportedPlaylistCount = playlists.size,
                exportedPlaylistEntryCount = playlistSongs.size,
                exportedArtistCount = artists.size,
                exportedAlbumCount = albums.size,
                exportedHistoryItemCount = events.size,
                exportedStatsRecordCount = playCounts.size,
            ),
            songs = songs.map { it.toBackupSong() },
            artists = artists.map { it.toBackupArtist() },
            albums = albums.map { it.toBackupAlbum() },
            playlists = playlists.map { it.toBackupPlaylist() },
            playlistSongs = playlistSongs.map { it.toBackupPlaylistSong() },
            songArtists = songArtists.map { it.toBackupSongArtist() },
            songAlbums = songAlbums.map { it.toBackupSongAlbum() },
            albumArtists = albumArtists.map { it.toBackupAlbumArtist() },
            history = events.map { it.toBackupHistoryItem() },
            stats = playCounts.map { it.toBackupStatsItem() },
            settings = BackupSettingsSection(),
        )

        val bytes = json.encodeToString(snapshot).toByteArray(StandardCharsets.UTF_8)
        outputStream.use { stream ->
            stream.write(bytes)
            stream.flush()
        }
        OmniBackupExportResult(
            counts = snapshot.exportedCounts(),
            byteCount = bytes.size.toLong(),
            createdAtEpochMillis = createdAt,
        )
    }

    suspend fun importBackup(
        inputStream: InputStream,
        mode: OmniRestoreMode = OmniRestoreMode.MERGE,
    ): OmniBackupImportResult = withContext(Dispatchers.IO) {
        require(mode == OmniRestoreMode.MERGE) { "Only merge restore is supported in this build" }

        val snapshot = readSnapshot(inputStream)
        validate(snapshot)

        val counts = database.withTransaction {
            restoreMerge(snapshot)
        }

        OmniBackupImportResult(
            counts = counts,
            formatVersion = snapshot.formatVersion,
            createdAtEpochMillis = snapshot.createdAtEpochMillis,
        )
    }

    private fun readSnapshot(inputStream: InputStream): OmniBackupSnapshot {
        val text = inputStream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
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

    private fun validate(snapshot: OmniBackupSnapshot) {
        require(snapshot.appName == "OmniTune") { "This backup was not created by OmniTune" }
        require(snapshot.formatVersion in 1..OMNI_BACKUP_FORMAT_VERSION) {
            "Unsupported backup format version ${snapshot.formatVersion}"
        }
    }

    private suspend fun MusicDatabase.restoreMerge(snapshot: OmniBackupSnapshot): OmniBackupCounts {
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
            val merged = song.toSongEntity(existing)
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
}

private fun OmniBackupSnapshot.exportedCounts() = OmniBackupCounts(
    songs = songs.size,
    likedSongs = songs.count { it.liked },
    playlists = playlists.size,
    playlistEntries = playlistSongs.size,
    artists = artists.size,
    albums = albums.size,
    historyItems = history.size,
    statRecords = stats.size,
)

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

private fun BackupSong.toSongEntity(existing: SongEntity?) = SongEntity(
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
    downloadState = existing?.downloadState ?: 0,
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
