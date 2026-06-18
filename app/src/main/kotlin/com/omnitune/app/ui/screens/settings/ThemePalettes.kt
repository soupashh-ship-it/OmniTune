package com.omnitune.app.ui.screens.settings

import kotlin.random.Random

data class ThemePalette(
    val primary: Long,
    val secondary: Long,
    val tertiary: Long,
    val neutral: Long
)

object ThemePalettes {
    fun generateRandomPalette(): ThemePalette {
        return ThemePalette(
            primary = Random.nextLong() and 0xFFFFFF,
            secondary = Random.nextLong() and 0xFFFFFF,
            tertiary = Random.nextLong() and 0xFFFFFF,
            neutral = Random.nextLong() and 0xFFFFFF
        )
    }
}
