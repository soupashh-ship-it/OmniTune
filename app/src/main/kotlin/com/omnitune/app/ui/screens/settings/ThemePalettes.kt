package com.omnitune.app.ui.screens.settings

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class ThemePalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val neutral: Color
)

object ThemePalettes {
    fun generateRandomPalette(): ThemePalette {
        fun randomColor(): Color = Color(0xFF000000L or (Random.nextLong() and 0xFFFFFFL))
        return ThemePalette(
            primary = randomColor(),
            secondary = randomColor(),
            tertiary = randomColor(),
            neutral = randomColor()
        )
    }
}
