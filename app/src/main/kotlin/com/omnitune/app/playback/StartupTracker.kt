package com.omnitune.app.playback

import timber.log.Timber

object StartupTracker {
    var tapTimeMs = 0L
    var resolverStartTime = 0L
    var resolverDoneTime = 0L
    var playerPrepareTime = 0L
    var cacheHit = false
    var networkType = "WIFI"

    fun reset(tapMs: Long = System.currentTimeMillis()) {
        tapTimeMs = tapMs
        resolverStartTime = 0L
        resolverDoneTime = 0L
        playerPrepareTime = 0L
        cacheHit = false
    }

    fun logResolverStart() {
        resolverStartTime = System.currentTimeMillis()
        val elapsed = resolverStartTime - tapTimeMs
        Timber.tag("OmniTuneStartup").i("tap_to_resolver_start=ms cache= network=")
    }

    fun logResolverDone() {
        resolverDoneTime = System.currentTimeMillis()
        val elapsed = resolverDoneTime - tapTimeMs
        Timber.tag("OmniTuneStartup").i("resolver_done=ms")
    }

    fun logPlayerPrepare() {
        playerPrepareTime = System.currentTimeMillis()
    }

    fun logState(state: Int) {
        val now = System.currentTimeMillis()
        if (state == androidx.media3.common.Player.STATE_BUFFERING) {
            val elapsed = now - tapTimeMs
            Timber.tag("OmniTuneStartup").i("first_buffering=ms")
        } else if (state == androidx.media3.common.Player.STATE_READY) {
            val elapsed = now - tapTimeMs
            Timber.tag("OmniTuneStartup").i("player_ready=ms")
            Timber.tag("OmniTuneStartup").i("total_tap_to_ready=ms")
        }
    }
}
