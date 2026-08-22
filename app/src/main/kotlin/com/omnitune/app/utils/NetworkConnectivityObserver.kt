/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Synchronous availability checker used before lyric work begins.
 *
 * Callers do not collect connectivity changes, so registering a process-lifetime
 * callback would create work and a lifecycle burden without any consumer.
 */
class NetworkConnectivityObserver(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /** Checks current connectivity without treating an unvalidated network as offline. */
    fun isCurrentlyConnected(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

            networkCapabilities.isUsableForAppTraffic()
        } catch (e: Exception) {
            false
        }
    }

    private fun NetworkCapabilities?.isUsableForAppTraffic(): Boolean {
        if (this == null) return false
        return hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
