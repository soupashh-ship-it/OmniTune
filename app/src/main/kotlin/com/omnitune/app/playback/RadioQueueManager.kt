/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import androidx.media3.exoplayer.ExoPlayer
import com.omnitune.app.data.StreamExtractor
import com.omnitune.app.extensions.currentMetadata
import com.omnitune.app.models.PlaybackQualityMode
import com.omnitune.app.playback.queues.Queue
import com.omnitune.app.playback.queues.YouTubeQueue
import com.omnitune.innertube.models.WatchEndpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class RadioQueueManager(
    private val player: ExoPlayer,
    private val scope: CoroutineScope,
    private val streamExtractor: StreamExtractor,
    private val downloadUtil: DownloadUtil,
    private val playbackQualityModeProvider: suspend () -> PlaybackQualityMode,
    private val setQueueTitle: (String?) -> Unit,
    private val setCurrentQueue: (Queue) -> Unit,
) {
    fun startRadioSeamlessly() {
        val currentMeta = player.currentMetadata ?: return
        val currentIndex = player.currentMediaItemIndex
        val currentMediaId = currentMeta.id

        scope.launch {
            val radioQueue = YouTubeQueue(
                endpoint = WatchEndpoint(videoId = currentMediaId)
            )
            val initialStatus = radioQueue.getInitialStatus()

            if (initialStatus.title != null) {
                setQueueTitle(initialStatus.title)
            }

            val radioItems = initialStatus.items.filter { item ->
                item.mediaId != currentMediaId
            }

            if (radioItems.isNotEmpty()) {
                // Resolve YouTube video IDs to playable stream URLs before adding to player.
                val resolvedRadioItems = withContext(Dispatchers.IO) {
                    StreamUrlResolver.resolveMediaItems(
                        radioItems,
                        streamExtractor,
                        downloadUtil,
                        playbackQualityModeProvider()
                    )
                }
                if (resolvedRadioItems.isNotEmpty()) {
                    val itemCount = player.mediaItemCount
                    if (itemCount > currentIndex + 1) {
                        player.removeMediaItems(currentIndex + 1, itemCount)
                    }
                    player.addMediaItems(currentIndex + 1, resolvedRadioItems)
                    Timber.tag("MusicService").i("Radio: added ${resolvedRadioItems.size} resolved tracks")
                } else {
                    Timber.tag("MusicService").w("Radio: all stream resolutions failed")
                }
            }

            setCurrentQueue(radioQueue)
        }
    }
}
