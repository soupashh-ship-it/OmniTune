/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 *
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

/**
 * Staggered slide-up and fade-in entrance used by ported list sections.
 */
fun Modifier.animateEnter(
    index: Int,
    delayPerItem: Int = 20,
    slideDistance: Float = 24f,
): Modifier = composed {
    val hasAnimated = remember { mutableStateOf(false) }

    if (hasAnimated.value) {
        return@composed this
    }

    val alpha = remember { Animatable(0f) }
    val translationY = remember { Animatable(slideDistance) }

    LaunchedEffect(Unit) {
        val cappedIndex = index.coerceAtMost(8)
        val delay = cappedIndex * delayPerItem

        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 280,
                    delayMillis = delay,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
        launch {
            translationY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 320,
                    delayMillis = delay,
                    easing = FastOutSlowInEasing,
                ),
            )
        }

        hasAnimated.value = true
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = translationY.value
    }
}
