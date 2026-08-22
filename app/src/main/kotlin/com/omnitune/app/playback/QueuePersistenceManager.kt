package com.omnitune.app.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.QueueEntity
import com.omnitune.app.playback.queues.Queue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class QueuePersistenceManager(
    private val player: Player,
    private val database: MusicDatabase,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private var saveQueueJob: Job? = null

    suspend fun restoreQueueMetadataOnly(
        queue: Queue,
        onMetadataRestored: (androidx.media3.common.MediaMetadata?) -> Unit,
        onQueueTitleRestored: (String?) -> Unit
    ): Queue {
        val initialStatus = queue.getInitialStatus()
        if (initialStatus.items.isEmpty()) {
            Timber.tag("OmniTunePlaybackTrace").w("Restore skipped: saved queue is empty")
            return queue
        }

        val restoredIndex = initialStatus.mediaItemIndex.coerceIn(0, initialStatus.items.size - 1)
        onQueueTitleRestored(initialStatus.title)

        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
        player.setMediaItems(
            initialStatus.items.map { it.withOriginalVideoIdUri() },
            restoredIndex,
            initialStatus.position.coerceAtLeast(0L)
        )

        onMetadataRestored(player.currentMediaItem?.mediaMetadata)

        Timber.tag("OmniTunePlaybackTrace").i(
            "Restored queue metadata only: items=${initialStatus.items.size}, index=$restoredIndex, current=${player.currentMediaItem?.mediaId}"
        )
        return queue
    }

    fun saveQueueState(queueTitle: String?) {
        saveQueueJob?.cancel()
        saveQueueJob = scope.launch(ioDispatcher) {
            try {
                delay(1000) // Debounce

                val (count, currentIndex, currentPos) = withContext(Dispatchers.Main) {
                    Triple(player.mediaItemCount, player.currentMediaItemIndex, player.currentPosition)
                }

                if (count == 0) {
                    database.clearQueue()
                    return@launch
                }

                val mediaIds = mutableListOf<String>()
                withContext(Dispatchers.Main) {
                    for (i in 0 until count) {
                        mediaIds.add(player.getMediaItemAt(i).mediaId)
                    }
                }

                val entity = QueueEntity(
                    id = 1,
                    title = queueTitle,
                    mediaIdList = mediaIds.joinToString(","),
                    startIndex = currentIndex,
                    position = currentPos.coerceAtLeast(0L)
                )
                database.saveQueue(entity)
                Timber.tag("OmniTuneQueue").i("Queue saved: count=$count, index=$currentIndex, pos=$currentPos")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag("MusicService").e(e, "Error saving queue state")
            }
        }
    }

    private fun MediaItem.withOriginalVideoIdUri(): MediaItem {
        return if (StreamUrlResolver.isYouTubeVideoId(Uri.parse(mediaId))) {
            buildUpon()
                .setUri(mediaId)
                .setCustomCacheKey(mediaId)
                .build()
        } else {
            this
        }
    }
}
