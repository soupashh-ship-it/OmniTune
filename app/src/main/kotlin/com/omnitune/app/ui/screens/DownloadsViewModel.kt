package com.omnitune.app.ui.screens

import android.app.PendingIntent
import androidx.media3.exoplayer.offline.Download
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.models.Song
import com.omnitune.app.models.SongSource
import com.omnitune.app.models.toPresentationSong
import com.omnitune.app.playback.DownloadUtil
import com.omnitune.app.playback.downloads
import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SongStatus(
    val song: Song,
    val status: DownloadUiStatus = DownloadUiStatus.COMPLETED,
    val progress: Float = 1.0f,
    val failureReason: String? = null
)

enum class DownloadUiStatus {
    RESOLVING,
    QUEUED,
    WAITING_FOR_NETWORK,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    PAUSED,
    REMOVING,
}

sealed class DownloadItem {
    data class SongItem(
        val song: Song,
        val status: DownloadUiStatus = DownloadUiStatus.COMPLETED,
        val progress: Float = 1.0f,
        val failureReason: String? = null,
        val bytesDownloaded: Long = 0L,
        val contentLength: Long = -1L,
    ) : DownloadItem()

    data class CollectionItem(
        val id: String,
        val title: String,
        val thumbnailUrl: String?,
        val songs: List<SongStatus>
    ) : DownloadItem()
}

internal data class DownloadSnapshot(
    val id: String,
    val title: String,
    val state: Int,
    val progressPercent: Float,
    val bytesDownloaded: Long,
    val contentLength: Long,
    val failureReason: Int,
    val isPlayable: Boolean,
)

internal object DownloadItemsMapper {
    fun build(
        snapshots: Collection<DownloadSnapshot>,
        resolving: Map<String, String>,
        songsById: Map<String, Song>,
        videoIds: Set<String>,
        waitingForNetwork: Boolean,
    ): List<DownloadItem.SongItem> {
        val snapshotItems = snapshots.map { snapshot ->
            val status = snapshot.status(waitingForNetwork)
            val song = (songsById[snapshot.id] ?: fallbackSong(snapshot.id, snapshot.title))
                .copy(
                    source = if (status == DownloadUiStatus.COMPLETED) SongSource.DOWNLOADED else SongSource.YOUTUBE,
                    isVideo = snapshot.id in videoIds,
                )
            DownloadItem.SongItem(
                song = song,
                status = status,
                progress = snapshot.progressFraction(status),
                failureReason = snapshot.failureText(status),
                bytesDownloaded = snapshot.bytesDownloaded,
                contentLength = snapshot.contentLength,
            )
        }

        val snapshotIds = snapshots.mapTo(mutableSetOf()) { it.id }
        val resolvingItems = resolving
            .filterKeys { it !in snapshotIds }
            .map { (id, title) ->
                val song = (songsById[id] ?: fallbackSong(id, title))
                    .copy(isVideo = id in videoIds)
                DownloadItem.SongItem(
                    song = song,
                    status = DownloadUiStatus.RESOLVING,
                    progress = 0f,
                )
            }

        return (resolvingItems + snapshotItems)
            .filterNot { it.status == DownloadUiStatus.REMOVING }
            .sortedWith(
                compareBy<DownloadItem.SongItem> { it.status.sortWeight }
                    .thenBy { it.song.title.lowercase() }
            )
    }

    private val DownloadUiStatus.sortWeight: Int
        get() = when (this) {
            DownloadUiStatus.DOWNLOADING -> 0
            DownloadUiStatus.RESOLVING -> 1
            DownloadUiStatus.QUEUED -> 2
            DownloadUiStatus.WAITING_FOR_NETWORK -> 3
            DownloadUiStatus.PAUSED -> 4
            DownloadUiStatus.FAILED -> 5
            DownloadUiStatus.COMPLETED -> 6
            DownloadUiStatus.REMOVING -> 7
        }

    private fun DownloadSnapshot.status(waitingForNetwork: Boolean): DownloadUiStatus = when (state) {
        Download.STATE_COMPLETED -> if (isPlayable) DownloadUiStatus.COMPLETED else DownloadUiStatus.FAILED
        Download.STATE_FAILED -> DownloadUiStatus.FAILED
        Download.STATE_DOWNLOADING -> DownloadUiStatus.DOWNLOADING
        Download.STATE_QUEUED -> if (waitingForNetwork) DownloadUiStatus.WAITING_FOR_NETWORK else DownloadUiStatus.QUEUED
        Download.STATE_STOPPED -> DownloadUiStatus.PAUSED
        Download.STATE_REMOVING -> DownloadUiStatus.REMOVING
        else -> DownloadUiStatus.QUEUED
    }

    private fun DownloadSnapshot.progressFraction(status: DownloadUiStatus): Float = when {
        status == DownloadUiStatus.COMPLETED -> 1f
        progressPercent in 0f..100f -> progressPercent / 100f
        contentLength > 0L -> (bytesDownloaded.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
        else -> 0f
    }

    private fun DownloadSnapshot.failureText(status: DownloadUiStatus): String? = when {
        status != DownloadUiStatus.FAILED -> null
        state == Download.STATE_COMPLETED && !isPlayable -> "Downloaded file is incomplete. Tap retry."
        failureReason != 0 -> "Download failed. Tap retry."
        else -> "Download failed. Tap retry."
    }

    private fun fallbackSong(id: String, title: String): Song = Song(
        id = id,
        title = title.ifBlank { id },
        artist = "",
        source = SongSource.YOUTUBE,
    )
}

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val database: MusicDatabase,
    private val downloadUtil: DownloadUtil
) : ViewModel() {

    private val _pendingIntent = MutableStateFlow<PendingIntent?>(null)
    val pendingIntent = _pendingIntent.asStateFlow()

    fun consumePendingIntent() {
        _pendingIntent.value = null
    }

    private val _storageInfo = MutableStateFlow<DownloadUtil.StorageInfo?>(null)
    val storageInfo: StateFlow<DownloadUtil.StorageInfo?> = _storageInfo.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val downloadItems: StateFlow<List<DownloadItem>> = combine(
        downloadUtil.downloads,
        downloadUtil.resolvingDownloads,
        database.allSongs(),
        database.videoIds(),
    ) { downloads, resolving, songs, videoIds ->
        val songsById = songs.associate { dbSong ->
            dbSong.id to dbSong.toPresentationSong()
        }
        val videoIdSet = videoIds.toSet()
        val waitingForNetwork = runCatching {
            downloadUtil.downloadManager.notMetRequirements != 0
        }.getOrDefault(false)
        val snapshots = downloads.values.map { download ->
            val isPlayable = runCatching { downloadUtil.isPlayable(download) }.getOrDefault(false)
            download.toSnapshot(isPlayable)
        }
        DownloadItemsMapper.build(
            snapshots = snapshots,
            resolving = resolving,
            songsById = songsById,
            videoIds = videoIdSet,
            waitingForNetwork = waitingForNetwork,
        )
    }.flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedSongs: StateFlow<List<Song>> = downloadItems
        .map { items ->
            items.filterIsInstance<DownloadItem.SongItem>()
                .filter { it.status == DownloadUiStatus.COMPLETED && !it.song.isVideo }
                .map { it.song }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedVideos: StateFlow<List<Song>> = downloadItems
        .map { items ->
            items.filterIsInstance<DownloadItem.SongItem>()
                .filter { it.status == DownloadUiStatus.COMPLETED && it.song.isVideo }
                .map { it.song }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedSongIds: StateFlow<Set<String>> = _selectedSongIds

    val isSelectionMode = _selectedSongIds.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            downloadItems
                .map { items -> items.filterIsInstance<DownloadItem.SongItem>().mapTo(mutableSetOf()) { it.song.id } }
                .collect { activeIds ->
                    _selectedSongIds.update { selected -> selected.intersect(activeIds) }
                }
        }
        refreshDownloads()
    }

    fun deleteDownload(songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                downloadUtil.removeDownload(songId)
                _selectedSongIds.update { it - songId }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun retryDownload(songId: String) {
        val item = downloadItems.value
            .filterIsInstance<DownloadItem.SongItem>()
            .firstOrNull { it.song.id == songId }
        downloadUtil.enqueue(songId, item?.song?.title.orEmpty())
    }

    fun toggleSelection(songId: String) {
        _selectedSongIds.update { current ->
            if (current.contains(songId)) current - songId else current + songId
        }
    }

    fun selectAll(songIds: Collection<String>? = null) {
        val allIds = songIds?.toSet()
            ?: downloadItems.value
                .filterIsInstance<DownloadItem.SongItem>()
                .map { it.song.id }
                .toSet()
        _selectedSongIds.value = allIds
    }

    fun clearSelection() {
        _selectedSongIds.value = emptySet()
    }

    fun deleteSelected() {
        val toDelete = _selectedSongIds.value.toList()
        viewModelScope.launch(Dispatchers.IO) {
            toDelete.forEach { downloadUtil.removeDownload(it) }
            clearSelection()
        }
    }

    fun deleteAll() {
        val all = downloadItems.value
            .filterIsInstance<DownloadItem.SongItem>()
            .map { it.song.id }
        viewModelScope.launch(Dispatchers.IO) {
            all.forEach { downloadUtil.removeDownload(it) }
            clearSelection()
        }
    }

    fun refreshDownloads() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                downloadUtil.refreshDownloadIndex()
                _storageInfo.value = downloadUtil.getStorageInfo()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun startDownload(
        videoId: String,
        title: String,
        resolvedStreamUrl: String? = null,
        onResult: (success: Boolean, message: String) -> Unit = { _, _ -> }
    ) {
        downloadUtil.enqueue(videoId, title, resolvedStreamUrl, onResult)
    }

    private fun Download.toSnapshot(isPlayable: Boolean): DownloadSnapshot {
        val fallbackTitle = runCatching {
            request.data.toString(Charsets.UTF_8)
        }.getOrDefault(request.id)
        return DownloadSnapshot(
            id = request.id,
            title = fallbackTitle,
            state = state,
            progressPercent = percentDownloaded,
            bytesDownloaded = bytesDownloaded,
            contentLength = contentLength,
            failureReason = failureReason,
            isPlayable = isPlayable,
        )
    }
}
