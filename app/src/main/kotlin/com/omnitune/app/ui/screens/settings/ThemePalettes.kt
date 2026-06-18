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
            primary = 0xFF000000L or (Random.nextLong() and 0xFFFFFFL),
            secondary = 0xFF000000L or (Random.nextLong() and 0xFFFFFFL),
            tertiary = 0xFF000000L or (Random.nextLong() and 0xFFFFFFL),
            neutral = 0xFF000000L or (Random.nextLong() and 0xFFFFFFL)
        )
    }
}
