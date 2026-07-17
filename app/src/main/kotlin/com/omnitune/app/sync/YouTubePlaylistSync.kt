package com.omnitune.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.omnitune.app.constants.DataSyncIdKey
import com.omnitune.app.constants.InnerTubeCookieKey
import com.omnitune.app.constants.SelectedYtmPlaylistsKey
import com.omnitune.app.constants.UseLoginForBrowse
import com.omnitune.app.constants.YtmLastSyncAtKey
import com.omnitune.app.constants.YtmLastSyncErrorKey
import com.omnitune.app.constants.YtmLastSyncStatusKey
import com.omnitune.app.constants.YtmSyncKey
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.PlaylistEntity
import com.omnitune.app.db.entities.PlaylistSongMap
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.utils.SecurePreferenceCipher
import com.omnitune.app.utils.dataStore
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.utils.completed
import androidx.datastore.preferences.core.edit
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

private const val SYNC_WORK_NAME = "youtube-playlist-sync"
private const val PLAYLISTS_BROWSE_ID = "FEmusic_liked_playlists"

fun parseSelectedYouTubePlaylists(value: String): Set<String> =
    value.split(',').map(String::trim).filter(String::isNotEmpty).toSet()

fun scheduleYouTubePlaylistSync(context: Context, enabled: Boolean) {
    val workManager = WorkManager.getInstance(context)
    if (!enabled) {
        workManager.cancelUniqueWork(SYNC_WORK_NAME)
        return
    }
    val request = PeriodicWorkRequestBuilder<YouTubePlaylistSyncWorker>(12, TimeUnit.HOURS)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    workManager.enqueueUniquePeriodicWork(SYNC_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface YouTubePlaylistSyncEntryPoint {
    fun database(): MusicDatabase
}

class YouTubePlaylistSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    private val database = EntryPointAccessors.fromApplication(
        context.applicationContext,
        YouTubePlaylistSyncEntryPoint::class.java,
    ).database()

    override suspend fun doWork(): Result {
        val prefs = applicationContext.dataStore.data.first()
        if (prefs[YtmSyncKey] != true) return Result.success()
        val cookie = SecurePreferenceCipher.decryptOrPlain(prefs[InnerTubeCookieKey])
        val selectedIds = parseSelectedYouTubePlaylists(prefs[SelectedYtmPlaylistsKey].orEmpty())
        if (cookie.isBlank() || selectedIds.isEmpty()) return Result.success()

        YouTube.cookie = cookie
        YouTube.dataSyncId = prefs[DataSyncIdKey]
        YouTube.useLoginForBrowse = prefs[UseLoginForBrowse] ?: true

        return runCatching {
            val playlists = YouTube.library(PLAYLISTS_BROWSE_ID).completed().getOrThrow()
                .items.filterIsInstance<PlaylistItem>()
                .filter { it.id in selectedIds }
            val songCount = playlists.sumOf { syncYouTubePlaylist(database, it) }
            applicationContext.recordYouTubeSyncStatus(
                status = "Synced ${playlists.size} playlists, $songCount songs",
                error = "",
            )
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                applicationContext.recordYouTubeSyncStatus(
                    status = "Sync failed",
                    error = error.message ?: error::class.java.simpleName,
                )
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            },
        )
    }
}

suspend fun Context.recordYouTubeSyncStatus(status: String, error: String = "") {
    dataStore.edit { prefs ->
        prefs[YtmLastSyncAtKey] = System.currentTimeMillis()
        prefs[YtmLastSyncStatusKey] = status
        if (error.isBlank()) {
            prefs.remove(YtmLastSyncErrorKey)
        } else {
            prefs[YtmLastSyncErrorKey] = error.take(240)
        }
    }
}

suspend fun syncYouTubePlaylist(database: MusicDatabase, playlist: PlaylistItem): Int {
    val songs = YouTube.playlist(playlist.id).completed().getOrThrow().songs.map { it.toMediaMetadata() }
    val existing = database.playlistByBrowseId(playlist.id).first()?.playlist
    val now = LocalDateTime.now()
    val target = existing?.copy(
        name = playlist.title,
        browseId = playlist.id,
        thumbnailUrl = playlist.thumbnail,
        bookmarkedAt = existing.bookmarkedAt ?: now,
        lastUpdateTime = now,
        isEditable = playlist.isEditable,
        isAutoSync = true,
        remoteSongCount = playlist.songCountText?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() },
        playEndpointParams = playlist.playEndpoint?.params,
        shuffleEndpointParams = playlist.shuffleEndpoint?.params,
        radioEndpointParams = playlist.radioEndpoint?.params,
    ) ?: PlaylistEntity(
        name = playlist.title,
        browseId = playlist.id,
        thumbnailUrl = playlist.thumbnail,
        bookmarkedAt = now,
        lastUpdateTime = now,
        isEditable = playlist.isEditable,
        isAutoSync = true,
        remoteSongCount = playlist.songCountText?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() },
        playEndpointParams = playlist.playEndpoint?.params,
        shuffleEndpointParams = playlist.shuffleEndpoint?.params,
        radioEndpointParams = playlist.radioEndpoint?.params,
    )

    database.withTransaction {
        if (existing == null) insert(target) else update(target)
        clearPlaylist(target.id)
        songs.forEach(::insert)
        songs.mapIndexed { index, song ->
            PlaylistSongMap(songId = song.id, playlistId = target.id, position = index)
        }.forEach(::insert)
    }
    return songs.size
}
