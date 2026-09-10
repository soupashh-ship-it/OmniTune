/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.models

import java.util.Locale

object PlayerPresentationPreferenceMapper {
    val DefaultPlayerStyle: PlayerStyle = PlayerStyle.LIQUID_GLASS
    val DefaultMiniPlayerStyle: MiniPlayerStyle = MiniPlayerStyle.LIQUID_GLASS
    val DefaultSeekbarStyle: SeekbarStyle = SeekbarStyle.M3E_WAVY
    val DefaultArtworkShape: ArtworkShape = ArtworkShape.ROUNDED_SQUARE
    val DefaultArtworkSize: ArtworkSize = ArtworkSize.FULL

    fun resolvePlayerStyle(value: String?): PlayerStyle =
        parseEnumValue(value, DefaultPlayerStyle)

    fun resolveMiniPlayerStyle(value: String?): MiniPlayerStyle =
        parseEnumValue(value, DefaultMiniPlayerStyle)

    fun resolveSeekbarStyle(value: String?): SeekbarStyle =
        parseEnumValue(value, DefaultSeekbarStyle)

    fun resolveArtworkShape(value: String?): ArtworkShape =
        parseEnumValue(value, DefaultArtworkShape)

    fun resolveArtworkSize(value: String?): ArtworkSize =
        parseEnumValue(value, DefaultArtworkSize)

    private inline fun <reified T : Enum<T>> parseEnumValue(value: String?, defaultValue: T): T {
        val normalized = value
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.uppercase(Locale.ROOT)
            ?: return defaultValue

        return runCatching { enumValueOf<T>(normalized) }.getOrDefault(defaultValue)
    }
}
