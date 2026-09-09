/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.view.KeyEvent

internal object VolumeKeyRoutingPolicy {
    fun shouldIntercept(
        isSongPlaying: Boolean,
        volumeSliderEnabled: Boolean,
        keyCode: Int,
    ): Boolean =
        isSongPlaying &&
            volumeSliderEnabled &&
            (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
}
