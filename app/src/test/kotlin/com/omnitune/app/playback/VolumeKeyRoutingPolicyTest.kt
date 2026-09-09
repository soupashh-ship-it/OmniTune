package com.omnitune.app.playback

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeKeyRoutingPolicyTest {
    @Test
    fun `volume keys are intercepted only while music plays and overlay is enabled`() {
        assertTrue(
            VolumeKeyRoutingPolicy.shouldIntercept(
                isSongPlaying = true,
                volumeSliderEnabled = true,
                keyCode = KeyEvent.KEYCODE_VOLUME_UP,
            ),
        )
        assertFalse(
            VolumeKeyRoutingPolicy.shouldIntercept(
                isSongPlaying = false,
                volumeSliderEnabled = true,
                keyCode = KeyEvent.KEYCODE_VOLUME_UP,
            ),
        )
        assertFalse(
            VolumeKeyRoutingPolicy.shouldIntercept(
                isSongPlaying = true,
                volumeSliderEnabled = false,
                keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
            ),
        )
    }

    @Test
    fun `non-volume keys always use normal activity dispatch`() {
        assertFalse(
            VolumeKeyRoutingPolicy.shouldIntercept(
                isSongPlaying = true,
                volumeSliderEnabled = true,
                keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            ),
        )
    }
}
