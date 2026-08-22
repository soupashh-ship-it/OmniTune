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
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.omnitune.app.utils.isInternetAvailable
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

class NetworkPlaybackMonitor(
    private val context: Context,
    private val waitingForNetworkConnection: MutableStateFlow<Boolean>,
    private val isDownloadCompleted: (String?) -> Boolean,
    private val onNetworkRestored: () -> Unit,
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val mainHandler = Handler(Looper.getMainLooper())
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
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkCapabilities.TRANSPORT_VPN
                    else -> -1
                }
                if (lastNetworkTransport != -1 && lastNetworkTransport != currentTransport) {
                    Timber.tag("MusicService").i("Network transport changed ($lastNetworkTransport -> $currentTransport)")
                    StreamUrlResolver.clearMemoryCache("Network type changed")
                }
                lastNetworkTransport = currentTransport

                // Keep a healthy stream running through Wi-Fi/mobile/VPN handovers. A new
                // lookup is only appropriate after an actual offline failure was reported.
                if (waitingForNetworkConnection.value &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                ) {
                    waitingForNetworkConnection.value = false
                    // Connectivity callbacks do not promise the player's application
                    // thread. Player reads/recovery must happen on the main thread.
                    mainHandler.post(onNetworkRestored)
                }
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
        } else if (lastNetworkTransport == NetworkCapabilities.TRANSPORT_WIFI ||
            lastNetworkTransport == NetworkCapabilities.TRANSPORT_VPN
        ) {
            val message = "Playback failed on this network. Try another Wi-Fi, DNS/VPN profile, or mobile data."
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
        return false
    }
}
