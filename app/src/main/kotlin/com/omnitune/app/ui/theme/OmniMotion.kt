/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.Alignment

object OmniMotion {
    const val FastFadeMillis = 140
    const val ScreenTransitionMillis = 220
    const val ThumbnailFadeMillis = 180

    /** Snappy spring for press-scale on buttons and cards. */
    fun <T> pressSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /** Gentle spring for icon/label size transitions. */
    fun <T> gentleSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Stagger offset in ms per list item index. */
    fun listItemDelayMs(index: Int, baseDelayMs: Int = 30, maxItems: Int = 8): Int =
        (index.coerceAtMost(maxItems) * baseDelayMs)

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

    /** MiniPlayer expands into view from the top of its slot. */
    fun miniPlayerEnter(): EnterTransition =
        fadeIn(animationSpec = tween(FastFadeMillis)) +
            expandVertically(
                animationSpec = tween(ScreenTransitionMillis, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Top,
            )

    /** MiniPlayer collapses away when hidden. */
    fun miniPlayerExit(): ExitTransition =
        fadeOut(animationSpec = tween(FastFadeMillis)) +
            shrinkVertically(
                animationSpec = tween(ScreenTransitionMillis, easing = FastOutSlowInEasing),
                shrinkTowards = Alignment.Top,
            )
}
