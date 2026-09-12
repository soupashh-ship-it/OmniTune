/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.runtime

import androidx.datastore.preferences.core.Preferences
import com.omnitune.app.constants.ArtworkShapeKey
import com.omnitune.app.constants.ForceMaxRefreshRateKey
import com.omnitune.app.constants.KeepScreenOnKey
import com.omnitune.app.constants.LiquidGlassEnabledKey
import com.omnitune.app.constants.MiniPlayerAlphaKey
import com.omnitune.app.constants.MiniPlayerBlurKey
import com.omnitune.app.constants.MiniPlayerStyleKey
import com.omnitune.app.constants.NavBarAlphaKey
import com.omnitune.app.constants.NavBarBlurKey
import com.omnitune.app.constants.PictureInPictureEnabledKey
import com.omnitune.app.constants.SwipeDownToDismissPlayerKey
import com.omnitune.app.constants.VolumeSliderEnabledKey
import com.omnitune.app.models.ArtworkShape
import com.omnitune.app.models.MiniPlayerStyle
import com.omnitune.app.models.PlayerPresentationPreferenceMapper

data class ActivityRuntimeSettings(
    val pictureInPictureEnabled: Boolean = true,
    val volumeSliderEnabled: Boolean = true,
    val forceMaxRefreshRate: Boolean = false,
    val miniPlayerStyle: MiniPlayerStyle = PlayerPresentationPreferenceMapper.DefaultMiniPlayerStyle,
    val miniPlayerAlpha: Float = 0f,
    val miniPlayerBlur: Float = 50f,
    val miniPlayerArtworkShape: String = PlayerPresentationPreferenceMapper.DefaultArtworkShape.name,
    val iosLiquidGlassEnabled: Boolean = true,
    val navBarAlpha: Float = 1f,
    val navBarBlur: Float = 60f,
    val swipeDownToDismissPlayer: Boolean = true,
    val keepScreenOn: Boolean = false,
)

fun Preferences.toActivityRuntimeSettings(): ActivityRuntimeSettings {
    val savedArtworkShape = this[ArtworkShapeKey] ?: ArtworkShape.ROUNDED_SQUARE.name
    return ActivityRuntimeSettings(
        pictureInPictureEnabled = this[PictureInPictureEnabledKey] ?: true,
        volumeSliderEnabled = this[VolumeSliderEnabledKey] ?: true,
        forceMaxRefreshRate = this[ForceMaxRefreshRateKey] ?: false,
        miniPlayerStyle = PlayerPresentationPreferenceMapper.resolveMiniPlayerStyle(this[MiniPlayerStyleKey]),
        miniPlayerAlpha = (this[MiniPlayerAlphaKey] ?: 0f).coerceIn(0f, 1f),
        miniPlayerBlur = (this[MiniPlayerBlurKey] ?: 50f).coerceIn(0f, 100f),
        miniPlayerArtworkShape = PlayerPresentationPreferenceMapper.resolveArtworkShape(savedArtworkShape).name,
        iosLiquidGlassEnabled = this[LiquidGlassEnabledKey] ?: true,
        navBarAlpha = (this[NavBarAlphaKey] ?: 1f).coerceIn(0f, 1f),
        navBarBlur = (this[NavBarBlurKey] ?: 60f).coerceIn(0f, 100f),
        swipeDownToDismissPlayer = this[SwipeDownToDismissPlayerKey] ?: true,
        keepScreenOn = this[KeepScreenOnKey] ?: false,
    )
}
