package com.omnitune.app.models

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerPresentationPreferenceMapperTest {

    @Test
    fun defaultStylesMatchVisibleSettingsDefaults() {
        assertEquals(PlayerStyle.LIQUID_GLASS, PlayerPresentationPreferenceMapper.DefaultPlayerStyle)
        assertEquals(MiniPlayerStyle.LIQUID_GLASS, PlayerPresentationPreferenceMapper.DefaultMiniPlayerStyle)
        assertEquals(SeekbarStyle.M3E_WAVY, PlayerPresentationPreferenceMapper.DefaultSeekbarStyle)
        assertEquals(ArtworkShape.ROUNDED_SQUARE, PlayerPresentationPreferenceMapper.DefaultArtworkShape)
        assertEquals(ArtworkSize.FULL, PlayerPresentationPreferenceMapper.DefaultArtworkSize)
    }

    @Test
    fun resolvesSavedPresentationValues() {
        assertEquals(
            PlayerStyle.YT_MUSIC,
            PlayerPresentationPreferenceMapper.resolvePlayerStyle("YT_MUSIC")
        )
        assertEquals(
            MiniPlayerStyle.FLOATING_PILL,
            PlayerPresentationPreferenceMapper.resolveMiniPlayerStyle("FLOATING_PILL")
        )
        assertEquals(
            SeekbarStyle.CLASSIC,
            PlayerPresentationPreferenceMapper.resolveSeekbarStyle("CLASSIC")
        )
        assertEquals(
            ArtworkShape.CIRCLE,
            PlayerPresentationPreferenceMapper.resolveArtworkShape("CIRCLE")
        )
        assertEquals(
            ArtworkSize.MEDIUM,
            PlayerPresentationPreferenceMapper.resolveArtworkSize("MEDIUM")
        )
    }

    @Test
    fun defaultsInvalidAndMissingPresentationValues() {
        assertEquals(
            PlayerPresentationPreferenceMapper.DefaultPlayerStyle,
            PlayerPresentationPreferenceMapper.resolvePlayerStyle(null)
        )
        assertEquals(
            PlayerPresentationPreferenceMapper.DefaultMiniPlayerStyle,
            PlayerPresentationPreferenceMapper.resolveMiniPlayerStyle("")
        )
        assertEquals(
            PlayerPresentationPreferenceMapper.DefaultSeekbarStyle,
            PlayerPresentationPreferenceMapper.resolveSeekbarStyle("waves")
        )
        assertEquals(
            PlayerPresentationPreferenceMapper.DefaultArtworkShape,
            PlayerPresentationPreferenceMapper.resolveArtworkShape("round")
        )
        assertEquals(
            PlayerPresentationPreferenceMapper.DefaultArtworkSize,
            PlayerPresentationPreferenceMapper.resolveArtworkSize("huge")
        )
    }

    @Test
    fun parsesLegacyLowercasePreferenceValuesDefensively() {
        assertEquals(
            PlayerStyle.LIQUID_GLASS,
            PlayerPresentationPreferenceMapper.resolvePlayerStyle(" liquid_glass ")
        )
        assertEquals(
            MiniPlayerStyle.YT_MUSIC,
            PlayerPresentationPreferenceMapper.resolveMiniPlayerStyle("yt_music")
        )
    }
}
