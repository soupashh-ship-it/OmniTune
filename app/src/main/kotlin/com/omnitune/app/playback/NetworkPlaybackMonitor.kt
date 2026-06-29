/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.widget.Toast
import androidx.media3.common.Player
import com.omnitune.app.data.StreamExtractor
import com.omnitune.app.models.PlaybackQualityMode
import com.omnitune.app.utils.isInternetAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class NetworkPlaybackMonitor(
    private val context: Context,
    private val player: Player,
    private val scope: CoroutineScope,
    private val streamExtractor: StreamExtractor,
    private val downloadUtil: DownloadUtil,
    private val waitingForNetworkConnection: MutableStateFlow<Boolean>,
    private val playbackQualityModeProvider: suspend () -> PlaybackQualityMode,
    private val isDownloadCompleted: (String?) -> Boolean,
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var lastNetworkTransport: Int = -1

    fun register() {
        if (networkCallback != null) return

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val currentTransport = when {
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkCapabilities.TRANSPORT_WIFI
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkCapabilities.TRANSPORT_CELLULAR
                    else -> -1
                }
                if (lastNetworkTransport != -1 && lastNetworkTransport != currentTransport) {
                    Timber.tag("MusicService").i("Network transport changed ($lastNetworkTransport -> $currentTransport)")
                    StreamUrlResolver.clearMemoryCache("Network type changed")
                    recoverCurrentStreamOnNetworkChange()
                }
                lastNetworkTransport = currentTransport
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(request, callback)
            networkCallback = callback
        } catch (e: Exception) {
            Timber.w(e, "Failed to register network callback")
        }
    }

    fun release() {
        val callback = networkCallback ?: return
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            Timber.w(e, "Failed to unregister network callback")
        } finally {
            networkCallback = null
        }
    }

    fun handleNetworkError(mediaId: String?): Boolean {
        val hasInternet = isInternetAvailable(context)
        if (!hasInternet) {
            waitingForNetworkConnection.value = true
            val message = if (mediaId != null && !isDownloadCompleted(mediaId)) {
                "This song is not downloaded and cannot play offline."
            } else {
                "No internet connection. Retry when online."
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            return true
        } else if (lastNetworkTransport == NetworkCapabilities.TRANSPORT_WIFI) {
            val message = "Playback failed on this network. Try another Wi-Fi, disable VPN/Private DNS, or switch to mobile data."
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
        return false
    }

    private fun recoverCurrentStreamOnNetworkChange() {
        scope.launch(Dispatchers.Main) {
            if (!player.isPlaying && player.playbackState != Player.STATE_BUFFERING) return@launch

            val currentPos = player.currentPosition
            val currentIndex = player.currentMediaItemIndex
            val currentItem = player.currentMediaItem ?: return@launch
            val mediaId = currentItem.mediaId

            if (StreamUrlResolver.isYouTubeVideoId(Uri.parse(mediaId))) {
                val originalItem = currentItem.buildUpon().setUri(mediaId).build()
                val resolved = withContext(Dispatchers.IO) {
                    StreamUrlResolver.resolveMediaItem(
                        originalItem,
                        streamExtractor,
                        downloadUtil,
                        playbackQualityModeProvider(),
                    )
                }
                if (resolved != null) {
                    player.replaceMediaItem(currentIndex, resolved)
                    player.seekTo(currentIndex, currentPos)
                    player.prepare()
                    player.play()
                }
            }
        }
    }
}
