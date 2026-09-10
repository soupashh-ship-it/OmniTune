/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.PlaylistEntity
import com.omnitune.app.db.entities.PlaylistSongMap
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.utils.YouTubeUrlPolicy
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.utils.completed
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import javax.inject.Inject

sealed interface ImportState {
    data object Idle : ImportState
    data object Loading : ImportState

    data class Processing(
        val currentSong: String,
        val currentArtist: String,
        val thumbnail: String?,
        val totalSongs: Int,
        val currentIndex: Int,
        val status: String,
    ) : ImportState

    data class Success(
        val playlistId: String,
        val playlistName: String,
        val successCount: Int,
        val totalCount: Int,
        val failedSongs: List<Pair<String, String>>,
    ) : ImportState

    data class Error(val message: String) : ImportState
}

private data class ImportTrack(
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val sourceId: String? = null,
    val thumbnailUrl: String? = null,
    val setVideoId: String? = null,
)

@Serializable
private data class ImportedPlaylistBackup(
    val format: String? = null,
    val title: String? = null,
    val tracks: List<ImportedTrackBackup> = emptyList(),
)

@Serializable
private data class ImportedTrackBackup(
    val position: Int = 0,
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val durationMs: Long = 0L,
    val thumbnailUrl: String? = null,
    val url: String = "",
)

private val importJson = Json {
    ignoreUnknownKeys = true
}

@HiltViewModel
class ImportPlaylistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private var importJob: Job? = null

    fun importUrl(url: String) {
        startImport {
            importFromUrl(url)
        }
    }

    fun importM3U(uri: Uri) {
        startImport {
            parseM3U(uri)
        }
    }

    fun importCSV(uri: Uri) {
        startImport {
            parseCSV(uri)
        }
    }

    fun importOmniBackup(uri: Uri) {
        startImport {
            parseOmniBackup(uri)
        }
    }

    fun cancelImport() {
        importJob?.cancel()
        _importState.value = ImportState.Error("Import Cancelled")
    }

    fun resetImportState() {
        importJob?.cancel()
        _importState.value = ImportState.Idle
    }

    private fun startImport(loadTracks: suspend () -> Pair<String, List<ImportTrack>>) {
        if (importJob?.isActive == true) return
        importJob = viewModelScope.launch {
            try {
                _importState.value = ImportState.Loading
                val (playlistName, tracks) = withContext(Dispatchers.IO) {
                    loadTracks()
                }

                if (tracks.isEmpty()) {
                    _importState.value = ImportState.Error("No songs found")
                    return@launch
                }

                val resolvedSongs = mutableListOf<MediaMetadata>()
                val failedSongs = mutableListOf<Pair<String, String>>()
                val totalSongs = tracks.size

                tracks.forEachIndexed { index, track ->
                    ensureActive()
                    _importState.value = ImportState.Processing(
                        currentSong = track.title,
                        currentArtist = track.artist,
                        thumbnail = track.thumbnailUrl,
                        totalSongs = totalSongs,
                        currentIndex = index + 1,
                        status = "Importing",
                    )

                    val metadata = track.toMediaMetadataOrNull()
                    if (metadata != null) {
                        resolvedSongs += metadata
                    } else {
                        failedSongs += track.title to track.artist
                    }
                    delay(35)
                }

                if (resolvedSongs.isEmpty()) {
                    _importState.value = ImportState.Error(
                        "No playable songs were found. CSV and M3U imports need YouTube video IDs, YouTube URLs, or playable local file paths.",
                    )
                    return@launch
                }

                val playlistId = withContext(Dispatchers.IO) {
                    createImportedPlaylist(playlistName, resolvedSongs)
                }

                _importState.value = ImportState.Success(
                    playlistId = playlistId,
                    playlistName = playlistName.ifBlank { "Imported Playlist" },
                    successCount = resolvedSongs.size,
                    totalCount = tracks.size,
                    failedSongs = failedSongs,
                )
            } catch (error: CancellationException) {
                _importState.value = ImportState.Error("Import Cancelled")
            } catch (error: Exception) {
                _importState.value = ImportState.Error(error.message ?: "Import failed")
            }
        }
    }

    private suspend fun importFromUrl(url: String): Pair<String, List<ImportTrack>> {
        val playlistId = YouTubeUrlPolicy.extractPlaylistId(url)
            ?: throw IllegalArgumentException("Paste a YouTube or YouTube Music playlist link.")

        val page = YouTube.playlist(playlistId).completed().getOrThrow()
        return page.playlist.title to page.songs.map { song ->
            val metadata = song.toMediaMetadata()
            ImportTrack(
                title = metadata.title,
                artist = metadata.artists.joinToString(", ") { it.name }.ifBlank { "Unknown Artist" },
                album = metadata.album?.title.orEmpty(),
                durationMs = metadata.duration.toLong().coerceAtLeast(0L) * 1000L,
                sourceId = metadata.id,
                thumbnailUrl = metadata.thumbnailUrl,
                setVideoId = metadata.setVideoId,
            )
        }
    }

    private suspend fun parseM3U(uri: Uri): Pair<String, List<ImportTrack>> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<ImportTrack>()
        val playlistName = uri.importDisplayName("M3U Import")

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                var currentTitle: String? = null
                var currentArtist: String? = null
                var currentDurationMs = 0L

                while (true) {
                    val line = reader.readLine() ?: break
                    val trimmed = line.trim()
                    if (trimmed.startsWith("#EXTINF:")) {
                        val info = trimmed.substringAfter("#EXTINF:")
                        currentDurationMs = info.substringBefore(",").toLongOrNull()?.coerceAtLeast(0L)?.times(1000L) ?: 0L
                        val metadata = info.substringAfter(",", "")
                        if (" - " in metadata) {
                            currentArtist = metadata.substringBefore(" - ").trim()
                            currentTitle = metadata.substringAfter(" - ").trim()
                        } else if (metadata.isNotBlank()) {
                            currentTitle = metadata.trim()
                            currentArtist = "Unknown Artist"
                        }
                    } else if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                        val sourceId = extractPlayableSource(trimmed)
                        val title = currentTitle ?: trimmed.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
                        tracks += ImportTrack(
                            title = title.ifBlank { sourceId ?: "Unknown Song" },
                            artist = currentArtist ?: "Unknown Artist",
                            durationMs = currentDurationMs,
                            sourceId = sourceId,
                            thumbnailUrl = sourceId?.youtubeThumbnailUrl(),
                        )
                        currentTitle = null
                        currentArtist = null
                        currentDurationMs = 0L
                    }
                }
            }
        }

        playlistName to tracks
    }

    private suspend fun parseCSV(uri: Uri): Pair<String, List<ImportTrack>> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<ImportTrack>()
        val playlistName = uri.importDisplayName("CSV Import")

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                val records = csvRecords(reader).iterator()
                if (!records.hasNext()) return@use

                val firstRow = records.next()
                val headerIndexes = csvHeaderIndexes(firstRow)
                val hasHeader = headerIndexes.titleIndex != null || headerIndexes.artistIndex != null

                fun addTrack(row: List<String>) {
                    val titleIndex = headerIndexes.titleIndex ?: 0
                    val artistIndex = headerIndexes.artistIndex ?: 1
                    val title = row.getOrNull(titleIndex)?.trim().orEmpty()
                    if (title.isBlank()) return

                    val artist = row.getOrNull(artistIndex)?.trim()?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
                    val rawSource = headerIndexes.sourceIdIndex?.let { row.getOrNull(it) }
                        ?: headerIndexes.urlIndex?.let { row.getOrNull(it) }
                    val sourceId = rawSource?.trim()?.takeIf { it.isNotBlank() }?.let(::extractPlayableSource)
                    tracks += ImportTrack(
                        title = title,
                        artist = artist,
                        album = headerIndexes.albumIndex?.let { row.getOrNull(it) }?.trim().orEmpty(),
                        durationMs = parseCsvDuration(headerIndexes.durationIndex?.let { row.getOrNull(it) }),
                        sourceId = sourceId,
                        thumbnailUrl = sourceId?.youtubeThumbnailUrl(),
                    )
                }

                if (!hasHeader) addTrack(firstRow)
                while (records.hasNext()) addTrack(records.next())
            }
        }

        playlistName to tracks
    }

    private suspend fun parseOmniBackup(uri: Uri): Pair<String, List<ImportTrack>> = withContext(Dispatchers.IO) {
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
        parseOmniBackupJson(uri, content)?.let { return@withContext it }

        val tracksMap = linkedMapOf<String, ImportTrack>()
        var playlistName = uri.importDisplayName("OmniTune Import")
        var sequence = emptyList<String>()

        val metaStart = content.indexOf("[METADATA]")
        val metaEnd = content.indexOf("[/METADATA]")
        if (metaStart != -1 && metaEnd != -1 && metaStart < metaEnd) {
            content.substring(metaStart + 10, metaEnd).lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("title:")) {
                    playlistName = trimmed.substringAfter("title:").trim().ifBlank { playlistName }
                }
            }
        }

        val seqStart = content.indexOf("[SEQUENCE]")
        val seqEnd = content.indexOf("[/SEQUENCE]")
        if (seqStart != -1 && seqEnd != -1 && seqStart < seqEnd) {
            sequence = content.substring(seqStart + 10, seqEnd)
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }

        content.split("[SONG]").drop(1).forEach { rawBlock ->
            val block = rawBlock.substringBefore("[/SONG]")
            val fields = block.lines()
                .mapNotNull { line ->
                    val separator = line.indexOf(':')
                    if (separator <= 0) null else line.take(separator).trim() to line.drop(separator + 1).trim()
                }
                .toMap()
            val id = fields["id"].orEmpty()
            if (id.isBlank()) return@forEach

            tracksMap[id] = ImportTrack(
                title = fields["title"].orEmpty().ifBlank { id },
                artist = fields["artist"].orEmpty().ifBlank { "Unknown Artist" },
                album = fields["album"].orEmpty(),
                durationMs = fields["duration"]?.toLongOrNull() ?: 0L,
                sourceId = id,
                thumbnailUrl = id.youtubeThumbnailUrl(),
                setVideoId = id,
            )
        }

        val orderedTracks = if (sequence.isNotEmpty()) {
            sequence.mapNotNull { tracksMap[it] }
        } else {
            tracksMap.values.toList()
        }

        playlistName to orderedTracks
    }

    private fun parseOmniBackupJson(uri: Uri, content: String): Pair<String, List<ImportTrack>>? {
        if (!content.trimStart().startsWith("{")) return null

        val backup = runCatching {
            importJson.decodeFromString<ImportedPlaylistBackup>(content)
        }.getOrNull() ?: return null

        if (backup.format != "omnitune-playlist") return null

        val tracks = backup.tracks
            .sortedBy { it.position }
            .mapNotNull { track ->
                val sourceId = track.id.takeIf { it.isNotBlank() }
                    ?: extractPlayableSource(track.url)
                    ?: return@mapNotNull null
                ImportTrack(
                    title = track.title.ifBlank { sourceId },
                    artist = track.artist.ifBlank { "Unknown Artist" },
                    album = track.album,
                    durationMs = track.durationMs,
                    sourceId = sourceId,
                    thumbnailUrl = track.thumbnailUrl ?: sourceId.youtubeThumbnailUrl(),
                    setVideoId = sourceId,
                )
            }

        return (backup.title?.takeIf { it.isNotBlank() } ?: uri.importDisplayName("OmniTune Import")) to tracks
    }

    private suspend fun createImportedPlaylist(
        playlistName: String,
        songs: List<MediaMetadata>,
    ): String {
        val now = LocalDateTime.now()
        val playlistId = PlaylistEntity.generatePlaylistId()
        database.withTransaction {
            insert(
                PlaylistEntity(
                    id = playlistId,
                    name = playlistName.ifBlank { "Imported Playlist" },
                    bookmarkedAt = now,
                    lastUpdateTime = now,
                    thumbnailUrl = songs.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl,
                    isEditable = true,
                    isLocal = true,
                    remoteSongCount = songs.size,
                ),
            )
            songs.forEachIndexed { index, song ->
                insert(song) { entity ->
                    entity.copy(
                        inLibrary = entity.inLibrary ?: now,
                        isLocal = song.id.isLocalPlayableSource(),
                    )
                }
                insert(
                    PlaylistSongMap(
                        playlistId = playlistId,
                        songId = song.id,
                        position = index,
                        setVideoId = song.setVideoId,
                    ),
                )
            }
        }
        return playlistId
    }

    private fun ImportTrack.toMediaMetadataOrNull(): MediaMetadata? {
        val playableId = sourceId?.takeIf { it.isNotBlank() } ?: return null
        val title = title.ifBlank { playableId }
        val durationSeconds = (durationMs / 1000L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
            .takeIf { it > 0 }
            ?: -1

        return MediaMetadata(
            id = playableId,
            title = title,
            artists = listOf(MediaMetadata.Artist(id = null, name = artist.ifBlank { "Unknown Artist" })),
            duration = durationSeconds,
            thumbnailUrl = thumbnailUrl,
            album = album.takeIf { it.isNotBlank() }?.let {
                MediaMetadata.Album(
                    id = "import_album_${it.lowercase().hashCode()}",
                    title = it,
                )
            },
            setVideoId = setVideoId,
        )
    }

    private data class CsvHeaderIndexes(
        val titleIndex: Int? = null,
        val artistIndex: Int? = null,
        val albumIndex: Int? = null,
        val durationIndex: Int? = null,
        val sourceIdIndex: Int? = null,
        val urlIndex: Int? = null,
    )

    private fun csvHeaderIndexes(row: List<String>): CsvHeaderIndexes {
        fun normalized(value: String): String = value
            .removePrefix("\uFEFF")
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "")

        fun find(vararg names: String): Int? {
            val accepted = names.toSet()
            return row.indexOfFirst { normalized(it) in accepted }.takeIf { it >= 0 }
        }

        return CsvHeaderIndexes(
            titleIndex = find("trackname", "title", "song", "songname", "tracktitle"),
            artistIndex = find("artistnames", "artist", "artists", "artistname", "performer"),
            albumIndex = find("albumname", "album"),
            durationIndex = find("durationms", "duration", "length", "durationmillis"),
            sourceIdIndex = find("youtubeid", "videoid", "youtubevideoid", "id"),
            urlIndex = find("url", "link", "youtubeurl", "songurl"),
        )
    }

    private fun parseCsvDuration(value: String?): Long {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return 0L
        raw.toLongOrNull()?.let { numeric ->
            return if (numeric >= 10_000L) numeric else numeric * 1000L
        }
        val parts = raw.split(":")
        if (parts.size == 2) {
            val minutes = parts[0].toLongOrNull() ?: return 0L
            val seconds = parts[1].toLongOrNull() ?: return 0L
            return (minutes * 60L + seconds) * 1000L
        }
        return 0L
    }

    private fun csvRecords(reader: BufferedReader): Sequence<List<String>> = sequence {
        val record = StringBuilder()
        var insideQuotes = false

        while (true) {
            val line = reader.readLine() ?: break
            if (record.isNotEmpty()) record.append('\n')
            record.append(line)

            var index = 0
            while (index < line.length) {
                if (line[index] == '"') {
                    if (index + 1 < line.length && line[index + 1] == '"') {
                        index++
                    } else {
                        insideQuotes = !insideQuotes
                    }
                }
                index++
            }

            if (!insideQuotes) {
                yield(parseCsvRow(record.toString()))
                record.clear()
            }
        }

        if (record.isNotBlank()) yield(parseCsvRow(record.toString()))
    }

    private fun parseCsvRow(record: String): List<String> {
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var insideQuotes = false
        var index = 0

        while (index < record.length) {
            when (val character = record[index]) {
                '"' -> {
                    if (insideQuotes && index + 1 < record.length && record[index + 1] == '"') {
                        field.append('"')
                        index++
                    } else {
                        insideQuotes = !insideQuotes
                    }
                }
                ',' -> if (insideQuotes) {
                    field.append(character)
                } else {
                    fields += field.toString().trim()
                    field.clear()
                }
                else -> field.append(character)
            }
            index++
        }
        fields += field.toString().trim()
        return fields
    }

    private fun extractPlayableSource(value: String): String? =
        value.trim().takeIf(YouTubeUrlPolicy::isYouTubeVideoId)
            ?: YouTubeUrlPolicy.extractVideoId(value)
            ?: value.trim().takeIf { it.isLocalPlayableSource() }

    private fun Uri.importDisplayName(fallback: String): String =
        lastPathSegment
            ?.substringAfterLast('/')
            ?.substringBeforeLast('.')
            ?.takeIf { it.isNotBlank() }
            ?: fallback

    private fun String.youtubeThumbnailUrl(): String? =
        takeIf(YouTubeUrlPolicy::isYouTubeVideoId)?.let { "https://img.youtube.com/vi/$it/maxresdefault.jpg" }

    private fun String.isLocalPlayableSource(): Boolean {
        val clean = trim()
        return clean.startsWith("content://", ignoreCase = true) ||
            clean.startsWith("file://", ignoreCase = true) ||
            clean.startsWith("/")
    }
}
