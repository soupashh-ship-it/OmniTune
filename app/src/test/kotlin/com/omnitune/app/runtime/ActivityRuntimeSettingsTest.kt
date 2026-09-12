package com.omnitune.app.runtime

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.omnitune.app.constants.ForceMaxRefreshRateKey
import com.omnitune.app.constants.KeepScreenOnKey
import com.omnitune.app.constants.MiniPlayerAlphaKey
import com.omnitune.app.constants.MiniPlayerBlurKey
import com.omnitune.app.constants.NavBarAlphaKey
import com.omnitune.app.constants.PictureInPictureEnabledKey
import com.omnitune.app.constants.VolumeSliderEnabledKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityRuntimeSettingsTest {
    @Test
    fun `defaults keep visible activity settings functional`() {
        val settings = emptyPreferences().toActivityRuntimeSettings()

        assertTrue(settings.pictureInPictureEnabled)
        assertTrue(settings.volumeSliderEnabled)
        assertFalse(settings.forceMaxRefreshRate)
        assertFalse(settings.keepScreenOn)
        assertEquals(1f, settings.navBarAlpha)
        assertEquals(0f, settings.miniPlayerAlpha)
    }

    @Test
    fun `runtime mapper clamps alpha and blur settings`() {
        val settings = mutablePreferencesOf(
            PictureInPictureEnabledKey to false,
            VolumeSliderEnabledKey to false,
            ForceMaxRefreshRateKey to true,
            KeepScreenOnKey to true,
            NavBarAlphaKey to 2f,
            MiniPlayerAlphaKey to -1f,
            MiniPlayerBlurKey to 120f,
        ).toActivityRuntimeSettings()

        assertFalse(settings.pictureInPictureEnabled)
        assertFalse(settings.volumeSliderEnabled)
        assertTrue(settings.forceMaxRefreshRate)
        assertTrue(settings.keepScreenOn)
        assertEquals(1f, settings.navBarAlpha)
        assertEquals(0f, settings.miniPlayerAlpha)
        assertEquals(100f, settings.miniPlayerBlur)
    }
}
