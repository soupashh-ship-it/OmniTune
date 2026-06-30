/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

object OmniMotion {
    const val FastFadeMillis = 140
    const val ScreenTransitionMillis = 220
    const val ThumbnailFadeMillis = 180

    fun screenEnter(): EnterTransition =
        fadeIn(animationSpec = tween(FastFadeMillis)) +
            slideInVertically(
                animationSpec = tween(ScreenTransitionMillis, easing = FastOutSlowInEasing),
                initialOffsetY = { height -> height / 18 },
            )

    fun screenExit(): ExitTransition =
        fadeOut(animationSpec = tween(FastFadeMillis))

    fun screenPopEnter(): EnterTransition =
        fadeIn(animationSpec = tween(FastFadeMillis)) +
            slideInVertically(
                animationSpec = tween(ScreenTransitionMillis, easing = FastOutSlowInEasing),
                initialOffsetY = { height -> -height / 24 },
            )

    fun screenPopExit(): ExitTransition =
        fadeOut(animationSpec = tween(FastFadeMillis)) +
            slideOutVertically(
                animationSpec = tween(ScreenTransitionMillis, easing = FastOutSlowInEasing),
                targetOffsetY = { height -> height / 24 },
            )
}
