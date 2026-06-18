package com.omnitune.app.ui.player

import android.content.Context

object CanvasArtworkPlaybackCache {
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
    }
}
